import { cn } from "@/lib/utils";

export function Skeleton({ className }: { className?: string }) {
  return <div className={cn("animate-pulse rounded-[var(--radius)] bg-surface-high", className)} />;
}

export function EmptyState({
  title,
  hint,
  icon,
}: {
  title: string;
  hint?: string;
  icon?: React.ReactNode;
}) {
  return (
    <div className="flex flex-col items-center gap-3 px-6 py-16 text-center">
      {icon && <div className="text-gold/50">{icon}</div>}
      <p className="text-sm font-semibold text-marble">{title}</p>
      {hint && <p className="max-w-xs text-xs text-marble-dim/70">{hint}</p>}
    </div>
  );
}

export function Chip({
  children,
  active,
  onClick,
}: {
  children: React.ReactNode;
  active?: boolean;
  onClick?: () => void;
}) {
  return (
    <button
      onClick={onClick}
      className={cn(
        "rounded-full px-4 py-1.5 text-xs font-semibold transition-colors",
        active ? "bg-gold text-[var(--color-on-gold)]" : "bg-surface-high text-marble-dim hover:text-marble",
      )}
    >
      {children}
    </button>
  );
}

export function Spinner({ className }: { className?: string }) {
  return (
    <span
      className={cn(
        "inline-block size-5 animate-spin rounded-full border-2 border-gold border-t-transparent",
        className,
      )}
    />
  );
}

export function FullPageSpinner() {
  return (
    <div className="grid min-h-dvh place-items-center">
      <Spinner className="size-8" />
    </div>
  );
}
