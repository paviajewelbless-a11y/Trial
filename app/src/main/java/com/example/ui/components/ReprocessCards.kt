package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.OperationsCalculator
import com.example.model.ReprocessItem
import com.example.model.ReprocessStatus
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.GrainAmber
import com.example.ui.theme.OnStatusCriticalContainer
import com.example.ui.theme.OnStatusHealthyContainer
import com.example.ui.theme.OnStatusInProgressContainer
import com.example.ui.theme.OnStatusWatchContainer
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusCriticalContainer
import com.example.ui.theme.StatusCriticalTextDark
import com.example.ui.theme.StatusHealthy
import com.example.ui.theme.StatusHealthyContainer
import com.example.ui.theme.StatusHealthyTextDark
import com.example.ui.theme.StatusInProgress
import com.example.ui.theme.StatusInProgressContainer
import com.example.ui.theme.StatusWatchContainer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReprocessItemCard(
    item: ReprocessItem,
    onStartReprocess: () -> Unit,
    onFastFlagInventory: () -> Unit,
    onOpenFlagDialog: () -> Unit,
    onOpenDamagedDialog: () -> Unit,
    onToggleFastAction: () -> Unit,
    onEdit: () -> Unit,
    onOpenInventory: () -> Unit
) {
    val isFlagged = item.flaggedToInventory
    val isDamaged = item.isDamaged()
    val isPending = item.isPending()
    val isUrgent = isPending && item.fastActionNeeded

    val borderColor = when {
        isFlagged -> StatusHealthy
        isDamaged || isUrgent -> StatusCritical
        item.status == ReprocessStatus.REPROCESSING.label -> StatusInProgress
        else -> GrainAmber
    }
    val (statusBg, statusFg) = when {
        isFlagged -> StatusHealthyContainer to OnStatusHealthyContainer
        isDamaged -> StatusCriticalContainer to OnStatusCriticalContainer
        item.status == ReprocessStatus.REPROCESSING.label -> StatusInProgressContainer to OnStatusInProgressContainer
        else -> StatusWatchContainer to OnStatusWatchContainer
    }
    val statusLabel = when {
        isFlagged -> "MOVED TO STOCK"
        isDamaged -> "DAMAGED / WRITE-OFF"
        else -> item.status
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (isUrgent || isDamaged) 1.5.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reprocess_card_${item.id}")
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
                        text = item.productName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(item.section)
                            if (item.drReference.isNotBlank()) append(" • DR ${item.drReference}")
                            if (item.customer.isNotBlank()) append(" • ${item.customer}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatusPill(
                        label = statusLabel,
                        bgColor = statusBg,
                        textColor = statusFg
                    )
                    if (isUrgent) {
                        StatusPill(
                            label = "NEED FAST ACTION",
                            bgColor = StatusCriticalContainer,
                            textColor = OnStatusCriticalContainer
                        )
                    }
                }
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
                        text = "Returned: ${item.returnedBags} bags (${OperationsCalculator.formatKg(item.returnedWeightKg)} kg) on ${item.returnedDate}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Reason: ${item.returnReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isFlagged) {
                        Text(
                            text = "Moved Back to Stock: +${item.effectiveReprocessedBags()} bags (+${OperationsCalculator.formatKg(item.effectiveReprocessedWeightKg())} kg)${if (item.reprocessedDate.isNotBlank()) " on ${item.reprocessedDate}" else ""}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = StatusHealthyTextDark
                        )
                    } else if (isDamaged) {
                        Text(
                            text = "Marked Damaged (Write-Off): -${item.returnedBags} bags (-${OperationsCalculator.formatKg(item.returnedWeightKg)} kg)${if (item.reprocessedDate.isNotBlank()) " on ${item.reprocessedDate}" else ""}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = StatusCriticalTextDark
                        )
                    }
                }
            }

            if (item.actionNotes.isNotBlank()) {
                Text(
                    text = "Notes: ${item.actionNotes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isPending) {
                    Button(
                        onClick = onFastFlagInventory,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForestEmerald,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("reprocess_fast_flag_${item.id}")
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Move Back to Stock")
                    }
                    OutlinedButton(
                        onClick = onOpenDamagedDialog,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, StatusCritical),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCriticalTextDark),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("reprocess_mark_damaged_${item.id}")
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark Damaged")
                    }
                    OutlinedButton(
                        onClick = onOpenFlagDialog,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("reprocess_adjust_flag_${item.id}")
                    ) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Adjust Kg & Stock")
                    }
                    if (item.status == ReprocessStatus.PENDING_REPROCESS.label) {
                        OutlinedButton(
                            onClick = onStartReprocess,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("reprocess_start_${item.id}")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start Reprocess")
                        }
                    }
                    FilterChip(
                        selected = item.fastActionNeeded,
                        onClick = onToggleFastAction,
                        leadingIcon = {
                            Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(if (item.fastActionNeeded) "Fast Action On" else "Mark Fast Action") },
                        modifier = Modifier.testTag("reprocess_toggle_fast_${item.id}")
                    )
                } else if (isFlagged) {
                    OutlinedButton(
                        onClick = onOpenInventory,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("reprocess_view_inventory_${item.id}")
                    ) {
                        Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View in Finished Goods")
                    }
                }
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("reprocess_edit_${item.id}")
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
            }
        }
    }
}
