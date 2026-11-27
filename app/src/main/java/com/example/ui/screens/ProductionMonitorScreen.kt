package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.DailyOutputVsTargetPoint
import com.example.domain.OperationsCalculator
import com.example.domain.ProductionDateWindow
import com.example.domain.ProductionMonitorCalculator
import com.example.domain.ProductionTargetBasis
import com.example.model.PlantSection
import com.example.ui.components.DailyAttainmentTrendChartCard
import com.example.ui.components.DailyOutputVsTargetChartCard
import com.example.ui.components.IndustrialKpiCard
import com.example.ui.components.LocalIsDesktopLayout
import com.example.ui.components.SectionOutputVsTargetCard
import com.example.ui.components.desktopPointerHand
import com.example.ui.components.statusBadgeColors
import com.example.ui.components.statusColor
import com.example.ui.theme.DarkChartTrack
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.GrainAmberBright
import com.example.ui.theme.StatusCriticalTextDark
import com.example.ui.theme.StatusHealthyTextDark
import com.example.ui.theme.StatusWatchTextDark
import com.example.viewmodel.OpsUiState

@Composable
fun ProductionMonitorScreen(
    state: OpsUiState.Ready,
    onOpenLogBatch: () -> Unit,
    onOpenScheduleScreen: () -> Unit
) {
    var selectedWindow by remember { mutableStateOf(ProductionDateWindow.DAYS_14) }
    var sectionFilter by remember { mutableStateOf("ALL") }
    var targetBasis by remember { mutableStateOf(ProductionTargetBasis.SCHEDULED) }
    var customTargetInput by remember { mutableStateOf("5000") }
    val customTargetKg = remember(customTargetInput) {
        customTargetInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    }

    val monitorSummary = remember(
        state.batches,
        state.schedules,
        selectedWindow,
        sectionFilter,
        targetBasis,
        customTargetKg
    ) {
        ProductionMonitorCalculator.computeSummary(
            batches = state.batches,
            schedules = state.schedules,
            window = selectedWindow,
            sectionFilter = sectionFilter,
            targetBasis = targetBasis,
            customDailyTargetKg = customTargetKg
        )
    }

    var selectedDayIndex by remember(monitorSummary.points) {
        mutableIntStateOf((monitorSummary.points.size - 1).coerceAtLeast(0))
    }

    LaunchedEffect(monitorSummary.points.size) {
        if (selectedDayIndex >= monitorSummary.points.size) {
            selectedDayIndex = (monitorSummary.points.size - 1).coerceAtLeast(0)
        }
    }

    val isDesktop = LocalIsDesktopLayout.current
    val primaryColor = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("production_monitor_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header & Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Production Monitor",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Daily feed output vs. milling targets across plant shifts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onOpenScheduleScreen,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .desktopPointerHand()
                            .testTag("prod_monitor_open_schedule_button")
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Schedule", maxLines = 1)
                    }
                    Button(
                        onClick = onOpenLogBatch,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForestEmerald,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .desktopPointerHand()
                            .testTag("prod_monitor_log_batch_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Batch", maxLines = 1)
                    }
                }
            }
        }

        // 2. KPI Summary Strip
        item {
            val varianceSign = if (monitorSummary.totalVarianceKg >= 0.0) "+" else ""
            val varianceColor = when {
                monitorSummary.totalTargetKg <= 0.0 -> MaterialTheme.colorScheme.primary
                monitorSummary.totalVarianceKg >= 0.0 -> StatusHealthyTextDark
                monitorSummary.overallAttainmentPct >= 85.0 -> StatusWatchTextDark
                else -> StatusCriticalTextDark
            }
            val attainmentColor = when {
                monitorSummary.totalTargetKg <= 0.0 -> MaterialTheme.colorScheme.primary
                monitorSummary.overallAttainmentPct >= 100.0 -> StatusHealthyTextDark
                monitorSummary.overallAttainmentPct >= 85.0 -> StatusWatchTextDark
                else -> StatusCriticalTextDark
            }

            Row(
                modifier = if (isDesktop) {
                    Modifier.fillMaxWidth().height(IntrinsicSize.Max)
                } else {
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                },
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IndustrialKpiCard(
                    title = "DAILY OUTPUT",
                    primaryValue = "${OperationsCalculator.formatKg(monitorSummary.totalOutputKg)} kg",
                    secondaryValue = "${monitorSummary.totalActualBags} bags • ${monitorSummary.totalBatches} batches",
                    icon = Icons.Filled.Factory,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(195.dp)
                )
                IndustrialKpiCard(
                    title = "TARGET (${targetBasis.shortLabel.uppercase()})",
                    primaryValue = "${OperationsCalculator.formatKg(monitorSummary.totalTargetKg)} kg",
                    secondaryValue = "${monitorSummary.points.size} active day(s) in ${selectedWindow.label}",
                    icon = Icons.Filled.Flag,
                    accentColor = GrainAmberBright,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(195.dp)
                )
                IndustrialKpiCard(
                    title = "ATTAINMENT",
                    primaryValue = if (monitorSummary.totalTargetKg > 0.0) {
                        String.format("%.1f%%", monitorSummary.overallAttainmentPct)
                    } else "—",
                    secondaryValue = if (monitorSummary.evaluatedTargetDaysCount > 0) {
                        "${monitorSummary.onTargetDaysCount}/${monitorSummary.evaluatedTargetDaysCount} days on target"
                    } else "No targets in window",
                    icon = Icons.Filled.CheckCircle,
                    accentColor = attainmentColor,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(190.dp)
                )
                IndustrialKpiCard(
                    title = "NET VARIANCE",
                    primaryValue = if (monitorSummary.totalTargetKg > 0.0) {
                        "$varianceSign${OperationsCalculator.formatKg(monitorSummary.totalVarianceKg)} kg"
                    } else {
                        "${OperationsCalculator.formatKg(monitorSummary.avgDailyOutputKg)} kg/d"
                    },
                    secondaryValue = "Avg ${OperationsCalculator.formatKg(monitorSummary.avgDailyOutputKg)} kg/day",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    accentColor = varianceColor,
                    modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.width(195.dp)
                )
            }
        }

        // 3. Controls & Filter Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Monitoring Scope & Target Basis",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        if (selectedWindow != ProductionDateWindow.DAYS_14 ||
                            sectionFilter != "ALL" ||
                            targetBasis != ProductionTargetBasis.SCHEDULED
                        ) {
                            TextButton(
                                onClick = {
                                    selectedWindow = ProductionDateWindow.DAYS_14
                                    sectionFilter = "ALL"
                                    targetBasis = ProductionTargetBasis.SCHEDULED
                                },
                                modifier = Modifier.testTag("prod_monitor_reset_filters_button")
                            ) {
                                Text("Reset")
                            }
                        }
                    }

                    // Target Basis Segmented Control
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ProductionTargetBasis.entries.forEachIndexed { idx, basis ->
                            SegmentedButton(
                                selected = targetBasis == basis,
                                onClick = {
                                    targetBasis = basis
                                    if (basis == ProductionTargetBasis.CUSTOM_DAILY && customTargetKg <= 0.0) {
                                        customTargetInput = monitorSummary.recommendedCustomDailyTargetKg.toInt().toString()
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = idx,
                                    count = ProductionTargetBasis.entries.size
                                ),
                                modifier = Modifier.testTag("prod_monitor_basis_${basis.name.lowercase()}")
                            ) {
                                Text(basis.shortLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    AnimatedVisibility(visible = targetBasis == ProductionTargetBasis.CUSTOM_DAILY) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = customTargetInput,
                                onValueChange = { raw ->
                                    customTargetInput = raw.filter { it.isDigit() || it == '.' }
                                },
                                label = { Text("Daily Target Output (kg / day)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("prod_monitor_custom_target_input")
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(2500, 5000, 10000, 15000, 20000).forEach { preset ->
                                    AssistChip(
                                        onClick = { customTargetInput = preset.toString() },
                                        label = { Text("${OperationsCalculator.formatKg(preset.toDouble())} kg") },
                                        modifier = Modifier.testTag("prod_monitor_preset_$preset")
                                    )
                                }
                            }
                        }
                    }

                    // Date Window Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Window:", style = MaterialTheme.typography.labelMedium)
                        ProductionDateWindow.entries.forEach { w ->
                            FilterChip(
                                selected = selectedWindow == w,
                                onClick = { selectedWindow = w },
                                label = { Text(w.label) },
                                modifier = Modifier.testTag("prod_monitor_window_${w.name.lowercase()}")
                            )
                        }
                    }

                    // Plant Location Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Plant:", style = MaterialTheme.typography.labelMedium)
                        (listOf("ALL") + PlantSection.entries.map { it.label }).forEach { sec ->
                            FilterChip(
                                selected = sectionFilter == sec,
                                onClick = { sectionFilter = sec },
                                label = { Text(if (sec == "ALL") "All Locations" else sec) },
                                modifier = Modifier.testTag(
                                    "prod_monitor_section_${sec.lowercase().replace(" ", "_")}"
                                )
                            )
                        }
                    }
                }
            }
        }

        // 4. Primary Output vs Target Bar + Line Chart
        item {
            DailyOutputVsTargetChartCard(
                points = monitorSummary.points,
                targetBasis = targetBasis,
                selectedIndex = selectedDayIndex,
                onSelectIndex = { selectedDayIndex = it },
                onSwitchAllTime = if (selectedWindow != ProductionDateWindow.ALL) {
                    { selectedWindow = ProductionDateWindow.ALL }
                } else null
            )
        }

        // 5. Secondary Charts (Attainment Trend & Section Breakdown)
        item {
            if (isDesktop) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DailyAttainmentTrendChartCard(
                        points = monitorSummary.points,
                        selectedIndex = selectedDayIndex,
                        onSelectIndex = { selectedDayIndex = it },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    SectionOutputVsTargetCard(
                        sections = monitorSummary.sectionAttainments,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DailyAttainmentTrendChartCard(
                        points = monitorSummary.points,
                        selectedIndex = selectedDayIndex,
                        onSelectIndex = { selectedDayIndex = it }
                    )
                    SectionOutputVsTargetCard(sections = monitorSummary.sectionAttainments)
                }
            }
        }

        // 6. Daily Breakdown Section Header
        if (monitorSummary.points.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Output Log (${monitorSummary.points.size} day(s))",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Tap a row to focus chart",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val reversedPoints = monitorSummary.points.asReversed()
            itemsIndexed(
                items = reversedPoints,
                key = { _, pt -> pt.isoDate }
            ) { revIdx, pt ->
                val chronologicalIdx = monitorSummary.points.lastIndex - revIdx
                val isSelected = chronologicalIdx == selectedDayIndex
                DailyOutputBreakdownRowCard(
                    point = pt,
                    isSelected = isSelected,
                    defaultPrimary = primaryColor,
                    onClick = { selectedDayIndex = chronologicalIdx }
                )
            }
        }
    }
}

