package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

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


