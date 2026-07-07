package no.knoksen.i2pbrowser

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import no.knoksen.i2pbrowser.data.AppDatabase
import no.knoksen.i2pbrowser.data.ConnectIdentity
import no.knoksen.i2pbrowser.data.ConnectIdentityOrigin
import no.knoksen.i2pbrowser.data.ConnectIdentityPrivateMaterialState
import no.knoksen.i2pbrowser.data.Contact
import no.knoksen.i2pbrowser.data.EndpointConfigLoadStatus
import no.knoksen.i2pbrowser.data.I2PRepository
import no.knoksen.i2pbrowser.i2p.I2pEndpointConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationInstrumentedTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun removeExistingDatabase() {
        context.deleteDatabase(TEST_DB)
    }

    @After
    fun cleanUpDatabase() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun migrate4To5PreservesExistingRecordsAndAddsDefaultEndpointSettings() {
        helper.createDatabase(TEST_DB, 4).apply {
            insertV4Fixtures()
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            5,
            true,
            AppDatabase.MIGRATION_4_5
        )

        migrated.use {
            assertV4FixturesPreserved(it)
            assertEquals("Local Android Router", it.queryString("SELECT endpointLabel FROM app_settings WHERE id = 1"))
            assertEquals("127.0.0.1", it.queryString("SELECT endpointHost FROM app_settings WHERE id = 1"))
            assertEquals(7656L, it.queryLong("SELECT samPort FROM app_settings WHERE id = 1"))
            assertEquals(4444L, it.queryLong("SELECT httpProxyPort FROM app_settings WHERE id = 1"))
            assertEquals(7657L, it.queryLong("SELECT routerConsolePort FROM app_settings WHERE id = 1"))
        }
    }

    @Test
    fun migrate5To6PreservesSettingsAndCreatesConnectIdentityDefaults() {
        helper.createDatabase(TEST_DB, 5).apply {
            insertV4Fixtures()
            insertEndpointSettings(host = "10.0.2.2", samPort = 17656, httpProxyPort = 14444, routerConsolePort = 17657)
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            6,
            true,
            AppDatabase.MIGRATION_5_6
        )

        migrated.use {
            assertV4FixturesPreserved(it)
            assertEquals("10.0.2.2", it.queryString("SELECT endpointHost FROM app_settings WHERE id = 1"))
            assertEquals(17656L, it.queryLong("SELECT samPort FROM app_settings WHERE id = 1"))
            assertEquals(0L, it.queryLong("SELECT COUNT(*) FROM connect_identities"))

            it.execSQL(
                """
                INSERT INTO connect_identities (
                    displayName,
                    publicDestination,
                    publicAppKey,
                    fingerprint,
                    privateMaterialRef,
                    privateMaterialState,
                    origin,
                    createdAtMillis,
                    updatedAtMillis
                ) VALUES (
                    'Migrated Public Identity',
                    'TEST_PUBLIC_DESTINATION_NOT_REAL',
                    'TEST_PUBLIC_APP_KEY_NOT_REAL',
                    'TEST-FINGERPRINT-001',
                    'local-only-ref:test-migration',
                    'PROTECTED_REFERENCE',
                    'LOCAL',
                    1700000000000,
                    1700000000000
                )
                """.trimIndent()
            )
            assertEquals(0L, it.queryLong("SELECT cloudSyncEnabled FROM connect_identities WHERE fingerprint = 'TEST-FINGERPRINT-001'"))

            val duplicateFails = runCatching {
                it.execSQL(
                    """
                    INSERT INTO connect_identities (
                        displayName,
                        publicDestination,
                        publicAppKey,
                        fingerprint,
                        privateMaterialRef,
                        privateMaterialState,
                        origin,
                        createdAtMillis,
                        updatedAtMillis
                    ) VALUES (
                        'Duplicate Public Identity',
                        'TEST_PUBLIC_DESTINATION_2_NOT_REAL',
                        'TEST_PUBLIC_APP_KEY_2_NOT_REAL',
                        'TEST-FINGERPRINT-001',
                        'local-only-ref:test-migration-2',
                        'PROTECTED_REFERENCE',
                        'LOCAL',
                        1700000001000,
                        1700000001000
                    )
                    """.trimIndent()
                )
            }.isFailure
            assertTrue("Duplicate connect identity fingerprint should fail after migration", duplicateFails)
        }
    }

    @Test
    fun migrate4ToCurrentPreservesRecordsAndRepositoryCanReadMigratedDatabase() {
        helper.createDatabase(TEST_DB, 4).apply {
            insertV4Fixtures()
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            AppDatabase.CURRENT_DATABASE_VERSION,
            true,
            *AppDatabase.SUPPORTED_MIGRATIONS
        ).close()

        val database = openCurrentDatabase()
        try {
            val repository = database.repository()
            runBlocking {
                assertEquals("Fixture Bookmark", database.bookmarkDao().getAllBookmarks().first().single().title)
                assertEquals("Fixture Identity", database.identityDao().getAllIdentities().first().single().name)
                assertEquals("fixture-contact.i2p", database.secureMessageDao().getAllMessages().first().single().recipientAddress)
                assertEquals("Fixture Contact", database.contactDao().getAllContacts().first().single().name)

                val endpointState = repository.endpointConfigState.first()
                assertEquals(EndpointConfigLoadStatus.PERSISTED_VALID, endpointState.status)
                assertEquals(I2pEndpointConfig.LOCAL_ANDROID_ROUTER, endpointState.config)
                assertTrue(database.connectIdentityDao().getAllConnectIdentities().first().isEmpty())
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun migrate5ToCurrentWithMalformedEndpointUsesBoundedRepositoryFallback() {
        helper.createDatabase(TEST_DB, 5).apply {
            insertV4Fixtures()
            insertEndpointSettings(host = "bad host", samPort = 0, httpProxyPort = 14444, routerConsolePort = 17657)
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            AppDatabase.CURRENT_DATABASE_VERSION,
            true,
            AppDatabase.MIGRATION_5_6
        ).close()

        val database = openCurrentDatabase()
        try {
            val state = runBlocking { database.repository().endpointConfigState.first() }
            assertEquals(EndpointConfigLoadStatus.PERSISTED_INVALID_FALLBACK, state.status)
            assertEquals(I2pEndpointConfig.LOCAL_ANDROID_ROUTER, state.config)
            assertTrue(state.safeMessage.orEmpty().contains("malformed"))
        } finally {
            database.close()
        }
    }

    @Test
    fun currentSchemaOpensWithoutMigrationAndDaosCanWriteCurrentData() {
        helper.createDatabase(TEST_DB, AppDatabase.CURRENT_DATABASE_VERSION).close()

        val database = openCurrentDatabase()
        try {
            runBlocking {
                database.contactDao().insertContact(
                    Contact(
                        name = "Current Fixture Contact",
                        address = "current-fixture.i2p",
                        type = "SECURE_I2P",
                        status = "OFFLINE",
                        lastActiveTimestamp = 1_700_000_002_000
                    )
                )
                database.connectIdentityDao().insertConnectIdentity(
                    ConnectIdentity(
                        displayName = "Current Public Identity",
                        publicDestination = "TEST_CURRENT_PUBLIC_DESTINATION_NOT_REAL",
                        publicAppKey = "TEST_CURRENT_PUBLIC_APP_KEY_NOT_REAL",
                        fingerprint = "TEST-CURRENT-FINGERPRINT-001",
                        privateMaterialRef = "local-only-ref:current",
                        privateMaterialState = ConnectIdentityPrivateMaterialState.PROTECTED_REFERENCE,
                        origin = ConnectIdentityOrigin.LOCAL,
                        cloudSyncEnabled = false,
                        createdAtMillis = 1_700_000_002_000,
                        updatedAtMillis = 1_700_000_002_000
                    )
                )

                assertEquals(1, database.contactDao().getAllContacts().first().size)
                val identity = database.connectIdentityDao()
                    .getConnectIdentityByFingerprint("TEST-CURRENT-FINGERPRINT-001")
                assertEquals("Current Public Identity", identity?.displayName)
                assertFalse(identity?.cloudSyncEnabled ?: true)
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun newerDatabaseVersionIsNotDowngradedDestructively() {
        helper.createDatabase(TEST_DB, AppDatabase.CURRENT_DATABASE_VERSION).apply {
            execSQL("PRAGMA user_version = 7")
            close()
        }

        val result = runCatching {
            val database = openCurrentDatabase()
            try {
                database.openHelper.writableDatabase
            } finally {
                database.close()
            }
        }

        if (result.isSuccess) {
            fail("Opening a newer database version should fail instead of downgrading destructively.")
        }
    }

    private fun openCurrentDatabase(): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(*AppDatabase.SUPPORTED_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
    }

    private fun AppDatabase.repository(): I2PRepository {
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

    private fun SupportSQLiteDatabase.insertV4Fixtures() {
        execSQL(
            """
            INSERT INTO bookmarks (id, title, url, iconName, colorHex, safetyLevel)
            VALUES (1, 'Fixture Bookmark', 'http://fixture.i2p', 'public', '#00B0FF', 'SAFE')
            """.trimIndent()
        )
        execSQL(
            """
            INSERT INTO identities (id, name, publicKeyBase64, privateKeyBase64, i2pAddress, fullDestination)
            VALUES (
                1,
                'Fixture Identity',
                'TEST_PUBLIC_KEY_NOT_REAL',
                'TEST_PRIVATE_KEY_PLACEHOLDER_NOT_REAL',
                'fixture-identity.i2p',
                'TEST_FULL_DESTINATION_NOT_REAL'
            )
            """.trimIndent()
        )
        execSQL(
            """
            INSERT INTO secure_messages (
                id,
                senderAddress,
                recipientAddress,
                encryptedPayload,
                timestamp,
                isIncoming,
                isDecrypted,
                decryptedBody
            ) VALUES (
                1,
                'fixture-sender.i2p',
                'fixture-contact.i2p',
                'TEST_PAYLOAD_NOT_REAL',
                1700000000000,
                0,
                1,
                'synthetic migration body'
            )
            """.trimIndent()
        )
        execSQL(
            """
            INSERT INTO router_logs (id, timestamp, tag, message, level)
            VALUES (1, 1700000000001, 'TEST', 'synthetic migration log', 'INFO')
            """.trimIndent()
        )
        execSQL(
            """
            INSERT INTO trusted_keys (id, alias, i2pAddress, publicKeyBase64, isVerified, addedTimestamp, sessionTagCount)
            VALUES (1, 'Fixture Trusted Key', 'trusted-fixture.i2p', 'TEST_TRUSTED_PUBLIC_KEY_NOT_REAL', 1, 1700000000002, 42)
            """.trimIndent()
        )
        execSQL(
            """
            INSERT INTO contacts (id, name, address, type, status, avatarColorHex, lastActiveTimestamp)
            VALUES (1, 'Fixture Contact', 'fixture-contact.i2p', 'SECURE_I2P', 'OFFLINE', '#00B0FF', 1700000000003)
            """.trimIndent()
        )
    }

    private fun SupportSQLiteDatabase.insertEndpointSettings(
        host: String,
        samPort: Int,
        httpProxyPort: Int,
        routerConsolePort: Int
    ) {
        execSQL(
            """
            INSERT OR REPLACE INTO app_settings (
                id,
                endpointLabel,
                endpointHost,
                samPort,
                httpProxyPort,
                routerConsolePort
            ) VALUES (
                1,
                'Migrated Test Endpoint',
                '$host',
                $samPort,
                $httpProxyPort,
                $routerConsolePort
            )
            """.trimIndent()
        )
    }

    private fun assertV4FixturesPreserved(db: SupportSQLiteDatabase) {
        assertEquals("Fixture Bookmark", db.queryString("SELECT title FROM bookmarks WHERE id = 1"))
        assertEquals("Fixture Identity", db.queryString("SELECT name FROM identities WHERE id = 1"))
        assertEquals("TEST_PAYLOAD_NOT_REAL", db.queryString("SELECT encryptedPayload FROM secure_messages WHERE id = 1"))
        assertEquals(1_700_000_000_000L, db.queryLong("SELECT timestamp FROM secure_messages WHERE id = 1"))
        assertEquals("TEST", db.queryString("SELECT tag FROM router_logs WHERE id = 1"))
        assertEquals("Fixture Trusted Key", db.queryString("SELECT alias FROM trusted_keys WHERE id = 1"))
        assertEquals("Fixture Contact", db.queryString("SELECT name FROM contacts WHERE id = 1"))
    }

    private fun SupportSQLiteDatabase.queryString(sql: String): String {
        val cursor = query(sql)
        return cursor.use {
            assertTrue("Expected at least one row for $sql", it.moveToFirst())
            it.getString(0)
        }
    }

    private fun SupportSQLiteDatabase.queryLong(sql: String): Long {
        val cursor = query(sql)
        return cursor.use {
            assertTrue("Expected at least one row for $sql", it.moveToFirst())
            it.getLong(0)
        }
    }

    private companion object {
        const val TEST_DB = "i2p-browser-migration-test.db"
    }
}
