import type { HTMLAttributes } from "react";
import { cn } from "@/lib/utils";

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  tone?: "low" | "default" | "high";
  goldEdge?: boolean;
}

const tones = {
  low: "bg-surface-low",
  default: "bg-surface",
  high: "bg-surface-high",
};

// Slab de pierre : hiérarchie par ton de surface, pas de bordure 1px, angles vifs.
export function Card({ tone = "default", goldEdge, className, children, ...rest }: CardProps) {
  return (
    <div
      className={cn(
        "rounded-none p-5",
        tones[tone],
        goldEdge && "border-l-2 border-gold",
        className,
      )}
      {...rest}
    >
      {children}
    </div>
  );
}

export function SectionTitle({ children, className }: { children: React.ReactNode; className?: string }) {
  return (
    <h2 className={cn("lapidary text-xs text-marble-dim tracking-[0.18em] mb-3", className)}>
      {children}
    </h2>
  );
}
