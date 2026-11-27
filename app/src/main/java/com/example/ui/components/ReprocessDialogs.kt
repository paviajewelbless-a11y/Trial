package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.OperationsCalculator
import com.example.model.DispatchRecord
import com.example.model.DispatchReturnReasons
import com.example.model.PlantSection
import com.example.model.ReprocessItem
import com.example.model.SkuEntity
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.OnStatusCriticalContainer
import com.example.ui.theme.OnStatusHealthyContainer
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusCriticalContainer
import com.example.ui.theme.StatusHealthyContainer
import kotlin.math.round

private fun normalizeReprocessDateOrNull(raw: String): String? {
    val trimmed = raw.trim()
    val parsed = OperationsCalculator.parseDateOrNull(trimmed) ?: return null
    return parsed.toString()
}

@Composable
fun AddEditReprocessDialog(
    skus: List<SkuEntity>,
    unqueuedReturnedDispatches: List<DispatchRecord>,
    existingItem: ReprocessItem? = null,
    onDismiss: () -> Unit,
    onSubmit: (
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
    ) -> Unit
) {
    val isEditing = existingItem != null
    var entryModeDispatch by remember {
        mutableStateOf(!isEditing && unqueuedReturnedDispatches.isNotEmpty())
    }
    var selectedDispatch by remember {
        mutableStateOf(
            if (!isEditing) unqueuedReturnedDispatches.firstOrNull() else null
        )
    }

    val initialSku = remember(skus, existingItem, selectedDispatch) {
        val targetName = existingItem?.productName ?: selectedDispatch?.productName.orEmpty()
        skus.firstOrNull { it.productName.equals(targetName, ignoreCase = true) } ?: skus.firstOrNull()
    }

    var productName by remember {
        mutableStateOf(existingItem?.productName ?: selectedDispatch?.productName ?: initialSku?.productName.orEmpty())
    }
    var section by remember {
        mutableStateOf(
            existingItem?.section?.let { PlantSection.fromRaw(it) }
                ?: initialSku?.section?.let { PlantSection.fromRaw(it) }
                ?: PlantSection.SAN_ROQUE
        )
    }
    var returnedDate by remember {
        mutableStateOf(
            existingItem?.returnedDate?.ifBlank { null }
                ?: selectedDispatch?.date?.ifBlank { null }
                ?: OperationsCalculator.todayIsoDate()
        )
    }
    var returnedBagsText by remember {
        mutableStateOf(
            when {
                existingItem != null -> existingItem.returnedBags.toString()
                selectedDispatch != null -> selectedDispatch!!.returnedBags.coerceAtLeast(1L).toString()
                else -> "10"
            }
        )
    }
    var bagWtText by remember {
        mutableStateOf(
            when {
                existingItem != null -> existingItem.bagWeightKg.toString()
                selectedDispatch != null -> selectedDispatch!!.bagWeightKg.toString()
                initialSku != null -> initialSku.standardBagWeightKg.toString()
                else -> "50.0"
            }
        )
    }
    var reasonOption by remember {
        val base = existingItem?.returnReason ?: selectedDispatch?.returnReason.orEmpty()
        mutableStateOf(
            when {
                base.isBlank() -> DispatchReturnReasons.options.first()
                base in DispatchReturnReasons.options -> base
                else -> "Other"
            }
        )
    }
    var customReason by remember {
        val base = existingItem?.returnReason ?: selectedDispatch?.returnReason.orEmpty()
        mutableStateOf(if (base.isNotBlank() && base !in DispatchReturnReasons.options) base else "")
    }
    var customer by remember {
        mutableStateOf(existingItem?.customer ?: selectedDispatch?.customer.orEmpty())
    }
    var drReference by remember {
        mutableStateOf(existingItem?.drReference ?: selectedDispatch?.drReference.orEmpty())
    }
    var fastActionNeeded by remember {
        mutableStateOf(existingItem?.fastActionNeeded ?: true)
    }
    var actionNotes by remember {
        mutableStateOf(existingItem?.actionNotes ?: selectedDispatch?.remarks.orEmpty())
    }
    var skuExpanded by remember { mutableStateOf(false) }
    var dispatchExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val applyDispatchSelection: (DispatchRecord) -> Unit = { d ->
        selectedDispatch = d
        val matched = skus.firstOrNull { it.productName.equals(d.productName, ignoreCase = true) }
        productName = d.productName
        section = matched?.section?.let { PlantSection.fromRaw(it) } ?: PlantSection.SAN_ROQUE
        returnedDate = d.date.ifBlank { OperationsCalculator.todayIsoDate() }
        returnedBagsText = d.returnedBags.coerceAtLeast(1L).toString()
        bagWtText = d.bagWeightKg.coerceAtLeast(1.0).toString()
        customer = d.customer
        drReference = d.drReference
        actionNotes = d.remarks
        val r = d.returnReason.trim()
        if (r in DispatchReturnReasons.options) {
            reasonOption = r
            customReason = ""
        } else if (r.isNotBlank()) {
            reasonOption = "Other"
            customReason = r
        }
    }

    val bagsVal = returnedBagsText.toLongOrNull() ?: 0L
    val bagWtVal = bagWtText.toDoubleOrNull() ?: 0.0
    val totalReturnedKg = round(bagsVal * bagWtVal * 100.0) / 100.0
    val resolvedReason = if (reasonOption == "Other") customReason.trim() else reasonOption

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.RestartAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(if (isEditing) "Edit Reprocess Entry" else "Add Returned Product to Reprocess")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isEditing && unqueuedReturnedDispatches.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = entryModeDispatch,
                            onClick = {
                                entryModeDispatch = true
                                (selectedDispatch ?: unqueuedReturnedDispatches.firstOrNull())?.let(applyDispatchSelection)
                            },
                            label = { Text("Returned DR (${unqueuedReturnedDispatches.size})") },
                            modifier = Modifier.testTag("reprocess_mode_dispatch")
                        )
                        FilterChip(
                            selected = !entryModeDispatch,
                            onClick = {
                                entryModeDispatch = false
                                selectedDispatch = null
                            },
                            label = { Text("Manual Return") },
                            modifier = Modifier.testTag("reprocess_mode_manual")
                        )
                    }
                }

                if (!isEditing && entryModeDispatch && unqueuedReturnedDispatches.isNotEmpty()) {
                    Box {
                        OutlinedButton(
                            onClick = { dispatchExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("reprocess_pick_dispatch_dropdown")
                        ) {
                            Text(
                                text = selectedDispatch?.let {
                                    "DR ${it.drReference.ifBlank { "—" }} • ${it.productName} (${it.returnedBags}b)"
                                } ?: "Select Returned Dispatch",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Pick returned dispatch")
                        }
                        DropdownMenu(
                            expanded = dispatchExpanded,
                            onDismissRequest = { dispatchExpanded = false }
                        ) {
                            unqueuedReturnedDispatches.forEach { disp ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${disp.drReference.ifBlank { "No DR" }} • ${disp.customer} • ${disp.productName} (${disp.returnedBags}b)")
                                    },
                                    onClick = {
                                        applyDispatchSelection(disp)
                                        dispatchExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Box {
                    OutlinedButton(
                        onClick = { skuExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("reprocess_sku_picker_button")
                    ) {
                        Text(
                            text = if (productName.isBlank()) "Select Product SKU" else "$productName (${section.label})",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select SKU")
                    }
                    DropdownMenu(
                        expanded = skuExpanded,
                        onDismissRequest = { skuExpanded = false }
                    ) {
                        skus.forEach { sku ->
                            DropdownMenuItem(
                                text = { Text("${sku.productName} (${sku.section})") },
                                onClick = {
                                    productName = sku.productName
                                    section = PlantSection.fromRaw(sku.section)
                                    bagWtText = sku.standardBagWeightKg.toString()
                                    skuExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = returnedBagsText,
                        onValueChange = { returnedBagsText = it },
                        label = { Text("Returned Bags") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reprocess_returned_bags_input")
                    )
                    OutlinedTextField(
                        value = bagWtText,
                        onValueChange = { bagWtText = it },
                        label = { Text("Bag Wt (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reprocess_bag_wt_input")
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Returned Volume: ${bagsVal.coerceAtLeast(0L)} bags • ${OperationsCalculator.formatKg(totalReturnedKg.coerceAtLeast(0.0))} kg",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = returnedDate,
                        onValueChange = { returnedDate = it },
                        label = { Text("Return Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reprocess_date_input")
                    )
                    OutlinedTextField(
                        value = drReference,
                        onValueChange = { drReference = it },
                        label = { Text("DR Ref (Optional)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reprocess_dr_input")
                    )
                }

                OutlinedTextField(
                    value = customer,
                    onValueChange = { customer = it },
                    label = { Text("Customer / Source (Optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reprocess_customer_input")
                )

                Text(
                    text = "Return Reason",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DispatchReturnReasons.options.forEach { opt ->
                        FilterChip(
                            selected = reasonOption == opt,
                            onClick = { reasonOption = opt },
                            label = { Text(opt) }
                        )
                    }
                }
                if (reasonOption == "Other") {
                    OutlinedTextField(
                        value = customReason,
                        onValueChange = { customReason = it },
                        label = { Text("Custom Return Reason") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reprocess_custom_reason_input")
                    )
                }

                if (existingItem?.flaggedToInventory != true && existingItem?.isDamaged() != true) {
                    Surface(
                        color = if (fastActionNeeded) StatusCriticalContainer.copy(alpha = 0.72f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (fastActionNeeded) StatusCritical else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { fastActionNeeded = !fastActionNeeded }
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
                                    imageVector = Icons.Filled.PriorityHigh,
                                    contentDescription = null,
                                    tint = if (fastActionNeeded) OnStatusCriticalContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Needs Fast Action",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (fastActionNeeded) OnStatusCriticalContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Highlights urgent reprocessing across Summary & Inventory",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (fastActionNeeded) OnStatusCriticalContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = fastActionNeeded,
                                onCheckedChange = { fastActionNeeded = it },
                                modifier = Modifier.testTag("reprocess_fast_action_switch")
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = actionNotes,
                    onValueChange = { actionNotes = it },
                    label = { Text("Reprocessing Instructions / Notes") },
                    placeholder = { Text("e.g., Re-bag, re-screen, moisture check...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reprocess_notes_input")
                )

                if (error != null) {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val normDate = normalizeReprocessDateOrNull(returnedDate)
                    when {
                        productName.isBlank() -> error = "Select a product SKU"
                        normDate == null -> error = "Use YYYY-MM-DD date format"
                        bagsVal !in 1..1_000_000 -> error = "Returned bags must be 1–1,000,000"
                        bagWtVal <= 0.0 || bagWtVal > 2000.0 -> error = "Bag weight must be 0.01–2,000 kg"
                        totalReturnedKg <= 0.0 || totalReturnedKg > 50_000_000.0 -> error = "Total returned kg invalid"
                        resolvedReason.isBlank() -> error = "Enter or select a return reason"
                        else -> onSubmit(
                            productName.trim(),
                            section,
                            normDate,
                            bagsVal,
                            bagWtVal,
                            totalReturnedKg,
                            resolvedReason,
                            customer.trim(),
                            drReference.trim(),
                            existingItem?.sourceDispatchId ?: selectedDispatch?.id.orEmpty(),
                            fastActionNeeded,
                            actionNotes.trim()
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestEmerald,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("reprocess_submit_button")
            ) {
                Text(if (isEditing) "Save Entry" else "Add to Reprocess")
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

@Composable
fun FlagReprocessToInventoryDialog(
    item: ReprocessItem,
    onDismiss: () -> Unit,
    onConfirmFlag: (
        reprocessedBags: Long,
        reprocessedWeightKg: Double,
        reprocessedDate: String,
        actionNotes: String
    ) -> Unit
) {
    var bagsText by remember {
        mutableStateOf(
            (if (item.reprocessedBags > 0L) item.reprocessedBags else item.returnedBags).toString()
        )
    }
    var weightText by remember {
        mutableStateOf(
            String.format(
                java.util.Locale.US,
                "%.2f",
                if (item.reprocessedWeightKg > 0.0) item.reprocessedWeightKg else item.returnedWeightKg
            )
        )
    }
    var dateText by remember {
        mutableStateOf(item.reprocessedDate.ifBlank { OperationsCalculator.todayIsoDate() })
    }
    var notesText by remember { mutableStateOf(item.actionNotes) }
    var error by remember { mutableStateOf<String?>(null) }

    val bagsVal = bagsText.toLongOrNull() ?: 0L
    val weightVal = weightText.toDoubleOrNull() ?: 0.0
    val lossKg = (item.returnedWeightKg - weightVal).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Inventory2,
                contentDescription = null,
                tint = ForestEmerald
            )
        },
        title = { Text("Move Reprocessed Feed to Stock") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = StatusHealthyContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, ForestEmerald),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${item.productName} (${item.section})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = OnStatusHealthyContainer
                        )
                        Text(
                            text = "Returned: ${item.returnedBags} bags (${OperationsCalculator.formatKg(item.returnedWeightKg)} kg)${if (item.drReference.isNotBlank()) " • DR ${item.drReference}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnStatusHealthyContainer
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bagsText,
                        onValueChange = { bStr ->
                            bagsText = bStr
                            val b = bStr.toLongOrNull()
                            if (b != null && b > 0L && item.bagWeightKg > 0.0) {
                                weightText = String.format(java.util.Locale.US, "%.2f", b * item.bagWeightKg)
                            }
                        },
                        label = { Text("Recovered Bags") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("flag_reprocess_bags_input")
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("Reprocessed kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("flag_reprocess_weight_input")
                    )
                }

                if (lossKg > 0.01) {
                    Text(
                        text = "Reprocessing shrinkage / cull: -${OperationsCalculator.formatKg(lossKg)} kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Reprocessed Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("flag_reprocess_date_input")
                )

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("QC / Release Notes (Optional)") },
                    placeholder = { Text("e.g., Passed QA sieve & moisture check") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("flag_reprocess_notes_input")
                )

                HorizontalDivider()
                Text(
                    text = "Confirming marks this product as REPROCESSED, moves +${OperationsCalculator.formatKg(weightVal.coerceAtLeast(0.0))} kg back into Finished Goods stock, and clears the Fast Action alert.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (error != null) {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val normDate = normalizeReprocessDateOrNull(dateText)
                    when {
                        bagsVal !in 1..1_000_000 -> error = "Recovered bags must be 1–1,000,000"
                        weightVal <= 0.0 || weightVal > 50_000_000.0 -> error = "Enter valid recovered weight in kg"
                        normDate == null -> error = "Use YYYY-MM-DD date format"
                        else -> onConfirmFlag(bagsVal, round(weightVal * 100.0) / 100.0, normDate, notesText.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestEmerald,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("confirm_flag_reprocess_button")
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Move Back to Stock")
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
