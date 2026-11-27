package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.ExportFileFormat
import com.example.domain.OperationsCalculator
import com.example.domain.ReportExporter
import com.example.model.DailyOrderLogRow
import com.example.model.DailyPlantLog
import com.example.model.DailySkuLogRow
import com.example.model.OrderStatus
import com.example.model.PlantSection
import com.example.model.SkuEntity
import com.example.ui.components.DownloadAlsoPdfXlsDialog
import com.example.ui.components.DownloadedReportBanner
import com.example.ui.components.ExportFormatSelectorRow
import com.example.ui.components.IndustrialKpiCard
import com.example.ui.components.LocalIsDesktopLayout
import com.example.ui.components.LocalWindowWidthDp
import com.example.ui.components.ReportDatePickerModal
import com.example.ui.components.SendDailyLogToMessengerDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.desktopGridColumns
import com.example.ui.components.desktopGridItems
import com.example.ui.components.openExportedFileUri
import com.example.ui.components.sendFileToMessenger
import com.example.ui.components.shareExportedFileUri
import com.example.ui.theme.DarkChartTrack
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.GrainAmber
import com.example.ui.theme.GrainAmberBright
import com.example.ui.theme.MessengerBlue
import com.example.ui.theme.OnStatusCriticalContainer
import com.example.ui.theme.OnStatusHealthyContainer
import com.example.ui.theme.OnStatusInProgressContainer
import com.example.ui.theme.OnStatusWatchContainer
import com.example.ui.theme.StatusCriticalContainer
import com.example.ui.theme.StatusCriticalTextDark
import com.example.ui.theme.StatusHealthyContainer
import com.example.ui.theme.StatusHealthyTextDark
import com.example.ui.theme.StatusInProgressContainer
import com.example.ui.theme.StatusWatchContainer
import com.example.ui.theme.StatusWatchTextDark
import com.example.viewmodel.OpsUiState
import kotlin.math.max

