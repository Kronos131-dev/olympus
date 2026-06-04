// Détection des erreurs récupérables par un simple rechargement et garde anti-boucle.
//
// Après un déploiement, les anciens chunks (hash changé) ne sont plus servis : un import
// dynamique échoue (page lazy) ou React lève l'erreur #426 (Suspense sur input synchrone).
// Un rechargement récupère le nouveau manifeste. La garde temporelle évite toute boucle.

export function isRecoverableLoadError(error: unknown): boolean {
  const m = error instanceof Error ? error.message : String(error);
  return (
    /Failed to fetch dynamically imported module/i.test(m) ||
    /error loading dynamically imported module/i.test(m) ||
    /Loading chunk [\d]+ failed/i.test(m) ||
    /Importing a module script failed/i.test(m) ||
    /Minified React error #426/i.test(m)
  );
}

// Recharge la page au plus une fois par fenêtre de 10 s (anti-boucle). Renvoie true si un
// rechargement a été déclenché. Pas besoin de nettoyer un flag : la fenêtre expire seule.
export function maybeReloadOnce(): boolean {
  const KEY = "olympus.lastReload";
  const last = Number(sessionStorage.getItem(KEY) || 0);
  if (Date.now() - last < 10_000) return false;
  sessionStorage.setItem(KEY, String(Date.now()));
  window.location.reload();
  return true;
}
