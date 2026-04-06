package com.kronos.olympusfront.network;

import com.kronos.olympusfront.network.dto.AnalyticsResponse;
import com.kronos.olympusfront.network.dto.AuthRequest;
import com.kronos.olympusfront.network.dto.AuthResponse;
import com.kronos.olympusfront.network.dto.DailyLogResponse;
import com.kronos.olympusfront.network.dto.FoodItemResponse;
import com.kronos.olympusfront.network.dto.LogEntryRequest;
import com.kronos.olympusfront.network.dto.MealPresetRequest;
import com.kronos.olympusfront.network.dto.MealPresetResponse;
import com.kronos.olympusfront.network.dto.RegisterRequest;
import com.kronos.olympusfront.network.dto.UserResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("/api/v1/auth/login")
    Call<AuthResponse> login(@Body AuthRequest request);

    @POST("/api/v1/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @GET("/api/v1/daily-logs/{date}")
    Call<DailyLogResponse> getDailyLogByDate(@Path("date") String date);

    @GET("/api/v1/meal-presets")
    Call<List<MealPresetResponse>> getUserMealPresets();

    @POST("/api/v1/meal-presets")
    Call<MealPresetResponse> createMealPreset(@Body MealPresetRequest request);

    @POST("/api/v1/daily-logs/entries")
    Call<DailyLogResponse> addLogEntry(@Body LogEntryRequest request);

    @GET("/api/v1/analytics")
    Call<AnalyticsResponse> getAnalyticsForPeriod(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );

    @GET("/api/v1/users/profile")
    Call<UserResponse> getProfile();
    
    @GET("/api/v1/food-items/barcode/{barcode}")
    Call<FoodItemResponse> getFoodItemByBarcode(@Path("barcode") String barcode);

    @GET("/api/v1/food-items/search")
    Call<List<FoodItemResponse>> searchFoodItemsByName(@Query("query") String query);
}