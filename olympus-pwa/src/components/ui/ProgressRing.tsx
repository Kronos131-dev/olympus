import { cn } from "@/lib/utils";

interface Props {
  value: number;
  max: number;
  size?: number;
  stroke?: number;
  children?: React.ReactNode;
  className?: string;
}

// Anneau « héroïque » : couronne de laurier dorée encerclant les données.
export function ProgressRing({ value, max, size = 220, stroke = 16, children, className }: Props) {
  const radius = (size - stroke) / 2;
  const circ = 2 * Math.PI * radius;
  const ratio = max > 0 ? Math.min(value / max, 1) : 0;
  const offset = circ * (1 - ratio);
  const over = max > 0 && value > max;

  return (
    <div className={cn("relative inline-grid place-items-center", className)} style={{ width: size, height: size }}>
      <svg width={size} height={size} className="-rotate-90">
        <defs>
          <linearGradient id="goldRing" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#af8d11" />
            <stop offset="50%" stopColor="#e9c349" />
            <stop offset="100%" stopColor="#af8d11" />
          </linearGradient>
        </defs>
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="var(--color-surface-high)"
          strokeWidth={stroke}
        />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke={over ? "var(--color-danger)" : "url(#goldRing)"}
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={circ}
          strokeDashoffset={offset}
          style={{ transition: "stroke-dashoffset 0.6s cubic-bezier(0.4,0,0.2,1)" }}
        />
      </svg>
      <div className="absolute inset-0 grid place-items-center text-center">{children}</div>
    </div>
  );
}
