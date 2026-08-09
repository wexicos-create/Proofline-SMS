package com.proofline.sms.data.model

import java.util.*

/**
 * Message - Modelo de datos para un mensaje en Proofline SMS
 */
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val sender: String,
    val recipient: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = false,
    val secureLink: String? = null,
    val isDelivered: Boolean = false,
    val isRead: Boolean = false
)
