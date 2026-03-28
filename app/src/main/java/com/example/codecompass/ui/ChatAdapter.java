package com.example.codecompass.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.codecompass.R;
import com.example.codecompass.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_AI   = 1;

    private final List<ChatMessage> messages = new ArrayList<>();
    private RecyclerView recyclerView;
    private final boolean useLightBubbles;

    public ChatAdapter() {
        this.useLightBubbles = false; // dark mode — used by OnboardingActivity
    }

    public ChatAdapter(boolean useLightBubbles) {
        this.useLightBubbles = useLightBubbles;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_AI;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        holder.bind(messages.get(position), useLightBubbles);
    }

    @Override
    public void onViewRecycled(@NonNull MessageViewHolder holder) {
        super.onViewRecycled(holder);
        holder.stopDotAnimation();
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        int position = messages.size() - 1;
        notifyItemInserted(position);
        if (recyclerView != null) recyclerView.scrollToPosition(position);
    }

    /** Update bubble in-place — clears typing state and sets streamed content. */
    public void updateMessageAt(int index, String content) {
        if (index < 0 || index >= messages.size()) return;
        ChatMessage msg = messages.get(index);
        msg.setContent(content);
        msg.setTyping(false);
        notifyItemChanged(index);
        if (recyclerView != null) recyclerView.scrollToPosition(index);
    }

    public boolean isTypingAt(int index) {
        return index >= 0 && index < messages.size() && messages.get(index).isTyping();
    }

    public void removeMessageAt(int index) {
        if (index < 0 || index >= messages.size()) return;
        messages.remove(index);
        notifyItemRemoved(index);
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class MessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvMessage;
        private final TextView tvAvatar;
        private final View layoutTypingDots;
        private final View dot1, dot2, dot3;
        private final List<ObjectAnimator> dotAnimators = new ArrayList<>();

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage       = itemView.findViewById(R.id.tvMessage);
            tvAvatar        = itemView.findViewById(R.id.tvAvatar);
            layoutTypingDots = itemView.findViewById(R.id.layoutTypingDots);
            dot1            = itemView.findViewById(R.id.dot1);
            dot2            = itemView.findViewById(R.id.dot2);
            dot3            = itemView.findViewById(R.id.dot3);
        }

        void bind(ChatMessage message, boolean useLightBubbles) {
            if (message.isTyping()) {
                // Typing indicator — CC avatar + bouncing dots
                tvAvatar.setVisibility(View.VISIBLE);
                layoutTypingDots.setVisibility(View.VISIBLE);
                tvMessage.setVisibility(View.GONE);
                ((LinearLayout) itemView).setGravity(Gravity.START | Gravity.BOTTOM);
                if (useLightBubbles) {
                    layoutTypingDots.setBackgroundResource(R.drawable.bg_chat_bubble_ai_light);
                } else {
                    layoutTypingDots.setBackgroundResource(R.drawable.bg_chat_bubble);
                }
                // Tint dots to be visible against their bubble background
                int dotColor = useLightBubbles ? 0xFF888888 : 0xCCFFFFFF;
                ColorStateList tint = ColorStateList.valueOf(dotColor);
                dot1.setBackgroundTintList(tint);
                dot2.setBackgroundTintList(tint);
                dot3.setBackgroundTintList(tint);
                startDotAnimation();
                return;
            }

            // Normal message — stop any leftover animation
            stopDotAnimation();
            layoutTypingDots.setVisibility(View.GONE);
            tvMessage.setVisibility(View.VISIBLE);
            tvMessage.setText(renderMarkdown(message.getContent()));

            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) tvMessage.getLayoutParams();

            if (message.isUser()) {
                tvAvatar.setVisibility(View.GONE);
                tvMessage.setBackgroundResource(R.drawable.bg_chat_bubble_user);
                tvMessage.setTextColor(
                        itemView.getContext().getColor(R.color.colorChatUserText));
                params.gravity = Gravity.END;
                ((LinearLayout) itemView).setGravity(Gravity.END | Gravity.BOTTOM);
            } else {
                tvAvatar.setVisibility(View.VISIBLE);
                if (useLightBubbles) {
                    tvMessage.setBackgroundResource(R.drawable.bg_chat_bubble_ai_light);
                    tvMessage.setTextColor(
                            itemView.getContext().getColor(R.color.colorChatAiText));
                } else {
                    tvMessage.setBackgroundResource(R.drawable.bg_chat_bubble);
                    tvMessage.setTextColor(
                            itemView.getContext().getColor(R.color.colorChatAiTextDark));
                }
                params.gravity = Gravity.START;
                ((LinearLayout) itemView).setGravity(Gravity.START | Gravity.BOTTOM);
            }
            tvMessage.setLayoutParams(params);
        }

        void startDotAnimation() {
            stopDotAnimation();
            float bounce = itemView.getResources().getDisplayMetrics().density * -5f;
            View[] dots   = {dot1, dot2, dot3};
            int[]  delays = {0, 150, 300};
            for (int i = 0; i < 3; i++) {
                ObjectAnimator anim = ObjectAnimator.ofFloat(
                        dots[i], View.TRANSLATION_Y, 0f, bounce);
                anim.setDuration(450);
                anim.setStartDelay(delays[i]);
                anim.setRepeatCount(ValueAnimator.INFINITE);
                anim.setRepeatMode(ValueAnimator.REVERSE);
                anim.setInterpolator(new AccelerateDecelerateInterpolator());
                anim.start();
                dotAnimators.add(anim);
            }
        }

        void stopDotAnimation() {
            for (ObjectAnimator a : dotAnimators) a.cancel();
            dotAnimators.clear();
            dot1.setTranslationY(0f);
            dot2.setTranslationY(0f);
            dot3.setTranslationY(0f);
        }
    }

    /** Converts **bold** markdown to bold spans. Unrecognised syntax passes through unchanged. */
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*", Pattern.DOTALL);

    static CharSequence renderMarkdown(String text) {
        if (text == null || !text.contains("**")) return text;
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        Matcher m = BOLD_PATTERN.matcher(text);
        int offset = 0;
        while (m.find()) {
            int start = m.start() - offset;
            int end   = m.end()   - offset;
            String inner = m.group(1);
            ssb.replace(start, end, inner);
            ssb.setSpan(new StyleSpan(Typeface.BOLD), start, start + inner.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            offset += 4; // removed 2 opening + 2 closing asterisks
        }
        return ssb;
    }
}
