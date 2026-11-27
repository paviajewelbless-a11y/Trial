package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.ExportFileFormat
import com.example.domain.OperationsCalculator
import com.example.domain.ReportExporter
import com.example.model.BreadcrumbActor
import com.example.model.BreadcrumbCategory
import com.example.model.BreadcrumbEntry
import com.example.model.BreadcrumbSeverity
import com.example.ui.components.DownloadAlsoPdfXlsDialog
import com.example.ui.components.DownloadedReportBanner
import com.example.ui.components.IndustrialKpiCard
import com.example.ui.components.LocalWindowWidthDp
import com.example.ui.components.desktopPointerHand
import com.example.ui.components.openExportedFileUri
import com.example.ui.components.sendFileToMessenger
import com.example.ui.components.shareExportedFileUri
import com.example.ui.theme.ForestEmeraldContainer
import com.example.ui.theme.OnForestEmeraldContainer
import com.example.ui.theme.OnSlateTealContainer
import com.example.ui.theme.OnStatusCriticalContainer
import com.example.ui.theme.OnStatusInProgressContainer
import com.example.ui.theme.OnStatusWatchContainer
import com.example.ui.theme.SlateTeal
import com.example.ui.theme.SlateTealContainer
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusCriticalContainer
import com.example.ui.theme.StatusCriticalTextDark
import com.example.ui.theme.StatusHealthy
import com.example.ui.theme.StatusInProgress
import com.example.ui.theme.StatusInProgressContainer
import com.example.ui.theme.StatusWatch
import com.example.ui.theme.StatusWatchContainer
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private enum class HistoryTab(val label: String) {
    ACTIONS("Actions"),
    SYSTEM("System"),
    ALERTS("Alerts"),
    TRAIL("Trail")
}

