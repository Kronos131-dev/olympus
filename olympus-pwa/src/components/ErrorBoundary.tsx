import { Component, type ReactNode } from "react";
import { isRecoverableLoadError, maybeReloadOnce } from "@/lib/chunkReload";

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
}

/**
 * Error boundary global (filet de sécurité hors routeur). Sur une erreur de chunk périmé,
 * recharge automatiquement ; sinon affiche un écran de secours avec un bouton Recharger.
 */
export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: unknown) {
    if (isRecoverableLoadError(error)) maybeReloadOnce();
  }

  render() {
    if (!this.state.hasError) return this.props.children;

    return (
      <div className="grid min-h-dvh place-items-center px-6">
        <div className="flex flex-col items-center gap-4 text-center">
          <p className="text-sm font-semibold text-marble">Une erreur est survenue</p>
          <p className="max-w-xs text-xs text-marble-dim/70">Rechargez la page pour continuer.</p>
          <button
            onClick={() => window.location.reload()}
            className="rounded-full bg-gold px-5 py-2 text-xs font-semibold text-[var(--color-on-gold)]"
          >
            Recharger
          </button>
        </div>
      </div>
    );
  }
}
