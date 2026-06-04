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
  sm: "px-3.5 py-2 text-xs rounded-[var(--radius-sm)]",
  md: "px-5 py-3 text-sm rounded-[var(--radius)]",
  lg: "px-6 py-4 text-base rounded-[var(--radius)]",
};

const variants: Record<Variant, string> = {
  // Or plein et net.
  primary:
    "bg-gold text-[var(--color-on-gold)] hover:brightness-105 active:brightness-95 disabled:opacity-50",
  // Bord discret.
  ghost:
    "bg-transparent text-marble border border-outline hover:border-gold/60 hover:text-gold active:bg-gold/5 disabled:opacity-40",
  subtle:
    "bg-surface-high text-marble hover:bg-surface-variant active:brightness-95 disabled:opacity-40",
  danger:
    "bg-transparent text-[var(--color-danger)] border border-[var(--color-danger)]/40 hover:bg-[var(--color-danger)]/10",
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
        "inline-flex items-center justify-center gap-2 font-semibold transition-all duration-150 select-none active:scale-[0.98]",
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
