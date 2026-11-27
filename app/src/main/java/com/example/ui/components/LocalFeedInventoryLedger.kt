package com.example.ui.components

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.FeedInventoryLevelEntity
import com.example.data.local.FeedMeasurementUnit
import com.example.domain.OperationsCalculator
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.OnStatusCriticalContainer
import com.example.ui.theme.OnStatusHealthyContainer
import com.example.ui.theme.SlateTeal
import com.example.ui.theme.StatusCriticalContainer
import com.example.ui.theme.StatusCriticalTextDark
import com.example.ui.theme.StatusHealthyContainer
import com.example.ui.theme.StatusHealthyTextDark
import com.example.viewmodel.FeedInventorySortOption
import com.example.viewmodel.FeedInventoryUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocalFeedInventoryLedger(
    state: FeedInventoryUiState,
    onSearchQueryChange: (String) -> Unit,
    onUnitFilterChange: (String) -> Unit,
    onLowStockOnlyChange: (Boolean) -> Unit,
    onSortOptionChange: (FeedInventorySortOption) -> Unit,
    onClearFilters: () -> Unit,
    onSaveProduct: (
        existingId: Long,
        productName: String,
        stockQuantity: Double,
        unitOfMeasurement: String,
        category: String,
        section: String,
        reorderThreshold: Double,
        notes: String
    ) -> Unit,
    onAdjustStock: (String, Double) -> Unit,
    onDeleteItem: (FeedInventoryLevelEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var editorOpen by remember { mutableStateOf(false) }
    var editingEntity by remember { mutableStateOf<FeedInventoryLevelEntity?>(null) }

    val isDesktop = LocalIsDesktopLayout.current
    val windowWidthDp = LocalWindowWidthDp.current
    val gridColumns = desktopGridColumns(windowWidthDp, maxCols = 3)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("local_inventory_ledger"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Room Feed Stock Ledger",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Offline-ready SQLite tracking by product name, stock quantity & unit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        editingEntity = null
                        editorOpen = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestEmerald,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("local_inv_add_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Track Product", maxLines = 1)
                }
            }
        }

        // Unit-grouped KPI Strip
        item {
            Row(
                modifier = if (isDesktop) {
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                },
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IndustrialKpiCard(
                    title = "TRACKED PRODUCTS",
                    primaryValue = "${state.totalTrackedProducts} SKUs",
                    secondaryValue = "${state.unitTotals.size} unit group(s) stored in Room",
                    icon = Icons.Filled.Storage,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(185.dp)
                )
                IndustrialKpiCard(
                    title = "LOW STOCK ALERTS",
                    primaryValue = "${state.lowStockCount} Below Min",
                    secondaryValue = "At or below reorder threshold",
                    icon = Icons.Filled.Warning,
                    accentColor = if (state.lowStockCount > 0) StatusCriticalTextDark else StatusHealthyTextDark,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(185.dp)
                )
                state.unitTotals.take(3).forEach { unitTotal ->
                    IndustrialKpiCard(
                        title = "STOCK (${unitTotal.unitOfMeasurement.uppercase()})",
                        primaryValue = "${OperationsCalculator.formatKg(unitTotal.totalQuantity)} ${unitTotal.unitOfMeasurement}",
                        secondaryValue = "${unitTotal.itemCount} tracked product(s)",
                        icon = Icons.Filled.Scale,
                        accentColor = SlateTeal,
                        modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(185.dp)
                    )
                }
            }
        }

        // Search, Unit Filter, and Sort
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Room inventory") },
                    trailingIcon = if (state.searchQuery.isNotBlank()) {
                        {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("local_inv_clear_search_button")
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    } else null,
                    placeholder = { Text("Search product name, unit (kg/bags/tons), or notes...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("local_inv_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = state.selectedUnitFilter == "ALL",
                        onClick = { onUnitFilterChange("ALL") },
                        label = { Text("All Units (${state.totalTrackedProducts})") },
                        modifier = Modifier.testTag("local_inv_unit_chip_all")
                    )
                    state.availableUnits.forEach { unit ->
                        val count = state.items.count { it.unitOfMeasurement.equals(unit, ignoreCase = true) }
                        FilterChip(
                            selected = state.selectedUnitFilter.equals(unit, ignoreCase = true),
                            onClick = { onUnitFilterChange(unit) },
                            label = { Text("$unit ($count)") },
                            modifier = Modifier.testTag("local_inv_unit_chip_$unit")
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = state.lowStockOnly,
                        onClick = { onLowStockOnlyChange(!state.lowStockOnly) },
                        leadingIcon = {
                            Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Low Stock (${state.lowStockCount})") },
                        modifier = Modifier.testTag("local_inv_low_stock_chip")
                    )
                    FeedInventorySortOption.entries.forEach { option ->
                        FilterChip(
                            selected = state.sortOption == option,
                            onClick = { onSortOptionChange(option) },
                            label = { Text("Sort: ${option.label}") }
                        )
                    }
                }
            }
        }

        if (state.filteredItems.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (state.items.isEmpty()) {
                                    "No feed products tracked in Room yet"
                                } else {
                                    "No tracked products match your filters"
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = if (state.items.isEmpty()) {
                                "Tap Track Product to record a feed product name, stock quantity, and unit of measurement locally."
                            } else {
                                "Clear the search query or switch unit filters to view all ${state.totalTrackedProducts} tracked product(s)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (state.items.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = onClearFilters,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Text("Reset Filters")
                                }
                            }
                            Button(
                                onClick = {
                                    editingEntity = null
                                    editorOpen = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ForestEmerald,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text("Add Feed Product")
                            }
                        }
                    }
                }
            }
        } else {
            desktopGridItems(
                itemsList = state.filteredItems,
                columns = gridColumns,
                keyPrefix = "room_inv",
                itemKey = { "${it.id}_${it.productName}" }
            ) { entity ->
                RoomFeedInventoryCard(
                    entity = entity,
                    isSaving = state.isSaving,
                    onAdjust = { delta -> onAdjustStock(entity.productName, delta) },
                    onEdit = {
                        editingEntity = entity
                        editorOpen = true
                    },
                    onDelete = { onDeleteItem(entity) }
                )
            }
        }
    }

    if (editorOpen) {
        RoomFeedInventoryEditorDialog(
            existing = editingEntity,
            availableUnits = state.availableUnits,
            onDismiss = { editorOpen = false },
            onSave = { name, qty, unit, category, section, threshold, notes ->
                onSaveProduct(
                    editingEntity?.id ?: 0L,
                    name,
                    qty,
                    unit,
                    category,
                    section,
                    threshold,
                    notes
                )
                editorOpen = false
            }
        )
    }
}

