# Security Architecture - Proofline SMS

## 🔐 Security Model

### Threat Model

```
┌─────────────────────────────────────────────────────────────────┐
│                        Threat Analysis                           │
├─────────────────────────────────────────────────────────────────┤
│ Threat                    │ Impact  │ Mitigation                │
├───────────────────────────┼─────────┼───────────────────────────┤
│ Network eavesdropping     │ HIGH    │ AES-256-GCM encryption    │
│ Man-in-the-middle (MITM)  │ HIGH    │ GCM authentication tag    │
│ Message tampering         │ HIGH    │ AEAD integrity check      │
│ Key exposure              │ CRITICAL│ Android KeyStore          │
│ Link forgery              │ MEDIUM  │ URI validation            │
│ Replay attacks            │ MEDIUM  │ Unique IDs + timestamps   │
│ Backup threats            │ HIGH    │ allowBackup=false         │
│ Cleartext traffic         │ HIGH    │ usesCleartextTraffic=false│
└───────────────────────────┴─────────┴───────────────────────────┘
```

### Security Layers

**Layer 1: Key Management**
- AndroidKeyStore (Hardware-backed when available)
- AES-256 keys never exported
- SecureRandom for IV generation

**Layer 2: Encryption**
- AES/GCM/NoPadding
- 256-bit keys
- 96-bit random IV per message
- 128-bit authentication tag

**Layer 3: Transport**
- Deep links (offline-capable)
- No central server
- Link validation before decryption

**Layer 4: Application**
- Intent filter validation
- URI scheme verification
- Parameter sanitization

---

## 🔑 Key Management

### Primary Key (Stored)

```
Android KeyStore
    ├─ Storage: Hardware-backed Keystore (if available)
    ├─ Algorithm: AES-256
    ├─ Purpose: Encrypt/Decrypt
    ├─ Extraction: NOT ALLOWED
    └─ Lifetime: App lifecycle
```

### Per-Message Key (Ephemeral)

```
Generated for each secure link:
    ├─ Algorithm: AES-256
    ├─ Source: SecureRandom
    ├─ Distribution: Included in link
    ├─ Lifetime: Single message
    └─ Protection: Included in Base64-encoded URI
```

---

## 🔒 Encryption Scheme

### AES-256-GCM Details

```
Key Size:           256 bits (32 bytes)
IV Size:            96 bits (12 bytes) - optimal for GCM
Block Size:         128 bits (16 bytes)
Authentication Tag: 128 bits (16 bytes)
Mode:               Galois/Counter Mode (AEAD)
```

### Encryption Process

```
Plaintext: "Secret Message"
    ↓
┌─────────────────────────────────────┐
│ 1. Generate random 96-bit IV        │
└─────────┬───────────────────────────┘
          ↓
┌─────────────────────────────────────┐
│ 2. Initialize Cipher with AES-256   │
│    key and GCM parameters           │
└─────────┬───────────────────────────┘
          ↓
┌─────────────────────────────────────┐
│ 3. Encrypt plaintext using GCM      │
│    Output: ciphertext + auth tag    │
└─────────┬───────────────────────────┘
          ↓
Output: [IV (12)] + [Ciphertext] + [Auth Tag (16)]
          ↓
        Base64 Encode
          ↓
Base64String (suitable for URI)
```

### Decryption Process

```
Base64String (from URI)
    ↓
  Decode
    ↓
Extract: [IV (12 bytes)] [Ciphertext + Auth Tag]
    ↓
Initialize Cipher with:
  - Key (from encrypted URI)
  - IV (extracted)
  - GCM specification
    ↓
Decrypt and Verify Auth Tag
    ↓
  If verification succeeds:
    └─→ Plaintext (trusted)
  If verification fails:
    └─→ Exception (message tampered)
```

---

## 🛡️ Defense Mechanisms

### Against Eavesdropping

✅ **AES-256 Encryption**: Computationally infeasible to brute-force
✅ **GCM Mode**: Authenticated encryption prevents silent tampering
✅ **Random IV**: Each message uses unique IV

### Against MITM

