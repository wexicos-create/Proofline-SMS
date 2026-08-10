package com.proofline.sms.node.threat

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewTreeObserver
import android.view.accessibility.AccessibilityManager

/**
 * ThreatDetector - Detección de amenazas basada en eventos del sistema
 *
 * Responsabilidades:
 * - Detectar intentos de captura de pantalla
 * - Detectar servicios de accesibilidad sospechosos
 * - Detectar cambios en el foco de ventana
 * - Detectar overlay sospechosos
 * - Monitoreo pasivo sin ML
 *
 * FILOSOFÍA: Solo basado en APIs oficiales de Android, cero análisis de patrones.
 *
 * @author Proofline SMS - Sovereign Node
 */
class ThreatDetector(private val context: Context) {
    companion object {
        private const val TAG = "ThreatDetector"
    }

    private val accessibilityManager = context.getSystemService(
        Context.ACCESSIBILITY_SERVICE
    ) as AccessibilityManager

    private var threatListeners = mutableListOf<ThreatListener>()
    private var isMonitoring = false

    /**
     * REGLA 1: Detecta servicios de accesibilidad potencialmente maliciosos
     *
     * Heurística:
     * - Servicios de accesibilidad habilitados = acceso a contenido de pantalla
     * - Muchos servicios = posible ataque de solapamiento
     * - Servicios desconocidos = riesgo
     */
    fun detectAccessibilityThreats(): List<ThreatEvent> {
        val threats = mutableListOf<ThreatEvent>()

        try {
            val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASKS
            )

            if (enabledServices.isNotEmpty()) {
                Log.w(TAG, "${enabledServices.size} accessibility services detected")

                for (service in enabledServices) {
                    val packageName = service.resolveInfo.serviceInfo.packageName

                    // Excepto Google, diálogo del sistema y app propia
                    if (!isWhitelistedAccessibilityService(packageName)) {
                        threats.add(
                            ThreatEvent(
                                type = ThreatType.ACCESSIBILITY_SERVICE,
                                severity = ThreatSeverity.HIGH,
                                description = "Potential spyware accessibility service: $packageName",
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        Log.e(TAG, "THREAT: Suspicious accessibility service detected: $packageName")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting accessibility threats", e)
        }

        return threats
    }

    /**
     * REGLA 2: Detecta cambios en el foco de ventana
     *
     * Heurística:
     * - App pierde foco → posible captura de pantalla activa
     * - Si FLAG_SECURE está activo y hay pérdida de foco → amenaza
     */
    fun monitorWindowFocus(activity: Activity) {
        try {
            var lastFocusState = true
            val focusListener = ViewTreeObserver.OnGlobalFocusChangeListener { _, _ ->
                val hasFocus = activity.window.decorView.hasWindowFocus()

                if (lastFocusState && !hasFocus) {
                    Log.w(TAG, "App lost focus - Possible screen capture attempt")
                    emitThreat(
                        ThreatEvent(
                            type = ThreatType.WINDOW_FOCUS_LOSS,
                            severity = ThreatSeverity.MEDIUM,
                            description = "App lost window focus - possible screen recording",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }

                lastFocusState = hasFocus
            }

            activity.window.decorView.viewTreeObserver.addOnGlobalFocusChangeListener(focusListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error monitoring window focus", e)
        }
    }

    /**
     * REGLA 3: Detecta si se está ejecutando un overlay (flotante)
     *
     * Heurística:
     * - Presencia de SYSTEM_ALERT_WINDOW permiso + canDrawOverlays = riesgo
     */
    fun detectOverlayThreats(): List<ThreatEvent> {
        val threats = mutableListOf<ThreatEvent>()

        try {
            val context = context.applicationContext
            val hasOverlayPerm = context.packageManager.checkPermission(
                android.Manifest.permission.SYSTEM_ALERT_WINDOW,
                context.packageName
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasOverlayPerm) {
                Log.w(TAG, "App has SYSTEM_ALERT_WINDOW permission")
                threats.add(
                    ThreatEvent(
                        type = ThreatType.OVERLAY_DETECTED,
                        severity = ThreatSeverity.HIGH,
                        description = "System overlay permissions detected",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting overlay threats", e)
        }

        return threats
    }

    /**
     * REGLA 4: Inicia monitoreo continuo de amenazas
     *
     * Frecuencia: Cada 5 segundos (bajo overhead)
     */
    fun startContinuousMonitoring() {
        if (isMonitoring) return

        isMonitoring = true
        Log.d(TAG, "Starting continuous threat monitoring")

        val handler = Handler(Looper.getMainLooper())
        val monitoringTask = object : Runnable {
            override fun run() {
                if (!isMonitoring) return

                // Ejecutar checks
                val threats = mutableListOf<ThreatEvent>()
                threats.addAll(detectAccessibilityThreats())
                threats.addAll(detectOverlayThreats())

                // Emitir amenazas detectadas
                for (threat in threats) {
                    emitThreat(threat)
                }

                // Programar siguiente check
                handler.postDelayed(this, 5000)  // Cada 5 segundos
            }
        }

        handler.post(monitoringTask)
    }

    /**
     * Detiene el monitoreo continuo
     */
    fun stopContinuousMonitoring() {
        isMonitoring = false
        Log.d(TAG, "Stopping continuous threat monitoring")
    }

    /**
     * Registra listener para eventos de amenaza
     */
    fun addThreatListener(listener: ThreatListener) {
        threatListeners.add(listener)
    }

    /**
     * Elimina listener de eventos de amenaza
     */
    fun removeThreatListener(listener: ThreatListener) {
        threatListeners.remove(listener)
    }

    /**
     * Emite evento de amenaza a todos los listeners
     */
    private fun emitThreat(threat: ThreatEvent) {
        for (listener in threatListeners) {
            listener.onThreatDetected(threat)
        }
    }

    /**
     * Servicios de accesibilidad whitelisteados
     */
    private fun isWhitelistedAccessibilityService(packageName: String): Boolean {
        val whitelist = setOf(
            "com.google.android.gms",
            "com.android.systemui",
            "com.android.inputmethod.latin",
            context.packageName  // Nuestra app
        )
        return packageName in whitelist
    }
}

/**
 * Tipos de amenazas detectables
 */
enum class ThreatType {
    ACCESSIBILITY_SERVICE,
    WINDOW_FOCUS_LOSS,
    OVERLAY_DETECTED,
    SUSPICIOUS_PERMISSION,
    CLIPBOARD_ACCESS,
    SCREEN_RECORDING,
    UNKNOWN
}

/**
 * Severidad de la amenaza
 */
enum class ThreatSeverity {
    CRITICAL,  // Acción inmediata requerida
    HIGH,      // Elevar estado de alerta
    MEDIUM,    // Registrar y monitorear
    LOW        // Solo registrar
}

/**
 * Evento de amenaza detectada
 */
data class ThreatEvent(
    val type: ThreatType,
    val severity: ThreatSeverity,
    val description: String,
    val timestamp: Long,
    val sourcePackage: String = "SYSTEM"
)

/**
 * Interfaz para listeners de amenazas
 */
interface ThreatListener {
    fun onThreatDetected(threat: ThreatEvent)
}
