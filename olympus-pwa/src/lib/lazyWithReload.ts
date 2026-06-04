import { lazy, type ComponentType } from "react";
import { isRecoverableLoadError, maybeReloadOnce } from "./chunkReload";

/**
 * Variante de React.lazy() résiliente aux chunks périmés.
 *
 * Si l'import dynamique échoue parce que le chunk a changé de hash (déploiement), on recharge
 * la page une fois (garde anti-boucle dans {@link maybeReloadOnce}) ; sinon on relance l'erreur.
 */
export function lazyWithReload<T extends ComponentType<unknown>>(
  factory: () => Promise<{ default: T }>,
) {
  return lazy(async () => {
    try {
      return await factory();
    } catch (error) {
      if (isRecoverableLoadError(error) && maybeReloadOnce()) {
        // Le reload prend le relais : on renvoie une promesse qui ne se résout jamais.
        return new Promise<{ default: T }>(() => {});
      }
      throw error;
    }
  });
}
