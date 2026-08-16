#!/usr/bin/env bash
# Répare les FoodItem importés depuis Open Food Facts avant le correctif
# a234b7c ("fix: read the product name back from Open Food Facts").
#
# Avant ce correctif, productName/genericName n'étaient jamais mappés
# (clé JSON snake_case côté OFF, champ Java camelCase sans @JsonProperty),
# donc tout produit scanné a été mis en cache avec name = "Marque - Produit
# Inconnu". Ce script relit chaque code-barres concerné sur Open Food
# Facts et corrige le nom en base — les repas/journaux qui référencent
# ces lignes par id sont réparés du même coup, sans y toucher.
#
# Usage :
#   ./repair-off-names.sh              # dry-run : affiche ce qui serait fait
#   ./repair-off-names.sh --apply      # applique réellement les UPDATE
#
# Prérequis sur le serveur : docker (conteneur olympus-db déjà en place),
# curl, python3. Rien d'autre n'est installé ni modifié.

set -uo pipefail

DB_CONTAINER="${DB_CONTAINER:-olympus-db}"
USER_AGENT="Olympus-Repair-Script/1.0 (+https://github.com/Kronos131-dev/olympus)"
SLEEP_BETWEEN_CALLS="0.4"

APPLY=false
if [[ "${1:-}" == "--apply" ]]; then
  APPLY=true
fi

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
  # Exécute une requête SQL dans le conteneur, avec les identifiants déjà
  # présents dans son environnement (POSTGRES_USER / POSTGRES_DB) : aucun
  # mot de passe n'a besoin de transiter par ce script.
  docker exec -i "$DB_CONTAINER" bash -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 "$@"' _ "$@"
}

psql_update_name() {
  # La substitution :'var' de psql n'est interpolée qu'en mode script (-f) ;
  # -c "..." passe le ":" tel quel à Postgres et échoue. On envoie donc la
  # requête par stdin plutôt que comme argument -c.
  local id="$1" new_name="$2"
  echo "UPDATE food_items SET name = :'newname' WHERE id = :id;" | \
    docker exec -i "$DB_CONTAINER" bash -c \
      'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -v id="$1" -v newname="$2" -f -' \
      _ "$id" "$new_name"
}

off_field() {
  # off_field <json> <path...> : lit un champ imbriqué, chaîne vide si absent.
  python3 -c '
import json, sys
try:
    data = json.loads(sys.argv[1])
except ValueError:
    print("")
    sys.exit(0)
node = data
for key in sys.argv[2:]:
    if not isinstance(node, dict):
        node = None
        break
    node = node.get(key)
print(node if isinstance(node, str) else (node if node is not None else ""))
' "$1" "${@:2}"
}

echo "Mode : $([ "$APPLY" = true ] && echo 'APPLICATION RÉELLE' || echo 'dry-run (aucune écriture — relancer avec --apply pour appliquer)')"
echo

mapfile -t rows < <(psql_exec -t -A -F $'\t' -c \
  "SELECT id, barcode FROM food_items WHERE source = 'OFF' AND name LIKE '%Produit Inconnu%' AND barcode IS NOT NULL AND barcode <> '';")

total="${#rows[@]}"
echo "$total ligne(s) à examiner."
echo

repaired=0
still_unknown=0
not_found=0
failed=0

for row in "${rows[@]}"; do
  id="${row%%$'\t'*}"
  barcode="${row#*$'\t'}"

  response=$(curl -s --max-time 8 -H "User-Agent: $USER_AGENT" \
    "https://world.openfoodfacts.org/api/v2/product/${barcode}.json?fields=product_name,generic_name,brands,status")

  status=$(off_field "$response" status)
  if [[ "$status" != "1" ]]; then
    echo "[introuvable sur OFF] id=$id barcode=$barcode"
    not_found=$((not_found + 1))
    sleep "$SLEEP_BETWEEN_CALLS"
    continue
  fi

  product_name=$(off_field "$response" product product_name)
  generic_name=$(off_field "$response" product generic_name)
  brands=$(off_field "$response" product brands)

  base_name="${product_name:-${generic_name:-"Produit Inconnu"}}"
  if [[ -n "$brands" ]]; then
    new_name="${brands} - ${base_name}"
  else
    new_name="$base_name"
  fi

  if [[ "$new_name" == *"Produit Inconnu"* ]]; then
    echo "[toujours sans nom sur OFF] id=$id barcode=$barcode"
    still_unknown=$((still_unknown + 1))
    sleep "$SLEEP_BETWEEN_CALLS"
    continue
  fi

  echo "[réparé] id=$id barcode=$barcode -> \"$new_name\""
  if [[ "$APPLY" == true ]]; then
    if ! psql_update_name "$id" "$new_name" >/dev/null; then
      echo "  échec de l'UPDATE pour id=$id" >&2
      failed=$((failed + 1))
      sleep "$SLEEP_BETWEEN_CALLS"
      continue
    fi
  fi
  repaired=$((repaired + 1))

  sleep "$SLEEP_BETWEEN_CALLS"
done

echo
echo "Résumé : $repaired réparé(s), $still_unknown sans nom sur OFF, $not_found introuvable(s) sur OFF, $failed échec(s) SQL."
if [[ "$APPLY" == false && "$repaired" -gt 0 ]]; then
  echo "Dry-run : relancer avec --apply pour appliquer ces $repaired mise(s) à jour."
fi
