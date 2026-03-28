package com.example.codecompass.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.codecompass.R;
import com.example.codecompass.api.ApiClient;
import com.example.codecompass.api.TokenManager;
import com.example.codecompass.model.ChatSession;
import com.example.codecompass.model.PagedResponse;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AIChatHubActivity extends AppCompatActivity {

    private RecyclerView rvSessions;
    private View loadingBar;
    private LinearLayout layoutEmpty;
    private SwipeRefreshLayout swipeRefresh;
    private ChatSessionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat_hub);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.chat_hub_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        rvSessions   = findViewById(R.id.rvSessions);
        loadingBar   = findViewById(R.id.loadingBar);
        layoutEmpty  = findViewById(R.id.layoutEmpty);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        adapter = new ChatSessionAdapter(session -> openSession(session));
        rvSessions.setLayoutManager(new LinearLayoutManager(this));
        rvSessions.setAdapter(adapter);

        swipeRefresh.setColorSchemeColors(getColor(R.color.colorPrimary));
        swipeRefresh.setOnRefreshListener(this::loadSessions);

        FloatingActionButton fab = findViewById(R.id.fabNewChat);
        fab.setOnClickListener(v -> openFreshChat());

        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSessions();
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadSessions() {
        loadingBar.setVisibility(View.VISIBLE);

        ApiClient.getService()
                .getChatSessions(TokenManager.getBearerToken(this))
                .enqueue(new Callback<PagedResponse<ChatSession>>() {
                    @Override
                    public void onResponse(Call<PagedResponse<ChatSession>> call,
                                           Response<PagedResponse<ChatSession>> response) {
                        loadingBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);

                        if (response.code() == 401) {
                            handle401();
                            return;
                        }
                        if (response.isSuccessful() && response.body() != null) {
                            List<ChatSession> sessions = response.body().getResults();
                            adapter.setSessions(sessions);
                            layoutEmpty.setVisibility(sessions.isEmpty() ? View.VISIBLE : View.GONE);
                            rvSessions.setVisibility(sessions.isEmpty() ? View.GONE : View.VISIBLE);
                        } else {
                            showError();
                        }
                    }

                    @Override
                    public void onFailure(Call<PagedResponse<ChatSession>> call, Throwable t) {
                        loadingBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        showError();
                    }
                });
    }

    private void showError() {
        Snackbar.make(rvSessions, R.string.error_network, Snackbar.LENGTH_LONG)
                .setAction(R.string.retry, v -> loadSessions())
                .show();
    }

    private void handle401() {
        TokenManager.clear(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void openFreshChat() {
        Intent intent = new Intent(this, AIChatActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void openSession(ChatSession session) {
        Intent intent = new Intent(this, AIChatActivity.class);
        intent.putExtra(AIChatActivity.EXTRA_SESSION_ID, session.getSessionId());
        intent.putExtra(AIChatActivity.EXTRA_CONTEXT_TYPE, session.getContextType());
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void openRoadmap() {
        Intent intent = new Intent(this, RoadmapActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    // ── Bottom nav ────────────────────────────────────────────────────────────

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_chat);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_chat) return true;
            if (id == R.id.nav_home) {
                Intent homeIntent = new Intent(this, DashboardActivity.class);
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(homeIntent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return false;
            }
            if (id == R.id.nav_roadmap) {
                openRoadmap();
                return false;
            }
            if (id == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return false;
            }
            Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show();
            return false;
        });
    }
}
