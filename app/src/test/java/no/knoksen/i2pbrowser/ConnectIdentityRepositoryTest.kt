package no.knoksen.i2pbrowser

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import no.knoksen.i2pbrowser.data.AppSettingsDao
import no.knoksen.i2pbrowser.data.AppSettingsEntity
import no.knoksen.i2pbrowser.data.Bookmark
import no.knoksen.i2pbrowser.data.BookmarkDao
import no.knoksen.i2pbrowser.data.ConnectIdentity
import no.knoksen.i2pbrowser.data.ConnectIdentityDao
import no.knoksen.i2pbrowser.data.ConnectIdentityExportCodec
import no.knoksen.i2pbrowser.data.ConnectIdentityFactory
import no.knoksen.i2pbrowser.data.ConnectIdentityImportFailure
import no.knoksen.i2pbrowser.data.ConnectIdentityImportResult
import no.knoksen.i2pbrowser.data.ConnectIdentityInsertOutcome
import no.knoksen.i2pbrowser.data.ConnectIdentityOrigin
import no.knoksen.i2pbrowser.data.ConnectIdentityPrivateMaterialState
import no.knoksen.i2pbrowser.data.ConnectIdentityValidationError
import no.knoksen.i2pbrowser.data.Contact
import no.knoksen.i2pbrowser.data.ContactDao
import no.knoksen.i2pbrowser.data.I2PRepository
import no.knoksen.i2pbrowser.data.Identity
import no.knoksen.i2pbrowser.data.IdentityDao
import no.knoksen.i2pbrowser.data.LogDao
import no.knoksen.i2pbrowser.data.LogEntry
import no.knoksen.i2pbrowser.data.SecureMessage
import no.knoksen.i2pbrowser.data.SecureMessageDao
import no.knoksen.i2pbrowser.data.TrustedKey
import no.knoksen.i2pbrowser.data.TrustedKeyDao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectIdentityRepositoryTest {
    @Test
    fun `first valid public identity import stores one public-only row`() = runTest {
        val connectDao = FakeConnectIdentityDao()
        val repository = repository(connectDao)

        val result = repository.importConnectIdentityPublic(identityExport())

        assertTrue(result is ConnectIdentityImportResult.Imported)
        assertEquals(1, connectDao.countConnectIdentities())
        val stored = connectDao.single()
        assertEquals((result as ConnectIdentityImportResult.Imported).identityId, stored.id)
        assertEquals(stored.fingerprint, result.fingerprint)
        assertEquals(ConnectIdentityOrigin.IMPORTED_PUBLIC_ONLY, stored.origin)
        assertEquals(ConnectIdentityPrivateMaterialState.MISSING_PRIVATE_MATERIAL, stored.privateMaterialState)
        assertEquals("missing-private-material", stored.privateMaterialRef)
        assertFalse(stored.cloudSyncEnabled)
    }

    @Test
    fun `sequential duplicate import returns existing id and preserves metadata`() = runTest {
        val connectDao = FakeConnectIdentityDao()
        val repository = repository(connectDao)

        val first = repository.importConnectIdentityPublic(identityExport(displayName = "Original Label"))
        val second = repository.importConnectIdentityPublic(identityExport(displayName = "Different Label"))

        assertTrue(first is ConnectIdentityImportResult.Imported)
        assertTrue(second is ConnectIdentityImportResult.AlreadyExists)
        assertEquals((first as ConnectIdentityImportResult.Imported).identityId, (second as ConnectIdentityImportResult.AlreadyExists).identityId)
        assertEquals(1, connectDao.countConnectIdentities())
        assertEquals("Original Label", connectDao.single().displayName)
    }

    @Test
    fun `equivalent export representation returns duplicate without creating another row`() = runTest {
        val connectDao = FakeConnectIdentityDao()
        val repository = repository(connectDao)
        val export = identityExport()

        val first = repository.importConnectIdentityPublic(export)
        val second = repository.importConnectIdentityPublic(" \r\n${export.replace("\n", "\r\n")}\r\n ")

        assertTrue(first is ConnectIdentityImportResult.Imported)
        assertTrue(second is ConnectIdentityImportResult.AlreadyExists)
        assertEquals(1, connectDao.countConnectIdentities())
    }

    @Test
    fun `different public identities do not collide through metadata`() = runTest {
        val connectDao = FakeConnectIdentityDao()
        val repository = repository(connectDao)

        val first = repository.importConnectIdentityPublic(identityExport(displayName = "Shared Label"))
        val second = repository.importConnectIdentityPublic(
            identityExport(
                displayName = "Shared Label",
                publicDestination = "different-public-destination.b32.i2p",
                publicAppKey = "different-public-app-key"
            )
        )

        assertTrue(first is ConnectIdentityImportResult.Imported)
        assertTrue(second is ConnectIdentityImportResult.Imported)
        assertEquals(2, connectDao.countConnectIdentities())
        assertFalse((first as ConnectIdentityImportResult.Imported).fingerprint == (second as ConnectIdentityImportResult.Imported).fingerprint)
    }

    @Test
    fun `concurrent duplicate imports resolve to one row and one stable identity`() = runTest {
        val connectDao = FakeConnectIdentityDao()
        val repository = repository(connectDao)
        val gate = CompletableDeferred<Unit>()
        connectDao.blockInsertsUntil(gate, expectedWaiters = 8)

        val imports = (1..8).map {
            async {
                repository.importConnectIdentityPublic(identityExport())
            }
        }

        connectDao.awaitAllInsertAttempts()
        gate.complete(Unit)
        val results = imports.awaitAll()

        assertEquals(1, connectDao.countConnectIdentities())
        assertEquals(1, results.count { it is ConnectIdentityImportResult.Imported })
        assertEquals(7, results.count { it is ConnectIdentityImportResult.AlreadyExists })
        val ids = results.map {
            when (it) {
                is ConnectIdentityImportResult.Imported -> it.identityId
                is ConnectIdentityImportResult.AlreadyExists -> it.identityId
                else -> error("unexpected result $it")
            }
        }.toSet()
        assertEquals(setOf(connectDao.single().id), ids)
    }

    @Test
    fun `invalid and unsupported public identity inputs do not reach persistence`() = runTest {
        val connectDao = FakeConnectIdentityDao()
        val repository = repository(connectDao)

        val invalid = repository.importConnectIdentityPublic("I2P_CONNECT_IDENTITY_V1\ndisplayName=%\n")
        val unsupported = repository.importConnectIdentityPublic("I2P_CONNECT_IDENTITY_V99\n")

        assertEquals(ConnectIdentityImportResult.Invalid(ConnectIdentityValidationError.MALFORMED_ENCODING), invalid)
        assertTrue(unsupported is ConnectIdentityImportResult.Unsupported)
        assertEquals(0, connectDao.countConnectIdentities())
    }

    @Test
    fun `non uniqueness database error maps to bounded storage failure`() = runTest {
        val connectDao = FakeConnectIdentityDao().apply {
            insertFailure = SQLiteException("raw database path C:/private/app.db")
        }
        val logDao = CapturingLogDao()
        val repository = repository(connectDao, logDao)

        val result = repository.importConnectIdentityPublic(identityExport())

        assertEquals(ConnectIdentityImportResult.Failure(ConnectIdentityImportFailure.STORAGE_UNAVAILABLE), result)
        assertEquals(0, connectDao.countConnectIdentities())
        assertFalse(logDao.messages.joinToString("\n").contains("C:/private"))
    }

    @Test
    fun `duplicate lookup failure is not misclassified as already exists`() = runTest {
        val connectDao = FakeConnectIdentityDao()
        val repository = repository(connectDao)

        repository.importConnectIdentityPublic(identityExport())
        connectDao.failDuplicateLookup = true

        val result = repository.importConnectIdentityPublic(identityExport())

        assertEquals(ConnectIdentityImportResult.Failure(ConnectIdentityImportFailure.DUPLICATE_LOOKUP_FAILED), result)
        assertEquals(1, connectDao.countConnectIdentities())
    }

    @Test
    fun `cancellation propagates and does not emit terminal success`() = runTest {
        val connectDao = FakeConnectIdentityDao()
        val repository = repository(connectDao)
        val gate = CompletableDeferred<Unit>()
        connectDao.blockInsertsUntil(gate, expectedWaiters = 1)

        val importJob = async {
            repository.importConnectIdentityPublic(identityExport())
        }

        connectDao.awaitAllInsertAttempts()
        importJob.cancel()
        val cancelled = runCatching { importJob.await() }.exceptionOrNull()

        assertTrue(cancelled is CancellationException)
        assertEquals(0, connectDao.countConnectIdentities())
    }
}

