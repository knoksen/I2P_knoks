package no.knoksen.i2pbrowser.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Bookmark::class,
        Identity::class,
        SecureMessage::class,
        LogEntry::class,
        TrustedKey::class,
        Contact::class,
        AppSettingsEntity::class,
        ConnectIdentity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun identityDao(): IdentityDao
    abstract fun secureMessageDao(): SecureMessageDao
    abstract fun logDao(): LogDao
    abstract fun trustedKeyDao(): TrustedKeyDao
    abstract fun contactDao(): ContactDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun connectIdentityDao(): ConnectIdentityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "i2p_browser_database"
                )
                .addMigrations(*SUPPORTED_MIGRATIONS)
                .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(APP_SETTINGS_CREATE_SQL)
                db.execSQL(APP_SETTINGS_DEFAULT_INSERT_SQL)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(CONNECT_IDENTITIES_CREATE_SQL)
                db.execSQL(CONNECT_IDENTITIES_FINGERPRINT_INDEX_SQL)
            }
        }

        const val CURRENT_DATABASE_VERSION = 6
        val SUPPORTED_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_4_5, MIGRATION_5_6)
    }
}

const val APP_SETTINGS_CREATE_SQL = """
CREATE TABLE IF NOT EXISTS app_settings (
    id INTEGER NOT NULL PRIMARY KEY,
    endpointLabel TEXT NOT NULL,
    endpointHost TEXT NOT NULL,
    samPort INTEGER NOT NULL,
    httpProxyPort INTEGER NOT NULL,
    routerConsolePort INTEGER NOT NULL
)
"""

const val CONNECT_IDENTITIES_CREATE_SQL = """
CREATE TABLE IF NOT EXISTS connect_identities (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    displayName TEXT NOT NULL,
    publicDestination TEXT NOT NULL,
    publicAppKey TEXT NOT NULL,
    fingerprint TEXT NOT NULL,
    privateMaterialRef TEXT NOT NULL,
    privateMaterialState TEXT NOT NULL,
    origin TEXT NOT NULL,
    cloudSyncEnabled INTEGER NOT NULL DEFAULT 0,
    createdAtMillis INTEGER NOT NULL,
    updatedAtMillis INTEGER NOT NULL
)
"""

const val CONNECT_IDENTITIES_FINGERPRINT_INDEX_SQL = """
CREATE UNIQUE INDEX IF NOT EXISTS index_connect_identities_fingerprint
ON connect_identities (fingerprint)
"""

const val APP_SETTINGS_DEFAULT_INSERT_SQL = """
INSERT OR IGNORE INTO app_settings (
    id,
    endpointLabel,
    endpointHost,
    samPort,
    httpProxyPort,
    routerConsolePort
) VALUES (
    1,
    'Local Android Router',
    '127.0.0.1',
    7656,
    4444,
    7657
)
"""
