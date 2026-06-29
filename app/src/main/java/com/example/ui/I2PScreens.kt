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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.*
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RouterScreen(
    viewModel: I2PViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.routerState.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val scope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.networkAlerts.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
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
                        Column {
                            Text(
                                "GARLIC ROUTER STATUS",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (state.isConnected) "ACTIVE & ROUTING" else if (state.isConnecting) "BUILDING TUNNELS" else "OFFLINE",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (state.isConnected) CyberGreen else if (state.isConnecting) CyberOrange else CyberRed,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Switch(
                            checked = state.isConnected,
                            onCheckedChange = { checked ->
                                if (checked) viewModel.connectRouter() else viewModel.disconnectRouter()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberBlack,
                                checkedTrackColor = CyberGreen,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = CyberCardBg
                            ),
                            modifier = Modifier.testTag("router_toggle_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        state.statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )

                    if (state.isConnecting) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { state.connectionProgress },
                            color = CyberOrange,
                            trackColor = CyberBorder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    } else if (state.isConnected) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { 1.0f },
                            color = CyberGreen,
                            trackColor = CyberBorder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }

        // Tunnel Configuration
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "GARLIC ENCRYPTION PATH & HOPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(1, 3, 5).forEach { hops ->
                            val isSelected = state.tunnelHops == hops
                            OutlinedButton(
                                onClick = { viewModel.setTunnelHops(hops) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) CyberGreen.copy(alpha = 0.15f) else Color.Transparent,
                                    contentColor = if (isSelected) CyberGreen else TextPrimary
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) CyberGreen else CyberBorder
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hops_button_$hops")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "$hops Hop${if (hops > 1) "s" else ""}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        when (hops) {
                                            1 -> "Low Privacy"
                                            3 -> "Standard I2P"
                                            else -> "High Latency"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) CyberGreen else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Each hop encapsulates packets in multiple layers of asymmetric encryption. Higher hops increase onion-routing garlic protection against network node correlations.",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // Live Garlic Routing Path Simulation Dashboard
        item {
            GarlicRoutingPathVisualizer(
                tunnelHops = state.tunnelHops,
                isConnected = state.isConnected,
                isConnecting = state.isConnecting
            )
        }

        // Live Circuit Statistics Dashboard Widget
        item {
            I2PCircuitStatsWidget(state = state)
        }

        // Live Health Optimizer Widget
        item {
            I2POptimizerWidget(state = state, viewModel = viewModel)
        }

        // Garlic Routing Knowledge Base Help Center
        item {
            I2PHelpCenterWidget()
        }

        // Telemetry Statistics
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ROUTER NETWORK TELEMETRY",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryItem(
                            label = "ACTIVE TUNNELS",
                            value = "${state.activeTunnels}",
                            icon = Icons.Default.Cyclone,
                            color = CyberBlue
                        )
                        TelemetryItem(
                            label = "PEERS IN NETDB",
                            value = "${state.knownPeers}",
                            icon = Icons.Default.Groups,
                            color = CyberPurple
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = CyberBorder)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryItem(
                            label = "BANDWIDTH IN",
                            value = String.format("%.1f KB/s", state.bandwidthInKbps),
                            icon = Icons.Default.ArrowDownward,
                            color = CyberGreen
                        )
                        TelemetryItem(
                            label = "BANDWIDTH OUT",
                            value = String.format("%.1f KB/s", state.bandwidthOutKbps),
                            icon = Icons.Default.ArrowUpward,
                            color = CyberOrange
                        )
                    }
                }
            }
        }

        // Global Peer Discovery System
        item {
            PeerDiscoverySection(viewModel = viewModel)
        }

        // Router Console Logs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ROUTER CONSOLE ACTIVITY LOGS",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { viewModel.clearRouterLogs() },
                    colors = ButtonDefaults.textButtonColors(contentColor = CyberRed)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Wipe", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Console is empty. Enable router to trace garlic connections.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(logs, key = { it.id }) { log ->
                LogItemRow(log = log)
            }
        }
    }
}

