package com.example.codecompass.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.codecompass.R;
import com.example.codecompass.api.ApiClient;
import com.example.codecompass.api.TokenManager;
import com.example.codecompass.model.JobListing;
import com.example.codecompass.model.PagedResponse;
import com.example.codecompass.model.SavedJob;
import com.example.codecompass.ui.adapter.JobAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JobsActivity extends AppCompatActivity
        implements JobAdapter.OnJobActionListener,
                   JobDetailBottomSheet.OnSaveChangedListener {

    // ── Job type filter chips ─────────────────────────────────────────────────
    private static final String[][] JOB_TYPE_CHIPS = {
        {null,          "All"},
        {"full_time",   "Full-time"},
        {"part_time",   "Part-time"},
        {"internship",  "Internship"},
        {"freelance",   "Freelance"},
        {"remote",      "Remote"},
    };

    // ── Views ─────────────────────────────────────────────────────────────────
    private LinearProgressIndicator loadingBar;
    private LinearLayout layoutLoadingState;
    private TextInputEditText etSearch;
    private LinearLayout chipGroupJobType;
    private TextView tabAll;
    private TextView tabSaved;
    private View tabIndicator;
    private RecyclerView rvJobs;
    private TextView tvEmpty;
    private TextView tvResultCount;
    private MaterialButton btnLoadMore;
    private SwipeRefreshLayout swipeRefresh;

    // ── State ─────────────────────────────────────────────────────────────────
    private final List<JobListing> jobs     = new ArrayList<>();
    private final List<JobListing> savedJobsList = new ArrayList<>();
    private final Set<Integer> savedJobIds  = new HashSet<>();
    private int  currentPage  = 1;
    private boolean hasMore   = false;
    private boolean tabAllActive = true;
    private boolean savedLoaded  = false;
    private String  activeJobType = null;
    private String  searchQuery   = "";
    private boolean isHandling401 = false;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());

    // ── Adapter ───────────────────────────────────────────────────────────────
    private JobAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jobs);

        loadingBar        = findViewById(R.id.loadingBar);
        layoutLoadingState = findViewById(R.id.layoutLoadingState);
        etSearch          = findViewById(R.id.etSearch);
        chipGroupJobType = findViewById(R.id.chipGroupJobType);
        tabAll          = findViewById(R.id.tabAll);
        tabSaved        = findViewById(R.id.tabSaved);
        tabIndicator    = findViewById(R.id.tabIndicator);
        rvJobs          = findViewById(R.id.rvJobs);
        tvEmpty         = findViewById(R.id.tvEmpty);
        tvResultCount   = findViewById(R.id.tvResultCount);
        btnLoadMore     = findViewById(R.id.btnLoadMore);
        swipeRefresh    = findViewById(R.id.swipeRefresh);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new JobAdapter(this);
        rvJobs.setLayoutManager(new LinearLayoutManager(this));
        rvJobs.setAdapter(adapter);
        rvJobs.setNestedScrollingEnabled(false);

        buildJobTypeChips();
        setActiveTab(true);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacksAndMessages(null);
                searchHandler.postDelayed(() -> {
                    searchQuery = s.toString().trim();
                    loadJobs(true);
                }, 500);
            }
        });

        tabAll.setOnClickListener(v -> {
            if (!tabAllActive) {
                setActiveTab(true);
                refreshDisplay();
            }
        });

        tabSaved.setOnClickListener(v -> {
            if (tabAllActive) {
                setActiveTab(false);
                if (!savedLoaded) {
                    loadSavedJobs();
                } else {
                    refreshDisplay();
                }
            }
        });

        btnLoadMore.setOnClickListener(v -> {
            currentPage++;
            loadJobs(false);
        });

        swipeRefresh.setOnRefreshListener(() -> {
            loadJobs(true);
            loadSavedJobIds();
        });

        loadJobs(true);
        loadSavedJobIds();
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadJobs(boolean reset) {
        if (reset) {
            currentPage = 1;
            jobs.clear();
        }
        showLoading(true);
        String token = TokenManager.getBearerToken(this);
        String search = searchQuery.isEmpty() ? null : searchQuery;

        ApiClient.getService()
                .getJobs(token, currentPage, search, activeJobType)
                .enqueue(new Callback<PagedResponse<JobListing>>() {
                    @Override
                    public void onResponse(@NonNull Call<PagedResponse<JobListing>> call,
                                           @NonNull Response<PagedResponse<JobListing>> response) {
                        if (isHandling401) return;
                        swipeRefresh.setRefreshing(false);
                        showLoading(false);
                        if (response.code() == 401) { handle401(); return; }
                        if (response.isSuccessful() && response.body() != null) {
                            PagedResponse<JobListing> page = response.body();
                            if (page.getResults() != null) {
                                jobs.addAll(page.getResults());
                            }
                            hasMore = page.getNext() != null;
                        }
                        if (tabAllActive) refreshDisplay();
                    }

                    @Override
                    public void onFailure(@NonNull Call<PagedResponse<JobListing>> call,
                                          @NonNull Throwable t) {
                        swipeRefresh.setRefreshing(false);
                        showLoading(false);
                        if (tabAllActive) refreshDisplay();
                    }
                });
    }

    private void loadSavedJobIds() {
        String token = TokenManager.getBearerToken(this);
        ApiClient.getService()
                .getSavedJobs(token)
                .enqueue(new Callback<PagedResponse<SavedJob>>() {
                    @Override
                    public void onResponse(@NonNull Call<PagedResponse<SavedJob>> call,
                                           @NonNull Response<PagedResponse<SavedJob>> response) {
                        if (isHandling401) return;
                        if (response.code() == 401) { handle401(); return; }
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResults() != null) {
                            savedJobIds.clear();
                            savedJobsList.clear();
                            for (SavedJob sj : response.body().getResults()) {
                                if (sj.getJob() != null) {
                                    savedJobIds.add(sj.getJob().getId());
                                    savedJobsList.add(sj.getJob());
                                }
                            }
                            savedLoaded = true;
                            adapter.setSavedIds(savedJobIds);
                            if (!tabAllActive) refreshDisplay();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PagedResponse<SavedJob>> call,
                                          @NonNull Throwable t) {}
                });
    }

    private void loadSavedJobs() {
        showLoading(true);
        String token = TokenManager.getBearerToken(this);
        ApiClient.getService()
                .getSavedJobs(token)
                .enqueue(new Callback<PagedResponse<SavedJob>>() {
                    @Override
                    public void onResponse(@NonNull Call<PagedResponse<SavedJob>> call,
                                           @NonNull Response<PagedResponse<SavedJob>> response) {
                        if (isHandling401) return;
                        showLoading(false);
                        if (response.code() == 401) { handle401(); return; }
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResults() != null) {
                            savedJobIds.clear();
                            savedJobsList.clear();
                            for (SavedJob sj : response.body().getResults()) {
                                if (sj.getJob() != null) {
                                    savedJobIds.add(sj.getJob().getId());
                                    savedJobsList.add(sj.getJob());
                                }
                            }
                            savedLoaded = true;
                        }
                        refreshDisplay();
                    }

                    @Override
                    public void onFailure(@NonNull Call<PagedResponse<SavedJob>> call,
                                          @NonNull Throwable t) {
                        showLoading(false);
                        refreshDisplay();
                    }
                });
    }

    // ── Save / unsave ─────────────────────────────────────────────────────────

    private void toggleSave(JobListing job) {
        boolean wasSaved = savedJobIds.contains(job.getId());
        String token = TokenManager.getBearerToken(this);

        if (wasSaved) {
            // Optimistic remove
            savedJobIds.remove(job.getId());
            savedJobsList.removeIf(j -> j.getId() == job.getId());
            adapter.setSavedIds(savedJobIds);
            if (!tabAllActive) refreshDisplay();

            ApiClient.getService()
                    .unsaveJob(token, job.getId())
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call,
                                               @NonNull Response<Void> response) {
                            if (!response.isSuccessful()) revertSave(job, true);
                        }
                        @Override
                        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                            revertSave(job, true);
                        }
                    });
        } else {
            // Optimistic add
            savedJobIds.add(job.getId());
            savedJobsList.add(0, job);
            adapter.setSavedIds(savedJobIds);

            ApiClient.getService()
                    .saveJob(token, job.getId())
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call,
                                               @NonNull Response<Void> response) {
                            if (!response.isSuccessful() && response.code() != 201
                                    && response.code() != 200) {
                                revertSave(job, false);
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                            revertSave(job, false);
                        }
                    });
        }
    }

    private void revertSave(JobListing job, boolean reAddToSaved) {
        if (reAddToSaved) {
            savedJobIds.add(job.getId());
            savedJobsList.add(0, job);
        } else {
            savedJobIds.remove(job.getId());
            savedJobsList.removeIf(j -> j.getId() == job.getId());
        }
        adapter.setSavedIds(savedJobIds);
        if (!tabAllActive) refreshDisplay();
    }

    // ── JobAdapter.OnJobActionListener ────────────────────────────────────────

    @Override
    public void onJobTap(JobListing job) {
        JobDetailBottomSheet sheet = JobDetailBottomSheet.newInstance(job, savedJobIds);
        sheet.setOnSaveChangedListener(this);
        sheet.show(getSupportFragmentManager(), "job_detail");
    }

    @Override
    public void onSaveToggle(JobListing job, boolean currentlySaved) {
        toggleSave(job);
    }

    // ── JobDetailBottomSheet.OnSaveChangedListener ────────────────────────────

    @Override
    public void onSaveToggled(JobListing job, boolean nowSaved) {
        if (nowSaved) {
            savedJobIds.add(job.getId());
            if (savedJobsList.stream().noneMatch(j -> j.getId() == job.getId())) {
                savedJobsList.add(0, job);
            }
        } else {
            savedJobIds.remove(job.getId());
            savedJobsList.removeIf(j -> j.getId() == job.getId());
        }
        adapter.setSavedIds(savedJobIds);
        if (!tabAllActive) refreshDisplay();
    }

    // ── Display ───────────────────────────────────────────────────────────────

    private void refreshDisplay() {
        List<JobListing> displayList = tabAllActive ? jobs : savedJobsList;
        adapter.setData(displayList, savedJobIds);

        boolean empty = displayList.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvJobs.setVisibility(empty ? View.GONE : View.VISIBLE);

        if (!empty) {
            tvResultCount.setText(displayList.size() + " job" + (displayList.size() == 1 ? "" : "s"));
            tvResultCount.setVisibility(View.VISIBLE);
        } else {
            tvResultCount.setVisibility(View.GONE);
        }

        if (tabAllActive) {
            tvEmpty.setText(R.string.jobs_empty);
            btnLoadMore.setVisibility(hasMore ? View.VISIBLE : View.GONE);
        } else {
            tvEmpty.setText(R.string.jobs_saved_empty);
            btnLoadMore.setVisibility(View.GONE);
        }
    }

    private void setActiveTab(boolean all) {
        tabAllActive = all;
        int primaryColor = getResources().getColor(R.color.colorPrimary, getTheme());
        int secondaryColor = getResources().getColor(R.color.colorTextSecondary, getTheme());

        tabAll.setTextColor(all ? primaryColor : secondaryColor);
        tabSaved.setTextColor(all ? secondaryColor : primaryColor);

        // Move underline indicator
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        tabIndicator.post(() -> {
            int totalWidth = tabAll.getWidth() + tabSaved.getWidth();
            if (totalWidth > 0) {
                tabIndicator.setBackgroundColor(primaryColor);
            }
        });

    }

    // ── Job type chips ────────────────────────────────────────────────────────

    private void buildJobTypeChips() {
        for (String[] entry : JOB_TYPE_CHIPS) {
            String value = entry[0];
            String label = entry[1];
            Chip chip = new Chip(this);
            chip.setText(label);
            chip.setCheckable(true);
            chip.setChecked(value == null);
            chip.setChipBackgroundColorResource(R.color.colorSurface);
            chip.setChipStrokeColorResource(R.color.colorTextPrimary);
            chip.setChipStrokeWidth(dpToPx(1));
            chip.setTextColor(getColor(R.color.colorTextPrimary));
            chip.setTextSize(12f);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dpToPx(6));
            chip.setLayoutParams(lp);

            chip.setOnCheckedChangeListener((btn, checked) -> {
                applyChipActiveStyle((Chip) btn, checked);
                if (checked) {
                    activeJobType = value;
                    for (int i = 0; i < chipGroupJobType.getChildCount(); i++) {
                        View child = chipGroupJobType.getChildAt(i);
                        if (child instanceof Chip && child != btn) {
                            ((Chip) child).setChecked(false);
                        }
                    }
                    loadJobs(true);
                } else {
                    if (activeJobType == null && value == null) btn.setChecked(true);
                    else if (value != null && value.equals(activeJobType)) btn.setChecked(true);
                }
            });

            // Apply initial active state for "All" chip
            applyChipActiveStyle(chip, value == null);

            chipGroupJobType.addView(chip);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void applyChipActiveStyle(Chip chip, boolean active) {
        int primary   = getColor(R.color.colorPrimary);
        int onPrimary = getColor(R.color.colorOnPrimary);
        int surface   = getColor(R.color.colorSurface);
        int textPrimary = getColor(R.color.colorTextPrimary);
        chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(active ? primary : surface));
        chip.setTextColor(active ? onPrimary : textPrimary);
        chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(active ? primary : textPrimary));
    }

    private void showLoading(boolean show) {
        loadingBar.setVisibility(show ? View.VISIBLE : View.GONE);
        // Show centered spinner only during the very first load (list is empty)
        boolean initialLoad = show && jobs.isEmpty() && tabAllActive;
        layoutLoadingState.setVisibility(initialLoad ? View.VISIBLE : View.GONE);
        if (initialLoad) {
            swipeRefresh.setVisibility(View.GONE);
        } else if (!show) {
            swipeRefresh.setVisibility(View.VISIBLE);
            layoutLoadingState.setVisibility(View.GONE);
        }
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
