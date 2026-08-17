#!/usr/bin/env bash

set -uo pipefail

DB_CONTAINER="${DB_CONTAINER:-olympus-db}"
USER_AGENT="Olympus-Repair-Script/1.0 (+https://github.com/Kronos131-dev/olympus)"
SLEEP_BETWEEN_CALLS="0.4"

usage() {
  cat <<'TXT'
repair-nutrient-data.sh

Repare les aliments enregistres avant l'ajout des fibres, sucres, acides gras
satures, sel et micronutriments. Les repas et journaux qui les referencent sont
corriges du meme coup, sans y toucher : leurs totaux sont recalcules a la lecture.

  Phase 1  Aliments CIQUAL restes en double apres le reimport du referentiel.
           Les references sont repointees vers la ligne enrichie, puis la ligne
           obsolete est supprimee.
  Phase 2  Aliments Open Food Facts mis en cache avant l'enrichissement.
           Chaque code-barres est relu sur OFF pour completer les valeurs.

Usage :
  ./repair-nutrient-data.sh              dry-run : affiche ce qui serait fait
  ./repair-nutrient-data.sh --apply      applique reellement les changements

Prerequis : docker (conteneur olympus-db en place), curl, python3.
TXT
}

APPLY=false
case "${1:-}" in
  --apply) APPLY=true ;;
  --help|-h) usage; exit 0 ;;
  "") ;;
  *) usage; exit 1 ;;
esac

for bin in docker curl python3; do
  if ! command -v "$bin" >/dev/null 2>&1; then
    echo "Erreur : '$bin' est requis mais introuvable dans le PATH." >&2
    exit 1
  fi
done

if ! docker inspect "$DB_CONTAINER" >/dev/null 2>&1; then
  echo "Erreur : le conteneur '$DB_CONTAINER' n'existe pas ou n'est pas accessible." >&2
  exit 1
fi

psql_exec() {
  docker exec -i "$DB_CONTAINER" bash -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 "$@"' _ "$@"
}

psql_stdin() {
  docker exec -i "$DB_CONTAINER" bash -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 "$@" -f -' _ "$@"
}

off_number() {
  python3 -c '
import json, sys
try:
    data = json.loads(sys.argv[1])
except ValueError:
    print("")
    sys.exit(0)
value = data.get("product", {}).get("nutriments", {}).get(sys.argv[2])
if isinstance(value, (int, float)) and value >= 0:
    print(value)
else:
    print("")
' "$1" "$2"
}

off_status() {
  python3 -c '
import json, sys
try:
    print(json.loads(sys.argv[1]).get("status", ""))
except ValueError:
    print("")
' "$1"
}

echo "Mode : $([ "$APPLY" = true ] && echo 'APPLICATION REELLE' || echo 'dry-run (aucune ecriture - relancer avec --apply pour appliquer)')"
echo

echo "== Phase 1 : doublons CIQUAL =="

