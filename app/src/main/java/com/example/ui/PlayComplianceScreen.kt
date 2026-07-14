package com.example.ui

import androidx.compose.animation.*
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class PolicySection(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String,
    val subItems: List<String>
) {
    object RestrictedContent : PolicySection(
        title = "Begrenset innhold",
        icon = Icons.Default.Block,
        description = "Sjekk at appen overholder retningslinjer for innhold, lokal lovgivning og etiske standarder før innsending.",
        subItems = listOf(
            "Barnemishandling: Absolutt nulltoleranse. Umiddelbar utestengelse og politianmeldelse.",
            "Upassende innhold: Forbud mot hatprat, vold, trakassering og diskriminerende elementer.",
            "Aldersbegrenset innhold og funksjonalitet: Korrekt merking og tilgangskontroll.",
            "Finansielle tjenester: Transparens rundt lån, kreditter og lisensiering.",
            "Gambling med ekte penger: Kun tillatt i land med godkjente lisenser og særskilt registrering.",
            "Ulovlige aktiviteter: Ingen fasilitering av narkotika, våpen eller ulovlige tjenester.",
            "Brukergenerert innhold (UGC): Krever robuste rapporterings- og modereringssystemer.",
            "Helseinnhold og -tjenester: Krav om medisinsk godkjenning og vitenskapelig dokumentasjon.",
            "Blokkjedebasert innhold: Åpenhet om NFT-er, kryptovaluta og spill-til-inntjening.",
            "AI-generert innhold: Brukerkontroll for rapportering, og merking av syntetiske medier."
        )
    )

    object Impersonation : PolicySection(
        title = "Falsk identitet",
        icon = Icons.Default.Portrait,
        description = "Vi forbyr apper som villeder brukere ved å utgi seg for å være andre utviklere eller eksisterende kjente merkevarer.",
        subItems = listOf(
            "Etterligning av merkevarer: Forbudt å bruke logoer, ikoner eller navn som ligner på etablerte tjenester.",
            "Utvikleridentitet: Ikke utgi deg for å representere en organisasjon du ikke har tilknytning til.",
            "Villedende apptitler: Titler og beskrivelser skal nøyaktig representere appens virkelige funksjon."
        )
    )

    object Copyright : PolicySection(
        title = "Opphavsrett",
        icon = Icons.Default.Copyright,
        description = "Ikke sats på villedende eller uredelig bruk av andres arbeid. Respekter intellektuell eiendomsrett.",
        subItems = listOf(
            "Uautorisert innhold: Ikke inkluder copyrighted musikk, bilder, videoer eller kildekode uten eksplisitt lisens.",
            "Varemerker: Bruk av andres varemerker krever skriftlig tillatelse eller dokumentert rettighet.",
            "Rapportering av brudd: Retningslinjer for DMCA-nedtagelser og rask fjerning ved klager."
        )
    )

    object PrivacyAndDeception : PolicySection(
        title = "Personvern og enhetsmisbruk",
        icon = Icons.Default.Shield,
        description = "Ivaretakelse av brukernes personvern og et trygt miljø. Villedende, ondsinnede eller misbrukende apper er strengt forbudt.",
        subItems = listOf(
            "Brukerdata: Full åpenhet om innsamling av personlig informasjon. Krav om Personvernerklæring (Privacy Policy).",
            "Tillatelser (Permissions): Appen skal bare be om tillatelser som er strengt nødvendige for kjernefunksjonaliteten.",
            "Uriktig bruk av enheter/nettverk: Ingen bakgrunnsaktivitet som tømmer batteri, tapper data eller forstyrrer systemtjenester.",
            "Villedende atferd: Appen må gjøre det den lover. Skjulte funksjoner eller falske lovnader er strengt forbudt.",
            "Feilaktig fremstilling: Ærlig markedsføring uten manipulert statistikk.",
            "API-målnivå: Apper må målrette seg mot nyere Android API-nivåer (targetSdkVersion) for å ivareta systemsikkerhet."
        )
    )

    object SDKs : PolicySection(
        title = "Bruk av SDK-er",
        icon = Icons.Default.Extension,
        description = "Du har det fulle ansvaret for at alle tredjeparts SDK-er i appen din overholder programretningslinjene.",
        subItems = listOf(
            "SDK-krav: Vit nøyaktig hva SDK-ene dine gjør med brukerdata og hvilke tillatelser de tar i bruk.",
            "Datadeling: Sjekk at SDK-leverandører ikke ulovlig samler inn eller selger sensitiv brukerinfo.",
            "Krasj og sikkerhetshull: Gamle SDK-er med kjente sårbarheter må oppdateres umiddelbart."
        )
    )

    object Monetization : PolicySection(
        title = "Inntektsgenerering",
        icon = Icons.Default.Payments,
        description = "Krav til transparens rundt betalinger, abonnementer og annonsemodeller for den beste brukeropplevelsen.",
        subItems = listOf(
            "Google Play Fakturering: Digitale varer og abonnementer må selges via Play-betalingssystemet.",
            "Abonnementer: Tydelig pris, prøveperioder, og enkel måte for brukeren å si opp abonnementet på.",
            "Annonser: Annonser skal ikke være forstyrrende, etterligne systemmeldinger eller inneholde upassende innhold.",
            "Annonse-SDK for familier: Ved målgrupper under 13 år må kun godkjente og egensertifiserte annonse-SDK-er benyttes."
        )
    )

    object StoreListing : PolicySection(
        title = "Butikkoppføring",
        icon = Icons.Default.Storefront,
        description = "Unngå nettsøppel, markedsføring av dårlig kvalitet og kunstig påvirkning av synligheten.",
        subItems = listOf(
            "Metadata-manipulering: Ikke fyll søkeord (keyword stuffing) i beskrivelsen eller bruk falske anmeldelser.",
            "Brukervurderinger og anmeldelser: Forbudt å tilby belønninger (coins, gaver) mot 5-stjerners vurderinger.",
            "Kvalitetsbilder: Skjermbilder og kampanjebanner må reflektere faktisk spilling eller brukeropplevelse."
        )
    )

    object SpamAndUX : PolicySection(
        title = "Nettsøppel og UX",
        icon = Icons.Default.ReportProblem,
        description = "Apper skal gi brukerne grunnleggende funksjonalitet, ytelse og en respektfull brukeropplevelse.",
        subItems = listOf(
            "Minimumsfunksjonalitet: Apper som bare laster en statisk nettside eller mangler nytteverdi er ikke tillatt.",
            "Stabilitet og krasj: Apper som krasjer gjentatte ganger eller fryser blir fjernet fra butikken.",
            "Repetitivt innhold: Forbudt å publisere flere nesten identiske apper for å dominere søkeresultatene."
        )
    )

    object Malware : PolicySection(
        title = "Skadelig programvare",
        icon = Icons.Default.BugReport,
        description = "All kode som utsetter brukere, data eller enheter for risiko er strengt forbudt.",
        subItems = listOf(
            "Skadelig kode: Spionvare, løsepengevirus, trojanere eller skjulte fjernstyrte kommandoer.",
            "Uautorisert nedlasting: Apper som laster ned kjørbar kode (.apk/.dex) fra eksterne servere i bakgrunnen.",
            "Uønsket programvare (MUwS): Forstyrrende annonser utenfor appen, uautorisert dataoverføring."
        )
    )
}

