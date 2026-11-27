package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Today
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.ExportFileFormat
import com.example.domain.OperationsCalculator
import com.example.domain.ReportExporter
import com.example.model.PlantReportData
import com.example.model.PlantSection
import com.example.model.ReportSectionOption
import com.example.ui.components.DownloadAlsoPdfXlsDialog
import com.example.ui.components.DownloadedReportBanner
import com.example.ui.components.ExportFormatSelectorRow
import com.example.ui.components.LocalWindowWidthDp
import com.example.ui.components.ReportDatePickerModal
import com.example.ui.components.ReportSectionSelectorRow
import com.example.ui.components.SendPlantReportToMessengerDialog
import com.example.ui.components.copyReportTextToClipboard
import com.example.ui.components.describeSelection
import com.example.ui.components.desktopGridColumns
import com.example.ui.components.desktopGridItems
import com.example.ui.components.openExportedFileUri
import com.example.ui.components.resolveIncludedSections
import com.example.ui.components.sendFileToMessenger
import com.example.ui.components.shareExportedFileUri
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.MessengerBlue
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusCriticalTextDark
import com.example.viewmodel.OpsUiState

private enum class ReportPeriodMode(val label: String) {
    DAILY_TODAY("Daily (Today)"),
    SELECTED_DATE("Selected Date"),
    DATE_RANGE("Date Range")
}

private enum class ActiveDatePickerTarget {
    SINGLE_DATE,
    START_DATE,
    END_DATE
}

