import { forwardRef, type ButtonHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

type Variant = "primary" | "ghost" | "subtle" | "danger";
type Size = "sm" | "md" | "lg";

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
  block?: boolean;
}

const sizes: Record<Size, string> = {
  sm: "px-3 py-1.5 text-xs",
  md: "px-5 py-3 text-sm",
  lg: "px-6 py-4 text-base",
};

const variants: Record<Variant, string> = {
  // Les Lauriers : or plein, texte sombre, angles vifs.
  primary:
    "gold-sheen text-[var(--color-on-gold)] font-bold tracking-wide hover:brightness-110 active:brightness-95 disabled:opacity-50",
  // Le Sénat : ghost border or.
  ghost:
    "bg-transparent text-gold border border-gold/30 hover:border-gold/70 hover:bg-gold/5 active:bg-gold/10 disabled:opacity-40",
  subtle:
    "bg-surface-high text-marble hover:bg-surface-variant active:brightness-95 disabled:opacity-40",
  danger:
    "bg-transparent text-[var(--color-danger)] border border-[var(--color-danger)]/30 hover:bg-[var(--color-danger)]/10",
};

export const Button = forwardRef<HTMLButtonElement, Props>(function Button(
  { variant = "primary", size = "md", loading, block, className, children, disabled, ...rest },
  ref,
) {
  return (
    <button
      ref={ref}
      disabled={disabled || loading}
      className={cn(
        "lapidary inline-flex items-center justify-center gap-2 rounded-none uppercase transition-all select-none",
        sizes[size],
        variants[variant],
        block && "w-full",
        className,
      )}
      {...rest}
    >
      {loading && (
        <span className="size-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
      )}
      {children}
    </button>
  );
});
