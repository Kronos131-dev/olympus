import { useEffect, useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { mealPlanApi } from "@/lib/api/endpoints";
import { PageHeader } from "@/components/AppLayout";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Chip, Skeleton, Spinner } from "@/components/ui/misc";
import { useToast } from "@/components/ui/Toast";
import { FoodFinder } from "@/components/FoodFinder";
import { usePresets, useWeeklyPlan } from "@/hooks/queries";
import { IconPlus, IconSparkle, IconTrash } from "@/components/icons";
import { macrosFor, round, WEEK_DAYS } from "@/lib/utils";
import { useT } from "@/lib/i18n";
import type {
  DayOfWeek,
  FoodItemResponse,
  PlannedMealEntryRequest,
  PlannedMealEntryResponse,
} from "@/types/api";

// Entrée de travail : identifiant local + données d'affichage.
interface WorkEntry extends PlannedMealEntryResponse {
  localId: string;
}

let counter = 0;
const localId = () => `e${counter++}`;

function entryKcal(e: WorkEntry): number {
  if (e.mealPreset) return e.mealPreset.totalKcal;
  if (e.foodItem) return macrosFor(e.foodItem, e.quantityGrams).kcal;
  return 0;
}

export default function PlanPage() {
  const t = useT();
  const toast = useToast();
  const qc = useQueryClient();
  const plan = useWeeklyPlan();
  const presets = usePresets();

  const [entries, setEntries] = useState<WorkEntry[]>([]);
  const [saving, setSaving] = useState(false);
  const [addDay, setAddDay] = useState<DayOfWeek | null>(null);
  const [genOpen, setGenOpen] = useState(false);

  // Hydrate l'état local depuis le serveur.
  useEffect(() => {
    if (plan.data?.entries) {
      setEntries(plan.data.entries.map((e) => ({ ...e, localId: localId() })));
    }
  }, [plan.data]);

  const byDay = useMemo(() => {
    const map: Record<string, WorkEntry[]> = {};
    for (const d of WEEK_DAYS) map[d] = [];
    for (const e of entries) (map[e.dayOfWeek] ??= []).push(e);
    return map;
  }, [entries]);

  const persist = async (next: WorkEntry[]) => {
    setEntries(next);
    setSaving(true);
    const body: { entries: PlannedMealEntryRequest[] } = {
      entries: next.map((e) => ({
        dayOfWeek: e.dayOfWeek,
        quantityGrams: e.quantityGrams,
        foodItemId: e.foodItem?.id,
        mealPresetId: e.mealPreset?.id,
      })),
    };
    try {
      await mealPlanApi.saveWeekly(body);
    } catch {
      toast(t.plan.saveError, "error");
    } finally {
      setSaving(false);
    }
  };

  const removeEntry = (lid: string) => persist(entries.filter((e) => e.localId !== lid));

  const addPreset = (day: DayOfWeek, presetId: number) => {
    const preset = presets.data?.find((p) => p.id === presetId);
    if (!preset) return;
    persist([
      ...entries,
      { localId: localId(), id: 0, dayOfWeek: day, quantityGrams: 0, mealPreset: preset },
    ]);
    setAddDay(null);
  };

  const addFood = (day: DayOfWeek, food: FoodItemResponse, grams: number) => {
    persist([
      ...entries,
      { localId: localId(), id: 0, dayOfWeek: day, quantityGrams: grams, foodItem: food },
    ]);
    setAddDay(null);
  };

  const generate = async (prompt: string) => {
    try {
      const generated = await mealPlanApi.generate({ prompt: prompt || undefined });
      setEntries(generated.entries.map((e) => ({ ...e, localId: localId() })));
      qc.setQueryData(["weeklyPlan"], generated);
      toast(t.plan.generated, "success");
      setGenOpen(false);
    } catch {
      toast(t.plan.genError, "error");
    }
  };

  return (
    <div>
      <PageHeader
        overline={t.plan.overline}
        title={t.plan.title}
        action={
          <Button size="sm" variant="ghost" onClick={() => setGenOpen(true)}>
            <IconSparkle size={16} /> {t.plan.oracle}
          </Button>
        }
      />

      <div className="px-5">
        {saving && (
          <p className="mb-2 flex items-center gap-2 text-xs text-marble-dim">
            <Spinner className="size-3" /> {t.plan.saving}
          </p>
        )}
        {plan.isLoading ? (
          <div className="space-y-3">
            <Skeleton className="h-28 w-full" />
            <Skeleton className="h-28 w-full" />
          </div>
        ) : (
          <div className="space-y-4">
            {WEEK_DAYS.map((day) => {
              const dayEntries = byDay[day] ?? [];
              const total = dayEntries.reduce((s, e) => s + entryKcal(e), 0);
              return (
                <Card key={day} tone="low">
                  <div className="mb-3 flex items-baseline justify-between">
                    <h3 className="lapidary text-sm tracking-[0.12em] text-gold">{t.days[day]}</h3>
                    <span className="text-xs text-marble-dim">{round(total)} {t.common.kcal}</span>
                  </div>
                  <ul className="space-y-1">
                    {dayEntries.map((e) => (
                      <li key={e.localId} className="flex items-center justify-between bg-surface px-3 py-2">
                        <div className="min-w-0">
                          <p className="truncate text-sm text-marble">
                            {e.mealPreset?.name ?? e.foodItem?.name}
                          </p>
                          <p className="text-xs text-marble-dim">
                            {e.foodItem ? `${round(e.quantityGrams)} g · ` : ""}
                            {round(entryKcal(e))} {t.common.kcal}
                          </p>
                        </div>
                        <button
                          onClick={() => removeEntry(e.localId)}
                          className="text-marble-dim hover:text-[var(--color-danger)]"
                          aria-label={t.plan.remove}
                        >
                          <IconTrash size={16} />
                        </button>
                      </li>
                    ))}
                  </ul>
                  <Button variant="subtle" size="sm" block className="mt-3" onClick={() => setAddDay(day)}>
                    <IconPlus size={14} /> {t.plan.add}
                  </Button>
                </Card>
              );
            })}
          </div>
        )}
      </div>

      {addDay && (
        <AddToDayModal
          day={addDay}
          onClose={() => setAddDay(null)}
          onAddPreset={addPreset}
          onAddFood={addFood}
        />
      )}
      <GenerateModal open={genOpen} onClose={() => setGenOpen(false)} onGenerate={generate} />
    </div>
  );
}