mapfile -t ciqual_rows < <(psql_exec -t -A -F $'\t' -c "
  SELECT ancien.id, neuf.id, ancien.name
    FROM food_items ancien
    JOIN food_items neuf
      ON neuf.ciqual_code IS NOT NULL
     AND lower(btrim(neuf.name, '\"')) = lower(btrim(ancien.name, '\"'))
   WHERE ancien.source = 'CIQUAL'
     AND ancien.ciqual_code IS NULL
   ORDER BY ancien.id;")

ciqual_total="${#ciqual_rows[@]}"
[[ "$ciqual_total" -eq 1 && -z "${ciqual_rows[0]}" ]] && ciqual_total=0

if [[ "$ciqual_total" -eq 0 ]]; then
  echo "Aucun doublon CIQUAL a reparer."
else
  echo "$ciqual_total ligne(s) obsolete(s) a repointer."
fi

ciqual_repaired=0
ciqual_failed=0

for row in "${ciqual_rows[@]}"; do
  [[ -z "$row" ]] && continue
  old_id="$(cut -f1 <<<"$row")"
  new_id="$(cut -f2 <<<"$row")"
  name="$(cut -f3 <<<"$row")"

  refs=$(psql_exec -t -A -c "
    SELECT (SELECT count(*) FROM log_entries WHERE food_item_id = $old_id)
         + (SELECT count(*) FROM meal_ingredients WHERE food_item_id = $old_id)
         + (SELECT count(*) FROM planned_meal_entries WHERE food_item_id = $old_id);")

  echo "[repointe] $name : id=$old_id -> id=$new_id ($refs reference(s))"

  if [[ "$APPLY" == true ]]; then
    if ! echo "
      BEGIN;
      UPDATE log_entries          SET food_item_id = $new_id WHERE food_item_id = $old_id;
      UPDATE meal_ingredients     SET food_item_id = $new_id WHERE food_item_id = $old_id;
      UPDATE planned_meal_entries SET food_item_id = $new_id WHERE food_item_id = $old_id;
      DELETE FROM food_items WHERE id = $old_id;
      COMMIT;" | psql_stdin >/dev/null 2>&1; then
      echo "  echec SQL pour id=$old_id" >&2
      ciqual_failed=$((ciqual_failed + 1))
      continue
    fi
  fi
  ciqual_repaired=$((ciqual_repaired + 1))
done

echo
echo "== Phase 2 : aliments Open Food Facts =="

mapfile -t off_rows < <(psql_exec -t -A -F $'\t' -c "
  SELECT id, barcode FROM food_items
   WHERE source = 'OFF'
     AND barcode IS NOT NULL AND barcode <> ''
     AND fibers100g IS NULL
     AND sugars100g IS NULL
     AND saturated_fat100g IS NULL
     AND salt100g IS NULL
   ORDER BY id;")

off_total="${#off_rows[@]}"
[[ "$off_total" -eq 1 && -z "${off_rows[0]}" ]] && off_total=0
echo "$off_total ligne(s) a examiner."

off_repaired=0
off_empty=0
off_not_found=0
off_failed=0

for row in "${off_rows[@]}"; do
  [[ -z "$row" ]] && continue
  id="${row%%$'\t'*}"
  barcode="${row#*$'\t'}"

  response=$(curl -s --max-time 8 -H "User-Agent: $USER_AGENT" \
    "https://world.openfoodfacts.org/api/v2/product/${barcode}.json?fields=status,nutriments")

  if [[ "$(off_status "$response")" != "1" ]]; then
    echo "[introuvable sur OFF] id=$id barcode=$barcode"
    off_not_found=$((off_not_found + 1))
    sleep "$SLEEP_BETWEEN_CALLS"
    continue
  fi

  fib=$(off_number "$response" "fiber_100g")
  sug=$(off_number "$response" "sugars_100g")
  sat=$(off_number "$response" "saturated-fat_100g")
  sel=$(off_number "$response" "salt_100g")

  if [[ -z "$fib" && -z "$sug" && -z "$sat" && -z "$sel" ]]; then
    echo "[rien a completer] id=$id barcode=$barcode"
    off_empty=$((off_empty + 1))
    sleep "$SLEEP_BETWEEN_CALLS"
    continue
  fi

  echo "[complete] id=$id barcode=$barcode fibres=${fib:--} sucres=${sug:--} ags=${sat:--} sel=${sel:--}"

  if [[ "$APPLY" == true ]]; then
    if ! echo "UPDATE food_items SET
        fibers100g        = NULLIF(:'fib','')::double precision,
        sugars100g        = NULLIF(:'sug','')::double precision,
        saturated_fat100g = NULLIF(:'sat','')::double precision,
        salt100g          = NULLIF(:'sel','')::double precision
      WHERE id = :id;" | \
      docker exec -i "$DB_CONTAINER" bash -c \
        'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -v id="$1" -v fib="$2" -v sug="$3" -v sat="$4" -v sel="$5" -f -' \
        _ "$id" "$fib" "$sug" "$sat" "$sel" >/dev/null 2>&1; then
      echo "  echec SQL pour id=$id" >&2
      off_failed=$((off_failed + 1))
      sleep "$SLEEP_BETWEEN_CALLS"
      continue
    fi
  fi
  off_repaired=$((off_repaired + 1))

  sleep "$SLEEP_BETWEEN_CALLS"
done

echo
echo "Resume"
echo "  CIQUAL : $ciqual_repaired repointe(s), $ciqual_failed echec(s)."
echo "  OFF    : $off_repaired complete(s), $off_empty sans valeur sur OFF, $off_not_found introuvable(s), $off_failed echec(s)."
if [[ "$APPLY" == false && $((ciqual_repaired + off_repaired)) -gt 0 ]]; then
  echo
  echo "Dry-run : relancer avec --apply pour appliquer ces $((ciqual_repaired + off_repaired)) correction(s)."
fi
