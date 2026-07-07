package no.knoksen.i2pbrowser

import no.knoksen.i2pbrowser.data.ConnectIdentityExportCodec
import no.knoksen.i2pbrowser.data.ConnectIdentityFactory
import no.knoksen.i2pbrowser.data.ConnectIdentityFingerprint
import no.knoksen.i2pbrowser.data.ConnectIdentityDecodeResult
import no.knoksen.i2pbrowser.data.ConnectIdentityOrigin
import no.knoksen.i2pbrowser.data.ConnectIdentityPrivateMaterialState
import no.knoksen.i2pbrowser.data.ConnectIdentitySecurityWarnings
import no.knoksen.i2pbrowser.data.ConnectIdentityUnsupportedReason
import no.knoksen.i2pbrowser.data.ConnectIdentityValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectIdentityModelTest {
    @Test
    fun `local identity stores only private material reference and disables cloud sync`() {
        val identity = ConnectIdentityFactory.createLocal(
            displayName = "  Local Circle  ",
            publicDestination = ConnectIdentityTestData.PUBLIC_DESTINATION,
            publicAppKey = ConnectIdentityTestData.PUBLIC_APP_KEY,
            nowMillis = 1_700_000_000_000,
            privateMaterialRef = ConnectIdentityTestData.PRIVATE_MATERIAL_REF
        )

        assertEquals("Local Circle", identity.displayName)
        assertEquals(ConnectIdentityPrivateMaterialState.PROTECTED_REFERENCE, identity.privateMaterialState)
        assertEquals(ConnectIdentityOrigin.LOCAL, identity.origin)
        assertEquals(ConnectIdentityTestData.PRIVATE_MATERIAL_REF, identity.privateMaterialRef)
        assertFalse(identity.cloudSyncEnabled)
        assertFalse(identity.privateMaterialRef.contains(ConnectIdentityTestData.PRIVATE_KEY_MATERIAL))
    }

    @Test
    fun `fingerprint is deterministic from public material`() {
        val first = ConnectIdentityFingerprint.from(
            ConnectIdentityTestData.PUBLIC_DESTINATION,
            ConnectIdentityTestData.PUBLIC_APP_KEY
        )
        val second = ConnectIdentityFingerprint.from(
            ConnectIdentityTestData.PUBLIC_DESTINATION,
            ConnectIdentityTestData.PUBLIC_APP_KEY
        )

        assertEquals(first, second)
        assertEquals(8, first.split("-").size)
    }

    @Test
    fun `public export excludes private material and carries safety warnings`() {
        val identity = ConnectIdentityTestData.localIdentity()

        val publicExport = ConnectIdentityExportCodec.toPublicExport(identity)
        val encoded = ConnectIdentityExportCodec.encodePublic(identity)

        assertTrue(publicExport.warnings.contains(ConnectIdentitySecurityWarnings.EXPORT_PUBLIC_ONLY))
        assertTrue(publicExport.warnings.contains(ConnectIdentitySecurityWarnings.CLOUD_SYNC_DISABLED))
        assertFalse(encoded.contains(identity.privateMaterialRef))
        assertFalse(encoded.contains(ConnectIdentityTestData.PRIVATE_KEY_MATERIAL))
        assertTrue(encoded.contains("privateMaterial=excluded"))
        assertTrue(encoded.contains("cloudSync=disabled"))
    }

    @Test
    fun `public import creates public-only identity without cloud sync`() {
        val encoded = ConnectIdentityExportCodec.encodePublic(ConnectIdentityTestData.localIdentity())

        val result = ConnectIdentityExportCodec.decodePublic(encoded, nowMillis = 1_700_000_001_000)

        assertTrue(result is ConnectIdentityDecodeResult.Success)
        val success = result as ConnectIdentityDecodeResult.Success
        assertEquals(ConnectIdentityOrigin.IMPORTED_PUBLIC_ONLY, success.identity.origin)
        assertEquals(ConnectIdentityPrivateMaterialState.MISSING_PRIVATE_MATERIAL, success.identity.privateMaterialState)
        assertEquals("missing-private-material", success.identity.privateMaterialRef)
        assertFalse(success.identity.cloudSyncEnabled)
        assertTrue(success.warnings.contains(ConnectIdentitySecurityWarnings.IMPORT_PUBLIC_ONLY))
        assertTrue(success.warnings.contains(ConnectIdentitySecurityWarnings.PRIVATE_BACKUP_NOT_IMPLEMENTED))
    }

    @Test
    fun `public import rejects private material fields`() {
        val encoded = ConnectIdentityExportCodec.encodePublic(ConnectIdentityTestData.localIdentity()) +
            "privateMaterialRef=${ConnectIdentityTestData.PRIVATE_MATERIAL_REF}\n"

        val result = ConnectIdentityExportCodec.decodePublic(encoded)

        assertEquals(ConnectIdentityDecodeResult.Invalid(ConnectIdentityValidationError.PRIVATE_MATERIAL_PRESENT), result)
    }

    @Test
    fun `public import rejects tampered fingerprint`() {
        val encoded = ConnectIdentityExportCodec.encodePublic(ConnectIdentityTestData.localIdentity())
            .replace("fingerprint=", "fingerprint=TAMPERED-")

        val result = ConnectIdentityExportCodec.decodePublic(encoded)

        assertEquals(ConnectIdentityDecodeResult.Invalid(ConnectIdentityValidationError.FINGERPRINT_MISMATCH), result)
    }

    @Test
    fun `public import accepts surrounding whitespace and line ending differences`() {
        val encoded = ConnectIdentityExportCodec.encodePublic(ConnectIdentityTestData.localIdentity())
            .replace("\n", "\r\n")

        val first = ConnectIdentityExportCodec.decodePublic(encoded)
        val second = ConnectIdentityExportCodec.decodePublic(" \n$encoded\n ")

        assertTrue(first is ConnectIdentityDecodeResult.Success)
        assertTrue(second is ConnectIdentityDecodeResult.Success)
        assertEquals(
            (first as ConnectIdentityDecodeResult.Success).identity.fingerprint,
            (second as ConnectIdentityDecodeResult.Success).identity.fingerprint
        )
    }

    @Test
    fun `fingerprint ignores surrounding public material whitespace but not metadata`() {
        val first = ConnectIdentityFingerprint.from(
            "  ${ConnectIdentityTestData.PUBLIC_DESTINATION}  ",
            "\n${ConnectIdentityTestData.PUBLIC_APP_KEY}\t"
        )
        val second = ConnectIdentityFingerprint.from(
            ConnectIdentityTestData.PUBLIC_DESTINATION,
            ConnectIdentityTestData.PUBLIC_APP_KEY
        )

        assertEquals(first, second)
    }

    @Test
    fun `unsupported header is classified separately from invalid content`() {
        val result = ConnectIdentityExportCodec.decodePublic("I2P_CONNECT_IDENTITY_V99\n")

        assertEquals(ConnectIdentityDecodeResult.Unsupported(ConnectIdentityUnsupportedReason.UNSUPPORTED_FORMAT), result)
    }

    @Test
    fun `malformed percent encoding is rejected before persistence`() {
        val encoded = ConnectIdentityExportCodec.encodePublic(ConnectIdentityTestData.localIdentity())
            .replace("displayName=", "displayName=%")

        val result = ConnectIdentityExportCodec.decodePublic(encoded)

        assertEquals(ConnectIdentityDecodeResult.Invalid(ConnectIdentityValidationError.MALFORMED_ENCODING), result)
    }

    @Test
    fun `oversized public identity export is rejected`() {
        val result = ConnectIdentityExportCodec.decodePublic(
            "I2P_CONNECT_IDENTITY_V1\n" + "x".repeat(12_001)
        )

        assertEquals(ConnectIdentityDecodeResult.Invalid(ConnectIdentityValidationError.TOO_LARGE), result)
    }
}

private object ConnectIdentityTestData {
    const val PUBLIC_DESTINATION = "public-destination-for-tests.b32.i2p"
    const val PUBLIC_APP_KEY = "connect-public-key-for-tests"
    const val PRIVATE_MATERIAL_REF = "local-only-ref:test-identity"
    const val PRIVATE_KEY_MATERIAL = "private-key-material-that-must-not-export"

    fun localIdentity() = ConnectIdentityFactory.createLocal(
        displayName = "Test Local",
        publicDestination = PUBLIC_DESTINATION,
        publicAppKey = PUBLIC_APP_KEY,
        nowMillis = 1_700_000_000_000,
        privateMaterialRef = PRIVATE_MATERIAL_REF
    )
}
