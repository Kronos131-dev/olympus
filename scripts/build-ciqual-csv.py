#!/usr/bin/env python3
"""Génère olympus-back/src/main/resources/ciqual.csv depuis la Table Ciqual de l'ANSES.

    python3 scripts/build-ciqual-csv.py "~/Téléchargements/Table Ciqual 2025_FR_2025_11_03.xlsx"

Le classeur source fait 84 colonnes ; on n'en garde que 26, et on ne garde que les aliments
bruts : les plats composés préemballés sont retirés parce qu'ils empêchent l'analyse photo de
décomposer une assiette en ingrédients (le matcher s'accroche à « Bœuf bourguignon, préemballé »
et on perd à la fois les micronutriments réels et la possibilité de corriger un ingrédient).

Aucune dépendance : un .xlsx est une archive zip de XML, lue ici avec la bibliothèque standard.
"""

import csv
import re
import sys
import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path

NS = "{http://schemas.openxmlformats.org/spreadsheetml/2006/main}"

DEFAULT_SOURCE = "~/Téléchargements/Table Ciqual 2025_FR_2025_11_03.xlsx"
DEFAULT_OUTPUT = Path(__file__).resolve().parent.parent / "olympus-back/src/main/resources/ciqual.csv"

# Groupes retirés : plats composés préemballés, sandwichs, pizzas, soupes, salades toutes faites
# d'un côté ; petits pots et laits infantiles de l'autre, hors sujet pour l'application.
EXCLUDED_GROUPS = {"entrées et plats composés", "aliments infantiles"}

# Index 0-based des colonnes de la feuille « composition nutritionnelle ».
COL_GROUP, COL_SUBGROUP = 3, 4
COL_CODE, COL_NAME = 6, 7
COL_RETINOL, COL_BETA_CAROTENE = 63, 64
COL_B9_FOLATES_TOTAUX = 79
COL_EPA, COL_DHA = 46, 47

# (en-tête de sortie, index source). Les colonnes calculées valent None et sont traitées à part.
COLUMNS = [
    ("code", COL_CODE),
    ("nom", COL_NAME),
    ("groupe", COL_GROUP),
    ("sous_groupe", COL_SUBGROUP),
    ("kcal", 10),               # Energie, Règlement UE N° 1169/2011
    ("proteines", 14),          # Protéines, N x facteur de Jones
    ("glucides", 16),
    ("lipides", 17),
    ("fibres_g", 26),
    ("sucres_g", 18),
    ("ags_g", 31),
    ("sel_g", 49),              # « Sel chlorure de sodium », mieux renseigné que la colonne sodium
    ("calcium_mg", 50),
    ("fer_mg", 53),
    ("magnesium_mg", 55),
    ("potassium_mg", 58),
    ("zinc_mg", 61),
    ("selenium_ug", 59),
    ("iode_ug", 54),
    ("vit_a_er_ug", 62),        # complétée par rétinol + β-carotène/12, voir vitamin_a_equivalent
    ("vit_c_mg", 72),
    ("vit_d_ug", 65),
    ("vit_b9_ug", 78),          # DFE, complétée par les folates totaux
    ("vit_b12_ug", 82),
    ("omega3_ala_g", 44),
    ("omega3_epadha_g", None),  # somme EPA + DHA
]

LESS_THAN = re.compile(r"^<\s*")


def column_index(cell_ref):
    letters = "".join(c for c in cell_ref if c.isalpha())
    index = 0
    for letter in letters:
        index = index * 26 + (ord(letter) - 64)
    return index - 1


def read_sheet(xlsx_path):
    """Renvoie la liste des lignes de la première feuille, chacune sous forme {index: texte}."""
    with zipfile.ZipFile(xlsx_path) as archive:
        shared = [
            "".join(node.text or "" for node in item.iter(NS + "t"))
            for item in ET.fromstring(archive.read("xl/sharedStrings.xml")).iter(NS + "si")
        ]
        sheet = ET.fromstring(archive.read("xl/worksheets/sheet1.xml"))

    def text_of(cell):
        if cell.get("t") == "inlineStr":
            return "".join(node.text or "" for node in cell.iter(NS + "t"))
        value = cell.find(NS + "v")
        if value is None or value.text is None:
            return ""
        return shared[int(value.text)] if cell.get("t") == "s" else value.text

    return [
        {column_index(cell.get("r")): text_of(cell) for cell in row if cell.get("r")}
        for row in sheet.iter(NS + "row")
    ]


