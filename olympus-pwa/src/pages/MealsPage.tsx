import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { PageHeader } from "@/components/AppLayout";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { EmptyState, Skeleton } from "@/components/ui/misc";
import { Modal } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";
import { errorMessage } from "@/lib/api/client";
import { IconEdit, IconPlus, IconSparkle, IconTrash } from "@/components/icons";
import { useAddLogEntry, useDeletePreset, usePresets } from "@/hooks/queries";
import { round, todayIso } from "@/lib/utils";
import type { MealPresetResponse } from "@/types/api";

// Liste des repas prédéfinis : consommer, éditer, supprimer, créer (manuel ou IA).
export default function MealsPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const presets = usePresets();
  const deletePreset = useDeletePreset();
  const addEntry = useAddLogEntry(todayIso());
  const [search, setSearch] = useState("");
  const [toDelete, setToDelete] = useState<MealPresetResponse | null>(null);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    const list = presets.data ?? [];
    return q ? list.filter((p) => p.name.toLowerCase().includes(q)) : list;
  }, [presets.data, search]);

  const consume = (preset: MealPresetResponse) => {
    addEntry.mutate(
      { targetDate: todayIso(), mealPresetId: preset.id },
      {
        onSuccess: () => toast(`${preset.name} consommé`, "success"),
        onError: (e) => toast(errorMessage(e, "Impossible de consommer"), "error"),
      },
    );
  };

  return (
    <div>
      <PageHeader
        overline="Mon arsenal"
        title="Repas"
        action={
          <Button size="sm" onClick={() => navigate("/meals/new")}>
            <IconPlus size={16} /> Nouveau
          </Button>
        }
      />

      <div className="px-5">
        <div className="mb-4 flex gap-2">
          <div className="flex-1">
            <Input placeholder="Rechercher un repas…" value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
          <Button variant="ghost" onClick={() => navigate("/meals/new?ai=1")}>
            <IconSparkle size={16} /> IA
          </Button>
        </div>

        {presets.isLoading ? (
          <div className="space-y-2">
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-24 w-full" />
          </div>
        ) : filtered.length === 0 ? (
          <EmptyState
            title="Aucun repas enregistré"
            hint="Crée ton premier repas pour le consommer en un geste."
          />
        ) : (
          <div className="space-y-3">
            {filtered.map((preset) => (
              <Card key={preset.id} tone="low" goldEdge>
                <div className="flex items-start justify-between">
                  <div className="min-w-0">
                    <h3 className="truncate text-lg text-marble">{preset.name}</h3>
                    <p className="text-xs text-marble-dim">
                      {round(preset.totalKcal)} kcal · {round(preset.totalProteins)}P /{" "}
                      {round(preset.totalCarbs)}G / {round(preset.totalFats)}L ·{" "}
                      {preset.ingredients.length} ingrédient{preset.ingredients.length > 1 ? "s" : ""}
                    </p>
                  </div>
                  <div className="flex shrink-0 gap-1">
                    <button
                      onClick={() => navigate(`/meals/${preset.id}/edit`)}
                      className="p-1.5 text-marble-dim hover:text-gold"
                      aria-label="Éditer"
                    >
                      <IconEdit size={18} />
                    </button>
                    <button
                      onClick={() => setToDelete(preset)}
                      className="p-1.5 text-marble-dim hover:text-[var(--color-danger)]"
                      aria-label="Supprimer"
                    >
                      <IconTrash size={18} />
                    </button>
                  </div>
                </div>
                <Button block size="sm" className="mt-3" loading={addEntry.isPending} onClick={() => consume(preset)}>
                  Consommer
                </Button>
              </Card>
            ))}
          </div>
        )}
      </div>

      <Modal open={!!toDelete} onClose={() => setToDelete(null)} title="Supprimer ce repas ?">
        <p className="mb-5 text-sm text-marble-dim">
          « {toDelete?.name} » sera définitivement retiré de ton arsenal.
        </p>
        <div className="flex gap-3">
          <Button block variant="ghost" onClick={() => setToDelete(null)}>
            Annuler
          </Button>
          <Button
            block
            variant="danger"
            loading={deletePreset.isPending}
            onClick={() =>
              toDelete &&
              deletePreset.mutate(toDelete.id, {
                onSuccess: () => {
                  toast("Repas supprimé", "success");
                  setToDelete(null);
                },
                onError: (e) => toast(errorMessage(e, "Suppression impossible"), "error"),
              })
            }
          >
            Supprimer
          </Button>
        </div>
      </Modal>
    </div>
  );
}