@Composable
fun TelemetryItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(value, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LogItemRow(log: LogEntry) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeStr = formatter.format(Date(log.timestamp))

    val levelColor = when (log.level) {
        "SUCCESS" -> CyberGreen
        "WARN" -> CyberRed
        "ROUTING" -> CyberPurple
        else -> CyberBlue
    }

    val badgeResId = when (log.tag) {
        "GARLIC", "CRYPT", "KEYRING", "KEYGEN" -> com.example.R.drawable.img_security_gold_1782670586675
        "TUNNEL", "NETDB", "ROUTER" -> com.example.R.drawable.img_security_silver_1782670571201
        else -> com.example.R.drawable.img_security_bronze_1782670550813
    }

    val securityLevelLabel = when (log.tag) {
        "GARLIC", "CRYPT", "KEYRING", "KEYGEN" -> "GOLD"
        "TUNNEL", "NETDB", "ROUTER" -> "SILVER"
        else -> "BRONZE"
    }

    val securityLevelColor = when (securityLevelLabel) {
        "GOLD" -> Color(0xFFD4AF37) // Metallic Gold
        "SILVER" -> Color(0xFFA0A2A6) // Cyber Silver
        else -> Color(0xFFCD7F32) // Tech Bronze
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberDarkSurface, RoundedCornerShape(6.dp))
            .border(1.dp, securityLevelColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // High quality security level image badge
        Image(
            painter = painterResource(id = badgeResId),
            contentDescription = "Security Level: $securityLevelLabel",
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(0.5.dp, securityLevelColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
        )

        Text(
            "[$timeStr]",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = TextSecondary,
            fontSize = 11.sp
        )

        // Security level indicator tag
        Surface(
            color = securityLevelColor.copy(alpha = 0.12f),
            shape = RoundedCornerShape(3.dp),
            border = BorderStroke(0.5.dp, securityLevelColor.copy(alpha = 0.4f))
        ) {
            Text(
                securityLevelLabel,
                color = securityLevelColor,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        Text(
            log.tag,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = levelColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
        Text(
            log.message,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            fontSize = 11.sp
        )
    }
}

@Composable
fun BrowserScreen(
    viewModel: I2PViewModel,
    modifier: Modifier = Modifier
) {
    val tabState by viewModel.browserTab.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val routerState by viewModel.routerState.collectAsState()
    
    var urlInput by remember { mutableStateOf(tabState.url) }
    var showBookmarkDialog by remember { mutableStateOf(false) }
    var bookmarkTitleInput by remember { mutableStateOf("") }
    var bookmarkSafetyInput by remember { mutableStateOf("SAFE") }
    
    // Sync address bar when page navigates
    LaunchedEffect(tabState.url) {
        urlInput = tabState.url
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedSafetyFilter by remember { mutableStateOf("ALL") }
    var isIndexingSearchActive by remember { mutableStateOf(false) }

    // Simulated I2P Distributed Hash Table (DHT) / hosts.txt indexing lookup
    LaunchedEffect(searchQuery, selectedSafetyFilter) {
        if (searchQuery.isNotEmpty()) {
            isIndexingSearchActive = true
            delay(350) // Simulated network hop lookup delay
            isIndexingSearchActive = false
        }
    }

    val filteredBookmarks = remember(bookmarks, searchQuery, selectedSafetyFilter) {
        bookmarks.filter { bookmark ->
            val matchesQuery = bookmark.title.contains(searchQuery, ignoreCase = true) ||
                    bookmark.url.contains(searchQuery, ignoreCase = true)
            val matchesFilter = selectedSafetyFilter == "ALL" || bookmark.safetyLevel == selectedSafetyFilter
            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        // Safe Address & Proxy Navigation Bar
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
            shape = RoundedCornerShape(0.dp),
            border = BorderStroke(0.dp, Color.Transparent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.browserBack() },
                        enabled = tabState.currentHistoryIndex > 0,
                        modifier = Modifier.testTag("browser_back_button")
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = if (tabState.currentHistoryIndex > 0) CyberGreen else TextSecondary
                        )
                    }

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("browser_address_bar"),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        ),
                        placeholder = { Text("enter.i2p address...", color = TextSecondary) },
                        leadingIcon = {
                            Icon(
                                if (urlInput.endsWith(".i2p")) Icons.Default.VpnLock else Icons.Default.Language,
                                contentDescription = null,
                                tint = if (urlInput.endsWith(".i2p")) CyberGreen else TextSecondary
                            )
                        },
                        trailingIcon = {
                            if (urlInput.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        bookmarkTitleInput = tabState.pageTitle
                                        showBookmarkDialog = true
                                    },
                                    modifier = Modifier.testTag("browser_bookmark_icon")
                                ) {
                                    Icon(
                                        Icons.Default.BookmarkBorder,
                                        contentDescription = "Bookmark this site",
                                        tint = CyberBlue
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CyberBlack,
                            unfocusedContainerColor = CyberBlack,
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = CyberBorder
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            if (urlInput.isNotBlank()) {
                                viewModel.navigateBrowser(urlInput)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberGreen,
                            contentColor = CyberBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("browser_go_button")
                    ) {
                        Text("GO", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (routerState.isConnected) CyberGreen else CyberRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (routerState.isConnected) "Anonymous Tunnel Connection Active" else "Warning: Router is Offline (Browsing is simulated local cached assets)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (routerState.isConnected) CyberGreen else CyberOrange
                    )
                }
            }
        }

        Divider(color = CyberBorder)

        // Quick Handshake & Accessory Cockpit
        var isDockExpanded by remember { mutableStateOf(false) }
        val activeAccessories by viewModel.activeAccessories.collectAsState()

        Card(
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
            shape = RoundedCornerShape(0.dp),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isDockExpanded = !isDockExpanded }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = CyberGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "QUICK HANDSHAKE & ACCESSORY COCKPIT",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Icon(
                        if (isDockExpanded) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = if (isDockExpanded) "Collapse" else "Expand",
                        tint = CyberGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (isDockExpanded) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = CyberBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Simplified connection menu
                    Text(
                        "SAFE DARKNET ADDRESSES (ONE-CLICK HANDSHAKE)",
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val safeSites = listOf(
                        "i2p-project.i2p" to "I2P Home",
                        "anon.chat.i2p" to "AnonIRC",
                        "wiki.leaks.i2p" to "WikiLeaks",
                        "secure.mail.i2p" to "GarlicMail",
                        "darkbert.intel.i2p" to "DarkBERT AI"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        safeSites.forEach { (siteUrl, label) ->
                            val isCurrent = tabState.url.contains(siteUrl)
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isCurrent) CyberGreen.copy(alpha = 0.15f) else CyberBlack,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isCurrent) CyberGreen else CyberBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        urlInput = siteUrl
                                        viewModel.navigateBrowser(siteUrl)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(if (isCurrent) CyberGreen else CyberBlue, RoundedCornerShape(3.dp))
                                    )
                                    Text(
                                        text = label,
                                        color = if (isCurrent) CyberGreen else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Manual Connection Gate with encryption hop level picker
                    Text(
                        "MANUAL GATEWAY ENTRY",
                        color = CyberOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    var manualAddressInput by remember { mutableStateOf("") }
                    var garlicHopLevel by remember { mutableStateOf("Double Hop") }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualAddressInput,
                            onValueChange = { manualAddressInput = it },
                            placeholder = { Text("any-address.i2p / .onion", color = TextSecondary, fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("manual_gateway_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = CyberOrange,
                                unfocusedBorderColor = CyberBorder,
                                focusedContainerColor = CyberBlack,
                                unfocusedContainerColor = CyberBlack
                            ),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        )

                        Button(
                            onClick = {
                                if (manualAddressInput.isNotBlank()) {
                                    val cleanManual = manualAddressInput.trim()
                                    urlInput = cleanManual
                                    viewModel.navigateBrowser(cleanManual)
                                    manualAddressInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberOrange,
                                contentColor = CyberBlack
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("manual_gateway_connect_button")
                        ) {
                            Text("GATE CONNECT", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Hop level picker chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Direct proxy", "Double Hop", "Garlic-Clad").forEach { level ->
                            val isSelected = garlicHopLevel == level
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) CyberOrange.copy(alpha = 0.15f) else Color.Transparent,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) CyberOrange else CyberBorder.copy(alpha = 0.5f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { garlicHopLevel = level }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = level,
                                    color = if (isSelected) CyberOrange else TextSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = CyberBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Adviced accessories
                    Text(
                        "ADVICED SECURITY ACCESSORIES (ACTIVE LAYER PROTECTION)",
                        color = CyberPurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Configure active browser payloads to adapt to routing latency and secure sandboxing profiles.",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val accessoriesList = listOf(
                        Triple("noscript", "NoScript Shield", "Disables Javascript to block browser exploit vectors (-300ms latency)"),
                        Triple("https_everywhere", "HTTPS-Everywhere", "Enforces end-to-end transport-layer TLS encapsulation (+100ms latency)"),
                        Triple("user_agent", "UA-Cloaking Spoofer", "Hides hardware fingerprint and client user-agent headers (+50ms latency)"),
                        Triple("metadata_stripper", "Metadata Stripper", "Purges EXIF data and upload tracking metadata tags (+150ms latency)")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        accessoriesList.forEach { (id, title, desc) ->
                            val isEnabled = activeAccessories.contains(id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isEnabled) CyberPurple else CyberBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .background(
                                        if (isEnabled) CyberPurple.copy(alpha = 0.10f) else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { viewModel.toggleAccessory(id) }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        color = if (isEnabled) CyberPurple else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = desc,
                                        color = TextSecondary,
                                        fontSize = 9.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp, 20.dp)
                                        .background(
                                            if (isEnabled) CyberPurple else CyberBlack,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isEnabled) CyberPurple else CyberBorder,
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = if (isEnabled) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(2.dp)
                                            .size(16.dp)
                                            .background(Color.White, RoundedCornerShape(8.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Divider(color = CyberBorder)

        // Web Content Render area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (tabState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = CyberBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Routing Garlic Clove payloads...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title and connection info
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = CyberGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "END-TO-END GARLIC TUNNEL SECURED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyberGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    tabState.pageTitle,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    tabState.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Simulated Page HTML Contents according to current active URL
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                RenderWebpageContents(url = tabState.url, onNavigate = { viewModel.navigateBrowser(it) })
                            }
                        }
                    }

                    // Simulated I2P Indexing Search Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = CyberBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "I2P DISTRIBUTED INDEX LOOKUP",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CyberBlue,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    
                                    // Indexer Node status
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isIndexingSearchActive) CyberOrange.copy(alpha = 0.15f) else CyberGreen.copy(alpha = 0.15f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                0.5.dp,
                                                if (isIndexingSearchActive) CyberOrange else CyberGreen,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isIndexingSearchActive) "QUERYING DHT" else "INDEX READY",
                                            fontSize = 7.sp,
                                            color = if (isIndexingSearchActive) CyberOrange else CyberGreen,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("i2p_index_search_bar"),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextPrimary,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    placeholder = { Text("Search addresses, services, or titles...", color = TextSecondary, fontSize = 12.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = CyberBlue,
                                        unfocusedBorderColor = CyberBorder
                                    ),
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                )

                                // Safety/Verification Category filter chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("ALL", "SAFE", "SUSPICIOUS", "DANGEROUS").forEach { level ->
                                        val isSelected = selectedSafetyFilter == level
                                        val chipColor = when (level) {
                                            "SAFE" -> CyberGreen
                                            "SUSPICIOUS" -> CyberOrange
                                            "DANGEROUS" -> CyberRed
                                            else -> CyberBlue
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    if (isSelected) chipColor.copy(alpha = 0.2f) else CyberBlack,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) chipColor else CyberBorder,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable { selectedSafetyFilter = level }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = level,
                                                fontSize = 9.sp,
                                                color = if (isSelected) chipColor else TextSecondary,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                // Interactive Indexing system telemetry log output
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CyberBlack, RoundedCornerShape(4.dp))
                                        .border(0.5.dp, CyberBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = when {
                                            isIndexingSearchActive -> "i2p://indexer: dht_find_providers query sent to [3] active garlic peers..."
                                            searchQuery.isNotEmpty() -> "i2p://indexer: returned ${filteredBookmarks.size} verified Leaseset record(s) matching \"$searchQuery\"."
                                            else -> "i2p://indexer: local addressbook (hosts.txt) parsed • ${bookmarks.size} router entries indexed."
                                        },
                                        fontSize = 9.sp,
                                        color = if (isIndexingSearchActive) CyberOrange else if (searchQuery.isNotEmpty()) CyberBlue else TextSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    // Bookmarked Sites Grid Header
                    item {
                        Text(
                            if (searchQuery.isNotEmpty() || selectedSafetyFilter != "ALL") "INDEX MATCHES" else "YOUR SEED BOOKMARKS",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Bookmarks List
                    if (filteredBookmarks.isEmpty()) {
                        item {
                            Text(
                                if (searchQuery.isNotEmpty() || selectedSafetyFilter != "ALL") "No matching verified addresses found in I2P index." else "No bookmarks saved.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    } else {
                        items(filteredBookmarks) { bookmark ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                                border = BorderStroke(1.dp, CyberBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.navigateBrowser(bookmark.url) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    Color(android.graphics.Color.parseColor(bookmark.colorHex)).copy(alpha = 0.2f),
                                                    RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                when (bookmark.iconName) {
                                                    "chat" -> Icons.Default.ChatBubble
                                                    "description" -> Icons.Default.Description
                                                    "mail" -> Icons.Default.Mail
                                                    "forum" -> Icons.Default.Forum
                                                    "shield" -> Icons.Default.Shield
                                                    else -> Icons.Default.Language
                                                },
                                                contentDescription = null,
                                                tint = Color(android.graphics.Color.parseColor(bookmark.colorHex))
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    bookmark.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                val safetyColor = when (bookmark.safetyLevel) {
                                                    "SAFE" -> CyberGreen
                                                    "SUSPICIOUS" -> CyberOrange
                                                    "DANGEROUS" -> CyberRed
                                                    else -> CyberBlue
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .background(safetyColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                        .border(0.5.dp, safetyColor, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        bookmark.safetyLevel,
                                                        fontSize = 7.sp,
                                                        color = safetyColor,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                            Text(
                                                bookmark.url,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteWebBookmark(bookmark) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Bookmark", tint = CyberRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Bookmark Dialog
    if (showBookmarkDialog) {
        AlertDialog(
            onDismissRequest = { showBookmarkDialog = false },
            title = { Text("Add Darkweb Bookmark", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = bookmarkTitleInput,
                        onValueChange = { bookmarkTitleInput = it },
                        label = { Text("Title", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = CyberBorder
                        )
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("URL Address", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = CyberBorder
                        )
                    )

                    Text("Safety Level", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SAFE", "SUSPICIOUS", "DANGEROUS").forEach { level ->
                            val isSelected = bookmarkSafetyInput == level
                            val levelColor = when (level) {
                                "SAFE" -> CyberGreen
                                "SUSPICIOUS" -> CyberOrange
                                "DANGEROUS" -> CyberRed
                                else -> CyberBlue
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) levelColor.copy(alpha = 0.2f) else CyberBlack,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) levelColor else CyberBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { bookmarkSafetyInput = level }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = level,
                                    fontSize = 10.sp,
                                    color = if (isSelected) levelColor else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bookmarkTitleInput.isNotEmpty() && urlInput.isNotEmpty()) {
                            viewModel.addWebBookmark(bookmarkTitleInput, urlInput, bookmarkSafetyInput)
                        }
                        showBookmarkDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberBlack)
                ) {
                    Text("Bookmark")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBookmarkDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CyberDarkSurface
        )
    }
}

@Composable
fun RenderWebpageContents(url: String, onNavigate: (String) -> Unit) {
    when {
        url.contains("i2p-project.i2p") -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Simulating standard documentation wiki host inside garlic-routed local directory.", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                Text(
                    "Welcome to the Invisible Internet Project (I2P) decentralized anonymous communication layer. Unlike peer-to-peer torrents, I2P routing layers protect both the client identity and the host server (known as eepsite services) through dynamic garlic routing pools.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text("Suggested Core Portals:", style = MaterialTheme.typography.bodyMedium, color = CyberBlue, fontWeight = FontWeight.Bold)
                WebpageHyperlink("Browse Anonymous Relay Chat (anon.chat.i2p)", "http://anon.chat.i2p", onNavigate)
                WebpageHyperlink("Browse Invisible Cryptic Wiki (wiki.leaks.i2p)", "http://wiki.leaks.i2p", onNavigate)
                WebpageHyperlink("Browse Garlic Mail Service (secure.mail.i2p)", "http://secure.mail.i2p", onNavigate)
            }
        }
        url.contains("anon.chat.i2p") -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(CyberGreen, RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Live Node Chat Pool Active", color = CyberGreen, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Text(
                    "AnonIRC lets you converse safely inside high-entropy garlic tunnels. This portal is linked with your cryptographic keys to secure conversations. Navigate to the Encrypted Communications tab to dispatch private keypairs or secure text packets.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text("Available Channels:", style = MaterialTheme.typography.bodyMedium, color = CyberBlue, fontWeight = FontWeight.Bold)
                Text("#lobby - Generic discussions (241 active peers)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text("#cryptography - Cryptographic tunnel discussions (88 active peers)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text("#leaks - Document leaks submissions (15 active peers)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
        }
        url.contains("wiki.leaks.i2p") -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("WIKILEAKS INTERNAL INVISIBLE REPOSITORY", style = MaterialTheme.typography.bodySmall, color = CyberOrange, fontWeight = FontWeight.Bold)
                Text(
                    "This decentralized database acts as an anonymous cryptographic drop box. Our system receives encrypted garlic payloads, logs peer correlations, and redistributes documents without centralized physical hosts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Divider(color = CyberBorder)
                Text("Recently leaked reports:", style = MaterialTheme.typography.bodyMedium, color = CyberBlue, fontWeight = FontWeight.Bold)
                Text("• ISP Data Retention Audits 2026 - Leaked PDF index", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text("• Surveillance telemetry logs - Automated router interception", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
        }
        url.contains("secure.mail.i2p") -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("GARLIC SECURE MAIL ENGINE", style = MaterialTheme.typography.bodySmall, color = CyberPurple, fontWeight = FontWeight.Bold)
                Text(
                    "Send and collect asymmetric encrypted emails. Messages sent via secure.mail.i2p are automatically wrapped in Garlic cryptocodes and dispatched to host destination hashes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Divider(color = CyberBorder)
                Text("Inbox status:", style = MaterialTheme.typography.bodyMedium, color = CyberBlue, fontWeight = FontWeight.Bold)
                Text("All messages are digitally signed. Private communication is protected under ElGamal keys.", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
        }
        url.contains("darkbert.intel") -> {
            DarkBERTPortal()
        }
        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("GENERIC DECENTRALIZED EEPSITE SERVICE", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                Text(
                    "This website is hosted completely inside the decentralized Peer-to-Peer I2P network, routing traffic dynamically without relying on DNS servers or clearweb hosting providers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text("The connection remains end-to-end encrypted under garlic-clad leaseSets.", style = MaterialTheme.typography.bodySmall, color = CyberGreen)
            }
        }
    }
}

@Composable
fun I2PCircuitStatsWidget(
    state: RouterState,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, CyberBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("circuit_stats_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Widget Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = CyberPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "I2P ACTIVE CIRCUIT HEALTH",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberPurple,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (state.isConnected) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(CyberGreen, RoundedCornerShape(4.dp))
                        )
                        Text(
                            "LIVE",
                            color = CyberGreen,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(TextSecondary, RoundedCornerShape(4.dp))
                        )
                        Text(
                            "INACTIVE",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Latency Stat
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(CyberBlack, RoundedCornerShape(8.dp))
                        .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = CyberOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "LATENCY",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (state.isConnected) "${state.latencyMs} ms" else "---",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (state.isConnected) {
                            if (state.latencyMs > 800) CyberRed else if (state.latencyMs > 400) CyberOrange else CyberGreen
                        } else TextSecondary,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("circuit_latency_value")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // Latency bar indicator
                    if (state.isConnected) {
                        val percentage = (state.latencyMs.toFloat() / 1200f).coerceIn(0.1f, 1.0f)
                        LinearProgressIndicator(
                            progress = { percentage },
                            color = if (state.latencyMs > 800) CyberRed else if (state.latencyMs > 400) CyberOrange else CyberGreen,
                            trackColor = CyberBorder.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(CyberBorder.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                        )
                    }
                }

                // Packet Loss Stat
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(CyberBlack, RoundedCornerShape(8.dp))
                        .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = CyberRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "LOSS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (state.isConnected) String.format(Locale.US, "%.2f%%", state.packetLoss) else "---",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (state.isConnected) {
                            if (state.packetLoss > 2.0f) CyberRed else if (state.packetLoss > 0.8f) CyberOrange else CyberGreen
                        } else TextSecondary,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("circuit_packet_loss_value")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // Loss bar indicator
                    if (state.isConnected) {
                        val percentage = (state.packetLoss / 5.0f).coerceIn(0.05f, 1.0f)
                        LinearProgressIndicator(
                            progress = { percentage },
                            color = if (state.packetLoss > 2.0f) CyberRed else if (state.packetLoss > 0.8f) CyberOrange else CyberGreen,
                            trackColor = CyberBorder.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(CyberBorder.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Tunnel Peers row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberBlack, RoundedCornerShape(8.dp))
                    .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Router,
                        contentDescription = null,
                        tint = CyberBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            "ACTIVE CIRCUIT PEERS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Nodes participating in current leaseSets",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = 9.sp
                        )
                    }
                }

                Text(
                    text = if (state.isConnected) "${state.activePeerCount} peers" else "0 peers",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state.isConnected) CyberBlue else TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("circuit_active_peers_value")
                )
            }

            if (state.isConnected) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = CyberGreen.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Circuit is stable. Double-hop ElGamal/AES-256 wrapping active across ${state.tunnelHops} hops.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
fun I2POptimizerWidget(
    state: RouterState,
    viewModel: I2PViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, CyberBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("circuit_optimizer_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = CyberGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "NETWORK HEALTH OPTIMIZER",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberGreen,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!state.isConnected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberBlack, RoundedCornerShape(8.dp))
                        .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Connect the Garlic Router to activate real-time self-healing and optimization utilities.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = "Operational anomalies detected in the circuit can be automatically patched using the recommended tactical procedures below:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 2x2 Grid of Actions
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Fast Path
                        OutlinedButton(
                            onClick = { viewModel.optimizeLatency() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = CyberBlack,
                                contentColor = CyberOrange
                            ),
                            border = BorderStroke(1.dp, CyberOrange.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .testTag("btn_optimize_latency")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("FAST PATH", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Text("Optimize Latency", fontSize = 9.sp, color = TextSecondary)
                            }
                        }

                        // Pool Refresh
                        OutlinedButton(
                            onClick = { viewModel.rebuildTunnelPool() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = CyberBlack,
                                contentColor = CyberBlue
                            ),
                            border = BorderStroke(1.dp, CyberBlue.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .testTag("btn_rebuild_pool")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("POOL REFRESH", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Text("Purge Packet Loss", fontSize = 9.sp, color = TextSecondary)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Auto-Stabilize
                        OutlinedButton(
                            onClick = { viewModel.autoStabilizeCircuit() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = CyberBlack,
                                contentColor = CyberPurple
                            ),
                            border = BorderStroke(1.dp, CyberPurple.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .testTag("btn_auto_stabilize")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("STABILIZE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Text("Restore Balanced 3-Hops", fontSize = 9.sp, color = TextSecondary)
                            }
                        }

                        // Full Self-Healing
                        OutlinedButton(
                            onClick = { viewModel.autoFixAnomalies() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = CyberBlack,
                                contentColor = CyberGreen
                            ),
                            border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .testTag("btn_self_healing")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("SELF-HEALING", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Text("Run Full Diagnostics", fontSize = 9.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun I2PHelpCenterWidget(
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, CyberBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("circuit_help_center_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row (clickable to expand/collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = CyberBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "GARLIC ROUTING KNOWLEDGE BASE",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = CyberBlue,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = CyberBorder.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Section 1: Garlic Circuit Mechanics
                    Column {
                        Text(
                            "What is Garlic Routing?",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Garlic routing is an extension of onion routing. It encrypts multiple messages together (called garlic cloves) to make traffic analysis extremely difficult. Packets travel through a unidirectional path of pre-configured tunnel nodes (hops). Each hop strips off one layer of encryption, forwarding the payload to the next participant until the destination is reached.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    // Section 2: Safe Metric Ranges
                    Column {
                        Text(
                            "Safe Operational Thresholds",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Latency Table/Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Metric", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text("Safe Range", style = MaterialTheme.typography.labelSmall, color = CyberGreen, fontWeight = FontWeight.Bold)
                            Text("Critical Spike", style = MaterialTheme.typography.labelSmall, color = CyberRed, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Circuit Latency", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            Text("< 400 ms", style = MaterialTheme.typography.bodySmall, color = CyberGreen, fontFamily = FontFamily.Monospace)
                            Text("> 800 ms", style = MaterialTheme.typography.bodySmall, color = CyberRed, fontFamily = FontFamily.Monospace)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Packet Loss Rate", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            Text("< 0.80%", style = MaterialTheme.typography.bodySmall, color = CyberGreen, fontFamily = FontFamily.Monospace)
                            Text("> 2.00%", style = MaterialTheme.typography.bodySmall, color = CyberRed, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Section 3: Diagnostic Fixes
                    Column {
                        Text(
                            "Mitigation Strategies",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        BulletPoint("High Latency Spikes", "Caused by traversing multiple global nodes. Activate 'FAST PATH' to temporarily drop to a low-latency 1-hop tunnel pool.")
                        BulletPoint("High Packet Loss Rates", "Caused by unstable or compromised intermediary garlic peers. Trigger 'POOL REFRESH' to flush local netDB descriptors and establish new circuits.")
                        BulletPoint("Degraded Security Scores", "Always utilize standard 3-hop or high-privacy 5-hop configurations when communicating sensitive garlic-encrypted payloads.")
                    }
                }
            }
        }
    }
}

@Composable
fun BulletPoint(title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("•", color = CyberBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Column {
            Text(title, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
fun DarkBERTPortal(modifier: Modifier = Modifier) {
    var queryInput by remember { mutableStateOf("") }
    var analysisResult by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CyberBlack)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Portal Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberPurple, RoundedCornerShape(8.dp))
                .background(CyberPurple.copy(alpha = 0.05f))
                .padding(12.dp)
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = CyberPurple,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    "DarkBERT THREAT INTEL v2.5",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "Domain-Specific Darknet Security & Threat Intelligence",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Text(
            "Powered by deep darknet crawl repositories. DarkBERT provides automated intelligence parsing on leak databases, malware exploits, and deanonymization vectors.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        // Preset chips
        Text(
            "QUICK THREAT PROFILE QUERY:",
            style = MaterialTheme.typography.labelSmall,
            color = CyberPurple,
            fontWeight = FontWeight.Bold
        )

        val presets = listOf(
            "Scan wiki.leaks.i2p for active security leaks",
            "Garlic vs Onion Routing: Timing correlations",
            "What accessories protect my browser fingerprint?",
            "Explain common darknet e-mail phishing exploits"
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            presets.forEach { preset ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                    border = BorderStroke(1.dp, CyberBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { queryInput = preset }
                ) {
                    Text(
                        text = "• $preset",
                        color = CyberBlue,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // Custom input
        OutlinedTextField(
            value = queryInput,
            onValueChange = { queryInput = it },
            label = { Text("Query DarkBERT Intel Database", color = TextSecondary) },
            placeholder = { Text("Enter target IP, eepsite, or security topic...", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth().testTag("darkbert_query_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = CyberPurple,
                unfocusedBorderColor = CyberBorder,
                focusedContainerColor = CyberBlack,
                unfocusedContainerColor = CyberBlack
            ),
            singleLine = true
        )

        // Action Button
        Button(
            onClick = {
                if (queryInput.isNotBlank()) {
                    scope.launch {
                        isAnalyzing = true
                        analysisResult = ""
                        analysisResult = com.example.data.queryDarkBERT(queryInput)
                        isAnalyzing = false
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberPurple,
                contentColor = Color.White
            ),
            enabled = !isAnalyzing && queryInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("darkbert_submit_button")
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing Threat Signatures...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("DISPATCH DARKBERT INQUIRY", fontWeight = FontWeight.Bold)
            }
        }

        // Analysis Output
        if (isAnalyzing || analysisResult.isNotEmpty()) {
            Text(
                "DARKBERT INTELLIGENCE OUTPUT:",
                style = MaterialTheme.typography.labelSmall,
                color = CyberGreen,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberBlack, RoundedCornerShape(8.dp))
                    .border(1.dp, if (isAnalyzing) CyberPurple else CyberGreen, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                if (isAnalyzing) {
                    Text(
                        "system@darkbert_core:~$ querying deep database hashes...\n[Analyzing entropy packets and correlation models...]",
                        color = CyberPurple,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.testTag("darkbert_output_box")
                    )
                } else {
                    Text(
                        text = analysisResult,
                        color = if (analysisResult.startsWith("ERROR")) CyberRed else CyberGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.testTag("darkbert_output_box")
                    )
                }
            }
        }
    }
}

@Composable
fun WebpageHyperlink(text: String, url: String, onNavigate: (String) -> Unit) {
    Text(
        text = text,
        color = CyberBlue,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .clickable { onNavigate(url) }
            .padding(vertical = 4.dp)
    )
}

enum class CommsSubTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    GARLIC_CHAT("Garlic Chat", Icons.Default.Forum),
    TERMINAL_CRYPTOR("Terminal Cryptor", Icons.Default.Code)
}

@Composable
fun CommunicationsScreen(
    viewModel: I2PViewModel,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf(CommsSubTab.GARLIC_CHAT) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        // Sub-tabs indicator
        TabRow(
            selectedTabIndex = activeSubTab.ordinal,
            containerColor = CyberDarkSurface,
            contentColor = CyberBlue,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeSubTab.ordinal]),
                    color = CyberBlue
                )
            }
        ) {
            CommsSubTab.values().forEach { tab ->
                val isSelected = activeSubTab == tab
                Tab(
                    selected = isSelected,
                    onClick = { activeSubTab = tab },
                    text = {
                        Text(
                            tab.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = {
                        Icon(
                            tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) CyberBlue else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    selectedContentColor = CyberBlue,
                    unselectedContentColor = TextSecondary,
                    modifier = Modifier.testTag("comms_subtab_${tab.name.lowercase()}")
                )
            }
        }

        when (activeSubTab) {
            CommsSubTab.GARLIC_CHAT -> GarlicChatTab(viewModel = viewModel)
            CommsSubTab.TERMINAL_CRYPTOR -> TerminalCryptorTab(viewModel = viewModel)
        }
    }
}

@Composable
fun GarlicChatTab(
    viewModel: I2PViewModel,
    modifier: Modifier = Modifier
) {
    val contacts by viewModel.contacts.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val activeIdentity by viewModel.activeIdentity.collectAsState()
    val routerState by viewModel.routerState.collectAsState()

    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var messageInput by remember { mutableStateOf("") }

    // Synchronize selected contact if list changes or becomes empty
    LaunchedEffect(contacts) {
        if (selectedContact == null && contacts.isNotEmpty()) {
            selectedContact = contacts.firstOrNull()
        } else if (contacts.isNotEmpty() && !contacts.contains(selectedContact)) {
            selectedContact = contacts.firstOrNull()
        }
    }

    // Add Contact Form States
    var newContactName by remember { mutableStateOf("") }
    var newContactAddress by remember { mutableStateOf("") }
    var newContactType by remember { mutableStateOf("GOOGLE_CHAT") } // GOOGLE_CHAT, SMS, SECURE_I2P

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "SECURE DIRECTORY & CHAT",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    "Signal Protocol • OTR over OAuth2 • Garlic Routing",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            
            Button(
                onClick = { showAddContactDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberBlue,
                    contentColor = CyberBlack
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Contact", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ADD", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        // Service Type Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ALL", "SECURE_I2P", "GOOGLE_CHAT", "SMS").forEach { type ->
                val isSelected = selectedTypeFilter == type
                val label = when (type) {
                    "SECURE_I2P" -> "I2P GARLIC"
                    "GOOGLE_CHAT" -> "GOOGLE CHAT"
                    "SMS" -> "SECURE SMS"
                    else -> "ALL NETWORKS"
                }
                val chipColor = when (type) {
                    "SECURE_I2P" -> CyberGreen
                    "GOOGLE_CHAT" -> CyberYellow
                    "SMS" -> CyberBlue
                    else -> CyberPurple
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) chipColor.copy(alpha = 0.15f) else CyberDarkSurface,
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) chipColor else CyberBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { selectedTypeFilter = type }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 8.sp,
                        color = if (isSelected) chipColor else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Scrollable Contacts Carousel Row
        val filteredContacts = remember(contacts, selectedTypeFilter) {
            contacts.filter { selectedTypeFilter == "ALL" || it.type == selectedTypeFilter }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    "PEER CONTACT DIRECTORY (${filteredContacts.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (filteredContacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No contacts found for selected network filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        filteredContacts.forEach { contact ->
                            val isSelected = selectedContact?.id == contact.id
                            val contactColor = Color(android.graphics.Color.parseColor(contact.avatarColorHex))
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { selectedContact = contact }
                                    .padding(4.dp)
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(
                                                if (isSelected) contactColor.copy(alpha = 0.25f) else CyberBlack,
                                                CircleShape
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) contactColor else CyberBorder,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = contact.name.take(2).uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = contactColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    // Status and Service Overlay Badges
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Network service symbol badge
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .background(CyberBlack, CircleShape)
                                                .border(0.5.dp, contactColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (contact.type) {
                                                    "GOOGLE_CHAT" -> Icons.Default.AlternateEmail
                                                    "SMS" -> Icons.Default.Sms
                                                    else -> Icons.Default.Lock
                                                },
                                                contentDescription = contact.type,
                                                tint = contactColor,
                                                modifier = Modifier.size(8.dp)
                                            )
                                        }

                                        // Online/Offline status dot
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(
                                                    if (contact.status == "ONLINE") CyberGreen else TextSecondary,
                                                    CircleShape
                                                )
                                                .border(1.dp, CyberBlack, CircleShape)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = contact.name,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Chat Conversation Pane
        if (selectedContact != null) {
            val contact = selectedContact!!
            val contactColor = Color(android.graphics.Color.parseColor(contact.avatarColorHex))

            // Contact Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(contactColor.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, contactColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                contact.name.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = contactColor
                            )
                        }
                        
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    contact.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .background(contactColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .border(0.5.dp, contactColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = when (contact.type) {
                                            "GOOGLE_CHAT" -> "GOOGLE CHAT (E2EE)"
                                            "SMS" -> "SMS (SILENCE PROTOCOL)"
                                            else -> "I2P GARLIC PEER"
                                        },
                                        fontSize = 7.sp,
                                        color = contactColor,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Text(
                                text = "Address: ${contact.address}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Delete Contact Button
                    IconButton(
                        onClick = {
                            viewModel.deleteContact(contact)
                            selectedContact = null
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Contact", tint = CyberRed.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Filtering messages for the active conversation
            val conversationMessages = remember(messages, contact) {
                messages.filter { msg ->
                    msg.senderAddress == contact.address || msg.recipientAddress == contact.address
                }.reversed()
            }

            // Message Stream Column
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(CyberBlack)
            ) {
                if (conversationMessages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Forum, contentDescription = null, tint = contactColor.copy(alpha = 0.4f), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No messages exchanged in this secure tunnel.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(conversationMessages) { msg ->
                            ContactMessageBubble(msg = msg, contact = contact)
                        }
                    }
                }
            }

            // Message Input Drawer
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (contact.type) {
                                "GOOGLE_CHAT" -> "SENDING VIA GOOGLE CHAT PROTOCOL"
                                "SMS" -> "SENDING VIA SMS CELLULAR CARRIER"
                                else -> "SENDING VIA I2P GARLIC TUNNEL"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = contactColor,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp
                        )

                        Text(
                            text = if (routerState.isConnected || contact.type != "SECURE_I2P") "CARRIER ENCRYPTED" else "OFFLINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (routerState.isConnected || contact.type != "SECURE_I2P") CyberGreen else CyberRed,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text("Enter private chat content...", color = TextSecondary, fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("comms_message_input"),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = contactColor,
                            unfocusedBorderColor = CyberBorder
                        ),
                        maxLines = 3,
                        trailingIcon = {
                            if (messageInput.isNotEmpty()) {
                                IconButton(onClick = { messageInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (messageInput.isNotEmpty()) {
                                viewModel.sendSecureContactMessage(contact, messageInput)
                                messageInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = contactColor,
                            contentColor = CyberBlack
                        ),
                        enabled = (routerState.isConnected || contact.type != "SECURE_I2P") && activeIdentity != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("comms_send_button"),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = when (contact.type) {
                                "GOOGLE_CHAT" -> Icons.Default.AlternateEmail
                                "SMS" -> Icons.Default.Sms
                                else -> Icons.Default.EnhancedEncryption
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                activeIdentity == null -> "Configure Profile to Send"
                                contact.type == "SECURE_I2P" && !routerState.isConnected -> "Connect Router to Send"
                                contact.type == "GOOGLE_CHAT" -> "Double-Ratchet Encrypt & Dispatch"
                                contact.type == "SMS" -> "OTR SMS Encrypt & Send"
                                else -> "Garlic-Encrypt & Send"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(CyberDarkSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContactMail,
                        contentDescription = "Select Contact",
                        tint = CyberPurple,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "SECURE MESSAGING CHANNEL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Please tap any peer profile from the directory carousel above to establish a secure E2EE chat tunnel, or tap ADD to register new Google Chat or SMS contacts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 280.dp)
                    )
                }
            }
        }
    }

    // Add Contact Pop-up Dialog
    if (showAddContactDialog) {
        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            containerColor = CyberDarkSurface,
            title = {
                Text(
                    "ADD SECURE DISPATCH CONTACT",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyberBlue,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Register a cryptographic peer handle, Google Chat email, or SMS phone number to support client-side payload encryption.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = newContactName,
                        onValueChange = { newContactName = it },
                        label = { Text("Display Name", color = TextSecondary) },
                        placeholder = { Text("e.g. Alice Watson", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newContactAddress,
                        onValueChange = { newContactAddress = it },
                        label = { Text("Network Address", color = TextSecondary) },
                        placeholder = {
                            Text(
                                text = when (newContactType) {
                                    "GOOGLE_CHAT" -> "e.g. alice@gmail.com"
                                    "SMS" -> "e.g. +155512345"
                                    else -> "e.g. alice.i2p"
                                },
                                color = TextSecondary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        ),
                        singleLine = true
                    )

                    // Network Platform selector
                    Text(
                        "INTEGRATED CARRIER TRANSPORT",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("SECURE_I2P", "GOOGLE_CHAT", "SMS").forEach { type ->
                            val isSelected = newContactType == type
                            val typeLabel = when (type) {
                                "SECURE_I2P" -> "I2P Garlic"
                                "GOOGLE_CHAT" -> "Google Chat"
                                "SMS" -> "Secure SMS"
                                else -> ""
                            }
                            val typeColor = when (type) {
                                "SECURE_I2P" -> CyberGreen
                                "GOOGLE_CHAT" -> CyberYellow
                                "SMS" -> CyberBlue
                                else -> CyberPurple
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) typeColor.copy(alpha = 0.2f) else CyberBlack,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) typeColor else CyberBorder,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { newContactType = type }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = typeLabel,
                                    fontSize = 9.sp,
                                    color = if (isSelected) typeColor else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newContactName.isNotEmpty() && newContactAddress.isNotEmpty()) {
                            viewModel.addContact(newContactName, newContactAddress, newContactType)
                            showAddContactDialog = false
                            newContactName = ""
                            newContactAddress = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberBlue, contentColor = CyberBlack),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("SAVE PEER", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddContactDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun ContactMessageBubble(
    msg: SecureMessage,
    contact: Contact
) {
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = formatter.format(Date(msg.timestamp))
    val isOutgoing = !msg.isIncoming
    val contactColor = Color(android.graphics.Color.parseColor(contact.avatarColorHex))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isOutgoing) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(contactColor.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, contactColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.take(1).uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = contactColor
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column(
                horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val serviceIcon = when (contact.type) {
                        "GOOGLE_CHAT" -> Icons.Default.AlternateEmail
                        "SMS" -> Icons.Default.Sms
                        else -> Icons.Default.Lock
                    }
                    Icon(
                        imageVector = serviceIcon,
                        contentDescription = contact.type,
                        tint = contactColor,
                        modifier = Modifier.size(9.dp)
                    )
                    Text(
                        text = if (isOutgoing) "SECURE DISPATCH" else contact.name.uppercase(),
                        fontSize = 8.sp,
                        color = if (isOutgoing) CyberGreen else CyberPurple,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Box(
                    modifier = Modifier
                        .background(
                            color = if (isOutgoing) CyberBorder.copy(alpha = 0.6f) else CyberDarkSurface,
                            shape = RoundedCornerShape(
                                topStart = 10.dp,
                                topEnd = 10.dp,
                                bottomStart = if (isOutgoing) 10.dp else 0.dp,
                                bottomEnd = if (isOutgoing) 0.dp else 10.dp
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = if (isOutgoing) CyberGreen.copy(alpha = 0.3f) else CyberPurple.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(
                                topStart = 10.dp,
                                topEnd = 10.dp,
                                bottomStart = if (isOutgoing) 10.dp else 0.dp,
                                bottomEnd = if (isOutgoing) 0.dp else 10.dp
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .widthIn(max = 240.dp)
                ) {
                    Column {
                        Text(
                            text = msg.decryptedBody ?: "[Encrypted Payload]",
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = timeStr,
                                fontSize = 8.sp,
                                color = TextSecondary
                            )
                            if (isOutgoing) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Delivered",
                                    tint = CyberGreen,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isOutgoing) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(CyberPurple.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, CyberPurple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ME",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberPurple
                    )
                }
            }
        }
    }
}

// Global Memory Cache for Peer Sandbox Key Generation Fallback
private val dynamicKeysCache = HashMap<String, Pair<String, String>>()

fun getOrCreateValidKeysForPeer(alias: String, keyBase64: String): Pair<String, String> {
    if (keyBase64.length < 50 || keyBase64.contains("...")) {
        val cached = dynamicKeysCache[alias]
        if (cached != null) return cached

        return try {
            val keyGen = java.security.KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(1024)
            val pair = keyGen.generateKeyPair()
            val pub = android.util.Base64.encodeToString(pair.public.encoded, android.util.Base64.NO_WRAP)
            val priv = android.util.Base64.encodeToString(pair.private.encoded, android.util.Base64.NO_WRAP)
            val result = Pair(pub, priv)
            dynamicKeysCache[alias] = result
            result
        } catch (e: Exception) {
            Pair(keyBase64, "")
        }
    }
    return Pair(keyBase64, "")
}

fun encryptRSA(plainText: String, publicKeyBase64: String): String {
    return try {
        val cleanKey = publicKeyBase64
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .replace(" ", "")
            .trim()
        val keyBytes = android.util.Base64.decode(cleanKey, android.util.Base64.DEFAULT)
        val spec = java.security.spec.X509EncodedKeySpec(keyBytes)
        val kf = java.security.KeyFactory.getInstance("RSA")
        val publicKey = kf.generatePublic(spec)
        val cipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, publicKey)
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.NO_WRAP)
    } catch (e: Exception) {
        "ERROR: Encryption failed - ${e.localizedMessage}"
    }
}

fun decryptRSA(cipherTextBase64: String, privateKeyBase64: String): String {
    return try {
        val cleanKey = privateKeyBase64
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .replace(" ", "")
            .trim()
        val keyBytes = android.util.Base64.decode(cleanKey, android.util.Base64.DEFAULT)
        val spec = java.security.spec.X509EncodedKeySpec(keyBytes) // Wait, PKCS8 is used for RSA Private Keys!
        // Wait, standard java.security.spec.PKCS8EncodedKeySpec is used for Private Keys:
        val specPriv = java.security.spec.PKCS8EncodedKeySpec(keyBytes)
        val kf = java.security.KeyFactory.getInstance("RSA")
        val privateKey = kf.generatePrivate(specPriv)
        val cipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, privateKey)
        val encryptedBytes = android.util.Base64.decode(cipherTextBase64, android.util.Base64.DEFAULT)
        String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
    } catch (e: Exception) {
        "ERROR: Decryption failed - ${e.localizedMessage}"
    }
}

@Composable
fun TerminalCryptorTab(
    viewModel: I2PViewModel,
    modifier: Modifier = Modifier
) {
    val activeIdentity by viewModel.activeIdentity.collectAsState()
    val trustedKeysList by viewModel.trustedKeys.collectAsState()

    var cryptorPlaintext by remember { mutableStateOf("") }
    var encryptedResultText by remember { mutableStateOf("") }
    var selectedKeyType by remember { mutableStateOf("my_identity") } // "my_identity" or "peer_key"
    var selectedPeerKey by remember { mutableStateOf<TrustedKey?>(null) }

    val terminalLogs = remember { mutableStateListOf<String>() }
    var isProcessing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Automatically set default selected peer key if any exists
    LaunchedEffect(trustedKeysList) {
        if (selectedPeerKey == null && trustedKeysList.isNotEmpty()) {
            selectedPeerKey = trustedKeysList.first()
        }
    }

    // Automatically scroll terminal to bottom on change
    LaunchedEffect(terminalLogs.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Block
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "CRYPTOGRAPHIC TERMINAL PLAYGROUND",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberPurple,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Encrypt secure messages using real asymmetric RSA keys. Load either your own generated key pair or any peer's public key from your verified keyring, then run and watch the encryption steps stream live inside the mock terminal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Input and Options Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "PREPARE PLAINTEXT & CRYPTOGRAPHIC PARAMS",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberBlue,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Plaintext text input field
                OutlinedTextField(
                    value = cryptorPlaintext,
                    onValueChange = { cryptorPlaintext = it },
                    label = { Text("Secure plaintext message to encrypt", color = TextSecondary) },
                    placeholder = { Text("Enter secret information here...", color = TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("terminal_cryptor_plaintext"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberGreen,
                        unfocusedBorderColor = CyberBorder
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Select Encryption Key Option
                Text(
                    "SELECT TARGET ENCRYPTION KEY",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1: My Identity
                    OutlinedButton(
                        onClick = { selectedKeyType = "my_identity" },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedKeyType == "my_identity") CyberPurple.copy(alpha = 0.15f) else Color.Transparent,
                            contentColor = if (selectedKeyType == "my_identity") CyberPurple else TextPrimary
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selectedKeyType == "my_identity") CyberPurple else CyberBorder
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("My Identity Public Key", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = activeIdentity?.name ?: "No active identity",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedKeyType == "my_identity") CyberPurple else TextSecondary
                            )
                        }
                    }

                    // Option 2: Peer Key
                    OutlinedButton(
                        onClick = { selectedKeyType = "peer_key" },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedKeyType == "peer_key") CyberBlue.copy(alpha = 0.15f) else Color.Transparent,
                            contentColor = if (selectedKeyType == "peer_key") CyberBlue else TextPrimary
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selectedKeyType == "peer_key") CyberBlue else CyberBorder
                        ),
                        modifier = Modifier.weight(1f),
                        enabled = trustedKeysList.isNotEmpty(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Trusted Peer Public Key", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = if (trustedKeysList.isEmpty()) "No trusted keys" else selectedPeerKey?.alias ?: "Select peer",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedKeyType == "peer_key") CyberBlue else TextSecondary
                            )
                        }
                    }
                }

                // Peer list picker if Option 2 selected
                if (selectedKeyType == "peer_key" && trustedKeysList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "CHOOSE VERIFIED PEER KEYRING:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        trustedKeysList.forEach { peerKey ->
                            val isChosen = selectedPeerKey?.id == peerKey.id
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isChosen) CyberBlue.copy(alpha = 0.2f) else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isChosen) CyberBlue else CyberBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedPeerKey = peerKey }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = peerKey.alias,
                                    color = if (isChosen) CyberBlue else TextSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Button 1: Encrypt
                    Button(
                        onClick = {
                            if (cryptorPlaintext.isNotEmpty()) {
                                scope.launch {
                                    isProcessing = true
                                    terminalLogs.clear()
                                    encryptedResultText = ""

                                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                                    terminalLogs.add("[$timestamp] SHADOW-CRYPT RSA MODULE ENGINE v1.4.2 INITIALIZING...")
                                    delay(200)
                                    terminalLogs.add("[$timestamp] CYPHER-MODE: RSA 1024-bit key structure asymmetric algorithm.")
                                    delay(150)

                                    val keyName = if (selectedKeyType == "my_identity") activeIdentity?.name ?: "My Identity" else selectedPeerKey?.alias ?: "Peer Key"
                                    val keyBase64 = if (selectedKeyType == "my_identity") activeIdentity?.publicKeyBase64 else selectedPeerKey?.publicKeyBase64

                                    if (keyBase64 == null) {
                                        terminalLogs.add("[$timestamp] ERROR: Selected encryption key is null!")
                                        isProcessing = false
                                        return@launch
                                    }

                                    terminalLogs.add("[$timestamp] LOADING: Retrieving public key bytes for '$keyName'...")
                                    delay(200)

                                    val resolvedKeys = getOrCreateValidKeysForPeer(keyName, keyBase64)
                                    val activePubKey = resolvedKeys.first
                                    val resolvedPrivKey = resolvedKeys.second

                                    if (resolvedPrivKey.isNotEmpty() && selectedKeyType == "peer_key") {
                                        terminalLogs.add("[$timestamp] WARN: Truncated mock key identified! Automatically resolved to a fully valid RSA key pair for session sandbox.")
                                        delay(300)
                                    }

                                    terminalLogs.add("[$timestamp] ALGO: Setting padding mode to RSA/ECB/PKCS1Padding...")
                                    delay(150)
                                    terminalLogs.add("[$timestamp] ENCRYPT: Commencing cryptographic cipher transformation on plaintext bytes...")
                                    delay(350)

                                    val finalCipher = encryptRSA(cryptorPlaintext, activePubKey)

                                    if (finalCipher.startsWith("ERROR")) {
                                        terminalLogs.add("[$timestamp] CRYPT-FAIL: Cipher output aborted.")
                                        terminalLogs.add("[$timestamp] DETAILS: $finalCipher")
                                    } else {
                                        terminalLogs.add("[$timestamp] SUCCESS: Cipher completed successfully! Encrypted payload generated.")
                                        delay(150)
                                        terminalLogs.add("[$timestamp] GARLIC: Sealed and wrapped with session ephemeral tags.")
                                        delay(150)
                                        terminalLogs.add("[$timestamp] CIPHERTEXT IN BASE64:")
                                        terminalLogs.add(finalCipher)
                                        encryptedResultText = finalCipher
                                    }
                                    isProcessing = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberGreen,
                            contentColor = CyberBlack
                        ),
                        enabled = !isProcessing && cryptorPlaintext.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.EnhancedEncryption, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RSA-Encrypt", fontWeight = FontWeight.Bold)
                    }

                    // Button 2: Decrypt
                    val canDecrypt = encryptedResultText.isNotEmpty() && !isProcessing
                    Button(
                        onClick = {
                            scope.launch {
                                isProcessing = true
                                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                                terminalLogs.add("[$timestamp] AUTHORIZE: Accessing private key certificate container...")
                                delay(200)

                                val keyName = if (selectedKeyType == "my_identity") activeIdentity?.name ?: "My Identity" else selectedPeerKey?.alias ?: "Peer Key"
                                val keyBase64 = if (selectedKeyType == "my_identity") activeIdentity?.publicKeyBase64 else selectedPeerKey?.publicKeyBase64

                                val privateKeyToUse = if (selectedKeyType == "my_identity") {
                                    activeIdentity?.privateKeyBase64
                                } else {
                                    if (keyBase64 != null) {
                                        getOrCreateValidKeysForPeer(keyName, keyBase64).second
                                    } else null
                                }

                                if (privateKeyToUse == null || privateKeyToUse.isEmpty()) {
                                    terminalLogs.add("[$timestamp] ERROR: Corresponding asymmetric private key not found! Decryption impossible without private key.")
                                    isProcessing = false
                                    return@launch
                                }

                                terminalLogs.add("[$timestamp] DECRYPT: Instantiating RSA decryption core engine...")
                                delay(200)
                                terminalLogs.add("[$timestamp] UNWRAP: Removing garlic encapsulation routing envelopes...")
                                delay(200)
                                terminalLogs.add("[$timestamp] TRANSFORM: Deciphering RSA base64 block...")
                                delay(350)

                                val decryptedResultText = decryptRSA(encryptedResultText, privateKeyToUse)
                                if (decryptedResultText.startsWith("ERROR")) {
                                    terminalLogs.add("[$timestamp] DECRYPT-FAIL: Decryption unsuccessful.")
                                    terminalLogs.add("[$timestamp] DETAILS: $decryptedResultText")
                                } else {
                                    terminalLogs.add("[$timestamp] DECRYPT-OK: Asymmetric private key decryption completed.")
                                    delay(150)
                                    terminalLogs.add("[$timestamp] DECRYPTED VERIFIED BODY:")
                                    terminalLogs.add(">> \"$decryptedResultText\"")
                                }
                                isProcessing = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberPurple,
                            contentColor = Color.White
                        ),
                        enabled = canDecrypt,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RSA-Decrypt", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Mock Terminal visual area
        Text(
            "MOCK ENCRYPTION TERMINAL LOGS",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(CyberBlack, RoundedCornerShape(8.dp))
                .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                if (terminalLogs.isEmpty()) {
                    Text(
                        text = "system@shadow_comms:~$ \n[Waiting for encryption/decryption dispatch. Type a plaintext message above and click Encrypt to trace cryptographic execution...]",
                        color = CyberGreen.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("terminal_cryptor_output")
                    )
                } else {
                    terminalLogs.forEach { logLine ->
                        val color = when {
                            logLine.contains("ERROR") || logLine.contains("FAIL") -> CyberRed
                            logLine.contains("SUCCESS") || logLine.contains("DECRYPT-OK") -> CyberGreen
                            logLine.contains("WARN") -> CyberOrange
                            logLine.contains(">>") -> CyberPurple
                            else -> CyberGreen.copy(alpha = 0.85f)
                        }

                        Text(
                            text = logLine,
                            color = color,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = if (logLine == terminalLogs.last()) Modifier.testTag("terminal_cryptor_output") else Modifier
                        )
                    }

                    // Blinking block cursor at the end!
                    val transition = rememberInfiniteTransition(label = "cursor_blink")
                    val cursorAlpha by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "cursor_alpha"
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "system@shadow_comms:~$ ",
                            color = CyberGreen.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 8.dp, height = 12.dp)
                                .background(CyberGreen.copy(alpha = cursorAlpha))
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun MessageCardItem(msg: SecureMessage) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeStr = formatter.format(Date(msg.timestamp))

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (msg.isIncoming) CyberCardBg else CyberDarkSurface
        ),
        border = BorderStroke(
            1.dp,
            if (msg.isIncoming) CyberPurple.copy(alpha = 0.4f) else CyberGreen.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (msg.isIncoming) "INCOMING GARLIC PAYLOAD" else "OUTGOING DEPLOYMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (msg.isIncoming) CyberPurple else CyberGreen,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Sender: ${msg.senderAddress}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
            Text(
                "Recipient: ${msg.recipientAddress}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = CyberBorder)
            Spacer(modifier = Modifier.height(8.dp))

            // Body text
            Text(
                msg.decryptedBody ?: "[Encrypted payload base64 bytes...]",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Raw Block: ${msg.encryptedPayload.take(24)}...",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

enum class IdentitySubTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    MY_KEYRINGS("My Keyrings", Icons.Default.Key),
    PEER_KEYS("Peer Key Manager", Icons.Default.VpnKey),
    INVITATION_LAB("Secure Invite", Icons.Default.Share)
}

@Composable
fun IdentityScreen(
    viewModel: I2PViewModel,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf(IdentitySubTab.MY_KEYRINGS) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        // Sub-tabs indicator
        TabRow(
            selectedTabIndex = activeSubTab.ordinal,
            containerColor = CyberDarkSurface,
            contentColor = CyberBlue,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeSubTab.ordinal]),
                    color = CyberBlue
                )
            }
        ) {
            IdentitySubTab.values().forEach { tab ->
                val isSelected = activeSubTab == tab
                Tab(
                    selected = isSelected,
                    onClick = { activeSubTab = tab },
                    text = {
                        Text(
                            tab.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = {
                        Icon(
                            tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) CyberBlue else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    selectedContentColor = CyberBlue,
                    unselectedContentColor = TextSecondary,
                    modifier = Modifier.testTag("identity_subtab_${tab.name.lowercase()}")
                )
            }
        }

        when (activeSubTab) {
            IdentitySubTab.MY_KEYRINGS -> MyKeyringsTab(viewModel = viewModel)
            IdentitySubTab.PEER_KEYS -> PeerKeyManagerTab(viewModel = viewModel)
            IdentitySubTab.INVITATION_LAB -> InvitationLabTab(viewModel = viewModel)
        }
    }
}

@Composable
fun MyKeyringsTab(viewModel: I2PViewModel) {
    val identities by viewModel.identities.collectAsState()
    val activeIdentity by viewModel.activeIdentity.collectAsState()
    var aliasNameInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Register New Alias Block
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "REGISTER CRYPTOGRAPHIC SECURITY ALIAS",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Generates standard ElGamal and session-tag asymmetric public/private keys used to advertise leaseSets in NetDB.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = aliasNameInput,
                        onValueChange = { aliasNameInput = it },
                        label = { Text("Comrade pseudonym", color = TextSecondary) },
                        placeholder = { Text("e.g. ComradeX", color = TextSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("identity_alias_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = CyberBorder
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (aliasNameInput.isNotBlank()) {
                                viewModel.createNewIdentity(aliasNameInput)
                                aliasNameInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberBlue,
                            contentColor = CyberBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("identity_generate_button")
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate KeyPair & Alias", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active Identity details with raw keys
        if (activeIdentity != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                    border = BorderStroke(1.dp, CyberBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "ACTIVE KEYRING INFORMATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            activeIdentity!!.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Address Domain: ${activeIdentity!!.i2pAddress}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyberGreen,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Public Key Copy
                        Text(
                            "Asymmetric Public Key (Base64)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                activeIdentity!!.publicKeyBase64.take(32) + "...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(activeIdentity!!.publicKeyBase64))
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Public Key", tint = CyberBlue, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Private Key Warning
                        Text(
                            "Asymmetric Private Key (Secret)",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberOrange
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "••••••••••••••••••••••••••••••••",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberOrange,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(activeIdentity!!.privateKeyBase64))
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Private Key", tint = CyberOrange, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // All profiles list
        item {
            Text(
                "REGISTERED COM-KEYRINGS",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
        }

        if (identities.isEmpty()) {
            item {
                Text(
                    "No aliases configured on this key ring.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        } else {
            items(identities) { identity ->
                val isActive = activeIdentity?.id == identity.id
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) CyberBlue.copy(alpha = 0.1f) else CyberDarkSurface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isActive) CyberBlue else CyberBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.switchIdentity(identity) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                identity.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                identity.i2pAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (isActive) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Active KeyRing",
                                tint = CyberGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PeerKeyManagerTab(viewModel: I2PViewModel) {
    val trustedKeys by viewModel.trustedKeys.collectAsState()
    
    var manualAlias by remember { mutableStateOf("") }
    var manualAddress by remember { mutableStateOf("") }
    var manualPublicKey by remember { mutableStateOf("") }
    var showAddManualDialog by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Statistics Card & Action
        item {
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
                        Column {
                            Text(
                                "PEER KEYRING DATABASE",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Local Address Book & Peer Verification",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Button(
                            onClick = { showAddManualDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberBlue),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Key Manually", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import Key", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "TRUSTED CONTACT KEYRINGS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${trustedKeys.size} active peers",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (trustedKeys.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(CyberDarkSurface, RoundedCornerShape(8.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No trusted peer keys in keyring database.", color = TextSecondary, fontSize = 13.sp)
                        Text("Exchange secure invitations to establish contact.", color = TextSecondary.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                }
            }
        } else {
            items(trustedKeys) { key ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                    border = BorderStroke(1.dp, if (key.isVerified) CyberGreen.copy(alpha = 0.6f) else CyberBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        key.alias,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (key.isVerified) {
                                        Surface(
                                            color = CyberGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(0.5.dp, CyberGreen)
                                        ) {
                                            Text(
                                                "VERIFIED",
                                                color = CyberGreen,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            color = CyberOrange.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(0.5.dp, CyberOrange)
                                        ) {
                                            Text(
                                                "UNVERIFIED",
                                                color = CyberOrange,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    key.i2pAddress,
                                    fontSize = 11.sp,
                                    color = CyberBlue,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            IconButton(onClick = { viewModel.deleteTrustedKey(key) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Revoke Peer Key", tint = CyberRed, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = CyberBorder.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Public Key Segment
                        Text(
                            "Public Key Fingerprint (Base64)",
                            fontSize = 9.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                key.publicKeyBase64.take(48) + "...",
                                fontSize = 10.sp,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { clipboardManager.setText(AnnotatedString(key.publicKeyBase64)) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Public Key", tint = CyberBlue, modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dynamic Hands-on Security Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Session Tag Status Bar
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "GARLIC SESSION TAGS",
                                        fontSize = 8.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${key.sessionTagCount}/100 remaining",
                                        fontSize = 8.sp,
                                        color = if (key.sessionTagCount > 20) CyberGreen else CyberOrange,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = key.sessionTagCount / 100f,
                                    color = if (key.sessionTagCount > 20) CyberGreen else CyberOrange,
                                    trackColor = CyberBorder,
                                    modifier = Modifier.fillMaxWidth().height(4.dp).border(0.5.dp, CyberBorder, RoundedCornerShape(2.dp))
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Verification/Challenge Action Button
                            Button(
                                onClick = { viewModel.verifyPeerKey(key) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (key.isVerified) CyberBorder else CyberGreen.copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, if (key.isVerified) CyberBorder else CyberGreen),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (key.isVerified) Icons.Default.Refresh else Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (key.isVerified) TextSecondary else CyberGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (key.isVerified) "Re-Verify" else "Verify Identity",
                                    fontSize = 10.sp,
                                    color = if (key.isVerified) TextPrimary else CyberGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Manual Key Dialog
    if (showAddManualDialog) {
        AlertDialog(
            onDismissRequest = { showAddManualDialog = false },
            containerColor = CyberBlack,
            title = {
                Text(
                    "IMPORT TRUSTED PEER KEY",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyberBlue,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Paste a peer's cryptographic address parameters to trust their public keys for garlic-encryption.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = manualAlias,
                        onValueChange = { manualAlias = it },
                        label = { Text("Peer Alias / Pseudonym", color = TextSecondary) },
                        placeholder = { Text("e.g. Alice", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = manualAddress,
                        onValueChange = { manualAddress = it },
                        label = { Text("Peer I2P Address", color = TextSecondary) },
                        placeholder = { Text("e.g. alice.i2p", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = manualPublicKey,
                        onValueChange = { manualPublicKey = it },
                        label = { Text("Base64 Public Key", color = TextSecondary) },
                        placeholder = { Text("Paste long cryptographic base64 hash...", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        ),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualAlias.isNotBlank() && manualAddress.isNotBlank() && manualPublicKey.isNotBlank()) {
                            viewModel.importTrustedKey(manualAlias, manualAddress, manualPublicKey, isVerified = false)
                            manualAlias = ""
                            manualAddress = ""
                            manualPublicKey = ""
                            showAddManualDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberBlue),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Trust Key", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddManualDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            modifier = Modifier.border(1.dp, CyberBorder, RoundedCornerShape(28.dp))
        )
    }
}

@Composable
fun InvitationLabTab(viewModel: I2PViewModel) {
    val activeIdentity by viewModel.activeIdentity.collectAsState()
    
    var sharePinCode by remember { mutableStateOf("") }
    var exportFormatByGarlic by remember { mutableStateOf(false) } // False = Basic Link, True = Garlic Encrypted Packet
    
    var importInvitationText by remember { mutableStateOf("") }
    var importPinCode by remember { mutableStateOf("") }
    
    var importStatusMessage by remember { mutableStateOf("") }
    var isImportSuccess by remember { mutableStateOf<Boolean?>(null) }
    
    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Generate Invitation
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "DISPATCH CRYPTOGRAPHIC INVITATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Package your active public key parameters so another comrade can trust you and send you garlic-encrypted messages.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (activeIdentity == null) {
                        Text(
                            "Please create an identity in the 'My Keyrings' tab first.",
                            color = CyberOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        // Interactive Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberBlack, RoundedCornerShape(4.dp))
                                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { exportFormatByGarlic = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!exportFormatByGarlic) CyberBlue.copy(alpha = 0.15f) else Color.Transparent
                                ),
                                shape = RoundedCornerShape(4.dp),
                                border = if (!exportFormatByGarlic) BorderStroke(1.dp, CyberBlue) else null,
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    "Basic Link",
                                    fontSize = 11.sp,
                                    color = if (!exportFormatByGarlic) CyberBlue else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { exportFormatByGarlic = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (exportFormatByGarlic) CyberGreen.copy(alpha = 0.15f) else Color.Transparent
                                ),
                                shape = RoundedCornerShape(4.dp),
                                border = if (exportFormatByGarlic) BorderStroke(1.dp, CyberGreen) else null,
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    "Garlic Packet (Secure)",
                                    fontSize = 11.sp,
                                    color = if (exportFormatByGarlic) CyberGreen else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val activeId = activeIdentity!!
                        if (exportFormatByGarlic) {
                            // PIN input
                            OutlinedTextField(
                                value = sharePinCode,
                                onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) sharePinCode = it },
                                label = { Text("Shared Secret PIN (6 Digits)", color = TextSecondary) },
                                placeholder = { Text("e.g. 583921", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = CyberGreen,
                                    unfocusedBorderColor = CyberBorder
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val code = sharePinCode.ifBlank { "123456" }
                                    val packet = viewModel.generateGarlicEncryptedInvite(
                                        activeId.name, activeId.i2pAddress, activeId.publicKeyBase64, code
                                    )
                                    clipboardManager.setText(AnnotatedString(packet))
                                    importStatusMessage = "Secure Garlic Packet copied! Share it and the PIN: $code"
                                    isImportSuccess = true
                                    sharePinCode = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberBlack),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate & Copy Garlic Packet", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                "Quick, unencrypted invitation link suitable for trusted channels.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Button(
                                onClick = {
                                    val link = viewModel.generateBasicInviteLink(
                                        activeId.name, activeId.i2pAddress, activeId.publicKeyBase64
                                    )
                                    clipboardManager.setText(AnnotatedString(link))
                                    importStatusMessage = "Invitation Link copied to clipboard!"
                                    isImportSuccess = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberBlue, contentColor = CyberBlack),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copy Basic Invitation Link", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Import Invitation
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "IMPORT INVITATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Paste a basic link or garlic-encrypted packet received from another comrade to add them to your Keyring database.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = importInvitationText,
                        onValueChange = { importInvitationText = it },
                        label = { Text("Paste Invitation Code or Link", color = TextSecondary) },
                        placeholder = { Text("i2p://invite... OR garlic-packet-invite:...", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (importInvitationText.startsWith("garlic-packet-invite:")) {
                        OutlinedTextField(
                            value = importPinCode,
                            onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) importPinCode = it },
                            label = { Text("Decryption PIN Code", color = TextSecondary) },
                            placeholder = { Text("Enter 6-digit PIN shared with you...", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = CyberGreen,
                                unfocusedBorderColor = CyberBorder
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Button(
                        onClick = {
                            if (importInvitationText.isBlank()) return@Button
                            
                            val text = importInvitationText.trim()
                            if (text.startsWith("i2p://invite")) {
                                val result = viewModel.parseBasicInviteLink(text)
                                if (result != null) {
                                    viewModel.importTrustedKey(result.first, result.second, result.third, isVerified = true)
                                    importStatusMessage = "Successfully imported peer key for: ${result.first}"
                                    isImportSuccess = true
                                    importInvitationText = ""
                                } else {
                                    importStatusMessage = "Error: Invalid invitation link format."
                                    isImportSuccess = false
                                }
                            } else if (text.startsWith("garlic-packet-invite:")) {
                                val code = importPinCode.ifBlank { "123456" }
                                val result = viewModel.decryptGarlicEncryptedInvite(text, code)
                                if (result != null) {
                                    viewModel.importTrustedKey(result.first, result.second, result.third, isVerified = true)
                                    importStatusMessage = "Decrypted & imported peer key: ${result.first}"
                                    isImportSuccess = true
                                    importInvitationText = ""
                                    importPinCode = ""
                                } else {
                                    importStatusMessage = "Decryption Failed: Incorrect PIN or tampered packet."
                                    isImportSuccess = false
                                }
                            } else {
                                importStatusMessage = "Error: Unrecognized invitation format."
                                isImportSuccess = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberBlue, contentColor = CyberBlack),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify & Decode Invite Packet", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Import status alert message
        if (importStatusMessage.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isImportSuccess == true) CyberGreen.copy(alpha = 0.15f) else CyberRed.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, if (isImportSuccess == true) CyberGreen else CyberRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isImportSuccess == true) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (isImportSuccess == true) CyberGreen else CyberRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            importStatusMessage,
                            color = if (isImportSuccess == true) CyberGreen else CyberRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { importStatusMessage = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss status", tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GarlicRoutingPathVisualizer(
    tunnelHops: Int,
    isConnected: Boolean,
    isConnecting: Boolean
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Linear Flow Path, 1: Force-Directed Topology
    var healthThreshold by remember { mutableStateOf(70f) } // Default 70% connection health threshold

    val infiniteTransition = rememberInfiniteTransition(label = "garlic_flow")
    
    // Animate the dash phase shift for connection lines
    val dashPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dash_phase"
    )

    // Animate packet/pulse progress along each tunnel segment
    val packetProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "packet_progress"
    )

    // Pulse size for glow animations
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Ticker state to fluctuate latencies and make simulation feel real & alive
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(isConnected, isConnecting) {
        if (isConnected) {
            while (true) {
                kotlinx.coroutines.delay(2000)
                tick++
            }
        }
    }

    // Build the nodes dynamically based on current hop settings and connection/tick states
    val nodes = remember(tunnelHops, tick, isConnected, isConnecting) {
        val random = java.util.Random(tick.toLong() + tunnelHops * 17)
        
        fun getLatency(base: Int): Int {
            if (!isConnected) return 0
            val jitter = random.nextInt(16) - 8 // +/- 8ms jitter
            return (base + jitter).coerceAtLeast(1)
        }
        
        fun getHealth(latency: Int): NodeHealth {
            if (!isConnected) return NodeHealth.OFFLINE
            return when {
                latency < 50 -> NodeHealth.EXCELLENT
                latency < 110 -> NodeHealth.GOOD
                latency < 180 -> NodeHealth.DEGRADED
                else -> NodeHealth.DEGRADED
            }
        }

        when (tunnelHops) {
            1 -> {
                val lat0 = 2 // Client always low latency
                val lat1 = getLatency(42)
                val lat2 = getLatency(94)
                listOf(
                    VisualNode("Client", "Src Portal", "client", lat0, NodeHealth.EXCELLENT),
                    VisualNode("Hop 1", "Tunnel Relay", "relay", lat1, getHealth(lat1)),
                    VisualNode("Eepsite", "Dest Server", "destination", lat2, getHealth(lat2))
                )
            }
            3 -> {
                val lat0 = 3
                val lat1 = getLatency(38)
                val lat2 = getLatency(85)
                val lat3 = getLatency(135)
                val lat4 = getLatency(198)
                listOf(
                    VisualNode("Client", "Src Portal", "client", lat0, NodeHealth.EXCELLENT),
                    VisualNode("I-Gate", "Inbound Gateway", "gate", lat1, getHealth(lat1)),
                    VisualNode("Transit", "Standard Relay", "relay", lat2, getHealth(lat2)),
                    VisualNode("O-Gate", "Outbound Gateway", "gate", lat3, getHealth(lat3)),
                    VisualNode("Eepsite", "Dest Server", "destination", lat4, getHealth(lat4))
                )
            }
            else -> {
                val lat0 = 4
                val lat1 = getLatency(32)
                val lat2 = getLatency(74)
                val lat3 = getLatency(115)
                val lat4 = getLatency(162)
                val lat5 = getLatency(225)
                listOf(
                    VisualNode("Client", "Src Portal", "client", lat0, NodeHealth.EXCELLENT),
                    VisualNode("I-Gate", "Inbound Gateway", "gate", lat1, getHealth(lat1)),
                    VisualNode("Transit A", "Relay Alpha", "relay", lat2, getHealth(lat2)),
                    VisualNode("Transit B", "Relay Beta", "relay", lat3, getHealth(lat3)),
                    VisualNode("O-Gate", "Outbound Gateway", "gate", lat4, getHealth(lat4)),
                    VisualNode("Eepsite", "Dest Server", "destination", lat5, getHealth(lat5))
                )
            }
        }
    }

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
                Column {
                    Text(
                        "LIVE GARLIC TUNNEL PATH SIMULATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        if (isConnected) "Garlic Packet Streams Active" else if (isConnecting) "Assembling Tunnel Layers..." else "Tunnel Inactive",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isConnected) CyberGreen else if (isConnecting) CyberOrange else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Indicators Legend
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF00E676), androidx.compose.foundation.shape.CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stable", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFFFF9100), androidx.compose.foundation.shape.CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Jitter", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Modern Cyber Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberBlack, RoundedCornerShape(6.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf("Linear Flow Path", "Force-Directed Topology")
                tabs.forEachIndexed { idx, title ->
                    val selected = selectedTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selected) CyberBlue.copy(alpha = 0.15f) else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                1.dp,
                                if (selected) CyberBlue else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { selectedTab = idx }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) CyberBlue else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Tab 0: Standard Linear Canvas visualization
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(CyberBlack, RoundedCornerShape(8.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val paddingX = 40.dp.toPx()
                        
                        val nodeCount = nodes.size
                        val xStep = (width - paddingX * 2) / (nodeCount - 1)
                        
                        val nodePositions = nodes.mapIndexed { index, node ->
                            val x = paddingX + index * xStep
                            val yOffset = if (index == 0 || index == nodeCount - 1) 0f else {
                                if (index % 2 == 1) -height * 0.2f else height * 0.2f
                            }
                            val y = (height / 2) + yOffset
                            Pair(node, Offset(x, y))
                        }

                        // 1. Draw connecting lines between nodes
                        for (i in 0 until nodePositions.size - 1) {
                            val start = nodePositions[i].second
                            val end = nodePositions[i + 1].second

                            // Draw background track line
                            drawLine(
                                color = CyberBorder,
                                start = start,
                                end = end,
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )

                            // Draw animated path flow if active or connecting
                            if (isConnected || isConnecting) {
                                val lineColor = if (isConnected) {
                                    // Transition from green to purple across the tunnel hops to show encryption to decryption transition
                                    val ratio = i.toFloat() / (nodePositions.size - 2)
                                    lerpColor(CyberGreen, CyberPurple, ratio)
                                } else {
                                    CyberOrange
                                }

                                // Flowing dashed line effect
                                val dashPathEffect = PathEffect.dashPathEffect(
                                    intervals = floatArrayOf(24f, 16f),
                                    phase = -dashPhase
                                )

                                drawLine(
                                    color = lineColor,
                                    start = start,
                                    end = end,
                                    strokeWidth = 3.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    pathEffect = dashPathEffect
                                )

                                // 2. Draw moving Garlic Clove packet pulses
                                val currentPos = start + (end - start) * packetProgress
                                drawCircle(
                                    color = lineColor,
                                    radius = 4.dp.toPx(),
                                    center = currentPos
                                )
                                // Packet outer glow
                                drawCircle(
                                    color = lineColor.copy(alpha = 0.3f),
                                    radius = 8.dp.toPx() * pulseScale,
                                    center = currentPos
                                )
                            }
                        }

                        // 3. Draw nodes
                        nodePositions.forEachIndexed { index, pair ->
                            val node = pair.first
                            val center = pair.second

                            val nodeColor = when {
                                !isConnected && !isConnecting -> CyberBorder
                                isConnecting -> CyberOrange
                                else -> {
                                    val ratio = index.toFloat() / (nodePositions.size - 1)
                                    lerpColor(CyberGreen, CyberPurple, ratio)
                                }
                            }

                            // Node Outer Glow
                            if (isConnected || isConnecting) {
                                drawCircle(
                                    color = nodeColor.copy(alpha = 0.2f),
                                    radius = 14.dp.toPx() * pulseScale,
                                    center = center
                                )
                            }

                            // Node Border Ring
                            drawCircle(
                                color = nodeColor,
                                radius = 8.dp.toPx(),
                                center = center,
                                style = Stroke(width = 2.dp.toPx())
                            )

                            // Node Solid Center
                            drawCircle(
                                color = if (isConnected || isConnecting) CyberBlack else CyberCardBg,
                                radius = 6.dp.toPx(),
                                center = center
                            )

                            drawCircle(
                                color = nodeColor,
                                radius = 3.dp.toPx(),
                                center = center
                            )

                            // Connection Status Dot Indicator on top-right of node
                            val badgeOffset = Offset(center.x + 8.dp.toPx(), center.y - 8.dp.toPx())
                            val statusDotColor = if (isConnected) {
                                node.health.color
                            } else if (isConnecting) {
                                if (index == 0) Color(0xFF00E676) else CyberOrange
                            } else {
                                if (index == 0) Color(0xFF00E676) else Color(0xFFFF1744)
                            }

                            // Status dot outer glow
                            drawCircle(
                                color = statusDotColor.copy(alpha = 0.4f),
                                radius = 5.dp.toPx(),
                                center = badgeOffset
                            )

                            // Status dot solid center
                            drawCircle(
                                color = statusDotColor,
                                radius = 3.dp.toPx(),
                                center = badgeOffset
                            )

                            // Status dot crisp border line
                            drawCircle(
                                color = CyberBlack,
                                radius = 3.dp.toPx(),
                                center = badgeOffset,
                                style = Stroke(width = 0.8.dp.toPx())
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dynamic Path Description Text/Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    nodes.forEachIndexed { index, node ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                node.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) {
                                    val ratio = index.toFloat() / (nodes.size - 1)
                                    lerpColor(CyberGreen, CyberPurple, ratio)
                                } else {
                                    TextPrimary
                                },
                                textAlign = TextAlign.Center
                            )
                            Text(
                                node.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            
                            // Added Latency & Connection Health indicator badges below descriptions
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                val dotColor = if (isConnected) {
                                    node.health.color
                                } else if (isConnecting) {
                                    if (index == 0) Color(0xFF00E676) else CyberOrange
                                } else {
                                    if (index == 0) Color(0xFF00E676) else Color(0xFFFF1744)
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(dotColor, androidx.compose.foundation.shape.CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val latencyText = when {
                                    !isConnected && !isConnecting -> if (index == 0) "2ms" else "offline"
                                    isConnecting -> if (index == 0) "2ms" else "ping..."
                                    else -> "${node.latencyMs}ms"
                                }
                                Text(
                                    text = latencyText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isConnected) TextPrimary else TextSecondary,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            } else {
                // Tab 1: Interactive Force-Directed Network Graph
                ForceDirectedCircuitGraph(
                    nodes = nodes,
                    isConnected = isConnected,
                    isConnecting = isConnecting,
                    healthThreshold = healthThreshold,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Connection Health Highlight Threshold Slider
                ThresholdSlider(
                    thresholdValue = healthThreshold,
                    onValueChange = { healthThreshold = it }
                )
            }

            if (isConnected) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = CyberBorder)
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    "Path Cryptography: Client encrypts message inside nested ElGamal layers. Each intermediary hop decrypts its specific instruction layer (using Session Tags) to locate the next hop without learning the payload content or original source.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

// Interactive Force-Directed Node State Model
class ForceNodeState(
    val id: String,
    val label: String,
    val type: String, // "client", "relay", "gate", "destination", "decoy"
    val healthScore: Int, // 0 to 100
    val latencyMs: Int,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var isDragging: Boolean = false
)

data class ForceEdge(
    val fromId: String,
    val toId: String,
    val isMainCircuit: Boolean
)

@Composable
fun ForceDirectedCircuitGraph(
    nodes: List<VisualNode>,
    isConnected: Boolean,
    isConnecting: Boolean,
    healthThreshold: Float,
    modifier: Modifier = Modifier
) {
    // Construct dynamic force-directed node states matching current active garlic path
    val forceNodes = remember(nodes) {
        val list = mutableListOf<ForceNodeState>()
        
        // 1. Map main routing path nodes
        nodes.forEachIndexed { idx, node ->
            val angle = (idx.toFloat() / nodes.size) * Math.PI
            val r = 100f
            val startX = 300f + (r * cos(angle)).toFloat()
            val startY = 130f + (r * sin(angle)).toFloat()
            
            val hScore = when (node.health) {
                NodeHealth.EXCELLENT -> 98
                NodeHealth.GOOD -> 85
                NodeHealth.DEGRADED -> 60
                NodeHealth.OFFLINE -> 0
            }
            
            list.add(
                ForceNodeState(
                    id = "main_$idx",
                    label = node.name,
                    type = node.type,
                    healthScore = hScore,
                    latencyMs = node.latencyMs,
                    x = startX,
                    y = startY
                )
            )
        }
        
        // 2. Add 3 decoy global network nodes to represent the background decentralized mesh
        val decoyData = listOf(
            Triple("Decoy-Alpha", 88, 92),
            Triple("Decoy-Beta", 52, 175),
            Triple("Decoy-Gamma", 94, 34)
        )
        
        decoyData.forEachIndexed { idx, data ->
            val angle = (idx.toFloat() / decoyData.size) * 2 * Math.PI + Math.PI / 4
            val r = 160f
            val startX = 300f + (r * cos(angle)).toFloat()
            val startY = 130f + (r * sin(angle)).toFloat()
            
            list.add(
                ForceNodeState(
                    id = "decoy_$idx",
                    label = data.first,
                    type = "decoy",
                    healthScore = data.second,
                    latencyMs = data.third,
                    x = startX,
                    y = startY
                )
            )
        }
        
        list
    }

    // Build spring-connection edges
    val forceEdges = remember(nodes) {
        val edges = mutableListOf<ForceEdge>()
        
        // Main path chain
        for (i in 0 until nodes.size - 1) {
            edges.add(ForceEdge("main_$i", "main_${i + 1}", isMainCircuit = true))
        }
        
        // Decoy network background link meshes
        edges.add(ForceEdge("decoy_0", "main_0", isMainCircuit = false))
        if (nodes.size > 2) {
            edges.add(ForceEdge("decoy_1", "main_2", isMainCircuit = false))
        } else {
            edges.add(ForceEdge("decoy_1", "main_1", isMainCircuit = false))
        }
        edges.add(ForceEdge("decoy_2", "decoy_0", isMainCircuit = false))
        edges.add(ForceEdge("decoy_2", "main_${nodes.size - 1}", isMainCircuit = false))
        
        edges
    }

    var triggerUpdate by remember { mutableStateOf(0) }
    var canvasSize by remember { mutableStateOf(IntSize(600, 300)) }

    // Spring physics loop running natively at ~60fps
    LaunchedEffect(forceNodes, canvasSize) {
        val kAttractive = 0.05f // spring stiffness
        val kRepulsive = 32000f // repulsion strength
        val kGravity = 0.02f // pull strength to center
        val dRate = 0.83f // damping velocity friction
        val restLengthMain = 100f
        val restLengthDecoy = 130f
        
        while (true) {
            val cx = canvasSize.width / 2f
            val cy = canvasSize.height / 2f
            
            // 1. Node Repulsion
            for (i in forceNodes.indices) {
                val ni = forceNodes[i]
                if (ni.isDragging) continue
                
                var fx = 0f
                var fy = 0f
                
                for (j in forceNodes.indices) {
                    if (i == j) continue
                    val nj = forceNodes[j]
                    val dx = ni.x - nj.x
                    val dy = ni.y - nj.y
                    val distSq = dx * dx + dy * dy + 1f
                    val dist = sqrt(distSq)
                    
                    if (dist < 320f) {
                        val force = kRepulsive / distSq
                        fx += (dx / dist) * force
                        fy += (dy / dist) * force
                    }
                }
                
                // 2. Link attraction
                forceEdges.forEach { edge ->
                    if (edge.fromId == ni.id || edge.toId == ni.id) {
                        val otherId = if (edge.fromId == ni.id) edge.toId else edge.fromId
                        val otherNode = forceNodes.find { it.id == otherId }
                        if (otherNode != null) {
                            val dx = ni.x - otherNode.x
                            val dy = ni.y - otherNode.y
                            val dist = sqrt(dx * dx + dy * dy + 1f)
                            val restL = if (edge.isMainCircuit) restLengthMain else restLengthDecoy
                            val force = -kAttractive * (dist - restL)
                            fx += (dx / dist) * force
                            fy += (dy / dist) * force
                        }
                    }
                }
                
                // 3. Central gravity
                val dcx = ni.x - cx
                val dcy = ni.y - cy
                fx -= dcx * kGravity
                fy -= dcy * kGravity
                
                // Apply final updates
                ni.vx = (ni.vx + fx) * dRate
                ni.vy = (ni.vy + fy) * dRate
                
                ni.x += ni.vx
                ni.y += ni.vy
                
                // Keep strictly inside Canvas boundaries
                ni.x = ni.x.coerceIn(25f, canvasSize.width.toFloat() - 25f)
                ni.y = ni.y.coerceIn(25f, canvasSize.height.toFloat() - 25f)
            }
            
            triggerUpdate++
            kotlinx.coroutines.delay(16)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(CyberBlack, RoundedCornerShape(8.dp))
            .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
            .pointerInput(forceNodes, canvasSize) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val tapRadius = 32.dp.toPx()
                        val tapped = forceNodes.find {
                            val dx = it.x - offset.x
                            val dy = it.y - offset.y
                            dx * dx + dy * dy < tapRadius * tapRadius
                        }
                        if (tapped != null) {
                            tapped.isDragging = true
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val active = forceNodes.find { it.isDragging }
                        if (active != null) {
                            active.x += dragAmount.x
                            active.y += dragAmount.y
                            active.x = active.x.coerceIn(25f, canvasSize.width.toFloat() - 25f)
                            active.y = active.y.coerceIn(25f, canvasSize.height.toFloat() - 25f)
                        }
                    },
                    onDragEnd = {
                        forceNodes.forEach { it.isDragging = false }
                    },
                    onDragCancel = {
                        forceNodes.forEach { it.isDragging = false }
                    }
                )
            }
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "force_pulse")
        val dashPhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 100f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "dash_phase"
        )
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )

        val tickCount = triggerUpdate

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    if (size.width > 0 && size.height > 0) {
                        canvasSize = size
                    }
                }
        ) {
            val _t = tickCount // Triggers Canvas redraw on physics ticks
            
            // 1. Draw connections (links)
            forceEdges.forEach { edge ->
                val fromNode = forceNodes.find { it.id == edge.fromId }
                val toNode = forceNodes.find { it.id == edge.toId }
                
                if (fromNode != null && toNode != null) {
                    val start = Offset(fromNode.x, fromNode.y)
                    val end = Offset(toNode.x, toNode.y)
                    
                    // Background link line
                    drawLine(
                        color = if (edge.isMainCircuit) CyberBorder.copy(alpha = 0.6f) else CyberBorder.copy(alpha = 0.25f),
                        start = start,
                        end = end,
                        strokeWidth = if (edge.isMainCircuit) 2.dp.toPx() else 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    
                    // Draw animated Garlic Layer streams if circuit is active
                    if (edge.isMainCircuit && (isConnected || isConnecting)) {
                        val isFromHealthy = fromNode.healthScore >= healthThreshold
                        val isToHealthy = toNode.healthScore >= healthThreshold
                        val lineHighlightColor = if (!isFromHealthy || !isToHealthy) {
                            CyberRed // RED Alert status for unhealthy links
                        } else if (isConnected) {
                            CyberGreen // Stable Encryption Green
                        } else {
                            CyberOrange // Assembling
                        }

                        val dashPathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(20f, 15f),
                            phase = -dashPhase
                        )

                        drawLine(
                            color = lineHighlightColor,
                            start = start,
                            end = end,
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = dashPathEffect
                        )
                    }
                }
            }
            
            // 2. Draw nodes & interactive visual details
            forceNodes.forEach { fNode ->
                val center = Offset(fNode.x, fNode.y)
                val isBelowThreshold = fNode.healthScore < healthThreshold
                
                // Color mapping: solid red if health drops below specific threshold
                val baseNodeColor = if (isBelowThreshold) {
                    CyberRed
                } else {
                    when (fNode.type) {
                        "client" -> CyberGreen
                        "destination" -> CyberPurple
                        "decoy" -> CyberBlue.copy(alpha = 0.7f)
                        else -> CyberBlue
                    }
                }
                
                // Outer halo glow
                drawCircle(
                    color = baseNodeColor.copy(alpha = if (isBelowThreshold) 0.35f else 0.18f),
                    radius = (14.dp.toPx()) * (if (isBelowThreshold) pulseScale * 1.2f else pulseScale),
                    center = center
                )
                
                // Warning Pulse Ring for Unhealthy/Degraded nodes
                if (isBelowThreshold) {
                    drawCircle(
                        color = CyberRed,
                        radius = 18.dp.toPx() * pulseScale,
                        center = center,
                        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                    )
                }

                // Node border circle outline
                drawCircle(
                    color = baseNodeColor,
                    radius = 9.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
                
                // Black core center
                drawCircle(
                    color = CyberBlack,
                    radius = 7.dp.toPx(),
                    center = center
                )
                
                // Small solid core dot
                drawCircle(
                    color = baseNodeColor,
                    radius = 4.dp.toPx(),
                    center = center
                )

                // Render Labels and Health percentages
                drawContext.canvas.nativeCanvas.apply {
                    val textPaint = android.graphics.Paint().apply {
                        color = if (isBelowThreshold) android.graphics.Color.RED else android.graphics.Color.WHITE
                        textSize = 9.dp.toPx()
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.MONOSPACE
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    val subPaint = android.graphics.Paint().apply {
                        color = if (isBelowThreshold) android.graphics.Color.argb(220, 255, 70, 70) else android.graphics.Color.GRAY
                        textSize = 7.5.dp.toPx()
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.MONOSPACE
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    
                    // Node Name Label
                    drawText(
                        fNode.label,
                        fNode.x,
                        fNode.y - 14.dp.toPx(),
                        textPaint
                    )
                    
                    // Node HP & Latency Description
                    val hpLabel = if (isBelowThreshold) "HP: ${fNode.healthScore}% [CRIT]" else "${fNode.healthScore}% HP | ${fNode.latencyMs}ms"
                    drawText(
                        hpLabel,
                        fNode.x,
                        fNode.y + 23.dp.toPx(),
                        subPaint
                    )
                }
            }
        }
        
        // Dynamic bottom-right helper overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .background(CyberBlack.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                .border(0.5.dp, CyberBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                "Drag nodes to manipulate layout structure",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun ThresholdSlider(
    thresholdValue: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Alert Threshold Indicator",
                    tint = CyberRed,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "BOTTLE-NECK HIGHLIGHT THRESHOLD: ${thresholdValue.toInt()}% HP",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberRed,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
            Text(
                text = "Flags nodes in RED below this HP",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 8.sp
            )
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Slider(
            value = thresholdValue,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = CyberRed,
                activeTrackColor = CyberRed,
                inactiveTrackColor = CyberBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .testTag("health_threshold_slider")
        )
    }
}

// Utility function to interpolate colors
fun lerpColor(start: Color, stop: Color, fraction: Float): Color {
    return Color(
        red = start.red + (stop.red - start.red) * fraction,
        green = start.green + (stop.green - start.green) * fraction,
        blue = start.blue + (stop.blue - start.blue) * fraction,
        alpha = start.alpha + (stop.alpha - start.alpha) * fraction
    )
}

enum class NodeHealth(val label: String, val color: Color) {
    EXCELLENT("Excellent", Color(0xFF00E676)),
    GOOD("Good", Color(0xFF00B0FF)),
    DEGRADED("Degraded", Color(0xFFFF9100)),
    OFFLINE("Offline", Color(0xFFFF1744))
}

data class VisualNode(
    val name: String,
    val description: String,
    val type: String,
    val latencyMs: Int,
    val health: NodeHealth
)

@Composable
fun PeerDiscoverySection(
    viewModel: I2PViewModel,
    modifier: Modifier = Modifier
) {
    val peers by viewModel.discoveredPeers.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()

    Card(
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, CyberBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = "Global Peer Map",
                        tint = CyberBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "GLOBAL PEER DISCOVERY",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isDiscovering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = CyberBlue
                    )
                } else {
                    IconButton(
                        onClick = { viewModel.discoverPeers() },
                        modifier = Modifier.size(24.dp).testTag("refresh_peers_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Peer List",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Simulated view of cryptographically verified active global routing nodes registered in local NetDB database.",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Discovered Peers List
            if (peers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(CyberCardBg, RoundedCornerShape(8.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = "No Peers",
                            tint = TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "NetDB peer list is currently unseeded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    peers.forEach { peer ->
                        PeerRowItem(peer = peer)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Trigger Button
            Button(
                onClick = { viewModel.discoverPeers() },
                enabled = !isDiscovering,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberBlue.copy(alpha = 0.15f),
                    contentColor = CyberBlue,
                    disabledContainerColor = CyberBorder.copy(alpha = 0.2f),
                    disabledContentColor = TextSecondary
                ),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, if (isDiscovering) CyberBorder else CyberBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("scan_netdb_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isDiscovering) Icons.Default.Sync else Icons.Default.Search,
                        contentDescription = "Scan icon",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDiscovering) "RESOLVING GLOBAL PEER ENVELOPS..." else "SCAN GLOBAL NETWORK DATABASE (NETDB)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PeerRowItem(peer: DiscoveredPeer) {
    val statusColor = when (peer.status) {
        PeerStatus.ACTIVE -> CyberGreen
        PeerStatus.STABLE -> CyberBlue
        PeerStatus.DEGRADED -> CyberOrange
    }

    val healthBg = statusColor.copy(alpha = 0.1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberCardBg, RoundedCornerShape(6.dp))
            .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Column: Node identity, country region
        Column(modifier = Modifier.weight(1.3f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${peer.flagEmoji} ",
                    fontSize = 14.sp
                )
                Text(
                    text = peer.routerId,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                if (peer.isVerified) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Cryptographically Verified",
                        tint = CyberGreen,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = peer.i2pAddress.take(8) + "..." + peer.i2pAddress.takeLast(7),
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "• ${peer.region}",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        // Right Column: Latency and Health score pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.weight(0.7f)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${peer.latencyMs}ms",
                    color = CyberBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Latency",
                    color = TextSecondary,
                    fontSize = 9.sp
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                color = healthBg,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${peer.healthScore}%",
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "HEALTH",
                        color = statusColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 7.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

enum class VpnVpsSubTab(val label: String) {
    VPN("SECURE VPN"),
    VPS("VPS / HOME NODE")
}

@Composable
fun VpnVpsScreen(
    viewModel: I2PViewModel,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf(VpnVpsSubTab.VPN) }
    val vpnState by viewModel.vpnState.collectAsState()
    val vpsState by viewModel.vpsState.collectAsState()
    
    // Dialog states for VPS profile creation
    var showAddVpsDialog by remember { mutableStateOf(false) }
    var vpsNameInput by remember { mutableStateOf("") }
    var vpsIpInput by remember { mutableStateOf("") }
    var vpsUserInput by remember { mutableStateOf("") }
    var vpsPortInput by remember { mutableStateOf("22") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        // Tab header
        TabRow(
            selectedTabIndex = activeSubTab.ordinal,
            containerColor = CyberDarkSurface,
            contentColor = CyberGreen,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeSubTab.ordinal]),
                    color = CyberGreen
                )
            }
        ) {
            VpnVpsSubTab.values().forEach { tab ->
                val isSelected = activeSubTab == tab
                Tab(
                    selected = isSelected,
                    onClick = { activeSubTab = tab },
                    text = {
                        Text(
                            tab.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            letterSpacing = 1.sp
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (activeSubTab == VpnVpsSubTab.VPN) {
                // VPN Tab
                item {
                    Text(
                        text = "VIRTUAL PRIVATE NETWORKS",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberGreen,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Active connection status card
                item {
                    val statusColor = when (vpnState.status) {
                        VpnStatus.CONNECTED -> CyberGreen
                        VpnStatus.CONNECTING -> CyberOrange
                        VpnStatus.DISCONNECTED -> CyberRed
                    }
                    val statusLabel = when (vpnState.status) {
                        VpnStatus.CONNECTED -> "SECURE END-TO-END ESCROW ACTIVE"
                        VpnStatus.CONNECTING -> "ESTABLISHING HANDSHAKE..."
                        VpnStatus.DISCONNECTED -> "ROUTING UNPROTECTED BY VPN"
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                        border = BorderStroke(1.dp, if (vpnState.status == VpnStatus.CONNECTED) CyberGreen else CyberBorder),
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
                                            .background(statusColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = vpnState.status.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = statusColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Icon(
                                    imageVector = if (vpnState.status == VpnStatus.CONNECTED) Icons.Default.Security else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            
                            if (vpnState.status == VpnStatus.CONNECTED) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = CyberBorder.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Transmitted", fontSize = 10.sp, color = TextSecondary)
                                        Text(
                                            String.format(java.util.Locale.US, "%.2f MB", vpnState.bytesTransmitted / (1024f * 1024f)),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Column {
                                        Text("Uptime", fontSize = 10.sp, color = TextSecondary)
                                        Text(
                                            String.format(java.util.Locale.US, "%02d:%02d", vpnState.connectedDurationSeconds / 60, vpnState.connectedDurationSeconds % 60),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Column {
                                        Text("Interface", fontSize = 10.sp, color = TextSecondary)
                                        Text(
                                            "tun0 (IKEv2)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = CyberBlue,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Connect / Disconnect button
                item {
                    val isConnected = vpnState.status == VpnStatus.CONNECTED
                    val isConnecting = vpnState.status == VpnStatus.CONNECTING
                    Button(
                        onClick = { viewModel.toggleVpn() },
                        enabled = !isConnecting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) CyberRed else CyberGreen,
                            contentColor = CyberBlack,
                            disabledContainerColor = CyberBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("vpn_toggle_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isConnecting) "NEGOTIATING KEYS..." else if (isConnected) "TERMINATE VPN TUNNEL" else "INITIATE SECURE VPN",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // VPN Configuration selector
                item {
                    Text(
                        text = "AVAILABLE SECURE ENDPOINTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                items(vpnState.availableVpns) { config ->
                    val isSelected = vpnState.selectedVpn == config.name
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) CyberGreen.copy(alpha = 0.05f) else CyberDarkSurface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) CyberGreen else CyberBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = vpnState.status == VpnStatus.DISCONNECTED) {
                                viewModel.selectVpn(config.name)
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { if (vpnState.status == VpnStatus.DISCONNECTED) viewModel.selectVpn(config.name) },
                                        enabled = vpnState.status == VpnStatus.DISCONNECTED,
                                        colors = RadioButtonDefaults.colors(selectedColor = CyberGreen)
                                    )
                                    Column {
                                        Text(
                                            text = config.name,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = config.speedRating,
                                                fontSize = 9.sp,
                                                color = CyberBlue,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Box(modifier = Modifier.size(3.dp).background(TextSecondary, CircleShape))
                                            Text(
                                                text = "Ping: ${config.pingMs}ms",
                                                fontSize = 9.sp,
                                                color = CyberOrange,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                                
                                if (config.recommended) {
                                    Box(
                                        modifier = Modifier
                                            .background(CyberGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .border(0.5.dp, CyberGreen, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "RECOMMENDED",
                                            fontSize = 7.sp,
                                            color = CyberGreen,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(10.dp))
                                Text(
                                    text = "Encryption: ${config.securityLevel}",
                                    fontSize = 9.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // Help/Recommendation Section
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                        border = BorderStroke(1.dp, CyberBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AISP PROXY GUIDELINES", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "While darkweb garlic networks encrypt routing layers between network peers, they do not mask your ISP's trace of initial gateway connections. We highly recommend utilizing Private VPN (ShadowTunnel) as it encapsulates your garlic packages inside quantum-resistant metadata packets, hiding network topology entirely from external eavesdroppers.",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

            } else {
                // VPS / Home Portal Tab
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SSH ROUTING PROXIES",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberGreen,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Button(
                            onClick = { showAddVpsDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberBlue.copy(alpha = 0.15f), contentColor = CyberBlue),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, CyberBlue),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ADD VPS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Remote status monitor card (Visible when connected)
                item {
                    val statusColor = when (vpsState.status) {
                        VpsStatus.CONNECTED -> CyberBlue
                        VpsStatus.CONNECTING -> CyberOrange
                        VpsStatus.DISCONNECTED -> TextSecondary
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                        border = BorderStroke(1.dp, if (vpsState.status == VpsStatus.CONNECTED) CyberBlue else CyberBorder),
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
                                        modifier = Modifier.size(8.dp).background(statusColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "PORTAL: ${vpsState.status.name}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = statusColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            vpsState.activeProfile?.let { profile ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Connected gateway: ${profile.name} (${profile.ipAddress})",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }

                            if (vpsState.status == VpsStatus.CONNECTED) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Divider(color = CyberBorder.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(14.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // CPU Indicator
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("VPS CPU", fontSize = 9.sp, color = TextSecondary)
                                            Text("${vpsState.cpuUsagePercent}%", fontSize = 9.sp, color = CyberBlue, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = vpsState.cpuUsagePercent / 100f,
                                            modifier = Modifier.fillMaxWidth().height(4.dp),
                                            color = CyberBlue,
                                            trackColor = CyberBorder
                                        )
                                    }
                                    
                                    // RAM Indicator
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("VPS RAM", fontSize = 9.sp, color = TextSecondary)
                                            Text("${vpsState.ramUsagePercent}%", fontSize = 9.sp, color = CyberOrange, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = vpsState.ramUsagePercent / 100f,
                                            modifier = Modifier.fillMaxWidth().height(4.dp),
                                            color = CyberOrange,
                                            trackColor = CyberBorder
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Gateway Proxy Speed", fontSize = 10.sp, color = TextSecondary)
                                    Text(
                                        String.format(java.util.Locale.US, "%.1f Mbps", vpsState.bandwidthUsageMbps),
                                        fontSize = 11.sp,
                                        color = CyberGreen,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Connected VPS control button
                if (vpsState.status != VpsStatus.DISCONNECTED) {
                    item {
                        Button(
                            onClick = { viewModel.disconnectVps() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberRed, contentColor = CyberBlack),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text("DISCONNECT PROXY ROUTER", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                    }
                }

                // List of profiles
                item {
                    Text(
                        text = "SAVED GATEWAY PROFILES",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                if (vpsState.savedProfiles.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberDarkSurface, RoundedCornerShape(6.dp))
                                .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No VPS profiles added. Tap ADD VPS above to register.", fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center)
                        }
                    }
                }

                items(vpsState.savedProfiles) { profile ->
                    val isConnectedToThis = vpsState.activeProfile == profile
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                        border = BorderStroke(1.dp, if (isConnectedToThis) CyberBlue else CyberBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Dns,
                                        contentDescription = null,
                                        tint = if (isConnectedToThis) CyberBlue else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(profile.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            if (profile.isDefault) {
                                                Text(
                                                    "DEFAULT",
                                                    fontSize = 7.sp,
                                                    color = CyberGreen,
                                                    modifier = Modifier
                                                        .background(CyberGreen.copy(alpha = 0.12f), RoundedCornerShape(2.dp))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            "${profile.username}@${profile.ipAddress}:${profile.port}",
                                            fontSize = 10.sp,
                                            color = TextSecondary,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (vpsState.status == VpsStatus.DISCONNECTED) {
                                        Button(
                                            onClick = { viewModel.connectVps(profile) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberBlue, contentColor = CyberBlack),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("CONNECT", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeVpsProfile(profile) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete VPS Profile", tint = CyberRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add VPS Profile Dialog
    if (showAddVpsDialog) {
        AlertDialog(
            onDismissRequest = { showAddVpsDialog = false },
            title = { Text("Add Remote VPS Gateway", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = vpsNameInput,
                        onValueChange = { vpsNameInput = it },
                        label = { Text("Profile Name", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        )
                    )
                    OutlinedTextField(
                        value = vpsIpInput,
                        onValueChange = { vpsIpInput = it },
                        label = { Text("IP Address / Hostname", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        )
                    )
                    OutlinedTextField(
                        value = vpsUserInput,
                        onValueChange = { vpsUserInput = it },
                        label = { Text("Username", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        )
                    )
                    OutlinedTextField(
                        value = vpsPortInput,
                        onValueChange = { vpsPortInput = it },
                        label = { Text("SSH Port", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (vpsNameInput.isNotEmpty() && vpsIpInput.isNotEmpty() && vpsUserInput.isNotEmpty()) {
                            val port = vpsPortInput.toIntOrNull() ?: 22
                            viewModel.addVpsProfile(vpsNameInput, vpsIpInput, vpsUserInput, port)
                        }
                        showAddVpsDialog = false
                        vpsNameInput = ""
                        vpsIpInput = ""
                        vpsUserInput = ""
                        vpsPortInput = "22"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberBlue, contentColor = CyberBlack)
                ) {
                    Text("Save Node")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVpsDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CyberDarkSurface
        )
    }
}
