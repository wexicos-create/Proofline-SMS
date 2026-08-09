# Proofline SMS - Testing Guide

## 🧪 Unit Tests

### Test Encryption

```java
package com.proofline.sms.data.crypto;

import android.content.Context;
import androidx.test.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class EncryptionHelperTest {
    
    private Context context;
    private EncryptionHelper encryptionHelper;
    
    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        encryptionHelper = new EncryptionHelper(context);
    }
    
    @Test
    public void testEncryptionAndDecryption() throws Exception {
        String plaintext = "Hello, Proofline!";
        
        String ciphertext = encryptionHelper.encrypt(plaintext);
        String decrypted = encryptionHelper.decrypt(ciphertext);
        
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    public void testDifferentCiphertextsForSamePlaintext() throws Exception {
        String plaintext = "Same message";
        
        String cipher1 = encryptionHelper.encrypt(plaintext);
        String cipher2 = encryptionHelper.encrypt(plaintext);
        
        assertNotEquals(cipher1, cipher2);  // Different IVs
    }
    
    @Test
    public void testEncryptionWithCustomKey() throws Exception {
        String key = EncryptionHelper.generateRandomKey();
        String plaintext = "Secret";
        
        String ciphertext = EncryptionHelper.encryptWithKey(plaintext, key);
        String decrypted = EncryptionHelper.decryptWithKey(ciphertext, key);
        
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    public void testWrongKeyDecryptionFails() throws Exception {
        String key1 = EncryptionHelper.generateRandomKey();
        String key2 = EncryptionHelper.generateRandomKey();
        String plaintext = "Secret";
        
        String ciphertext = EncryptionHelper.encryptWithKey(plaintext, key1);
        
        assertThrows(Exception.class, () -> {
            EncryptionHelper.decryptWithKey(ciphertext, key2);
        });
    }
    
    @Test
    public void testTamperedCiphertextFails() throws Exception {
        String plaintext = "Important";
        String ciphertext = encryptionHelper.encrypt(plaintext);
        
        // Tamper with ciphertext
        String tampered = ciphertext.substring(0, ciphertext.length() - 5) + "XXXXX";
        
        assertThrows(Exception.class, () -> {
            encryptionHelper.decrypt(tampered);
        });
    }
    
    @Test
    public void testLongMessageEncryption() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("This is a long message to test encryption. ");
        }
        String plaintext = sb.toString();
        
        String ciphertext = encryptionHelper.encrypt(plaintext);
        String decrypted = encryptionHelper.decrypt(ciphertext);
        
        assertEquals(plaintext, decrypted);
    }
}
```

### Test Secure Link Generation

```java
package com.proofline.sms.ui.compose;

import android.net.Uri;
import com.proofline.sms.data.model.Message;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Date;

public class SecureLinkGeneratorTest {
    
    @Test
    public void testGenerateSecureLink() {
        Message message = new Message();
        message.setContent("Test message");
        message.setSender("Alice");
        
        String link = SecureLinkGenerator.generateSecureLink(message, null);
        
        assertTrue(link.contains("proofline://secure-link"));
        assertTrue(link.contains("message="));
        assertTrue(link.contains("key="));
        assertTrue(link.contains("id="));
    }
    
    @Test
    public void testValidateSecureLink() {
        Message message = new Message();
        message.setContent("Test");
        
        String link = SecureLinkGenerator.generateSecureLink(message, null);
        Uri uri = Uri.parse(link);
        
        assertTrue(SecureLinkGenerator.isValidSecureLink(uri));
    }
    
    @Test
    public void testExtractLinkParameters() {
        Message message = new Message();
        message.setContent("Test message");
        message.setSender("Bob");
        
        String link = SecureLinkGenerator.generateSecureLink(message, null);
        Uri uri = Uri.parse(link);
        
        SecureLinkGenerator.LinkParameters params = 
            SecureLinkGenerator.extractLinkParameters(uri);
        
        assertNotNull(params.encryptedMessage);
        assertNotNull(params.encryptionKey);
        assertNotNull(params.messageId);
        assertEquals("Bob", params.sender);
    }
    
    @Test
    public void testInvalidLinkRejected() {
        Uri invalidUri = Uri.parse("http://example.com/test");
        assertFalse(SecureLinkGenerator.isValidSecureLink(invalidUri));
    }
}
```

### Test Link Handler

