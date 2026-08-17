import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { PageHeader } from "@/components/AppLayout";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { EmptyState, Skeleton } from "@/components/ui/misc";
import { Modal } from "@/components/ui/Modal";
import { NutrientLine } from "@/components/ui/NutrientLine";
import { useToast } from "@/components/ui/Toast";
import { errorMessage } from "@/lib/api/client";
import { IconEdit, IconPlus, IconSparkle, IconTrash } from "@/components/icons";
import {
  useAddLogEntry,
  useDeletePreset,
  usePresets,
  useProfile,
} from "@/hooks/queries";
import { todayIso } from "@/lib/utils";
import { useT } from "@/lib/i18n";
import type { MealPresetResponse } from "@/types/api";

// Liste des repas prédéfinis : consommer, éditer, supprimer, créer (manuel ou IA).
export default function MealsPage() {
  const t = useT();
  const navigate = useNavigate();
  const toast = useToast();
  const presets = usePresets();
  const profile = useProfile();
  const deletePreset = useDeletePreset();
  const addEntry = useAddLogEntry(todayIso());
  const [search, setSearch] = useState("");
  const [toDelete, setToDelete] = useState<MealPresetResponse | null>(null);
  // addEntry.isPending est partagé par toutes les cartes : sans cet id, consommer un repas
  // fait tourner le spinner du bouton de tous les autres repas de la liste.
  const [consumingId, setConsumingId] = useState<number | null>(null);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    const list = presets.data ?? [];
    return q ? list.filter((p) => p.name.toLowerCase().includes(q)) : list;
  }, [presets.data, search]);

  const consume = (preset: MealPresetResponse) => {
    setConsumingId(preset.id);
    addEntry.mutate(
      { targetDate: todayIso(), mealPresetId: preset.id },
      {
        onSuccess: () => toast(t.meals.consumed(preset.name), "success"),
        onError: (e) => toast(errorMessage(e, t.meals.consumeError), "error"),
        onSettled: () => setConsumingId(null),
      },
    );
  };

  return (
    <div>
      <PageHeader
        overline={t.meals.overline}
        title={t.meals.title}
        action={
          <Button size="sm" onClick={() => navigate("/meals/new")}>
            <IconPlus size={16} /> {t.meals.new}
          </Button>
        }
      />

      <div className="px-5">
        <div className="mb-4 flex gap-2">
          <div className="flex-1">
            <Input
              placeholder={t.meals.searchPlaceholder}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <Button variant="ghost" onClick={() => navigate("/meals/new?ai=1")}>
            <IconSparkle size={16} /> {t.meals.ai}
          </Button>
        </div>

        {presets.isLoading ? (
          <div className="space-y-2">
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-24 w-full" />
          </div>
        ) : filtered.length === 0 ? (
          <EmptyState title={t.meals.emptyTitle} hint={t.meals.emptyHint} />
        ) : (
          <div className="space-y-3">
            {filtered.map((preset) => (
              <Card key={preset.id} tone="low" goldEdge>
                <div className="flex items-start justify-between">
                  <div className="min-w-0">
                    <h3 className="truncate text-lg text-marble">
                      {preset.name}
                    </h3>
                    <p className="text-xs text-marble-dim">
                      {t.meals.ingredientCount(preset.ingredients.length)}
                    </p>
                  </div>
                  <div className="flex shrink-0 gap-1">
                    <button
                      onClick={() => navigate(`/meals/${preset.id}/edit`)}
                      className="p-1.5 text-marble-dim hover:text-gold"
                      aria-label={t.meals.edit}
                    >
                      <IconEdit size={18} />
                    </button>
                    <button
                      onClick={() => setToDelete(preset)}
                      className="p-1.5 text-marble-dim hover:text-[var(--color-danger)]"
                      aria-label={t.meals.delete}
                    >
                      <IconTrash size={18} />
                    </button>
                  </div>
                </div>

                <div className="mt-2">
                  <NutrientLine
                    label={t.common.kcal}
                    value={preset.totalKcal}
                    target={profile.data?.targetKcal}
                    unit=""
                    color="var(--color-marble)"
                  />
                  <NutrientLine
                    label={t.common.macros.proteins}
                    value={preset.totalProteins}
                    target={profile.data?.targetProteins}
                    color="var(--color-purple-bright)"
                  />
                  <NutrientLine
                    label={t.common.macros.carbs}
                    value={preset.totalCarbs}
                    target={profile.data?.targetCarbs}
                    color="var(--color-gold)"
                  />
                  <NutrientLine
                    label={t.common.macros.fats}
                    value={preset.totalFats}
                    target={profile.data?.targetFats}
                    color="var(--color-pink)"
                  />
                  <NutrientLine
                    label={t.common.macros.fibers}
                    value={preset.totalFibers}
                    target={profile.data?.targetFibers}
                    color="var(--color-success)"
                  />
                </div>

                <Button
                  block
                  size="sm"
                  className="mt-3"
                  loading={consumingId === preset.id}
                  onClick={() => consume(preset)}
                >
                  {t.meals.consume}
                </Button>
              </Card>
            ))}
          </div>
        )}
      </div>

      <Modal
        open={!!toDelete}
        onClose={() => setToDelete(null)}
        title={t.meals.deleteTitle}
      >
        <p className="mb-5 text-sm text-marble-dim">
          {toDelete ? t.meals.deleteConfirm(toDelete.name) : ""}
        </p>
        <div className="flex gap-3">
          <Button block variant="ghost" onClick={() => setToDelete(null)}>
            {t.common.cancel}
          </Button>
          <Button
            block
            variant="danger"
            loading={deletePreset.isPending}
            onClick={() =>
              toDelete &&
              deletePreset.mutate(toDelete.id, {
                onSuccess: () => {
                  toast(t.meals.deleted, "success");
                  setToDelete(null);
                },
                onError: (e) =>
                  toast(errorMessage(e, t.meals.deleteError), "error"),
              })
            }
          >
            {t.common.delete}
          </Button>
        </div>
      </Modal>
    </div>
  );
}
