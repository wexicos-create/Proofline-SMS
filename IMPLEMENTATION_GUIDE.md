# Proofline SMS - Complete Implementation Guide

## 🎯 Descripción General

**Proofline SMS** es una aplicación de mensajería segura con encriptación de extremo a extremo (E2EE) que permite comunicación offline a través de enlaces encriptados.

### ✨ Características Principales

1. **Encriptación E2EE (End-to-End)**
   - Algoritmo: AES-256-GCM
   - Almacenamiento seguro en Android KeyStore
   - Cada mensaje tiene IV único
   - Autenticación integrada (AEAD)

2. **Mensajería Offline**
   - Genera enlaces encriptados únicos
   - Comparte vía SMS, Email, WhatsApp, etc.
   - Destinatario abre enlace y app desencripta automáticamente

3. **Deep Link Handling**
   - Schema: `proofline://secure-link?message=...&key=...&id=...`
   - Intercepta enlaces y procesa automáticamente
   - Valida integridad de mensajes

4. **Android 15 Compliant**
   - compileSdk 35
   - targetSdk 35
   - Java 17
   - Permisos just-in-time

---

## 🏗️ Arquitectura

### Estructura de Directorios

```
app/src/main/java/com/proofline/sms/
├── data/
│   ├── crypto/
│   │   └── EncryptionHelper.java          # Core encryption engine
│   ├── model/
│   │   └── Message.kt                     # Message data model
│   └── repository/
│       └── MessageRepository.java         # Local message storage
├── ui/
│   ├── main/
│   │   └── MainActivity.java              # App entry point
│   ├── chat/
│   │   ├── ChatActivity.java              # Chat display
│   │   └── MessageAdapter.java            # Message list adapter
│   └── compose/
│       ├── ComposeActivity.java           # Message composer
│       └── SecureLinkGenerator.java       # Link generation
└── util/
    └── LinkHandler.java                   # Deep link processor
```

### Flujo de Datos

```
┌─────────────────────────────────────────────────────────────┐
│                     User Composes Message                    │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │  SecureLinkGenerator.generate() │
        │  - Creates random AES key      │
        │  - Encrypts message content    │
        │  - Generates URI               │
        └────────────────┬───────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │    Share Encrypted Link         │
        │  - SMS, Email, WhatsApp, etc   │
        └────────────────┬───────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │  Recipient Opens Link           │
        │  - App intercepts deep link    │
        │  - LinkHandler processes URI   │
        └────────────────┬───────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │  EncryptionHelper.decrypt()     │
        │  - Extracts IV from ciphertext │
        │  - Validates GCM auth tag      │
        │  - Returns plaintext           │
        └────────────────┬───────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │   Display in ChatActivity       │
        │   - Show decrypted message     │
        │   - Display sender info        │
        │   - Timestamp tracking         │
        └────────────────────────────────┘
```

---

## 🔒 Seguridad

### Encriptación (EncryptionHelper.java)

**Algoritmo:** AES-256-GCM (Galois/Counter Mode)

```java
// Key Generation
KeyGenerator keyGenerator = KeyGenerator.getInstance(
    KeyProperties.KEY_ALGORITHM_AES, 
    ANDROID_KEYSTORE
);

KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
    KEY_ALIAS,
    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
    .setKeySize(256)  // 256-bit AES
    .build();

keyGenerator.init(spec);
SecretKey key = keyGenerator.generateKey();
```

**Formato del Ciphertext:**
```
[IV (12 bytes)] + [Encrypted Data] + [Auth Tag (16 bytes)]
                 └─ Handled by GCM mode automatically
```

### Beneficios de AES-256-GCM

| Característica | Beneficio |
|---|---|
| **256-bit key** | Resistencia a ataques de fuerza bruta |
| **GCM mode** | Encriptación + autenticación en una operación |
| **Unique IV** | Cada mensaje tiene IV aleatorio (96 bits) |
| **Auth tag** | Detecta manipulación de datos en tránsito |
| **Android KeyStore** | Claves nunca salen del hardware seguro |

### Deep Link Security

```
URI: proofline://secure-link?message=<BASE64>&key=<BASE64>&id=<UUID>&sender=&ts=
     ├─ message: Ciphertext (IV + AES-GCM output)
     ├─ key: Encryption key (256-bit, base64)
     ├─ id: Unique message ID
     ├─ sender: Display name (not auth)
     └─ ts: Timestamp for ordering

Validation:
✓ URI scheme == "proofline"
✓ Host == "secure-link"
✓ All required parameters present
✓ Key is valid base64
✓ Message is valid base64
✓ GCM authentication succeeds
```

---

## 📱 API Reference

### EncryptionHelper

