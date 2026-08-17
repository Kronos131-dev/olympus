import { useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Modal } from "@/components/ui/Modal";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { Chip } from "@/components/ui/misc";
import { NutrientGauge } from "@/components/ui/NutrientGauge";
import { foodItemApi } from "@/lib/api/endpoints";
import { useProfile } from "@/hooks/queries";
import { NUTRIENT_ORDER, referenceFor, unitOf } from "@/lib/nutrients";
import { macrosFor, round } from "@/lib/utils";
import { availableUnits, fromGrams, toGrams, type Unit } from "@/lib/units";
import { useT } from "@/lib/i18n";
import type { FoodItemResponse, Nutrient } from "@/types/api";

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
  const profile = useProfile();
  const [unit, setUnit] = useState<Unit>("g");
  const [amountStr, setAmountStr] = useState("0");
  const [showMicros, setShowMicros] = useState(false);

  const detail = useQuery({
    queryKey: ["foodItemDetail", food?.id],
    queryFn: () => foodItemApi.detail(food!.id),
    enabled: open && !!food?.id,
  });

  useEffect(() => {
    if (!open || !food) return;
    const opts = availableUnits(food);
    const startUnit =
      initialUnit && opts.some((o) => o.unit === initialUnit)
        ? initialUnit
        : "g";
    const startOpt = opts.find((o) => o.unit === startUnit) ?? opts[0];
    const startAmount =
      initialAmount ?? fromGrams(initialGrams, startOpt.gramsPerUnit);
    setUnit(startUnit);
    setAmountStr(String(round(startAmount, startUnit === "g" ? 0 : 1)));
    setShowMicros(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, food?.id]);

  const micros = useMemo(
    () => NUTRIENT_ORDER.filter((n) => (detail.data?.micros100g[n] ?? 0) > 0),
    [detail.data],
  );

  if (!food) return null;

  const units = availableUnits(food);
  const opt = units.find((o) => o.unit === unit) ?? units[0];
  const amount = Number(amountStr) || 0;
  const quantityGrams = toGrams(amount, opt.gramsPerUnit);
  const ratio = quantityGrams / 100;
  const preview = macrosFor(
    { ...food, fibers100g: detail.data?.fibers100g },
    quantityGrams,
  );

  const changeUnit = (next: Unit) => {
    const nextOpt = units.find((o) => o.unit === next);
    if (!nextOpt) return;
    setAmountStr(
      String(
        round(
          fromGrams(quantityGrams, nextOpt.gramsPerUnit),
          next === "g" ? 0 : 1,
        ),
      ),
    );
    setUnit(next);
  };

  return (
    <Modal open={open} onClose={onClose} title={food.name}>
      <div className="space-y-4">
        <p className="text-xs text-marble-dim">
          {t.food.finder.per100(round(food.kcal100g))}
        </p>

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
              <Chip
                key={o.unit}
                active={o.unit === unit}
                onClick={() => changeUnit(o.unit)}
              >
                {t.common.units[o.unit]}
              </Chip>
            ))}
          </div>
        )}

        {unit !== "g" && (
          <p className="text-xs text-marble-dim">
            {t.common.approxGrams(round(quantityGrams))}
          </p>
        )}

        <div className="flex flex-wrap gap-2">
          {SHORTCUTS[unit].map((v) => (
            <Chip key={v} onClick={() => setAmountStr(String(v))}>
              {v}
            </Chip>
          ))}
        </div>

        <div className="grid grid-cols-5 gap-1 rounded-[var(--radius)] bg-surface-lowest p-3 text-center">
          {[
            [t.common.kcal, round(preview.kcal)],
            [t.common.macrosShort.proteins, round(preview.proteins)],
            [t.common.macrosShort.carbs, round(preview.carbs)],
            [t.common.macrosShort.fats, round(preview.fats)],
            [t.common.macrosShort.fibers, round(preview.fibers)],
          ].map(([k, v]) => (
            <div key={k}>
              <p className="text-base font-bold text-marble">{v}</p>
              <p className="text-[0.6rem] font-medium text-marble-dim">{k}</p>
            </div>
          ))}
        </div>

        {micros.length > 0 && (
          <div>
            <button
              onClick={() => setShowMicros((o) => !o)}
              className="w-full rounded-[var(--radius)] bg-surface-low px-4 py-2.5 text-left text-xs font-semibold text-marble"
            >
              {showMicros ? t.food.hideMicros : t.food.showMicros}
            </button>
            {showMicros && (
              <div className="mt-2 px-1">
                {micros.map((nutrient) => (
                  <NutrientGauge
                    key={nutrient}
                    label={t.nutrients[nutrient]}
                    value={(detail.data?.micros100g[nutrient] ?? 0) * ratio}
                    reference={referenceFor(
                      nutrient as Nutrient,
                      profile.data?.gender,
                    )}
                    unit={unitOf(nutrient as Nutrient)}
                  />
                ))}
              </div>
            )}
          </div>
        )}

        <Button
          block
          loading={loading}
          onClick={() => onConfirm({ quantityGrams, unit, amount })}
        >
          {confirmLabel}
        </Button>
      </div>
    </Modal>
  );
}
