import { round } from "@/lib/utils";

interface Props {
  label: string;
  value: number;
  target?: number | null;
  unit?: string;
  color: string;
}

// Jauge en ligne pour un repas : contrairement à NutrientGauge (qui rougit sous 50% de la
// référence), la couleur ici est TOUJOURS fixe. Un repas ne pèse qu'une fraction de la journée —
// le remplir par rapport à la cible journalière donnerait cinq barres rouges sur un repas normal.
export function NutrientLine({
  label,
  value,
  target,
  unit = "g",
  color,
}: Props) {
  const ratio = target && target > 0 ? Math.min(value / target, 1) : 0;
  return (
    <div className="py-1">
      <div className="flex items-baseline justify-between gap-3">
        <p className="min-w-0 truncate text-xs text-marble-dim">{label}</p>
        <p className="shrink-0 text-xs tabular-nums text-marble-dim">
          {round(value)}
          {unit}
          {target != null && target > 0 && (
            <span className="text-marble-dim/60">
              {" "}
              / {round(target)}
              {unit}
            </span>
          )}
        </p>
      </div>
      <div className="mt-1 h-1 w-full overflow-hidden rounded-full bg-surface-high">
        <div
          className="h-full rounded-full transition-[width] duration-500"
          style={{ width: `${ratio * 100}%`, backgroundColor: color }}
        />
      </div>
    </div>
  );
}
