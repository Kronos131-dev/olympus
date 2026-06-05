import { useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { PageHeader } from "@/components/AppLayout";
import { SectionTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { ProgressRing } from "@/components/ui/ProgressRing";
import { MacroBar } from "@/components/ui/MacroBar";
import { EmptyState, Skeleton } from "@/components/ui/misc";
import { useToast } from "@/components/ui/Toast";
import { errorMessage } from "@/lib/api/client";
import { IconPlus, IconTrash } from "@/components/icons";
import { useDailyLog, useDeleteLogEntry, useProfile } from "@/hooks/queries";
import { formatDateLong, macrosFor, round, todayIso } from "@/lib/utils";
import { localeFor, useLang, useT, type Dict } from "@/lib/i18n";
import type { LogEntryResponse } from "@/types/api";

function entryMacros(e: LogEntryResponse, t: Dict) {
  if (e.mealPreset) {
    return {
      name: e.mealPreset.name,
      kcal: e.mealPreset.totalKcal,
      detail: t.dashboard.addMeal,
    };
  }
  if (e.foodItem) {
    const m = macrosFor(e.foodItem, e.quantityGrams);
    return { name: e.foodItem.name, kcal: m.kcal, detail: `${round(e.quantityGrams)} g` };
  }
  return { name: "—", kcal: 0, detail: "" };
}

export default function DashboardPage() {
  const t = useT();
  const { lang } = useLang();
  const today = todayIso();
  const navigate = useNavigate();
  const toast = useToast();
  const profile = useProfile();
  const log = useDailyLog(today);
  const deleteEntry = useDeleteLogEntry(today);

  const targetKcal = profile.data?.targetKcal ?? 2000;
  const effectiveTarget = round(targetKcal);
  const consumed = round(log.data?.totalKcal ?? 0);
  const remaining = effectiveTarget - consumed;

  const sortedEntries = useMemo(
    () => [...(log.data?.entries ?? [])].reverse(),
    [log.data?.entries],
  );

  return (
    <div>
      <PageHeader overline={formatDateLong(today, localeFor(lang))} title={t.dashboard.title} />

      <div className="px-5">
        {log.isLoading || profile.isLoading ? (
          <Skeleton className="mx-auto h-56 w-56 rounded-full" />
        ) : (
          <div className="flex flex-col items-center">
            <ProgressRing value={consumed} max={effectiveTarget || 1}>
              <div>
                <p
                  className="font-extrabold leading-none tracking-tight text-marble"
                  style={{ fontSize: "clamp(2.5rem, 14vw, 3.75rem)" }}
                >
                  {consumed}
                </p>
                <p className="text-xs font-medium text-marble-dim">
                  / {effectiveTarget} {t.common.kcal}
                </p>
                <p
                  className={`mt-1 text-xs ${remaining >= 0 ? "text-gold" : "text-[var(--color-danger)]"}`}
                >
                  {remaining >= 0 ? t.dashboard.remaining(remaining) : t.dashboard.over(-remaining)}
                </p>
              </div>
            </ProgressRing>
          </div>
        )}

        <div className="mt-6 grid grid-cols-3 gap-2">
          <MacroBar label={t.common.macros.proteins} value={log.data?.totalProteins ?? 0} target={profile.data?.targetProteins} color="var(--color-purple-bright)" />
          <MacroBar label={t.common.macros.carbs} value={log.data?.totalCarbs ?? 0} target={profile.data?.targetCarbs} color="var(--color-gold)" />
          <MacroBar label={t.common.macros.fats} value={log.data?.totalFats ?? 0} target={profile.data?.targetFats} color="var(--color-pink)" />
        </div>

        <div className="mt-5 flex gap-3">
          <Button block onClick={() => navigate("/food/add")}>
            <IconPlus size={16} /> {t.dashboard.addFood}
          </Button>
          <Button block variant="ghost" onClick={() => navigate("/meals")}>
            <IconPlus size={16} /> {t.dashboard.addMeal}
          </Button>
        </div>

        <section className="mt-6">
          <SectionTitle>{t.dashboard.recent}</SectionTitle>
          {log.isLoading ? (
            <div className="space-y-1">
              <Skeleton className="h-14 w-full" />
              <Skeleton className="h-14 w-full" />
            </div>
          ) : sortedEntries.length === 0 ? (
            <EmptyState title={t.dashboard.emptyTitle} hint={t.dashboard.emptyHint} />
          ) : (
            <ul className="space-y-1">
              {sortedEntries.map((e) => {
                const m = entryMacros(e, t);
                return (
                  <li
                    key={e.id}
                    className="flex items-center justify-between rounded-[var(--radius)] bg-surface-low px-4 py-3"
                  >
                    <div className="min-w-0">
                      <p className="truncate text-sm text-marble">{m.name}</p>
                      <p className="text-xs text-marble-dim">
                        {m.detail} · {round(m.kcal)} {t.common.kcal}
                        {e.fromPlan && <span className="text-gold/70"> · plan</span>}
                      </p>
                    </div>
                    <button
                      onClick={() =>
                        deleteEntry.mutate(e.id, {
                          onError: (e) => toast(errorMessage(e, t.dashboard.deleteError), "error"),
                        })
                      }
                      className="text-marble-dim transition-colors hover:text-[var(--color-danger)]"
                      aria-label={t.common.delete}
                    >
                      <IconTrash size={18} />
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </section>
      </div>
    </div>
  );
}