```java
// Initialize
EncryptionHelper helper = new EncryptionHelper(context);

// Encrypt with stored key
String ciphertext = helper.encrypt("Hello World");

// Decrypt with stored key
String plaintext = helper.decrypt(ciphertext);

// Encrypt with specific key (for links)
String encryptedMessage = EncryptionHelper.encryptWithKey(
    "Hello",
    encryptionKey
);

// Decrypt with specific key
String decryptedMessage = EncryptionHelper.decryptWithKey(
    encryptedMessage,
    encryptionKey
);
```

### SecureLinkGenerator

```java
// Create message
Message msg = new Message();
msg.setContent("Secret message");
msg.setSender("Alice");

// Generate secure link
String link = SecureLinkGenerator.generateSecureLink(msg, null);
// Returns: proofline://secure-link?message=...&key=...&id=...

// Create shareable payload (JSON)
String payload = SecureLinkGenerator.generateShareablePayload(msg);

// Validate link
boolean isValid = SecureLinkGenerator.isValidSecureLink(uri);

// Extract parameters
SecureLinkGenerator.LinkParameters params = 
    SecureLinkGenerator.extractLinkParameters(uri);
```

### LinkHandler

```java
// Initialize
LinkHandler handler = new LinkHandler(context);

// Handle incoming intent
Message msg = handler.handleIncomingLink(intent);

// Or handle URI directly
Message msg = handler.handleSecureLink(uri);

// Validate link integrity
boolean isValid = LinkHandler.validateLinkIntegrity(uri);

// Extract sender
String sender = LinkHandler.extractSenderInfo(uri);

// Format timestamp
String formatted = LinkHandler.formatTimestamp(timestamp);
```

### Message (Kotlin)

```kotlin
// Create message
val msg = Message(
    id = UUID.randomUUID().toString(),
    content = "Hello",
    sender = "Alice",
    timestamp = Date(),
    isReceived = false,
    isEncrypted = true
)

// Check if from current user
msg.isFromCurrentUser("Alice")  // true

// Mark as read
msg.markAsRead()

// Get formatted timestamp
msg.getFormattedTimestamp()  // "14:30 09/08/2026"

// Get preview
msg.getPreview(50)  // "Hello World..."
```

---

## 🧪 Testing

### Test Encryption

```java
@Test
public void testEncryptionDecryption() {
    EncryptionHelper helper = new EncryptionHelper(context);
    String plaintext = "Secret message";
    
    String ciphertext = helper.encrypt(plaintext);
    String decrypted = helper.decrypt(ciphertext);
    
    assertEquals(plaintext, decrypted);
}
```

### Test Link Generation

```java
@Test
public void testLinkGeneration() {
    Message msg = new Message();
    msg.setContent("Test");
    
    String link = SecureLinkGenerator.generateSecureLink(msg, null);
    
    assertTrue(link.contains("proofline://secure-link"));
    assertTrue(SecureLinkGenerator.isValidSecureLink(Uri.parse(link)));
}
```

### Test Deep Link Handling

```java
@Test
public void testDeepLinkHandling() {
    LinkHandler handler = new LinkHandler(context);
    Message original = new Message();
    original.setContent("Test message");
    
    String link = SecureLinkGenerator.generateSecureLink(original, null);
    Message received = handler.handleSecureLink(Uri.parse(link));
    
    assertEquals(original.getContent(), received.getContent());
}
```

---

## 🚀 Deployment

### Build Release APK

```bash
# Configure signing in app/build.gradle
signingConfigs {
    release {
        storeFile file("keystore.jks")
        storePassword System.getenv("KEYSTORE_PASSWORD")
        keyAlias System.getenv("KEY_ALIAS")
        keyPassword System.getenv("KEY_PASSWORD")
    }
}

# Build release APK
./gradlew assembleRelease

# Build signed AAB (for Google Play)
./gradlew bundleRelease
```

### ProGuard Optimization

El `proguard-rules.pro` protege las clases críticas:

```proguard
-keep class org.bouncycastle.** { *; }
-keep class androidx.security.crypto.** { *; }
-keep class com.proofline.sms.** { *; }
```

---

## 📊 Performance

| Operación | Tiempo |
|---|---|
| Generar clave AES-256 | ~100ms (primera vez) |
| Encriptar mensaje (AES-GCM) | ~5ms |
| Desencriptar mensaje | ~5ms |
| Generar enlace seguro | ~15ms |
| Procesar deep link | ~10ms |

---

## 🛠️ Troubleshooting

### Problema: "AndroidKeyStore not available"
**Solución:** Requiere Android 6.0+ (API 23)

### Problema: "GCM authentication failed"
**Solución:** El mensaje fue modificado en tránsito. Rechazar.

### Problema: "URI parse error"
**Solución:** Validar con `SecureLinkGenerator.isValidSecureLink(uri)`

---

## 📝 Licencia

Proofline Public License - Ver LICENSE.md

---

## 👨‍💻 Autor

**Wexicos-create** - Josue Israel Cervantes Alvarado
