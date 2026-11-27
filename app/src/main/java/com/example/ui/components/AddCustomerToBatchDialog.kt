package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
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
import com.example.model.SkuEntity
import com.example.model.SkuInventorySummary
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.GrainAmberBright
import com.example.ui.theme.OnStatusHealthyContainer
import com.example.ui.theme.StatusHealthyContainer
import kotlin.math.ceil
import kotlin.math.max

@Composable
fun AddCustomerToBatchDialog(
    allCustomers: List<String>,
    openOrders: List<CustomerOrder>,
    skus: List<SkuEntity>,
    summaries: List<SkuInventorySummary>,
    alreadyConnectedOrderIds: Set<String>,
    currentBatchCustomers: Set<String>,
    priorityProductKeys: Set<String> = emptySet(),
    restrictToPriorityKeys: Boolean = priorityProductKeys.isNotEmpty(),
    sourcePlantFilter: String = "",
    onDismiss: () -> Unit,
    onConfirmAdd: (customerName: String, connectedOrders: List<CustomerOrder>, directSkus: List<SkuEntity>, addDirectAsRushOrder: Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeCustomer by remember { mutableStateOf<String?>(null) }
    var checkedOrderIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var checkedDirectSkuKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var skuSearchQuery by remember { mutableStateOf("") }
    var showDirectSkuList by remember { mutableStateOf(false) }
    var addDirectAsRushOrder by remember { mutableStateOf(true) }

    val availByKey = remember(summaries) {
        summaries.associate { skuStableKey(it.sku) to it.availableStockKg }
    }
    val availByName = remember(summaries) {
        summaries.associate { it.sku.productName.trim().lowercase() to it.availableStockKg }
    }
    val availableSelectedSkus = remember(
        skus,
        availByKey,
        availByName,
        priorityProductKeys,
        restrictToPriorityKeys,
        sourcePlantFilter
    ) {
        OperationsCalculator.filterDispatchAvailableSelectedSkus(
            skus = skus,
            availByKey = availByKey,
            availByName = availByName,
            selectedPriorityKeys = priorityProductKeys,
            restrictToSelectedKeys = restrictToPriorityKeys,
            sourcePlantFilter = sourcePlantFilter
        )
    }

    val openOrdersByCustomerLower = remember(openOrders, alreadyConnectedOrderIds) {
        openOrders
            .filter { it.id !in alreadyConnectedOrderIds && it.customer.isNotBlank() }
            .sortedWith(
                compareBy<CustomerOrder> { it.orderDate }
                    .thenBy { it.dueDate }
                    .thenByDescending { it.priority }
            )
            .groupBy { it.customer.trim().lowercase() }
    }

    val combinedCustomerDirectory = remember(allCustomers, openOrders) {
        (allCustomers + openOrders.map { it.customer })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedWith(
                compareByDescending<String> {
                    (openOrdersByCustomerLower[it.lowercase()]?.size ?: 0) > 0
                }.thenBy { it.lowercase() }
            )
    }

    val filteredCustomers = remember(combinedCustomerDirectory, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) combinedCustomerDirectory
        else combinedCustomerDirectory.filter { it.contains(q, ignoreCase = true) }
    }

    val selectCustomer = { custName: String ->
        val custOrders = openOrdersByCustomerLower[custName.trim().lowercase()].orEmpty()
        activeCustomer = custName
        checkedOrderIds = custOrders.map { it.id }.toSet()
        checkedDirectSkuKeys = emptySet()
        skuSearchQuery = ""
        showDirectSkuList = custOrders.isEmpty()
        addDirectAsRushOrder = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (activeCustomer != null) {
                    IconButton(
                        onClick = { activeCustomer = null },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("add_batch_customer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to customer list"
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = "Add customer to batch",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeCustomer ?: "Add Customer to Batch",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (activeCustomer == null) {
                            "Pick a customer to connect their previous orders or add stops"
                        } else {
                            "Connect previous open orders or add direct products"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            val chosenCustomer = activeCustomer
            if (chosenCustomer == null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search customers") },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        placeholder = { Text("Search or type new customer name...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_batch_customer_search")
                    )

                    val typedTrimmed = searchQuery.trim()
                    val exactMatchExists = typedTrimmed.isNotBlank() &&
                        combinedCustomerDirectory.any { it.equals(typedTrimmed, ignoreCase = true) }
                    if (typedTrimmed.isNotBlank() && !exactMatchExists) {
                        OutlinedButton(
                            onClick = { selectCustomer(typedTrimmed) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_batch_new_customer_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Use \"$typedTrimmed\" as Customer")
                        }
                    }

                    if (filteredCustomers.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No matching customers. Type a name above to add them to this batch.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 330.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredCustomers, key = { it.lowercase() }) { custName ->
                                val custOrders = openOrdersByCustomerLower[custName.lowercase()].orEmpty()
                                val inBatch = custName.lowercase() in currentBatchCustomers
                                val remKg = custOrders.sumOf { max(0.0, it.totalWeightKg - it.dispatchedWeightKg) }

                                Surface(
                                    color = if (inBatch) {
                                        StatusHealthyContainer.copy(alpha = 0.45f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (custOrders.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectCustomer(custName) }
                                        .testTag("add_batch_customer_row_${custName.lowercase().replace(" ", "_")}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = if (custOrders.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = custName,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = when {
                                                    custOrders.isNotEmpty() -> "${custOrders.size} previous open order(s) • ${OperationsCalculator.formatKg(remKg)} kg left"
                                                    inBatch -> "Already in batch • tap to add more stops"
                                                    else -> "No open orders • tap to add direct SKU stop"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (custOrders.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (inBatch) {
                                            StatusPill(
                                                label = "In Batch",
                                                bgColor = StatusHealthyContainer,
                                                textColor = OnStatusHealthyContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val custOrders = openOrdersByCustomerLower[chosenCustomer.trim().lowercase()].orEmpty()
                val filteredDirectSkus = remember(availableSelectedSkus, skuSearchQuery) {
                    val q = skuSearchQuery.trim()
                    val base = if (q.isBlank()) availableSelectedSkus else availableSelectedSkus.filter {
                        it.productName.contains(q, ignoreCase = true) || it.section.contains(q, ignoreCase = true)
                    }
                    base.sortedBy { it.productName.lowercase() }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (custOrders.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Previous Open Orders (${custOrders.size})",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = {
                                    checkedOrderIds = if (checkedOrderIds.size == custOrders.size) {
                                        emptySet()
                                    } else {
                                        custOrders.map { it.id }.toSet()
                                    }
                                }
                            ) {
                                Text(if (checkedOrderIds.size == custOrders.size) "Unselect All" else "Select All")
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = if (showDirectSkuList) 170.dp else 260.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(custOrders, key = { it.id }) { ord ->
                                val isChecked = ord.id in checkedOrderIds
                                val remKg = max(0.0, ord.totalWeightKg - ord.dispatchedWeightKg)
                                val remBags = if (ord.bagWeightKg > 0.0) {
                                    ceil(remKg / ord.bagWeightKg).toLong().coerceAtLeast(1L)
                                } else ord.orderedBags
                                val toggle = {
                                    checkedOrderIds = if (isChecked) checkedOrderIds - ord.id else checkedOrderIds + ord.id
                                }
                                Surface(
                                    color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { toggle() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { toggle() },
                                            modifier = Modifier.testTag("add_batch_order_check_${ord.id}")
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (ord.priority) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Star,
                                                        contentDescription = "Priority order",
                                                        tint = GrainAmberBright,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "${ord.orderCode} • ${ord.productName}",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = "Ordered ${ord.orderDate} • Rem: $remBags bags (${OperationsCalculator.formatKg(remKg)} kg)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!showDirectSkuList) {
                            TextButton(
                                onClick = { showDirectSkuList = true },
                                modifier = Modifier.testTag("add_batch_show_direct_skus_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Also add direct product (without PO)")
                            }
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No previous open orders found for $chosenCustomer. Select one or more products below to add a direct stop.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (showDirectSkuList) {
                        HorizontalDivider()
                        Surface(
                            color = if (addDirectAsRushOrder) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (addDirectAsRushOrder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { addDirectAsRushOrder = !addDirectAsRushOrder }
                                .testTag("add_batch_rush_order_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = addDirectAsRushOrder,
                                    onCheckedChange = { addDirectAsRushOrder = it }
                                )
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = if (addDirectAsRushOrder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Add to Orders → Rush Orders",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Unlinked items for $chosenCustomer will auto-create Rush Orders when dispatched",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = skuSearchQuery,
                            onValueChange = { skuSearchQuery = it },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search products") },
                            placeholder = { Text("Search feed SKU to add for $chosenCustomer...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_batch_direct_sku_search")
                        )
                        if (filteredDirectSkus.isEmpty()) {
                            Text(
                                text = if (sourcePlantFilter.isNotBlank()) {
                                    "No available selected products with stock in $sourcePlantFilter."
                                } else {
                                    "No available selected products with stock match this filter."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp)
                            ) {
                                items(filteredDirectSkus.take(25), key = { skuStableKey(it) }) { sku ->
                                    val key = skuStableKey(sku)
                                    val checked = key in checkedDirectSkuKeys
                                    val availKg = max(0.0, availByKey[key] ?: availByName[sku.productName.trim().lowercase()] ?: 0.0)
                                    val toggleSku = {
                                        checkedDirectSkuKeys = if (checked) checkedDirectSkuKeys - key else checkedDirectSkuKeys + key
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { toggleSku() }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = { toggleSku() }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = sku.productName,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${sku.section} • Avail: ${OperationsCalculator.formatKg(availKg)} kg",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        },
        confirmButton = {
            val chosenCustomer = activeCustomer
            if (chosenCustomer != null) {
                val custOrders = openOrdersByCustomerLower[chosenCustomer.trim().lowercase()].orEmpty()
                val chosenOrders = custOrders.filter { it.id in checkedOrderIds }
                val chosenSkus = availableSelectedSkus.filter { skuStableKey(it) in checkedDirectSkuKeys }
                val totalSelected = chosenOrders.size + chosenSkus.size
                Button(
                    onClick = { onConfirmAdd(chosenCustomer, chosenOrders, chosenSkus, addDirectAsRushOrder) },
                    enabled = totalSelected > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestEmerald,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_add_customer_to_batch_button")
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add $totalSelected Stop(s)")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
