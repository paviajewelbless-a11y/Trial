package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.model.PlantMaterialCatalog
import com.example.model.PlantMaterialSpec
import com.example.model.PlantSection
import com.example.model.RawMaterialCategory
import com.example.model.RawMaterialEntity
import com.example.model.SkuEntity
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.GrainAmber
import com.example.ui.theme.GrainAmberBright
import com.example.ui.theme.OnStatusHealthyContainer
import com.example.ui.theme.OnStatusWatchContainer
import com.example.ui.theme.StatusCriticalTextDark
import com.example.ui.theme.StatusHealthyContainer
import com.example.ui.theme.StatusHealthyTextDark
import com.example.ui.theme.StatusWatchContainer
import com.example.ui.theme.StatusWatchTextDark
import kotlin.math.abs
import kotlin.math.floor

private data class DraftBomLineUi(
    val key: Int,
    val materialId: String,
    val materialName: String,
    val category: RawMaterialCategory,
    val inclusionPctText: String,
    val notes: String,
    val isNewMaterial: Boolean,
    val newStockKg: Double = 0.0,
    val newReorderKg: Double = 400.0,
    val newLeadDays: Long = 4L,
    val newSafetyDays: Long = 3L,
    val newSupplier: String = "",
    val alertsEnabled: Boolean = true
)

