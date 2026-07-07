package no.knoksen.i2pbrowser

import no.knoksen.i2pbrowser.data.AppDatabase
import no.knoksen.i2pbrowser.data.APP_SETTINGS_CREATE_SQL
import no.knoksen.i2pbrowser.data.APP_SETTINGS_DEFAULT_INSERT_SQL
import no.knoksen.i2pbrowser.data.CONNECT_IDENTITIES_CREATE_SQL
import no.knoksen.i2pbrowser.data.CONNECT_IDENTITIES_FINGERPRINT_INDEX_SQL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AppDatabaseMigrationTest {
    @Test
    fun `current database version and migration graph are explicit`() {
        assertEquals(6, AppDatabase.CURRENT_DATABASE_VERSION)
        assertEquals(
            listOf(4 to 5, 5 to 6),
            AppDatabase.SUPPORTED_MIGRATIONS.map { it.startVersion to it.endVersion }
        )
    }

    @Test
    fun `schema files are committed for every supported Room version`() {
        val schemaDir = findSchemaDirectory()
        val expectedFiles = listOf("4.json", "5.json", "6.json")

        expectedFiles.forEach { name ->
            assertTrue("Missing schema file $name", schemaDir.resolve(name).isFile)
        }
    }

    @Test
    fun `schema files reflect supported table evolution`() {
        val schemaDir = findSchemaDirectory()
        val v4 = schemaDir.resolve("4.json").readText()
        val v5 = schemaDir.resolve("5.json").readText()
        val v6 = schemaDir.resolve("6.json").readText()

        assertTrue(v4.contains("\"version\": 4"))
        assertTrue(v4.contains("\"tableName\": \"bookmarks\""))
        assertTrue(v4.contains("\"tableName\": \"contacts\""))
        assertFalse(v4.contains("\"tableName\": \"app_settings\""))
        assertFalse(v4.contains("\"tableName\": \"connect_identities\""))

        assertTrue(v5.contains("\"version\": 5"))
        assertTrue(v5.contains("\"tableName\": \"app_settings\""))
        assertTrue(v5.contains("\"columnName\": \"endpointHost\""))
        assertFalse(v5.contains("\"tableName\": \"connect_identities\""))

        assertTrue(v6.contains("\"version\": 6"))
        assertTrue(v6.contains("\"tableName\": \"connect_identities\""))
        assertTrue(v6.contains("\"name\": \"index_connect_identities_fingerprint\""))
        assertTrue(v6.contains("\"defaultValue\": \"0\""))
    }

    @Test
    fun `current database builder does not use destructive migration fallback`() {
        val source = findSourceFile("app/src/main/java/no/knoksen/i2pbrowser/data/AppDatabase.kt").readText()

        assertFalse(source.contains("fallbackToDestructiveMigration("))
        assertFalse(source.contains("fallbackToDestructiveMigrationFrom("))
        assertFalse(source.contains("fallbackToDestructiveMigrationOnDowngrade("))
    }

    @Test
    fun `v4 to v5 migration creates app settings table`() {
        val sql = APP_SETTINGS_CREATE_SQL

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS app_settings"))
        assertTrue(sql.contains("id INTEGER NOT NULL PRIMARY KEY"))
        assertTrue(sql.contains("endpointLabel TEXT NOT NULL"))
        assertTrue(sql.contains("endpointHost TEXT NOT NULL"))
        assertTrue(sql.contains("samPort INTEGER NOT NULL"))
        assertTrue(sql.contains("httpProxyPort INTEGER NOT NULL"))
        assertTrue(sql.contains("routerConsolePort INTEGER NOT NULL"))
    }

    @Test
    fun `v4 to v5 migration inserts default local Android router row`() {
        val sql = APP_SETTINGS_DEFAULT_INSERT_SQL

        assertTrue(sql.contains("INSERT OR IGNORE INTO app_settings"))
        assertTrue(sql.contains("'Local Android Router'"))
        assertTrue(sql.contains("'127.0.0.1'"))
        assertTrue(sql.contains("7656"))
        assertTrue(sql.contains("4444"))
        assertTrue(sql.contains("7657"))
    }

    @Test
    fun `v5 to v6 migration creates connect identities table without private key columns`() {
        val sql = CONNECT_IDENTITIES_CREATE_SQL

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS connect_identities"))
        assertTrue(sql.contains("displayName TEXT NOT NULL"))
        assertTrue(sql.contains("publicDestination TEXT NOT NULL"))
        assertTrue(sql.contains("publicAppKey TEXT NOT NULL"))
        assertTrue(sql.contains("fingerprint TEXT NOT NULL"))
        assertTrue(sql.contains("privateMaterialRef TEXT NOT NULL"))
        assertTrue(sql.contains("privateMaterialState TEXT NOT NULL"))
        assertTrue(sql.contains("cloudSyncEnabled INTEGER NOT NULL DEFAULT 0"))
        assertTrue(sql.contains("createdAtMillis INTEGER NOT NULL"))
        assertTrue(sql.contains("updatedAtMillis INTEGER NOT NULL"))
        assertTrue(!sql.contains("privateKeyBase64"))
        assertTrue(!sql.contains("privateDestination TEXT"))
    }

    @Test
    fun `v5 to v6 migration creates unique fingerprint index`() {
        val sql = CONNECT_IDENTITIES_FINGERPRINT_INDEX_SQL

        assertTrue(sql.contains("CREATE UNIQUE INDEX IF NOT EXISTS index_connect_identities_fingerprint"))
        assertTrue(sql.contains("ON connect_identities (fingerprint)"))
    }

    private fun findSchemaDirectory(): File {
        val candidates = listOf(
            File("app/schemas/no.knoksen.i2pbrowser.data.AppDatabase"),
            File("../app/schemas/no.knoksen.i2pbrowser.data.AppDatabase")
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Could not locate Room schema directory from ${File(".").absolutePath}")
    }

    private fun findSourceFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        return candidates.firstOrNull { it.isFile }
            ?: error("Could not locate $path from ${File(".").absolutePath}")
    }
}
