package com.proofline.sms.ui.compose;

import com.proofline.sms.data.crypto.EncryptionHelper;
import java.util.UUID;
import javax.crypto.SecretKey;

/**
 * SecureLinkGenerator - Genera enlaces seguros encriptados para compartir mensajes
 */
public class SecureLinkGenerator {

    private static final String BASE_URL = "proofline://secure-link/";

    /**
     * Genera un enlace seguro para un mensaje
     * @param message El mensaje a encriptar y compartir
     * @return Un enlace seguro que contiene el mensaje encriptado
     */
    public static String generateSecureLink(String message) throws Exception {
        // Generar una clave única para este mensaje
        SecretKey encryptionKey = EncryptionHelper.generateKey();
        
        // Encriptar el mensaje
        String encryptedMessage = EncryptionHelper.encrypt(message, encryptionKey);
        
        // Generar un UUID único
        String uniqueId = UUID.randomUUID().toString();
        
        // Convertir la clave a String para incluirla en el enlace
        String keyString = EncryptionHelper.keyToString(encryptionKey);
        
        // Construir el enlace: proofline://secure-link/{uniqueId}?msg={encryptedMessage}&key={keyString}
        return BASE_URL + uniqueId + "?msg=" + encryptedMessage.replaceAll("\\n", "") + "&key=" + keyString.replaceAll("\\n", "");
    }

    /**
     * Extrae y desencripta un mensaje de un enlace seguro
     * @param link El enlace seguro que contiene el mensaje encriptado
     * @return El mensaje desencriptado
     */
    public static String extractMessageFromLink(String link) throws Exception {
        // Extraer los parámetros del enlace
        String[] parts = link.split("\\?");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Enlace inválido");
        }

        String queryString = parts[1];
        String encryptedMessage = null;
        String keyString = null;

        String[] params = queryString.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue[0].equals("msg")) {
                encryptedMessage = keyValue[1];
            } else if (keyValue[0].equals("key")) {
                keyString = keyValue[1];
            }
        }

        if (encryptedMessage == null || keyString == null) {
            throw new IllegalArgumentException("Parámetros faltantes en el enlace");
        }

        // Reconvertir la clave
        SecretKey key = EncryptionHelper.stringToKey(keyString);
        
        // Desencriptar el mensaje
        return EncryptionHelper.decrypt(encryptedMessage, key);
    }
}
