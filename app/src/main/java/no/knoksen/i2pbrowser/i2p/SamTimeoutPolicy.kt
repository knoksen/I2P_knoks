package no.knoksen.i2pbrowser.i2p

data class SamTimeoutPolicy(
    val connectTimeoutMs: Int = 2_000,
    val helloReadTimeoutMs: Int = 5_000,
    val destinationReadTimeoutMs: Int = 10_000,
    val sessionCreateReadTimeoutMs: Int = 120_000,
    val nameLookupReadTimeoutMs: Int = 15_000
)

enum class SamProtocolStep {
    CONNECT,
    HELLO,
    DEST_GENERATE,
    SESSION_CREATE,
    NAME_LOOKUP,
    CLOSE
}
