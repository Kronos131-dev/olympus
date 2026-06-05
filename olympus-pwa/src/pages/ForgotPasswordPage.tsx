import { useState } from "react";
import { Link } from "react-router-dom";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { authApi } from "@/lib/api/endpoints";
import { useT } from "@/lib/i18n";

export default function ForgotPasswordPage() {
  const t = useT();
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await authApi.requestPasswordReset(email.trim());
    } catch {
      // On ne révèle jamais si l'email existe : message identique dans tous les cas.
    } finally {
      setLoading(false);
      setSent(true);
    }
  };

  return (
    <div className="page-enter mx-auto flex min-h-dvh max-w-md flex-col justify-center px-6">
      <div className="mb-10 text-center">
        <p className="lapidary text-[0.65rem] tracking-[0.4em] text-gold">{t.auth.login.overline}</p>
        <h1 className="text-3xl text-marble">{t.auth.forgot.title}</h1>
        <p className="mt-3 text-sm text-marble-dim">{t.auth.forgot.subtitle}</p>
      </div>

      {sent ? (
        <p className="rounded-[var(--radius)] bg-surface-high px-4 py-3 text-center text-sm text-marble">
          {t.auth.forgot.sent}
        </p>
      ) : (
        <form onSubmit={submit} className="space-y-4">
          <Input
            label={t.auth.forgot.email}
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <Button type="submit" block size="lg" loading={loading}>
            {t.auth.forgot.submit}
          </Button>
        </form>
      )}

      <p className="mt-8 text-center text-sm text-marble-dim">
        <Link to="/login" className="text-gold hover:underline">
          {t.auth.forgot.back}
        </Link>
      </p>
    </div>
  );
}
