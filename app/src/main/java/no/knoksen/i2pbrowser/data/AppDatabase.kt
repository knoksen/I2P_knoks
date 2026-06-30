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
        AppSettingsEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun identityDao(): IdentityDao
    abstract fun secureMessageDao(): SecureMessageDao
    abstract fun logDao(): LogDao
    abstract fun trustedKeyDao(): TrustedKeyDao
    abstract fun contactDao(): ContactDao
    abstract fun appSettingsDao(): AppSettingsDao

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
                .addMigrations(MIGRATION_4_5)
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
