import { round } from "@/lib/utils";
import { cn } from "@/lib/utils";

interface Props {
  label: string;
  value: number;
  reference: number;
  unit: string;
  coverage?: number;
  coverageLabel?: (percent: number) => string;
}

const RELIABLE_COVERAGE = 0.5;

function barColor(ratio: number, reliable: boolean): string {
  if (!reliable) return "var(--color-outline)";
  if (ratio >= 1) return "var(--color-success)";
  if (ratio >= 0.5) return "var(--color-gold)";
  return "var(--color-danger)";
}

export function NutrientGauge({
  label,
  value,
  reference,
  unit,
  coverage,
  coverageLabel,
}: Props) {
  const ratio = reference > 0 ? value / reference : 0;
  const reliable = coverage == null || coverage >= RELIABLE_COVERAGE;
  const percent = Math.round(ratio * 100);

  return (
    <div className="py-2.5">
      <div className="flex items-baseline justify-between gap-3">
        <p className="min-w-0 truncate text-sm text-marble">{label}</p>
        <p className="shrink-0 text-xs tabular-nums text-marble-dim">
          {round(value, value < 10 ? 1 : 0)} /{" "}
          {round(reference, reference < 10 ? 1 : 0)} {unit}
          <span
            className={cn(
              "ml-2 font-semibold",
              reliable ? "text-marble" : "text-marble-dim/60",
            )}
          >
            {percent}%
          </span>
        </p>
      </div>
      <div className="mt-1.5 h-1.5 w-full overflow-hidden rounded-full bg-surface-high">
        <div
          className="h-full rounded-full transition-[width] duration-500"
          style={{
            width: `${Math.min(ratio, 1) * 100}%`,
            backgroundColor: barColor(ratio, reliable),
          }}
        />
      </div>
      {!reliable && coverage != null && coverageLabel && (
        <p className="mt-1 text-[0.65rem] text-marble-dim/70">
          {coverageLabel(Math.round(coverage * 100))}
        </p>
      )}
    </div>
  );
}
