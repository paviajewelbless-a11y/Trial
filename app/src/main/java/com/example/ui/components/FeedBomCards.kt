package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.OperationsCalculator
import com.example.model.BomIngredientLine
import com.example.model.FeedBomEntity
import com.example.model.MaterialSector
import com.example.model.MicroMovementTag
import com.example.model.PlantMaterialCatalog
import com.example.model.RawMaterialCategory
import com.example.model.RawMaterialEntity
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.GrainAmberBright
import com.example.ui.theme.OnStatusCriticalContainer
import com.example.ui.theme.OnStatusHealthyContainer
import com.example.ui.theme.OnStatusInProgressContainer
import com.example.ui.theme.OnStatusWatchContainer
import com.example.ui.theme.SlateTeal
import com.example.ui.theme.StatusCriticalContainer
import com.example.ui.theme.StatusCriticalTextDark
import com.example.ui.theme.StatusHealthy
import com.example.ui.theme.StatusHealthyContainer
import com.example.ui.theme.StatusHealthyTextDark
import com.example.ui.theme.StatusInProgressContainer
import com.example.ui.theme.StatusWatchContainer
import com.example.ui.theme.StatusWatchTextDark
import kotlin.math.abs
import kotlin.math.floor

enum class FeedTypeCategory(
    val label: String,
    val shortLabel: String
) {
    SWINE("Swine & Hog", "Swine"),
    POULTRY("Poultry & Broiler", "Poultry"),
    DUCK_PIGEON("Duck & Pigeon", "Duck / Pigeon"),
    QUAIL("Quail & Layer", "Quail"),
    RUMINANT_SPECIALTY("Ruminant & Specialty", "Specialty")
}

fun classifyFeedType(bom: FeedBomEntity): FeedTypeCategory {
    val haystack = "${bom.productName} ${bom.notes} ${bom.section}".lowercase()
    return when {
        "quail" in haystack || "la union quail" in bom.section.lowercase() && "duck" !in haystack ->
            FeedTypeCategory.QUAIL
        "duck" in haystack || "pigeon" in haystack || "flash" in haystack || "waterfowl" in haystack ->
            FeedTypeCategory.DUCK_PIGEON
        "goat" in haystack || "ruminant" in haystack || "dog" in haystack || "balanok" in haystack || "corn grits" in haystack || "crack corn" in haystack ->
            FeedTypeCategory.RUMINANT_SPECIALTY
        "chookmax" in haystack || "gigamax" in haystack || "broiler" in haystack ||
            ("barako" in haystack && "duck" !in haystack && "pigeon" !in haystack) ||
            "red pellet" in haystack ->
            FeedTypeCategory.POULTRY
        "broodsow" in haystack || "sow" in haystack || "hog" in haystack ||
            "lactation" in haystack || "pre starter" in haystack || "hpdp" in haystack ||
            "pdp" in haystack || "mb bio" in haystack || "swine" in haystack ->
            FeedTypeCategory.SWINE
        else -> FeedTypeCategory.POULTRY
    }
}

fun inferFeedFormLabel(productName: String): String {
    val lower = productName.lowercase()
    return when {
        "crumble" in lower -> "Crumble"
        "pellet" in lower || "3mm" in lower || "4mm" in lower -> "Pellet"
        "mash" in lower -> "Mash"
        "pre starter" in lower || "starter" in lower || "1000" in lower -> "Starter"
        "grower" in lower || "2000" in lower -> "Grower"
        "3000" in lower || "4000" in lower -> "Finisher"
        else -> "Formula"
    }
}

data class ResolvedBomLineDetail(
    val line: BomIngredientLine,
    val matchedMaterial: RawMaterialEntity?,
    val sector: MaterialSector,
    val movementTag: MicroMovementTag,
    val bomCode: String,
    val batchKg: Double,
    val maxBatches: Int?,
    val isMacro: Boolean
)

