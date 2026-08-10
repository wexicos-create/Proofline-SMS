package com.proofline.sms.node.monitor

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.proofline.sms.node.model.Transport

/**
 * P2PCapabilityDetector - Detecta capacidades P2P del dispositivo
 *
 * Responsabilidades:
 * - Detectar disponibilidad de WiFi Direct
 * - Detectar disponibilidad de Bluetooth/BLE
 * - Detectar si dos nodos están en misma LAN
 * - Obtener lista de transportes disponibles
 *
 * Diseño: Check-based (sin listeners), determinista.
 *
 * @author Proofline SMS - Sovereign Node
 */
class P2PCapabilityDetector(private val context: Context) {
    companion object {
        private const val TAG = "P2PCapabilityDetector"
    }

    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager?
    private val wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    private val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?)
        ?.adapter

    /**
     * Detecta si WiFi Direct está disponible
     */
    fun isWiFiDirectAvailable(targetNodeId: String): Boolean {
        return try {
            // Verificar permiso
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.w(TAG, "WiFi Direct permission not granted")
                return false
            }

            // Verificar si el dispositivo soporta WiFi Direct
            val supportsP2p = context.packageManager.hasSystemFeature(
                PackageManager.FEATURE_WIFI_DIRECT
            )

            if (!supportsP2p) {
                Log.w(TAG, "Device does not support WiFi Direct")
                return false
            }

            // Verificar si WiFi está habilitado (WiFi Direct requiere WiFi encendido)
            val wifiEnabled = wifiManager?.isWifiEnabled ?: false

            Log.d(TAG, "WiFi Direct available for $targetNodeId: $wifiEnabled")
            wifiEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Error checking WiFi Direct", e)
            false
        }
    }

    /**
     * Detecta si Bluetooth Low Energy (BLE) está disponible
     */
    fun isBLEAvailable(targetNodeId: String): Boolean {
        return try {
            if (!hasPermission(Manifest.permission.BLUETOOTH)) {
                Log.w(TAG, "Bluetooth permission not granted")
                return false
            }

            val isSupported = context.packageManager.hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE
            )

            if (!isSupported) {
                Log.w(TAG, "Device does not support BLE")
                return false
            }

            val isEnabled = bluetoothAdapter?.isEnabled ?: false

            Log.d(TAG, "BLE available for $targetNodeId: $isEnabled")
            isEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Error checking BLE", e)
            false
        }
    }

    /**
     * Detecta si Bluetooth clásico está disponible
     */
    fun isBluetoothClassicAvailable(): Boolean {
        return try {
            if (!hasPermission(Manifest.permission.BLUETOOTH)) {
                return false
            }

            bluetoothAdapter?.isEnabled ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Bluetooth Classic", e)
            false
        }
    }

    /**
     * Detecta si dos nodos están en la misma LAN (Red Local)
     *
     * Heurística: Si ambos dispositivos están conectados a la misma red WiFi
     */
    fun isOnSameLAN(targetNodeId: String): Boolean {
        return try {
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                return false
            }

            // Verificar si estamos conectados a WiFi
            val connectionInfo = wifiManager?.connectionInfo ?: return false

            // Si tenemos SSID, asumimos que podemos comunicarnos con nodos en la misma red
            val ssid = connectionInfo.ssid
            val isConnected = ssid != null && ssid != "<unknown ssid>"

            Log.d(TAG, "Same LAN check for $targetNodeId: $isConnected (SSID: $ssid)")
            isConnected
        } catch (e: Exception) {
            Log.e(TAG, "Error checking LAN", e)
            false
        }
    }

    /**
     * Obtiene lista de transportes disponibles en el dispositivo
     */
    fun getAvailableTransports(): List<Transport> {
        val transports = mutableListOf<Transport>()

        // Siempre disponible (fallback)
        transports.add(Transport.STORE_FORWARD)

        // Verificar WiFi Direct
        if (isWiFiDirectAvailable("")) {
            transports.add(Transport.WIFI_DIRECT)
        }

        // Verificar BLE
        if (isBLEAvailable("")) {
            transports.add(Transport.BLUETOOTH_LE)
        }

        // Verificar Bluetooth Clásico
        if (isBluetoothClassicAvailable()) {
            transports.add(Transport.BLUETOOTH_CLASSIC)
        }

        // Verificar LAN
        if (isOnSameLAN("")) {
            transports.add(Transport.LAN_MESH)
        }

        Log.d(TAG, "Available transports: $transports")
        return transports
    }

    /**
     * Obtiene el mejor transporte disponible basado en orden de preferencia
     */
    fun getBestAvailableTransport(): Transport {
        val available = getAvailableTransports()

        // Orden de preferencia
        val preference = listOf(
            Transport.WIFI_DIRECT,
            Transport.LAN_MESH,
            Transport.BLUETOOTH_LE,
            Transport.BLUETOOTH_CLASSIC,
            Transport.STORE_FORWARD
        )

        for (transport in preference) {
            if (transport in available) {
                Log.d(TAG, "Best available transport: $transport")
                return transport
            }
        }

        return Transport.STORE_FORWARD
    }

    /**
     * Verifica si un permiso está otorgado
     */
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}