@Composable
fun HistoryScreen(
    breadcrumbs: List<BreadcrumbEntry>,
    sessionTrail: List<String>,
    onJumpToTabLabel: (String) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onClearHistory: () -> Unit,
    onHistoryExported: (List<ExportFileFormat>, Int) -> Unit,
    onShowToast: (String) -> Unit,
    canManageHistory: Boolean = true
) {
    val context = LocalContext.current
    val windowWidthDp = LocalWindowWidthDp.current

    var selectedTab by remember { mutableStateOf(HistoryTab.ACTIONS) }
    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var trailCategoryFilter by remember { mutableStateOf<BreadcrumbCategory?>(null) }
    var expandedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showDownloadPrompt by remember { mutableStateOf(false) }
    var downloadedFileInfo by remember { mutableStateOf<Triple<Uri, String, String>?>(null) }
    var activeSaveRequest by remember { mutableStateOf<Triple<ByteArray, String, String>?>(null) }
    var pendingSavesQueue by remember { mutableStateOf<List<Triple<ByteArray, String, String>>>(emptyList()) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val current = activeSaveRequest
        activeSaveRequest = null
        if (uri != null && current != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(current.first)
                    out.flush()
                }
                downloadedFileInfo = Triple(uri, current.second, current.third)
                onShowToast("Saved ${current.second}")
            } catch (e: Exception) {
                onShowToast("Failed to save file: ${e.localizedMessage}")
            }
        }
    }

    LaunchedEffect(pendingSavesQueue, activeSaveRequest) {
        if (activeSaveRequest == null && pendingSavesQueue.isNotEmpty()) {
            val next = pendingSavesQueue.first()
            pendingSavesQueue = pendingSavesQueue.drop(1)
            activeSaveRequest = next
            createDocumentLauncher.launch(next.second)
        }
    }

    val totalCount = breadcrumbs.size
    val userActionsCount = remember(breadcrumbs) {
        breadcrumbs.count {
            it.actorType.equals(BreadcrumbActor.USER.label, ignoreCase = true) && !it.isRoutineNavigation()
        }
    }
    val systemCount = remember(breadcrumbs) {
        breadcrumbs.count { it.actorType.equals(BreadcrumbActor.SYSTEM.label, ignoreCase = true) }
    }
    val alertsCount = remember(breadcrumbs) {
        breadcrumbs.count {
            it.severity.equals(BreadcrumbSeverity.WARNING.label, ignoreCase = true) ||
                it.severity.equals(BreadcrumbSeverity.ERROR.label, ignoreCase = true)
        }
    }

    val tabEntries = remember(breadcrumbs, selectedTab, searchQuery, trailCategoryFilter) {
        val q = searchQuery.trim().lowercase()
        breadcrumbs.filter { entry ->
            val tabMatch = when (selectedTab) {
                HistoryTab.ACTIONS ->
                    entry.actorType.equals(BreadcrumbActor.USER.label, ignoreCase = true) &&
                        !entry.isRoutineNavigation()
                HistoryTab.SYSTEM ->
                    entry.actorType.equals(BreadcrumbActor.SYSTEM.label, ignoreCase = true)
                HistoryTab.ALERTS ->
                    entry.severity.equals(BreadcrumbSeverity.WARNING.label, ignoreCase = true) ||
                        entry.severity.equals(BreadcrumbSeverity.ERROR.label, ignoreCase = true)
                HistoryTab.TRAIL ->
                    trailCategoryFilter == null ||
                        entry.category.equals(trailCategoryFilter!!.label, ignoreCase = true)
            }
            val queryMatch = q.isEmpty() ||
                friendlyActionTitle(entry).lowercase().contains(q) ||
                entry.action.lowercase().contains(q) ||
                entry.details.lowercase().contains(q) ||
                entry.entityRef.lowercase().contains(q) ||
                entry.screenTab.lowercase().contains(q) ||
                entry.category.lowercase().contains(q)
            tabMatch && queryMatch
        }
    }

    val groupedByDate = remember(tabEntries) {
        tabEntries.groupBy { it.dateStr.ifBlank { "Recent" } }
            .entries
            .sortedByDescending { it.key }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Minimal Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Activity History",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = when {
                            breadcrumbs.isEmpty() -> "No recent activity yet"
                            selectedTab == HistoryTab.ACTIONS -> "${tabEntries.size} recent action(s) • tap any row for details"
                            selectedTab == HistoryTab.SYSTEM -> "${tabEntries.size} automatic plant update(s)"
                            selectedTab == HistoryTab.ALERTS -> "${tabEntries.size} alert(s) needing attention"
                            else -> "Overview, session path & screen visits"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (searchVisible && searchQuery.isNotEmpty()) {
                                searchQuery = ""
                            }
                            searchVisible = !searchVisible
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .desktopPointerHand()
                            .testTag("history_search_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (searchVisible) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = if (searchVisible) "Hide search" else "Search history"
                        )
                    }

                    OutlinedButton(
                        onClick = { showDownloadPrompt = true },
                        enabled = tabEntries.isNotEmpty(),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .desktopPointerHand()
                            .testTag("history_download_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download")
                    }

                    if (canManageHistory) {
                        IconButton(
                            onClick = { showClearConfirm = true },
                            enabled = breadcrumbs.isNotEmpty(),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .desktopPointerHand()
                                .testTag("history_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = "Clear history",
                                tint = if (breadcrumbs.isNotEmpty()) StatusCriticalTextDark else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Hidden-until-tapped search bar
            AnimatedVisibility(visible = searchVisible || searchQuery.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by product, customer, DR #, or action...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input")
                )
            }

            downloadedFileInfo?.let { (uri, fileName, mimeType) ->
                DownloadedReportBanner(
                    fileName = fileName,
                    mimeType = mimeType,
                    onOpen = { openExportedFileUri(context, uri, mimeType, onShowToast) },
                    onShare = { shareExportedFileUri(context, uri, fileName, mimeType) },
                    onSendMessenger = {
                        sendFileToMessenger(
                            context = context,
                            fileUri = uri,
                            fileName = fileName,
                            mimeType = mimeType,
                            captionText = "10K Agri Feeds - Activity History ($fileName)",
                            onToast = onShowToast
                        )
                    }
                )
            }
        }

        // Sub-Tabs: keep details hidden until user switches to the needed tab
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                if (selectedTab.ordinal < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            HistoryTab.entries.forEach { tab ->
                val tabLabel = when (tab) {
                    HistoryTab.ALERTS -> if (alertsCount > 0) "Alerts ($alertsCount)" else "Alerts"
                    else -> tab.label
                }
                val isSelected = selectedTab == tab
                Tab(
                    selected = isSelected,
                    onClick = { selectedTab = tab },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Text(
                            text = tabLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .desktopPointerHand()
                        .testTag("history_tab_${tab.name.lowercase()}")
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (selectedTab == HistoryTab.TRAIL) {
                // Tucked-away overview KPIs
                item {
                    if (windowWidthDp >= 900) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IndustrialKpiCard(
                                title = "All Events",
                                primaryValue = totalCount.toString(),
                                secondaryValue = "Saved in cloud history",
                                icon = Icons.Filled.History,
                                accentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            IndustrialKpiCard(
                                title = "Your Actions",
                                primaryValue = userActionsCount.toString(),
                                secondaryValue = "Edits, orders & dispatches",
                                icon = Icons.Filled.Person,
                                accentColor = StatusInProgress,
                                modifier = Modifier.weight(1f)
                            )
                            IndustrialKpiCard(
                                title = "System Updates",
                                primaryValue = systemCount.toString(),
                                secondaryValue = "Sync & auto-updates",
                                icon = Icons.Filled.Dns,
                                accentColor = SlateTeal,
                                modifier = Modifier.weight(1f)
                            )
                            IndustrialKpiCard(
                                title = "Alerts",
                                primaryValue = alertsCount.toString(),
                                secondaryValue = if (alertsCount == 0) "Everything looks good" else "Items to review",
                                icon = Icons.Filled.WarningAmber,
                                accentColor = if (alertsCount > 0) StatusWatch else StatusHealthy,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IndustrialKpiCard(
                                    title = "All Events",
                                    primaryValue = totalCount.toString(),
                                    secondaryValue = "Total saved",
                                    icon = Icons.Filled.History,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                IndustrialKpiCard(
                                    title = "Your Actions",
                                    primaryValue = userActionsCount.toString(),
                                    secondaryValue = "Orders & work",
                                    icon = Icons.Filled.Person,
                                    accentColor = StatusInProgress,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IndustrialKpiCard(
                                    title = "System Updates",
                                    primaryValue = systemCount.toString(),
                                    secondaryValue = "Sync & checks",
                                    icon = Icons.Filled.Dns,
                                    accentColor = SlateTeal,
                                    modifier = Modifier.weight(1f)
                                )
                                IndustrialKpiCard(
                                    title = "Alerts",
                                    primaryValue = alertsCount.toString(),
                                    secondaryValue = if (alertsCount == 0) "All clear" else "Needs review",
                                    icon = Icons.Filled.WarningAmber,
                                    accentColor = if (alertsCount > 0) StatusWatch else StatusHealthy,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Tucked-away session path
                if (sessionTrail.isNotEmpty()) {
                    item {
                        SessionTrailBannerCard(
                            sessionTrail = sessionTrail,
                            onJumpToTabLabel = onJumpToTabLabel
                        )
                    }
                }

                // Category Filter chips inside Trail tab
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Filter by Area",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = trailCategoryFilter == null,
                                onClick = { trailCategoryFilter = null },
                                label = { Text("All") },
                                modifier = Modifier
                                    .desktopPointerHand()
                                    .testTag("history_category_all")
                            )
                            BreadcrumbCategory.entries.forEach { category ->
                                FilterChip(
                                    selected = trailCategoryFilter == category,
                                    onClick = {
                                        trailCategoryFilter = if (trailCategoryFilter == category) null else category
                                    },
                                    label = { Text(category.label) },
                                    modifier = Modifier
                                        .desktopPointerHand()
                                        .testTag("history_category_${category.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            if (tabEntries.isEmpty()) {
                item {
                    EmptyHistoryTabCard(
                        selectedTab = selectedTab,
                        hasAnyBreadcrumbs = breadcrumbs.isNotEmpty(),
                        hasSearchOrFilter = searchQuery.isNotBlank() || trailCategoryFilter != null,
                        onOpenTrailTab = { selectedTab = HistoryTab.TRAIL },
                        onResetSearch = {
                            searchQuery = ""
                            trailCategoryFilter = null
                        }
                    )
                }
            } else {
                groupedByDate.forEach { (dateKey, dayEntries) ->
                    item(key = "header_${selectedTab.name}_$dateKey") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = friendlyDateHeader(dateKey),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${dayEntries.size} item${if (dayEntries.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(
                        items = dayEntries,
                        key = { entry -> "${selectedTab.name}_${entry.id.ifBlank { "${entry.timestampMillis}_${entry.action}" }}" }
                    ) { entry ->
                        val rowKey = entry.id.ifBlank { "${entry.timestampMillis}_${entry.action}" }
                        val isExpanded = rowKey in expandedIds
                        MinimalBreadcrumbRow(
                            entry = entry,
                            expanded = isExpanded,
                            canDelete = canManageHistory,
                            onToggleExpand = {
                                expandedIds = if (isExpanded) expandedIds - rowKey else expandedIds + rowKey
                            },
                            onDelete = { if (entry.id.isNotBlank()) onDeleteEntry(entry.id) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showDownloadPrompt) {
        DownloadAlsoPdfXlsDialog(
            title = "Download Activity History",
            initialFormat = ExportFileFormat.CSV,
            onDismiss = { showDownloadPrompt = false },
            onConfirmFormats = { chosenFormats ->
                showDownloadPrompt = false
                val savedLabels = mutableListOf<String>()
                val queuedFallback = mutableListOf<Triple<ByteArray, String, String>>()
                val scopeLabel = when (selectedTab) {
                    HistoryTab.ACTIONS -> "User Actions"
                    HistoryTab.SYSTEM -> "System Updates"
                    HistoryTab.ALERTS -> "Alerts & Warnings"
                    HistoryTab.TRAIL -> trailCategoryFilter?.label ?: "Full Activity & Trail"
                }
                chosenFormats.forEach { fmt ->
                    try {
                        val res = ReportExporter.saveBreadcrumbsToDownloads(
                            context = context,
                            entries = tabEntries,
                            format = fmt,
                            scopeLabel = scopeLabel
                        )
                        if (res.uri != null) {
                            downloadedFileInfo = Triple(res.uri, res.fileName, fmt.mimeType)
                            savedLabels.add(fmt.label)
                        } else {
                            queuedFallback.add(Triple(res.bytes, res.fileName, fmt.mimeType))
                        }
                    } catch (e: Exception) {
                        onShowToast("${fmt.label} export failed: ${e.localizedMessage}")
                    }
                }
                if (queuedFallback.isNotEmpty()) {
                    pendingSavesQueue = pendingSavesQueue + queuedFallback
                }
                if (savedLabels.isNotEmpty()) {
                    onShowToast("Downloaded ${savedLabels.joinToString(", ")} to Downloads")
                }
                onHistoryExported(chosenFormats, tabEntries.size)
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.DeleteSweep,
                    contentDescription = null,
                    tint = StatusCritical
                )
            },
            title = { Text("Clear Activity History?") },
            text = {
                Text(
                    "This removes all $totalCount recorded history entries. Your inventory, customer orders, schedules, dispatches, and production batches will stay safe."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearHistory()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusCritical,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_clear_history_button")
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearConfirm = false },
                    modifier = Modifier.testTag("cancel_clear_history_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun BreadcrumbEntry.isRoutineNavigation(): Boolean =
    category.equals(BreadcrumbCategory.NAVIGATION.label, ignoreCase = true) ||
        action.startsWith("Opened ", ignoreCase = true) ||
        action.startsWith("Switched Tab", ignoreCase = true) ||
        action.startsWith("Quick Jumped", ignoreCase = true)

private fun friendlyDateHeader(dateKey: String): String {
    if (dateKey.isBlank() || dateKey == "Recent") return "Recent"
    val today = OperationsCalculator.todayIsoDate()
    if (dateKey == today) return "Today"
    val parsed = OperationsCalculator.parseDateOrNull(dateKey) ?: return dateKey
    val todayDate = OperationsCalculator.parseDateOrNull(today)
    if (todayDate != null && parsed == todayDate.minusDays(1)) {
        return "Yesterday"
    }
    return parsed.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
}

private fun friendlyTimeLabel(entry: BreadcrumbEntry): String {
    if (entry.timestampMillis > 0L) {
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entry.timestampMillis))
    }
    return entry.timeLabel.ifBlank { "—" }
}

private fun friendlyActionTitle(entry: BreadcrumbEntry): String {
    val raw = entry.action.trim()
    if (raw.isEmpty()) return "Activity Update"
    return when {
        raw.startsWith("Navigated to ", ignoreCase = true) ->
            "Opened ${raw.removePrefix("Navigated to ").trim()} screen"
        raw.startsWith("Quick Jumped to ", ignoreCase = true) ->
            "Jumped to ${raw.removePrefix("Quick Jumped to ").trim()}"
        raw.startsWith("Switched Tab to ", ignoreCase = true) ->
            "Viewed ${raw.removePrefix("Switched Tab to ").trim()}"
        raw.equals("Added Dispatch", ignoreCase = true) -> "Recorded delivery dispatch"
        raw.equals("Updated Dispatch", ignoreCase = true) -> "Updated delivery dispatch"
        raw.equals("Deleted Dispatch", ignoreCase = true) -> "Removed delivery dispatch"
        raw.equals("Added Customer Order", ignoreCase = true) -> "Created customer order"
        raw.equals("Updated Customer Order", ignoreCase = true) -> "Updated customer order"
        raw.equals("Deleted Customer Order", ignoreCase = true) -> "Removed customer order"
        raw.equals("Added Schedule", ignoreCase = true) -> "Scheduled production run"
        raw.equals("Updated Schedule", ignoreCase = true) -> "Updated production schedule"
        raw.equals("Completed Schedule", ignoreCase = true) -> "Completed scheduled run"
        raw.equals("Added Batch", ignoreCase = true) -> "Recorded production batch"
        raw.equals("Updated Batch", ignoreCase = true) -> "Updated production batch"
        raw.equals("Deleted Batch", ignoreCase = true) -> "Removed production batch"
        raw.equals("Added SKU", ignoreCase = true) -> "Added product SKU"
        raw.equals("Updated SKU", ignoreCase = true) -> "Updated product inventory"
        raw.equals("Adjusted Stock", ignoreCase = true) -> "Adjusted product stock"
        raw.startsWith("Firestore Sync", ignoreCase = true) -> "Synced data with cloud"
        else -> raw
    }
}

private fun categoryIcon(categoryRaw: String): ImageVector = when (BreadcrumbCategory.fromRaw(categoryRaw)) {
    BreadcrumbCategory.DISPATCH -> Icons.Filled.LocalShipping
    BreadcrumbCategory.REPROCESS -> Icons.Filled.Autorenew
    BreadcrumbCategory.ORDERS -> Icons.Filled.ShoppingCart
    BreadcrumbCategory.INVENTORY -> Icons.Filled.Inventory2
    BreadcrumbCategory.SCHEDULE -> Icons.Filled.EventNote
    BreadcrumbCategory.RECOVERY -> Icons.Filled.Factory
    BreadcrumbCategory.CUSTOMERS -> Icons.Filled.Person
    BreadcrumbCategory.REPORTS -> Icons.Filled.Assessment
    BreadcrumbCategory.AI_OPS -> Icons.Filled.AutoAwesome
    BreadcrumbCategory.NAVIGATION -> Icons.Filled.Route
    BreadcrumbCategory.SYSTEM -> Icons.Filled.Dns
}

@Composable
private fun MinimalBreadcrumbRow(
    entry: BreadcrumbEntry,
    expanded: Boolean,
    canDelete: Boolean,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit
) {
    val isUser = entry.actorType.equals(BreadcrumbActor.USER.label, ignoreCase = true)
    val severity = BreadcrumbSeverity.fromRaw(entry.severity)

    val (iconBg, iconFg) = when (severity) {
        BreadcrumbSeverity.ERROR -> StatusCriticalContainer to OnStatusCriticalContainer
        BreadcrumbSeverity.WARNING -> StatusWatchContainer to OnStatusWatchContainer
        BreadcrumbSeverity.SUCCESS -> ForestEmeraldContainer to OnForestEmeraldContainer
        BreadcrumbSeverity.INFO -> if (isUser) {
            StatusInProgressContainer to OnStatusInProgressContainer
        } else {
            SlateTealContainer to OnSlateTealContainer
        }
    }

    val title = remember(entry.action) { friendlyActionTitle(entry) }
    val timeText = remember(entry.timestampMillis, entry.timeLabel) { friendlyTimeLabel(entry) }
    val summaryText = entry.details.ifBlank {
        listOfNotNull(
            entry.screenTab.takeIf { it.isNotBlank() },
            entry.entityRef.takeIf { it.isNotBlank() }
        ).joinToString(" • ")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggleExpand)
            .desktopPointerHand()
            .testTag("breadcrumb_card_${entry.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Minimal Collapsed Summary Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (severity == BreadcrumbSeverity.WARNING || severity == BreadcrumbSeverity.ERROR) {
                            Icons.Filled.WarningAmber
                        } else {
                            categoryIcon(entry.category)
                        },
                        contentDescription = null,
                        tint = iconFg,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (summaryText.isNotBlank()) {
                        Text(
                            text = summaryText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse details" else "Expand details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Expand-on-demand Details
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                    if (entry.details.isNotBlank()) {
                        Text(
                            text = entry.details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DetailFact(
                            label = "Done by",
                            value = if (isUser) "You" else "Plant System",
                            modifier = Modifier.weight(1f)
                        )
                        DetailFact(
                            label = "Area",
                            value = entry.category,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (entry.screenTab.isNotBlank() || entry.entityRef.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (entry.screenTab.isNotBlank()) {
                                DetailFact(
                                    label = "Screen",
                                    value = entry.screenTab,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (entry.entityRef.isNotBlank()) {
                                DetailFact(
                                    label = "Reference",
                                    value = entry.entityRef,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (entry.trailPath.isNotBlank()) {
                        DetailFact(
                            label = "Navigation Path",
                            value = entry.trailPath,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (canDelete && entry.id.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = onDelete,
                                colors = ButtonDefaults.textButtonColors(contentColor = StatusCriticalTextDark),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .desktopPointerHand()
                                    .testTag("delete_breadcrumb_${entry.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailFact(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EmptyHistoryTabCard(
    selectedTab: HistoryTab,
    hasAnyBreadcrumbs: Boolean,
    hasSearchOrFilter: Boolean,
    onOpenTrailTab: () -> Unit,
    onResetSearch: () -> Unit
) {
    val (title, subtitle) = when {
        hasSearchOrFilter -> "No matching activity found" to "Try a different search word or clear the filter."
        !hasAnyBreadcrumbs -> "No activity recorded yet" to "Actions you take—like adding orders, dispatching deliveries, or updating schedules—appear here cleanly."
        selectedTab == HistoryTab.ACTIONS -> "No recent work actions yet" to "Your edits, orders, and dispatches will appear here. Screen visits are tucked in the Trail tab."
        selectedTab == HistoryTab.SYSTEM -> "No system updates yet" to "Cloud syncs, automatic stock checks, and report updates will show here."
        selectedTab == HistoryTab.ALERTS -> "No warnings or errors" to "Everything is running smoothly right now."
        else -> "No trail activity recorded" to "Screen navigation and full activity trail appear here."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (selectedTab == HistoryTab.ALERTS) Icons.Filled.WarningAmber else Icons.Filled.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hasSearchOrFilter) {
                TextButton(
                    onClick = onResetSearch,
                    modifier = Modifier.testTag("history_reset_filters_button")
                ) {
                    Text("Reset Search & Filters")
                }
            } else if (hasAnyBreadcrumbs && selectedTab == HistoryTab.ACTIONS) {
                TextButton(
                    onClick = onOpenTrailTab,
                    modifier = Modifier.testTag("history_view_trail_button")
                ) {
                    Text("View Screen Trail")
                }
            }
        }
    }
}

@Composable
private fun SessionTrailBannerCard(
    sessionTrail: List<String>,
    onJumpToTabLabel: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Route,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Where You've Been This Session",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                sessionTrail.forEachIndexed { idx, tabLabel ->
                    val isLast = idx == sessionTrail.lastIndex
                    Surface(
                        color = if (isLast) ForestEmeraldContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onJumpToTabLabel(tabLabel) }
                            .minimumInteractiveComponentSize()
                            .desktopPointerHand()
                    ) {
                        Text(
                            text = tabLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isLast) OnForestEmeraldContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    if (!isLast) {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
