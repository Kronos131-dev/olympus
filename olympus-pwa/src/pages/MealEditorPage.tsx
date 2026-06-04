import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { mealPresetApi } from "@/lib/api/endpoints";
import { useSavePreset } from "@/hooks/queries";
import { FoodFinder } from "@/components/FoodFinder";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { EmptyState, Spinner } from "@/components/ui/misc";
import { useToast } from "@/components/ui/Toast";
import { IconBack, IconTrash } from "@/components/icons";
import { macrosFor, round } from "@/lib/utils";
import type { FoodItemResponse } from "@/types/api";

interface DraftIngredient {
  foodItem: FoodItemResponse;
  quantityGrams: number;
}

export default function MealEditorPage() {
  const { id } = useParams();
  const editId = id ? Number(id) : undefined;
  const navigate = useNavigate();
  const toast = useToast();
  const qc = useQueryClient();
  const save = useSavePreset();

  const [name, setName] = useState("");
  const [ingredients, setIngredients] = useState<DraftIngredient[]>([]);
  const [loading, setLoading] = useState(!!editId);
  const [pending, setPending] = useState<FoodItemResponse | null>(null);
  const [pendingGrams, setPendingGrams] = useState("100");

  // Charge le preset existant en mode édition.
  useEffect(() => {
    if (!editId) return;
    mealPresetApi
      .get(editId)
      .then((p) => {
        setName(p.name);
        setIngredients(
          p.ingredients.map((i) => ({ foodItem: i.foodItem, quantityGrams: i.quantityGrams })),
        );
      })
      .catch(() => toast("Repas introuvable", "error"))
      .finally(() => setLoading(false));
  }, [editId, toast]);

  const totals = ingredients.reduce(
    (acc, i) => {
      const m = macrosFor(i.foodItem, i.quantityGrams);
      acc.kcal += m.kcal;
      acc.p += m.proteins;
      acc.c += m.carbs;
      acc.f += m.fats;
      return acc;
    },
    { kcal: 0, p: 0, c: 0, f: 0 },
  );

  const addPending = () => {
    if (!pending) return;
    setIngredients((list) => [...list, { foodItem: pending, quantityGrams: Number(pendingGrams) || 0 }]);
    setPending(null);
    setPendingGrams("100");
  };

  const submit = () => {
    if (!name.trim()) return toast("Donne un nom au repas", "error");
    if (ingredients.length === 0) return toast("Ajoute au moins un ingrédient", "error");
    save.mutate(
      {
        id: editId,
        body: {
          name: name.trim(),
          ingredients: ingredients.map((i) => ({
            foodItemId: i.foodItem.id,
            quantityGrams: i.quantityGrams,
          })),
        },
      },
      {
        onSuccess: () => {
          toast(editId ? "Repas mis à jour" : "Repas créé", "success");
          qc.invalidateQueries({ queryKey: ["presets"] });
          navigate("/meals");
        },
        onError: () => toast("Échec de l'enregistrement", "error"),
      },
    );
  };

  if (loading) return <div className="grid min-h-dvh place-items-center"><Spinner className="size-8" /></div>;

  return (
    <div className="page-enter mx-auto max-w-lg px-5 pb-32">
      <header className="flex items-center gap-3 py-5">
        <button onClick={() => navigate(-1)} className="text-marble-dim hover:text-marble" aria-label="Retour">
          <IconBack size={24} />
        </button>
        <h1 className="text-2xl text-marble">{editId ? "Modifier le repas" : "Nouveau repas"}</h1>
      </header>

      <Input label="Nom du repas" value={name} onChange={(e) => setName(e.target.value)} placeholder="ex. Bol protéiné" />

      <section className="mt-6">
        <div className="mb-3 flex items-baseline justify-between">
          <h2 className="text-sm font-semibold text-marble">Ingrédients</h2>
          <span className="text-xs text-gold">
            {round(totals.kcal)} kcal · {round(totals.p)}P / {round(totals.c)}G / {round(totals.f)}L
          </span>
        </div>
        {ingredients.length === 0 ? (
          <EmptyState title="Aucun ingrédient" hint="Recherche, scanne ou crée un aliment ci-dessous." />
        ) : (
          <ul className="space-y-1">
            {ingredients.map((i, idx) => {
              const m = macrosFor(i.foodItem, i.quantityGrams);
              return (
                <li key={idx} className="flex items-center justify-between rounded-[var(--radius)] bg-surface-low px-4 py-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm text-marble">{i.foodItem.name}</p>
                    <p className="text-xs text-marble-dim">
                      {round(i.quantityGrams)} g · {round(m.kcal)} kcal
                    </p>
                  </div>
                  <button
                    onClick={() => setIngredients((list) => list.filter((_, j) => j !== idx))}
                    className="text-marble-dim hover:text-[var(--color-danger)]"
                    aria-label="Retirer"
                  >
                    <IconTrash size={18} />
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </section>

      <section className="mt-6">
        <FoodFinder onPick={(f) => { setPending(f); setPendingGrams(f.estimatedWeightGrams ? String(round(f.estimatedWeightGrams)) : "100"); }} />
      </section>

      <div className="fixed inset-x-0 bottom-0 z-30 mx-auto max-w-lg p-4" style={{ paddingBottom: "calc(1rem + env(safe-area-inset-bottom))" }}>
        <Button block size="lg" loading={save.isPending} onClick={submit} className="shadow-[0px_24px_48px_rgba(0,0,0,0.5)]">
          {editId ? "Enregistrer" : "Créer le repas"}
        </Button>
      </div>

      <Modal open={!!pending} onClose={() => setPending(null)} title={pending?.name}>
        <div className="space-y-4">
          <Input
            label="Quantité (g)"
            type="number"
            inputMode="decimal"
            value={pendingGrams}
            onChange={(e) => setPendingGrams(e.target.value)}
            autoFocus
          />
          <Button block onClick={addPending}>
            Ajouter l'ingrédient
          </Button>
        </div>
      </Modal>
    </div>
  );
}
