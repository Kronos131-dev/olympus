import { ProgressRing } from "./ProgressRing";
import { MacroRing } from "./MacroRing";
import { round } from "@/lib/utils";
import { useT } from "@/lib/i18n";

export interface NutritionTotals {
  kcal: number;
  proteins: number;
  carbs: number;
  fats: number;
  fibers: number;
}

export interface NutritionTargets {
  targetKcal?: number | null;
  targetProteins?: number | null;
  targetCarbs?: number | null;
  targetFats?: number | null;
  targetFibers?: number | null;
}

interface Props {
  totals: NutritionTotals;
  targets: NutritionTargets;
  /** Ligne optionnelle sous la cible calorique — « 458 restantes » sur l'accueil. */
  caption?: React.ReactNode;
}

// Grande roue calories + rangée de 4 petites roues macros (protéines / glucides / lipides /
// fibres). Extrait de DashboardPage et MealAnalysisPage, qui dupliquaient exactement ce JSX —
// et où seul l'accueil affichait la roue fibres.
export function NutritionRings({ totals, targets, caption }: Props) {
  const t = useT();
  const consumed = round(totals.kcal);
  const max =
    targets.targetKcal && targets.targetKcal > 0
      ? round(targets.targetKcal)
      : consumed || 1;

  return (
    <div>
      <div className="flex flex-col items-center">
        <ProgressRing value={consumed} max={max}>
          <div>
            <p
              className="font-extrabold leading-none tracking-tight text-marble"
              style={{ fontSize: "clamp(2.5rem, 14vw, 3.75rem)" }}
            >
              {consumed}
            </p>
            <p className="text-xs font-medium text-marble-dim">
              / {max} {t.common.kcal}
            </p>
            {caption}
          </div>
        </ProgressRing>
      </div>

      <div className="mt-6 grid grid-cols-4 gap-1">
        <MacroRing
          label={t.common.macros.proteins}
          value={totals.proteins}
          target={targets.targetProteins}
          color="var(--color-purple-bright)"
        />
        <MacroRing
          label={t.common.macros.carbs}
          value={totals.carbs}
          target={targets.targetCarbs}
          color="var(--color-gold)"
        />
        <MacroRing
          label={t.common.macros.fats}
          value={totals.fats}
          target={targets.targetFats}
          color="var(--color-pink)"
        />
        <MacroRing
          label={t.common.macros.fibers}
          value={totals.fibers}
          target={targets.targetFibers}
          color="var(--color-success)"
        />
      </div>
    </div>
  );
}
