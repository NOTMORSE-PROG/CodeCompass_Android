package com.example.codecompass.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.codecompass.R;
import com.example.codecompass.api.ApiClient;
import com.example.codecompass.api.TokenManager;
import com.example.codecompass.model.Certification;
import com.example.codecompass.model.PagedResponse;
import com.example.codecompass.model.Roadmap;
import com.example.codecompass.model.RoadmapNode;
import com.example.codecompass.model.TrackCertRequest;
import com.example.codecompass.model.UserCertification;
import com.example.codecompass.ui.adapter.CertificationAdapter;
import com.example.codecompass.ui.adapter.RecommendationAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CertificationsActivity extends AppCompatActivity
        implements CertificationAdapter.OnCertActionListener,
                   CertDetailBottomSheet.OnTrackChangeListener {

    // ── Track filter chips (replaces 22-provider row) ─────────────────────────
    private static final String[][] TRACK_CHIPS = {
        {"all",           "All"},
        {"web",           "Web Dev"},
        {"backend",       "Backend"},
        {"data_science",  "Data Science"},
        {"cybersecurity", "Cybersecurity"},
        {"cloud",         "Cloud"},
        {"mobile",        "Mobile Dev"},
        {"networking",    "Networking"},
        {"algorithms",    "Algorithms"},
        {"marketing",     "Marketing"},
        {"agile",         "Agile & PM"},
        {"general",       "General IT"},
    };

    // ── Views ──────────────────────────────────────────────────────────────────
    private LinearProgressIndicator loadingBar;
    private TextInputEditText etSearch;
    private LinearLayout chipGroupProvider;
    private Chip chipMyCerts;
    private Chip chipFreeOnly;
    private RecyclerView rvCertifications;
    private RecyclerView rvRecommendations;
    private LinearLayout sectionRecommendations;
    private TextView tvEmpty;
    private TextView tvResultCount;

    // ── State ──────────────────────────────────────────────────────────────────
    private final List<Certification> allCerts = new ArrayList<>();
    private final Map<Integer, UserCertification> myCerts = new HashMap<>();
    private String activeTrack = "all";
    private boolean showMyCerts = false;
    private boolean showFreeOnly = false;
    private String searchQuery = "";
    private int pendingRequests = 0;
    private boolean isHandling401 = false;
    private Roadmap primaryRoadmap = null;
    private boolean roadmapLoaded = false;

    // ── Adapters ────────────────────────────────────────────────────────────────
    private CertificationAdapter adapter;
    private RecommendationAdapter recommendationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_certifications);

        loadingBar             = findViewById(R.id.loadingBar);
        etSearch               = findViewById(R.id.etSearch);
        chipGroupProvider      = findViewById(R.id.chipGroupProvider);
        chipMyCerts            = findViewById(R.id.chipMyCerts);
        chipFreeOnly           = findViewById(R.id.chipFreeOnly);
        rvCertifications       = findViewById(R.id.rvCertifications);
        rvRecommendations      = findViewById(R.id.rvRecommendations);
        sectionRecommendations = findViewById(R.id.sectionRecommendations);
        tvEmpty                = findViewById(R.id.tvEmpty);
        tvResultCount          = findViewById(R.id.tvResultCount);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new CertificationAdapter(this);
        rvCertifications.setLayoutManager(new LinearLayoutManager(this));
        rvCertifications.setAdapter(adapter);

        recommendationAdapter = new RecommendationAdapter(cert -> onCardTap(cert));
        rvRecommendations.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvRecommendations.setAdapter(recommendationAdapter);

        buildTrackChips();

        chipMyCerts.setOnCheckedChangeListener((btn, checked) -> {
            showMyCerts = checked;
            applyFilter();
        });
        chipFreeOnly.setOnCheckedChangeListener((btn, checked) -> {
            showFreeOnly = checked;
            applyFilter();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase(Locale.getDefault()).trim();
                applyFilter();
            }
        });

        loadData();
    }

    // ── Data loading ───────────────────────────────────────────────────────────

    private void loadData() {
        pendingRequests = 2;
        showLoading(true);
        String token = TokenManager.getBearerToken(this);
        loadCertPage(token, 1);
        loadMyCerts(token);
        loadPrimaryRoadmapForRecs(token);   // non-blocking — not in pendingRequests
    }

    /** Fetches all certification pages recursively until `next` is null. */
    private void loadCertPage(String token, int page) {
        ApiClient.getService()
                .getCertifications(token, null, null, null, null, page)
                .enqueue(new Callback<PagedResponse<Certification>>() {
                    @Override
                    public void onResponse(@NonNull Call<PagedResponse<Certification>> call,
                                           @NonNull Response<PagedResponse<Certification>> response) {
                        if (isHandling401) return;
                        if (response.code() == 401) { handle401(); return; }
                        if (response.isSuccessful() && response.body() != null) {
                            PagedResponse<Certification> pageData = response.body();
                            if (pageData.getResults() != null) {
                                allCerts.addAll(pageData.getResults());
                            }
                            if (pageData.getNext() != null) {
                                loadCertPage(token, page + 1);
                                return;
                            }
                        }
                        decrementAndRefresh();
                    }
                    @Override
                    public void onFailure(@NonNull Call<PagedResponse<Certification>> call,
                                          @NonNull Throwable t) {
                        decrementAndRefresh();
                    }
                });
    }

    private void loadMyCerts(String token) {
        ApiClient.getService()
                .getMyCertifications(token)
                .enqueue(new Callback<PagedResponse<UserCertification>>() {
                    @Override
                    public void onResponse(@NonNull Call<PagedResponse<UserCertification>> call,
                                           @NonNull Response<PagedResponse<UserCertification>> response) {
                        if (isHandling401) return;
                        if (response.code() == 401) { handle401(); return; }
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResults() != null) {
                            myCerts.clear();
                            for (UserCertification uc : response.body().getResults()) {
                                if (uc.getCertification() != null) {
                                    myCerts.put(uc.getCertification().getId(), uc);
                                }
                            }
                        }
                        decrementAndRefresh();
                    }
                    @Override
                    public void onFailure(@NonNull Call<PagedResponse<UserCertification>> call,
                                          @NonNull Throwable t) {
                        decrementAndRefresh();
                    }
                });
    }

    /**
     * Loads the primary roadmap (list → detail) to power Recommendations.
     * Not counted in pendingRequests — runs independently and calls
     * updateRecommendations() when done.
     */
    private void loadPrimaryRoadmapForRecs(String token) {
        ApiClient.getService().getRoadmaps(token).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(@NonNull Call<JsonElement> call,
                                   @NonNull Response<JsonElement> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    roadmapLoaded = true;
                    updateRecommendations();
                    return;
                }
                int id = extractPrimaryRoadmapId(response.body());
                if (id == -1) {
                    roadmapLoaded = true;
                    updateRecommendations();
                    return;
                }
                ApiClient.getService().getRoadmap(token, id).enqueue(new Callback<Roadmap>() {
                    @Override
                    public void onResponse(@NonNull Call<Roadmap> call2,
                                           @NonNull Response<Roadmap> r2) {
                        if (r2.isSuccessful() && r2.body() != null) {
                            primaryRoadmap = r2.body();
                        }
                        roadmapLoaded = true;
                        updateRecommendations();
                    }
                    @Override
                    public void onFailure(@NonNull Call<Roadmap> call2, @NonNull Throwable t) {
                        roadmapLoaded = true;
                        updateRecommendations();
                    }
                });
            }
            @Override
            public void onFailure(@NonNull Call<JsonElement> call, @NonNull Throwable t) {
                roadmapLoaded = true;
                updateRecommendations();
            }
        });
    }

    private int extractPrimaryRoadmapId(JsonElement body) {
        try {
            JsonArray arr;
            if (body.isJsonArray()) {
                arr = body.getAsJsonArray();
            } else {
                JsonObject obj = body.getAsJsonObject();
                if (!obj.has("results")) return -1;
                arr = obj.getAsJsonArray("results");
            }
            int firstId = -1;
            for (int i = 0; i < arr.size(); i++) {
                JsonObject r = arr.get(i).getAsJsonObject();
                int rid = r.get("id").getAsInt();
                if (firstId == -1) firstId = rid;
                if (r.has("isPrimary") && r.get("isPrimary").getAsBoolean()) return rid;
            }
            return firstId;
        } catch (Exception e) {
            return -1;
        }
    }

    private void decrementAndRefresh() {
        pendingRequests--;
        if (pendingRequests <= 0) {
            showLoading(false);
            applyFilter();
            updateRecommendations();
        }
    }

    // ── Recommendations ────────────────────────────────────────────────────────

    private void updateRecommendations() {
        runOnUiThread(() -> {
            if (allCerts.isEmpty() || !roadmapLoaded
                    || primaryRoadmap == null
                    || primaryRoadmap.getNodes() == null
                    || primaryRoadmap.getNodes().isEmpty()) {
                sectionRecommendations.setVisibility(View.GONE);
                return;
            }

            // Build keyword blob from roadmap node titles
            StringBuilder sb = new StringBuilder();
            for (RoadmapNode node : primaryRoadmap.getNodes()) {
                if (node.getTitle() != null) {
                    sb.append(node.getTitle().toLowerCase(Locale.getDefault())).append(" ");
                }
            }
            String keywordBlob = sb.toString();

            // Score untracked certs by relevantSkills ∩ keywordBlob
            List<ScoredCert> scored = new ArrayList<>();
            for (Certification cert : allCerts) {
                if (myCerts.containsKey(cert.getId())) continue;
                if (cert.getRelevantSkills() == null || cert.getRelevantSkills().isEmpty()) continue;
                int score = 0;
                for (String skill : cert.getRelevantSkills()) {
                    if (skill != null
                            && keywordBlob.contains(skill.toLowerCase(Locale.getDefault()))) {
                        score++;
                    }
                }
                if (score > 0) scored.add(new ScoredCert(cert, score));
            }
            scored.sort((a, b) -> b.score - a.score);

            List<Certification> recs = new ArrayList<>();
            for (int i = 0; i < Math.min(4, scored.size()); i++) {
                recs.add(scored.get(i).cert);
            }

            if (recs.isEmpty()) {
                sectionRecommendations.setVisibility(View.GONE);
            } else {
                sectionRecommendations.setVisibility(View.VISIBLE);
                recommendationAdapter.setData(recs);
            }
        });
    }

    private static class ScoredCert {
        final Certification cert;
        final int score;
        ScoredCert(Certification c, int s) { cert = c; score = s; }
    }

    // ── Filtering ──────────────────────────────────────────────────────────────

    private void applyFilter() {
        List<Certification> filtered = allCerts.stream()
                .filter(c -> activeTrack.equals("all") || activeTrack.equals(c.getTrack()))
                .filter(c -> !showFreeOnly || c.isFree())
                .filter(c -> !showMyCerts || myCerts.containsKey(c.getId()))
                .filter(c -> searchQuery.isEmpty()
                        || c.getName().toLowerCase(Locale.getDefault()).contains(searchQuery)
                        || (c.getAbbreviation() != null
                            && c.getAbbreviation().toLowerCase(Locale.getDefault()).contains(searchQuery)))
                .collect(Collectors.toList());

        adapter.setData(filtered, myCerts);

        boolean empty = filtered.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvCertifications.setVisibility(empty ? View.GONE : View.VISIBLE);

        if (!allCerts.isEmpty()) {
            tvResultCount.setText(filtered.size() + " certification"
                    + (filtered.size() == 1 ? "" : "s"));
            tvResultCount.setVisibility(View.VISIBLE);
        }
    }

    // ── Track chips ────────────────────────────────────────────────────────────

    private void buildTrackChips() {
        for (String[] entry : TRACK_CHIPS) {
            String value = entry[0];
            String label = entry[1];
            Chip chip = new Chip(this);
            chip.setText(label);
            chip.setCheckable(true);
            chip.setChecked(value.equals("all"));
            chip.setChipBackgroundColorResource(R.color.colorSurface);
            chip.setChipStrokeColorResource(R.color.colorDivider);
            chip.setChipStrokeWidth(dpToPx(1));
            chip.setTextSize(12f);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(dpToPx(6));
            chip.setLayoutParams(params);

            chip.setOnCheckedChangeListener((btn, checked) -> {
                if (checked) {
                    activeTrack = value;
                    for (int i = 0; i < chipGroupProvider.getChildCount(); i++) {
                        View child = chipGroupProvider.getChildAt(i);
                        if (child instanceof Chip && child != btn) {
                            ((Chip) child).setChecked(false);
                        }
                    }
                    applyFilter();
                } else {
                    if (activeTrack.equals(value)) btn.setChecked(true);
                }
            });

            chipGroupProvider.addView(chip);
        }
    }

    // ── CertificationAdapter.OnCertActionListener ─────────────────────────────

    @Override
    public void onTrack(Certification cert) {
        if (!myCerts.containsKey(cert.getId())) {
            String token = TokenManager.getBearerToken(this);
            ApiClient.getService()
                    .trackCertification(token, new TrackCertRequest(cert.getId(), "interested"))
                    .enqueue(new Callback<UserCertification>() {
                        @Override
                        public void onResponse(@NonNull Call<UserCertification> call,
                                               @NonNull Response<UserCertification> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                onTracked(response.body());
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<UserCertification> call,
                                              @NonNull Throwable t) {}
                    });
        } else {
            onCardTap(cert);
        }
    }

    @Override
    public void onCardTap(Certification cert) {
        UserCertification uc = myCerts.get(cert.getId());
        CertDetailBottomSheet sheet = CertDetailBottomSheet.newInstance(cert, uc);
        sheet.setOnTrackChangeListener(this);
        sheet.show(getSupportFragmentManager(), "cert_detail");
    }

    // ── CertDetailBottomSheet.OnTrackChangeListener ───────────────────────────

    @Override
    public void onTracked(UserCertification uc) {
        if (uc.getCertification() != null) {
            myCerts.put(uc.getCertification().getId(), uc);
        }
        applyFilter();
        updateRecommendations();
    }

    @Override
    public void onStatusUpdated(UserCertification uc) {
        if (uc.getCertification() != null) {
            myCerts.put(uc.getCertification().getId(), uc);
        }
        applyFilter();
        updateRecommendations();
    }

    @Override
    public void onUntracked(int certId) {
        myCerts.remove(certId);
        applyFilter();
        updateRecommendations();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void showLoading(boolean show) {
        loadingBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void handle401() {
        if (isHandling401) return;
        isHandling401 = true;
        TokenManager.clear(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
