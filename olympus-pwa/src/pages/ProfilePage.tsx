import { useEffect, useState } from "react";
import { PageHeader } from "@/components/AppLayout";
import { Card, SectionTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input, Select } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";
import { useAuth } from "@/lib/auth/AuthContext";
import { useProfile, useUpdateProfile } from "@/hooks/queries";
import { ACTIVITY_LABELS, GENDER_LABELS, GOAL_LABELS, optionsFrom } from "@/lib/labels";
import { round } from "@/lib/utils";
import { Skeleton } from "@/components/ui/misc";
import type { ActivityLevel, Goal } from "@/types/api";

export default function ProfilePage() {
  const { user, logout } = useAuth();
  const profile = useProfile();
  const data = profile.data ?? user;
  const [editOpen, setEditOpen] = useState(false);
  const [targetsOpen, setTargetsOpen] = useState(false);

  return (
    <div>
      <PageHeader overline="L'athlète" title={data?.email ?? "Profil"} />

      <div className="space-y-5 px-5">
        {!data ? (
          <Skeleton className="h-40 w-full" />
        ) : (
          <>
            <Card tone="low">
              <SectionTitle>Cibles quotidiennes</SectionTitle>
              <div className="grid grid-cols-4 gap-2 text-center">
                <Target label="Kcal" value={round(data.targetKcal)} />
                <Target label="Prot" value={round(data.targetProteins)} />
                <Target label="Gluc" value={round(data.targetCarbs)} />
                <Target label="Lip" value={round(data.targetFats)} />
              </div>
              <p className="mt-3 text-xs text-marble-dim">
                {data.autoCalculateTargets ? "Calcul automatique" : "Cibles manuelles"}
              </p>
              <Button variant="ghost" size="sm" block className="mt-3" onClick={() => setTargetsOpen(true)}>
                Ajuster les cibles
              </Button>
            </Card>

            <Card tone="low">
              <SectionTitle>Biométrie</SectionTitle>
              <dl className="space-y-2 text-sm">
                <Row label="Genre" value={GENDER_LABELS[data.gender]} />
                <Row label="Taille" value={`${round(data.heightCm)} cm`} />
                <Row label="Poids actuel" value={`${round(data.currentWeightKg, 1)} kg`} />
                <Row label="Objectif" value={GOAL_LABELS[data.goal]} />
                <Row label="Activité" value={ACTIVITY_LABELS[data.activityLevel]} />
              </dl>
              <Button variant="ghost" size="sm" block className="mt-3" onClick={() => setEditOpen(true)}>
                Modifier
              </Button>
            </Card>

            <Button variant="danger" block onClick={logout}>
              Quitter l'arène
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
  const toast = useToast();
  const profile = useProfile();
  const update = useUpdateProfile();
  const [weight, setWeight] = useState("");
  const [height, setHeight] = useState("");
  const [goal, setGoal] = useState<Goal>("MAINTAIN");
  const [activity, setActivity] = useState<ActivityLevel>("MODERATE");

  useEffect(() => {
    if (open && profile.data) {
      setWeight(String(round(profile.data.currentWeightKg, 1)));
      setHeight(String(round(profile.data.heightCm)));
      setGoal(profile.data.goal);
      setActivity(profile.data.activityLevel);
    }
  }, [open, profile.data]);

  const save = () => {
    update.mutate(
      {
        currentWeightKg: Number(weight),
        heightCm: Number(height),
        goal,
        activityLevel: activity,
      },
      {
        onSuccess: () => {
          toast("Profil mis à jour", "success");
          onClose();
        },
        onError: () => toast("Mise à jour impossible", "error"),
      },
    );
  };

  return (
    <Modal open={open} onClose={onClose} title="Modifier le profil">
      <div className="space-y-3">
        <div className="grid grid-cols-2 gap-3">
          <Input label="Poids (kg)" type="number" inputMode="decimal" value={weight} onChange={(e) => setWeight(e.target.value)} />
          <Input label="Taille (cm)" type="number" inputMode="numeric" value={height} onChange={(e) => setHeight(e.target.value)} />
        </div>
        <Select label="Objectif" value={goal} onChange={(e) => setGoal(e.target.value as Goal)} options={optionsFrom(GOAL_LABELS)} />
        <Select label="Niveau d'activité" value={activity} onChange={(e) => setActivity(e.target.value as ActivityLevel)} options={optionsFrom(ACTIVITY_LABELS)} />
        <Button block loading={update.isPending} onClick={save}>
          Enregistrer
        </Button>
      </div>
    </Modal>
  );
}

function EditTargetsModal({ open, onClose }: { open: boolean; onClose: () => void }) {
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
          toast("Cibles mises à jour", "success");
          onClose();
        },
        onError: () => toast("Mise à jour impossible", "error"),
      },
    );
  };

  return (
    <Modal open={open} onClose={onClose} title="Cibles caloriques">
      <div className="space-y-4">
        <label className="flex items-center justify-between rounded-[var(--radius)] bg-surface-lowest px-4 py-3">
          <span className="text-sm text-marble">Calcul automatique</span>
          <input type="checkbox" checked={auto} onChange={(e) => setAuto(e.target.checked)} className="size-5 accent-[var(--color-gold)]" />
        </label>
        {!auto && (
          <div className="grid grid-cols-2 gap-3">
            <Input label="Calories" type="number" inputMode="numeric" value={kcal} onChange={(e) => setKcal(e.target.value)} />
            <Input label="Protéines" type="number" inputMode="numeric" value={prot} onChange={(e) => setProt(e.target.value)} />
            <Input label="Glucides" type="number" inputMode="numeric" value={carbs} onChange={(e) => setCarbs(e.target.value)} />
            <Input label="Lipides" type="number" inputMode="numeric" value={fats} onChange={(e) => setFats(e.target.value)} />
          </div>
        )}
        <Button block loading={update.isPending} onClick={save}>
          Enregistrer
        </Button>
      </div>
    </Modal>
  );
}
