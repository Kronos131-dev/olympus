import { exchangeChironToken } from "@/lib/api/endpoints";
import { tokenStore } from "@/lib/api/tokenStore";

// Chiron ouvre la PWA avec le token de liaison dans le fragment : #ctk=<token>
// (le fragment n'est pas envoyé au serveur ni journalisé par le proxy).
function readHandoffToken(): string | null {
  const hash = window.location.hash;
  if (!hash) return null;
  const match = hash.match(/(?:^#|&)ctk=([^&]+)/);
  return match ? decodeURIComponent(match[1]) : null;
}

/** Vrai si Chiron a transmis un token de liaison dans l'URL (`#ctk=…`). */
export function hasHandoffToken(): boolean {
  return readHandoffToken() !== null;
}

function stripHandoffFromUrl() {
  const cleaned = window.location.hash.replace(/(?:^#|&)ctk=[^&]+/, "").replace(/^#&/, "#");
  const hash = cleaned === "#" ? "" : cleaned;
  history.replaceState(null, "", window.location.pathname + window.location.search + hash);
}

/**
 * Si Chiron a transmis un token de liaison, on l'échange contre une session Olympus
 * complète (« entrée directe »). Retourne true si une session a été ouverte.
 */
export async function tryChironHandoff(): Promise<boolean> {
  const token = readHandoffToken();
  if (!token) return false;
  stripHandoffFromUrl();
  try {
    const auth = await exchangeChironToken(token);
    tokenStore.set(auth.token, auth.refreshToken);
    return true;
  } catch {
    return false;
  }
}
