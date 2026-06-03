import { lazy, Suspense, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { aiApi, foodItemApi } from "@/lib/api/endpoints";
import { useDebounce } from "@/hooks/useDebounce";
import { useSpeech } from "@/hooks/useSpeech";
import { useToast } from "@/components/ui/Toast";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { Chip, EmptyState, Skeleton } from "@/components/ui/misc";
import { IconBarcode, IconMic, IconPlus, IconSearch, IconSparkle } from "@/components/icons";

// Le scanner embarque la lib zxing : on ne la charge qu'au moment du scan.
const BarcodeScanner = lazy(() =>
  import("@/components/BarcodeScanner").then((m) => ({ default: m.BarcodeScanner })),
);
import { round } from "@/lib/utils";
import type { FoodItemResponse } from "@/types/api";

interface Props {
  onPick: (food: FoodItemResponse) => void;
}

// Recherche/scan/manuel/IA d'un aliment. Appelle onPick avec le FoodItem choisi.
export function FoodFinder({ onPick }: Props) {
  const toast = useToast();
  const [query, setQuery] = useState("");
  const [useCiqual, setUseCiqual] = useState(false);
  const debounced = useDebounce(query.trim(), 350);
  const [scanOpen, setScanOpen] = useState(false);
  const [manualOpen, setManualOpen] = useState(false);
  const [aiOpen, setAiOpen] = useState(false);

  const results = useQuery({
    queryKey: ["foodSearch", useCiqual, debounced],
    queryFn: ({ signal }) =>
      useCiqual ? foodItemApi.searchCiqual(debounced, signal) : foodItemApi.search(debounced, signal),
    enabled: debounced.length >= 3,
  });

  const handleBarcode = async (code: string) => {
    try {
      const food = await foodItemApi.byBarcode(code);
      onPick(food);
      toast(`${food.name} trouvé`, "success");
    } catch {
      toast("Aucun aliment pour ce code-barres", "error");
    }
  };

  return (
    <div className="space-y-4">
      <Input
        label="Rechercher un aliment"
        placeholder="ex. poulet, riz, banane…"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        autoFocus
      />

      <div className="flex flex-wrap items-center gap-2">
        <Chip active={!useCiqual} onClick={() => setUseCiqual(false)}>
          Base produits
        </Chip>
        <Chip active={useCiqual} onClick={() => setUseCiqual(true)}>
          Ciqual
        </Chip>
        <div className="ml-auto flex gap-2">
          <Button variant="ghost" size="sm" onClick={() => setScanOpen(true)}>
            <IconBarcode size={16} /> Scanner
          </Button>
        </div>
      </div>

      <div className="flex gap-2">
        <Button variant="subtle" size="sm" block onClick={() => setManualOpen(true)}>
          <IconPlus size={16} /> Manuel
        </Button>
        <Button variant="subtle" size="sm" block onClick={() => setAiOpen(true)}>
          <IconSparkle size={16} /> Analyser IA
        </Button>
      </div>

      <div className="min-h-24 space-y-1">
        {debounced.length < 3 && (
          <p className="px-1 py-4 text-center text-xs text-marble-dim">
            Saisis au moins 3 caractères, scanne un code-barres, ou décris ton repas à l'IA.
          </p>
        )}
        {results.isLoading && (
          <>
            <Skeleton className="h-14 w-full" />
            <Skeleton className="h-14 w-full" />
            <Skeleton className="h-14 w-full" />
          </>
        )}
        {results.data?.length === 0 && debounced.length >= 3 && (
          <EmptyState title="Aucun résultat" hint="Essaie un autre terme ou la base Ciqual." />
        )}
        {results.data?.map((food) => (
          <button
            key={`${food.source}-${food.id}-${food.barcode ?? ""}`}
            onClick={() => onPick(food)}
            className="flex w-full items-center justify-between bg-surface-low px-4 py-3 text-left transition-colors hover:bg-surface-high"
          >
            <div className="min-w-0">
              <p className="truncate text-sm text-marble">{food.name}</p>
              <p className="text-xs text-marble-dim">{round(food.kcal100g)} kcal / 100g</p>
            </div>
            <span className="lapidary text-[0.55rem] text-gold/70">{food.source}</span>
          </button>
        ))}
      </div>

      {scanOpen && (
        <Suspense fallback={null}>
          <BarcodeScanner open onClose={() => setScanOpen(false)} onDetected={handleBarcode} />
        </Suspense>
      )}
      <ManualFoodModal open={manualOpen} onClose={() => setManualOpen(false)} onCreated={onPick} />
      <AiFoodModal open={aiOpen} onClose={() => setAiOpen(false)} onAnalyzed={onPick} />
    </div>
  );
}

function ManualFoodModal({
  open,
  onClose,
  onCreated,
}: {
  open: boolean;
  onClose: () => void;
  onCreated: (f: FoodItemResponse) => void;
}) {
  const toast = useToast();
  const [form, setForm] = useState({ name: "", kcal: "", prot: "", carbs: "", fats: "" });
  const [saving, setSaving] = useState(false);

  const submit = async () => {
    if (!form.name.trim()) return toast("Nom requis", "error");
    setSaving(true);
    try {
      const food = await foodItemApi.createManual({
        name: form.name.trim(),
        kcal100g: Number(form.kcal) || 0,
        proteins100g: Number(form.prot) || 0,
        carbs100g: Number(form.carbs) || 0,
        fats100g: Number(form.fats) || 0,
      });
      onCreated(food);
      onClose();
      setForm({ name: "", kcal: "", prot: "", carbs: "", fats: "" });
    } catch {
      toast("Échec de la création", "error");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Aliment manuel (valeurs / 100g)">
      <div className="space-y-3">
        <Input
          label="Nom"
          value={form.name}
          onChange={(e) => setForm({ ...form, name: e.target.value })}
        />
        <div className="grid grid-cols-2 gap-3">
          <Input label="Calories" type="number" inputMode="decimal" value={form.kcal} onChange={(e) => setForm({ ...form, kcal: e.target.value })} />
          <Input label="Protéines" type="number" inputMode="decimal" value={form.prot} onChange={(e) => setForm({ ...form, prot: e.target.value })} />
          <Input label="Glucides" type="number" inputMode="decimal" value={form.carbs} onChange={(e) => setForm({ ...form, carbs: e.target.value })} />
          <Input label="Lipides" type="number" inputMode="decimal" value={form.fats} onChange={(e) => setForm({ ...form, fats: e.target.value })} />
        </div>
        <Button block loading={saving} onClick={submit}>
          Créer l'aliment
        </Button>
      </div>
    </Modal>
  );
}

function AiFoodModal({
  open,
  onClose,
  onAnalyzed,
}: {
  open: boolean;
  onClose: () => void;
  onAnalyzed: (f: FoodItemResponse) => void;
}) {
  const toast = useToast();
  const [description, setDescription] = useState("");
  const [loading, setLoading] = useState(false);
  const { listening, supported, toggle } = useSpeech((t) =>
    setDescription((prev) => (prev ? `${prev} ${t}` : t)),
  );

  const analyze = async () => {
    if (!description.trim()) return;
    setLoading(true);
    try {
      const food = await aiApi.analyzeMeal({ description: description.trim() });
      onAnalyzed(food);
      onClose();
      setDescription("");
      toast("Repas estimé par l'Oracle", "success");
    } catch {
      toast("L'analyse a échoué", "error");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Analyser un repas avec l'IA">
      <div className="space-y-3">
        <p className="text-xs text-marble-dim">
          Décris ce que tu as mangé ; l'Oracle estime les calories et macros.
        </p>
        <div className="relative">
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={4}
            placeholder="ex. une assiette de pâtes carbonara avec 200g de pâtes"
            className="w-full resize-none border-b-2 border-outline/30 bg-surface-lowest p-4 pr-12 text-marble outline-none focus:border-gold"
          />
          {supported && (
            <button
              onClick={toggle}
              className={`absolute right-3 top-3 ${listening ? "text-gold" : "text-marble-dim"}`}
              aria-label="Dicter"
            >
              <IconMic size={20} />
            </button>
          )}
        </div>
        <Button block loading={loading} onClick={analyze}>
          <IconSearch size={16} /> Analyser
        </Button>
      </div>
    </Modal>
  );
}
