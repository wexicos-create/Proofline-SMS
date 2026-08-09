package com.proofline.sms.ui.compose;

import android.net.Uri;
import android.util.Log;

import com.proofline.sms.data.crypto.EncryptionHelper;
import com.proofline.sms.data.model.Message;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * SecureLinkGenerator - Generates encrypted secure links for offline messaging
 * 
 * Link Format: proofline://secure-link?message=<encrypted_message>&key=<encryption_key>&id=<message_id>
 * 
 * Features:
 * - Generates unique message IDs
 * - Creates encrypted message payloads
 * - Generates shareable links
 * - Supports metadata embedding
 * 
 * @author Proofline SMS
 * @version 1.0
 */
public class SecureLinkGenerator {
    
    private static final String TAG = "SecureLinkGenerator";
    private static final String SCHEME = "proofline";
    private static final String HOST = "secure-link";
    private static final String PARAM_MESSAGE = "message";
    private static final String PARAM_KEY = "key";
    private static final String PARAM_ID = "id";
    private static final String PARAM_SENDER = "sender";
    private static final String PARAM_TIMESTAMP = "ts";
    
    /**
     * Generates a secure link for a message
     * 
     * @param message The message to encode
     * @param recipientPublicKey Recipient's public key (optional)
     * @return Secure link URI
     */
    public static String generateSecureLink(Message message, String recipientPublicKey) {
        try {
            // Generate encryption key for this link
            String encryptionKey = EncryptionHelper.generateRandomKey();
            
            // Create message payload
            String messagePayload = message.getContent();
            
            // Encrypt message with generated key
            String encryptedMessage = EncryptionHelper.encryptWithKey(messagePayload, encryptionKey);
            
            // URL encode parameters
            String encodedMessage = URLEncoder.encode(encryptedMessage, StandardCharsets.UTF_8.name());
            String encodedKey = URLEncoder.encode(encryptionKey, StandardCharsets.UTF_8.name());
            String messageId = message.getId() != null ? message.getId() : UUID.randomUUID().toString();
            String timestamp = String.valueOf(System.currentTimeMillis());
            
            // Build URI
            Uri.Builder builder = new Uri.Builder()
                    .scheme(SCHEME)
                    .authority(HOST)
                    .appendQueryParameter(PARAM_MESSAGE, encodedMessage)
                    .appendQueryParameter(PARAM_KEY, encodedKey)
                    .appendQueryParameter(PARAM_ID, messageId)
                    .appendQueryParameter(PARAM_SENDER, message.getSender() != null ? message.getSender() : "Anonymous")
                    .appendQueryParameter(PARAM_TIMESTAMP, timestamp);
            
            String link = builder.build().toString();
            Log.d(TAG, "Generated secure link: " + link);
            
            return link;
            
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error encoding link", e);
            throw new RuntimeException("Failed to generate secure link", e);
        } catch (Exception e) {
            Log.e(TAG, "Error generating secure link", e);
            throw new RuntimeException("Failed to encrypt message for link", e);
        }
    }
    
    /**
     * Creates a complete shareable message with QR code data
     * 
     * @param message The message to share
     * @return JSON-formatted shareable payload
     */
    public static String generateShareablePayload(Message message) {
        try {
            String secureLink = generateSecureLink(message, null);
            
            // Create JSON payload for easy sharing
            String payload = "{"
                    + "\"link\":\"" + secureLink + "\","
                    + "\"messageId\":\"" + (message.getId() != null ? message.getId() : UUID.randomUUID().toString()) + "\","
                    + "\"sender\":\"" + (message.getSender() != null ? message.getSender() : "Anonymous") + "\","
                    + "\"timestamp\":\"" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date()) + "\","
                    + "\"type\":\"secure-message\""
                    + "}";
            
            Log.d(TAG, "Generated shareable payload");
            return payload;
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating shareable payload", e);
            throw new RuntimeException("Failed to generate payload", e);
        }
    }
    
    /**
     * Validates if a URI is a valid Proofline secure link
     */
    public static boolean isValidSecureLink(Uri uri) {
        return uri != null
                && SCHEME.equals(uri.getScheme())
                && HOST.equals(uri.getAuthority())
                && uri.getQueryParameter(PARAM_MESSAGE) != null
                && uri.getQueryParameter(PARAM_KEY) != null;
    }
    
    /**
     * Extracts parameters from a secure link
     */
    public static LinkParameters extractLinkParameters(Uri uri) {
        if (!isValidSecureLink(uri)) {
            throw new IllegalArgumentException("Invalid secure link");
        }
        
        return new LinkParameters(
                uri.getQueryParameter(PARAM_MESSAGE),
                uri.getQueryParameter(PARAM_KEY),
                uri.getQueryParameter(PARAM_ID),
                uri.getQueryParameter(PARAM_SENDER),
                uri.getQueryParameter(PARAM_TIMESTAMP)
        );
    }
    
    /**
     * Inner class to hold link parameters
     */
    public static class LinkParameters {
        public String encryptedMessage;
        public String encryptionKey;
        public String messageId;
        public String sender;
        public String timestamp;
        
        public LinkParameters(String encryptedMessage, String encryptionKey, String messageId, String sender, String timestamp) {
            this.encryptedMessage = encryptedMessage;
            this.encryptionKey = encryptionKey;
            this.messageId = messageId;
            this.sender = sender;
            this.timestamp = timestamp;
        }
    }
}
