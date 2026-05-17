package com.spokeneasy.app.dialogue;

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

public class RoleplayAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 0;
    private static final int TYPE_AI = 1;

    private List<DialogueViewModel.RoleplayMessage> messages = new ArrayList<>();
    private TtsCallback ttsCallback;

    public interface TtsCallback {
        void onPlayTts(String text);
    }

    public RoleplayAdapter(TtsCallback ttsCallback) {
        this.ttsCallback = ttsCallback;
    }

    public void setMessages(List<DialogueViewModel.RoleplayMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser ? TYPE_USER : TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_roleplay_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DialogueViewModel.RoleplayMessage msg = messages.get(position);
        MessageViewHolder vh = (MessageViewHolder) holder;

        if (msg.isUser) {
            vh.userCard.setVisibility(View.VISIBLE);
            vh.aiLayout.setVisibility(View.GONE);
            vh.userText.setText(msg.text);
        } else {
            vh.userCard.setVisibility(View.GONE);
            vh.aiLayout.setVisibility(View.VISIBLE);
            vh.aiText.setText(msg.text);
            vh.btnTts.setOnClickListener(v -> {
                if (ttsCallback != null) ttsCallback.onPlayTts(msg.text);
            });
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        final View userCard, aiLayout;
        final TextView userText, aiText;
        final ImageButton btnTts;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            userCard = itemView.findViewById(R.id.user_message_card);
            aiLayout = itemView.findViewById(R.id.ai_message_layout);
            userText = itemView.findViewById(R.id.user_message_text);
            aiText = itemView.findViewById(R.id.ai_message_text);
            btnTts = itemView.findViewById(R.id.btn_roleplay_tts);
        }
    }
}
