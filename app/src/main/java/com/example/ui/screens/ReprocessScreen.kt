package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import com.example.model.DispatchRecord
import com.example.model.PlantSection
import com.example.model.ReprocessItem
import com.example.model.ReprocessStatus
import com.example.ui.components.AddEditReprocessDialog
import com.example.ui.components.FlagReprocessToInventoryDialog
import com.example.ui.components.IndustrialKpiCard
import com.example.ui.components.LocalIsDesktopLayout
import com.example.ui.components.LocalWindowWidthDp
import com.example.ui.components.MarkReprocessDamagedDialog
import com.example.ui.components.ReprocessItemCard
import com.example.ui.components.SwipeLeftActionsContainer
import com.example.ui.components.desktopGridColumns
import com.example.ui.components.desktopGridItems
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.GrainAmber
import com.example.ui.theme.GrainAmberBright
import com.example.ui.theme.OnStatusCriticalContainer
import com.example.ui.theme.OnStatusWatchContainer
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusCriticalContainer
import com.example.ui.theme.StatusCriticalTextDark
import com.example.ui.theme.StatusHealthyTextDark
import com.example.ui.theme.StatusWatchContainer
import com.example.viewmodel.OpsUiState

private enum class ReprocessFilter(val label: String) {
    ALL("All"),
    FAST_ACTION("Fast Action Needed"),
    PENDING("Pending Reprocess"),
    REPROCESSING("Reprocessing"),
    FLAGGED("Moved to Stock"),
    DAMAGED("Damaged")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReprocessScreen(
    state: OpsUiState.Ready,
    onAddReprocessItem: (
        productName: String,
        section: PlantSection,
        returnedDate: String,
        returnedBags: Long,
        bagWeightKg: Double,
        returnedWeightKg: Double,
        returnReason: String,
        customer: String,
        drReference: String,
        sourceDispatchId: String,
        fastActionNeeded: Boolean,
        actionNotes: String
    ) -> Unit,
    onUpdateReprocessItem: (
        existing: ReprocessItem,
        productName: String,
        section: PlantSection,
        returnedDate: String,
        returnedBags: Long,
        bagWeightKg: Double,
        returnedWeightKg: Double,
        returnReason: String,
        customer: String,
        drReference: String,
        fastActionNeeded: Boolean,
        actionNotes: String
    ) -> Unit,
    onQueueReturnedDispatch: (DispatchRecord) -> Unit,
    onAdvanceStatus: (ReprocessItem, ReprocessStatus) -> Unit,
    onToggleFastAction: (ReprocessItem) -> Unit,
    onFlagToInventory: (
        item: ReprocessItem,
        reprocessedBags: Long,
        reprocessedWeightKg: Double,
        reprocessedDate: String,
        actionNotes: String
    ) -> Unit,
    onMarkDamaged: (
        item: ReprocessItem,
        damagedDate: String,
        damageNotes: String
    ) -> Unit = { _, _, _ -> },
    onDeleteReprocessItem: (ReprocessItem) -> Unit,
    onOpenFinishedGoodsTab: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf(ReprocessFilter.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ReprocessItem?>(null) }
    var flaggingItem by remember { mutableStateOf<ReprocessItem?>(null) }
    var damagingItem by remember { mutableStateOf<ReprocessItem?>(null) }
    var pendingDeleteItem by remember { mutableStateOf<ReprocessItem?>(null) }

    val linkedDispatchIds = remember(state.reprocessItems) {
        state.reprocessItems.map { it.sourceDispatchId }.filter { it.isNotBlank() }.toSet()
    }
    val unqueuedReturnedDispatches = remember(state.dispatches, linkedDispatchIds) {
        state.dispatches.filter { d ->
            d.returnConfirmed && d.returnedBags > 0L && d.id.isNotBlank() && d.id !in linkedDispatchIds
        }.sortedByDescending { it.date }
    }

    val fastActionItems = remember(state.reprocessItems) {
        state.reprocessItems.filter { it.isPending() && it.fastActionNeeded }
    }
    val pendingItems = remember(state.reprocessItems) {
        state.reprocessItems.filter { it.isPending() && it.status == ReprocessStatus.PENDING_REPROCESS.label }
    }
    val reprocessingItems = remember(state.reprocessItems) {
        state.reprocessItems.filter { it.isPending() && it.status == ReprocessStatus.REPROCESSING.label }
    }
    val flaggedItems = remember(state.reprocessItems) {
        state.reprocessItems.filter { it.flaggedToInventory }
    }
    val damagedItems = remember(state.reprocessItems) {
        state.reprocessItems.filter { it.isDamaged() }
    }

    val totalPendingKg = remember(pendingItems, reprocessingItems) {
        (pendingItems + reprocessingItems).sumOf { it.returnedWeightKg }
    }
    val totalReprocessedKg = remember(flaggedItems) {
        flaggedItems.sumOf { it.effectiveReprocessedWeightKg() }
    }
    val totalDamagedKg = remember(damagedItems) {
        damagedItems.sumOf { it.returnedWeightKg }
    }
    val totalFlaggedReturnedKg = remember(flaggedItems) {
        flaggedItems.sumOf { it.returnedWeightKg }
    }
    val recoveryYieldPct = if (totalFlaggedReturnedKg > 0.0) {
        (totalReprocessedKg / totalFlaggedReturnedKg) * 100.0
    } else 100.0

    val filteredItems = remember(state.reprocessItems, searchQuery, filterMode) {
        val q = searchQuery.trim()
        state.reprocessItems.filter { item ->
            val matchesFilter = when (filterMode) {
                ReprocessFilter.ALL -> true
                ReprocessFilter.FAST_ACTION -> item.isPending() && item.fastActionNeeded
                ReprocessFilter.PENDING -> item.isPending() && item.status == ReprocessStatus.PENDING_REPROCESS.label
                ReprocessFilter.REPROCESSING -> item.isPending() && item.status == ReprocessStatus.REPROCESSING.label
                ReprocessFilter.FLAGGED -> item.flaggedToInventory
                ReprocessFilter.DAMAGED -> item.isDamaged()
            }
            val matchesQuery = q.isBlank() ||
                item.productName.contains(q, ignoreCase = true) ||
                item.section.contains(q, ignoreCase = true) ||
                item.customer.contains(q, ignoreCase = true) ||
                item.drReference.contains(q, ignoreCase = true) ||
                item.returnReason.contains(q, ignoreCase = true) ||
                item.actionNotes.contains(q, ignoreCase = true)
            matchesFilter && matchesQuery
        }.sortedWith(
            compareBy<ReprocessItem> {
                when {
                    it.isPending() -> 0
                    it.flaggedToInventory -> 1
                    else -> 2
                }
            }
                .thenByDescending { it.fastActionNeeded }
                .thenByDescending { it.returnedDate }
        )
    }

    val isDesktop = LocalIsDesktopLayout.current
    val windowWidthDp = LocalWindowWidthDp.current
    val gridColumns = desktopGridColumns(windowWidthDp, maxCols = 2)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reprocess_screen"),
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
                        text = "Reprocess Management",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Review feed items flagged as Reprocess, move recovered stock back, or mark damaged",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestEmerald,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("reprocess_add_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Return", maxLines = 1)
                }
            }
        }

        // Urgent Fast Action Alert Banner
        if (fastActionItems.isNotEmpty()) {
            item {
                val fastActionKg = fastActionItems.sumOf { it.returnedWeightKg }
                val fastActionBags = fastActionItems.sumOf { it.returnedBags }
                Surface(
                    color = StatusCriticalContainer,
                    border = BorderStroke(1.5.dp, StatusCritical),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reprocess_fast_action_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = "Fast action needed",
                            tint = OnStatusCriticalContainer,
                            modifier = Modifier.size(26.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "FAST ACTION NEEDED: ${fastActionItems.size} Returned Lot(s)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = OnStatusCriticalContainer
                            )
                            Text(
                                text = "$fastActionBags bags (${OperationsCalculator.formatKg(fastActionKg)} kg) awaiting immediate QA: move back to stock or mark damaged.",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnStatusCriticalContainer.copy(alpha = 0.9f)
                            )
                        }
                        if (filterMode != ReprocessFilter.FAST_ACTION) {
                            OutlinedButton(
                                onClick = { filterMode = ReprocessFilter.FAST_ACTION },
                                border = BorderStroke(1.dp, OnStatusCriticalContainer),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("reprocess_banner_filter_fast")
                            ) {
                                Text("Focus", color = OnStatusCriticalContainer, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // KPI Strip
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
                    title = "FAST ACTION QUEUE",
                    primaryValue = "${fastActionItems.size} urgent",
                    secondaryValue = "${pendingItems.size + reprocessingItems.size} open (${OperationsCalculator.formatKg(totalPendingKg)} kg)",
                    icon = Icons.Filled.PriorityHigh,
                    accentColor = if (fastActionItems.isNotEmpty()) StatusCriticalTextDark else StatusHealthyTextDark,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(195.dp)
                )
                IndustrialKpiCard(
                    title = "IN REPROCESSING",
                    primaryValue = "${reprocessingItems.size} batches",
                    secondaryValue = "${reprocessingItems.sumOf { it.returnedBags }} bags on floor",
                    icon = Icons.Filled.Autorenew,
                    accentColor = GrainAmberBright,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(175.dp)
                )
                IndustrialKpiCard(
                    title = "MOVED TO STOCK",
                    primaryValue = "+${OperationsCalculator.formatKg(totalReprocessedKg)} kg",
                    secondaryValue = "${flaggedItems.size} lots (${String.format("%.1f%%", recoveryYieldPct)} yield)",
                    icon = Icons.Filled.CheckCircle,
                    accentColor = StatusHealthyTextDark,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(195.dp)
                )
                IndustrialKpiCard(
                    title = "DAMAGED WRITE-OFF",
                    primaryValue = "${OperationsCalculator.formatKg(totalDamagedKg)} kg",
                    secondaryValue = "${damagedItems.size} lot(s) unfit for stock",
                    icon = Icons.Filled.DeleteForever,
                    accentColor = StatusCriticalTextDark,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(185.dp)
                )
            }
        }

        // Quick-queue strip for unqueued returned dispatches
        if (unqueuedReturnedDispatches.isNotEmpty()) {
            item {
                Surface(
                    color = StatusWatchContainer.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, GrainAmber),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Unqueued Dispatch Returns (${unqueuedReturnedDispatches.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = OnStatusWatchContainer
                        )
                        unqueuedReturnedDispatches.take(3).forEach { disp ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "DR ${disp.drReference.ifBlank { "—" }} • ${disp.productName}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = OnStatusWatchContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${disp.customer} • ${disp.returnedBags} bags (${OperationsCalculator.formatKg(disp.returnedWeightKg)} kg) • ${disp.returnReason}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OnStatusWatchContainer.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onQueueReturnedDispatch(disp) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GrainAmber,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("reprocess_quick_queue_${disp.id}")
                                ) {
                                    Text("Queue Fast Action", maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Search & Filter Controls
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
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search reprocess") },
                        trailingIcon = if (searchQuery.isNotBlank()) {
                            {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                }
                            }
                        } else null,
                        placeholder = { Text("Search product, DR#, customer, reason...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reprocess_search_input")
                    )
                    OutlinedButton(
                        onClick = onOpenFinishedGoodsTab,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("reprocess_open_fg_button")
                    ) {
                        Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Inventory", maxLines = 1)
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val counts = mapOf(
                        ReprocessFilter.ALL to state.reprocessItems.size,
                        ReprocessFilter.FAST_ACTION to fastActionItems.size,
                        ReprocessFilter.PENDING to pendingItems.size,
                        ReprocessFilter.REPROCESSING to reprocessingItems.size,
                        ReprocessFilter.FLAGGED to flaggedItems.size,
                        ReprocessFilter.DAMAGED to damagedItems.size
                    )
                    ReprocessFilter.entries.forEach { mode ->
                        FilterChip(
                            selected = filterMode == mode,
                            onClick = { filterMode = mode },
                            label = { Text("${mode.label} (${counts[mode] ?: 0})") },
                            modifier = Modifier.testTag("reprocess_filter_${mode.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (filteredItems.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (state.reprocessItems.isEmpty()) {
                                "No feed items flagged as Reprocess in the database."
                            } else {
                                "No reprocess items match your current filter or search."
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Confirm a product return in Dispatched or tap + Add Return above to manage returned feed bags — move recovered stock back to Finished Goods or mark damaged lots as write-offs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            desktopGridItems(
                itemsList = filteredItems,
                columns = gridColumns,
                keyPrefix = "reprocess",
                itemKey = { it.id.ifBlank { "${it.productName}_${it.returnedDate}_${it.drReference}" } }
            ) { item ->
                SwipeLeftActionsContainer(
                    itemKey = item.id.ifBlank { "${item.productName}_${item.returnedDate}" },
                    editTestTag = "reprocess_swipe_edit_${item.id}",
                    deleteTestTag = "reprocess_swipe_delete_${item.id}",
                    onEdit = { editingItem = item },
                    onDelete = { pendingDeleteItem = item }
                ) {
                    ReprocessItemCard(
                        item = item,
                        onStartReprocess = { onAdvanceStatus(item, ReprocessStatus.REPROCESSING) },
                        onFastFlagInventory = {
                            val bags = if (item.reprocessedBags > 0L) item.reprocessedBags else item.returnedBags
                            val kg = if (item.reprocessedWeightKg > 0.0) item.reprocessedWeightKg else item.returnedWeightKg
                            onFlagToInventory(item, bags, kg, OperationsCalculator.todayIsoDate(), item.actionNotes)
                        },
                        onOpenFlagDialog = { flaggingItem = item },
                        onOpenDamagedDialog = { damagingItem = item },
                        onToggleFastAction = { onToggleFastAction(item) },
                        onEdit = { editingItem = item },
                        onOpenInventory = onOpenFinishedGoodsTab
                    )
                }
            }
        }
    }

    if (showAddDialog || editingItem != null) {
        val currentEdit = editingItem
        AddEditReprocessDialog(
            skus = state.skus,
            unqueuedReturnedDispatches = unqueuedReturnedDispatches,
            existingItem = currentEdit,
            onDismiss = {
                showAddDialog = false
                editingItem = null
            },
            onSubmit = { prod, sec, date, bags, bagWt, retKg, reason, cust, dr, srcId, fast, notes ->
                if (currentEdit == null) {
                    onAddReprocessItem(prod, sec, date, bags, bagWt, retKg, reason, cust, dr, srcId, fast, notes)
                } else {
                    onUpdateReprocessItem(currentEdit, prod, sec, date, bags, bagWt, retKg, reason, cust, dr, fast, notes)
                }
                showAddDialog = false
                editingItem = null
            }
        )
    }

    flaggingItem?.let { toFlag ->
        FlagReprocessToInventoryDialog(
            item = toFlag,
            onDismiss = { flaggingItem = null },
            onConfirmFlag = { bags, kg, date, notes ->
                onFlagToInventory(toFlag, bags, kg, date, notes)
                flaggingItem = null
            }
        )
    }

    damagingItem?.let { toDamage ->
        MarkReprocessDamagedDialog(
            item = toDamage,
            onDismiss = { damagingItem = null },
            onConfirmDamaged = { damagedDate, damageNotes ->
                onMarkDamaged(toDamage, damagedDate, damageNotes)
                damagingItem = null
            }
        )
    }

    pendingDeleteItem?.let { toDelete ->
        AlertDialog(
            onDismissRequest = { pendingDeleteItem = null },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = StatusCriticalTextDark) },
            title = { Text("Delete Reprocess Entry?") },
            text = {
                Text(
                    "Remove ${toDelete.productName} (${toDelete.returnedBags} bags, ${OperationsCalculator.formatKg(toDelete.returnedWeightKg)} kg) from Reprocess? Any linked inventory adjustment from this entry will be reversed."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteReprocessItem(toDelete)
                        pendingDeleteItem = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusCritical,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("confirm_delete_reprocess_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDeleteItem = null },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
