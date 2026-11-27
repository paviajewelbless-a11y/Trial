package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.OperationsCalculator
import com.example.model.CustomerOrder
import com.example.model.OrderStatus
import com.example.model.PlantSection
import com.example.model.ProductionBatch
import com.example.model.SchedulePriority
import com.example.model.ScheduleRun
import com.example.model.ScheduleStatus
import com.example.model.SkuEntity
import com.example.ui.components.LocalIsDesktopLayout
import com.example.ui.components.LocalWindowWidthDp
import com.example.ui.components.MultiOrderLinkPickerDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.StockStatusBadge
import com.example.ui.components.SwipeLeftActionsContainer
import com.example.ui.components.WhoseOrderDialog
import com.example.ui.components.desktopGridColumns
import com.example.ui.components.desktopGridItems
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.GrainAmber
import com.example.ui.theme.OnStatusCriticalContainer
import com.example.ui.theme.OnStatusHealthyContainer
import com.example.ui.theme.OnStatusInProgressContainer
import com.example.ui.theme.OnStatusWatchContainer
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusCriticalContainer
import com.example.ui.theme.StatusCriticalTextDark
import com.example.ui.theme.StatusHealthyContainer
import com.example.ui.theme.StatusHealthyTextDark
import com.example.ui.theme.StatusInProgressContainer
import com.example.ui.theme.StatusWatchContainer
import com.example.ui.theme.StatusWatchTextDark
import com.example.viewmodel.OpsUiState
import kotlin.math.ceil
import kotlin.math.max

private enum class ScheduleQueueSortMode(val label: String) {
    PRIORITY_FIRST("Priority → Standard"),
    LATEST_FIRST("Date: Latest → Oldest")
}

