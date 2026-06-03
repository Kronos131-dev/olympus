import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { tokenStore } from "@/lib/api/tokenStore";
import { setAuthExpiredHandler } from "@/lib/api/client";
import { authApi, userApi } from "@/lib/api/endpoints";
import { tryChironHandoff } from "./handoff";
import type { AuthRequest, RegisterRequest, UserResponse } from "@/types/api";

interface AuthState {
  status: "loading" | "authenticated" | "anonymous";
  user: UserResponse | null;
  login: (body: AuthRequest) => Promise<void>;
  register: (body: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  setUser: (user: UserResponse) => void;
}

const AuthCtx = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthState["status"]>("loading");
  const [user, setUser] = useState<UserResponse | null>(null);

  const reset = useCallback(() => {
    tokenStore.clear();
    setUser(null);
    setStatus("anonymous");
  }, []);

  // Bootstrap : handoff Chiron -> session existante -> anonyme.
  useEffect(() => {
    setAuthExpiredHandler(() => {
      setUser(null);
      setStatus("anonymous");
    });

    let cancelled = false;
    (async () => {
      if (!tokenStore.hasSession()) {
        await tryChironHandoff();
      }
      if (cancelled) return;
      if (!tokenStore.hasSession()) {
        setStatus("anonymous");
        return;
      }
      try {
        const profile = await userApi.profile();
        if (cancelled) return;
        setUser(profile);
        setStatus("authenticated");
      } catch {
        if (!cancelled) reset();
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [reset]);

  const login = useCallback(async (body: AuthRequest) => {
    const auth = await authApi.login(body);
    tokenStore.set(auth.token, auth.refreshToken);
    setUser(auth.user);
    setStatus("authenticated");
  }, []);

  const register = useCallback(async (body: RegisterRequest) => {
    const auth = await authApi.register(body);
    tokenStore.set(auth.token, auth.refreshToken);
    setUser(auth.user);
    setStatus("authenticated");
  }, []);

  const logout = useCallback(async () => {
    const refresh = tokenStore.getRefresh();
    if (refresh) {
      try {
        await authApi.logout(refresh);
      } catch {
        /* fire-and-forget */
      }
    }
    reset();
  }, [reset]);

  const value = useMemo<AuthState>(
    () => ({ status, user, login, register, logout, setUser }),
    [status, user, login, register, logout],
  );

  return <AuthCtx.Provider value={value}>{children}</AuthCtx.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthCtx);
  if (!ctx) throw new Error("useAuth doit être utilisé dans AuthProvider");
  return ctx;
}
