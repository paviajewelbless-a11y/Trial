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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.OpsTab
import com.example.data.TabPreferencesStore
import com.example.ui.theme.ForestEmerald

@Composable
fun CustomizeBottomTabsDialog(
    currentPinnedTabs: List<OpsTab>,
    onDismiss: () -> Unit,
    onSaveTabs: (List<OpsTab>) -> Unit,
    onResetDefaults: () -> Unit
) {
    var draftTabs by remember(currentPinnedTabs) {
        mutableStateOf(
            currentPinnedTabs
                .distinct()
                .take(TabPreferencesStore.MAX_PINNED_BOTTOM_TABS)
                .ifEmpty { listOf(OpsTab.SUMMARY, OpsTab.MONITOR, OpsTab.ORDERS, OpsTab.DISPATCHED) }
        )
    }

    val unpinnedTabs = remember(draftTabs) {
        OpsTab.entries.filter { it !in draftTabs }
    }
    val maxSlots = TabPreferencesStore.MAX_PINNED_BOTTOM_TABS
    val minSlots = TabPreferencesStore.MIN_PINNED_BOTTOM_TABS

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Customize Bottom Tabs")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Choose 1–$maxSlots tabs to show on your bottom navigation bar. All other screens remain in More.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Live Bottom Bar Preview
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "LIVE BOTTOM BAR PREVIEW",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            draftTabs.forEachIndexed { idx, tab ->
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Surface(
                                        color = if (idx == 0) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            Color.Transparent
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (idx == 0) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label,
                                            tint = if (idx == 0) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp).size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = tab.bottomNavLabel(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (idx == 0) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Apps,
                                    contentDescription = "More",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp).size(18.dp)
                                )
                                Text(
                                    text = "More",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Role Presets
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Quick Presets",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                draftTabs = listOf(OpsTab.SUMMARY, OpsTab.MONITOR, OpsTab.ORDERS, OpsTab.DISPATCHED)
                            },
                            label = { Text("Default Ops") },
                            modifier = Modifier.testTag("tabs_preset_default")
                        )
                        AssistChip(
                            onClick = {
                                draftTabs = listOf(
                                    OpsTab.FINISHED_GOODS,
                                    OpsTab.MACRO_INVENTORY,
                                    OpsTab.MICRO_INVENTORY,
                                    OpsTab.FEED_BOM
                                )
                            },
                            label = { Text("All Inventory") },
                            modifier = Modifier.testTag("tabs_preset_inventory")
                        )
                        AssistChip(
                            onClick = {
                                draftTabs = listOf(OpsTab.SUMMARY, OpsTab.SCHEDULE, OpsTab.RECOVERY, OpsTab.DISPATCHED)
                            },
                            label = { Text("Production") },
                            modifier = Modifier.testTag("tabs_preset_production")
                        )
                    }
                }

                HorizontalDivider()

                // Pinned Tabs (Orderable)
                Text(
                    text = "Pinned Bottom Tabs (${draftTabs.size}/$maxSlots)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                draftTabs.forEachIndexed { idx, tab ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pinned_tab_row_${tab.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = tab.selectedIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Slot ${idx + 1} • Label: \"${tab.bottomNavLabel()}\"",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (idx > 0) {
                                        val mutable = draftTabs.toMutableList()
                                        val swapped = mutable[idx - 1]
                                        mutable[idx - 1] = mutable[idx]
                                        mutable[idx] = swapped
                                        draftTabs = mutable
                                    }
                                },
                                enabled = idx > 0,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("move_tab_up_${tab.name.lowercase()}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowUp,
                                    contentDescription = "Move ${tab.label} left"
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (idx < draftTabs.lastIndex) {
                                        val mutable = draftTabs.toMutableList()
                                        val swapped = mutable[idx + 1]
                                        mutable[idx + 1] = mutable[idx]
                                        mutable[idx] = swapped
                                        draftTabs = mutable
                                    }
                                },
                                enabled = idx < draftTabs.lastIndex,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("move_tab_down_${tab.name.lowercase()}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Move ${tab.label} right"
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (draftTabs.size > minSlots) {
                                        draftTabs = draftTabs - tab
                                    }
                                },
                                enabled = draftTabs.size > minSlots,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("unpin_tab_${tab.name.lowercase()}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove ${tab.label} from bottom bar"
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Available Tabs to Pin
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (draftTabs.size >= maxSlots) {
                            "Available Tabs (Remove one above to swap)"
                        } else {
                            "Tap to Add to Bottom Bar (${maxSlots - draftTabs.size} slot(s) left)"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    unpinnedTabs.forEach { tab ->
                        val canAdd = draftTabs.size < maxSlots
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = canAdd) {
                                    if (canAdd) draftTabs = draftTabs + tab
                                }
                                .testTag("available_tab_row_${tab.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = tab.unselectedIcon,
                                    contentDescription = null,
                                    tint = if (canAdd) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (canAdd) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                        }
                                    )
                                    Text(
                                        text = tab.navDescription(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (canAdd) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Pin ${tab.label}",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveTabs(draftTabs) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestEmerald,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("save_bottom_tabs_button")
            ) {
                Text("Save Tabs")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onResetDefaults,
                    modifier = Modifier.testTag("reset_default_tabs_button")
                ) {
                    Text("Default")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
