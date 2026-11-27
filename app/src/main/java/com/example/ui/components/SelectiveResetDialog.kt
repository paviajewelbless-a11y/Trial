package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.OpsTab
import com.example.data.AdminResetSecurityStore
import com.example.data.AdminSecurityConfig
import com.example.model.ResetScopeTarget
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.OnStatusCriticalContainer
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusCriticalContainer
import com.example.ui.theme.StatusCriticalTextDark

private sealed interface PendingResetPlan {
    data class Selective(val targets: Set<ResetScopeTarget>) : PendingResetPlan
    data object FullBaseline : PendingResetPlan
}

fun exclusiveDataTargetForTab(tab: OpsTab): ResetScopeTarget? = when (tab) {
    OpsTab.MONITOR -> ResetScopeTarget.RAW_MATERIALS
    OpsTab.FINISHED_GOODS -> ResetScopeTarget.SKUS
    OpsTab.MACRO_INVENTORY -> ResetScopeTarget.MACRO_MATERIALS
    OpsTab.MICRO_INVENTORY -> ResetScopeTarget.MICRO_MATERIALS
    OpsTab.ORDERS -> ResetScopeTarget.ORDERS
    OpsTab.SCHEDULE -> ResetScopeTarget.SCHEDULES
    OpsTab.DISPATCHED -> ResetScopeTarget.DISPATCHES
    OpsTab.REPROCESS -> ResetScopeTarget.REPROCESS_ITEMS
    OpsTab.RECOVERY -> ResetScopeTarget.PRODUCTION_BATCHES
    OpsTab.FEED_BOM -> ResetScopeTarget.FEED_BOMS
    OpsTab.AI_OPS -> ResetScopeTarget.AI_CHAT
    OpsTab.HISTORY -> ResetScopeTarget.HISTORY
    OpsTab.SUMMARY, OpsTab.DAILY_LOG, OpsTab.REPORTS, OpsTab.PRODUCTION_MONITOR, OpsTab.TEAM -> null
}

private fun exclusiveDataButtonLabel(tab: OpsTab, target: ResetScopeTarget): String = when (tab) {
    OpsTab.MONITOR -> "Restore All Raw Materials Only"
    OpsTab.FINISHED_GOODS -> "Restore Finished Products Only"
    OpsTab.MACRO_INVENTORY -> "Restore Macro Materials Only"
    OpsTab.MICRO_INVENTORY -> "Restore Micro Materials Only"
    OpsTab.ORDERS -> "Clear Orders Only"
    OpsTab.SCHEDULE -> "Clear Production Schedule Only"
    OpsTab.DISPATCHED -> "Clear Dispatches Only"
    OpsTab.REPROCESS -> "Clear Reprocess Queue Only"
    OpsTab.RECOVERY -> "Clear Production Batches Only"
    OpsTab.FEED_BOM -> "Restore Default Feed BOMs Only"
    OpsTab.AI_OPS -> "Clear AI Insights & Chat Only"
    OpsTab.HISTORY -> "Clear Activity History Only"
    else -> "Reset ${target.label} Only"
}

