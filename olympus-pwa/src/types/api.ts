// Types miroir des DTO et enums du backend Olympus (olympus-back).

export type Gender = "MALE" | "FEMALE";
export type ActivityLevel = "SEDENTARY" | "LIGHT" | "MODERATE" | "INTENSE";
export type Goal = "LOSE_WEIGHT" | "MAINTAIN" | "GAIN_MUSCLE";
export type AiProvider = "MISTRAL" | "GEMINI";
export type FoodSource = "OFF" | "MANUAL" | "CIQUAL" | "AI";
export type ChatRole = "USER" | "ASSISTANT" | "SYSTEM";
export type DayOfWeek =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY";
export type RecurrenceType =
  "DAILY" | "SPECIFIC_WEEKDAYS" | "EVERY_OTHER_DAY" | "CUSTOM" | "WEEKLY";

// ---- Auth ----
export interface AuthRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  recoveryEmail: string;
  password: string;
  gender: Gender;
  heightCm: number;
  weightKg: number;
  birthDate: string; // YYYY-MM-DD
  activityLevel: ActivityLevel;
  goal: Goal;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  expiresIn: number;
  user: UserResponse;
}

// ---- User ----
export interface UserResponse {
  id: number;
  email: string;
  recoveryEmail?: string | null;
  gender: Gender;
  heightCm: number;
  currentWeightKg: number;
  birthDate: string;
  activityLevel: ActivityLevel;
  goal: Goal;
  autoCalculateTargets: boolean;
  manualTargetKcal?: number | null;
  manualTargetProteins?: number | null;
  manualTargetCarbs?: number | null;
  manualTargetFats?: number | null;
  targetKcal?: number | null;
  targetProteins?: number | null;
  targetCarbs?: number | null;
  targetFats?: number | null;
  targetFibers?: number | null;
  aiProvider: AiProvider;
  createdAt?: string;
}

export interface UpdateProfileRequest {
  currentWeightKg?: number;
  recoveryEmail?: string;
  heightCm?: number;
  gender?: Gender;
  birthDate?: string;
  activityLevel?: ActivityLevel;
  goal?: Goal;
  autoCalculateTargets?: boolean;
  manualTargetKcal?: number;
  manualTargetProteins?: number;
  manualTargetCarbs?: number;
  manualTargetFats?: number;
  aiProvider?: AiProvider;
}

// ---- Food items ----
export interface FoodItemResponse {
  id: number;
  barcode?: string | null;
  name: string;
  kcal100g: number;
  proteins100g: number;
  carbs100g: number;
  fats100g: number;
  fibers100g?: number | null;
  sugars100g?: number | null;
  saturatedFat100g?: number | null;
  salt100g?: number | null;
  source: FoodSource;
  estimatedWeightGrams?: number | null;
}

export interface FoodItemRequest {
  name: string;
  kcal100g: number;
  proteins100g: number;
  carbs100g: number;
  fats100g: number;
}

export interface AiMealRequest {
  description: string;
}

// ---- Micronutriments ----
export type Nutrient =
  | "CALCIUM"
  | "IRON"
  | "MAGNESIUM"
  | "POTASSIUM"
  | "ZINC"
  | "SELENIUM"
  | "IODINE"
  | "VITAMIN_A"
  | "VITAMIN_C"
  | "VITAMIN_D"
  | "VITAMIN_B9"
  | "VITAMIN_B12"
  | "OMEGA3_ALA"
  | "OMEGA3_EPA_DHA";

export type NutrientCategory = "MINERAL" | "VITAMIN" | "FATTY_ACID";

export interface MicronutrientResponse {
  nutrient: Nutrient;
  category: NutrientCategory;
  unit: string;
  consumed: number;
  reference: number;
  coverage: number;
}

export interface DailyMicronutrientsResponse {
  targetDate: string;
  nutrients: MicronutrientResponse[];
  overallCoverage: number;
}

// ---- Analyse d'un repas ----
export interface AnalyzedFoodResponse {
  name: string;
  quantityGrams: number;
  source: FoodSource;
  foodItemId?: number | null;
  kcal: number;
  proteins: number;
  carbs: number;
  fats: number;
  fibers?: number | null;
  sugars?: number | null;
  saturatedFat?: number | null;
  salt?: number | null;
  micros: Partial<Record<Nutrient, number>>;
}

export interface MealAnalysisResponse {
  mealName: string;
  items: AnalyzedFoodResponse[];
  totalKcal: number;
  totalProteins: number;
  totalCarbs: number;
  totalFats: number;
  totalFibers: number;
  totalSugars: number;
  totalSaturatedFat: number;
  totalSalt: number;
  micros: Partial<Record<Nutrient, number>>;
  microCoverage: number;
}

