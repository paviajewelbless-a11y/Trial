package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.model.CustomerOrder
import com.example.model.DispatchRecord
import com.example.model.DispatchStatus
import com.example.model.PlantSection
import com.example.model.ReprocessItem
import com.example.ui.components.IndustrialKpiCard
import com.example.ui.components.LocalIsDesktopLayout
import com.example.ui.components.LocalWindowWidthDp
import com.example.ui.components.StatusPill
import com.example.ui.components.SwipeLeftActionsContainer
import com.example.ui.components.desktopGridColumns
import com.example.ui.components.desktopGridItems
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
import com.example.ui.theme.StatusWatchTextDark
import com.example.viewmodel.OpsUiState

private data class TruckTripGroup(
    val key: String,
    val date: String,
    val truckNumber: String,
    val driverName: String,
    val helperName: String,
    val stops: List<DispatchRecord>
)

private fun dispatchTripKey(record: DispatchRecord): String {
    val truck = record.truckNumber.trim().lowercase()
    val driver = record.driverName.trim().lowercase()
    val dr = record.drReference.trim().lowercase()
    return when {
        truck.isNotBlank() -> "${record.date}|TRK|$truck|$driver"
        dr.isNotBlank() -> "${record.date}|DR|$dr"
        else -> "${record.date}|ID|${record.id}"
    }
}

