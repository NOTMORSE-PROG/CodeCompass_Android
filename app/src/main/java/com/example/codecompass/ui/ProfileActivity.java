package com.example.codecompass.ui;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.codecompass.R;
import com.example.codecompass.api.ApiClient;
import com.example.codecompass.ui.AIChatHubActivity;
import com.example.codecompass.api.TokenManager;
import com.example.codecompass.model.ChangePasswordRequest;
import com.example.codecompass.model.GamificationProfile;
import com.example.codecompass.model.Roadmap;
import com.example.codecompass.util.JwtUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    // ── Views — Identity ──────────────────────────────────────────────────────
    private TextView tvProfileInitial;
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvProfileRole;

    // ── Views — Stats ─────────────────────────────────────────────────────────
    private TextView tvProfileXp;
    private TextView tvProfileStreak;
    private TextView tvProfileBadges;

    // ── Views — Roadmap ───────────────────────────────────────────────────────
    private TextView tvProfileRoadmapTitle;
    private TextView tvProfileRoadmapPct;
    private LinearProgressIndicator progressProfileRoadmap;
    private MaterialCardView cardProfileRoadmap;

    // ── Views — Account ───────────────────────────────────────────────────────
    private LinearLayout rowChangePassword;
    private View dividerChangePassword;
    private LinearLayout rowGoogleConnected;
    private LinearLayout rowConnectGoogle;
    private LinearLayout rowGoogleOnly;
    private LinearLayout btnLogout;

    // ── Misc ──────────────────────────────────────────────────────────────────
    private View loadingBarProfile;
    private int pendingRequests = 0;
    private boolean isHandling401 = false;
    private int currentRoadmapId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Bind views
        tvProfileInitial       = findViewById(R.id.tvProfileInitial);
        tvProfileName          = findViewById(R.id.tvProfileName);
        tvProfileEmail         = findViewById(R.id.tvProfileEmail);
        tvProfileRole          = findViewById(R.id.tvProfileRole);
        tvProfileXp            = findViewById(R.id.tvProfileXp);
        tvProfileStreak        = findViewById(R.id.tvProfileStreak);
        tvProfileBadges        = findViewById(R.id.tvProfileBadges);
        tvProfileRoadmapTitle  = findViewById(R.id.tvProfileRoadmapTitle);
        tvProfileRoadmapPct    = findViewById(R.id.tvProfileRoadmapPct);
        progressProfileRoadmap = findViewById(R.id.progressProfileRoadmap);
        cardProfileRoadmap     = findViewById(R.id.cardProfileRoadmap);
        loadingBarProfile      = findViewById(R.id.loadingBarProfile);
        rowChangePassword      = findViewById(R.id.rowChangePassword);
        dividerChangePassword  = findViewById(R.id.dividerChangePassword);
        rowGoogleConnected     = findViewById(R.id.rowGoogleConnected);
        rowConnectGoogle       = findViewById(R.id.rowConnectGoogle);
        rowGoogleOnly          = findViewById(R.id.rowGoogleOnly);
        btnLogout              = findViewById(R.id.btnLogout);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Logout
        btnLogout.setOnClickListener(v -> logout());

        // Bottom navigation — Profile is the selected tab
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavProfile);
        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) return true;
            if (id == R.id.nav_home) {
                finish();
                return false;
            }
            if (id == R.id.nav_roadmap) {
                openRoadmap();
                return false;
            }
            if (id == R.id.nav_chat) {
                Intent intent = new Intent(this, AIChatHubActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return false;
            }
            Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show();
            return false;
        });

        // Roadmap card taps open RoadmapActivity
        cardProfileRoadmap.setOnClickListener(v -> openRoadmap());

        populateHeaderFromToken();
        setupAccountSection();
        startLoading();
        loadGamificationProfile();
        loadRoadmap();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void openRoadmap() {
        Intent intent = new Intent(this, RoadmapActivity.class);
        intent.putExtra(RoadmapActivity.EXTRA_ROADMAP_ID, currentRoadmapId);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    // ── Smooth tab-switch exit ────────────────────────────────────────────────

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    // ── Token-based header ────────────────────────────────────────────────────

    private void populateHeaderFromToken() {
        String token = TokenManager.getAccessToken(this);

        String fullName = JwtUtils.getFullName(token);
        if (fullName != null && !fullName.trim().isEmpty()) {
            tvProfileName.setText(fullName);
            tvProfileInitial.setText(fullName.substring(0, 1).toUpperCase());
        }

        String email = JwtUtils.getEmail(token);
        if (email != null && !email.isEmpty()) {
            tvProfileEmail.setText(email);
        }

        tvProfileRole.setText(formatRole(JwtUtils.getRole(token)));
    }

    private String formatRole(String role) {
        if (role == null || role.isEmpty()) return getString(R.string.profile_role_student);
        switch (role.toLowerCase()) {
            case "mentor":           return getString(R.string.profile_role_mentor);
            case "admin":            return getString(R.string.profile_role_admin);
            case "incoming_student": return "Incoming Student";
            case "undergraduate":    return "Undergraduate Student";
            default:                 return getString(R.string.profile_role_student);
        }
    }

    // ── Account section ───────────────────────────────────────────────────────

    private void setupAccountSection() {
        boolean hasPwd    = TokenManager.isHasPassword(this);
        boolean googleConn = TokenManager.isGoogleConnected(this);

        rowChangePassword.setVisibility(hasPwd ? View.VISIBLE : View.GONE);
        dividerChangePassword.setVisibility(hasPwd ? View.VISIBLE : View.GONE);
        rowGoogleConnected.setVisibility(googleConn ? View.VISIBLE : View.GONE);
        rowConnectGoogle.setVisibility((hasPwd && !googleConn) ? View.VISIBLE : View.GONE);
        rowGoogleOnly.setVisibility(!hasPwd ? View.VISIBLE : View.GONE);

        rowChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        rowConnectGoogle.setOnClickListener(v ->
                Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show());
    }

    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        TextInputEditText etOld     = dialogView.findViewById(R.id.etOldPassword);
        TextInputEditText etNew     = dialogView.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirm = dialogView.findViewById(R.id.etConfirmPassword);
        TextInputLayout   tilOld    = dialogView.findViewById(R.id.tilOldPassword);
        TextInputLayout   tilNew    = dialogView.findViewById(R.id.tilNewPassword);
        TextInputLayout   tilConfirm = dialogView.findViewById(R.id.tilConfirmPassword);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.profile_pwd_dialog_title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.profile_pwd_update, null) // set listener below to prevent auto-dismiss
                .create();

        dialog.show();

        // Override positive button so we can validate before dismissing
        Button btnUpdate = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnUpdate.setOnClickListener(v -> {
            String old     = text(etOld);
            String newPwd  = text(etNew);
            String confirm = text(etConfirm);

            tilOld.setError(null);
            tilNew.setError(null);
            tilConfirm.setError(null);

            if (old.isEmpty()) { tilOld.setError(getString(R.string.error_field_required)); return; }
            if (newPwd.length() < 8) { tilNew.setError(getString(R.string.error_password_short)); return; }
            if (!newPwd.equals(confirm)) { tilConfirm.setError(getString(R.string.profile_pwd_mismatch)); return; }

            btnUpdate.setEnabled(false);
            ChangePasswordRequest req = new ChangePasswordRequest(
                    old, newPwd, confirm, TokenManager.getRefreshToken(this));

            ApiClient.getService()
                    .changePassword(TokenManager.getBearerToken(this), req)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                dialog.dismiss();
                                Toast.makeText(ProfileActivity.this,
                                        R.string.profile_pwd_success, Toast.LENGTH_SHORT).show();
                            } else if (response.code() == 400) {
                                btnUpdate.setEnabled(true);
                                tilOld.setError(getString(R.string.error_network));
                            } else if (response.code() == 401) {
                                dialog.dismiss();
                                handle401();
                            } else {
                                btnUpdate.setEnabled(true);
                                tilOld.setError(getString(R.string.error_network));
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            btnUpdate.setEnabled(true);
                            tilOld.setError(getString(R.string.error_network));
                        }
                    });
        });
    }

    private String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    // ── Loading helpers ───────────────────────────────────────────────────────

    private void startLoading() {
        pendingRequests = 2;
        loadingBarProfile.setVisibility(View.VISIBLE);
    }

    private void onRequestDone() {
        pendingRequests = Math.max(0, pendingRequests - 1);
        if (pendingRequests == 0) {
            loadingBarProfile.setVisibility(View.GONE);
        }
    }

    // ── Gamification ──────────────────────────────────────────────────────────

    private void loadGamificationProfile() {
        ApiClient.getService()
                .getGamificationProfile(TokenManager.getBearerToken(this))
                .enqueue(new Callback<GamificationProfile>() {
                    @Override
                    public void onResponse(Call<GamificationProfile> call,
                                           Response<GamificationProfile> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            GamificationProfile profile = response.body();
                            tvProfileXp.setText(String.format(Locale.getDefault(), "%,d", profile.getXpTotal()));
                            tvProfileStreak.setText(String.valueOf(profile.getStreakCount()));
                            tvProfileBadges.setText(String.valueOf(profile.getBadgesEarnedCount()));
                        } else if (response.code() == 401) {
                            handle401();
                            return;
                        }
                        onRequestDone();
                    }

                    @Override
                    public void onFailure(Call<GamificationProfile> call, Throwable t) {
                        onRequestDone();
                    }
                });
    }

    // ── Roadmap ───────────────────────────────────────────────────────────────

    private void loadRoadmap() {
        ApiClient.getService()
                .getRoadmaps(TokenManager.getBearerToken(this))
                .enqueue(new Callback<JsonElement>() {
                    @Override
                    public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            onRequestDone();
                            return;
                        }
                        int roadmapId = extractFirstRoadmapId(response.body());
                        if (roadmapId != -1) {
                            currentRoadmapId = roadmapId;
                            fetchRoadmapDetail(TokenManager.getBearerToken(ProfileActivity.this), roadmapId);
                        } else {
                            onRequestDone();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonElement> call, Throwable t) {
                        onRequestDone();
                    }
                });
    }

    private int extractFirstRoadmapId(JsonElement body) {
        try {
            JsonArray arr;
            if (body.isJsonArray()) {
                arr = body.getAsJsonArray();
            } else {
                JsonObject obj = body.getAsJsonObject();
                if (!obj.has("results")) return -1;
                arr = obj.getAsJsonArray("results");
            }
            if (arr.size() == 0) return -1;
            return arr.get(0).getAsJsonObject().get("id").getAsInt();
        } catch (Exception e) {
            return -1;
        }
    }

    private void fetchRoadmapDetail(String bearer, int roadmapId) {
        ApiClient.getService()
                .getRoadmap(bearer, roadmapId)
                .enqueue(new Callback<Roadmap>() {
                    @Override
                    public void onResponse(Call<Roadmap> call, Response<Roadmap> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            populateRoadmapCard(response.body());
                        } else if (response.code() == 401) {
                            handle401();
                            return;
                        }
                        onRequestDone();
                    }

                    @Override
                    public void onFailure(Call<Roadmap> call, Throwable t) {
                        onRequestDone();
                    }
                });
    }

    private void populateRoadmapCard(Roadmap roadmap) {
        String title = roadmap.getTitle();
        if (title != null && !title.isEmpty()) {
            tvProfileRoadmapTitle.setText(title);
        }
        int pct = roadmap.getCompletionPercentage();
        tvProfileRoadmapPct.setText(String.format(Locale.getDefault(), "%d%%", pct));

        ObjectAnimator anim = ObjectAnimator.ofInt(progressProfileRoadmap, "progress", 0, pct);
        anim.setDuration(900);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();

        cardProfileRoadmap.setVisibility(View.VISIBLE);
    }

    // ── Auth helpers ──────────────────────────────────────────────────────────

    private void logout() {
        TokenManager.clear(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handle401() {
        if (isHandling401) return;
        isHandling401 = true;
        TokenManager.clear(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
