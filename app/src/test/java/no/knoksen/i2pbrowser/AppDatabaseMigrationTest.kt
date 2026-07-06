package no.knoksen.i2pbrowser

import no.knoksen.i2pbrowser.data.APP_SETTINGS_CREATE_SQL
import no.knoksen.i2pbrowser.data.APP_SETTINGS_DEFAULT_INSERT_SQL
import no.knoksen.i2pbrowser.data.CONNECT_IDENTITIES_CREATE_SQL
import no.knoksen.i2pbrowser.data.CONNECT_IDENTITIES_FINGERPRINT_INDEX_SQL
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDatabaseMigrationTest {
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
}