def parse_value(raw):
    """Convertit une cellule CIQUAL en nombre, ou en None quand la valeur est inconnue.

    La distinction est le point qui décide de la crédibilité de tout l'écran micronutriments :
    « - » et une cellule vide signifient « non déterminé », pas « zéro ». Les confondre ferait
    afficher des carences imaginaires dès qu'un aliment n'a pas été analysé pour un nutriment.
    """
    text = (raw or "").strip().replace("\xa0", " ")
    if text in ("", "-"):
        return None
    if "race" in text.lower():          # « traces » : sous le seuil de détection
        return 0.0
    if text.startswith("<"):            # « < 0,5 » : censure à gauche, convention EFSA
        text = LESS_THAN.sub("", text)
        divisor = 2.0
    else:
        divisor = 1.0
    try:
        return float(text.replace(",", ".").replace(" ", "")) / divisor
    except ValueError:
        return None


def vitamin_a_equivalent(row):
    """Équivalents rétinol, en complétant la colonne pré-calculée quand elle est vide.

    Elle n'est renseignée que sur 56 % des aliments, alors que le rétinol seul l'est sur 77 % :
    recalculer rétinol + β-carotène/12 fait passer la couverture à 80 %.
    """
    precomputed = parse_value(row.get(62))
    if precomputed is not None:
        return precomputed
    retinol = parse_value(row.get(COL_RETINOL))
    beta_carotene = parse_value(row.get(COL_BETA_CAROTENE))
    if retinol is None and beta_carotene is None:
        return None
    return (retinol or 0.0) + (beta_carotene or 0.0) / 12.0


def folates(row):
    """Folates en µg, DFE d'abord puis folates totaux : 29 % de couverture deviennent 71 %."""
    dfe = parse_value(row.get(78))
    return dfe if dfe is not None else parse_value(row.get(COL_B9_FOLATES_TOTAUX))


def epa_plus_dha(row):
    epa = parse_value(row.get(COL_EPA))
    dha = parse_value(row.get(COL_DHA))
    if epa is None and dha is None:
        return None
    return (epa or 0.0) + (dha or 0.0)


def format_value(value):
    if value is None:
        return ""
    rounded = round(value, 3)
    return str(int(rounded)) if rounded == int(rounded) else f"{rounded:g}"


def build_row(row):
    values = []
    for header, source in COLUMNS:
        if header in ("code", "nom", "groupe", "sous_groupe"):
            values.append((row.get(source) or "").strip())
        elif header == "vit_a_er_ug":
            values.append(format_value(vitamin_a_equivalent(row)))
        elif header == "vit_b9_ug":
            values.append(format_value(folates(row)))
        elif header == "omega3_epadha_g":
            values.append(format_value(epa_plus_dha(row)))
        else:
            values.append(format_value(parse_value(row.get(source))))
    return values


def report_coverage(rows):
    headers = [header for header, _ in COLUMNS]
    print(f"\n{'nutriment':18s} {'renseigné':>10s}")
    for position, header in enumerate(headers):
        if position < 4:
            continue
        known = sum(1 for row in rows if row[position] != "")
        print(f"{header:18s} {100 * known / len(rows):9.1f}%")


def main():
    source = Path(sys.argv[1]).expanduser() if len(sys.argv) > 1 else Path(DEFAULT_SOURCE).expanduser()
    output = Path(sys.argv[2]) if len(sys.argv) > 2 else DEFAULT_OUTPUT

    sheet = read_sheet(source)
    print(f"source : {source}\n{len(sheet) - 1} aliments lus")

    rows = [
        build_row(row)
        for row in sheet[1:]
        if (row.get(COL_NAME) or "").strip() and (row.get(COL_GROUP) or "").strip() not in EXCLUDED_GROUPS
    ]
    print(f"{len(rows)} aliments retenus après retrait de {', '.join(sorted(EXCLUDED_GROUPS))}")

    with output.open("w", encoding="utf-8", newline="\n") as handle:
        writer = csv.writer(handle, quoting=csv.QUOTE_MINIMAL, lineterminator="\n")
        writer.writerow([header for header, _ in COLUMNS])
        writer.writerows(rows)

    report_coverage(rows)
    print(f"\nécrit : {output}")


if __name__ == "__main__":
    main()
