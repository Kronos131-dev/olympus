import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "@/lib/auth/AuthContext";
import { Input, Select } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { Chip } from "@/components/ui/misc";
import { ACTIVITY_LABELS, GENDER_LABELS, GOAL_LABELS, optionsFrom } from "@/lib/labels";
import { ApiError } from "@/lib/api/client";
import type { ActivityLevel, Gender, Goal } from "@/types/api";

export default function RegisterPage() {
  const { register } = useAuth();
  const [form, setForm] = useState({
    email: "",
    recoveryEmail: "",
    password: "",
    gender: "MALE" as Gender,
    heightCm: "",
    weightKg: "",
    birthDate: "",
    activityLevel: "MODERATE" as ActivityLevel,
    goal: "MAINTAIN" as Goal,
  });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const set = <K extends keyof typeof form>(k: K, v: (typeof form)[K]) =>
    setForm((f) => ({ ...f, [k]: v }));

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await register({
        email: form.email.trim(),
        recoveryEmail: form.recoveryEmail.trim(),
        password: form.password,
        gender: form.gender,
        heightCm: Number(form.heightCm),
        weightKg: Number(form.weightKg),
        birthDate: form.birthDate,
        activityLevel: form.activityLevel,
        goal: form.goal,
      });
    } catch (err) {
      setError(
        err instanceof ApiError && err.message ? err.message : "Inscription impossible",
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-enter mx-auto max-w-md px-6 py-10">
      <div className="mb-8 text-center">
        <p className="lapidary text-[0.65rem] tracking-[0.4em] text-gold">Rejoindre</p>
        <h1 className="text-4xl text-marble">Olympus</h1>
      </div>

      <form onSubmit={submit} className="space-y-4">
        <Input label="Pseudo" value={form.email} onChange={(e) => set("email", e.target.value)} required />
        <Input
          label="Email"
          type="email"
          autoComplete="email"
          value={form.recoveryEmail}
          onChange={(e) => set("recoveryEmail", e.target.value)}
          required
        />
        <Input
          label="Mot de passe"
          type="password"
          minLength={6}
          value={form.password}
          onChange={(e) => set("password", e.target.value)}
          required
        />

        <div>
          <span className="lapidary mb-2 block text-[0.65rem] tracking-[0.15em] text-gold">Genre</span>
          <div className="flex gap-2">
            {(Object.keys(GENDER_LABELS) as Gender[]).map((g) => (
              <Chip key={g} active={form.gender === g} onClick={() => set("gender", g)}>
                {GENDER_LABELS[g]}
              </Chip>
            ))}
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <Input label="Taille (cm)" type="number" inputMode="numeric" value={form.heightCm} onChange={(e) => set("heightCm", e.target.value)} required />
          <Input label="Poids (kg)" type="number" inputMode="decimal" value={form.weightKg} onChange={(e) => set("weightKg", e.target.value)} required />
        </div>

        <Input label="Date de naissance" type="date" value={form.birthDate} onChange={(e) => set("birthDate", e.target.value)} required />

        <Select
          label="Niveau d'activité"
          value={form.activityLevel}
          onChange={(e) => set("activityLevel", e.target.value as ActivityLevel)}
          options={optionsFrom(ACTIVITY_LABELS)}
        />
        <Select
          label="Objectif"
          value={form.goal}
          onChange={(e) => set("goal", e.target.value as Goal)}
          options={optionsFrom(GOAL_LABELS)}
        />

        {error && <p className="text-sm text-[var(--color-danger)]">{error}</p>}
        <Button type="submit" block size="lg" loading={loading}>
          Forger mon destin
        </Button>
      </form>

      <p className="mt-8 text-center text-sm text-marble-dim">
        Déjà initié ?{" "}
        <Link to="/login" className="text-gold hover:underline">
          Se connecter
        </Link>
      </p>
    </div>
  );
}
