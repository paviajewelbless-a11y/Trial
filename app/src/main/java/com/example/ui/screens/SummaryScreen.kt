package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.OpsTab
import com.example.domain.ExportFileFormat
import com.example.domain.OperationsCalculator
import com.example.domain.ReportExporter
import com.example.model.DispatchStatus
import com.example.model.OrderStatus
import com.example.model.PlantReportData
import com.example.model.PlantSection
import com.example.model.RawMaterialRiskStatus
import com.example.model.ReportSectionOption
import com.example.model.ScheduleStatus
import com.example.model.StockHealthStatus
import com.example.ui.components.DailyFlowBarChartCard
import com.example.ui.components.DailyFlowChartPoint
import com.example.ui.components.DonutSliceItem
import com.example.ui.components.DonutSplitChartCard
import com.example.ui.components.DownloadAlsoPdfXlsDialog
import com.example.ui.components.DownloadedReportBanner
import com.example.ui.components.ExportFormatSelectorRow
import com.example.ui.components.LocalIsDesktopLayout
import com.example.ui.components.RecoveryTrendChartCard
import com.example.ui.components.ReportDatePickerModal
import com.example.ui.components.ReportSectionSelectorRow
import com.example.ui.components.SectionSupplyDemandChartCard
import com.example.ui.components.SectionSupplyDemandRow
import com.example.ui.components.SendPlantReportToMessengerDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.copyReportTextToClipboard
import com.example.ui.components.desktopPointerHand
import com.example.ui.components.openExportedFileUri
import com.example.ui.components.resolveIncludedSections
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
import com.example.ui.theme.OnSlateTealContainer
import com.example.ui.theme.SlateTealContainer
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusCriticalContainer
import com.example.ui.theme.StatusCriticalTextDark
import com.example.ui.theme.StatusHealthyContainer
import com.example.ui.theme.StatusHealthyTextDark
import com.example.ui.theme.StatusInProgressContainer
import com.example.ui.theme.StatusWatchContainer
import com.example.ui.theme.StatusWatchTextDark
import com.example.viewmodel.OpsUiState
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

private enum class SummaryViewTab(val label: String) {
    OVERVIEW("Overview"),
    ANALYTICS("Analytics"),
    LOCATIONS("Locations"),
    HIGHLIGHTS("Highlights")
}

