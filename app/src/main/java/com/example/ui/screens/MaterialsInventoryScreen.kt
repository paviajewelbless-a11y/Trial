package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import com.example.model.MaterialSector
import com.example.model.PlantMaterialCatalog
import com.example.model.RawMaterialCategory
import com.example.model.RawMaterialEntity
import com.example.model.RawMaterialProjection
import com.example.model.RawMaterialRiskStatus
import com.example.ui.components.IndustrialKpiCard
import com.example.ui.components.LocalIsDesktopLayout
import com.example.ui.components.LocalWindowWidthDp
import com.example.ui.components.RawMaterialAdjustStockDialog
import com.example.ui.components.RawMaterialEditorDialog
import com.example.ui.components.RawMaterialForecastChartCard
import com.example.ui.components.RawMaterialNotificationBanner
import com.example.ui.components.RawMaterialProjectionCard
import com.example.ui.components.desktopGridColumns
import com.example.ui.components.desktopGridItems
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.GrainAmberBright
import com.example.ui.theme.SlateTeal
import com.example.ui.theme.StatusCriticalTextDark
import com.example.ui.theme.StatusHealthyTextDark
import com.example.ui.theme.StatusWatchTextDark
import com.example.viewmodel.OpsUiState
import kotlin.math.roundToLong

private enum class MaterialInventorySort(val label: String) {
    RISK("Risk First"),
    STOCK_ASC("Lowest Stock"),
    NAME("Name A-Z")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaterialsInventoryScreen(
    state: OpsUiState.Ready,
    isMicroInventory: Boolean,
    demandMultiplier: Double = 1.0,
    hasNotificationPermission: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    onSendNotificationNow: () -> Unit = {},
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
    onOpenBomScreen: () -> Unit = {},
    onSwitchToOtherInventoryTab: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("ALL") }
    var sortMode by remember { mutableStateOf(MaterialInventorySort.RISK) }
    var groupMicroBySector by remember { mutableStateOf(isMicroInventory) }
    var showForecastChart by remember { mutableStateOf(false) }

    var adjustingRawProjection by remember { mutableStateOf<Pair<RawMaterialProjection, Boolean>?>(null) }
    var editingRawMaterial by remember { mutableStateOf<Pair<Boolean, RawMaterialEntity?>?>(null) }

    val sectorProjections = remember(state.rawMaterialProjections, isMicroInventory) {
        state.rawMaterialProjections.filter { proj ->
            PlantMaterialCatalog.inferSector(proj.material).isMicro == isMicroInventory
        }
    }

    val missingSectorCatalog = remember(state.rawMaterials, isMicroInventory) {
        PlantMaterialCatalog.missingCatalogEntities(state.rawMaterials).filter { entity ->
            PlantMaterialCatalog.inferSector(entity).isMicro == isMicroInventory
        }
    }

    val criticalCount = remember(sectorProjections) {
        sectorProjections.count { it.status == RawMaterialRiskStatus.CRITICAL }
    }
    val lowWatchCount = remember(sectorProjections) {
        sectorProjections.count { it.status == RawMaterialRiskStatus.LOW_WATCH }
    }
    val urgentCount = criticalCount + lowWatchCount
    val healthyCount = (sectorProjections.size - urgentCount).coerceAtLeast(0)
    val totalStockKg = remember(sectorProjections) {
        sectorProjections.sumOf { it.material.currentStockKg }
    }
    val totalThresholdKg = remember(sectorProjections) {
        sectorProjections.sumOf { it.material.reorderThresholdKg }
    }
    val avgLeadDays = remember(sectorProjections) {
        if (sectorProjections.isEmpty()) 0L
        else (sectorProjections.sumOf { it.material.leadTimeDays }.toDouble() / sectorProjections.size).roundToLong()
    }

    val fastCount = remember(sectorProjections) {
        sectorProjections.count { PlantMaterialCatalog.inferSector(it.material) == MaterialSector.MICRO_FAST }
    }
    val slowCount = remember(sectorProjections) {
        sectorProjections.count { PlantMaterialCatalog.inferSector(it.material) == MaterialSector.MICRO_SLOW }
    }
    val nonMovCount = remember(sectorProjections) {
        sectorProjections.count { PlantMaterialCatalog.inferSector(it.material) == MaterialSector.MICRO_NON_MOVING }
    }