export interface AnalyzedFoodRequest {
  name: string;
  quantityGrams: number;
  foodItemId?: number | null;
}

export interface MealCorrectionRequest {
  correction: string;
  mealName?: string;
  items: AnalyzedFoodRequest[];
}

export interface MealConfirmationRequest {
  targetDate: string;
  items: AnalyzedFoodRequest[];
}

// ---- Meal presets ----
export interface MealIngredientResponse {
  id: number;
  foodItem: FoodItemResponse;
  quantityGrams: number;
  unit?: string | null;
  amount?: number | null;
}

export interface MealIngredientRequest {
  foodItemId: number;
  quantityGrams: number;
  unit?: string;
  amount?: number;
}

export interface MealPresetResponse {
  id: number;
  name: string;
  ingredients: MealIngredientResponse[];
  totalKcal: number;
  totalProteins: number;
  totalCarbs: number;
  totalFats: number;
  totalFibers: number;
  totalSugars: number;
  totalSaturatedFat: number;
  totalSalt: number;
}

export interface MealPresetRequest {
  name: string;
  ingredients: MealIngredientRequest[];
}

// ---- Daily log ----
export interface LogEntryResponse {
  id: number;
  foodItem?: FoodItemResponse | null;
  mealPreset?: MealPresetResponse | null;
  quantityGrams: number;
  unit?: string | null;
  amount?: number | null;
  consumedAt?: string;
  fromPlan?: boolean;
}

export interface LogEntryRequest {
  targetDate: string;
  foodItemId?: number;
  mealPresetId?: number;
  quantityGrams?: number;
  unit?: string;
  amount?: number;
}

export interface UpdateLogEntryRequest {
  quantityGrams: number;
  unit?: string;
  amount?: number;
}

export interface UpdateActivityRequest {
  targetDate: string;
  stepCount?: number;
  workoutDurationMinutes?: number;
  manualKcalBurned?: number;
}

export interface DailyLogResponse {
  id: number;
  targetDate: string;
  totalKcal: number;
  totalProteins: number;
  totalCarbs: number;
  totalFats: number;
  totalFibers: number;
  totalSugars: number;
  totalSaturatedFat: number;
  totalSalt: number;
  stepCount?: number | null;
  workoutDurationMinutes?: number | null;
  manualKcalBurned?: number | null;
  extraKcalBurned?: number | null;
  planApplied?: boolean;
  entries: LogEntryResponse[];
}

// ---- Meal plans ----
export interface PlannedMealEntryResponse {
  id: number;
  foodItem?: FoodItemResponse | null;
  mealPreset?: MealPresetResponse | null;
  quantityGrams: number;
  plannedTime?: string | null;
  dayOfWeek: DayOfWeek;
}

export interface PlannedMealEntryRequest {
  foodItemId?: number;
  mealPresetId?: number;
  quantityGrams: number;
  plannedTime?: string;
  dayOfWeek: DayOfWeek;
}

export interface MealPlanResponse {
  id: number;
  name?: string;
  recurrenceType?: RecurrenceType;
  weekdaysMask?: string | null;
  anchorDate?: string | null;
  customIntervalDays?: number | null;
  startDate?: string;
  endDate?: string;
  active?: boolean;
  entries: PlannedMealEntryResponse[];
}

export interface WeeklyPlanRequest {
  entries: PlannedMealEntryRequest[];
}

export interface MealPlanGenerationRequest {
  prompt?: string;
}

// ---- Analytics ----
export interface DailyMetricPoint {
  date: string;
  weightKg?: number | null;
  targetKcal?: number | null;
  totalKcal?: number | null;
  totalProteins?: number | null;
  totalCarbs?: number | null;
  totalFats?: number | null;
  extraKcalBurned?: number | null;
}

export interface AnalyticsResponse {
  startDate: string;
  endDate: string;
  averageKcal?: number | null;
  averageWeight?: number | null;
  estimatedFatLossGrams?: number | null;
  dailyData: DailyMetricPoint[];
}

// ---- Agent / chat ----
export interface ChatResponseDto {
  conversationId: number;
  reply: string;
  actionsTaken?: string[];
  provider?: AiProvider;
  timestamp?: string;
}

export interface ConversationSummaryDto {
  id: number;
  title?: string;
  updatedAt?: string;
}

export interface ChatMessageDto {
  id: number;
  role: ChatRole;
  content: string;
  createdAt?: string;
}
