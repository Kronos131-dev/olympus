// Concatène des classes en ignorant les valeurs falsy.
export function cn(...parts: Array<string | false | null | undefined>): string {
  return parts.filter(Boolean).join(" ");
}

export function todayIso(): string {
  return new Date().toLocaleDateString("en-CA"); // YYYY-MM-DD en heure locale
}

export function isoToDate(iso: string): Date {
  const [y, m, d] = iso.split("-").map(Number);
  return new Date(y, m - 1, d);
}

export function shiftIso(iso: string, days: number): string {
  const d = isoToDate(iso);
  d.setDate(d.getDate() + days);
  return d.toLocaleDateString("en-CA");
}

const DAY_LABELS_FR: Record<string, string> = {
  MONDAY: "Lundi",
  TUESDAY: "Mardi",
  WEDNESDAY: "Mercredi",
  THURSDAY: "Jeudi",
  FRIDAY: "Vendredi",
  SATURDAY: "Samedi",
  SUNDAY: "Dimanche",
};

export const WEEK_DAYS = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
] as const;

export function dayLabel(day: string): string {
  return DAY_LABELS_FR[day] ?? day;
}

export function formatDateLong(iso: string): string {
  return isoToDate(iso).toLocaleDateString("fr-FR", {
    weekday: "long",
    day: "numeric",
    month: "long",
  });
}

export function round(n: number | null | undefined, digits = 0): number {
  if (n == null) return 0;
  const f = 10 ** digits;
  return Math.round(n * f) / f;
}

// Macros d'un aliment pour une quantité donnée (valeurs /100g).
export function macrosFor(
  per100g: { kcal100g: number; proteins100g: number; carbs100g: number; fats100g: number },
  grams: number,
) {
  const r = grams / 100;
  return {
    kcal: per100g.kcal100g * r,
    proteins: per100g.proteins100g * r,
    carbs: per100g.carbs100g * r,
    fats: per100g.fats100g * r,
  };
}
