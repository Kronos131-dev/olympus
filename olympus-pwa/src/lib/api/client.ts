import { tokenStore } from "./tokenStore";
import type { AuthResponse } from "@/types/api";

// Même origine : la PWA est servie à la racine de son domaine, l'API est relayée
// par nginx sous /api/v1 (aucun CORS). import.meta.env.BASE_URL = "/".
const BASE_URL = `${import.meta.env.BASE_URL}api/v1`;

export class ApiError extends Error {
  status: number;
  body: unknown;
  constructor(status: number, message: string, body?: unknown) {
    super(message);
    this.status = status;
    this.body = body;
  }
}

// Extrait un message lisible d'une erreur (le vrai message du backend si dispo), sinon un
// libellé de repli. Évite les toasts génériques qui masquent la cause réelle (ex. 500).
export function errorMessage(e: unknown, fallback: string): string {
  if (e instanceof ApiError && e.message) return e.message;
  if (e instanceof Error && e.message) return e.message;
  return fallback;
}

// Déclenché quand le refresh échoue : l'app doit rediriger vers le login.
let onAuthExpired: (() => void) | null = null;
export function setAuthExpiredHandler(fn: () => void) {
  onAuthExpired = fn;
}

// Un seul refresh en vol à la fois ; les requêtes 401 concurrentes l'attendent.
let refreshing: Promise<boolean> | null = null;

async function doRefresh(): Promise<boolean> {
  const refreshToken = tokenStore.getRefresh();
  if (!refreshToken) return false;
  try {
    const res = await fetch(`${BASE_URL}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) return false;
    const data = (await res.json()) as AuthResponse;
    tokenStore.set(data.token, data.refreshToken);
    return true;
  } catch {
    return false;
  }
}

function refreshOnce(): Promise<boolean> {
  if (!refreshing) {
    refreshing = doRefresh().finally(() => {
      refreshing = null;
    });
  }
  return refreshing;
}

interface RequestOptions {
  method?: string;
  body?: unknown; // objet JSON ou FormData
  query?: Record<string, string | number | undefined>;
  signal?: AbortSignal;
  auth?: boolean; // ajoute le Bearer (défaut: true)
}

function buildUrl(path: string, query?: RequestOptions["query"]): string {
  const url = new URL(`${BASE_URL}${path}`, window.location.origin);
  if (query) {
    for (const [k, v] of Object.entries(query)) {
      if (v !== undefined && v !== null) url.searchParams.set(k, String(v));
    }
  }
  return url.toString();
}

async function rawRequest<T>(path: string, opts: RequestOptions, retry = true): Promise<T> {
  const { method = "GET", body, query, signal, auth = true } = opts;
  const isForm = body instanceof FormData;
  const headers: Record<string, string> = {};
  if (auth) {
    const token = tokenStore.getAccess();
    if (token) headers.Authorization = `Bearer ${token}`;
  }
  if (body !== undefined && !isForm) headers["Content-Type"] = "application/json";

  const res = await fetch(buildUrl(path, query), {
    method,
    headers,
    body: body === undefined ? undefined : isForm ? (body as FormData) : JSON.stringify(body),
    signal,
  });

  if (res.status === 401 && auth && retry) {
    const ok = await refreshOnce();
    if (ok) return rawRequest<T>(path, opts, false);
    tokenStore.clear();
    onAuthExpired?.();
    throw new ApiError(401, "Session expirée");
  }

  if (!res.ok) {
    let parsed: unknown;
    let message = `Erreur ${res.status}`;
    try {
      parsed = await res.json();
      if (parsed && typeof parsed === "object") {
        const m = (parsed as Record<string, unknown>).message;
        if (typeof m === "string") message = m;
      }
    } catch {
      /* corps non-JSON */
    }
    throw new ApiError(res.status, message, parsed);
  }

  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export const api = {
  get: <T>(path: string, opts: Omit<RequestOptions, "method" | "body"> = {}) =>
    rawRequest<T>(path, { ...opts, method: "GET" }),
  post: <T>(path: string, body?: unknown, opts: Omit<RequestOptions, "method" | "body"> = {}) =>
    rawRequest<T>(path, { ...opts, method: "POST", body }),
  put: <T>(path: string, body?: unknown, opts: Omit<RequestOptions, "method" | "body"> = {}) =>
    rawRequest<T>(path, { ...opts, method: "PUT", body }),
  del: <T>(path: string, opts: Omit<RequestOptions, "method" | "body"> = {}) =>
    rawRequest<T>(path, { ...opts, method: "DELETE" }),
};