@Composable
fun SummaryScreen(
    state: OpsUiState.Ready,
    onNavigateToTab: (OpsTab) -> Unit,
    onNavigateToRawMaterials: () -> Unit = {},
    buildReport: (String, String, String, Set<ReportSectionOption>) -> PlantReportData?,
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

    var selectedViewTab by remember { mutableStateOf(SummaryViewTab.OVERVIEW) }
    var showFilters by remember { mutableStateOf(false) }
    var showReportOptions by remember { mutableStateOf(false) }
    var sectionFilter by remember { mutableStateOf("ALL") }
    var useSelectedDateScope by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(todayStr) }
    var selectedExportSections by remember { mutableStateOf<Set<ReportSectionOption>>(emptySet()) }
    var selectedExportFormat by remember { mutableStateOf(ExportFileFormat.PDF) }
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

    val sectionByProductLower = remember(state.skus) {
        state.skus.associate { it.productName.lowercase() to it.section }
    }
    val targetRecByProductLower = remember(state.skus) {
        state.skus.associate { it.productName.lowercase() to it.targetRecoveryPct }
    }
    val priorityCustomerLower = remember(state.customers) {
        state.customers.filter { it.priority }.map { it.name.trim().lowercase() }.toSet()
    }

    val filteredSummaries = remember(state.summaries, sectionFilter) {
        state.summaries.filter { sectionFilter == "ALL" || it.sku.section == sectionFilter }
    }
    val filteredBatches = remember(state.batches, sectionFilter, useSelectedDateScope, selectedDate) {
        state.batches.filter {
            (sectionFilter == "ALL" || it.section == sectionFilter) &&
                (!useSelectedDateScope || it.date == selectedDate)
        }
    }
    val filteredSchedules = remember(state.schedules, sectionFilter, useSelectedDateScope, selectedDate) {
        state.schedules.filter {
            (sectionFilter == "ALL" || it.section == sectionFilter) &&
                (!useSelectedDateScope || it.scheduledDate == selectedDate)
        }
    }
    val filteredOrders = remember(state.orders, sectionFilter, sectionByProductLower, useSelectedDateScope, selectedDate) {
        state.orders.filter {
            (sectionFilter == "ALL" || sectionByProductLower[it.productName.lowercase()] == sectionFilter) &&
                (!useSelectedDateScope || it.orderDate == selectedDate || it.dueDate == selectedDate)
        }
    }
    val filteredDispatches = remember(state.dispatches, sectionFilter, sectionByProductLower, useSelectedDateScope, selectedDate) {
        state.dispatches.filter {
            (sectionFilter == "ALL" || sectionByProductLower[it.productName.lowercase()] == sectionFilter) &&
                (!useSelectedDateScope || it.date == selectedDate)
        }
    }

    val resolvedExportSections = remember(selectedExportSections) {
        selectedExportSections.resolveIncludedSections()
    }
    val dailyReportPreview = remember(state, selectedDate, sectionFilter, resolvedExportSections) {
        buildReport(selectedDate, selectedDate, sectionFilter, resolvedExportSections)
    }

    // Inventory Metrics
    val totalAvailKg = filteredSummaries.sumOf { max(0.0, it.availableStockKg) }
    val totalAvailBags = filteredSummaries.sumOf { max(0.0, it.availableBags) }
    val healthyCount = filteredSummaries.count { it.status == StockHealthStatus.HEALTHY }
    val watchCount = filteredSummaries.count { it.status == StockHealthStatus.LOW_WATCH }
    val criticalCount = filteredSummaries.count { it.status == StockHealthStatus.CRITICAL }
    val needingProdCount = filteredSummaries.count { it.netProductionNeededKg > 0.0 }
    val totalSuggestedBags = filteredSummaries.sumOf { it.suggestedBags }

    // Production & Recovery Metrics
    val totalProducedBags = filteredBatches.sumOf { it.bagsProduced }
    val totalExpectedKg = filteredBatches.sumOf { it.expectedWeightKg }
    val totalRecoveredKg = filteredBatches.sumOf { it.actualRecoveredKg }
    val avgRecoveryPct = if (totalExpectedKg > 0.0) (totalRecoveredKg / totalExpectedKg) * 100.0 else 100.0
    val flaggedLowBatches = filteredBatches.filter {
        val target = targetRecByProductLower[it.productName.lowercase()] ?: 98.5
        it.recoveryPct < target
    }

    // Order Metrics
    val activeOrders = filteredOrders.filter {
        it.status == OrderStatus.OPEN.label || it.status == OrderStatus.PARTIALLY_DISPATCHED.label
    }
    val fulfilledOrderCount = filteredOrders.count { it.status == OrderStatus.FULFILLED.label }
    val cancelledOrderCount = filteredOrders.count { it.status == OrderStatus.CANCELLED.label }
    val priorityActiveOrders = activeOrders.filter { it.priority }
    val activeOrderedTotalKg = activeOrders.sumOf { it.totalWeightKg }
    val activeDispatchedTotalKg = activeOrders.sumOf { it.dispatchedWeightKg }
    val activeRemainingTotalKg = activeOrders.sumOf { max(0.0, it.totalWeightKg - it.dispatchedWeightKg) }
    val activeFulfillmentFrac = if (activeOrderedTotalKg > 0.0) {
        (activeDispatchedTotalKg / activeOrderedTotalKg).toFloat().coerceIn(0f, 1f)
    } else 1f
    val availByProdLower = filteredSummaries.associate { it.sku.productName.lowercase() to max(0.0, it.availableStockKg) }
    val activeOrderShortfallKg = activeOrders
        .groupBy { it.productName.lowercase() }
        .entries
        .sumOf { (prodLower, list) ->
            val rem = list.sumOf { max(0.0, it.totalWeightKg - it.dispatchedWeightKg) }
            val avail = availByProdLower[prodLower] ?: 0.0
            max(0.0, rem - avail)
        }
    val stockReadyOrderKg = max(0.0, activeRemainingTotalKg - activeOrderShortfallKg)

    // Dispatch Metrics
    val deliveredDispatches = filteredDispatches.filter { it.resolvedStatusLabel() == DispatchStatus.DELIVERED.label }
    val returnedDispatches = filteredDispatches.filter { it.resolvedStatusLabel() == DispatchStatus.RETURNED.label }
    val heldDispatches = filteredDispatches.filter { it.resolvedStatusLabel() == DispatchStatus.HELD.label }
    val totalNetDeliveredKg = filteredDispatches.sumOf { it.effectiveDispatchedWeightKg() }
    val totalGrossDispatchedKg = filteredDispatches.sumOf { it.totalWeightKg }
    val totalReturnedBags = returnedDispatches.sumOf { it.returnedBags }
    val totalReturnedKg = returnedDispatches.sumOf { it.returnedWeightKg }
    val netDeliveryRatePct = if (totalGrossDispatchedKg > 0.0) {
        (totalNetDeliveredKg / totalGrossDispatchedKg) * 100.0
    } else 100.0
    val topReturnReasons = returnedDispatches
        .map { it.returnReason.ifBlank { "Unspecified" } }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(3)

    // Schedule Metrics
    val scheduledRuns = filteredSchedules.filter { it.status == ScheduleStatus.SCHEDULED.label }
    val inProgressRuns = filteredSchedules.filter { it.status == ScheduleStatus.IN_PROGRESS.label }
    val completedRunsCount = filteredSchedules.count { it.status == ScheduleStatus.COMPLETED.label }
    val activeRuns = scheduledRuns + inProgressRuns
    val activeQueueBags = activeRuns.sumOf { it.targetBags }

    // Section Performance Breakdown
    val sectionBreakdowns = remember(filteredSummaries, filteredBatches, filteredDispatches, activeOrders, sectionFilter) {
        PlantSection.entries
            .filter { sectionFilter == "ALL" || it.label == sectionFilter }
            .map { sec ->
                val secSums = filteredSummaries.filter { it.sku.section == sec.label }
                val secBatches = filteredBatches.filter { it.section == sec.label }
                val secProdNames = secSums.map { it.sku.productName.lowercase() }.toSet()
                val secDispatches = filteredDispatches.filter { it.productName.lowercase() in secProdNames }
                val secOrders = activeOrders.filter { it.productName.lowercase() in secProdNames }
                val exp = secBatches.sumOf { it.expectedWeightKg }
                val rec = secBatches.sumOf { it.actualRecoveredKg }
                SectionStatRow(
                    section = sec,
                    skuCount = secSums.size,
                    criticalCount = secSums.count { it.status == StockHealthStatus.CRITICAL },
                    availKg = secSums.sumOf { max(0.0, it.availableStockKg) },
                    availBags = secSums.sumOf { max(0.0, it.availableBags) },
                    producedBags = secBatches.sumOf { it.bagsProduced },
                    recoveryPct = if (exp > 0.0) (rec / exp) * 100.0 else 100.0,
                    dispatchedKg = secDispatches.sumOf { it.effectiveDispatchedWeightKg() },
                    openOrdersKg = secOrders.sumOf { max(0.0, it.totalWeightKg - it.dispatchedWeightKg) }
                )
            }
    }
    val maxSectionAvailKg = sectionBreakdowns.maxOfOrNull { it.availKg }?.coerceAtLeast(1.0) ?: 1.0

    // Top Critical SKUs, Urgent Raw Materials, Top Customers, Top Dispatched SKUs
    val topCriticalSkus = filteredSummaries
        .filter { it.status == StockHealthStatus.CRITICAL }
        .sortedByDescending { it.netProductionNeededKg }
        .take(3)
    val urgentRawMaterials = remember(state.rawMaterialProjections) {
        state.rawMaterialProjections.filter {
            it.status == RawMaterialRiskStatus.CRITICAL || it.status == RawMaterialRiskStatus.LOW_WATCH
        }
    }
    val criticalRawCount = urgentRawMaterials.count { it.status == RawMaterialRiskStatus.CRITICAL }
    val watchRawCount = urgentRawMaterials.count { it.status == RawMaterialRiskStatus.LOW_WATCH }
    val pendingReprocessItems = remember(state.reprocessItems, sectionFilter) {
        state.reprocessItems.filter {
            it.isPending() && (sectionFilter == "ALL" || it.section == sectionFilter)
        }
    }
    val fastActionReprocessItems = remember(pendingReprocessItems) {
        pendingReprocessItems.filter { it.fastActionNeeded }
    }

    val topCustomers = remember(activeOrders, filteredOrders, priorityCustomerLower) {
        val base = if (activeOrders.isNotEmpty()) activeOrders else filteredOrders.filter { it.status != OrderStatus.CANCELLED.label }
        base.groupBy { it.customer.ifBlank { "Walk-in / Unspecified" } }
            .entries
            .map { (cust, list) ->
                CustomerStatRow(
                    customer = cust,
                    isPriority = list.any { it.priority } || cust.trim().lowercase() in priorityCustomerLower,
                    orderCount = list.size,
                    remainingKg = list.sumOf { max(0.0, it.totalWeightKg - it.dispatchedWeightKg) },
                    totalOrderedKg = list.sumOf { it.totalWeightKg }
                )
            }
            .sortedWith(compareByDescending<CustomerStatRow> { it.isPriority }.thenByDescending { it.remainingKg }.thenByDescending { it.totalOrderedKg })
            .take(5)
    }

    val topDispatchedSkus = remember(filteredSummaries) {
        filteredSummaries
            .filter { it.dispatchedKg > 0.0 }
            .sortedByDescending { it.dispatchedKg }
            .take(5)
    }

    // Daily trend data (up to last 7 active days in the selected location/date scope)
    val dailyTrendPoints = remember(filteredBatches, filteredDispatches) {
        val batchDates = filteredBatches.map { it.date }.filter { it.isNotBlank() }
        val dispatchDates = filteredDispatches.map { it.date }.filter { it.isNotBlank() }
        val distinctDates = (batchDates + dispatchDates)
            .distinct()
            .mapNotNull { d -> OperationsCalculator.parseDateOrNull(d)?.let { it to d } }
            .sortedBy { it.first }
            .takeLast(7)

        val shortFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
        distinctDates.map { (parsedDate, iso) ->
            val dayBatches = filteredBatches.filter { it.date == iso }
            val dayDispatches = filteredDispatches.filter { it.date == iso }
            val expKg = dayBatches.sumOf { it.expectedWeightKg }
            val recKg = dayBatches.sumOf { it.actualRecoveredKg }
            val recPct = if (expKg > 0.0) (recKg / expKg) * 100.0 else 100.0
            DailyFlowChartPoint(
                isoDate = iso,
                shortLabel = parsedDate.format(shortFormatter),
                producedKg = recKg,
                deliveredKg = dayDispatches.sumOf { it.effectiveDispatchedWeightKg() },
                recoveryPct = recPct,
                batchCount = dayBatches.size,
                dispatchCount = dayDispatches.size
            )
        }
    }

    val supplyDemandRows = remember(sectionBreakdowns) {
        sectionBreakdowns.map { row ->
            SectionSupplyDemandRow(
                sectionLabel = row.section.label,
                availableKg = row.availKg,
                openOrdersKg = row.openOrdersKg,
                deliveredKg = row.dispatchedKg
            )
        }
    }

    val openOnlyOrderCount = filteredOrders.count { it.status == OrderStatus.OPEN.label }
    val partialOrderCount = filteredOrders.count { it.status == OrderStatus.PARTIALLY_DISPATCHED.label }

    val isDesktop = LocalIsDesktopLayout.current
    val hasActiveFilter = sectionFilter != "ALL" || useSelectedDateScope
    val scopeDescription = buildString {
        append(if (sectionFilter == "ALL") "All Locations" else sectionFilter)
        append(" • ")
        append(
            if (useSelectedDateScope) {
                friendlySummaryDate(selectedDate, todayStr)
            } else {
                "All-Time"
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("summary_tab_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Clean Header + Hidden-until-tapped Filter Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Plant Overview",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = scopeDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasActiveFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = {
                                if (dailyReportPreview != null) {
                                    showDownloadPrompt = true
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForestEmerald,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .desktopPointerHand()
                                .testTag("summary_download_report_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Report")
                        }
                    }
                }

                // 1-Tap Quick Scope Bar (Always easy to reach, Locations expandable on tap)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !useSelectedDateScope,
                        onClick = { useSelectedDateScope = false },
                        label = { Text("All-Time") },
                        modifier = Modifier.testTag("summary_scope_all_time")
                    )
                    FilterChip(
                        selected = useSelectedDateScope && selectedDate == todayStr,
                        onClick = {
                            selectedDate = todayStr
                            useSelectedDateScope = true
                        },
                        leadingIcon = { Icon(Icons.Filled.Today, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        label = { Text("Today") },
                        modifier = Modifier.testTag("summary_scope_today")
                    )
                    FilterChip(
                        selected = useSelectedDateScope && selectedDate != todayStr,
                        onClick = { showDatePicker = true },
                        leadingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        label = {
                            Text(
                                if (useSelectedDateScope && selectedDate != todayStr) {
                                    friendlySummaryDate(selectedDate, todayStr)
                                } else {
                                    "Pick Date"
                                }
                            )
                        },
                        modifier = Modifier.testTag("summary_scope_pick_date")
                    )
                    FilterChip(
                        selected = showFilters || sectionFilter != "ALL",
                        onClick = { showFilters = !showFilters },
                        leadingIcon = {
                            Icon(
                                imageVector = if (showFilters) Icons.Filled.Close else Icons.Filled.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        label = {
                            Text(if (sectionFilter == "ALL") "Location" else sectionFilter)
                        },
                        modifier = Modifier.testTag("summary_toggle_filters_button")
                    )
                    if (hasActiveFilter) {
                        TextButton(
                            onClick = {
                                sectionFilter = "ALL"
                                useSelectedDateScope = false
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.testTag("summary_reset_filter_button")
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset")
                        }
                    }
                }

                AnimatedVisibility(visible = showFilters) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Choose Plant Location",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                (listOf("ALL") + PlantSection.entries.map { it.label }).forEach { sec ->
                                    FilterChip(
                                        selected = sectionFilter == sec,
                                        onClick = { sectionFilter = sec },
                                        label = { Text(if (sec == "ALL") "All Locations" else sec) },
                                        modifier = Modifier.testTag("summary_filter_${sec.lowercase().replace(" ", "_")}")
                                    )
                                }
                            }

                            if (latestActivityDate != null && latestActivityDate != todayStr) {
                                FilterChip(
                                    selected = useSelectedDateScope && selectedDate == latestActivityDate,
                                    onClick = {
                                        selectedDate = latestActivityDate
                                        useSelectedDateScope = true
                                    },
                                    label = { Text("Jump to Latest Activity (${friendlySummaryDate(latestActivityDate, todayStr)})") },
                                    modifier = Modifier.testTag("summary_scope_latest")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Plant Health Banner (Plain English, Right Up Front)
        item {
            val isCritical = criticalCount > 0 || criticalRawCount > 0 || heldDispatches.isNotEmpty() || activeOrderShortfallKg > 0.0 || fastActionReprocessItems.isNotEmpty()
            val isWatch = !isCritical && (watchCount > 0 || watchRawCount > 0 || flaggedLowBatches.isNotEmpty() || pendingReprocessItems.isNotEmpty())
            val (bannerBg, bannerFg, bannerBorder, statusTitle) = when {
                isCritical -> QuadTuple(StatusCriticalContainer, OnStatusCriticalContainer, StatusCritical, "Needs Attention")
                isWatch -> QuadTuple(StatusWatchContainer, OnStatusWatchContainer, GrainAmber, "Keep an Eye On")
                else -> QuadTuple(StatusHealthyContainer, OnStatusHealthyContainer, StatusHealthyTextDark, "Everything Looks Good")
            }

            val summaryLine = when {
                isCritical -> buildList {
                    if (fastActionReprocessItems.isNotEmpty()) add("${fastActionReprocessItems.size} returned lot(s) need fast reprocess")
                    if (criticalRawCount > 0) add("$criticalRawCount raw material${if (criticalRawCount == 1) "" else "s"} critical")
                    if (criticalCount > 0) add("$criticalCount product${if (criticalCount == 1) "" else "s"} critically low")
                    if (activeOrderShortfallKg > 0.0) add("${OperationsCalculator.formatKg(activeOrderShortfallKg)} kg to produce for orders")
                    if (heldDispatches.isNotEmpty()) add("${heldDispatches.size} held delivery")
                }.joinToString(" • ")
                isWatch -> buildList {
                    if (pendingReprocessItems.isNotEmpty()) add("${pendingReprocessItems.size} lot(s) in reprocess")
                    if (watchRawCount > 0) add("$watchRawCount raw material${if (watchRawCount == 1) "" else "s"} running low")
                    if (watchCount > 0) add("$watchCount product${if (watchCount == 1) "" else "s"} running low")
                    if (flaggedLowBatches.isNotEmpty()) add("${flaggedLowBatches.size} low-yield batch")
                }.joinToString(" • ")
                else -> "Stock, raw materials, orders, and deliveries are running smoothly (${String.format("%.1f%%", avgRecoveryPct)} average yield)"
            }

            Surface(
                color = bannerBg,
                border = BorderStroke(1.dp, bannerBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("summary_health_banner")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isCritical || isWatch) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                        contentDescription = statusTitle,
                        tint = bannerFg,
                        modifier = Modifier.size(26.dp)
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = statusTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = bannerFg
                        )
                        Text(
                            text = summaryLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = bannerFg
                        )
                    }
                }
            }
        }

        // 3. Urgent Action Items (Shown automatically ONLY when there's something to fix)
        if (pendingReprocessItems.isNotEmpty() || urgentRawMaterials.isNotEmpty() || topCriticalSkus.isNotEmpty() || heldDispatches.isNotEmpty() || flaggedLowBatches.isNotEmpty()) {
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
                        Text(
                            text = "Action Items",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val alertReprocessList = if (fastActionReprocessItems.isNotEmpty()) {
                            fastActionReprocessItems
                        } else {
                            pendingReprocessItems
                        }
                        alertReprocessList.take(3).forEach { rep ->
                            AlertActionRow(
                                title = "${rep.productName}${if (rep.drReference.isNotBlank()) " • DR ${rep.drReference}" else ""}",
                                subtitle = "${rep.returnedBags} bags (${OperationsCalculator.formatKg(rep.returnedWeightKg)} kg) • ${rep.returnReason.ifBlank { rep.status }}",
                                badge = if (rep.fastActionNeeded) "FAST REPROCESS" else "REPROCESS",
                                badgeBg = if (rep.fastActionNeeded) StatusCriticalContainer else StatusWatchContainer,
                                badgeFg = if (rep.fastActionNeeded) OnStatusCriticalContainer else OnStatusWatchContainer,
                                actionLabel = "Reprocess",
                                onClick = { onNavigateToTab(OpsTab.REPROCESS) }
                            )
                        }
                        urgentRawMaterials.take(3).forEach { rawProj ->
                            val isCrit = rawProj.status == RawMaterialRiskStatus.CRITICAL
                            AlertActionRow(
                                title = "${rawProj.material.name} (${rawProj.projectedRunOutLabel})",
                                subtitle = "${OperationsCalculator.formatKg(rawProj.material.currentStockKg)} kg left • ${OperationsCalculator.formatKg(rawProj.simulatedAvgDailyKg)} kg/day usage",
                                badge = if (isCrit) "RAW MAT CRITICAL" else "RAW MAT LOW",
                                badgeBg = if (isCrit) StatusCriticalContainer else StatusWatchContainer,
                                badgeFg = if (isCrit) OnStatusCriticalContainer else OnStatusWatchContainer,
                                actionLabel = "Restock",
                                onClick = onNavigateToRawMaterials
                            )
                        }
                        topCriticalSkus.forEach { skuSum ->
                            AlertActionRow(
                                title = "${skuSum.sku.productName} (${skuSum.sku.section})",
                                subtitle = "Only ${OperationsCalculator.formatKg(skuSum.availableStockKg)} kg left • Make ~${skuSum.suggestedBags} bags",
                                badge = "LOW STOCK",
                                badgeBg = StatusCriticalContainer,
                                badgeFg = OnStatusCriticalContainer,
                                actionLabel = "Schedule",
                                onClick = { onNavigateToTab(OpsTab.SCHEDULE) }
                            )
                        }
                        heldDispatches.take(2).forEach { held ->
                            AlertActionRow(
                                title = "DR ${held.drReference} • ${held.customer}",
                                subtitle = "${held.productName} (${held.bagsDispatched} bags) • ${held.heldRemarks.ifBlank { "Waiting for clearance" }}",
                                badge = "DELIVERY HELD",
                                badgeBg = StatusCriticalContainer,
                                badgeFg = OnStatusCriticalContainer,
                                actionLabel = "Inspect",
                                onClick = { onNavigateToTab(OpsTab.DISPATCHED) }
                            )
                        }
                        flaggedLowBatches.take(2).forEach { batch ->
                            AlertActionRow(
                                title = "${batch.productName} (${batch.date})",
                                subtitle = "${String.format("%.1f%%", batch.recoveryPct)} yield",
                                badge = "LOW YIELD",
                                badgeBg = StatusWatchContainer,
                                badgeFg = OnStatusWatchContainer,
                                actionLabel = "Check",
                                onClick = { onNavigateToTab(OpsTab.RECOVERY) }
                            )
                        }
                    }
                }
            }
        }

        // 4. Core Plant Metrics (Tap any card to jump to that workspace)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val stockCardSub = if (needingProdCount > 0) {
                    "$needingProdCount product${if (needingProdCount == 1) "" else "s"} need ~${OperationsCalculator.formatBags(totalSuggestedBags.toDouble())} bags"
                } else {
                    "All product stock levels look healthy"
                }
                val prodCardSub = "${String.format("%.1f%%", avgRecoveryPct)} average yield • ${filteredBatches.size} batch${if (filteredBatches.size == 1) "" else "es"}"
                val orderCardSub = "${priorityActiveOrders.size} priority • $fulfilledOrderCount completed"
                val dispatchCardSub = "${deliveredDispatches.size} delivered • ${returnedDispatches.size} returned • ${heldDispatches.size} held"

                if (isDesktop) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryStatCard(
                            title = "AVAILABLE STOCK",
                            primary = "${OperationsCalculator.formatKg(totalAvailKg)} kg",
                            highlight = "${OperationsCalculator.formatBags(totalAvailBags)} bags across ${filteredSummaries.size} SKUs",
                            detail = stockCardSub,
                            icon = Icons.Filled.Inventory2,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onClick = { onNavigateToTab(OpsTab.MONITOR) },
                            modifier = Modifier.weight(1f).fillMaxHeight().testTag("summary_card_stock")
                        )
                        SummaryStatCard(
                            title = "PRODUCTION",
                            primary = "${String.format("%,d", totalProducedBags)} bags",
                            highlight = "${OperationsCalculator.formatKg(totalRecoveredKg)} kg made",
                            detail = prodCardSub,
                            icon = Icons.Filled.Factory,
                            accentColor = if (avgRecoveryPct < 98.5) StatusWatchTextDark else StatusHealthyTextDark,
                            onClick = { onNavigateToTab(OpsTab.RECOVERY) },
                            modifier = Modifier.weight(1f).fillMaxHeight().testTag("summary_card_production")
                        )
                        SummaryStatCard(
                            title = "ORDERS",
                            primary = "${activeOrders.size} active",
                            highlight = "${OperationsCalculator.formatKg(activeRemainingTotalKg)} kg remaining",
                            detail = orderCardSub,
                            icon = Icons.Filled.ShoppingCart,
                            accentColor = if (activeOrderShortfallKg > 0.0) StatusCriticalTextDark else GrainAmberBright,
                            onClick = { onNavigateToTab(OpsTab.ORDERS) },
                            modifier = Modifier.weight(1f).fillMaxHeight().testTag("summary_card_orders")
                        )
                        SummaryStatCard(
                            title = "DELIVERIES",
                            primary = "${OperationsCalculator.formatKg(totalNetDeliveredKg)} kg",
                            highlight = "${filteredDispatches.size} trips (${String.format("%.1f%%", netDeliveryRatePct)} net)",
                            detail = dispatchCardSub,
                            icon = Icons.Filled.LocalShipping,
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            onClick = { onNavigateToTab(OpsTab.DISPATCHED) },
                            modifier = Modifier.weight(1f).fillMaxHeight().testTag("summary_card_dispatches")
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryStatCard(
                            title = "AVAILABLE STOCK",
                            primary = "${OperationsCalculator.formatKg(totalAvailKg)} kg",
                            highlight = "${OperationsCalculator.formatBags(totalAvailBags)} bags (${filteredSummaries.size} SKUs)",
                            detail = stockCardSub,
                            icon = Icons.Filled.Inventory2,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onClick = { onNavigateToTab(OpsTab.MONITOR) },
                            modifier = Modifier.weight(1f).testTag("summary_card_stock")
                        )
                        SummaryStatCard(
                            title = "PRODUCTION",
                            primary = "${String.format("%,d", totalProducedBags)} bags",
                            highlight = "${OperationsCalculator.formatKg(totalRecoveredKg)} kg made",
                            detail = prodCardSub,
                            icon = Icons.Filled.Factory,
                            accentColor = if (avgRecoveryPct < 98.5) StatusWatchTextDark else StatusHealthyTextDark,
                            onClick = { onNavigateToTab(OpsTab.RECOVERY) },
                            modifier = Modifier.weight(1f).testTag("summary_card_production")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryStatCard(
                            title = "ORDERS",
                            primary = "${activeOrders.size} active",
                            highlight = "${OperationsCalculator.formatKg(activeRemainingTotalKg)} kg remaining",
                            detail = orderCardSub,
                            icon = Icons.Filled.ShoppingCart,
                            accentColor = if (activeOrderShortfallKg > 0.0) StatusCriticalTextDark else GrainAmberBright,
                            onClick = { onNavigateToTab(OpsTab.ORDERS) },
                            modifier = Modifier.weight(1f).testTag("summary_card_orders")
                        )
                        SummaryStatCard(
                            title = "DELIVERIES",
                            primary = "${OperationsCalculator.formatKg(totalNetDeliveredKg)} kg",
                            highlight = "${filteredDispatches.size} trips (${String.format("%.1f%%", netDeliveryRatePct)} net)",
                            detail = dispatchCardSub,
                            icon = Icons.Filled.LocalShipping,
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            onClick = { onNavigateToTab(OpsTab.DISPATCHED) },
                            modifier = Modifier.weight(1f).testTag("summary_card_dispatches")
                        )
                    }
                }

                Surface(
                    onClick = { onNavigateToTab(OpsTab.SCHEDULE) },
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("summary_card_schedule")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(StatusInProgressContainer, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = null,
                                    tint = OnStatusInProgressContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Production Schedule",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${scheduledRuns.size} queued • ${inProgressRuns.size} running now (${String.format("%,d", activeQueueBags)} target bags) • $completedRunsCount finished",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Schedule",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (state.rawMaterialProjections.isNotEmpty()) {
                    val lowestRaw = state.rawMaterialProjections.first()
                    val urgentCount = urgentRawMaterials.size
                    val rawBannerColor = when {
                        criticalRawCount > 0 -> StatusCriticalContainer
                        watchRawCount > 0 -> StatusWatchContainer
                        else -> StatusHealthyContainer
                    }
                    val rawIconColor = when {
                        criticalRawCount > 0 -> OnStatusCriticalContainer
                        watchRawCount > 0 -> OnStatusWatchContainer
                        else -> OnStatusHealthyContainer
                    }
                    Surface(
                        onClick = onNavigateToRawMaterials,
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("summary_card_raw_materials")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(rawBannerColor, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (urgentCount > 0) Icons.Filled.Warning else Icons.Filled.Inventory2,
                                        contentDescription = null,
                                        tint = rawIconColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Raw Materials Forecast",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (urgentCount > 0) {
                                            "$urgentCount low-stock ingredient${if (urgentCount == 1) "" else "s"} • Shortest runway: ${lowestRaw.material.name} (${lowestRaw.projectedRunOutLabel})"
                                        } else {
                                            "${state.rawMaterials.size} ingredients tracked • Shortest runway: ${lowestRaw.material.name} (${lowestRaw.projectedRunOutLabel})"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open Raw Materials",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Surface(
                    onClick = { onNavigateToTab(OpsTab.FEED_BOM) },
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("summary_card_feed_bom")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(SlateTealContainer, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Science,
                                    contentDescription = null,
                                    tint = OnSlateTealContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Feed Bill of Materials (BOM)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${state.boms.size} feed formula${if (state.boms.size == 1) "" else "s"} • Macro & Micro ingredient cards by feed type",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Feed BOM Screen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 5. Focused Sub-Tabs so deeper details don't overwhelm the screen
        item {
            TabRow(
                selectedTabIndex = selectedViewTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (selectedViewTab.ordinal < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedViewTab.ordinal]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            ) {
                SummaryViewTab.entries.forEach { tab ->
                    val isSelected = selectedViewTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { selectedViewTab = tab },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = {
                            Text(
                                text = tab.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .desktopPointerHand()
                            .testTag("summary_subtab_${tab.name.lowercase()}")
                    )
                }
            }
        }

        when (selectedViewTab) {
            SummaryViewTab.OVERVIEW -> {
                item {
                    DailyFlowBarChartCard(
                        title = "Made vs. Delivered",
                        subtitle = if (useSelectedDateScope) {
                            "Activity on ${friendlySummaryDate(selectedDate, todayStr)}"
                        } else {
                            "Recent ${dailyTrendPoints.size.coerceAtLeast(1)} active day(s)"
                        },
                        points = dailyTrendPoints,
                        onOpenAnalytics = { selectedViewTab = SummaryViewTab.ANALYTICS },
                        onSwitchAllTime = if (useSelectedDateScope) {
                            { useSelectedDateScope = false }
                        } else null
                    )
                }

                item {
                    val totalProducts = filteredSummaries.size.coerceAtLeast(1)
                    val healthyPct = (healthyCount * 100.0 / totalProducts)
                    DonutSplitChartCard(
                        title = "Stock Health (${filteredSummaries.size} products)",
                        centerHeadline = "${String.format("%.0f%%", healthyPct)}",
                        centerSublabel = "Healthy",
                        slices = listOf(
                            DonutSliceItem("Healthy", healthyCount, StatusHealthyTextDark),
                            DonutSliceItem("Running Low", watchCount, GrainAmberBright),
                            DonutSliceItem("Critical", criticalCount, StatusCriticalTextDark)
                        ),
                        actionLabel = "View Stock",
                        onActionClick = { onNavigateToTab(OpsTab.MONITOR) }
                    )
                }

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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Orders & Delivery Progress",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${String.format("%.0f%%", activeFulfillmentFrac * 100f)} of active orders delivered",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${OperationsCalculator.formatKg(activeDispatchedTotalKg)} / ${OperationsCalculator.formatKg(activeOrderedTotalKg)} kg",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            LinearProgressIndicator(
                                progress = { activeFulfillmentFrac },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = DarkChartTrack
                            )

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    MiniStat("Ready Stock", "${OperationsCalculator.formatKg(stockReadyOrderKg)} kg", StatusHealthyTextDark)
                                    MiniStat(
                                        "To Produce",
                                        "${OperationsCalculator.formatKg(activeOrderShortfallKg)} kg",
                                        if (activeOrderShortfallKg > 0.0) StatusCriticalTextDark else MaterialTheme.colorScheme.onSurface
                                    )
                                    MiniStat("Priority", "${priorityActiveOrders.size} open", GrainAmberBright)
                                    MiniStat("Cancelled", "$cancelledOrderCount", MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            HorizontalDivider()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${filteredDispatches.size} delivery trips",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${OperationsCalculator.formatKg(totalNetDeliveredKg)} kg net delivered",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatusPill("${deliveredDispatches.size} Delivered", StatusHealthyContainer, OnStatusHealthyContainer)
                                StatusPill("${returnedDispatches.size} Returned", StatusWatchContainer, OnStatusWatchContainer)
                                StatusPill("${heldDispatches.size} Held", StatusCriticalContainer, OnStatusCriticalContainer)
                            }

                            if (returnedDispatches.isNotEmpty()) {
                                val reasonsText = topReturnReasons.joinToString(" • ") { "${it.key} (${it.value})" }
                                Text(
                                    text = "Returned: $totalReturnedBags bags (${OperationsCalculator.formatKg(totalReturnedKg)} kg) • $reasonsText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusWatchTextDark
                                )
                            }
                        }
                    }
                }

                // Compact Collapsible Report & Export Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                            .testTag("summary_download_report_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Daily Report • ${friendlySummaryDate(selectedDate, todayStr)}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${dailyReportPreview?.productRows?.size ?: 0} products • ${dailyReportPreview?.dispatches?.size ?: 0} deliveries • ${dailyReportPreview?.activeOpenOrders?.size ?: 0} open orders",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(
                                    onClick = { showReportOptions = !showReportOptions },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Text(if (showReportOptions) "Less" else "Options")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (showReportOptions) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = if (showReportOptions) "Hide report options" else "Show report options",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            AnimatedVisibility(visible = showReportOptions) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Report Date: $selectedDate",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        OutlinedButton(
                                            onClick = { showDatePicker = true },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("summary_report_pick_date_button")
                                        ) {
                                            Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Change Date", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }

                                    ReportSectionSelectorRow(
                                        selectedSections = selectedExportSections,
                                        onUpdateSections = { selectedExportSections = it },
                                        tagPrefix = "summary_export"
                                    )

                                    ExportFormatSelectorRow(
                                        selectedFormat = selectedExportFormat,
                                        onSelectFormat = { selectedExportFormat = it },
                                        tagPrefix = "summary_export_format"
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                if (dailyReportPreview != null) {
                                                    showMessengerDialog = true
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MessengerBlue,
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("summary_messenger_report_button")
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Messenger")
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val rep = dailyReportPreview
                                                if (rep != null) {
                                                    try {
                                                        val res = ReportExporter.saveReportToDownloads(context, rep, selectedExportFormat)
                                                        if (res.uri != null) {
                                                            downloadedFileInfo = Triple(res.uri, res.fileName, selectedExportFormat.mimeType)
                                                        }
                                                        val shareUri = ReportExporter.createShareableReportUri(context, res.fileName, res.bytes)
                                                        shareExportedFileUri(context, shareUri, res.fileName, selectedExportFormat.mimeType)
                                                    } catch (e: Exception) {
                                                        onShowToast("${selectedExportFormat.label} share failed: ${e.localizedMessage}")
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("summary_share_report_button")
                                        ) {
                                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Share ${selectedExportFormat.label}")
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                dailyReportPreview?.let { ReportExporter.printReportHtml(context, it) }
                                            },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("summary_print_report_button")
                                        ) {
                                            Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Print")
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                dailyReportPreview?.let { rep ->
                                                    val copied = copyReportTextToClipboard(
                                                        context,
                                                        "Bulacan Daily Summary ($selectedDate)",
                                                        ReportExporter.buildPlainTextReport(rep)
                                                    )
                                                    onShowToast(
                                                        if (copied) "Summary for $selectedDate copied to clipboard."
                                                        else "Summary too large for clipboard; tap Share or Download."
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("summary_copy_report_button")
                                        ) {
                                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Copy Text")
                                        }
                                    }
                                }
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
                                            captionText = "10K Agri Feeds - Bulacan Daily Report ($fileName)",
                                            onToast = onShowToast
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            SummaryViewTab.ANALYTICS -> {
                item {
                    Surface(
                        onClick = { onNavigateToTab(OpsTab.PRODUCTION_MONITOR) },
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("summary_open_production_monitor_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.BarChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Production Monitor (Output vs. Targets)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Inspect daily feed output against scheduled, expected & custom targets",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open Production Monitor",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                item {
                    DailyFlowBarChartCard(
                        title = "Production vs. Deliveries Over Time",
                        subtitle = if (useSelectedDateScope) {
                            "Showing ${friendlySummaryDate(selectedDate, todayStr)} • switch to All-Time for multi-day trends"
                        } else {
                            "Compare daily feed produced vs. delivered (kg)"
                        },
                        points = dailyTrendPoints,
                        onSwitchAllTime = if (useSelectedDateScope) {
                            { useSelectedDateScope = false }
                        } else null
                    )
                }

                item {
                    RecoveryTrendChartCard(
                        points = dailyTrendPoints,
                        targetPct = 98.5,
                        onSwitchAllTime = if (useSelectedDateScope) {
                            { useSelectedDateScope = false }
                        } else null
                    )
                }

                item {
                    SectionSupplyDemandChartCard(rows = supplyDemandRows)
                }

                item {
                    val orderSlices = listOf(
                        DonutSliceItem("Fulfilled", fulfilledOrderCount, StatusHealthyTextDark),
                        DonutSliceItem("Partial", partialOrderCount, GrainAmberBright),
                        DonutSliceItem("Open", openOnlyOrderCount, MaterialTheme.colorScheme.primary),
                        DonutSliceItem("Cancelled", cancelledOrderCount, MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    val dispatchSlices = listOf(
                        DonutSliceItem("Delivered", deliveredDispatches.size, StatusHealthyTextDark),
                        DonutSliceItem("Returned", returnedDispatches.size, GrainAmberBright),
                        DonutSliceItem("Held", heldDispatches.size, StatusCriticalTextDark)
                    )

                    if (isDesktop) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DonutSplitChartCard(
                                title = "Order Status Mix",
                                centerHeadline = "${filteredOrders.size}",
                                centerSublabel = "Orders",
                                slices = orderSlices,
                                actionLabel = "Orders",
                                onActionClick = { onNavigateToTab(OpsTab.ORDERS) },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            DonutSplitChartCard(
                                title = "Delivery Outcomes",
                                centerHeadline = "${filteredDispatches.size}",
                                centerSublabel = "Trips",
                                slices = dispatchSlices,
                                actionLabel = "Deliveries",
                                onActionClick = { onNavigateToTab(OpsTab.DISPATCHED) },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            DonutSplitChartCard(
                                title = "Order Status Mix",
                                centerHeadline = "${filteredOrders.size}",
                                centerSublabel = "Orders",
                                slices = orderSlices,
                                actionLabel = "Orders",
                                onActionClick = { onNavigateToTab(OpsTab.ORDERS) }
                            )
                            DonutSplitChartCard(
                                title = "Delivery Outcomes",
                                centerHeadline = "${filteredDispatches.size}",
                                centerSublabel = "Trips",
                                slices = dispatchSlices,
                                actionLabel = "Deliveries",
                                onActionClick = { onNavigateToTab(OpsTab.DISPATCHED) }
                            )
                        }
                    }
                }
            }

            SummaryViewTab.LOCATIONS -> {
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Stock & Activity by Location",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )

                            sectionBreakdowns.forEachIndexed { idx, row ->
                                val shareFrac = (row.availKg / maxSectionAvailKg).toFloat().coerceIn(0f, 1f)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${row.section.label} (${row.skuCount} products)",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${OperationsCalculator.formatBags(row.availBags)} bags • ${OperationsCalculator.formatKg(row.availKg)} kg",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { shareFrac },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = DarkChartTrack
                                    )
                                    Text(
                                        text = "Made: ${row.producedBags} bags (${String.format("%.1f%%", row.recoveryPct)} yield) • Delivered: ${OperationsCalculator.formatKg(row.dispatchedKg)} kg • Open orders: ${OperationsCalculator.formatKg(row.openOrdersKg)} kg" +
                                            if (row.criticalCount > 0) " • ${row.criticalCount} low" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (row.criticalCount > 0) StatusWatchTextDark else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (idx < sectionBreakdowns.lastIndex) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }

            SummaryViewTab.HIGHLIGHTS -> {
                if (topCustomers.isNotEmpty() || topDispatchedSkus.isNotEmpty()) {
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
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (topCustomers.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Top Customer Orders",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        TextButton(
                                            onClick = { onNavigateToTab(OpsTab.ORDERS) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Text("View Orders", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                    topCustomers.forEach { c ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (c.isPriority) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Star,
                                                        contentDescription = "Priority customer",
                                                        tint = GrainAmberBright,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "${c.customer} (${c.orderCount})",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (c.remainingKg > 0.0) {
                                                    "${OperationsCalculator.formatKg(c.remainingKg)} kg pending"
                                                } else {
                                                    "${OperationsCalculator.formatKg(c.totalOrderedKg)} kg total"
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (c.remainingKg > 0.0) StatusWatchTextDark else StatusHealthyTextDark
                                            )
                                        }
                                    }
                                }

                                if (topCustomers.isNotEmpty() && topDispatchedSkus.isNotEmpty()) {
                                    HorizontalDivider()
                                }

                                if (topDispatchedSkus.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Most Delivered Products",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        TextButton(
                                            onClick = { onNavigateToTab(OpsTab.REPORTS) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Text("Full Reports", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                    topDispatchedSkus.forEach { item ->
                                        val cover = if (item.daysOfCover.isInfinite()) "ample supply" else "${String.format("%.1f", item.daysOfCover)}d left"
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.sku.productName,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${OperationsCalculator.formatKg(item.dispatchedKg)} kg • $cover",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Helpful Shortcuts Card
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
                            Text(
                                text = "Quick Jump",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuickJumpPill("Daily Log", Icons.Filled.Today, "summary_jump_daily_log") { onNavigateToTab(OpsTab.DAILY_LOG) }
                                QuickJumpPill("Reports", Icons.Filled.Assessment, "summary_jump_reports") { onNavigateToTab(OpsTab.REPORTS) }
                                QuickJumpPill("AI Ops", Icons.Filled.AutoAwesome, "summary_jump_ai") { onNavigateToTab(OpsTab.AI_OPS) }
                                QuickJumpPill("History", Icons.Filled.History, "summary_jump_history") { onNavigateToTab(OpsTab.HISTORY) }
                                QuickJumpPill("Team", Icons.Filled.Group, "summary_jump_team") { onNavigateToTab(OpsTab.TEAM) }
                            }
                        }
                    }
                }
            }
        }

        // Selected Date Activity Log (shown automatically when a specific date filter is active)
        if (useSelectedDateScope) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                        .testTag("summary_selected_date_activity_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Activity on ${friendlySummaryDate(selectedDate, todayStr)}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        if (filteredBatches.isEmpty() && filteredDispatches.isEmpty() && filteredSchedules.isEmpty() && filteredOrders.isEmpty()) {
                            Text(
                                text = "No production batches, deliveries, schedules, or orders recorded on this date.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            if (filteredBatches.isNotEmpty()) {
                                Text(
                                    text = "Production Batches (${filteredBatches.size})",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                filteredBatches.take(6).forEach { b ->
                                    Text(
                                        text = "• ${b.productName} (${b.section}): ${b.bagsProduced} bags (${OperationsCalculator.formatKg(b.actualRecoveredKg)} kg, ${String.format("%.1f%%", b.recoveryPct)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            if (filteredDispatches.isNotEmpty()) {
                                HorizontalDivider()
                                Text(
                                    text = "Deliveries (${filteredDispatches.size})",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                filteredDispatches.take(6).forEach { d ->
                                    Text(
                                        text = "• DR ${d.drReference} • ${d.customer}: ${d.productName} (${d.bagsDispatched} bags) • ${d.resolvedStatusLabel()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            if (filteredSchedules.isNotEmpty()) {
                                HorizontalDivider()
                                Text(
                                    text = "Scheduled Runs (${filteredSchedules.size})",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                filteredSchedules.take(6).forEach { s ->
                                    Text(
                                        text = "• ${s.section} • ${s.productName}: ${s.targetBags} bags (${s.status})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            if (filteredOrders.isNotEmpty()) {
                                HorizontalDivider()
                                Text(
                                    text = "Orders (${filteredOrders.size})",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                filteredOrders.take(6).forEach { o ->
                                    Text(
                                        text = "• ${o.orderCode} • ${o.customer}: ${o.productName} (${OperationsCalculator.formatKg(o.totalWeightKg)} kg) • ${o.status}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        ReportDatePickerModal(
            initialIsoDate = selectedDate,
            title = "Select Summary Date",
            onDismiss = { showDatePicker = false },
            onConfirmDate = { picked ->
                selectedDate = picked
                useSelectedDateScope = true
                showDatePicker = false
            }
        )
    }

    val summaryReportForPrompt = dailyReportPreview
    if (showDownloadPrompt && summaryReportForPrompt != null) {
        DownloadAlsoPdfXlsDialog(
            title = "Download Summary Report",
            initialFormat = selectedExportFormat,
            onDismiss = { showDownloadPrompt = false },
            onConfirmFormats = { chosenFormats ->
                showDownloadPrompt = false
                chosenFormats.lastOrNull()?.let { selectedExportFormat = it }
                val savedLabels = mutableListOf<String>()
                val queuedFallback = mutableListOf<Triple<ByteArray, String, String>>()
                chosenFormats.forEach { fmt ->
                    try {
                        val res = ReportExporter.saveReportToDownloads(context, summaryReportForPrompt, fmt)
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

    if (showMessengerDialog && summaryReportForPrompt != null) {
        SendPlantReportToMessengerDialog(
            report = summaryReportForPrompt,
            initialFormat = selectedExportFormat,
            onDismiss = { showMessengerDialog = false },
            onShowToast = onShowToast
        )
    }
}

private fun friendlySummaryDate(dateStr: String, todayStr: String): String {
    if (dateStr == todayStr) return "Today"
    val parsed = OperationsCalculator.parseDateOrNull(dateStr) ?: return dateStr
    val todayDate = OperationsCalculator.parseDateOrNull(todayStr)
    if (todayDate != null && parsed == todayDate.minusDays(1)) return "Yesterday"
    return parsed.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
}

private data class QuadTuple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private data class SectionStatRow(
    val section: PlantSection,
    val skuCount: Int,
    val criticalCount: Int,
    val availKg: Double,
    val availBags: Double,
    val producedBags: Long,
    val recoveryPct: Double,
    val dispatchedKg: Double,
    val openOrdersKg: Double
)

private data class CustomerStatRow(
    val customer: String,
    val isPriority: Boolean,
    val orderCount: Int,
    val remainingKg: Double,
    val totalOrderedKg: Double
)

@Composable
private fun QuickJumpPill(
    label: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SummaryStatCard(
    title: String,
    primary: String,
    highlight: String,
    detail: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .desktopPointerHand(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(accentColor.copy(alpha = 0.16f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Text(
                text = primary,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = highlight,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Tap to open ›",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LegendStat(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = color)
    }
}

@Composable
private fun AlertActionRow(
    title: String,
    subtitle: String,
    badge: String,
    badgeBg: Color,
    badgeFg: Color,
    actionLabel: String,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                StatusPill(badge, badgeBg, badgeFg)
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onClick,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
