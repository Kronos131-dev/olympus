import { useEffect } from "react";
import { useRouteError } from "react-router-dom";
import { isRecoverableLoadError, maybeReloadOnce } from "@/lib/chunkReload";

/**
 * errorElement du routeur : remplace l'écran brut « Unexpected Application Error! » de React
 * Router. Sur une erreur récupérable (chunk périmé, #426), recharge automatiquement ; sinon
 * affiche un écran de secours sobre avec un bouton Recharger.
 */
export function RouteError() {
  const error = useRouteError();

  useEffect(() => {
    if (isRecoverableLoadError(error)) maybeReloadOnce();
  }, [error]);

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