✅ **Authentication Tag**: Verifies message integrity
✅ **URI Validation**: Malformed URIs rejected
✅ **Timestamp Checking**: Can detect old/replayed messages

### Against Key Exposure

✅ **Android KeyStore**: Keys never leave secure storage
✅ **Hardware-backed**: When available, uses Trusted Execution Environment (TEE)
✅ **Per-message Keys**: Compromise of one key doesn't expose others

### Against Backup/Restoration Threats

✅ `android:allowBackup="false"`: Prevents automated backups
✅ Sensitive data not stored in SharedPreferences
✅ Keys stored in Android KeyStore (excluded from backups)

### Against Cleartext Traffic

✅ `android:usesCleartextTraffic="false"`: Prevents unencrypted connections
✅ All communication is link-based (no network at all)
✅ No server communication for message delivery

---

## ✅ Validation Checks

### Link Validation

```java
// 1. URI Format Check
if (!uri.getScheme().equals("proofline")) {
    reject("Invalid scheme");
}

// 2. Authority Check
if (!uri.getAuthority().equals("secure-link")) {
    reject("Invalid authority");
}

// 3. Parameter Validation
if (uri.getQueryParameter("message") == null) {
    reject("Missing encrypted message");
}

// 4. Base64 Validation
try {
    Base64.getDecoder().decode(message);
} catch (IllegalArgumentException e) {
    reject("Invalid base64 encoding");
}

// 5. GCM Authentication (automatic in doFinal())
try {
    plaintext = cipher.doFinal(ciphertext);
} catch (AEADBadTagException e) {
    reject("Authentication failed - message tampered");
}
```

---

## 🔄 Security Update Policy

- **Dependency Updates**: Monthly security patches
- **Android Target**: Updated within 3 months of new Android version
- **Encryption Library**: BouncyCastle updates tracked
- **ProGuard Rules**: Reviewed with each update

---

## 📋 Compliance

### Android Security Guidelines ✅
- Use AndroidKeyStore for sensitive keys
- Implement ProGuard/R8 obfuscation
- Disable backups for sensitive data
- Require HTTPS (N/A - no server)
- Use cleartext traffic detection

### OWASP Top 10 ✅
- A01: No broken authentication (keys in KeyStore)
- A02: Cryptographic failures prevented (AES-256-GCM)
- A03: Injection - validated URIs
- A04: Insecure design - security by default
- A05: Security misconfiguration - hardened manifest

---

## 🧪 Security Testing

### Test Cases

```java
// 1. Encryption robustness
@Test
public void testEncryptionDoesNotProduceSameCiphertext() {
    String plain = "test";
    String cipher1 = helper.encrypt(plain);
    String cipher2 = helper.encrypt(plain);
    assertNotEquals(cipher1, cipher2);  // Different IVs
}

// 2. Tampered message detection
@Test
public void testTamperedMessageRejected() {
    String cipher = helper.encrypt("message");
    String tampered = cipher.substring(0, cipher.length()-5) + "XXXXX";
    
    assertThrows(Exception.class, () -> {
        helper.decrypt(tampered);
    });
}

// 3. Link validation
@Test
public void testInvalidLinkRejected() {
    Uri invalidUri = Uri.parse("proofline://wrong-host?test=1");
    assertFalse(SecureLinkGenerator.isValidSecureLink(invalidUri));
}
```

---

## 🚨 Incident Response

### If Key Compromise Suspected

1. **Immediate**: Force app update to regenerate keys
2. **Short-term**: Generate new ephemeral keys for future messages
3. **Long-term**: Rotate KeyStore keys periodically

### If Tampered Message Detected

1. **Automatic**: GCM authentication fails
2. **User Alert**: Display security warning
3. **Log**: Track attempt (with privacy)

---

## 📚 References

- [Android KeyStore System](https://developer.android.com/training/articles/keystore)
- [Galois/Counter Mode (GCM)](https://en.wikipedia.org/wiki/Galois/Counter_Mode)
- [NIST SP 800-38D](https://csrc.nist.gov/publications/detail/sp/800-38d/final)
- [BouncyCastle Documentation](https://www.bouncycastle.org/)
- [Android Security & Privacy](https://developer.android.com/privacy-and-security)

