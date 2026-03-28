package com.example.codecompass.ui;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.codecompass.R;
import com.example.codecompass.model.ChatSession;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ChatSessionAdapter extends RecyclerView.Adapter<ChatSessionAdapter.VH> {

    public interface OnSessionClickListener {
        void onSessionClick(ChatSession session);
    }

    private final List<ChatSession> sessions = new ArrayList<>();
    private final OnSessionClickListener listener;

    public ChatSessionAdapter(OnSessionClickListener listener) {
        this.listener = listener;
    }

    public void setSessions(List<ChatSession> newSessions) {
        sessions.clear();
        if (newSessions != null) sessions.addAll(newSessions);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_session, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(sessions.get(position), listener);
    }

    @Override
    public int getItemCount() { return sessions.size(); }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class VH extends RecyclerView.ViewHolder {
        private final TextView tvEmoji;
        private final TextView tvTitle;
        private final TextView tvMode;
        private final TextView tvTime;

        VH(@NonNull View itemView) {
            super(itemView);
            tvEmoji = itemView.findViewById(R.id.tvEmoji);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMode  = itemView.findViewById(R.id.tvMode);
            tvTime  = itemView.findViewById(R.id.tvTime);
        }

        void bind(ChatSession session, OnSessionClickListener listener) {
            tvEmoji.setText(emojiFor(session.getContextType()));
            String title = session.getTitle();
            tvTitle.setText((title != null && !title.isEmpty()) ? title : "Untitled Chat");
            tvMode.setText(labelFor(session.getContextType()));
            tvTime.setText(relativeTime(session.getUpdatedAt()));
            itemView.setOnClickListener(v -> listener.onSessionClick(session));
        }

        private static String emojiFor(String contextType) {
            if (contextType == null) return "💬";
            switch (contextType) {
                case "roadmap":    return "🗺️";
                case "job":        return "💼";
                case "university": return "🏫";
                default:           return "💬";
            }
        }

        private static String labelFor(String contextType) {
            if (contextType == null) return "General";
            switch (contextType) {
                case "roadmap":    return "Roadmap";
                case "job":        return "Jobs";
                case "university": return "University";
                default:           return "General";
            }
        }

        private static CharSequence relativeTime(String isoDate) {
            if (isoDate == null || isoDate.isEmpty()) return "";
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                long millis = sdf.parse(isoDate).getTime();
                return DateUtils.getRelativeTimeSpanString(
                        millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            } catch (ParseException e) {
                // Try without microseconds
                try {
                    SimpleDateFormat sdf2 = new SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                    sdf2.setTimeZone(TimeZone.getTimeZone("UTC"));
                    long millis = sdf2.parse(isoDate).getTime();
                    return DateUtils.getRelativeTimeSpanString(
                            millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
                } catch (ParseException ex) {
                    return "";
                }
            }
        }
    }
}
