package com.proofline.sms.node.engine

import android.content.Context
import com.proofline.sms.node.model.MessageSensitivity
import com.proofline.sms.node.model.Transport
import com.proofline.sms.node.monitor.P2PCapabilityDetector
import com.proofline.sms.node.monitor.ResourceMonitor
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class NodeDecisionEngineTest {

    @Mock
    private lateinit var resourceMonitor: ResourceMonitor

    @Mock
    private lateinit var p2pCapabilities: P2PCapabilityDetector

    private lateinit var engine: NodeDecisionEngine
    private lateinit var config: NodeConfig

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        config = NodeConfig()
        engine = NodeDecisionEngine(resourceMonitor, p2pCapabilities, config)
    }

    @Test
    fun testSelectTransport_HighBattery_WiFiDirectAvailable_ReturnWiFiDirect() {
        // Arrange
        whenever(resourceMonitor.getBatteryLevel()).thenReturn(75)
        whenever(resourceMonitor.getCpuUsage()).thenReturn(40f)
        whenever(p2pCapabilities.isWiFiDirectAvailable("")).thenReturn(true)

        // Act
        val transport = engine.selectTransport(
            messageSize = 102400,  // > 100KB
            targetNodeId = "test-node"
        )

        // Assert
        assertEquals(Transport.WIFI_DIRECT, transport)
    }

    @Test
    fun testSelectTransport_SmallMessage_BLEAvailable_ReturnBLE() {
        // Arrange
        whenever(resourceMonitor.getBatteryLevel()).thenReturn(50)
        whenever(resourceMonitor.getCpuUsage()).thenReturn(30f)
        whenever(p2pCapabilities.isBLEAvailable("")).thenReturn(true)
        whenever(p2pCapabilities.isWiFiDirectAvailable("")).thenReturn(false)

        // Act
        val transport = engine.selectTransport(
            messageSize = 512,  // < 1KB
            targetNodeId = "test-node"
        )

        // Assert
        assertEquals(Transport.BLUETOOTH_LE, transport)
    }

    @Test
    fun testSelectTransport_CriticalMessage_ReturnSecureTransport() {
        // Arrange
        whenever(resourceMonitor.getBatteryLevel()).thenReturn(50)
        whenever(resourceMonitor.getCpuUsage()).thenReturn(30f)
        whenever(p2pCapabilities.isWiFiDirectAvailable("")).thenReturn(true)

        // Act
        val transport = engine.selectTransport(
            messageSize = 1024,
            targetNodeId = "test-node",
            isCritical = true
        )

        // Assert
        assertEquals(Transport.WIFI_DIRECT, transport)
    }

    @Test
    fun testSelectTransport_LowBattery_ReturnStoreForward() {
        // Arrange
        whenever(resourceMonitor.getBatteryLevel()).thenReturn(10)  // < 15%

        // Act
        val transport = engine.selectTransport(
            messageSize = 1024,
            targetNodeId = "test-node"
        )

        // Assert
        assertEquals(Transport.STORE_FORWARD, transport)
    }

    @Test
    fun testSelectTransport_HighCPU_ReturnStoreForward() {
        // Arrange
        whenever(resourceMonitor.getBatteryLevel()).thenReturn(50)
        whenever(resourceMonitor.getCpuUsage()).thenReturn(90f)  // > 85%

        // Act
        val transport = engine.selectTransport(
            messageSize = 1024,
            targetNodeId = "test-node"
        )

        // Assert
        assertEquals(Transport.STORE_FORWARD, transport)
    }

    @Test
    fun testGetTTLForMessage_Critical_30Seconds() {
        // Act
        val ttl = engine.getTTLForMessage(MessageSensitivity.CRITICAL)

        // Assert
        assertEquals(30.seconds, ttl)
    }

    @Test
    fun testGetTTLForMessage_High_2Minutes() {
        // Act
        val ttl = engine.getTTLForMessage(MessageSensitivity.HIGH)

        // Assert
        assertEquals(2.minutes, ttl)
    }

    @Test
    fun testGetTTLForMessage_Normal_5Minutes() {
        // Act
        val ttl = engine.getTTLForMessage(MessageSensitivity.NORMAL)

        // Assert
        assertEquals(5.minutes, ttl)
    }

    @Test
    fun testSelectCryptoAlgorithm_Sensitive_AES256GCM() {
        // Arrange
        whenever(resourceMonitor.getCpuUsage()).thenReturn(40f)
        whenever(resourceMonitor.getBatteryLevel()).thenReturn(50)

        // Act
        val algo = engine.selectCryptoAlgorithm(isSensitive = true)

        // Assert
        assertEquals("AES-256-GCM", algo)
    }

    @Test
    fun testSelectCryptoAlgorithm_HighCPU_AES128GCM() {
        // Arrange
        whenever(resourceMonitor.getCpuUsage()).thenReturn(80f)  // > 70%
        whenever(resourceMonitor.getBatteryLevel()).thenReturn(50)

        // Act
        val algo = engine.selectCryptoAlgorithm(isSensitive = false)

        // Assert
        assertEquals("AES-128-GCM", algo)
    }

    @Test
    fun testSelectCryptoAlgorithm_LowBattery_ChaCha20() {
        // Arrange
        whenever(resourceMonitor.getCpuUsage()).thenReturn(40f)
        whenever(resourceMonitor.getBatteryLevel()).thenReturn(15)  // < 30%

        // Act
        val algo = engine.selectCryptoAlgorithm(isSensitive = false)

        // Assert
        assertEquals("ChaCha20-Poly1305", algo)
    }

    @Test
    fun testShouldEnableLowPowerMode_LowBattery_True() {
        // Arrange
        whenever(resourceMonitor.getBatteryLevel()).thenReturn(10)  // < 15%
        whenever(resourceMonitor.getCpuUsage()).thenReturn(40f)
        whenever(resourceMonitor.getDeviceTemperature()).thenReturn(35f)

        // Act
        val shouldEnable = engine.shouldEnableLowPowerMode()

        // Assert
        assertTrue(shouldEnable)
    }

    @Test
    fun testShouldEnableLowPowerMode_HighCPU_True() {
        // Arrange
        whenever(resourceMonitor.getBatteryLevel()).thenReturn(50)
        whenever(resourceMonitor.getCpuUsage()).thenReturn(90f)  // > 85%
        whenever(resourceMonitor.getDeviceTemperature()).thenReturn(35f)

        // Act
        val shouldEnable = engine.shouldEnableLowPowerMode()

        // Assert
        assertTrue(shouldEnable)
    }

    @Test
    fun testShouldEnableLowPowerMode_NormalConditions_False() {
        // Arrange
        whenever(resourceMonitor.getBatteryLevel()).thenReturn(50)
        whenever(resourceMonitor.getCpuUsage()).thenReturn(40f)
        whenever(resourceMonitor.getDeviceTemperature()).thenReturn(35f)

        // Act
        val shouldEnable = engine.shouldEnableLowPowerMode()

        // Assert
        assertFalse(shouldEnable)
    }

    @Test
    fun testShouldCompressMessage_LargeMessage_True() {
        // Arrange
        whenever(resourceMonitor.getBatteryLevel()).thenReturn(50)
        whenever(resourceMonitor.isMeteredConnection()).thenReturn(false)

        // Act
        val shouldCompress = engine.shouldCompressMessage(600000)  // > 500KB

        // Assert
        assertTrue(shouldCompress)
    }

    @Test
    fun testShouldCompressMessage_SmallMessage_False() {
        // Arrange
        whenever(resourceMonitor.getBatteryLevel()).thenReturn(50)
        whenever(resourceMonitor.isMeteredConnection()).thenReturn(false)

        // Act
        val shouldCompress = engine.shouldCompressMessage(1024)  // 1KB

        // Assert
        assertFalse(shouldCompress)
    }

    @Test
    fun testGetNodeStatus_ReturnsCompleteStatus() {
        // Arrange
        whenever(resourceMonitor.getBatteryLevel()).thenReturn(75)
        whenever(resourceMonitor.getCpuUsage()).thenReturn(35f)
        whenever(resourceMonitor.getDeviceTemperature()).thenReturn(38f)
        whenever(resourceMonitor.isNetworkConnected()).thenReturn(true)
        whenever(resourceMonitor.isMeteredConnection()).thenReturn(false)
        whenever(p2pCapabilities.getAvailableTransports()).thenReturn(
            listOf(Transport.WIFI_DIRECT, Transport.BLUETOOTH_LE)
        )

        // Act
        val status = engine.getNodeStatus()

        // Assert
        assertEquals(75, status.batteryLevel)
        assertEquals(35f, status.cpuUsage)
        assertEquals(38f, status.temperature)
        assertTrue(status.isConnected)
        assertFalse(status.isMetered)
    }
}