@Composable
fun SelectiveResetDialog(
    currentTab: OpsTab,
    currentUserEmail: String = "",
    onDismiss: () -> Unit,
    onExecuteReset: (Set<ResetScopeTarget>, adminVerificationSummary: String) -> Unit,
    onFullBaselineReset: (adminVerificationSummary: String) -> Unit
) {
    val context = LocalContext.current
    val adminConfig by AdminResetSecurityStore
        .observeConfig(context, defaultAdminEmail = currentUserEmail)
        .collectAsStateWithLifecycle(initialValue = AdminSecurityConfig(adminEmail = currentUserEmail))

    val exclusiveTarget = remember(currentTab) { exclusiveDataTargetForTab(currentTab) }
    val defaultTabSelection = remember(currentTab, exclusiveTarget) {
        buildSet {
            add(ResetScopeTarget.VIEW_FILTERS)
            if (exclusiveTarget != null) add(exclusiveTarget)
        }
    }

    var selectedTargets by remember(currentTab) { mutableStateOf(defaultTabSelection) }
    var pendingPlan by remember { mutableStateOf<PendingResetPlan?>(null) }

    // Confirmation step state: selectable "are you sure?" + admin verification
    var confirmedSelectiveTargets by remember(pendingPlan) {
        mutableStateOf((pendingPlan as? PendingResetPlan.Selective)?.targets ?: emptySet())
    }
    var isSureToReset by remember(pendingPlan) { mutableStateOf(false) }
    var isPinVerified by remember(pendingPlan) { mutableStateOf(false) }
    var isGoogleVerified by remember(pendingPlan) { mutableStateOf(false) }

    val activePlan = pendingPlan
    if (activePlan != null) {
        val isAdminVerified = adminConfig.isConfigured &&
            (!adminConfig.requiresPin || isPinVerified) &&
            (!adminConfig.requiresGoogleReauth || isGoogleVerified)

        val hasTargetsReady = when (activePlan) {
            is PendingResetPlan.FullBaseline -> true
            is PendingResetPlan.Selective -> confirmedSelectiveTargets.isNotEmpty()
        }
        val canConfirmFinal = isSureToReset && isAdminVerified && hasTargetsReady

        AlertDialog(
            onDismissRequest = { pendingPlan = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = StatusCriticalTextDark
                )
            },
            title = {
                Text(
                    text = when (activePlan) {
                        is PendingResetPlan.FullBaseline -> "Confirm Full Plant Reset"
                        is PendingResetPlan.Selective -> "Are You Sure You Want to Reset?"
                    }
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (activePlan) {
                        is PendingResetPlan.FullBaseline -> {
                            Surface(
                                color = StatusCriticalContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Full Plant Reset replaces all Product Names, Raw Materials, Feed BOMs, Orders, Schedules, Dispatches, Production Batches, and Activity History with Beginning Inventory.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnStatusCriticalContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        is PendingResetPlan.Selective -> {
                            Text(
                                text = "1. Review & Select Items to Reset:",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            activePlan.targets.forEach { target ->
                                val checked = target in confirmedSelectiveTargets
                                Surface(
                                    color = if (checked) {
                                        if (target.isDataMutation) {
                                            StatusCriticalContainer.copy(alpha = 0.45f)
                                        } else {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                        }
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (checked && target.isDataMutation) StatusCritical
                                        else if (checked) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            confirmedSelectiveTargets = if (checked) {
                                                confirmedSelectiveTargets - target
                                            } else {
                                                confirmedSelectiveTargets + target
                                            }
                                        }
                                        .testTag("confirm_toggle_${target.name.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = { nowChecked ->
                                                confirmedSelectiveTargets = if (nowChecked) {
                                                    confirmedSelectiveTargets + target
                                                } else {
                                                    confirmedSelectiveTargets - target
                                                }
                                            },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = target.label,
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = if (checked && target.isDataMutation) {
                                                    OnStatusCriticalContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                            Text(
                                                text = target.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (checked && target.isDataMutation) {
                                                    OnStatusCriticalContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Step 2: Selectable "Are you sure?" decision
                    Text(
                        text = if (activePlan is PendingResetPlan.Selective) {
                            "2. Are you sure you want to reset?"
                        } else {
                            "1. Are you sure you want to reset?"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = if (!isSureToReset) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                width = if (!isSureToReset) 1.5.dp else 1.dp,
                                color = if (!isSureToReset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { isSureToReset = false }
                                .testTag("reset_sure_no_option")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = !isSureToReset,
                                    onClick = { isSureToReset = false },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                )
                                Column {
                                    Text(
                                        text = "No — Keep current data",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Hold reset until you confirm you are sure.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Surface(
                            color = if (isSureToReset) {
                                StatusCriticalContainer.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                width = if (isSureToReset) 1.5.dp else 1.dp,
                                color = if (isSureToReset) StatusCritical else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { isSureToReset = true }
                                .testTag("reset_sure_yes_option")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSureToReset,
                                    onClick = { isSureToReset = true },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                )
                                Column {
                                    Text(
                                        text = "Yes — I am sure, proceed with reset",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSureToReset) OnStatusCriticalContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Admin security verification below is required to execute.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSureToReset) OnStatusCriticalContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Step 3: Admin Security Verification (selected by Admin)
                    AdminResetVerificationCard(
                        config = adminConfig,
                        currentUserEmail = currentUserEmail,
                        isPinVerified = isPinVerified,
                        isGoogleVerified = isGoogleVerified,
                        onPinVerified = { isPinVerified = true },
                        onGoogleVerified = { isGoogleVerified = true },
                        onConfigUpdated = { updatedMethod, pinVerifiedNow ->
                            if (pinVerifiedNow) isPinVerified = true
                            if (updatedMethod == com.example.data.AdminVerificationMethod.GOOGLE_ADMIN) {
                                isPinVerified = false
                            }
                        }
                    )
                }
            },
            confirmButton = {
                val summary = "Verified via ${adminConfig.method.label} by ${adminConfig.adminEmail.ifBlank { currentUserEmail }}"
                when (activePlan) {
                    is PendingResetPlan.FullBaseline -> {
                        Button(
                            onClick = {
                                pendingPlan = null
                                onFullBaselineReset(summary)
                            },
                            enabled = canConfirmFinal,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusCritical,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("confirm_full_baseline_reset_button")
                        ) {
                            Text("Reset Everything")
                        }
                    }

                    is PendingResetPlan.Selective -> {
                        Button(
                            onClick = {
                                val chosen = confirmedSelectiveTargets
                                pendingPlan = null
                                onExecuteReset(chosen, summary)
                            },
                            enabled = canConfirmFinal,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (confirmedSelectiveTargets.any { it.isDataMutation }) {
                                    StatusCritical
                                } else {
                                    ForestEmerald
                                },
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("confirm_destructive_selective_reset_button")
                        ) {
                            Text("Yes, Reset Selected (${confirmedSelectiveTargets.size})")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPlan = null }) {
                    Text("Back")
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.RestartAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Reset • ${currentTab.label}")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Admin security indicator pill
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (adminConfig.isConfigured) {
                                "Protected by Admin (${adminConfig.method.label})"
                            } else {
                                "Admin security setup required on reset confirmation"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Exclusive per-tab section
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Exclusive to ${currentTab.label} Tab",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = {
                                pendingPlan = PendingResetPlan.Selective(setOf(ResetScopeTarget.VIEW_FILTERS))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("reset_current_tab_filters_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FilterAltOff,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset ${currentTab.label} Filters & View Only")
                        }

                        if (exclusiveTarget != null) {
                            val tabTargets = setOf(ResetScopeTarget.VIEW_FILTERS, exclusiveTarget)
                            Button(
                                onClick = {
                                    pendingPlan = PendingResetPlan.Selective(tabTargets)
                                },
                                colors = if (exclusiveTarget.isDataMutation) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = StatusCritical,
                                        contentColor = Color.White
                                    )
                                } else {
                                    ButtonDefaults.buttonColors(
                                        containerColor = ForestEmerald,
                                        contentColor = Color.White
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("reset_current_tab_data_button")
                            ) {
                                Text(exclusiveDataButtonLabel(currentTab, exclusiveTarget))
                            }
                            Text(
                                text = exclusiveTarget.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Multi-select selective reset section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Or Select What to Reset",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { selectedTargets = defaultTabSelection },
                            label = { Text("Current Tab") },
                            modifier = Modifier.testTag("reset_preset_current_tab")
                        )
                        AssistChip(
                            onClick = { selectedTargets = ResetScopeTarget.entries.toSet() },
                            label = { Text("Select All") },
                            modifier = Modifier.testTag("reset_preset_all")
                        )
                        AssistChip(
                            onClick = { selectedTargets = emptySet() },
                            label = { Text("Clear") },
                            modifier = Modifier.testTag("reset_preset_clear")
                        )
                    }

                    ResetScopeTarget.entries.forEach { target ->
                        val isChecked = target in selectedTargets
                        Surface(
                            color = if (isChecked) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTargets = if (isChecked) {
                                        selectedTargets - target
                                    } else {
                                        selectedTargets + target
                                    }
                                }
                                .testTag("reset_scope_${target.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedTargets = if (checked) {
                                            selectedTargets + target
                                        } else {
                                            selectedTargets - target
                                        }
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = target.label,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (target.isDataMutation) {
                                            Surface(
                                                color = StatusCriticalContainer,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "Cloud Data",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = OnStatusCriticalContainer,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = target.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            pendingPlan = PendingResetPlan.Selective(selectedTargets)
                        },
                        enabled = selectedTargets.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTargets.any { it.isDataMutation }) {
                                StatusCritical
                            } else {
                                ForestEmerald
                            },
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("confirm_selective_reset_button")
                    ) {
                        Text("Reset Selected (${selectedTargets.size})")
                    }

                    TextButton(
                        onClick = { pendingPlan = PendingResetPlan.FullBaseline },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_reset_oct2026_button")
                    ) {
                        Text(
                            text = "Full Plant Reset to Beginning Inventory",
                            color = StatusCriticalTextDark
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
