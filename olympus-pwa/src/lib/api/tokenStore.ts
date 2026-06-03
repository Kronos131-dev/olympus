// Stockage local des tokens d'authentification.
const ACCESS_KEY = "olympus.accessToken";
const REFRESH_KEY = "olympus.refreshToken";

type Listener = () => void;
const listeners = new Set<Listener>();

export const tokenStore = {
  getAccess(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  },
  getRefresh(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  },
  set(access: string, refresh: string) {
    localStorage.setItem(ACCESS_KEY, access);
    localStorage.setItem(REFRESH_KEY, refresh);
    listeners.forEach((l) => l());
  },
  clear() {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    listeners.forEach((l) => l());
  },
  hasSession(): boolean {
    return !!localStorage.getItem(ACCESS_KEY);
  },
  subscribe(l: Listener): () => void {
    listeners.add(l);
    return () => listeners.delete(l);
  },
};
