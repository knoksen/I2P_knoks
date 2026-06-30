package no.knoksen.i2pbrowser.data

import kotlinx.coroutines.flow.Flow
import java.security.KeyPairGenerator
import java.security.KeyPair
import android.util.Base64
import java.util.UUID

class I2PRepository(
    private val bookmarkDao: BookmarkDao,
    private val identityDao: IdentityDao,
    private val secureMessageDao: SecureMessageDao,
    private val logDao: LogDao,
    private val trustedKeyDao: TrustedKeyDao,
    private val contactDao: ContactDao
) {
    val allBookmarks: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks()
    val allIdentities: Flow<List<Identity>> = identityDao.getAllIdentities()
    val allMessages: Flow<List<SecureMessage>> = secureMessageDao.getAllMessages()
    val recentLogs: Flow<List<LogEntry>> = logDao.getRecentLogs()
    val allTrustedKeys: Flow<List<TrustedKey>> = trustedKeyDao.getAllTrustedKeys()
    val allContacts: Flow<List<Contact>> = contactDao.getAllContacts()

    suspend fun addContact(name: String, address: String, type: String, status: String = "ONLINE", avatarColorHex: String = "#00B0FF") {
        contactDao.insertContact(
            Contact(
                name = name,
                address = address,
                type = type,
                status = status,
                avatarColorHex = avatarColorHex
            )
        )
        addLog("CONTACTS", "Added new contact: $name ($type - $address)", "SUCCESS")
    }

    suspend fun removeContact(contact: Contact) {
        contactDao.deleteContact(contact)
        addLog("CONTACTS", "Deleted contact: ${contact.name}", "WARN")
    }

    suspend fun addBookmark(title: String, url: String, iconName: String = "public", colorHex: String = "#00B0FF", safetyLevel: String = "SAFE") {
        bookmarkDao.insertBookmark(Bookmark(title = title, url = url, iconName = iconName, colorHex = colorHex, safetyLevel = safetyLevel))
    }

    suspend fun removeBookmark(bookmark: Bookmark) {
        bookmarkDao.deleteBookmark(bookmark)
    }

    suspend fun addLog(tag: String, message: String, level: String = "INFO") {
        logDao.insertLog(LogEntry(tag = tag, message = message, level = level))
    }

    suspend fun clearLogs() {
        logDao.clearLogs()
    }

    suspend fun clearAllMessages() {
        secureMessageDao.clearAllMessages()
    }

    suspend fun sendMessage(sender: String, recipient: String, body: String) {
        addLog("CRYPT", "Demo messenger payload encoded locally for $recipient. Not audited cryptography.", "WARN")
        addLog("ROUTING", "Simulating Garlic Packet wrapping for UI preview.", "ROUTING")
        addLog("TUNNEL", "Simulated outbound tunnel event recorded.", "INFO")

        // Base64 is storage/demo encoding, not encryption.
        val dummyCipher = Base64.encodeToString(body.toByteArray(), Base64.NO_WRAP)
        
        val secureMsg = SecureMessage(
            senderAddress = sender,
            recipientAddress = recipient,
            encryptedPayload = dummyCipher,
            isIncoming = false,
            isDecrypted = true,
            decryptedBody = body
        )
        secureMessageDao.insertMessage(secureMsg)
        addLog("TUNNEL", "Demo message stored locally for destination $recipient.", "INFO")

        // Trigger an automatic response simulation on selected known endpoints to make communication active and beautiful
        if (recipient.endsWith(".i2p")) {
            simulateAutoResponse(recipient, sender)
        }
    }

    private suspend fun simulateAutoResponse(fromContact: String, toSelf: String) {
        val responseText = when {
            fromContact.contains("anon.chat") -> "Welcome to the anonymous relay chat room! Ensure your garlic routing tunnels maintain at least 3 hops for solid anonymity."
            fromContact.contains("secure.mail") -> "Hello. Demo mailbox preview is active. Real encrypted mail is not implemented yet."
            fromContact.contains("wiki.leaks") -> "Demo endpoint received your preview payload. Use a real crypto backend before sharing sensitive files."
            else -> "Message acknowledged by cryptographic endpoint. Connection status: Active."
        }
        
        // Background delay simulator
        addLog("GARLIC", "Simulated incoming Garlic Message from $fromContact", "ROUTING")
        addLog("CRYPT", "Decoded demo Base64 payload locally. No ElGamal decryption was performed.", "WARN")

        val incomingMsg = SecureMessage(
            senderAddress = fromContact,
            recipientAddress = toSelf,
            encryptedPayload = Base64.encodeToString(responseText.toByteArray(), Base64.NO_WRAP),
            isIncoming = true,
            isDecrypted = true,
            decryptedBody = responseText
        )
        secureMessageDao.insertMessage(incomingMsg)
    }

    suspend fun createIdentity(name: String): Identity {
        addLog("KEYGEN", "Initializing demo RSA keypair for local identity preview.", "INFO")
        val keys = try {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(1024) // fast for mock/simulation keys
            keyGen.generateKeyPair()
        } catch (e: Exception) {
            null
        }

        val publicKey = keys?.public?.encoded?.let { Base64.encodeToString(it, Base64.DEFAULT) } ?: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC3Y..."
        val privateKey = keys?.private?.encoded?.let { Base64.encodeToString(it, Base64.DEFAULT) } ?: "MIICXAIBAAKBgQC3Y..."

        val uniqueAddr = "${name.lowercase().replace(" ", "")}.i2p"
        val fullDestinationHash = "i2p-destination-hash-${UUID.randomUUID().toString().take(12)}"

        val identity = Identity(
            name = name,
            publicKeyBase64 = publicKey,
            privateKeyBase64 = privateKey,
            i2pAddress = uniqueAddr,
            fullDestination = fullDestinationHash
        )

        identityDao.insertIdentity(identity)
        addLog("KEYGEN", "Identity generated successfully: $uniqueAddr", "SUCCESS")
        return identity
    }

    suspend fun addTrustedKey(alias: String, i2pAddress: String, publicKeyBase64: String, isVerified: Boolean = false) {
        val existing = trustedKeyDao.getTrustedKeyByAddress(i2pAddress)
        if (existing != null) {
            // Update
            trustedKeyDao.insertTrustedKey(existing.copy(
                alias = alias,
                publicKeyBase64 = publicKeyBase64,
                isVerified = isVerified
            ))
            addLog("KEYRING", "Updated peer public key for: $i2pAddress", "SUCCESS")
        } else {
            // Insert new
            val newKey = TrustedKey(
                alias = alias,
                i2pAddress = i2pAddress,
                publicKeyBase64 = publicKeyBase64,
                isVerified = isVerified
            )
            trustedKeyDao.insertTrustedKey(newKey)
            addLog("KEYRING", "Imported new trusted peer key: $alias ($i2pAddress)", "SUCCESS")
        }
    }

    suspend fun removeTrustedKey(key: TrustedKey) {
        trustedKeyDao.deleteTrustedKey(key)
        addLog("KEYRING", "Revoked & removed peer key for: ${key.alias} (${key.i2pAddress})", "WARN")
    }

    suspend fun verifyTrustedKey(key: TrustedKey) {
        addLog("CRYPT", "Simulating key verification for ${key.alias}.", "INFO")
        addLog("CRYPT", "Demo challenge packet generated locally.", "ROUTING")
        addLog("CRYPT", "Demo verification complete. Not proof of real peer ownership.", "WARN")
        trustedKeyDao.insertTrustedKey(key.copy(isVerified = true, sessionTagCount = 100))
        addLog("KEYRING", "Identity verification complete for: ${key.alias}. Status: TRUSTED.", "SUCCESS")
    }

    // Seed initial database state with safe default darkweb wiki, secure chats, and cryptographic profiles
    suspend fun seedDefaultsIfNeeded() {
        val defaultBookmarks = listOf(
            Bookmark(title = "I2P Project Homepage", url = "http://i2p-project.i2p", iconName = "language", colorHex = "#00E676"),
            Bookmark(title = "AnonIRC Relay Chat", url = "http://anon.chat.i2p", iconName = "chat", colorHex = "#D500F9"),
            Bookmark(title = "Invisible Cryptic Wiki", url = "http://wiki.leaks.i2p", iconName = "description", colorHex = "#FF3D00"),
            Bookmark(title = "Garlic Mail Service", url = "http://secure.mail.i2p", iconName = "mail", colorHex = "#FFD600"),
            Bookmark(title = "Hidden Forum Feed", url = "http://forum.feed.i2p", iconName = "forum", colorHex = "#00B0FF"),
            Bookmark(title = "DarkBERT Threat Intelligence", url = "http://darkbert.intel.i2p", iconName = "shield", colorHex = "#AA00FF")
        )

        // Seed bookmarks if empty
        bookmarkDao.getAllBookmarks().collect { list ->
            if (list.isEmpty()) {
                defaultBookmarks.forEach { bookmarkDao.insertBookmark(it) }
                addLog("DATABASE", "Seeded default darkweb bookmarks.", "SUCCESS")
            }
        }

        // Seed trusted peer keys if empty
        val defaultPeerKeys = listOf(
            TrustedKey(
                alias = "Postman (Mailmaster)",
                i2pAddress = "postman.i2p",
                publicKeyBase64 = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDZ2G4H1Vp83D...",
                isVerified = true,
                sessionTagCount = 100
            ),
            TrustedKey(
                alias = "Sybil (Network Radar)",
                i2pAddress = "sybil.i2p",
                publicKeyBase64 = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCmF9V3Xf4e...",
                isVerified = false,
                sessionTagCount = 0
            ),
            TrustedKey(
                alias = "JRandom (I2P Creator)",
                i2pAddress = "jrandom.i2p",
                publicKeyBase64 = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDH3sLz1p5B...",
                isVerified = true,
                sessionTagCount = 50
            )
        )

        trustedKeyDao.getAllTrustedKeys().collect { list ->
            if (list.isEmpty()) {
                defaultPeerKeys.forEach { trustedKeyDao.insertTrustedKey(it) }
                addLog("DATABASE", "Seeded default trusted peer keyrings.", "SUCCESS")
            }
        }

        val defaultContacts = listOf(
            Contact(name = "Postman (I2P Mailmaster)", address = "postman.i2p", type = "SECURE_I2P", status = "ONLINE", avatarColorHex = "#00E676"),
            Contact(name = "Sybil (I2P Router Radar)", address = "sybil.i2p", type = "SECURE_I2P", status = "OFFLINE", avatarColorHex = "#FF3D00"),
            Contact(name = "Google Chat: Project Dev Lead", address = "lead.dev@gmail.com", type = "GOOGLE_CHAT", status = "ONLINE", avatarColorHex = "#FFD600"),
            Contact(name = "Google Chat: AI Bridge Agent", address = "gemini.bridge@gmail.com", type = "GOOGLE_CHAT", status = "ONLINE", avatarColorHex = "#AA00FF"),
            Contact(name = "SMS: Secure Dispatcher", address = "+1555019283", type = "SMS", status = "ONLINE", avatarColorHex = "#00B0FF"),
            Contact(name = "SMS: Emergency Backup Canary", address = "+1800555992", type = "SMS", status = "OFFLINE", avatarColorHex = "#FF3D00")
        )

        contactDao.getAllContacts().collect { list ->
            if (list.isEmpty()) {
                defaultContacts.forEach { contactDao.insertContact(it) }
                addLog("DATABASE", "Seeded default secure contacts list (I2P, Google Chat, SMS).", "SUCCESS")
            }
        }
    }
}