@Composable
fun ReportsScreen(
    state: OpsUiState.Ready,
    buildReport: (String, String, String, Set<ReportSectionOption>) -> PlantReportData?,
    exportJsonBackup: () -> String?,
    onRestoreJsonBackup: (String) -> Unit,
    onResetSeed: () -> Unit,
    onOpenSelectiveReset: (() -> Unit)? = null,
    canManageBackupsAndResets: Boolean = true,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val todayStr = remember { OperationsCalculator.todayIsoDate() }
    val latestActivityDate = remember(state.batches, state.dispatches, state.schedules, state.orders) {
        (state.batches.map { it.date } +
            state.dispatches.map { it.date } +
            state.schedules.map { it.scheduledDate } +
            state.orders.map { it.orderDate })
            .mapNotNull { str -> OperationsCalculator.parseDateOrNull(str)?.let { it to str } }
            .maxByOrNull { it.first }
            ?.second
    }

    var periodMode by remember { mutableStateOf(ReportPeriodMode.DAILY_TODAY) }
    var selectedSingleDate by remember { mutableStateOf(todayStr) }
    var startDate by remember { mutableStateOf("2026-11-01") }
    var endDate by remember { mutableStateOf(todayStr) }
    var sectionFilter by remember { mutableStateOf("ALL") }
    var selectedExportSections by remember { mutableStateOf<Set<ReportSectionOption>>(emptySet()) }
    var selectedExportFormat by remember { mutableStateOf(ExportFileFormat.PDF) }
    var pickerTarget by remember { mutableStateOf<ActiveDatePickerTarget?>(null) }

    var showRestoreDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showDownloadPrompt by remember { mutableStateOf(false) }
    var showMessengerDialog by remember { mutableStateOf(false) }
    var downloadedFileInfo by remember { mutableStateOf<Triple<Uri, String, String>?>(null) }
    var activeSaveRequest by remember { mutableStateOf<Triple<ByteArray, String, String>?>(null) }
    var pendingSavesQueue by remember { mutableStateOf<List<Triple<ByteArray, String, String>>>(emptyList()) }

    val effectiveStart = when (periodMode) {
        ReportPeriodMode.DAILY_TODAY -> todayStr
        ReportPeriodMode.SELECTED_DATE -> selectedSingleDate
        ReportPeriodMode.DATE_RANGE -> startDate
    }
    val effectiveEnd = when (periodMode) {
        ReportPeriodMode.DAILY_TODAY -> todayStr
        ReportPeriodMode.SELECTED_DATE -> selectedSingleDate
        ReportPeriodMode.DATE_RANGE -> endDate
    }

    val resolvedExportSections = remember(selectedExportSections) {
        selectedExportSections.resolveIncludedSections()
    }
    val report = remember(state, effectiveStart, effectiveEnd, sectionFilter, resolvedExportSections) {
        buildReport(effectiveStart, effectiveEnd, sectionFilter, resolvedExportSections)
    }

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

    val windowWidthDp = LocalWindowWidthDp.current
    val gridColumns = desktopGridColumns(windowWidthDp, maxCols = 2)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Bulacan Plant Summary Reports", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "10K Agri Feeds • Download Daily, Selected Date, or Range Reports (CSV, PDF, XLS, Print & JSON Backup)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Period Mode Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = periodMode == ReportPeriodMode.DAILY_TODAY,
                        onClick = { periodMode = ReportPeriodMode.DAILY_TODAY },
                        leadingIcon = { Icon(Icons.Filled.Today, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Daily ($todayStr)") },
                        modifier = Modifier.testTag("report_mode_daily")
                    )
                    FilterChip(
                        selected = periodMode == ReportPeriodMode.SELECTED_DATE,
                        onClick = {
                            periodMode = ReportPeriodMode.SELECTED_DATE
                            pickerTarget = ActiveDatePickerTarget.SINGLE_DATE
                        },
                        leadingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = {
                            Text(
                                if (periodMode == ReportPeriodMode.SELECTED_DATE) "Date: $selectedSingleDate"
                                else "Selected Date"
                            )
                        },
                        modifier = Modifier.testTag("report_mode_selected_date")
                    )
                    if (latestActivityDate != null && latestActivityDate != todayStr) {
                        FilterChip(
                            selected = periodMode == ReportPeriodMode.SELECTED_DATE && selectedSingleDate == latestActivityDate,
                            onClick = {
                                selectedSingleDate = latestActivityDate
                                periodMode = ReportPeriodMode.SELECTED_DATE
                            },
                            label = { Text("Latest ($latestActivityDate)") },
                            modifier = Modifier.testTag("report_mode_latest_activity")
                        )
                    }
                    FilterChip(
                        selected = periodMode == ReportPeriodMode.DATE_RANGE,
                        onClick = { periodMode = ReportPeriodMode.DATE_RANGE },
                        leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Date Range") },
                        modifier = Modifier.testTag("report_mode_range")
                    )
                }

                when (periodMode) {
                    ReportPeriodMode.DAILY_TODAY -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Daily Summary Date: $todayStr",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                TextButton(
                                    onClick = {
                                        periodMode = ReportPeriodMode.SELECTED_DATE
                                        pickerTarget = ActiveDatePickerTarget.SINGLE_DATE
                                    }
                                ) {
                                    Text("Pick Other Date")
                                }
                            }
                        }
                    }

                    ReportPeriodMode.SELECTED_DATE -> {
                        OutlinedTextField(
                            value = selectedSingleDate,
                            onValueChange = { selectedSingleDate = it },
                            label = { Text("Selected Date (YYYY-MM-DD)") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = { pickerTarget = ActiveDatePickerTarget.SINGLE_DATE },
                                    modifier = Modifier.testTag("report_pick_single_date_button")
                                ) {
                                    Icon(Icons.Filled.CalendarToday, contentDescription = "Open calendar date picker")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("report_single_date_input")
                        )
                    }

                    ReportPeriodMode.DATE_RANGE -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = startDate,
                                onValueChange = { startDate = it },
                                label = { Text("Start Date") },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(
                                        onClick = { pickerTarget = ActiveDatePickerTarget.START_DATE },
                                        modifier = Modifier.testTag("report_pick_start_date_button")
                                    ) {
                                        Icon(Icons.Filled.CalendarToday, contentDescription = "Pick start date")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = endDate,
                                onValueChange = { endDate = it },
                                label = { Text("End Date") },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(
                                        onClick = { pickerTarget = ActiveDatePickerTarget.END_DATE },
                                        modifier = Modifier.testTag("report_pick_end_date_button")
                                    ) {
                                        Icon(Icons.Filled.CalendarToday, contentDescription = "Pick end date")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Location Filter
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
                            label = { Text(if (sec == "ALL") "All Locations" else sec) }
                        )
                    }
                    if (periodMode != ReportPeriodMode.DAILY_TODAY || sectionFilter != "ALL" || selectedExportSections.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                periodMode = ReportPeriodMode.DAILY_TODAY
                                selectedSingleDate = todayStr
                                startDate = "2026-11-01"
                                endDate = todayStr
                                sectionFilter = "ALL"
                                selectedExportSections = emptySet()
                            },
                            modifier = Modifier.testTag("reset_report_filters_button")
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset")
                        }
                    }
                }

                // Selectable Sections & File Format (CSV, PDF, XLS)
                ReportSectionSelectorRow(
                    selectedSections = selectedExportSections,
                    onUpdateSections = { selectedExportSections = it },
                    tagPrefix = "report_export"
                )

                ExportFormatSelectorRow(
                    selectedFormat = selectedExportFormat,
                    onSelectFormat = { selectedExportFormat = it },
                    tagPrefix = "report_format"
                )

                // Export, Share, Print & Backup Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (report != null) {
                                showDownloadPrompt = true
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("download_report_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForestEmerald,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (selectedExportFormat == ExportFileFormat.PDF) Icons.Filled.PictureAsPdf else Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download ${selectedExportFormat.label}")
                    }
                    OutlinedButton(
                        onClick = {
                            if (report != null) {
                                try {
                                    val result = ReportExporter.saveReportToDownloads(context, report, selectedExportFormat)
                                    if (result.uri != null) {
                                        downloadedFileInfo = Triple(result.uri, result.fileName, selectedExportFormat.mimeType)
                                    }
                                    val shareUri = ReportExporter.createShareableReportUri(context, result.fileName, result.bytes)
                                    shareExportedFileUri(context, shareUri, result.fileName, selectedExportFormat.mimeType)
                                } catch (e: Exception) {
                                    onShowToast("${selectedExportFormat.label} share failed: ${e.localizedMessage}")
                                }
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("share_report_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share ${selectedExportFormat.label}")
                    }
                    Button(
                        onClick = {
                            if (report != null) {
                                showMessengerDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MessengerBlue,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("send_messenger_report_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Messenger")
                    }
                    OutlinedButton(
                        onClick = {
                            if (report != null) {
                                ReportExporter.printReportHtml(context, report)
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("print_report_button")
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print")
                    }
                    OutlinedButton(
                        onClick = {
                            if (report != null) {
                                val copied = copyReportTextToClipboard(
                                    context,
                                    "Bulacan Plant Report",
                                    ReportExporter.buildPlainTextReport(report)
                                )
                                onShowToast(
                                    if (copied) "Report text copied to clipboard."
                                    else "Report is too large for clipboard; tap Share or Download."
                                )
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("copy_report_button")
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Text")
                    }
                    OutlinedButton(
                        onClick = {
                            exportJsonBackup()?.let { json ->
                                try {
                                    val fileName = "FeedMill_Ops_Backup.json"
                                    val shareUri = ReportExporter.createShareableReportUri(
                                        context,
                                        fileName,
                                        json.toByteArray(Charsets.UTF_8)
                                    )
                                    val copied = copyReportTextToClipboard(context, "FeedMill Ops JSON Backup", json)
                                    shareExportedFileUri(context, shareUri, fileName, "application/json")
                                    onShowToast(
                                        if (copied) "JSON backup copied & ready to share."
                                        else "JSON backup file ready to share."
                                    )
                                } catch (e: Exception) {
                                    onShowToast("JSON backup export failed: ${e.localizedMessage}")
                                }
                            }
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("JSON Backup")
                    }
                    if (canManageBackupsAndResets) {
                        OutlinedButton(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore JSON")
                        }
                        if (onOpenSelectiveReset != null) {
                            OutlinedButton(
                                onClick = onOpenSelectiveReset,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("reports_selective_reset_button")
                            ) {
                                Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = StatusCriticalTextDark)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choose Reset...", color = StatusCriticalTextDark)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showResetConfirm = true },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = StatusCriticalTextDark)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset Baseline", color = StatusCriticalTextDark)
                            }
                        }
                    }
                }
            }
        }

        downloadedFileInfo?.let { (fileUri, fileName, mimeType) ->
            item {
                DownloadedReportBanner(
                    fileName = fileName,
                    mimeType = mimeType,
                    onOpen = { openExportedFileUri(context, fileUri, mimeType, onShowToast) },
                    onShare = { shareExportedFileUri(context, fileUri, fileName, mimeType) },
                    onSendMessenger = {
                        sendFileToMessenger(
                            context = context,
                            fileUri = fileUri,
                            fileName = fileName,
                            mimeType = mimeType,
                            captionText = "10K Agri Feeds - Bulacan Plant Report ($fileName)",
                            onToast = onShowToast
                        )
                    }
                )
            }
        }

        if (report != null) {
            item {
                val periodHeader = if (report.isDailyReport) {
                    "Daily • ${report.startDate}"
                } else {
                    "${report.startDate} to ${report.endDate}"
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "10K Agri Feeds – Bulacan Plant ($periodHeader)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Selected Scope: ${selectedExportSections.describeSelection()}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (report.includes(ReportSectionOption.INVENTORY)) {
                            Text(
                                "Available Stock: ${OperationsCalculator.formatBags(report.sectionSummaries.sumOf { it.totalAvailableBags })} bags (${OperationsCalculator.formatKg(report.sectionSummaries.sumOf { it.totalAvailableStockKg })} kg) across ${report.productRows.size} SKUs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (report.includes(ReportSectionOption.RECOVERY) || report.includes(ReportSectionOption.DISPATCHES) || report.includes(ReportSectionOption.SCHEDULE)) {
                            val lineParts = buildList {
                                if (report.includes(ReportSectionOption.RECOVERY)) {
                                    add("Produced: ${String.format("%,d", report.totalProducedBags)}b (${OperationsCalculator.formatKg(report.totalRecoveredKg)} kg)")
                                }
                                if (report.includes(ReportSectionOption.DISPATCHES)) {
                                    add("Dispatched: ${OperationsCalculator.formatKg(report.totalDispatchedKg)} kg (${report.dispatches.size} trips)")
                                }
                                if (report.includes(ReportSectionOption.SCHEDULE)) {
                                    add("Schedules: ${report.schedules.size} (${report.scheduledQueueBags}b active)")
                                }
                            }.joinToString(" | ")
                            Text(
                                lineParts,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (report.includes(ReportSectionOption.ORDERS)) {
                            Text(
                                "Period Orders: ${report.orders.size} | Active Open Orders: ${report.activeOpenOrders.size} (${OperationsCalculator.formatKg(report.totalOpenOrdersKg)} kg, Backlog: ${OperationsCalculator.formatKg(report.activeOrderShortfallKg)} kg)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            if (report.includes(ReportSectionOption.INVENTORY) || report.includes(ReportSectionOption.RECOVERY)) {
                desktopGridItems(
                    itemsList = report.sectionSummaries,
                    columns = gridColumns,
                    keyPrefix = "report_sec",
                    itemKey = { it.section.name }
                ) { sec ->
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
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${sec.section.label} (${sec.skuCount} SKUs)", style = MaterialTheme.typography.titleMedium)
                                if (report.includes(ReportSectionOption.INVENTORY)) {
                                    Text(
                                        "${OperationsCalculator.formatBags(sec.totalAvailableBags)} bags avail",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                buildList {
                                    if (report.includes(ReportSectionOption.INVENTORY)) {
                                        add("Avail Stock: ${OperationsCalculator.formatKg(sec.totalAvailableStockKg)} kg")
                                    }
                                    if (report.includes(ReportSectionOption.RECOVERY)) {
                                        add("Produced: ${sec.totalBags} bags (${OperationsCalculator.formatKg(sec.totalRecoveredKg)} kg)")
                                        add("Recovery: ${String.format("%.2f%%", sec.recoveryPct)}")
                                    }
                                }.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (report.includes(ReportSectionOption.RECOVERY)) {
                                HorizontalDivider()
                                if (sec.batches.isEmpty()) {
                                    Text(
                                        if (report.isDailyReport) "No production batches logged on ${report.startDate}."
                                        else "No production batches logged in ${report.startDate} to ${report.endDate}.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    sec.batches.forEach { b ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${b.date} • ${b.productName}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                                Text("${b.bagsProduced} bags x ${b.bagWeightKg}kg • ${b.remarks}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text(
                                                "${OperationsCalculator.formatKg(b.actualRecoveredKg)} kg",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (report.includes(ReportSectionOption.DISPATCHES)) {
                item {
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
                            Text("Dispatches & Fleet Trips (${report.dispatches.size})", style = MaterialTheme.typography.titleMedium)
                            HorizontalDivider()
                            if (report.dispatches.isEmpty()) {
                                Text("No dispatches logged in this period.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                report.dispatches.forEach { d ->
                                    Text(
                                        text = "${d.date} • DR ${d.drReference} • ${d.customer} • ${d.productName} (${d.bagsDispatched}b, ${OperationsCalculator.formatKg(d.effectiveDispatchedWeightKg())} kg) [${d.resolvedStatusLabel()}]",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (report.includes(ReportSectionOption.SCHEDULE)) {
                item {
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
                            Text("Production Schedule Runs (${report.schedules.size})", style = MaterialTheme.typography.titleMedium)
                            HorizontalDivider()
                            if (report.schedules.isEmpty()) {
                                Text("No scheduled production runs in this period.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                report.schedules.forEach { s ->
                                    Text(
                                        text = "${s.scheduledDate} • [${s.section}] ${s.productName} (${s.targetBags}b) • ${s.priority} • ${s.status}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (report.includes(ReportSectionOption.ORDERS)) {
                item {
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
                            Text("Customer Orders • Period (${report.orders.size}) & Active Open (${report.activeOpenOrders.size})", style = MaterialTheme.typography.titleMedium)
                            HorizontalDivider()
                            if (report.orders.isEmpty()) {
                                Text("No orders placed or due in selected period.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text("Orders in Period", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                report.orders.forEach { o ->
                                    Text(
                                        text = "${o.orderCode} • ${o.customer} • ${o.productName} (${OperationsCalculator.formatKg(o.totalWeightKg)} kg) • Due ${o.dueDate} [${o.status}]",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            if (report.activeOpenOrders.isNotEmpty()) {
                                HorizontalDivider()
                                Text("Live Active Open Orders", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                report.activeOpenOrders.forEach { o ->
                                    val remKg = kotlin.math.max(0.0, o.totalWeightKg - o.dispatchedWeightKg)
                                    Text(
                                        text = "${o.orderCode} • ${o.customer} • ${o.productName} (Rem ${OperationsCalculator.formatKg(remKg)} kg) • Due ${o.dueDate} [${o.status}]",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (report.includes(ReportSectionOption.INVENTORY)) {
                item {
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
                            Text("Per-Product Inventory & Balance (${report.productRows.size})", style = MaterialTheme.typography.titleMedium)
                            HorizontalDivider()
                            report.productRows.forEach { r ->
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("${r.productName} [${r.section}]", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    Text(
                                        "Avail: ${OperationsCalculator.formatKg(r.availableStockKg)}kg | Prod: ${r.producedBags}b (${OperationsCalculator.formatKg(r.recoveredKg)}kg) | Disp: ${OperationsCalculator.formatKg(r.dispatchedKg)}kg | OpenOrd: ${OperationsCalculator.formatKg(r.openOrdersKg)}kg",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    pickerTarget?.let { target ->
        val initialIso = when (target) {
            ActiveDatePickerTarget.SINGLE_DATE -> selectedSingleDate
            ActiveDatePickerTarget.START_DATE -> startDate
            ActiveDatePickerTarget.END_DATE -> endDate
        }
        val title = when (target) {
            ActiveDatePickerTarget.SINGLE_DATE -> "Select Summary Report Date"
            ActiveDatePickerTarget.START_DATE -> "Select Report Start Date"
            ActiveDatePickerTarget.END_DATE -> "Select Report End Date"
        }
        ReportDatePickerModal(
            initialIsoDate = initialIso,
            title = title,
            onDismiss = { pickerTarget = null },
            onConfirmDate = { picked ->
                when (target) {
                    ActiveDatePickerTarget.SINGLE_DATE -> {
                        selectedSingleDate = picked
                        periodMode = ReportPeriodMode.SELECTED_DATE
                    }
                    ActiveDatePickerTarget.START_DATE -> {
                        startDate = picked
                        periodMode = ReportPeriodMode.DATE_RANGE
                    }
                    ActiveDatePickerTarget.END_DATE -> {
                        endDate = picked
                        periodMode = ReportPeriodMode.DATE_RANGE
                    }
                }
                pickerTarget = null
            }
        )
    }

    if (showRestoreDialog) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore JSON Backup") },
            text = {
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    label = { Text("Paste FeedMill Ops JSON backup") },
                    minLines = 6,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (jsonInput.isNotBlank()) {
                        onRestoreJsonBackup(jsonInput)
                        showRestoreDialog = false
                    }
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset to Beginning Inventory?") },
            text = { Text("This replaces current SKUs, dispatches, orders, batches, and schedules with the default Beginning Inventory records for SAN ROQUE, PANDACAQUI, LA UNION QUAIL FEEDS, and SELECTA.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetSeed()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusCritical,
                        contentColor = Color.White
                    )
                ) { Text("Reset Data") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showDownloadPrompt && report != null) {
        DownloadAlsoPdfXlsDialog(
            title = "Download Plant Report",
            initialFormat = selectedExportFormat,
            onDismiss = { showDownloadPrompt = false },
            onConfirmFormats = { chosenFormats ->
                showDownloadPrompt = false
                chosenFormats.lastOrNull()?.let { selectedExportFormat = it }
                val savedLabels = mutableListOf<String>()
                val queuedFallback = mutableListOf<Triple<ByteArray, String, String>>()
                chosenFormats.forEach { fmt ->
                    try {
                        val result = ReportExporter.saveReportToDownloads(context, report, fmt)
                        if (result.uri != null) {
                            downloadedFileInfo = Triple(result.uri, result.fileName, fmt.mimeType)
                            savedLabels.add(fmt.label)
                        } else {
                            queuedFallback.add(Triple(result.bytes, result.fileName, fmt.mimeType))
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

    if (showMessengerDialog && report != null) {
        SendPlantReportToMessengerDialog(
            report = report,
            initialFormat = selectedExportFormat,
            onDismiss = { showMessengerDialog = false },
            onShowToast = onShowToast
        )
    }
}
