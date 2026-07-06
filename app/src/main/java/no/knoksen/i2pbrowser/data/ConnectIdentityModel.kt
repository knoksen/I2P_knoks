package no.knoksen.i2pbrowser.data

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

object ConnectIdentitySecurityWarnings {
    const val EXPORT_PUBLIC_ONLY =
        "Identity export is public-only. Private destination and private app key material are excluded."
    const val IMPORT_PUBLIC_ONLY =
        "Imported identity records do not contain private material and cannot prove local ownership."
    const val PRIVATE_BACKUP_NOT_IMPLEMENTED =
        "Private identity backup is not implemented. Do not paste private keys into this import flow."
    const val CLOUD_SYNC_DISABLED =
        "Cloud sync is disabled by default for I2P Connect identities."
}

data class ConnectIdentityPublicExport(
    val displayName: String,
    val publicDestination: String,
    val publicAppKey: String,
    val fingerprint: String,
    val createdAtMillis: Long,
    val warnings: List<String>
)

sealed class ConnectIdentityImportResult {
    data class Success(val identity: ConnectIdentity, val warnings: List<String>) : ConnectIdentityImportResult()
    data class Failure(val reason: String) : ConnectIdentityImportResult()
}

object ConnectIdentityFactory {
    fun createLocal(
        displayName: String,
        publicDestination: String,
        publicAppKey: String = "connect-pub-${UUID.randomUUID()}",
        nowMillis: Long = System.currentTimeMillis(),
        privateMaterialRef: String = "local-only-ref:${UUID.randomUUID()}"
    ): ConnectIdentity {
        val cleanDisplayName = displayName.trim().ifBlank { "Local I2P Connect Identity" }
        val cleanDestination = publicDestination.trim()
        val cleanPublicAppKey = publicAppKey.trim()
        return ConnectIdentity(
            displayName = cleanDisplayName,
            publicDestination = cleanDestination,
            publicAppKey = cleanPublicAppKey,
            fingerprint = ConnectIdentityFingerprint.from(cleanDestination, cleanPublicAppKey),
            privateMaterialRef = privateMaterialRef,
            privateMaterialState = ConnectIdentityPrivateMaterialState.PROTECTED_REFERENCE,
            origin = ConnectIdentityOrigin.LOCAL,
            cloudSyncEnabled = false,
            createdAtMillis = nowMillis,
            updatedAtMillis = nowMillis
        )
    }
}

object ConnectIdentityFingerprint {
    fun from(publicDestination: String, publicAppKey: String): String {
        val input = "${publicDestination.trim()}|${publicAppKey.trim()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(StandardCharsets.UTF_8))
        return digest.take(8).joinToString("-") { byte ->
            "%02X".format(Locale.US, byte)
        }
    }
}

object ConnectIdentityExportCodec {
    private const val HEADER = "I2P_CONNECT_IDENTITY_V1"
    private const val MAX_IMPORT_LENGTH = 12_000

    private val blockedPrivateFields = listOf(
        "privateKey",
        "privateDestination",
        "privateMaterialRef",
        "privateAppKey",
        "PRIV="
    )

    fun toPublicExport(identity: ConnectIdentity): ConnectIdentityPublicExport {
        return ConnectIdentityPublicExport(
            displayName = identity.displayName,
            publicDestination = identity.publicDestination,
            publicAppKey = identity.publicAppKey,
            fingerprint = identity.fingerprint,
            createdAtMillis = identity.createdAtMillis,
            warnings = listOf(
                ConnectIdentitySecurityWarnings.EXPORT_PUBLIC_ONLY,
                ConnectIdentitySecurityWarnings.CLOUD_SYNC_DISABLED
            )
        )
    }

    fun encodePublic(identity: ConnectIdentity): String {
        val publicExport = toPublicExport(identity)
        return buildString {
            appendLine(HEADER)
            appendLine("displayName=${encode(publicExport.displayName)}")
            appendLine("publicDestination=${encode(publicExport.publicDestination)}")
            appendLine("publicAppKey=${encode(publicExport.publicAppKey)}")
            appendLine("fingerprint=${encode(publicExport.fingerprint)}")
            appendLine("createdAtMillis=${publicExport.createdAtMillis}")
            appendLine("cloudSync=disabled")
            appendLine("privateMaterial=excluded")
        }
    }

    fun decodePublic(text: String, nowMillis: Long = System.currentTimeMillis()): ConnectIdentityImportResult {
        val trimmed = text.trim()
        if (trimmed.length > MAX_IMPORT_LENGTH) {
            return ConnectIdentityImportResult.Failure("Identity export is too large.")
        }
        if (!trimmed.lineSequence().firstOrNull().equals(HEADER)) {
            return ConnectIdentityImportResult.Failure("Unsupported identity export format.")
        }
        val blocked = blockedPrivateFields.firstOrNull { field ->
            trimmed.contains(field, ignoreCase = true)
        }
        if (blocked != null) {
            return ConnectIdentityImportResult.Failure("Import rejected because it contains private material field: $blocked.")
        }

        val values = trimmed.lineSequence()
            .drop(1)
            .mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) {
                    null
                } else {
                    line.substring(0, separator) to line.substring(separator + 1)
                }
            }
            .toMap()

        val displayName = values["displayName"]?.let(::decode)?.trim().orEmpty()
        val publicDestination = values["publicDestination"]?.let(::decode)?.trim().orEmpty()
        val publicAppKey = values["publicAppKey"]?.let(::decode)?.trim().orEmpty()
        val fingerprint = values["fingerprint"]?.let(::decode)?.trim().orEmpty()
        val createdAtMillis = values["createdAtMillis"]?.toLongOrNull() ?: nowMillis

        if (displayName.isBlank()) return ConnectIdentityImportResult.Failure("Missing display name.")
        if (publicDestination.isBlank()) return ConnectIdentityImportResult.Failure("Missing public destination.")
        if (publicAppKey.isBlank()) return ConnectIdentityImportResult.Failure("Missing public app key.")
        if (fingerprint.isBlank()) return ConnectIdentityImportResult.Failure("Missing fingerprint.")

        val expectedFingerprint = ConnectIdentityFingerprint.from(publicDestination, publicAppKey)
        if (!fingerprint.equals(expectedFingerprint, ignoreCase = true)) {
            return ConnectIdentityImportResult.Failure("Fingerprint does not match public identity material.")
        }

        val identity = ConnectIdentity(
            displayName = displayName,
            publicDestination = publicDestination,
            publicAppKey = publicAppKey,
            fingerprint = expectedFingerprint,
            privateMaterialRef = "missing-private-material",
            privateMaterialState = ConnectIdentityPrivateMaterialState.MISSING_PRIVATE_MATERIAL,
            origin = ConnectIdentityOrigin.IMPORTED_PUBLIC_ONLY,
            cloudSyncEnabled = false,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = nowMillis
        )

        return ConnectIdentityImportResult.Success(
            identity = identity,
            warnings = listOf(
                ConnectIdentitySecurityWarnings.IMPORT_PUBLIC_ONLY,
                ConnectIdentitySecurityWarnings.PRIVATE_BACKUP_NOT_IMPLEMENTED,
                ConnectIdentitySecurityWarnings.CLOUD_SYNC_DISABLED
            )
        )
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun decode(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}
