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
        <span className="lapidary mb-1.5 block text-[0.65rem] tracking-[0.15em] text-gold">
          {label}
        </span>
      )}
      <input
        ref={ref}
        id={id}
        className={cn(
          "w-full rounded-none bg-surface-lowest px-4 py-3 text-marble outline-none",
          "border-b-2 border-outline/30 transition-colors placeholder:text-marble-dim/60",
          "focus:border-gold",
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
        <span className="lapidary mb-1.5 block text-[0.65rem] tracking-[0.15em] text-gold">
          {label}
        </span>
      )}
      <select
        ref={ref}
        className={cn(
          "w-full rounded-none bg-surface-lowest px-4 py-3 text-marble outline-none",
          "border-b-2 border-outline/30 transition-colors focus:border-gold",
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
