package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.OperationsCalculator
import com.example.model.BomIngredientLine
import com.example.model.FeedBomEntity
import com.example.model.MaterialSector
import com.example.model.MicroMovementTag
import com.example.model.PlantMaterialCatalog
import com.example.model.PlantSection
import com.example.model.RawMaterialCategory
import com.example.model.RawMaterialEntity
import com.example.model.SkuEntity
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.OnStatusCriticalContainer
import com.example.ui.theme.OnStatusHealthyContainer
import com.example.ui.theme.OnStatusWatchContainer
import com.example.ui.theme.StatusCriticalContainer
import com.example.ui.theme.StatusCriticalTextDark
import com.example.ui.theme.StatusHealthyContainer
import com.example.ui.theme.StatusHealthyTextDark
import com.example.ui.theme.StatusWatchContainer
import com.example.ui.theme.StatusWatchTextDark
import kotlin.math.abs

fun rawMaterialBomCode(category: RawMaterialCategory): String = when (category) {
    RawMaterialCategory.ENERGY -> "RM-ENG"
    RawMaterialCategory.PROTEIN -> "RM-PRO"
    RawMaterialCategory.MINERAL -> "RM-MIN"
    RawMaterialCategory.ADDITIVE -> "RM-PRM"
    RawMaterialCategory.LIQUID -> "RM-LIQ"
}

@Composable
fun movementTagBadgeColors(tag: MicroMovementTag): Pair<Color, Color> = when (tag) {
    MicroMovementTag.MACRO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    MicroMovementTag.FAST_MOVING -> StatusHealthyContainer to OnStatusHealthyContainer
    MicroMovementTag.SLOW_MOVING -> StatusWatchContainer to OnStatusWatchContainer
    MicroMovementTag.TRIAL -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    MicroMovementTag.NEW_MICROMIX -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    MicroMovementTag.NON_MOVING -> StatusCriticalContainer to OnStatusCriticalContainer
}

private data class SectorBomRowUi(
    val key: Int,
    val name: String,
    val sector: MaterialSector,
    val movementTag: MicroMovementTag,
    val bomCode: String,
    val category: RawMaterialCategory,
    val selected: Boolean = false,
    val inclusionPctText: String,
    val stockKgText: String,
    val reorderKgText: String,
    val leadDaysText: String,
    val supplierName: String,
    val isCustom: Boolean = false
)

private fun defaultSectorBomCatalog(): List<SectorBomRowUi> {
    var k = 1
    return PlantMaterialCatalog.allSpecs.map { spec ->
        SectorBomRowUi(
            key = k++,
            name = spec.name,
            sector = spec.sector,
            movementTag = spec.movementTag,
            bomCode = spec.bomCode,
            category = spec.category,
            selected = false,
            inclusionPctText = String.format("%.1f", spec.defaultInclusionPct),
            stockKgText = spec.defaultStockKg.toInt().toString(),
            reorderKgText = spec.defaultReorderKg.toInt().toString(),
            leadDaysText = spec.defaultLeadDays.toString(),
            supplierName = spec.defaultSupplier
        )
    }
}

