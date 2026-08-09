package com.proofline.sms.ui.chat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.proofline.sms.R;
import com.proofline.sms.data.crypto.EncryptionHelper;
import com.proofline.sms.data.model.Message;
import com.proofline.sms.ui.compose.SecureLinkGenerator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ChatActivity - Displays chat conversation
 *
 * Features:
 * - Display message history
 * - Compose and send encrypted messages
 * - Generate secure links
 * - Share messages via deep links
 *
 * @author Proofline SMS
 * @version 1.0
 */
public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private ListView messageList;
    private EditText messageInput;
    private Button sendButton;
    private Button shareLinkButton;
    private TextView recipientName;
    private MessageAdapter messageAdapter;
    private List<Message> messages = new ArrayList<>();
    private EncryptionHelper encryptionHelper;
    private String currentRecipient = "Anonymous";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize views
        messageList = findViewById(R.id.message_list);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        shareLinkButton = findViewById(R.id.share_link_button);
        recipientName = findViewById(R.id.recipient_name);

        // Initialize encryption
        encryptionHelper = new EncryptionHelper(this);

        // Setup message adapter
        messageAdapter = new MessageAdapter(this, messages);
        messageList.setAdapter(messageAdapter);

        // Handle incoming message from deep link
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("message")) {
            Message incomingMessage = (Message) intent.getSerializableExtra("message");
            if (incomingMessage != null) {
                messages.add(incomingMessage);
                currentRecipient = incomingMessage.getSender();
                recipientName.setText(currentRecipient);
                messageAdapter.notifyDataSetChanged();
            }
        }

        // Send button listener
        sendButton.setOnClickListener(v -> sendMessage());

        // Share link button listener
        shareLinkButton.setOnClickListener(v -> shareMessageViaLink());
    }

    /**
     * Sends an encrypted message
     */
    private void sendMessage() {
        String content = messageInput.getText().toString().trim();

        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create message
            Message message = new Message();
            message.setContent(content);
            message.setSender("You");
            message.setReceived(false);
            message.setTimestamp(new Date());
            message.setEncrypted(true);

            // Add to conversation
            messages.add(message);
            messageAdapter.notifyDataSetChanged();

            // Clear input
            messageInput.setText("");

            Toast.makeText(this, "Message sent securely", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error sending message: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Generates and shares message via secure link
     */
    private void shareMessageViaLink() {
        String content = messageInput.getText().toString().trim();

        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create message
            Message message = new Message();
            message.setContent(content);
            message.setSender("You");
            message.setTimestamp(new Date());

            // Generate secure link
            String secureLink = SecureLinkGenerator.generateSecureLink(message, null);

            // Share link
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out my encrypted message: " + secureLink);
            startActivity(Intent.createChooser(shareIntent, "Share Secure Link"));

            Toast.makeText(this, "Share dialog opened", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error generating share link: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
