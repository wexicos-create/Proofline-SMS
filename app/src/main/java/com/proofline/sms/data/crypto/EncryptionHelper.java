package com.proofline.sms.data.crypto;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * EncryptionHelper - Handles end-to-end encryption using AES-256-GCM
 * 
 * Security Features:
 * - AES-256-GCM encryption (authenticated encryption)
 * - Android KeyStore for key storage
 * - 96-bit random IV for each encryption
 * - Authentication tag for integrity verification
 * 
 * @author Proofline SMS
 * @version 1.0
 */
public class EncryptionHelper {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final int KEY_SIZE = 256;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12; // 96 bits for GCM
    
    private static final String KEY_ALIAS = "ProoflineSMSKey";
    private final Context context;
    private SecretKey secretKey;
    
    public EncryptionHelper(Context context) {
        this.context = context.getApplicationContext();
        initializeKey();
    }
    
    /**
     * Initializes or retrieves the encryption key from Android KeyStore
     */
    private void initializeKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            
            // Check if key already exists
            if (keyStore.containsAlias(KEY_ALIAS)) {
                secretKey = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
            } else {
                // Generate new key
                secretKey = generateKey();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption key", e);
        }
    }
    
    /**
     * Generates a new AES-256 key using Android KeyStore
     */
    private SecretKey generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .setRandomizedEncryptionRequired(true)
                .build();
        
        keyGenerator.init(spec);
        return keyGenerator.generateKey();
    }
    
    /**
     * Encrypts plaintext message using AES-256-GCM
     * 
     * Format: IV (12 bytes) + Ciphertext + AuthTag (16 bytes)
     * 
     * @param plaintext Message to encrypt
     * @return Base64-encoded encrypted data
     * @throws Exception if encryption fails
     */
    public String encrypt(String plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        
        byte[] iv = cipher.getIV();
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        
        // Combine IV + Ciphertext
        ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        buffer.put(iv);
        buffer.put(ciphertext);
        
        return Base64.getEncoder().encodeToString(buffer.array());
    }
    
    /**
     * Decrypts Base64-encoded ciphertext using AES-256-GCM
     * 
     * @param encryptedData Base64-encoded encrypted data
     * @return Decrypted plaintext
     * @throws Exception if decryption fails
     */
    public String decrypt(String encryptedData) throws Exception {
        byte[] decodedData = Base64.getDecoder().decode(encryptedData);
        
        ByteBuffer buffer = ByteBuffer.wrap(decodedData);
        byte[] iv = new byte[IV_LENGTH_BYTES];
        buffer.get(iv);
        
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
        
        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }
    
    /**
     * Generates a random symmetric key for temporary encryption
     * Used for creating shareable encrypted links
     */
    public static String generateRandomKey() {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[32]; // 256 bits
        random.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
    
    /**
     * Encrypts data with a specific key (for link-based encryption)
     */
    public static String encryptWithKey(String plaintext, String keyBase64) throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode(keyBase64);
        SecretKey key = new javax.crypto.spec.SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        
        byte[] iv = cipher.getIV();
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        
        ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        buffer.put(iv);
        buffer.put(ciphertext);
        
        return Base64.getEncoder().encodeToString(buffer.array());
    }
    
    /**
     * Decrypts data with a specific key
     */
    public static String decryptWithKey(String encryptedData, String keyBase64) throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode(keyBase64);
        SecretKey key = new javax.crypto.spec.SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
        
        byte[] decodedData = Base64.getDecoder().decode(encryptedData);
        ByteBuffer buffer = ByteBuffer.wrap(decodedData);
        
        byte[] iv = new byte[IV_LENGTH_BYTES];
        buffer.get(iv);
        
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        
        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }
}