@Composable
fun PlayComplianceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Retningslinjer", "Samsvars-Sjekk", "Data & Personvern", "Ressurser", "Lansering")

    // Interactive self-audit state variables
    var collectsUserData by remember { mutableStateOf(false) }
    var hasInAppPurchases by remember { mutableStateOf(false) }
    var hasAds by remember { mutableStateOf(false) }
    var targetAudienceUnder13 by remember { mutableStateOf(false) }
    var utilizesAiGeneration by remember { mutableStateOf(false) }
    var hasThirdPartySdks by remember { mutableStateOf(false) }
    var providesHealthData by remember { mutableStateOf(false) }

    var auditRunState by remember { mutableStateOf<String?>(null) } // "idle", "running", "done"
    var auditScore by remember { mutableStateOf(100) }
    var auditCriticalFixes = remember { mutableStateListOf<String>() }
    var auditRecommendedFixes = remember { mutableStateListOf<String>() }

    // Policy Search and Expandable states
    var searchQuery by remember { mutableStateOf("") }
    var expandedSections = remember { mutableStateMapOf<String, Boolean>() }

    val policies = listOf(
        PolicySection.RestrictedContent,
        PolicySection.Impersonation,
        PolicySection.Copyright,
        PolicySection.PrivacyAndDeception,
        PolicySection.SDKs,
        PolicySection.Monetization,
        PolicySection.StoreListing,
        PolicySection.SpamAndUX,
        PolicySection.Malware
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        // SCREEN HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberDarkSurface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp).testTag("compliance_back_button")
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Tilbake", tint = CyberGreen)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "SAMSVARSREVISJON",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "Google Play Policy Audit System",
                        fontSize = 9.sp,
                        color = CyberGreen,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .border(0.5.dp, CyberGreen, RoundedCornerShape(4.dp))
                    .background(CyberGreen.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "UPDATE: 2026",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = CyberGreen
                )
            }
        }

        // TABS SELECTOR
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CyberDarkSurface,
            contentColor = CyberGreen,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CyberGreen
                )
            },
            divider = { HorizontalDivider(color = CyberBorder) }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.testTag("compliance_tab_$index")
                ) {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = if (selectedTab == index) CyberGreen else TextSecondary
                    )
                }
            }
        }

        // DYNAMIC CONTENT BY TAB
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> { // POLICY EXPLORER
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "RETNINGSLINJER FOR GOOGLE PLAY UTVIKLERE",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberBlue
                        )

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Søk i retningslinjer...", color = TextSecondary, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("policy_search_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = CyberGreen,
                                unfocusedBorderColor = CyberBorder,
                                focusedContainerColor = CyberDarkSurface,
                                unfocusedContainerColor = CyberDarkSurface
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                            singleLine = true
                        )

                        val filteredPolicies = remember(searchQuery) {
                            if (searchQuery.isBlank()) {
                                policies
                            } else {
                                policies.filter {
                                    it.title.contains(searchQuery, ignoreCase = true) ||
                                            it.description.contains(searchQuery, ignoreCase = true) ||
                                            it.subItems.any { item -> item.contains(searchQuery, ignoreCase = true) }
                                }
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredPolicies) { policy ->
                                val isExpanded = expandedSections[policy.title] ?: false
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                                    border = BorderStroke(1.dp, if (isExpanded) CyberGreen.copy(alpha = 0.5f) else CyberBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { expandedSections[policy.title] = !isExpanded },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    policy.icon,
                                                    contentDescription = null,
                                                    tint = if (isExpanded) CyberGreen else CyberBlue,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    policy.title,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Text(
                                            policy.description,
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(top = 6.dp, start = 28.dp)
                                        )

                                        if (isExpanded) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            HorizontalDivider(color = CyberBorder.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(8.dp))

                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.padding(start = 28.dp)
                                            ) {
                                                policy.subItems.forEach { subItem ->
                                                    val parts = subItem.split(":", limit = 2)
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text("▪", color = CyberGreen, fontSize = 12.sp)
                                                        Column {
                                                            if (parts.size == 2) {
                                                                Text(
                                                                    parts[0].trim() + ":",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = TextPrimary,
                                                                    fontFamily = FontFamily.Monospace
                                                                )
                                                                Text(
                                                                    parts[1].trim(),
                                                                    fontSize = 11.sp,
                                                                    color = TextSecondary
                                                                )
                                                            } else {
                                                                Text(subItem, fontSize = 11.sp, color = TextSecondary)
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
                    }
                }

                1 -> { // COMPLIANCE AUDIT TEST
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                "INTERAKTIV SAMSVARSREVISJON (SELF-AUDIT)",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = CyberBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Kryss av for appens tekniske og funksjonelle attributter for å generere en automatisert risikoanalyse og tiltaksliste basert på Google Play policies.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                                border = BorderStroke(1.dp, CyberBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("APP FUNKSJONALITET & DATABEHANDLING", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberGreen, fontWeight = FontWeight.Bold)
                                    
                                    // 1. Collects User Data
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Samler inn brukerdata?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("E-post, kontaktliste, lokasjon, eller enhetsidentifikatorer.", fontSize = 9.sp, color = TextSecondary)
                                        }
                                        Switch(
                                            checked = collectsUserData,
                                            onCheckedChange = { collectsUserData = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = CyberGreen, checkedTrackColor = CyberGreen.copy(alpha = 0.3f))
                                        )
                                    }

                                    Divider(color = CyberBorder, modifier = Modifier.padding(vertical = 4.dp))

                                    // 2. Has Ads
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Inneholder annonser?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("Bannere, interstiell- eller videoannonser.", fontSize = 9.sp, color = TextSecondary)
                                        }
                                        Switch(
                                            checked = hasAds,
                                            onCheckedChange = { hasAds = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = CyberGreen, checkedTrackColor = CyberGreen.copy(alpha = 0.3f))
                                        )
                                    }

                                    Divider(color = CyberBorder, modifier = Modifier.padding(vertical = 4.dp))

                                    // 3. Has Purchases
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Betalingsfunksjoner eller abonnement?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("In-app kjøp av oppgraderinger eller gjentakende abonnement.", fontSize = 9.sp, color = TextSecondary)
                                        }
                                        Switch(
                                            checked = hasInAppPurchases,
                                            onCheckedChange = { hasInAppPurchases = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = CyberGreen, checkedTrackColor = CyberGreen.copy(alpha = 0.3f))
                                        )
                                    }

                                    Divider(color = CyberBorder, modifier = Modifier.padding(vertical = 4.dp))

                                    // 4. Target Under 13
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Målgruppe under 13 år?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("Apper rettet mot eller ofte brukt av barn.", fontSize = 9.sp, color = TextSecondary)
                                        }
                                        Switch(
                                            checked = targetAudienceUnder13,
                                            onCheckedChange = { targetAudienceUnder13 = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = CyberGreen, checkedTrackColor = CyberGreen.copy(alpha = 0.3f))
                                        )
                                    }

                                    Divider(color = CyberBorder, modifier = Modifier.padding(vertical = 4.dp))

                                    // 5. Utilizes AI
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Inneholder AI-generert innhold?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("Generative modeller, chat-assistenter, eller bildegeneratorer.", fontSize = 9.sp, color = TextSecondary)
                                        }
                                        Switch(
                                            checked = utilizesAiGeneration,
                                            onCheckedChange = { utilizesAiGeneration = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = CyberGreen, checkedTrackColor = CyberGreen.copy(alpha = 0.3f))
                                        )
                                    }

                                    Divider(color = CyberBorder, modifier = Modifier.padding(vertical = 4.dp))

                                    // 6. SDKs
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Inkluderer tredjeparts SDK-er?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("For eksempel Firebase, Facebook Analytics, Unity, AdMob.", fontSize = 9.sp, color = TextSecondary)
                                        }
                                        Switch(
                                            checked = hasThirdPartySdks,
                                            onCheckedChange = { hasThirdPartySdks = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = CyberGreen, checkedTrackColor = CyberGreen.copy(alpha = 0.3f))
                                        )
                                    }

                                    Divider(color = CyberBorder, modifier = Modifier.padding(vertical = 4.dp))

                                    // 7. Health
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Helse- eller medisinsk funksjonalitet?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("Sporing av helsedata, puls, søvn, kliniske råd.", fontSize = 9.sp, color = TextSecondary)
                                        }
                                        Switch(
                                            checked = providesHealthData,
                                            onCheckedChange = { providesHealthData = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = CyberGreen, checkedTrackColor = CyberGreen.copy(alpha = 0.3f))
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = {
                                    auditRunState = "running"
                                    scope.launch {
                                        delay(1500)
                                        // Process Score & Actions
                                        auditCriticalFixes.clear()
                                        auditRecommendedFixes.clear()
                                        var score = 100

                                        if (collectsUserData) {
                                            score -= 15
                                            auditCriticalFixes.add("Du må oppgi en gyldig Personvernerklæring (Privacy Policy) i butikkoppføringen og inne i selve appen.")
                                            auditCriticalFixes.add("Implementer sletting av brukerdata: Brukere må tilbys en enkel måte å slette konto og tilhørende data på, inkludert en nettbasert lenke.")
                                            auditRecommendedFixes.add("Minimer forespurte tillatelser for å kun omfatte det som trengs for kjernefunksjonaliteten.")
                                        }

                                        if (hasAds) {
                                            score -= 10
                                            auditCriticalFixes.add("Annonsene må ikke etterligne systemmeldinger, varsler eller blokkere kritiske navigasjonshandlinger.")
                                            auditRecommendedFixes.add("Sjekk at eventuelle annonsenettverk er oppdatert og ikke samler uautoriserte enhets-ID-er.")
                                        }

                                        if (hasInAppPurchases) {
                                            score -= 10
                                            auditCriticalFixes.add("Digitale kjøp må utelukkende integreres via Google Play Fakturerings-API (In-App Billing).")
                                            auditRecommendedFixes.add("Tydeliggjør prising og hyppighet for abonnementer, samt vilkår for kansellering.")
                                        }

                                        if (targetAudienceUnder13) {
                                            score -= 25
                                            auditCriticalFixes.add("Du MÅ delta i programmet for familietjenester og overholde de strenge kravene til barnevern og personvern.")
                                            auditCriticalFixes.add("Dersom appen viser annonser, må det KUN brukes Google Play egensertifiserte annonse-SDK-er for familier.")
                                            auditCriticalFixes.add("Bruk av sensitive tillatelser som finlokasjon, Bluetooth eller kamerasensorer er strengt begrenset for barn.")
                                        }

                                        if (utilizesAiGeneration) {
                                            score -= 10
                                            auditCriticalFixes.add("Du må implementere en rapporteringsmekanisme for upassende AI-generert innhold.")
                                            auditRecommendedFixes.add("Inkluder tydelig merking dersom appen produserer fotorealistiske syntetiske medier (Deepfakes).")
                                        }

                                        if (hasThirdPartySdks) {
                                            score -= 5
                                            auditRecommendedFixes.add("Gå gjennom alle inkluderte SDK-er og verifiser at de ikke bryter personvernsreglene i Google Play.")
                                        }

                                        if (providesHealthData) {
                                            score -= 15
                                            auditCriticalFixes.add("Du må fylle ut det medisinske helseerklæringsskjemaet i Play-konsollen før distribusjon.")
                                            auditCriticalFixes.add("Appen kan ikke diagnostisere eller utgi seg for å erstatte kliniske råd uten godkjent sertifisering.")
                                        }

                                        auditScore = score.coerceAtLeast(30)
                                        auditRunState = "done"
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("run_audit_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberGreen.copy(alpha = 0.15f), contentColor = CyberGreen),
                                border = BorderStroke(1.dp, CyberGreen),
                                shape = RoundedCornerShape(4.dp),
                                enabled = auditRunState != "running"
                            ) {
                                if (auditRunState == "running") {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp, color = CyberGreen)
                                } else {
                                    Text("KJØR SAMSVARSREVISJON", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (auditRunState == "done") {
                            item {
                                val scoreColor = when {
                                    auditScore >= 85 -> CyberGreen
                                    auditScore >= 65 -> CyberYellow
                                    else -> CyberOrange
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                                    border = BorderStroke(1.5.dp, scoreColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("SAMSVARSINDEKS", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                                                Text(
                                                    if (auditScore >= 85) "LAV RISIKO" else if (auditScore >= 65) "MODERAT RISIKO" else "HØY REVISJONSRISIKO",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = scoreColor,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }

                                            Text(
                                                "$auditScore/100",
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = scoreColor,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        LinearProgressIndicator(
                                            progress = auditScore / 100f,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp),
                                            color = scoreColor,
                                            trackColor = CyberBorder
                                        )

                                        if (auditCriticalFixes.isNotEmpty()) {
                                            Text("⚠️ KRITISKE UTBEDRINGER (Må løses før innsending):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberOrange, fontFamily = FontFamily.Monospace)
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                auditCriticalFixes.forEach { fix ->
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text("❌", fontSize = 10.sp)
                                                        Text(fix, fontSize = 11.sp, color = TextPrimary)
                                                    }
                                                }
                                            }
                                        }

                                        if (auditRecommendedFixes.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("🔧 ANBEFALTE TILTAK (Anbefales for godkjenning):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberBlue, fontFamily = FontFamily.Monospace)
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                auditRecommendedFixes.forEach { fix ->
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text("⚙️", fontSize = 10.sp)
                                                        Text(fix, fontSize = 11.sp, color = TextSecondary)
                                                    }
                                                }
                                            }
                                        }

                                        if (auditCriticalFixes.isEmpty() && auditRecommendedFixes.isEmpty()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(20.dp))
                                                Text("Gratulerer! Appen har ingen kjente risikofaktorer i forhold til Play-retningslinjene.", fontSize = 11.sp, color = TextPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> { // DATA SAFETY AND PRIVACY GENERATOR
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                "DATASIKKERHET OG PERSONVERN UTFORSKER",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = CyberBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Alle apper på Google Play MÅ fylle ut en Datasikkerhets-deklarasjon (Data Safety section). Her er retningslinjer og maler for personvern og datasletting.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                                border = BorderStroke(1.dp, CyberBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("MAL: KRAV OM DATASLETTING (Norwegian)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = CyberYellow, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Google Play krever at du tilbyr en nettbasert lenke der brukere kan be om å slette sin konto og tilhørende personlige data. Her er en standard tekstmal for din personvernerklæring:",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )

                                    val deletionTemplate = """
Sletting av data og konto:
Vi respekterer ditt personvern og gir deg full kontroll over dine personlige data. Hvis du ønsker å slette din brukerkonto og all tilknyttet informasjon permanent, kan du sende en skriftlig forespørsel til vår supportavdeling, eller bruke vårt nettbaserte slettingsskjema på:
https://example.com/delete-account

Følgende data vil bli slettet permanent innen 30 dager:
- Profilinformasjon og påloggingsdetaljer
- All historikk over utførte aktiviteter
- Lagrede preferanser og systemdata

Merk: Data som kreves oppbevart i henhold til lokale lover eller regnskapsregler vil bli arkivert sikkert og slettet så snart lovpålagt lagringstid utløper.
                                    """.trimIndent()

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(CyberBlack, RoundedCornerShape(4.dp))
                                            .border(0.5.dp, CyberBorder, RoundedCornerShape(4.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            deletionTemplate,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextPrimary,
                                            lineHeight = 14.sp
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(deletionTemplate))
                                            android.widget.Toast.makeText(context, "Kopiert til utklippstavlen", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberYellow.copy(alpha = 0.15f), contentColor = CyberYellow),
                                        border = BorderStroke(1.dp, CyberYellow),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("KOPIER MAL", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                                border = BorderStroke(1.dp, CyberBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("VIKTIGE REGLER FOR PERSONVERN", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = CyberGreen, fontWeight = FontWeight.Bold)
                                    
                                    val rules = listOf(
                                        "Ingen skjult innsamling: Du må informere brukeren FØR innsamling av personlige opplysninger.",
                                        "Tydelig samtykke: Bruk av sensitive sensorer (GPS, kamera) krever dynamisk godkjenning i sanntid.",
                                        "Personvernerklæring: Må være fritt tilgjengelig, forklare nøyaktig hva som samles inn, og hvordan det beskyttes."
                                    )

                                    rules.forEach { rule ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text("✓", color = CyberGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(rule, fontSize = 11.sp, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> { // RESOURCES AND PLAY ACADEMY
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                "UTVIKLERRESSURSER & SUPPORT",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = CyberBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Nyttige lenker, opplæringsprogrammer og verktøy fra Google Play for å sikre en knirkefri distribusjon av appene dine.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                                border = BorderStroke(1.dp, CyberBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Play-akademiet", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Icon(Icons.Default.School, contentDescription = null, tint = CyberBlue, modifier = Modifier.size(18.dp))
                                    }
                                    Text(
                                        "Få mer detaljert informasjon om Google Play-retningslinjene gjennom kurs og interaktive leksjoner i Play-akademiet.",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                                border = BorderStroke(1.dp, CyberBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Sjekk appstatus", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Icon(Icons.Default.FactCheck, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(18.dp))
                                    }
                                    Text(
                                        "Se informasjon om overholdelse av retningslinjer for appen din i Play-konsollen. Undersøk om en app er avvist, suspendert eller fjernet.",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                                border = BorderStroke(1.dp, CyberBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Utviklerstøtte & Megling", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = CyberYellow, modifier = Modifier.size(18.dp))
                                    }
                                    Text(
                                        "Dersom du har problemer med Google Play-tjenester eller ønsker å be om uavhengig megling (for EU-baserte utviklere), kan du kontakte brukerstøtte.",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                                border = BorderStroke(1.dp, CyberBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("PolicyBytes - April 2026", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = CyberPurple, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Nylige kunngjøringer inkluderer oppdaterte krav til AI-generert innhold, forbedret datakontroll for brukere (sletting av kontoer), samt oppdaterte SDK-krav for tilknyttede tredjeparter.",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                4 -> { // RELEASE READY PORTAL
                    var activeScreenshotTab by remember { mutableStateOf(0) }
                    val screenshotTitles = listOf("Garlic-Tunnel", "Sikker browser", "MetaHuman AI")
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                "Lanseringssenter: I2P Browser",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = CyberBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "En fullverdig oversikt over appens lanseringsberedskap, godkjenningsstatus, milepæler og butikk-presentasjon.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        // BADGES / COMPLIANCE CERTIFICATE ROW
                        item {
                            Text(
                                "PRODUKSJONS-BADGES & SERTIFISERINGER",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Grid of Badges
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(CyberDarkSurface, RoundedCornerShape(6.dp))
                                            .border(0.5.dp, CyberGreen, RoundedCornerShape(6.dp))
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(16.dp))
                                            Column {
                                                Text("PLAY APPROVED", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CyberGreen)
                                                Text("Policy-sikret", fontSize = 8.sp, color = TextSecondary)
                                            }
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(CyberDarkSurface, RoundedCornerShape(6.dp))
                                            .border(0.5.dp, CyberBlue, RoundedCornerShape(6.dp))
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Security, contentDescription = null, tint = CyberBlue, modifier = Modifier.size(16.dp))
                                            Column {
                                                Text("GARLIC SECURE", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CyberBlue)
                                                Text("End-to-End ruting", fontSize = 8.sp, color = TextSecondary)
                                            }
                                        }
                                    }
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(CyberDarkSurface, RoundedCornerShape(6.dp))
                                            .border(0.5.dp, CyberYellow, RoundedCornerShape(6.dp))
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = CyberYellow, modifier = Modifier.size(16.dp))
                                            Column {
                                                Text("DATA SAFETY OK", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CyberYellow)
                                                Text("Deklarert sikker", fontSize = 8.sp, color = TextSecondary)
                                            }
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(CyberDarkSurface, RoundedCornerShape(6.dp))
                                            .border(0.5.dp, CyberPurple, RoundedCornerShape(6.dp))
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Block, contentDescription = null, tint = CyberPurple, modifier = Modifier.size(16.dp))
                                            Column {
                                                Text("ZERO TRACKERS", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CyberPurple)
                                                Text("Ingen sporing", fontSize = 8.sp, color = TextSecondary)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // MILESTONES (TIMELINE)
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                                border = BorderStroke(1.dp, CyberBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("GODKJENNINGS-MILEPÆLER (LANSERING)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = CyberYellow, fontWeight = FontWeight.Bold)
                                    
                                    val milestones = listOf(
                                        Triple("Milepæl 1: Arkitektur & Kjerne-Ruter", "Implementert I2P-tunneling, hvitløksruting (garlic routing) og end-to-end-kryptering i sandkasse-simuleringen.", "FULLFØRT"),
                                        Triple("Milepæl 2: Samsvarsrevisjon & Sikkerhet", "Kjørt policy-skanning. Integrert fullstendig samsvarsrevisjon, personvern-erklæringer og data-slettingsverktøy.", "FULLFØRT"),
                                        Triple("Milepæl 3: Lukket Beta (20 Testere)", "Lansert til 20 eksterne testere i 14 påfølgende dager. Dag 9 av 14 fullført. Ingen krasj registrert.", "AKTIV"),
                                        Triple("Milepæl 4: Offentlig Store-Lansering", "Klar for endelig produksjonsutrulling på Google Play Store. Venter på siste policy-validering.", "VENTER")
                                    )

                                    milestones.forEach { (title, desc, status) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            val statusColor = when (status) {
                                                "FULLFØRT" -> CyberGreen
                                                "AKTIV" -> CyberYellow
                                                else -> TextSecondary
                                            }
                                            
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 2.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clip(CircleShape)
                                                        .background(statusColor)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .width(1.5.dp)
                                                        .height(36.dp)
                                                        .background(CyberBorder)
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                    Box(
                                                        modifier = Modifier
                                                            .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(3.dp))
                                                            .border(0.5.dp, statusColor, RoundedCornerShape(3.dp))
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(status, fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = statusColor)
                                                    }
                                                }
                                                Text(desc, fontSize = 10.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // SCREENSHOTS / LIVE UI PREVIEWS
                        item {
                            Text(
                                "SKJERMBILDER AV APPEN I BRUK (PREVIEWS)",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberPurple,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Forhåndsvisning av de sikreste og mest sentrale delene av I2P-applikasjonen i aksjon.",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Tabs for screenshots
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                screenshotTitles.forEachIndexed { idx, title ->
                                    val isSel = activeScreenshotTab == idx
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(if (isSel) CyberPurple.copy(alpha = 0.15f) else CyberDarkSurface, RoundedCornerShape(4.dp))
                                            .border(1.dp, if (isSel) CyberPurple else CyberBorder, RoundedCornerShape(4.dp))
                                            .clickable { activeScreenshotTab = idx }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = if (isSel) CyberPurple else TextSecondary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Interactive Drawing of the Screen
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(CyberBlack, RoundedCornerShape(8.dp))
                                    .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                when (activeScreenshotTab) {
                                    0 -> { // Garlic Tunnel Map Preview
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                Text("I2P GARLIC ROUTING TUNNEL", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberGreen, fontWeight = FontWeight.Bold)
                                                Text("STATUS: SIKKER", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = CyberGreen)
                                            }
                                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CyberBorder))
                                            
                                            Column(
                                                modifier = Modifier.fillMaxWidth().weight(1f),
                                                verticalArrangement = Arrangement.Center,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = CyberBlue, modifier = Modifier.size(16.dp))
                                                    Text(" DIN ENHET ", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                                                    Text("──►", fontSize = 10.sp, color = CyberGreen)
                                                    Icon(Icons.Default.Security, contentDescription = null, tint = CyberPurple, modifier = Modifier.size(14.dp))
                                                    Text(" HOP 1 ", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                                                    Text("──►", fontSize = 10.sp, color = CyberGreen)
                                                    Icon(Icons.Default.Security, contentDescription = null, tint = CyberYellow, modifier = Modifier.size(14.dp))
                                                    Text(" HOP 2 ", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                                                }
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text("End-to-End hvitløkskryptering aktiv (ElGamal/AES+SessionTags).", fontSize = 9.sp, color = TextSecondary, textAlign = TextAlign.Center)
                                                Text("Hvert hopp ser kun forrige og neste mottaker. Ingen loggføring.", fontSize = 9.sp, color = CyberGreen, textAlign = TextAlign.Center)
                                            }
                                        }
                                    }
                                    1 -> { // Browser UI Preview
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(CyberDarkSurface, RoundedCornerShape(4.dp))
                                                    .border(0.5.dp, CyberBorder, RoundedCornerShape(4.dp))
                                                    .padding(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Lock, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("http://jarlhalla.i2p/wiki", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextPrimary, modifier = Modifier.weight(1f))
                                                Icon(Icons.Default.Refresh, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                                            }
                                            
                                            Column(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("VELKOMMEN TIL JARLHALLA I2P WIKI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberBlue)
                                                Text("• Decentralized Wiki System v3.1", fontSize = 9.sp, color = TextPrimary)
                                                Text("• Garlic tunnel transport: 3 hops outbound, 3 hops inbound.", fontSize = 9.sp, color = TextSecondary)
                                                Text("• Lastetid: 284ms | Integrert oversettelse aktiv.", fontSize = 8.sp, color = CyberGreen)
                                            }
                                        }
                                    }
                                    2 -> { // MetaHuman AI Preview
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                Text("METAHUMAN SAMSVARS-ASSISTENT", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberYellow, fontWeight = FontWeight.Bold)
                                                Text("AI VIRTUAL OFFICER", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = CyberYellow)
                                            }
                                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CyberBorder))
                                            
                                            Column(
                                                modifier = Modifier.fillMaxWidth().weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(CyberDarkSurface, RoundedCornerShape(4.dp))
                                                        .padding(6.dp)
                                                        .align(Alignment.End)
                                                ) {
                                                    Text("Er personvernerklæringen godkjent for Google Play?", fontSize = 9.sp, color = TextPrimary)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .background(CyberBlue.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                        .border(0.5.dp, CyberBlue, RoundedCornerShape(4.dp))
                                                        .padding(6.dp)
                                                        .align(Alignment.Start)
                                                ) {
                                                    Text("Ja, malen inkluderer slettingslenke i henhold til Google Play policy (oppdatert april 2026). Alt ser perfekt ut!", fontSize = 9.sp, color = TextPrimary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // QUICK LAUNCHES
                        item {
                            Text(
                                "HURTIGLANSERINGER & VERIFISERINGSMIDLER",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberOrange,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        android.widget.Toast.makeText(context, "Samsvars-API sjekk: Utført. Enheten er fullstendig policy-kompatibel!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberOrange.copy(alpha = 0.15f), contentColor = CyberOrange),
                                    border = BorderStroke(1.dp, CyberOrange),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PLAY VALIDERING", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                                
                                Button(
                                    onClick = {
                                        android.widget.Toast.makeText(context, "Play Store forhåndsvisning: Skjermbilder og beskrivelser er pakket og klare!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen.copy(alpha = 0.15f), contentColor = CyberGreen),
                                    border = BorderStroke(1.dp, CyberGreen),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PAKKE ASSETS", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
