import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "@/lib/auth/AuthContext";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { ApiError } from "@/lib/api/client";
import { useT } from "@/lib/i18n";

export default function LoginPage() {
  const t = useT();
  const { login } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login({ email: email.trim(), password });
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 401
          ? t.auth.login.errInvalid
          : t.auth.login.errGeneric,
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-enter mx-auto flex min-h-dvh max-w-md flex-col justify-center px-6">
      <div className="mb-10 text-center">
        <p className="lapidary text-[0.65rem] tracking-[0.4em] text-gold">{t.auth.login.overline}</p>
        <h1 className="text-marble" style={{ fontSize: "clamp(2.5rem, 16vw, 3.75rem)" }}>
          Olympus
        </h1>
        <p className="mt-3 text-sm text-marble-dim">{t.auth.login.subtitle}</p>
      </div>

      <form onSubmit={submit} className="space-y-4">
        <Input
          label={t.auth.login.pseudo}
          type="text"
          autoComplete="username"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <Input
          label={t.auth.login.password}
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        {error && <p className="text-sm text-[var(--color-danger)]">{error}</p>}
        <Button type="submit" block size="lg" loading={loading}>
          {t.auth.login.submit}
        </Button>
      </form>

      <p className="mt-4 text-center text-sm">
        <Link to="/forgot-password" className="text-marble-dim hover:text-gold hover:underline">
          {t.auth.login.forgot}
        </Link>
      </p>

      <p className="mt-6 text-center text-sm text-marble-dim">
        {t.auth.login.noAccount}{" "}
        <Link to="/register" className="text-gold hover:underline">
          {t.auth.login.register}
        </Link>
      </p>
    </div>
  );
}