fun resolveBomLineDetail(
    line: BomIngredientLine,
    targetBatchKg: Double,
    materialById: Map<String, RawMaterialEntity>,
    materialByName: Map<String, RawMaterialEntity>
): ResolvedBomLineDetail {
    val matchedMat = materialById[line.materialId]
        ?: materialByName[line.materialName.trim().lowercase()]
    val spec = PlantMaterialCatalog.resolveSpecByName(line.materialName)
    val noteLower = line.notes.lowercase()
    val sector = when {
        spec != null -> spec.sector
        matchedMat != null -> PlantMaterialCatalog.inferSector(matchedMat)
        "micro fast" in noteLower || "fast moving" in noteLower -> MaterialSector.MICRO_FAST
        "micro slow" in noteLower || "slow moving" in noteLower -> MaterialSector.MICRO_SLOW
        "trial" in noteLower || "new micromix" in noteLower || "non moving" in noteLower || "non-moving" in noteLower ->
            MaterialSector.MICRO_NON_MOVING
        "macro" in noteLower -> MaterialSector.MACRO
        else -> when (RawMaterialCategory.fromRaw(line.category)) {
            RawMaterialCategory.ENERGY,
            RawMaterialCategory.PROTEIN,
            RawMaterialCategory.LIQUID -> MaterialSector.MACRO
            RawMaterialCategory.MINERAL,
            RawMaterialCategory.ADDITIVE -> MaterialSector.MICRO_SLOW
        }
    }
    val movementTag = when {
        spec != null -> spec.movementTag
        matchedMat != null -> PlantMaterialCatalog.inferMovementTag(matchedMat)
        "fast" in noteLower -> MicroMovementTag.FAST_MOVING
        "trial" in noteLower -> MicroMovementTag.TRIAL
        "new micromix" in noteLower -> MicroMovementTag.NEW_MICROMIX
        "non moving" in noteLower || "non-moving" in noteLower -> MicroMovementTag.NON_MOVING
        sector == MaterialSector.MACRO -> MicroMovementTag.MACRO
        sector == MaterialSector.MICRO_FAST -> MicroMovementTag.FAST_MOVING
        sector == MaterialSector.MICRO_NON_MOVING -> MicroMovementTag.NON_MOVING
        else -> MicroMovementTag.SLOW_MOVING
    }
    val bomCode = spec?.bomCode ?: when (sector) {
        MaterialSector.MACRO -> when (RawMaterialCategory.fromRaw(line.category)) {
            RawMaterialCategory.PROTEIN -> "MAC-PRO"
            RawMaterialCategory.LIQUID -> "MAC-LIQ"
            else -> "MAC-ENG"
        }
        MaterialSector.MICRO_FAST -> "MIC-FST"
        MaterialSector.MICRO_SLOW -> "MIC-SLW"
        MaterialSector.MICRO_NON_MOVING -> when (movementTag) {
            MicroMovementTag.TRIAL -> "MIC-TRL"
            MicroMovementTag.NEW_MICROMIX -> "MIC-NEW"
            else -> "MIC-NON"
        }
    }
    val batchKg = (line.inclusionPct / 100.0) * targetBatchKg
    val maxBatches = if (matchedMat != null && batchKg > 0.0001) {
        floor(matchedMat.currentStockKg / batchKg).toInt().coerceAtLeast(0)
    } else {
        null
    }
    return ResolvedBomLineDetail(
        line = line,
        matchedMaterial = matchedMat,
        sector = sector,
        movementTag = movementTag,
        bomCode = bomCode,
        batchKg = batchKg,
        maxBatches = maxBatches,
        isMacro = sector == MaterialSector.MACRO
    )
}

