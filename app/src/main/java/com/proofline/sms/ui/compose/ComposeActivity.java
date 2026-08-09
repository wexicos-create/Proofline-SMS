package com.proofline.sms.ui.compose;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.proofline.sms.R;
import com.proofline.sms.data.model.Message;

import java.util.Date;

/**
 * ComposeActivity - Compose and send secure messages
 *
 * Features:
 * - Compose new messages
 * - Generate secure links
 * - Copy link to clipboard
 * - Share via various methods
 *
 * @author Proofline SMS
 * @version 1.0
 */
public class ComposeActivity extends AppCompatActivity {

    private static final String TAG = "ComposeActivity";
    private EditText messageContent;
    private EditText recipientField;
    private Button generateLinkButton;
    private Button shareButton;
    private Button copyButton;
    private TextView generatedLink;
    private ClipboardManager clipboardManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        // Initialize views
        messageContent = findViewById(R.id.message_content);
        recipientField = findViewById(R.id.recipient_field);
        generateLinkButton = findViewById(R.id.generate_link_button);
        shareButton = findViewById(R.id.share_button);
        copyButton = findViewById(R.id.copy_button);
        generatedLink = findViewById(R.id.generated_link);

        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        // Generate link button listener
        generateLinkButton.setOnClickListener(v -> generateSecureLink());

        // Share button listener
        shareButton.setOnClickListener(v -> shareLink());

        // Copy button listener
        copyButton.setOnClickListener(v -> copyLinkToClipboard());
    }

    /**
     * Generates a secure link from message content
     */
    private void generateSecureLink() {
        String content = messageContent.getText().toString().trim();
        String recipient = recipientField.getText().toString().trim();

        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create message
            Message message = new Message();
            message.setContent(content);
            message.setSender(recipient.isEmpty() ? "Anonymous" : recipient);
            message.setTimestamp(new Date());

            // Generate secure link
            String secureLink = SecureLinkGenerator.generateSecureLink(message, null);

            // Display generated link
            generatedLink.setText(secureLink);
            generatedLink.setVisibility(android.view.View.VISIBLE);

            Toast.makeText(this, "Secure link generated", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Generated link: " + secureLink);

        } catch (Exception e) {
            Log.e(TAG, "Error generating link: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Copies generated link to clipboard
     */
    private void copyLinkToClipboard() {
        String link = generatedLink.getText().toString();

        if (TextUtils.isEmpty(link)) {
            Toast.makeText(this, "Please generate a link first", Toast.LENGTH_SHORT).show();
            return;
        }

        android.content.ClipData clip = android.content.ClipData.newPlainText("Proofline Link", link);
        clipboardManager.setPrimaryClip(clip);

        Toast.makeText(this, "Link copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    /**
     * Shares generated link via intent
     */
    private void shareLink() {
        String link = generatedLink.getText().toString();

        if (TextUtils.isEmpty(link)) {
            Toast.makeText(this, "Please generate a link first", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Encrypted message: " + link);
        startActivity(Intent.createChooser(shareIntent, "Share Secure Message"));
    }
}