@Composable
private fun DailyOutputBreakdownRowCard(
    point: DailyOutputVsTargetPoint,
    isSelected: Boolean,
    defaultPrimary: Color,
    onClick: () -> Unit
) {
    val barColor = statusColor(point.status, defaultPrimary)
    val (badgeBg, badgeFg) = statusBadgeColors(point.status)
    val progressFrac = if (point.targetKg > 0.0) {
        (point.actualOutputKg / point.targetKg).toFloat().coerceIn(0f, 1f)
    } else if (point.actualOutputKg > 0.0) 1f else 0f
    val variancePrefix = if (point.varianceKg >= 0.0) "+" else ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .desktopPointerHand()
            .testTag("prod_monitor_day_row_${point.isoDate}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${point.shortLabel} • ${point.isoDate}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${point.batchCount} batch(es) • ${point.scheduleCount} schedule(s) • ${String.format("%.1f%%", point.avgRecoveryPct)} yield",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (point.targetKg > 0.0) {
                            "${point.status.label} (${String.format("%.0f%%", point.attainmentPct)})"
                        } else {
                            point.status.label
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progressFrac },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = barColor,
                trackColor = DarkChartTrack
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "OUTPUT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${OperationsCalculator.formatKg(point.actualOutputKg)} kg (${point.actualBags}b)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TARGET",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (point.targetKg > 0.0) "${OperationsCalculator.formatKg(point.targetKg)} kg" else "—",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = GrainAmberBright
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "VARIANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (point.targetKg > 0.0) {
                            "$variancePrefix${OperationsCalculator.formatKg(point.varianceKg)} kg"
                        } else {
                            "+${OperationsCalculator.formatKg(point.actualOutputKg)} kg"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = barColor
                    )
                }
            }
        }
    }
}