private fun formatBatchWeight(weightKg: Double): String = when {
    weightKg < 1.0 -> "${OperationsCalculator.formatKg(weightKg * 1000.0)} g"
    else -> "${OperationsCalculator.formatKg(weightKg)} kg"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedTypeBomBreakdownCard(
    bom: FeedBomEntity,
    materials: List<RawMaterialEntity>,
    batchWeightOverrideKg: Double?,
    searchQuery: String = "",
    onEditBom: (FeedBomEntity) -> Unit,
    onCloneBom: (FeedBomEntity) -> Unit,
    onDeleteBom: (FeedBomEntity) -> Unit,
    onQuickRestockMaterial: (RawMaterialEntity) -> Unit,
    onEditMaterial: (RawMaterialEntity) -> Unit = {},
    onDeleteMaterial: (RawMaterialEntity) -> Unit = {},
    onCreateUnlinkedMaterial: (BomIngredientLine) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var materialPendingDelete by remember { mutableStateOf<RawMaterialEntity?>(null) }

    val targetBatchKg = (batchWeightOverrideKg ?: bom.batchBaseKg).coerceAtLeast(1.0)
    val feedType = remember(bom.productName, bom.notes, bom.section) { classifyFeedType(bom) }
    val formLabel = remember(bom.productName) { inferFeedFormLabel(bom.productName) }

    val materialById = remember(materials) { materials.associateBy { it.id } }
    val materialByName = remember(materials) { materials.associateBy { it.name.trim().lowercase() } }

    val resolvedDetails = remember(bom, materials, targetBatchKg) {
        bom.lines.map { line ->
            resolveBomLineDetail(line, targetBatchKg, materialById, materialByName)
        }
    }

    val macroItems = remember(resolvedDetails) { resolvedDetails.filter { it.isMacro } }
    val microItems = remember(resolvedDetails) {
        resolvedDetails.filter { !it.isMacro }.sortedBy { it.sector.ordinal }
    }

    val totalPct = bom.totalInclusionPct()
    val isBalanced = abs(totalPct - 100.0) <= 0.15
    val macroPct = macroItems.sumOf { it.line.inclusionPct }
    val microPct = microItems.sumOf { it.line.inclusionPct }
    val macroBatchKg = macroItems.sumOf { it.batchKg }
    val microBatchKg = microItems.sumOf { it.batchKg }

    val bottleneckBatches = resolvedDetails.mapNotNull { it.maxBatches }.minOrNull()
    val shortCount = resolvedDetails.count { detail ->
        detail.matchedMaterial != null && detail.matchedMaterial.currentStockKg + 0.01 < detail.batchKg
    }

    val fastMicroCount = microItems.count { it.sector == MaterialSector.MICRO_FAST }
    val slowMicroCount = microItems.count { it.sector == MaterialSector.MICRO_SLOW }
    val nonMovingMicroCount = microItems.count { it.sector == MaterialSector.MICRO_NON_MOVING }

    val safeTag = remember(bom.id, bom.productName) {
        bom.id.ifBlank { bom.productName.lowercase().replace(Regex("[^a-z0-9]+"), "_") }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .testTag("feed_bom_card_$safeTag")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Feed Type & Formulation Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = feedType.shortLabel.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            color = StatusInProgressContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = formLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = OnStatusInProgressContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = bom.section,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = bom.productName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "Scaled Batch: ${OperationsCalculator.formatKg(targetBatchKg)} kg • Yield Loss: ${String.format("%.1f%%", bom.yieldLossPct)} • ${resolvedDetails.size} ingredients",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = if (isBalanced) StatusHealthyContainer else StatusWatchContainer,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = if (isBalanced) "100% Mix" else "${String.format("%.1f%%", totalPct)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isBalanced) OnStatusHealthyContainer else OnStatusWatchContainer,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                    if (bottleneckBatches != null) {
                        val (runBg, runFg) = when {
                            bottleneckBatches <= 0 -> StatusCriticalContainer to OnStatusCriticalContainer
                            bottleneckBatches <= 2 -> StatusWatchContainer to OnStatusWatchContainer
                            else -> StatusHealthyContainer to OnStatusHealthyContainer
                        }
                        Surface(
                            color = runBg,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = "$bottleneckBatches batch ready",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = runFg,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            if (bom.notes.isNotBlank()) {
                Text(
                    text = bom.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 2. Visual Macro vs Micro Inclusion Split Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
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
                            text = "Macro ${String.format("%.1f%%", macroPct)} (${OperationsCalculator.formatKg(macroBatchKg)} kg)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ForestEmerald
                        )
                        Text(
                            text = "Micro ${String.format("%.1f%%", microPct)} (${formatBatchWeight(microBatchKg)})",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = GrainAmberBright
                        )
                    }
                    val totalSpan = (macroPct + microPct).coerceAtLeast(1.0)
                    val macroFraction = (macroPct / totalSpan).toFloat().coerceIn(0.05f, 0.98f)
                    val microFraction = (1f - macroFraction).coerceAtLeast(0.02f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        if (macroItems.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(macroFraction)
                                    .fillMaxHeight()
                                    .background(ForestEmerald)
                            )
                        }
                        if (microItems.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(microFraction)
                                    .fillMaxHeight()
                                    .background(GrainAmberBright)
                            )
                        }
                    }
                }
            }

            if (shortCount > 0) {
                Surface(
                    color = StatusCriticalContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = OnStatusCriticalContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "$shortCount ingredient${if (shortCount == 1) "" else "s"} short for a ${OperationsCalculator.formatKg(targetBatchKg)} kg batch. Tap a stock pill below to restock.",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = OnStatusCriticalContainer
                        )
                    }
                }
            }

            // 3. CATEGORIZED CARD A: MACRO INGREDIENTS
            IngredientCategorySubCard(
                title = "Macro Ingredients",
                subtitle = "${macroItems.size} bulk item${if (macroItems.size == 1) "" else "s"}",
                summaryBadge = "${String.format("%.1f%%", macroPct)} • ${OperationsCalculator.formatKg(macroBatchKg)} kg",
                accentColor = ForestEmerald,
                icon = Icons.Filled.Grain,
                sectorBreakdownText = null,
                items = macroItems,
                searchQuery = searchQuery,
                emptyText = "No macro ingredients assigned to this formula.",
                onQuickRestockMaterial = onQuickRestockMaterial,
                onEditMaterial = onEditMaterial,
                onRequestDeleteMaterial = { mat -> materialPendingDelete = mat },
                onCreateUnlinkedMaterial = onCreateUnlinkedMaterial,
                testTagPrefix = "macro_subcard_$safeTag"
            )

            // 4. CATEGORIZED CARD B: MICRO INGREDIENTS
            val sectorSummary = buildList {
                if (fastMicroCount > 0) add("Fast: $fastMicroCount")
                if (slowMicroCount > 0) add("Slow: $slowMicroCount")
                if (nonMovingMicroCount > 0) add("Trial/Non-Mov: $nonMovingMicroCount")
            }.joinToString(" • ").ifBlank { null }

            IngredientCategorySubCard(
                title = "Micro Ingredients",
                subtitle = "${microItems.size} premix / additive${if (microItems.size == 1) "" else "s"}",
                summaryBadge = "${String.format("%.1f%%", microPct)} • ${formatBatchWeight(microBatchKg)}",
                accentColor = SlateTeal,
                icon = Icons.Filled.Science,
                sectorBreakdownText = sectorSummary,
                items = microItems,
                searchQuery = searchQuery,
                emptyText = "No micro ingredients assigned to this formula.",
                onQuickRestockMaterial = onQuickRestockMaterial,
                onEditMaterial = onEditMaterial,
                onRequestDeleteMaterial = { mat -> materialPendingDelete = mat },
                onCreateUnlinkedMaterial = onCreateUnlinkedMaterial,
                testTagPrefix = "micro_subcard_$safeTag"
            )

            // 5. Card Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onEditBom(bom) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("feed_bom_edit_$safeTag")
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit BOM", maxLines = 1)
                }
                OutlinedButton(
                    onClick = { onCloneBom(bom) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("feed_bom_clone_$safeTag")
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clone", maxLines = 1)
                }
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCriticalTextDark),
                    border = BorderStroke(1.dp, StatusCriticalTextDark.copy(alpha = 0.55f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("feed_bom_delete_$safeTag")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete ${bom.productName} BOM",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete Feed BOM?") },
            text = {
                Text("Remove the Bill of Materials for ${bom.productName}? Raw material stock records will stay in inventory.")
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
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }

    materialPendingDelete?.let { targetMat ->
        AlertDialog(
            onDismissRequest = { materialPendingDelete = null },
            title = { Text("Delete Raw Material?") },
            text = {
                Text("Remove ${targetMat.name} from raw material inventory? Feed BOM formulas will remain intact and show this line as Unlinked until re-added.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMaterial(targetMat)
                        materialPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete Material")
                }
            },
            dismissButton = {
                TextButton(onClick = { materialPendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IngredientCategorySubCard(
    title: String,
    subtitle: String,
    summaryBadge: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    sectorBreakdownText: String?,
    items: List<ResolvedBomLineDetail>,
    searchQuery: String,
    emptyText: String,
    onQuickRestockMaterial: (RawMaterialEntity) -> Unit,
    onEditMaterial: (RawMaterialEntity) -> Unit,
    onRequestDeleteMaterial: (RawMaterialEntity) -> Unit,
    onCreateUnlinkedMaterial: (BomIngredientLine) -> Unit,
    testTagPrefix: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTagPrefix)
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(accentColor.copy(alpha = 0.16f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (sectorBreakdownText != null) "$subtitle • $sectorBreakdownText" else subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = accentColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = summaryBadge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (items.isEmpty()) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    val q = searchQuery.trim()
                    items.forEachIndexed { idx, item ->
                        val (tagBg, tagFg) = movementTagBadgeColors(item.movementTag)
                        val mat = item.matchedMaterial
                        val isMatch = q.isNotBlank() && (
                            item.line.materialName.contains(q, ignoreCase = true) ||
                                item.movementTag.label.contains(q, ignoreCase = true) ||
                                item.bomCode.contains(q, ignoreCase = true)
                            )
                        val isShort = mat != null && mat.currentStockKg + 0.01 < item.batchKg
                        val stockBg = when {
                            mat == null -> MaterialTheme.colorScheme.surfaceVariant
                            isShort -> StatusCriticalContainer
                            (item.maxBatches ?: 99) <= 4 -> StatusWatchContainer
                            else -> StatusHealthyContainer
                        }
                        val stockFg = when {
                            mat == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            isShort -> OnStatusCriticalContainer
                            (item.maxBatches ?: 99) <= 4 -> OnStatusWatchContainer
                            else -> OnStatusHealthyContainer
                        }

                        Surface(
                            color = if (isMatch) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            border = if (isMatch) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = if (isMatch) 6.dp else 0.dp, vertical = if (isMatch) 4.dp else 0.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Surface(
                                            color = tagBg,
                                            shape = RoundedCornerShape(5.dp)
                                        ) {
                                            Text(
                                                text = if (item.isMacro) item.bomCode else item.movementTag.label,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = tagFg,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                        if (!item.isMacro) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surface,
                                                shape = RoundedCornerShape(5.dp)
                                            ) {
                                                Text(
                                                    text = item.bomCode,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = item.line.materialName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.line.notes.isNotBlank()) {
                                        Text(
                                            text = item.line.notes,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${String.format("%.1f%%", item.line.inclusionPct)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = formatBatchWeight(item.batchKg),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Surface(
                                            color = stockBg,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.clickable {
                                                if (mat != null) {
                                                    onQuickRestockMaterial(mat)
                                                } else {
                                                    onCreateUnlinkedMaterial(item.line)
                                                }
                                            }
                                        ) {
                                            val stockLabel = when {
                                                mat == null -> "+ Link Stock"
                                                isShort -> "Short (${OperationsCalculator.formatKg(mat.currentStockKg)} kg)"
                                                else -> "${OperationsCalculator.formatKg(mat.currentStockKg)} kg"
                                            }
                                            Text(
                                                text = stockLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = stockFg,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        if (mat != null) {
                                            androidx.compose.material3.IconButton(
                                                onClick = { onEditMaterial(mat) },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("bom_edit_material_${mat.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Edit,
                                                    contentDescription = "Edit ${mat.name}",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            androidx.compose.material3.IconButton(
                                                onClick = { onRequestDeleteMaterial(mat) },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("bom_delete_material_${mat.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "Delete ${mat.name}",
                                                    tint = StatusCriticalTextDark,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (idx < items.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}
