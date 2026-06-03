import { useState } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Line,
  LineChart,
  Pie,
  PieChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { PageHeader } from "@/components/AppLayout";
import { Card, SectionTitle } from "@/components/ui/Card";
import { Chip, Skeleton, EmptyState } from "@/components/ui/misc";
import { useAnalytics } from "@/hooks/queries";
import { round, shiftIso, todayIso } from "@/lib/utils";

const PERIODS = [
  { days: 7, label: "7 j" },
  { days: 30, label: "30 j" },
  { days: 90, label: "90 j" },
  { days: 365, label: "1 an" },
];

const MACRO_COLORS = ["#7b5cff", "#e9c349", "#c98b6b"];

export default function StatsPage() {
  const [days, setDays] = useState(30);
  const end = todayIso();
  const start = shiftIso(end, -(days - 1));
  const analytics = useAnalytics(start, end);

  const data = analytics.data;
  const points = data?.dailyData ?? [];

  const kcalSeries = points.map((p) => ({
    date: p.date.slice(5),
    kcal: p.totalKcal ?? 0,
    target: p.targetKcal ?? null,
  }));
  const weightSeries = points
    .filter((p) => p.weightKg != null)
    .map((p) => ({ date: p.date.slice(5), poids: p.weightKg }));

  const avgTarget =
    points.length > 0
      ? round(points.reduce((s, p) => s + (p.targetKcal ?? 0), 0) / points.length)
      : 0;

  const macroTotals = points.reduce(
    (acc, p) => {
      acc[0].value += p.totalProteins ?? 0;
      acc[1].value += p.totalCarbs ?? 0;
      acc[2].value += p.totalFats ?? 0;
      return acc;
    },
    [
      { name: "Protéines", value: 0 },
      { name: "Glucides", value: 0 },
      { name: "Lipides", value: 0 },
    ],
  );
  const hasMacros = macroTotals.some((m) => m.value > 0);

  return (
    <div>
      <PageHeader overline="Le marbre du temps" title="Statistiques" />

      <div className="px-5">
        <div className="mb-5 flex gap-2">
          {PERIODS.map((p) => (
            <Chip key={p.days} active={days === p.days} onClick={() => setDays(p.days)}>
              {p.label}
            </Chip>
          ))}
        </div>

        {analytics.isLoading ? (
          <div className="space-y-4">
            <Skeleton className="h-40 w-full" />
            <Skeleton className="h-40 w-full" />
          </div>
        ) : points.length === 0 ? (
          <EmptyState title="Pas encore de données" hint="Enregistre tes repas pour voir tes statistiques." />
        ) : (
          <div className="space-y-5">
            <div className="grid grid-cols-3 gap-3">
              <Stat label="Kcal moy." value={round(data?.averageKcal)} />
              <Stat label="Poids moy." value={data?.averageWeight ? `${round(data.averageWeight, 1)} kg` : "—"} />
              <Stat
                label="Graisse est."
                value={
                  data?.estimatedFatLossGrams != null
                    ? `${round(data.estimatedFatLossGrams / 1000, 2)} kg`
                    : "—"
                }
              />
            </div>

            <Card tone="low">
              <SectionTitle>Calories / jour</SectionTitle>
              <ResponsiveContainer width="100%" height={200}>
                <BarChart data={kcalSeries} margin={{ top: 8, right: 4, left: -20, bottom: 0 }}>
                  <CartesianGrid stroke="#333535" vertical={false} />
                  <XAxis dataKey="date" tick={{ fill: "#9a9a96", fontSize: 10 }} interval="preserveStartEnd" />
                  <YAxis tick={{ fill: "#9a9a96", fontSize: 10 }} />
                  <Tooltip contentStyle={tooltipStyle} cursor={{ fill: "#ffffff10" }} />
                  {avgTarget > 0 && (
                    <ReferenceLine y={avgTarget} stroke="#e9c349" strokeDasharray="4 4" />
                  )}
                  <Bar dataKey="kcal" fill="#5d3fd3" />
                </BarChart>
              </ResponsiveContainer>
            </Card>

            {hasMacros && (
              <Card tone="low">
                <SectionTitle>Répartition des macros</SectionTitle>
                <div className="flex items-center gap-4">
                  <ResponsiveContainer width="50%" height={160}>
                    <PieChart>
                      <Pie data={macroTotals} dataKey="value" nameKey="name" innerRadius={40} outerRadius={70} stroke="none">
                        {macroTotals.map((_, i) => (
                          <Cell key={i} fill={MACRO_COLORS[i]} />
                        ))}
                      </Pie>
                      <Tooltip contentStyle={tooltipStyle} />
                    </PieChart>
                  </ResponsiveContainer>
                  <ul className="space-y-2 text-sm">
                    {macroTotals.map((m, i) => (
                      <li key={m.name} className="flex items-center gap-2">
                        <span className="inline-block size-3" style={{ backgroundColor: MACRO_COLORS[i] }} />
                        <span className="text-marble-dim">{m.name}</span>
                        <span className="text-marble">{round(m.value)} g</span>
                      </li>
                    ))}
                  </ul>
                </div>
              </Card>
            )}

            {weightSeries.length > 1 && (
              <Card tone="low">
                <SectionTitle>Évolution du poids</SectionTitle>
                <ResponsiveContainer width="100%" height={200}>
                  <LineChart data={weightSeries} margin={{ top: 8, right: 8, left: -20, bottom: 0 }}>
                    <CartesianGrid stroke="#333535" vertical={false} />
                    <XAxis dataKey="date" tick={{ fill: "#9a9a96", fontSize: 10 }} interval="preserveStartEnd" />
                    <YAxis domain={["auto", "auto"]} tick={{ fill: "#9a9a96", fontSize: 10 }} />
                    <Tooltip contentStyle={tooltipStyle} />
                    <Line type="monotone" dataKey="poids" stroke="#e9c349" strokeWidth={2} dot={false} />
                  </LineChart>
                </ResponsiveContainer>
              </Card>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

const tooltipStyle = {
  backgroundColor: "#1a1c1c",
  border: "none",
  borderRadius: 0,
  color: "#e2e2e2",
  fontSize: 12,
};

function Stat({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="bg-surface-low p-3 text-center">
      <p className="font-display text-xl text-marble">{value}</p>
      <p className="lapidary text-[0.5rem] tracking-[0.1em] text-marble-dim">{label}</p>
    </div>
  );
}
