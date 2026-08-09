package com.proofline.sms.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.proofline.sms.R;
import com.proofline.sms.data.model.Message;
import com.proofline.sms.util.LinkHandler;

/**
 * MainActivity - Main entry point of Proofline SMS
 *
 * Responsibilities:
 * - Display conversation list
 * - Handle deep link intents
 * - Navigate to chat/compose activities
 * - Manage application lifecycle
 *
 * @author Proofline SMS
 * @version 1.0
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ListView conversationList;
    private Button composeButton;
    private LinkHandler linkHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        conversationList = findViewById(R.id.conversation_list);
        composeButton = findViewById(R.id.compose_button);

        // Initialize link handler
        linkHandler = new LinkHandler(this);

        // Set up compose button
        composeButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, com.proofline.sms.ui.compose.ComposeActivity.class);
            startActivity(intent);
        });

        // Handle incoming intents
        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    /**
     * Handles incoming deep link intents
     */
    private void handleIncomingIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        // Check if this is a deep link
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            if (uri != null) {
                handleSecureLink(uri);
            }
        }
    }

    /**
     * Processes secure link and displays message
     */
    private void handleSecureLink(Uri uri) {
        try {
            Log.d(TAG, "Handling secure link: " + uri);

            // Process the link
            Message message = linkHandler.handleSecureLink(uri);

            if (message != null) {
                // Successfully decrypted message
                Toast.makeText(this, "Message received from " + message.getSender(), Toast.LENGTH_SHORT).show();

                // Open chat activity with message
                Intent chatIntent = new Intent(MainActivity.this, com.proofline.sms.ui.chat.ChatActivity.class);
                chatIntent.putExtra("message", message);
                startActivity(chatIntent);
            } else {
                Toast.makeText(this, "Failed to process message", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling secure link: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
