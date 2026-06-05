import { useState } from "react";
import { Link } from "react-router-dom";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { authApi } from "@/lib/api/endpoints";

export default function ForgotPasswordPage() {
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
        <p className="lapidary text-[0.65rem] tracking-[0.4em] text-gold">Le Panthéon</p>
        <h1 className="text-3xl text-marble">Mot de passe oublié</h1>
        <p className="mt-3 text-sm text-marble-dim">
          Saisis l'email associé à ton compte pour recevoir un lien de réinitialisation.
        </p>
      </div>

      {sent ? (
        <p className="rounded-[var(--radius)] bg-surface-high px-4 py-3 text-center text-sm text-marble">
          Si un compte est associé à cet email, un lien de réinitialisation vient d'être envoyé.
          Pense à vérifier tes spams.
        </p>
      ) : (
        <form onSubmit={submit} className="space-y-4">
          <Input
            label="Email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <Button type="submit" block size="lg" loading={loading}>
            Envoyer le lien
          </Button>
        </form>
      )}

      <p className="mt-8 text-center text-sm text-marble-dim">
        <Link to="/login" className="text-gold hover:underline">
          Retour à la connexion
        </Link>
      </p>
    </div>
  );
}
