package com.proofline.sms.data.model

import java.io.Serializable
import java.util.Date
import java.util.UUID

/**
 * Message - Data model for encrypted messages in Proofline SMS
 *
 * Features:
 * - Unique ID generation
 * - Timestamps for message tracking
 * - Encryption status tracking
 * - Read/Unread status
 * - Sender/Recipient information
 *
 * @author Proofline SMS
 * @version 1.0
 */
data class Message(
    var id: String = UUID.randomUUID().toString(),
    var content: String = "",
    var sender: String = "Anonymous",
    var recipient: String = "",
    var timestamp: Date = Date(),
    var isReceived: Boolean = false,
    var isRead: Boolean = false,
    var isEncrypted: Boolean = true,
    var encryptionKey: String = "",
    var messageType: MessageType = MessageType.TEXT,
    var secureLink: String = ""
) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

    /**
     * Returns true if message is from current user
     */
    fun isFromCurrentUser(currentUser: String): Boolean {
        return sender == currentUser
    }

    /**
     * Returns true if message is older than specified minutes
     */
    fun isOlderThan(minutes: Int): Boolean {
        val messageAge = System.currentTimeMillis() - timestamp.time
        return messageAge > minutes * 60 * 1000L
    }

    /**
     * Marks message as read
     */
    fun markAsRead() {
        isRead = true
    }

    /**
     * Marks message as unread
     */
    fun markAsUnread() {
        isRead = false
    }

    /**
     * Returns display name for sender
     */
    fun getSenderDisplayName(): String {
        return if (sender.isEmpty() || sender == "Anonymous") "Anonymous" else sender
    }

    /**
     * Returns formatted timestamp
     */
    fun getFormattedTimestamp(): String {
        val formatter = java.text.SimpleDateFormat("HH:mm dd/MM/yyyy", java.util.Locale.getDefault())
        return formatter.format(timestamp)
    }

    /**
     * Returns short formatted timestamp (for chat bubbles)
     */
    fun getShortTimestamp(): String {
        val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return formatter.format(timestamp)
    }

    /**
     * Returns message preview (truncated)
     */
    fun getPreview(maxLength: Int = 50): String {
        return if (content.length > maxLength) {
            content.substring(0, maxLength) + "..."
        } else {
            content
        }
    }
}

/**
 * Enum for message types
 */
enum class MessageType {
    TEXT,
    IMAGE,
    FILE,
    VOICE,
    VIDEO,
    SECURE_LINK
}
