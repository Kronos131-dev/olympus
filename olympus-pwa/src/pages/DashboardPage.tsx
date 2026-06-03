import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { PageHeader } from "@/components/AppLayout";
import { Card, SectionTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { ProgressRing } from "@/components/ui/ProgressRing";
import { MacroBar } from "@/components/ui/MacroBar";
import { Modal } from "@/components/ui/Modal";
import { Input } from "@/components/ui/Input";
import { EmptyState, Skeleton } from "@/components/ui/misc";
import { useToast } from "@/components/ui/Toast";
import { IconActivity, IconPlus, IconTrash } from "@/components/icons";
import { useDailyLog, useDeleteLogEntry, useProfile, useUpdateActivity } from "@/hooks/queries";
import { formatDateLong, macrosFor, round, todayIso } from "@/lib/utils";
import type { LogEntryResponse } from "@/types/api";

function entryMacros(e: LogEntryResponse) {
  if (e.mealPreset) {
    return {
      name: e.mealPreset.name,
      kcal: e.mealPreset.totalKcal,
      detail: "Repas",
    };
  }
  if (e.foodItem) {
    const m = macrosFor(e.foodItem, e.quantityGrams);
    return { name: e.foodItem.name, kcal: m.kcal, detail: `${round(e.quantityGrams)} g` };
  }
  return { name: "Entrée", kcal: 0, detail: "" };
}

export default function DashboardPage() {
  const today = todayIso();
  const navigate = useNavigate();
  const toast = useToast();
  const profile = useProfile();
  const log = useDailyLog(today);
  const deleteEntry = useDeleteLogEntry(today);
  const [activityOpen, setActivityOpen] = useState(false);

  const targetKcal = profile.data?.targetKcal ?? 0;
  const extraBurned = log.data?.extraKcalBurned ?? 0;
  const effectiveTarget = round(targetKcal + extraBurned);
  const consumed = round(log.data?.totalKcal ?? 0);
  const remaining = effectiveTarget - consumed;

  const sortedEntries = useMemo(
    () => [...(log.data?.entries ?? [])].reverse(),
    [log.data?.entries],
  );

  return (
    <div>
      <PageHeader overline={formatDateLong(today)} title="Résumé du jour" />

      <div className="px-5">
        {log.isLoading || profile.isLoading ? (
          <Skeleton className="mx-auto h-56 w-56 rounded-full" />
        ) : (
          <div className="flex flex-col items-center">
            <ProgressRing value={consumed} max={effectiveTarget || 1}>
              <div>
                <p className="font-display text-5xl text-marble">{consumed}</p>
                <p className="lapidary text-[0.6rem] tracking-[0.2em] text-marble-dim">
                  / {effectiveTarget} kcal
                </p>
                <p
                  className={`mt-1 text-xs ${remaining >= 0 ? "text-gold" : "text-[var(--color-danger)]"}`}
                >
                  {remaining >= 0 ? `${remaining} restantes` : `${-remaining} en trop`}
                </p>
              </div>
            </ProgressRing>
          </div>
        )}

        <div className="mt-6 grid grid-cols-3 gap-3">
          <MacroBar label="Protéines" value={log.data?.totalProteins ?? 0} target={profile.data?.targetProteins} color="var(--color-purple-bright)" />
          <MacroBar label="Glucides" value={log.data?.totalCarbs ?? 0} target={profile.data?.targetCarbs} color="var(--color-gold)" />
          <MacroBar label="Lipides" value={log.data?.totalFats ?? 0} target={profile.data?.targetFats} color="#c98b6b" />
        </div>

        <div className="mt-5 flex gap-3">
          <Button block onClick={() => navigate("/food/add")}>
            <IconPlus size={16} /> Aliment
          </Button>
          <Button block variant="ghost" onClick={() => navigate("/meals")}>
            <IconPlus size={16} /> Repas
          </Button>
        </div>

        <Card tone="low" className="mt-5">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <span className="text-gold">
                <IconActivity size={20} />
              </span>
              <div>
                <p className="text-sm text-marble">
                  {log.data?.stepCount ? `${log.data.stepCount} pas` : "Activité"}
                </p>
                <p className="text-xs text-marble-dim">
                  {round(extraBurned)} kcal brûlées
                  {log.data?.workoutDurationMinutes
                    ? ` · ${log.data.workoutDurationMinutes} min`
                    : ""}
                </p>
              </div>
            </div>
            <Button variant="ghost" size="sm" onClick={() => setActivityOpen(true)}>
              Éditer
            </Button>
          </div>
        </Card>

        <section className="mt-6">
          <SectionTitle>Consommations récentes</SectionTitle>
          {log.isLoading ? (
            <div className="space-y-1">
              <Skeleton className="h-14 w-full" />
              <Skeleton className="h-14 w-full" />
            </div>
          ) : sortedEntries.length === 0 ? (
            <EmptyState title="Rien de consommé" hint="Ajoute un aliment ou un repas pour commencer." />
          ) : (
            <ul className="space-y-1">
              {sortedEntries.map((e) => {
                const m = entryMacros(e);
                return (
                  <li
                    key={e.id}
                    className="flex items-center justify-between bg-surface-low px-4 py-3"
                  >
                    <div className="min-w-0">
                      <p className="truncate text-sm text-marble">{m.name}</p>
                      <p className="text-xs text-marble-dim">
                        {m.detail} · {round(m.kcal)} kcal
                        {e.fromPlan && <span className="text-gold/70"> · plan</span>}
                      </p>
                    </div>
                    <button
                      onClick={() =>
                        deleteEntry.mutate(e.id, {
                          onError: () => toast("Suppression impossible", "error"),
                        })
                      }
                      className="text-marble-dim transition-colors hover:text-[var(--color-danger)]"
                      aria-label="Supprimer"
                    >
                      <IconTrash size={18} />
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </section>
      </div>

      <ActivityModal open={activityOpen} onClose={() => setActivityOpen(false)} date={today} />
    </div>
  );
}

function ActivityModal({ open, onClose, date }: { open: boolean; onClose: () => void; date: string }) {
  const toast = useToast();
  const log = useDailyLog(date);
  const update = useUpdateActivity(date);
  const [steps, setSteps] = useState("");
  const [workout, setWorkout] = useState("");
  const [burned, setBurned] = useState("");

  // Pré-remplit à l'ouverture.
  const data = log.data;
  useEffect(() => {
    if (open) {
      setSteps(data?.stepCount ? String(data.stepCount) : "");
      setWorkout(data?.workoutDurationMinutes ? String(data.workoutDurationMinutes) : "");
      setBurned(data?.manualKcalBurned ? String(data.manualKcalBurned) : "");
    }
  }, [open, data]);

  const save = () => {
    update.mutate(
      {
        targetDate: date,
        stepCount: steps ? Number(steps) : undefined,
        workoutDurationMinutes: workout ? Number(workout) : undefined,
        manualKcalBurned: burned ? Number(burned) : undefined,
      },
      {
        onSuccess: () => {
          toast("Activité mise à jour", "success");
          onClose();
        },
        onError: () => toast("Échec de la mise à jour", "error"),
      },
    );
  };

  return (
    <Modal open={open} onClose={onClose} title="Activité du jour">
      <div className="space-y-3">
        <Input label="Pas" type="number" inputMode="numeric" value={steps} onChange={(e) => setSteps(e.target.value)} />
        <Input label="Entraînement (min)" type="number" inputMode="numeric" value={workout} onChange={(e) => setWorkout(e.target.value)} />
        <Input label="Calories brûlées (manuel)" type="number" inputMode="numeric" value={burned} onChange={(e) => setBurned(e.target.value)} />
        <Button block loading={update.isPending} onClick={save}>
          Enregistrer
        </Button>
      </div>
    </Modal>
  );
}