@Composable
private fun RoomFeedInventoryCard(
    entity: FeedInventoryLevelEntity,
    isSaving: Boolean,
    onAdjust: (Double) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val updatedLabel = remember(entity.lastUpdatedTimestamp) {
        SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(entity.lastUpdatedTimestamp))
    }
    val stepAmount = if (entity.unitOfMeasurement.equals("tons", ignoreCase = true)) 1.0 else 10.0

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (entity.isLowStock) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("local_inv_card_${entity.id}")
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entity.productName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val meta = listOf(entity.category, entity.section)
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                    Text(
                        text = if (meta.isNotBlank()) "$meta • Updated $updatedLabel" else "Updated $updatedLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusPill(
                    label = if (entity.isLowStock) "LOW STOCK" else "HEALTHY",
                    bgColor = if (entity.isLowStock) StatusCriticalContainer else StatusHealthyContainer,
                    textColor = if (entity.isLowStock) OnStatusCriticalContainer else OnStatusHealthyContainer
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "STOCK LEVEL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${OperationsCalculator.formatKg(entity.stockQuantity)} ${entity.unitOfMeasurement}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "UNIT / MIN ALERT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${entity.unitOfMeasurement} • Min ${OperationsCalculator.formatKg(entity.reorderThreshold)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (entity.notes.isNotBlank()) {
                Text(
                    text = entity.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onAdjust(-stepAmount) },
                    enabled = !isSaving && entity.stockQuantity > 0.0,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("local_inv_dec_${entity.id}")
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease stock", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${OperationsCalculator.formatKg(stepAmount)} ${entity.unitOfMeasurement}", maxLines = 1)
                }
                Button(
                    onClick = { onAdjust(stepAmount) },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestEmerald,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("local_inv_inc_${entity.id}")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase stock", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${OperationsCalculator.formatKg(stepAmount)} ${entity.unitOfMeasurement}", maxLines = 1)
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("local_inv_edit_${entity.id}")
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit ${entity.productName}")
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("local_inv_delete_${entity.id}")
                ) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "Delete ${entity.productName}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoomFeedInventoryEditorDialog(
    existing: FeedInventoryLevelEntity?,
    availableUnits: List<String>,
    onDismiss: () -> Unit,
    onSave: (
        productName: String,
        stockQuantity: Double,
        unitOfMeasurement: String,
        category: String,
        section: String,
        reorderThreshold: Double,
        notes: String
    ) -> Unit
) {
    var productName by remember(existing) { mutableStateOf(existing?.productName.orEmpty()) }
    var stockQtyText by remember(existing) {
        mutableStateOf(existing?.stockQuantity?.let { OperationsCalculator.formatKg(it).replace(",", "") }.orEmpty())
    }
    var unitText by remember(existing) { mutableStateOf(existing?.unitOfMeasurement ?: FeedMeasurementUnit.KILOGRAMS.symbol) }
    var category by remember(existing) { mutableStateOf(existing?.category ?: "Finished Feed") }
    var section by remember(existing) { mutableStateOf(existing?.section.orEmpty()) }
    var thresholdText by remember(existing) {
        mutableStateOf((existing?.reorderThreshold ?: 500.0).toString())
    }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val unitChoices = remember(availableUnits) {
        (FeedMeasurementUnit.defaultSymbols + availableUnits).distinct()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing == null) "Track Feed Product (Room)" else "Edit Tracked Feed Product",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = productName,
                    onValueChange = {
                        productName = it
                        validationError = null
                    },
                    label = { Text("Product Name") },
                    placeholder = { Text("e.g. MB BROODSOW MASH") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("local_inv_product_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockQtyText,
                        onValueChange = {
                            stockQtyText = it
                            validationError = null
                        },
                        label = { Text("Stock Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("local_inv_stock_qty_input")
                    )
                    OutlinedTextField(
                        value = unitText,
                        onValueChange = {
                            unitText = it
                            validationError = null
                        },
                        label = { Text("Unit") },
                        placeholder = { Text("kg / bags / tons") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("local_inv_unit_input")
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    unitChoices.forEach { preset ->
                        FilterChip(
                            selected = unitText.equals(preset, ignoreCase = true),
                            onClick = { unitText = preset },
                            label = { Text(preset) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = { Text("Section") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = {
                        thresholdText = it
                        validationError = null
                    },
                    label = { Text("Low Stock Alert Threshold (${unitText.ifBlank { "kg" }})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("local_inv_threshold_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Warehouse / Bin Notes (Optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                validationError?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = stockQtyText.trim().toDoubleOrNull()
                    val threshold = thresholdText.trim().ifBlank { "500" }.toDoubleOrNull()
                    when {
                        productName.isBlank() -> validationError = "Enter a product name."
                        qty == null || qty < 0.0 -> validationError = "Enter a valid non-negative stock quantity."
                        unitText.isBlank() -> validationError = "Enter a unit of measurement (e.g. kg, bags, tons)."
                        threshold == null || threshold < 0.0 -> validationError = "Enter a valid reorder threshold."
                        else -> onSave(productName, qty, unitText, category, section, threshold, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestEmerald,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("local_inv_save_button")
            ) {
                Text(if (existing == null) "Save Product" else "Update")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text("Cancel")
            }
        }
    )
}
