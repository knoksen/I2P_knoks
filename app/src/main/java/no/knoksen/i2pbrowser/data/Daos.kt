package no.knoksen.i2pbrowser.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CancellationException

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY title ASC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)
}

@Dao
interface IdentityDao {
    @Query("SELECT * FROM identities ORDER BY name ASC")
    fun getAllIdentities(): Flow<List<Identity>>

    @Query("SELECT * FROM identities WHERE i2pAddress = :address LIMIT 1")
    suspend fun getIdentityByAddress(address: String): Identity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdentity(identity: Identity)

    @Delete
    suspend fun deleteIdentity(identity: Identity)
}

@Dao
interface SecureMessageDao {
    @Query("SELECT * FROM secure_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<SecureMessage>>

    @Query("SELECT * FROM secure_messages WHERE senderAddress = :contact OR recipientAddress = :contact ORDER BY timestamp ASC")
    fun getMessagesWithContact(contact: String): Flow<List<SecureMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: SecureMessage)

    @Query("DELETE FROM secure_messages")
    suspend fun clearAllMessages()
}

@Dao
interface LogDao {
    @Query("SELECT * FROM router_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentLogs(): Flow<List<LogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntry)

    @Query("DELETE FROM router_logs")
    suspend fun clearLogs()
}

@Dao
interface TrustedKeyDao {
    @Query("SELECT * FROM trusted_keys ORDER BY alias ASC")
    fun getAllTrustedKeys(): Flow<List<TrustedKey>>

    @Query("SELECT * FROM trusted_keys WHERE i2pAddress = :address LIMIT 1")
    suspend fun getTrustedKeyByAddress(address: String): TrustedKey?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrustedKey(key: TrustedKey)

    @Delete
    suspend fun deleteTrustedKey(key: TrustedKey)
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)

    @Delete
    suspend fun deleteContact(contact: Contact)

    @Query("DELETE FROM contacts")
    suspend fun clearAllContacts()
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettings(): Flow<AppSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: AppSettingsEntity)
}

@Dao
interface ConnectIdentityDao {
    @Query("SELECT * FROM connect_identities ORDER BY createdAtMillis DESC")
    fun getAllConnectIdentities(): Flow<List<ConnectIdentity>>

    @Query("SELECT * FROM connect_identities WHERE id = :id LIMIT 1")
    suspend fun getConnectIdentityById(id: Long): ConnectIdentity?

    @Query("SELECT * FROM connect_identities WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun getConnectIdentityByFingerprint(fingerprint: String): ConnectIdentity?

    @Query("SELECT COUNT(*) FROM connect_identities")
    suspend fun countConnectIdentities(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertConnectIdentity(identity: ConnectIdentity): Long

    @Transaction
    suspend fun insertOrFindConnectIdentity(identity: ConnectIdentity): ConnectIdentityInsertOutcome {
        return try {
            val id = insertConnectIdentity(identity)
            ConnectIdentityInsertOutcome.Inserted(identity.copy(id = id))
        } catch (error: SQLiteConstraintException) {
            if (!error.isConnectIdentityFingerprintUniqueConflict()) {
                throw error
            }
            val existing = try {
                getConnectIdentityByFingerprint(identity.fingerprint)
            } catch (lookupError: CancellationException) {
                throw lookupError
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

    @Update
    suspend fun updateConnectIdentity(identity: ConnectIdentity)

    @Delete
    suspend fun deleteConnectIdentity(identity: ConnectIdentity)
}

sealed class ConnectIdentityInsertOutcome {
    data class Inserted(val identity: ConnectIdentity) : ConnectIdentityInsertOutcome()
    data class Existing(val identity: ConnectIdentity) : ConnectIdentityInsertOutcome()
    data object DuplicateLookupFailed : ConnectIdentityInsertOutcome()
}

private fun SQLiteConstraintException.isConnectIdentityFingerprintUniqueConflict(): Boolean {
    val text = message.orEmpty()
    return text.contains("connect_identities.fingerprint", ignoreCase = true) ||
        text.contains("index_connect_identities_fingerprint", ignoreCase = true)
}
