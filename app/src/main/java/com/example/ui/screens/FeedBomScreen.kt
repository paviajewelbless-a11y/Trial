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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.model.BomIngredientLine
import com.example.model.FeedBomEntity
import com.example.model.MaterialSector
import com.example.model.PlantMaterialCatalog
import com.example.model.PlantSection
import com.example.model.RawMaterialCategory
import com.example.model.RawMaterialEntity
import com.example.model.RawMaterialProjection
import com.example.model.RawMaterialRiskStatus
import com.example.ui.components.BomFormulaBuilderCard
import com.example.ui.components.FeedTypeBomBreakdownCard
import com.example.ui.components.FeedTypeCategory
import com.example.ui.components.IndustrialKpiCard
import com.example.ui.components.LocalIsDesktopLayout
import com.example.ui.components.LocalWindowWidthDp
import com.example.ui.components.RawMaterialAdjustStockDialog
import com.example.ui.components.RawMaterialEditorDialog
import com.example.ui.components.classifyFeedType
import com.example.ui.components.desktopGridColumns
import com.example.ui.components.desktopGridItems
import com.example.ui.components.movementTagBadgeColors
import com.example.ui.components.resolveBomLineDetail
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.GrainAmberBright
import com.example.ui.theme.SlateTeal
import com.example.ui.theme.StatusCriticalTextDark
import com.example.ui.theme.StatusHealthyTextDark
import com.example.viewmodel.OpsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedBomScreen(
    state: OpsUiState.Ready,
    onSaveBomWithNewMaterials: (
        newMaterials: List<RawMaterialEntity>,
        existingBom: FeedBomEntity?,
        productName: String,
        section: PlantSection,
        batchBaseKg: Double,
        yieldLossPct: Double,
        draftLines: List<BomIngredientLine>,
        notes: String
    ) -> Unit,
    onDeleteBom: (FeedBomEntity) -> Unit,
    onAdjustRawMaterialStock: (RawMaterialEntity, Double, String) -> Unit,
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
    onDeleteRawMaterial: (RawMaterialEntity) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFeedType by remember { mutableStateOf<FeedTypeCategory?>(null) }
    var selectedSectorFilter by remember { mutableStateOf<MaterialSector?>(null) }
    var selectedSectionFilter by remember { mutableStateOf("ALL") }
    var batchWeightOverrideKg by remember { mutableStateOf<Double?>(null) }

    var showBomEditorSheet by remember { mutableStateOf(false) }
    var editorPreloadedBom by remember { mutableStateOf<FeedBomEntity?>(null) }
    var adjustingRawProjection by remember { mutableStateOf<RawMaterialProjection?>(null) }
    var editingRawMaterial by remember { mutableStateOf<Pair<Boolean, RawMaterialEntity?>?>(null) }
    var showManageMaterialsDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = showBomEditorSheet || showManageMaterialsDialog || editingRawMaterial != null) {
        when {
            editingRawMaterial != null -> editingRawMaterial = null
            showManageMaterialsDialog -> showManageMaterialsDialog = false
            showBomEditorSheet -> {
                showBomEditorSheet = false
                editorPreloadedBom = null
            }
        }
    }

    val materialById = remember(state.rawMaterials) { state.rawMaterials.associateBy { it.id } }
    val materialByName = remember(state.rawMaterials) {
        state.rawMaterials.associateBy { it.name.trim().lowercase() }
    }

    val bomStats = remember(state.boms, state.rawMaterials) {
        var totalMacroPct = 0.0
        var totalMicroPct = 0.0
        var readyCount = 0
        val distinctIngredients = mutableSetOf<String>()
        val feedTypesUsed = mutableSetOf<FeedTypeCategory>()

        state.boms.forEach { bom ->
            feedTypesUsed += classifyFeedType(bom)
            val details = bom.lines.map {
                resolveBomLineDetail(it, bom.batchBaseKg, materialById, materialByName)
            }
            totalMacroPct += details.filter { it.isMacro }.sumOf { it.line.inclusionPct }
            totalMicroPct += details.filter { !it.isMacro }.sumOf { it.line.inclusionPct }
            val minBatches = details.mapNotNull { it.maxBatches }.minOrNull() ?: 0
            if (minBatches >= 1) readyCount += 1
            details.forEach { distinctIngredients += it.line.materialName.trim().lowercase() }
        }

        val count = state.boms.size.coerceAtLeast(1)
        QuadrupleBomMetrics(
            feedTypeCount = feedTypesUsed.size,
            avgMacroPct = if (state.boms.isEmpty()) 0.0 else totalMacroPct / count,
            avgMicroPct = if (state.boms.isEmpty()) 0.0 else totalMicroPct / count,
            readyToMillCount = readyCount,
            distinctIngredientCount = distinctIngredients.size
        )
    }

    val countsByFeedType = remember(state.boms) {
        FeedTypeCategory.entries.associateWith { type ->
            state.boms.count { classifyFeedType(it) == type }
        }
    }

    val filteredBoms = remember(
        state.boms,
        state.rawMaterials,
        searchQuery,
        selectedFeedType,
        selectedSectorFilter,
        selectedSectionFilter
    ) {
        val q = searchQuery.trim()
        state.boms.filter { bom ->
            val feedType = classifyFeedType(bom)
            val matchesFeedType = selectedFeedType == null || feedType == selectedFeedType
            val matchesSection = selectedSectionFilter == "ALL" || bom.section == selectedSectionFilter

            val lineDetails = bom.lines.map {
                resolveBomLineDetail(it, bom.batchBaseKg, materialById, materialByName)
            }
            val matchesSector = selectedSectorFilter == null || lineDetails.any { it.sector == selectedSectorFilter }

            val matchesSearch = q.isBlank() ||
                bom.productName.contains(q, ignoreCase = true) ||
                bom.section.contains(q, ignoreCase = true) ||
                feedType.label.contains(q, ignoreCase = true) ||
                feedType.shortLabel.contains(q, ignoreCase = true) ||
                bom.notes.contains(q, ignoreCase = true) ||
                lineDetails.any {
                    it.line.materialName.contains(q, ignoreCase = true) ||
                        it.movementTag.label.contains(q, ignoreCase = true) ||
                        it.bomCode.contains(q, ignoreCase = true) ||
                        it.sector.label.contains(q, ignoreCase = true)
                }

            matchesFeedType && matchesSection && matchesSector && matchesSearch
        }
    }

    val isDesktop = LocalIsDesktopLayout.current
    val windowWidthDp = LocalWindowWidthDp.current
    val gridColumns = desktopGridColumns(windowWidthDp, maxCols = 2)
    val hasActiveFilters = searchQuery.isNotBlank() ||
        selectedFeedType != null ||
        selectedSectorFilter != null ||
        selectedSectionFilter != "ALL" ||
        batchWeightOverrideKg != null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("feed_bom_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header with Materials Manager + Create BOM buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Feed Bill of Materials",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Card-based Macro & Micro breakdown by feed type",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showManageMaterialsDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("feed_bom_manage_materials_button")
                    ) {
                        Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Materials (${state.rawMaterials.size})", maxLines = 1)
                    }
                    Button(
                        onClick = {
                            editorPreloadedBom = null
                            showBomEditorSheet = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForestEmerald,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("feed_bom_new_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New BOM", maxLines = 1)
                    }
                }
            }
        }

        // 2. Top-Pinned BOM Search Bar (Fast filtering across feed types & specific ingredients)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search feed types and ingredients"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("feed_bom_clear_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    placeholder = {
                        Text("Search feed types (Swine, Poultry, Duck, Quail) or ingredients (Corn, Soya, Limestone)...")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feed_bom_search_input")
                )
                if (searchQuery.isNotBlank()) {
                    Text(
                        text = "Matching ${filteredBoms.size} of ${state.boms.size} BOM formula(s) for \"${searchQuery.trim()}\"",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 3. KPI Strip
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
                    title = "FEED FORMULAS",
                    primaryValue = "${state.boms.size} BOMs",
                    secondaryValue = "${bomStats.feedTypeCount} feed types categorized",
                    icon = Icons.Filled.Category,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(185.dp)
                )
                IndustrialKpiCard(
                    title = "MACRO VS MICRO",
                    primaryValue = "${String.format("%.1f%%", bomStats.avgMacroPct)} / ${String.format("%.1f%%", bomStats.avgMicroPct)}",
                    secondaryValue = "Avg macro bulk vs micro premix",
                    icon = Icons.Filled.Grain,
                    accentColor = ForestEmerald,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(205.dp)
                )
                IndustrialKpiCard(
                    title = "READY TO MILL",
                    primaryValue = "${bomStats.readyToMillCount} / ${state.boms.size}",
                    secondaryValue = "Formulas with >= 1 batch stocked",
                    icon = Icons.Filled.CheckCircle,
                    accentColor = StatusHealthyTextDark,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(195.dp)
                )
                IndustrialKpiCard(
                    title = "CATALOG USED",
                    primaryValue = "${bomStats.distinctIngredientCount} / ${PlantMaterialCatalog.allSpecs.size}",
                    secondaryValue = "Active raw materials in BOMs",
                    icon = Icons.Filled.Inventory2,
                    accentColor = SlateTeal,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(190.dp)
                )
            }
        }

        // 4. Feed Type, Sector & Batch Scaler Filter Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
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
                        Text(
                            text = "Feed Type & Batch Filters",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (hasActiveFilters) {
                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedFeedType = null
                                    selectedSectorFilter = null
                                    selectedSectionFilter = "ALL"
                                    batchWeightOverrideKg = null
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("feed_bom_reset_filters")
                            ) {
                                Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Filters")
                            }
                        }
                    }

                    // Feed Type Filter Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedFeedType == null,
                            onClick = { selectedFeedType = null },
                            label = { Text("All Types (${state.boms.size})") },
                            modifier = Modifier.testTag("feed_type_chip_all")
                        )
                        FeedTypeCategory.entries.forEach { type ->
                            val count = countsByFeedType[type] ?: 0
                            FilterChip(
                                selected = selectedFeedType == type,
                                onClick = {
                                    selectedFeedType = if (selectedFeedType == type) null else type
                                },
                                label = { Text("${type.shortLabel} ($count)") },
                                modifier = Modifier.testTag("feed_type_chip_${type.name.lowercase()}")
                            )
                        }
                    }

                    // Ingredient Sector Filter Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Sector:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilterChip(
                            selected = selectedSectorFilter == null,
                            onClick = { selectedSectorFilter = null },
                            label = { Text("All") }
                        )
                        MaterialSector.entries.forEach { sector ->
                            FilterChip(
                                selected = selectedSectorFilter == sector,
                                onClick = {
                                    selectedSectorFilter = if (selectedSectorFilter == sector) null else sector
                                },
                                label = { Text(sector.shortLabel) },
                                modifier = Modifier.testTag("feed_bom_sector_${sector.name.lowercase()}")
                            )
                        }
                    }

                    // Batch Scaling Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Scale,
                            contentDescription = null,
                            tint = GrainAmberBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Batch Scale:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val scaleOptions = listOf(
                            null to "Formula Base",
                            500.0 to "500 kg (10b)",
                            1000.0 to "1T (20b)",
                            2000.0 to "2T (40b)",
                            5000.0 to "5T (100b)"
                        )
                        scaleOptions.forEach { (kg, label) ->
                            FilterChip(
                                selected = batchWeightOverrideKg == kg,
                                onClick = { batchWeightOverrideKg = kg },
                                label = { Text(label) }
                            )
                        }
                    }

                    // Plant Section Filter Row
                    val sectionCounts = remember(state.boms) {
                        PlantSection.entries.associateWith { sec -> state.boms.count { it.section == sec.label } }
                    }
                    if (sectionCounts.values.count { it > 0 } > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Plant:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FilterChip(
                                selected = selectedSectionFilter == "ALL",
                                onClick = { selectedSectionFilter = "ALL" },
                                label = { Text("All Plants") }
                            )
                            PlantSection.entries.forEach { sec ->
                                val count = sectionCounts[sec] ?: 0
                                if (count > 0) {
                                    FilterChip(
                                        selected = selectedSectionFilter == sec.label,
                                        onClick = { selectedSectionFilter = sec.label },
                                        label = { Text("${sec.label} ($count)") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Feed Type BOM Cards (Categorized Macro & Micro Layout)
        if (filteredBoms.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = if (state.boms.isEmpty()) {
                                "No Feed Bills of Materials yet"
                            } else {
                                "No feed formulas match these filters"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (state.boms.isEmpty()) {
                                "Tap New BOM to build a Macro & Micro ingredient recipe for any swine, poultry, duck, or quail feed."
                            } else {
                                "Clear a filter or create a new BOM for this feed type."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (hasActiveFilters) {
                                OutlinedButton(
                                    onClick = {
                                        searchQuery = ""
                                        selectedFeedType = null
                                        selectedSectorFilter = null
                                        selectedSectionFilter = "ALL"
                                        batchWeightOverrideKg = null
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Text("Reset Filters")
                                }
                            }
                            Button(
                                onClick = {
                                    editorPreloadedBom = null
                                    showBomEditorSheet = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ForestEmerald,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Feed BOM")
                            }
                        }
                    }
                }
            }
        } else {
            desktopGridItems(
                itemsList = filteredBoms,
                columns = gridColumns,
                keyPrefix = "feed_bom",
                itemKey = { it.id.ifBlank { "${it.section}_${it.productName}" } }
            ) { bom ->
                FeedTypeBomBreakdownCard(
                    bom = bom,
                    materials = state.rawMaterials,
                    batchWeightOverrideKg = batchWeightOverrideKg,
                    searchQuery = searchQuery,
                    onEditBom = { selected ->
                        editorPreloadedBom = selected
                        showBomEditorSheet = true
                    },
                    onCloneBom = { source ->
                        editorPreloadedBom = source.copy(
                            id = "",
                            productName = "${source.productName} (Variant)",
                            notes = if (source.notes.isBlank()) "Cloned from ${source.productName}" else source.notes
                        )
                        showBomEditorSheet = true
                    },
                    onDeleteBom = onDeleteBom,
                    onQuickRestockMaterial = { mat ->
                        val matchedProj = state.rawMaterialProjections.firstOrNull {
                            it.material.id == mat.id || it.material.name.equals(mat.name, ignoreCase = true)
                        }
                        adjustingRawProjection = matchedProj
                            ?: RawMaterialProjection(
                                material = mat,
                                historicalAvgDailyKg = 0.0,
                                simulatedAvgDailyKg = 0.0,
                                recentTotalConsumedKg = 0.0,
                                scheduledDemandKg = 0.0,
                                daysUntilRunOut = Double.POSITIVE_INFINITY,
                                projectedRunOutIsoDate = null,
                                projectedRunOutLabel = "Manual",
                                reorderTriggerKg = mat.reorderThresholdKg,
                                recommendedRestockKg = 500.0,
                                status = RawMaterialRiskStatus.HEALTHY,
                                statusReason = "Direct BOM inventory check",
                                dailyTrend = emptyList()
                            )
                    },
                    onEditMaterial = { mat ->
                        editingRawMaterial = true to mat
                    },
                    onDeleteMaterial = onDeleteRawMaterial,
                    onCreateUnlinkedMaterial = { line ->
                        val spec = PlantMaterialCatalog.resolveSpecByName(line.materialName)
                        val seedMat = spec?.toEntity() ?: RawMaterialEntity(
                            name = line.materialName,
                            category = line.category,
                            currentStockKg = 0.0,
                            reorderThresholdKg = 200.0,
                            usageSharePct = line.inclusionPct.coerceIn(0.1, 100.0),
                            leadTimeDays = 4L,
                            safetyDays = 3L,
                            supplierName = line.notes.take(60)
                        )
                        editingRawMaterial = true to seedMat
                    }
                )
            }
        }
    }

    if (showBomEditorSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                showBomEditorSheet = false
                editorPreloadedBom = null
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            editorPreloadedBom?.id?.isNotBlank() == true -> "Edit Feed BOM"
                            editorPreloadedBom != null -> "Clone Feed BOM"
                            else -> "New Feed BOM"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = {
                            showBomEditorSheet = false
                            editorPreloadedBom = null
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close BOM editor"
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                BomFormulaBuilderCard(
                    skus = state.skus,
                    existingMaterials = state.rawMaterials,
                    existingBoms = state.boms,
                    preloadedBom = editorPreloadedBom,
                    onClearPreloadedBom = { editorPreloadedBom = null },
                    onSaveBomWithNewMaterials = { newMats, existingBom, prodName, sec, baseKg, lossPct, lines, notes ->
                        onSaveBomWithNewMaterials(newMats, existingBom, prodName, sec, baseKg, lossPct, lines, notes)
                        showBomEditorSheet = false
                        editorPreloadedBom = null
                    }
                )
            }
        }
    }

    if (showManageMaterialsDialog) {
        ManageRawMaterialsDialog(
            materials = state.rawMaterials,
            onDismiss = { showManageMaterialsDialog = false },
            onAddNew = { editingRawMaterial = true to null },
            onEditMaterial = { mat -> editingRawMaterial = true to mat },
            onDeleteMaterial = onDeleteRawMaterial
        )
    }

    adjustingRawProjection?.let { proj ->
        RawMaterialAdjustStockDialog(
            projection = proj,
            initialIsRestock = true,
            onDismiss = { adjustingRawProjection = null },
            onConfirm = { deltaKg, reason ->
                onAdjustRawMaterialStock(proj.material, deltaKg, reason)
                adjustingRawProjection = null
            }
        )
    }

    if (editingRawMaterial?.first == true) {
        val target = editingRawMaterial?.second
        val isExistingInDb = target != null && target.id.isNotBlank()
        RawMaterialEditorDialog(
            existing = target,
            onDismiss = { editingRawMaterial = null },
            onSave = { name, cat, stockKg, thresholdKg, sharePct, leadDays, safetyDays, supplier, alerts ->
                onSaveRawMaterial(
                    if (isExistingInDb) target else null,
                    name,
                    cat,
                    stockKg,
                    thresholdKg,
                    sharePct,
                    leadDays,
                    safetyDays,
                    supplier,
                    alerts
                )
                editingRawMaterial = null
            },
            onDelete = if (isExistingInDb) {
                {
                    onDeleteRawMaterial(target!!)
                    editingRawMaterial = null
                }
            } else {
                null
            }
        )
    }
}

@Composable
private fun ManageRawMaterialsDialog(
    materials: List<RawMaterialEntity>,
    onDismiss: () -> Unit,
    onAddNew: () -> Unit,
    onEditMaterial: (RawMaterialEntity) -> Unit,
    onDeleteMaterial: (RawMaterialEntity) -> Unit
) {
    var matSearch by remember { mutableStateOf("") }
    var sectorFilter by remember { mutableStateOf<MaterialSector?>(null) }
    var pendingDelete by remember { mutableStateOf<RawMaterialEntity?>(null) }

    val filtered = remember(materials, matSearch, sectorFilter) {
        val q = matSearch.trim()
        materials.filter { mat ->
            val sector = PlantMaterialCatalog.inferSector(mat)
            val tag = PlantMaterialCatalog.inferMovementTag(mat)
            (sectorFilter == null || sector == sectorFilter) &&
                (q.isBlank() ||
                    mat.name.contains(q, ignoreCase = true) ||
                    mat.category.contains(q, ignoreCase = true) ||
                    tag.label.contains(q, ignoreCase = true) ||
                    mat.supplierName.contains(q, ignoreCase = true))
        }.sortedBy { it.name.lowercase() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Manage Raw Materials (${materials.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Edit stock specs or delete ingredients",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onAddNew,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestEmerald,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", maxLines = 1)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = matSearch,
                    onValueChange = { matSearch = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (matSearch.isNotBlank()) {
                            IconButton(onClick = { matSearch = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    placeholder = { Text("Search ingredient or sector...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manage_raw_materials_search")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = sectorFilter == null,
                        onClick = { sectorFilter = null },
                        label = { Text("All (${materials.size})") }
                    )
                    MaterialSector.entries.forEach { sec ->
                        val count = materials.count { PlantMaterialCatalog.inferSector(it) == sec }
                        FilterChip(
                            selected = sectorFilter == sec,
                            onClick = { sectorFilter = if (sectorFilter == sec) null else sec },
                            label = { Text("${sec.shortLabel} ($count)") }
                        )
                    }
                }

                if (filtered.isEmpty()) {
                    Text(
                        text = "No raw materials match your search.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered, key = { it.id.ifBlank { it.name } }) { mat ->
                            val tag = PlantMaterialCatalog.inferMovementTag(mat)
                            val (tagBg, tagFg) = movementTagBadgeColors(tag)
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = tagBg,
                                                shape = RoundedCornerShape(5.dp)
                                            ) {
                                                Text(
                                                    text = tag.shortCode,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = tagFg,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                            Text(
                                                text = mat.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "${OperationsCalculator.formatKg(mat.currentStockKg)} kg on hand • Min ${OperationsCalculator.formatKg(mat.reorderThresholdKg)} kg • ${mat.category}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onEditMaterial(mat) },
                                            modifier = Modifier
                                                .size(44.dp)
                                                .minimumInteractiveComponentSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = "Edit ${mat.name}",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { pendingDelete = mat },
                                            modifier = Modifier
                                                .size(44.dp)
                                                .minimumInteractiveComponentSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Delete ${mat.name}",
                                                tint = StatusCriticalTextDark,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )

    pendingDelete?.let { mat ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Raw Material?") },
            text = { Text("Permanently remove ${mat.name} from inventory?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMaterial(mat)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

private data class QuadrupleBomMetrics(
    val feedTypeCount: Int,
    val avgMacroPct: Double,
    val avgMicroPct: Double,
    val readyToMillCount: Int,
    val distinctIngredientCount: Int
)
