package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.model.ScheduleRun
import com.example.model.ScheduleStatus
import com.example.ui.components.IndustrialKpiCard
import com.example.ui.components.LocalIsDesktopLayout
import com.example.ui.components.LocalWindowWidthDp
import com.example.ui.components.MultiOrderLinkPickerDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.WhoseOrderDialog
import com.example.ui.components.desktopGridColumns
import com.example.ui.components.desktopGridItems
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.OnStatusCriticalContainer
import com.example.ui.theme.OnStatusHealthyContainer
import com.example.ui.theme.OnStatusWatchContainer
import com.example.ui.theme.StatusCriticalContainer
import com.example.ui.theme.StatusHealthyContainer
import com.example.ui.theme.StatusWatchContainer
import com.example.ui.theme.statusCriticalColor
import com.example.ui.theme.statusHealthyColor
import com.example.ui.theme.statusWatchColor
import com.example.viewmodel.OpsUiState

private data class BatchCustomerAttribution(
    val matchedRun: ScheduleRun?,
    val linkedOrders: List<CustomerOrder>,
    val unresolvedCodes: List<String>,
    val fallbackCustomers: List<String>,
    val openForSku: List<CustomerOrder>
)

@Composable
fun RecoveryScreen(
    state: OpsUiState.Ready,
    onOpenLogBatch: () -> Unit,
    onOpenBulkPaste: () -> Unit,
    onEditBatch: (ProductionBatch) -> Unit,
    onUpdateBatchLinks: (ProductionBatch, List<CustomerOrder>) -> Unit
) {
    var sectionFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var whoseOrderTarget by remember { mutableStateOf<Pair<ProductionBatch, BatchCustomerAttribution>?>(null) }
    var linkPickerTarget by remember { mutableStateOf<Pair<ProductionBatch, List<CustomerOrder>>?>(null) }

    val sectionTotals = remember(state.batches) {
        PlantSection.entries.map { sec ->
            val secBatches = state.batches.filter { it.section == sec.label }
            Triple(
                sec,
                secBatches.sumOf { it.bagsProduced },
                secBatches.sumOf { it.actualRecoveredKg }
            )
        }
    }

    val targetMap = remember(state.skus) {
        state.skus.associate { it.productName to it.targetRecoveryPct }
    }

    val orderById = remember(state.orders) { state.orders.associateBy { it.id } }
    val orderByCode = remember(state.orders) { state.orders.associateBy { it.orderCode } }
    val scheduleById = remember(state.schedules) { state.schedules.associateBy { it.id } }
    val openOrdersByProduct = remember(state.orders) {
        state.orders
            .filter { it.status == OrderStatus.OPEN.label || it.status == OrderStatus.PARTIALLY_DISPATCHED.label }
            .sortedWith(
                compareByDescending<CustomerOrder> { it.priority }
                    .thenByDescending { it.orderDate }
                    .thenByDescending { it.dueDate }
            )
            .groupBy { it.productName }
    }

    val filteredBatches = remember(state.batches, sectionFilter, searchQuery, orderById, orderByCode) {
        val q = searchQuery.trim()
        state.batches.filter { batch ->
            val matchesSection = sectionFilter == "ALL" || batch.section == sectionFilter
            if (!matchesSection) return@filter false
            if (q.isBlank()) return@filter true

            val linkedOrderMatches = batch.allLinkedOrderIds().any { id ->
                val ord = orderById[id]
                ord != null && (ord.customer.contains(q, ignoreCase = true) || ord.orderCode.contains(q, ignoreCase = true))
            } || batch.allLinkedOrderCodes().any { code ->
                code.contains(q, ignoreCase = true) ||
                    (orderByCode[code]?.customer?.contains(q, ignoreCase = true) == true)
            }

            batch.productName.contains(q, ignoreCase = true) ||
                batch.section.contains(q, ignoreCase = true) ||
                batch.remarks.contains(q, ignoreCase = true) ||
                batch.date.contains(q, ignoreCase = true) ||
                batch.allScheduledForCustomers().any { it.contains(q, ignoreCase = true) } ||
                linkedOrderMatches
        }
    }

    val primaryAccent = MaterialTheme.colorScheme.primary
    val tertiaryAccent = MaterialTheme.colorScheme.tertiary
    val watchAccent = statusWatchColor()
    val healthyAccent = statusHealthyColor()
    val criticalTextColor = statusCriticalColor()
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
                Text(
                    text = "Production & Recovery Logger",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Track production, customer order attribution & recovery across SAN ROQUE, PANDACAQUI, LA UNION QUAIL FEEDS & SELECTA. Batches below SKU target (98.5%) are flagged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onOpenLogBatch,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("log_batch_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForestEmerald,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Log Batch")
                    }
                    OutlinedButton(
                        onClick = onOpenBulkPaste,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bulk_paste_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bulk Paste")
                    }
                }
            }
        }

        // Section Totals Cards
        item {
            Row(
                modifier = if (isDesktop) {
                    Modifier.fillMaxWidth().height(IntrinsicSize.Max)
                } else {
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                },
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                sectionTotals.forEach { (sec, bags, kg) ->
                    val accent = when (sec) {
                        PlantSection.SAN_ROQUE -> primaryAccent
                        PlantSection.PANDACAQUI -> watchAccent
                        PlantSection.LA_UNION_QUAIL_FEEDS -> tertiaryAccent
                        PlantSection.SELECTA -> healthyAccent
                    }
                    IndustrialKpiCard(
                        title = sec.label,
                        primaryValue = "${String.format("%,d", bags)} bags",
                        secondaryValue = "${OperationsCalculator.formatKg(kg)} kg recovered",
                        icon = Icons.Filled.Factory,
                        accentColor = accent,
                        modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(205.dp)
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search product name or plant section") },
                placeholder = { Text("Type product, section, date, or customer...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search recovery batches"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recovery_search_input")
            )
        }

        item {
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
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        label = { Text(if (sec == "ALL") "All Locations (${state.batches.size})" else sec) }
                    )
                }
                if (sectionFilter != "ALL" || searchQuery.isNotBlank()) {
                    TextButton(
                        onClick = {
                            sectionFilter = "ALL"
                            searchQuery = ""
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("reset_recovery_filter_button")
                    ) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset")
                    }
                }
            }
        }

        if (filteredBatches.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when {
                            state.batches.isEmpty() ->
                                "No production batches logged yet. Tap Log Batch or Bulk Paste to record production & recovery."
                            searchQuery.isNotBlank() && sectionFilter != "ALL" ->
                                "No production batches match \"$searchQuery\" in $sectionFilter."
                            searchQuery.isNotBlank() ->
                                "No production batches match \"$searchQuery\"."
                            else ->
                                "No production batches logged for this location."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            desktopGridItems(
                itemsList = filteredBatches,
                columns = gridColumns,
                keyPrefix = "batch",
                itemKey = { it.id.ifBlank { "${it.date}_${it.productName}_${it.bagsProduced}" } }
            ) { b ->
                val batchKey = b.id.ifBlank { "${b.date}_${b.productName}_${b.bagsProduced}" }
                val targetPct = targetMap[b.productName] ?: 98.5
                val isFlagged = b.recoveryPct < targetPct

                val attribution = remember(b, state.schedules, scheduleById, orderById, orderByCode, openOrdersByProduct) {
                    resolveBatchCustomerAttribution(
                        batch = b,
                        schedules = state.schedules,
                        scheduleById = scheduleById,
                        orderById = orderById,
                        orderByCode = orderByCode,
                        openOrdersByProduct = openOrdersByProduct
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isFlagged) 1.5.dp else 1.dp,
                            color = if (isFlagged) criticalTextColor else MaterialTheme.colorScheme.outlineVariant,
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
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = b.productName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${b.date} • ${b.section} • ${b.remarks}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusPill(
                                label = if (isFlagged) "LOW ${String.format("%.2f%%", b.recoveryPct)}" else "${String.format("%.2f%%", b.recoveryPct)} OK",
                                bgColor = if (isFlagged) StatusCriticalContainer else StatusHealthyContainer,
                                textColor = if (isFlagged) OnStatusCriticalContainer else OnStatusHealthyContainer
                            )
                        }

                        BatchScheduledForBanner(
                            batchKey = batchKey,
                            attribution = attribution,
                            onOpenWhoseOrder = { whoseOrderTarget = b to attribution },
                            onOpenLinkPicker = { linkPickerTarget = b to attribution.linkedOrders }
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                BatchStat("BAGS x WT", "${b.bagsProduced} x ${b.bagWeightKg}kg")
                                BatchStat("EXPECTED", "${OperationsCalculator.formatKg(b.expectedWeightKg)} kg")
                                BatchStat("RECOVERED", "${OperationsCalculator.formatKg(b.actualRecoveredKg)} kg")
                                BatchStat(
                                    "VARIANCE",
                                    "${if (b.varianceKg >= 0) "+" else ""}${OperationsCalculator.formatKg(b.varianceKg)} kg",
                                    isNegative = b.varianceKg < 0,
                                    criticalTextColor = criticalTextColor
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { onEditBatch(b) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_batch_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
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

    whoseOrderTarget?.let { (batch, attr) ->
        val unresolvedLabels = attr.fallbackCustomers.map { "Customer: $it" } + attr.unresolvedCodes
        WhoseOrderDialog(
            productName = batch.productName,
            run = attr.matchedRun,
            linkedOrders = attr.linkedOrders,
            unresolvedLinkedCodes = unresolvedLabels,
            skuOpenOrders = attr.openForSku,
            canEditLinks = state.orders.isNotEmpty(),
            onEditLinkedOrders = {
                whoseOrderTarget = null
                linkPickerTarget = batch to attr.linkedOrders
            },
            onLinkAllOpenOrders = if (attr.openForSku.isNotEmpty()) {
                {
                    val merged = (attr.linkedOrders + attr.openForSku)
                        .distinctBy { it.id.ifBlank { it.orderCode } }
                    onUpdateBatchLinks(batch, merged)
                    whoseOrderTarget = null
                }
            } else null,
            onDismiss = { whoseOrderTarget = null }
        )
    }

    linkPickerTarget?.let { (batch, currentLinked) ->
        MultiOrderLinkPickerDialog(
            productName = batch.productName,
            allOrders = state.orders,
            initiallySelectedIds = currentLinked.map { it.id }.filter { it.isNotBlank() }.toSet(),
            onDismiss = { linkPickerTarget = null },
            onConfirm = { picked ->
                onUpdateBatchLinks(batch, picked)
                linkPickerTarget = null
            }
        )
    }
}

private fun resolveBatchCustomerAttribution(
    batch: ProductionBatch,
    schedules: List<ScheduleRun>,
    scheduleById: Map<String, ScheduleRun>,
    orderById: Map<String, CustomerOrder>,
    orderByCode: Map<String, CustomerOrder>,
    openOrdersByProduct: Map<String, List<CustomerOrder>>
): BatchCustomerAttribution {
    val matchedRun = if (batch.scheduleRunId.isNotBlank()) {
        scheduleById[batch.scheduleRunId]
    } else {
        val candidates = schedules.filter {
            it.productName == batch.productName &&
                it.section == batch.section &&
                it.status != ScheduleStatus.CANCELLED.label
        }
        candidates.firstOrNull { it.scheduledDate == batch.date && it.status == ScheduleStatus.COMPLETED.label }
            ?: candidates.firstOrNull { it.scheduledDate == batch.date }
            ?: candidates.firstOrNull { it.status == ScheduleStatus.COMPLETED.label }
    }

    val batchIds = batch.allLinkedOrderIds()
    val batchCodes = batch.allLinkedOrderCodes()
    val batchCustomers = batch.allScheduledForCustomers()
    val hasExplicitAttribution =
        batchIds.isNotEmpty() || batchCodes.isNotEmpty() || batchCustomers.isNotEmpty()

    val effectiveIds = if (hasExplicitAttribution) batchIds else matchedRun?.allLinkedOrderIds().orEmpty()
    val effectiveCodes = if (hasExplicitAttribution) batchCodes else matchedRun?.allLinkedOrderCodes().orEmpty()

    val linkedOrders = (effectiveIds.mapNotNull { orderById[it] } +
        effectiveCodes.mapNotNull { orderByCode[it] })
        .distinctBy { it.id.ifBlank { it.orderCode } }

    val linkedIdSet = linkedOrders.map { it.id }.toSet()
    val linkedCodeSet = linkedOrders.map { it.orderCode }.toSet()
    val linkedCustomerSet = linkedOrders.map { it.customer.trim().lowercase() }.toSet()

    val unresolvedCodes = effectiveCodes.filter { it !in linkedCodeSet }
    val fallbackCustomers = batchCustomers.filter { it.trim().lowercase() !in linkedCustomerSet }
    val openForSku = openOrdersByProduct[batch.productName].orEmpty().filter { it.id !in linkedIdSet }

    return BatchCustomerAttribution(
        matchedRun = matchedRun,
        linkedOrders = linkedOrders,
        unresolvedCodes = unresolvedCodes,
        fallbackCustomers = fallbackCustomers,
        openForSku = openForSku
    )
}

@Composable
private fun BatchScheduledForBanner(
    batchKey: String,
    attribution: BatchCustomerAttribution,
    onOpenWhoseOrder: () -> Unit,
    onOpenLinkPicker: () -> Unit
) {
    val linkedOrders = attribution.linkedOrders
    val fallbackCustomers = attribution.fallbackCustomers
    val unresolvedCodes = attribution.unresolvedCodes
    val openForSku = attribution.openForSku
    val hasAttribution = linkedOrders.isNotEmpty() || fallbackCustomers.isNotEmpty() || unresolvedCodes.isNotEmpty()

    if (hasAttribution) {
        val totalEntries = linkedOrders.size + fallbackCustomers.size + unresolvedCodes.size
        val isCompactList = totalEntries <= 2
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recovery_scheduled_for_banner_$batchKey")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isCompactList) {
                    linkedOrders.forEach { ord ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ShoppingCart,
                                contentDescription = "Scheduled for customer order",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Scheduled for: ${ord.customer}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (ord.priority) {
                                        StatusPill("PRIORITY", StatusWatchContainer, OnStatusWatchContainer)
                                    }
                                }
                                Text(
                                    text = "Order: ${ord.orderCode} • ${ord.orderedBags} bags • Due ${ord.dueDate} • ${ord.status}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.88f)
                                )
                            }
                        }
                    }
                    fallbackCustomers.forEach { custName ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.People,
                                contentDescription = "Scheduled for customer",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Scheduled for: $custName",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    unresolvedCodes.forEach { code ->
                        Text(
                            text = "Linked Order Ref: $code",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.88f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onOpenWhoseOrder,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.testTag("recovery_whose_order_button_$batchKey")
                        ) {
                            Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (openForSku.isNotEmpty()) {
                                    "Whose Order? (+${openForSku.size} Open)"
                                } else {
                                    "View / Edit Customers"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                } else {
                    val firstCustomer = linkedOrders.firstOrNull()?.customer
                        ?: fallbackCustomers.firstOrNull()
                        ?: unresolvedCodes.firstOrNull().orEmpty()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.People,
                            contentDescription = "Multiple scheduled customers",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Scheduled for: $firstCustomer (+${totalEntries - 1} more)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (linkedOrders.any { it.priority }) {
                            StatusPill("PRIORITY", StatusWatchContainer, OnStatusWatchContainer)
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenWhoseOrder,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recovery_whose_order_button_$batchKey")
                    ) {
                        Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Whose Order? View All (${totalEntries + openForSku.size})",
                            maxLines = 1
                        )
                    }
                }
            }
        }
    } else {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recovery_scheduled_for_banner_$batchKey")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.People,
                        contentDescription = "General stock or buffer run",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text(
                            text = if (attribution.matchedRun != null) {
                                "Scheduled for: Buffer / General Stock (${attribution.matchedRun.priority})"
                            } else {
                                "Scheduled for: General Stock (Direct Batch)"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (openForSku.isNotEmpty()) {
                            Text(
                                text = "${openForSku.size} open customer order(s) for this SKU",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                TextButton(
                    onClick = if (openForSku.isNotEmpty()) onOpenWhoseOrder else onOpenLinkPicker,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("recovery_assign_customer_button_$batchKey")
                ) {
                    Text(
                        text = if (openForSku.isNotEmpty()) "Whose Order?" else "Link Order",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun BatchStat(
    label: String,
    value: String,
    isNegative: Boolean = false,
    criticalTextColor: Color = MaterialTheme.colorScheme.error
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isNegative) criticalTextColor else MaterialTheme.colorScheme.onSurface
        )
    }
}
