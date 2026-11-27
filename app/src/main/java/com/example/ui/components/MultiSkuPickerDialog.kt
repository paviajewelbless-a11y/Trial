package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.model.PlantSection
import com.example.model.SkuEntity
import com.example.model.SkuInventorySummary
import com.example.ui.theme.ForestEmerald

internal fun skuStableKey(sku: SkuEntity): String =
    sku.id.ifBlank { "${sku.section}_${sku.productName}" }

data class MultiOrderItemDraft(
    val key: String,
    val sku: SkuEntity,
    val bagsText: String,
    val bagWtText: String
)

data class MultiScheduleItemDraft(
    val key: String,
    val sku: SkuEntity,
    val targetBagsText: String,
    val lineRemarks: String,
    val linkedOrderId: String = "",
    val linkedOrderCode: String = "",
    val linkedCustomer: String = "",
    val linkedOrderIds: List<String> = emptyList(),
    val linkedOrderCodes: List<String> = emptyList(),
    val linkedCustomers: List<String> = emptyList()
)

data class MultiDispatchItemDraft(
    val key: String,
    val sku: SkuEntity,
    val bagsText: String,
    val bagWtText: String,
    val customer: String = "",
    val drReference: String = "",
    val linkedOrder: CustomerOrder? = null,
    val returnedBagsText: String = "",
    val addAsRushOrder: Boolean = false
)

data class ScheduleDraftSeed(
    val sku: SkuEntity,
    val bags: Long,
    val linkedOrder: CustomerOrder? = null,
    val linkedOrders: List<CustomerOrder> = listOfNotNull(linkedOrder)
)

@Composable
fun MultiSkuPickerDialog(
    title: String,
    skus: List<SkuEntity>,
    summaries: List<SkuInventorySummary> = emptyList(),
    initiallySelectedKeys: Set<String>,
    singleSelection: Boolean = false,
    onlyAvailablePositiveStock: Boolean = false,
    allowedPriorityKeys: Set<String> = emptySet(),
    restrictToAllowedKeys: Boolean = allowedPriorityKeys.isNotEmpty(),
    initialSectionFilter: String = "ALL",
    onDismiss: () -> Unit,
    onConfirm: (List<SkuEntity>) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var sectionFilter by remember(initialSectionFilter) {
        mutableStateOf(initialSectionFilter.ifBlank { "ALL" })
    }
    var checkedKeys by remember(initiallySelectedKeys) { mutableStateOf(initiallySelectedKeys) }

    val availByKey = remember(summaries) {
        summaries.associate { skuStableKey(it.sku) to it.availableStockKg }
    }
    val availByName = remember(summaries) {
        summaries.associate { it.sku.productName to it.availableStockKg }
    }
    val availByNameLower = remember(summaries) {
        summaries.associate { it.sku.productName.trim().lowercase() to it.availableStockKg }
    }

    val eligibleSkus = remember(
        skus,
        availByKey,
        availByNameLower,
        onlyAvailablePositiveStock,
        allowedPriorityKeys,
        restrictToAllowedKeys
    ) {
        when {
            onlyAvailablePositiveStock -> OperationsCalculator.filterDispatchAvailableSelectedSkus(
                skus = skus,
                availByKey = availByKey,
                availByName = availByNameLower,
                selectedPriorityKeys = allowedPriorityKeys,
                restrictToSelectedKeys = restrictToAllowedKeys,
                sourcePlantFilter = ""
            )
            restrictToAllowedKeys -> skus.filter {
                OperationsCalculator.dispatchSkuPriorityKey(it) in allowedPriorityKeys
            }
            else -> skus
        }
    }

    val filtered = remember(eligibleSkus, search, sectionFilter) {
        val q = search.trim()
        eligibleSkus
            .filter { sku ->
                (sectionFilter == "ALL" || sku.section == sectionFilter) &&
                    (q.isBlank() ||
                        sku.productName.contains(q, ignoreCase = true) ||
                        sku.section.contains(q, ignoreCase = true))
            }
            .sortedWith(compareBy({ it.productName.lowercase() }, { it.section }))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onlyAvailablePositiveStock) {
                    Text(
                        text = if (restrictToAllowedKeys) {
                            "Showing available selected products with stock (> 0 kg)."
                        } else {
                            "Showing available products with stock (> 0 kg)."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search items") },
                    placeholder = { Text("Search raw material or SKU...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("multi_sku_picker_search")
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
                    Text(
                        text = "${checkedKeys.size} selected",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row {
                        if (!singleSelection) {
                            TextButton(
                                onClick = {
                                    checkedKeys = checkedKeys + filtered.map { skuStableKey(it) }
                                }
                            ) {
                                Text("Select Visible")
                            }
                        }
                        if (checkedKeys.isNotEmpty()) {
                            TextButton(onClick = { checkedKeys = emptySet() }) {
                                Text("Clear")
                            }
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    Text(
                        text = if (onlyAvailablePositiveStock) {
                            "No available selected products with stock match this filter."
                        } else {
                            "No matching SKUs or raw materials found."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                    ) {
                        items(filtered, key = { skuStableKey(it) }) { sku ->
                            val key = skuStableKey(sku)
                            val isChecked = key in checkedKeys
                            val availKg = availByKey[key] ?: availByName[sku.productName] ?: 0.0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        checkedKeys = if (singleSelection) {
                                            setOf(key)
                                        } else if (isChecked) {
                                            checkedKeys - key
                                        } else {
                                            checkedKeys + key
                                        }
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        checkedKeys = if (singleSelection) {
                                            if (checked) setOf(key) else emptySet()
                                        } else if (checked) {
                                            checkedKeys + key
                                        } else {
                                            checkedKeys - key
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sku.productName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${sku.section} • ${sku.standardBagWeightKg}kg • Avail: ${OperationsCalculator.formatKg(availKg)} kg",
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
                onClick = {
                    val selectedList = skus.filter { skuStableKey(it) in checkedKeys }
                    onConfirm(selectedList)
                },
                enabled = !singleSelection || checkedKeys.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestEmerald,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_multi_sku_picker_button")
            ) {
                Text(if (singleSelection) "Select Item" else "Apply (${checkedKeys.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