```java
package com.proofline.sms.util;

import android.content.Context;
import android.net.Uri;
import androidx.test.InstrumentationRegistry;
import com.proofline.sms.data.model.Message;
import com.proofline.sms.ui.compose.SecureLinkGenerator;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class LinkHandlerTest {
    
    private Context context;
    private LinkHandler linkHandler;
    
    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        linkHandler = new LinkHandler(context);
    }
    
    @Test
    public void testHandleSecureLink() {
        Message originalMessage = new Message();
        originalMessage.setContent("Secret message");
        originalMessage.setSender("Alice");
        
        String link = SecureLinkGenerator.generateSecureLink(originalMessage, null);
        Uri uri = Uri.parse(link);
        
        Message receivedMessage = linkHandler.handleSecureLink(uri);
        
        assertNotNull(receivedMessage);
        assertEquals(originalMessage.getContent(), receivedMessage.getContent());
        assertEquals(originalMessage.getSender(), receivedMessage.getSender());
        assertTrue(receivedMessage.isReceived());
        assertTrue(receivedMessage.isEncrypted());
    }
    
    @Test
    public void testValidateLinkIntegrity() {
        Message message = new Message();
        message.setContent("Test");
        
        String link = SecureLinkGenerator.generateSecureLink(message, null);
        Uri uri = Uri.parse(link);
        
        assertTrue(LinkHandler.validateLinkIntegrity(uri));
    }
    
    @Test
    public void testInvalidLinkIntegrity() {
        Uri invalidUri = Uri.parse("proofline://secure-link?invalid=params");
        assertFalse(LinkHandler.validateLinkIntegrity(invalidUri));
    }
    
    @Test
    public void testExtractSenderInfo() {
        Message message = new Message();
        message.setContent("Test");
        message.setSender("Charlie");
        
        String link = SecureLinkGenerator.generateSecureLink(message, null);
        Uri uri = Uri.parse(link);
        
        String sender = LinkHandler.extractSenderInfo(uri);
        assertEquals("Charlie", sender);
    }
    
    @Test
    public void testFormatTimestamp() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String formatted = LinkHandler.formatTimestamp(timestamp);
        
        assertNotNull(formatted);
        assertFalse(formatted.isEmpty());
        assertNotEquals("Invalid time", formatted);
    }
}
```

## 🧪 Integration Tests

```java
public class ProoflineIntegrationTest {
    
    private Context context;
    private EncryptionHelper encryptionHelper;
    private LinkHandler linkHandler;
    
    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        encryptionHelper = new EncryptionHelper(context);
        linkHandler = new LinkHandler(context);
    }
    
    @Test
    public void testEndToEndMessaging() {
        // Alice composes message
        Message originalMessage = new Message();
        originalMessage.setContent("Hello Bob, this is Alice!");
        originalMessage.setSender("Alice");
        
        // Generate secure link
        String secureLink = SecureLinkGenerator.generateSecureLink(originalMessage, null);
        
        // Bob receives and opens link
        Uri uri = Uri.parse(secureLink);
        Message receivedMessage = linkHandler.handleSecureLink(uri);
        
        // Verify integrity
        assertEquals(originalMessage.getContent(), receivedMessage.getContent());
        assertEquals(originalMessage.getSender(), receivedMessage.getSender());
        assertTrue(receivedMessage.isEncrypted());
    }
    
    @Test
    public void testMultipleMessagesWithDifferentKeys() {
        Message msg1 = new Message();
        msg1.setContent("Message 1");
        
        Message msg2 = new Message();
        msg2.setContent("Message 2");
        
        String link1 = SecureLinkGenerator.generateSecureLink(msg1, null);
        String link2 = SecureLinkGenerator.generateSecureLink(msg2, null);
        
        // Each message should have different encryption key
        assertNotEquals(link1, link2);
        
        Message received1 = linkHandler.handleSecureLink(Uri.parse(link1));
        Message received2 = linkHandler.handleSecureLink(Uri.parse(link2));
        
        assertEquals("Message 1", received1.getContent());
        assertEquals("Message 2", received2.getContent());
    }
}
```

## 🏃 Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests EncryptionHelperTest

# Run with coverage
./gradlew testDebugUnitTest --coverage

# Run instrumented tests (on device/emulator)
./gradlew connectedAndroidTest
```

## 📊 Coverage Goals

- **EncryptionHelper**: 95%+ coverage
- **SecureLinkGenerator**: 90%+ coverage
- **LinkHandler**: 90%+ coverage
- **Message model**: 85%+ coverage

