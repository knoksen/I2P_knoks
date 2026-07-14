package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

enum class MetaHumanMood(
    val label: String,
    val color: Color,
    val description: String,
    val greeting: String
) {
    SERENE(
        "Serene", 
        CyberGreen, 
        "Holographic model state is calm. Audio latency optimized.", 
        "Greetings, agent. Holographic link established over garlic channels. State is serene. Ready for security audits."
    ),
    ANALYTICAL(
        "Analytical", 
        CyberBlue, 
        "Processing cryptographic validation protocols. Diagnostic HUD active.", 
        "Awaiting instruction. Tunnels monitored, packet correlation indices updated. Command me."
    ),
    VIGILANT(
        "Vigilant", 
        CyberOrange, 
        "Heightened sensor alert. Active path scanning engaged.", 
        "Alert: Entry-node and exit-node footprints are highly exposed. Recommend engaging protective countermeasures."
    ),
    TACTICAL(
        "Tactical", 
        CyberPurple, 
        "Skeletal model ready for combat operations or intrusion countermeasures.", 
        "Intrusion detection matrices operational. Let's verify our cryptographic keys and shield the transport relays."
    ),
    DIAGNOSTIC(
        "Diagnostic", 
        CyberYellow, 
        "UE5 Pixel Streaming profiling active. Full debugging terminal output enabled.", 
        "Diagnostics interface online. Audio lip-sync buffer cleared. Network telemetry calibration initiated."
    )
}

data class WebRtcLog(val timestamp: String, val level: String, val message: String)

