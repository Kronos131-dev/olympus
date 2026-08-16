import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { FoodFinder } from "@/components/FoodFinder";
import { QuantitySheet } from "@/components/QuantitySheet";
import { useToast } from "@/components/ui/Toast";
import { errorMessage } from "@/lib/api/client";
import { IconBack } from "@/components/icons";
import { useAddLogEntry } from "@/hooks/queries";
import { todayIso } from "@/lib/utils";
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

  const confirm = (result: { quantityGrams: number }) => {
    if (!picked) return;
    addEntry.mutate(
      { targetDate: today, foodItemId: picked.id, quantityGrams: result.quantityGrams },
      {
        onSuccess: () => {
          toast(t.food.added(picked.name), "success");
          navigate("/");
        },
        onError: (e) => toast(errorMessage(e, t.food.addError), "error"),
      },
    );
  };

  return (
    <div className="page-enter mx-auto max-w-lg px-5 pb-10">
      <header className="flex items-center gap-3 py-5">
        <button onClick={() => navigate(-1)} className="text-marble-dim hover:text-marble" aria-label={t.common.back}>
          <IconBack size={24} />
        </button>
        <h1 className="text-2xl text-marble">{t.food.addTitle}</h1>
      </header>

      <FoodFinder onPick={setPicked} />

      <QuantitySheet
        open={!!picked}
        onClose={() => setPicked(null)}
        food={picked}
        initialGrams={picked?.estimatedWeightGrams ?? 100}
        confirmLabel={t.common.consume}
        loading={addEntry.isPending}
        onConfirm={confirm}
      />
    </div>
  );
}
