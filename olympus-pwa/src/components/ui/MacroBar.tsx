import { round } from "@/lib/utils";

interface Props {
  label: string;
  value: number;
  target?: number | null;
  unit?: string;
  color?: string;
}

export function MacroBar({ label, value, target, unit = "g", color = "var(--color-purple-bright)" }: Props) {
  const ratio = target && target > 0 ? Math.min(value / target, 1) : 0;
  return (
    // Structure centrée et de hauteur constante → les colonnes restent alignées, quelle que
    // soit la largeur de l'écran (barre en haut, label, valeur, cible).
    <div className="min-w-0 text-center">
      <div className="h-1.5 w-full overflow-hidden rounded-full bg-surface-high">
        <div
          className="h-full rounded-full transition-[width] duration-500"
          style={{ width: `${ratio * 100}%`, backgroundColor: color }}
        />
      </div>
      <p className="mt-2 truncate text-[0.65rem] font-medium uppercase tracking-wide text-marble-dim">
        {label}
      </p>
      <p className="text-lg font-bold tabular-nums text-marble">
        {round(value)}
        {unit}
      </p>
      {target != null && (
        <p className="text-xs tabular-nums text-marble-dim">
          / {round(target)}
          {unit}
        </p>
      )}
    </div>
  );
}
