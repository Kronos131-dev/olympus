import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { mealAnalysisApi } from "@/lib/api/endpoints";
import { errorMessage } from "@/lib/api/client";
import { compressImage } from "@/lib/image";
import { useProfile } from "@/hooks/queries";
import { useSpeech } from "@/hooks/useSpeech";
import { useToast } from "@/components/ui/Toast";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { NutritionRings } from "@/components/ui/NutritionRings";
import { NutrientGauge } from "@/components/ui/NutrientGauge";
import { Spinner } from "@/components/ui/misc";
import {
  IconBack,
  IconCamera,
  IconImage,
  IconMic,
  IconSearch,
  IconTrash,
} from "@/components/icons";
import { NUTRIENT_ORDER, referenceFor, unitOf } from "@/lib/nutrients";
import { round, todayIso } from "@/lib/utils";
import { localeFor, useLang, useT } from "@/lib/i18n";
import { cn } from "@/lib/utils";
import type {
  AnalyzedFoodResponse,
  FoodSource,
  MealAnalysisResponse,
  Nutrient,
} from "@/types/api";

type Stage = "input" | "loading" | "result";

export default function MealAnalysisPage() {
  const t = useT();
  const { lang } = useLang();
  const navigate = useNavigate();
  const toast = useToast();
  const qc = useQueryClient();
  const profile = useProfile();
  const today = todayIso();

  const [stage, setStage] = useState<Stage>("input");
  const [analysis, setAnalysis] = useState<MealAnalysisResponse | null>(null);
  const [description, setDescription] = useState("");
  const [note, setNote] = useState("");
  const [correction, setCorrection] = useState("");
  const [correcting, setCorrecting] = useState(false);
  const [saving, setSaving] = useState(false);
  const [showMicros, setShowMicros] = useState(false);

  const cameraRef = useRef<HTMLInputElement>(null);
  const galleryRef = useRef<HTMLInputElement>(null);

  const {
    listening,
    supported,
    error: speechError,
    toggle,
  } = useSpeech(
    (text) => setCorrection((prev) => (prev ? `${prev} ${text}` : text)),
    localeFor(lang),
  );

  useEffect(() => {
    if (!speechError) return;
    toast(
      speechError === "denied" ? t.common.micDenied : t.common.micError,
      "error",
    );
  }, [speechError, toast, t]);

  const run = async (call: () => Promise<MealAnalysisResponse>) => {
    setStage("loading");
    try {
      setAnalysis(await call());
      setStage("result");
    } catch (e) {
      toast(errorMessage(e, t.analysis.error), "error");
      setStage("input");
    }
  };

  const analyzeFile = async (file: File | null | undefined) => {
    if (!file) return;
    const blob = await compressImage(file);
    run(() => mealAnalysisApi.photo(blob, note));
  };

  const applyCorrection = async () => {
    const text = correction.trim();
    if (!text || !analysis) return;
    setCorrecting(true);
    try {
      const corrected = await mealAnalysisApi.correct({
        correction: text,
        mealName: analysis.mealName,
        items: analysis.items.map((item) => ({
          name: item.name,
          quantityGrams: item.quantityGrams,
          foodItemId: item.foodItemId,
        })),
      });
      setAnalysis(corrected);
      setCorrection("");
    } catch (e) {
      toast(errorMessage(e, t.analysis.error), "error");
    } finally {
      setCorrecting(false);
    }
  };

  const removeItem = (index: number) => {
    if (!analysis) return;
    setAnalysis(
      recompute(
        analysis,
        analysis.items.filter((_, i) => i !== index),
      ),
    );
  };

  const setGrams = (index: number, grams: number) => {
    if (!analysis) return;
    const items = analysis.items.map((item, i) =>
      i === index ? rescale(item, grams) : item,
    );
    setAnalysis(recompute(analysis, items));
  };

  const save = async () => {
    if (!analysis?.items.length) return;
    setSaving(true);
    try {
      await mealAnalysisApi.confirm({
        targetDate: today,
        items: analysis.items.map((item) => ({
          name: item.name,
          quantityGrams: item.quantityGrams,
          foodItemId: item.foodItemId,
        })),
      });
      qc.invalidateQueries({ queryKey: ["dailyLog"] });
      qc.invalidateQueries({ queryKey: ["micronutrients"] });
      toast(t.analysis.added(analysis.items.length), "success");
      navigate("/");
    } catch (e) {
      toast(errorMessage(e, t.analysis.addError), "error");
    } finally {
      setSaving(false);
    }
  };

  const micros = useMemo(
    () =>
      NUTRIENT_ORDER.filter(
        (nutrient) => (analysis?.micros[nutrient] ?? 0) > 0,
      ),
    [analysis],
  );

  return (
    <div className="page-enter mx-auto min-h-dvh max-w-lg px-5 pb-32">
      <header className="flex items-center gap-3 py-5">
        <button
          onClick={() => navigate(-1)}
          className="text-marble-dim hover:text-marble"
          aria-label={t.common.back}
        >
          <IconBack size={24} />
        </button>
        <div>
          <p className="text-xs font-semibold tracking-wide text-gold">
            {t.analysis.overline}
          </p>
          <h1 className="text-2xl text-marble">
            {analysis?.mealName ?? t.analysis.title}
          </h1>
        </div>
      </header>

      <input
        ref={cameraRef}
        type="file"
        accept="image/*"
        capture="environment"
        className="hidden"
        onChange={(e) => analyzeFile(e.target.files?.[0])}
      />
      <input
        ref={galleryRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={(e) => analyzeFile(e.target.files?.[0])}
      />

      {stage === "input" && (
        <div className="space-y-5">
          <div className="grid grid-cols-2 gap-3">
            <Button block size="lg" onClick={() => cameraRef.current?.click()}>
              <IconCamera size={18} /> {t.analysis.takePhoto}
            </Button>
            <Button
              block
              size="lg"
              variant="subtle"
              onClick={() => galleryRef.current?.click()}
            >
              <IconImage size={18} /> {t.analysis.pickPhoto}
            </Button>
          </div>

          <Input
            label={t.analysis.noteLabel}
            placeholder={t.analysis.notePlaceholder}
            value={note}
            onChange={(e) => setNote(e.target.value)}
          />

          <div className="rounded-[var(--radius)] bg-surface-low p-4">
            <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-marble-dim">
              {t.analysis.describe}
            </p>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              placeholder={t.analysis.describeHint}
              className="w-full resize-none rounded-[var(--radius-sm)] border border-outline/60 bg-surface-lowest p-3 text-sm text-marble outline-none focus:border-gold"
            />
            <Button
              block
              variant="subtle"
              className="mt-3"
              disabled={!description.trim()}
              onClick={() =>
                run(() => mealAnalysisApi.text(description.trim()))
              }
            >
              <IconSearch size={16} /> {t.analysis.title}
            </Button>
          </div>
        </div>
      )}

      {stage === "loading" && (
        <div className="flex flex-col items-center gap-4 py-24 text-center">
          <Spinner className="size-8" />
          <div>
            <p className="text-sm font-semibold text-marble">
              {t.analysis.analyzing}
            </p>
            <p className="mt-1 max-w-xs text-xs text-marble-dim/80">
              {t.analysis.analyzingHint}
            </p>
          </div>
        </div>
      )}

      {stage === "result" && analysis && (
        <div className="space-y-7">
          <NutritionRings
            totals={{
              kcal: analysis.totalKcal,
              proteins: analysis.totalProteins,
              carbs: analysis.totalCarbs,
              fats: analysis.totalFats,
              fibers: analysis.totalFibers,
            }}
            targets={profile.data ?? {}}
          />

          <section>
            <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-marble-dim">
              {t.analysis.detected}
            </p>
            <ul className="space-y-1">
              {analysis.items.map((item, index) => (
                <li
                  key={`${item.name}-${index}`}
                  className="rounded-[var(--radius)] bg-surface-low px-4 py-3"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm text-marble">
                        {item.name}
                      </p>
                      <p className="text-xs text-marble-dim">
                        {round(item.kcal)} {t.common.kcal} ·{" "}
                        {round(item.proteins)}P / {round(item.carbs)}G /{" "}
                        {round(item.fats)}L
                      </p>
                    </div>
                    <SourceBadge source={item.source} />
                    <button
                      onClick={() => removeItem(index)}
                      className="text-marble-dim transition-colors hover:text-[var(--color-danger)]"
                      aria-label={t.analysis.remove}
                    >
                      <IconTrash size={16} />
                    </button>
                  </div>
                  <div className="mt-2 flex items-center gap-2">
                    <input
                      type="number"
                      inputMode="decimal"
                      value={round(item.quantityGrams)}
                      onChange={(e) =>
                        setGrams(index, Number(e.target.value) || 0)
                      }
                      className="w-24 rounded-[var(--radius-sm)] border border-outline/60 bg-surface-lowest px-2 py-1 text-sm tabular-nums text-marble outline-none focus:border-gold"
                    />
                    <span className="text-xs text-marble-dim">
                      {t.analysis.grams}
                    </span>
                  </div>
                </li>
              ))}
            </ul>
          </section>

          {micros.length > 0 && (
            <section>
              <button
                onClick={() => setShowMicros((open) => !open)}
                className="w-full rounded-[var(--radius)] bg-surface-low px-4 py-3 text-left text-sm font-semibold text-marble"
              >
                {showMicros ? t.analysis.hideMicros : t.analysis.showMicros}
              </button>
              {showMicros && (
                <div className="mt-2 px-1">
                  <p className="pb-2 text-xs text-marble-dim/80">
                    {t.micros.coverage(
                      Math.round(analysis.microCoverage * 100),
                    )}
                  </p>
                  {micros.map((nutrient) => (
                    <NutrientGauge
                      key={nutrient}
                      label={t.nutrients[nutrient]}
                      value={analysis.micros[nutrient] ?? 0}
                      reference={referenceFor(nutrient, profile.data?.gender)}
                      unit={unitOf(nutrient)}
                    />
                  ))}
                </div>
              )}
            </section>
          )}

          <section className="rounded-[var(--radius)] bg-surface-low p-4">
            <p className="text-xs font-semibold uppercase tracking-wide text-marble-dim">
              {t.analysis.correct}
            </p>
            <p className="mt-1 text-xs text-marble-dim/70">
              {t.analysis.correctHint}
            </p>
            <div className="relative mt-3">
              <textarea
                value={correction}
                onChange={(e) => setCorrection(e.target.value)}
                rows={2}
                placeholder={t.analysis.correctPlaceholder}
                className="w-full resize-none rounded-[var(--radius-sm)] border border-outline/60 bg-surface-lowest p-3 pr-11 text-sm text-marble outline-none focus:border-gold"
              />
              {supported && (
                <button
                  onClick={toggle}
                  className={cn(
                    "absolute right-2.5 top-2.5 rounded-full p-1",
                    listening ? "text-gold" : "text-marble-dim hover:text-gold",
                  )}
                  aria-label={t.analysis.correct}
                >
                  <IconMic size={20} />
                </button>
              )}
            </div>
            <Button
              block
              variant="subtle"
              className="mt-3"
              loading={correcting}
              disabled={!correction.trim()}
              onClick={applyCorrection}
            >
              {t.analysis.apply}
            </Button>
          </section>
        </div>
      )}

      {stage === "result" && analysis && (
        <div
          className="glass fixed inset-x-0 bottom-0 z-40 border-t border-outline/20 px-5 pt-3"
          style={{
            paddingBottom: "calc(0.75rem + env(safe-area-inset-bottom))",
          }}
        >
          <div className="mx-auto max-w-lg">
            <Button
              block
              size="lg"
              loading={saving}
              disabled={analysis.items.length === 0}
              onClick={save}
            >
              {t.analysis.addToJournal}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

const SOURCE_STYLE: Record<FoodSource, string> = {
  CIQUAL: "text-gold/80",
  OFF: "text-marble-dim",
  MANUAL: "text-marble-dim",
  AI: "text-[var(--color-purple-bright)]",
};

function SourceBadge({ source }: { source: FoodSource }) {
  const t = useT();
  const label = {
    CIQUAL: t.analysis.sourceCiqual,
    OFF: t.analysis.sourceOff,
    MANUAL: t.analysis.sourceManual,
    AI: t.analysis.sourceAi,
  }[source];
  return (
    <span
      className={cn(
        "shrink-0 text-[0.6rem] font-semibold",
        SOURCE_STYLE[source],
      )}
    >
      {label}
    </span>
  );
}

function rescale(
  item: AnalyzedFoodResponse,
  grams: number,
): AnalyzedFoodResponse {
  const ratio = item.quantityGrams > 0 ? grams / item.quantityGrams : 0;
  const micros: Partial<Record<Nutrient, number>> = {};
  for (const [nutrient, value] of Object.entries(item.micros)) {
    micros[nutrient as Nutrient] = (value ?? 0) * ratio;
  }
  return {
    ...item,
    quantityGrams: grams,
    kcal: item.kcal * ratio,
    proteins: item.proteins * ratio,
    carbs: item.carbs * ratio,
    fats: item.fats * ratio,
    fibers: item.fibers == null ? item.fibers : item.fibers * ratio,
    sugars: item.sugars == null ? item.sugars : item.sugars * ratio,
    saturatedFat:
      item.saturatedFat == null ? item.saturatedFat : item.saturatedFat * ratio,
    salt: item.salt == null ? item.salt : item.salt * ratio,
    micros,
  };
}

function recompute(
  analysis: MealAnalysisResponse,
  items: AnalyzedFoodResponse[],
): MealAnalysisResponse {
  const micros: Partial<Record<Nutrient, number>> = {};
  let kcalWithMicros = 0;
  for (const item of items) {
    if (Object.keys(item.micros).length > 0) kcalWithMicros += item.kcal;
    for (const [nutrient, value] of Object.entries(item.micros)) {
      const key = nutrient as Nutrient;
      micros[key] = (micros[key] ?? 0) + (value ?? 0);
    }
  }
  const total = (
    field: (item: AnalyzedFoodResponse) => number | null | undefined,
  ) => items.reduce((sum, item) => sum + (field(item) ?? 0), 0);
  const totalKcal = total((item) => item.kcal);

  return {
    ...analysis,
    items,
    totalKcal,
    totalProteins: total((item) => item.proteins),
    totalCarbs: total((item) => item.carbs),
    totalFats: total((item) => item.fats),
    totalFibers: total((item) => item.fibers),
    totalSugars: total((item) => item.sugars),
    totalSaturatedFat: total((item) => item.saturatedFat),
    totalSalt: total((item) => item.salt),
    micros,
    microCoverage: totalKcal > 0 ? kcalWithMicros / totalKcal : 0,
  };
}
