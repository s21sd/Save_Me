package com.mobile.saveme;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {
    private List<Message> messageList;

    public ChatAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        Message message = messageList.get(position);

        holder.messageTextView.setText(message.getMessage());

        // Customize the message view according to whether it is sent or received
        if (message.isSent()) {
            holder.messageTextView.setBackgroundResource(R.drawable.message_background_sent);
            ((LinearLayout.LayoutParams) holder.messageTextView.getLayoutParams()).gravity = Gravity.END;
        } else {
            holder.messageTextView.setBackgroundResource(R.drawable.message_background_received);
            ((LinearLayout.LayoutParams) holder.messageTextView.getLayoutParams()).gravity = Gravity.START;
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;

        MessageViewHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }
    }
}