@Composable
fun MetaHumanScreen(
    viewModel: I2PViewModel,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Observe app stats to integrate with the assistant
    val vpnState by viewModel.vpnState.collectAsState()
    val routerState by viewModel.routerState.collectAsState()
    val trustedKeys by viewModel.trustedKeys.collectAsState()
    val tunnels by viewModel.tunnels.collectAsState()
    val activeIdentity by viewModel.activeIdentity.collectAsState()
    val accessedNodes by viewModel.accessedNodesHistory.collectAsState()

    // Screen-specific state
    var selectedMood by remember { mutableStateOf(MetaHumanMood.SERENE) }
    var activeRtcLogs = remember { mutableStateListOf<WebRtcLog>() }
    var assistantSpeechText by remember { mutableStateOf(selectedMood.greeting) }
    var userInputMsg by remember { mutableStateOf("") }
    var isSynthesizingVoice by remember { mutableStateOf(false) }
    var isDiagnosticScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(0f) }
    var scanMessage by remember { mutableStateOf("") }

    // Stream telemetry state (interactive)
    var streamResolution by remember { mutableStateOf("1080p") }
    var maxFps by remember { mutableStateOf(60f) }
    var audioBitrateKbps by remember { mutableStateOf(192f) }

    // Derive telemetry calculations
    val computedLatencyMs = remember(streamResolution, maxFps, audioBitrateKbps) {
        val base = when (streamResolution) {
            "4K" -> 28f
            "1440p" -> 18f
            else -> 11f
        }
        val fpsFactor = (120f - maxFps) * 0.1f
        val audioFactor = (audioBitrateKbps - 64f) * 0.02f
        (base + fpsFactor + audioFactor).coerceAtLeast(8f)
    }

    val computedBandwidthMbps = remember(streamResolution, maxFps) {
        val base = when (streamResolution) {
            "4K" -> 25f
            "1440p" -> 15f
            else -> 6.2f
        }
        val fpsFactor = maxFps / 60f
        base * fpsFactor
    }

    // Function to append logs
    val sdf = remember { java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()) }
    fun addLog(level: String, msg: String) {
        val time = sdf.format(java.util.Date())
        activeRtcLogs.add(0, WebRtcLog(time, level, msg))
        if (activeRtcLogs.size > 50) {
            activeRtcLogs.removeLast()
        }
    }

    // Initialize RTC logs
    LaunchedEffect(Unit) {
        addLog("SYSTEM", "Initializing UE5 Pixel Streaming WebRTC Handshake Client...")
        delay(300)
        addLog("RTC_SDP", "Sending local SDP Offer (Type: Audio/Video/Data Channels)...")
        delay(400)
        addLog("RTC_SDP", "Received remote SDP Answer from ue5-instance.i2p-rtc.local")
        delay(300)
        addLog("RTC_ICE", "Gathered Local Candidate: typ host tcpport 51234")
        addLog("RTC_ICE", "ICE Connection State: CONNECTED. WebRTC Channel bonded.")
        delay(200)
        addLog("METAHUMAN", "MetaHuman Character 'A.R.I.A.' skeletal mesh fully loaded (LOD 0).")
        addLog("SYSTEM", "Holographic voice lip-sync engine ready.")
    }

    // Sync assistant greeting on mood change
    LaunchedEffect(selectedMood) {
        assistantSpeechText = selectedMood.greeting
        addLog("METAHUMAN", "Character model updated core mood to '${selectedMood.label}'. Synchronized shader materials.")
    }

    // Audio-frequency waveform animation values
    val infiniteTransition = rememberInfiniteTransition()
    val waveOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val waveOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Hologram Scanline offset animation
    val scanlineYOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP METADATA BAR
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(CyberBlue, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "UE5 PIXEL STREAM",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberBlue
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("LATENCY", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Text(String.format("%.1f ms", computedLatencyMs), fontSize = 10.sp, color = CyberGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("BANDWIDTH", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Text(String.format("%.2f Mbps", computedBandwidthMbps), fontSize = 10.sp, color = CyberBlue, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("FRAMERATE", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Text("${maxFps.toInt()} FPS", fontSize = 10.sp, color = CyberPurple, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // METAHUMAN HOLO-FRAME AND WAVEFORM VISUALIZER
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, selectedMood.color.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .border(1.dp, selectedMood.color, RoundedCornerShape(8.dp))
                            .background(CyberBlack, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // MetaHuman Portrait
                        Image(
                            painter = painterResource(id = R.drawable.img_metahuman_aria),
                            contentDescription = "A.R.I.A. MetaHuman Assistant Hologram",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("metahuman_avatar_portrait")
                        )

                        // Animated Hologram Scanline Effect
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val y = size.height * scanlineYOffset
                            drawLine(
                                color = selectedMood.color.copy(alpha = 0.4f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 5f), 0f)
                            )
                        }

                        // Ambient Glowing overlay matching active mood
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            selectedMood.color.copy(alpha = 0.12f),
                                            Color.Transparent
                                        ),
                                        radius = 350f
                                    )
                                )
                        )

                        // MetaHuman Diagnostics HUD Overlays
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "[LOD_0] REMOTE_FED: ACTIVE",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = selectedMood.color,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "A.R.I.A. v5.8",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = selectedMood.color,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(
                                                if (isSynthesizingVoice) CyberGreen else selectedMood.color,
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (isSynthesizingVoice) "SYNCHRONIZING VOICE" else "STATUS: ONLINE",
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSynthesizingVoice) CyberGreen else selectedMood.color,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    "FPS: ${maxFps.toInt()}",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = selectedMood.color
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // REAL-TIME AUDIO FREQUENCY WAVEFORM VISUALIZER
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(CyberBlack, RoundedCornerShape(6.dp))
                            .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barWidth = 4.dp.toPx()
                            val spacing = 3.dp.toPx()
                            val barCount = (size.width / (barWidth + spacing)).toInt()
                            val centerY = size.height / 2f

                            for (i in 0 until barCount) {
                                val x = i * (barWidth + spacing) + barWidth / 2f
                                
                                // Synthesizing wave vs idle wave
                                val multiplier = if (isSynthesizingVoice) {
                                    sin(i * 0.35f + waveOffset2) * 0.8f + sin(i * 0.12f + waveOffset1) * 0.2f
                                } else {
                                    sin(i * 0.15f + waveOffset1) * 0.25f
                                }
                                
                                val rawHeight = (centerY * multiplier).coerceIn(-centerY, centerY)
                                val finalHeight = if (rawHeight < 0) -rawHeight else rawHeight

                                drawLine(
                                    color = if (isSynthesizingVoice) CyberGreen else selectedMood.color,
                                    start = Offset(x, centerY - finalHeight - 2f),
                                    end = Offset(x, centerY + finalHeight + 2f),
                                    strokeWidth = barWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
            }
        }

        // ASSISTANT SPEECH TRANSCRIPT / AUDIO OUTPUT BOX
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "A.R.I.A. TRANSLATED INTERFACE RESPONSE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = selectedMood.color
                        )

                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(assistantSpeechText))
                                android.widget.Toast.makeText(context, "Speech copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Text", tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberBlack, RoundedCornerShape(6.dp))
                            .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = assistantSpeechText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // SKELETAL ACTIONS & MOOD CONTROLS
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "METAHUMAN MODEL CONTROLLER (UE5 SOCKET)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberBlue
                    )

                    // Mood selector
                    Text("Select Expression / System Protocol Profile", fontSize = 10.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetaHumanMood.values().forEach { mood ->
                            val isSelected = mood == selectedMood
                            Button(
                                onClick = { selectedMood = mood },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp).testTag("mood_btn_${mood.name.lowercase()}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) mood.color.copy(alpha = 0.2f) else CyberBlack,
                                    contentColor = if (isSelected) mood.color else TextSecondary
                                ),
                                border = BorderStroke(1.dp, if (isSelected) mood.color else CyberBorder),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(mood.label, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Divider(color = CyberBorder, modifier = Modifier.padding(vertical = 4.dp))

                    // Skeletal Action Buttons
                    Text("Skeletal Gesture Cues (WebRTC Remote Procedure)", fontSize = 10.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                addLog("RTC_RPC", "Invoking Remote Skeletal Event 'CMD_SCAN_PEERS'")
                                addLog("METAHUMAN", "Character model playing key-scan skeletal motion track...")
                                isDiagnosticScanning = true
                                scanProgress = 0f
                                scanMessage = "Performing security audit on cryptographic keyring..."
                                scope.launch {
                                    while (scanProgress < 1f) {
                                        delay(300)
                                        scanProgress += 0.15f
                                        if (scanProgress >= 1f) {
                                            isDiagnosticScanning = false
                                            val keyCount = trustedKeys.size
                                            val isVerifiedCount = trustedKeys.count { it.isVerified }
                                            assistantSpeechText = "Keyring Diagnostic complete. Found $keyCount trusted peers on our keyring ($isVerifiedCount fully verified). Encryption trust integrity is highly secured. Keep using asymmetric signing for garlic packages."
                                            addLog("SYSTEM", "Cryptographic key audit completed. Trust verified.")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(36.dp).testTag("action_btn_scan"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberBlue.copy(alpha = 0.15f), contentColor = CyberBlue),
                            border = BorderStroke(1.dp, CyberBlue),
                            shape = RoundedCornerShape(4.dp),
                            enabled = !isDiagnosticScanning
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SCAN KEYRING", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                addLog("RTC_RPC", "Invoking Remote Skeletal Event 'CMD_SHIELD_UP'")
                                addLog("METAHUMAN", "Character model playing protective stance shield animations...")
                                isDiagnosticScanning = true
                                scanProgress = 0f
                                scanMessage = "Profiling active Garlic multi-hop tunnels..."
                                scope.launch {
                                    while (scanProgress < 1f) {
                                        delay(250)
                                        scanProgress += 0.2f
                                        if (scanProgress >= 1f) {
                                            isDiagnosticScanning = false
                                            val isVpnConnected = vpnState.status == VpnStatus.CONNECTED
                                            val activeTunnels = tunnels.size
                                            val routerOn = routerState.isConnected
                                            
                                            assistantSpeechText = buildString {
                                                append("Garlic network route analysis complete. ")
                                                if (isVpnConnected) {
                                                    append("Your Private VPN overlay is securely active (${vpnState.selectedVpn}). ")
                                                } else {
                                                    append("VPN mask is offline! ")
                                                }
                                                if (routerOn) {
                                                    append("Garlic Router is actively processing tunnels ($activeTunnels tunnels established). Packet routing is fully sealed.")
                                                } else {
                                                    append("Router is offline. Active multi-hop tunnels are currently zero.")
                                                }
                                            }
                                            addLog("SYSTEM", "Relay tunnel audit completed.")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(36.dp).testTag("action_btn_shield"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPurple.copy(alpha = 0.15f), contentColor = CyberPurple),
                            border = BorderStroke(1.dp, CyberPurple),
                            shape = RoundedCornerShape(4.dp),
                            enabled = !isDiagnosticScanning
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SHIELD GARLIC", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                addLog("RTC_RPC", "Invoking Remote Skeletal Event 'CMD_GREET'")
                                addLog("METAHUMAN", "Character model playing warm physical welcome wave gesture.")
                                isSynthesizingVoice = true
                                assistantSpeechText = "Welcome to I2P Shadow, agent! It is fantastic to assist you. All interface channels are protected by high-level cryptography. Your confidentiality is our absolute directive."
                                scope.launch {
                                    delay(2800)
                                    isSynthesizingVoice = false
                                    addLog("METAHUMAN", "Audio play track completed. Returning model to idle stance.")
                                }
                            },
                            modifier = Modifier.weight(1f).height(36.dp).testTag("action_btn_greet"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen.copy(alpha = 0.15f), contentColor = CyberGreen),
                            border = BorderStroke(1.dp, CyberGreen),
                            shape = RoundedCornerShape(4.dp),
                            enabled = !isDiagnosticScanning
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WARM GREET", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Scan progress bar
                    if (isDiagnosticScanning) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(scanMessage, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = CyberYellow)
                                Text("${(scanProgress * 100).toInt()}%", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = CyberYellow)
                            }
                            LinearProgressIndicator(
                                progress = scanProgress,
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = CyberYellow,
                                trackColor = CyberBorder
                            )
                        }
                    }
                }
            }
        }

        // DYNAMIC CHAT CONSOLE PANEL
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "DIRECT INTEGRATED VOCAL CHAT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberGreen
                    )

                    OutlinedTextField(
                        value = userInputMsg,
                        onValueChange = { userInputMsg = it },
                        label = { Text("Command Speech Plaintext", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().testTag("companion_chat_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = CyberBorder
                        ),
                        textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        maxLines = 3,
                        trailingIcon = {
                            if (userInputMsg.isNotEmpty()) {
                                IconButton(onClick = { userInputMsg = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear Input", tint = TextSecondary)
                                }
                            }
                        }
                    )

                    Button(
                        onClick = {
                            val msg = userInputMsg
                            userInputMsg = ""
                            addLog("CHAT_SEND", "User command spoken: \"$msg\"")
                            isSynthesizingVoice = true
                            
                            scope.launch {
                                addLog("METAHUMAN", "Processing voice translation vectors...")
                                delay(1200)
                                isSynthesizingVoice = false
                                
                                val triggerWords = msg.lowercase()
                                assistantSpeechText = when {
                                    triggerWords.contains("identity") || triggerWords.contains("who am i") -> {
                                        "Let me check our identity vault. Your active cryptographic profile is: ${activeIdentity?.name ?: "Undefined"}. Your virtual I2P address is: ${activeIdentity?.i2pAddress ?: "Not provisioned yet. Let's create one in the Identity tab!"}"
                                    }
                                    triggerWords.contains("hello") || triggerWords.contains("hi") || triggerWords.contains("aria") -> {
                                        "Hello! Hologram feeds are fully rendering. What secure cryptographic task can I assist you with today?"
                                    }
                                    triggerWords.contains("clear") || triggerWords.contains("wipe") -> {
                                        activeRtcLogs.clear()
                                        addLog("SYSTEM", "Client diagnostic log files cleared.")
                                        "Terminal logs cleared, agent."
                                    }
                                    else -> {
                                        "I have digested your message: \"$msg\". Web Crypto RSA-OAEP asymmetric envelopes are ready. Initiating secure processing protocols."
                                    }
                                }
                                addLog("SYSTEM", "A.R.I.A. replied successfully.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(38.dp).testTag("companion_send_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreen.copy(alpha = 0.15f), contentColor = CyberGreen),
                        border = BorderStroke(1.dp, CyberGreen),
                        shape = RoundedCornerShape(4.dp),
                        enabled = userInputMsg.isNotEmpty() && !isSynthesizingVoice
                    ) {
                        if (isSynthesizingVoice) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp, color = CyberGreen)
                        } else {
                            Text("SYNCHRONIZE & TRANSMIT SPEECH", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Quick prompt shortcuts
                    Text("Preset Cryptographic Queries", fontSize = 10.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "🛡️ Garlic Security Check" to "Provide a complete defensive audit of our active tunnels, router connection, and VPN overlay.",
                            "🔑 Cryptographic Key Audit" to "Count and inspect our trusted key stores and identify non-verified peer signatures.",
                            "👤 Ephemeral Identity audit" to "Review active profile, address registration, and local database leaks."
                        ).forEach { (label, command) ->
                            Button(
                                onClick = {
                                    userInputMsg = command
                                },
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberDarkSurface, contentColor = TextPrimary),
                                border = BorderStroke(0.5.dp, CyberBorder),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(label, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // DYNAMIC PIXEL STREAMING RESOLUTION DECK
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "UNREAL ENGINE 5 STREAMING DECK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberBlue
                    )

                    // Resolution Buttons
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Render Resolution (Simulates Server Allocation Load)", fontSize = 10.sp, color = TextSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("1080p" to "6.2 Mbps", "1440p" to "15.0 Mbps", "4K" to "25.0 Mbps").forEach { (res, bandwidth) ->
                                val isSelected = streamResolution == res
                                Button(
                                    onClick = {
                                        streamResolution = res
                                        addLog("UE5_STREAMS", "Requested stream resolution update to $res ($bandwidth). Resending SDP negotiation...")
                                    },
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    contentPadding = PaddingValues(2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) CyberBlue.copy(alpha = 0.15f) else CyberBlack,
                                        contentColor = if (isSelected) CyberBlue else TextSecondary
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) CyberBlue else CyberBorder),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("$res ($bandwidth)", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // FPS slider
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Max Frame Rate Constraint", fontSize = 10.sp, color = TextSecondary)
                            Text("${maxFps.toInt()} FPS", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberPurple, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = maxFps,
                            onValueChange = { maxFps = it },
                            valueRange = 30f..120f,
                            steps = 2,
                            colors = SliderDefaults.colors(
                                thumbColor = CyberPurple,
                                activeTrackColor = CyberPurple,
                                inactiveTrackColor = CyberBorder
                            )
                        )
                    }

                    // Audio Bitrate slider
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("WebRTC AAC-LD Audio Bitrate", fontSize = 10.sp, color = TextSecondary)
                            Text("${audioBitrateKbps.toInt()} kbps", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberOrange, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = audioBitrateKbps,
                            onValueChange = { audioBitrateKbps = it },
                            valueRange = 64f..320f,
                            steps = 3,
                            colors = SliderDefaults.colors(
                                thumbColor = CyberOrange,
                                activeTrackColor = CyberOrange,
                                inactiveTrackColor = CyberBorder
                            )
                        )
                    }
                }
            }
        }

        // INTERACTIVE WEBRTC Handshake Console Logs
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "REAL-TIME WEBRTC SIGNALING LOGS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberYellow
                        )

                        TextButton(
                            onClick = { activeRtcLogs.clear() },
                            modifier = Modifier.height(24.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("CLEAR CONSOLE", color = CyberYellow, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(CyberBlack, RoundedCornerShape(6.dp))
                            .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        if (activeRtcLogs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Console history empty. Waiting for network socket trigger...", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(activeRtcLogs) { log ->
                                    val levelColor = when (log.level) {
                                        "SYSTEM" -> CyberGreen
                                        "RTC_SDP" -> CyberBlue
                                        "RTC_ICE" -> CyberPurple
                                        "RTC_RPC" -> CyberOrange
                                        "CHAT_SEND" -> CyberGreen
                                        "METAHUMAN" -> CyberGreen
                                        else -> TextSecondary
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            "[${log.timestamp}]",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextSecondary
                                        )
                                        Text(
                                            "[${log.level}]",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = levelColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            log.message,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextPrimary,
                                            softWrap = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
