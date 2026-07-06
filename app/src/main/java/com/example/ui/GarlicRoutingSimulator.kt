package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Enums for simulation state
enum class SimState {
    CONFIG,       // Configuring cloves
    PACKAGING,    // Animating cloves wrapping into garlic bulb
    ROUTING,      // Animated bulb moving through hops
    PEELING,      // Reached destination, peeling layers and releasing cloves
    COMPLETED     // Cloves completely revealed and decrypted
}

// Representation of a Garlic Clove (individual message/instruction nested in the bulb)
data class GarlicClove(
    val id: String,
    val type: String,      // "CHAT_MESSAGE", "LEASE_REQUEST", "TUNNEL_BUILD", "NETDB_QUERY"
    val recipient: String, // Final recipient/destination
    val payload: String,   // Raw data
    var isDecrypted: Boolean = false
)

// Represents a Node in our custom simulated tunnel
data class SimNode(
    val id: String,
    val label: String,
    val subLabel: String,
    val description: String,
    val ipAddress: String,
    val sessionTag: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarlicRoutingSimulatorComponent(
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Simulation states
    var simState by remember { mutableStateOf(SimState.CONFIG) }
    var speedMultiplier by remember { mutableStateOf(1.0f) } // 0.5f, 1.0f, 2.0f
    var autoPlay by remember { mutableStateOf(false) }
    var currentHopIndex by remember { mutableStateOf(0) } // 0 to 5 (Client to Destination)
    var stepProgress by remember { mutableStateOf(0f) } // 0f to 1f between hops
    var activeLogTab by remember { mutableStateOf("LIVE_TERM") } // "LIVE_TERM", "CRYPT_EXPLAIN"

    // Configurable Cloves
    var clove1Type by remember { mutableStateOf("CHAT_MESSAGE") }
    var clove1Payload by remember { mutableStateOf("Hello secure world! This is E2EE via I2P.") }
    var clove2Type by remember { mutableStateOf("LEASE_REQUEST") }
    var clove2Payload by remember { mutableStateOf("LeaseSet: outbound_gateway=0x9B2A duration=10m") }
    var clove3Type by remember { mutableStateOf("TUNNEL_BUILD") }
    var clove3Payload by remember { mutableStateOf("Action: BuildHop Node=0x3F81 SessionTag=0x7A9F") }

    val cloves = remember(simState, clove1Type, clove1Payload, clove2Type, clove2Payload, clove3Type, clove3Payload) {
        listOf(
            GarlicClove("clove_1", clove1Type, "Alice (Destination Peer)", clove1Payload),
            GarlicClove("clove_2", clove2Type, "Inbound Gateway", clove2Payload),
            GarlicClove("clove_3", clove3Type, "Relay Hop 2", clove3Payload)
        )
    }

    // Static simulation nodes layout
    val simNodes = remember {
        listOf(
            SimNode("node_0", "Client (Source)", "Sender", "Originates messages, packages cloves into layered garlic bulb.", "127.0.0.1", "INITIATOR", CyberGreen),
            SimNode("node_1", "OBGW", "Outbound Gateway", "Batches multiple tunnel packets and wraps them in garlic layers.", "192.168.1.50", "0xFA2C8E", CyberBlue),
            SimNode("node_2", "Relay Hop 1", "Tunnel Router A", "Decrypts outer garlic layer using matched Session Tag key.", "172.16.89.14", "0xBC8421", CyberPurple),
            SimNode("node_3", "Relay Hop 2", "Tunnel Router B", "Decrypts second layer. Reveals next hop destination instruction.", "10.0.4.195", "0x3A9F5E", CyberOrange),
            SimNode("node_4", "IBGW", "Inbound Gateway", "Decrypts last garlic layer, un-bulbs cloves, dispatches to peers.", "192.168.12.80", "0x98D21B", CyberYellow),
            SimNode("node_5", "Peer (Dest)", "Eepsite Server", "Alice's peer node. Receives chat clove and executes command.", "80.95.122.3", "RECIPIENT", CyberGreen)
        )
    }

    // Interactive node details selection
    var selectedInspectorNode by remember { mutableStateOf<SimNode?>(simNodes[0]) }

    // Coroutine job for auto-playing simulation
    var simulationJob by remember { mutableStateOf<Job?>(null) }

    // Start auto-play loop if turned on
    LaunchedEffect(autoPlay, currentHopIndex, simState) {
        if (autoPlay) {
            simulationJob?.cancel()
            simulationJob = launch {
                while (autoPlay) {
                    when (simState) {
                        SimState.CONFIG -> {
                            simState = SimState.PACKAGING
                            delay((1500 / speedMultiplier).toLong())
                        }
                        SimState.PACKAGING -> {
                            simState = SimState.ROUTING
                            currentHopIndex = 0
                            stepProgress = 0f
                        }
                        SimState.ROUTING -> {
                            if (currentHopIndex < simNodes.size - 1) {
                                // Animate stepProgress from 0f to 1f
                                val steps = 30
                                val stepDelay = ((1200 / speedMultiplier) / steps).toLong()
                                for (i in 1..steps) {
                                    stepProgress = i.toFloat() / steps
                                    delay(stepDelay)
                                }
                                currentHopIndex++
                                stepProgress = 0f
                                // Pause briefly at node for "decryption/peeling" visual
                                delay((1000 / speedMultiplier).toLong())
                            } else {
                                simState = SimState.PEELING
                                delay((2000 / speedMultiplier).toLong())
                            }
                        }
                        SimState.PEELING -> {
                            simState = SimState.COMPLETED
                            autoPlay = false
                        }
                        SimState.COMPLETED -> {
                            autoPlay = false
                        }
                    }
                }
            }
        } else {
            simulationJob?.cancel()
        }
    }

    // Text descriptions for current cryptographic state
    val liveStatusText = remember(simState, currentHopIndex) {
        when (simState) {
            SimState.CONFIG -> "SYSTEM IDLE: Custom cloves ready for garlic encryption packaging. Configure payloads and click 'Package & Dispatch'."
            SimState.PACKAGING -> "COMPILING GARLIC BULB: Nesting multiple encrypted instructions (cloves) inside a single layered container. Encrypting with Outbound Gateway public keys..."
            SimState.ROUTING -> {
                val currentNode = simNodes[currentHopIndex]
                when (currentHopIndex) {
                    0 -> "DISPATCHED: Garlic bulb leaves Client. Fully encrypted with 4 nested layers of asymmetric/symmetric cryptography. No hop knows total length of path."
                    1 -> "OUTBOUND GATEWAY (OBGW): Routing bulb into the tunnel. Layer 4 of encryption decrypted. Payload still encapsulated. Next Hop instruction revealed: ${simNodes[2].ipAddress}."
                    2 -> "TUNNEL RELAY 1: Matches Session Tag ${simNodes[2].sessionTag}. Decrypts layer 3 using ephemeral session key. Target hop address disclosed. Garlic core remains encrypted."
                    3 -> "TUNNEL RELAY 2: Matches Session Tag ${simNodes[3].sessionTag}. Decrypts layer 2. Revealing Inbound Gateway address (${simNodes[4].ipAddress}). Garlic bulb size unchanged."
                    4 -> "INBOUND GATEWAY (IBGW): Reached end of tunnel. Decrypts final garlic encryption layer. Peeling bulb container open to inspect nested cloves for delivery."
                    else -> "FORWARDING CLOVES: Dispatched individual instructions. Chat message clove is now being delivered to Destination Peer via local secure link."
                }
            }
            SimState.PEELING -> "UNWRAPPING CLOVES: Inbound Gateway peels off all metadata. Routing individual message cloves. Verifying cryptographic signatures..."
            SimState.COMPLETED -> "SUCCESS: All message cloves processed. Alice decrypted the main Chat Clove using her private key! Integrity and total zero-knowledge anonymity preserved."
        }
    }

    // Dynamic hex string simulation
    val currentHexPayload = remember(simState, currentHopIndex) {
        val baseHex = "a8f3c7e0b1154c8e79fd3c1a4038a8e2d409fec9a10237e8bbdd0c5e71029c"
        when (simState) {
            SimState.CONFIG -> "[NO ENCRYPTED PACKET IN FLIGHT]"
            SimState.PACKAGING -> "0xENCRYPTING_BULB_7657_SHA256..."
            SimState.ROUTING -> {
                // Strips hex characters as layers decrypt
                val ratio = currentHopIndex.toFloat() / (simNodes.size - 1)
                val len = (baseHex.length * (1f - ratio * 0.7f)).toInt().coerceIn(16, baseHex.length)
                "0x" + baseHex.take(len).uppercase() + "..."
            }
            SimState.PEELING -> "0x[EXTRACTING_CLOVES_DECRYPTED]"
            SimState.COMPLETED -> "0x[DECRYPTED_PAYLOAD_SUCCESS]"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CyberBlack)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. SIMULATOR CANVAS PANEL ---
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (autoPlay) CyberGreen else if (simState != SimState.CONFIG) CyberOrange else TextSecondary,
                                    androidx.compose.foundation.shape.CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "REALTIME GARLIC ROUTING GRAPH",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberBlue,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Reset button
                    if (simState != SimState.CONFIG) {
                        TextButton(
                            onClick = {
                                autoPlay = false
                                simState = SimState.CONFIG
                                currentHopIndex = 0
                                stepProgress = 0f
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = CyberRed),
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RESET SIM", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Animated Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(CyberBlack, RoundedCornerShape(8.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "garlic_simulation")

                    // Flowing dashed line shift
                    val dashPhase by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 100f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "dash_phase"
                    )

                    // Glow scaling
                    val glowScale by infiniteTransition.animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "glow_scale"
                    )

                    var canvasSize by remember { mutableStateOf(IntSize(600, 200)) }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { size ->
                                if (size.width > 0 && size.height > 0) {
                                    canvasSize = size
                                }
                            }
                            .pointerInput(canvasSize) {
                                detectTapGestures { offset ->
                                    // Check if tapped a node to inspect it
                                    val width = canvasSize.width
                                    val height = canvasSize.height
                                    val paddingX = 40.dp.toPx()
                                    val stepX = (width - paddingX * 2) / (simNodes.size - 1)

                                    simNodes.forEachIndexed { index, node ->
                                        val x = paddingX + index * stepX
                                        val yOffset = if (index == 0 || index == simNodes.size - 1) 0f else {
                                            if (index % 2 == 1) -height * 0.22f else height * 0.22f
                                        }
                                        val y = (height / 2) + yOffset
                                        val dist = sqrt((offset.x - x) * (offset.x - x) + (offset.y - y) * (offset.y - y))
                                        if (dist < 30.dp.toPx()) {
                                            selectedInspectorNode = node
                                        }
                                    }
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height
                        val paddingX = 40.dp.toPx()
                        val stepX = (width - paddingX * 2) / (simNodes.size - 1)

                        // 1. Calculate positions for all nodes
                        val nodePositions = simNodes.mapIndexed { index, node ->
                            val x = paddingX + index * stepX
                            val yOffset = if (index == 0 || index == simNodes.size - 1) 0f else {
                                if (index % 2 == 1) -height * 0.22f else height * 0.22f
                            }
                            val y = (height / 2) + yOffset
                            Pair(node, Offset(x, y))
                        }

                        // 2. Draw connections/tunnels with gradient flow
                        for (i in 0 until nodePositions.size - 1) {
                            val start = nodePositions[i].second
                            val end = nodePositions[i + 1].second

                            // Draw baseline background tunnel
                            drawLine(
                                color = CyberBorder.copy(alpha = 0.5f),
                                start = start,
                                end = end,
                                strokeWidth = 2.dp.toPx()
                            )

                            // Draw flowing encrypted traffic line
                            if (simState == SimState.ROUTING || simState == SimState.PEELING || simState == SimState.COMPLETED) {
                                val isPassed = i < currentHopIndex
                                val isCurrent = i == currentHopIndex

                                val activeLineColor = when {
                                    isPassed -> CyberGreen.copy(alpha = 0.6f)
                                    isCurrent -> CyberOrange
                                    else -> CyberBlue.copy(alpha = 0.15f)
                                }

                                val dashEffect = PathEffect.dashPathEffect(
                                    intervals = floatArrayOf(20f, 15f),
                                    phase = -dashPhase
                                )

                                drawLine(
                                    color = activeLineColor,
                                    start = start,
                                    end = end,
                                    strokeWidth = if (isCurrent) 3.5.dp.toPx() else 2.dp.toPx(),
                                    pathEffect = if (isCurrent) dashEffect else null
                                )
                            }
                        }

                        // 3. Draw garlic packet bulb in motion
                        if (simState == SimState.ROUTING) {
                            val startNode = nodePositions[currentHopIndex].second
                            val endNode = if (currentHopIndex < nodePositions.size - 1) {
                                nodePositions[currentHopIndex + 1].second
                            } else {
                                nodePositions[currentHopIndex].second
                            }

                            // Interpolate position along current segment
                            val packetPos = startNode + (endNode - startNode) * stepProgress

                            // Remaining encryption layers based on hop index
                            val totalLayers = 4
                            val layersLeft = (totalLayers - currentHopIndex).coerceAtLeast(1)

                            // Outer layer rings representing nested encryption
                            for (l in 1..layersLeft) {
                                val radius = (8.dp.toPx() + (l * 4.5.dp.toPx()))
                                val alpha = 0.8f / l
                                drawCircle(
                                    color = CyberBlue.copy(alpha = alpha),
                                    radius = radius * glowScale,
                                    center = packetPos,
                                    style = Stroke(width = 1.2.dp.toPx())
                                )
                            }

                            // Solid Core Garlic Clove Package
                            drawCircle(
                                color = CyberOrange,
                                radius = 6.dp.toPx(),
                                center = packetPos
                            )

                            // High-power glow aura
                            drawCircle(
                                color = CyberOrange.copy(alpha = 0.25f),
                                radius = 12.dp.toPx() * glowScale,
                                center = packetPos
                            )
                        }

                        // 4. Draw individual nodes
                        nodePositions.forEachIndexed { index, pair ->
                            val node = pair.first
                            val pos = pair.second

                            val isCurrentActive = simState == SimState.ROUTING && currentHopIndex == index
                            val isPastActive = simState == SimState.ROUTING && index < currentHopIndex
                            val isCompletedNode = simState == SimState.COMPLETED || simState == SimState.PEELING

                            val accentColor = if (isCurrentActive) {
                                CyberOrange
                            } else if (isPastActive || isCompletedNode) {
                                CyberGreen
                            } else {
                                node.color.copy(alpha = 0.7f)
                            }

                            // Inspect halo selection ring
                            if (selectedInspectorNode?.id == node.id) {
                                drawCircle(
                                    color = CyberBlue.copy(alpha = 0.15f),
                                    radius = 24.dp.toPx(),
                                    center = pos
                                )
                                drawCircle(
                                    color = CyberBlue.copy(alpha = 0.5f),
                                    radius = 24.dp.toPx(),
                                    center = pos,
                                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
                                )
                            }

                            // Outer glowing ring
                            drawCircle(
                                color = accentColor.copy(alpha = if (isCurrentActive) 0.3f else 0.15f),
                                radius = 16.dp.toPx() * (if (isCurrentActive) glowScale else 1f),
                                center = pos
                            )

                            // Solid border
                            drawCircle(
                                color = accentColor,
                                radius = 10.dp.toPx(),
                                center = pos,
                                style = Stroke(width = 2.dp.toPx())
                            )

                            // Center core dot
                            drawCircle(
                                color = if (isCurrentActive) CyberBlack else accentColor,
                                radius = 5.dp.toPx(),
                                center = pos
                            )

                            // Inner core glowing dot for currently decrypting node
                            if (isCurrentActive) {
                                drawCircle(
                                    color = CyberOrange,
                                    radius = 3.dp.toPx(),
                                    center = pos
                                )
                            }
                        }
                    }

                    // 5. Draw overlay text on Canvas
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ℹ️ Tap nodes to view IP / cryptographic keys in inspector.",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )

                        Text(
                            text = when (simState) {
                                SimState.CONFIG -> "CONFIG LEVEL"
                                SimState.PACKAGING -> "PACKAGING LAYER"
                                SimState.ROUTING -> "ROUTING HOP: $currentHopIndex/5"
                                SimState.PEELING -> "UNPACKAGING"
                                SimState.COMPLETED -> "REACHED DEST"
                            },
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (simState == SimState.COMPLETED) CyberGreen else CyberBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(CyberDarkSurface, RoundedCornerShape(4.dp))
                                .border(0.5.dp, CyberBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // --- 2. CONTROLLER DASHBOARD ---
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "SIMULATION CONTROL BOARD",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberBlue,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play/Pause button
                    Button(
                        onClick = {
                            if (simState == SimState.COMPLETED) {
                                // Reset to route if completed
                                simState = SimState.CONFIG
                                currentHopIndex = 0
                                stepProgress = 0f
                            }
                            autoPlay = !autoPlay
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (autoPlay) CyberOrange.copy(alpha = 0.15f) else CyberGreen.copy(alpha = 0.15f),
                            contentColor = if (autoPlay) CyberOrange else CyberGreen
                        ),
                        border = BorderStroke(1.dp, if (autoPlay) CyberOrange else CyberGreen),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1.5f).height(40.dp)
                    ) {
                        Icon(
                            imageVector = if (autoPlay) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (autoPlay) "Pause" else "Play",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (autoPlay) "PAUSE SIM" else if (simState == SimState.CONFIG) "START SIMULATION" else "RESUME SIM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Step Forward
                    IconButton(
                        onClick = {
                            autoPlay = false
                            coroutineScope.launch {
                                when (simState) {
                                    SimState.CONFIG -> {
                                        simState = SimState.PACKAGING
                                    }
                                    SimState.PACKAGING -> {
                                        simState = SimState.ROUTING
                                        currentHopIndex = 0
                                        stepProgress = 0f
                                    }
                                    SimState.ROUTING -> {
                                        if (currentHopIndex < simNodes.size - 1) {
                                            currentHopIndex++
                                            stepProgress = 0f
                                        } else {
                                            simState = SimState.PEELING
                                        }
                                    }
                                    SimState.PEELING -> {
                                        simState = SimState.COMPLETED
                                    }
                                    SimState.COMPLETED -> {
                                        // Loop back
                                        simState = SimState.CONFIG
                                        currentHopIndex = 0
                                        stepProgress = 0f
                                    }
                                }
                            }
                        },
                        enabled = !autoPlay,
                        modifier = Modifier
                            .size(40.dp)
                            .background(CyberCardBg, RoundedCornerShape(4.dp))
                            .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Step Forward", tint = if (autoPlay) TextSecondary else CyberBlue)
                    }

                    // Reset button
                    IconButton(
                        onClick = {
                            autoPlay = false
                            simState = SimState.CONFIG
                            currentHopIndex = 0
                            stepProgress = 0f
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(CyberCardBg, RoundedCornerShape(4.dp))
                            .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Simulation", tint = CyberRed)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Speed Selectors Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "TUNNEL PROPAGATION SPEED:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 9.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0.5f, 1.0f, 2.0f).forEach { speed ->
                            val isSelected = speedMultiplier == speed
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) CyberBlue.copy(alpha = 0.15f) else Color.Transparent,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(0.5.dp, if (isSelected) CyberBlue else CyberBorder, RoundedCornerShape(4.dp))
                                    .clickable { speedMultiplier = speed }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${speed}x",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) CyberBlue else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. DYNAMIC INSPECTOR / TELEMETRY CARD ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // A. Left Col: Packet inspector in transit
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(210.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "📦 PACKET ENVELOPE",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberOrange,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        InspectorRow("ENVELOPE TYPE", "GARLIC BULB", CyberOrange)

                        val activeLayers = (4 - currentHopIndex).coerceAtLeast(0)
                        InspectorRow("ENC LAYERS", "$activeLayers/4 Layer(s)", if (activeLayers > 1) CyberOrange else CyberGreen)

                        InspectorRow("SESSION TAG", if (simState == SimState.ROUTING) simNodes[currentHopIndex].sessionTag else "N/A", CyberBlue)

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "CYPHERTEXT PAYLOAD",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(CyberBlack, RoundedCornerShape(4.dp))
                                .border(0.5.dp, CyberBorder, RoundedCornerShape(4.dp))
                                .padding(6.dp)
                        ) {
                            Text(
                                text = currentHexPayload,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (simState == SimState.ROUTING) CyberOrange else TextSecondary,
                                lineHeight = 10.sp
                            )
                        }
                    }
                }
            }

            // B. Right Col: Node static profile inspector
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, if (selectedInspectorNode != null) selectedInspectorNode!!.color.copy(alpha = 0.5f) else CyberBorder),
                modifier = Modifier
                    .weight(1.1f)
                    .height(210.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🔍 NODE INSPECTOR",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberBlue,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        selectedInspectorNode?.let { node ->
                            Box(
                                modifier = Modifier
                                    .background(node.color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .border(0.5.dp, node.color, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    node.subLabel.uppercase(),
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = node.color,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    selectedInspectorNode?.let { node ->
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = node.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )

                            InspectorRow("IP ADDR", node.ipAddress, CyberBlue)
                            InspectorRow("SESSION TAG", node.sessionTag, node.color)

                            Text(
                                text = node.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                lineHeight = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    } ?: Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No Node Selected", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // --- 4. CLOVE PACKAGING CONFIGURATION SYSTEM (Only shown during SimState.CONFIG) ---
        AnimatedVisibility(
            visible = simState == SimState.CONFIG,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "GARLIC BULB ASSEMBLY: PACK CORES",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Select and customize the individual message cloves. Unlike onion routing, garlic routing groups multiple encrypted cloves inside a single transport container.",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Clove 1 Config (Chat Message)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "CLOVE 1: RECIPIENT PEER PAYLOAD",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberGreen,
                                fontFamily = FontFamily.Monospace
                            )
                            Box(
                                modifier = Modifier
                                    .background(CyberGreen.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                    .border(0.5.dp, CyberGreen, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("SECURE CHAT", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = CyberGreen)
                            }
                        }

                        OutlinedTextField(
                            value = clove1Payload,
                            onValueChange = { clove1Payload = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = CyberGreen,
                                unfocusedBorderColor = CyberBorder
                            ),
                            maxLines = 2
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Clove 2 Config (Inbound Gateway metadata)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("CLOVE 2: TUNNEL LEASE DATA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberBlue, fontFamily = FontFamily.Monospace)
                            OutlinedTextField(
                                value = clove2Payload,
                                onValueChange = { clove2Payload = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = CyberBlue,
                                    unfocusedBorderColor = CyberBorder
                                ),
                                maxLines = 1
                            )
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("CLOVE 3: ROUTING SIGNAL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberPurple, fontFamily = FontFamily.Monospace)
                            OutlinedTextField(
                                value = clove3Payload,
                                onValueChange = { clove3Payload = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = CyberPurple,
                                    unfocusedBorderColor = CyberBorder
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            simState = SimState.PACKAGING
                            autoPlay = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberBlack),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ENCRYPT, PACKAGE & DISPATCH BULB", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // --- 5. REALTIME TERMINAL ACTIVITY / EXPLANATION PANEL ---
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Interactive Tab Selector for logs vs general guide
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberBlack, RoundedCornerShape(4.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        Pair("LIVE_TERM", "🖥️ CRYPTO TERMINAL"),
                        Pair("CRYPT_EXPLAIN", "📖 HOW IT WORKS")
                    ).forEach { tab ->
                        val selected = activeLogTab == tab.first
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (selected) CyberBlue.copy(alpha = 0.15f) else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .border(0.5.dp, if (selected) CyberBlue else Color.Transparent, RoundedCornerShape(4.dp))
                                .clickable { activeLogTab = tab.first }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.second,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (selected) CyberBlue else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (activeLogTab == "LIVE_TERM") {
                    // Modern styled terminal logs
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(CyberBlack, RoundedCornerShape(6.dp))
                            .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item {
                                Text(
                                    text = ">>> I2P GARLIC CRYPTO STREAM MONITOR INTERFACE",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            item {
                                Text(
                                    text = ">>> TIME: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())} | ACTIVE_KEY_TAG_SYNC=TRUE",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary
                                )
                            }
                            item {
                                Divider(color = CyberBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 4.dp))
                            }
                            item {
                                Text(
                                    text = liveStatusText,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary,
                                    lineHeight = 13.sp
                                )
                            }

                            if (simState == SimState.PEELING || simState == SimState.COMPLETED) {
                                item {
                                    Text(
                                        text = "================ REVEALED CORES ================",
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberGreen,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                itemsIndexed(cloves) { index, clove ->
                                    val isShowing = simState == SimState.COMPLETED || (simState == SimState.PEELING && index < 2)
                                    val color = if (index == 0) CyberGreen else if (index == 1) CyberBlue else CyberPurple
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = " ▸ CLOVE ${index + 1} (${clove.type}):",
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = color,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isShowing) clove.payload else "[ENCRYPTED COAXIAL PAYLOAD]",
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isShowing) TextPrimary else TextSecondary,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(1f).padding(start = 12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Educational section explaining Garlic Routing vs Onion Routing
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExplanationItem(
                            title = "🧄 What is Garlic Routing?",
                            body = "Garlic routing is an extension of Onion Routing used exclusively by I2P. Instead of wrapping a single instruction message, it packages multiple messages ('cloves') into a single encrypted bulb. This allows transit nodes to deliver separate control logs, peer messages, and tunnel database queries together, improving efficiency and bandwidth blending."
                        )

                        ExplanationItem(
                            title = "🛡️ Nested Encryption Layer Decapsulation",
                            body = "The client builds the bulb by recursively encrypting cloves inside multiple layers. Each intermediate hop (Relay) uses Ephemeral Session Tags to look up the correct symmetric session key. The node decrypts EXACTLY one layer, reveals only the address of the next hop, and immediately dispatches it. Nodes cannot read the payloads or determine overall tunnel length."
                        )

                        ExplanationItem(
                            title = "🔄 Asymmetric vs Symmetric Cryptography",
                            body = "I2P uses asymmetric public/private keys (ElGamal / ECIES) to initiate tunnels and establish a shared secure bridge. Once constructed, high-speed symmetric AES-256 keys are used for encrypting Garlic bulb payloads. Ephemeral session tags are appended with each packet to avoid expensive asymmetric cryptography on every packet hop."
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InspectorRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ExplanationItem(
    title: String,
    body: String
) {
    Column {
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = CyberBlue,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = body,
            fontSize = 9.sp,
            color = TextSecondary,
            lineHeight = 12.sp
        )
    }
}
