package com.proofline.sms.node.engine

import android.util.Log
import com.proofline.sms.node.model.MessageSensitivity
import com.proofline.sms.node.model.Transport
import com.proofline.sms.node.monitor.ResourceMonitor
import com.proofline.sms.node.monitor.P2PCapabilityDetector
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.minutes

/**
 * NodeDecisionEngine - Motor de decisión determinista SIN IA
 *
 * Responsabilidades:
 * - Seleccionar transporte óptimo basado en reglas explícitas
 * - Determinar TTL dinámico según sensibilidad del mensaje
 * - Elegir algoritmo de crypto según capacidades del dispositivo
 * - Gestionar recursos bajo umbrales configurables
 *
 * FILOSOFÍA: Todas las decisiones son deterministas y auditables.
 * Mismo input → Mismo output siempre. Cero ML/IA.
 *
 * @author Proofline SMS - Sovereign Node
 * @version 1.0
 */
class NodeDecisionEngine(
    private val resourceMonitor: ResourceMonitor,
    private val p2pCapabilities: P2PCapabilityDetector,
    private val config: NodeConfig = NodeConfig()
) {
    companion object {
        private const val TAG = "NodeDecisionEngine"
    }

    /**
     * ÁRBOL DE DECISIÓN: Selecciona transporte óptimo
     *
     * Regla 1: Si batería > 30% Y WiFi Direct disponible Y mensaje > 1KB → WIFI_DIRECT
     * Regla 2: Si BLE disponible Y mensaje <= 1KB → BLUETOOTH_LE
     * Regla 3: Si en misma LAN → LAN_MESH
     * Regla 4: Si batería < 15% → STORE_FORWARD (ahorra batería)
     * Regla 5: Fallback → STORE_FORWARD
     */
    fun selectTransport(
        messageSize: Int,
        targetNodeId: String,
        isCritical: Boolean = false
    ): Transport {
        val battery = resourceMonitor.getBatteryLevel()
        val cpu = resourceMonitor.getCpuUsage()
        val isConnected = resourceMonitor.isNetworkConnected()

        Log.d(TAG, "selectTransport: size=$messageSize, battery=$battery%, cpu=$cpu%, critical=$isCritical")

        // REGLA CRÍTICA: Mensajes críticos siempre por transporte más seguro
        if (isCritical) {
            return when {
                battery > 40 && p2pCapabilities.isWiFiDirectAvailable(targetNodeId) ->
                    Transport.WIFI_DIRECT
                p2pCapabilities.isBLEAvailable(targetNodeId) ->
                    Transport.BLUETOOTH_LE
                else -> Transport.STORE_FORWARD
            }
        }

        // REGLA 1: Alta velocidad si hay recursos
        if (battery > config.batteryThresholdHigh &&
            cpu < config.cpuThresholdHigh &&
            messageSize > config.messageSizeThresholdLarge &&
            p2pCapabilities.isWiFiDirectAvailable(targetNodeId)) {
            Log.d(TAG, "→ WIFI_DIRECT (batería suficiente, mensaje grande)")
            return Transport.WIFI_DIRECT
        }

        // REGLA 2: Bajo consumo para mensajes pequeños
        if (p2pCapabilities.isBLEAvailable(targetNodeId) &&
            messageSize <= config.messageSizeThresholdSmall) {
            Log.d(TAG, "→ BLUETOOTH_LE (mensaje pequeño, bajo consumo)")
            return Transport.BLUETOOTH_LE
        }

        // REGLA 3: Red local
        if (p2pCapabilities.isOnSameLAN(targetNodeId) && battery > config.batteryThresholdLow) {
            Log.d(TAG, "→ LAN_MESH (en misma red)")
            return Transport.LAN_MESH
        }

        // REGLA 4: Batería muy baja → almacenar
        if (battery < config.batteryThresholdCritical) {
            Log.d(TAG, "→ STORE_FORWARD (batería crítica < ${config.batteryThresholdCritical}%)")
            return Transport.STORE_FORWARD
        }

        // REGLA 5: CPU muy alta → almacenar
        if (cpu > config.cpuThresholdCritical) {
            Log.d(TAG, "→ STORE_FORWARD (CPU crítica > ${config.cpuThresholdCritical}%)")
            return Transport.STORE_FORWARD
        }

        // FALLBACK: Siempre almacenar
        Log.d(TAG, "→ STORE_FORWARD (fallback)")
        return Transport.STORE_FORWARD
    }

    /**
     * ÁRBOL DE DECISIÓN: Determina TTL (Time To Live) del mensaje
     *
     * Regla 1: CRITICAL → 30 segundos (máxima privacidad)
     * Regla 2: HIGH → 2 minutos (muy sensible)
     * Regla 3: NORMAL → 5 minutos (estándar)
     * Regla 4: DRAFT → 15 minutos (borrador local)
     * Regla 5: ARCHIVE → Sin expiración (para almacenamiento)
     */
    fun getTTLForMessage(sensitivityFlag: MessageSensitivity): Duration {
        return when (sensitivityFlag) {
            MessageSensitivity.CRITICAL -> {
                Log.d(TAG, "TTL CRITICAL: 30s")
                30.seconds
            }
            MessageSensitivity.HIGH -> {
                Log.d(TAG, "TTL HIGH: 2m")
                2.minutes
            }
            MessageSensitivity.NORMAL -> {
                Log.d(TAG, "TTL NORMAL: 5m")
                5.minutes
            }
            MessageSensitivity.DRAFT -> {
                Log.d(TAG, "TTL DRAFT: 15m")
                15.minutes
            }
            MessageSensitivity.ARCHIVE -> {
                Log.d(TAG, "TTL ARCHIVE: Sin expiración")
                Duration.INFINITE
            }
        }
    }

    /**
     * ÁRBOL DE DECISIÓN: Selecciona algoritmo de criptografía
     *
     * Regla 1: Si CPU > 70% → AES-128 (más rápido)
     * Regla 2: Si mensaje sensible → AES-256-GCM (máxima seguridad)
     * Regla 3: Si batería < 20% → ChaCha20-Poly1305 (eficiente)
     * Regla 4: Default → AES-256-GCM
     */
    fun selectCryptoAlgorithm(isSensitive: Boolean = false): String {
        val cpu = resourceMonitor.getCpuUsage()
        val battery = resourceMonitor.getBatteryLevel()

        return when {
            cpu > config.cpuThresholdHigh -> {
                Log.d(TAG, "Crypto: AES-128 (CPU alta)")
                "AES-128-GCM"
            }
            isSensitive -> {
                Log.d(TAG, "Crypto: AES-256-GCM (mensaje sensible)")
                "AES-256-GCM"
            }
            battery < config.batteryThresholdLow -> {
                Log.d(TAG, "Crypto: ChaCha20-Poly1305 (batería baja)")
                "ChaCha20-Poly1305"
            }
            else -> {
                Log.d(TAG, "Crypto: AES-256-GCM (default)")
                "AES-256-GCM"
            }
        }
    }

    /**
     * ÁRBOL DE DECISIÓN: Determina frecuencia de discovery (descubrimiento de nodos)
     *
     * Regla 1: Si CPU > 80% durante 5s → reducir a 10s
     * Regla 2: Si batería < 20% → reducir a 15s
     * Regla 3: Si en metered connection → reducir a 20s
     * Regla 4: Default → 5s
     */
    fun getDiscoveryInterval(): Duration {
        val cpu = resourceMonitor.getCpuUsage()
        val battery = resourceMonitor.getBatteryLevel()
        val isMetered = resourceMonitor.isMeteredConnection()

        return when {
            cpu > config.cpuThresholdCritical -> {
                Log.d(TAG, "Discovery interval: 10s (CPU crítica)")
                10.seconds
            }
            battery < config.batteryThresholdLow -> {
                Log.d(TAG, "Discovery interval: 15s (batería baja)")
                15.seconds
            }
            isMetered -> {
                Log.d(TAG, "Discovery interval: 20s (conexión medida)")
                20.seconds
            }
            else -> {
                Log.d(TAG, "Discovery interval: 5s (normal)")
                5.seconds
            }
        }
    }

    /**
     * ÁRBOL DE DECISIÓN: Determina si debe activarse modo de bajo consumo
     *
     * Regla 1: Si batería < 15% → activar
     * Regla 2: Si CPU > 85% durante 10s → activar
     * Regla 3: Si temperatura del dispositivo > umbral → activar
     */
    fun shouldEnableLowPowerMode(): Boolean {
        val battery = resourceMonitor.getBatteryLevel()
        val cpu = resourceMonitor.getCpuUsage()
        val temperature = resourceMonitor.getDeviceTemperature()

        val isLowBattery = battery < config.batteryThresholdCritical
        val isHighCpu = cpu > 85f
        val isOverheating = temperature > config.temperatureThreshold

        val shouldEnable = isLowBattery || isHighCpu || isOverheating

        if (shouldEnable) {
            Log.w(TAG, "LOW POWER MODE ENABLED: battery=$battery%, cpu=$cpu%, temp=$temperature°C")
        }

        return shouldEnable
    }

    /**
     * ÁRBOL DE DECISIÓN: Determina compresión de datos
     *
     * Regla 1: Si mensaje > 500KB → comprimir (ahorra ancho de banda)
     * Regla 2: Si batería < 20% → comprimir (ahorra energía)
     * Regla 3: Si en metered connection → comprimir
     */
    fun shouldCompressMessage(messageSize: Int): Boolean {
        val battery = resourceMonitor.getBatteryLevel()
        val isMetered = resourceMonitor.isMeteredConnection()

        val shouldCompress = messageSize > config.messageSizeThresholdCompress ||
                battery < config.batteryThresholdLow ||
                isMetered

        if (shouldCompress) {
            Log.d(TAG, "Message compression enabled (size=$messageSize bytes)")
        }

        return shouldCompress
    }

    /**
     * Obtiene estado completo del nodo para debugging
     */
    fun getNodeStatus(): NodeStatus {
        return NodeStatus(
            batteryLevel = resourceMonitor.getBatteryLevel(),
            cpuUsage = resourceMonitor.getCpuUsage(),
            temperature = resourceMonitor.getDeviceTemperature(),
            isConnected = resourceMonitor.isNetworkConnected(),
            isMetered = resourceMonitor.isMeteredConnection(),
            availableTransports = p2pCapabilities.getAvailableTransports(),
            discoveryInterval = getDiscoveryInterval(),
            lowPowerMode = shouldEnableLowPowerMode(),
            timestamp = System.currentTimeMillis()
        )
    }
}

/**
 * Configuración de umbrales para el motor de decisión
 */
data class NodeConfig(
    // Umbrales de batería (%)
    val batteryThresholdCritical: Int = 15,
    val batteryThresholdLow: Int = 30,
    val batteryThresholdHigh: Int = 50,

    // Umbrales de CPU (%)
    val cpuThresholdHigh: Int = 70,
    val cpuThresholdCritical: Int = 85,

    // Umbrales de tamaño de mensaje (bytes)
    val messageSizeThresholdSmall: Int = 1024,        // 1 KB
    val messageSizeThresholdLarge: Int = 102400,      // 100 KB
    val messageSizeThresholdCompress: Int = 512000,   // 500 KB

    // Temperatura (%C)
    val temperatureThreshold: Float = 45f
)

/**
 * Estado actual del nodo
 */
data class NodeStatus(
    val batteryLevel: Int,
    val cpuUsage: Float,
    val temperature: Float,
    val isConnected: Boolean,
    val isMetered: Boolean,
    val availableTransports: List<Transport>,
    val discoveryInterval: Duration,
    val lowPowerMode: Boolean,
    val timestamp: Long
)
