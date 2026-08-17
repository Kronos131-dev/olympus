import { useMemo } from "react";
import { PageHeader } from "@/components/AppLayout";
import { NutrientGauge } from "@/components/ui/NutrientGauge";
import { EmptyState, Skeleton } from "@/components/ui/misc";
import { useMicronutrients } from "@/hooks/queries";
import { NUTRIENT_CATEGORIES } from "@/lib/nutrients";
import { formatDateLong, todayIso } from "@/lib/utils";
import { localeFor, useLang, useT } from "@/lib/i18n";
import type { MicronutrientResponse, NutrientCategory } from "@/types/api";

export default function MicronutrientsPage() {
  const t = useT();
  const { lang } = useLang();
  const today = todayIso();
  const micros = useMicronutrients(today);

  const byCategory = useMemo(() => {
    const groups = new Map<NutrientCategory, MicronutrientResponse[]>();
    for (const nutrient of micros.data?.nutrients ?? []) {
      const list = groups.get(nutrient.category) ?? [];
      list.push(nutrient);
      groups.set(nutrient.category, list);
    }
    return groups;
  }, [micros.data]);

  const coverage = Math.round((micros.data?.overallCoverage ?? 0) * 100);
  const hasData = (micros.data?.nutrients ?? []).some(
    (nutrient) => nutrient.consumed > 0,
  );

  return (
    <div>
      <PageHeader
        overline={formatDateLong(today, localeFor(lang))}
        title={t.micros.title}
      />

      <div className="px-5">
        {micros.isLoading ? (
          <div className="space-y-2">
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-40 w-full" />
            <Skeleton className="h-40 w-full" />
          </div>
        ) : !hasData ? (
          <EmptyState title={t.micros.emptyTitle} hint={t.micros.emptyHint} />
        ) : (
          <>
            {/* Sans ce chiffre, une journée où l'on a scanné un produit industriel afficherait
                des carences qui n'existent pas : les micros n'y sont tout simplement pas connus. */}
            <section className="rounded-[var(--radius)] bg-surface-low px-4 py-3">
              <p className="text-sm font-semibold text-marble">
                {t.micros.coverage(coverage)}
              </p>
              <p className="mt-1 text-xs text-marble-dim/80">
                {t.micros.coverageHint}
              </p>
            </section>

            {NUTRIENT_CATEGORIES.map((category) => {
              const nutrients = byCategory.get(category) ?? [];
              if (nutrients.length === 0) return null;
              return (
                <section key={category} className="mt-7">
                  <h2 className="mb-1 text-xs font-semibold uppercase tracking-wide text-gold">
                    {t.micros.categories[category]}
                  </h2>
                  <div className="divide-y divide-outline/15">
                    {nutrients.map((nutrient) => (
                      <NutrientGauge
                        key={nutrient.nutrient}
                        label={t.nutrients[nutrient.nutrient]}
                        value={nutrient.consumed}
                        reference={nutrient.reference}
                        unit={nutrient.unit}
                        coverage={nutrient.coverage}
                        coverageLabel={t.micros.partial}
                      />
                    ))}
                  </div>
                </section>
              );
            })}

            <p className="mt-7 text-xs text-marble-dim/70">
              {t.micros.vitaminDNote}
            </p>
          </>
        )}
      </div>
    </div>
  );
}
