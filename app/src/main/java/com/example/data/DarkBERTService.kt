package com.example.data

import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

suspend fun queryDarkBERT(prompt: String): String = withContext(Dispatchers.IO) {
    val apiKey = com.example.BuildConfig.GEMINI_API_KEY
    if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "null") {
        return@withContext getOfflineDarkBERTResponse(prompt)
    }

    val systemInstruction = Content(
        parts = listOf(Part(text = "You are DarkBERT, an expert cybersecurity threat intelligence AI trained specifically to analyze darknet logs, hacker forums, security vulnerabilities, and onion/i2p network leasesets. Provide replies formatted in a technical hacker terminal tone, highly detailed, precise, using bullet points or code snippets. Keep it highly objective and focused on cyber defense, anonymity, and threat mitigation. Avoid chatty conversational intro or outro phrases."))
    )

    val requestBody = GeminiRequest(
        contents = listOf(Content(parts = listOf(Part(text = prompt)))),
        systemInstruction = systemInstruction,
        generationConfig = GenerationConfig(temperature = 0.5f, maxOutputTokens = 800)
    )

    try {
        val response = GeminiClient.service.generateContent(apiKey, requestBody)
        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: "ERROR: Empty response from DarkBERT Core. (Check network connection)"
    } catch (e: Exception) {
        "ERROR: API connection failed: ${e.localizedMessage}\n\n[Falling back to Offline Security Database]\n\n${getOfflineDarkBERTResponse(prompt)}"
    }
}

fun getOfflineDarkBERTResponse(prompt: String): String {
    val lower = prompt.lowercase()
    return when {
        lower.contains("wiki") || lower.contains("leak") -> {
            """
            == [DarkBERT Threat Intel Engine] ==
            TARGET NODES: wiki.leaks.i2p
            THREAT CLASS: DATA COMPROMISE / TRACKING RISK
            
            VULNERABILITY AUDIT:
            - Active transport verification: Decentering relays confirmed.
            - Payload Integrity: Document drops use standard SHA-256 signatures, preventing man-in-the-middle modification.
            - Deanonymization Threat: Script-based trackers embedded in uploaded media or documents could run locally in user browsers if NoScript is disabled.
            
            MITIGATION ACCESSORIES ADVICE:
            1. Enforce NoScript Shield to block Javascript payloads within document previews.
            2. Activate Metadata Stripper to sanitize files before uploading to the cryptographic drop box.
            """.trimIndent()
        }
        lower.contains("onion") || lower.contains("garlic") || lower.contains("routing") -> {
            """
            == [DarkBERT Threat Intel Engine] ==
            TOPIC: GARLIC vs. ONION ROUTING ARCHITECTURE
            
            COMPARATIVE ANONYMITY LEVEL:
            - Onion Routing (Tor-style): Aggregates user messages into unified circuits. Increases vulnerability to statistical timing analysis if entry and exit node pools are controlled by the same entity.
            - Garlic Routing (I2P-style): Encapsulates multiple messages ("cloves") in a single packet. Uses unidirectional tunnels (separate inbound and outbound tunnels). A single compromised hop cannot correlate both streams.
            
            INTEGRITY METRICS:
            - Routing state: Decentralized, zero-directory server reliant.
            - Security Recommendations: Deploy Tunnel Hop counts >= 3 to maximize the path complexity and defeat Sybil timing models.
            """.trimIndent()
        }
        lower.contains("exploit") || lower.contains("vulnerability") || lower.contains("attack") || lower.contains("hack") -> {
            """
            == [DarkBERT Threat Intel Engine] ==
            THREAT FOCUS: DARKNET ATTAK VECTORS
            
            IDENTIFIED SHADOW VECTORS:
            1. Peer Profiling: Malicious actors running high-bandwidth nodes to gather statistical flow data.
            2. Fingerprinting: Querying browser capabilities (canvas dimensions, OS tags) to track single client sessions across different sites.
            3. Sybil Flood: Compromising NetDb storage by spawning thousands of virtual nodes.
            
            ACTIVE MITIGATIONS:
            - Toggle on User-Agent Cloaking to obfuscate browser fingerprints.
            - Enable Cookie Isolation to sandbox local session keys.
            - Enforce NoScript Shield to disable canvas query scripts.
            """.trimIndent()
        }
        lower.contains("postman") || lower.contains("mail") || lower.contains("email") -> {
            """
            == [DarkBERT Threat Intel Engine] ==
            TARGET: secure.mail.i2p (Postman Mail Service)
            THREAT PROFILE: INFORMATION INTERCEPTION
            
            SECURITY PROFILE:
            - Encrypted storage blocks protect mail bodies on disk.
            - Risk: Metadata sizing analysis. Spikes in mail dispatch times can relate specific active identities to clearweb operators.
            
            DEFENSIVE ACCESSORIES ADVICE:
            - Never reuse clearweb PGP keys. Establish a brand-new cryptographic identity in the 'Identity & Keyrings' panel.
            - Refresh Session Tags frequently to encrypt mail traffic streams with rotating ephemerals.
            """.trimIndent()
        }
        else -> {
            """
            == [DarkBERT Threat Intel Engine] ==
            QUERY: $prompt
            
            THREAT SCAN RESULT:
            - Security Index: OPTIMAL.
            - Peer Status: Verified secure routing tunnels active.
            - Active recommendations:
              * Deploy "NoScript Shield" to prevent interactive Javascript exploits.
              * Force "HTTPS-Everywhere" to prevent proxy transit spying.
              * Use "User-Agent Cloaking" to spoof browser headers and defeat tracking filters.
            """.trimIndent()
        }
    }
}
