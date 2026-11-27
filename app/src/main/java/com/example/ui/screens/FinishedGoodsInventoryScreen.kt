package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import com.example.data.TabPreferencesStore
import com.example.data.local.FeedInventoryLevelEntity
import com.example.domain.OperationsCalculator
import com.example.model.PlantSection
import com.example.model.SchedulePriority
import com.example.model.SkuEntity
import com.example.model.SkuInventorySummary
import com.example.model.StockHealthStatus
import com.example.ui.components.IndustrialKpiCard
import com.example.ui.components.LocalFeedInventoryLedger
import com.example.ui.components.LocalIsDesktopLayout
import com.example.ui.components.LocalWindowWidthDp
import com.example.ui.components.PriorityProductsPickerDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.StockStatusBadge
import com.example.ui.components.desktopGridColumns
import com.example.ui.components.desktopGridItems
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.GrainAmber
import com.example.ui.theme.GrainAmberBright
import com.example.ui.theme.OnStatusCriticalContainer as OnCritCont
import com.example.ui.theme.OnStatusHealthyContainer
import com.example.ui.theme.OnStatusWatchContainer
import com.example.ui.theme.SlateTeal
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusCriticalContainer
import com.example.ui.theme.StatusCriticalTextDark
import com.example.ui.theme.StatusHealthy
import com.example.ui.theme.StatusHealthyContainer
import com.example.ui.theme.StatusHealthyTextDark
import com.example.ui.theme.StatusWatchContainer
import com.example.viewmodel.FeedInventorySortOption
import com.example.viewmodel.FeedInventoryUiState
import com.example.viewmodel.OpsUiState

private enum class FinishedGoodsSort(val label: String) {
    RISK("Risk First"),
    BAGS_ASC("Lowest Bags"),
    BAGS_DESC("Highest Stock"),
    NAME("Name A-Z")
}

