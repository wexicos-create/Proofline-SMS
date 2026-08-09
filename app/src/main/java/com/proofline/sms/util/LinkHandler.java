package com.proofline.sms.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.proofline.sms.data.crypto.EncryptionHelper;
import com.proofline.sms.data.model.Message;
import com.proofline.sms.ui.compose.SecureLinkGenerator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * LinkHandler - Processes incoming secure links and decrypts messages
 * 
 * Responsibilities:
 * - Intercepts deep links from other apps
 * - Extracts and decrypts message content
 * - Creates Message objects from link data
 * - Validates link integrity
 * 
 * @author Proofline SMS
 * @version 1.0
 */
public class LinkHandler {
    
    private static final String TAG = "LinkHandler";
    private final Context context;
    private final EncryptionHelper encryptionHelper;
    
    public LinkHandler(Context context) {
        this.context = context.getApplicationContext();
        this.encryptionHelper = new EncryptionHelper(context);
    }
    
    /**
     * Handles incoming intent and extracts message from secure link
     * 
     * @param intent The intent containing the deep link
     * @return Decrypted Message object, or null if link is invalid
     */
    public Message handleIncomingLink(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return null;
        }
        
        // Check if this is a VIEW action (deep link)
        if (!Intent.ACTION_VIEW.equals(intent.getAction())) {
            return null;
        }
        
        Uri uri = intent.getData();
        return handleSecureLink(uri);
    }
    
    /**
     * Processes a secure link URI and decrypts the message
     * 
     * @param uri The secure link URI
     * @return Decrypted Message object
     */
    public Message handleSecureLink(Uri uri) {
        try {
            // Validate link format
            if (!SecureLinkGenerator.isValidSecureLink(uri)) {
                Log.w(TAG, "Invalid secure link format");
                return null;
            }
            
            // Extract link parameters
            SecureLinkGenerator.LinkParameters params = SecureLinkGenerator.extractLinkParameters(uri);
            
            // Decrypt message
            String decryptedContent = EncryptionHelper.decryptWithKey(params.encryptedMessage, params.encryptionKey);
            
            // Create message object
            Message message = new Message();
            message.setId(params.messageId);
            message.setContent(decryptedContent);
            message.setSender(params.sender != null ? params.sender : "Unknown");
            message.setReceived(true);
            message.setTimestamp(new Date());
            message.setEncrypted(true);
            
            // Parse timestamp if available
            if (params.timestamp != null) {
                try {
                    long ts = Long.parseLong(params.timestamp);
                    message.setTimestamp(new Date(ts));
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid timestamp in link");
                }
            }
            
            Log.d(TAG, "Successfully processed secure link");
            return message;
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing secure link: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Validates link integrity by checking encryption parameters
     */
    public static boolean validateLinkIntegrity(Uri uri) {
        try {
            if (!SecureLinkGenerator.isValidSecureLink(uri)) {
                return false;
            }
            
            SecureLinkGenerator.LinkParameters params = SecureLinkGenerator.extractLinkParameters(uri);
            
            // Check if all required parameters are present
            return params.encryptedMessage != null && !params.encryptedMessage.isEmpty()
                    && params.encryptionKey != null && !params.encryptionKey.isEmpty();
                    
        } catch (Exception e) {
            Log.e(TAG, "Error validating link integrity", e);
            return false;
        }
    }
    
    /**
     * Extracts sender information from link
     */
    public static String extractSenderInfo(Uri uri) {
        try {
            if (SecureLinkGenerator.isValidSecureLink(uri)) {
                SecureLinkGenerator.LinkParameters params = SecureLinkGenerator.extractLinkParameters(uri);
                return params.sender != null ? params.sender : "Anonymous";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting sender info", e);
        }
        return "Unknown";
    }
    
    /**
     * Formats timestamp from link for display
     */
    public static String formatTimestamp(String timestamp) {
        try {
            if (timestamp == null || timestamp.isEmpty()) {
                return "Unknown time";
            }
            long ts = Long.parseLong(timestamp);
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return formatter.format(new Date(ts));
        } catch (Exception e) {
            Log.e(TAG, "Error formatting timestamp", e);
            return "Invalid time";
        }
    }
}
