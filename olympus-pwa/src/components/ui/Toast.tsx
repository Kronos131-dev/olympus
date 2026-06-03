import { createContext, useCallback, useContext, useState, type ReactNode } from "react";
import { cn } from "@/lib/utils";

type ToastKind = "info" | "success" | "error";
interface Toast {
  id: number;
  message: string;
  kind: ToastKind;
}

const ToastCtx = createContext<(message: string, kind?: ToastKind) => void>(() => {});

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const push = useCallback((message: string, kind: ToastKind = "info") => {
    const id = Date.now() + Math.random();
    setToasts((t) => [...t, { id, message, kind }]);
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 3800);
  }, []);

  return (
    <ToastCtx.Provider value={push}>
      {children}
      <div
        className="pointer-events-none fixed inset-x-0 top-0 z-[100] flex flex-col items-center gap-2 p-4"
        style={{ paddingTop: "calc(1rem + env(safe-area-inset-top))" }}
      >
        {toasts.map((t) => (
          <div
            key={t.id}
            className={cn(
              "glass pointer-events-auto max-w-sm px-4 py-3 text-sm text-marble shadow-lg animate-[page-fade_0.2s_ease]",
              t.kind === "success" && "border-l-2 border-gold",
              t.kind === "error" && "border-l-2 border-[var(--color-danger)]",
            )}
          >
            {t.message}
          </div>
        ))}
      </div>
    </ToastCtx.Provider>
  );
}

export function useToast() {
  return useContext(ToastCtx);
}
