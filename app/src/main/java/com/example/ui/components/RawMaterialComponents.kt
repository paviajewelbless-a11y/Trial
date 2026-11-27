package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.OperationsCalculator
import com.example.model.MaterialSector
import com.example.model.PlantMaterialCatalog
import com.example.model.RawMaterialCategory
import com.example.model.RawMaterialEntity
import com.example.model.RawMaterialProjection
import com.example.model.RawMaterialRiskStatus
import com.example.ui.theme.DarkChartTrack
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.GrainAmber
import com.example.ui.theme.GrainAmberBright
import com.example.ui.theme.OnStatusCriticalContainer
import com.example.ui.theme.OnStatusHealthyContainer
import com.example.ui.theme.OnStatusWatchContainer
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusCriticalContainer
import com.example.ui.theme.StatusCriticalTextDark
import com.example.ui.theme.StatusHealthy
import com.example.ui.theme.StatusHealthyContainer
import com.example.ui.theme.StatusHealthyTextDark
import com.example.ui.theme.StatusWatchContainer
import com.example.ui.theme.StatusWatchTextDark
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun RawMaterialNotificationBanner(
    projections: List<RawMaterialProjection>,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onSendNotificationNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val criticalCount = projections.count { it.status == RawMaterialRiskStatus.CRITICAL }
    val watchCount = projections.count { it.status == RawMaterialRiskStatus.LOW_WATCH }
    val urgentCount = criticalCount + watchCount
    val enabledUrgentNames = projections
        .filter { it.material.alertsEnabled && it.status != RawMaterialRiskStatus.HEALTHY }
        .take(3)
        .joinToString(", ") { it.material.name }

    val (bg, fg, borderCol) = when {
        criticalCount > 0 -> Triple(StatusCriticalContainer, OnStatusCriticalContainer, StatusCritical)
        watchCount > 0 -> Triple(StatusWatchContainer, OnStatusWatchContainer, GrainAmber)
        else -> Triple(StatusHealthyContainer, OnStatusHealthyContainer, StatusHealthyTextDark)
    }

    val titleText = when {
        criticalCount > 0 -> "$criticalCount Ingredient${if (criticalCount == 1) "" else "s"} Critical"
        watchCount > 0 -> "$watchCount Ingredient${if (watchCount == 1) "" else "s"} Running Low"
        else -> "Raw Material Stocks Healthy"
    }
    val subText = when {
        urgentCount > 0 && enabledUrgentNames.isNotBlank() ->
            "Low forecast: $enabledUrgentNames • Calculated from mill batch history"
        urgentCount > 0 ->
            "Alerts muted for low-stock items — tap bell icon on a card to re-enable notifications"
        else ->
            "Projected stocks cover delivery lead times based on historical usage"
    }

    Surface(
        color = bg,
        border = BorderStroke(1.dp, borderCol),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("raw_material_notification_banner")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (urgentCount > 0) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                    contentDescription = titleText,
                    tint = fg,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = fg
                    )
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodySmall,
                        color = fg.copy(alpha = 0.92f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!hasNotificationPermission) {
                    Button(
                        onClick = onRequestNotificationPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForestEmerald,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("enable_raw_material_alerts_button")
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Turn On Phone Alerts", maxLines = 1)
                    }
                } else {
                    OutlinedButton(
                        onClick = onSendNotificationNow,
                        border = BorderStroke(1.dp, fg.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = fg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("send_raw_material_alert_now_button")
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (urgentCount > 0) "Send Low-Stock Alert" else "Check & Notify",
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RawMaterialSimulatorCard(
    lookbackDays: Int,
    demandMultiplier: Double,
    includeScheduled: Boolean,
    leadOverrideDays: Long?,
    onChangeLookback: (Int) -> Unit,
    onChangeDemandMultiplier: (Double) -> Unit,
    onToggleIncludeScheduled: (Boolean) -> Unit,
    onChangeLeadOverrideDays: (Long?) -> Unit,
    onResetSimulator: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val isModified = lookbackDays != 14 ||
        kotlin.math.abs(demandMultiplier - 1.0) > 0.01 ||
        includeScheduled ||
        leadOverrideDays != null

    val summaryChipsText = buildString {
        append("${lookbackDays}d history")
        append(" • ${String.format("%.1fx", demandMultiplier)} pace")
        if (includeScheduled) append(" • +Scheduled runs")
        if (leadOverrideDays != null) append(" • ${leadOverrideDays}d lead")
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .minimumInteractiveComponentSize()
                    .testTag("raw_material_simulator_toggle"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "What-If Forecast Simulator",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = summaryChipsText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isModified) {
                        TextButton(
                            onClick = onResetSimulator,
                            modifier = Modifier.testTag("reset_raw_material_sim_button")
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset")
                        }
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse simulator" else "Expand simulator",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Historical Usage Lookback",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(7 to "Last 7 Days", 14 to "Last 14 Days", 30 to "Last 30 Days").forEach { (days, lbl) ->
                                FilterChip(
                                    selected = lookbackDays == days,
                                    onClick = { onChangeLookback(days) },
                                    label = { Text(lbl) },
                                    modifier = Modifier.testTag("raw_lookback_${days}d")
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mill Demand Pace",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${String.format("%.1fx", demandMultiplier)} ${
                                    when {
                                        demandMultiplier < 0.9 -> "(Slower)"
                                        demandMultiplier > 1.1 -> "(Surge)"
                                        else -> "(Historical Normal)"
                                    }
                                }",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (demandMultiplier > 1.1) GrainAmberBright else MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = demandMultiplier.toFloat().coerceIn(0.5f, 2.5f),
                            onValueChange = { raw ->
                                val stepped = (raw * 10f).roundToInt() / 10.0
                                onChangeDemandMultiplier(stepped)
                            },
                            valueRange = 0.5f..2.5f,
                            steps = 19,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("raw_material_pace_slider")
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0.7 to "0.7x Slow", 1.0 to "1.0x Normal", 1.5 to "1.5x Busy", 2.0 to "2.0x Surge").forEach { (mult, lbl) ->
                                FilterChip(
                                    selected = kotlin.math.abs(demandMultiplier - mult) < 0.05,
                                    onClick = { onChangeDemandMultiplier(mult) },
                                    label = { Text(lbl) }
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Supplier Delivery Lead Time Override",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(null to "Item Default", 2L to "2 Days", 5L to "5 Days", 7L to "7 Days").forEach { (days, lbl) ->
                                FilterChip(
                                    selected = leadOverrideDays == days,
                                    onClick = { onChangeLeadOverrideDays(days) },
                                    label = { Text(lbl) }
                                )
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Include Planned Mill Runs",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Adds scheduled & in-progress feed batches into daily depletion",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = includeScheduled,
                                onCheckedChange = onToggleIncludeScheduled,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("raw_include_scheduled_switch")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RawMaterialForecastChartCard(
    projections: List<RawMaterialProjection>,
    onSelectRestock: (RawMaterialProjection) -> Unit,
    modifier: Modifier = Modifier
) {
    if (projections.isEmpty()) return

    var chartMode by remember { mutableIntStateOf(0) } // 0 = Runway (Days Left), 1 = 7-Day Usage
    var selectedMaterialId by remember(projections) {
        mutableStateOf(projections.firstOrNull()?.material?.id.orEmpty())
    }
    val selectedProj = projections.firstOrNull { it.material.id == selectedMaterialId } ?: projections.first()

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (chartMode == 0) "Days of Stock Runway" else "7-Day Consumption History",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("Runway (Days)", "7-Day Usage (kg)").forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = chartMode == idx,
                        onClick = { chartMode = idx },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = 2),
                        modifier = Modifier.testTag("raw_chart_mode_$idx")
                    ) {
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            if (chartMode == 0) {
                val displayItems = projections.take(7)
                val maxDisplayDays = 21.0
                val trackColor = DarkChartTrack
                val markerColor = GrainAmberBright

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayItems.forEach { proj ->
                        val isSelected = proj.material.id == selectedProj.material.id
                        val barColor = when (proj.status) {
                            RawMaterialRiskStatus.CRITICAL -> StatusCritical
                            RawMaterialRiskStatus.LOW_WATCH -> GrainAmberBright
                            RawMaterialRiskStatus.HEALTHY -> StatusHealthy
                        }
                        val finiteDays = if (proj.daysUntilRunOut.isFinite()) proj.daysUntilRunOut else maxDisplayDays
                        val fraction = (finiteDays / maxDisplayDays).toFloat().coerceIn(0.03f, 1f)
                        val thresholdFraction = ((proj.material.leadTimeDays + proj.material.safetyDays).toDouble() / maxDisplayDays)
                            .toFloat().coerceIn(0.05f, 0.95f)
                        val daysLabel = when {
                            proj.material.currentStockKg <= 0.0 -> "0d"
                            !proj.daysUntilRunOut.isFinite() -> "Idle"
                            proj.daysUntilRunOut > 60.0 -> ">60d"
                            else -> "${String.format("%.1f", proj.daysUntilRunOut)}d"
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                                )
                                .clickable { selectedMaterialId = proj.material.id }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("raw_runway_bar_${proj.material.id}"),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = proj.material.name,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$daysLabel • ${OperationsCalculator.formatKg(proj.material.currentStockKg)} kg",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = when (proj.status) {
                                        RawMaterialRiskStatus.CRITICAL -> StatusCriticalTextDark
                                        RawMaterialRiskStatus.LOW_WATCH -> StatusWatchTextDark
                                        RawMaterialRiskStatus.HEALTHY -> StatusHealthyTextDark
                                    }
                                )
                            }
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                            ) {
                                drawRoundRect(
                                    color = trackColor,
                                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                                    size = size
                                )
                                drawRoundRect(
                                    color = barColor,
                                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                                    size = Size(size.width * fraction, size.height)
                                )
                                val markerX = size.width * thresholdFraction
                                drawLine(
                                    color = markerColor,
                                    start = Offset(markerX, 0f),
                                    end = Offset(markerX, size.height),
                                    strokeWidth = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Dashed amber marker = supplier lead + safety days buffer. Tap any bar to inspect.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // 7-Day Historical Consumption Bar Chart for Selected Ingredient
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    projections.forEach { proj ->
                        FilterChip(
                            selected = proj.material.id == selectedProj.material.id,
                            onClick = { selectedMaterialId = proj.material.id },
                            label = { Text(proj.material.name, maxLines = 1) }
                        )
                    }
                }

                val trend = selectedProj.dailyTrend
                val maxDayKg = trend.maxOfOrNull { it.materialConsumedKg }?.coerceAtLeast(1.0) ?: 1.0
                val barCol = when (selectedProj.status) {
                    RawMaterialRiskStatus.CRITICAL -> StatusCriticalTextDark
                    RawMaterialRiskStatus.LOW_WATCH -> GrainAmberBright
                    RawMaterialRiskStatus.HEALTHY -> MaterialTheme.colorScheme.primary
                }
                val trackCol = DarkChartTrack

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${selectedProj.material.name} (${selectedProj.material.usageSharePct}% mix share)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${OperationsCalculator.formatKg(selectedProj.simulatedAvgDailyKg)} kg/day",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = barCol
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(124.dp)
                            .padding(top = 8.dp)
                    ) {
                        if (trend.isNotEmpty()) {
                            val count = trend.size
                            val slotW = size.width / count
                            val barW = (slotW * 0.56f).coerceAtLeast(10.dp.toPx())
                            trend.forEachIndexed { idx, day ->
                                val x = idx * slotW + (slotW - barW) / 2f
                                drawRoundRect(
                                    color = trackCol,
                                    topLeft = Offset(x, 0f),
                                    size = Size(barW, size.height),
                                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )
                                val hFrac = (day.materialConsumedKg / maxDayKg).toFloat().coerceIn(0f, 1f)
                                val barH = max(if (day.materialConsumedKg > 0.0) 6.dp.toPx() else 0f, size.height * hFrac)
                                if (barH > 0f) {
                                    drawRoundRect(
                                        color = barCol,
                                        topLeft = Offset(x, size.height - barH),
                                        size = Size(barW, barH),
                                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        trend.forEach { day ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = OperationsCalculator.formatKg(day.materialConsumedKg),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = day.shortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Quick Action Strip for Selected Ingredient
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
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = selectedProj.material.name,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = selectedProj.statusReason,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSelectRestock(selectedProj) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForestEmerald,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restock", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun RawMaterialProjectionCard(
    proj: RawMaterialProjection,
    demandMultiplier: Double,
    onRestock: () -> Unit,
    onLogUsage: () -> Unit,
    onToggleAlert: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val mat = proj.material
    var confirmDelete by remember(mat.id) { mutableStateOf(false) }
    val (statusBg, statusFg) = when (proj.status) {
        RawMaterialRiskStatus.CRITICAL -> StatusCriticalContainer to OnStatusCriticalContainer
        RawMaterialRiskStatus.LOW_WATCH -> StatusWatchContainer to OnStatusWatchContainer
        RawMaterialRiskStatus.HEALTHY -> StatusHealthyContainer to OnStatusHealthyContainer
    }
    val accentCol = when (proj.status) {
        RawMaterialRiskStatus.CRITICAL -> StatusCriticalTextDark
        RawMaterialRiskStatus.LOW_WATCH -> StatusWatchTextDark
        RawMaterialRiskStatus.HEALTHY -> StatusHealthyTextDark
    }
    val barFillCol = when (proj.status) {
        RawMaterialRiskStatus.CRITICAL -> StatusCritical
        RawMaterialRiskStatus.LOW_WATCH -> GrainAmberBright
        RawMaterialRiskStatus.HEALTHY -> StatusHealthy
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .testTag("raw_material_card_${mat.id}")
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
                val movementTag = remember(mat.name, mat.supplierName, mat.category) {
                    PlantMaterialCatalog.inferMovementTag(mat)
                }
                val (tagBg, tagFg) = movementTagBadgeColors(movementTag)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = tagBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = movementTag.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = tagFg,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = mat.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        StatusPill(proj.status.label, statusBg, statusFg)
                    }
                    Text(
                        text = mat.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${mat.supplierName.ifBlank { "No supplier set" }} • ${mat.leadTimeDays}d lead • ${String.format("%.1f", mat.usageSharePct)}% mix",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleAlert,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("toggle_raw_alert_${mat.id}")
                    ) {
                        Icon(
                            imageVector = if (mat.alertsEnabled) {
                                Icons.Filled.NotificationsActive
                            } else {
                                Icons.Filled.NotificationsOff
                            },
                            contentDescription = if (mat.alertsEnabled) {
                                "Mute alerts for ${mat.name}"
                            } else {
                                "Enable alerts for ${mat.name}"
                            },
                            tint = if (mat.alertsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("edit_raw_material_${mat.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit ${mat.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("delete_raw_material_${mat.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete ${mat.name}",
                            tint = StatusCriticalTextDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (confirmDelete) {
                AlertDialog(
                    onDismissRequest = { confirmDelete = false },
                    title = { Text("Delete ${mat.name}?") },
                    text = {
                        Text(
                            "This removes ${mat.name} from raw material inventory. Any BOM formulas referencing it will show it as unlinked until re-linked."
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                confirmDelete = false
                                onDelete()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusCritical,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("confirm_delete_raw_material_${mat.id}")
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmDelete = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Visual stock meter vs reorder trigger
            val scaleMaxKg = max(max(mat.currentStockKg, proj.reorderTriggerKg * 1.5), 100.0)
            val fillFrac = (mat.currentStockKg / scaleMaxKg).toFloat().coerceIn(0f, 1f)
            val triggerFrac = (proj.reorderTriggerKg / scaleMaxKg).toFloat().coerceIn(0.05f, 0.95f)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                ) {
                    drawRoundRect(
                        color = DarkChartTrack,
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                        size = size
                    )
                    if (fillFrac > 0f) {
                        drawRoundRect(
                            color = barFillCol,
                            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                            size = Size(size.width * fillFrac, size.height)
                        )
                    }
                    val markerX = size.width * triggerFrac
                    drawLine(
                        color = GrainAmberBright,
                        start = Offset(markerX, 0f),
                        end = Offset(markerX, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Min / Trigger: ${OperationsCalculator.formatKg(proj.reorderTriggerKg)} kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (proj.recommendedRestockKg > 0.0) {
                        Text(
                            text = "Suggest order: +${OperationsCalculator.formatKg(proj.recommendedRestockKg)} kg",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = GrainAmberBright
                        )
                    }
                }
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
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("ON HAND", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${OperationsCalculator.formatKg(mat.currentStockKg)} kg",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column {
                        Text(
                            text = if (kotlin.math.abs(demandMultiplier - 1.0) > 0.05) "SIM. DAILY USE" else "AVG DAILY USE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${OperationsCalculator.formatKg(proj.simulatedAvgDailyKg)} kg/d",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("EST. RUN-OUT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = proj.projectedRunOutLabel,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = accentCol
                        )
                    }
                }
            }

            Text(
                text = proj.statusReason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onLogUsage,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("use_raw_material_${mat.id}")
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Use", maxLines = 1)
                }
                Button(
                    onClick = onRestock,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (proj.status == RawMaterialRiskStatus.HEALTHY) ForestEmerald else GrainAmber,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("restock_raw_material_${mat.id}")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restock", maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun RawMaterialAdjustStockDialog(
    projection: RawMaterialProjection,
    initialIsRestock: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (deltaKg: Double, reasonLabel: String) -> Unit
) {
    val mat = projection.material
    var isRestock by remember { mutableStateOf(initialIsRestock) }
    val defaultAmount = if (initialIsRestock) {
        if (projection.recommendedRestockKg >= 50.0) {
            ((projection.recommendedRestockKg / 50.0).roundToInt() * 50).toString()
        } else {
            "500"
        }
    } else {
        "250"
    }
    var amountText by remember { mutableStateOf(defaultAmount) }
    val amountVal = amountText.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 0.0
    val deltaKg = if (isRestock) amountVal else -amountVal
    val previewStock = max(0.0, mat.currentStockKg + deltaKg)
    val previewDays = if (projection.simulatedAvgDailyKg > 0.01) {
        previewStock / projection.simulatedAvgDailyKg
    } else {
        Double.POSITIVE_INFINITY
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isRestock) "Restock ${mat.name}" else "Log Usage • ${mat.name}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(true to "+ Restock", false to "- Log Usage").forEachIndexed { idx, (restockFlag, lbl) ->
                        SegmentedButton(
                            selected = isRestock == restockFlag,
                            onClick = { isRestock = restockFlag },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = 2)
                        ) {
                            Text(lbl)
                        }
                    }
                }

                Text(
                    text = "Quick presets:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = if (isRestock) {
                        buildList {
                            if (projection.recommendedRestockKg >= 50.0) {
                                val recRounded = ((projection.recommendedRestockKg / 50.0).roundToInt() * 50)
                                add(recRounded.toDouble() to "Rec +${OperationsCalculator.formatKg(recRounded.toDouble())} kg")
                            }
                            add(250.0 to "+250 kg")
                            add(500.0 to "+500 kg")
                            add(1000.0 to "+1,000 kg")
                            add(2500.0 to "+2,500 kg")
                        }.distinctBy { it.first }
                    } else {
                        listOf(
                            100.0 to "-100 kg",
                            250.0 to "-250 kg",
                            500.0 to "-500 kg",
                            1000.0 to "-1,000 kg"
                        )
                    }
                    presets.forEach { (presetVal, lbl) ->
                        FilterChip(
                            selected = kotlin.math.abs(amountVal - presetVal) < 0.5,
                            onClick = { amountText = presetVal.toInt().toString() },
                            label = { Text(lbl) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(if (isRestock) "Quantity Received (kg)" else "Quantity Used (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("raw_adjust_amount_input")
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Live Forecast Impact",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${OperationsCalculator.formatKg(mat.currentStockKg)} kg → ${OperationsCalculator.formatKg(previewStock)} kg",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val coverStr = if (previewDays.isFinite()) {
                            "Covers ~${String.format("%.1f", previewDays)} days at current pace"
                        } else {
                            "No active daily usage logged"
                        }
                        Text(
                            text = coverStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val reason = if (isRestock) "Restock" else "Manual Usage"
                    onConfirm(deltaKg, reason)
                },
                enabled = amountVal > 0.0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestEmerald,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_raw_adjust_button")
            ) {
                Text(if (isRestock) "Add Stock" else "Deduct Stock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RawMaterialEditorDialog(
    existing: RawMaterialEntity?,
    defaultSector: MaterialSector? = null,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        category: RawMaterialCategory,
        currentStockKg: Double,
        reorderThresholdKg: Double,
        usageSharePct: Double,
        leadTimeDays: Long,
        safetyDays: Long,
        supplierName: String,
        alertsEnabled: Boolean
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val initialSector = remember(existing, defaultSector) {
        when {
            existing != null -> PlantMaterialCatalog.inferSector(existing)
            defaultSector != null -> defaultSector
            else -> MaterialSector.MACRO
        }
    }
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var selectedSector by remember { mutableStateOf(initialSector) }
    var category by remember {
        mutableStateOf(
            existing?.category?.let { RawMaterialCategory.fromRaw(it) }
                ?: if (initialSector.isMicro) RawMaterialCategory.ADDITIVE else RawMaterialCategory.ENERGY
        )
    }
    var stockText by remember {
        mutableStateOf(
            existing?.currentStockKg?.toString()
                ?: if (initialSector.isMicro) "50" else "1000"
        )
    }
    var thresholdText by remember {
        mutableStateOf(
            existing?.reorderThresholdKg?.toString()
                ?: if (initialSector.isMicro) "20" else "500"
        )
    }
    var usageShareText by remember {
        mutableStateOf(
            existing?.usageSharePct?.toString()
                ?: if (initialSector.isMicro) "0.2" else "10"
        )
    }
    var leadTimeText by remember { mutableStateOf(existing?.leadTimeDays?.toString() ?: "4") }
    var safetyDaysText by remember {
        mutableStateOf(
            existing?.safetyDays?.toString()
                ?: if (initialSector.isMicro) "5" else "3"
        )
    }
    var supplierName by remember {
        mutableStateOf(
            existing?.supplierName ?: when (defaultSector) {
                MaterialSector.MACRO -> "Macro Supply"
                MaterialSector.MICRO_FAST -> "Micro • Fast Moving"
                MaterialSector.MICRO_SLOW -> "Micro • Slow Moving"
                MaterialSector.MICRO_NON_MOVING -> "Micro • Non-Moving"
                null -> ""
            }
        )
    }
    var alertsEnabled by remember { mutableStateOf(existing?.alertsEnabled ?: true) }
    var confirmDelete by remember { mutableStateOf(false) }

    val parsedStock = stockText.toDoubleOrNull()?.takeIf { it >= 0.0 }
    val parsedThreshold = thresholdText.toDoubleOrNull()?.takeIf { it >= 0.0 }
    val parsedShare = usageShareText.toDoubleOrNull()?.takeIf { it in 0.01..100.0 }
    val parsedLead = leadTimeText.toLongOrNull()?.takeIf { it in 1L..60L }
    val parsedSafety = safetyDaysText.toLongOrNull()?.takeIf { it in 0L..30L }

    val isValid = name.trim().isNotEmpty() &&
        parsedStock != null &&
        parsedThreshold != null &&
        parsedShare != null &&
        parsedLead != null &&
        parsedSafety != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing == null) "Add Raw Material" else "Edit ${existing.name}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ingredient Name") },
                    placeholder = { Text("e.g., Whole Yellow Corn/Balanok Corn") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("raw_name_input")
                )

                Text(
                    text = "Inventory Sector (Macro vs Micro)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MaterialSector.entries.forEach { sec ->
                        FilterChip(
                            selected = selectedSector == sec,
                            onClick = {
                                selectedSector = sec
                                if (sec.isMicro && category !in listOf(RawMaterialCategory.ADDITIVE, RawMaterialCategory.MINERAL)) {
                                    category = RawMaterialCategory.ADDITIVE
                                } else if (!sec.isMicro && category in listOf(RawMaterialCategory.ADDITIVE, RawMaterialCategory.MINERAL)) {
                                    category = RawMaterialCategory.ENERGY
                                }
                                val sectorHint = when (sec) {
                                    MaterialSector.MACRO -> "Macro Supply"
                                    MaterialSector.MICRO_FAST -> "Micro • Fast Moving"
                                    MaterialSector.MICRO_SLOW -> "Micro • Slow Moving"
                                    MaterialSector.MICRO_NON_MOVING -> "Micro • Non-Moving"
                                }
                                val curSup = supplierName.trim()
                                val isGenericHint = curSup.isEmpty() ||
                                    curSup.startsWith("Macro", ignoreCase = true) ||
                                    curSup.startsWith("Micro •", ignoreCase = true)
                                if (isGenericHint) {
                                    supplierName = sectorHint
                                }
                            },
                            label = { Text(sec.shortLabel) }
                        )
                    }
                }

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RawMaterialCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it },
                        label = { Text("Stock (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = thresholdText,
                        onValueChange = { thresholdText = it },
                        label = { Text("Min Alert (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = usageShareText,
                        onValueChange = { usageShareText = it },
                        label = { Text("Mix Share %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = leadTimeText,
                        onValueChange = { leadTimeText = it },
                        label = { Text("Lead (days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = safetyDaysText,
                        onValueChange = { safetyDaysText = it },
                        label = { Text("Safety (d)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text("Supplier / Sector Hint (optional)") },
                    placeholder = { Text("e.g., Micro • Fast Moving or supplier") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Low-Stock Alerts", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "Notify phone when forecast hits warning zone",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = alertsEnabled,
                            onCheckedChange = { alertsEnabled = it },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }

                if (existing != null && onDelete != null) {
                    if (!confirmDelete) {
                        TextButton(
                            onClick = { confirmDelete = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = StatusCriticalTextDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Remove Material")
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { confirmDelete = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Keep")
                            }
                            Button(
                                onClick = onDelete,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StatusCritical,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Confirm Delete")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (parsedStock != null && parsedThreshold != null && parsedShare != null && parsedLead != null && parsedSafety != null && name.trim().isNotEmpty()) {
                        onSave(
                            name.trim(),
                            category,
                            parsedStock,
                            parsedThreshold,
                            parsedShare,
                            parsedLead,
                            parsedSafety,
                            supplierName.trim(),
                            alertsEnabled
                        )
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestEmerald,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("save_raw_material_button")
            ) {
                Text(if (existing == null) "Add Material" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