    val filteredProjections = remember(sectorProjections, searchQuery, activeFilter, sortMode) {
        val base = sectorProjections.filter { proj ->
            val mat = proj.material
            val sector = PlantMaterialCatalog.inferSector(mat)
            val movementTag = PlantMaterialCatalog.inferMovementTag(mat)
            val matchesFilter = when {
                activeFilter == "ALL" -> true
                activeFilter == "URGENT" -> proj.status != RawMaterialRiskStatus.HEALTHY
                activeFilter.startsWith("SECTOR:") -> sector.name == activeFilter.removePrefix("SECTOR:")
                else -> mat.category == activeFilter
            }
            val matchesSearch = searchQuery.isBlank() ||
                mat.name.contains(searchQuery, ignoreCase = true) ||
                mat.supplierName.contains(searchQuery, ignoreCase = true) ||
                mat.category.contains(searchQuery, ignoreCase = true) ||
                movementTag.label.contains(searchQuery, ignoreCase = true) ||
                sector.label.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }

        when (sortMode) {
            MaterialInventorySort.RISK -> base.sortedWith(
                compareBy<RawMaterialProjection> {
                    when (it.status) {
                        RawMaterialRiskStatus.CRITICAL -> 0
                        RawMaterialRiskStatus.LOW_WATCH -> 1
                        RawMaterialRiskStatus.HEALTHY -> 2
                    }
                }.thenBy { it.daysUntilRunOut }
                    .thenBy { it.material.name.lowercase() }
            )
            MaterialInventorySort.STOCK_ASC -> base.sortedBy { it.material.currentStockKg }
            MaterialInventorySort.NAME -> base.sortedBy { it.material.name.lowercase() }
        }
    }

