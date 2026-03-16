package com.example.codecompass.api;

import com.example.codecompass.model.ChangePasswordRequest;
import com.example.codecompass.model.CompleteOnboardingRequest;
import com.example.codecompass.model.CompleteOnboardingResponse;
import com.example.codecompass.model.CreateSessionRequest;
import com.example.codecompass.model.CreateSessionResponse;
import com.example.codecompass.model.GamificationProfile;
import com.example.codecompass.model.GoogleAuthRequest;
import com.example.codecompass.model.GoogleAuthResponse;
import com.example.codecompass.model.LoginRequest;
import com.example.codecompass.model.LoginResponse;
import com.example.codecompass.model.RegisterRequest;
import com.example.codecompass.model.RegisterResponse;
import com.example.codecompass.model.Roadmap;
import com.google.gson.JsonElement;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    // ── Authentication ────────────────────────────────────────────────────────

    @POST("auth/login/")
    Call<LoginResponse> login(@Body LoginRequest body);

    @POST("auth/register/")
    Call<RegisterResponse> register(@Body RegisterRequest body);

    @POST("auth/google/")
    Call<GoogleAuthResponse> googleAuth(@Body GoogleAuthRequest body);

    @POST("auth/change-password/")
    Call<Void> changePassword(
            @Header("Authorization") String bearerToken,
            @Body ChangePasswordRequest body
    );

    // ── Onboarding ────────────────────────────────────────────────────────────

    @POST("onboarding/complete-from-chat/")
    Call<CompleteOnboardingResponse> completeOnboarding(
            @Header("Authorization") String bearerToken,
            @Body CompleteOnboardingRequest body
    );

    // ── Chat sessions ─────────────────────────────────────────────────────────

    @POST("chat/sessions/")
    Call<CreateSessionResponse> createChatSession(
            @Header("Authorization") String bearerToken,
            @Body CreateSessionRequest body
    );

    // ── Gamification ──────────────────────────────────────────────────────────

    @GET("gamification/profile/")
    Call<GamificationProfile> getGamificationProfile(
            @Header("Authorization") String bearerToken
    );

    // ── Roadmaps ──────────────────────────────────────────────────────────────

    /** Returns either a JSON array or a paginated object with "results" key. */
    @GET("roadmaps/")
    Call<JsonElement> getRoadmaps(
            @Header("Authorization") String bearerToken
    );

    @GET("roadmaps/{id}/")
    Call<Roadmap> getRoadmap(
            @Header("Authorization") String bearerToken,
            @Path("id") int id
    );

    @POST("roadmaps/generate/")
    Call<Roadmap> generateRoadmap(
            @Header("Authorization") String bearerToken
    );
}
