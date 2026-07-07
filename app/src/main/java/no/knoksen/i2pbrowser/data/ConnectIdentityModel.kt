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

data class ConnectIdentityCanonicalPublicMaterial(
    val publicDestination: String,
    val publicAppKey: String
)

enum class ConnectIdentityValidationError {
    EMPTY_INPUT,
    TOO_LARGE,
    PRIVATE_MATERIAL_PRESENT,
    MALFORMED_FIELD,
    MALFORMED_ENCODING,
    MALFORMED_CHARACTER,
    MISSING_DISPLAY_NAME,
    MISSING_PUBLIC_DESTINATION,
    MISSING_PUBLIC_APP_KEY,
    MISSING_FINGERPRINT,
    FINGERPRINT_MISMATCH
}

enum class ConnectIdentityUnsupportedReason {
    UNSUPPORTED_FORMAT
}

enum class ConnectIdentityImportFailure {
    STORAGE_UNAVAILABLE,
    DUPLICATE_LOOKUP_FAILED,
    FINGERPRINT_CONFLICT
}

sealed class ConnectIdentityDecodeResult {
    data class Success(val identity: ConnectIdentity, val warnings: List<String>) : ConnectIdentityDecodeResult()
    data class Invalid(val reason: ConnectIdentityValidationError) : ConnectIdentityDecodeResult()
    data class Unsupported(val reason: ConnectIdentityUnsupportedReason) : ConnectIdentityDecodeResult()
}

sealed interface ConnectIdentityImportResult {
    data class Imported(
        val identityId: Long,
        val fingerprint: String,
        val warnings: List<String>
    ) : ConnectIdentityImportResult

    data class AlreadyExists(
        val identityId: Long,
        val fingerprint: String,
        val warnings: List<String>
    ) : ConnectIdentityImportResult

    data class Invalid(val reason: ConnectIdentityValidationError) : ConnectIdentityImportResult
    data class Unsupported(val reason: ConnectIdentityUnsupportedReason) : ConnectIdentityImportResult
    data class Failure(val category: ConnectIdentityImportFailure) : ConnectIdentityImportResult
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
        val publicMaterial = ConnectIdentityPublicCanonicalizer.from(publicDestination, publicAppKey)
        return ConnectIdentity(
            displayName = cleanDisplayName,
            publicDestination = publicMaterial.publicDestination,
            publicAppKey = publicMaterial.publicAppKey,
            fingerprint = ConnectIdentityFingerprint.from(publicMaterial),
            privateMaterialRef = privateMaterialRef,
            privateMaterialState = ConnectIdentityPrivateMaterialState.PROTECTED_REFERENCE,
            origin = ConnectIdentityOrigin.LOCAL,
            cloudSyncEnabled = false,
            createdAtMillis = nowMillis,
            updatedAtMillis = nowMillis
        )
    }
}

object ConnectIdentityPublicCanonicalizer {
    fun from(publicDestination: String, publicAppKey: String): ConnectIdentityCanonicalPublicMaterial {
        return ConnectIdentityCanonicalPublicMaterial(
            publicDestination = publicDestination.trim(),
            publicAppKey = publicAppKey.trim()
        )
    }
}

fun ConnectIdentity.hasSameCanonicalPublicMaterial(other: ConnectIdentity): Boolean {
    val thisMaterial = ConnectIdentityPublicCanonicalizer.from(publicDestination, publicAppKey)
    val otherMaterial = ConnectIdentityPublicCanonicalizer.from(other.publicDestination, other.publicAppKey)
    return thisMaterial == otherMaterial
}

object ConnectIdentityFingerprint {
    fun from(publicDestination: String, publicAppKey: String): String {
        return from(ConnectIdentityPublicCanonicalizer.from(publicDestination, publicAppKey))
    }