@Composable
fun CategoryBomIntakeTab(
    skus: List<SkuEntity>,
    existingMaterials: List<RawMaterialEntity>,
    onAddRawMaterialsBulk: (List<RawMaterialEntity>) -> Unit,
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
    onDoneNavigateToStock: () -> Unit,
    onDoneNavigateToBoms: () -> Unit
) {
    var activeSector by remember { mutableStateOf(MaterialSector.MACRO) }
    var nonMovingSubFilter by remember { mutableStateOf<MicroMovementTag?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var batchBaseKg by remember { mutableStateOf(1000.0) }
    var nextKey by remember { mutableIntStateOf(200) }
    val rows = remember { mutableStateListOf<SectorBomRowUi>().apply { addAll(defaultSectorBomCatalog()) } }

    var alsoCreateBom by remember { mutableStateOf(false) }
    var bomProductName by remember { mutableStateOf(skus.firstOrNull()?.productName.orEmpty()) }
    var bomSection by remember {
        mutableStateOf(
            skus.firstOrNull()?.let { PlantSection.fromRaw(it.section) } ?: PlantSection.SAN_ROQUE
        )
    }
    var bomNotes by remember { mutableStateOf("") }
    var showCustomDialog by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val existingByNameLower = remember(existingMaterials) {
        existingMaterials.associateBy { it.name.trim().lowercase() }
    }

    val sectorRows = rows.filter { row ->
        val matchesSector = row.sector == activeSector
        val matchesSubTag = activeSector != MaterialSector.MICRO_NON_MOVING ||
            nonMovingSubFilter == null ||
            row.movementTag == nonMovingSubFilter
        val matchesQuery = searchQuery.isBlank() ||
            row.name.contains(searchQuery, ignoreCase = true) ||
            row.movementTag.label.contains(searchQuery, ignoreCase = true) ||
            row.category.label.contains(searchQuery, ignoreCase = true)
        matchesSector && matchesSubTag && matchesQuery
    }

    val missingInActiveSector = rows.filter {
        it.sector == activeSector && it.name.trim().lowercase() !in existingByNameLower
    }
    val selectedRows = rows.filter { it.selected }
    val selectedNewRows = selectedRows.filter { it.name.trim().lowercase() !in existingByNameLower }
    val totalSelectedPct = selectedRows.sumOf { it.inclusionPctText.toDoubleOrNull() ?: 0.0 }
    val totalSelectedBatchKg = selectedRows.sumOf {
        ((it.inclusionPctText.toDoubleOrNull() ?: 0.0) / 100.0) * batchBaseKg
    }
    val totalNewStockKg = selectedNewRows.sumOf { it.stockKgText.toDoubleOrNull() ?: 0.0 }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Micro (3 Sectors) & Macro BOM Catalog",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "52 plant materials grouped into Macro (9) and 3 Micro sectors: Fast Moving (11), Slow Moving (17), and Non-Moving / Trial / New (15).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 4 Sector Tabs: Macro + 3 Micro Sectors
                val sectors = MaterialSector.entries
                val selectedSectorIdx = sectors.indexOf(activeSector).coerceAtLeast(0)
                ScrollableTabRow(
                    selectedTabIndex = selectedSectorIdx,
                    edgePadding = 12.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        if (selectedSectorIdx in tabPositions.indices) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSectorIdx]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    sectors.forEach { sec ->
                        val secTotal = rows.count { it.sector == sec }
                        val secSelected = rows.count { it.sector == sec && it.selected }
                        val isCurrent = activeSector == sec
                        val tabText = if (secSelected > 0) {
                            "${sec.shortLabel} ($secSelected/$secTotal)"
                        } else {
                            "${sec.shortLabel} ($secTotal)"
                        }
                        Tab(
                            selected = isCurrent,
                            onClick = { activeSector = sec },
                            text = {
                                Text(
                                    text = tabText,
                                    maxLines = 1,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("raw_sector_tab_${sec.name.lowercase()}")
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Sub-filter chips when viewing Micro: Non-Moving / Trial / New Micromix
                    if (activeSector == MaterialSector.MICRO_NON_MOVING) {
                        val allCount = rows.count { it.sector == MaterialSector.MICRO_NON_MOVING }
                        val nonCount = rows.count { it.sector == MaterialSector.MICRO_NON_MOVING && it.movementTag == MicroMovementTag.NON_MOVING }
                        val trialCount = rows.count { it.sector == MaterialSector.MICRO_NON_MOVING && it.movementTag == MicroMovementTag.TRIAL }
                        val newCount = rows.count { it.sector == MaterialSector.MICRO_NON_MOVING && it.movementTag == MicroMovementTag.NEW_MICROMIX }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = nonMovingSubFilter == null,
                                onClick = { nonMovingSubFilter = null },
                                label = { Text("All ($allCount)") }
                            )
                            FilterChip(
                                selected = nonMovingSubFilter == MicroMovementTag.NON_MOVING,
                                onClick = { nonMovingSubFilter = MicroMovementTag.NON_MOVING },
                                label = { Text("Non-Moving ($nonCount)") }
                            )
                            FilterChip(
                                selected = nonMovingSubFilter == MicroMovementTag.TRIAL,
                                onClick = { nonMovingSubFilter = MicroMovementTag.TRIAL },
                                label = { Text("Trial ($trialCount)") }
                            )
                            FilterChip(
                                selected = nonMovingSubFilter == MicroMovementTag.NEW_MICROMIX,
                                onClick = { nonMovingSubFilter = MicroMovementTag.NEW_MICROMIX },
                                label = { Text("New Micromix ($newCount)") }
                            )
                        }
                    }

                    // Search within sector
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        placeholder = { Text("Search ${activeSector.shortLabel} ingredients...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Batch Base + Quick Actions (Select Missing / Custom)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Base:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        listOf(500.0 to "500 kg", 1000.0 to "1T", 2000.0 to "2T").forEach { (kg, lbl) ->
                            FilterChip(
                                selected = abs(batchBaseKg - kg) < 1.0,
                                onClick = { batchBaseKg = kg },
                                label = { Text(lbl) }
                            )
                        }
                        if (missingInActiveSector.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    for (i in rows.indices) {
                                        val r = rows[i]
                                        if (r.sector == activeSector && r.name.trim().lowercase() !in existingByNameLower) {
                                            rows[i] = r.copy(selected = true)
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("select_missing_sector_button")
                            ) {
                                Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Check Missing (${missingInActiveSector.size})", maxLines = 1)
                            }
                        }
                        OutlinedButton(
                            onClick = { showCustomDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("category_bom_add_custom_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Custom", maxLines = 1)
                        }
                    }

                    // BOM Table Container for Active Sector
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // BOM Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "USE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(48.dp)
                                )
                                Text(
                                    text = "BOM LINE & SECTOR",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "BOM %",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(50.dp)
                                )
                                Text(
                                    text = "KG/RUN",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(64.dp)
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            if (sectorRows.isEmpty()) {
                                Text(
                                    text = "No materials match \"$searchQuery\" in ${activeSector.shortLabel}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }

                            sectorRows.forEachIndexed { idx, row ->
                                val globalIdx = rows.indexOfFirst { it.key == row.key }
                                val existingMatch = existingByNameLower[row.name.trim().lowercase()]
                                val pctVal = (row.inclusionPctText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                                val kgPerBatch = (pctVal / 100.0) * batchBaseKg
                                val (tagBg, tagFg) = movementTagBadgeColors(row.movementTag)

                                Surface(
                                    color = if (row.selected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (row.selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .minimumInteractiveComponentSize()
                                                .clickable {
                                                    if (globalIdx >= 0) {
                                                        rows[globalIdx] = row.copy(selected = !row.selected)
                                                    }
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = row.selected,
                                                onCheckedChange = { checked ->
                                                    if (globalIdx >= 0) {
                                                        rows[globalIdx] = row.copy(selected = checked)
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("cat_bom_check_${row.key}")
                                            )
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = String.format("%02d", idx + 1),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Surface(color = tagBg, shape = RoundedCornerShape(4.dp)) {
                                                        Text(
                                                            text = row.movementTag.label,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = tagFg,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                    if (existingMatch != null) {
                                                        Surface(
                                                            color = StatusHealthyContainer,
                                                            shape = RoundedCornerShape(999.dp)
                                                        ) {
                                                            Text(
                                                                text = "In Stock",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                color = OnStatusHealthyContainer,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = row.name,
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (existingMatch != null) {
                                                        "${row.bomCode} • On hand: ${OperationsCalculator.formatKg(existingMatch.currentStockKg)} kg"
                                                    } else {
                                                        "${row.bomCode} • ${row.category.label} • Init ${row.stockKgText} kg"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = "${String.format("%.1f", pctVal)}%",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary,
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.width(50.dp)
                                            )
                                            Text(
                                                text = OperationsCalculator.formatKg(kgPerBatch),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.width(64.dp)
                                            )
                                        }

                                        if (row.selected && globalIdx >= 0) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = row.inclusionPctText,
                                                    onValueChange = { rows[globalIdx] = row.copy(inclusionPctText = it) },
                                                    label = { Text("BOM %") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    modifier = Modifier.weight(0.85f)
                                                )
                                                if (existingMatch == null) {
                                                    OutlinedTextField(
                                                        value = row.stockKgText,
                                                        onValueChange = { rows[globalIdx] = row.copy(stockKgText = it) },
                                                        label = { Text("Init Stock kg") },
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                        modifier = Modifier.weight(1.1f)
                                                    )
                                                    OutlinedTextField(
                                                        value = row.reorderKgText,
                                                        onValueChange = { rows[globalIdx] = row.copy(reorderKgText = it) },
                                                        label = { Text("Min Alert kg") },
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                            if (existingMatch == null) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    OutlinedTextField(
                                                        value = row.supplierName,
                                                        onValueChange = { rows[globalIdx] = row.copy(supplierName = it) },
                                                        label = { Text("Supplier / Sector Tag") },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1.4f)
                                                    )
                                                    OutlinedTextField(
                                                        value = row.leadDaysText,
                                                        onValueChange = { rows[globalIdx] = row.copy(leadDaysText = it) },
                                                        label = { Text("Lead (d)") },
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.weight(0.75f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // BOM Totals Summary Bar across all Sector Tabs
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "BOM Selection: ${selectedRows.size} item(s) (${selectedNewRows.size} new)",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${String.format("%.1f%%", totalSelectedPct)} • ${OperationsCalculator.formatKg(totalSelectedBatchKg)} kg/run",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (totalSelectedPct > 100.5) StatusCriticalTextDark else MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "New intake stock to add: ${OperationsCalculator.formatKg(totalNewStockKg)} kg across checked items",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Optional: Also Save Selected Lines as a Feed BOM Recipe
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Also Save as Feed BOM Recipe",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Creates a linked Bill of Materials formula alongside adding new raw materials",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = alsoCreateBom,
                                    onCheckedChange = { alsoCreateBom = it },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("category_bom_also_create_switch")
                                )
                            }
                            if (alsoCreateBom) {
                                OutlinedTextField(
                                    value = bomProductName,
                                    onValueChange = { bomProductName = it },
                                    label = { Text("Finished Feed / BOM Name") },
                                    placeholder = { Text("e.g. Hog Starter Pellet BOM") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PlantSection.entries.forEach { sec ->
                                        FilterChip(
                                            selected = bomSection == sec,
                                            onClick = { bomSection = sec },
                                            label = { Text(sec.label) }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = bomNotes,
                                    onValueChange = { bomNotes = it },
                                    label = { Text("Mix Notes (optional)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    validationError?.let { err ->
                        Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }

                    Button(
                        onClick = {
                            if (selectedRows.isEmpty()) {
                                validationError = "Check at least one material across the Macro or Micro sectors."
                                return@Button
                            }
                            val newEntities = mutableListOf<RawMaterialEntity>()
                            val bomLines = mutableListOf<BomIngredientLine>()
                            for (row in selectedRows) {
                                val cleanName = row.name.trim()
                                val pct = row.inclusionPctText.toDoubleOrNull() ?: 0.0
                                if (cleanName.isBlank() || pct <= 0.0 || pct > 100.0) {
                                    validationError = "$cleanName: BOM % must be between 0.1 and 100."
                                    return@Button
                                }
                                val existing = existingByNameLower[cleanName.lowercase()]
                                if (existing == null) {
                                    val stock = row.stockKgText.toDoubleOrNull()
                                    val reorder = row.reorderKgText.toDoubleOrNull() ?: 100.0
                                    val lead = row.leadDaysText.toLongOrNull() ?: 4L
                                    if (stock == null || stock < 0.0) {
                                        validationError = "$cleanName: enter valid initial stock (kg)."
                                        return@Button
                                    }
                                    newEntities.add(
                                        RawMaterialEntity(
                                            name = cleanName,
                                            category = row.category.label,
                                            currentStockKg = stock,
                                            reorderThresholdKg = reorder.coerceAtLeast(0.0),
                                            usageSharePct = pct.coerceIn(0.1, 100.0),
                                            leadTimeDays = lead.coerceIn(1L, 60L),
                                            safetyDays = if (row.sector == MaterialSector.MACRO) 3L else 5L,
                                            supplierName = row.supplierName.trim().ifBlank { row.sector.label },
                                            alertsEnabled = row.sector != MaterialSector.MICRO_NON_MOVING
                                        )
                                    )
                                }
                                bomLines.add(
                                    BomIngredientLine(
                                        materialId = existing?.id.orEmpty(),
                                        materialName = cleanName,
                                        category = row.category.label,
                                        inclusionPct = pct,
                                        notes = "${row.movementTag.label}${if (row.supplierName.isNotBlank()) " • ${row.supplierName.trim()}" else ""}"
                                    )
                                )
                            }

                            if (!alsoCreateBom && newEntities.isEmpty()) {
                                validationError = "Checked ingredients are already in inventory. Select new items or enable 'Also Save as Feed BOM Recipe'."
                                return@Button
                            }

                            if (alsoCreateBom) {
                                val cleanFormula = bomProductName.trim()
                                if (cleanFormula.isBlank()) {
                                    validationError = "Enter a Finished Feed / BOM Name."
                                    return@Button
                                }
                                if (totalSelectedPct > 100.5) {
                                    validationError = "Total BOM inclusion exceeds 100.5%."
                                    return@Button
                                }
                                validationError = null
                                onSaveBomWithNewMaterials(
                                    newEntities,
                                    null,
                                    cleanFormula,
                                    bomSection,
                                    batchBaseKg,
                                    1.5,
                                    bomLines,
                                    bomNotes.trim()
                                )
                                for (i in rows.indices) {
                                    if (rows[i].selected) rows[i] = rows[i].copy(selected = false)
                                }
                                onDoneNavigateToBoms()
                            } else {
                                validationError = null
                                onAddRawMaterialsBulk(newEntities)
                                for (i in rows.indices) {
                                    if (rows[i].selected) rows[i] = rows[i].copy(selected = false)
                                }
                                onDoneNavigateToStock()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestEmerald, contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("submit_category_bom_button")
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        val btnText = when {
                            alsoCreateBom && selectedNewRows.isNotEmpty() ->
                                "Add ${selectedNewRows.size} Material(s) & Save BOM"
                            alsoCreateBom ->
                                "Save BOM (${selectedRows.size} lines)"
                            else ->
                                "Add ${selectedNewRows.size.coerceAtLeast(1)} Raw Material(s)"
                        }
                        Text(btnText)
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        AddCustomSectorBomRowDialog(
            sector = activeSector,
            onDismiss = { showCustomDialog = false },
            onConfirm = { name, category, movementTag, pct, stockKg, reorderKg, leadDays, supplier ->
                rows.add(
                    0,
                    SectorBomRowUi(
                        key = nextKey++,
                        name = name,
                        sector = activeSector,
                        movementTag = movementTag,
                        bomCode = if (activeSector == MaterialSector.MACRO) "MAC-USR" else "MIC-${movementTag.shortCode}",
                        category = category,
                        selected = true,
                        inclusionPctText = String.format("%.1f", pct),
                        stockKgText = stockKg.toInt().toString(),
                        reorderKgText = reorderKg.toInt().toString(),
                        leadDaysText = leadDays.toString(),
                        supplierName = supplier.ifBlank { movementTag.label },
                        isCustom = true
                    )
                )
                showCustomDialog = false
            }
        )
    }
}

@Composable
private fun AddCustomSectorBomRowDialog(
    sector: MaterialSector,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        category: RawMaterialCategory,
        movementTag: MicroMovementTag,
        pct: Double,
        stockKg: Double,
        reorderKg: Double,
        leadDays: Long,
        supplier: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember {
        mutableStateOf(
            if (sector == MaterialSector.MACRO) RawMaterialCategory.ENERGY else RawMaterialCategory.ADDITIVE
        )
    }
    var movementTag by remember {
        mutableStateOf(
            when (sector) {
                MaterialSector.MACRO -> MicroMovementTag.MACRO
                MaterialSector.MICRO_FAST -> MicroMovementTag.FAST_MOVING
                MaterialSector.MICRO_SLOW -> MicroMovementTag.SLOW_MOVING
                MaterialSector.MICRO_NON_MOVING -> MicroMovementTag.NON_MOVING
            }
        )
    }
    var pctText by remember { mutableStateOf(if (sector == MaterialSector.MACRO) "10.0" else "0.2") }
    var stockText by remember { mutableStateOf(if (sector == MaterialSector.MACRO) "2000" else "100") }
    var reorderText by remember { mutableStateOf(if (sector == MaterialSector.MACRO) "500" else "30") }
    var leadText by remember { mutableStateOf("4") }
    var supplier by remember { mutableStateOf(sector.label) }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom ${sector.shortLabel} Material") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ingredient Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RawMaterialCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }
                if (sector.isMicro) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            MicroMovementTag.FAST_MOVING,
                            MicroMovementTag.SLOW_MOVING,
                            MicroMovementTag.NON_MOVING,
                            MicroMovementTag.TRIAL,
                            MicroMovementTag.NEW_MICROMIX
                        ).forEach { tag ->
                            FilterChip(
                                selected = movementTag == tag,
                                onClick = { movementTag = tag },
                                label = { Text(tag.label) }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pctText,
                        onValueChange = { pctText = it },
                        label = { Text("BOM %") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it },
                        label = { Text("Stock kg") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reorderText,
                        onValueChange = { reorderText = it },
                        label = { Text("Min Alert kg") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = leadText,
                        onValueChange = { leadText = it },
                        label = { Text("Lead (d)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = supplier,
                    onValueChange = { supplier = it },
                    label = { Text("Supplier / Sector Tag") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                err?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = name.trim()
                    val pct = pctText.toDoubleOrNull() ?: 0.0
                    val stock = stockText.toDoubleOrNull()
                    val reorder = reorderText.toDoubleOrNull() ?: 50.0
                    val lead = leadText.toLongOrNull() ?: 4L
                    if (clean.isBlank()) {
                        err = "Enter ingredient name."
                        return@Button
                    }
                    if (pct <= 0.0 || pct > 100.0) {
                        err = "BOM % must be 0.1–100."
                        return@Button
                    }
                    if (stock == null || stock < 0.0) {
                        err = "Enter valid stock kg."
                        return@Button
                    }
                    onConfirm(clean, category, movementTag, pct, stock, reorder.coerceAtLeast(0.0), lead.coerceIn(1L, 60L), supplier.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForestEmerald, contentColor = Color.White)
            ) {
                Text("Add Row")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private data class BulkBomSheetRowUi(
    val key: Int,
    val name: String = "",
    val category: RawMaterialCategory = RawMaterialCategory.ENERGY,
    val stockKgText: String = "",
    val reorderKgText: String = "400",
    val sharePctText: String = "10.0",
    val leadDaysText: String = "4",
    val supplierName: String = ""
)

@Composable
fun BulkBomSheetTab(
    skus: List<SkuEntity>,
    existingMaterials: List<RawMaterialEntity>,
    onAddBulk: (List<RawMaterialEntity>) -> Unit,
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
    onDoneNavigateToStock: () -> Unit,
    onDoneNavigateToBoms: () -> Unit
) {
    var nextRowKey by remember { mutableIntStateOf(20) }
    var batchBaseKg by remember { mutableStateOf(1000.0) }
    var alsoSaveAsBom by remember { mutableStateOf(false) }
    var formulaName by remember { mutableStateOf(skus.firstOrNull()?.productName.orEmpty()) }
    var bomSection by remember {
        mutableStateOf(
            skus.firstOrNull()?.let { PlantSection.fromRaw(it.section) } ?: PlantSection.SAN_ROQUE
        )
    }

    val rows = remember {
        mutableStateListOf(
            BulkBomSheetRowUi(key = 1),
            BulkBomSheetRowUi(key = 2),
            BulkBomSheetRowUi(key = 3)
        )
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val existingByNameLower = remember(existingMaterials) {
        existingMaterials.associateBy { it.name.trim().lowercase() }
    }

    val filledRows = rows.filter { it.name.trim().isNotEmpty() }
    val validNewCount = filledRows.count {
        it.name.trim().lowercase() !in existingByNameLower && (it.stockKgText.toDoubleOrNull() ?: -1.0) >= 0.0
    }
    val totalStockKg = filledRows.sumOf { it.stockKgText.toDoubleOrNull() ?: 0.0 }
    val totalMixPct = filledRows.sumOf { (it.sharePctText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0) }
    val totalBatchKg = (totalMixPct / 100.0) * batchBaseKg

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Bulk BOM Sheet & Sector Loader",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Load an entire Macro or Micro sector or enter custom rows in Bill of Materials spreadsheet format.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Batch Reference Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BOM Batch Base:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(500.0 to "500 kg", 1000.0 to "1,000 kg (1T)", 2000.0 to "2,000 kg (2T)", 5000.0 to "5,000 kg").forEach { (kg, lbl) ->
                    FilterChip(
                        selected = abs(batchBaseKg - kg) < 1.0,
                        onClick = { batchBaseKg = kg },
                        label = { Text(lbl) }
                    )
                }
            }

            // Action & Real Sector Template Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { rows.add(BulkBomSheetRowUi(key = nextRowKey++)) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("bulk_add_row_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Add Row")
                }
                OutlinedButton(
                    onClick = {
                        rows.clear()
                        PlantMaterialCatalog.specsForSector(MaterialSector.MACRO).forEach { spec ->
                            rows.add(
                                BulkBomSheetRowUi(
                                    key = nextRowKey++,
                                    name = spec.name,
                                    category = spec.category,
                                    stockKgText = spec.defaultStockKg.toInt().toString(),
                                    reorderKgText = spec.defaultReorderKg.toInt().toString(),
                                    sharePctText = String.format("%.1f", spec.defaultInclusionPct),
                                    leadDaysText = spec.defaultLeadDays.toString(),
                                    supplierName = spec.defaultSupplier
                                )
                            )
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("bulk_load_macro_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Load Macro (9)")
                }
                OutlinedButton(
                    onClick = {
                        rows.clear()
                        PlantMaterialCatalog.specsForSector(MaterialSector.MICRO_FAST).forEach { spec ->
                            rows.add(
                                BulkBomSheetRowUi(
                                    key = nextRowKey++,
                                    name = spec.name,
                                    category = spec.category,
                                    stockKgText = spec.defaultStockKg.toInt().toString(),
                                    reorderKgText = spec.defaultReorderKg.toInt().toString(),
                                    sharePctText = String.format("%.1f", spec.defaultInclusionPct),
                                    leadDaysText = spec.defaultLeadDays.toString(),
                                    supplierName = spec.defaultSupplier
                                )
                            )
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("bulk_load_micro_fast_button")
                ) {
                    Text("Load Micro Fast (11)")
                }
                OutlinedButton(
                    onClick = {
                        rows.clear()
                        PlantMaterialCatalog.specsForSector(MaterialSector.MICRO_SLOW).forEach { spec ->
                            rows.add(
                                BulkBomSheetRowUi(
                                    key = nextRowKey++,
                                    name = spec.name,
                                    category = spec.category,
                                    stockKgText = spec.defaultStockKg.toInt().toString(),
                                    reorderKgText = spec.defaultReorderKg.toInt().toString(),
                                    sharePctText = String.format("%.1f", spec.defaultInclusionPct),
                                    leadDaysText = spec.defaultLeadDays.toString(),
                                    supplierName = spec.defaultSupplier
                                )
                            )
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("bulk_load_micro_slow_button")
                ) {
                    Text("Load Micro Slow (17)")
                }
                OutlinedButton(
                    onClick = {
                        rows.clear()
                        PlantMaterialCatalog.specsForSector(MaterialSector.MICRO_NON_MOVING).forEach { spec ->
                            rows.add(
                                BulkBomSheetRowUi(
                                    key = nextRowKey++,
                                    name = spec.name,
                                    category = spec.category,
                                    stockKgText = spec.defaultStockKg.toInt().toString(),
                                    reorderKgText = spec.defaultReorderKg.toInt().toString(),
                                    sharePctText = String.format("%.1f", spec.defaultInclusionPct),
                                    leadDaysText = spec.defaultLeadDays.toString(),
                                    supplierName = spec.defaultSupplier
                                )
                            )
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("bulk_load_micro_nonmoving_button")
                ) {
                    Text("Load Micro Non-Moving (15)")
                }
                OutlinedButton(
                    onClick = {
                        rows.clear()
                        rows.addAll(
                            listOf(
                                BulkBomSheetRowUi(nextRowKey++, "Whole Yellow Corn/Balanok Corn", RawMaterialCategory.ENERGY, "5000", "2500", "50.0", "4", "Macro Grain Supply"),
                                BulkBomSheetRowUi(nextRowKey++, "US Soya (Soy Bean Meal)", RawMaterialCategory.PROTEIN, "3200", "1500", "26.0", "5", "US Soya Import"),
                                BulkBomSheetRowUi(nextRowKey++, "Rice Bran D1", RawMaterialCategory.ENERGY, "3500", "1200", "12.0", "3", "Nueva Ecija Ricemill"),
                                BulkBomSheetRowUi(nextRowKey++, "Hard Pollard", RawMaterialCategory.ENERGY, "2200", "800", "6.5", "3", "Flour Mill Supply"),
                                BulkBomSheetRowUi(nextRowKey++, "Mollases", RawMaterialCategory.LIQUID, "1100", "450", "2.0", "4", "Sugar Central"),
                                BulkBomSheetRowUi(nextRowKey++, "Mesh 100 (Powder Limestone)", RawMaterialCategory.MINERAL, "850", "300", "1.5", "3", "Micro • Fast Moving"),
                                BulkBomSheetRowUi(nextRowKey++, "MDCP/MonodiCalcium Phosphate", RawMaterialCategory.MINERAL, "550", "200", "1.0", "5", "Micro • Slow Moving"),
                                BulkBomSheetRowUi(nextRowKey++, "Swine Vitamin 1", RawMaterialCategory.ADDITIVE, "180", "60", "0.5", "5", "Micro • Slow Moving"),
                                BulkBomSheetRowUi(nextRowKey++, "Iodized Salt", RawMaterialCategory.MINERAL, "350", "100", "0.5", "3", "Micro • Slow Moving")
                            )
                        )
                        if (formulaName.isBlank()) formulaName = "Swine Grower 100% BOM"
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("bulk_load_starter_bom_button")
                ) {
                    Text("100% Mix BOM")
                }
            }

            // BOM Table Header
            Surface(
                color = ForestEmerald.copy(alpha = 0.14f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LINE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(42.dp)
                    )
                    Text(
                        text = "CODE & INGREDIENT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "BOM %",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(54.dp)
                    )
                    Text(
                        text = "KG/RUN",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(66.dp)
                    )
                }
            }

            rows.forEachIndexed { idx, row ->
                val pctVal = (row.sharePctText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                val rowBatchKg = (pctVal / 100.0) * batchBaseKg
                val isExisting = row.name.trim().lowercase() in existingByNameLower
                val catalogMatch = PlantMaterialCatalog.resolveSpecByName(row.name)
                val rowCode = catalogMatch?.bomCode ?: rawMaterialBomCode(row.category)

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = String.format("%02d • %s", idx + 1, rowCode),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                                if (catalogMatch != null) {
                                    val (tagBg, tagFg) = movementTagBadgeColors(catalogMatch.movementTag)
                                    Surface(color = tagBg, shape = RoundedCornerShape(4.dp)) {
                                        Text(
                                            text = catalogMatch.movementTag.label,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = tagFg,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (isExisting) {
                                    Surface(
                                        color = StatusHealthyContainer,
                                        shape = RoundedCornerShape(999.dp)
                                    ) {
                                        Text(
                                            text = "In Stock",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = OnStatusHealthyContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${OperationsCalculator.formatKg(rowBatchKg)} kg/run",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (rows.size > 1) {
                                IconButton(
                                    onClick = { rows.removeAt(idx) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Remove BOM row ${idx + 1}",
                                        tint = StatusCriticalTextDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = row.name,
                            onValueChange = { rows[idx] = row.copy(name = it) },
                            label = { Text("Ingredient Name") },
                            placeholder = { Text("e.g. Whole Yellow Corn / Mesh 100") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RawMaterialCategory.entries.forEach { cat ->
                                FilterChip(
                                    selected = row.category == cat,
                                    onClick = { rows[idx] = row.copy(category = cat) },
                                    label = { Text(cat.label) }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = row.sharePctText,
                                onValueChange = { rows[idx] = row.copy(sharePctText = it) },
                                label = { Text("BOM %") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(0.85f)
                            )
                            OutlinedTextField(
                                value = row.stockKgText,
                                onValueChange = { rows[idx] = row.copy(stockKgText = it) },
                                label = { Text("Stock (kg)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1.05f)
                            )
                            OutlinedTextField(
                                value = row.reorderKgText,
                                onValueChange = { rows[idx] = row.copy(reorderKgText = it) },
                                label = { Text("Min (kg)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(0.95f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = row.supplierName,
                                onValueChange = { rows[idx] = row.copy(supplierName = it) },
                                label = { Text("Supplier / Sector Tag") },
                                singleLine = true,
                                modifier = Modifier.weight(1.4f)
                            )
                            OutlinedTextField(
                                value = row.leadDaysText,
                                onValueChange = { rows[idx] = row.copy(leadDaysText = it) },
                                label = { Text("Lead (d)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.75f)
                            )
                        }
                    }
                }
            }

            // Totals Row in BOM format
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL (${filledRows.size} lines • $validNewCount new)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${String.format("%.1f%%", totalMixPct)} • ${OperationsCalculator.formatKg(totalBatchKg)} kg/run",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = when {
                                abs(totalMixPct - 100.0) <= 0.15 -> StatusHealthyTextDark
                                totalMixPct > 100.5 -> StatusCriticalTextDark
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                    Text(
                        text = "Total receiving stock: ${OperationsCalculator.formatKg(totalStockKg)} kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Optional Linked BOM Toggle
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Save Sheet as BOM Formula Too",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Creates a Feed Bill of Materials recipe using all rows above",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = alsoSaveAsBom,
                            onCheckedChange = { alsoSaveAsBom = it },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("bulk_save_as_bom_switch")
                        )
                    }
                    if (alsoSaveAsBom) {
                        OutlinedTextField(
                            value = formulaName,
                            onValueChange = { formulaName = it },
                            label = { Text("Finished Feed / BOM Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PlantSection.entries.forEach { sec ->
                                FilterChip(
                                    selected = bomSection == sec,
                                    onClick = { bomSection = sec },
                                    label = { Text(sec.label) }
                                )
                            }
                        }
                    }
                }
            }

            errorMsg?.let { err ->
                Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    val newEntities = mutableListOf<RawMaterialEntity>()
                    val bomLines = mutableListOf<BomIngredientLine>()
                    for (row in rows) {
                        val clean = row.name.trim()
                        if (clean.isBlank() && row.stockKgText.isBlank()) continue
                        if (clean.isBlank()) {
                            errorMsg = "Every filled row needs an ingredient name."
                            return@Button
                        }
                        val pct = row.sharePctText.toDoubleOrNull() ?: 0.0
                        if (pct <= 0.0 || pct > 100.0) {
                            errorMsg = "$clean: BOM % must be between 0.1 and 100."
                            return@Button
                        }
                        val existing = existingByNameLower[clean.lowercase()]
                        if (existing == null) {
                            val stock = row.stockKgText.toDoubleOrNull()
                            if (stock == null || stock < 0.0) {
                                errorMsg = "$clean: enter a valid stock (kg)."
                                return@Button
                            }
                            newEntities.add(
                                RawMaterialEntity(
                                    name = clean,
                                    category = row.category.label,
                                    currentStockKg = stock,
                                    reorderThresholdKg = (row.reorderKgText.toDoubleOrNull() ?: 100.0).coerceAtLeast(0.0),
                                    usageSharePct = pct.coerceIn(0.1, 100.0),
                                    leadTimeDays = (row.leadDaysText.toLongOrNull() ?: 4L).coerceIn(1L, 60L),
                                    safetyDays = 3L,
                                    supplierName = row.supplierName.trim(),
                                    alertsEnabled = true
                                )
                            )
                        }
                        bomLines.add(
                            BomIngredientLine(
                                materialId = existing?.id.orEmpty(),
                                materialName = clean,
                                category = row.category.label,
                                inclusionPct = pct,
                                notes = row.supplierName.trim()
                            )
                        )
                    }
                    if (bomLines.isEmpty()) {
                        errorMsg = "Fill in at least one raw material row to save."
                        return@Button
                    }
                    if (alsoSaveAsBom) {
                        val cleanFormula = formulaName.trim()
                        if (cleanFormula.isBlank()) {
                            errorMsg = "Enter a Finished Feed / BOM Name."
                            return@Button
                        }
                        if (totalMixPct > 100.5) {
                            errorMsg = "Total BOM % exceeds 100.5%. Adjust row percentages first."
                            return@Button
                        }
                        errorMsg = null
                        onSaveBomWithNewMaterials(
                            newEntities,
                            null,
                            cleanFormula,
                            bomSection,
                            batchBaseKg,
                            1.5,
                            bomLines,
                            "Created from Bulk BOM Sheet"
                        )
                        rows.clear()
                        rows.add(BulkBomSheetRowUi(key = nextRowKey++))
                        onDoneNavigateToBoms()
                    } else {
                        if (newEntities.isEmpty()) {
                            errorMsg = "All entered ingredients are already in inventory. Add new items or toggle 'Save Sheet as BOM Formula Too'."
                            return@Button
                        }
                        errorMsg = null
                        onAddBulk(newEntities)
                        rows.clear()
                        rows.add(BulkBomSheetRowUi(key = nextRowKey++))
                        onDoneNavigateToStock()
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestEmerald, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("submit_bulk_raw_materials_button")
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (alsoSaveAsBom) {
                        "Save BOM Sheet ($validNewCount New Mat)"
                    } else {
                        "Add ${validNewCount.coerceAtLeast(1)} Raw Material(s)"
                    }
                )
            }
        }
    }
}

@Composable
fun SingleBomSpecEntryCard(
    onSaveSingle: (
        name: String,
        category: RawMaterialCategory,
        stockKg: Double,
        reorderKg: Double,
        sharePct: Double,
        leadDays: Long,
        safetyDays: Long,
        supplier: String,
        alertsEnabled: Boolean
    ) -> Unit
) {
    var presetSector by remember { mutableStateOf(MaterialSector.MACRO) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(RawMaterialCategory.ENERGY) }
    var stockKgText by remember { mutableStateOf("") }
    var reorderKgText by remember { mutableStateOf("500") }
    var usageShareText by remember { mutableStateOf("10.0") }
    var leadTimeText by remember { mutableStateOf("4") }
    var safetyDaysText by remember { mutableStateOf("3") }
    var supplierName by remember { mutableStateOf("") }
    var alertsEnabled by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val sectorPresets = remember(presetSector) {
        PlantMaterialCatalog.specsForSector(presetSector)
    }

    val matchedCatalog = remember(name) { PlantMaterialCatalog.resolveSpecByName(name) }
    val sharePct = (usageShareText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
    val stockKgVal = (stockKgText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val kgPer1Ton = (sharePct / 100.0) * 1000.0
    val batchesSupported = if (kgPer1Ton > 0.0) (stockKgVal / kgPer1Ton).toInt() else 0

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Single Raw Material BOM Spec",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Pick from Macro or 3 Micro sectors or enter a custom ingredient spec",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = matchedCatalog?.bomCode ?: rawMaterialBomCode(category),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Quick Select by Sector (${sectorPresets.size} items)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MaterialSector.entries.forEach { sec ->
                        FilterChip(
                            selected = presetSector == sec,
                            onClick = { presetSector = sec },
                            label = { Text(sec.shortLabel) }
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sectorPresets.forEach { spec ->
                        FilterChip(
                            selected = name.equals(spec.name, ignoreCase = true),
                            onClick = {
                                name = spec.name
                                category = spec.category
                                usageShareText = String.format("%.1f", spec.defaultInclusionPct)
                                stockKgText = spec.defaultStockKg.toInt().toString()
                                reorderKgText = spec.defaultReorderKg.toInt().toString()
                                leadTimeText = spec.defaultLeadDays.toString()
                                supplierName = spec.defaultSupplier
                                alertsEnabled = spec.sector != MaterialSector.MICRO_NON_MOVING
                            },
                            label = { Text(spec.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Ingredient Name") },
                placeholder = { Text("e.g. Mesh 100 (Powder Limestone)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("single_raw_name_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RawMaterialCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.label) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = stockKgText,
                    onValueChange = { stockKgText = it },
                    label = { Text("Stock on Hand (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("single_raw_stock_input")
                )
                OutlinedTextField(
                    value = reorderKgText,
                    onValueChange = { reorderKgText = it },
                    label = { Text("Reorder Alert (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = usageShareText,
                    onValueChange = { usageShareText = it },
                    label = { Text("BOM Mix %") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = leadTimeText,
                    onValueChange = { leadTimeText = it },
                    label = { Text("Lead (days)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = safetyDaysText,
                    onValueChange = { safetyDaysText = it },
                    label = { Text("Safety (d)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            // BOM Batch Scaling Preview Table
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "BOM Batch Requirement Preview (${String.format("%.1f%%", sharePct)})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(500.0 to "500 kg", 1000.0 to "1T Batch", 2000.0 to "2T Batch", 5000.0 to "5T Batch").forEach { (bKg, lbl) ->
                            val req = (sharePct / 100.0) * bKg
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(lbl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${OperationsCalculator.formatKg(req)} kg",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    if (stockKgVal > 0.0 && kgPer1Ton > 0.0) {
                        Text(
                            text = "Current stock supports ~$batchesSupported standard 1,000 kg (1T) batches.",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusWatchTextDark
                        )
                    }
                }
            }

            OutlinedTextField(
                value = supplierName,
                onValueChange = { supplierName = it },
                label = { Text("Supplier / Sector Tag (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Low-Stock Forecast Alerts",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Notify phone when projected runway drops below lead time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = alertsEnabled,
                        onCheckedChange = { alertsEnabled = it },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                }
            }

            errorMsg?.let { err ->
                Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    val clean = name.trim()
                    val stock = stockKgText.toDoubleOrNull()
                    val reorder = reorderKgText.toDoubleOrNull()
                    val share = usageShareText.toDoubleOrNull()
                    val lead = leadTimeText.toLongOrNull() ?: 4L
                    val safety = safetyDaysText.toLongOrNull() ?: 3L
                    if (clean.isBlank()) {
                        errorMsg = "Please enter an ingredient name."
                        return@Button
                    }
                    if (stock == null || stock < 0.0) {
                        errorMsg = "Please enter valid stock on hand (kg)."
                        return@Button
                    }
                    if (reorder == null || reorder < 0.0) {
                        errorMsg = "Please enter valid reorder alert kg."
                        return@Button
                    }
                    if (share == null || share <= 0.0 || share > 100.0) {
                        errorMsg = "BOM Mix % must be > 0 and <= 100."
                        return@Button
                    }
                    errorMsg = null
                    onSaveSingle(clean, category, stock, reorder, share, lead.coerceIn(1L, 60L), safety.coerceIn(0L, 30L), supplierName.trim(), alertsEnabled)
                    name = ""
                    stockKgText = ""
                    supplierName = ""
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestEmerald, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("submit_single_raw_material_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Raw Material")
            }
        }
    }
}
