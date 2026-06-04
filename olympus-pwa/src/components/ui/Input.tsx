import { forwardRef, type InputHTMLAttributes, type SelectHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

interface FieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

// Champ : corps en surface_lowest, label or au-dessus, soulignement or au focus.
export const Input = forwardRef<HTMLInputElement, FieldProps>(function Input(
  { label, error, className, id, ...rest },
  ref,
) {
  return (
    <label className="block">
      {label && (
        <span className="mb-1.5 block text-xs font-medium text-marble-dim">
          {label}
        </span>
      )}
      <input
        ref={ref}
        id={id}
        className={cn(
          "w-full rounded-[var(--radius)] bg-surface-lowest px-4 py-3 text-marble outline-none",
          "border border-outline/60 transition-colors placeholder:text-marble-dim/60",
          "focus:border-gold focus:ring-1 focus:ring-gold/40",
          error && "border-[var(--color-danger)]",
          className,
        )}
        {...rest}
      />
      {error && <span className="mt-1 block text-xs text-[var(--color-danger)]">{error}</span>}
    </label>
  );
});

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
  options: { value: string; label: string }[];
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { label, error, options, className, ...rest },
  ref,
) {
  return (
    <label className="block">
      {label && (
        <span className="mb-1.5 block text-xs font-medium text-marble-dim">
          {label}
        </span>
      )}
      <select
        ref={ref}
        className={cn(
          "w-full rounded-[var(--radius)] bg-surface-lowest px-4 py-3 text-marble outline-none",
          "border border-outline/60 transition-colors focus:border-gold focus:ring-1 focus:ring-gold/40",
          className,
        )}
        {...rest}
      >
        {options.map((o) => (
          <option key={o.value} value={o.value} className="bg-surface text-marble">
            {o.label}
          </option>
        ))}
      </select>
      {error && <span className="mt-1 block text-xs text-[var(--color-danger)]">{error}</span>}
    </label>
  );
});
