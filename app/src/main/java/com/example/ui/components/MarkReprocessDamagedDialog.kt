package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.OperationsCalculator
import com.example.model.ReprocessItem
import com.example.ui.theme.OnStatusCriticalContainer
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusCriticalContainer

private val damagedReasonPresets = listOf(
    "Water / Moisture Spoilage",
    "Contaminated / Moldy",
    "Torn Sacks / Spillage",
    "Expired / Infested",
    "Other"
)

@Composable
fun MarkReprocessDamagedDialog(
    item: ReprocessItem,
    onDismiss: () -> Unit,
    onConfirmDamaged: (damagedDate: String, damageNotes: String) -> Unit
) {
    var dateText by remember {
        mutableStateOf(item.reprocessedDate.ifBlank { OperationsCalculator.todayIsoDate() })
    }
    var selectedReason by remember { mutableStateOf(damagedReasonPresets.first()) }
    var extraNotes by remember { mutableStateOf(item.actionNotes) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Mark item as damaged",
                tint = StatusCritical
            )
        },
        title = { Text("Mark Feed Lot as Damaged") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = StatusCriticalContainer.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, StatusCritical),
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
                            color = OnStatusCriticalContainer
                        )
                        Text(
                            text = "Write-off volume: ${item.returnedBags} bags (${OperationsCalculator.formatKg(item.returnedWeightKg)} kg)${if (item.drReference.isNotBlank()) " • DR ${item.drReference}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnStatusCriticalContainer
                        )
                        if (item.customer.isNotBlank()) {
                            Text(
                                text = "Source: ${item.customer} • Reason: ${item.returnReason}",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnStatusCriticalContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Text(
                    text = "Damage Reason",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    damagedReasonPresets.forEach { preset ->
                        FilterChip(
                            selected = selectedReason == preset,
                            onClick = { selectedReason = preset },
                            label = { Text(preset) }
                        )
                    }
                }

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Write-Off Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("damaged_reprocess_date_input")
                )

                OutlinedTextField(
                    value = extraNotes,
                    onValueChange = { extraNotes = it },
                    label = { Text("Inspection / Disposal Notes") },
                    placeholder = { Text("e.g., Wet sacks discarded after QA check...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("damaged_reprocess_notes_input")
                )

                HorizontalDivider()
                Text(
                    text = "Marking this lot Damaged closes the pending Reprocess alert and writes off ${OperationsCalculator.formatKg(item.returnedWeightKg)} kg so it will NOT be returned to sellable stock.",
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
                    val parsed = OperationsCalculator.parseDateOrNull(dateText.trim())
                    if (parsed == null) {
                        error = "Use YYYY-MM-DD date format"
                    } else {
                        val combinedNotes = buildString {
                            append("Damaged: ")
                            append(selectedReason)
                            val detail = extraNotes.trim()
                            if (detail.isNotBlank() && !detail.equals(selectedReason, ignoreCase = true)) {
                                append(" — ")
                                append(detail)
                            }
                        }.take(300)
                        onConfirmDamaged(parsed.toString(), combinedNotes)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusCritical,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("confirm_mark_damaged_button")
            ) {
                Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Mark as Damaged")
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
