import { useEffect, type ReactNode } from "react";
import { cn } from "@/lib/utils";

interface Props {
  open: boolean;
  onClose: () => void;
  title?: string;
  children: ReactNode;
  className?: string;
}

// Modale en feuille du bas (mobile-first), ombre ambiante diffuse, angles vifs.
export function Modal({ open, onClose, title, children, className }: Props) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    document.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center">
      <div
        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
        onClick={onClose}
        aria-hidden
      />
      <div
        role="dialog"
        aria-modal="true"
        className={cn(
          "relative w-full max-w-lg bg-surface-low p-6",
          "rounded-t-[var(--radius-xl)] sm:rounded-[var(--radius-xl)]",
          "max-h-[88vh] overflow-y-auto",
          "shadow-[0px_24px_48px_rgba(0,0,0,0.5)]",
          "animate-[page-fade_0.22s_ease]",
          className,
        )}
        style={{ paddingBottom: "calc(1.5rem + env(safe-area-inset-bottom))" }}
      >
        {title && (
          <h2 className="mb-5 text-lg font-bold text-marble">{title}</h2>
        )}
        {children}
      </div>
    </div>
  );
}
