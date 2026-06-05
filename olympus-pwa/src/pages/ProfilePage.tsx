import { useEffect, useState } from "react";
import { PageHeader } from "@/components/AppLayout";
import { Card, SectionTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input, Select } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";
import { errorMessage } from "@/lib/api/client";
import { useAuth } from "@/lib/auth/AuthContext";
import { useProfile, useUpdateProfile } from "@/hooks/queries";
import { optionsFrom } from "@/lib/labels";
import { useLang, useT } from "@/lib/i18n";
import { cn, round } from "@/lib/utils";
import { Skeleton } from "@/components/ui/misc";
import type { ActivityLevel, Gender, Goal } from "@/types/api";

export default function ProfilePage() {
  const t = useT();
  const { lang, setLang } = useLang();
  const { user, logout } = useAuth();
  const profile = useProfile();
  const data = profile.data ?? user;
  const [editOpen, setEditOpen] = useState(false);
  const [targetsOpen, setTargetsOpen] = useState(false);

  return (
    <div>
      <PageHeader overline={t.profile.overline} title={data?.email ?? t.profile.titleFallback} />

      <div className="space-y-5 px-5">
        {!data ? (
          <Skeleton className="h-40 w-full" />
        ) : (
          <>
            <Card tone="low">
              <SectionTitle>{t.profile.targets}</SectionTitle>
              <div className="grid grid-cols-2 gap-2 text-center sm:grid-cols-4">
                <Target label={t.common.kcal} value={round(data.targetKcal)} />
                <Target label={t.common.macrosShort.proteins} value={round(data.targetProteins)} />
                <Target label={t.common.macrosShort.carbs} value={round(data.targetCarbs)} />
                <Target label={t.common.macrosShort.fats} value={round(data.targetFats)} />
              </div>
              <p className="mt-3 text-xs text-marble-dim">
                {data.autoCalculateTargets ? t.profile.autoTargets : t.profile.manualTargets}
              </p>
              <Button variant="ghost" size="sm" block className="mt-3" onClick={() => setTargetsOpen(true)}>
                {t.profile.adjustTargets}
              </Button>
            </Card>

            <Card tone="low">
              <SectionTitle>{t.profile.biometry}</SectionTitle>
              <dl className="space-y-2 text-sm">
                <Row label={t.profile.gender} value={t.enums.gender[data.gender]} />
                <Row label={t.profile.height} value={`${round(data.heightCm)} cm`} />
                <Row label={t.profile.currentWeight} value={`${round(data.currentWeightKg, 1)} kg`} />
                <Row label={t.profile.goal} value={t.enums.goal[data.goal]} />
                <Row label={t.profile.activity} value={t.enums.activity[data.activityLevel]} />
              </dl>
              <Button variant="ghost" size="sm" block className="mt-3" onClick={() => setEditOpen(true)}>
                {t.profile.edit}
              </Button>
            </Card>

            <Card tone="low">
              <SectionTitle>{t.profile.preferences}</SectionTitle>
              <div className="flex items-center justify-between">
                <span className="text-sm text-marble">{t.profile.language}</span>
                <div className="flex rounded-full bg-surface-high p-0.5 text-xs font-semibold">
                  {(["fr", "en"] as const).map((l) => (
                    <button
                      key={l}
                      onClick={() => setLang(l)}
                      className={cn(
                        "rounded-full px-3 py-1 transition-colors",
                        lang === l ? "bg-gold text-[var(--color-on-gold)]" : "text-marble-dim",
                      )}
                    >
                      {l === "fr" ? "Français" : "English"}
                    </button>
                  ))}
                </div>
              </div>
            </Card>

            <Button variant="danger" block onClick={logout}>
              {t.profile.logout}
            </Button>
          </>
        )}
      </div>

      {data && <EditProfileModal open={editOpen} onClose={() => setEditOpen(false)} />}
      {data && <EditTargetsModal open={targetsOpen} onClose={() => setTargetsOpen(false)} />}
    </div>
  );
}

function Target({ label, value }: { label: string; value: number }) {
  return (
    <div>
      <p className="font-display text-xl text-gold">{value}</p>
      <p className="lapidary text-[0.5rem] tracking-[0.1em] text-marble-dim">{label}</p>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between">
      <dt className="text-marble-dim">{label}</dt>
      <dd className="text-marble">{value}</dd>
    </div>
  );
}

function EditProfileModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const t = useT();
  const toast = useToast();
  const profile = useProfile();
  const update = useUpdateProfile();
  const [weight, setWeight] = useState("");
  const [height, setHeight] = useState("");
  const [gender, setGender] = useState<Gender>("MALE");
  const [birthDate, setBirthDate] = useState("");
  const [goal, setGoal] = useState<Goal>("MAINTAIN");
  const [activity, setActivity] = useState<ActivityLevel>("MODERATE");
  const [recoveryEmail, setRecoveryEmail] = useState("");

  useEffect(() => {
    if (open && profile.data) {
      setWeight(String(round(profile.data.currentWeightKg, 1)));
      setHeight(String(round(profile.data.heightCm)));
      setGender(profile.data.gender ?? "MALE");
      setBirthDate(profile.data.birthDate ?? "");
      setGoal(profile.data.goal);
      setActivity(profile.data.activityLevel);
      setRecoveryEmail(profile.data.recoveryEmail ?? "");
    }
  }, [open, profile.data]);

  const save = () => {
    update.mutate(
      {
        currentWeightKg: Number(weight),
        heightCm: Number(height),
        gender,
        birthDate: birthDate || undefined,
        goal,
        activityLevel: activity,
        recoveryEmail: recoveryEmail.trim() || undefined,
      },
      {
        onSuccess: () => {
          toast(t.profile.updated, "success");
          onClose();
        },
        onError: (e) => toast(errorMessage(e, t.profile.updateError), "error"),
      },
    );
  };

  return (
    <Modal open={open} onClose={onClose} title={t.profile.editTitle}>
      <div className="space-y-3">
        <div className="grid grid-cols-2 gap-3">
          <Input label={t.profile.weight} type="number" inputMode="decimal" value={weight} onChange={(e) => setWeight(e.target.value)} />
          <Input label={t.profile.heightCm} type="number" inputMode="numeric" value={height} onChange={(e) => setHeight(e.target.value)} />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Select label={t.profile.gender} value={gender} onChange={(e) => setGender(e.target.value as Gender)} options={optionsFrom(t.enums.gender)} />
          <Input label={t.profile.birthDate} type="date" value={birthDate} onChange={(e) => setBirthDate(e.target.value)} />
        </div>
        <Select label={t.profile.goal} value={goal} onChange={(e) => setGoal(e.target.value as Goal)} options={optionsFrom(t.enums.goal)} />
        <Select label={t.auth.register.activity} value={activity} onChange={(e) => setActivity(e.target.value as ActivityLevel)} options={optionsFrom(t.enums.activity)} />
        <Input label={t.profile.recoveryEmail} type="email" autoComplete="email" value={recoveryEmail} onChange={(e) => setRecoveryEmail(e.target.value)} />
        <Button block loading={update.isPending} onClick={save}>
          {t.common.save}
        </Button>
      </div>
    </Modal>
  );
}

function EditTargetsModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const t = useT();
  const toast = useToast();
  const profile = useProfile();
  const update = useUpdateProfile();
  const [auto, setAuto] = useState(true);
  const [kcal, setKcal] = useState("");
  const [prot, setProt] = useState("");
  const [carbs, setCarbs] = useState("");
  const [fats, setFats] = useState("");

  useEffect(() => {
    if (open && profile.data) {
      const d = profile.data;
      setAuto(d.autoCalculateTargets);
      setKcal(String(round(d.manualTargetKcal ?? d.targetKcal)));
      setProt(String(round(d.manualTargetProteins ?? d.targetProteins)));
      setCarbs(String(round(d.manualTargetCarbs ?? d.targetCarbs)));
      setFats(String(round(d.manualTargetFats ?? d.targetFats)));
    }
  }, [open, profile.data]);

  const save = () => {
    update.mutate(
      {
        autoCalculateTargets: auto,
        ...(auto
          ? {}
          : {
              manualTargetKcal: Number(kcal),
              manualTargetProteins: Number(prot),
              manualTargetCarbs: Number(carbs),
              manualTargetFats: Number(fats),
            }),
      },
      {
        onSuccess: () => {
          toast(t.profile.targetsUpdated, "success");
          onClose();
        },
        onError: (e) => toast(errorMessage(e, t.profile.updateError), "error"),
      },
    );
  };

  return (
    <Modal open={open} onClose={onClose} title={t.profile.targetsTitle}>
      <div className="space-y-4">
        <label className="flex items-center justify-between rounded-[var(--radius)] bg-surface-lowest px-4 py-3">
          <span className="text-sm text-marble">{t.profile.autoCalc}</span>
          <input type="checkbox" checked={auto} onChange={(e) => setAuto(e.target.checked)} className="size-5 accent-[var(--color-gold)]" />
        </label>
        {!auto && (
          <div className="grid grid-cols-2 gap-3">
            <Input label={t.profile.calories} type="number" inputMode="numeric" value={kcal} onChange={(e) => setKcal(e.target.value)} />
            <Input label={t.profile.proteins} type="number" inputMode="numeric" value={prot} onChange={(e) => setProt(e.target.value)} />
            <Input label={t.profile.carbs} type="number" inputMode="numeric" value={carbs} onChange={(e) => setCarbs(e.target.value)} />
            <Input label={t.profile.fats} type="number" inputMode="numeric" value={fats} onChange={(e) => setFats(e.target.value)} />
          </div>
        )}
        <Button block loading={update.isPending} onClick={save}>
          {t.common.save}
        </Button>
      </div>
    </Modal>
  );
}
