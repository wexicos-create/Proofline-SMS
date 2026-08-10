package com.proofline.sms.node.model

/**
 * Transporte disponible para comunicación P2P
 */
enum class Transport {
    WIFI_DIRECT,      // Rápido, consume batería
    BLUETOOTH_LE,     // Bajo consumo, corto alcance
    BLUETOOTH_CLASSIC,// Compatible, mediano consumo
    LAN_MESH,         // Red local, requiere WiFi
    STORE_FORWARD;    // Almacenar para envío posterior

    val priority: Int
        get() = when (this) {
            WIFI_DIRECT -> 1        // Más alta prioridad
            LAN_MESH -> 2
            BLUETOOTH_LE -> 3
            BLUETOOTH_CLASSIC -> 4
            STORE_FORWARD -> 5      // Más baja prioridad
        }

    fun isFast(): Boolean = this in listOf(WIFI_DIRECT, LAN_MESH)
    fun isLowPower(): Boolean = this in listOf(BLUETOOTH_LE, STORE_FORWARD)
}

/**
 * Nivel de sensibilidad del mensaje
 */
enum class MessageSensitivity {
    CRITICAL,  // Máxima privacidad, TTL 30s
    HIGH,      // Muy sensible, TTL 2min
    NORMAL,    // Estándar, TTL 5min
    DRAFT,     // Borrador local, TTL 15min
    ARCHIVE    // Sin expiración, para almacenamiento
}
