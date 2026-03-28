package com.example.codecompass.api;

import com.example.codecompass.model.ChangePasswordRequest;
import com.example.codecompass.model.ChatSession;
import com.example.codecompass.model.ChatSessionDetail;
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
import com.example.codecompass.model.TokenRefreshRequest;
import com.example.codecompass.model.TokenRefreshResponse;
import com.example.codecompass.model.roadmap.NodeResource;
import com.example.codecompass.model.roadmap.AssessmentResponse;
import com.example.codecompass.model.roadmap.QuizResult;
import com.example.codecompass.model.roadmap.SubmitAnswersRequest;
import com.example.codecompass.model.roadmap.UpdateNodeResponse;
import com.example.codecompass.model.roadmap.UpdateNodeStatusRequest;
import com.google.gson.JsonElement;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    // ── Authentication ────────────────────────────────────────────────────────

    @POST("auth/token/refresh/")
    Call<TokenRefreshResponse> refreshToken(@Body TokenRefreshRequest body);

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

    @GET("chat/sessions/")
    Call<List<ChatSession>> getChatSessions(
            @Header("Authorization") String bearerToken
    );

    @GET("chat/sessions/{session_id}/")
    Call<ChatSessionDetail> getChatSessionDetail(
            @Header("Authorization") String bearerToken,
            @Path("session_id") String sessionId
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

    // ── Roadmap node operations ───────────────────────────────────────────────

    @PATCH("roadmaps/{roadmapId}/nodes/{nodeId}/")
    Call<UpdateNodeResponse> updateNodeStatus(
            @Header("Authorization") String bearerToken,
            @Path("roadmapId") int roadmapId,
            @Path("nodeId") int nodeId,
            @Body UpdateNodeStatusRequest body
    );

    @POST("roadmaps/{roadmapId}/nodes/{nodeId}/fetch-resources/")
    Call<List<NodeResource>> fetchNodeResources(
            @Header("Authorization") String bearerToken,
            @Path("roadmapId") int roadmapId,
            @Path("nodeId") int nodeId
    );

    @POST("roadmaps/{roadmapId}/nodes/{nodeId}/resources/{resourceId}/unlock/")
    Call<Void> unlockVideoWatch(
            @Header("Authorization") String bearerToken,
            @Path("roadmapId") int roadmapId,
            @Path("nodeId") int nodeId,
            @Path("resourceId") int resourceId
    );

    @POST("roadmaps/{roadmapId}/nodes/{nodeId}/resources/{resourceId}/assessment/")
    Call<AssessmentResponse> generateQuiz(
            @Header("Authorization") String bearerToken,
            @Path("roadmapId") int roadmapId,
            @Path("nodeId") int nodeId,
            @Path("resourceId") int resourceId
    );

    @POST("roadmaps/{roadmapId}/nodes/{nodeId}/resources/{resourceId}/assessment/{sessionId}/submit/")
    Call<QuizResult> submitQuiz(
            @Header("Authorization") String bearerToken,
            @Path("roadmapId") int roadmapId,
            @Path("nodeId") int nodeId,
            @Path("resourceId") int resourceId,
            @Path("sessionId") int sessionId,
            @Body SubmitAnswersRequest body
    );

    @POST("roadmaps/{id}/repair/")
    Call<Void> repairRoadmap(
            @Header("Authorization") String bearerToken,
            @Path("id") int roadmapId
    );

    @POST("roadmaps/{id}/fix-structure/")
    Call<Roadmap> fixRoadmapStructure(
            @Header("Authorization") String bearerToken,
            @Path("id") int roadmapId
    );
}
