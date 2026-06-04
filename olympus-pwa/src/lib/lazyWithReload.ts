import { lazy, type ComponentType } from "react";

/**
 * Variante de React.lazy() résiliente aux chunks périmés.
 *
 * Après un déploiement, le hash des chunks change : un bouton qui navigue vers une page
 * lazy tente de charger un ancien chunk (qui n'existe plus) → l'import dynamique échoue →
 * React plante (« page error »). On recharge alors la page une seule fois (l'index récupère
 * le nouveau manifeste). Le flag en sessionStorage évite une boucle de rechargement si
 * l'échec persiste pour une autre raison.
 */
export function lazyWithReload<T extends ComponentType<unknown>>(
  factory: () => Promise<{ default: T }>,
) {
  return lazy(async () => {
    try {
      return await factory();
    } catch (error) {
      const key = "olympus.chunkReloaded";
      if (!sessionStorage.getItem(key)) {
        sessionStorage.setItem(key, "1");
        window.location.reload();
        // On renvoie une promesse qui ne se résout jamais : le reload prend le relais.
        return new Promise<{ default: T }>(() => {});
      }
      throw error;
    }
  });
}

/** À appeler après un chargement réussi pour réarmer le mécanisme de rechargement. */
export function clearChunkReloadFlag() {
  sessionStorage.removeItem("olympus.chunkReloaded");
}
