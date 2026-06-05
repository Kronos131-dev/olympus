// Les libellés traduits (objectifs, niveaux d'activité, genres) vivent désormais dans
// le dictionnaire i18n (src/lib/i18n/{fr,en}.ts → t.enums.*). On conserve seulement le
// helper générique de construction d'options pour les <Select>.
export function optionsFrom<T extends string>(map: Record<T, string>) {
  return (Object.keys(map) as T[]).map((value) => ({ value, label: map[value] }));
}
