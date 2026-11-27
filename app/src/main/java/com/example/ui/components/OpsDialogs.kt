package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.OperationsCalculator
import com.example.model.CustomerEntity
import com.example.model.CustomerOrder
import com.example.model.DispatchRecord
import com.example.model.DispatchReturnReasons
import com.example.model.OrderStatus
import com.example.model.PlantCustomers
import com.example.model.PlantLineRemarks
import com.example.model.PlantSection
import com.example.model.ProductionBatch
import com.example.model.SchedulePriority
import com.example.model.ScheduleRun
import com.example.model.SkuEntity
import com.example.model.SkuInventorySummary
import com.example.ui.theme.DarkChartTrack
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.GrainAmber
import com.example.ui.theme.GrainAmberBright
import com.example.ui.theme.OnStatusCriticalContainer
import com.example.ui.theme.OnStatusHealthyContainer
import com.example.ui.theme.OnStatusWatchContainer
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusCriticalContainer
import com.example.ui.theme.StatusCriticalTextDark
import com.example.ui.theme.StatusHealthyContainer
import com.example.ui.theme.StatusHealthyTextDark
import com.example.ui.theme.StatusWatchContainer
import com.example.viewmodel.DispatchEntryInput
import com.example.viewmodel.NewOrderItemInput
import com.example.viewmodel.NewScheduleItemInput
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round

private val ISO_DATE_REGEX = Regex("^(\\d{4})-(\\d{1,2})-(\\d{1,2})$")
private val SLASH_YMD_REGEX = Regex("^(\\d{4})/(\\d{1,2})/(\\d{1,2})$")
private val SLASH_MDY_REGEX = Regex("^(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})$")

