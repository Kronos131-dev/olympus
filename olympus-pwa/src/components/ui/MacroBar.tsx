import { cn } from "@/lib/utils";
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
    <div>
      <div className="mb-1 flex items-baseline justify-between">
        <span className="lapidary text-[0.6rem] tracking-[0.12em] text-marble-dim">{label}</span>
        <span className="text-xs text-marble">
          {round(value)}
          {target != null && <span className="text-marble-dim"> / {round(target)}</span>}
          {unit}
        </span>
      </div>
      <div className="h-1.5 w-full bg-surface-high">
        <div
          className={cn("h-full transition-[width] duration-500")}
          style={{ width: `${ratio * 100}%`, backgroundColor: color }}
        />
      </div>
    </div>
  );
}
