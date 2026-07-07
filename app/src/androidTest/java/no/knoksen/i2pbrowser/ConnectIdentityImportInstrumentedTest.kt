package no.knoksen.i2pbrowser

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import no.knoksen.i2pbrowser.data.AppDatabase
import no.knoksen.i2pbrowser.data.ConnectIdentity
import no.knoksen.i2pbrowser.data.ConnectIdentityExportCodec
import no.knoksen.i2pbrowser.data.ConnectIdentityFactory
import no.knoksen.i2pbrowser.data.ConnectIdentityImportResult
import no.knoksen.i2pbrowser.data.ConnectIdentityOrigin
import no.knoksen.i2pbrowser.data.ConnectIdentityPrivateMaterialState
import no.knoksen.i2pbrowser.data.I2PRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConnectIdentityImportInstrumentedTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var database: AppDatabase
    private lateinit var repository: I2PRepository

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = database.toRepository()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun firstAndSequentialDuplicateImportUseOneStableRoomRow() = runBlocking {
        val first = repository.importConnectIdentityPublic(identityExport(displayName = "Original Label"))
        val second = repository.importConnectIdentityPublic(identityExport(displayName = "Changed Label"))

        assertTrue(first is ConnectIdentityImportResult.Imported)
        assertTrue(second is ConnectIdentityImportResult.AlreadyExists)
        assertEquals((first as ConnectIdentityImportResult.Imported).identityId, (second as ConnectIdentityImportResult.AlreadyExists).identityId)
        assertEquals(1, database.connectIdentityDao().countConnectIdentities())

        val stored = database.connectIdentityDao().getConnectIdentityByFingerprint(first.fingerprint)
        assertEquals(first.identityId, stored?.id)
        assertEquals("Original Label", stored?.displayName)
        assertEquals(ConnectIdentityOrigin.IMPORTED_PUBLIC_ONLY, stored?.origin)
        assertEquals(ConnectIdentityPrivateMaterialState.MISSING_PRIVATE_MATERIAL, stored?.privateMaterialState)
        assertEquals("missing-private-material", stored?.privateMaterialRef)
        assertFalse(stored?.cloudSyncEnabled ?: true)
    }

    @Test
    fun uniqueFingerprintIndexStillRejectsRawDuplicateInsert() = runBlocking {
        val imported = repository.importConnectIdentityPublic(identityExport())
        assertTrue(imported is ConnectIdentityImportResult.Imported)
        val stored = database.connectIdentityDao().getAllConnectIdentities().first().single()

        try {
            database.connectIdentityDao().insertConnectIdentity(
                stored.copy(id = 0, displayName = "Duplicate Raw Insert")
            )
            fail("Raw duplicate fingerprint insert should fail through SQLite unique index")
        } catch (_: SQLiteConstraintException) {
            // Expected: the repository maps this path, but the raw DAO insert remains protected by SQLite.
        }

        assertEquals(1, database.connectIdentityDao().countConnectIdentities())
    }

    @Test
    fun concurrentDuplicateImportsCreateOneRoomRow() = runBlocking {
        val start = CompletableDeferred<Unit>()
        val imports = (1..8).map {
            async(Dispatchers.Default) {
                start.await()
                repository.importConnectIdentityPublic(identityExport())
            }
        }

        start.complete(Unit)
        val results = imports.awaitAll()

        assertEquals(1, database.connectIdentityDao().countConnectIdentities())
        assertEquals(1, results.count { it is ConnectIdentityImportResult.Imported })
        assertEquals(7, results.count { it is ConnectIdentityImportResult.AlreadyExists })

        val stored = database.connectIdentityDao().getAllConnectIdentities().first().single()
        val ids = results.map {
            when (it) {
                is ConnectIdentityImportResult.Imported -> it.identityId
                is ConnectIdentityImportResult.AlreadyExists -> it.identityId
                else -> fail("Unexpected import result: $it")
            }
        }.toSet()
        assertEquals(setOf(stored.id), ids)
    }

    @Test
    fun distinctPublicIdentitiesCreateDistinctRoomRows() = runBlocking {
        val first = repository.importConnectIdentityPublic(identityExport())
        val second = repository.importConnectIdentityPublic(
            identityExport(
                publicDestination = "second-public-destination.b32.i2p",
                publicAppKey = "second-public-app-key"
            )
        )

        assertTrue(first is ConnectIdentityImportResult.Imported)
        assertTrue(second is ConnectIdentityImportResult.Imported)
        assertEquals(2, database.connectIdentityDao().countConnectIdentities())
        assertFalse((first as ConnectIdentityImportResult.Imported).fingerprint == (second as ConnectIdentityImportResult.Imported).fingerprint)
    }

    @Test
    fun invalidPublicIdentityImportDoesNotCreateRoomRow() = runBlocking {
        val result = repository.importConnectIdentityPublic("I2P_CONNECT_IDENTITY_V1\ndisplayName=%\n")

        assertTrue(result is ConnectIdentityImportResult.Invalid)
        assertEquals(0, database.connectIdentityDao().countConnectIdentities())
    }
}

private fun AppDatabase.toRepository(): I2PRepository {
    return I2PRepository(
        bookmarkDao = bookmarkDao(),
        identityDao = identityDao(),
        secureMessageDao = secureMessageDao(),
        logDao = logDao(),
        trustedKeyDao = trustedKeyDao(),
        contactDao = contactDao(),
        appSettingsDao = appSettingsDao(),
        connectIdentityDao = connectIdentityDao()
    )
}

private fun identityExport(
    displayName: String = "Room Public Identity",
    publicDestination: String = "room-public-destination.b32.i2p",
    publicAppKey: String = "room-public-app-key"
): String {
    val identity = ConnectIdentityFactory.createLocal(
        displayName = displayName,
        publicDestination = publicDestination,
        publicAppKey = publicAppKey,
        nowMillis = 1_700_000_000_000,
        privateMaterialRef = "local-only-ref:room-test"
    )
    return ConnectIdentityExportCodec.encodePublic(
        ConnectIdentity(
            displayName = identity.displayName,
            publicDestination = identity.publicDestination,
            publicAppKey = identity.publicAppKey,
            fingerprint = identity.fingerprint,
            privateMaterialRef = identity.privateMaterialRef,
            privateMaterialState = identity.privateMaterialState,
            origin = identity.origin,
            cloudSyncEnabled = false,
            createdAtMillis = identity.createdAtMillis,
            updatedAtMillis = identity.updatedAtMillis
        )
    )
}
