package com.spokeneasy.app.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.spokeneasy.app.R;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 0;
    private static final int TYPE_AI = 1;

    private List<ChatMessage> messages = new ArrayList<>();
    private TtsPlayCallback ttsCallback;

    public interface TtsPlayCallback {
        void onPlayTts(String text);
    }

    public ChatAdapter(TtsPlayCallback ttsCallback) {
        this.ttsCallback = ttsCallback;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getRole() == ChatMessage.Role.USER ? TYPE_USER : TYPE_AI;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) {
            View view = inflater.inflate(R.layout.item_chat_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_chat_ai, parent, false);
            return new AiViewHolder(view, ttsCallback);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).bind(msg);
        } else if (holder instanceof AiViewHolder) {
            ((AiViewHolder) holder).bind(msg);
        }
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.chat_user_text);
        }

        void bind(ChatMessage msg) {
            textView.setText(msg.getContent());
        }
    }

    static class AiViewHolder extends RecyclerView.ViewHolder {
        private final TextView replyText;
        private final TextView correctionText;
        private final View correctionCard;
        private final ImageButton btnPlayTts;
        private final TtsPlayCallback ttsCallback;

        AiViewHolder(@NonNull View itemView, TtsPlayCallback callback) {
            super(itemView);
            this.ttsCallback = callback;
            replyText = itemView.findViewById(R.id.chat_ai_reply);
            correctionText = itemView.findViewById(R.id.chat_correction_text);
            correctionCard = itemView.findViewById(R.id.chat_correction_card);
            btnPlayTts = itemView.findViewById(R.id.btn_chat_play_tts);
        }

        void bind(ChatMessage msg) {
            // Show reply text
            String display = msg.getReplyText() != null ? msg.getReplyText() : msg.getContent();
            replyText.setText(display);

            // Show correction section if available
            if (msg.hasCorrections()) {
                correctionCard.setVisibility(View.VISIBLE);
                correctionText.setText(msg.getCorrectionContent());
            } else {
                correctionCard.setVisibility(View.GONE);
            }

            // TTS button
            String speakableText = msg.getReplyText() != null ? msg.getReplyText() : msg.getContent();
            if (speakableText != null && !speakableText.isEmpty()) {
                btnPlayTts.setVisibility(View.VISIBLE);
                btnPlayTts.setOnClickListener(v -> {
                    if (ttsCallback != null) {
                        ttsCallback.onPlayTts(speakableText);
                    }
                });
            } else {
                btnPlayTts.setVisibility(View.GONE);
            }
        }
    }
}
