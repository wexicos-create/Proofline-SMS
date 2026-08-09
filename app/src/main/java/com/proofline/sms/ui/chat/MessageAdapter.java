package com.proofline.sms.ui.chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.proofline.sms.R;
import com.proofline.sms.data.model.Message;

import java.util.List;

/**
 * MessageAdapter - Displays messages in chat list
 *
 * @author Proofline SMS
 * @version 1.0
 */
public class MessageAdapter extends ArrayAdapter<Message> {

    private final Context context;
    private final List<Message> messages;
    private static final int SENT_VIEW_TYPE = 0;
    private static final int RECEIVED_VIEW_TYPE = 1;

    public MessageAdapter(Context context, List<Message> messages) {
        super(context, 0, messages);
        this.context = context;
        this.messages = messages;
    }

    @Override
    public int getViewTypeCount() {
        return 2; // Sent and received
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isReceived() ? RECEIVED_VIEW_TYPE : SENT_VIEW_TYPE;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Message message = messages.get(position);
        int viewType = getItemViewType(position);

        if (convertView == null) {
            int layoutId = viewType == SENT_VIEW_TYPE ? R.layout.item_message_sent : R.layout.item_message_received;
            convertView = LayoutInflater.from(context).inflate(layoutId, parent, false);
        }

        TextView messageText = convertView.findViewById(R.id.message_text);
        TextView messageTime = convertView.findViewById(R.id.message_time);
        TextView senderName = convertView.findViewById(R.id.sender_name);

        messageText.setText(message.getContent());
        messageTime.setText(message.getShortTimestamp());
        if (senderName != null) {
            senderName.setText(message.getSender());
        }

        return convertView;
    }
}