@Composable
fun DailyLogScreen(
    state: OpsUiState.Ready,
    onOpenDispatchForSku: (SkuEntity) -> Unit,
    onOpenOrdersTab: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val todayStr = remember { OperationsCalculator.todayIsoDate() }

    val dailyLogs = remember(state.skus, state.batches, state.dispatches, state.orders, todayStr) {
        OperationsCalculator.computeDailyLogs(
            skus = state.skus,
            batches = state.batches,
            dispatches = state.dispatches,
            orders = state.orders,
            todayIso = todayStr
        )
    }

    var selectedDate by remember(todayStr) { mutableStateOf(todayStr) }
    var sectionFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var activityOnly by remember { mutableStateOf(false) }
    var logView by remember { mutableIntStateOf(0) } // 0 = All, 1 = Inventory, 2 = Orders
    var selectedExportFormat by remember { mutableStateOf(ExportFileFormat.CSV) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDownloadPrompt by remember { mutableStateOf(false) }
    var showMessengerDialog by remember { mutableStateOf(false) }
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

    val activeLog: DailyPlantLog = remember(dailyLogs, selectedDate, state, todayStr) {
        dailyLogs.firstOrNull { it.date == selectedDate }
            ?: OperationsCalculator.computeDailyLogs(
                skus = state.skus,
                batches = state.batches,
                dispatches = state.dispatches,
                orders = state.orders,
                todayIso = selectedDate
            ).firstOrNull { it.date == selectedDate }
            ?: dailyLogs.first()
    }

    val sectionByProductLower = remember(state.skus) {
        state.skus.associate { it.productName.lowercase() to it.section }
    }

    val filteredSkuRows = remember(activeLog, sectionFilter, searchQuery, activityOnly) {
        val q = searchQuery.trim()
        activeLog.skuRows.filter { row ->
            val matchesSec = sectionFilter == "ALL" || row.sku.section == sectionFilter
            val matchesQuery = q.isBlank() ||
                row.sku.productName.contains(q, ignoreCase = true) ||
                row.sku.section.contains(q, ignoreCase = true)
            val hasDayActivity = row.editedOnDay ||
                row.producedDayKg > 0.0 ||
                row.dispatchedDayKg > 0.0 ||
                row.returnedDayKg > 0.0
            matchesSec && matchesQuery && (!activityOnly || hasDayActivity)
        }
    }

    val filteredOrderRows = remember(activeLog, sectionFilter, sectionByProductLower, searchQuery, activityOnly) {
        val q = searchQuery.trim()
        activeLog.orderRows.filter { row ->
            val sec = sectionByProductLower[row.order.productName.lowercase()]
            val matchesSec = sectionFilter == "ALL" || sec == null || sec == sectionFilter
            val matchesQuery = q.isBlank() ||
                row.order.customer.contains(q, ignoreCase = true) ||
                row.order.orderCode.contains(q, ignoreCase = true) ||
                row.order.productName.contains(q, ignoreCase = true)
            val hasMovement = row.editedOnDay || row.placedOnDay || row.dispatchedOnDayKg > 0.0
            matchesSec && matchesQuery && (!activityOnly || hasMovement)
        }
    }

    val isDesktop = LocalIsDesktopLayout.current
    val windowWidthDp = LocalWindowWidthDp.current
    val gridColumns = desktopGridColumns(windowWidthDp, maxCols = 2)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header & Export (CSV, PDF, XLS)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto Daily Log",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Automatically captures daily inventory balances, production, dispatches, edits & customer orders",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { showMessengerDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MessengerBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("daily_log_messenger_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Messenger")
                        }
                        Button(
                            onClick = { showDownloadPrompt = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForestEmerald,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("daily_log_export_button")
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export ${selectedExportFormat.label}")
                        }
                    }
                }
                ExportFormatSelectorRow(
                    selectedFormat = selectedExportFormat,
                    onSelectFormat = { selectedExportFormat = it },
                    tagPrefix = "daily_log_format"
                )
            }
        }

        // Auto-Log Status Banner
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_log_auto_status_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoMode,
                        contentDescription = "Automatic daily logging active",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (activeLog.date == todayStr) {
                                "Auto-Logging Today (${activeLog.date}) • Live Firestore Sync"
                            } else {
                                "Auto-Logged Snapshot • ${activeLog.date}"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${activeLog.skusEditedCount} SKU updates • ${activeLog.ordersPlacedCount} placed / ${activeLog.ordersEditedCount} edited / ${activeLog.ordersDueCount} due orders • ${activeLog.linkedDispatchOrdersCount} dispatched POs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        downloadedFileInfo?.let { (uri, fileName, mimeType) ->
            item {
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
                            captionText = "10K Agri Feeds - Auto Daily Log (${activeLog.date})",
                            onToast = onShowToast
                        )
                    }
                )
            }
        }

        // Date Carousel + Calendar Date Picker
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Logged Dates (${dailyLogs.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("daily_log_pick_date_button")
                    ) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = "Pick specific date", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Date ($selectedDate)", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dailyLogs.take(21).forEach { dayLog ->
                        val isSelected = selectedDate == dayLog.date
                        val activityCount = dayLog.skusEditedCount +
                            dayLog.orderRows.size +
                            dayLog.skuRows.count { it.producedDayKg > 0.0 || it.dispatchedDayKg > 0.0 }
                        val chipLabel = if (dayLog.date == todayStr) {
                            "Today (${dayLog.date})"
                        } else if (activityCount > 0) {
                            "${dayLog.date} ($activityCount)"
                        } else {
                            dayLog.date
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDate = dayLog.date },
                            leadingIcon = if (dayLog.date == todayStr) {
                                { Icon(Icons.Filled.Today, contentDescription = null, modifier = Modifier.size(15.dp)) }
                            } else null,
                            label = { Text(chipLabel, maxLines = 1) },
                            modifier = Modifier.testTag("daily_log_date_${dayLog.date}")
                        )
                    }
                }
            }
        }

        // Daily Summary KPI Strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isDesktop) Modifier.height(IntrinsicSize.Max) else Modifier.horizontalScroll(rememberScrollState())),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IndustrialKpiCard(
                    title = "CLOSING STOCK",
                    primaryValue = "${OperationsCalculator.formatKg(activeLog.totalClosingStockKg)} kg",
                    secondaryValue = "${OperationsCalculator.formatBags(activeLog.totalClosingBags)} bags balance",
                    icon = Icons.Filled.Inventory2,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(185.dp)
                )
                IndustrialKpiCard(
                    title = "DAY PRODUCED",
                    primaryValue = "${OperationsCalculator.formatKg(activeLog.totalProducedDayKg)} kg",
                    secondaryValue = "${activeLog.skuRows.sumOf { it.producedDayBags }} bags recovered",
                    icon = Icons.Filled.Factory,
                    accentColor = StatusHealthyTextDark,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(175.dp)
                )
                IndustrialKpiCard(
                    title = "DAY DISPATCHED",
                    primaryValue = "${OperationsCalculator.formatKg(activeLog.totalDispatchedDayKg)} kg",
                    secondaryValue = "${activeLog.skuRows.sumOf { it.dispatchedDayBags }} bags delivered",
                    icon = Icons.Filled.LocalShipping,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(180.dp)
                )
                IndustrialKpiCard(
                    title = "DAILY ORDERS LOG",
                    primaryValue = "${activeLog.orderRows.size} orders",
                    secondaryValue = "${OperationsCalculator.formatKg(activeLog.totalOpenOrdersSnapshotKg)} kg open demand",
                    icon = Icons.Filled.ShoppingCart,
                    accentColor = GrainAmberBright,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(190.dp)
                )
            }
        }

        // Search + Location & Activity Filters + View Toggle
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search daily log") },
                    placeholder = { Text("Search SKU, customer, or PO code...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_log_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (listOf("ALL") + PlantSection.entries.map { it.label }).forEach { sec ->
                        FilterChip(
                            selected = sectionFilter == sec,
                            onClick = { sectionFilter = sec },
                            label = { Text(if (sec == "ALL") "All Locations" else sec) },
                            modifier = Modifier.testTag("daily_log_loc_${sec.lowercase().replace(" ", "_")}")
                        )
                    }
                    FilterChip(
                        selected = activityOnly,
                        onClick = { activityOnly = !activityOnly },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        label = { Text("Edits & Activity Only") },
                        modifier = Modifier.testTag("daily_log_activity_only_chip")
                    )
                    if (sectionFilter != "ALL" || searchQuery.isNotBlank() || activityOnly) {
                        TextButton(
                            onClick = {
                                sectionFilter = "ALL"
                                searchQuery = ""
                                activityOnly = false
                            },
                            modifier = Modifier.testTag("daily_log_reset_filters_button")
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset")
                        }
                    }
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = logView == 0,
                        onClick = { logView = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        modifier = Modifier.testTag("daily_log_tab_all")
                    ) {
                        Text("All (${filteredSkuRows.size + filteredOrderRows.size})", maxLines = 1)
                    }
                    SegmentedButton(
                        selected = logView == 1,
                        onClick = { logView = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        modifier = Modifier.testTag("daily_log_tab_inventory")
                    ) {
                        Text("Inventory (${filteredSkuRows.size})", maxLines = 1)
                    }
                    SegmentedButton(
                        selected = logView == 2,
                        onClick = { logView = 2 },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        modifier = Modifier.testTag("daily_log_tab_orders")
                    ) {
                        Text("Orders (${filteredOrderRows.size})", maxLines = 1)
                    }
                }
            }
        }

        // Daily Orders Section (Shown first or when All / Orders selected)
        if (logView == 0 || logView == 2) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Customer Orders Log (${filteredOrderRows.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = onOpenOrdersTab) {
                        Text("Open Orders Tab")
                    }
                }
            }

            if (filteredOrderRows.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No customer order placements, edits, due dates, or linked dispatches logged on ${activeLog.date}. Any new order, edit, or dispatch automatically logs here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                desktopGridItems(
                    itemsList = filteredOrderRows,
                    columns = gridColumns,
                    keyPrefix = "daily_ord_${activeLog.date}",
                    itemKey = { it.order.id.ifBlank { it.order.orderCode } }
                ) { row ->
                    DailyOrderLogCard(row = row)
                }
            }
        }

        // Daily Inventory Section
        if (logView == 0 || logView == 1) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Inventory Log (${filteredSkuRows.size} SKUs)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "As of ${activeLog.date}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (filteredSkuRows.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (activityOnly) {
                                "No SKUs had production, dispatch, returns, or inventory edits on ${activeLog.date}. Turn off 'Edits & Activity Only' to view all closing stock balances."
                            } else {
                                "No inventory items match the selected filter."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                desktopGridItems(
                    itemsList = filteredSkuRows,
                    columns = gridColumns,
                    keyPrefix = "daily_sku_${activeLog.date}",
                    itemKey = { it.sku.id.ifBlank { "${it.sku.section}_${it.sku.productName}" } }
                ) { row ->
                    DailySkuLogCard(
                        row = row,
                        onDispatchSku = { onOpenDispatchForSku(row.sku) }
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        ReportDatePickerModal(
            initialIsoDate = selectedDate,
            title = "Select Daily Log Date",
            onDismiss = { showDatePicker = false },
            onConfirmDate = { picked ->
                selectedDate = picked
                showDatePicker = false
            }
        )
    }

    if (showDownloadPrompt) {
        DownloadAlsoPdfXlsDialog(
            title = "Download Daily Log (${activeLog.date})",
            initialFormat = selectedExportFormat,
            onDismiss = { showDownloadPrompt = false },
            onConfirmFormats = { chosenFormats ->
                showDownloadPrompt = false
                chosenFormats.lastOrNull()?.let { selectedExportFormat = it }
                val savedLabels = mutableListOf<String>()
                val queuedFallback = mutableListOf<Triple<ByteArray, String, String>>()
                chosenFormats.forEach { fmt ->
                    try {
                        val res = ReportExporter.saveDailyLogToDownloads(
                            context = context,
                            dayLog = activeLog,
                            sectionFilter = sectionFilter,
                            format = fmt
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
            }
        )
    }

    if (showMessengerDialog) {
        SendDailyLogToMessengerDialog(
            dayLog = activeLog,
            sectionFilter = sectionFilter,
            initialFormat = selectedExportFormat,
            onDismiss = { showMessengerDialog = false },
            onShowToast = onShowToast
        )
    }
}

@Composable
private fun DailySkuLogCard(
    row: DailySkuLogRow,
    onDispatchSku: () -> Unit
) {
    val hasMovement = row.producedDayKg > 0.0 || row.dispatchedDayKg > 0.0 || row.returnedDayKg > 0.0 || row.editedOnDay
    val borderColor = when {
        row.closingStockKg <= 0.0 -> StatusCriticalTextDark
        row.editedOnDay -> ForestEmerald
        hasMovement -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (row.editedOnDay || row.closingStockKg <= 0.0) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.sku.productName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${row.sku.section} • ${row.sku.standardBagWeightKg} kg/bag",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (row.editedOnDay) {
                        val editLabel = if (row.editedTimeLabel.isNotBlank()) "Updated ${row.editedTimeLabel}" else "Updated"
                        StatusPill(editLabel, StatusInProgressContainer, OnStatusInProgressContainer)
                    }
                    if (row.closingStockKg <= 0.0) {
                        StatusPill("OUT OF STOCK", StatusCriticalContainer, OnStatusCriticalContainer)
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Closing Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${OperationsCalculator.formatKg(row.closingStockKg)} kg",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (row.closingStockKg <= 0.0) StatusCriticalTextDark else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${OperationsCalculator.formatBags(row.closingBags)} bags",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Open Orders: ${OperationsCalculator.formatKg(row.openOrdersSnapshotKg)} kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (row.producedDayKg > 0.0 || row.producedDayBags > 0L) {
                    StatusPill(
                        label = "+${OperationsCalculator.formatKg(row.producedDayKg)} kg (${row.producedDayBags}b) Prod",
                        bgColor = StatusHealthyContainer,
                        textColor = OnStatusHealthyContainer
                    )
                }
                if (row.dispatchedDayKg > 0.0 || row.dispatchedDayBags > 0L) {
                    StatusPill(
                        label = "-${OperationsCalculator.formatKg(row.dispatchedDayKg)} kg (${row.dispatchedDayBags}b) Disp",
                        bgColor = StatusWatchContainer,
                        textColor = OnStatusWatchContainer
                    )
                }
                if (row.returnedDayKg > 0.0) {
                    StatusPill(
                        label = "Returned ${OperationsCalculator.formatKg(row.returnedDayKg)} kg",
                        bgColor = StatusCriticalContainer,
                        textColor = OnStatusCriticalContainer
                    )
                }
                if (row.producedDayKg == 0.0 && row.dispatchedDayKg == 0.0 && row.returnedDayKg == 0.0) {
                    Text(
                        text = "No stock movement on this date",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onDispatchSku,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dispatch")
                }
            }
        }
    }
}

@Composable
private fun DailyOrderLogCard(row: DailyOrderLogRow) {
    val ord = row.order
    val remainingKg = max(0.0, ord.totalWeightKg - ord.dispatchedWeightKg)
    val progress = if (ord.totalWeightKg > 0.0) (ord.dispatchedWeightKg / ord.totalWeightKg).toFloat().coerceIn(0f, 1f) else 0f
    val (bg, fg) = when (OrderStatus.fromRaw(ord.status)) {
        OrderStatus.OPEN -> StatusWatchContainer to OnStatusWatchContainer
        OrderStatus.PARTIALLY_DISPATCHED -> StatusInProgressContainer to OnStatusInProgressContainer
        OrderStatus.FULFILLED -> StatusHealthyContainer to OnStatusHealthyContainer
        OrderStatus.CANCELLED -> StatusCriticalContainer to OnStatusCriticalContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (ord.priority) 1.5.dp else 1.dp,
                color = if (ord.priority) GrainAmber else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (ord.priority) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Priority order",
                                tint = GrainAmber,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = ord.customer,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = ord.productName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = ord.orderCode,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StatusPill(ord.status, bg, fg)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${ord.orderedBags} bags (${OperationsCalculator.formatKg(ord.totalWeightKg)} kg)",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "Rem: ${OperationsCalculator.formatKg(remainingKg)} kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = if (progress >= 0.99f) StatusHealthyTextDark else MaterialTheme.colorScheme.primary,
                    trackColor = DarkChartTrack
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (row.placedOnDay) {
                    StatusPill("Placed", StatusInProgressContainer, OnStatusInProgressContainer)
                }
                if (row.dueOnDay) {
                    StatusPill("Due Today", StatusWatchContainer, OnStatusWatchContainer)
                }
                if (row.editedOnDay) {
                    val t = if (row.editedTimeLabel.isNotBlank()) "Edited ${row.editedTimeLabel}" else "Edited"
                    StatusPill(t, StatusInProgressContainer, OnStatusInProgressContainer)
                }
                if (row.dispatchedOnDayKg > 0.0 || row.dispatchedOnDayBags > 0L) {
                    StatusPill(
                        label = "Dispatched ${OperationsCalculator.formatKg(row.dispatchedOnDayKg)} kg (${row.dispatchedOnDayBags}b)",
                        bgColor = StatusHealthyContainer,
                        textColor = OnStatusHealthyContainer
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ordered: ${ord.orderDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Due: ${ord.dueDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