internal fun normalizeIsoDateOrNull(raw: String): String? {
    val trimmed = raw.trim()
    val ymd = ISO_DATE_REGEX.matchEntire(trimmed) ?: SLASH_YMD_REGEX.matchEntire(trimmed)
    if (ymd != null) {
        val y = ymd.groupValues[1].toIntOrNull() ?: return null
        val m = ymd.groupValues[2].toIntOrNull() ?: return null
        val d = ymd.groupValues[3].toIntOrNull() ?: return null
        if (m !in 1..12 || d !in 1..31) return null
        return String.format("%04d-%02d-%02d", y, m, d)
    }
    val mdy = SLASH_MDY_REGEX.matchEntire(trimmed)
    if (mdy != null) {
        val m = mdy.groupValues[1].toIntOrNull() ?: return null
        val d = mdy.groupValues[2].toIntOrNull() ?: return null
        val y = mdy.groupValues[3].toIntOrNull() ?: return null
        if (m !in 1..12 || d !in 1..31) return null
        return String.format("%04d-%02d-%02d", y, m, d)
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDispatchDialog(
    skus: List<SkuEntity>,
    summaries: List<SkuInventorySummary>,
    openOrders: List<CustomerOrder>,
    customers: List<CustomerEntity> = emptyList(),
    existingCustomers: List<String> = emptyList(),
    initialSku: SkuEntity? = null,
    initialSkuName: String? = null,
    initialOrder: CustomerOrder? = null,
    initialOrders: List<CustomerOrder> = emptyList(),
    existingDispatch: DispatchRecord? = null,
    priorityProductKeys: Set<String> = emptySet(),
    restrictToSelectedProducts: Boolean = priorityProductKeys.isNotEmpty(),
    preselectedPlant: String = "",
    onPreselectPlant: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onManageCustomers: (() -> Unit)? = null,
    onAddNewProduction: (sku: SkuEntity, neededBags: Long, linkedOrder: CustomerOrder?) -> Unit,
    onAddMultipleProduction: ((List<ScheduleDraftSeed>) -> Unit)? = null,
    onSubmitMultiple: ((List<DispatchEntryInput>) -> Unit)? = null,
    onSubmit: (DispatchEntryInput) -> Unit
) {
    val isEditing = existingDispatch != null
    val defaultProduct = existingDispatch?.productName
        ?: initialOrder?.productName
        ?: initialOrders.firstOrNull()?.productName
        ?: initialSku?.productName
        ?: initialSkuName
        ?: ""
    val defaultSku = initialSku ?: skus.firstOrNull { it.productName.equals(defaultProduct, ignoreCase = true) }

    val availByKey = remember(summaries) {
        summaries.associate { skuStableKey(it.sku) to it.availableStockKg }
    }
    val availByName = remember(summaries) {
        summaries.associate { it.sku.productName.lowercase() to it.availableStockKg }
    }
    val skuByNameLower = remember(skus) {
        skus.associateBy { it.productName.lowercase() }
    }

    val initialMultiDrafts = remember(isEditing, initialSku, initialOrder, initialOrders, skus) {
        if (isEditing) {
            emptyList()
        } else {
            val seedOrders = when {
                initialOrders.isNotEmpty() -> initialOrders
                initialOrder != null -> listOf(initialOrder)
                else -> emptyList()
            }
            when {
                seedOrders.isNotEmpty() -> {
                    seedOrders.mapIndexed { idx, ord ->
                        val resolvedSku = skuByNameLower[ord.productName.lowercase()] ?: SkuEntity(
                            productName = ord.productName,
                            section = PlantSection.SAN_ROQUE.label,
                            standardBagWeightKg = ord.bagWeightKg.coerceAtLeast(1.0),
                            defaultRemarks = PlantLineRemarks.options.first()
                        )
                        val remKg = max(0.0, ord.totalWeightKg - ord.dispatchedWeightKg)
                        val bags = if (ord.bagWeightKg > 0.0) {
                            ceil(remKg / ord.bagWeightKg).toLong().coerceAtLeast(1L)
                        } else 20L
                        MultiDispatchItemDraft(
                            key = "ord_${ord.id.ifBlank { "${idx}_${UUID.randomUUID().toString().take(6)}" }}",
                            sku = resolvedSku,
                            bagsText = bags.toString(),
                            bagWtText = ord.bagWeightKg.toString(),
                            customer = ord.customer,
                            drReference = "",
                            linkedOrder = ord,
                            returnedBagsText = ""
                        )
                    }
                }
                initialSku != null -> {
                    listOf(
                        MultiDispatchItemDraft(
                            key = "stop_${skuStableKey(initialSku)}_${UUID.randomUUID().toString().take(6)}",
                            sku = initialSku,
                            bagsText = "20",
                            bagWtText = initialSku.standardBagWeightKg.toString(),
                            customer = "",
                            drReference = "",
                            linkedOrder = null,
                            returnedBagsText = ""
                        )
                    )
                }
                else -> emptyList()
            }
        }
    }

    var date by remember { mutableStateOf(existingDispatch?.date ?: OperationsCalculator.todayIsoDate()) }
    var productName by remember { mutableStateOf(defaultProduct) }
    var bagWeightText by remember {
        mutableStateOf(
            (existingDispatch?.bagWeightKg
                ?: initialOrder?.bagWeightKg
                ?: defaultSku?.standardBagWeightKg
                ?: 50.0).toString()
        )
    }
    var bagsText by remember {
        val initBags = existingDispatch?.bagsDispatched?.toString() ?: "20"
        mutableStateOf(initBags)
    }
    var drRef by remember { mutableStateOf(existingDispatch?.drReference ?: "DR-BLC-${(1000..9999).random()}") }
    var customer by remember {
        mutableStateOf(
            existingDispatch?.customer
                ?: initialOrder?.customer
                ?: initialOrders.map { it.customer.trim() }.filter { it.isNotBlank() }.distinct().singleOrNull()
                ?: ""
        )
    }
    var selectedOrder by remember {
        mutableStateOf(
            initialOrder ?: existingDispatch?.linkedOrderId?.takeIf { it.isNotBlank() }?.let { lid ->
                openOrders.firstOrNull { it.id == lid }
            }
        )
    }
    var selectedSourcePlant by remember(
        existingDispatch,
        initialOrder,
        initialOrders,
        initialSku,
        preselectedPlant,
        skuByNameLower
    ) {
        mutableStateOf(
            OperationsCalculator.resolveInitialDispatchSourcePlant(
                existingDispatch = existingDispatch,
                initialOrder = initialOrder,
                initialOrders = initialOrders,
                initialSku = initialSku,
                preselectedPlant = preselectedPlant,
                skuByNameLower = skuByNameLower
            )
        )
    }
    var selectedItems by remember(initialMultiDrafts) { mutableStateOf(initialMultiDrafts) }
    var productSearchQuery by remember { mutableStateOf("") }
    var showMultiSkuPicker by remember { mutableStateOf(false) }
    var showMultiOrderPicker by remember { mutableStateOf(false) }
    var showAddCustomerToBatchDialog by remember { mutableStateOf(false) }
    var showAskAddCustomerDialog by remember { mutableStateOf(false) }
    var extraCustomerPromptAnswered by remember { mutableStateOf(false) }
    var pendingBatchEntriesForAsk by remember { mutableStateOf<List<DispatchEntryInput>>(emptyList()) }
    var autoConnectedCustomerNotice by remember { mutableStateOf<String?>(null) }
    var showEditSingleSkuPicker by remember { mutableStateOf(false) }
    var expandedOrderDropdownKey by remember { mutableStateOf<String?>(null) }
    var expandedCustomerDropdownKey by remember { mutableStateOf<String?>(null) }
    var showLogisticsExpanded by remember { mutableStateOf(false) }
    var expandedStopOptionsKeys by remember { mutableStateOf<Set<String>>(emptySet()) }

    val buildOrderDraft: (CustomerOrder) -> MultiDispatchItemDraft = { ord ->
        val resolvedSku = skuByNameLower[ord.productName.lowercase()] ?: SkuEntity(
            productName = ord.productName,
            section = PlantSection.SAN_ROQUE.label,
            standardBagWeightKg = ord.bagWeightKg.coerceAtLeast(1.0),
            defaultRemarks = PlantLineRemarks.options.first()
        )
        val remKg = max(0.0, ord.totalWeightKg - ord.dispatchedWeightKg)
        val bags = if (ord.bagWeightKg > 0.0) {
            ceil(remKg / ord.bagWeightKg).toLong().coerceAtLeast(1L)
        } else 20L
        MultiDispatchItemDraft(
            key = "ord_${ord.id.ifBlank { UUID.randomUUID().toString().take(8) }}",
            sku = resolvedSku,
            bagsText = bags.toString(),
            bagWtText = ord.bagWeightKg.toString(),
            customer = ord.customer,
            drReference = "",
            linkedOrder = ord,
            returnedBagsText = ""
        )
    }

    var driverName by remember { mutableStateOf(existingDispatch?.driverName ?: "") }
    var truckNumber by remember { mutableStateOf(existingDispatch?.truckNumber ?: "") }
    var helperName by remember { mutableStateOf(existingDispatch?.helperName ?: "") }
    var remarks by remember { mutableStateOf(existingDispatch?.remarks ?: "") }

    var hasReturn by remember { mutableStateOf(existingDispatch?.hasReturn ?: false) }
    var returnConfirmed by remember { mutableStateOf(existingDispatch?.returnConfirmed ?: false) }
    var returnedBagsText by remember {
        mutableStateOf(
            if ((existingDispatch?.returnedBags ?: 0L) > 0L) {
                existingDispatch!!.returnedBags.toString()
            } else {
                ""
            }
        )
    }
    var returnReason by remember {
        mutableStateOf(existingDispatch?.returnReason?.ifBlank { null } ?: DispatchReturnReasons.options.first())
    }
    var customReturnReason by remember {
        val existingReason = existingDispatch?.returnReason.orEmpty()
        mutableStateOf(if (existingReason.isNotBlank() && existingReason !in DispatchReturnReasons.options) existingReason else "")
    }
    var sendToReprocess by remember {
        mutableStateOf(existingDispatch?.returnConfirmed != true)
    }

    var isHeld by remember { mutableStateOf(existingDispatch?.isHeld ?: false) }
    var heldRemarks by remember { mutableStateOf(existingDispatch?.heldRemarks ?: "") }

    var orderExpanded by remember { mutableStateOf(false) }
    var customerExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showShortageFlagDialog by remember { mutableStateOf(false) }

    val resolvedReturnReason = if (returnReason == "Other") customReturnReason.trim() else returnReason

    // Edit-mode computed values
    val bagsVal = bagsText.toLongOrNull() ?: 0L
    val bagWtVal = bagWeightText.toDoubleOrNull() ?: 0.0
    val computedTotalKg = round(bagsVal * bagWtVal * 100.0) / 100.0
    val returnedBagsVal = if (hasReturn && returnConfirmed) (returnedBagsText.toLongOrNull() ?: 0L) else 0L
    val computedReturnedKg = round(returnedBagsVal * bagWtVal * 100.0) / 100.0
    val netDeliveredKg = max(0.0, round((computedTotalKg - computedReturnedKg) * 100.0) / 100.0)

    val matchedSummary = summaries.firstOrNull {
        initialSku != null &&
            it.sku.id == initialSku.id &&
            it.sku.productName.equals(productName, ignoreCase = true)
    } ?: summaries.firstOrNull {
        it.sku.productName.equals(productName, ignoreCase = true)
    }
    val matchedSku = matchedSummary?.sku
        ?: skus.firstOrNull { it.productName.equals(productName, ignoreCase = true) }
    val rawAvailableKg = max(0.0, matchedSummary?.availableStockKg ?: 0.0)
    val priorCreditKg = existingDispatch?.takeIf {
        it.productName.equals(productName, ignoreCase = true)
    }?.effectiveDispatchedWeightKg() ?: 0.0
    val availableKg = rawAvailableKg + priorCreditKg
    val availableBags = if (bagWtVal > 0.0) availableKg / bagWtVal else max(0.0, matchedSummary?.availableBags ?: 0.0)
    val maxWholeBagsAvailable = if (bagWtVal > 0.0) floor((availableKg + 0.01) / bagWtVal).toLong().coerceAtLeast(0L) else 0L
    val singleShortfallKg = max(0.0, netDeliveredKg - availableKg)
    val singleShortfallBags = if (bagWtVal > 0.0) ceil(singleShortfallKg / bagWtVal).toLong().coerceAtLeast(1L) else 1L
    val singleExceedsAvailableStock = isEditing && bagsVal > 0 && bagWtVal > 0.0 && netDeliveredKg > availableKg + 0.01

    // Multi-mode computed values
    val multiTotalBags = selectedItems.sumOf { (it.bagsText.toLongOrNull() ?: 0L).coerceAtLeast(0L) }
    val multiTotalGrossKg = selectedItems.sumOf { item ->
        val b = (item.bagsText.toLongOrNull() ?: 0L).coerceAtLeast(0L)
        val w = (item.bagWtText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
        round(b * w * 100.0) / 100.0
    }
    val multiTotalReturnedKg = if (hasReturn && returnConfirmed) {
        selectedItems.sumOf { item ->
            val rb = (item.returnedBagsText.toLongOrNull() ?: 0L).coerceAtLeast(0L)
            val w = (item.bagWtText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
            round(rb * w * 100.0) / 100.0
        }
    } else 0.0
    val multiTotalNetKg = max(0.0, round((multiTotalGrossKg - multiTotalReturnedKg) * 100.0) / 100.0)

    val totalNetKgByProduct = remember(selectedItems, hasReturn, returnConfirmed) {
        selectedItems
            .groupBy { it.sku.productName.trim().lowercase() }
            .mapValues { (_, group) ->
                group.sumOf { item ->
                    val b = (item.bagsText.toLongOrNull() ?: 0L).coerceAtLeast(0L)
                    val w = (item.bagWtText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                    val gross = round(b * w * 100.0) / 100.0
                    val rb = if (hasReturn && returnConfirmed) (item.returnedBagsText.toLongOrNull() ?: 0L).coerceAtLeast(0L) else 0L
                    val ret = round(rb * w * 100.0) / 100.0
                    max(0.0, gross - ret)
                }
            }
    }

    val multiShortageItems = remember(selectedItems, totalNetKgByProduct, availByKey, availByName) {
        selectedItems
            .groupBy { it.sku.productName.trim().lowercase() }
            .values
            .mapNotNull { group ->
                val rep = group.first()
                val prodKey = rep.sku.productName.trim().lowercase()
                val netTotal = totalNetKgByProduct[prodKey] ?: 0.0
                val avail = max(0.0, availByKey[skuStableKey(rep.sku)] ?: availByName[prodKey] ?: 0.0)
                val activeWt = group.firstNotNullOfOrNull { (it.bagWtText.toDoubleOrNull() ?: 0.0).takeIf { w -> w > 0.0 } }
                    ?: rep.sku.standardBagWeightKg.coerceAtLeast(1.0)
                if (netTotal > avail + 0.01 && activeWt > 0.0) {
                    val shortKg = netTotal - avail
                    val shortBags = ceil(shortKg / activeWt).toLong().coerceAtLeast(1L)
                    val groupOrders = group.mapNotNull { it.linkedOrder }.distinctBy { it.id }
                    ScheduleDraftSeed(
                        sku = rep.sku,
                        bags = shortBags,
                        linkedOrder = groupOrders.firstOrNull(),
                        linkedOrders = groupOrders
                    ) to shortKg
                } else {
                    null
                }
            }
    }
    val multiExceedsAvailableStock = !isEditing && multiShortageItems.isNotEmpty()
    val anyExceedsStock = if (isEditing) singleExceedsAvailableStock else multiExceedsAvailableStock

    val availableSelectedSkus = remember(
        skus,
        availByKey,
        availByName,
        priorityProductKeys,
        restrictToSelectedProducts
    ) {
        OperationsCalculator.filterDispatchAvailableSelectedSkus(
            skus = skus,
            availByKey = availByKey,
            availByName = availByName,
            selectedPriorityKeys = priorityProductKeys,
            restrictToSelectedKeys = restrictToSelectedProducts,
            sourcePlantFilter = ""
        )
    }

    val plantFilteredAvailableSelectedSkus = remember(
        availableSelectedSkus,
        availByKey,
        availByName,
        priorityProductKeys,
        restrictToSelectedProducts,
        selectedSourcePlant
    ) {
        OperationsCalculator.filterDispatchAvailableSelectedSkus(
            skus = availableSelectedSkus,
            availByKey = availByKey,
            availByName = availByName,
            selectedPriorityKeys = priorityProductKeys,
            restrictToSelectedKeys = restrictToSelectedProducts,
            sourcePlantFilter = selectedSourcePlant
        )
    }

    val filteredQuickSkus = remember(plantFilteredAvailableSelectedSkus, productSearchQuery) {
        val q = productSearchQuery.trim()
        val list = if (q.isBlank()) {
            plantFilteredAvailableSelectedSkus
        } else {
            plantFilteredAvailableSelectedSkus.filter {
                it.productName.contains(q, ignoreCase = true) ||
                    it.section.contains(q, ignoreCase = true)
            }
        }
        list.sortedWith(compareBy({ it.productName.lowercase() }, { it.section }))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingDispatch == null) "Log Dispatch" else "Edit Dispatch ${existingDispatch.drReference}",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dispatch_date_input")
                    )
                    OutlinedTextField(
                        value = drRef,
                        onValueChange = { drRef = it },
                        label = { Text("DR #") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dispatch_dr_input")
                    )
                }

                // Preselected Source Plant ("Came From") Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Factory,
                                contentDescription = "Source plant",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "From Plant: ${selectedSourcePlant.ifBlank { "All Plants" }}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (onPreselectPlant != null) {
                            val isSavedDefault = selectedSourcePlant == OperationsCalculator.normalizePlantLabel(preselectedPlant)
                            FilterChip(
                                selected = isSavedDefault,
                                onClick = { onPreselectPlant(selectedSourcePlant) },
                                label = {
                                    Text(
                                        text = if (isSavedDefault) "Default ✓" else "Set Default",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.testTag("dispatch_set_default_plant_chip")
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedSourcePlant.isBlank(),
                            onClick = { selectedSourcePlant = "" },
                            label = { Text("All Plants", maxLines = 1) },
                            modifier = Modifier.testTag("dispatch_source_plant_all")
                        )
                        PlantSection.entries.forEach { plant ->
                            FilterChip(
                                selected = selectedSourcePlant.equals(plant.label, ignoreCase = true),
                                onClick = { selectedSourcePlant = plant.label },
                                label = { Text(plant.label, maxLines = 1) },
                                modifier = Modifier.testTag("dispatch_source_plant_${plant.name.lowercase()}")
                            )
                        }
                    }
                }

                // Customer / Destination Selector (auto-connects and surfaces previous open orders for the customer)
                val allDispatchCustomers = remember(customers, existingCustomers, openOrders) {
                    val baseList = if (customers.isNotEmpty()) customers.map { it.name } else PlantCustomers.directory
                    (baseList + existingCustomers + openOrders.map { it.customer })
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinctBy { it.lowercase() }
                        .sortedBy { it.lowercase() }
                }
                val dispatchCustomerOptions = remember(customer, allDispatchCustomers) {
                    val isExact = allDispatchCustomers.any { it.equals(customer.trim(), ignoreCase = true) }
                    if (customer.isBlank() || isExact) allDispatchCustomers
                    else allDispatchCustomers.filter { it.contains(customer.trim(), ignoreCase = true) }
                }
                val connectedOrderIds = remember(selectedItems) {
                    selectedItems.mapNotNull { it.linkedOrder?.id }.toSet()
                }
                val trimmedTopCustomer = customer.trim()
                val customerPreviousOrders = remember(trimmedTopCustomer, openOrders) {
                    if (trimmedTopCustomer.isBlank()) emptyList()
                    else openOrders
                        .filter { it.customer.trim().equals(trimmedTopCustomer, ignoreCase = true) }
                        .sortedWith(
                            compareBy<CustomerOrder> { it.orderDate }
                                .thenBy { it.dueDate }
                                .thenByDescending { it.priority }
                        )
                }
                val unconnectedCustomerOrders = remember(customerPreviousOrders, connectedOrderIds) {
                    customerPreviousOrders.filter { it.id !in connectedOrderIds }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = customerExpanded,
                        onExpandedChange = { customerExpanded = !customerExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = customer,
                            onValueChange = {
                                customer = it
                                autoConnectedCustomerNotice = null
                                customerExpanded = true
                            },
                            label = { Text(if (isEditing) "Customer" else "Default Customer") },
                            placeholder = { Text("Select customer...") },
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryEditable)
                                .fillMaxWidth()
                                .testTag("dispatch_customer_input")
                        )
                        if (dispatchCustomerOptions.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = customerExpanded,
                                onDismissRequest = { customerExpanded = false }
                            ) {
                                dispatchCustomerOptions.forEach { custName ->
                                    val custOpenCount = openOrders.count {
                                        it.customer.trim().equals(custName, ignoreCase = true)
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (custOpenCount > 0) "$custName ($custOpenCount open PO)"
                                                else custName
                                            )
                                        },
                                        onClick = {
                                            customer = custName
                                            customerExpanded = false
                                            if (!isEditing) {
                                                val matchingPrev = openOrders
                                                    .filter {
                                                        it.customer.trim().equals(custName, ignoreCase = true) &&
                                                            it.id !in connectedOrderIds
                                                    }
                                                    .sortedWith(
                                                        compareBy<CustomerOrder> { it.orderDate }
                                                            .thenBy { it.dueDate }
                                                            .thenByDescending { it.priority }
                                                    )
                                                if (selectedItems.isEmpty() && matchingPrev.isNotEmpty()) {
                                                    selectedItems = matchingPrev.map { buildOrderDraft(it) }
                                                    autoConnectedCustomerNotice =
                                                        "Connected ${matchingPrev.size} previous open order(s) for $custName"
                                                }
                                            } else if (selectedOrder == null) {
                                                val matchSingle = openOrders
                                                    .filter {
                                                        it.customer.trim().equals(custName, ignoreCase = true) &&
                                                            it.productName.equals(productName, ignoreCase = true)
                                                    }
                                                    .minByOrNull { it.orderDate }
                                                if (matchSingle != null) {
                                                    selectedOrder = matchSingle
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (onManageCustomers != null) {
                        IconButton(
                            onClick = onManageCustomers,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("dispatch_manage_customers_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit Customers List",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (!isEditing && customerPreviousOrders.isNotEmpty()) {
                    if (unconnectedCustomerOrders.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dispatch_customer_previous_orders_card")
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
                                        text = "Previous Orders for $trimmedTopCustomer (${unconnectedCustomerOrders.size})",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = {
                                            selectedItems = selectedItems + unconnectedCustomerOrders.map { buildOrderDraft(it) }
                                            autoConnectedCustomerNotice =
                                                "Connected ${unconnectedCustomerOrders.size} previous order(s) for $trimmedTopCustomer"
                                        },
                                        modifier = Modifier.testTag("dispatch_connect_all_customer_orders_button")
                                    ) {
                                        Text("Connect All (${unconnectedCustomerOrders.size})")
                                    }
                                }
                                unconnectedCustomerOrders.forEach { ord ->
                                    val remKg = max(0.0, ord.totalWeightKg - ord.dispatchedWeightKg)
                                    val remBags = if (ord.bagWeightKg > 0.0) {
                                        ceil(remKg / ord.bagWeightKg).toLong().coerceAtLeast(1L)
                                    } else ord.orderedBags
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${ord.orderCode} • ${ord.productName}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Rem: $remBags bags (${OperationsCalculator.formatKg(remKg)} kg)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                selectedItems = selectedItems + buildOrderDraft(ord)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("dispatch_connect_order_${ord.id}")
                                        ) {
                                            Text("Connect")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = StatusHealthyContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dispatch_customer_orders_connected_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Orders connected",
                                    tint = OnStatusHealthyContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = autoConnectedCustomerNotice
                                        ?: "Connected ${customerPreviousOrders.size} open order(s)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = OnStatusHealthyContainer
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                if (!isEditing) {
                    // MULTI-ORDER & MULTI-CUSTOMER TRUCKLOAD SECTION (Minimized & Focused)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Stops (${selectedItems.size})",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (selectedSourcePlant.isNotBlank()) {
                                    "${plantFilteredAvailableSelectedSkus.size} avail in $selectedSourcePlant"
                                } else {
                                    "${plantFilteredAvailableSelectedSkus.size} avail selected"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val linkedOrdersCount = selectedItems.count { it.linkedOrder != null }
                            OutlinedButton(
                                onClick = { showMultiOrderPicker = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dispatch_pick_orders_button")
                            ) {
                                Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (linkedOrdersCount > 0) "Orders ($linkedOrdersCount)" else "Pick Orders",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = { showMultiSkuPicker = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dispatch_select_products_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (selectedItems.isEmpty()) "Select Products" else "Add Product",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Inline Search Bar for available selected products only
                    OutlinedTextField(
                        value = productSearchQuery,
                        onValueChange = { productSearchQuery = it },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search products") },
                        trailingIcon = {
                            if (productSearchQuery.isNotBlank()) {
                                IconButton(onClick = { productSearchQuery = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear product search")
                                }
                            }
                        },
                        placeholder = { Text("Search available selected products...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dispatch_product_search_input")
                    )

                    // Show searchable quick-add SKU list when searching OR when no stops added yet
                    if (productSearchQuery.isNotBlank() || selectedItems.isEmpty()) {
                        val stopCountBySkuName = selectedItems.groupingBy { it.sku.productName.trim().lowercase() }.eachCount()
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = if (filteredQuickSkus.isEmpty()) {
                                        "No available selected products"
                                    } else {
                                        "Available selected products (${filteredQuickSkus.size})"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                )
                                if (filteredQuickSkus.isEmpty()) {
                                    Text(
                                        text = when {
                                            productSearchQuery.isNotBlank() -> "No available selected product matches \"$productSearchQuery\"."
                                            selectedSourcePlant.isNotBlank() -> "No available selected products with stock in $selectedSourcePlant. Switch From Plant or check Inventory."
                                            else -> "No available selected products with stock (> 0 kg). Select products or produce stock first."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 200.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        filteredQuickSkus.take(25).forEach { sku ->
                                            val k = skuStableKey(sku)
                                            val prodKey = sku.productName.trim().lowercase()
                                            val countInTruck = stopCountBySkuName[prodKey] ?: 0
                                            val avail = max(0.0, availByKey[k] ?: availByName[prodKey] ?: 0.0)
                                            val skuOrdersCount = openOrders.count {
                                                it.productName.equals(sku.productName, ignoreCase = true)
                                            }
                                            val addStopForSku = {
                                                val usedOrderIds = selectedItems.mapNotNull { it.linkedOrder?.id }.toSet()
                                                val preferredOrder = openOrders
                                                    .filter {
                                                        it.productName.equals(sku.productName, ignoreCase = true) &&
                                                            it.id !in usedOrderIds
                                                    }
                                                    .sortedWith(
                                                        compareByDescending<CustomerOrder> {
                                                            customer.isNotBlank() && it.customer.equals(customer.trim(), ignoreCase = true)
                                                        }.thenByDescending { it.priority }
                                                    )
                                                    .firstOrNull {
                                                        customer.isNotBlank() && it.customer.equals(customer.trim(), ignoreCase = true)
                                                    }
                                                val initBags = if (preferredOrder != null && preferredOrder.bagWeightKg > 0.0) {
                                                    val rem = max(0.0, preferredOrder.totalWeightKg - preferredOrder.dispatchedWeightKg)
                                                    ceil(rem / preferredOrder.bagWeightKg).toLong().coerceAtLeast(1L).toString()
                                                } else "20"
                                                val initWt = (preferredOrder?.bagWeightKg ?: sku.standardBagWeightKg).toString()
                                                val initCust = preferredOrder?.customer ?: customer.trim()
                                                selectedItems = selectedItems + MultiDispatchItemDraft(
                                                    key = "stop_${k}_${UUID.randomUUID().toString().take(6)}",
                                                    sku = sku,
                                                    bagsText = initBags,
                                                    bagWtText = initWt,
                                                    customer = initCust,
                                                    drReference = "",
                                                    linkedOrder = preferredOrder,
                                                    returnedBagsText = ""
                                                )
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { addStopForSku() }
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = countInTruck > 0,
                                                    onCheckedChange = { wantChecked ->
                                                        if (wantChecked || countInTruck > 0) {
                                                            if (wantChecked) {
                                                                addStopForSku()
                                                            } else {
                                                                selectedItems = selectedItems.filterNot {
                                                                    it.sku.productName.equals(sku.productName, ignoreCase = true)
                                                                }
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.testTag("dispatch_quick_sku_check_$k")
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (countInTruck > 0) "${sku.productName} ($countInTruck in truck)" else sku.productName,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = buildString {
                                                            append("${sku.section} • Avail: ${OperationsCalculator.formatKg(avail)} kg")
                                                            if (skuOrdersCount > 0) append(" • $skuOrdersCount open PO(s)")
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (avail <= 0.0) StatusCriticalTextDark else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                IconButton(
                                                    onClick = addStopForSku,
                                                    modifier = Modifier.size(48.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Add,
                                                        contentDescription = "Add stop for ${sku.productName}",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                            HorizontalDivider()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Selected Dispatch Stops Draft Cards (per-stop customer, order, DR, qty)
                    if (selectedItems.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedItems.forEachIndexed { stopIdx, item ->
                                val itemBags = item.bagsText.toLongOrNull() ?: 0L
                                val itemWt = item.bagWtText.toDoubleOrNull() ?: 0.0
                                val itemGrossKg = round(itemBags * itemWt * 100.0) / 100.0
                                val itemRetBags = if (hasReturn && returnConfirmed) (item.returnedBagsText.toLongOrNull() ?: 0L) else 0L
                                val itemRetKg = round(itemRetBags * itemWt * 100.0) / 100.0
                                val itemNetKg = max(0.0, round((itemGrossKg - itemRetKg) * 100.0) / 100.0)

                                val prodKey = item.sku.productName.trim().lowercase()
                                val itemAvailKg = max(0.0, availByKey[skuStableKey(item.sku)] ?: availByName[prodKey] ?: 0.0)
                                val itemAvailBags = if (itemWt > 0.0) itemAvailKg / itemWt else 0.0
                                val combinedSkuNetKg = totalNetKgByProduct[prodKey] ?: itemNetKg
                                val otherStopsNetKg = max(0.0, combinedSkuNetKg - itemNetKg)
                                val remainingAvailForThisStopKg = max(0.0, itemAvailKg - otherStopsNetKg)
                                val itemMaxWholeBags = if (itemWt > 0.0) {
                                    floor((remainingAvailForThisStopKg + 0.01) / itemWt).toLong().coerceAtLeast(0L)
                                } else 0L
                                val skuShortKg = max(0.0, combinedSkuNetKg - itemAvailKg)
                                val itemOverStock = itemBags > 0L && itemWt > 0.0 && combinedSkuNetKg > itemAvailKg + 0.01
                                val sameSkuStopCount = selectedItems.count { it.sku.productName.equals(item.sku.productName, ignoreCase = true) }

                                val effectiveStopCustomer = item.customer.trim().ifBlank { customer.trim() }
                                val itemMatchingOrders = openOrders.filter {
                                    it.productName.equals(item.sku.productName, ignoreCase = true)
                                }.sortedWith(
                                    compareByDescending<CustomerOrder> {
                                        effectiveStopCustomer.isNotBlank() && it.customer.equals(effectiveStopCustomer, ignoreCase = true)
                                    }.thenByDescending { it.priority }
                                        .thenBy { it.dueDate }
                                )

                                val isStopOptionsExpanded = item.key in expandedStopOptionsKeys
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        width = if (itemOverStock) 1.5.dp else 1.dp,
                                        color = if (itemOverStock) StatusCritical else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Minimized Stop Header: Product + Plant/Avail + More/Remove
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${stopIdx + 1}. ${item.sku.productName}",
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = buildString {
                                                        append("${item.sku.section} • ${OperationsCalculator.formatBags(itemAvailBags)}b left")
                                                        item.linkedOrder?.let { append(" • ${it.orderCode}") }
                                                        if (item.linkedOrder == null && item.addAsRushOrder) append(" • Rush")
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (itemOverStock) StatusCriticalTextDark else StatusHealthyTextDark,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                TextButton(
                                                    onClick = {
                                                        expandedStopOptionsKeys = if (isStopOptionsExpanded) {
                                                            expandedStopOptionsKeys - item.key
                                                        } else {
                                                            expandedStopOptionsKeys + item.key
                                                        }
                                                    },
                                                    modifier = Modifier.testTag("dispatch_stop_more_${item.key}")
                                                ) {
                                                    Text(if (isStopOptionsExpanded) "Less" else "More")
                                                    Icon(
                                                        imageVector = if (isStopOptionsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        selectedItems = selectedItems.filterNot { it.key == item.key }
                                                    },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("dispatch_remove_stop_${item.key}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Delete,
                                                        contentDescription = "Remove stop ${stopIdx + 1}",
                                                        tint = StatusCritical,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Essential Stop Row: Customer + Bags
                                        val isCustMenuOpen = expandedCustomerDropdownKey == item.key
                                        val stopCustOptions = remember(item.customer, allDispatchCustomers) {
                                            val q = item.customer.trim()
                                            val exact = allDispatchCustomers.any { it.equals(q, ignoreCase = true) }
                                            if (q.isBlank() || exact) allDispatchCustomers
                                            else allDispatchCustomers.filter { it.contains(q, ignoreCase = true) }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            ExposedDropdownMenuBox(
                                                expanded = isCustMenuOpen,
                                                onExpandedChange = {
                                                    expandedCustomerDropdownKey = if (isCustMenuOpen) null else item.key
                                                },
                                                modifier = Modifier.weight(1.4f)
                                            ) {
                                                OutlinedTextField(
                                                    value = item.customer,
                                                    onValueChange = { newCust ->
                                                        selectedItems = selectedItems.map {
                                                            if (it.key == item.key) it.copy(customer = newCust) else it
                                                        }
                                                        expandedCustomerDropdownKey = item.key
                                                    },
                                                    label = { Text("Customer") },
                                                    placeholder = {
                                                        Text(customer.trim().ifBlank { "Select..." })
                                                    },
                                                    singleLine = true,
                                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCustMenuOpen) },
                                                    modifier = Modifier
                                                        .menuAnchor(MenuAnchorType.PrimaryEditable)
                                                        .fillMaxWidth()
                                                        .testTag("dispatch_item_customer_${item.key}")
                                                )
                                                if (stopCustOptions.isNotEmpty()) {
                                                    ExposedDropdownMenu(
                                                        expanded = isCustMenuOpen,
                                                        onDismissRequest = { expandedCustomerDropdownKey = null }
                                                    ) {
                                                        stopCustOptions.forEach { custName ->
                                                            DropdownMenuItem(
                                                                text = { Text(custName) },
                                                                onClick = {
                                                                    val usedOrdIds = selectedItems.mapNotNull { it.linkedOrder?.id }.toSet()
                                                                    val autoMatchOrd = if (item.linkedOrder == null) {
                                                                        openOrders
                                                                            .filter {
                                                                                it.customer.trim().equals(custName, ignoreCase = true) &&
                                                                                    it.productName.equals(item.sku.productName, ignoreCase = true) &&
                                                                                    it.id !in usedOrdIds
                                                                            }
                                                                            .minByOrNull { it.orderDate }
                                                                    } else null
                                                                    val recBags = autoMatchOrd?.let { ord ->
                                                                        val rem = max(0.0, ord.totalWeightKg - ord.dispatchedWeightKg)
                                                                        if (ord.bagWeightKg > 0.0) {
                                                                            ceil(rem / ord.bagWeightKg).toLong().coerceAtLeast(1L).toString()
                                                                        } else item.bagsText
                                                                    } ?: item.bagsText
                                                                    val primaryCust = customer.trim()
                                                                        .ifBlank { selectedItems.firstOrNull()?.customer?.trim().orEmpty() }
                                                                    val isAdditionalCust = primaryCust.isNotBlank() &&
                                                                        !custName.equals(primaryCust, ignoreCase = true)
                                                                    selectedItems = selectedItems.map {
                                                                        if (it.key == item.key) {
                                                                            it.copy(
                                                                                customer = custName,
                                                                                linkedOrder = autoMatchOrd ?: it.linkedOrder,
                                                                                bagsText = recBags,
                                                                                bagWtText = autoMatchOrd?.bagWeightKg?.toString() ?: it.bagWtText,
                                                                                addAsRushOrder = if (autoMatchOrd == null && isAdditionalCust) true else it.addAsRushOrder
                                                                            )
                                                                        } else it
                                                                    }
                                                                    expandedCustomerDropdownKey = null
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            OutlinedTextField(
                                                value = item.bagsText,
                                                onValueChange = { newVal ->
                                                    selectedItems = selectedItems.map {
                                                        if (it.key == item.key) it.copy(bagsText = newVal) else it
                                                    }
                                                },
                                                label = { Text("Bags") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier
                                                    .weight(0.75f)
                                                    .testTag("dispatch_item_bags_${item.key}")
                                            )
                                        }

                                        // Quick Summary Strip
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (itemRetBags > 0L) {
                                                    "${OperationsCalculator.formatKg(itemNetKg)} kg net (${itemRetBags}b ret)"
                                                } else {
                                                    "${OperationsCalculator.formatKg(itemGrossKg)} kg (${item.bagWtText}kg/b)"
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            item.linkedOrder?.let { ord ->
                                                val ordRem = max(0.0, ord.totalWeightKg - ord.dispatchedWeightKg)
                                                Text(
                                                    text = "PO Rem: ${OperationsCalculator.formatKg(ordRem)} kg",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (hasReturn && returnConfirmed) {
                                            OutlinedTextField(
                                                value = item.returnedBagsText,
                                                onValueChange = { newVal ->
                                                    selectedItems = selectedItems.map {
                                                        if (it.key == item.key) it.copy(returnedBagsText = newVal) else it
                                                    }
                                                },
                                                label = { Text("Returned Bags (0–$itemBags)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("dispatch_item_returned_bags_${item.key}")
                                            )
                                        }

                                        val otherCustomerPrevOrders = if (effectiveStopCustomer.isNotBlank()) {
                                            openOrders
                                                .filter {
                                                    it.customer.trim().equals(effectiveStopCustomer, ignoreCase = true) &&
                                                        it.id !in connectedOrderIds
                                                }
                                                .sortedBy { it.orderDate }
                                        } else emptyList()
                                        if (otherCustomerPrevOrders.isNotEmpty()) {
                                            TextButton(
                                                onClick = {
                                                    selectedItems = selectedItems + otherCustomerPrevOrders.map { buildOrderDraft(it) }
                                                },
                                                modifier = Modifier.testTag("dispatch_stop_connect_more_${item.key}")
                                            ) {
                                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Connect ${otherCustomerPrevOrders.size} open PO(s) for $effectiveStopCustomer")
                                            }
                                        }

                                        // Secondary Stop Options (collapsed by default to minimize box height)
                                        if (isStopOptionsExpanded) {
                                            if (itemMatchingOrders.isNotEmpty()) {
                                                val isOrdMenuOpen = expandedOrderDropdownKey == item.key
                                                ExposedDropdownMenuBox(
                                                    expanded = isOrdMenuOpen,
                                                    onExpandedChange = {
                                                        expandedOrderDropdownKey = if (isOrdMenuOpen) null else item.key
                                                    }
                                                ) {
                                                    OutlinedTextField(
                                                        value = item.linkedOrder?.let { "${it.orderCode} - ${it.customer}" }
                                                            ?: "None (${itemMatchingOrders.size} open PO)",
                                                        onValueChange = {},
                                                        readOnly = true,
                                                        label = { Text("Link Customer Order") },
                                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isOrdMenuOpen) },
                                                        modifier = Modifier
                                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                                            .fillMaxWidth()
                                                            .testTag("dispatch_item_order_dropdown_${item.key}")
                                                    )
                                                    ExposedDropdownMenu(
                                                        expanded = isOrdMenuOpen,
                                                        onDismissRequest = { expandedOrderDropdownKey = null }
                                                    ) {
                                                        DropdownMenuItem(
                                                            text = { Text("None (Direct Stock-Out)") },
                                                            onClick = {
                                                                selectedItems = selectedItems.map {
                                                                    if (it.key == item.key) it.copy(linkedOrder = null) else it
                                                                }
                                                                expandedOrderDropdownKey = null
                                                            }
                                                        )
                                                        itemMatchingOrders.forEach { ord ->
                                                            val rem = max(0.0, ord.totalWeightKg - ord.dispatchedWeightKg)
                                                            val prefix = if (ord.priority) "★ " else ""
                                                            DropdownMenuItem(
                                                                text = { Text("$prefix${ord.orderCode} | ${ord.customer} (${OperationsCalculator.formatKg(rem)} kg left)") },
                                                                onClick = {
                                                                    val recBags = if (ord.bagWeightKg > 0.0) {
                                                                        ceil(rem / ord.bagWeightKg).toLong().coerceAtLeast(1L).toString()
                                                                    } else item.bagsText
                                                                    selectedItems = selectedItems.map {
                                                                        if (it.key == item.key) {
                                                                            it.copy(
                                                                                customer = ord.customer,
                                                                                linkedOrder = ord,
                                                                                bagsText = recBags,
                                                                                bagWtText = ord.bagWeightKg.toString()
                                                                            )
                                                                        } else it
                                                                    }
                                                                    expandedOrderDropdownKey = null
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            if (item.linkedOrder == null) {
                                                Surface(
                                                    color = if (item.addAsRushOrder) StatusWatchContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                    border = BorderStroke(
                                                        width = 1.dp,
                                                        color = if (item.addAsRushOrder) GrainAmber else MaterialTheme.colorScheme.outlineVariant
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedItems = selectedItems.map {
                                                                if (it.key == item.key) it.copy(addAsRushOrder = !it.addAsRushOrder) else it
                                                            }
                                                        }
                                                        .testTag("dispatch_stop_rush_toggle_${item.key}")
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Bolt,
                                                            contentDescription = null,
                                                            tint = if (item.addAsRushOrder) OnStatusWatchContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                            text = if (item.addAsRushOrder) "Rush Order ON (Adds to Orders)" else "Direct Stock-Out (Tap for Rush Order)",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (item.addAsRushOrder) OnStatusWatchContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Checkbox(
                                                            checked = item.addAsRushOrder,
                                                            onCheckedChange = { checked ->
                                                                selectedItems = selectedItems.map {
                                                                    if (it.key == item.key) it.copy(addAsRushOrder = checked) else it
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                    value = item.bagWtText,
                                                    onValueChange = { newVal ->
                                                        selectedItems = selectedItems.map {
                                                            if (it.key == item.key) it.copy(bagWtText = newVal) else it
                                                        }
                                                    },
                                                    label = { Text("Bag Wt (kg)") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(0.9f)
                                                )
                                                OutlinedTextField(
                                                    value = item.drReference,
                                                    onValueChange = { newDr ->
                                                        selectedItems = selectedItems.map {
                                                            if (it.key == item.key) it.copy(drReference = newDr) else it
                                                        }
                                                    },
                                                    label = { Text("Stop DR #") },
                                                    placeholder = { Text(drRef.ifBlank { "Shared" }) },
                                                    singleLine = true,
                                                    modifier = Modifier
                                                        .weight(1.1f)
                                                        .testTag("dispatch_item_dr_${item.key}")
                                                )
                                            }

                                            TextButton(
                                                onClick = {
                                                    val dup = MultiDispatchItemDraft(
                                                        key = "stop_${skuStableKey(item.sku)}_${UUID.randomUUID().toString().take(6)}",
                                                        sku = item.sku,
                                                        bagsText = "20",
                                                        bagWtText = item.sku.standardBagWeightKg.toString(),
                                                        customer = "",
                                                        drReference = "",
                                                        linkedOrder = null,
                                                        returnedBagsText = ""
                                                    )
                                                    selectedItems = selectedItems + dup
                                                },
                                                modifier = Modifier.testTag("dispatch_duplicate_stop_${item.key}")
                                            ) {
                                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Add Another Stop for This Product")
                                            }
                                        }

                                        if (itemOverStock) {
                                            Surface(
                                                color = StatusCriticalContainer,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = if (sameSkuStopCount > 1) {
                                                            "Truckload for ${item.sku.productName} (${OperationsCalculator.formatKg(combinedSkuNetKg)} kg across $sameSkuStopCount stops) short by ${OperationsCalculator.formatKg(skuShortKg)} kg"
                                                        } else {
                                                            "Short by ${OperationsCalculator.formatKg(skuShortKg)} kg"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = OnStatusCriticalContainer,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    if (itemMaxWholeBags > 0L) {
                                                        TextButton(
                                                            onClick = {
                                                                selectedItems = selectedItems.map {
                                                                    if (it.key == item.key) it.copy(bagsText = itemMaxWholeBags.toString()) else it
                                                                }
                                                            },
                                                            modifier = Modifier.testTag("use_max_available_bags_${item.key}")
                                                        ) {
                                                            Text("Use Max ($itemMaxWholeBags)")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val batchCustomerList = selectedItems
                            .map { it.customer.trim().ifBlank { customer.trim() } }
                            .filter { it.isNotBlank() }
                            .distinctBy { it.lowercase() }
                        val distinctCustomersCount = batchCustomerList.size
                        val linkedOrderIdsCount = selectedItems.mapNotNull { it.linkedOrder?.id }.distinct().size
                        val otherCustomersWithOrdersCount = openOrders
                            .filter { ord ->
                                ord.id !in connectedOrderIds &&
                                    batchCustomerList.none { it.equals(ord.customer.trim(), ignoreCase = true) }
                            }
                            .map { it.customer.trim().lowercase() }
                            .filter { it.isNotBlank() }
                            .distinct()
                            .size
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dispatch_stock_status_banner")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${selectedItems.size} stop(s) • $distinctCustomersCount customer(s) • $linkedOrderIdsCount order(s) • $multiTotalBags bags • ${OperationsCalculator.formatKg(multiTotalGrossKg)} kg",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (hasReturn && returnConfirmed && multiTotalReturnedKg > 0.0) {
                                    Text(
                                        text = "Returned: ${OperationsCalculator.formatKg(multiTotalReturnedKg)} kg → Net Delivered: ${OperationsCalculator.formatKg(multiTotalNetKg)} kg",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        // Compact Add Customer to Batch Bar
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dispatch_ask_add_customer_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { showAddCustomerToBatchDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dispatch_add_customer_to_batch_button")
                                ) {
                                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Customer", maxLines = 1)
                                }
                                FilterChip(
                                    selected = extraCustomerPromptAnswered,
                                    onClick = { extraCustomerPromptAnswered = !extraCustomerPromptAnswered },
                                    label = {
                                        Text(
                                            text = if (extraCustomerPromptAnswered) "Batch Done ✓" else "No More",
                                            maxLines = 1
                                        )
                                    },
                                    modifier = Modifier.testTag("dispatch_no_more_customers_button")
                                )
                            }
                        }
                    }
                } else {
                    // EDIT EXISTING DISPATCH: Searchable single SKU picker + Order link + Stock banner
                    OutlinedButton(
                        onClick = { showEditSingleSkuPicker = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dispatch_item_input")
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (productName.isBlank()) "Search & Select Product SKU" else productName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Change", style = MaterialTheme.typography.labelSmall)
                    }

                    val matchingOrders = openOrders.filter {
                        it.productName.equals(productName, ignoreCase = true) &&
                            (it.status == OrderStatus.OPEN.label || it.status == OrderStatus.PARTIALLY_DISPATCHED.label || it.id == selectedOrder?.id)
                    }
                    if (matchingOrders.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = orderExpanded,
                            onExpandedChange = { orderExpanded = !orderExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedOrder?.let { "${it.orderCode} - ${it.customer}" } ?: "None (Direct Stock-Out)",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Link Customer Order (Optional)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orderExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = orderExpanded,
                                onDismissRequest = { orderExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("None (Direct Stock-Out)") },
                                    onClick = {
                                        selectedOrder = null
                                        orderExpanded = false
                                    }
                                )
                                matchingOrders.forEach { ord ->
                                    val rem = max(0.0, ord.totalWeightKg - ord.dispatchedWeightKg)
                                    DropdownMenuItem(
                                        text = { Text("${ord.orderCode} | ${ord.customer} (${OperationsCalculator.formatKg(rem)} kg left)") },
                                        onClick = {
                                            selectedOrder = ord
                                            customer = ord.customer
                                            bagWeightText = ord.bagWeightKg.toString()
                                            bagsText = ceil(rem / ord.bagWeightKg).toLong().coerceAtLeast(1L).toString()
                                            orderExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bagsText,
                            onValueChange = { bagsText = it },
                            label = { Text("Bags Dispatched") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dispatch_bags_input")
                        )
                        OutlinedTextField(
                            value = bagWeightText,
                            onValueChange = { bagWeightText = it },
                            label = { Text("Bag Wt (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Dispatched Weight: ${OperationsCalculator.formatKg(computedTotalKg)} kg",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (hasReturn && returnConfirmed && returnedBagsVal > 0L) {
                                Text(
                                    text = "Returned: $returnedBagsVal bags (${OperationsCalculator.formatKg(computedReturnedKg)} kg) → Net Delivered: ${OperationsCalculator.formatKg(netDeliveredKg)} kg",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Surface(
                        color = if (singleExceedsAvailableStock) StatusCriticalContainer else StatusHealthyContainer,
                        border = if (singleExceedsAvailableStock) BorderStroke(1.dp, StatusCritical) else null,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dispatch_stock_status_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (singleExceedsAvailableStock) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = "Insufficient stock warning",
                                        tint = OnStatusCriticalContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = if (singleExceedsAvailableStock) {
                                        "FLAGGED: Exceeds Available Stock"
                                    } else {
                                        "Available Stock: ${OperationsCalculator.formatKg(availableKg)} kg (${OperationsCalculator.formatBags(availableBags)} bags)"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (singleExceedsAvailableStock) OnStatusCriticalContainer else OnStatusHealthyContainer
                                )
                            }
                            if (singleExceedsAvailableStock) {
                                Text(
                                    text = "Available: ${OperationsCalculator.formatKg(availableKg)} kg (${OperationsCalculator.formatBags(availableBags)} bags) • Backlogs: -${OperationsCalculator.formatKg(singleShortfallKg)} kg ($singleShortfallBags bags needed)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnStatusCriticalContainer
                                )
                                if (maxWholeBagsAvailable > 0L) {
                                    TextButton(
                                        onClick = { bagsText = maxWholeBagsAvailable.toString() },
                                        modifier = Modifier.testTag("use_max_available_bags_button")
                                    ) {
                                        Text("Use Max Available ($maxWholeBagsAvailable bags)")
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()
                // Collapsible Logistics Section (Driver / Truck / Helper / Notes)
                val logisticsSummary = buildList {
                    if (truckNumber.isNotBlank()) add(truckNumber.trim())
                    if (driverName.isNotBlank()) add(driverName.trim())
                    if (helperName.isNotBlank()) add(helperName.trim())
                    if (remarks.isNotBlank()) add("Notes")
                }.joinToString(" • ")

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLogisticsExpanded = !showLogisticsExpanded }
                        .testTag("dispatch_logistics_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Driver & Truck Details",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = logisticsSummary.ifBlank { "Optional — tap to add driver, plate or notes" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = if (showLogisticsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Toggle driver and truck details"
                        )
                    }
                }

                if (showLogisticsExpanded) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = driverName,
                            onValueChange = { driverName = it },
                            label = { Text("Driver") },
                            placeholder = { Text("e.g., Juan Dela Cruz") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dispatch_driver_input")
                        )
                        OutlinedTextField(
                            value = truckNumber,
                            onValueChange = { truckNumber = it },
                            label = { Text("Truck Plate") },
                            placeholder = { Text("e.g., ABC-1234") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dispatch_truck_input")
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = helperName,
                            onValueChange = { helperName = it },
                            label = { Text("Helper") },
                            placeholder = { Text("Optional") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dispatch_helper_input")
                        )
                        OutlinedTextField(
                            value = remarks,
                            onValueChange = { remarks = it },
                            label = { Text("Remarks") },
                            placeholder = { Text("Route notes") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dispatch_remarks_input")
                        )
                    }
                }

                // Compact 1-Row Return & Hold Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = if (hasReturn) StatusWatchContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (hasReturn) BorderStroke(1.dp, GrainAmber) else null,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                hasReturn = !hasReturn
                                if (!hasReturn) returnConfirmed = false
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Return?",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (hasReturn) OnStatusWatchContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = hasReturn,
                                onCheckedChange = { checked ->
                                    hasReturn = checked
                                    if (!checked) returnConfirmed = false
                                },
                                modifier = Modifier.testTag("dispatch_has_return_switch")
                            )
                        }
                    }

                    Surface(
                        color = if (isHeld) StatusCriticalContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isHeld) BorderStroke(1.dp, StatusCritical) else null,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isHeld = !isHeld }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Hold?",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isHeld) OnStatusCriticalContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = isHeld,
                                onCheckedChange = { isHeld = it },
                                modifier = Modifier.testTag("dispatch_held_switch")
                            )
                        }
                    }
                }

                if (hasReturn) {
                    Surface(
                        color = StatusWatchContainer,
                        border = BorderStroke(1.dp, GrainAmber),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    returnConfirmed = !returnConfirmed
                                    if (returnConfirmed) {
                                        if (isEditing && returnedBagsText.isBlank()) {
                                            returnedBagsText = bagsVal.coerceAtLeast(1L).toString()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (returnConfirmed) ForestEmerald else GrainAmber,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dispatch_confirm_return_button")
                            ) {
                                Icon(
                                    imageVector = if (returnConfirmed) Icons.Filled.CheckCircle else Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (returnConfirmed) "Return Confirmed ✓" else "Confirm Return")
                            }

                            if (returnConfirmed) {
                                if (isEditing) {
                                    OutlinedTextField(
                                        value = returnedBagsText,
                                        onValueChange = { returnedBagsText = it },
                                        label = { Text("Returned Bags (max $bagsVal)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("dispatch_returned_bags_input")
                                    )
                                } else {
                                    Text(
                                        text = "Enter returned bags on each affected product card above.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OnStatusWatchContainer
                                    )
                                }
                                Text(
                                    text = "Why was it returned?",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = OnStatusWatchContainer
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    DispatchReturnReasons.options.forEach { reasonOpt ->
                                        FilterChip(
                                            selected = returnReason == reasonOpt,
                                            onClick = { returnReason = reasonOpt },
                                            label = { Text(reasonOpt) },
                                            modifier = Modifier.testTag("dispatch_return_reason_${reasonOpt.lowercase().replace(" ", "_")}")
                                        )
                                    }
                                }
                                if (returnReason == "Other") {
                                    OutlinedTextField(
                                        value = customReturnReason,
                                        onValueChange = { customReturnReason = it },
                                        label = { Text("Specify Return Reason") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("dispatch_custom_return_reason_input")
                                    )
                                }

                                Surface(
                                    color = if (sendToReprocess) StatusCriticalContainer.copy(alpha = 0.72f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (sendToReprocess) StatusCritical else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { sendToReprocess = !sendToReprocess }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Send to Reprocess Tab (Fast Action)",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (sendToReprocess) OnStatusCriticalContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (sendToReprocess) {
                                                    "Quarantines returned bags & flags urgent reprocessing"
                                                } else {
                                                    "Keep in dispatch log only (queue manually later)"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (sendToReprocess) OnStatusCriticalContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = sendToReprocess,
                                            onCheckedChange = { sendToReprocess = it },
                                            modifier = Modifier.testTag("dispatch_send_reprocess_switch")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isHeld) {
                    Surface(
                        color = StatusCriticalContainer,
                        border = BorderStroke(1.dp, StatusCritical),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Awaiting Payment", "Customer Closed", "Truck Issue", "DR Pending").forEach { preset ->
                                    FilterChip(
                                        selected = heldRemarks.equals(preset, ignoreCase = true),
                                        onClick = { heldRemarks = preset },
                                        label = { Text(preset) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = heldRemarks,
                                onValueChange = { heldRemarks = it },
                                label = { Text("Held Remarks (Required)") },
                                placeholder = { Text("Why is this dispatch held?") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dispatch_held_remarks_input")
                            )
                        }
                    }
                }

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val normDate = normalizeIsoDateOrNull(date)
                    if (isEditing) {
                        when {
                            normDate == null -> error = "Use YYYY-MM-DD format"
                            productName.isBlank() -> error = "Select a product SKU"
                            bagsVal !in 1..1_000_000 -> error = "Enter bags between 1 and 1,000,000"
                            bagWtVal <= 0.0 || bagWtVal > 2000.0 -> error = "Bag weight must be between 0.01 and 2,000 kg"
                            computedTotalKg <= 0.0 || computedTotalKg > 50_000_000.0 -> error = "Total weight must be <= 50,000,000 kg"
                            drRef.trim().length !in 1..80 -> error = "DR Number must be 1–80 characters"
                            customer.trim().length !in 1..120 -> error = "Customer must be 1–120 characters"
                            hasReturn && !returnConfirmed -> error = "Tap Confirm Return or switch off product returns"
                            hasReturn && returnConfirmed && returnedBagsVal !in 1..bagsVal -> error = "Returned bags must be 1–$bagsVal"
                            hasReturn && returnConfirmed && resolvedReturnReason.isBlank() -> error = "Select or enter why products were returned"
                            isHeld && heldRemarks.trim().isBlank() -> error = "Enter held remarks explaining why dispatch is held"
                            singleExceedsAvailableStock -> {
                                error = null
                                showShortageFlagDialog = true
                            }
                            else -> onSubmit(
                                DispatchEntryInput(
                                    date = normDate,
                                    productName = productName,
                                    bags = bagsVal,
                                    bagWeightKg = bagWtVal,
                                    totalWeightKg = computedTotalKg,
                                    drReference = drRef.trim(),
                                    customer = customer.trim(),
                                    linkedOrder = selectedOrder,
                                    driverName = driverName.trim(),
                                    truckNumber = truckNumber.trim(),
                                    helperName = helperName.trim(),
                                    remarks = remarks.trim(),
                                    hasReturn = hasReturn,
                                    returnConfirmed = hasReturn && returnConfirmed,
                                    returnedBags = returnedBagsVal,
                                    returnedWeightKg = computedReturnedKg,
                                    returnReason = resolvedReturnReason,
                                    isHeld = isHeld,
                                    heldRemarks = heldRemarks.trim(),
                                    sendReturnedToReprocess = hasReturn && returnConfirmed && sendToReprocess,
                                    sourcePlant = selectedSourcePlant.ifBlank { matchedSku?.section.orEmpty() }
                                )
                            )
                        }
                    } else {
                        var stopValidationError: String? = null
                        val parsedEntries = mutableListOf<DispatchEntryInput>()
                        selectedItems.forEachIndexed { idx, item ->
                            val b = item.bagsText.toLongOrNull() ?: 0L
                            val w = item.bagWtText.toDoubleOrNull() ?: 0.0
                            val totKg = round(b * w * 100.0) / 100.0
                            val rb = if (hasReturn && returnConfirmed) (item.returnedBagsText.toLongOrNull() ?: 0L) else 0L
                            val rKg = round(rb * w * 100.0) / 100.0
                            val resolvedCustomer = item.customer.trim().ifBlank { customer.trim() }
                            val resolvedDr = item.drReference.trim().ifBlank { drRef.trim() }

                            when {
                                resolvedCustomer.length !in 1..120 -> {
                                    if (stopValidationError == null) {
                                        stopValidationError = "Stop ${idx + 1} (${item.sku.productName}): Enter a customer (1–120 chars) or set Default Customer"
                                    }
                                }
                                resolvedDr.length !in 1..80 -> {
                                    if (stopValidationError == null) {
                                        stopValidationError = "Stop ${idx + 1} (${item.sku.productName}): DR Number must be 1–80 characters"
                                    }
                                }
                                b !in 1..1_000_000 || w <= 0.0 || w > 2000.0 || totKg <= 0.0 || totKg > 50_000_000.0 || rb !in 0..b -> {
                                    if (stopValidationError == null) {
                                        stopValidationError = "Stop ${idx + 1} (${item.sku.productName}): Check bags (1–1,000,000), bag weight (0.01–2,000 kg) & returned bags"
                                    }
                                }
                                else -> {
                                    parsedEntries.add(
                                        DispatchEntryInput(
                                            date = normDate ?: "",
                                            productName = item.sku.productName.trim(),
                                            bags = b,
                                            bagWeightKg = w,
                                            totalWeightKg = totKg,
                                            drReference = resolvedDr,
                                            customer = resolvedCustomer,
                                            linkedOrder = item.linkedOrder,
                                            driverName = driverName.trim(),
                                            truckNumber = truckNumber.trim(),
                                            helperName = helperName.trim(),
                                            remarks = remarks.trim(),
                                            hasReturn = hasReturn,
                                            returnConfirmed = hasReturn && returnConfirmed && rb > 0L,
                                            returnedBags = rb,
                                            returnedWeightKg = rKg,
                                            returnReason = if (rb > 0L) resolvedReturnReason else "",
                                            isHeld = isHeld,
                                            heldRemarks = heldRemarks.trim(),
                                            createRushOrderIfUnlinked = item.linkedOrder == null && item.addAsRushOrder,
                                            sendReturnedToReprocess = hasReturn && returnConfirmed && rb > 0L && sendToReprocess,
                                            sourcePlant = selectedSourcePlant.ifBlank { item.sku.section }
                                        )
                                    )
                                }
                            }
                        }
                        val totalRetAcrossItems = parsedEntries.sumOf { it.returnedBags }
                        val batchCustomerSet = parsedEntries
                            .map { it.customer.trim() }
                            .filter { it.isNotBlank() }
                            .distinctBy { it.lowercase() }
                        when {
                            normDate == null -> error = "Use YYYY-MM-DD format"
                            selectedItems.isEmpty() -> error = "Add at least one stop or product to dispatch"
                            stopValidationError != null -> error = stopValidationError
                            parsedEntries.size != selectedItems.size -> error = "Check quantities, customer, and DR number for every stop"
                            hasReturn && !returnConfirmed -> error = "Tap Confirm Return or switch off product returns"
                            hasReturn && returnConfirmed && totalRetAcrossItems <= 0L -> error = "Enter returned bags on at least one stop"
                            hasReturn && returnConfirmed && resolvedReturnReason.isBlank() -> error = "Select or enter why products were returned"
                            isHeld && heldRemarks.trim().isBlank() -> error = "Enter held remarks explaining why dispatch is held"
                            multiExceedsAvailableStock -> {
                                error = null
                                showShortageFlagDialog = true
                            }
                            !extraCustomerPromptAnswered && batchCustomerSet.size <= 1 -> {
                                error = null
                                pendingBatchEntriesForAsk = parsedEntries
                                showAskAddCustomerDialog = true
                            }
                            onSubmitMultiple != null -> onSubmitMultiple(parsedEntries)
                            else -> onSubmit(parsedEntries.first())
                        }
                    }
                },
                colors = if (anyExceedsStock) {
                    ButtonDefaults.buttonColors(containerColor = StatusCritical, contentColor = Color.White)
                } else {
                    ButtonDefaults.buttonColors(containerColor = ForestEmerald, contentColor = Color.White)
                },
                modifier = Modifier.testTag("submit_dispatch_button")
            ) {
                Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    when {
                        isEditing -> "Save Changes"
                        selectedItems.size > 1 -> "Dispatch ${selectedItems.size} Stops"
                        else -> "Confirm Dispatch"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showAskAddCustomerDialog && !isEditing) {
        val currentCustNames = pendingBatchEntriesForAsk
            .map { it.customer.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
        val currentOrderIdsInBatch = selectedItems.mapNotNull { it.linkedOrder?.id }.toSet()
        val waitingOtherCustomers = openOrders
            .filter { ord ->
                ord.id !in currentOrderIdsInBatch &&
                    currentCustNames.none { it.equals(ord.customer.trim(), ignoreCase = true) }
            }
            .map { it.customer.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }

        AlertDialog(
            onDismissRequest = { showAskAddCustomerDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = "Add another customer",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Add Another Customer to Batch?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Currently dispatching ${pendingBatchEntriesForAsk.size} stop(s) for ${currentCustNames.firstOrNull() ?: "1 customer"}. Is there any other customer you want to add to this dispatched batch?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (waitingOtherCustomers.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${waitingOtherCustomers.size} other customer(s) have open orders waiting: ${waitingOtherCustomers.take(3).joinToString(", ")}${if (waitingOtherCustomers.size > 3) "..." else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                showAskAddCustomerDialog = false
                                showAddCustomerToBatchDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ask_batch_add_customer_yes")
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Yes, Add Another Customer")
                        }
                        Button(
                            onClick = {
                                extraCustomerPromptAnswered = true
                                showAskAddCustomerDialog = false
                                val toSubmit = pendingBatchEntriesForAsk
                                if (toSubmit.isNotEmpty()) {
                                    if (onSubmitMultiple != null) onSubmitMultiple(toSubmit)
                                    else onSubmit(toSubmit.first())
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForestEmerald,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ask_batch_add_customer_no")
                        ) {
                            Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("No, Dispatch Batch Now")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAskAddCustomerDialog = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }

    if (showAddCustomerToBatchDialog && !isEditing) {
        val dispatchCustomerList = remember(customers, existingCustomers, openOrders) {
            val baseList = if (customers.isNotEmpty()) customers.map { it.name } else PlantCustomers.directory
            (baseList + existingCustomers + openOrders.map { it.customer })
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
        }
        val currentOrderIds = selectedItems.mapNotNull { it.linkedOrder?.id }.toSet()
        val currentCustomersLower = selectedItems
            .map { it.customer.trim().ifBlank { customer.trim() }.lowercase() }
            .filter { it.isNotBlank() }
            .toSet()

        AddCustomerToBatchDialog(
            allCustomers = dispatchCustomerList,
            openOrders = openOrders,
            skus = skus,
            summaries = summaries,
            alreadyConnectedOrderIds = currentOrderIds,
            currentBatchCustomers = currentCustomersLower,
            priorityProductKeys = priorityProductKeys,
            restrictToPriorityKeys = restrictToSelectedProducts,
            sourcePlantFilter = selectedSourcePlant,
            onDismiss = { showAddCustomerToBatchDialog = false },
            onConfirmAdd = { chosenCustomer, chosenOrders, chosenDirectSkus, addDirectAsRushOrder ->
                val orderDrafts = chosenOrders.map { buildOrderDraft(it) }
                val directDrafts = chosenDirectSkus.map { sku ->
                    MultiDispatchItemDraft(
                        key = "stop_${skuStableKey(sku)}_${UUID.randomUUID().toString().take(6)}",
                        sku = sku,
                        bagsText = "20",
                        bagWtText = sku.standardBagWeightKg.toString(),
                        customer = chosenCustomer,
                        drReference = "",
                        linkedOrder = null,
                        returnedBagsText = "",
                        addAsRushOrder = addDirectAsRushOrder
                    )
                }
                val addedStops = orderDrafts + directDrafts
                selectedItems = selectedItems + addedStops
                if (customer.isBlank() && selectedItems.size == addedStops.size) {
                    customer = chosenCustomer
                }
                if (addedStops.isNotEmpty()) {
                    extraCustomerPromptAnswered = true
                }
                showAddCustomerToBatchDialog = false
            }
        )
    }

    if (showMultiOrderPicker && !isEditing) {
        val currentOrderIds = selectedItems.mapNotNull { it.linkedOrder?.id }.toSet()
        MultiOrderDispatchPickerDialog(
            openOrders = openOrders,
            initiallySelectedOrderIds = currentOrderIds,
            onDismiss = { showMultiOrderPicker = false },
            onConfirm = { pickedOrders ->
                val pickedIds = pickedOrders.map { it.id }.toSet()
                val existingByOrderId = selectedItems
                    .filter { it.linkedOrder != null }
                    .associateBy { it.linkedOrder!!.id }
                val directStops = selectedItems.filter { it.linkedOrder == null }
                val orderStops = pickedOrders.map { ord ->
                    existingByOrderId[ord.id] ?: run {
                        val resolvedSku = skuByNameLower[ord.productName.lowercase()] ?: SkuEntity(
                            productName = ord.productName,
                            section = PlantSection.SAN_ROQUE.label,
                            standardBagWeightKg = ord.bagWeightKg.coerceAtLeast(1.0),
                            defaultRemarks = PlantLineRemarks.options.first()
                        )
                        val remKg = max(0.0, ord.totalWeightKg - ord.dispatchedWeightKg)
                        val bags = if (ord.bagWeightKg > 0.0) {
                            ceil(remKg / ord.bagWeightKg).toLong().coerceAtLeast(1L)
                        } else 20L
                        MultiDispatchItemDraft(
                            key = "ord_${ord.id}",
                            sku = resolvedSku,
                            bagsText = bags.toString(),
                            bagWtText = ord.bagWeightKg.toString(),
                            customer = ord.customer,
                            drReference = "",
                            linkedOrder = ord,
                            returnedBagsText = ""
                        )
                    }
                }
                selectedItems = orderStops + directStops.filter { it.linkedOrder?.id !in pickedIds }
                showMultiOrderPicker = false
            }
        )
    }

    if (showMultiSkuPicker && !isEditing) {
        MultiSkuPickerDialog(
            title = "Select Available Products",
            skus = skus,
            summaries = summaries,
            initiallySelectedKeys = emptySet(),
            onlyAvailablePositiveStock = true,
            allowedPriorityKeys = priorityProductKeys,
            restrictToAllowedKeys = restrictToSelectedProducts,
            initialSectionFilter = selectedSourcePlant.ifBlank { "ALL" },
            onDismiss = { showMultiSkuPicker = false },
            onConfirm = { pickedSkus ->
                val newStops = pickedSkus.map { sku ->
                    val k = skuStableKey(sku)
                    val usedOrderIds = selectedItems.mapNotNull { it.linkedOrder?.id }.toSet()
                    val preferredOrder = openOrders
                        .filter {
                            it.productName.equals(sku.productName, ignoreCase = true) &&
                                it.id !in usedOrderIds
                        }
                        .sortedWith(
                            compareByDescending<CustomerOrder> {
                                customer.isNotBlank() && it.customer.equals(customer.trim(), ignoreCase = true)
                            }.thenByDescending { it.priority }
                        )
                        .firstOrNull {
                            customer.isNotBlank() && it.customer.equals(customer.trim(), ignoreCase = true)
                        }
                    val initBags = if (preferredOrder != null && preferredOrder.bagWeightKg > 0.0) {
                        val rem = max(0.0, preferredOrder.totalWeightKg - preferredOrder.dispatchedWeightKg)
                        ceil(rem / preferredOrder.bagWeightKg).toLong().coerceAtLeast(1L).toString()
                    } else "20"
                    MultiDispatchItemDraft(
                        key = "stop_${k}_${UUID.randomUUID().toString().take(6)}",
                        sku = sku,
                        bagsText = initBags,
                        bagWtText = (preferredOrder?.bagWeightKg ?: sku.standardBagWeightKg).toString(),
                        customer = preferredOrder?.customer ?: customer.trim(),
                        drReference = "",
                        linkedOrder = preferredOrder,
                        returnedBagsText = ""
                    )
                }
                selectedItems = selectedItems + newStops
                showMultiSkuPicker = false
            }
        )
    }

    if (showEditSingleSkuPicker && isEditing) {
        val currentKey = matchedSku?.let { skuStableKey(it) }?.let { setOf(it) } ?: emptySet()
        MultiSkuPickerDialog(
            title = "Select Available Product",
            skus = skus,
            summaries = summaries,
            initiallySelectedKeys = currentKey,
            singleSelection = true,
            onlyAvailablePositiveStock = true,
            allowedPriorityKeys = priorityProductKeys,
            restrictToAllowedKeys = restrictToSelectedProducts,
            initialSectionFilter = selectedSourcePlant.ifBlank { "ALL" },
            onDismiss = { showEditSingleSkuPicker = false },
            onConfirm = { pickedSkus ->
                pickedSkus.firstOrNull()?.let { picked ->
                    productName = picked.productName
                    bagWeightText = picked.standardBagWeightKg.toString()
                    if (selectedSourcePlant.isBlank()) {
                        selectedSourcePlant = picked.section
                    }
                    if (selectedOrder?.productName?.equals(picked.productName, ignoreCase = true) != true) {
                        selectedOrder = null
                    }
                }
                showEditSingleSkuPicker = false
            }
        )
    }

    if (showShortageFlagDialog) {
        val seeds = if (isEditing) {
            val resolvedSku = matchedSku ?: SkuEntity(
                productName = productName,
                section = PlantSection.SAN_ROQUE.label,
                standardBagWeightKg = bagWtVal.coerceAtLeast(1.0),
                defaultRemarks = PlantLineRemarks.options.first()
            )
            listOf(ScheduleDraftSeed(resolvedSku, singleShortfallBags, selectedOrder) to singleShortfallKg)
        } else {
            multiShortageItems
        }
        AlertDialog(
            onDismissRequest = { showShortageFlagDialog = false },
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
                        text = if (seeds.size == 1 && isEditing) {
                            "Dispatching $bagsVal bags (${OperationsCalculator.formatKg(computedTotalKg)} kg) of $productName exceeds available stock of ${OperationsCalculator.formatKg(availableKg)} kg (${OperationsCalculator.formatBags(availableBags)} bags)."
                        } else {
                            "${seeds.size} selected product(s) exceed available warehouse inventory:"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = StatusCriticalContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            seeds.forEach { (seed, shortKg) ->
                                Text(
                                    text = "• ${seed.sku.productName}: Backlogs ${seed.bags} bags (-${OperationsCalculator.formatKg(shortKg)} kg)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = OnStatusCriticalContainer
                                )
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                showShortageFlagDialog = false
                                val seedList = seeds.map { it.first }
                                if (onAddMultipleProduction != null && (!isEditing || seedList.size > 1)) {
                                    onAddMultipleProduction(seedList)
                                } else {
                                    val first = seedList.first()
                                    onAddNewProduction(first.sku, first.bags, first.linkedOrder)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForestEmerald,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shortage_add_production_button")
                        ) {
                            Icon(Icons.Filled.AddTask, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            val totalNeedBags = seeds.sumOf { it.first.bags }
                            Text(
                                if (seeds.size > 1) "Add Production for ${seeds.size} Shortfalls ($totalNeedBags bags)"
                                else "Add New Production ($totalNeedBags bags)"
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                showShortageFlagDialog = false
                                onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCriticalTextDark),
                            border = BorderStroke(1.dp, StatusCriticalTextDark),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shortage_cancel_dispatch_button")
                        ) {
                            Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancel Dispatching")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showShortageFlagDialog = false }) {
                    Text("Adjust Quantity")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderEditorDialog(
    skus: List<SkuEntity>,
    summaries: List<SkuInventorySummary> = emptyList(),
    customers: List<CustomerEntity> = emptyList(),
    existingCustomers: List<String> = emptyList(),
    existingOrder: CustomerOrder? = null,
    initialRushOrder: Boolean = false,
    onDismiss: () -> Unit,
    onManageCustomers: (() -> Unit)? = null,
    onSubmitMultiple: ((
        orderCode: String,
        orderDate: String,
        dueDate: String,
        customer: String,
        items: List<NewOrderItemInput>,
        priority: Boolean,
        alsoScheduleProduction: Boolean,
        rushOrder: Boolean
    ) -> Unit)? = null,
    onSubmit: (
        orderCode: String,
        orderDate: String,
        dueDate: String,
        productName: String,
        orderedBags: Long,
        bagWeightKg: Double,
        customer: String,
        priority: Boolean,
        rushOrder: Boolean
    ) -> Unit
) {
    var orderCode by remember {
        mutableStateOf(
            existingOrder?.orderCode
                ?: if (initialRushOrder) "RUSH-2611-${(10..99).random()}" else "PO-2611-${(10..99).random()}"
        )
    }
    var orderDate by remember { mutableStateOf(existingOrder?.orderDate ?: OperationsCalculator.todayIsoDate()) }
    var dueDate by remember {
        mutableStateOf(
            existingOrder?.dueDate
                ?: if (initialRushOrder) OperationsCalculator.todayIsoDate() else OperationsCalculator.todayPlusDaysIso(3)
        )
    }
    var productName by remember { mutableStateOf(existingOrder?.productName ?: skus.firstOrNull()?.productName.orEmpty()) }
    var bagsText by remember { mutableStateOf((existingOrder?.orderedBags ?: 100L).toString()) }
    var bagWtText by remember {
        mutableStateOf(
            (existingOrder?.bagWeightKg
                ?: skus.firstOrNull { it.productName == productName }?.standardBagWeightKg
                ?: 50.0).toString()
        )
    }
    var customer by remember { mutableStateOf(existingOrder?.customer ?: "") }
    var isPriorityOrder by remember { mutableStateOf(existingOrder?.priority ?: initialRushOrder) }
    var isRushOrder by remember { mutableStateOf(existingOrder?.rushOrder ?: initialRushOrder) }
    var customerExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Multi-item order state for new orders
    var selectedItems by remember { mutableStateOf<List<MultiOrderItemDraft>>(emptyList()) }
    var showItemPicker by remember { mutableStateOf(false) }
    var alsoScheduleProduction by remember { mutableStateOf(false) }

    val availByKey = remember(summaries) {
        summaries.associate { skuStableKey(it.sku) to it.availableStockKg }
    }
    val availByName = remember(summaries) {
        summaries.associate { it.sku.productName to it.availableStockKg }
    }

    val editBags = bagsText.toLongOrNull() ?: 0L
    val editBagWt = bagWtText.toDoubleOrNull() ?: 0.0
    val editTotalKg = round(editBags * editBagWt * 100.0) / 100.0

    val multiTotalBags = selectedItems.sumOf { (it.bagsText.toLongOrNull() ?: 0L).coerceAtLeast(0L) }
    val multiTotalKg = selectedItems.sumOf {
        val b = (it.bagsText.toLongOrNull() ?: 0L).coerceAtLeast(0L)
        val w = (it.bagWtText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
        round(b * w * 100.0) / 100.0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingOrder == null) "New Customer Order" else "Edit Order ${existingOrder.orderCode}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = orderCode,
                    onValueChange = { orderCode = it },
                    enabled = existingOrder == null,
                    label = { Text("Order ID / PO Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = orderDate,
                        onValueChange = { orderDate = it },
                        enabled = existingOrder == null,
                        label = { Text("Order Date") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Due Date") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                val priorityCustomerNames = remember(customers) {
                    customers.filter { it.priority }.map { it.name.trim().lowercase() }.toSet()
                }
                val allOrderCustomers = remember(customers, existingCustomers) {
                    val baseList = if (customers.isNotEmpty()) customers.map { it.name } else PlantCustomers.directory
                    (baseList + existingCustomers)
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sortedWith(
                            compareByDescending<String> { it.lowercase() in priorityCustomerNames }
                                .thenBy { it.lowercase() }
                        )
                }
                val orderCustomerOptions = remember(customer, allOrderCustomers) {
                    val isExact = allOrderCustomers.any { it.equals(customer.trim(), ignoreCase = true) }
                    if (customer.isBlank() || isExact) allOrderCustomers
                    else allOrderCustomers.filter { it.contains(customer.trim(), ignoreCase = true) }
                }
                ExposedDropdownMenuBox(
                    expanded = customerExpanded,
                    onExpandedChange = { customerExpanded = !customerExpanded }
                ) {
                    OutlinedTextField(
                        value = customer,
                        onValueChange = {
                            customer = it
                            customerExpanded = true
                        },
                        label = { Text("Customer (${allOrderCustomers.size} listed)") },
                        placeholder = { Text("Select or search customer...") },
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .fillMaxWidth()
                            .testTag("order_customer_input")
                    )
                    if (orderCustomerOptions.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = customerExpanded,
                            onDismissRequest = { customerExpanded = false }
                        ) {
                            orderCustomerOptions.forEach { custName ->
                                val isCustPriority = custName.lowercase() in priorityCustomerNames
                                DropdownMenuItem(
                                    text = {
                                        Text(if (isCustPriority) "★ $custName (Priority)" else custName)
                                    },
                                    onClick = {
                                        customer = custName
                                        if (existingOrder == null) {
                                            isPriorityOrder = isCustPriority
                                        }
                                        customerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                if (onManageCustomers != null) {
                    TextButton(onClick = onManageCustomers) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Customers List")
                    }
                }

                Surface(
                    color = if (isPriorityOrder) StatusWatchContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isPriorityOrder) BorderStroke(1.dp, GrainAmber) else null,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPriorityOrder = !isPriorityOrder }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = if (isPriorityOrder) OnStatusWatchContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Priority Order",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isPriorityOrder) OnStatusWatchContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isPriorityOrder) "Ranked ahead of standard orders" else "Standard order priority",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPriorityOrder) OnStatusWatchContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isPriorityOrder,
                            onCheckedChange = { isPriorityOrder = it },
                            modifier = Modifier.testTag("order_priority_switch")
                        )
                    }
                }

                Surface(
                    color = if (isRushOrder) StatusCriticalContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isRushOrder) BorderStroke(1.dp, StatusCritical) else null,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val next = !isRushOrder
                            isRushOrder = next
                            if (next) isPriorityOrder = true
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = if (isRushOrder) OnStatusCriticalContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Rush Order",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isRushOrder) OnStatusCriticalContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isRushOrder) "Listed under Orders → Rush Orders tab" else "Standard scheduled order",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isRushOrder) OnStatusCriticalContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isRushOrder,
                            onCheckedChange = { next ->
                                isRushOrder = next
                                if (next) isPriorityOrder = true
                            },
                            modifier = Modifier.testTag("order_rush_switch")
                        )
                    }
                }

                if (existingOrder == null) {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Order Items (${selectedItems.size})",
                            style = MaterialTheme.typography.titleSmall
                        )
                        OutlinedButton(
                            onClick = { showItemPicker = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("order_select_items_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (selectedItems.isEmpty()) "Select Items" else "Add / Edit Items")
                        }
                    }

                    if (selectedItems.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showItemPicker = true }
                        ) {
                            Text(
                                text = "Tap Select Items to choose one or multiple feeds / raw materials for this order.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedItems.forEach { item ->
                                val itemBags = item.bagsText.toLongOrNull() ?: 0L
                                val itemWt = item.bagWtText.toDoubleOrNull() ?: 0.0
                                val itemKg = round(itemBags * itemWt * 100.0) / 100.0
                                val availKg = availByKey[item.key] ?: availByName[item.sku.productName] ?: 0.0
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.sku.productName,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${item.sku.section} • Avail: ${OperationsCalculator.formatKg(availKg)} kg",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    selectedItems = selectedItems.filterNot { it.key == item.key }
                                                },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    contentDescription = "Remove ${item.sku.productName}",
                                                    tint = StatusCritical,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = item.bagsText,
                                                onValueChange = { newVal ->
                                                    selectedItems = selectedItems.map {
                                                        if (it.key == item.key) it.copy(bagsText = newVal) else it
                                                    }
                                                },
                                                label = { Text("Bags") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = item.bagWtText,
                                                onValueChange = { newVal ->
                                                    selectedItems = selectedItems.map {
                                                        if (it.key == item.key) it.copy(bagWtText = newVal) else it
                                                    }
                                                },
                                                label = { Text("Bag Wt") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Text(
                                            text = "Item Weight: ${OperationsCalculator.formatKg(itemKg)} kg",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${selectedItems.size} item(s) • $multiTotalBags total bags • ${OperationsCalculator.formatKg(multiTotalKg)} kg",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { alsoScheduleProduction = !alsoScheduleProduction }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = alsoScheduleProduction,
                                onCheckedChange = { alsoScheduleProduction = it }
                            )
                            Text(
                                text = "Also schedule production runs for these items",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = productName,
                        onValueChange = {},
                        enabled = false,
                        label = { Text("Product Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bagsText,
                            onValueChange = { bagsText = it },
                            label = { Text("Ordered Bags") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = bagWtText,
                            onValueChange = { bagWtText = it },
                            label = { Text("Bag Wt (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = "Total Order Weight: ${OperationsCalculator.formatKg(editTotalKg)} kg",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val normOrderDate = normalizeIsoDateOrNull(orderDate)
                    val normDueDate = normalizeIsoDateOrNull(dueDate)
                    if (existingOrder != null) {
                        when {
                            orderCode.trim().length !in 1..40 -> error = "Order ID must be 1–40 characters"
                            normOrderDate == null -> error = "Order date must be YYYY-MM-DD"
                            normDueDate == null -> error = "Due date must be YYYY-MM-DD"
                            productName.trim().length !in 1..120 -> error = "Select a valid product SKU"
                            editBags !in 1..1_000_000 -> error = "Ordered bags must be 1–1,000,000"
                            editBagWt <= 0.0 || editBagWt > 2000.0 -> error = "Bag weight must be 0.01–2,000 kg"
                            editTotalKg <= 0.0 || editTotalKg > 50_000_000.0 -> error = "Total weight must be <= 50,000,000 kg"
                            customer.trim().length !in 1..120 -> error = "Customer name must be 1–120 characters"
                            else -> onSubmit(orderCode.trim(), normOrderDate, normDueDate, productName.trim(), editBags, editBagWt, customer.trim(), isPriorityOrder, isRushOrder)
                        }
                    } else {
                        val parsedItems = selectedItems.mapNotNull { draft ->
                            val b = draft.bagsText.toLongOrNull() ?: 0L
                            val w = draft.bagWtText.toDoubleOrNull() ?: 0.0
                            val kg = round(b * w * 100.0) / 100.0
                            if (b in 1..1_000_000 && w > 0.0 && w <= 2000.0 && kg > 0.0 && kg <= 50_000_000.0) {
                                NewOrderItemInput(draft.sku.productName.trim(), b, w)
                            } else {
                                null
                            }
                        }
                        when {
                            orderCode.trim().length !in 1..40 -> error = "Order ID must be 1–40 characters"
                            normOrderDate == null -> error = "Order date must be YYYY-MM-DD"
                            normDueDate == null -> error = "Due date must be YYYY-MM-DD"
                            customer.trim().length !in 1..120 -> error = "Customer name must be 1–120 characters"
                            selectedItems.isEmpty() -> error = "Select at least one feed / raw material item"
                            parsedItems.size != selectedItems.size -> error = "Ensure every item has valid bags (1–1,000,000) and bag weight (0.01–2,000 kg)"
                            onSubmitMultiple != null -> {
                                onSubmitMultiple(orderCode.trim(), normOrderDate, normDueDate, customer.trim(), parsedItems, isPriorityOrder, alsoScheduleProduction, isRushOrder)
                            }
                            else -> {
                                val first = parsedItems.first()
                                onSubmit(orderCode.trim(), normOrderDate, normDueDate, first.productName, first.orderedBags, first.bagWeightKg, customer.trim(), isPriorityOrder, isRushOrder)
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestEmerald,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("submit_order_button")
            ) {
                Text(
                    when {
                        existingOrder != null -> "Update Order"
                        selectedItems.size > 1 -> "Save ${selectedItems.size} Orders"
                        else -> "Save Order"
                    }
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showItemPicker && existingOrder == null) {
        MultiSkuPickerDialog(
            title = "Select Order Items",
            skus = skus,
            summaries = summaries,
            initiallySelectedKeys = selectedItems.map { it.key }.toSet(),
            onDismiss = { showItemPicker = false },
            onConfirm = { pickedSkus ->
                val existingByKey = selectedItems.associateBy { it.key }
                selectedItems = pickedSkus.map { sku ->
                    val k = skuStableKey(sku)
                    existingByKey[k] ?: MultiOrderItemDraft(
                        key = k,
                        sku = sku,
                        bagsText = "100",
                        bagWtText = sku.standardBagWeightKg.toString()
                    )
                }
                showItemPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleRunDialog(
    skus: List<SkuEntity>,
    summaries: List<SkuInventorySummary> = emptyList(),
    openOrders: List<CustomerOrder> = emptyList(),
    initialSku: SkuEntity? = null,
    initialBags: Long = 40L,
    initialItems: List<ScheduleDraftSeed> = emptyList(),
    initialPriority: SchedulePriority = SchedulePriority.ORDER_DRIVEN,
    linkedOrder: CustomerOrder? = null,
    onDismiss: () -> Unit,
    onSubmitMultiple: ((
        scheduledDate: String,
        priority: SchedulePriority,
        items: List<NewScheduleItemInput>
    ) -> Unit)? = null,
    onSubmit: (
        scheduledDate: String,
        section: PlantSection,
        productName: String,
        targetBags: Long,
        bagWeightKg: Double,
        lineRemarks: String,
        priority: SchedulePriority,
        linkedOrderId: String,
        linkedOrderCode: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> }
) {
    val initialDrafts = remember(initialItems, initialSku, initialBags, linkedOrder, openOrders) {
        when {
            initialItems.isNotEmpty() -> initialItems.mapIndexed { idx, seed ->
                val seedOrders = when {
                    seed.linkedOrders.isNotEmpty() -> seed.linkedOrders
                    seed.linkedOrder != null -> listOf(seed.linkedOrder)
                    else -> openOrders.filter { it.productName.equals(seed.sku.productName, ignoreCase = true) }
                }
                val ordSuffix = seedOrders.firstOrNull()?.id?.takeIf { it.isNotBlank() } ?: "idx_$idx"
                MultiScheduleItemDraft(
                    key = "${skuStableKey(seed.sku)}__${ordSuffix}_${seedOrders.size}",
                    sku = seed.sku,
                    targetBagsText = seed.bags.coerceAtLeast(1L).toString(),
                    lineRemarks = seed.sku.defaultRemarks.ifBlank { PlantLineRemarks.options.first() },
                    linkedOrderId = seedOrders.firstOrNull()?.id.orEmpty(),
                    linkedOrderCode = seedOrders.firstOrNull()?.orderCode.orEmpty(),
                    linkedCustomer = seedOrders.firstOrNull()?.customer.orEmpty(),
                    linkedOrderIds = seedOrders.map { it.id }.filter { it.isNotBlank() }.distinct(),
                    linkedOrderCodes = seedOrders.map { it.orderCode }.filter { it.isNotBlank() }.distinct(),
                    linkedCustomers = seedOrders.map { it.customer }.filter { it.isNotBlank() }.distinct()
                )
            }
            initialSku != null -> {
                val seedOrders = if (linkedOrder != null) {
                    listOf(linkedOrder)
                } else {
                    openOrders.filter { it.productName.equals(initialSku.productName, ignoreCase = true) }
                }
                listOf(
                    MultiScheduleItemDraft(
                        key = skuStableKey(initialSku),
                        sku = initialSku,
                        targetBagsText = initialBags.coerceAtLeast(20L).toString(),
                        lineRemarks = initialSku.defaultRemarks.ifBlank { PlantLineRemarks.options.first() },
                        linkedOrderId = seedOrders.firstOrNull()?.id.orEmpty(),
                        linkedOrderCode = seedOrders.firstOrNull()?.orderCode.orEmpty(),
                        linkedCustomer = seedOrders.firstOrNull()?.customer.orEmpty(),
                        linkedOrderIds = seedOrders.map { it.id }.filter { it.isNotBlank() }.distinct(),
                        linkedOrderCodes = seedOrders.map { it.orderCode }.filter { it.isNotBlank() }.distinct(),
                        linkedCustomers = seedOrders.map { it.customer }.filter { it.isNotBlank() }.distinct()
                    )
                )
            }
            else -> emptyList()
        }
    }

    var scheduledDate by remember {
        mutableStateOf(
            linkedOrder?.dueDate
                ?: initialItems.firstOrNull { it.linkedOrders.isNotEmpty() || it.linkedOrder != null }?.let {
                    it.linkedOrders.firstOrNull()?.dueDate ?: it.linkedOrder?.dueDate
                }
                ?: OperationsCalculator.todayIsoDate()
        )
    }
    var draftItems by remember { mutableStateOf(initialDrafts) }
    var priority by remember { mutableStateOf(initialPriority) }
    var priorityExpanded by remember { mutableStateOf(false) }
    var expandedLineForKey by remember { mutableStateOf<String?>(null) }
    var pickingOrdersForKey by remember { mutableStateOf<String?>(null) }
    var showSkuPicker by remember { mutableStateOf(false) }
    var showOrderSchedulePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val availByKey = remember(summaries) {
        summaries.associate { skuStableKey(it.sku) to it.availableStockKg }
    }
    val availByName = remember(summaries) {
        summaries.associate { it.sku.productName to it.availableStockKg }
    }

    val totalBags = draftItems.sumOf { (it.targetBagsText.toLongOrNull() ?: 0L).coerceAtLeast(0L) }
    val totalExpectedKg = draftItems.sumOf {
        val b = (it.targetBagsText.toLongOrNull() ?: 0L).coerceAtLeast(0L)
        round(b * it.sku.standardBagWeightKg * 100.0) / 100.0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (draftItems.size > 1) "Schedule ${draftItems.size} Production Runs"
                else "Schedule Production Run"
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val linkedLabels = remember(linkedOrder, draftItems) {
                    val fromDrafts = draftItems.flatMap { draft ->
                        if (draft.linkedOrderCodes.isNotEmpty()) {
                            draft.linkedOrderCodes.mapIndexed { idx, code ->
                                val cust = draft.linkedCustomers.getOrNull(idx).orEmpty()
                                if (cust.isNotBlank()) "$code ($cust)" else code
                            }
                        } else if (draft.linkedOrderCode.isNotBlank()) {
                            listOf(
                                if (draft.linkedCustomer.isNotBlank()) "${draft.linkedOrderCode} (${draft.linkedCustomer})"
                                else draft.linkedOrderCode
                            )
                        } else emptyList()
                    }.distinct()
                    if (fromDrafts.isNotEmpty()) {
                        fromDrafts
                    } else if (linkedOrder != null) {
                        listOf("${linkedOrder.orderCode} (${linkedOrder.customer})")
                    } else {
                        emptyList()
                    }
                }
                if (linkedLabels.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val preview = if (linkedLabels.size <= 2) {
                            "Linked Orders: ${linkedLabels.joinToString(", ")}"
                        } else {
                            "Linked Orders (${linkedLabels.size}): ${linkedLabels.take(2).joinToString(", ")} +${linkedLabels.size - 2} more"
                        }
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = scheduledDate,
                    onValueChange = { scheduledDate = it },
                    label = { Text("Scheduled Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = priorityExpanded,
                    onExpandedChange = { priorityExpanded = !priorityExpanded }
                ) {
                    OutlinedTextField(
                        value = priority.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = priorityExpanded,
                        onDismissRequest = { priorityExpanded = false }
                    ) {
                        SchedulePriority.entries.forEach { pr ->
                            DropdownMenuItem(
                                text = { Text(pr.label) },
                                onClick = {
                                    priority = pr
                                    priorityExpanded = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Production Items (${draftItems.size})",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (openOrders.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { showOrderSchedulePicker = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("schedule_select_orders_button")
                            ) {
                                Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Orders", maxLines = 1)
                            }
                        }
                        OutlinedButton(
                            onClick = { showSkuPicker = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("schedule_select_items_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (draftItems.isEmpty()) "SKUs" else "Edit SKUs", maxLines = 1)
                        }
                    }
                }

                if (draftItems.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (openOrders.isNotEmpty()) {
                                    "Select multiple customer orders or SKUs with checkboxes to schedule together:"
                                } else {
                                    "Select one or multiple product SKUs with checkboxes to schedule:"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (openOrders.isNotEmpty()) {
                                Button(
                                    onClick = { showOrderSchedulePicker = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ForestEmerald,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("empty_schedule_select_orders_button")
                                ) {
                                    Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Select Orders (Checkboxes)")
                                }
                            }
                            OutlinedButton(
                                onClick = { showSkuPicker = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("empty_schedule_select_skus_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Select SKUs (Checkboxes)")
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        draftItems.forEach { item ->
                            val bags = item.targetBagsText.toLongOrNull() ?: 0L
                            val expectedKg = round(bags * item.sku.standardBagWeightKg * 100.0) / 100.0
                            val availKg = availByKey[skuStableKey(item.sku)] ?: availByName[item.sku.productName] ?: 0.0
                            val lineOpen = expandedLineForKey == item.key
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.sku.productName,
                                                style = MaterialTheme.typography.labelLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val linkedCount = item.linkedOrderIds.size.coerceAtLeast(
                                                if (item.linkedOrderCode.isNotBlank()) 1 else 0
                                            )
                                            Text(
                                                text = buildString {
                                                    append("${item.sku.section} • Avail: ${OperationsCalculator.formatKg(availKg)} kg")
                                                    if (linkedCount == 1) {
                                                        val cCode = item.linkedOrderCodes.firstOrNull() ?: item.linkedOrderCode
                                                        val cCust = item.linkedCustomers.firstOrNull() ?: item.linkedCustomer
                                                        append("\nLinked: $cCode")
                                                        if (cCust.isNotBlank()) append(" • For: $cCust")
                                                    } else if (linkedCount > 1) {
                                                        append("\nLinked: $linkedCount customer orders")
                                                    }
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                draftItems = draftItems.filterNot { it.key == item.key }
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Remove ${item.sku.productName}",
                                                tint = StatusCritical,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    val matchingOpenOrders = openOrders.filter {
                                        it.productName.equals(item.sku.productName, ignoreCase = true)
                                    }
                                    val currentLinkedIds = (item.linkedOrderIds + item.linkedOrderId)
                                        .filter { it.isNotBlank() }
                                        .distinct()
                                    if (openOrders.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = { pickingOrdersForKey = item.key },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("pick_orders_for_schedule_item_${item.key}")
                                            ) {
                                                Icon(
                                                    Icons.Filled.ShoppingCart,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (currentLinkedIds.isEmpty()) {
                                                        "Link Customer Orders"
                                                    } else {
                                                        "Whose Order? (${currentLinkedIds.size} Linked)"
                                                    },
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (matchingOpenOrders.isNotEmpty() && currentLinkedIds.size < matchingOpenOrders.size) {
                                                OutlinedButton(
                                                    onClick = {
                                                        val mergedOrders = (matchingOpenOrders + openOrders.filter { it.id in currentLinkedIds })
                                                            .distinctBy { it.id }
                                                        draftItems = draftItems.map { d ->
                                                            if (d.key == item.key) {
                                                                d.copy(
                                                                    linkedOrderId = mergedOrders.firstOrNull()?.id.orEmpty(),
                                                                    linkedOrderCode = mergedOrders.firstOrNull()?.orderCode.orEmpty(),
                                                                    linkedCustomer = mergedOrders.firstOrNull()?.customer.orEmpty(),
                                                                    linkedOrderIds = mergedOrders.map { it.id },
                                                                    linkedOrderCodes = mergedOrders.map { it.orderCode },
                                                                    linkedCustomers = mergedOrders.map { it.customer }
                                                                )
                                                            } else d
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text("All (${matchingOpenOrders.size})", maxLines = 1)
                                                }
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = item.targetBagsText,
                                        onValueChange = { newVal ->
                                            draftItems = draftItems.map {
                                                if (it.key == item.key) it.copy(targetBagsText = newVal) else it
                                            }
                                        },
                                        label = { Text("Target Bags (${item.sku.standardBagWeightKg} kg/bag)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    ExposedDropdownMenuBox(
                                        expanded = lineOpen,
                                        onExpandedChange = { expandedLineForKey = if (lineOpen) null else item.key }
                                    ) {
                                        OutlinedTextField(
                                            value = item.lineRemarks,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Line / Machine Remarks") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lineOpen) },
                                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = lineOpen,
                                            onDismissRequest = { expandedLineForKey = null }
                                        ) {
                                            PlantLineRemarks.options.forEach { opt ->
                                                DropdownMenuItem(
                                                    text = { Text(opt) },
                                                    onClick = {
                                                        draftItems = draftItems.map {
                                                            if (it.key == item.key) it.copy(lineRemarks = opt) else it
                                                        }
                                                        expandedLineForKey = null
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "Expected Output: ${OperationsCalculator.formatKg(expectedKg)} kg",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${draftItems.size} run(s) • $totalBags total bags • ${OperationsCalculator.formatKg(totalExpectedKg)} kg",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val normDate = normalizeIsoDateOrNull(scheduledDate)
                    val parsedItems = draftItems.mapNotNull { item ->
                        val b = item.targetBagsText.toLongOrNull() ?: 0L
                        val wt = item.sku.standardBagWeightKg
                        val expKg = round(b * wt * 100.0) / 100.0
                        val safeRemarks = item.lineRemarks.trim().ifBlank { PlantLineRemarks.options.first() }.take(120)
                        if (b in 1..1_000_000 && wt > 0.0 && wt <= 2000.0 && expKg > 0.0 && expKg <= 50_000_000.0) {
                            val ids = (item.linkedOrderIds + item.linkedOrderId).ifEmpty {
                                listOfNotNull(linkedOrder?.id?.takeIf { it.isNotBlank() })
                            }.map { it.trim().take(128) }.filter { it.isNotBlank() }.distinct().take(100)
                            val codes = (item.linkedOrderCodes + item.linkedOrderCode).ifEmpty {
                                listOfNotNull(linkedOrder?.orderCode?.takeIf { it.isNotBlank() })
                            }.map { it.trim().take(40) }.filter { it.isNotBlank() }.distinct().take(100)
                            NewScheduleItemInput(
                                section = PlantSection.fromRaw(item.sku.section),
                                productName = item.sku.productName.trim().take(120),
                                targetBags = b,
                                bagWeightKg = wt,
                                lineRemarks = safeRemarks,
                                linkedOrderId = ids.firstOrNull().orEmpty(),
                                linkedOrderCode = codes.firstOrNull().orEmpty(),
                                linkedOrderIds = ids,
                                linkedOrderCodes = codes
                            )
                        } else {
                            null
                        }
                    }
                    when {
                        normDate == null -> error = "Use YYYY-MM-DD date"
                        draftItems.isEmpty() -> error = "Select at least one product SKU to schedule"
                        parsedItems.size != draftItems.size -> error = "Target bags must be 1–1,000,000 for each item"
                        onSubmitMultiple != null -> onSubmitMultiple(normDate, priority, parsedItems)
                        else -> {
                            val first = parsedItems.first()
                            onSubmit(
                                normDate,
                                first.section,
                                first.productName,
                                first.targetBags,
                                first.bagWeightKg,
                                first.lineRemarks,
                                priority,
                                first.linkedOrderId,
                                first.linkedOrderCode
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestEmerald,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("submit_schedule_button")
            ) {
                Text(if (draftItems.size > 1) "Queue ${draftItems.size} Runs" else "Add to Queue")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showSkuPicker) {
        val currentSkuKeys = draftItems.map { skuStableKey(it.sku) }.toSet()
        MultiSkuPickerDialog(
            title = "Select Production Items",
            skus = skus,
            summaries = summaries,
            initiallySelectedKeys = currentSkuKeys,
            onDismiss = { showSkuPicker = false },
            onConfirm = { pickedSkus ->
                val existingBySkuKey = draftItems.groupBy { skuStableKey(it.sku) }
                val nextDrafts = mutableListOf<MultiScheduleItemDraft>()
                pickedSkus.forEach { sku ->
                    val sKey = skuStableKey(sku)
                    val existingForSku = existingBySkuKey[sKey]
                    if (!existingForSku.isNullOrEmpty()) {
                        nextDrafts.addAll(existingForSku)
                    } else {
                        val seedOrders = if (linkedOrder != null) {
                            listOf(linkedOrder)
                        } else {
                            openOrders.filter { it.productName.equals(sku.productName, ignoreCase = true) }
                        }
                        nextDrafts.add(
                            MultiScheduleItemDraft(
                                key = sKey,
                                sku = sku,
                                targetBagsText = "40",
                                lineRemarks = sku.defaultRemarks.ifBlank { PlantLineRemarks.options.first() },
                                linkedOrderId = seedOrders.firstOrNull()?.id.orEmpty(),
                                linkedOrderCode = seedOrders.firstOrNull()?.orderCode.orEmpty(),
                                linkedCustomer = seedOrders.firstOrNull()?.customer.orEmpty(),
                                linkedOrderIds = seedOrders.map { it.id }.filter { it.isNotBlank() }.distinct(),
                                linkedOrderCodes = seedOrders.map { it.orderCode }.filter { it.isNotBlank() }.distinct(),
                                linkedCustomers = seedOrders.map { it.customer }.filter { it.isNotBlank() }.distinct()
                            )
                        )
                    }
                }
                draftItems = nextDrafts
                showSkuPicker = false
            }
        )
    }

    if (showOrderSchedulePicker) {
        val currentOrderIds = draftItems.flatMap { it.linkedOrderIds + it.linkedOrderId }
            .filter { it.isNotBlank() }
            .toSet()
        MultiOrderSchedulePickerDialog(
            openOrders = openOrders,
            summaries = summaries,
            initiallySelectedOrderIds = currentOrderIds,
            onDismiss = { showOrderSchedulePicker = false },
            onConfirm = { pickedOrders ->
                val skuByProdLower = skus.associateBy { it.productName.trim().lowercase() }
                val sumByProdLower = summaries.associateBy { it.sku.productName.trim().lowercase() }
                val groupedDrafts = pickedOrders
                    .groupBy { it.productName.trim().lowercase() }
                    .values
                    .map { groupOrders ->
                        val rep = groupOrders.first()
                        val prodKey = rep.productName.trim().lowercase()
                        val resolvedSku = skuByProdLower[prodKey] ?: SkuEntity(
                            productName = rep.productName,
                            section = PlantSection.SAN_ROQUE.label,
                            standardBagWeightKg = rep.bagWeightKg.coerceAtLeast(1.0),
                            defaultRemarks = PlantLineRemarks.options.first()
                        )
                        val totalRemKg = groupOrders.sumOf { max(0.0, it.totalWeightKg - it.dispatchedWeightKg) }
                        val availKg = max(0.0, sumByProdLower[prodKey]?.availableStockKg ?: 0.0)
                        val shortfallKg = max(0.0, totalRemKg - availKg)
                        val targetKg = if (shortfallKg > 0.0) shortfallKg else totalRemKg
                        val bagWt = resolvedSku.standardBagWeightKg.coerceAtLeast(0.1)
                        val bags = ceil(targetKg / bagWt).toLong().coerceAtLeast(1L)
                        MultiScheduleItemDraft(
                            key = "${skuStableKey(resolvedSku)}__orders_${groupOrders.size}",
                            sku = resolvedSku,
                            targetBagsText = bags.toString(),
                            lineRemarks = resolvedSku.defaultRemarks.ifBlank { PlantLineRemarks.options.first() },
                            linkedOrderId = groupOrders.firstOrNull()?.id.orEmpty(),
                            linkedOrderCode = groupOrders.firstOrNull()?.orderCode.orEmpty(),
                            linkedCustomer = groupOrders.firstOrNull()?.customer.orEmpty(),
                            linkedOrderIds = groupOrders.map { it.id }.filter { it.isNotBlank() }.distinct(),
                            linkedOrderCodes = groupOrders.map { it.orderCode }.filter { it.isNotBlank() }.distinct(),
                            linkedCustomers = groupOrders.map { it.customer }.filter { it.isNotBlank() }.distinct()
                        )
                    }
                if (groupedDrafts.isNotEmpty()) {
                    draftItems = groupedDrafts
                    if (pickedOrders.any { it.priority }) {
                        priority = SchedulePriority.URGENT
                    }
                }
                showOrderSchedulePicker = false
            }
        )
    }

    pickingOrdersForKey?.let { targetKey ->
        val draft = draftItems.firstOrNull { it.key == targetKey }
        if (draft != null) {
            val initIds = (draft.linkedOrderIds + draft.linkedOrderId).filter { it.isNotBlank() }.toSet()
            MultiOrderLinkPickerDialog(
                productName = draft.sku.productName,
                allOrders = openOrders,
                initiallySelectedIds = initIds,
                onDismiss = { pickingOrdersForKey = null },
                onConfirm = { selected ->
                    draftItems = draftItems.map { d ->
                        if (d.key == targetKey) {
                            d.copy(
                                linkedOrderId = selected.firstOrNull()?.id.orEmpty(),
                                linkedOrderCode = selected.firstOrNull()?.orderCode.orEmpty(),
                                linkedCustomer = selected.firstOrNull()?.customer.orEmpty(),
                                linkedOrderIds = selected.map { it.id }.filter { it.isNotBlank() }.distinct(),
                                linkedOrderCodes = selected.map { it.orderCode }.filter { it.isNotBlank() }.distinct(),
                                linkedCustomers = selected.map { it.customer }.filter { it.isNotBlank() }.distinct()
                            )
                        } else d
                    }
                    pickingOrdersForKey = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduleRunDialog(
    run: ScheduleRun,
    allOrders: List<CustomerOrder> = emptyList(),
    onDismiss: () -> Unit,
    onSubmit: (
        scheduledDate: String,
        targetBags: Long,
        bagWeightKg: Double,
        lineRemarks: String,
        priority: SchedulePriority,
        linkedOrders: List<CustomerOrder>
    ) -> Unit
) {
    val initialLinkedOrders = remember(run, allOrders) {
        val ids = run.allLinkedOrderIds().toSet()
        val codes = run.allLinkedOrderCodes().toSet()
        allOrders.filter { it.id in ids || it.orderCode in codes }
    }

    var scheduledDate by remember { mutableStateOf(run.scheduledDate) }
    var targetBagsText by remember { mutableStateOf(run.targetBags.toString()) }
    var bagWtText by remember { mutableStateOf(run.bagWeightKg.toString()) }
    var lineRemarks by remember { mutableStateOf(run.lineRemarks) }
    var priority by remember { mutableStateOf(SchedulePriority.fromRaw(run.priority)) }
    var selectedOrders by remember(initialLinkedOrders) { mutableStateOf(initialLinkedOrders) }
    var lineExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }
    var showOrderLinkPicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val bags = targetBagsText.toLongOrNull() ?: 0L
    val wt = bagWtText.toDoubleOrNull() ?: 0.0
    val expectedKg = round(bags * wt * 100.0) / 100.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Scheduled Run") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(run.productName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = buildString {
                                append(run.section)
                                if (selectedOrders.isNotEmpty()) {
                                    append(" • ${selectedOrders.size} linked customer order(s)")
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (selectedOrders.isEmpty()) {
                                "No customer orders linked (Buffer / General Stock)"
                            } else if (selectedOrders.size <= 2) {
                                "For: ${selectedOrders.joinToString(", ") { "${it.customer} (${it.orderCode})" }}"
                            } else {
                                "Linked to ${selectedOrders.size} customer orders (${selectedOrders.first().customer} + ${selectedOrders.size - 1} more)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        OutlinedButton(
                            onClick = { showOrderLinkPicker = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_run_link_orders_button")
                        ) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (selectedOrders.isEmpty()) "Link Customer Orders"
                                else "Whose Order? Edit Links (${selectedOrders.size})"
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = scheduledDate,
                    onValueChange = { scheduledDate = it },
                    label = { Text("Scheduled Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetBagsText,
                        onValueChange = { targetBagsText = it },
                        label = { Text("Target Bags") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = bagWtText,
                        onValueChange = { bagWtText = it },
                        label = { Text("Bag Wt (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "Expected Output: ${OperationsCalculator.formatKg(expectedKg)} kg",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                ExposedDropdownMenuBox(
                    expanded = priorityExpanded,
                    onExpandedChange = { priorityExpanded = !priorityExpanded }
                ) {
                    OutlinedTextField(
                        value = priority.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = priorityExpanded,
                        onDismissRequest = { priorityExpanded = false }
                    ) {
                        SchedulePriority.entries.forEach { pr ->
                            DropdownMenuItem(
                                text = { Text(pr.label) },
                                onClick = {
                                    priority = pr
                                    priorityExpanded = false
                                }
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = lineExpanded,
                    onExpandedChange = { lineExpanded = !lineExpanded }
                ) {
                    OutlinedTextField(
                        value = lineRemarks,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Line / Machine Remarks") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lineExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = lineExpanded,
                        onDismissRequest = { lineExpanded = false }
                    ) {
                        PlantLineRemarks.options.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    lineRemarks = opt
                                    lineExpanded = false
                                }
                            )
                        }
                    }
                }
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val normDate = normalizeIsoDateOrNull(scheduledDate)
                    when {
                        normDate == null -> error = "Scheduled date must be YYYY-MM-DD"
                        bags !in 1..1_000_000 -> error = "Target bags must be 1–1,000,000"
                        wt <= 0.0 || wt > 2000.0 -> error = "Bag weight must be 0.01–2,000 kg"
                        expectedKg <= 0.0 || expectedKg > 50_000_000.0 -> error = "Expected weight must be <= 50,000,000 kg"
                        else -> onSubmit(normDate, bags, wt, lineRemarks, priority, selectedOrders)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestEmerald,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("submit_edit_schedule_button")
            ) {
                Text("Save Schedule")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showOrderLinkPicker) {
        MultiOrderLinkPickerDialog(
            productName = run.productName,
            allOrders = allOrders,
            initiallySelectedIds = selectedOrders.map { it.id }.filter { it.isNotBlank() }.toSet(),
            onDismiss = { showOrderLinkPicker = false },
            onConfirm = { picked ->
                selectedOrders = picked
                showOrderLinkPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogBatchDialog(
    skus: List<SkuEntity>,
    summaries: List<SkuInventorySummary> = emptyList(),
    orders: List<CustomerOrder> = emptyList(),
    schedules: List<ScheduleRun> = emptyList(),
    fromScheduleRun: ScheduleRun? = null,
    existingBatch: ProductionBatch? = null,
    onDismiss: () -> Unit,
    onSubmit: (
        date: String,
        section: PlantSection,
        productName: String,
        bagsProduced: Long,
        bagWeightKg: Double,
        actualRecoveredKg: Double,
        remarks: String,
        linkedOrders: List<CustomerOrder>
    ) -> Unit
) {
    val initSku = existingBatch?.let { b -> skus.firstOrNull { it.productName == b.productName } }
        ?: fromScheduleRun?.let { run -> skus.firstOrNull { it.productName == run.productName } }
        ?: skus.firstOrNull()

    var date by remember { mutableStateOf(existingBatch?.date ?: fromScheduleRun?.scheduledDate ?: OperationsCalculator.todayIsoDate()) }
    var section by remember {
        mutableStateOf(
            existingBatch?.let { PlantSection.fromRaw(it.section) }
                ?: fromScheduleRun?.let { PlantSection.fromRaw(it.section) }
                ?: initSku?.let { PlantSection.fromRaw(it.section) }
                ?: PlantSection.SAN_ROQUE
        )
    }
    var productName by remember {
        mutableStateOf(existingBatch?.productName ?: fromScheduleRun?.productName ?: initSku?.productName.orEmpty())
    }
    var bagsText by remember {
        mutableStateOf((existingBatch?.bagsProduced ?: fromScheduleRun?.targetBags ?: 100L).toString())
    }
    var bagWtText by remember {
        mutableStateOf((existingBatch?.bagWeightKg ?: fromScheduleRun?.bagWeightKg ?: initSku?.standardBagWeightKg ?: 50.0).toString())
    }
    var actualText by remember {
        val exp = existingBatch?.actualRecoveredKg
            ?: ((fromScheduleRun?.targetBags ?: 100L) * (fromScheduleRun?.bagWeightKg ?: initSku?.standardBagWeightKg ?: 50.0))
        mutableStateOf(String.format("%.2f", exp))
    }
    var remarks by remember {
        mutableStateOf(
            existingBatch?.remarks?.ifBlank { null }
                ?: fromScheduleRun?.lineRemarks?.ifBlank { null }
                ?: initSku?.defaultRemarks?.ifBlank { null }
                ?: "MIXING/CONVEYOR"
        )
    }
    val initialLinkedOrders = remember(orders, schedules, fromScheduleRun, existingBatch) {
        val matchedExistingRun = if (existingBatch != null && existingBatch.allLinkedOrderIds().isEmpty() && existingBatch.allLinkedOrderCodes().isEmpty()) {
            schedules.firstOrNull { it.id.isNotBlank() && it.id == existingBatch.scheduleRunId }
                ?: schedules.firstOrNull {
                    it.productName == existingBatch.productName &&
                        it.section == existingBatch.section &&
                        it.scheduledDate == existingBatch.date &&
                        it.status != "Cancelled"
                }
        } else null
        val targetIds = when {
            fromScheduleRun != null -> fromScheduleRun.allLinkedOrderIds().toSet()
            existingBatch != null && existingBatch.allLinkedOrderIds().isNotEmpty() -> existingBatch.allLinkedOrderIds().toSet()
            matchedExistingRun != null -> matchedExistingRun.allLinkedOrderIds().toSet()
            else -> emptySet()
        }
        val targetCodes = when {
            fromScheduleRun != null -> fromScheduleRun.allLinkedOrderCodes().toSet()
            existingBatch != null && existingBatch.allLinkedOrderCodes().isNotEmpty() -> existingBatch.allLinkedOrderCodes().toSet()
            matchedExistingRun != null -> matchedExistingRun.allLinkedOrderCodes().toSet()
            else -> emptySet()
        }
        orders.filter { it.id in targetIds || it.orderCode in targetCodes }
    }
    var selectedOrders by remember(initialLinkedOrders) { mutableStateOf(initialLinkedOrders) }
    var sectionQuery by remember { mutableStateOf(section.label) }
    var sectionSearchActive by remember { mutableStateOf(false) }
    var productSearchActive by remember { mutableStateOf(false) }
    var showBatchSkuPicker by remember { mutableStateOf(false) }
    var showOrderPicker by remember { mutableStateOf(false) }
    var secExpanded by remember { mutableStateOf(false) }
    var skuExpanded by remember { mutableStateOf(false) }
    var remExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val bags = bagsText.toLongOrNull() ?: 0L
    val bagWt = bagWtText.toDoubleOrNull() ?: 0.0
    val expectedKg = round(bags * bagWt * 100.0) / 100.0
    val actualKg = actualText.toDoubleOrNull() ?: 0.0
    val varianceKg = round((actualKg - expectedKg) * 100.0) / 100.0
    val recoveryPct = if (expectedKg > 0.0) (actualKg / expectedKg) * 100.0 else 0.0
    val targetRec = skus.firstOrNull { it.productName == productName }?.targetRecoveryPct ?: 98.5
    val isBelowTarget = recoveryPct > 0.0 && recoveryPct < targetRec

    val selectedSummary = remember(summaries, productName) {
        summaries.firstOrNull { it.sku.productName == productName }
    }
    val effectiveBagWt = if (bagWt > 0.0) bagWt else (initSku?.standardBagWeightKg ?: 50.0).coerceAtLeast(1.0)
    val availStockBeforeBatchKg = remember(selectedSummary, existingBatch) {
        val rawAvail = max(0.0, selectedSummary?.availableStockKg ?: 0.0)
        if (existingBatch != null && existingBatch.productName == selectedSummary?.sku?.productName) {
            max(0.0, rawAvail - existingBatch.actualRecoveredKg)
        } else {
            rawAvail
        }
    }
    val openOrdersTotalKg = max(0.0, selectedSummary?.openOrdersKg ?: 0.0)
    val backlogToProduceKg = max(0.0, openOrdersTotalKg - availStockBeforeBatchKg)
    val backlogToProduceBags = if (backlogToProduceKg > 0.0) {
        ceil(backlogToProduceKg / effectiveBagWt).toLong()
    } else 0L
    val linkedActiveOrders = remember(selectedOrders) {
        selectedOrders.filter {
            it.status == OrderStatus.OPEN.label || it.status == OrderStatus.PARTIALLY_DISPATCHED.label
        }
    }
    val linkedRemainingKg = linkedActiveOrders.sumOf { max(0.0, it.totalWeightKg - it.dispatchedWeightKg) }
    val coveredBacklogKg = actualKg.coerceAtMost(backlogToProduceKg)
    val remainingBacklogAfterKg = max(0.0, backlogToProduceKg - actualKg)
    val remainingBacklogAfterBags = if (remainingBacklogAfterKg > 0.0) {
        ceil(remainingBacklogAfterKg / effectiveBagWt).toLong()
    } else 0L
    val surplusAfterBacklogKg = max(0.0, actualKg - backlogToProduceKg)
    val backlogLinkPct = if (backlogToProduceKg > 0.0) {
        ((actualKg / backlogToProduceKg) * 100.0).coerceIn(0.0, 100.0)
    } else 100.0
    val backlogLinkProgress = if (backlogToProduceKg > 0.0) {
        (actualKg / backlogToProduceKg).toFloat().coerceIn(0f, 1f)
    } else 1f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    existingBatch != null -> "Edit Bags Recovered"
                    fromScheduleRun != null -> "Complete Run & Log Recovery"
                    else -> "Log Production & Recovery Batch"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (fromScheduleRun != null) {
                    val bagDiff = bags - fromScheduleRun.targetBags
                    val diffStr = if (bagDiff == 0L) "Matches target" else if (bagDiff > 0) "+$bagDiff bags vs target" else "$bagDiff bags vs target"
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Scheduled Target: ${fromScheduleRun.targetBags} bags (${OperationsCalculator.formatKg(fromScheduleRun.expectedWeightKg)} kg)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Recovered Entry: $bags bags ($diffStr)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                if (orders.isNotEmpty() || selectedOrders.isNotEmpty() || (existingBatch?.allScheduledForCustomers()?.isNotEmpty() == true)) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
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
                                    text = if (selectedOrders.isEmpty()) {
                                        val fallbackCust = existingBatch?.allScheduledForCustomers().orEmpty()
                                        if (fallbackCust.isNotEmpty()) {
                                            "Scheduled For: ${fallbackCust.joinToString(", ")}"
                                        } else {
                                            "Scheduled For: Buffer / General Stock"
                                        }
                                    } else {
                                        "Scheduled For (${selectedOrders.size} order${if (selectedOrders.size == 1) "" else "s"})"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { showOrderPicker = true },
                                    modifier = Modifier.testTag("batch_edit_customer_orders_button")
                                ) {
                                    Text(if (selectedOrders.isEmpty()) "Link Orders" else "Edit (${selectedOrders.size})")
                                }
                            }
                            if (selectedOrders.isNotEmpty()) {
                                Text(
                                    text = selectedOrders.joinToString("\n") { "${it.customer} (${it.orderCode} • ${it.orderedBags} bags)" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                if (summaries.isNotEmpty() || fromScheduleRun != null) {
                    val isBacklogCleared = backlogToProduceKg <= 0.01 || remainingBacklogAfterKg <= 0.01
                    Surface(
                        color = when {
                            backlogToProduceKg <= 0.01 -> StatusHealthyContainer
                            isBacklogCleared -> StatusHealthyContainer
                            else -> StatusWatchContainer
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isBacklogCleared) StatusHealthyTextDark else GrainAmberBright
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("batch_backlog_link_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Backlogs to Produce",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isBacklogCleared) OnStatusHealthyContainer else OnStatusWatchContainer
                                )
                                Text(
                                    text = if (backlogToProduceKg > 0.0) {
                                        "$backlogToProduceBags bags (${OperationsCalculator.formatKg(backlogToProduceKg)} kg)"
                                    } else {
                                        "0 bags (0.00 kg)"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = if (isBacklogCleared) OnStatusHealthyContainer else OnStatusCriticalContainer
                                )
                            }
                            Text(
                                text = "Open Orders: ${OperationsCalculator.formatKg(openOrdersTotalKg)} kg • Avail Stock: ${OperationsCalculator.formatKg(availStockBeforeBatchKg)} kg",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (linkedActiveOrders.isNotEmpty()) {
                                val linkedNames = linkedActiveOrders.joinToString(", ") { "${it.customer} (${it.orderCode})" }
                                Text(
                                    text = "Linked Orders (${linkedActiveOrders.size}): $linkedNames • Need ${OperationsCalculator.formatKg(linkedRemainingKg)} kg",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (backlogToProduceKg > 0.0) {
                                LinearProgressIndicator(
                                    progress = { backlogLinkProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = if (isBacklogCleared) StatusHealthyTextDark else GrainAmberBright,
                                    trackColor = DarkChartTrack
                                )
                            }
                            Text(
                                text = when {
                                    backlogToProduceKg <= 0.01 ->
                                        "No open backlogs • +${OperationsCalculator.formatKg(actualKg)} kg recovered boosts available stock"
                                    remainingBacklogAfterKg <= 0.01 ->
                                        "Backlogs Cleared (100%): ${OperationsCalculator.formatKg(coveredBacklogKg)} kg covered (+${OperationsCalculator.formatKg(surplusAfterBacklogKg)} kg surplus)"
                                    else ->
                                        "Recovered ${OperationsCalculator.formatKg(actualKg)} kg clears ${String.format("%.1f%%", backlogLinkPct)} • Remaining Backlog: $remainingBacklogAfterBags bags (${OperationsCalculator.formatKg(remainingBacklogAfterKg)} kg)"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isBacklogCleared) OnStatusHealthyContainer else OnStatusWatchContainer
                            )
                            if (backlogToProduceBags > 0L && bags != backlogToProduceBags) {
                                OutlinedButton(
                                    onClick = {
                                        bagsText = backlogToProduceBags.toString()
                                        actualText = String.format("%.2f", backlogToProduceBags * effectiveBagWt)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("match_backlog_recovery_button")
                                ) {
                                    Text(
                                        text = "Link & Match Backlog ($backlogToProduceBags bags • ${OperationsCalculator.formatKg(backlogToProduceBags * effectiveBagWt)} kg)",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Batch Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val matchingSections = remember(sectionQuery, sectionSearchActive) {
                    val q = sectionQuery.trim()
                    if (!sectionSearchActive || q.isBlank()) {
                        PlantSection.entries
                    } else {
                        PlantSection.entries.filter { it.label.contains(q, ignoreCase = true) }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = secExpanded,
                    onExpandedChange = {
                        secExpanded = !secExpanded
                        if (secExpanded) sectionSearchActive = false
                    }
                ) {
                    OutlinedTextField(
                        value = sectionQuery,
                        onValueChange = { next ->
                            sectionQuery = next
                            sectionSearchActive = true
                            secExpanded = true
                            val exact = PlantSection.entries.firstOrNull {
                                it.label.equals(next.trim(), ignoreCase = true)
                            }
                            if (exact != null) {
                                section = exact
                            }
                        },
                        label = { Text("Plant Section (Search)") },
                        placeholder = { Text("Type plant section...") },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = "Search plant section")
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (sectionQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            sectionQuery = ""
                                            sectionSearchActive = true
                                            secExpanded = true
                                        }
                                    ) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear plant section search")
                                    }
                                }
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = secExpanded)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .fillMaxWidth()
                            .testTag("batch_section_search_input")
                    )
                    ExposedDropdownMenu(
                        expanded = secExpanded,
                        onDismissRequest = { secExpanded = false }
                    ) {
                        if (matchingSections.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No matching plant sections") },
                                onClick = {},
                                enabled = false
                            )
                        } else {
                            matchingSections.forEach { sec ->
                                DropdownMenuItem(
                                    text = { Text(sec.label) },
                                    onClick = {
                                        section = sec
                                        sectionQuery = sec.label
                                        sectionSearchActive = false
                                        secExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                val matchingSkus = remember(skus, productName, productSearchActive, section) {
                    val q = productName.trim()
                    val base = if (!productSearchActive || q.isBlank()) {
                        skus
                    } else {
                        skus.filter { sku ->
                            sku.productName.contains(q, ignoreCase = true) ||
                                sku.section.contains(q, ignoreCase = true)
                        }
                    }
                    base.sortedWith(
                        compareBy<SkuEntity> { it.section != section.label }
                            .thenBy { it.productName.lowercase() }
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = skuExpanded,
                    onExpandedChange = {
                        skuExpanded = !skuExpanded
                        if (skuExpanded) productSearchActive = false
                    }
                ) {
                    OutlinedTextField(
                        value = productName,
                        onValueChange = { next ->
                            productName = next
                            productSearchActive = true
                            skuExpanded = true
                            val exact = skus.firstOrNull {
                                it.productName.equals(next.trim(), ignoreCase = true)
                            }
                            if (exact != null) {
                                section = PlantSection.fromRaw(exact.section)
                                sectionQuery = section.label
                                sectionSearchActive = false
                            }
                        },
                        label = { Text("Product Name (Search SKU)") },
                        placeholder = { Text("Type product name or section...") },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = "Search product name")
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (productName.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            productName = ""
                                            productSearchActive = true
                                            skuExpanded = true
                                        }
                                    ) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear product search")
                                    }
                                }
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = skuExpanded)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .fillMaxWidth()
                            .testTag("batch_product_search_input")
                    )
                    ExposedDropdownMenu(
                        expanded = skuExpanded,
                        onDismissRequest = { skuExpanded = false }
                    ) {
                        if (matchingSkus.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No matching SKUs — tap Browse SKUs") },
                                onClick = {},
                                enabled = false
                            )
                        } else {
                            matchingSkus.forEach { sku ->
                                DropdownMenuItem(
                                    text = { Text("${sku.productName} (${sku.section} • ${sku.standardBagWeightKg}kg)") },
                                    onClick = {
                                        productName = sku.productName
                                        productSearchActive = false
                                        section = PlantSection.fromRaw(sku.section)
                                        sectionQuery = section.label
                                        sectionSearchActive = false
                                        bagWtText = sku.standardBagWeightKg.toString()
                                        remarks = sku.defaultRemarks
                                        val newExp = (bagsText.toLongOrNull() ?: 0L) * sku.standardBagWeightKg
                                        actualText = String.format("%.2f", newExp)
                                        skuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (skus.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showBatchSkuPicker = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("batch_browse_skus_button")
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Browse All SKUs (${skus.size})")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bagsText,
                        onValueChange = {
                            bagsText = it
                            val b = it.toLongOrNull() ?: 0L
                            val w = bagWtText.toDoubleOrNull() ?: 0.0
                            actualText = String.format("%.2f", b * w)
                        },
                        label = {
                            Text(
                                if (fromScheduleRun != null || existingBatch != null) "Bags Recovered"
                                else "Bags Produced"
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("batch_bags_recovered_input")
                    )
                    OutlinedTextField(
                        value = bagWtText,
                        onValueChange = {
                            bagWtText = it
                            val b = bagsText.toLongOrNull() ?: 0L
                            val w = it.toDoubleOrNull() ?: 0.0
                            actualText = String.format("%.2f", b * w)
                        },
                        label = { Text("Bag Wt (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = actualText,
                    onValueChange = { actualText = it },
                    label = { Text("Actual Recovered Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    color = if (isBelowTarget) StatusCriticalContainer else StatusHealthyContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Expected: ${OperationsCalculator.formatKg(expectedKg)} kg | Variance: ${OperationsCalculator.formatKg(varianceKg)} kg",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isBelowTarget) OnStatusCriticalContainer else OnStatusHealthyContainer
                        )
                        Text(
                            text = "Recovery: ${String.format("%.2f%%", recoveryPct)} (Target: ${targetRec}%)" +
                                if (isBelowTarget) " • FLAGGED BELOW TARGET" else " • ON TARGET",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isBelowTarget) OnStatusCriticalContainer else OnStatusHealthyContainer
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = remExpanded,
                    onExpandedChange = { remExpanded = !remExpanded }
                ) {
                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Line / Remarks") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = remExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = remExpanded,
                        onDismissRequest = { remExpanded = false }
                    ) {
                        PlantLineRemarks.options.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    remarks = opt
                                    remExpanded = false
                                }
                            )
                        }
                    }
                }

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val normDate = normalizeIsoDateOrNull(date)
                    val trimmedSection = sectionQuery.trim()
                    val resolvedSection = when {
                        trimmedSection.isEmpty() -> null
                        else -> PlantSection.entries.firstOrNull {
                            it.label.equals(trimmedSection, ignoreCase = true)
                        } ?: PlantSection.entries.singleOrNull {
                            it.label.contains(trimmedSection, ignoreCase = true)
                        }
                    }
                    val trimmedProduct = productName.trim()
                    val matchedSku = skus.firstOrNull {
                        it.productName.equals(trimmedProduct, ignoreCase = true)
                    } ?: if (trimmedProduct.isNotBlank()) {
                        skus.singleOrNull { it.productName.contains(trimmedProduct, ignoreCase = true) }
                    } else null
                    val resolvedProduct = matchedSku?.productName ?: trimmedProduct

                    when {
                        normDate == null -> error = "Use YYYY-MM-DD format"
                        resolvedSection == null -> error = "Select a plant section from the list"
                        resolvedProduct.isBlank() -> error = "Select or enter a product SKU"
                        bags !in 1..1_000_000 -> error = "Bags must be 1–1,000,000"
                        bagWt <= 0.0 || bagWt > 2000.0 -> error = "Bag weight must be 0.01–2,000 kg"
                        actualKg <= 0.0 || actualKg > 50_000_000.0 -> error = "Recovered weight must be 0.01–50,000,000 kg"
                        recoveryPct > 200.0 -> error = "Recovery % cannot exceed 200%"
                        else -> onSubmit(normDate, resolvedSection, resolvedProduct, bags, bagWt, actualKg, remarks.trim().take(120), selectedOrders)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestEmerald,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("submit_batch_button")
            ) {
                Text("Save Batch")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showBatchSkuPicker) {
        val currentKey = skus.firstOrNull {
            it.productName.equals(productName.trim(), ignoreCase = true)
        }?.let { setOf(skuStableKey(it)) } ?: emptySet()
        MultiSkuPickerDialog(
            title = "Select Product SKU",
            skus = skus,
            summaries = summaries,
            initiallySelectedKeys = currentKey,
            singleSelection = true,
            onDismiss = { showBatchSkuPicker = false },
            onConfirm = { picked ->
                picked.firstOrNull()?.let { sku ->
                    productName = sku.productName
                    productSearchActive = false
                    section = PlantSection.fromRaw(sku.section)
                    sectionQuery = section.label
                    sectionSearchActive = false
                    bagWtText = sku.standardBagWeightKg.toString()
                    remarks = sku.defaultRemarks
                    val newExp = (bagsText.toLongOrNull() ?: 0L) * sku.standardBagWeightKg
                    actualText = String.format("%.2f", newExp)
                }
                showBatchSkuPicker = false
            }
        )
    }

    if (showOrderPicker) {
        MultiOrderLinkPickerDialog(
            productName = productName,
            allOrders = orders,
            initiallySelectedIds = selectedOrders.map { it.id }.filter { it.isNotBlank() }.toSet(),
            onDismiss = { showOrderPicker = false },
            onConfirm = { picked ->
                selectedOrders = picked
                showOrderPicker = false
            }
        )
    }
}

@Composable
fun BulkPasteDialog(
    onDismiss: () -> Unit,
    onImport: (rawText: String, defaultSection: PlantSection) -> Unit
) {
    var pasteText by remember {
        mutableStateOf(
            "2026-11-19, SAN ROQUE, MB BROODSOW MASH, 50, 50, 2490.00, GRINDING /MIXING\n" +
                "2026-11-19, PANDACAQUI, BARAKO DUCK PELLET, 40, 50, 1988.00, MIXING/CONVEYOR"
        )
    }
    var selectedSection by remember { mutableStateOf(PlantSection.SAN_ROQUE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk Paste Production Rows") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Paste CSV or Tab-separated rows:\nDate, Section, Product, Bags, BagWt, ActualKg, Remarks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pasteText,
                    onValueChange = { pasteText = it },
                    minLines = 5,
                    maxLines = 9,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(pasteText, selectedSection) },
                modifier = Modifier.testTag("submit_bulk_import_button")
            ) {
                Text("Import Rows")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkuEditorDialog(
    existingSku: SkuEntity? = null,
    onDismiss: () -> Unit,
    onSubmit: (
        productName: String,
        section: PlantSection,
        openingBags: Double,
        standardBagWeightKg: Double,
        targetRecoveryPct: Double,
        safetyBufferPct: Double,
        defaultRemarks: String
    ) -> Unit
) {
    var productName by remember { mutableStateOf(existingSku?.productName.orEmpty()) }
    var section by remember {
        mutableStateOf(existingSku?.let { PlantSection.fromRaw(it.section) } ?: PlantSection.SAN_ROQUE)
    }
    val initBagWt = existingSku?.standardBagWeightKg ?: 50.0
    val initBags = if (existingSku != null && initBagWt > 0.0) existingSku.openingStockKg / initBagWt else 0.0
    var openingBagsText by remember {
        mutableStateOf(if (initBags % 1.0 == 0.0) initBags.toLong().toString() else String.format("%.2f", initBags))
    }
    var bagWtText by remember { mutableStateOf(initBagWt.toString()) }
    var targetRecText by remember { mutableStateOf((existingSku?.targetRecoveryPct ?: 98.5).toString()) }
    var bufferText by remember { mutableStateOf((existingSku?.safetyBufferPct ?: 15.0).toString()) }
    var lineRemarks by remember { mutableStateOf(existingSku?.defaultRemarks ?: PlantLineRemarks.options.first()) }
    var secExpanded by remember { mutableStateOf(false) }
    var lineExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val bagsVal = openingBagsText.toDoubleOrNull() ?: -1.0
    val wtVal = bagWtText.toDoubleOrNull() ?: 0.0
    val openingKgPreview = if (bagsVal >= 0.0 && wtVal > 0.0) round(bagsVal * wtVal * 100.0) / 100.0 else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingSku == null) "Add Product" else "Edit Product") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Product Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sku_name_input")
                )

                ExposedDropdownMenuBox(
                    expanded = secExpanded,
                    onExpandedChange = { secExpanded = !secExpanded }
                ) {
                    OutlinedTextField(
                        value = section.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Location / Plant Section") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = secExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = secExpanded,
                        onDismissRequest = { secExpanded = false }
                    ) {
                        PlantSection.entries.forEach { sec ->
                            DropdownMenuItem(
                                text = { Text(sec.label) },
                                onClick = {
                                    section = sec
                                    secExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = openingBagsText,
                        onValueChange = { openingBagsText = it },
                        label = { Text("Beginning Bags") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = bagWtText,
                        onValueChange = { bagWtText = it },
                        label = { Text("Bag Wt (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Beginning Stock: ${OperationsCalculator.formatKg(openingKgPreview)} kg",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetRecText,
                        onValueChange = { targetRecText = it },
                        label = { Text("Target Rec %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = bufferText,
                        onValueChange = { bufferText = it },
                        label = { Text("Buffer %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = lineExpanded,
                    onExpandedChange = { lineExpanded = !lineExpanded }
                ) {
                    OutlinedTextField(
                        value = lineRemarks,
                        onValueChange = { lineRemarks = it },
                        label = { Text("Default Line / Remarks") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lineExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = lineExpanded,
                        onDismissRequest = { lineExpanded = false }
                    ) {
                        PlantLineRemarks.options.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    lineRemarks = opt
                                    lineExpanded = false
                                }
                            )
                        }
                    }
                }

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val recVal = targetRecText.toDoubleOrNull() ?: 0.0
                    val bufVal = bufferText.toDoubleOrNull() ?: -1.0
                    when {
                        productName.trim().length !in 1..120 -> error = "Product name must be 1–120 characters"
                        bagsVal < 0.0 || openingKgPreview > 10_000_000.0 -> error = "Beginning stock must be 0–10,000,000 kg"
                        wtVal <= 0.0 || wtVal > 2000.0 -> error = "Bag weight must be 0.01–2,000 kg"
                        recVal <= 0.0 || recVal > 120.0 -> error = "Target recovery must be 1–120%"
                        bufVal < 0.0 || bufVal > 200.0 -> error = "Safety buffer must be 0–200%"
                        else -> onSubmit(
                            productName.trim(),
                            section,
                            bagsVal,
                            wtVal,
                            recVal,
                            bufVal,
                            lineRemarks.trim().ifBlank { PlantLineRemarks.options.first() }.take(120)
                        )
                    }
                },
                modifier = Modifier.testTag("submit_sku_button")
            ) {
                Text(if (existingSku == null) "Add Item" else "Save Changes")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ManageSkusDialog(
    skus: List<SkuEntity>,
    canDeleteSkus: Boolean = true,
    onDismiss: () -> Unit,
    onAddNew: () -> Unit,
    onEditSku: (SkuEntity) -> Unit,
    onDeleteSku: (SkuEntity) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<SkuEntity?>(null) }

    val filtered = remember(skus, search) {
        skus
            .filter {
                search.isBlank() ||
                    it.productName.contains(search.trim(), ignoreCase = true) ||
                    it.section.contains(search.trim(), ignoreCase = true)
            }
            .sortedWith(compareBy({ it.productName.lowercase() }, { it.section }))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Product Name List (${skus.size})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search product names") },
                    placeholder = { Text("Search product name or location...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = onAddNew,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestEmerald,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manage_add_sku_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Product Name")
                }
                if (filtered.isEmpty()) {
                    Text(
                        text = "No products match \"$search\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp)
                    ) {
                        items(filtered, key = { it.id.ifBlank { "${it.section}_${it.productName}" } }) { sku ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sku.productName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                    )
                                    Text(
                                        text = "${sku.section} • ${sku.standardBagWeightKg} kg/bag • ${OperationsCalculator.formatKg(sku.openingStockKg)} kg beg.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onEditSku(sku) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "Edit ${sku.productName}"
                                    )
                                }
                                if (canDeleteSkus) {
                                    IconButton(
                                        onClick = { pendingDelete = sku },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Delete ${sku.productName}",
                                            tint = StatusCriticalTextDark
                                        )
                                    }
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                    if (!canDeleteSkus) {
                        Text(
                            text = "Note: Admin permission is required to delete product SKUs.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Product?") },
            text = {
                Text("Remove \"${item.productName}\" (${item.section}) from the list? Existing historical batches/orders will remain intact.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSku(item)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusCritical,
                        contentColor = Color.White
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ManageCustomersDialog(
    customers: List<CustomerEntity>,
    onDismiss: () -> Unit,
    onSaveCustomer: (existing: CustomerEntity?, name: String, notes: String, priority: Boolean) -> Unit,
    onDeleteCustomer: (CustomerEntity) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var priorityOnly by remember { mutableStateOf(false) }
    var editorState by remember { mutableStateOf<Pair<Boolean, CustomerEntity?>>(false to null) }
    var pendingEditConfirm by remember { mutableStateOf<CustomerEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<CustomerEntity?>(null) }

    val filtered = remember(customers, search, priorityOnly) {
        customers
            .filter {
                (!priorityOnly || it.priority) &&
                    (search.isBlank() ||
                        it.name.contains(search.trim(), ignoreCase = true) ||
                        it.notes.contains(search.trim(), ignoreCase = true))
            }
            .sortedWith(
                compareByDescending<CustomerEntity> { it.priority }
                    .thenBy { it.name.lowercase() }
            )
    }
    val priorityCount = remember(customers) { customers.count { it.priority } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customers Directory (${customers.size})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search customers") },
                    placeholder = { Text("Search customers...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !priorityOnly,
                        onClick = { priorityOnly = false },
                        label = { Text("All (${customers.size})") }
                    )
                    FilterChip(
                        selected = priorityOnly,
                        onClick = { priorityOnly = true },
                        leadingIcon = {
                            Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Priority ($priorityCount)") },
                        modifier = Modifier.testTag("filter_priority_customers_chip")
                    )
                }
                Button(
                    onClick = { editorState = true to null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestEmerald,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manage_add_customer_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add New Customer")
                }
                Text(
                    text = "Swipe left on a customer to Edit (name/priority) or Delete",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (filtered.isEmpty()) {
                    Text(
                        text = when {
                            customers.isEmpty() -> "No customers in list yet."
                            priorityOnly -> "No priority customers marked yet. Swipe left to Edit and mark Priority."
                            else -> "No customers match \"$search\"."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered, key = { it.id.ifBlank { it.name } }) { cust ->
                            SwipeLeftActionsContainer(
                                itemKey = cust.id.ifBlank { cust.name },
                                shape = RoundedCornerShape(10.dp),
                                editTestTag = "swipe_edit_customer_${cust.name}",
                                deleteTestTag = "swipe_delete_customer_${cust.name}",
                                onEdit = { pendingEditConfirm = cust },
                                onDelete = { pendingDelete = cust }
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        width = if (cust.priority) 1.5.dp else 1.dp,
                                        color = if (cust.priority) GrainAmber else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = cust.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Surface(
                                                    onClick = { onSaveCustomer(cust, cust.name, cust.notes, !cust.priority) },
                                                    color = if (cust.priority) StatusWatchContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                    border = BorderStroke(
                                                        width = 1.dp,
                                                        color = if (cust.priority) GrainAmber else MaterialTheme.colorScheme.outline
                                                    ),
                                                    shape = RoundedCornerShape(999.dp),
                                                    modifier = Modifier.testTag("toggle_customer_priority_${cust.name}")
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Star,
                                                            contentDescription = if (cust.priority) "Priority customer" else "Not priority customer",
                                                            tint = if (cust.priority) OnStatusWatchContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = if (cust.priority) "PRIORITY" else "Not Priority",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = if (cust.priority) OnStatusWatchContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                            if (cust.notes.isNotBlank()) {
                                                Text(
                                                    text = cust.notes,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "← Swipe",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )

    if (editorState.first) {
        val editing = editorState.second
        CustomerEditorDialog(
            existingCustomer = editing,
            onDismiss = { editorState = false to null },
            onSubmit = { name, notes, priority ->
                onSaveCustomer(editing, name, notes, priority)
                editorState = false to null
            }
        )
    }

    pendingEditConfirm?.let { cust ->
        AlertDialog(
            onDismissRequest = { pendingEditConfirm = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit customer confirmation",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Edit Customer?") },
            text = {
                Text("Do you want to edit \"${cust.name}\"?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingEditConfirm = null
                        editorState = true to cust
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestEmerald,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_edit_customer_button")
                ) {
                    Text("Edit Customer")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingEditConfirm = null }) { Text("Cancel") }
            }
        )
    }

    pendingDelete?.let { cust ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete customer confirmation",
                    tint = StatusCriticalTextDark
                )
            },
            title = { Text("Delete Customer?") },
            text = {
                Text("Remove \"${cust.name}\" from the Customers list? Past orders and dispatches will keep their records.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCustomer(cust)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusCritical,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_customer_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun CustomerEditorDialog(
    existingCustomer: CustomerEntity? = null,
    onDismiss: () -> Unit,
    onSubmit: (name: String, notes: String, priority: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(existingCustomer?.name.orEmpty()) }
    var notes by remember { mutableStateOf(existingCustomer?.notes.orEmpty()) }
    var isPriority by remember { mutableStateOf(existingCustomer?.priority ?: false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingCustomer == null) "Add New Customer" else "Edit Customer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Branch (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Surface(
                    color = if (isPriority) StatusWatchContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isPriority) BorderStroke(1.dp, GrainAmber) else null,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPriority = !isPriority }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = if (isPriority) OnStatusWatchContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Priority Customer",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isPriority) OnStatusWatchContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isPriority) "Default new orders as Priority" else "Standard account",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPriority) OnStatusWatchContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isPriority,
                            onCheckedChange = { isPriority = it },
                            modifier = Modifier.testTag("customer_priority_switch")
                        )
                    }
                }
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    when {
                        trimmed.length !in 1..120 -> error = "Customer name must be 1–120 characters"
                        notes.trim().length > 120 -> error = "Notes must be <= 120 characters"
                        else -> onSubmit(trimmed, notes.trim(), isPriority)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestEmerald,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("submit_customer_button")
            ) {
                Text(if (existingCustomer == null) "Add Customer" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