@Composable
fun ScheduleScreen(
    state: OpsUiState.Ready,
    onOpenOrdersTab: () -> Unit,
    onOpenMultiSchedule: (List<Pair<SkuEntity, Long>>) -> Unit,
    onScheduleSku: (SkuEntity, Long, SchedulePriority) -> Unit,
    onScheduleSelectedOrders: (List<CustomerOrder>) -> Unit,
    onAdvanceScheduleStatus: (ScheduleRun, ScheduleStatus) -> Unit,
    onStartMultipleRuns: (List<ScheduleRun>) -> Unit = {},
    onEditSchedule: (ScheduleRun) -> Unit,
    onUpdateScheduleLinks: (ScheduleRun, List<CustomerOrder>) -> Unit,
    onDeleteSchedule: (ScheduleRun) -> Unit,
    onCancelSchedule: (ScheduleRun) -> Unit,
    onCompleteScheduleWithBatch: (ScheduleRun) -> Unit,
    onEditRecoveredBatch: (ProductionBatch) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var sectionFilter by remember { mutableStateOf("ALL") }
    var queueSortMode by remember { mutableStateOf(ScheduleQueueSortMode.PRIORITY_FIRST) }
    var pendingEditRun by remember { mutableStateOf<ScheduleRun?>(null) }
    var pendingDeleteRun by remember { mutableStateOf<ScheduleRun?>(null) }
    var pendingCancelRun by remember { mutableStateOf<ScheduleRun?>(null) }
    var whoseOrderRun by remember { mutableStateOf<ScheduleRun?>(null) }
    var whoseOrderBuilderProduct by remember { mutableStateOf<String?>(null) }
    var linkPickerRun by remember { mutableStateOf<ScheduleRun?>(null) }
    var multiSelectBuilder by remember { mutableStateOf(true) }
    var checkedSkuKeys by remember { mutableStateOf(emptySet<String>()) }
    var orderSearchQuery by remember { mutableStateOf("") }
    var checkedOrderIds by remember { mutableStateOf(emptySet<String>()) }
    var checkedQueueRunIds by remember { mutableStateOf(emptySet<String>()) }

    val orderById = remember(state.orders) { state.orders.associateBy { it.id } }
    val orderByCode = remember(state.orders) { state.orders.associateBy { it.orderCode } }
    val openOrdersByProduct = remember(state.orders, queueSortMode) {
        val activeOrders = state.orders
            .filter { it.status == OrderStatus.OPEN.label || it.status == OrderStatus.PARTIALLY_DISPATCHED.label }
        val sortedOrders = when (queueSortMode) {
            ScheduleQueueSortMode.PRIORITY_FIRST -> activeOrders.sortedWith(
                compareByDescending<CustomerOrder> { it.priority }
                    .thenByDescending { it.orderDate }
                    .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    .thenByDescending { it.dueDate }
            )
            ScheduleQueueSortMode.LATEST_FIRST -> activeOrders.sortedWith(
                compareByDescending<CustomerOrder> { it.orderDate }
                    .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    .thenByDescending { it.dueDate }
            )
        }
        sortedOrders.groupBy { it.productName }
    }

    val sortedQueue = remember(state.schedules, sectionFilter, queueSortMode, orderById, orderByCode) {
        val filtered = state.schedules.filter { sectionFilter == "ALL" || it.section == sectionFilter }
        val statusComparator = compareBy<ScheduleRun> {
            when (it.status) {
                ScheduleStatus.SCHEDULED.label, ScheduleStatus.IN_PROGRESS.label -> 0
                ScheduleStatus.COMPLETED.label -> 1
                else -> 2
            }
        }
        when (queueSortMode) {
            ScheduleQueueSortMode.PRIORITY_FIRST -> filtered.sortedWith(
                statusComparator
                    .thenBy { SchedulePriority.fromRaw(it.priority).sortRank }
                    .thenByDescending { run ->
                        val hasPriorityById = run.allLinkedOrderIds().any { orderById[it]?.priority == true }
                        val hasPriorityByCode = run.allLinkedOrderCodes().any { orderByCode[it]?.priority == true }
                        hasPriorityById || hasPriorityByCode
                    }
                    .thenByDescending { it.scheduledDate }
                    .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
            )
            ScheduleQueueSortMode.LATEST_FIRST -> filtered.sortedWith(
                statusComparator
                    .thenByDescending { it.scheduledDate }
                    .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    .thenBy { it.productName }
            )
        }
    }

    val queueRuns = remember(sortedQueue) {
        sortedQueue.filter { it.status != ScheduleStatus.IN_PROGRESS.label }
    }
    val inProgressRuns = remember(sortedQueue) {
        sortedQueue.filter { it.status == ScheduleStatus.IN_PROGRESS.label }
    }
    val scheduledCount = remember(sortedQueue) {
        sortedQueue.count { it.status == ScheduleStatus.SCHEDULED.label }
    }

    val filteredSummaries = remember(state.summaries, sectionFilter, queueSortMode, openOrdersByProduct) {
        val base = state.summaries.filter { sectionFilter == "ALL" || it.sku.section == sectionFilter }
        when (queueSortMode) {
            ScheduleQueueSortMode.PRIORITY_FIRST -> base.sortedWith(
                compareByDescending<com.example.model.SkuInventorySummary> { s ->
                    openOrdersByProduct[s.sku.productName].orEmpty().any { it.priority }
                }
                    .thenBy { s -> s.recommendedPriority?.sortRank ?: 99 }
                    .thenByDescending { it.netProductionNeededKg }
                    .thenBy { it.sku.productName }
            )
            ScheduleQueueSortMode.LATEST_FIRST -> base.sortedWith(
                compareByDescending<com.example.model.SkuInventorySummary> { s ->
                    openOrdersByProduct[s.sku.productName].orEmpty().maxOfOrNull { it.orderDate }.orEmpty()
                }
                    .thenByDescending { s ->
                        openOrdersByProduct[s.sku.productName].orEmpty().maxOfOrNull { it.createdAt?.toDate()?.time ?: 0L } ?: 0L
                    }
                    .thenByDescending { it.openOrdersKg }
                    .thenBy { it.sku.productName }
            )
        }
    }

    val summaryByProductLower = remember(state.summaries) {
        state.summaries.associateBy { it.sku.productName.trim().lowercase() }
    }
    val filteredOpenOrders = remember(state.orders, sectionFilter, orderSearchQuery, queueSortMode, summaryByProductLower) {
        val q = orderSearchQuery.trim()
        val active = state.orders.filter { ord ->
            val isOpen = ord.status == OrderStatus.OPEN.label || ord.status == OrderStatus.PARTIALLY_DISPATCHED.label
            if (!isOpen) return@filter false
            val sec = summaryByProductLower[ord.productName.trim().lowercase()]?.sku?.section
            val matchesSec = sectionFilter == "ALL" || sec == null || sec == sectionFilter
            val matchesQuery = q.isBlank() ||
                ord.customer.contains(q, ignoreCase = true) ||
                ord.orderCode.contains(q, ignoreCase = true) ||
                ord.productName.contains(q, ignoreCase = true)
            matchesSec && matchesQuery
        }
        when (queueSortMode) {
            ScheduleQueueSortMode.PRIORITY_FIRST -> active.sortedWith(
                compareByDescending<CustomerOrder> { it.priority }
                    .thenBy { it.dueDate }
                    .thenByDescending { it.orderDate }
            )
            ScheduleQueueSortMode.LATEST_FIRST -> active.sortedWith(
                compareByDescending<CustomerOrder> { it.orderDate }
                    .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    .thenBy { it.dueDate }
            )
        }
    }

    val isDesktop = LocalIsDesktopLayout.current
    val windowWidthDp = LocalWindowWidthDp.current
    val gridColumns = desktopGridColumns(windowWidthDp, maxCols = 2)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text(
                        text = "Production Scheduler",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Order-driven & dispatch-velocity prioritization",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenOrdersTab,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Orders Tab", maxLines = 1)
                    }
                    Button(
                        onClick = { onOpenMultiSchedule(emptyList()) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_multi_schedule_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForestEmerald,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.AddTask, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Multi-Schedule", maxLines = 1)
                    }
                }

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
                    if (selectedTab == 1) {
                        FilterChip(
                            selected = multiSelectBuilder,
                            onClick = {
                                multiSelectBuilder = !multiSelectBuilder
                                if (!multiSelectBuilder) checkedSkuKeys = emptySet()
                            },
                            label = { Text(if (multiSelectBuilder) "Checkboxes On" else "Show Checkboxes") },
                            modifier = Modifier.testTag("toggle_schedule_multiselect")
                        )
                    }
                    if (sectionFilter != "ALL" || (selectedTab == 0 && orderSearchQuery.isNotBlank())) {
                        TextButton(
                            onClick = {
                                sectionFilter = "ALL"
                                orderSearchQuery = ""
                            },
                            modifier = Modifier.testTag("reset_schedule_filter_button")
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset")
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "Orders (${filteredOpenOrders.size})",
                            "Products (${filteredSummaries.size})",
                            "Queue ($scheduledCount)",
                            "Active (${inProgressRuns.size})"
                        ).forEachIndexed { idx, label ->
                            val isSelected = selectedTab == idx
                            Surface(
                                color = if (isSelected) ForestEmerald else Color.Transparent,
                                shape = RoundedCornerShape(9.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clickable { selectedTab = idx }
                                    .testTag("schedule_subtab_$idx")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sort:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ScheduleQueueSortMode.entries.forEach { mode ->
                        val isSelected = queueSortMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { queueSortMode = mode },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ForestEmerald,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            ),
                            label = { Text(mode.label) },
                            modifier = Modifier.testTag("sort_schedule_${mode.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (selectedTab == 0) {
            item {
                OutlinedTextField(
                    value = orderSearchQuery,
                    onValueChange = { orderSearchQuery = it },
                    label = { Text("Search open orders") },
                    placeholder = { Text("Search customer, PO code, or product...") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "Search orders to schedule")
                    },
                    trailingIcon = {
                        if (orderSearchQuery.isNotBlank()) {
                            IconButton(onClick = { orderSearchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear order search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("schedule_orders_search_input")
                )
            }

            item {
                val selectedOrders = state.orders.filter {
                    val key = it.id.ifBlank { it.orderCode }
                    key in checkedOrderIds &&
                        (it.status == OrderStatus.OPEN.label || it.status == OrderStatus.PARTIALLY_DISPATCHED.label)
                }
                val groupedRunCount = selectedOrders.map { it.productName.trim().lowercase() }.distinct().size
                val totalCheckedRemKg = selectedOrders.sumOf { max(0.0, it.totalWeightKg - it.dispatchedWeightKg) }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedOrders.isEmpty()) {
                                        "Check Orders to Schedule in 1 Run"
                                    } else {
                                        "${selectedOrders.size} order(s) checked → $groupedRunCount run(s)"
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = if (selectedOrders.isEmpty()) {
                                        "Tick checkboxes below — same-SKU orders combine into one run"
                                    } else {
                                        "Total remaining: ${OperationsCalculator.formatKg(totalCheckedRemKg)} kg"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            Button(
                                onClick = {
                                    if (selectedOrders.isNotEmpty()) {
                                        onScheduleSelectedOrders(selectedOrders)
                                        checkedOrderIds = emptySet()
                                    }
                                },
                                enabled = selectedOrders.isNotEmpty(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ForestEmerald,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("schedule_checked_orders_button")
                            ) {
                                Icon(Icons.Filled.AddTask, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Schedule (${selectedOrders.size})")
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    checkedOrderIds = checkedOrderIds + filteredOpenOrders.map { it.id.ifBlank { it.orderCode } }
                                },
                                enabled = filteredOpenOrders.isNotEmpty(),
                                modifier = Modifier.testTag("schedule_orders_select_all_button")
                            ) {
                                Text("Select All (${filteredOpenOrders.size})")
                            }
                            if (checkedOrderIds.isNotEmpty()) {
                                TextButton(
                                    onClick = { checkedOrderIds = emptySet() },
                                    modifier = Modifier.testTag("schedule_orders_clear_button")
                                ) {
                                    Text("Clear")
                                }
                            }
                        }
                    }
                }
            }

            if (filteredOpenOrders.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (orderSearchQuery.isNotBlank()) {
                                    "No open customer orders match \"$orderSearchQuery\"."
                                } else {
                                    "No open or partially dispatched customer orders right now."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = { selectedTab = 1 },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Browse SKUs (${filteredSummaries.size})")
                            }
                        }
                    }
                }
            } else {
                desktopGridItems(
                    itemsList = filteredOpenOrders,
                    columns = gridColumns,
                    keyPrefix = "sched_ord",
                    itemKey = { it.id.ifBlank { it.orderCode } }
                ) { ord ->
                    val ordKey = ord.id.ifBlank { ord.orderCode }
                    val isChecked = ordKey in checkedOrderIds
                    val remKg = max(0.0, ord.totalWeightKg - ord.dispatchedWeightKg)
                    val skuSummary = summaryByProductLower[ord.productName.trim().lowercase()]
                    val availKg = max(0.0, skuSummary?.availableStockKg ?: 0.0)
                    val secLabel = skuSummary?.sku?.section ?: "BULACAN"
                    val bagWt = (skuSummary?.sku?.standardBagWeightKg ?: ord.bagWeightKg).coerceAtLeast(1.0)
                    val remBags = ceil(remKg / bagWt).toLong().coerceAtLeast(0L)
                    val (sBg, sFg) = if (ord.status == OrderStatus.PARTIALLY_DISPATCHED.label) {
                        StatusInProgressContainer to OnStatusInProgressContainer
                    } else {
                        StatusWatchContainer to OnStatusWatchContainer
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isChecked) 2.dp else if (ord.priority) 1.5.dp else 1.dp,
                                color = when {
                                    isChecked -> MaterialTheme.colorScheme.primary
                                    ord.priority -> GrainAmber
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                checkedOrderIds = if (isChecked) checkedOrderIds - ordKey else checkedOrderIds + ordKey
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    checkedOrderIds = if (checked) checkedOrderIds + ordKey else checkedOrderIds - ordKey
                                },
                                modifier = Modifier.testTag("schedule_order_checkbox_${ord.orderCode}")
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
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
                                        Text(
                                            text = ord.customer,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (ord.priority) {
                                            StatusPill("PRIORITY", StatusWatchContainer, OnStatusWatchContainer)
                                        }
                                    }
                                    StatusPill(ord.status, sBg, sFg)
                                }
                                Text(
                                    text = "${ord.orderCode} • ${ord.productName}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$secLabel • ${ord.orderedBags} ordered ($remBags rem bags / ${OperationsCalculator.formatKg(remKg)} kg) • Due ${ord.dueDate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (remKg > availKg + 0.01) {
                                        val shortfallKg = remKg - availKg
                                        val needBags = ceil(shortfallKg / bagWt).toLong().coerceAtLeast(1L)
                                        "Avail: ${OperationsCalculator.formatKg(availKg)} kg • Need ~$needBags bags (${OperationsCalculator.formatKg(shortfallKg)} kg shortfall)"
                                    } else {
                                        "Avail: ${OperationsCalculator.formatKg(availKg)} kg (stock covers order)"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (remKg > availKg + 0.01) StatusCriticalTextDark else StatusHealthyTextDark
                                )
                            }
                        }
                    }
                }
            }
        } else if (selectedTab == 1) {
            if (multiSelectBuilder) {
                item {
                    val selectedList = filteredSummaries.filter {
                        val key = it.sku.id.ifBlank { "${it.sku.section}_${it.sku.productName}" }
                        key in checkedSkuKeys
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${selectedList.size} SKU(s) checked",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Check SKUs below to queue together",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Button(
                                    onClick = {
                                        if (selectedList.isNotEmpty()) {
                                            onOpenMultiSchedule(
                                                selectedList.map { it.sku to it.suggestedBags.coerceAtLeast(40L) }
                                            )
                                            checkedSkuKeys = emptySet()
                                        }
                                    },
                                    enabled = selectedList.isNotEmpty(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ForestEmerald,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("schedule_selected_skus_button")
                                ) {
                                    Icon(Icons.Filled.AddTask, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Schedule (${selectedList.size})")
                                }
                            }
                            if (checkedSkuKeys.isNotEmpty()) {
                                TextButton(onClick = { checkedSkuKeys = emptySet() }) {
                                    Text("Clear SKU Selection")
                                }
                            }
                        }
                    }
                }
            }

            desktopGridItems(
                itemsList = filteredSummaries,
                columns = gridColumns,
                keyPrefix = "builder",
                itemKey = { it.sku.id.ifBlank { "${it.sku.section}_${it.sku.productName}" } }
            ) { s ->
                val skuKey = s.sku.id.ifBlank { "${s.sku.section}_${s.sku.productName}" }
                val isChecked = skuKey in checkedSkuKeys
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isChecked) 2.dp else 1.dp,
                            color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
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
                            if (multiSelectBuilder) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        checkedSkuKeys = if (checked) checkedSkuKeys + skuKey else checkedSkuKeys - skuKey
                                    },
                                    modifier = Modifier.testTag("schedule_sku_checkbox_$skuKey")
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.sku.productName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${s.sku.section} • Line: ${s.sku.defaultRemarks}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StockStatusBadge(status = s.status)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SmallStat("Avail", "${OperationsCalculator.formatKg(s.availableStockKg)} kg")
                            SmallStat("Open Ord", "${OperationsCalculator.formatKg(s.openOrdersKg)} kg")
                            SmallStat("Disp Rate", "${OperationsCalculator.formatKg(s.avgDailyDispatchKg)} kg/d")
                            SmallStat("Net Needed", "${OperationsCalculator.formatKg(s.netProductionNeededKg)} kg")
                        }

                        val builderOrders = openOrdersByProduct[s.sku.productName].orEmpty()
                        if (builderOrders.isNotEmpty()) {
                            val builderTextLen = builderOrders.sumOf { it.customer.length + it.orderCode.length + 6 }
                            val isBuilderTooLong = builderOrders.size > 2 || builderTextLen > 85
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (!isBuilderTooLong) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ShoppingCart,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "Orders For: ${
                                                builderOrders.joinToString(", ") {
                                                    val star = if (it.priority) "★ " else ""
                                                    "$star${it.customer} (${it.orderCode})"
                                                }
                                            }",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.People,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                text = "${builderOrders.size} customer orders open",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        TextButton(
                                            onClick = { whoseOrderBuilderProduct = s.sku.productName },
                                            modifier = Modifier.testTag("builder_whose_order_${skuKey}")
                                        ) {
                                            Text("Whose Order? (${builderOrders.size})")
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val recText = if (s.suggestedBags > 0) {
                                "Rec: ${s.suggestedBags} bags (${s.recommendedPriority?.label ?: ""})"
                            } else {
                                "Stock healthy • No urgent run needed"
                            }
                            Text(
                                text = recText,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (s.suggestedBags > 0) StatusWatchTextDark else StatusHealthyTextDark,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    onScheduleSku(
                                        s.sku,
                                        s.suggestedBags.coerceAtLeast(40L),
                                        s.recommendedPriority ?: SchedulePriority.BUFFER_TOP_UP
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (s.suggestedBags > 0) GrainAmber else ForestEmerald,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Filled.AddTask, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (s.suggestedBags > 0) "Schedule ${s.suggestedBags}b" else "Schedule")
                            }
                        }
                    }
                }
            }
        } else if (selectedTab == 2) {
            val startableRuns = queueRuns.filter { it.status == ScheduleStatus.SCHEDULED.label && it.id.isNotBlank() }
            if (startableRuns.isNotEmpty()) {
                item {
                    val selectedQueueRuns = startableRuns.filter {
                        val key = it.id.ifBlank { "${it.scheduledDate}_${it.productName}" }
                        key in checkedQueueRunIds
                    }
                    val selectedTotalBags = selectedQueueRuns.sumOf { it.targetBags }
                    val selectedTotalKg = selectedQueueRuns.sumOf { it.expectedWeightKg }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (selectedQueueRuns.isEmpty()) {
                                            "Check Runs to Start Together"
                                        } else {
                                            "${selectedQueueRuns.size} run(s) checked • $selectedTotalBags bags"
                                        },
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = if (selectedQueueRuns.isEmpty()) {
                                            "Tick checkboxes on scheduled runs below to move them to Active at once"
                                        } else {
                                            "Total expected output: ${OperationsCalculator.formatKg(selectedTotalKg)} kg"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                                Button(
                                    onClick = {
                                        if (selectedQueueRuns.isNotEmpty()) {
                                            onStartMultipleRuns(selectedQueueRuns)
                                            checkedQueueRunIds = emptySet()
                                        }
                                    },
                                    enabled = selectedQueueRuns.isNotEmpty(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ForestEmerald,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("start_selected_runs_button")
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Start Runs (${selectedQueueRuns.size})")
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        checkedQueueRunIds = checkedQueueRunIds + startableRuns.map {
                                            it.id.ifBlank { "${it.scheduledDate}_${it.productName}" }
                                        }
                                    },
                                    modifier = Modifier.testTag("queue_select_all_button")
                                ) {
                                    Text("Select All (${startableRuns.size})")
                                }
                                if (checkedQueueRunIds.isNotEmpty()) {
                                    TextButton(
                                        onClick = { checkedQueueRunIds = emptySet() },
                                        modifier = Modifier.testTag("queue_clear_selection_button")
                                    ) {
                                        Text("Clear")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (queueRuns.isNotEmpty()) {
                item {
                    Text(
                        text = if (isDesktop) {
                            "Desktop Actions: Click Edit or Delete below any queued run"
                        } else {
                            "Tip: Swipe left on a queued run to Edit or Delete"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (queueRuns.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "No scheduled runs in the queue. Use Orders or SKUs tab above to queue runs.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                desktopGridItems(
                    itemsList = queueRuns,
                    columns = gridColumns,
                    keyPrefix = "queue",
                    itemKey = { it.id.ifBlank { "${it.scheduledDate}_${it.productName}" } }
                ) { run ->
                    val qKey = run.id.ifBlank { "${run.scheduledDate}_${run.productName}" }
                    val canSelectToStart = run.status == ScheduleStatus.SCHEDULED.label && run.id.isNotBlank()
                    val isQueueChecked = canSelectToStart && qKey in checkedQueueRunIds
                    ScheduleQueueRunCard(
                        run = run,
                        state = state,
                        orderById = orderById,
                        orderByCode = orderByCode,
                        openOrdersByProduct = openOrdersByProduct,
                        selectableForStart = canSelectToStart,
                        isCheckedForStart = isQueueChecked,
                        onToggleCheckedForStart = { checked ->
                            checkedQueueRunIds = if (checked) checkedQueueRunIds + qKey else checkedQueueRunIds - qKey
                        },
                        onAdvanceScheduleStatus = onAdvanceScheduleStatus,
                        onCompleteScheduleWithBatch = onCompleteScheduleWithBatch,
                        onEditRecoveredBatch = onEditRecoveredBatch,
                        onRequestEdit = { pendingEditRun = run },
                        onRequestDelete = { pendingDeleteRun = run },
                        onRequestCancel = { pendingCancelRun = run },
                        onOpenWhoseOrder = { whoseOrderRun = run },
                        onOpenLinkPicker = { linkPickerRun = run }
                    )
                }
            }
        } else {
            if (inProgressRuns.isNotEmpty()) {
                item {
                    Text(
                        text = if (isDesktop) {
                            "Active runs on the mill floor • Use Edit or Delete buttons below any card"
                        } else {
                            "Active runs on the mill floor • Swipe left to Edit or Delete"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (inProgressRuns.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "No production runs currently in progress. Start a scheduled run from the Queue tab to track it here.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedButton(
                                onClick = { selectedTab = 2 },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("in_progress_open_queue_button")
                            ) {
                                Text("Go to Queue ($scheduledCount)")
                            }
                        }
                    }
                }
            } else {
                desktopGridItems(
                    itemsList = inProgressRuns,
                    columns = gridColumns,
                    keyPrefix = "inprog",
                    itemKey = { it.id.ifBlank { "${it.scheduledDate}_${it.productName}" } }
                ) { run ->
                    ScheduleQueueRunCard(
                        run = run,
                        state = state,
                        orderById = orderById,
                        orderByCode = orderByCode,
                        openOrdersByProduct = openOrdersByProduct,
                        onAdvanceScheduleStatus = onAdvanceScheduleStatus,
                        onCompleteScheduleWithBatch = onCompleteScheduleWithBatch,
                        onEditRecoveredBatch = onEditRecoveredBatch,
                        onRequestEdit = { pendingEditRun = run },
                        onRequestDelete = { pendingDeleteRun = run },
                        onRequestCancel = { pendingCancelRun = run },
                        onOpenWhoseOrder = { whoseOrderRun = run },
                        onOpenLinkPicker = { linkPickerRun = run }
                    )
                }
            }
        }
    }

    pendingCancelRun?.let { runToCancel ->
        AlertDialog(
            onDismissRequest = { pendingCancelRun = null },
            title = { Text("Cancel Production Run?") },
            text = {
                Text(
                    "Cancel ${runToCancel.targetBags} bags of ${runToCancel.productName} (${runToCancel.section}) scheduled for ${runToCancel.scheduledDate}?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelSchedule(runToCancel)
                        pendingCancelRun = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusCritical,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_cancel_schedule_button")
                ) {
                    Text("Cancel Run")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCancelRun = null }) {
                    Text("Keep Run")
                }
            }
        )
    }

    pendingEditRun?.let { runToEdit ->
        val canEdit = runToEdit.status == ScheduleStatus.SCHEDULED.label || runToEdit.status == ScheduleStatus.IN_PROGRESS.label
        AlertDialog(
            onDismissRequest = { pendingEditRun = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit schedule run confirmation",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(if (canEdit) "Edit Production Run?" else "Run Locked") },
            text = {
                Text(
                    if (canEdit) {
                        "Confirm editing ${runToEdit.productName} (${runToEdit.targetBags} bags scheduled for ${runToEdit.scheduledDate})?"
                    } else {
                        "This run is marked ${runToEdit.status} and cannot be edited. You can delete it from the queue instead."
                    }
                )
            },
            confirmButton = {
                if (canEdit) {
                    Button(
                        onClick = {
                            pendingEditRun = null
                            onEditSchedule(runToEdit)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForestEmerald,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("confirm_edit_schedule_button")
                    ) {
                        Text("Edit Run")
                    }
                } else {
                    Button(
                        onClick = { pendingEditRun = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForestEmerald,
                            contentColor = Color.White
                        )
                    ) {
                        Text("OK")
                    }
                }
            },
            dismissButton = {
                if (canEdit) {
                    TextButton(onClick = { pendingEditRun = null }) { Text("Cancel") }
                }
            }
        )
    }

    pendingDeleteRun?.let { runToDelete ->
        AlertDialog(
            onDismissRequest = { pendingDeleteRun = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete schedule run confirmation",
                    tint = StatusCriticalTextDark
                )
            },
            title = { Text("Delete Production Run?") },
            text = {
                Text(
                    "Permanently delete ${runToDelete.targetBags} bags of ${runToDelete.productName} (${runToDelete.section}, ${runToDelete.scheduledDate}) from the queue? This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteRun = null
                        onDeleteSchedule(runToDelete)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusCritical,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_schedule_button")
                ) {
                    Text("Delete Run")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRun = null }) { Text("Keep Run") }
            }
        )
    }

    whoseOrderRun?.let { runCtx ->
        val allIds = runCtx.allLinkedOrderIds()
        val allCodes = runCtx.allLinkedOrderCodes()
        val linkedOrders = (allIds.mapNotNull { orderById[it] } +
            allCodes.mapNotNull { orderByCode[it] })
            .distinctBy { it.id.ifBlank { it.orderCode } }
        val linkedIdSet = linkedOrders.map { it.id }.toSet()
        val unresolvedCodes = allCodes.filter { c -> linkedOrders.none { it.orderCode == c } }
        val skuOpenOrders = openOrdersByProduct[runCtx.productName].orEmpty().filter { it.id !in linkedIdSet }
        val canEdit = runCtx.status == ScheduleStatus.SCHEDULED.label || runCtx.status == ScheduleStatus.IN_PROGRESS.label

        WhoseOrderDialog(
            productName = runCtx.productName,
            run = runCtx,
            linkedOrders = linkedOrders,
            unresolvedLinkedCodes = unresolvedCodes,
            skuOpenOrders = skuOpenOrders,
            canEditLinks = canEdit,
            onEditLinkedOrders = {
                whoseOrderRun = null
                linkPickerRun = runCtx
            },
            onLinkAllOpenOrders = {
                val merged = (linkedOrders + skuOpenOrders).distinctBy { it.id.ifBlank { it.orderCode } }
                whoseOrderRun = null
                onUpdateScheduleLinks(runCtx, merged)
            },
            onDismiss = { whoseOrderRun = null }
        )
    }

    whoseOrderBuilderProduct?.let { prodName ->
        val openOrders = openOrdersByProduct[prodName].orEmpty()
        WhoseOrderDialog(
            productName = prodName,
            run = null,
            linkedOrders = emptyList(),
            skuOpenOrders = openOrders,
            canEditLinks = false,
            onDismiss = { whoseOrderBuilderProduct = null }
        )
    }

    linkPickerRun?.let { runCtx ->
        val allIds = runCtx.allLinkedOrderIds()
        val allCodes = runCtx.allLinkedOrderCodes()
        val existingLinkedIds = state.orders
            .filter { it.id in allIds || it.orderCode in allCodes }
            .map { it.id }
            .filter { it.isNotBlank() }
            .toSet()
        MultiOrderLinkPickerDialog(
            productName = runCtx.productName,
            allOrders = state.orders,
            initiallySelectedIds = existingLinkedIds,
            onDismiss = { linkPickerRun = null },
            onConfirm = { picked ->
                linkPickerRun = null
                onUpdateScheduleLinks(runCtx, picked)
            }
        )
    }
}

@Composable
private fun ScheduleQueueRunCard(
    run: ScheduleRun,
    state: OpsUiState.Ready,
    orderById: Map<String, CustomerOrder>,
    orderByCode: Map<String, CustomerOrder>,
    openOrdersByProduct: Map<String, List<CustomerOrder>>,
    selectableForStart: Boolean = false,
    isCheckedForStart: Boolean = false,
    onToggleCheckedForStart: (Boolean) -> Unit = {},
    onAdvanceScheduleStatus: (ScheduleRun, ScheduleStatus) -> Unit,
    onCompleteScheduleWithBatch: (ScheduleRun) -> Unit,
    onEditRecoveredBatch: (ProductionBatch) -> Unit,
    onRequestEdit: () -> Unit,
    onRequestDelete: () -> Unit,
    onRequestCancel: () -> Unit,
    onOpenWhoseOrder: () -> Unit,
    onOpenLinkPicker: () -> Unit
) {
    val runKey = run.id.ifBlank { "${run.scheduledDate}_${run.productName}" }
    val (pBg, pFg) = when (SchedulePriority.fromRaw(run.priority)) {
        SchedulePriority.URGENT -> StatusCriticalContainer to OnStatusCriticalContainer
        SchedulePriority.ORDER_DRIVEN -> StatusWatchContainer to OnStatusWatchContainer
        SchedulePriority.BUFFER_TOP_UP -> StatusInProgressContainer to OnStatusInProgressContainer
    }
    val (sBg, sFg) = when (run.status) {
        ScheduleStatus.COMPLETED.label -> StatusHealthyContainer to OnStatusHealthyContainer
        ScheduleStatus.IN_PROGRESS.label -> StatusInProgressContainer to OnStatusInProgressContainer
        ScheduleStatus.CANCELLED.label -> StatusCriticalContainer to OnStatusCriticalContainer
        else -> StatusWatchContainer to OnStatusWatchContainer
    }
    val isActiveRun =
        run.status == ScheduleStatus.SCHEDULED.label || run.status == ScheduleStatus.IN_PROGRESS.label
    val matchedBatch = remember(state.batches, run) {
        if (run.status != ScheduleStatus.COMPLETED.label) {
            null
        } else {
            state.batches.firstOrNull {
                it.productName == run.productName &&
                    it.section == run.section &&
                    it.date == run.scheduledDate
            } ?: state.batches.firstOrNull {
                it.productName == run.productName && it.section == run.section
            }
        }
    }
    SwipeLeftActionsContainer(
        itemKey = runKey,
        shape = RoundedCornerShape(14.dp),
        editTestTag = "swipe_edit_schedule_$runKey",
        deleteTestTag = "swipe_delete_schedule_$runKey",
        onEdit = onRequestEdit,
        onDelete = onRequestDelete
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isCheckedForStart) 2.dp else 1.dp,
                    color = if (isCheckedForStart) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(14.dp)
                ),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isCheckedForStart) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (selectableForStart) {
                                Modifier.clickable { onToggleCheckedForStart(!isCheckedForStart) }
                            } else {
                                Modifier
                            }
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    if (selectableForStart) {
                        Checkbox(
                            checked = isCheckedForStart,
                            onCheckedChange = onToggleCheckedForStart,
                            modifier = Modifier.testTag("queue_run_checkbox_$runKey")
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(run.productName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${run.scheduledDate} • ${run.section} • ${run.lineRemarks}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StatusPill(run.priority, pBg, pFg)
                        StatusPill(run.status, sBg, sFg)
                    }
                }
                val allIds = run.allLinkedOrderIds()
                val allCodes = run.allLinkedOrderCodes()
                val linkedOrdersForRun = (allIds.mapNotNull { orderById[it] } +
                    allCodes.mapNotNull { orderByCode[it] })
                    .distinctBy { it.id.ifBlank { it.orderCode } }
                val linkedIdSet = linkedOrdersForRun.map { it.id }.toSet()
                val unresolvedCodes = allCodes.filter { c -> linkedOrdersForRun.none { it.orderCode == c } }
                val openForSku = openOrdersByProduct[run.productName].orEmpty().filter { it.id !in linkedIdSet }
                val hasAnyLinked = linkedOrdersForRun.isNotEmpty() || unresolvedCodes.isNotEmpty()
                val linkedCount = linkedOrdersForRun.size + unresolvedCodes.size
                val totalChars = linkedOrdersForRun.sumOf { it.customer.length + it.orderCode.length + 24 } +
                    unresolvedCodes.sumOf { it.length + 14 }
                val isLinkedTooLong = linkedCount > 2 || totalChars > 110

                if (hasAnyLinked) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!isLinkedTooLong) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                linkedOrdersForRun.forEach { linkedOrd ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ShoppingCart,
                                            contentDescription = "Linked order",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "For: ${linkedOrd.customer}",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (linkedOrd.priority) {
                                                    StatusPill("PRIORITY ORDER", StatusWatchContainer, OnStatusWatchContainer)
                                                }
                                            }
                                            Text(
                                                text = "Order: ${linkedOrd.orderCode} • ${linkedOrd.orderedBags} bags • Due ${linkedOrd.dueDate} • ${linkedOrd.status}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }
                                unresolvedCodes.forEach { code ->
                                    Text(
                                        text = "Linked Order Ref: $code",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                if (openForSku.isNotEmpty() || isActiveRun) {
                                    OutlinedButton(
                                        onClick = onOpenWhoseOrder,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("queue_whose_order_button_$runKey")
                                    ) {
                                        Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (openForSku.isNotEmpty()) {
                                                "Whose Order? (+${openForSku.size} Open for SKU)"
                                            } else {
                                                "Manage Linked Orders"
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val firstCustomer = linkedOrdersForRun.firstOrNull()?.customer
                                    ?: unresolvedCodes.firstOrNull().orEmpty()
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.People,
                                        contentDescription = "Multiple linked orders",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "$linkedCount Customer Orders Linked ($firstCustomer +${linkedCount - 1} more)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (linkedOrdersForRun.any { it.priority }) {
                                        StatusPill("PRIORITY", StatusWatchContainer, OnStatusWatchContainer)
                                    }
                                }
                                Button(
                                    onClick = onOpenWhoseOrder,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ForestEmerald,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("queue_whose_order_button_$runKey")
                                ) {
                                    Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val totalRel = linkedCount + openForSku.size
                                    Text(
                                        text = "Whose Order? View All ($totalRel)",
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                } else if (openForSku.isNotEmpty()) {
                    val openChars = openForSku.sumOf { it.customer.length + it.orderCode.length + 18 }
                    val isOpenTooLong = openForSku.size > 2 || openChars > 95
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!isOpenTooLong) {
                                openForSku.forEach { ord ->
                                    val tag = if (ord.priority) "★ " else ""
                                    Text(
                                        text = "$tag${ord.customer} • ${ord.orderCode} (${ord.orderedBags} bags • Due ${ord.dueDate})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Text(
                                    text = "${openForSku.size} Open Customer Orders for ${run.productName}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = onOpenWhoseOrder,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("queue_whose_order_button_$runKey")
                            ) {
                                Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isOpenTooLong) {
                                        "Whose Order? View All (${openForSku.size})"
                                    } else if (isActiveRun) {
                                        "Link Customer Orders (${openForSku.size})"
                                    } else {
                                        "View Order Details (${openForSku.size})"
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Link: Buffer / General Stock Replenishment",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (isActiveRun && state.orders.any {
                                it.status == OrderStatus.OPEN.label || it.status == OrderStatus.PARTIALLY_DISPATCHED.label
                            }
                        ) {
                            TextButton(
                                onClick = onOpenLinkPicker,
                                modifier = Modifier.testTag("queue_link_orders_button_$runKey")
                            ) {
                                Text("Link Orders")
                            }
                        }
                    }
                }
                Text(
                    "Target: ${run.targetBags} bags x ${run.bagWeightKg}kg = ${OperationsCalculator.formatKg(run.expectedWeightKg)} kg • Status: ${run.status}",
                    style = MaterialTheme.typography.labelLarge
                )
                val skuSummaryForRun = state.summaries.firstOrNull { it.sku.productName == run.productName }
                if (isActiveRun && skuSummaryForRun != null) {
                    val backlogKg = kotlin.math.max(0.0, skuSummaryForRun.openOrdersKg - kotlin.math.max(0.0, skuSummaryForRun.availableStockKg))
                    val backlogBags = if (run.bagWeightKg > 0.0 && backlogKg > 0.0) {
                        kotlin.math.ceil(backlogKg / run.bagWeightKg).toLong()
                    } else 0L
                    if (backlogKg > 0.0) {
                        Text(
                            text = "Backlogs to Produce: $backlogBags bags (${OperationsCalculator.formatKg(backlogKg)} kg)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = StatusCriticalTextDark
                        )
                    } else if (skuSummaryForRun.openOrdersKg > 0.0) {
                        Text(
                            text = "Open Orders Backlogs: 0 kg (covered by available stock)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = StatusHealthyTextDark
                        )
                    }
                }
                if (matchedBatch != null) {
                    Text(
                        text = "Recovered: ${matchedBatch.bagsProduced} bags (${OperationsCalculator.formatKg(matchedBatch.actualRecoveredKg)} kg • ${String.format("%.2f%%", matchedBatch.recoveryPct)})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (matchedBatch.recoveryPct < 98.5) StatusCriticalTextDark else StatusHealthyTextDark
                    )
                }
                if (isActiveRun) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (run.status == ScheduleStatus.SCHEDULED.label) {
                                OutlinedButton(
                                    onClick = { onAdvanceScheduleStatus(run, ScheduleStatus.IN_PROGRESS) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Start Run")
                                }
                            }
                            OutlinedButton(
                                onClick = onRequestCancel,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("cancel_schedule_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = StatusCriticalTextDark
                                )
                            ) {
                                Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cancel Run")
                            }
                        }
                        Button(
                            onClick = { onCompleteScheduleWithBatch(run) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("complete_schedule_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForestEmerald,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Complete & Log Recovery")
                        }
                    }
                } else if (matchedBatch != null) {
                    OutlinedButton(
                        onClick = { onEditRecoveredBatch(matchedBatch) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_schedule_recovered_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Bags Recovered")
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
    }
}