function AddToDayModal({
  day,
  onClose,
  onAddPreset,
  onAddFood,
}: {
  day: DayOfWeek;
  onClose: () => void;
  onAddPreset: (day: DayOfWeek, presetId: number) => void;
  onAddFood: (day: DayOfWeek, food: FoodItemResponse, grams: number) => void;
}) {
  const t = useT();
  const presets = usePresets();
  const [tab, setTab] = useState<"presets" | "food">("presets");
  const [pendingFood, setPendingFood] = useState<FoodItemResponse | null>(null);
  const [grams, setGrams] = useState("100");

  return (
    <Modal open onClose={onClose} title={t.plan.addModalTitle(t.days[day])}>
      <div className="mb-4 flex gap-2">
        <Chip active={tab === "presets"} onClick={() => setTab("presets")}>
          {t.plan.chipMeals}
        </Chip>
        <Chip active={tab === "food"} onClick={() => setTab("food")}>
          {t.plan.chipFood}
        </Chip>
      </div>

      {tab === "presets" ? (
        <div className="space-y-1">
          {(presets.data ?? []).map((p) => (
            <button
              key={p.id}
              onClick={() => onAddPreset(day, p.id)}
              className="flex w-full items-center justify-between rounded-[var(--radius)] bg-surface-low px-4 py-3 text-left hover:bg-surface-high"
            >
              <span className="truncate text-sm text-marble">{p.name}</span>
              <span className="text-xs text-marble-dim">{round(p.totalKcal)} {t.common.kcal}</span>
            </button>
          ))}
          {presets.data?.length === 0 && (
            <p className="py-6 text-center text-xs text-marble-dim">{t.plan.noMeals}</p>
          )}
        </div>
      ) : pendingFood ? (
        <div className="space-y-4">
          <p className="text-sm text-marble">{pendingFood.name}</p>
          <Input label={t.common.quantityG} type="number" inputMode="decimal" value={grams} onChange={(e) => setGrams(e.target.value)} autoFocus />
          <Button block onClick={() => onAddFood(day, pendingFood, Number(grams) || 0)}>
            {t.plan.addToDay(t.days[day].toLowerCase())}
          </Button>
        </div>
      ) : (
        <FoodFinder onPick={setPendingFood} />
      )}
    </Modal>
  );
}

function GenerateModal({
  open,
  onClose,
  onGenerate,
}: {
  open: boolean;
  onClose: () => void;
  onGenerate: (prompt: string) => Promise<void>;
}) {
  const t = useT();
  const [prompt, setPrompt] = useState("");
  const [loading, setLoading] = useState(false);

  const run = async () => {
    setLoading(true);
    await onGenerate(prompt);
    setLoading(false);
  };

  return (
    <Modal open={open} onClose={onClose} title={t.plan.genTitle}>
      <div className="space-y-4">
        <p className="text-xs text-marble-dim">{t.plan.genDesc}</p>
        <textarea
          value={prompt}
          onChange={(e) => setPrompt(e.target.value)}
          rows={3}
          placeholder={t.plan.genPlaceholder}
          className="w-full resize-none rounded-[var(--radius)] border border-outline/60 bg-surface-lowest p-4 text-marble outline-none focus:border-gold"
        />
        <Button block loading={loading} onClick={run}>
          <IconSparkle size={16} /> {t.plan.generate}
        </Button>
      </div>
    </Modal>
  );
}