private fun repository(
    connectIdentityDao: ConnectIdentityDao,
    logDao: LogDao = CapturingLogDao()
): I2PRepository {
    return I2PRepository(
        bookmarkDao = NoopBookmarkDao,
        identityDao = NoopIdentityDao,
        secureMessageDao = NoopSecureMessageDao,
        logDao = logDao,
        trustedKeyDao = NoopTrustedKeyDao,
        contactDao = NoopContactDao,
        appSettingsDao = NoopAppSettingsDao,
        connectIdentityDao = connectIdentityDao
    )
}

private fun identityExport(
    displayName: String = "Public Test Identity",
    publicDestination: String = "public-destination-for-repository-tests.b32.i2p",
    publicAppKey: String = "connect-public-key-for-repository-tests"
): String {
    return ConnectIdentityExportCodec.encodePublic(
        ConnectIdentityFactory.createLocal(
            displayName = displayName,
            publicDestination = publicDestination,
            publicAppKey = publicAppKey,
            nowMillis = 1_700_000_000_000,
            privateMaterialRef = "local-only-ref:test"
        )
    )
}

private class FakeConnectIdentityDao : ConnectIdentityDao {
    private val mutex = Mutex()
    private val rows = linkedMapOf<String, ConnectIdentity>()
    private var nextId = 1L
    private var insertGate: CompletableDeferred<Unit>? = null
    private var allWaiting: CompletableDeferred<Unit>? = null
    private var expectedWaiters = 0
    private val waiters = AtomicInteger(0)

