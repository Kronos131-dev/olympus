import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { authApi } from "@/lib/api/endpoints";
import { errorMessage } from "@/lib/api/client";

export default function ResetPasswordPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const token = params.get("token") ?? "";

  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!token) {
      setError("Lien de réinitialisation invalide ou manquant.");
      return;
    }
    if (password.length < 6) {
      setError("Le mot de passe doit contenir au moins 6 caractères.");
      return;
    }
    if (password !== confirm) {
      setError("Les mots de passe ne correspondent pas.");
      return;
    }
    setLoading(true);
    try {
      await authApi.resetPassword(token, password);
      setDone(true);
      setTimeout(() => navigate("/login"), 2500);
    } catch (err) {
      setError(errorMessage(err, "Lien invalide ou expiré. Demande un nouveau lien."));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-enter mx-auto flex min-h-dvh max-w-md flex-col justify-center px-6">
      <div className="mb-10 text-center">
        <p className="lapidary text-[0.65rem] tracking-[0.4em] text-gold">Le Panthéon</p>
        <h1 className="text-3xl text-marble">Nouveau mot de passe</h1>
      </div>

      {done ? (
        <p className="rounded-[var(--radius)] bg-surface-high px-4 py-3 text-center text-sm text-marble">
          Mot de passe réinitialisé. Redirection vers la connexion…
        </p>
      ) : (
        <form onSubmit={submit} className="space-y-4">
          <Input
            label="Nouveau mot de passe"
            type="password"
            autoComplete="new-password"
            minLength={6}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          <Input
            label="Confirmer le mot de passe"
            type="password"
            autoComplete="new-password"
            minLength={6}
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            required
          />
          {error && <p className="text-sm text-[var(--color-danger)]">{error}</p>}
          <Button type="submit" block size="lg" loading={loading}>
            Réinitialiser
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
