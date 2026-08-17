import type { Gender, Nutrient, NutrientCategory } from "@/types/api";

interface NutrientSpec {
  unit: string;
  category: NutrientCategory;
  male: number;
  female: number;
}

const NUTRIENTS: Record<Nutrient, NutrientSpec> = {
  CALCIUM: { unit: "mg", category: "MINERAL", male: 950, female: 950 },
  IRON: { unit: "mg", category: "MINERAL", male: 11, female: 16 },
  MAGNESIUM: { unit: "mg", category: "MINERAL", male: 380, female: 300 },
  POTASSIUM: { unit: "mg", category: "MINERAL", male: 3500, female: 3500 },
  ZINC: { unit: "mg", category: "MINERAL", male: 14, female: 11 },
  SELENIUM: { unit: "µg", category: "MINERAL", male: 70, female: 70 },
  IODINE: { unit: "µg", category: "MINERAL", male: 150, female: 150 },
  VITAMIN_A: { unit: "µg", category: "VITAMIN", male: 750, female: 650 },
  VITAMIN_C: { unit: "mg", category: "VITAMIN", male: 110, female: 110 },
  VITAMIN_D: { unit: "µg", category: "VITAMIN", male: 15, female: 15 },
  VITAMIN_B9: { unit: "µg", category: "VITAMIN", male: 330, female: 330 },
  VITAMIN_B12: { unit: "µg", category: "VITAMIN", male: 4, female: 4 },
  OMEGA3_ALA: { unit: "g", category: "FATTY_ACID", male: 2.8, female: 2.2 },
  OMEGA3_EPA_DHA: { unit: "g", category: "FATTY_ACID", male: 0.5, female: 0.5 },
};

export const NUTRIENT_ORDER = Object.keys(NUTRIENTS) as Nutrient[];

export const NUTRIENT_CATEGORIES: NutrientCategory[] = [
  "MINERAL",
  "VITAMIN",
  "FATTY_ACID",
];

export function unitOf(nutrient: Nutrient): string {
  return NUTRIENTS[nutrient].unit;
}

export function categoryOf(nutrient: Nutrient): NutrientCategory {
  return NUTRIENTS[nutrient].category;
}

export function referenceFor(
  nutrient: Nutrient,
  gender?: Gender | null,
): number {
  const spec = NUTRIENTS[nutrient];
  return gender === "FEMALE" ? spec.female : spec.male;
}
