import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  analyticsApi,
  dailyLogApi,
  mealPlanApi,
  mealPresetApi,
  userApi,
} from "@/lib/api/endpoints";
import type {
  DailyLogResponse,
  LogEntryRequest,
  MealPresetRequest,
  UpdateActivityRequest,
  UpdateLogEntryRequest,
  UpdateProfileRequest,
} from "@/types/api";

export const qk = {
  profile: ["profile"] as const,
  dailyLog: (date: string) => ["dailyLog", date] as const,
  micronutrients: (date: string) => ["micronutrients", date] as const,
  presets: ["presets"] as const,
  weeklyPlan: ["weeklyPlan"] as const,
  analytics: (start: string, end: string) => ["analytics", start, end] as const,
};

// ---- Profil ----
export function useProfile() {
  return useQuery({ queryKey: qk.profile, queryFn: userApi.profile });
}

export function useUpdateProfile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: UpdateProfileRequest) => userApi.update(body),
    onSuccess: (user) => qc.setQueryData(qk.profile, user),
  });
}

// ---- Journal du jour ----
export function useDailyLog(date: string) {
  return useQuery({
    queryKey: qk.dailyLog(date),
    queryFn: () => dailyLogApi.get(date),
  });
}

export function useMicronutrients(date: string) {
  return useQuery({
    queryKey: qk.micronutrients(date),
    queryFn: () => dailyLogApi.micronutrients(date),
  });
}

export function useAddLogEntry(date: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: LogEntryRequest) => dailyLogApi.addEntry(body),
    onSuccess: (log) => qc.setQueryData(qk.dailyLog(date), log),
  });
}

export function useUpdateLogEntry(date: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      entryId,
      body,
    }: {
      entryId: number;
      body: UpdateLogEntryRequest;
    }) => dailyLogApi.updateEntry(entryId, body),
    onSuccess: (log) => qc.setQueryData(qk.dailyLog(date), log),
  });
}

export function useDeleteLogEntry(date: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (entryId: number) => dailyLogApi.deleteEntry(entryId),
    // Suppression optimiste : on retire l'entrée immédiatement.
    onMutate: async (entryId) => {
      await qc.cancelQueries({ queryKey: qk.dailyLog(date) });
      const prev = qc.getQueryData<DailyLogResponse>(qk.dailyLog(date));
      if (prev) {
        qc.setQueryData<DailyLogResponse>(qk.dailyLog(date), {
          ...prev,
          entries: prev.entries.filter((e) => e.id !== entryId),
        });
      }
      return { prev };
    },
    onError: (_e, _id, ctx) => {
      if (ctx?.prev) qc.setQueryData(qk.dailyLog(date), ctx.prev);
    },
    onSuccess: (log) => qc.setQueryData(qk.dailyLog(date), log),
  });
}

export function useUpdateActivity(date: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: UpdateActivityRequest) =>
      dailyLogApi.updateActivity(body),
    onSuccess: (log) => qc.setQueryData(qk.dailyLog(date), log),
  });
}

// ---- Repas prédéfinis ----
export function usePresets() {
  return useQuery({ queryKey: qk.presets, queryFn: mealPresetApi.list });
}

export function useDeletePreset() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => mealPresetApi.remove(id),
    onSuccess: () => {
      // Le repas supprimé est détaché du planning et de l'historique côté backend :
      // on rafraîchit aussi ces vues. ["dailyLog"] en préfixe couvre toutes les dates.
      qc.invalidateQueries({ queryKey: qk.presets });
      qc.invalidateQueries({ queryKey: qk.weeklyPlan });
      qc.invalidateQueries({ queryKey: ["dailyLog"] });
    },
  });
}

export function useSavePreset() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id?: number; body: MealPresetRequest }) =>
      id ? mealPresetApi.update(id, body) : mealPresetApi.create(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.presets }),
  });
}

// ---- Planning hebdo ----
export function useWeeklyPlan() {
  return useQuery({ queryKey: qk.weeklyPlan, queryFn: mealPlanApi.weekly });
}

// ---- Analytics ----
export function useAnalytics(start: string, end: string) {
  return useQuery({
    queryKey: qk.analytics(start, end),
    queryFn: () => analyticsApi.range(start, end),
  });
}