@Composable
fun DispatchedScreen(
    state: OpsUiState.Ready,
    preselectedPlant: String = "",
    onPreselectPlant: (String) -> Unit = {},
    skuSectionByProductName: Map<String, String> = emptyMap(),
    onLogDispatch: () -> Unit,
    onEditDispatch: (DispatchRecord) -> Unit,
    onDeleteDispatch: (DispatchRecord) -> Unit,
    onAddReturnToReprocess: (DispatchRecord) -> Unit = {},
    onOpenReprocessTab: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }
    var viewByTruck by remember { mutableStateOf(false) }
    var pendingDeleteDispatch by remember { mutableStateOf<DispatchRecord?>(null) }

    val ordersById = remember(state.orders) {
        state.orders.associateBy { it.id }
    }
    val reprocessByDispatchId = remember(state.reprocessItems) {
        state.reprocessItems
            .filter { it.sourceDispatchId.isNotBlank() }
            .associateBy { it.sourceDispatchId }
    }

    val totalNetDispatchedKg = remember(state.dispatches) {
        state.dispatches.sumOf { it.effectiveDispatchedWeightKg() }
    }
    val deliveredList = remember(state.dispatches) {
        state.dispatches.filter { it.resolvedStatusLabel() == DispatchStatus.DELIVERED.label }
    }
    val returnedList = remember(state.dispatches) {
        state.dispatches.filter { it.resolvedStatusLabel() == DispatchStatus.RETURNED.label }
    }
    val heldList = remember(state.dispatches) {
        state.dispatches.filter { it.resolvedStatusLabel() == DispatchStatus.HELD.label }
    }
    val totalReturnedBags = remember(returnedList) { returnedList.sumOf { it.returnedBags } }
    val totalReturnedKg = remember(returnedList) { returnedList.sumOf { it.returnedWeightKg } }

    val filteredDispatches = remember(state.dispatches, ordersById, searchQuery, statusFilter) {
        val q = searchQuery.trim()
        state.dispatches.filter { d ->
            val resolved = d.resolvedStatusLabel()
            val linkedOrderCode = ordersById[d.linkedOrderId]?.orderCode.orEmpty()
            val matchesStatus = statusFilter == "ALL" || resolved.equals(statusFilter, ignoreCase = true)
            val matchesQuery = q.isBlank() ||
                d.drReference.contains(q, ignoreCase = true) ||
                d.customer.contains(q, ignoreCase = true) ||
                d.productName.contains(q, ignoreCase = true) ||
                linkedOrderCode.contains(q, ignoreCase = true) ||
                d.driverName.contains(q, ignoreCase = true) ||
                d.truckNumber.contains(q, ignoreCase = true) ||
                d.helperName.contains(q, ignoreCase = true) ||
                d.returnReason.contains(q, ignoreCase = true) ||
                d.heldRemarks.contains(q, ignoreCase = true) ||
                d.date.contains(q, ignoreCase = true)
            matchesStatus && matchesQuery
        }.sortedWith(
            compareByDescending<DispatchRecord> { it.date }
                .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                .thenByDescending { it.drReference }
        )
    }

    val truckTrips = remember(filteredDispatches) {
        filteredDispatches
            .groupBy { dispatchTripKey(it) }
            .entries
            .map { (key, stops) ->
                val lead = stops.first()
                TruckTripGroup(
                    key = key,
                    date = lead.date,
                    truckNumber = stops.firstOrNull { it.truckNumber.isNotBlank() }?.truckNumber.orEmpty(),
                    driverName = stops.firstOrNull { it.driverName.isNotBlank() }?.driverName.orEmpty(),
                    helperName = stops.firstOrNull { it.helperName.isNotBlank() }?.helperName.orEmpty(),
                    stops = stops
                )
            }
    }

    val tripContextByRecordKey = remember(state.dispatches) {
        state.dispatches.groupBy { dispatchTripKey(it) }.mapValues { (_, stops) ->
            stops.size to stops.map { it.customer.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }.size
        }
    }

    val isDesktop = LocalIsDesktopLayout.current
    val windowWidthDp = LocalWindowWidthDp.current
    val gridColumns = desktopGridColumns(windowWidthDp, maxCols = 2)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                        text = "Dispatched & Delivery Log",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "DRs, driver & truck tracking, returns & held shipments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onLogDispatch,
                    modifier = Modifier.testTag("dispatched_tab_add_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestEmerald,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Dispatch", maxLines = 1)
                }
            }
        }

        // KPI Summary Strip
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
                    title = "NET DISPATCHED",
                    primaryValue = "${OperationsCalculator.formatKg(totalNetDispatchedKg)} kg",
                    secondaryValue = "${state.dispatches.size} DR deliveries",
                    icon = Icons.Filled.LocalShipping,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(195.dp)
                )
                IndustrialKpiCard(
                    title = "DELIVERED",
                    primaryValue = "${deliveredList.size} trips",
                    secondaryValue = "${OperationsCalculator.formatKg(deliveredList.sumOf { it.effectiveDispatchedWeightKg() })} kg completed",
                    icon = Icons.Filled.CheckCircle,
                    accentColor = StatusHealthyTextDark,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(180.dp)
                )
                IndustrialKpiCard(
                    title = "RETURNED",
                    primaryValue = "${returnedList.size} trips",
                    secondaryValue = "$totalReturnedBags bags (${OperationsCalculator.formatKg(totalReturnedKg)} kg)",
                    icon = Icons.Filled.RestartAlt,
                    accentColor = GrainAmberBright,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(190.dp)
                )
                IndustrialKpiCard(
                    title = "HELD",
                    primaryValue = "${heldList.size} trips",
                    secondaryValue = if (heldList.isEmpty()) "All clear" else "Requires release",
                    icon = Icons.Filled.PauseCircle,
                    accentColor = StatusCriticalTextDark,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(175.dp)
                )
            }
        }

        // Preselected Source Plant Strip ("Came From Plant")
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Factory,
                            contentDescription = "Preselect dispatch source plant",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Dispatch From Plant: ${preselectedPlant.ifBlank { "All Plants" }}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = preselectedPlant.isBlank(),
                            onClick = { onPreselectPlant("") },
                            label = { Text("All Plants", maxLines = 1) },
                            modifier = Modifier.testTag("dispatched_preselect_plant_all")
                        )
                        PlantSection.entries.forEach { plant ->
                            FilterChip(
                                selected = preselectedPlant.equals(plant.label, ignoreCase = true),
                                onClick = { onPreselectPlant(plant.label) },
                                label = { Text(plant.label, maxLines = 1) },
                                modifier = Modifier.testTag("dispatched_preselect_plant_${plant.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // Search & Filter Controls
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search dispatches") },
                    placeholder = { Text("Search DR#, PO#, customer, item, driver, truck...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dispatched_search_input")
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !viewByTruck,
                        onClick = { viewByTruck = false },
                        leadingIcon = { Icon(Icons.Filled.ViewAgenda, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("All Stops (${filteredDispatches.size})") },
                        modifier = Modifier.testTag("dispatch_view_all")
                    )
                    FilterChip(
                        selected = viewByTruck,
                        onClick = { viewByTruck = true },
                        leadingIcon = { Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("By Truck / Trip (${truckTrips.size})") },
                        modifier = Modifier.testTag("dispatch_view_by_truck")
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val countByStatus = mapOf(
                        "ALL" to state.dispatches.size,
                        DispatchStatus.DELIVERED.label to deliveredList.size,
                        DispatchStatus.RETURNED.label to returnedList.size,
                        DispatchStatus.HELD.label to heldList.size
                    )
                    listOf(
                        "ALL",
                        DispatchStatus.DELIVERED.label,
                        DispatchStatus.RETURNED.label,
                        DispatchStatus.HELD.label
                    ).forEach { st ->
                        val count = countByStatus[st] ?: 0
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = st },
                            label = { Text(if (st == "ALL") "All ($count)" else "$st ($count)") },
                            modifier = Modifier.testTag("dispatch_filter_${st.lowercase()}")
                        )
                    }
                    if (searchQuery.isNotBlank() || statusFilter != "ALL") {
                        TextButton(
                            onClick = {
                                searchQuery = ""
                                statusFilter = "ALL"
                            },
                            modifier = Modifier.testTag("dispatched_reset_filters_button")
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset")
                        }
                    }
                }
            }
        }

        if (filteredDispatches.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (state.dispatches.isEmpty()) {
                                "No dispatches logged yet. Tap \"Log Dispatch\" to record a delivery with driver, truck, DR number, returns, or held status."
                            } else {
                                "No dispatches match the current search or status filter."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (state.dispatches.isEmpty()) {
                            Button(
                                onClick = onLogDispatch,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ForestEmerald,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Log First Dispatch")
                            }
                        }
                    }
                }
            }
        } else if (viewByTruck) {
            desktopGridItems(
                itemsList = truckTrips,
                columns = gridColumns,
                keyPrefix = "truck_trip",
                itemKey = { it.key }
            ) { trip ->
                TruckTripCard(
                    trip = trip,
                    ordersById = ordersById,
                    reprocessByDispatchId = reprocessByDispatchId,
                    onEditStop = onEditDispatch,
                    onDeleteStop = { pendingDeleteDispatch = it },
                    onAddReturnToReprocess = onAddReturnToReprocess,
                    onOpenReprocessTab = onOpenReprocessTab
                )
            }
        } else {
            desktopGridItems(
                itemsList = filteredDispatches,
                columns = gridColumns,
                keyPrefix = "dispatch",
                itemKey = { it.id.ifBlank { "${it.drReference}_${it.date}_${it.productName}" } }
            ) { record ->
                val linkedOrder = ordersById[record.linkedOrderId]
                val tripCounts = tripContextByRecordKey[dispatchTripKey(record)]
                val linkedReprocess = reprocessByDispatchId[record.id]
                SwipeLeftActionsContainer(
                    itemKey = record.id.ifBlank { "${record.drReference}_${record.date}" },
                    editTestTag = "dispatch_swipe_edit_${record.id}",
                    deleteTestTag = "dispatch_swipe_delete_${record.id}",
                    onEdit = { onEditDispatch(record) },
                    onDelete = { pendingDeleteDispatch = record }
                ) {
                    DispatchLogCard(
                        record = record,
                        resolvedSourcePlant = record.sourcePlant.ifBlank {
                            skuSectionByProductName[record.productName.trim().lowercase()].orEmpty()
                        },
                        linkedOrder = linkedOrder,
                        reprocessItem = linkedReprocess,
                        tripStopCount = tripCounts?.first ?: 1,
                        tripCustomerCount = tripCounts?.second ?: 1,
                        onEdit = { onEditDispatch(record) },
                        onSendToReprocess = { onAddReturnToReprocess(record) },
                        onOpenReprocessTab = onOpenReprocessTab
                    )
                }
            }
        }
    }

    pendingDeleteDispatch?.let { toDelete ->
        AlertDialog(
            onDismissRequest = { pendingDeleteDispatch = null },
            icon = { Icon(Icons.Filled.Warning, contentDescription = "Delete dispatch", tint = StatusCriticalTextDark) },
            title = { Text("Delete Dispatch ${toDelete.drReference}?") },
            text = {
                Text(
                    "Remove ${toDelete.drReference} (${toDelete.productName} → ${toDelete.customer}, ${OperationsCalculator.formatKg(toDelete.totalWeightKg)} kg)? Stock and linked order progress will be adjusted accordingly."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteDispatch(toDelete)
                        pendingDeleteDispatch = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusCritical,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_dispatch_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteDispatch = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TruckTripCard(
    trip: TruckTripGroup,
    ordersById: Map<String, CustomerOrder>,
    reprocessByDispatchId: Map<String, ReprocessItem> = emptyMap(),
    onEditStop: (DispatchRecord) -> Unit,
    onDeleteStop: (DispatchRecord) -> Unit,
    onAddReturnToReprocess: (DispatchRecord) -> Unit = {},
    onOpenReprocessTab: () -> Unit = {}
) {
    val distinctCustomers = remember(trip.stops) {
        trip.stops.map { it.customer.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }
    }
    val linkedOrderCodes = remember(trip.stops, ordersById) {
        trip.stops.mapNotNull { ordersById[it.linkedOrderId]?.orderCode }.distinct()
    }
    val totalBags = remember(trip.stops) { trip.stops.sumOf { it.bagsDispatched } }
    val totalNetKg = remember(trip.stops) { trip.stops.sumOf { it.effectiveDispatchedWeightKg() } }
    val hasHeld = trip.stops.any { it.isHeld }
    val hasReturned = trip.stops.any { it.returnConfirmed && it.returnedBags > 0L }
    val borderColor = when {
        hasHeld -> StatusCritical
        hasReturned -> GrainAmber
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalShipping,
                            contentDescription = "Truck Trip",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (trip.truckNumber.isNotBlank()) "Truck ${trip.truckNumber}" else "DR ${trip.stops.firstOrNull()?.drReference.orEmpty()}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "${trip.date} • Driver: ${trip.driverName.ifBlank { "—" }}${if (trip.helperName.isNotBlank()) " • Helper: ${trip.helperName}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(
                    label = "${trip.stops.size} stop(s)",
                    bgColor = MaterialTheme.colorScheme.primaryContainer,
                    textColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${distinctCustomers.size} customer(s) • ${linkedOrderCodes.size} PO(s) • $totalBags bags • ${OperationsCalculator.formatKg(totalNetKg)} kg net",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (distinctCustomers.isNotEmpty()) {
                        Text(
                            text = "Customers: ${distinctCustomers.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (linkedOrderCodes.isNotEmpty()) {
                        Text(
                            text = "Orders: ${linkedOrderCodes.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            trip.stops.forEachIndexed { idx, stop ->
                val linkedOrder = ordersById[stop.linkedOrderId]
                val stopStatus = stop.resolvedStatusLabel()
                val (pillBg, pillFg) = when (stopStatus) {
                    DispatchStatus.HELD.label -> StatusCriticalContainer to OnStatusCriticalContainer
                    DispatchStatus.RETURNED.label -> StatusWatchContainer to OnStatusWatchContainer
                    else -> StatusHealthyContainer to OnStatusHealthyContainer
                }
                if (idx > 0) HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${idx + 1}. ${stop.customer.ifBlank { "Unspecified Customer" }}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            StatusPill(label = stopStatus, bgColor = pillBg, textColor = pillFg)
                        }
                        Text(
                            text = "${stop.productName} • ${stop.bagsDispatched} bags (${OperationsCalculator.formatKg(stop.effectiveDispatchedWeightKg())} kg net)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = buildString {
                                append("DR: ${stop.drReference.ifBlank { "—" }}")
                                if (linkedOrder != null) append(" • PO: ${linkedOrder.orderCode}")
                                if (stop.returnConfirmed && stop.returnedBags > 0L) {
                                    append(" • Ret: ${stop.returnedBags} bags")
                                }
                                if (stop.isHeld && stop.heldRemarks.isNotBlank()) {
                                    append(" • Held: ${stop.heldRemarks}")
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (stop.returnConfirmed && stop.returnedBags > 0L) {
                            val stopReprocess = reprocessByDispatchId[stop.id]
                            if (stopReprocess != null) {
                                OutlinedButton(
                                    onClick = onOpenReprocessTab,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .minimumInteractiveComponentSize()
                                        .testTag("dispatch_trip_view_reprocess_${stop.id}")
                                ) {
                                    Icon(Icons.Filled.Autorenew, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (stopReprocess.flaggedToInventory) {
                                            "Reprocessed → Inventory"
                                        } else if (stopReprocess.fastActionNeeded) {
                                            "Reprocess • Fast Action"
                                        } else {
                                            "In Reprocess"
                                        },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onAddReturnToReprocess(stop) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .minimumInteractiveComponentSize()
                                        .testTag("dispatch_trip_send_reprocess_${stop.id}")
                                ) {
                                    Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Send to Reprocess", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    IconButton(
                        onClick = { onEditStop(stop) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("dispatch_trip_edit_${stop.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit stop ${stop.drReference}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { onDeleteStop(stop) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("dispatch_trip_delete_${stop.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete stop ${stop.drReference}",
                            tint = StatusCritical
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DispatchLogCard(
    record: DispatchRecord,
    resolvedSourcePlant: String = "",
    linkedOrder: CustomerOrder?,
    reprocessItem: ReprocessItem? = null,
    tripStopCount: Int = 1,
    tripCustomerCount: Int = 1,
    onEdit: () -> Unit,
    onSendToReprocess: () -> Unit = {},
    onOpenReprocessTab: () -> Unit = {}
) {
    var showDetails by remember { mutableStateOf(false) }
    val resolvedStatus = record.resolvedStatusLabel()
    val (pillBg, pillFg) = when (resolvedStatus) {
        DispatchStatus.HELD.label -> StatusCriticalContainer to OnStatusCriticalContainer
        DispatchStatus.RETURNED.label -> StatusWatchContainer to OnStatusWatchContainer
        else -> StatusHealthyContainer to OnStatusHealthyContainer
    }
    val borderColor = when (resolvedStatus) {
        DispatchStatus.HELD.label -> StatusCritical
        DispatchStatus.RETURNED.label -> GrainAmber
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Minimized Top Row: Customer + Product on Left, DR# + StatusPill on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.customer.ifBlank { "Unspecified Customer" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = record.productName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = record.drReference.ifBlank { "No DR#" },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StatusPill(label = resolvedStatus, bgColor = pillBg, textColor = pillFg)
                }
            }

            // Minimized Core Metrics Strip: Qty + Source Plant + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${record.bagsDispatched} bags • ${OperationsCalculator.formatKg(record.effectiveDispatchedWeightKg())} kg",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = buildString {
                        if (resolvedSourcePlant.isNotBlank()) {
                            append(resolvedSourcePlant)
                            append(" • ")
                        }
                        append(record.date.ifBlank { "No Date" })
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Compact Urgent Return / Hold Action Bar (only shown if applicable)
            if (record.returnConfirmed) {
                Surface(
                    color = StatusWatchContainer,
                    border = BorderStroke(1.dp, GrainAmber),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ret: ${record.returnedBags}b (${OperationsCalculator.formatKg(record.returnedWeightKg)} kg)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = OnStatusWatchContainer,
                            modifier = Modifier.weight(1f)
                        )
                        if (reprocessItem != null) {
                            TextButton(
                                onClick = onOpenReprocessTab,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("dispatch_view_reprocess_${record.id}")
                            ) {
                                Icon(Icons.Filled.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (reprocessItem.flaggedToInventory) "Moved ✓" else "Reprocess",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        } else if (record.returnedBags > 0L && record.returnedWeightKg > 0.0) {
                            TextButton(
                                onClick = onSendToReprocess,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("dispatch_send_reprocess_${record.id}")
                            ) {
                                Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(16.dp), tint = StatusCriticalTextDark)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reprocess", style = MaterialTheme.typography.labelMedium, color = StatusCriticalTextDark)
                            }
                        }
                    }
                }
            }

            if (record.isHeld) {
                Surface(
                    color = StatusCriticalContainer,
                    border = BorderStroke(1.dp, StatusCritical),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "HELD: ${record.heldRemarks.ifBlank { "Pending release" }}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = OnStatusCriticalContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Expandable Secondary Metadata
            if (showDetails) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${record.bagsDispatched} bags × ${record.bagWeightKg} kg/bag = ${OperationsCalculator.formatKg(record.totalWeightKg)} kg gross",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LogisticsTag(
                                icon = Icons.Filled.Person,
                                label = "Driver: ${record.driverName.ifBlank { "—" }}",
                                modifier = Modifier.weight(1f)
                            )
                            LogisticsTag(
                                icon = Icons.Filled.DirectionsCar,
                                label = "Truck: ${record.truckNumber.ifBlank { "—" }}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (record.helperName.isNotBlank()) {
                            LogisticsTag(
                                icon = Icons.Filled.Person,
                                label = "Helper: ${record.helperName}",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (linkedOrder != null) {
                            Text(
                                text = "Linked PO: ${linkedOrder.orderCode} (${linkedOrder.customer})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (tripStopCount > 1) {
                            Text(
                                text = if (record.truckNumber.isNotBlank()) {
                                    "Shared Truck ${record.truckNumber} • $tripStopCount stops across $tripCustomerCount customer(s)"
                                } else {
                                    "Shared Trip DR ${record.drReference} • $tripStopCount stops across $tripCustomerCount customer(s)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (record.returnConfirmed && record.returnReason.isNotBlank()) {
                            Text(
                                text = "Return Reason: ${record.returnReason}",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusWatchTextDark
                            )
                        }
                        if (record.remarks.isNotBlank()) {
                            Text(
                                text = "Notes: ${record.remarks}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Compact Bottom Bar: Details toggle + Edit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showDetails = !showDetails },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("dispatch_card_details_${record.id}")
                ) {
                    Text(if (showDetails) "Hide Details" else "Details")
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = if (showDetails) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("dispatch_edit_action_${record.id}")
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (resolvedStatus == DispatchStatus.HELD.label) "Release / Edit" else "Edit")
                }
            }
        }
    }
}

@Composable
private fun LogisticsTag(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
