import { ProgressRing } from "./ProgressRing";
import { round } from "@/lib/utils";

interface Props {
  label: string;
  value: number;
  target?: number | null;
  unit?: string;
  color: string;
}

export function MacroRing({ label, value, target, unit = "g", color }: Props) {
  return (
    <div className="flex min-w-0 flex-col items-center gap-1.5">
      <ProgressRing
        value={value}
        max={target && target > 0 ? target : 1}
        size={78}
        stroke={7}
        color={color}
      >
        <p className="text-sm font-bold tabular-nums text-marble">
          {round(value)}
          <span className="text-[0.6rem] font-medium text-marble-dim">
            {unit}
          </span>
        </p>
      </ProgressRing>
      <p className="truncate text-[0.65rem] font-medium uppercase tracking-wide text-marble-dim">
        {label}
      </p>
      {target != null && target > 0 && (
        <p className="text-[0.65rem] tabular-nums text-marble-dim/70">
          / {round(target)}
          {unit}
        </p>
      )}
    </div>
  );
}