    var insertFailure: RuntimeException? = null
    var failDuplicateLookup: Boolean = false

    fun blockInsertsUntil(gate: CompletableDeferred<Unit>, expectedWaiters: Int) {
        insertGate = gate
        allWaiting = CompletableDeferred()
        this.expectedWaiters = expectedWaiters
    }

    suspend fun awaitAllInsertAttempts() {
        allWaiting?.await()
    }

    suspend fun single(): ConnectIdentity = mutex.withLock { rows.values.single() }

    override fun getAllConnectIdentities(): Flow<List<ConnectIdentity>> = flowOf(rows.values.toList())

    override suspend fun getConnectIdentityById(id: Long): ConnectIdentity? = mutex.withLock {
        rows.values.firstOrNull { it.id == id }
    }

    override suspend fun getConnectIdentityByFingerprint(fingerprint: String): ConnectIdentity? {
        if (failDuplicateLookup) {
            throw SQLiteException("lookup failed")
        }
        return mutex.withLock { rows[fingerprint] }
    }

    override suspend fun countConnectIdentities(): Int = mutex.withLock { rows.size }

    override suspend fun insertConnectIdentity(identity: ConnectIdentity): Long {
        insertGate?.let { gate ->
            if (waiters.incrementAndGet() == expectedWaiters) {
                allWaiting?.complete(Unit)
            }
            gate.await()
        }
        insertFailure?.let { throw it }
        return mutex.withLock {
            if (rows.containsKey(identity.fingerprint)) {
                throw SQLiteConstraintException("UNIQUE constraint failed: connect_identities.fingerprint")
            }
            val stored = identity.copy(id = nextId++)
            rows[stored.fingerprint] = stored
            stored.id
        }
    }

    override suspend fun insertOrFindConnectIdentity(identity: ConnectIdentity): ConnectIdentityInsertOutcome {
        return try {
            val id = insertConnectIdentity(identity)
            ConnectIdentityInsertOutcome.Inserted(identity.copy(id = id))
        } catch (_: SQLiteConstraintException) {
            val existing = try {
                getConnectIdentityByFingerprint(identity.fingerprint)
            } catch (_: Exception) {
                null
            }
            if (existing == null) {
                ConnectIdentityInsertOutcome.DuplicateLookupFailed
            } else {
                ConnectIdentityInsertOutcome.Existing(existing)
            }
        }
    }

    override suspend fun updateConnectIdentity(identity: ConnectIdentity) {
        mutex.withLock {
            rows[identity.fingerprint] = identity
        }
    }

    override suspend fun deleteConnectIdentity(identity: ConnectIdentity) {
        mutex.withLock {
            rows.remove(identity.fingerprint)
        }
    }
}

private class CapturingLogDao : LogDao {
    val messages = mutableListOf<String>()

    override fun getRecentLogs(): Flow<List<LogEntry>> = flowOf(emptyList())

    override suspend fun insertLog(log: LogEntry) {
        messages += log.message
    }

    override suspend fun clearLogs() {
        messages.clear()
    }
}

private object NoopBookmarkDao : BookmarkDao {
    override fun getAllBookmarks(): Flow<List<Bookmark>> = flowOf(emptyList())
    override suspend fun insertBookmark(bookmark: Bookmark) = Unit
    override suspend fun deleteBookmark(bookmark: Bookmark) = Unit
}

private object NoopIdentityDao : IdentityDao {
    override fun getAllIdentities(): Flow<List<Identity>> = flowOf(emptyList())
    override suspend fun getIdentityByAddress(address: String): Identity? = null
    override suspend fun insertIdentity(identity: Identity) = Unit
    override suspend fun deleteIdentity(identity: Identity) = Unit
}

private object NoopSecureMessageDao : SecureMessageDao {
    override fun getAllMessages(): Flow<List<SecureMessage>> = flowOf(emptyList())
    override fun getMessagesWithContact(contact: String): Flow<List<SecureMessage>> = flowOf(emptyList())
    override suspend fun insertMessage(message: SecureMessage) = Unit
    override suspend fun clearAllMessages() = Unit
}

private object NoopTrustedKeyDao : TrustedKeyDao {
    override fun getAllTrustedKeys(): Flow<List<TrustedKey>> = flowOf(emptyList())
    override suspend fun getTrustedKeyByAddress(address: String): TrustedKey? = null
    override suspend fun insertTrustedKey(key: TrustedKey) = Unit
    override suspend fun deleteTrustedKey(key: TrustedKey) = Unit
}

private object NoopContactDao : ContactDao {
    override fun getAllContacts(): Flow<List<Contact>> = flowOf(emptyList())
    override suspend fun insertContact(contact: Contact) = Unit
    override suspend fun deleteContact(contact: Contact) = Unit
    override suspend fun clearAllContacts() = Unit
}

private object NoopAppSettingsDao : AppSettingsDao {
    override fun getSettings(): Flow<AppSettingsEntity?> = flowOf(null)
    override suspend fun upsertSettings(settings: AppSettingsEntity) = Unit
}