@Composable
fun BomRecipeCard(
    bom: FeedBomEntity,
    materials: List<RawMaterialEntity>,
    onEditBom: (FeedBomEntity) -> Unit,
    onDeleteBom: (FeedBomEntity) -> Unit,
    onQuickRestockMaterial: (RawMaterialEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSimulator by remember { mutableStateOf(false) }
    var simBatchKgText by remember(bom.id) { mutableStateOf(bom.batchBaseKg.toInt().coerceAtLeast(100).toString()) }
    var confirmDelete by remember { mutableStateOf(false) }

    val materialById = remember(materials) { materials.associateBy { it.id } }
    val materialByName = remember(materials) { materials.associateBy { it.name.trim().lowercase() } }

    val resolvedRows = remember(bom, materials) {
        bom.lines.map { line ->
            val matchedMat = materialById[line.materialId]
                ?: materialByName[line.materialName.trim().lowercase()]
            val kgPerBaseBatch = (line.inclusionPct / 100.0) * bom.batchBaseKg
            val maxBatches = if (matchedMat != null && kgPerBaseBatch > 0.0) {
                floor(matchedMat.currentStockKg / kgPerBaseBatch).toInt().coerceAtLeast(0)
            } else {
                null
            }
            Triple(line, matchedMat, maxBatches)
        }
    }

    val totalPct = bom.totalInclusionPct()
    val isBalanced = abs(totalPct - 100.0) <= 0.15
    val bottleneckBatches = resolvedRows.mapNotNull { it.third }.minOrNull()

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
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
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = bom.productName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${bom.section} • Base: ${OperationsCalculator.formatKg(bom.batchBaseKg)} kg • Loss: ${String.format("%.1f%%", bom.yieldLossPct)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = if (isBalanced) StatusHealthyContainer else StatusWatchContainer,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = if (isBalanced) "100% Mix" else "${String.format("%.1f%%", totalPct)} Mix",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isBalanced) OnStatusHealthyContainer else OnStatusWatchContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (bottleneckBatches != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Max Producible Runs (${OperationsCalculator.formatKg(bom.batchBaseKg)} kg/run):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$bottleneckBatches batch(es)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = when {
                                bottleneckBatches <= 2 -> StatusCriticalTextDark
                                bottleneckBatches <= 5 -> StatusWatchTextDark
                                else -> StatusHealthyTextDark
                            }
                        )
                    }
                }
            }

            // BOM Structured Table Header
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(22.dp)
                        )
                        Text(
                            text = "Ingredient",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(48.dp)
                        )
                        Text(
                            text = "kg/Run",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(62.dp)
                        )
                        Text(
                            text = "On Hand",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(70.dp)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    resolvedRows.forEachIndexed { idx, (line, mat, maxBatches) ->
                        val kgPerRun = (line.inclusionPct / 100.0) * bom.batchBaseKg
                        val stockColor = when {
                            mat == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            (maxBatches ?: 99) <= 2 -> StatusCriticalTextDark
                            (maxBatches ?: 99) <= 5 -> StatusWatchTextDark
                            else -> StatusHealthyTextDark
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = line.materialName,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (line.notes.isNotBlank()) {
                                    Text(
                                        text = line.notes,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text(
                                text = "${String.format("%.1f", line.inclusionPct)}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(48.dp)
                            )
                            Text(
                                text = OperationsCalculator.formatKg(kgPerRun),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(62.dp)
                            )
                            Text(
                                text = if (mat != null) "${OperationsCalculator.formatKg(mat.currentStockKg)}" else "Unlinked",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = stockColor,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(70.dp)
                            )
                        }
                        if (idx < resolvedRows.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                        }
                    }
                }
            }

            if (bom.notes.isNotBlank()) {
                Text(
                    text = "Mix Note: ${bom.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expandable Batch Requirement Calculator
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSimulator = !showSimulator }
                    .minimumInteractiveComponentSize()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Batch Scaling Calculator",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = if (showSimulator) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (showSimulator) "Collapse calculator" else "Expand calculator",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = showSimulator) {
                val targetBatchKg = simBatchKgText.toDoubleOrNull()?.coerceIn(1.0, 500_000.0) ?: bom.batchBaseKg
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(500 to "500 kg (10b)", 1000 to "1T (20b)", 2000 to "2T (40b)", 5000 to "5T (100b)").forEach { (kg, label) ->
                            FilterChip(
                                selected = abs(targetBatchKg - kg) < 1.0,
                                onClick = { simBatchKgText = kg.toString() },
                                label = { Text(label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = simBatchKgText,
                        onValueChange = { simBatchKgText = it },
                        label = { Text("Custom Mix Batch Size (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    resolvedRows.forEach { (line, mat, _) ->
                        val neededKg = (line.inclusionPct / 100.0) * targetBatchKg
                        val shortKg = if (mat != null) (neededKg - mat.currentStockKg).coerceAtLeast(0.0) else neededKg
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${line.materialName} (${String.format("%.1f%%", line.inclusionPct)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Need ${OperationsCalculator.formatKg(neededKg)} kg",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (mat != null && shortKg > 0.1) {
                                TextButton(
                                    onClick = { onQuickRestockMaterial(mat) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Text(
                                        text = "Short ${OperationsCalculator.formatKg(shortKg)} kg",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = StatusCriticalTextDark
                                    )
                                }
                            } else {
                                Text(
                                    text = if (mat != null) "OK" else "No stock",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (mat != null) StatusHealthyTextDark else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onEditBom(bom) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("edit_bom_button_${bom.id}")
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Formula", maxLines = 1)
                }
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCriticalTextDark),
                    border = BorderStroke(1.dp, StatusCriticalTextDark.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_bom_button_${bom.id}")
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete BOM ${bom.productName}", modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete Bill of Materials?") },
            text = {
                Text("Remove the BOM recipe for ${bom.productName}? Existing raw materials remain in inventory, and forecasts will fall back to item mix share %.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDeleteBom(bom)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete BOM")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AddRawMaterialsTabbedWorkspace(
    skus: List<SkuEntity>,
    existingMaterials: List<RawMaterialEntity>,
    existingBoms: List<FeedBomEntity>,
    preloadedBom: FeedBomEntity?,
    onClearPreloadedBom: () -> Unit,
    onSaveSingleRawMaterial: (
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
    ) -> Unit,
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
    var selectedEntryTab by remember { mutableIntStateOf(0) } // 0 = BOM Formula, 1 = By Category, 2 = Bulk BOM Sheet, 3 = Single Spec

    LaunchedEffect(preloadedBom) {
        if (preloadedBom != null) {
            selectedEntryTab = 0
        }
    }

    val entryTabs = listOf(
        Triple("BOM Formula", Icons.Filled.Science, "add_mat_tab_bom"),
        Triple("Micro & Macro", Icons.Filled.Category, "add_mat_tab_category"),
        Triple("Bulk BOM Sheet", Icons.Filled.TableRows, "add_mat_tab_bulk"),
        Triple("Single Spec", Icons.Filled.Add, "add_mat_tab_single")
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
        ) {
            Column {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Add Raw Materials (Bill of Materials Studio)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Build a BOM formula, browse Micro (3 sectors) & Macro materials, fill a bulk BOM sheet, or log a single spec.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ScrollableTabRow(
                    selectedTabIndex = selectedEntryTab,
                    edgePadding = 12.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        if (selectedEntryTab in tabPositions.indices) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedEntryTab]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    entryTabs.forEachIndexed { idx, (label, icon, tag) ->
                        Tab(
                            selected = selectedEntryTab == idx,
                            onClick = { selectedEntryTab = idx },
                            text = {
                                Text(
                                    text = label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (selectedEntryTab == idx) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag(tag)
                        )
                    }
                }
            }
        }

        when (selectedEntryTab) {
            0 -> BomFormulaBuilderCard(
                skus = skus,
                existingMaterials = existingMaterials,
                existingBoms = existingBoms,
                preloadedBom = preloadedBom,
                onClearPreloadedBom = onClearPreloadedBom,
                onSaveBomWithNewMaterials = { newMats, existingBom, prodName, sec, baseKg, lossPct, lines, notes ->
                    onSaveBomWithNewMaterials(newMats, existingBom, prodName, sec, baseKg, lossPct, lines, notes)
                    onDoneNavigateToBoms()
                }
            )

            1 -> CategoryBomIntakeTab(
                skus = skus,
                existingMaterials = existingMaterials,
                onAddRawMaterialsBulk = onAddRawMaterialsBulk,
                onSaveBomWithNewMaterials = onSaveBomWithNewMaterials,
                onDoneNavigateToStock = onDoneNavigateToStock,
                onDoneNavigateToBoms = onDoneNavigateToBoms
            )

            2 -> BulkBomSheetTab(
                skus = skus,
                existingMaterials = existingMaterials,
                onAddBulk = onAddRawMaterialsBulk,
                onSaveBomWithNewMaterials = onSaveBomWithNewMaterials,
                onDoneNavigateToStock = onDoneNavigateToStock,
                onDoneNavigateToBoms = onDoneNavigateToBoms
            )

            else -> SingleBomSpecEntryCard(
                onSaveSingle = { name, cat, stockKg, reorderKg, sharePct, leadDays, safetyDays, supplier, alerts ->
                    onSaveSingleRawMaterial(
                        null,
                        name,
                        cat,
                        stockKg,
                        reorderKg,
                        sharePct,
                        leadDays,
                        safetyDays,
                        supplier,
                        alerts
                    )
                    onDoneNavigateToStock()
                }
            )
        }
    }
}

@Composable
fun BomFormulaBuilderCard(
    skus: List<SkuEntity>,
    existingMaterials: List<RawMaterialEntity>,
    existingBoms: List<FeedBomEntity>,
    preloadedBom: FeedBomEntity?,
    onClearPreloadedBom: () -> Unit,
    onSaveBomWithNewMaterials: (
        newMaterials: List<RawMaterialEntity>,
        existingBom: FeedBomEntity?,
        productName: String,
        section: PlantSection,
        batchBaseKg: Double,
        yieldLossPct: Double,
        draftLines: List<BomIngredientLine>,
        notes: String
    ) -> Unit
) {
    var nextKey by remember { mutableIntStateOf(100) }
    var editingBomTarget by remember { mutableStateOf(preloadedBom?.takeIf { it.id.isNotBlank() }) }
    var productName by remember {
        mutableStateOf(preloadedBom?.productName ?: skus.firstOrNull()?.productName ?: "")
    }
    var selectedSection by remember {
        mutableStateOf(
            preloadedBom?.let { PlantSection.fromRaw(it.section) }
                ?: skus.firstOrNull()?.let { PlantSection.fromRaw(it.section) }
                ?: PlantSection.SAN_ROQUE
        )
    }
    var batchBaseKgText by remember {
        mutableStateOf((preloadedBom?.batchBaseKg ?: 1000.0).toInt().toString())
    }
    var yieldLossText by remember {
        mutableStateOf((preloadedBom?.yieldLossPct ?: 1.5).toString())
    }
    var bomNotes by remember { mutableStateOf(preloadedBom?.notes.orEmpty()) }
    val draftLines = remember { mutableStateListOf<DraftBomLineUi>() }

    var showPickExistingDialog by remember { mutableStateOf(false) }
    var showNewMaterialDialog by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(preloadedBom) {
        if (preloadedBom != null) {
            editingBomTarget = preloadedBom.takeIf { it.id.isNotBlank() }
            productName = preloadedBom.productName
            selectedSection = PlantSection.fromRaw(preloadedBom.section)
            batchBaseKgText = preloadedBom.batchBaseKg.toInt().coerceAtLeast(1).toString()
            yieldLossText = preloadedBom.yieldLossPct.toString()
            bomNotes = preloadedBom.notes
            draftLines.clear()
            preloadedBom.lines.forEachIndexed { idx, line ->
                draftLines.add(
                    DraftBomLineUi(
                        key = idx + 1,
                        materialId = line.materialId,
                        materialName = line.materialName,
                        category = RawMaterialCategory.fromRaw(line.category),
                        inclusionPctText = String.format("%.1f", line.inclusionPct),
                        notes = line.notes,
                        isNewMaterial = false
                    )
                )
            }
            nextKey = draftLines.size + 10
        }
    }

    val batchBaseKg = batchBaseKgText.toDoubleOrNull()?.coerceIn(1.0, 100_000.0) ?: 1000.0
    val totalInclusionPct = draftLines.sumOf { it.inclusionPctText.toDoubleOrNull() ?: 0.0 }
    val remainingPct = (100.0 - totalInclusionPct)
    val newMaterialsCount = draftLines.count { it.isNewMaterial }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (editingBomTarget != null) "Editing BOM Recipe" else "New Feed Bill of Materials (BOM)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Add existing stocks or create brand-new raw materials straight into this mix table.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (editingBomTarget != null) {
                    TextButton(
                        onClick = {
                            editingBomTarget = null
                            draftLines.clear()
                            onClearPreloadedBom()
                        }
                    ) {
                        Text("New Blank")
                    }
                }
            }

            if (skus.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Quick Select SKU (or type custom feed below)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        skus.take(10).forEach { sku ->
                            val selected = productName.equals(sku.productName, ignoreCase = true)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    productName = sku.productName
                                    selectedSection = PlantSection.fromRaw(sku.section)
                                    val matchingBom = existingBoms.firstOrNull {
                                        it.productName.equals(sku.productName, ignoreCase = true)
                                    }
                                    if (matchingBom != null) {
                                        editingBomTarget = matchingBom
                                        batchBaseKgText = matchingBom.batchBaseKg.toInt().toString()
                                        yieldLossText = matchingBom.yieldLossPct.toString()
                                        bomNotes = matchingBom.notes
                                        draftLines.clear()
                                        matchingBom.lines.forEach { l ->
                                            draftLines.add(
                                                DraftBomLineUi(
                                                    key = nextKey++,
                                                    materialId = l.materialId,
                                                    materialName = l.materialName,
                                                    category = RawMaterialCategory.fromRaw(l.category),
                                                    inclusionPctText = String.format("%.1f", l.inclusionPct),
                                                    notes = l.notes,
                                                    isNewMaterial = false
                                                )
                                            )
                                        }
                                    } else {
                                        editingBomTarget = null
                                    }
                                },
                                label = { Text(sku.productName, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it },
                label = { Text("Finished Feed / Formula Name") },
                placeholder = { Text("e.g. ChookMax 1000 Starter") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bom_product_name_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlantSection.entries.forEach { sec ->
                    FilterChip(
                        selected = selectedSection == sec,
                        onClick = { selectedSection = sec },
                        label = { Text(sec.label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = batchBaseKgText,
                    onValueChange = { batchBaseKgText = it },
                    label = { Text("Batch Base (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = yieldLossText,
                    onValueChange = { yieldLossText = it },
                    label = { Text("Yield Loss (%)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            // Formula Balance Progress Meter
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
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
                            text = "Total BOM Inclusion: ${String.format("%.1f%%", totalInclusionPct)} / 100.0%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = when {
                                abs(remainingPct) <= 0.15 -> StatusHealthyTextDark
                                totalInclusionPct > 100.15 -> StatusCriticalTextDark
                                else -> GrainAmberBright
                            }
                        )
                        if (draftLines.isNotEmpty() && abs(remainingPct) > 0.15) {
                            TextButton(
                                onClick = {
                                    val lastIdx = draftLines.lastIndex
                                    val last = draftLines[lastIdx]
                                    val lastPct = last.inclusionPctText.toDoubleOrNull() ?: 0.0
                                    val balanced = (lastPct + remainingPct).coerceIn(0.1, 100.0)
                                    draftLines[lastIdx] = last.copy(inclusionPctText = String.format("%.1f", balanced))
                                }
                            ) {
                                Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Balance Last Row")
                            }
                        }
                    }
                    LinearProgressIndicator(
                        progress = { (totalInclusionPct / 100.0).toFloat().coerceIn(0f, 1f) },
                        color = when {
                            abs(remainingPct) <= 0.15 -> StatusHealthyTextDark
                            totalInclusionPct > 100.15 -> StatusCriticalTextDark
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                    )
                    Text(
                        text = when {
                            draftLines.isEmpty() -> "Add ingredients below using existing stock or brand-new raw materials."
                            abs(remainingPct) <= 0.15 -> "Formula balanced at 100.0% (${OperationsCalculator.formatKg(batchBaseKg)} kg batch)."
                            remainingPct > 0 -> "${String.format("%.1f%%", remainingPct)} (${OperationsCalculator.formatKg((remainingPct / 100.0) * batchBaseKg)} kg) remaining to reach 100%."
                            else -> "Over 100% by ${String.format("%.1f%%", -remainingPct)} — reduce ingredient % before saving."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action Buttons to Add Rows into BOM
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showPickExistingDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("bom_pick_existing_button")
                ) {
                    Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Existing", maxLines = 1)
                }
                Button(
                    onClick = { showNewMaterialDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestEmerald,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("bom_add_new_material_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ New Raw Mat", maxLines = 1)
                }
            }

            // Structured Bill of Materials Table
            if (draftLines.isNotEmpty()) {
                val totalBatchKg = (totalInclusionPct / 100.0) * batchBaseKg
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
                        Surface(
                            color = ForestEmerald.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LINE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(34.dp)
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
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(74.dp)
                                )
                                Text(
                                    text = "KG/DEL",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(68.dp)
                                )
                            }
                        }

                        draftLines.forEachIndexed { idx, line ->
                            val pct = (line.inclusionPctText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                            val rowKg = (pct / 100.0) * batchBaseKg
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = String.format("%02d", idx + 1),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(26.dp)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val catalogSpec = PlantMaterialCatalog.resolveSpecByName(line.materialName)
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = catalogSpec?.bomCode ?: rawMaterialBomCode(line.category),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        if (catalogSpec != null) {
                                            val (tagBg, tagFg) = movementTagBadgeColors(catalogSpec.movementTag)
                                            Surface(color = tagBg, shape = RoundedCornerShape(4.dp)) {
                                                Text(
                                                    text = catalogSpec.movementTag.shortCode,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = tagFg,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        if (line.isNewMaterial) {
                                            Surface(
                                                color = StatusHealthyContainer,
                                                shape = RoundedCornerShape(999.dp)
                                            ) {
                                                Text(
                                                    text = "NEW",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = OnStatusHealthyContainer,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = line.materialName,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (line.isNewMaterial) {
                                            "${line.category.label} • Init ${OperationsCalculator.formatKg(line.newStockKg)} kg"
                                        } else {
                                            line.category.label
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedTextField(
                                    value = line.inclusionPctText,
                                    onValueChange = { raw ->
                                        draftLines[idx] = line.copy(inclusionPctText = raw)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.width(70.dp)
                                )
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.width(66.dp)
                                ) {
                                    Text(
                                        text = "${OperationsCalculator.formatKg(rowKg)} kg",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    IconButton(
                                        onClick = { draftLines.removeAt(idx) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Remove ${line.materialName}",
                                            tint = StatusCriticalTextDark,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            if (idx < draftLines.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }

                        // BOM Totals Footer Row
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BOM TOTAL (${draftLines.size} line${if (draftLines.size == 1) "" else "s"})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${String.format("%.1f%%", totalInclusionPct)} • ${OperationsCalculator.formatKg(totalBatchKg)} kg",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = when {
                                    abs(remainingPct) <= 0.15 -> StatusHealthyTextDark
                                    totalInclusionPct > 100.15 -> StatusCriticalTextDark
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = bomNotes,
                onValueChange = { bomNotes = it },
                label = { Text("Formula / Mixing Notes (optional)") },
                placeholder = { Text("e.g. Pellet at 82C • Add coco oil post-mixer") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            validationError?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    val cleanName = productName.trim()
                    val loss = yieldLossText.toDoubleOrNull()?.coerceIn(0.0, 50.0) ?: 1.5
                    if (cleanName.isBlank()) {
                        validationError = "Enter a finished feed / formula name."
                        return@Button
                    }
                    if (draftLines.isEmpty()) {
                        validationError = "Add at least one raw material line to the BOM."
                        return@Button
                    }
                    val parsedLines = draftLines.map { row ->
                        val pct = row.inclusionPctText.toDoubleOrNull() ?: 0.0
                        if (pct <= 0.0 || pct > 100.0) {
                            validationError = "${row.materialName}: inclusion % must be between 0.1 and 100."
                            return@Button
                        }
                        BomIngredientLine(
                            materialId = row.materialId,
                            materialName = row.materialName.trim(),
                            category = row.category.label,
                            inclusionPct = pct,
                            notes = row.notes.trim()
                        )
                    }
                    if (totalInclusionPct > 100.5) {
                        validationError = "Total inclusion % exceeds 100.5%. Adjust row percentages first."
                        return@Button
                    }
                    val newMatsToCreate = draftLines
                        .filter { it.isNewMaterial }
                        .distinctBy { it.materialName.trim().lowercase() }
                        .map { row ->
                            val pct = (row.inclusionPctText.toDoubleOrNull() ?: 10.0).coerceIn(0.1, 100.0)
                            RawMaterialEntity(
                                name = row.materialName.trim(),
                                category = row.category.label,
                                currentStockKg = row.newStockKg,
                                reorderThresholdKg = row.newReorderKg,
                                usageSharePct = pct,
                                leadTimeDays = row.newLeadDays,
                                safetyDays = row.newSafetyDays,
                                supplierName = row.newSupplier.trim(),
                                alertsEnabled = row.alertsEnabled
                            )
                        }
                    validationError = null
                    onSaveBomWithNewMaterials(
                        newMatsToCreate,
                        editingBomTarget,
                        cleanName,
                        selectedSection,
                        batchBaseKg,
                        loss,
                        parsedLines,
                        bomNotes.trim()
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestEmerald,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("save_bom_studio_button")
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                val ctaLabel = if (newMaterialsCount > 0) {
                    "Save BOM & Add $newMaterialsCount Material(s)"
                } else {
                    "Save Bill of Materials"
                }
                Text(ctaLabel)
            }
        }
    }

    if (showPickExistingDialog) {
        BomExistingMaterialPickerDialog(
            materials = existingMaterials,
            alreadyAddedNames = draftLines.map { it.materialName.trim().lowercase() }.toSet(),
            onDismiss = { showPickExistingDialog = false },
            onPickMaterial = { mat, pct ->
                draftLines.add(
                    DraftBomLineUi(
                        key = nextKey++,
                        materialId = mat.id,
                        materialName = mat.name,
                        category = RawMaterialCategory.fromRaw(mat.category),
                        inclusionPctText = String.format("%.1f", pct),
                        notes = mat.supplierName,
                        isNewMaterial = false
                    )
                )
                showPickExistingDialog = false
            },
            onPickCatalogSpec = { spec, pct ->
                val existingMatch = existingMaterials.firstOrNull {
                    it.name.equals(spec.name, ignoreCase = true)
                }
                if (existingMatch != null) {
                    draftLines.add(
                        DraftBomLineUi(
                            key = nextKey++,
                            materialId = existingMatch.id,
                            materialName = existingMatch.name,
                            category = RawMaterialCategory.fromRaw(existingMatch.category),
                            inclusionPctText = String.format("%.1f", pct),
                            notes = existingMatch.supplierName,
                            isNewMaterial = false
                        )
                    )
                } else {
                    draftLines.add(
                        DraftBomLineUi(
                            key = nextKey++,
                            materialId = "",
                            materialName = spec.name,
                            category = spec.category,
                            inclusionPctText = String.format("%.1f", pct),
                            notes = spec.movementTag.label,
                            isNewMaterial = true,
                            newStockKg = spec.defaultStockKg,
                            newReorderKg = spec.defaultReorderKg,
                            newLeadDays = spec.defaultLeadDays,
                            newSafetyDays = if (spec.sector == MaterialSector.MACRO) 3L else 5L,
                            newSupplier = spec.defaultSupplier,
                            alertsEnabled = spec.sector != MaterialSector.MICRO_NON_MOVING
                        )
                    )
                }
                showPickExistingDialog = false
            }
        )
    }

    if (showNewMaterialDialog) {
        BomInlineNewMaterialDialog(
            suggestedPct = remainingPct.coerceIn(1.0, 50.0),
            onDismiss = { showNewMaterialDialog = false },
            onConfirm = { name, cat, stockKg, reorderKg, pct, leadDays, supplier, alertsEnabled ->
                draftLines.add(
                    DraftBomLineUi(
                        key = nextKey++,
                        materialId = "",
                        materialName = name,
                        category = cat,
                        inclusionPctText = String.format("%.1f", pct),
                        notes = supplier,
                        isNewMaterial = true,
                        newStockKg = stockKg,
                        newReorderKg = reorderKg,
                        newLeadDays = leadDays,
                        newSafetyDays = 3L,
                        newSupplier = supplier,
                        alertsEnabled = alertsEnabled
                    )
                )
                showNewMaterialDialog = false
            }
        )
    }
}

@Composable
private fun BomExistingMaterialPickerDialog(
    materials: List<RawMaterialEntity>,
    alreadyAddedNames: Set<String>,
    onDismiss: () -> Unit,
    onPickMaterial: (RawMaterialEntity, Double) -> Unit,
    onPickCatalogSpec: (PlantMaterialSpec, Double) -> Unit
) {
    var sourceTab by remember { mutableIntStateOf(0) } // 0 = Inventory, 1 = Macro & Micro Catalog
    var sectorFilter by remember { mutableStateOf<MaterialSector?>(null) }
    var search by remember { mutableStateOf("") }
    var pctText by remember { mutableStateOf("") }

    val existingLower = remember(materials) {
        materials.associateBy { it.name.trim().lowercase() }
    }

    val filteredInventory = remember(materials, search, sectorFilter) {
        materials.filter { mat ->
            val matchesSector = sectorFilter == null || PlantMaterialCatalog.inferSector(mat) == sectorFilter
            val matchesQuery = search.isBlank() ||
                mat.name.contains(search, ignoreCase = true) ||
                mat.category.contains(search, ignoreCase = true)
            matchesSector && matchesQuery
        }
    }

    val filteredCatalog = remember(search, sectorFilter) {
        PlantMaterialCatalog.allSpecs.filter { spec ->
            val matchesSector = sectorFilter == null || spec.sector == sectorFilter
            val matchesQuery = search.isBlank() ||
                spec.name.contains(search, ignoreCase = true) ||
                spec.movementTag.label.contains(search, ignoreCase = true)
            matchesSector && matchesQuery
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick Material for BOM") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = sourceTab == 0,
                        onClick = { sourceTab = 0 },
                        label = { Text("Inventory (${materials.size})") }
                    )
                    FilterChip(
                        selected = sourceTab == 1,
                        onClick = { sourceTab = 1 },
                        label = { Text("Catalog (${PlantMaterialCatalog.allSpecs.size})") }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = sectorFilter == null,
                        onClick = { sectorFilter = null },
                        label = { Text("All") }
                    )
                    MaterialSector.entries.forEach { sec ->
                        FilterChip(
                            selected = sectorFilter == sec,
                            onClick = { sectorFilter = sec },
                            label = { Text(sec.shortLabel) }
                        )
                    }
                }
                OutlinedTextField(
                    value = pctText,
                    onValueChange = { pctText = it },
                    label = { Text("Inclusion % (leave blank for default)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("Filter Macro or Micro ingredients...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (sourceTab == 0) {
                        items(filteredInventory, key = { it.id.ifBlank { it.name } }) { mat ->
                            val isUsed = mat.name.trim().lowercase() in alreadyAddedNames
                            val tag = PlantMaterialCatalog.inferMovementTag(mat)
                            val (tagBg, tagFg) = movementTagBadgeColors(tag)
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val pct = (pctText.toDoubleOrNull() ?: mat.usageSharePct).coerceIn(0.1, 100.0)
                                        onPickMaterial(mat, pct)
                                    }
                                    .minimumInteractiveComponentSize()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Surface(color = tagBg, shape = RoundedCornerShape(4.dp)) {
                                            Text(
                                                text = tag.label,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = tagFg,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                        Text(
                                            text = mat.name,
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${mat.category} • ${OperationsCalculator.formatKg(mat.currentStockKg)} kg on hand",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isUsed) {
                                        Text(
                                            text = "In BOM",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GrainAmberBright
                                        )
                                    } else {
                                        Icon(Icons.Filled.Add, contentDescription = "Add ${mat.name}", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredCatalog, key = { it.idHint }) { spec ->
                            val isUsed = spec.name.trim().lowercase() in alreadyAddedNames
                            val existingMatch = existingLower[spec.name.trim().lowercase()]
                            val (tagBg, tagFg) = movementTagBadgeColors(spec.movementTag)
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val pct = (pctText.toDoubleOrNull() ?: spec.defaultInclusionPct).coerceIn(0.1, 100.0)
                                        onPickCatalogSpec(spec, pct)
                                    }
                                    .minimumInteractiveComponentSize()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Surface(color = tagBg, shape = RoundedCornerShape(4.dp)) {
                                                Text(
                                                    text = spec.movementTag.label,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = tagFg,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                            if (existingMatch != null) {
                                                Text(
                                                    text = "In Stock",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = StatusHealthyTextDark
                                                )
                                            }
                                        }
                                        Text(
                                            text = spec.name,
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${spec.bomCode} • Default ${String.format("%.1f%%", spec.defaultInclusionPct)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isUsed) {
                                        Text(
                                            text = "In BOM",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GrainAmberBright
                                        )
                                    } else {
                                        Icon(Icons.Filled.Add, contentDescription = "Add ${spec.name}", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun BomInlineNewMaterialDialog(
    suggestedPct: Double,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        category: RawMaterialCategory,
        stockKg: Double,
        reorderKg: Double,
        inclusionPct: Double,
        leadDays: Long,
        supplier: String,
        alertsEnabled: Boolean
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(RawMaterialCategory.ADDITIVE) }
    var stockText by remember { mutableStateOf("2500") }
    var reorderText by remember { mutableStateOf("500") }
    var pctText by remember { mutableStateOf(String.format("%.1f", suggestedPct)) }
    var leadText by remember { mutableStateOf("4") }
    var supplier by remember { mutableStateOf("") }
    var alertsEnabled by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Material + BOM Line") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ingredient Name") },
                    placeholder = { Text("e.g. Copra Meal / L-Lysine") },
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
                        label = { Text("Initial Stock (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reorderText,
                        onValueChange = { reorderText = it },
                        label = { Text("Min Alert (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = leadText,
                        onValueChange = { leadText = it },
                        label = { Text("Lead (days)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = supplier,
                    onValueChange = { supplier = it },
                    label = { Text("Supplier (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Low-stock alert", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = alertsEnabled, onCheckedChange = { alertsEnabled = it })
                }
                errorMsg?.let {
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
                    val reorder = reorderText.toDoubleOrNull() ?: 500.0
                    val lead = leadText.toLongOrNull() ?: 4L
                    if (clean.isBlank()) {
                        errorMsg = "Enter an ingredient name."
                        return@Button
                    }
                    if (pct <= 0.0 || pct > 100.0) {
                        errorMsg = "BOM inclusion % must be 0.1–100%."
                        return@Button
                    }
                    if (stock == null || stock < 0.0) {
                        errorMsg = "Enter valid initial stock kg."
                        return@Button
                    }
                    onConfirm(clean, category, stock, reorder.coerceAtLeast(0.0), pct, lead.coerceIn(1L, 60L), supplier.trim(), alertsEnabled)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForestEmerald, contentColor = Color.White)
            ) {
                Text("Add to BOM")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