private enum class FinishedGoodsHealthFilter(val label: String) {
    ALL("All"),
    NEEDS_PRODUCTION("Needs Production"),
    REPROCESSED("Reprocessed"),
    IN_REPROCESS("In Reprocess"),
    CRITICAL("Critical"),
    LOW_WATCH("Low Watch"),
    HEALTHY("Healthy")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FinishedGoodsInventoryScreen(
    state: OpsUiState.Ready,
    priorityProductKeys: Set<String> = emptySet(),
    hideNonPriority: Boolean = false,
    onToggleHideNonPriority: (Boolean) -> Unit = {},
    onToggleProductPriority: (SkuEntity) -> Unit = {},
    onSavePriorityProductKeys: (Set<String>) -> Unit = {},
    onResetPriorityToAutoActive: () -> Unit = {},
    onScheduleMultipleSkus: (List<Pair<SkuEntity, Long>>) -> Unit = {},
    onAddSku: () -> Unit,
    onEditSku: (SkuEntity) -> Unit,
    onManageSkus: () -> Unit,
    onOpenDispatchDialog: (SkuEntity?) -> Unit,
    onOpenScheduleDialog: (SkuEntity, Long, SchedulePriority) -> Unit,
    onOpenReprocessTab: () -> Unit = {},
    localInventoryState: FeedInventoryUiState = FeedInventoryUiState(isLoading = false),
    onLocalSearchChange: (String) -> Unit = {},
    onLocalUnitFilterChange: (String) -> Unit = {},
    onLocalLowStockOnlyChange: (Boolean) -> Unit = {},
    onLocalSortOptionChange: (FeedInventorySortOption) -> Unit = {},
    onLocalClearFilters: () -> Unit = {},
    onLocalSaveProduct: (Long, String, Double, String, String, String, Double, String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onLocalAdjustStock: (String, Double) -> Unit = { _, _ -> },
    onLocalDeleteItem: (FeedInventoryLevelEntity) -> Unit = {}
) {
    var activeSubTab by remember { mutableIntStateOf(0) }
    BackHandler(enabled = activeSubTab == 1) { activeSubTab = 0 }

    var searchQuery by remember { mutableStateOf("") }
    var sectionFilter by remember { mutableStateOf("ALL") }
    var healthFilter by remember { mutableStateOf(FinishedGoodsHealthFilter.ALL) }
    var sortMode by remember { mutableStateOf(FinishedGoodsSort.RISK) }
    var multiSelectMode by remember { mutableStateOf(false) }
    var selectedProductKeys by remember { mutableStateOf(emptySet<String>()) }
    var showPriorityPicker by remember { mutableStateOf(false) }

    val priorityTotalCount = remember(state.summaries, priorityProductKeys) {
        state.summaries.count { TabPreferencesStore.skuPriorityKey(it.sku) in priorityProductKeys }
    }
    val hiddenNonPriorityCount = (state.summaries.size - priorityTotalCount).coerceAtLeast(0)

    val visibleScopeSummaries = remember(state.summaries, hideNonPriority, priorityProductKeys) {
        if (hideNonPriority) {
            state.summaries.filter { TabPreferencesStore.skuPriorityKey(it.sku) in priorityProductKeys }
        } else {
            state.summaries
        }
    }

    val countsBySection = remember(visibleScopeSummaries) {
        visibleScopeSummaries.groupingBy { it.sku.section }.eachCount()
    }
    val criticalCount = remember(visibleScopeSummaries) {
        visibleScopeSummaries.count { it.status == StockHealthStatus.CRITICAL }
    }
    val watchCount = remember(visibleScopeSummaries) {
        visibleScopeSummaries.count { it.status == StockHealthStatus.LOW_WATCH }
    }
    val healthyCount = remember(visibleScopeSummaries) {
        visibleScopeSummaries.count { it.status == StockHealthStatus.HEALTHY }
    }
    val needsProdCount = remember(visibleScopeSummaries) {
        visibleScopeSummaries.count { it.netProductionNeededKg > 0.0 }
    }
    val reprocessedSkuCount = remember(visibleScopeSummaries) {
        visibleScopeSummaries.count { it.hasReprocessedFlag }
    }
    val inReprocessSkuCount = remember(visibleScopeSummaries) {
        visibleScopeSummaries.count { it.pendingReprocessKg > 0.0 }
    }
    val pendingReprocessItems = remember(state.reprocessItems) {
        state.reprocessItems.filter { it.isPending() }
    }
    val fastActionReprocessCount = remember(pendingReprocessItems) {
        pendingReprocessItems.count { it.fastActionNeeded }
    }
    val pendingReprocessKgTotal = remember(pendingReprocessItems) {
        pendingReprocessItems.sumOf { it.returnedWeightKg }
    }

    val filteredSummaries = remember(
        visibleScopeSummaries,
        priorityProductKeys,
        searchQuery,
        sectionFilter,
        healthFilter,
        sortMode
    ) {
        val base = visibleScopeSummaries.filter { item ->
            val matchesSection = sectionFilter == "ALL" || item.sku.section == sectionFilter
            val matchesHealth = when (healthFilter) {
                FinishedGoodsHealthFilter.ALL -> true
                FinishedGoodsHealthFilter.NEEDS_PRODUCTION -> item.netProductionNeededKg > 0.0
                FinishedGoodsHealthFilter.REPROCESSED -> item.hasReprocessedFlag
                FinishedGoodsHealthFilter.IN_REPROCESS -> item.pendingReprocessKg > 0.0
                FinishedGoodsHealthFilter.CRITICAL -> item.status == StockHealthStatus.CRITICAL
                FinishedGoodsHealthFilter.LOW_WATCH -> item.status == StockHealthStatus.LOW_WATCH
                FinishedGoodsHealthFilter.HEALTHY -> item.status == StockHealthStatus.HEALTHY
            }
            val matchesSearch = searchQuery.isBlank() ||
                item.sku.productName.contains(searchQuery, ignoreCase = true) ||
                item.sku.section.contains(searchQuery, ignoreCase = true) ||
                item.sku.defaultRemarks.contains(searchQuery, ignoreCase = true)
            matchesSection && matchesHealth && matchesSearch
        }

        when (sortMode) {
            FinishedGoodsSort.RISK -> base.sortedWith(
                compareByDescending<SkuInventorySummary> {
                    TabPreferencesStore.skuPriorityKey(it.sku) in priorityProductKeys
                }.thenBy {
                    when (it.status) {
                        StockHealthStatus.CRITICAL -> 0
                        StockHealthStatus.LOW_WATCH -> 1
                        StockHealthStatus.HEALTHY -> 2
                    }
                }.thenByDescending { it.netProductionNeededKg }
                    .thenBy { it.sku.productName.lowercase() }
            )
            FinishedGoodsSort.BAGS_ASC -> base.sortedBy { it.availableBags }
            FinishedGoodsSort.BAGS_DESC -> base.sortedByDescending { it.availableBags }
            FinishedGoodsSort.NAME -> base.sortedBy { it.sku.productName.lowercase() }
        }
    }

    val selectedSummaries = remember(filteredSummaries, selectedProductKeys) {
        filteredSummaries.filter { TabPreferencesStore.skuPriorityKey(it.sku) in selectedProductKeys }
    }

    val isDesktop = LocalIsDesktopLayout.current
    val windowWidthDp = LocalWindowWidthDp.current
    val gridColumns = desktopGridColumns(windowWidthDp, maxCols = 3)

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeSubTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = {
                    Text(
                        text = "Live Plant Stock (${state.summaries.size})",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.testTag("fg_subtab_live_stock")
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = {
                    Text(
                        text = "Room Ledger (${localInventoryState.totalTrackedProducts})",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.testTag("fg_subtab_room_ledger")
            )
        }

        if (activeSubTab == 1) {
            LocalFeedInventoryLedger(
                state = localInventoryState,
                onSearchQueryChange = onLocalSearchChange,
                onUnitFilterChange = onLocalUnitFilterChange,
                onLowStockOnlyChange = onLocalLowStockOnlyChange,
                onSortOptionChange = onLocalSortOptionChange,
                onClearFilters = onLocalClearFilters,
                onSaveProduct = onLocalSaveProduct,
                onAdjustStock = onLocalAdjustStock,
                onDeleteItem = onLocalDeleteItem,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("finished_goods_screen"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Finished Goods Inventory",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Beginning + Recovered - Dispatched = Available feed bags",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onManageSkus,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("fg_manage_skus_button")
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Manage (${state.skus.size})", maxLines = 1)
                    }
                    Button(
                        onClick = onAddSku,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForestEmerald,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("fg_add_sku_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Product", maxLines = 1)
                    }
                }
            }
        }

        // Fast Action Reprocess Alert Banner
        if (pendingReprocessItems.isNotEmpty()) {
            item {
                val isFast = fastActionReprocessCount > 0
                Surface(
                    color = if (isFast) StatusCriticalContainer else StatusWatchContainer,
                    border = BorderStroke(1.dp, if (isFast) StatusCritical else GrainAmber),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fg_reprocess_fast_action_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isFast) Icons.Filled.Bolt else Icons.Filled.Autorenew,
                            contentDescription = "Reprocess Fast Action",
                            tint = if (isFast) OnCritCont else OnStatusWatchContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (isFast) {
                                    "REPROCESS FAST ACTION NEEDED ($fastActionReprocessCount lot${if (fastActionReprocessCount == 1) "" else "s"})"
                                } else {
                                    "RETURNED FEED IN REPROCESS (${pendingReprocessItems.size} lot${if (pendingReprocessItems.size == 1) "" else "s"})"
                                },
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (isFast) OnCritCont else OnStatusWatchContainer
                            )
                            Text(
                                text = "${OperationsCalculator.formatKg(pendingReprocessKgTotal)} kg returned feed awaiting rework & inventory re-flagging.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isFast) OnCritCont else OnStatusWatchContainer
                            )
                        }
                        Button(
                            onClick = onOpenReprocessTab,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFast) StatusCritical else GrainAmber,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("fg_open_reprocess_button")
                        ) {
                            Text("Reprocess Tab", maxLines = 1)
                        }
                    }
                }
            }
        }

        // KPI Strip
        item {
            Row(
                modifier = if (isDesktop) {
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                },
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IndustrialKpiCard(
                    title = "AVAILABLE STOCK",
                    primaryValue = "${OperationsCalculator.formatBags(state.kpis.totalAvailableBags)} bags",
                    secondaryValue = "${OperationsCalculator.formatKg(state.kpis.totalAvailableStockKg)} kg across ${state.skus.size} products",
                    icon = Icons.Filled.Inventory2,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(195.dp)
                )
                IndustrialKpiCard(
                    title = "HEALTH STATUS",
                    primaryValue = "$healthyCount Healthy",
                    secondaryValue = "$watchCount Low Watch • $criticalCount Critical",
                    icon = Icons.Filled.Scale,
                    accentColor = StatusHealthyTextDark,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(185.dp)
                )
                IndustrialKpiCard(
                    title = "NEEDS MILLING",
                    primaryValue = "$needsProdCount Products",
                    secondaryValue = "${state.kpis.criticalSkuCount} critical / zero stock",
                    icon = Icons.Filled.Warning,
                    accentColor = if (needsProdCount > 0) StatusCriticalTextDark else StatusHealthyTextDark,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(185.dp)
                )
                IndustrialKpiCard(
                    title = "OPEN ORDERS",
                    primaryValue = "${OperationsCalculator.formatKg(state.kpis.totalOpenOrdersKg)} kg",
                    secondaryValue = "${OperationsCalculator.formatKg(state.kpis.totalDispatchedKg)} kg dispatched",
                    icon = Icons.Filled.ShoppingCart,
                    accentColor = SlateTeal,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(190.dp)
                )
            }
        }

        // Search + Filter + Sort
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search finished goods") },
                        trailingIcon = if (searchQuery.isNotBlank()) {
                            {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("finished_goods_clear_search_button")
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear finished goods search")
                                }
                            }
                        } else null,
                        placeholder = { Text("Search feed product, section, or line...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("finished_goods_search_input")
                    )
                    OutlinedButton(
                        onClick = { onOpenDispatchDialog(null) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("fg_quick_dispatch_button")
                    ) {
                        Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dispatch", maxLines = 1)
                    }
                }

                // Priority Visibility & Selection Controls
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = hideNonPriority,
                        onClick = { onToggleHideNonPriority(!hideNonPriority) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (hideNonPriority) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (hideNonPriority) GrainAmberBright else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                if (hideNonPriority) {
                                    "Priority Only ($priorityTotalCount)"
                                } else {
                                    "Hide Non-Priority ($hiddenNonPriorityCount)"
                                }
                            )
                        },
                        modifier = Modifier.testTag("fg_hide_non_priority_chip")
                    )
                    FilterChip(
                        selected = showPriorityPicker,
                        onClick = { showPriorityPicker = true },
                        leadingIcon = {
                            Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Select Priority ($priorityTotalCount/${state.summaries.size})") },
                        modifier = Modifier.testTag("fg_select_priority_button")
                    )
                    FilterChip(
                        selected = multiSelectMode,
                        onClick = {
                            multiSelectMode = !multiSelectMode
                            if (!multiSelectMode) selectedProductKeys = emptySet()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (multiSelectMode) Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(if (multiSelectMode) "Checkboxes On" else "Select") },
                        modifier = Modifier.testTag("fg_toggle_multiselect")
                    )
                }

                AnimatedVisibility(visible = multiSelectMode) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedSummaries.isEmpty()) {
                                        "Tap checkboxes to select products"
                                    } else {
                                        "${selectedSummaries.size} product(s) selected"
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(
                                        onClick = {
                                            selectedProductKeys = selectedProductKeys +
                                                filteredSummaries.map { TabPreferencesStore.skuPriorityKey(it.sku) }
                                        },
                                        modifier = Modifier.testTag("fg_multiselect_all_button")
                                    ) {
                                        Text("All Visible")
                                    }
                                    if (selectedProductKeys.isNotEmpty()) {
                                        TextButton(
                                            onClick = { selectedProductKeys = emptySet() },
                                            modifier = Modifier.testTag("fg_multiselect_clear_button")
                                        ) {
                                            Text("Clear")
                                        }
                                    }
                                }
                            }
                            if (selectedSummaries.isNotEmpty()) {
                                val selectedKeySet = selectedSummaries.map {
                                    TabPreferencesStore.skuPriorityKey(it.sku)
                                }.toSet()
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onSavePriorityProductKeys(priorityProductKeys + selectedKeySet)
                                            selectedProductKeys = emptySet()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ForestEmerald,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("fg_batch_mark_priority_button")
                                    ) {
                                        Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Mark Priority")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onSavePriorityProductKeys(priorityProductKeys - selectedKeySet)
                                            selectedProductKeys = emptySet()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("fg_batch_unmark_priority_button")
                                    ) {
                                        Icon(Icons.Outlined.StarBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Unmark / Hide")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val items = selectedSummaries.map { item ->
                                                item.sku to item.suggestedBags.coerceAtLeast(20L)
                                            }
                                            onScheduleMultipleSkus(items)
                                            selectedProductKeys = emptySet()
                                            multiSelectMode = false
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("fg_batch_schedule_button")
                                    ) {
                                        Icon(Icons.Filled.AddTask, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Schedule (${selectedSummaries.size})")
                                    }
                                }
                            }
                        }
                    }
                }

                // Section chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (listOf("ALL") + PlantSection.entries.map { it.label }).forEach { sec ->
                        val count = if (sec == "ALL") visibleScopeSummaries.size else (countsBySection[sec] ?: 0)
                        FilterChip(
                            selected = sectionFilter == sec,
                            onClick = { sectionFilter = sec },
                            label = { Text(if (sec == "ALL") "All Sections ($count)" else "$sec ($count)") }
                        )
                    }
                }

                // Health + Sort chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FinishedGoodsHealthFilter.entries.forEach { hf ->
                        val countLabel = when (hf) {
                            FinishedGoodsHealthFilter.ALL -> "${hf.label} (${visibleScopeSummaries.size})"
                            FinishedGoodsHealthFilter.NEEDS_PRODUCTION -> "${hf.label} ($needsProdCount)"
                            FinishedGoodsHealthFilter.REPROCESSED -> "${hf.label} ($reprocessedSkuCount)"
                            FinishedGoodsHealthFilter.IN_REPROCESS -> "${hf.label} ($inReprocessSkuCount)"
                            FinishedGoodsHealthFilter.CRITICAL -> "${hf.label} ($criticalCount)"
                            FinishedGoodsHealthFilter.LOW_WATCH -> "${hf.label} ($watchCount)"
                            FinishedGoodsHealthFilter.HEALTHY -> "${hf.label} ($healthyCount)"
                        }
                        FilterChip(
                            selected = healthFilter == hf,
                            onClick = { healthFilter = hf },
                            label = { Text(countLabel) },
                            modifier = Modifier.testTag("fg_filter_${hf.name.lowercase()}")
                        )
                    }
                    FinishedGoodsSort.entries.forEach { sm ->
                        FilterChip(
                            selected = sortMode == sm,
                            onClick = { sortMode = sm },
                            label = { Text("Sort: ${sm.label}") }
                        )
                    }
                }
            }
        }

        if (filteredSummaries.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val hiddenAllByPriority = hideNonPriority && priorityTotalCount == 0 && state.summaries.isNotEmpty()
                        Text(
                            text = when {
                                state.skus.isEmpty() -> "No finished goods SKUs loaded yet."
                                hiddenAllByPriority -> "All non-priority products are currently hidden."
                                else -> "No finished goods match your current filters."
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = when {
                                state.skus.isEmpty() -> "Tap + Add SKU above or use Reset to restore Beginning Inventory."
                                hideNonPriority -> "Tap Select Priority to choose which products to display, or show all ${state.summaries.size} products."
                                else -> "Clear the search bar or pick another plant section / health chip."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (hideNonPriority && state.summaries.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showPriorityPicker = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ForestEmerald,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Text("Select Priority")
                                }
                                OutlinedButton(
                                    onClick = { onToggleHideNonPriority(false) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Text("Show All (${state.summaries.size})")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            desktopGridItems(
                itemsList = filteredSummaries,
                columns = gridColumns,
                keyPrefix = "fg_sku",
                itemKey = { it.sku.id.ifBlank { "${it.sku.section}_${it.sku.productName}" } }
            ) { item ->
                val pKey = TabPreferencesStore.skuPriorityKey(item.sku)
                val isPriority = pKey in priorityProductKeys
                val isSelected = pKey in selectedProductKeys
                val toggleSelect = {
                    selectedProductKeys = if (isSelected) selectedProductKeys - pKey else selectedProductKeys + pKey
                }
                FinishedGoodsSkuCard(
                    item = item,
                    isPriority = isPriority,
                    multiSelectMode = multiSelectMode,
                    isSelected = isSelected,
                    onToggleSelect = toggleSelect,
                    onTogglePriority = { onToggleProductPriority(item.sku) },
                    onEdit = { onEditSku(item.sku) },
                    onDispatch = { onOpenDispatchDialog(item.sku) },
                    onSchedule = {
                        val bags = item.suggestedBags.coerceAtLeast(20L)
                        val priority = item.recommendedPriority ?: SchedulePriority.BUFFER_TOP_UP
                        onOpenScheduleDialog(item.sku, bags, priority)
                    },
                    onOpenReprocessTab = onOpenReprocessTab
                )
            }
        }
            }
        }
    }

    if (showPriorityPicker) {
        PriorityProductsPickerDialog(
            summaries = state.summaries,
            selectedKeys = priorityProductKeys,
            onDismiss = { showPriorityPicker = false },
            onSaveSelection = { keys ->
                onSavePriorityProductKeys(keys)
                showPriorityPicker = false
            },
            onResetAutoActive = {
                onResetPriorityToAutoActive()
                showPriorityPicker = false
            }
        )
    }
}

@Composable
private fun FinishedGoodsSkuCard(
    item: SkuInventorySummary,
    isPriority: Boolean,
    multiSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onTogglePriority: () -> Unit,
    onEdit: () -> Unit,
    onDispatch: () -> Unit,
    onSchedule: () -> Unit,
    onOpenReprocessTab: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isSelected || isPriority) 1.5.dp else 1.dp,
            color = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isPriority -> GrainAmberBright.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (multiSelectMode) Modifier.clickable { onToggleSelect() } else Modifier
            )
            .testTag("fg_sku_card_${item.sku.id}")
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
                if (multiSelectMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("fg_select_${item.sku.id}")
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.sku.productName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.sku.section} • ${item.sku.standardBagWeightKg} kg/bag • ${item.sku.defaultRemarks}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StockStatusBadge(status = item.status)
                    IconButton(
                        onClick = onTogglePriority,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("fg_star_${item.sku.id}")
                    ) {
                        Icon(
                            imageVector = if (isPriority) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (isPriority) {
                                "Remove ${item.sku.productName} from priority"
                            } else {
                                "Mark ${item.sku.productName} as priority"
                            },
                            tint = if (isPriority) GrainAmberBright else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("fg_edit_sku_${item.sku.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit ${item.sku.productName}",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (item.hasReprocessedFlag || item.pendingReprocessKg > 0.0 || item.damagedReprocessKg > 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.hasReprocessedFlag) {
                        Surface(
                            color = StatusHealthyContainer,
                            border = BorderStroke(1.dp, StatusHealthy.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("fg_reprocessed_badge_${item.sku.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Autorenew,
                                    contentDescription = "Reprocessed in Inventory",
                                    tint = OnStatusHealthyContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "REPROCESSED • +${OperationsCalculator.formatKg(item.reprocessedKg)} kg (${item.reprocessedBags} bags)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = OnStatusHealthyContainer
                                )
                            }
                        }
                    }
                    if (item.pendingReprocessKg > 0.0) {
                        val isFast = item.pendingFastActionCount > 0
                        Surface(
                            onClick = onOpenReprocessTab,
                            color = if (isFast) StatusCriticalContainer else StatusWatchContainer,
                            border = BorderStroke(1.dp, if (isFast) StatusCritical else GrainAmber),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("fg_pending_reprocess_badge_${item.sku.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFast) Icons.Filled.Bolt else Icons.Filled.Autorenew,
                                    contentDescription = "Pending Reprocess",
                                    tint = if (isFast) OnCritCont else OnStatusWatchContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "IN REPROCESS: ${OperationsCalculator.formatKg(item.pendingReprocessKg)} kg (${item.pendingReprocessBags} bags)${if (isFast) " • NEED FAST ACTION" else ""}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isFast) OnCritCont else OnStatusWatchContainer
                                )
                            }
                        }
                    }
                    if (item.damagedReprocessKg > 0.0) {
                        Surface(
                            onClick = onOpenReprocessTab,
                            color = StatusCriticalContainer,
                            border = BorderStroke(1.dp, StatusCritical),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("fg_damaged_reprocess_badge_${item.sku.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = "Damaged Reprocess Write-Off",
                                    tint = OnCritCont,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "DAMAGED: -${OperationsCalculator.formatKg(item.damagedReprocessKg)} kg (${item.damagedReprocessBags} bags)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = OnCritCont
                                )
                            }
                        }
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
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FinishedGoodsMetricCell("BEGINNING", "${OperationsCalculator.formatKg(item.openingStockKg)} kg")
                    FinishedGoodsMetricCell("RECOVERED", "+${OperationsCalculator.formatKg(item.recoveredKg)} kg")
                    FinishedGoodsMetricCell("DISPATCHED", "-${OperationsCalculator.formatKg(item.dispatchedKg)} kg")
                    FinishedGoodsMetricCell(
                        "AVAILABLE",
                        "${OperationsCalculator.formatKg(item.availableStockKg)} kg\n(${OperationsCalculator.formatBags(item.availableBags)} bags)",
                        highlight = true
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val coverStr = if (item.daysOfCover.isInfinite()) "∞ days" else "${String.format("%.1f", item.daysOfCover)} days"
                FinishedGoodsMetricCell("AVG DISPATCH", "${OperationsCalculator.formatKg(item.avgDailyDispatchKg)} kg/d")
                FinishedGoodsMetricCell("COVER", coverStr)
                FinishedGoodsMetricCell("ORDERS", "${OperationsCalculator.formatKg(item.openOrdersKg)} kg")
                FinishedGoodsMetricCell(
                    "NEEDED",
                    if (item.netProductionNeededKg > 0) "${item.suggestedBags} bags" else "0 bags"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDispatch,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dispatch")
                }
                Button(
                    onClick = onSchedule,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.netProductionNeededKg > 0) GrainAmber else ForestEmerald,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.AddTask, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (item.suggestedBags > 0) "Schedule ${item.suggestedBags}b" else "Schedule Run")
                }
            }
        }
    }
}

@Composable
private fun FinishedGoodsMetricCell(label: String, value: String, highlight: Boolean = false) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
