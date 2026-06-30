package no.knoksen.i2pbrowser.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
