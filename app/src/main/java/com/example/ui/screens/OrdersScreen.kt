package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.model.DispatchRecord
import com.example.model.OrderStatus
import com.example.model.PlantSection
import com.example.model.SchedulePriority
import com.example.model.ScheduleRun
import com.example.model.ScheduleStatus
import com.example.model.SkuEntity
import com.example.ui.components.IndustrialKpiCard
import com.example.ui.components.LocalIsDesktopLayout
import com.example.ui.components.LocalWindowWidthDp
import com.example.ui.components.StatusPill
import com.example.ui.components.SwipeLeftActionsContainer
import com.example.ui.components.desktopGridColumns
import com.example.ui.components.desktopGridItems
import com.example.ui.theme.DarkChartTrack
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.GrainAmber
import com.example.ui.theme.GrainAmberBright
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
import com.example.viewmodel.OpsUiState
import kotlin.math.ceil
import kotlin.math.max

private enum class OrderSortMode(val label: String) {
    PRIORITY_FIRST("Priority → Standard"),
    LATEST_FIRST("Date: Latest → Oldest")
}

private enum class OrdersViewTab(val label: String) {
    ALL("All Orders"),
    RUSH_ORDERS("Rush Orders")
}

@Composable
fun OrdersScreen(
    state: OpsUiState.Ready,
    onAddOrder: () -> Unit,
    onAddRushOrder: () -> Unit = onAddOrder,
    onEditOrder: (CustomerOrder) -> Unit,
    onToggleOrderPriority: (CustomerOrder) -> Unit,
    onCancelOrder: (CustomerOrder) -> Unit,
    onRetrieveOrder: (CustomerOrder) -> Unit,
    onDeleteOrder: (CustomerOrder) -> Unit,
    onManageCustomers: () -> Unit,
    onScheduleProductionForOrder: (CustomerOrder, SkuEntity, Long, SchedulePriority) -> Unit,
    onScheduleMultipleOrders: (List<CustomerOrder>) -> Unit,
    onDispatchMultipleOrders: (List<CustomerOrder>) -> Unit = {},
    onFulfillOrderViaDispatch: (CustomerOrder) -> Unit,
    onNavigateToSchedule: () -> Unit
) {
    var selectedOrderTab by remember { mutableStateOf(OrdersViewTab.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }
    var priorityOnlyFilter by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(OrderSortMode.PRIORITY_FIRST) }
    var flaggedDispatchOrder by remember { mutableStateOf<Pair<CustomerOrder, Double>?>(null) }
    var pendingEditOrder by remember { mutableStateOf<CustomerOrder?>(null) }
    var pendingCancelOrder by remember { mutableStateOf<CustomerOrder?>(null) }
    var pendingRetrieveOrder by remember { mutableStateOf<CustomerOrder?>(null) }
    var pendingDeleteOrder by remember { mutableStateOf<CustomerOrder?>(null) }
    var multiSelectMode by remember { mutableStateOf(true) }
    var selectedOrderKeys by remember { mutableStateOf(emptySet<String>()) }

    BackHandler(enabled = selectedOrderTab != OrdersViewTab.ALL) {
        selectedOrderTab = OrdersViewTab.ALL
    }

    val summaryByProduct = remember(state.summaries) {
        state.summaries.associateBy { it.sku.productName }
    }
    val skuByProduct = remember(state.skus) {
        state.skus.associateBy { it.productName }
    }

    val rushOrders = remember(state.orders) {
        state.orders.filter { it.rushOrder }
    }
    val tabOrders = remember(state.orders, rushOrders, selectedOrderTab) {
        when (selectedOrderTab) {
            OrdersViewTab.ALL -> state.orders
            OrdersViewTab.RUSH_ORDERS -> rushOrders
        }
    }

    val activeOrders = remember(tabOrders) {
        tabOrders.filter { it.status == OrderStatus.OPEN.label || it.status == OrderStatus.PARTIALLY_DISPATCHED.label }
    }
    val totalPendingKg = activeOrders.sumOf { max(0.0, it.totalWeightKg - it.dispatchedWeightKg) }
    val totalShortfallKg = activeOrders.sumOf { ord ->
        val rem = max(0.0, ord.totalWeightKg - ord.dispatchedWeightKg)
        val avail = max(0.0, summaryByProduct[ord.productName]?.availableStockKg ?: 0.0)
        max(0.0, rem - avail)
    }
    val fulfilledKg = tabOrders.filter { it.status == OrderStatus.FULFILLED.label }.sumOf { it.totalWeightKg }
    val linkedRunCount = state.schedules.count {
        it.allLinkedOrderIds().isNotEmpty() &&
            it.status != ScheduleStatus.COMPLETED.label &&
            it.status != ScheduleStatus.CANCELLED.label
    }

    val filteredOrders = remember(tabOrders, searchQuery, statusFilter, priorityOnlyFilter, sortMode) {
        val base = tabOrders.filter { ord ->
            (statusFilter == "ALL" || ord.status == statusFilter) &&
                (!priorityOnlyFilter || ord.priority) &&
                (searchQuery.isBlank() ||
                    ord.orderCode.contains(searchQuery, ignoreCase = true) ||
                    ord.customer.contains(searchQuery, ignoreCase = true) ||
                    ord.productName.contains(searchQuery, ignoreCase = true))
        }
        when (sortMode) {
            OrderSortMode.PRIORITY_FIRST -> base.sortedWith(
                compareByDescending<CustomerOrder> { it.priority }
                    .thenByDescending { it.orderDate }
                    .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    .thenByDescending { it.dueDate }
                    .thenByDescending { it.orderCode }
            )
            OrderSortMode.LATEST_FIRST -> base.sortedWith(
                compareByDescending<CustomerOrder> { it.orderDate }
                    .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    .thenByDescending { it.dueDate }
                    .thenByDescending { it.orderCode }
            )
        }
    }

    val listState = rememberLazyListState()
    val isDesktop = LocalIsDesktopLayout.current
    val windowWidthDp = LocalWindowWidthDp.current
    val gridColumns = desktopGridColumns(windowWidthDp, maxCols = 2)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text("Customer Orders & Links", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Link orders directly to Production Runs & DR Dispatches",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onManageCustomers,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manage_customers_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Customers (${state.customers.size})", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = {
                            if (selectedOrderTab == OrdersViewTab.RUSH_ORDERS) onAddRushOrder() else onAddOrder()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("orders_tab_new_order_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForestEmerald,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (selectedOrderTab == OrdersViewTab.RUSH_ORDERS) "New Rush Order" else "New Order",
                            maxLines = 1
                        )
                    }
                }
            }
        }

        item {
            val selectedTabIdx = selectedOrderTab.ordinal
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIdx,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        if (selectedTabIdx in tabPositions.indices) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIdx]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    OrdersViewTab.entries.forEach { tab ->
                        val isSelected = selectedOrderTab == tab
                        val count = if (tab == OrdersViewTab.ALL) state.orders.size else rushOrders.size
                        Tab(
                            selected = isSelected,
                            onClick = { selectedOrderTab = tab },
                            text = {
                                Text(
                                    text = "${tab.label} ($count)",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("orders_subtab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (selectedOrderTab == OrdersViewTab.RUSH_ORDERS) {
            item {
                Surface(
                    color = StatusCriticalContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, StatusCritical.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rush_orders_info_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = "Rush orders",
                            tint = OnStatusCriticalContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Additional customers or direct stops added during Dispatch automatically log here as Rush Orders.",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnStatusCriticalContainer
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = if (isDesktop) {
                    Modifier.fillMaxWidth().height(IntrinsicSize.Max)
                } else {
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                },
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IndustrialKpiCard(
                    title = "OPEN DEMAND",
                    primaryValue = "${OperationsCalculator.formatKg(totalPendingKg)} kg",
                    secondaryValue = "${activeOrders.size} active orders",
                    icon = Icons.Filled.ShoppingCart,
                    accentColor = GrainAmberBright,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(185.dp)
                )
                IndustrialKpiCard(
                    title = "STOCK BACKLOGS",
                    primaryValue = "${OperationsCalculator.formatKg(totalShortfallKg)} kg",
                    secondaryValue = "Needs production",
                    icon = Icons.Filled.Warning,
                    accentColor = StatusCriticalTextDark,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(180.dp)
                )
                IndustrialKpiCard(
                    title = "LINKED PROD RUNS",
                    primaryValue = "$linkedRunCount queued",
                    secondaryValue = "Order-driven runs",
                    icon = Icons.Filled.Factory,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(175.dp)
                )
                IndustrialKpiCard(
                    title = "FULFILLED ORDERS",
                    primaryValue = "${OperationsCalculator.formatKg(fulfilledKg)} kg",
                    secondaryValue = "Completed deliveries",
                    icon = Icons.Filled.CheckCircle,
                    accentColor = StatusHealthyTextDark,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(185.dp)
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search orders") },
                    placeholder = { Text("Search by PO code, customer, or product...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("ALL", OrderStatus.OPEN.label, OrderStatus.PARTIALLY_DISPATCHED.label, OrderStatus.FULFILLED.label, OrderStatus.CANCELLED.label).forEach { st ->
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = st },
                            label = { Text(if (st == "ALL") "All (${tabOrders.size})" else st) }
                        )
                    }
                    FilterChip(
                        selected = priorityOnlyFilter,
                        onClick = { priorityOnlyFilter = !priorityOnlyFilter },
                        label = { Text("Priority Only (${tabOrders.count { it.priority }})") },
                        modifier = Modifier.testTag("filter_priority_orders_chip")
                    )
                    FilterChip(
                        selected = multiSelectMode,
                        onClick = {
                            multiSelectMode = !multiSelectMode
                            if (!multiSelectMode) selectedOrderKeys = emptySet()
                        },
                        label = { Text(if (multiSelectMode) "Checkboxes On" else "Show Checkboxes") },
                        modifier = Modifier.testTag("toggle_orders_multiselect")
                    )
                    if (statusFilter != "ALL" || priorityOnlyFilter || searchQuery.isNotBlank()) {
                        TextButton(
                            onClick = {
                                statusFilter = "ALL"
                                priorityOnlyFilter = false
                                searchQuery = ""
                            },
                            modifier = Modifier.testTag("reset_orders_filter_button")
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset")
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
                    OrderSortMode.entries.forEach { mode ->
                        val isSelected = sortMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { sortMode = mode },
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
                            modifier = Modifier.testTag("sort_orders_${mode.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (multiSelectMode) {
            item {
                val activeFilteredOrders = filteredOrders.filter {
                    it.status == OrderStatus.OPEN.label || it.status == OrderStatus.PARTIALLY_DISPATCHED.label
                }
                val selectedOrders = state.orders.filter {
                    val key = it.id.ifBlank { it.orderCode }
                    key in selectedOrderKeys &&
                        (it.status == OrderStatus.OPEN.label || it.status == OrderStatus.PARTIALLY_DISPATCHED.label)
                }
                val totalSelectedRemKg = selectedOrders.sumOf { max(0.0, it.totalWeightKg - it.dispatchedWeightKg) }
                val groupedRunCount = selectedOrders.map { it.productName.trim().lowercase() }.distinct().size
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedOrders.isEmpty()) {
                                        "Select Multiple Orders (Checkbox)"
                                    } else {
                                        "${selectedOrders.size} order(s) checked → $groupedRunCount run(s)"
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = if (selectedOrders.isEmpty()) {
                                        "Check active orders below to dispatch or schedule together"
                                    } else {
                                        "Total remaining: ${OperationsCalculator.formatKg(totalSelectedRemKg)} kg"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = {
                                        selectedOrderKeys = selectedOrderKeys + activeFilteredOrders.map {
                                            it.id.ifBlank { it.orderCode }
                                        }
                                    },
                                    enabled = activeFilteredOrders.isNotEmpty(),
                                    modifier = Modifier.testTag("orders_select_all_button")
                                ) {
                                    Text("Select All (${activeFilteredOrders.size})")
                                }
                                if (selectedOrderKeys.isNotEmpty()) {
                                    TextButton(
                                        onClick = { selectedOrderKeys = emptySet() },
                                        modifier = Modifier.testTag("orders_clear_selection_button")
                                    ) {
                                        Text("Clear")
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (selectedOrders.isNotEmpty()) {
                                        onDispatchMultipleOrders(selectedOrders)
                                        selectedOrderKeys = emptySet()
                                    }
                                },
                                enabled = selectedOrders.isNotEmpty(),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dispatch_selected_orders_button")
                            ) {
                                Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Dispatch (${selectedOrders.size})", maxLines = 1)
                            }
                            Button(
                                onClick = {
                                    if (selectedOrders.isNotEmpty()) {
                                        onScheduleMultipleOrders(selectedOrders)
                                        selectedOrderKeys = emptySet()
                                    }
                                },
                                enabled = selectedOrders.isNotEmpty(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ForestEmerald,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("schedule_selected_orders_button")
                            ) {
                                Icon(Icons.Filled.AddTask, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Schedule (${selectedOrders.size})", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        if (filteredOrders.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when {
                            selectedOrderTab == OrdersViewTab.RUSH_ORDERS && rushOrders.isEmpty() -> {
                                "No rush orders logged yet. When adding an additional customer or direct product stop in Dispatch, enable 'Add to Orders → Rush Orders' and it will appear here."
                            }
                            state.orders.isEmpty() -> {
                                "No customer orders logged yet. Tap New Order to create an order and link it directly to Production or Dispatch."
                            }
                            else -> {
                                "No orders match your current search/status filter."
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            item {
                Text(
                    text = if (isDesktop) {
                        "Desktop Actions: Use the Schedule, Dispatch, Edit, or Delete buttons below each order card"
                    } else {
                        "Tip: Swipe right to Schedule / Dispatch • Swipe left to Edit / Delete"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            desktopGridItems(
                itemsList = filteredOrders,
                columns = gridColumns,
                keyPrefix = "order",
                itemKey = { it.id.ifBlank { it.orderCode } }
            ) { ord ->
                val skuSummary = summaryByProduct[ord.productName]
                val sku = skuByProduct[ord.productName]
                val linkedSchedules = state.schedules.filter {
                    val linkedIds = it.allLinkedOrderIds()
                    val linkedCodes = it.allLinkedOrderCodes()
                    ord.id in linkedIds ||
                        ord.orderCode in linkedCodes ||
                        (linkedIds.isEmpty() && linkedCodes.isEmpty() &&
                            it.productName == ord.productName &&
                            it.status != ScheduleStatus.COMPLETED.label &&
                            it.status != ScheduleStatus.CANCELLED.label)
                }
                val linkedDispatches = state.dispatches.filter { it.linkedOrderId == ord.id }

                val ordKey = ord.id.ifBlank { ord.orderCode }
                val isOrdActive = ord.status == OrderStatus.OPEN.label || ord.status == OrderStatus.PARTIALLY_DISPATCHED.label
                val triggerSchedule: () -> Unit = {
                    val resolvedSku = sku ?: SkuEntity(
                        productName = ord.productName,
                        section = PlantSection.SAN_ROQUE.label,
                        standardBagWeightKg = ord.bagWeightKg.coerceAtLeast(1.0),
                        defaultRemarks = "MIXING/CONVEYOR"
                    )
                    val remainingKg = max(0.0, ord.totalWeightKg - ord.dispatchedWeightKg)
                    val availKg = max(0.0, skuSummary?.availableStockKg ?: 0.0)
                    val shortfallKg = max(0.0, remainingKg - availKg)
                    val targetKg = if (shortfallKg > 0.0) shortfallKg else remainingKg
                    val bagWt = resolvedSku.standardBagWeightKg.coerceAtLeast(0.1)
                    val bags = ceil(targetKg / bagWt).toLong().coerceAtLeast(1L)
                    val priority = if (ord.priority || shortfallKg > 0.0) SchedulePriority.URGENT else SchedulePriority.ORDER_DRIVEN
                    onScheduleProductionForOrder(ord, resolvedSku, bags, priority)
                }
                val triggerDispatch: () -> Unit = {
                    val remainingKg = max(0.0, ord.totalWeightKg - ord.dispatchedWeightKg)
                    val availKg = max(0.0, skuSummary?.availableStockKg ?: 0.0)
                    if (availKg <= 0.01 && remainingKg > 0.0) {
                        flaggedDispatchOrder = ord to availKg
                    } else {
                        onFulfillOrderViaDispatch(ord)
                    }
                }
                SwipeLeftActionsContainer(
                    itemKey = ordKey,
                    shape = RoundedCornerShape(14.dp),
                    editTestTag = "swipe_edit_order_$ordKey",
                    deleteTestTag = "swipe_delete_order_$ordKey",
                    onEdit = { pendingEditOrder = ord },
                    onDelete = { pendingDeleteOrder = ord },
                    swipeRightEnabled = isOrdActive,
                    rightFirstLabel = "Schedule",
                    rightFirstTestTag = "swipe_schedule_order_$ordKey",
                    onRightFirst = triggerSchedule,
                    rightSecondLabel = "Dispatch",
                    rightSecondTestTag = "swipe_dispatch_order_$ordKey",
                    onRightSecond = triggerDispatch
                ) {
                    OrderLinkCard(
                        order = ord,
                        availableStockKg = skuSummary?.availableStockKg ?: 0.0,
                        linkedSchedules = linkedSchedules,
                        linkedDispatches = linkedDispatches,
                        multiSelectMode = multiSelectMode,
                        isSelected = ordKey in selectedOrderKeys,
                        onToggleSelect = {
                            selectedOrderKeys = if (ordKey in selectedOrderKeys) {
                                selectedOrderKeys - ordKey
                            } else {
                                selectedOrderKeys + ordKey
                            }
                        },
                        onTogglePriority = { onToggleOrderPriority(ord) },
                        onCancel = { pendingCancelOrder = ord },
                        onRetrieve = { pendingRetrieveOrder = ord },
                        onViewSchedule = onNavigateToSchedule
                    )
                }
            }
        }
    }

    flaggedDispatchOrder?.let { (ord, availKg) ->
        val remainingKg = max(0.0, ord.totalWeightKg - ord.dispatchedWeightKg)
        val shortfallKg = max(0.0, remainingKg - availKg)
        val resolvedSku = skuByProduct[ord.productName] ?: SkuEntity(
            productName = ord.productName,
            section = PlantSection.SAN_ROQUE.label,
            standardBagWeightKg = ord.bagWeightKg.coerceAtLeast(1.0),
            defaultRemarks = "MIXING/CONVEYOR"
        )
        val bagWt = resolvedSku.standardBagWeightKg.coerceAtLeast(0.1)
        val neededBags = ceil(shortfallKg / bagWt).toLong().coerceAtLeast(1L)
        AlertDialog(
            onDismissRequest = { flaggedDispatchOrder = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Insufficient stock warning",
                    tint = StatusCriticalTextDark
                )
            },
            title = { Text("Insufficient Available Stock") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Order ${ord.orderCode} (${ord.productName}) needs ${OperationsCalculator.formatKg(remainingKg)} kg, but available stock is ${OperationsCalculator.formatKg(availKg)} kg.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = StatusCriticalContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Backlogs: $neededBags bags (-${OperationsCalculator.formatKg(shortfallKg)} kg). Add new production or cancel dispatching.",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = OnStatusCriticalContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                flaggedDispatchOrder = null
                                onScheduleProductionForOrder(ord, resolvedSku, neededBags, SchedulePriority.URGENT)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForestEmerald,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("order_shortage_add_production_button")
                        ) {
                            Icon(Icons.Filled.AddTask, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add New Production ($neededBags bags)")
                        }
                        OutlinedButton(
                            onClick = { flaggedDispatchOrder = null },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCriticalTextDark),
                            border = BorderStroke(1.dp, StatusCriticalTextDark),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("order_shortage_cancel_dispatch_button")
                        ) {
                            Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancel Dispatching")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    pendingEditOrder?.let { ord ->
        val canEdit = ord.status == OrderStatus.OPEN.label || ord.status == OrderStatus.PARTIALLY_DISPATCHED.label
        val isCancelled = ord.status == OrderStatus.CANCELLED.label
        AlertDialog(
            onDismissRequest = { pendingEditOrder = null },
            icon = {
                Icon(
                    imageVector = if (isCancelled) Icons.Filled.Restore else Icons.Filled.Edit,
                    contentDescription = if (isCancelled) "Retrieve cancelled order" else "Edit order confirmation",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    when {
                        canEdit -> "Edit Order ${ord.orderCode}?"
                        isCancelled -> "Order ${ord.orderCode} is Cancelled"
                        else -> "Order Locked"
                    }
                )
            },
            text = {
                Text(
                    when {
                        canEdit -> "Confirm editing order ${ord.orderCode} for ${ord.customer} (${ord.productName}, ${ord.orderedBags} bags)?"
                        isCancelled -> "Order ${ord.orderCode} is currently Cancelled and locked from editing. Would you like to retrieve it back to active orders?"
                        else -> "Order ${ord.orderCode} is marked ${ord.status} and cannot be edited. You can delete it from the list instead."
                    }
                )
            },
            confirmButton = {
                when {
                    canEdit -> {
                        Button(
                            onClick = {
                                pendingEditOrder = null
                                onEditOrder(ord)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForestEmerald,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("confirm_edit_order_button")
                        ) {
                            Text("Edit Order")
                        }
                    }
                    isCancelled -> {
                        Button(
                            onClick = {
                                pendingEditOrder = null
                                onRetrieveOrder(ord)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForestEmerald,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("confirm_retrieve_from_locked_order_button")
                        ) {
                            Text("Retrieve Order")
                        }
                    }
                    else -> {
                        Button(
                            onClick = { pendingEditOrder = null },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForestEmerald,
                                contentColor = Color.White
                            )
                        ) {
                            Text("OK")
                        }
                    }
                }
            },
            dismissButton = {
                if (canEdit || isCancelled) {
                    TextButton(onClick = { pendingEditOrder = null }) {
                        Text(if (isCancelled) "Keep Cancelled" else "Cancel")
                    }
                }
            }
        )
    }

    pendingCancelOrder?.let { ord ->
        AlertDialog(
            onDismissRequest = { pendingCancelOrder = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = "Cancel order confirmation",
                    tint = StatusCriticalTextDark
                )
            },
            title = { Text("Cancel Order ${ord.orderCode}?") },
            text = {
                Text("Mark order ${ord.orderCode} for ${ord.customer} (${ord.productName}) as Cancelled?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingCancelOrder = null
                        onCancelOrder(ord)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusCritical,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_cancel_order_button")
                ) {
                    Text("Cancel Order")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCancelOrder = null }) { Text("Keep Order") }
            }
        )
    }

    pendingRetrieveOrder?.let { ord ->
        val restoredLabel = when {
            ord.dispatchedWeightKg >= ord.totalWeightKg - 0.01 -> OrderStatus.FULFILLED.label
            ord.dispatchedWeightKg > 0.0 -> OrderStatus.PARTIALLY_DISPATCHED.label
            else -> OrderStatus.OPEN.label
        }
        AlertDialog(
            onDismissRequest = { pendingRetrieveOrder = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Restore,
                    contentDescription = "Retrieve order confirmation",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Retrieve Order ${ord.orderCode}?") },
            text = {
                Text("Restore cancelled order ${ord.orderCode} for ${ord.customer} (${ord.productName}) back to $restoredLabel?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRetrieveOrder = null
                        onRetrieveOrder(ord)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestEmerald,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_retrieve_order_button")
                ) {
                    Text("Retrieve Order")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRetrieveOrder = null }) { Text("Keep Cancelled") }
            }
        )
    }

    pendingDeleteOrder?.let { ord ->
        AlertDialog(
            onDismissRequest = { pendingDeleteOrder = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete order confirmation",
                    tint = StatusCriticalTextDark
                )
            },
            title = { Text("Delete Order ${ord.orderCode}?") },
            text = {
                Text("Permanently delete order ${ord.orderCode} for ${ord.customer} (${ord.productName}, ${ord.orderedBags} bags)? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteOrder = null
                        onDeleteOrder(ord)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusCritical,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_order_button")
                ) {
                    Text("Delete Order")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteOrder = null }) { Text("Keep Order") }
            }
        )
    }
}

@Composable
private fun OrderLinkCard(
    order: CustomerOrder,
    availableStockKg: Double,
    linkedSchedules: List<ScheduleRun>,
    linkedDispatches: List<DispatchRecord>,
    multiSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onTogglePriority: () -> Unit,
    onCancel: () -> Unit,
    onRetrieve: () -> Unit,
    onViewSchedule: () -> Unit
) {
    val remainingKg = max(0.0, order.totalWeightKg - order.dispatchedWeightKg)
    val progress = if (order.totalWeightKg > 0.0) (order.dispatchedWeightKg / order.totalWeightKg).toFloat().coerceIn(0f, 1f) else 0f
    val isActive = order.status == OrderStatus.OPEN.label || order.status == OrderStatus.PARTIALLY_DISPATCHED.label
    val shortfallKg = max(0.0, remainingKg - max(0.0, availableStockKg))
    val ordKey = order.id.ifBlank { order.orderCode }

    val (bg, fg) = when (OrderStatus.fromRaw(order.status)) {
        OrderStatus.OPEN -> StatusWatchContainer to OnStatusWatchContainer
        OrderStatus.PARTIALLY_DISPATCHED -> StatusInProgressContainer to OnStatusInProgressContainer
        OrderStatus.FULFILLED -> StatusHealthyContainer to OnStatusHealthyContainer
        OrderStatus.CANCELLED -> StatusCriticalContainer to OnStatusCriticalContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = when {
                    isSelected -> 2.dp
                    order.priority -> 1.5.dp
                    else -> 1.dp
                },
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    order.priority -> GrainAmber
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
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
            // Top Row: Left Top = Highlighted Customer Name; Right Top = Clickable Priority + Small PO + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (multiSelectMode && isActive) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier = Modifier.testTag("order_checkbox_$ordKey")
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = order.customer,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    // Below Customer Name: Product Name / SKU
                    Text(
                        text = order.productName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (order.rushOrder) {
                            StatusPill(
                                label = "RUSH",
                                bgColor = StatusCriticalContainer,
                                textColor = OnStatusCriticalContainer
                            )
                        }
                        Text(
                            text = order.orderCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (order.status != OrderStatus.OPEN.label) {
                            StatusPill(order.status, bg, fg)
                        }
                    }
                    Surface(
                        onClick = { if (isActive) onTogglePriority() },
                        enabled = isActive,
                        color = if (order.priority) StatusWatchContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (order.priority) GrainAmber else MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.testTag("order_priority_toggle_$ordKey")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (order.priority) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = if (order.priority) "Priority order" else "Not priority order",
                                tint = if (order.priority) OnStatusWatchContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (order.priority) "PRIORITY" else "Not Priority",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (order.priority) OnStatusWatchContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isActive) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Tap to toggle priority",
                                    tint = if (order.priority) OnStatusWatchContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Below Product Name: Quantity x kg/bag = Total kg (from actual order data) & Dispatched progress
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${order.orderedBags} bags × ${order.bagWeightKg} kg/bag = ${OperationsCalculator.formatKg(order.totalWeightKg)} kg",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = "Dispatched: ${OperationsCalculator.formatKg(order.dispatchedWeightKg)} kg",
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

            // Below Quantity/Weight: Date Ordered & Target Delivery
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ordered: ${order.orderDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Target Delivery: ${order.dueDate}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Stock Coverage Indicator for active orders
            if (isActive) {
                Surface(
                    color = if (shortfallKg > 0.0) StatusCriticalContainer else StatusHealthyContainer,
                    border = if (shortfallKg > 0.0) BorderStroke(1.dp, StatusCritical) else null,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (shortfallKg > 0.0) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Stock backlogs flag",
                                tint = OnStatusCriticalContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = if (shortfallKg > 0.0) {
                                "FLAGGED Backlogs: -${OperationsCalculator.formatKg(shortfallKg)} kg (Avail: ${OperationsCalculator.formatKg(availableStockKg)} kg vs Remaining: ${OperationsCalculator.formatKg(remainingKg)} kg)"
                            } else {
                                "Stock Ready: ${OperationsCalculator.formatKg(availableStockKg)} kg available to fulfill ${OperationsCalculator.formatKg(remainingKg)} kg"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (shortfallKg > 0.0) OnStatusCriticalContainer else OnStatusHealthyContainer
                        )
                    }
                }
            }

            // Linked Production & Dispatch Traceability
            if (linkedSchedules.isNotEmpty() || linkedDispatches.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (linkedSchedules.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Linked Production (${linkedSchedules.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                TextButton(onClick = onViewSchedule, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                                    Text("Open Queue", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            linkedSchedules.forEach { run ->
                                Text(
                                    "• ${run.scheduledDate}: ${run.targetBags} bags (${OperationsCalculator.formatKg(run.expectedWeightKg)} kg) • ${run.lineRemarks} [${run.status}]",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (linkedSchedules.isNotEmpty() && linkedDispatches.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        }
                        if (linkedDispatches.isNotEmpty()) {
                            Text(
                                "Linked Dispatches (${linkedDispatches.size})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            linkedDispatches.forEach { d ->
                                Text(
                                    "• ${d.date} • ${d.drReference}: ${d.bagsDispatched} bags (${OperationsCalculator.formatKg(d.totalWeightKg)} kg)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Right Bottom: Cancel Order (active) or Retrieve Order (cancelled)
            if (isActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("order_cancel_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, StatusCriticalTextDark)
                    ) {
                        Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(16.dp), tint = StatusCriticalTextDark)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cancel Order", color = StatusCriticalTextDark, maxLines = 1)
                    }
                }
            } else if (order.status == OrderStatus.CANCELLED.label) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onRetrieve,
                        modifier = Modifier.testTag("order_retrieve_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForestEmerald,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retrieve Order", maxLines = 1)
                    }
                }
            }
        }
    }
}
