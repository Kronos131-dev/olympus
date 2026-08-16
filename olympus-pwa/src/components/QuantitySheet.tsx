import { useEffect, useState } from "react";
import { Modal } from "@/components/ui/Modal";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { Chip } from "@/components/ui/misc";
import { macrosFor, round } from "@/lib/utils";
import { availableUnits, fromGrams, toGrams, type Unit } from "@/lib/units";
import { useT } from "@/lib/i18n";
import type { FoodItemResponse } from "@/types/api";

export interface QuantityResult {
  quantityGrams: number;
  unit: Unit;
  amount: number;
}

interface Props {
  open: boolean;
  onClose: () => void;
  food: FoodItemResponse | null;
  initialGrams: number;
  initialUnit?: Unit | null;
  initialAmount?: number | null;
  confirmLabel: string;
  loading?: boolean;
  onConfirm: (result: QuantityResult) => void;
}

const SHORTCUTS: Record<Unit, number[]> = {
  g: [30, 60, 100, 150],
  tsp: [1, 2, 3],
  tbsp: [1, 2, 3],
  piece: [1, 2, 3],
};

export function QuantitySheet({
  open,
  onClose,
  food,
  initialGrams,
  initialUnit,
  initialAmount,
  confirmLabel,
  loading,
  onConfirm,
}: Props) {
  const t = useT();
  const [unit, setUnit] = useState<Unit>("g");
  const [amountStr, setAmountStr] = useState("0");

  useEffect(() => {
    if (!open || !food) return;
    const opts = availableUnits(food);
    const startUnit = initialUnit && opts.some((o) => o.unit === initialUnit) ? initialUnit : "g";
    const startOpt = opts.find((o) => o.unit === startUnit) ?? opts[0];
    const startAmount = initialAmount ?? fromGrams(initialGrams, startOpt.gramsPerUnit);
    setUnit(startUnit);
    setAmountStr(String(round(startAmount, startUnit === "g" ? 0 : 1)));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, food?.id]);

  if (!food) return null;

  const units = availableUnits(food);
  const opt = units.find((o) => o.unit === unit) ?? units[0];
  const amount = Number(amountStr) || 0;
  const quantityGrams = toGrams(amount, opt.gramsPerUnit);
  const preview = macrosFor(food, quantityGrams);

  const changeUnit = (next: Unit) => {
    const nextOpt = units.find((o) => o.unit === next);
    if (!nextOpt) return;
    setAmountStr(String(round(fromGrams(quantityGrams, nextOpt.gramsPerUnit), next === "g" ? 0 : 1)));
    setUnit(next);
  };

  return (
    <Modal open={open} onClose={onClose} title={food.name}>
      <div className="space-y-4">
        <p className="text-xs text-marble-dim">{t.food.finder.per100(round(food.kcal100g))}</p>

        <Input
          type="number"
          inputMode="decimal"
          value={amountStr}
          onChange={(e) => setAmountStr(e.target.value)}
          autoFocus
          className="text-center text-2xl font-bold"
        />

        {units.length > 1 && (
          <div className="flex flex-wrap gap-2">
            {units.map((o) => (
              <Chip key={o.unit} active={o.unit === unit} onClick={() => changeUnit(o.unit)}>
                {t.common.units[o.unit]}
              </Chip>
            ))}
          </div>
        )}

        {unit !== "g" && <p className="text-xs text-marble-dim">{t.common.approxGrams(round(quantityGrams))}</p>}

        <div className="flex flex-wrap gap-2">
          {SHORTCUTS[unit].map((v) => (
            <Chip key={v} onClick={() => setAmountStr(String(v))}>
              {v}
            </Chip>
          ))}
        </div>

        <div className="grid grid-cols-4 gap-2 rounded-[var(--radius)] bg-surface-lowest p-3 text-center">
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

        <Button block loading={loading} onClick={() => onConfirm({ quantityGrams, unit, amount })}>
          {confirmLabel}
        </Button>
      </div>
    </Modal>
  );
}
