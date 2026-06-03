package com.kronos.olympusfront.network;

import com.kronos.olympusfront.network.dto.AiMealRequest;
import com.kronos.olympusfront.network.dto.AnalyticsResponse;
import com.kronos.olympusfront.network.dto.AuthRequest;
import com.kronos.olympusfront.network.dto.AuthResponse;
import com.kronos.olympusfront.network.dto.ChatMessageDto;
import com.kronos.olympusfront.network.dto.ChatResponseDto;
import com.kronos.olympusfront.network.dto.ConversationSummaryDto;
import com.kronos.olympusfront.network.dto.DailyLogResponse;
import com.kronos.olympusfront.network.dto.FoodItemRequest;
import com.kronos.olympusfront.network.dto.FoodItemResponse;
import com.kronos.olympusfront.network.dto.LogEntryRequest;
import com.kronos.olympusfront.network.dto.LogoutRequest;
import com.kronos.olympusfront.network.dto.MealPlanGenerationRequest;
import com.kronos.olympusfront.network.dto.MealPlanRequest;
import com.kronos.olympusfront.network.dto.MealPlanResponse;
import com.kronos.olympusfront.network.dto.MealPresetRequest;
import com.kronos.olympusfront.network.dto.MealPresetResponse;
import com.kronos.olympusfront.network.dto.RefreshTokenRequest;
import com.kronos.olympusfront.network.dto.RegisterRequest;
import com.kronos.olympusfront.network.dto.UpdateActivityRequest;
import com.kronos.olympusfront.network.dto.UserResponse;
import com.kronos.olympusfront.network.dto.WeeklyPlanRequest;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("/api/v1/auth/login")
    Call<AuthResponse> login(@Body AuthRequest request);

    @POST("/api/v1/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("/api/v1/auth/refresh")
    Call<AuthResponse> refreshToken(@Body RefreshTokenRequest request);

    @POST("/api/v1/auth/logout")
    Call<Void> logout(@Body LogoutRequest request);

    @GET("/api/v1/daily-logs/{date}")
    Call<DailyLogResponse> getDailyLogByDate(@Path("date") String date);

    @GET("/api/v1/meal-presets")
    Call<List<MealPresetResponse>> getUserMealPresets();

    @POST("/api/v1/meal-presets")
    Call<MealPresetResponse> createMealPreset(@Body MealPresetRequest request);

    @PUT("/api/v1/meal-presets/{id}")
    Call<MealPresetResponse> updateMealPreset(@Path("id") Long id, @Body MealPresetRequest request);

    @DELETE("/api/v1/meal-presets/{id}")
    Call<Void> deleteMealPreset(@Path("id") Long id);

    // --- Planification des repas ---
    @GET("/api/v1/meal-plans")
    Call<List<MealPlanResponse>> getMealPlans();

    @GET("/api/v1/meal-plans/{id}")
    Call<MealPlanResponse> getMealPlan(@Path("id") Long id);

    @POST("/api/v1/meal-plans")
    Call<MealPlanResponse> createMealPlan(@Body MealPlanRequest request);

    @PUT("/api/v1/meal-plans/{id}")
    Call<MealPlanResponse> updateMealPlan(@Path("id") Long id, @Body MealPlanRequest request);

    @DELETE("/api/v1/meal-plans/{id}")
    Call<Void> deleteMealPlan(@Path("id") Long id);

    @GET("/api/v1/meal-plans/weekly")
    Call<MealPlanResponse> getWeeklyPlan();

    @PUT("/api/v1/meal-plans/weekly")
    Call<MealPlanResponse> saveWeeklyPlan(@Body WeeklyPlanRequest request);

    @POST("/api/v1/meal-plans/generate")
    Call<MealPlanResponse> generateMealPlan(@Body MealPlanGenerationRequest request);

    @POST("/api/v1/daily-logs/entries")
    Call<DailyLogResponse> addLogEntry(@Body LogEntryRequest request);

    @DELETE("/api/v1/daily-logs/entries/{entryId}")
    Call<DailyLogResponse> removeLogEntry(@Path("entryId") Long entryId);
    
    @PUT("/api/v1/daily-logs/activity")
    Call<DailyLogResponse> updateActivity(@Body UpdateActivityRequest request);

    @GET("/api/v1/analytics")
    Call<AnalyticsResponse> getAnalyticsForPeriod(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );

    @GET("/api/v1/users/profile")
    Call<UserResponse> getProfile();
    
    @PUT("/api/v1/users/profile")
    Call<UserResponse> updateProfile(@Body UserResponse userResponse);

    @GET("/api/v1/food-items/barcode/{barcode}")
    Call<FoodItemResponse> getFoodItemByBarcode(@Path("barcode") String barcode);

    @GET("/api/v1/food-items/search")
    Call<List<FoodItemResponse>> searchFoodItemsByName(@Query("query") String query);

    @POST("/api/v1/food-items/manual")
    Call<FoodItemResponse> createManualFoodItem(@Body FoodItemRequest request);

    // Endpoint IA
    @POST("/api/v1/ai/analyze-meal")
    Call<FoodItemResponse> analyzeMealWithAi(@Body AiMealRequest request);

    // --- Agent IA conversationnel ---
    @Multipart
    @POST("/api/v1/agent/chat")
    Call<ChatResponseDto> sendAgentMessage(
            @Part("message") RequestBody message,
            @Part("conversationId") RequestBody conversationId,
            @Part MultipartBody.Part image);

    @GET("/api/v1/agent/conversations")
    Call<List<ConversationSummaryDto>> getConversations();

    @GET("/api/v1/agent/conversations/{id}")
    Call<List<ChatMessageDto>> getConversationMessages(@Path("id") Long id);

    @DELETE("/api/v1/agent/conversations/{id}")
    Call<Void> deleteConversation(@Path("id") Long id);
}