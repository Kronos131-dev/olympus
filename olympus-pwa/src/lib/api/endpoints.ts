import { api } from "./client";
import type {
  AnalyticsResponse,
  AuthRequest,
  AuthResponse,
  AiMealRequest,
  ChatMessageDto,
  ChatResponseDto,
  ConversationSummaryDto,
  DailyLogResponse,
  FoodItemRequest,
  FoodItemResponse,
  LogEntryRequest,
  MealPlanResponse,
  MealPresetRequest,
  MealPresetResponse,
  RegisterRequest,
  UpdateActivityRequest,
  UpdateProfileRequest,
  UserResponse,
  WeeklyPlanRequest,
  MealPlanGenerationRequest,
} from "@/types/api";

export const authApi = {
  login: (body: AuthRequest) => api.post<AuthResponse>("/auth/login", body, { auth: false }),
  register: (body: RegisterRequest) =>
    api.post<AuthResponse>("/auth/register", body, { auth: false }),
  logout: (refreshToken: string) =>
    api.post<void>("/auth/logout", { refreshToken }, { auth: false }),
};

// L'échange d'un token de liaison Chiron a besoin de l'en-tête X-Integration-Token :
// on l'appelle via un fetch dédié (hors session, sans Bearer).
export async function exchangeChironToken(integrationToken: string): Promise<AuthResponse> {
  const res = await fetch(`${import.meta.env.BASE_URL}api/v1/integration/chiron/session`, {
    method: "POST",
    headers: { "X-Integration-Token": integrationToken },
  });
  if (!res.ok) throw new Error(`handoff ${res.status}`);
  return (await res.json()) as AuthResponse;
}

export const userApi = {
  profile: () => api.get<UserResponse>("/users/profile/"),
  update: (body: UpdateProfileRequest) => api.put<UserResponse>("/users/profile/", body),
};

export const dailyLogApi = {
  get: (date: string) => api.get<DailyLogResponse>(`/daily-logs/${date}`),
  addEntry: (body: LogEntryRequest) => api.post<DailyLogResponse>("/daily-logs/entries", body),
  deleteEntry: (entryId: number) =>
    api.del<DailyLogResponse>(`/daily-logs/entries/${entryId}`),
  updateActivity: (body: UpdateActivityRequest) =>
    api.put<DailyLogResponse>("/daily-logs/activity", body),
};

export const mealPresetApi = {
  list: () => api.get<MealPresetResponse[]>("/meal-presets"),
  get: (id: number) => api.get<MealPresetResponse>(`/meal-presets/${id}`),
  create: (body: MealPresetRequest) => api.post<MealPresetResponse>("/meal-presets", body),
  update: (id: number, body: MealPresetRequest) =>
    api.put<MealPresetResponse>(`/meal-presets/${id}`, body),
  remove: (id: number) => api.del<void>(`/meal-presets/${id}`),
};

export const mealPlanApi = {
  weekly: () => api.get<MealPlanResponse>("/meal-plans/weekly"),
  saveWeekly: (body: WeeklyPlanRequest) =>
    api.put<MealPlanResponse>("/meal-plans/weekly", body),
  generate: (body: MealPlanGenerationRequest) =>
    api.post<MealPlanResponse>("/meal-plans/generate", body),
};

export const foodItemApi = {
  search: (query: string, signal?: AbortSignal) =>
    api.get<FoodItemResponse[]>("/food-items/search", { query: { query }, signal }),
  searchCiqual: (query: string, signal?: AbortSignal) =>
    api.get<FoodItemResponse[]>("/food-items/search/ciqual", { query: { query }, signal }),
  byBarcode: (barcode: string) =>
    api.get<FoodItemResponse>(`/food-items/barcode/${encodeURIComponent(barcode)}`),
  createManual: (body: FoodItemRequest) =>
    api.post<FoodItemResponse>("/food-items/manual", body),
};

export const aiApi = {
  analyzeMeal: (body: AiMealRequest) => api.post<FoodItemResponse>("/ai/analyze-meal", body),
};

export const analyticsApi = {
  range: (startDate: string, endDate: string) =>
    api.get<AnalyticsResponse>("/analytics", { query: { startDate, endDate } }),
};

export const agentApi = {
  conversations: () => api.get<ConversationSummaryDto[]>("/agent/conversations"),
  conversation: (id: number) => api.get<ChatMessageDto[]>(`/agent/conversations/${id}`),
  deleteConversation: (id: number) => api.del<void>(`/agent/conversations/${id}`),
  chat: (message: string, conversationId?: number, image?: Blob) => {
    const form = new FormData();
    form.append("message", message);
    if (conversationId != null) form.append("conversationId", String(conversationId));
    if (image) form.append("image", image, "photo.jpg");
    return api.post<ChatResponseDto>("/agent/chat", form);
  },
};
