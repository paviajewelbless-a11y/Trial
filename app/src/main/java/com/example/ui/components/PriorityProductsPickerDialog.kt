package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
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
import com.example.data.TabPreferencesStore
import com.example.domain.OperationsCalculator
import com.example.model.PlantSection
import com.example.model.SkuInventorySummary
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.GrainAmberBright

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PriorityProductsPickerDialog(
    summaries: List<SkuInventorySummary>,
    selectedKeys: Set<String>,
    onDismiss: () -> Unit,
    onSaveSelection: (Set<String>) -> Unit,
    onResetAutoActive: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var sectionFilter by remember { mutableStateOf("ALL") }
    var showOnlySelected by remember { mutableStateOf(false) }
    var draftKeys by remember(selectedKeys) { mutableStateOf(selectedKeys) }

    val autoActiveKeys = remember(summaries) {
        summaries
            .filter { TabPreferencesStore.isAutoActivePriority(it) }
            .map { TabPreferencesStore.skuPriorityKey(it.sku) }
            .toSet()
    }

    val filtered = remember(summaries, searchQuery, sectionFilter, showOnlySelected, draftKeys) {
        val q = searchQuery.trim()
        summaries
            .filter { item ->
                val key = TabPreferencesStore.skuPriorityKey(item.sku)
                val matchesSection = sectionFilter == "ALL" || item.sku.section == sectionFilter
                val matchesSelected = !showOnlySelected || key in draftKeys
                val matchesSearch = q.isBlank() ||
                    item.sku.productName.contains(q, ignoreCase = true) ||
                    item.sku.section.contains(q, ignoreCase = true) ||
                    item.sku.defaultRemarks.contains(q, ignoreCase = true)
                matchesSection && matchesSelected && matchesSearch
            }
            .sortedWith(
                compareByDescending<SkuInventorySummary> { TabPreferencesStore.skuPriorityKey(it.sku) in draftKeys }
                    .thenByDescending { TabPreferencesStore.isAutoActivePriority(it) }
                    .thenBy { it.sku.productName.lowercase() }
            )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Select Priority Products",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Checked products stay visible when non-priority products are hidden.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search priority products") },
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
                    placeholder = { Text("Search feed product or section...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("priority_picker_search")
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

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = showOnlySelected,
                        onClick = { showOnlySelected = !showOnlySelected },
                        label = { Text("Selected (${draftKeys.size})") },
                        modifier = Modifier.testTag("priority_picker_only_selected_chip")
                    )
                    TextButton(
                        onClick = { draftKeys = autoActiveKeys },
                        modifier = Modifier.testTag("priority_picker_auto_active_button")
                    ) {
                        Text("Active (${autoActiveKeys.size})")
                    }
                    TextButton(
                        onClick = {
                            draftKeys = draftKeys + filtered.map { TabPreferencesStore.skuPriorityKey(it.sku) }
                        },
                        modifier = Modifier.testTag("priority_picker_select_visible_button")
                    ) {
                        Text("Select Visible")
                    }
                    if (draftKeys.isNotEmpty()) {
                        TextButton(
                            onClick = { draftKeys = emptySet() },
                            modifier = Modifier.testTag("priority_picker_clear_button")
                        ) {
                            Text("Clear")
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No products match your filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                    ) {
                        items(
                            items = filtered,
                            key = { it.sku.id.ifBlank { "${it.sku.section}_${it.sku.productName}" } }
                        ) { item ->
                            val key = TabPreferencesStore.skuPriorityKey(item.sku)
                            val isChecked = key in draftKeys
                            val toggle = {
                                draftKeys = if (isChecked) draftKeys - key else draftKeys + key
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clickable { toggle() }
                                    .padding(vertical = 6.dp, horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { toggle() },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.sku.productName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val stockText = "${OperationsCalculator.formatBags(item.availableBags)}b avail"
                                    val orderText = if (item.openOrdersKg > 0.0) {
                                        " • ${OperationsCalculator.formatKg(item.openOrdersKg)} kg ordered"
                                    } else ""
                                    Text(
                                        text = "${item.sku.section} • $stockText$orderText",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = if (isChecked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = if (isChecked) "Priority product" else "Non-priority product",
                                    tint = if (isChecked) GrainAmberBright else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onResetAutoActive,
                    modifier = Modifier.testTag("reset_priority_products_button")
                ) {
                    Text("Reset Auto")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveSelection(draftKeys) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestEmerald,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("save_priority_products_button")
            ) {
                Text("Save (${draftKeys.size})")
            }
        }
    )
}
