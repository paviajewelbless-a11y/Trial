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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.GrainAmberBright
import kotlin.math.ceil
import kotlin.math.max

@Composable
fun MultiOrderDispatchPickerDialog(
    openOrders: List<CustomerOrder>,
    initiallySelectedOrderIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<CustomerOrder>) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var priorityOnly by remember { mutableStateOf(false) }
    var checkedOrderIds by remember(initiallySelectedOrderIds) {
        mutableStateOf(initiallySelectedOrderIds)
    }

    val filteredOrders = remember(openOrders, search, priorityOnly) {
        val q = search.trim()
        openOrders
            .filter { ord ->
                (!priorityOnly || ord.priority) &&
                    (q.isBlank() ||
                        ord.orderCode.contains(q, ignoreCase = true) ||
                        ord.customer.contains(q, ignoreCase = true) ||
                        ord.productName.contains(q, ignoreCase = true))
            }
            .sortedWith(
                compareByDescending<CustomerOrder> { it.priority }
                    .thenBy { it.dueDate }
                    .thenBy { it.customer.lowercase() }
                    .thenBy { it.orderCode }
            )
    }

    val selectedOrdersList = remember(openOrders, checkedOrderIds) {
        openOrders.filter { it.id in checkedOrderIds }
    }
    val selectedCustomersCount = remember(selectedOrdersList) {
        selectedOrdersList.map { it.customer.trim().lowercase() }.filter { it.isNotBlank() }.distinct().size
    }
    val selectedRemainingKg = remember(selectedOrdersList) {
        selectedOrdersList.sumOf { max(0.0, it.totalWeightKg - it.dispatchedWeightKg) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Select Customer Orders for Truck")
                Text(
                    text = "Load multiple orders & customers onto this truck trip",
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
                        if (search.isNotBlank()) {
                            IconButton(onClick = { search = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    placeholder = { Text("Search PO#, customer, or feed...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("multi_order_dispatch_picker_search")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !priorityOnly,
                        onClick = { priorityOnly = false },
                        label = { Text("All Open (${openOrders.size})") }
                    )
                    FilterChip(
                        selected = priorityOnly,
                        onClick = { priorityOnly = true },
                        label = { Text("Priority Only (${openOrders.count { it.priority }})") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${checkedOrderIds.size} order(s) • $selectedCustomersCount customer(s) • ${OperationsCalculator.formatKg(selectedRemainingKg)} kg",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Row {
                        if (filteredOrders.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    checkedOrderIds = checkedOrderIds + filteredOrders.map { it.id }
                                }
                            ) {
                                Text("Select All")
                            }
                        }
                        if (checkedOrderIds.isNotEmpty()) {
                            TextButton(onClick = { checkedOrderIds = emptySet() }) {
                                Text("Clear")
                            }
                        }
                    }
                }

                if (filteredOrders.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (openOrders.isEmpty()) {
                                "No open or partially dispatched customer orders available."
                            } else {
                                "No orders match \"$search\"."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredOrders, key = { it.id }) { ord ->
                            val isChecked = ord.id in checkedOrderIds
                            val remKg = max(0.0, ord.totalWeightKg - ord.dispatchedWeightKg)
                            val remBags = if (ord.bagWeightKg > 0.0) {
                                ceil(remKg / ord.bagWeightKg).toLong().coerceAtLeast(1L)
                            } else ord.orderedBags
                            val toggle = {
                                checkedOrderIds = if (isChecked) {
                                    checkedOrderIds - ord.id
                                } else {
                                    checkedOrderIds + ord.id
                                }
                            }

                            Surface(
                                color = if (isChecked) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
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
                                        modifier = Modifier.testTag("multi_order_dispatch_check_${ord.id}")
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
                                                text = ord.customer,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "${ord.orderCode} • ${ord.productName}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Remaining: $remBags bags (${OperationsCalculator.formatKg(remKg)} kg) • Due ${ord.dueDate}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedOrdersList) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestEmerald,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_multi_order_dispatch_picker_button")
            ) {
                Text("Load ${selectedOrdersList.size} Order(s)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
