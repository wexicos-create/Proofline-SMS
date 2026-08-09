package com.proofline.sms.util;

import android.content.Intent;
import android.net.Uri;
import com.proofline.sms.ui.compose.SecureLinkGenerator;

/**
 * LinkHandler - Maneja los Intent Filters y procesa los enlaces seguros
 */
public class LinkHandler {

    /**
     * Procesa un Intent que contiene un enlace seguro
     * @param intent El Intent recibido
     * @return El mensaje desencriptado, o null si el Intent no es válido
     */
    public static String handleSecureLink(Intent intent) {
        if (intent == null) {
            return null;
        }

        String action = intent.getAction();
        Uri data = intent.getData();

        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            String link = data.toString();
            try {
                return SecureLinkGenerator.extractMessageFromLink(link);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        return null;
    }

    /**
     * Verifica si un Intent contiene un enlace seguro válido
     * @param intent El Intent a verificar
     * @return true si es un enlace seguro válido, false en caso contrario
     */
    public static boolean isSecureLink(Intent intent) {
        if (intent == null) {
            return false;
        }

        String action = intent.getAction();
        Uri data = intent.getData();

        return Intent.ACTION_VIEW.equals(action) && data != null && 
               data.getScheme().equals("proofline") && 
               data.getHost().equals("secure-link");
    }
}
