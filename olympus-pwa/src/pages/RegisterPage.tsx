import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "@/lib/auth/AuthContext";
import { Input, Select } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { Chip } from "@/components/ui/misc";
import { optionsFrom } from "@/lib/labels";
import { useT } from "@/lib/i18n";
import { ApiError } from "@/lib/api/client";
import type { ActivityLevel, Gender, Goal } from "@/types/api";

export default function RegisterPage() {
  const t = useT();
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
        err instanceof ApiError && err.message ? err.message : t.auth.register.error,
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-enter mx-auto max-w-md px-6 py-10">
      <div className="mb-8 text-center">
        <p className="lapidary text-[0.65rem] tracking-[0.4em] text-gold">{t.auth.register.overline}</p>
        <h1 className="text-4xl text-marble">Olympus</h1>
      </div>

      <form onSubmit={submit} className="space-y-4">
        <Input label={t.auth.register.pseudo} value={form.email} onChange={(e) => set("email", e.target.value)} required />
        <Input
          label={t.auth.register.email}
          type="email"
          autoComplete="email"
          value={form.recoveryEmail}
          onChange={(e) => set("recoveryEmail", e.target.value)}
          required
        />
        <Input
          label={t.auth.register.password}
          type="password"
          minLength={6}
          value={form.password}
          onChange={(e) => set("password", e.target.value)}
          required
        />

        <div>
          <span className="lapidary mb-2 block text-[0.65rem] tracking-[0.15em] text-gold">{t.auth.register.gender}</span>
          <div className="flex gap-2">
            {(Object.keys(t.enums.gender) as Gender[]).map((g) => (
              <Chip key={g} active={form.gender === g} onClick={() => set("gender", g)}>
                {t.enums.gender[g]}
              </Chip>
            ))}
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <Input label={t.auth.register.height} type="number" inputMode="numeric" value={form.heightCm} onChange={(e) => set("heightCm", e.target.value)} required />
          <Input label={t.auth.register.weight} type="number" inputMode="decimal" value={form.weightKg} onChange={(e) => set("weightKg", e.target.value)} required />
        </div>

        <Input label={t.auth.register.birthDate} type="date" value={form.birthDate} onChange={(e) => set("birthDate", e.target.value)} required />

        <Select
          label={t.auth.register.activity}
          value={form.activityLevel}
          onChange={(e) => set("activityLevel", e.target.value as ActivityLevel)}
          options={optionsFrom(t.enums.activity)}
        />
        <Select
          label={t.auth.register.goal}
          value={form.goal}
          onChange={(e) => set("goal", e.target.value as Goal)}
          options={optionsFrom(t.enums.goal)}
        />

        {error && <p className="text-sm text-[var(--color-danger)]">{error}</p>}
        <Button type="submit" block size="lg" loading={loading}>
          {t.auth.register.submit}
        </Button>
      </form>

      <p className="mt-8 text-center text-sm text-marble-dim">
        {t.auth.register.haveAccount}{" "}
        <Link to="/login" className="text-gold hover:underline">
          {t.auth.register.login}
        </Link>
      </p>
    </div>
  );
}
