import { useId } from "react";
import { cn } from "@/lib/utils";

interface Props {
  value: number;
  max: number;
  size?: number;
  stroke?: number;
  /** Couleur de l'arc. Par défaut le dégradé doré. */
  color?: string;
  children?: React.ReactNode;
  className?: string;
}

// Anneau « héroïque » : couronne de laurier dorée encerclant les données.
export function ProgressRing({
  value,
  max,
  size = 220,
  stroke = 16,
  color,
  children,
  className,
}: Props) {
  // WHY: l'id du dégradé doit être unique par instance. Avec un id constant, plusieurs anneaux
  // sur la même page déclarent le même <linearGradient> et le navigateur applique le premier
  // rencontré à tous — les quatre roues macros de l'accueil viraient toutes à l'or.
  const gradientId = useId().replace(/:/g, "");

  const radius = (size - stroke) / 2;
  const circ = 2 * Math.PI * radius;
  const ratio = max > 0 ? Math.min(value / max, 1) : 0;
  const offset = circ * (1 - ratio);
  const over = max > 0 && value > max;
  const arc = over ? "var(--color-danger)" : (color ?? `url(#${gradientId})`);

  return (
    // Taille fluide : ne dépasse jamais `size` (220px) mais se réduit sur les petits écrans.
    // Le SVG garde son repère interne via viewBox et remplit le conteneur à 100%.
    <div
      className={cn(
        "relative inline-grid aspect-square place-items-center",
        className,
      )}
      style={{ width: `min(${size}px, 62vw)` }}
    >
      <svg viewBox={`0 0 ${size} ${size}`} className="h-full w-full -rotate-90">
        <defs>
          <linearGradient id={gradientId} x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="var(--color-gold-deep)" />
            <stop offset="50%" stopColor="var(--color-gold)" />
            <stop offset="100%" stopColor="var(--color-gold-deep)" />
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
          stroke={arc}
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={circ}
          strokeDashoffset={offset}
          style={{
            transition: "stroke-dashoffset 0.6s cubic-bezier(0.4,0,0.2,1)",
          }}
        />
      </svg>
      <div className="absolute inset-0 grid place-items-center text-center">
        {children}
      </div>
    </div>
  );
}