    fun from(publicMaterial: ConnectIdentityCanonicalPublicMaterial): String {
        val input = "${publicMaterial.publicDestination}|${publicMaterial.publicAppKey}"
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

    fun decodePublic(text: String, nowMillis: Long = System.currentTimeMillis()): ConnectIdentityDecodeResult {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return ConnectIdentityDecodeResult.Invalid(ConnectIdentityValidationError.EMPTY_INPUT)
        }
        if (trimmed.length > MAX_IMPORT_LENGTH) {
            return ConnectIdentityDecodeResult.Invalid(ConnectIdentityValidationError.TOO_LARGE)
        }
        if (!trimmed.lineSequence().firstOrNull().equals(HEADER)) {
            return ConnectIdentityDecodeResult.Unsupported(ConnectIdentityUnsupportedReason.UNSUPPORTED_FORMAT)
        }
        val blocked = blockedPrivateFields.firstOrNull { field ->
            trimmed.contains(field, ignoreCase = true)
        }
        if (blocked != null) {
            return ConnectIdentityDecodeResult.Invalid(ConnectIdentityValidationError.PRIVATE_MATERIAL_PRESENT)
        }

        val encodedValues = mutableMapOf<String, String>()
        trimmed.lineSequence().drop(1).forEach { line ->
            if (line.isBlank()) return@forEach
            val separator = line.indexOf('=')
            if (separator <= 0) {
                return ConnectIdentityDecodeResult.Invalid(ConnectIdentityValidationError.MALFORMED_FIELD)
            }
            encodedValues[line.substring(0, separator)] = line.substring(separator + 1)
        }

        val values = mutableMapOf<String, String>()
        for ((key, value) in encodedValues) {
            values[key] = try {
                decode(value).trim()
            } catch (_: IllegalArgumentException) {
                return ConnectIdentityDecodeResult.Invalid(ConnectIdentityValidationError.MALFORMED_ENCODING)
            }
        }

        val displayName = values["displayName"].orEmpty()
        val publicDestination = values["publicDestination"].orEmpty()
        val publicAppKey = values["publicAppKey"].orEmpty()
        val fingerprint = values["fingerprint"].orEmpty()
        val createdAtMillis = values["createdAtMillis"]?.toLongOrNull() ?: nowMillis

        if (displayName.isBlank()) return ConnectIdentityDecodeResult.Invalid(ConnectIdentityValidationError.MISSING_DISPLAY_NAME)
        if (publicDestination.isBlank()) return ConnectIdentityDecodeResult.Invalid(ConnectIdentityValidationError.MISSING_PUBLIC_DESTINATION)
        if (publicAppKey.isBlank()) return ConnectIdentityDecodeResult.Invalid(ConnectIdentityValidationError.MISSING_PUBLIC_APP_KEY)
        if (fingerprint.isBlank()) return ConnectIdentityDecodeResult.Invalid(ConnectIdentityValidationError.MISSING_FINGERPRINT)
        if (listOf(displayName, publicDestination, publicAppKey, fingerprint).any(::hasDisallowedControlCharacter)) {
            return ConnectIdentityDecodeResult.Invalid(ConnectIdentityValidationError.MALFORMED_CHARACTER)
        }

        val publicMaterial = ConnectIdentityPublicCanonicalizer.from(publicDestination, publicAppKey)
        val expectedFingerprint = ConnectIdentityFingerprint.from(publicMaterial)
        if (!fingerprint.equals(expectedFingerprint, ignoreCase = true)) {
            return ConnectIdentityDecodeResult.Invalid(ConnectIdentityValidationError.FINGERPRINT_MISMATCH)
        }

        val identity = ConnectIdentity(
            displayName = displayName,
            publicDestination = publicMaterial.publicDestination,
            publicAppKey = publicMaterial.publicAppKey,
            fingerprint = expectedFingerprint,
            privateMaterialRef = "missing-private-material",
            privateMaterialState = ConnectIdentityPrivateMaterialState.MISSING_PRIVATE_MATERIAL,
            origin = ConnectIdentityOrigin.IMPORTED_PUBLIC_ONLY,
            cloudSyncEnabled = false,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = nowMillis
        )

        return ConnectIdentityDecodeResult.Success(
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

    private fun hasDisallowedControlCharacter(value: String): Boolean {
        return value.any { char ->
            char.isISOControl() && char != '\t'
        }
    }
}
