package no.knoksen.i2pbrowser.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
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
    val encryptedPayload: String, // Demo Base64 payload, not production encryption
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

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val endpointLabel: String = "Local Android Router",
    val endpointHost: String = "127.0.0.1",
    val samPort: Int = 7656,
    val httpProxyPort: Int = 4444,
    val routerConsolePort: Int = 7657
)

@Entity(
    tableName = "connect_identities",
    indices = [Index(value = ["fingerprint"], unique = true)]
)
data class ConnectIdentity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val publicDestination: String,
    val publicAppKey: String,
    val fingerprint: String,
    val privateMaterialRef: String,
    val privateMaterialState: String = ConnectIdentityPrivateMaterialState.PROTECTED_REFERENCE,
    val origin: String = ConnectIdentityOrigin.LOCAL,
    @ColumnInfo(defaultValue = "0")
    val cloudSyncEnabled: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = createdAtMillis
)

object ConnectIdentityPrivateMaterialState {
    const val PROTECTED_REFERENCE = "PROTECTED_REFERENCE"
    const val MISSING_PRIVATE_MATERIAL = "MISSING_PRIVATE_MATERIAL"
}

object ConnectIdentityOrigin {
    const val LOCAL = "LOCAL"
    const val IMPORTED_PUBLIC_ONLY = "IMPORTED_PUBLIC_ONLY"
}


