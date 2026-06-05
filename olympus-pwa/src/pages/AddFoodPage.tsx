import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { FoodFinder } from "@/components/FoodFinder";
import { Modal } from "@/components/ui/Modal";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { useToast } from "@/components/ui/Toast";
import { errorMessage } from "@/lib/api/client";
import { IconBack } from "@/components/icons";
import { useAddLogEntry } from "@/hooks/queries";
import { macrosFor, round, todayIso } from "@/lib/utils";
import { useT } from "@/lib/i18n";
import type { FoodItemResponse } from "@/types/api";

// Recherche/scan/manuel/IA puis ajout au journal du jour avec quantité.
export default function AddFoodPage() {
  const t = useT();
  const navigate = useNavigate();
  const toast = useToast();
  const today = todayIso();
  const addEntry = useAddLogEntry(today);
  const [picked, setPicked] = useState<FoodItemResponse | null>(null);
  const [grams, setGrams] = useState("100");

  const onPick = (food: FoodItemResponse) => {
    setPicked(food);
    setGrams(food.estimatedWeightGrams ? String(round(food.estimatedWeightGrams)) : "100");
  };

  const confirm = () => {
    if (!picked) return;
    addEntry.mutate(
      { targetDate: today, foodItemId: picked.id, quantityGrams: Number(grams) || 0 },
      {
        onSuccess: () => {
          toast(t.food.added(picked.name), "success");
          navigate("/");
        },
        onError: (e) => toast(errorMessage(e, t.food.addError), "error"),
      },
    );
  };

  const preview = picked ? macrosFor(picked, Number(grams) || 0) : null;

  return (
    <div className="page-enter mx-auto max-w-lg px-5 pb-10">
      <header className="flex items-center gap-3 py-5">
        <button onClick={() => navigate(-1)} className="text-marble-dim hover:text-marble" aria-label={t.common.back}>
          <IconBack size={24} />
        </button>
        <h1 className="text-2xl text-marble">{t.food.addTitle}</h1>
      </header>

      <FoodFinder onPick={onPick} />

      <Modal open={!!picked} onClose={() => setPicked(null)} title={picked?.name}>
        <div className="space-y-4">
          <Input
            label={t.common.quantityG}
            type="number"
            inputMode="decimal"
            value={grams}
            onChange={(e) => setGrams(e.target.value)}
            autoFocus
          />
          {preview && (
            <div className="grid grid-cols-2 gap-2 rounded-[var(--radius)] bg-surface-lowest p-3 text-center sm:grid-cols-4">
              {[
                [t.common.kcal, round(preview.kcal)],
                [t.common.macrosShort.proteins, round(preview.proteins)],
                [t.common.macrosShort.carbs, round(preview.carbs)],
                [t.common.macrosShort.fats, round(preview.fats)],
              ].map(([k, v]) => (
                <div key={k}>
                  <p className="text-lg font-bold text-marble">{v}</p>
                  <p className="text-[0.6rem] font-medium text-marble-dim">{k}</p>
                </div>
              ))}
            </div>
          )}
          <Button block loading={addEntry.isPending} onClick={confirm}>
            {t.common.consume}
          </Button>
        </div>
      </Modal>
    </div>
  );
}
