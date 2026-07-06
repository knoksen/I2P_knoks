package no.knoksen.i2pbrowser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import kotlinx.coroutines.launch
import no.knoksen.i2pbrowser.ui.*
import no.knoksen.i2pbrowser.ui.theme.*
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource

enum class AppTab(@StringRes val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ROUTER(R.string.tab_router, Icons.Default.Dns),
    BROWSER(R.string.tab_browser, Icons.Default.Language),
    VPN_VPS(R.string.tab_vpn_vps, Icons.Default.VpnLock),
    COMMS(R.string.tab_comms, Icons.Default.Forum),
    IDENTITY(R.string.tab_identity, Icons.Default.Fingerprint)
}

fun visibleAppTabs(
    mode: AppExperienceMode,
    hasRealSamIdentity: Boolean
): List<AppTab> {
    return when (mode) {
        AppExperienceMode.RELEASE_REAL -> buildList {
            add(AppTab.ROUTER)
            add(AppTab.BROWSER)
            if (hasRealSamIdentity) add(AppTab.IDENTITY)
        }
        AppExperienceMode.LAB_SIMULATION -> AppTab.values().toList()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: I2PViewModel = viewModel()
                val coroutineScope = rememberCoroutineScope()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                var currentTab by remember { mutableStateOf(AppTab.ROUTER) }
                val routerState by viewModel.routerState.collectAsState()
                val appMode by viewModel.appExperienceMode.collectAsState()
                val visibleTabs = visibleAppTabs(
                    mode = appMode,
                    hasRealSamIdentity = routerState.isRealI2p && routerState.realDestination.isNotBlank()
                )
                var showProxySettingsDialog by remember { mutableStateOf(false) }

                LaunchedEffect(visibleTabs, currentTab) {
                    if (currentTab !in visibleTabs) {
                        currentTab = AppTab.ROUTER
                    }
                }

                LaunchedEffect(viewModel) {
                    viewModel.activeTabFlow.collect { tabName ->
                        try {
                            currentTab = AppTab.valueOf(tabName)
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DrawerContent(
                            viewModel = viewModel,
                            onClose = {
                                coroutineScope.launch { drawerState.close() }
                            }
                        )
                    }
                ) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CyberBlack)
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                        topBar = {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CyberDarkSurface)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Security,
                                            contentDescription = stringResource(R.string.cd_connection_status),
                                            tint = CyberGreen,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                stringResource(R.string.app_title),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 18.sp,
                                                color = TextPrimary,
                                                letterSpacing = 1.5.sp
                                            )
                                            Text(
                                                stringResource(R.string.router_status_subtitle),
                                                fontSize = 9.sp,
                                                color = CyberGreen,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = when {
                                                    routerState.isRealI2p -> CyberGreen.copy(alpha = 0.15f)
                                                    routerState.isConnected -> CyberOrange.copy(alpha = 0.15f)
                                                    else -> CyberRed.copy(alpha = 0.15f)
                                                }
                                            ),
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(
                                                            if (routerState.isRealI2p) CyberGreen else if (routerState.isConnected) CyberOrange else CyberRed,
                                                            shape = MaterialTheme.shapes.small
                                                        )
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    if (routerState.isRealI2p) stringResource(R.string.status_sam_ready) else if (routerState.isConnected) stringResource(R.string.status_simulation) else stringResource(R.string.status_offline),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (routerState.isRealI2p) CyberGreen else if (routerState.isConnected) CyberOrange else CyberRed,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { showProxySettingsDialog = true },
                                            modifier = Modifier.size(36.dp).testTag("network_settings_button")
                                        ) {
                                            Icon(
                                                Icons.Default.Settings,
                                                contentDescription = stringResource(R.string.cd_configure_proxy),
                                                tint = CyberBlue,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                                }
                                            },
                                            modifier = Modifier.size(36.dp).testTag("sidebar_toggle_button")
                                        ) {
                                            Icon(
                                                Icons.Default.History,
                                                contentDescription = stringResource(R.string.cd_view_history),
                                                tint = CyberBlue,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                                Divider(color = CyberBorder)
                                if (appMode == AppExperienceMode.LAB_SIMULATION) {
                                    GlobalVpnStatusBar(viewModel = viewModel, onVpnSettingsClick = { currentTab = AppTab.VPN_VPS })
                                }
                            }
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = CyberDarkSurface,
                                tonalElevation = 8.dp
                            ) {
                                visibleTabs.forEach { tab ->
                                    val isSelected = currentTab == tab
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { currentTab = tab },
                                        icon = {
                                            Icon(
                                                tab.icon,
                                                contentDescription = stringResource(tab.labelRes),
                                                tint = if (isSelected) CyberGreen else TextSecondary
                                            )
                                        },
                                        label = {
                                            Text(
                                                stringResource(tab.labelRes),
                                                color = if (isSelected) CyberGreen else TextSecondary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = CyberGreen.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            when (currentTab) {
                                AppTab.ROUTER -> RouterScreen(viewModel = viewModel)
                                AppTab.BROWSER -> BrowserScreen(viewModel = viewModel)
                                AppTab.VPN_VPS -> VpnVpsScreen(viewModel = viewModel)
                                AppTab.COMMS -> CommunicationsScreen(viewModel = viewModel)
                                AppTab.IDENTITY -> IdentityScreen(viewModel = viewModel)
                            }
                        }
                    }

                    if (showProxySettingsDialog) {
                        ProxySettingsDialog(
                            viewModel = viewModel,
                            onDismiss = { showProxySettingsDialog = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerContent(
    viewModel: I2PViewModel,
    onClose: () -> Unit
) {
    val accessedNodes by viewModel.accessedNodesHistory.collectAsState()
    val appMode by viewModel.appExperienceMode.collectAsState()
    
    ModalDrawerSheet(
        drawerContainerColor = CyberBlack,
        drawerContentColor = TextPrimary,
        modifier = Modifier.width(320.dp).fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, CyberBorder)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        stringResource(R.string.drawer_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = CyberBlue,
                        letterSpacing = 1.sp
                    )
                    Text(
                        stringResource(R.string.drawer_subtitle),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_close_sidebar),
                        tint = TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = CyberBorder)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (appMode == AppExperienceMode.LAB_SIMULATION) {
                    Button(
                        onClick = { viewModel.simulateRandomNodeAccess() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberBlue.copy(alpha = 0.2f)),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, CyberBlue),
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.btn_simulate_node), tint = CyberBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_simulate_node), fontSize = 11.sp, color = CyberBlue, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { viewModel.clearAccessedNodesHistory() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberRed.copy(alpha = 0.15f)),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, CyberRed.copy(alpha = 0.6f)),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_clear_history), tint = CyberRed, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_clear_logs), fontSize = 11.sp, color = CyberRed, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.history_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.history_entries_count, accessedNodes.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Nodes Log list
            if (accessedNodes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(CyberDarkSurface, RoundedCornerShape(4.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = stringResource(R.string.cd_no_records),
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.no_history_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(accessedNodes.asReversed()) { node ->
                        NodeHistoryItem(node = node)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = CyberBorder)
            Spacer(modifier = Modifier.height(12.dp))

            // Footer metadata
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(CyberGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.footer_log_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.footer_log_description),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun NodeHistoryItem(node: AccessedNode) {
    val formatter = remember {
        java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    }
    val timeString = remember(node.timestamp) {
        formatter.format(java.util.Date(node.timestamp))
    }

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
                    text = node.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = timeString,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = node.url,
                fontSize = 10.sp,
                color = CyberBlue,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val isRealI2p = node.connectionStatus == "REAL_I2P"
                val statusColor = if (isRealI2p) CyberGreen else CyberOrange
                val statusText = if (isRealI2p) stringResource(R.string.status_real_i2p) else stringResource(R.string.status_simulated_preview)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = statusText,
                        fontSize = 9.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Quick lock/unlocked icon indicator
                Icon(
                    imageVector = if (isRealI2p) Icons.Default.Lock else Icons.Default.Visibility,
                    contentDescription = stringResource(R.string.cd_routing_status),
                    tint = statusColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun GlobalVpnStatusBar(
    viewModel: I2PViewModel,
    onVpnSettingsClick: () -> Unit
) {
    val vpnState by viewModel.vpnState.collectAsState()
    
    val statusColor = when (vpnState.status) {
        VpnStatus.CONNECTED -> CyberGreen
        VpnStatus.CONNECTING -> CyberOrange
        VpnStatus.DISCONNECTED -> TextSecondary
    }
    
    val statusText = when (vpnState.status) {
        VpnStatus.CONNECTED -> stringResource(R.string.vpn_status_connected, vpnState.selectedVpn)
        VpnStatus.CONNECTING -> stringResource(R.string.vpn_status_connecting)
        VpnStatus.DISCONNECTED -> stringResource(R.string.vpn_status_idle)
    }
    
    val statsText = if (vpnState.status == VpnStatus.CONNECTED) {
        stringResource(R.string.vpn_stats, 
            vpnState.bytesTransmitted / (1024f * 1024f), 
            vpnState.connectedDurationSeconds / 60, 
            vpnState.connectedDurationSeconds % 60)
    } else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberBlack)
            .clickable { onVpnSettingsClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VpnLock,
                contentDescription = stringResource(R.string.cd_vpn_status_indicator),
                tint = statusColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = stringResource(R.string.vpn_status_label),
                fontSize = 10.sp,
                color = TextSecondary,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = statusText + statsText,
                fontSize = 10.sp,
                color = statusColor,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (vpnState.status == VpnStatus.CONNECTED) stringResource(R.string.vpn_active) else stringResource(R.string.vpn_status_idle),
                fontSize = 8.sp,
                color = if (vpnState.status == VpnStatus.CONNECTED) CyberGreen else CyberOrange,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .background(if (vpnState.status == VpnStatus.CONNECTED) CyberGreen.copy(alpha = 0.12f) else CyberOrange.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                    .border(0.5.dp, if (vpnState.status == VpnStatus.CONNECTED) CyberGreen else CyberOrange, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.cd_vpn_settings_link),
                tint = TextSecondary,
                modifier = Modifier.size(12.dp)
            )
        }
    }
    Divider(color = CyberBorder.copy(alpha = 0.7f))
}
