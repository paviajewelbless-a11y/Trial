package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import com.example.model.OrderStatus
import com.example.model.PlantSection
import com.example.model.ScheduleRun
import com.example.model.SkuInventorySummary
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.GrainAmber
import com.example.ui.theme.OnStatusHealthyContainer
import com.example.ui.theme.OnStatusInProgressContainer
import com.example.ui.theme.OnStatusWatchContainer
import com.example.ui.theme.StatusHealthyContainer
import com.example.ui.theme.StatusInProgressContainer
import com.example.ui.theme.StatusWatchContainer
import kotlin.math.max

@Composable
fun MultiOrderLinkPickerDialog(
    productName: String,
    allOrders: List<CustomerOrder>,
    initiallySelectedIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<CustomerOrder>) -> Unit
) {
    val activeOrders = remember(allOrders, initiallySelectedIds) {
        allOrders
            .filter {
                it.status == OrderStatus.OPEN.label ||
                    it.status == OrderStatus.PARTIALLY_DISPATCHED.label ||
                    it.id in initiallySelectedIds
            }
            .sortedWith(
                compareByDescending<CustomerOrder> { it.productName.equals(productName, ignoreCase = true) }
                    .thenByDescending { it.priority }
                    .thenByDescending { it.orderDate }
            )
    }
    val skuOrders = remember(activeOrders, productName) {
        activeOrders.filter { it.productName.equals(productName, ignoreCase = true) }
    }

    var search by remember { mutableStateOf("") }
    var onlyThisSku by remember(skuOrders.isNotEmpty()) { mutableStateOf(skuOrders.isNotEmpty()) }
    var checkedIds by remember(initiallySelectedIds) { mutableStateOf(initiallySelectedIds) }

    val filtered = remember(activeOrders, skuOrders, onlyThisSku, search) {
        val base = if (onlyThisSku) skuOrders else activeOrders
        val q = search.trim()
        if (q.isBlank()) base
        else base.filter {
            it.customer.contains(q, ignoreCase = true) ||
                it.orderCode.contains(q, ignoreCase = true) ||
                it.productName.contains(q, ignoreCase = true)
        }
    }

    val selectedOrders = remember(activeOrders, checkedIds) {
        activeOrders.filter { it.id in checkedIds }
    }
    val totalSelectedRemainingKg = selectedOrders.sumOf { max(0.0, it.totalWeightKg - it.dispatchedWeightKg) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Link Customer Orders")
                Text(
                    text = productName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search customer orders") },
                    placeholder = { Text("Search customer name or PO code...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_link_picker_search")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = onlyThisSku,
                        onClick = { onlyThisSku = true },
                        label = { Text("This SKU (${skuOrders.size})") }
                    )
                    FilterChip(
                        selected = !onlyThisSku,
                        onClick = { onlyThisSku = false },
                        label = { Text("All Open Orders (${activeOrders.size})") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedOrders.size} linked (${OperationsCalculator.formatKg(totalSelectedRemainingKg)} kg)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row {
                        TextButton(
                            onClick = {
                                checkedIds = checkedIds + filtered.map { it.id }.filter { it.isNotBlank() }
                            },
                            enabled = filtered.isNotEmpty(),
                            modifier = Modifier.testTag("order_link_select_all_button")
                        ) {
                            Text("Select All")
                        }
                        if (checkedIds.isNotEmpty()) {
                            TextButton(onClick = { checkedIds = emptySet() }) {
                                Text("Clear")
                            }
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    Text(
                        text = if (onlyThisSku && activeOrders.isNotEmpty()) {
                            "No open orders for $productName. Tap 'All Open Orders' above to link another customer order."
                        } else {
                            "No matching customer orders found."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 310.dp)
                    ) {
                        items(filtered, key = { it.id.ifBlank { it.orderCode } }) { ord ->
                            val isChecked = ord.id in checkedIds
                            val remKg = max(0.0, ord.totalWeightKg - ord.dispatchedWeightKg)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (ord.id.isNotBlank()) {
                                            checkedIds = if (isChecked) checkedIds - ord.id else checkedIds + ord.id
                                        }
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (ord.id.isNotBlank()) {
                                            checkedIds = if (checked) checkedIds + ord.id else checkedIds - ord.id
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = ord.customer,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (ord.priority) {
                                            StatusPill("PRIORITY", StatusWatchContainer, OnStatusWatchContainer)
                                        }
                                    }
                                    Text(
                                        text = "${ord.orderCode} • ${ord.productName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${ord.orderedBags} bags • Rem: ${OperationsCalculator.formatKg(remKg)} kg • Due ${ord.dueDate}",
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
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedOrders) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestEmerald,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_order_links_button")
            ) {
                Text("Link (${selectedOrders.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun WhoseOrderDialog(
    productName: String,
    run: ScheduleRun?,
    linkedOrders: List<CustomerOrder>,
    unresolvedLinkedCodes: List<String> = emptyList(),
    skuOpenOrders: List<CustomerOrder>,
    canEditLinks: Boolean,
    onEditLinkedOrders: (() -> Unit)? = null,
    onLinkAllOpenOrders: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.People,
                contentDescription = "Customer orders for production run",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Column {
                Text("Whose Order?")
                Text(
                    text = if (run != null) {
                        "${run.productName} • ${run.scheduledDate} (${run.targetBags} bags)"
                    } else {
                        "$productName • Open Customer Orders"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (run != null) {
                        item {
                            Text(
                                text = "Linked to This Run (${linkedOrders.size + unresolvedLinkedCodes.size})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (linkedOrders.isEmpty() && unresolvedLinkedCodes.isEmpty()) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No customer order explicitly linked yet. Use Edit Linked Orders below to attach one or more orders.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        } else {
                            items(linkedOrders, key = { "linked_${it.id.ifBlank { it.orderCode }}" }) { ord ->
                                OrderDetailRowCard(order = ord, highlightLinked = true)
                            }
                            items(unresolvedLinkedCodes, key = { "code_$it" }) { code ->
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Linked Order Ref: $code",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (skuOpenOrders.isNotEmpty()) {
                        item {
                            Text(
                                text = if (run != null) {
                                    "Other Open Orders for $productName (${skuOpenOrders.size})"
                                } else {
                                    "All Open Customer Orders (${skuOpenOrders.size})"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = if (run != null) 4.dp else 0.dp)
                            )
                        }
                        items(skuOpenOrders, key = { "open_${it.id.ifBlank { it.orderCode }}" }) { ord ->
                            OrderDetailRowCard(order = ord, highlightLinked = false)
                        }
                    }
                }

                if (canEditLinks && (onEditLinkedOrders != null || (onLinkAllOpenOrders != null && skuOpenOrders.isNotEmpty()))) {
                    HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (onEditLinkedOrders != null) {
                            Button(
                                onClick = onEditLinkedOrders,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ForestEmerald,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("whose_order_edit_links_button")
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Select / Edit Linked Orders")
                            }
                        }
                        if (onLinkAllOpenOrders != null && skuOpenOrders.isNotEmpty()) {
                            OutlinedButton(
                                onClick = onLinkAllOpenOrders,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("whose_order_link_all_button")
                            ) {
                                Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Link All Open Orders (${linkedOrders.size + skuOpenOrders.size})")
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
private fun OrderDetailRowCard(
    order: CustomerOrder,
    highlightLinked: Boolean
) {
    val remKg = max(0.0, order.totalWeightKg - order.dispatchedWeightKg)
    val (sBg, sFg) = when (order.status) {
        OrderStatus.FULFILLED.label -> StatusHealthyContainer to OnStatusHealthyContainer
        OrderStatus.PARTIALLY_DISPATCHED.label -> StatusInProgressContainer to OnStatusInProgressContainer
        else -> StatusWatchContainer to OnStatusWatchContainer
    }
    Surface(
        color = if (highlightLinked) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (order.priority) 1.5.dp else 1.dp,
            color = if (order.priority) GrainAmber else MaterialTheme.colorScheme.outlineVariant
        ),
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
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = order.customer,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (order.priority) {
                        StatusPill("PRIORITY", StatusWatchContainer, OnStatusWatchContainer)
                    }
                }
                StatusPill(order.status, sBg, sFg)
            }
            Text(
                text = "${order.orderCode} • ${order.orderedBags} bags (${OperationsCalculator.formatKg(order.totalWeightKg)} kg)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Remaining: ${OperationsCalculator.formatKg(remKg)} kg • Ordered ${order.orderDate} • Due ${order.dueDate}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MultiOrderSchedulePickerDialog(
    openOrders: List<CustomerOrder>,
    summaries: List<SkuInventorySummary> = emptyList(),
    initiallySelectedOrderIds: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onConfirm: (List<CustomerOrder>) -> Unit
) {
    val sortedOrders = remember(openOrders) {
        openOrders
            .filter {
                val key = it.id.ifBlank { it.orderCode }
                it.status == OrderStatus.OPEN.label ||
                    it.status == OrderStatus.PARTIALLY_DISPATCHED.label ||
                    key in initiallySelectedOrderIds ||
                    it.id in initiallySelectedOrderIds
            }
            .sortedWith(
                compareByDescending<CustomerOrder> { it.priority }
                    .thenBy { it.dueDate }
                    .thenByDescending { it.orderDate }
            )
    }
    val summaryByProduct = remember(summaries) {
        summaries.associateBy { it.sku.productName.trim().lowercase() }
    }

    var search by remember { mutableStateOf("") }
    var sectionFilter by remember { mutableStateOf("ALL") }
    var checkedIds by remember(initiallySelectedOrderIds) { mutableStateOf(initiallySelectedOrderIds) }

    val filtered = remember(sortedOrders, search, sectionFilter, summaryByProduct) {
        val q = search.trim()
        sortedOrders.filter { ord ->
            val sec = summaryByProduct[ord.productName.trim().lowercase()]?.sku?.section
            val matchesSec = sectionFilter == "ALL" || sec == sectionFilter
            val matchesQuery = q.isBlank() ||
                ord.customer.contains(q, ignoreCase = true) ||
                ord.orderCode.contains(q, ignoreCase = true) ||
                ord.productName.contains(q, ignoreCase = true)
            matchesSec && matchesQuery
        }
    }

    val selectedOrders = remember(sortedOrders, checkedIds) {
        sortedOrders.filter {
            val key = it.id.ifBlank { it.orderCode }
            key in checkedIds || it.id in checkedIds
        }
    }
    val groupedRunCount = remember(selectedOrders) {
        selectedOrders.map { it.productName.trim().lowercase() }.distinct().size
    }
    val totalSelectedRemainingKg = selectedOrders.sumOf { max(0.0, it.totalWeightKg - it.dispatchedWeightKg) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Select Orders to Schedule")
                Text(
                    text = "Check multiple orders — orders for the same SKU combine into one run",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search orders") },
                    trailingIcon = {
                        if (search.isNotEmpty()) {
                            IconButton(onClick = { search = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear order search")
                            }
                        }
                    },
                    placeholder = { Text("Search customer, PO code, or product...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("schedule_order_picker_search")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (listOf("ALL") + PlantSection.entries.map { it.label }).forEach { sec ->
                        FilterChip(
                            selected = sectionFilter == sec,
                            onClick = { sectionFilter = sec },
                            label = { Text(if (sec == "ALL") "All" else sec, maxLines = 1) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${selectedOrders.size} order(s) • $groupedRunCount run(s)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Remaining: ${OperationsCalculator.formatKg(totalSelectedRemainingKg)} kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row {
                        TextButton(
                            onClick = {
                                checkedIds = checkedIds + filtered.map { it.id.ifBlank { it.orderCode } }.filter { it.isNotBlank() }
                            },
                            enabled = filtered.isNotEmpty(),
                            modifier = Modifier.testTag("schedule_order_picker_select_all")
                        ) {
                            Text("Select All")
                        }
                        if (checkedIds.isNotEmpty()) {
                            TextButton(onClick = { checkedIds = emptySet() }) {
                                Text("Clear")
                            }
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    Text(
                        text = "No matching open customer orders found.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 310.dp)
                    ) {
                        items(filtered, key = { it.id.ifBlank { it.orderCode } }) { ord ->
                            val ordKey = ord.id.ifBlank { ord.orderCode }
                            val isChecked = ordKey in checkedIds || ord.id in checkedIds
                            val remKg = max(0.0, ord.totalWeightKg - ord.dispatchedWeightKg)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (ordKey.isNotBlank()) {
                                            checkedIds = if (isChecked) checkedIds - ordKey - ord.id else checkedIds + ordKey
                                        }
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (ordKey.isNotBlank()) {
                                            checkedIds = if (checked) checkedIds + ordKey else checkedIds - ordKey - ord.id
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = ord.customer,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (ord.priority) {
                                            StatusPill("PRIORITY", StatusWatchContainer, OnStatusWatchContainer)
                                        }
                                    }
                                    Text(
                                        text = "${ord.orderCode} • ${ord.productName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${ord.orderedBags} bags • Rem: ${OperationsCalculator.formatKg(remKg)} kg • Due ${ord.dueDate}",
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
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedOrders) },
                enabled = selectedOrders.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestEmerald,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_schedule_orders_picker_button")
            ) {
                Text("Use Orders (${selectedOrders.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
