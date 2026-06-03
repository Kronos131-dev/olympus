import type { ActivityLevel, Gender, Goal } from "@/types/api";

export const GOAL_LABELS: Record<Goal, string> = {
  LOSE_WEIGHT: "Perdre du poids",
  MAINTAIN: "Maintenir",
  GAIN_MUSCLE: "Prendre du muscle",
};

export const ACTIVITY_LABELS: Record<ActivityLevel, string> = {
  SEDENTARY: "Sédentaire",
  LIGHT: "Léger",
  MODERATE: "Modéré",
  INTENSE: "Intense",
};

export const GENDER_LABELS: Record<Gender, string> = {
  MALE: "Homme",
  FEMALE: "Femme",
};

export function optionsFrom<T extends string>(map: Record<T, string>) {
  return (Object.keys(map) as T[]).map((value) => ({ value, label: map[value] }));
}
