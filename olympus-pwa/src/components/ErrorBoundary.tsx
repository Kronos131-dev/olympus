import { Component, type ReactNode } from "react";

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
}

// Reconnaît les erreurs de chargement de module dynamique (chunk périmé après déploiement).
function isChunkLoadError(error: unknown): boolean {
  const message = error instanceof Error ? error.message : String(error);
  return (
    /Failed to fetch dynamically imported module/i.test(message) ||
    /Loading chunk [\d]+ failed/i.test(message) ||
    /error loading dynamically imported module/i.test(message) ||
    /Importing a module script failed/i.test(message)
  );
}

/**
 * Error boundary global. Sur une erreur de chunk périmé, recharge automatiquement (une fois)
 * pour récupérer le nouveau manifeste. Pour les autres erreurs, affiche un écran de secours
 * avec un bouton Recharger.
 */
export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: unknown) {
    if (isChunkLoadError(error)) {
      const key = "olympus.chunkReloaded";
      if (!sessionStorage.getItem(key)) {
        sessionStorage.setItem(key, "1");
        window.location.reload();
      }
    }
  }

  render() {
    if (!this.state.hasError) return this.props.children;

    return (
      <div className="grid min-h-dvh place-items-center px-6">
        <div className="flex flex-col items-center gap-4 text-center">
          <p className="text-sm font-semibold text-marble">Une erreur est survenue</p>
          <p className="max-w-xs text-xs text-marble-dim/70">
            Rechargez la page pour continuer.
          </p>
          <button
            onClick={() => {
              sessionStorage.removeItem("olympus.chunkReloaded");
              window.location.reload();
            }}
            className="rounded-full bg-gold px-5 py-2 text-xs font-semibold text-[var(--color-on-gold)]"
          >
            Recharger
          </button>
        </div>
      </div>
    );
  }
}
