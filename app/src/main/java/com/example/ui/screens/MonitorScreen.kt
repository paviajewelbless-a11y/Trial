package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.example.domain.OperationsCalculator
import com.example.model.BomIngredientLine
import com.example.model.FeedBomEntity
import com.example.model.MaterialSector
import com.example.model.PlantMaterialCatalog
import com.example.model.PlantSection
import com.example.model.RawMaterialCategory
import com.example.model.RawMaterialEntity
import com.example.model.RawMaterialProjection
import com.example.model.RawMaterialRiskStatus
import com.example.model.SchedulePriority
import com.example.model.SkuEntity
import com.example.model.SkuInventorySummary
import com.example.ui.components.AddRawMaterialsTabbedWorkspace
import com.example.ui.components.BomRecipeCard
import com.example.ui.components.IndustrialKpiCard
import com.example.ui.components.LocalIsDesktopLayout
import com.example.ui.components.LocalWindowWidthDp
import com.example.ui.components.PriorityProductsPickerDialog
import com.example.ui.components.RawMaterialAdjustStockDialog
import com.example.ui.components.RawMaterialEditorDialog
import com.example.ui.components.RawMaterialForecastChartCard
import com.example.ui.components.RawMaterialNotificationBanner
import com.example.ui.components.RawMaterialProjectionCard
import com.example.ui.components.RawMaterialSimulatorCard
import com.example.ui.components.StockStatusBadge
import com.example.ui.components.StockVsDemandBarChart
import com.example.ui.components.desktopGridColumns
import com.example.ui.components.desktopGridItems
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.GrainAmber
import com.example.ui.theme.GrainAmberBright
import com.example.ui.theme.StatusCriticalTextDark
import com.example.ui.theme.StatusHealthyTextDark
import com.example.ui.theme.StatusWatchTextDark
import com.example.viewmodel.OpsUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonitorScreen(
    state: OpsUiState.Ready,
    lookbackDays: Int,
    safetyBufferPct: Double?,
    initialMonitorView: Int = 0,
    onConsumeDeepLink: () -> Unit = {},
    demandMultiplier: Double = 1.0,
    includeScheduled: Boolean = false,
    leadOverrideDays: Long? = null,
    hasNotificationPermission: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    onSendNotificationNow: () -> Unit = {},
    onChangeLookback: (Int) -> Unit,
    onChangeSafetyBuffer: (Double?) -> Unit,
    onChangeDemandMultiplier: (Double) -> Unit = {},
    onToggleIncludeScheduled: (Boolean) -> Unit = {},
    onChangeLeadOverrideDays: (Long?) -> Unit = {},
    onResetSimulator: () -> Unit = {},
    onResetFilters: () -> Unit,
    onAddSku: () -> Unit,
    onEditSku: (SkuEntity) -> Unit,
    onManageSkus: () -> Unit,
    onOpenDispatchDialog: (SkuEntity?) -> Unit,
    onOpenScheduleDialog: (SkuEntity, Long, SchedulePriority) -> Unit,
    priorityProductKeys: Set<String> = emptySet(),
    hideNonPriority: Boolean = false,
    onToggleHideNonPriority: (Boolean) -> Unit = {},
    onToggleProductPriority: (SkuEntity) -> Unit = {},
    onSavePriorityProductKeys: (Set<String>) -> Unit = {},
    onResetPriorityToAutoActive: () -> Unit = {},
    onScheduleMultipleSkus: (List<Pair<SkuEntity, Long>>) -> Unit = {},
    onRestockRawMaterial: (RawMaterialEntity, Double, String) -> Unit = { _, _, _ -> },
    onToggleRawMaterialAlert: (RawMaterialEntity) -> Unit = {},
    onSaveRawMaterial: (
        RawMaterialEntity?,
        String,
        RawMaterialCategory,
        Double,
        Double,
        Double,
        Long,
        Long,
        String,
        Boolean
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onDeleteRawMaterial: (RawMaterialEntity) -> Unit = {},
    onAddRawMaterialsBulk: (List<RawMaterialEntity>) -> Unit = {},
    onSaveBomWithNewMaterials: (
        List<RawMaterialEntity>,
        FeedBomEntity?,
        String,
        PlantSection,
        Double,
        Double,
        List<BomIngredientLine>,
        String
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onDeleteBom: (FeedBomEntity) -> Unit = {},
    onOpenFeedBomScreen: () -> Unit = {},
    onOpenFinishedGoodsTab: () -> Unit = {},
    onOpenMacroTab: () -> Unit = {},
    onOpenMicroTab: () -> Unit = {},
    onOpenProductionMonitor: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var sectionFilter by remember { mutableStateOf("ALL") }
    var multiSelectSkus by remember { mutableStateOf(false) }
    var selectedSkuKeys by remember { mutableStateOf(emptySet<String>()) }
    var showPriorityPicker by remember { mutableStateOf(false) }
    var rawSearchQuery by remember { mutableStateOf("") }
    var rawCategoryFilter by remember { mutableStateOf("ALL") }
    var monitorView by remember { mutableIntStateOf(initialMonitorView.coerceIn(0, 2)) } // 0 = SKUs, 1 = Raw Materials, 2 = Charts & Dispatches
    var rawSubTab by remember { mutableIntStateOf(0) } // 0 = Stock & Alerts, 1 = BOM Recipes, 2 = + Add (BOM)
    var bomSearchQuery by remember { mutableStateOf("") }
    var bomSectionFilter by remember { mutableStateOf("ALL") }
    var preloadedBomForStudio by remember { mutableStateOf<FeedBomEntity?>(null) }
    var adjustingRawProjection by remember { mutableStateOf<Pair<RawMaterialProjection, Boolean>?>(null) }
    var editingRawMaterial by remember { mutableStateOf<Pair<Boolean, RawMaterialEntity?>?>(null) }

    BackHandler(enabled = monitorView == 1 && rawSubTab != 0) {
        rawSubTab = 0
    }

    LaunchedEffect(initialMonitorView) {
        if (initialMonitorView in 1..2) {
            monitorView = initialMonitorView
            onConsumeDeepLink()
        }
    }

    val urgentRawCount = remember(state.rawMaterialProjections) {
        state.rawMaterialProjections.count { it.status != RawMaterialRiskStatus.HEALTHY }
    }

    val missingCatalogItems = remember(state.rawMaterials) {
        PlantMaterialCatalog.missingCatalogEntities(state.rawMaterials)
    }

    val filteredRawProjections = remember(state.rawMaterialProjections, rawSearchQuery, rawCategoryFilter) {
        state.rawMaterialProjections.filter { proj ->
            val mat = proj.material
            val sector = PlantMaterialCatalog.inferSector(mat)
            val movementTag = PlantMaterialCatalog.inferMovementTag(mat)
            val matchesFilter = when {
                rawCategoryFilter == "ALL" -> true
                rawCategoryFilter == "URGENT" -> proj.status != RawMaterialRiskStatus.HEALTHY
                rawCategoryFilter.startsWith("SECTOR:") -> {
                    sector.name == rawCategoryFilter.removePrefix("SECTOR:")
                }
                else -> mat.category == rawCategoryFilter
            }
            val matchesQuery = rawSearchQuery.isBlank() ||
                mat.name.contains(rawSearchQuery, ignoreCase = true) ||
                mat.supplierName.contains(rawSearchQuery, ignoreCase = true) ||
                mat.category.contains(rawSearchQuery, ignoreCase = true) ||
                movementTag.label.contains(rawSearchQuery, ignoreCase = true) ||
                sector.label.contains(rawSearchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    val filteredBoms = remember(state.boms, bomSearchQuery, bomSectionFilter) {
        state.boms.filter { bom ->
            val matchesSection = bomSectionFilter == "ALL" || bom.section == bomSectionFilter
            val matchesQuery = bomSearchQuery.isBlank() ||
                bom.productName.contains(bomSearchQuery, ignoreCase = true) ||
                bom.lines.any { it.materialName.contains(bomSearchQuery, ignoreCase = true) }
            matchesSection && matchesQuery
        }
    }

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

    val filteredSummaries = remember(visibleScopeSummaries, priorityProductKeys, searchQuery, sectionFilter) {
        visibleScopeSummaries
            .filter { item ->
                (sectionFilter == "ALL" || item.sku.section == sectionFilter) &&
                    (searchQuery.isBlank() ||
                        item.sku.productName.contains(searchQuery, ignoreCase = true) ||
                        item.sku.section.contains(searchQuery, ignoreCase = true))
            }
            .sortedByDescending { TabPreferencesStore.skuPriorityKey(it.sku) in priorityProductKeys }
    }

    val selectedSummaries = remember(filteredSummaries, selectedSkuKeys) {
        filteredSummaries.filter { TabPreferencesStore.skuPriorityKey(it.sku) in selectedSkuKeys }
    }

    val isDesktop = LocalIsDesktopLayout.current
    val windowWidthDp = LocalWindowWidthDp.current
    val gridColumns = desktopGridColumns(windowWidthDp, maxCols = 3)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Inventory & SKU Monitor",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Beginning + Recovered - Dispatched = Available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { onOpenDispatchDialog(null) },
                    modifier = Modifier.testTag("log_dispatch_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestEmerald,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dispatch")
                }
            }
        }

        // KPI Summary Strip
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
                    title = "AVAILABLE STOCK",
                    primaryValue = "${OperationsCalculator.formatKg(state.kpis.totalAvailableStockKg)} kg",
                    secondaryValue = "${OperationsCalculator.formatBags(state.kpis.totalAvailableBags)} bags across ${state.skus.size} products",
                    icon = Icons.Filled.Inventory2,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = (if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(205.dp))
                )
                IndustrialKpiCard(
                    title = "TOTAL DISPATCHED",
                    primaryValue = "${OperationsCalculator.formatKg(state.kpis.totalDispatchedKg)} kg",
                    secondaryValue = "${state.dispatches.size} DR deliveries",
                    icon = Icons.Filled.LocalShipping,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = (if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(180.dp))
                )
                IndustrialKpiCard(
                    title = "OPEN ORDERS",
                    primaryValue = "${OperationsCalculator.formatKg(state.kpis.totalOpenOrdersKg)} kg",
                    secondaryValue = "${state.orders.size} customer orders",
                    icon = Icons.Filled.ShoppingCart,
                    accentColor = GrainAmberBright,
                    modifier = (if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(180.dp))
                )
                IndustrialKpiCard(
                    title = "AVG RECOVERY",
                    primaryValue = String.format("%.2f%%", state.kpis.averageRecoveryPct),
                    secondaryValue = "${state.batches.size} batches logged",
                    icon = Icons.Filled.Speed,
                    accentColor = StatusHealthyTextDark,
                    modifier = (if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(175.dp))
                )
                IndustrialKpiCard(
                    title = "NEED PRODUCTION",
                    primaryValue = "${state.kpis.skusNeedingProduction} Products",
                    secondaryValue = "${state.kpis.criticalSkuCount} zero/critical stock",
                    icon = Icons.Filled.Warning,
                    accentColor = StatusCriticalTextDark,
                    modifier = (if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(185.dp))
                )
            }
        }

        // Switch between Product Name Inventory, Raw Materials Forecast, and Charts & Dispatches
        item {
            val rawLabel = if (urgentRawCount > 0) {
                "Raw Mat • !$urgentRawCount"
            } else {
                "Raw Mat (${state.rawMaterials.size})"
            }
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(
                    "Product Name (${state.skus.size})",
                    rawLabel,
                    "Charts (${state.dispatches.size})"
                ).forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = monitorView == idx,
                        onClick = { monitorView = idx },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = 3),
                        modifier = Modifier.testTag("monitor_subtab_$idx")
                    ) {
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        if (monitorView == 0) {
            // Search, Location Chips, Add SKU & Reset
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search product names") },
                        placeholder = { Text("Search ${state.skus.size} product names & raw materials...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onManageSkus,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manage_skus_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Products (${state.skus.size})", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = onAddSku,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(0.95f)
                                .testTag("add_sku_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Product", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Button(
                            onClick = {
                                preloadedBomForStudio = null
                                monitorView = 1
                                rawSubTab = 2
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForestEmerald,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1.15f)
                                .testTag("jump_add_raw_mat_bom_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Raw Mat (BOM)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

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
                            modifier = Modifier.testTag("monitor_hide_non_priority_chip")
                        )
                        FilterChip(
                            selected = showPriorityPicker,
                            onClick = { showPriorityPicker = true },
                            leadingIcon = {
                                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("Select Priority ($priorityTotalCount/${state.summaries.size})") },
                            modifier = Modifier.testTag("monitor_select_priority_button")
                        )
                        FilterChip(
                            selected = multiSelectSkus,
                            onClick = {
                                multiSelectSkus = !multiSelectSkus
                                if (!multiSelectSkus) selectedSkuKeys = emptySet()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (multiSelectSkus) Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(if (multiSelectSkus) "Checkboxes On" else "Select") },
                            modifier = Modifier.testTag("monitor_toggle_multiselect")
                        )
                    }

                    AnimatedVisibility(visible = multiSelectSkus) {
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
                                                selectedSkuKeys = selectedSkuKeys +
                                                    filteredSummaries.map { TabPreferencesStore.skuPriorityKey(it.sku) }
                                            }
                                        ) {
                                            Text("All Visible")
                                        }
                                        if (selectedSkuKeys.isNotEmpty()) {
                                            TextButton(onClick = { selectedSkuKeys = emptySet() }) {
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
                                                selectedSkuKeys = emptySet()
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ForestEmerald,
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Mark Priority")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onSavePriorityProductKeys(priorityProductKeys - selectedKeySet)
                                                selectedSkuKeys = emptySet()
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.minimumInteractiveComponentSize()
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
                                                selectedSkuKeys = emptySet()
                                                multiSelectSkus = false
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.minimumInteractiveComponentSize()
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
                                label = { Text(if (sec == "ALL") "All ($count)" else "$sec ($count)") }
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenFinishedGoodsTab,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("monitor_jump_finished_goods_button")
                        ) {
                            Text("Finished Goods Tab", maxLines = 1)
                        }
                        if (searchQuery.isNotBlank() || sectionFilter != "ALL" || lookbackDays != 14 || safetyBufferPct != null) {
                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    sectionFilter = "ALL"
                                    onResetFilters()
                                },
                                modifier = Modifier.testTag("reset_monitor_filters_button")
                            ) {
                                Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset")
                            }
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
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val hiddenAllByPriority = hideNonPriority && priorityTotalCount == 0 && state.summaries.isNotEmpty()
                            Text(
                                text = when {
                                    state.skus.isEmpty() -> "No SKUs loaded yet. Tap + SKU to add one or use Reset to load Beginning Inventory."
                                    hiddenAllByPriority -> "All non-priority products are currently hidden."
                                    hideNonPriority -> "No priority SKUs match \"$searchQuery\" in this location."
                                    else -> "No SKUs match \"$searchQuery\" in this location."
                                },
                                style = MaterialTheme.typography.bodyMedium
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
                    keyPrefix = "sku",
                    itemKey = { it.sku.id.ifBlank { "${it.sku.section}_${it.sku.productName}" } }
                ) { item ->
                    val pKey = TabPreferencesStore.skuPriorityKey(item.sku)
                    val isPriority = pKey in priorityProductKeys
                    val isSelected = pKey in selectedSkuKeys
                    val toggleSelect = {
                        selectedSkuKeys = if (isSelected) selectedSkuKeys - pKey else selectedSkuKeys + pKey
                    }
                    SkuInventoryCard(
                        item = item,
                        isPriority = isPriority,
                        multiSelectMode = multiSelectSkus,
                        isSelected = isSelected,
                        onToggleSelect = toggleSelect,
                        onTogglePriority = { onToggleProductPriority(item.sku) },
                        onEdit = { onEditSku(item.sku) },
                        onDispatch = { onOpenDispatchDialog(item.sku) },
                        onSchedule = {
                            val bags = item.suggestedBags.coerceAtLeast(20L)
                            val priority = item.recommendedPriority ?: SchedulePriority.BUFFER_TOP_UP
                            onOpenScheduleDialog(item.sku, bags, priority)
                        }
                    )
                }
            }
        } else if (monitorView == 1) {
            // Raw Materials Consumption Forecast, Bills of Materials & Multi-Tab Add Studio
            item {
                RawMaterialNotificationBanner(
                    projections = state.rawMaterialProjections,
                    hasNotificationPermission = hasNotificationPermission,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onSendNotificationNow = onSendNotificationNow
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val macroCount = state.rawMaterials.count { !PlantMaterialCatalog.inferSector(it).isMicro }
                    val microCount = state.rawMaterials.count { PlantMaterialCatalog.inferSector(it).isMicro }
                    OutlinedButton(
                        onClick = onOpenMacroTab,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("monitor_jump_macro_tab_button")
                    ) {
                        Text("Macro Tab ($macroCount)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = onOpenMicroTab,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("monitor_jump_micro_tab_button")
                    ) {
                        Text("Micro Tab ($microCount)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            item {
                val rawSubTabs = listOf(
                    "Stock & Alerts" to "raw_subtab_stock",
                    "BOM Recipes (${state.boms.size})" to "raw_subtab_boms",
                    "+ Add (BOM)" to "raw_subtab_add"
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    TabRow(
                        selectedTabIndex = rawSubTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            if (rawSubTab in tabPositions.indices) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[rawSubTab]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        rawSubTabs.forEachIndexed { idx, (label, tag) ->
                            Tab(
                                selected = rawSubTab == idx,
                                onClick = { rawSubTab = idx },
                                text = {
                                    Text(
                                        text = label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (rawSubTab == idx) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag(tag)
                            )
                        }
                    }
                }
            }

            if (rawSubTab == 0) {
                if (isDesktop && state.rawMaterialProjections.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                RawMaterialForecastChartCard(
                                    projections = state.rawMaterialProjections,
                                    onSelectRestock = { proj -> adjustingRawProjection = proj to true }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                RawMaterialSimulatorCard(
                                    lookbackDays = lookbackDays,
                                    demandMultiplier = demandMultiplier,
                                    includeScheduled = includeScheduled,
                                    leadOverrideDays = leadOverrideDays,
                                    onChangeLookback = onChangeLookback,
                                    onChangeDemandMultiplier = onChangeDemandMultiplier,
                                    onToggleIncludeScheduled = onToggleIncludeScheduled,
                                    onChangeLeadOverrideDays = onChangeLeadOverrideDays,
                                    onResetSimulator = onResetSimulator
                                )
                            }
                        }
                    }
                } else {
                    item {
                        RawMaterialSimulatorCard(
                            lookbackDays = lookbackDays,
                            demandMultiplier = demandMultiplier,
                            includeScheduled = includeScheduled,
                            leadOverrideDays = leadOverrideDays,
                            onChangeLookback = onChangeLookback,
                            onChangeDemandMultiplier = onChangeDemandMultiplier,
                            onToggleIncludeScheduled = onToggleIncludeScheduled,
                            onChangeLeadOverrideDays = onChangeLeadOverrideDays,
                            onResetSimulator = onResetSimulator
                        )
                    }

                    if (state.rawMaterialProjections.isNotEmpty()) {
                        item {
                            RawMaterialForecastChartCard(
                                projections = state.rawMaterialProjections,
                                onSelectRestock = { proj -> adjustingRawProjection = proj to true }
                            )
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = rawSearchQuery,
                                onValueChange = { rawSearchQuery = it },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search raw materials") },
                                trailingIcon = if (rawSearchQuery.isNotBlank()) {
                                    {
                                        IconButton(
                                            onClick = { rawSearchQuery = "" },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("clear_raw_search_button")
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "Clear raw material search")
                                        }
                                    }
                                } else null,
                                placeholder = { Text("Search ingredients or suppliers...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    preloadedBomForStudio = null
                                    rawSubTab = 2
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ForestEmerald,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("add_raw_material_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add (BOM)", maxLines = 1)
                            }
                        }

                        if (missingCatalogItems.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${missingCatalogItems.size} Macro / Micro catalog item(s) not yet in inventory",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Import all missing plant items or pick selectively in + Add (BOM).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { onAddRawMaterialsBulk(missingCatalogItems) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ForestEmerald,
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.testTag("sync_missing_catalog_button")
                                    ) {
                                        Text("Import All (${missingCatalogItems.size})", maxLines = 1)
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
                            FilterChip(
                                selected = rawCategoryFilter == "ALL",
                                onClick = { rawCategoryFilter = "ALL" },
                                label = { Text("All (${state.rawMaterialProjections.size})") }
                            )
                            FilterChip(
                                selected = rawCategoryFilter == "URGENT",
                                onClick = { rawCategoryFilter = "URGENT" },
                                label = { Text("Needs Action ($urgentRawCount)") }
                            )
                            MaterialSector.entries.forEach { sec ->
                                val count = state.rawMaterialProjections.count {
                                    PlantMaterialCatalog.inferSector(it.material) == sec
                                }
                                if (count > 0) {
                                    val filterKey = "SECTOR:${sec.name}"
                                    FilterChip(
                                        selected = rawCategoryFilter == filterKey,
                                        onClick = { rawCategoryFilter = filterKey },
                                        label = { Text("${sec.shortLabel} ($count)") },
                                        modifier = Modifier.testTag("raw_sector_filter_${sec.name.lowercase()}")
                                    )
                                }
                            }
                            RawMaterialCategory.entries.forEach { cat ->
                                val count = state.rawMaterialProjections.count { it.material.category == cat.label }
                                if (count > 0) {
                                    FilterChip(
                                        selected = rawCategoryFilter == cat.label,
                                        onClick = { rawCategoryFilter = cat.label },
                                        label = { Text("${cat.label} ($count)") }
                                    )
                                }
                            }
                        }
                    }
                }

                if (filteredRawProjections.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (state.rawMaterials.isEmpty()) {
                                    "No raw materials added yet. Switch to + Add (BOM) -> Micro & Macro to add ingredients or tap Import All."
                                } else {
                                    "No raw materials match your current filter."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    desktopGridItems(
                        itemsList = filteredRawProjections,
                        columns = gridColumns,
                        keyPrefix = "raw_mat",
                        itemKey = { it.material.id.ifBlank { it.material.name } }
                    ) { proj ->
                        RawMaterialProjectionCard(
                            proj = proj,
                            demandMultiplier = demandMultiplier,
                            onRestock = { adjustingRawProjection = proj to true },
                            onLogUsage = { adjustingRawProjection = proj to false },
                            onToggleAlert = { onToggleRawMaterialAlert(proj.material) },
                            onEdit = { editingRawMaterial = true to proj.material },
                            onDelete = { onDeleteRawMaterial(proj.material) }
                        )
                    }
                }
            } else if (rawSubTab == 1) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = bomSearchQuery,
                                onValueChange = { bomSearchQuery = it },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search BOM formulas") },
                                trailingIcon = if (bomSearchQuery.isNotBlank()) {
                                    {
                                        IconButton(
                                            onClick = { bomSearchQuery = "" },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("clear_bom_search_button")
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "Clear BOM search")
                                        }
                                    }
                                } else null,
                                placeholder = { Text("Search feed formula or ingredient...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    preloadedBomForStudio = null
                                    rawSubTab = 2
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ForestEmerald,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("create_bom_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New BOM", maxLines = 1)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onOpenFeedBomScreen,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("open_feed_bom_screen_button")
                            ) {
                                Text("Open Feed Type BOM Cards", maxLines = 1)
                            }
                            FilterChip(
                                selected = bomSectionFilter == "ALL",
                                onClick = { bomSectionFilter = "ALL" },
                                label = { Text("All (${state.boms.size})") }
                            )
                            PlantSection.entries.forEach { sec ->
                                val count = state.boms.count { it.section == sec.label }
                                if (count > 0) {
                                    FilterChip(
                                        selected = bomSectionFilter == sec.label,
                                        onClick = { bomSectionFilter = sec.label },
                                        label = { Text("${sec.label} ($count)") }
                                    )
                                }
                            }
                        }
                    }
                }

                if (filteredBoms.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (state.boms.isEmpty()) {
                                    "No Bills of Materials defined yet. Tap New BOM to build a feed formula and link raw material percentages."
                                } else {
                                    "No BOM formulas match your current filter."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    desktopGridItems(
                        itemsList = filteredBoms,
                        columns = gridColumns,
                        keyPrefix = "bom_recipe",
                        itemKey = { it.id.ifBlank { it.productName } }
                    ) { bom ->
                        BomRecipeCard(
                            bom = bom,
                            materials = state.rawMaterials,
                            onEditBom = { selected ->
                                preloadedBomForStudio = selected
                                rawSubTab = 2
                            },
                            onDeleteBom = onDeleteBom,
                            onQuickRestockMaterial = { mat ->
                                val matchedProj = state.rawMaterialProjections.firstOrNull {
                                    it.material.id == mat.id || it.material.name.equals(mat.name, ignoreCase = true)
                                }
                                if (matchedProj != null) {
                                    adjustingRawProjection = matchedProj to true
                                }
                            }
                        )
                    }
                }
            } else {
                item {
                    AddRawMaterialsTabbedWorkspace(
                        skus = state.skus,
                        existingMaterials = state.rawMaterials,
                        existingBoms = state.boms,
                        preloadedBom = preloadedBomForStudio,
                        onClearPreloadedBom = { preloadedBomForStudio = null },
                        onSaveSingleRawMaterial = onSaveRawMaterial,
                        onAddRawMaterialsBulk = onAddRawMaterialsBulk,
                        onSaveBomWithNewMaterials = onSaveBomWithNewMaterials,
                        onDoneNavigateToStock = { rawSubTab = 0 },
                        onDoneNavigateToBoms = {
                            preloadedBomForStudio = null
                            rawSubTab = 1
                        }
                    )
                }
            }
        } else {
            // Analytics & Dispatch Log View
            if (isDesktop) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            StockVsDemandBarChart(summaries = state.summaries)
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Text("Forecast & Buffer Parameters", style = MaterialTheme.typography.titleSmall)
                                        }
                                        TextButton(
                                            onClick = onResetFilters,
                                            modifier = Modifier.testTag("reset_chart_params_button")
                                        ) {
                                            Text("Reset")
                                        }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Window:", style = MaterialTheme.typography.labelMedium)
                                        listOf(7, 14, 30).forEach { d ->
                                            FilterChip(
                                                selected = lookbackDays == d,
                                                onClick = { onChangeLookback(d) },
                                                label = { Text("${d}d") }
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Safety Buffer:", style = MaterialTheme.typography.labelMedium)
                                        listOf(null to "SKU Default", 10.0 to "10%", 15.0 to "15%", 25.0 to "25%").forEach { (valPct, lbl) ->
                                            FilterChip(
                                                selected = safetyBufferPct == valPct,
                                                onClick = { onChangeSafetyBuffer(valPct) },
                                                label = { Text(lbl) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
            item {
                Surface(
                    onClick = onOpenProductionMonitor,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("monitor_open_production_monitor_card")
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
                                    text = "Daily Feed Output vs. Targets",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Open Production Monitor for interactive target vs. output charts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(onClick = onOpenProductionMonitor) {
                            Text("Open")
                        }
                    }
                }
            }
            item {
                StockVsDemandBarChart(summaries = state.summaries)
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("Forecast & Buffer Parameters", style = MaterialTheme.typography.titleSmall)
                            }
                            TextButton(
                                onClick = onResetFilters,
                                modifier = Modifier.testTag("reset_chart_params_button")
                            ) {
                                Text("Reset")
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Window:", style = MaterialTheme.typography.labelMedium)
                            listOf(7, 14, 30).forEach { d ->
                                FilterChip(
                                    selected = lookbackDays == d,
                                    onClick = { onChangeLookback(d) },
                                    label = { Text("${d}d") }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Safety Buffer:", style = MaterialTheme.typography.labelMedium)
                            listOf(null to "SKU Default", 10.0 to "10%", 15.0 to "15%", 25.0 to "25%").forEach { (valPct, lbl) ->
                                FilterChip(
                                    selected = safetyBufferPct == valPct,
                                    onClick = { onChangeSafetyBuffer(valPct) },
                                    label = { Text(lbl) }
                                )
                            }
                        }
                    }
                }
            }
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Recent Stock-Out / DR Log (${state.dispatches.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (state.dispatches.isEmpty()) {
                            Text(
                                "No dispatches logged yet. Tap Dispatch to record a customer delivery or stock-out.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            state.dispatches.take(15).forEach { d ->
                                val statusLabel = d.resolvedStatusLabel()
                                val netKg = d.effectiveDispatchedWeightKg()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${d.date} • ${d.drReference} • $statusLabel",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = when (statusLabel) {
                                                "Held" -> StatusCriticalTextDark
                                                "Returned" -> StatusWatchTextDark
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        val logistics = listOfNotNull(
                                            d.driverName.takeIf { it.isNotBlank() }?.let { "Driver: $it" },
                                            d.truckNumber.takeIf { it.isNotBlank() }?.let { "Truck: $it" }
                                        ).joinToString(" • ")
                                        Text(
                                            text = if (logistics.isNotBlank()) "${d.productName} → ${d.customer} ($logistics)" else "${d.productName} → ${d.customer}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (d.returnConfirmed && d.returnedBags > 0L) {
                                            "-${OperationsCalculator.formatKg(netKg)} kg (${d.bagsDispatched - d.returnedBags}b net)"
                                        } else {
                                            "-${OperationsCalculator.formatKg(d.totalWeightKg)} kg (${d.bagsDispatched}b)"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = StatusCriticalTextDark
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

    adjustingRawProjection?.let { (proj, isRestock) ->
        RawMaterialAdjustStockDialog(
            projection = proj,
            initialIsRestock = isRestock,
            onDismiss = { adjustingRawProjection = null },
            onConfirm = { deltaKg, reason ->
                onRestockRawMaterial(proj.material, deltaKg, reason)
                adjustingRawProjection = null
            }
        )
    }

    editingRawMaterial?.let { (open, existing) ->
        if (open) {
            RawMaterialEditorDialog(
                existing = existing,
                onDismiss = { editingRawMaterial = null },
                onSave = { name, cat, stockKg, threshKg, sharePct, leadDays, safetyDays, supplier, alertsEnabled ->
                    onSaveRawMaterial(existing, name, cat, stockKg, threshKg, sharePct, leadDays, safetyDays, supplier, alertsEnabled)
                    editingRawMaterial = null
                },
                onDelete = existing?.let { mat ->
                    {
                        onDeleteRawMaterial(mat)
                        editingRawMaterial = null
                    }
                }
            )
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
private fun SkuInventoryCard(
    item: SkuInventorySummary,
    isPriority: Boolean,
    multiSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onTogglePriority: () -> Unit,
    onEdit: () -> Unit,
    onDispatch: () -> Unit,
    onSchedule: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (multiSelectMode) Modifier.clickable { onToggleSelect() } else Modifier
            )
            .border(
                width = if (isSelected || isPriority) 1.5.dp else 1.dp,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isPriority -> GrainAmberBright.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
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
                            .testTag("monitor_select_${item.sku.id}")
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.sku.productName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.sku.section} • ${item.sku.standardBagWeightKg} kg/bag • ${item.sku.defaultRemarks}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StockStatusBadge(status = item.status)
                    IconButton(
                        onClick = onTogglePriority,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("monitor_star_${item.sku.id}")
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
                    IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit ${item.sku.productName}", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricCell("BEGINNING", "${OperationsCalculator.formatKg(item.openingStockKg)} kg")
                    MetricCell("RECOVERED", "+${OperationsCalculator.formatKg(item.recoveredKg)} kg")
                    MetricCell("DISPATCHED", "-${OperationsCalculator.formatKg(item.dispatchedKg)} kg")
                    MetricCell(
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
                MetricCell("AVG DISPATCH", "${OperationsCalculator.formatKg(item.avgDailyDispatchKg)} kg/d")
                MetricCell("DAYS OF COVER", coverStr)
                MetricCell("OPEN ORDERS", "${OperationsCalculator.formatKg(item.openOrdersKg)} kg")
                MetricCell(
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
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dispatch")
                }
                Button(
                    onClick = onSchedule,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.netProductionNeededKg > 0) GrainAmber else ForestEmerald,
                        contentColor = Color.White
                    )
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
private fun MetricCell(label: String, value: String, highlight: Boolean = false) {
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
