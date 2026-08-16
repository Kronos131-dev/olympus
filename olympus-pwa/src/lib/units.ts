import type { FoodItemResponse } from "@/types/api";

export type Unit = "g" | "tsp" | "tbsp" | "piece";

type Family = "oil" | "liquid" | "powder" | "sugar" | "spread" | "egg" | "default";

const GRAMS_PER_UNIT: Record<Family, Partial<Record<Exclude<Unit, "g">, number>>> = {
  oil: { tsp: 4.5, tbsp: 13.5 },
  liquid: { tsp: 5, tbsp: 15 },
  powder: { tsp: 3, tbsp: 9 },
  sugar: { tsp: 4, tbsp: 12 },
  spread: { tsp: 7, tbsp: 21 },
  egg: { piece: 50 },
  default: { tsp: 5, tbsp: 15 },
};

// WHY: \b en JS ne reconnaît que [A-Za-z0-9_] comme caractère de mot — "œ" (la ligature
// française, celle qu'utilise souvent Open Food Facts) n'en fait pas partie, donc \bœuf\b
// ne matche jamais. (?<![\p{L}])...(?![\p{L}]) est la même frontière mais Unicode-safe.
const KEYWORDS: Array<{ family: Family; pattern: RegExp }> = [
  { family: "oil", pattern: /(?<![\p{L}])(huiles?|oils?)(?![\p{L}])/iu },
  { family: "egg", pattern: /(?<![\p{L}])(œufs?|oeufs?|eggs?)(?![\p{L}])/iu },
  { family: "spread", pattern: /(miel|beurre|confiture|pâte à tartiner|honey|butter|jam|spread)/i },
  { family: "sugar", pattern: /(?<![\p{L}])(sucre|sel|sugar|salt)(?![\p{L}])/iu },
  { family: "powder", pattern: /(farine|cacao|poudre|flour|cocoa|powder)/i },
  { family: "liquid", pattern: /(lait|crème|jus|milk|cream|juice)/i },
];

function resolveFamily(food: Pick<FoodItemResponse, "name" | "kcal100g" | "fats100g">): Family {
  for (const { family, pattern } of KEYWORDS) {
    if (pattern.test(food.name)) return family;
  }
  if (food.fats100g > 90) return "oil";
  if (food.kcal100g < 100) return "liquid";
  return "default";
}

export interface UnitOption {
  unit: Unit;
  gramsPerUnit: number;
}

export function availableUnits(
  food: Pick<FoodItemResponse, "name" | "kcal100g" | "fats100g" | "estimatedWeightGrams">,
): UnitOption[] {
  const family = resolveFamily(food);
  const table = GRAMS_PER_UNIT[family];
  const options: UnitOption[] = [{ unit: "g", gramsPerUnit: 1 }];

  if (table.tsp) options.push({ unit: "tsp", gramsPerUnit: table.tsp });
  if (table.tbsp) options.push({ unit: "tbsp", gramsPerUnit: table.tbsp });

  const gramsPerPiece = family === "egg" ? table.piece : food.estimatedWeightGrams || undefined;
  if (gramsPerPiece) options.push({ unit: "piece", gramsPerUnit: gramsPerPiece });

  return options;
}

export function toGrams(amount: number, gramsPerUnit: number): number {
  return amount * gramsPerUnit;
}

export function fromGrams(grams: number, gramsPerUnit: number): number {
  return grams / gramsPerUnit;
}
