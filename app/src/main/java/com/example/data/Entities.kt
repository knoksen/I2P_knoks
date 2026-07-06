package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val iconName: String = "public",
    val colorHex: String = "#00B0FF",
    val safetyLevel: String = "SAFE"
)

@Entity(tableName = "identities")
data class Identity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val publicKeyBase64: String,
    val privateKeyBase64: String,
    val i2pAddress: String, // e.g., "username.i2p"
    val fullDestination: String // long hash or cryptographic descriptor
)

@Entity(tableName = "secure_messages")
data class SecureMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderAddress: String,
    val recipientAddress: String,
    val encryptedPayload: String, // Base64 garlic packet
    val timestamp: Long = System.currentTimeMillis(),
    val isIncoming: Boolean,
    val isDecrypted: Boolean = false,
    val decryptedBody: String? = null // only cached in-memory or decrypted securely
)

@Entity(tableName = "router_logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String, // e.g. "GARLIC", "TUNNEL", "ROUTER", "NETDB"
    val message: String,
    val level: String = "INFO" // INFO, SUCCESS, WARN, ROUTING
)

@Entity(tableName = "trusted_keys")
data class TrustedKey(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alias: String,
    val i2pAddress: String,
    val publicKeyBase64: String,
    val isVerified: Boolean = false,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val sessionTagCount: Int = 100 // Garlic routing ephemeral session tags remaining
)

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String, // e.g., "comrade.i2p", "pm@gmail.com", "+15551234"
    val type: String, // "SECURE_I2P", "GOOGLE_CHAT", "SMS"
    val status: String = "ONLINE", // "ONLINE", "OFFLINE"
    val avatarColorHex: String = "#00B0FF",
    val lastActiveTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "i2p_tunnels")
data class I2PTunnel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "CLIENT" or "SERVER"
    val localPort: Int,
    val targetAddress: String, // target .i2p address for clients, or local address/port for servers
    val i2pAddress: String, // generated address, e.g. "my-eepsite.i2p"
    val hops: Int = 3,
    val encryptionType: String = "ECIES-X25519",
    val isActive: Boolean = true,
    val bytesTransmitted: Long = 0,
    val bytesReceived: Long = 0
)


