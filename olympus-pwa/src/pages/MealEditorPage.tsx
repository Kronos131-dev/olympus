import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { mealPresetApi } from "@/lib/api/endpoints";
import { errorMessage } from "@/lib/api/client";
import { useSavePreset } from "@/hooks/queries";
import { FoodFinder } from "@/components/FoodFinder";
import { QuantitySheet } from "@/components/QuantitySheet";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { EmptyState, Spinner } from "@/components/ui/misc";
import { useToast } from "@/components/ui/Toast";
import { IconBack, IconTrash } from "@/components/icons";
import { macrosFor, round } from "@/lib/utils";
import type { Unit } from "@/lib/units";
import { useT } from "@/lib/i18n";
import type { FoodItemResponse } from "@/types/api";

interface DraftIngredient {
  foodItem: FoodItemResponse;
  quantityGrams: number;
  unit?: Unit;
  amount?: number;
}

export default function MealEditorPage() {
  const { id } = useParams();
  const editId = id ? Number(id) : undefined;
  const t = useT();
  const navigate = useNavigate();
  const toast = useToast();
  const qc = useQueryClient();
  const save = useSavePreset();

  const [name, setName] = useState("");
  const [ingredients, setIngredients] = useState<DraftIngredient[]>([]);
  const [loading, setLoading] = useState(!!editId);
  const [pending, setPending] = useState<FoodItemResponse | null>(null);
  const [editIndex, setEditIndex] = useState<number | null>(null);

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
      .catch(() => toast(t.mealEditor.notFound, "error"))
      .finally(() => setLoading(false));
  }, [editId, toast, t]);

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

  const closeSheet = () => {
    setPending(null);
    setEditIndex(null);
  };

  const confirmSheet = (result: { quantityGrams: number; unit: Unit; amount: number }) => {
    if (editIndex != null) {
      setIngredients((list) =>
        list.map((ing, j) => (j === editIndex ? { ...ing, ...result } : ing)),
      );
    } else if (pending) {
      setIngredients((list) => [...list, { foodItem: pending, ...result }]);
    }
    closeSheet();
  };

  const submit = () => {
    if (!name.trim()) return toast(t.mealEditor.errName, "error");
    if (ingredients.length === 0) return toast(t.mealEditor.errIngredient, "error");
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
          toast(editId ? t.mealEditor.updated : t.mealEditor.created, "success");
          qc.invalidateQueries({ queryKey: ["presets"] });
          navigate("/meals");
        },
        onError: (e) => toast(errorMessage(e, t.mealEditor.saveError), "error"),
      },
    );
  };

  if (loading) return <div className="grid min-h-dvh place-items-center"><Spinner className="size-8" /></div>;

  return (
    <div className="page-enter mx-auto max-w-lg px-5 pb-32">
      <header className="flex items-center gap-3 py-5">
        <button onClick={() => navigate(-1)} className="text-marble-dim hover:text-marble" aria-label={t.common.back}>
          <IconBack size={24} />
        </button>
        <h1 className="text-2xl text-marble">{editId ? t.mealEditor.editTitle : t.mealEditor.newTitle}</h1>
      </header>

      <Input label={t.mealEditor.nameLabel} value={name} onChange={(e) => setName(e.target.value)} placeholder={t.mealEditor.namePlaceholder} />

      <section className="mt-6">
        <div className="mb-3 flex items-baseline justify-between">
          <h2 className="text-sm font-semibold text-marble">{t.mealEditor.ingredients}</h2>
          <span className="text-xs text-gold">
            {t.mealEditor.macroLine(round(totals.kcal), round(totals.p), round(totals.c), round(totals.f))}
          </span>
        </div>
        {ingredients.length === 0 ? (
          <EmptyState title={t.mealEditor.emptyTitle} hint={t.mealEditor.emptyHint} />
        ) : (
          <ul className="space-y-1">
            {ingredients.map((i, idx) => {
              const m = macrosFor(i.foodItem, i.quantityGrams);
              return (
                <li key={idx} className="flex items-center justify-between rounded-[var(--radius)] bg-surface-low px-4 py-3">
                  <button
                    onClick={() => setEditIndex(idx)}
                    className="min-w-0 flex-1 text-left"
                  >
                    <p className="truncate text-sm text-marble">{i.foodItem.name}</p>
                    <p className="text-xs text-marble-dim">
                      {round(i.quantityGrams)} g · {round(m.kcal)} {t.common.kcal}
                    </p>
                  </button>
                  <button
                    onClick={() => setIngredients((list) => list.filter((_, j) => j !== idx))}
                    className="ml-2 text-marble-dim hover:text-[var(--color-danger)]"
                    aria-label={t.mealEditor.remove}
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
        <FoodFinder onPick={(f) => setPending(f)} />
      </section>

      <div className="fixed inset-x-0 bottom-0 z-30 mx-auto max-w-lg p-4" style={{ paddingBottom: "calc(1rem + env(safe-area-inset-bottom))" }}>
        <Button block size="lg" loading={save.isPending} onClick={submit} className="shadow-[0px_24px_48px_rgba(0,0,0,0.5)]">
          {editId ? t.mealEditor.save : t.mealEditor.create}
        </Button>
      </div>

      <QuantitySheet
        open={!!pending || editIndex != null}
        onClose={closeSheet}
        food={editIndex != null ? ingredients[editIndex].foodItem : pending}
        initialGrams={
          editIndex != null
            ? ingredients[editIndex].quantityGrams
            : (pending?.estimatedWeightGrams ?? 100)
        }
        initialUnit={editIndex != null ? ingredients[editIndex].unit : undefined}
        initialAmount={editIndex != null ? ingredients[editIndex].amount : undefined}
        confirmLabel={editIndex != null ? t.common.save : t.mealEditor.addIngredient}
        onConfirm={confirmSheet}
      />
    </div>
  );
}