    val isDesktop = LocalIsDesktopLayout.current
    val windowWidthDp = LocalWindowWidthDp.current
    val gridColumns = desktopGridColumns(windowWidthDp, maxCols = 3)
    val tabTagPrefix = if (isMicroInventory) "micro_inv" else "macro_inv"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("${tabTagPrefix}_screen"),
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
                        text = if (isMicroInventory) "Micro Materials Inventory" else "Macro Materials Inventory",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = if (isMicroInventory) {
                            "Fast Moving, Slow Moving & Non-Moving / Trial premixes & additives"
                        } else {
                            "Bulk grains, soya meals, pollards, brans & liquid molasses"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onSwitchToOtherInventoryTab,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("${tabTagPrefix}_switch_tab_button")
                    ) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isMicroInventory) "Macro" else "Micro", maxLines = 1)
                    }
                    Button(
                        onClick = { editingRawMaterial = true to null },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForestEmerald,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("${tabTagPrefix}_add_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isMicroInventory) "Add Micro" else "Add Macro", maxLines = 1)
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
                    title = if (isMicroInventory) "MICRO ITEMS" else "MACRO ITEMS",
                    primaryValue = "${sectorProjections.size}",
                    secondaryValue = "$healthyCount healthy stock",
                    icon = if (isMicroInventory) Icons.Filled.Science else Icons.Filled.Grain,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(170.dp)
                )
                IndustrialKpiCard(
                    title = "ON-HAND STOCK",
                    primaryValue = "${OperationsCalculator.formatKg(totalStockKg)} kg",
                    secondaryValue = "${OperationsCalculator.formatKg(totalThresholdKg)} kg min alert",
                    icon = Icons.Filled.Scale,
                    accentColor = SlateTeal,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(195.dp)
                )
                IndustrialKpiCard(
                    title = "NEEDS ACTION",
                    primaryValue = "$urgentCount items",
                    secondaryValue = "$criticalCount Order Now • $lowWatchCount Low",
                    icon = Icons.Filled.Warning,
                    accentColor = if (urgentCount > 0) StatusCriticalTextDark else StatusHealthyTextDark,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(185.dp)
                )
                IndustrialKpiCard(
                    title = if (isMicroInventory) "3 SECTORS" else "AVG LEAD TIME",
                    primaryValue = if (isMicroInventory) "$fastCount / $slowCount / $nonMovCount" else "${avgLeadDays}d lead",
                    secondaryValue = if (isMicroInventory) "Fast / Slow / Non-Mov" else "${state.boms.size} BOM formulas",
                    icon = Icons.Filled.Inventory2,
                    accentColor = GrainAmberBright,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(190.dp)
                )
            }
        }

        // Scoped Notification Banner (only when urgent or notifications off)
        if (sectorProjections.isNotEmpty() && (urgentCount > 0 || !hasNotificationPermission)) {
            item {
                RawMaterialNotificationBanner(
                    projections = sectorProjections,
                    hasNotificationPermission = hasNotificationPermission,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onSendNotificationNow = onSendNotificationNow
                )
            }
        }

        // Missing catalog import banner scoped to Macro or Micro
        if (missingSectorCatalog.isNotEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
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
                                text = "${missingSectorCatalog.size} ${if (isMicroInventory) "Micro" else "Macro"} catalog item(s) not in inventory",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Import all missing ${if (isMicroInventory) "Micro" else "Macro"} items into stock in one tap.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onAddRawMaterialsBulk(missingSectorCatalog) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForestEmerald,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("${tabTagPrefix}_import_catalog_button")
                        ) {
                            Text("Import All (${missingSectorCatalog.size})", maxLines = 1)
                        }
                    }
                }
            }
        }

        // Search Bar + Action Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search materials") },
                    trailingIcon = if (searchQuery.isNotBlank()) {
                        {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("${tabTagPrefix}_clear_search_button")
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    } else null,
                    placeholder = {
                        Text(
                            if (isMicroInventory) {
                                "Search micro name, Fast/Slow/Non-Moving, Trial..."
                            } else {
                                "Search macro grain, soya, pollard, supplier..."
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${tabTagPrefix}_search_input")
                )

                // Sector / Status / Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = activeFilter == "ALL",
                        onClick = { activeFilter = "ALL" },
                        label = { Text("All (${sectorProjections.size})") },
                        modifier = Modifier.testTag("${tabTagPrefix}_filter_all")
                    )
                    FilterChip(
                        selected = activeFilter == "URGENT",
                        onClick = { activeFilter = "URGENT" },
                        label = { Text("Needs Action ($urgentCount)") },
                        modifier = Modifier.testTag("${tabTagPrefix}_filter_urgent")
                    )

                    if (isMicroInventory) {
                        listOf(
                            MaterialSector.MICRO_FAST to fastCount,
                            MaterialSector.MICRO_SLOW to slowCount,
                            MaterialSector.MICRO_NON_MOVING to nonMovCount
                        ).forEach { (sec, cnt) ->
                            val key = "SECTOR:${sec.name}"
                            FilterChip(
                                selected = activeFilter == key,
                                onClick = { activeFilter = key },
                                label = { Text("${sec.shortLabel} ($cnt)") },
                                modifier = Modifier.testTag("${tabTagPrefix}_filter_${sec.name.lowercase()}")
                            )
                        }
                    }

                    RawMaterialCategory.entries.forEach { cat ->
                        val cnt = sectorProjections.count { it.material.category == cat.label }
                        if (cnt > 0) {
                            FilterChip(
                                selected = activeFilter == cat.label,
                                onClick = { activeFilter = cat.label },
                                label = { Text("${cat.label} ($cnt)") }
                            )
                        }
                    }
                }

                // Controls Row: Sort, Group by Sector (Micro), Forecast Toggle, BOM Link
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MaterialInventorySort.entries.forEach { mode ->
                        FilterChip(
                            selected = sortMode == mode,
                            onClick = { sortMode = mode },
                            label = { Text(mode.label) }
                        )
                    }
                    if (isMicroInventory) {
                        FilterChip(
                            selected = groupMicroBySector,
                            onClick = { groupMicroBySector = !groupMicroBySector },
                            leadingIcon = {
                                Icon(Icons.Filled.ViewAgenda, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("Group by Sector") },
                            modifier = Modifier.testTag("micro_inv_group_sector_toggle")
                        )
                    }
                    if (sectorProjections.isNotEmpty()) {
                        FilterChip(
                            selected = showForecastChart,
                            onClick = { showForecastChart = !showForecastChart },
                            leadingIcon = {
                                Icon(Icons.Filled.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(if (showForecastChart) "Hide Forecast" else "Forecast Chart") },
                            modifier = Modifier.testTag("${tabTagPrefix}_forecast_toggle")
                        )
                    }
                    FilterChip(
                        selected = false,
                        onClick = onOpenBomScreen,
                        leadingIcon = {
                            Icon(Icons.Filled.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Feed BOM (${state.boms.size})") },
                        modifier = Modifier.testTag("${tabTagPrefix}_open_bom_button")
                    )
                }
            }
        }

        if (showForecastChart && sectorProjections.isNotEmpty()) {
            item {
                AnimatedVisibility(visible = showForecastChart) {
                    RawMaterialForecastChartCard(
                        projections = sectorProjections,
                        onSelectRestock = { proj -> adjustingRawProjection = proj to true }
                    )
                }
            }
        }

        if (filteredProjections.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (sectorProjections.isEmpty()) {
                                "No ${if (isMicroInventory) "Micro" else "Macro"} materials in inventory yet."
                            } else {
                                "No ${if (isMicroInventory) "Micro" else "Macro"} materials match \"$searchQuery\"."
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (sectorProjections.isEmpty()) {
                                "Tap + Add ${if (isMicroInventory) "Micro" else "Macro"} above or Import All to load the plant catalog."
                            } else {
                                "Clear the search query or switch filter chips to view all items."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (isMicroInventory && groupMicroBySector && activeFilter == "ALL" && searchQuery.isBlank()) {
            val sectorsOrder = listOf(
                MaterialSector.MICRO_FAST,
                MaterialSector.MICRO_SLOW,
                MaterialSector.MICRO_NON_MOVING
            )
            sectorsOrder.forEach { sector ->
                val sectorItems = filteredProjections.filter {
                    PlantMaterialCatalog.inferSector(it.material) == sector
                }
                if (sectorItems.isNotEmpty()) {
                    val sectorWeightKg = sectorItems.sumOf { it.material.currentStockKg }
                    val sectorUrgent = sectorItems.count { it.status != RawMaterialRiskStatus.HEALTHY }
                    item(key = "header_${sector.name}") {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("micro_sector_header_${sector.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sector.label,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${sectorItems.size} items • ${OperationsCalculator.formatKg(sectorWeightKg)} kg total",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (sectorUrgent > 0) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "$sectorUrgent urgent",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    desktopGridItems(
                        itemsList = sectorItems,
                        columns = gridColumns,
                        keyPrefix = "micro_${sector.name}",
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
            }
        } else {
            desktopGridItems(
                itemsList = filteredProjections,
                columns = gridColumns,
                keyPrefix = tabTagPrefix,
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
    }

    adjustingRawProjection?.let { (projection, defaultRestock) ->
        RawMaterialAdjustStockDialog(
            projection = projection,
            initialIsRestock = defaultRestock,
            onDismiss = { adjustingRawProjection = null },
            onConfirm = { deltaKg, reason ->
                onRestockRawMaterial(projection.material, deltaKg, reason)
                adjustingRawProjection = null
            }
        )
    }

    editingRawMaterial?.let { (isOpen, existingMat) ->
        if (isOpen) {
            RawMaterialEditorDialog(
                existing = existingMat,
                defaultSector = if (existingMat == null) {
                    if (isMicroInventory) MaterialSector.MICRO_FAST else MaterialSector.MACRO
                } else {
                    PlantMaterialCatalog.inferSector(existingMat)
                },
                onDismiss = { editingRawMaterial = null },
                onSave = { name, cat, stock, thresh, share, lead, safety, supplier, alerts ->
                    onSaveRawMaterial(existingMat, name, cat, stock, thresh, share, lead, safety, supplier, alerts)
                    editingRawMaterial = null
                },
                onDelete = existingMat?.let { mat ->
                    {
                        onDeleteRawMaterial(mat)
                        editingRawMaterial = null
                    }
                }
            )
        }
    }
}
