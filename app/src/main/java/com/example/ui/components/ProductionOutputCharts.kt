package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.DailyOutputVsTargetPoint
import com.example.domain.DailyTargetStatus
import com.example.domain.OperationsCalculator
import com.example.domain.ProductionTargetBasis
import com.example.domain.SectionOutputAttainment
import com.example.ui.theme.DarkChartTrack
import com.example.ui.theme.GrainAmberBright
import com.example.ui.theme.OnStatusCriticalContainer
import com.example.ui.theme.OnStatusHealthyContainer
import com.example.ui.theme.OnStatusWatchContainer
import com.example.ui.theme.StatusCriticalContainer
import com.example.ui.theme.StatusCriticalTextDark
import com.example.ui.theme.StatusHealthyContainer
import com.example.ui.theme.StatusHealthyTextDark
import com.example.ui.theme.StatusWatchContainer
import com.example.ui.theme.StatusWatchTextDark
import kotlin.math.max
import kotlin.math.min

@Composable
fun DailyOutputVsTargetChartCard(
    points: List<DailyOutputVsTargetPoint>,
    targetBasis: ProductionTargetBasis,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onSwitchAllTime: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val trackColor = DarkChartTrack
    val targetLineColor = GrainAmberBright
    val unscheduledBarColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .testTag("production_output_vs_target_chart_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Daily Feed Output vs. Target",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Output bars (kg) vs. ${targetBasis.label.lowercase()} target line — tap any day to inspect",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (points.isEmpty()) {
                ProductionEmptyChartPrompt(
                    message = "No production batches or active schedules found for this filter.",
                    onSwitchAllTime = onSwitchAllTime
                )
            } else {
                val safeIdx = selectedIndex.coerceIn(0, points.lastIndex)
                val active = points[safeIdx]

                // Interactive Tooltip Readout Banner
                SelectedDayTooltipBanner(point = active, targetBasis = targetBasis)

                // Legend Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendChipDot(color = StatusHealthyTextDark, label = "On Target (≥100%)")
                    LegendChipDot(color = GrainAmberBright, label = "Near (85–99%)")
                    LegendChipDot(color = StatusCriticalTextDark, label = "Below (<85%)")
                    LegendChipDot(color = targetLineColor, label = "Target (${targetBasis.shortLabel})")
                }

                val maxVal = points.maxOf { max(it.actualOutputKg, it.targetKg) }.coerceAtLeast(100.0)
                val midVal = maxVal / 2.0

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val yAxisWidth = 46.dp
                    val chartPlotAvailWidth = (maxWidth - yAxisWidth - 6.dp).coerceAtLeast(200.dp)
                    val useHorizontalScroll = points.size > 8
                    val slotDp = if (useHorizontalScroll) 48.dp else (chartPlotAvailWidth / points.size.coerceAtLeast(1))
                    val plotContentWidth = if (useHorizontalScroll) slotDp * points.size else chartPlotAvailWidth
                    val chartHeight = 168.dp

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left Y-Axis Scale Labels
                        Column(
                            modifier = Modifier
                                .width(yAxisWidth)
                                .height(chartHeight),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = compactKgLabel(maxVal),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = compactKgLabel(midVal),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "0 kg",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Scrollable or Full-Width Plot + X-Axis Labels
                        val plotScrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (useHorizontalScroll) Modifier.horizontalScroll(plotScrollState) else Modifier
                                ),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(plotContentWidth)
                                    .height(chartHeight)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    if (size.width <= 0f || size.height <= 0f) return@Canvas
                                    val padTop = 10.dp.toPx()
                                    val padBottom = 4.dp.toPx()
                                    val chartH = (size.height - padTop - padBottom).coerceAtLeast(1f)

                                    fun yForKg(kg: Double): Float {
                                        val norm = (kg / maxVal).toFloat().coerceIn(0f, 1f)
                                        return padTop + chartH * (1f - norm)
                                    }

                                    // Horizontal grid lines
                                    listOf(0f, 0.5f, 1f).forEach { frac ->
                                        val y = padTop + chartH * frac
                                        drawLine(
                                            color = gridColor,
                                            start = Offset(0f, y),
                                            end = Offset(size.width, y),
                                            strokeWidth = 1.dp.toPx(),
                                            pathEffect = if (frac < 1f) {
                                                PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                                            } else null
                                        )
                                    }

                                    val slotW = size.width / points.size.coerceAtLeast(1)
                                    val barW = min(20.dp.toPx(), slotW * 0.42f)
                                    val targetBarW = min(10.dp.toPx(), slotW * 0.24f)

                                    // Draw bars per day
                                    points.forEachIndexed { idx, pt ->
                                        val centerX = slotW * idx + slotW / 2f
                                        if (idx == safeIdx) {
                                            drawRoundRect(
                                                color = trackColor.copy(alpha = 0.65f),
                                                topLeft = Offset(slotW * idx + 2.dp.toPx(), 0f),
                                                size = Size((slotW - 4.dp.toPx()).coerceAtLeast(2f), size.height),
                                                cornerRadius = CornerRadius(10f, 10f)
                                            )
                                        }

                                        // Background subtle target bar when non-custom so individual day targets are easy to gauge
                                        if (pt.targetKg > 0.0 && targetBasis != ProductionTargetBasis.CUSTOM_DAILY) {
                                            val targetTopY = yForKg(pt.targetKg)
                                            val targetH = (padTop + chartH) - targetTopY
                                            drawRoundRect(
                                                color = targetLineColor.copy(alpha = 0.22f),
                                                topLeft = Offset(centerX + 2.dp.toPx(), targetTopY),
                                                size = Size(targetBarW, targetH.coerceAtLeast(2.dp.toPx())),
                                                cornerRadius = CornerRadius(4f, 4f)
                                            )
                                        }

                                        // Actual output bar
                                        if (pt.actualOutputKg > 0.0) {
                                            val outTopY = yForKg(pt.actualOutputKg)
                                            val outH = (padTop + chartH) - outTopY
                                            val barColor = statusColor(pt.status, unscheduledBarColor)
                                            drawRoundRect(
                                                color = barColor,
                                                topLeft = Offset(
                                                    if (pt.targetKg > 0.0 && targetBasis != ProductionTargetBasis.CUSTOM_DAILY) {
                                                        centerX - barW + 1.dp.toPx()
                                                    } else {
                                                        centerX - barW / 2f
                                                    },
                                                    outTopY
                                                ),
                                                size = Size(barW, outH.coerceAtLeast(3.dp.toPx())),
                                                cornerRadius = CornerRadius(5f, 5f)
                                            )
                                        }
                                    }

                                    // Target reference line overlay
                                    if (targetBasis == ProductionTargetBasis.CUSTOM_DAILY) {
                                        val customKg = points.first().targetKg
                                        if (customKg > 0.0) {
                                            val targetY = yForKg(customKg)
                                            drawLine(
                                                color = targetLineColor,
                                                start = Offset(0f, targetY),
                                                end = Offset(size.width, targetY),
                                                strokeWidth = 2.dp.toPx(),
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                            )
                                        }
                                    } else {
                                        val targetCoords = points.mapIndexedNotNull { idx, pt ->
                                            if (pt.targetKg > 0.0) {
                                                idx to Offset(slotW * idx + slotW / 2f, yForKg(pt.targetKg))
                                            } else null
                                        }
                                        if (targetCoords.size >= 2) {
                                            val path = Path().apply {
                                                moveTo(targetCoords.first().second.x, targetCoords.first().second.y)
                                                for (i in 1 until targetCoords.size) {
                                                    lineTo(targetCoords[i].second.x, targetCoords[i].second.y)
                                                }
                                            }
                                            drawPath(
                                                path = path,
                                                color = targetLineColor,
                                                style = Stroke(
                                                    width = 2.dp.toPx(),
                                                    cap = StrokeCap.Round,
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                                                )
                                            )
                                        }
                                        targetCoords.forEach { (idx, center) ->
                                            val isSel = idx == safeIdx
                                            drawCircle(
                                                color = targetLineColor,
                                                radius = if (isSel) 5.dp.toPx() else 3.5.dp.toPx(),
                                                center = center
                                            )
                                        }
                                    }
                                }

                                // Touch target overlays per day
                                Row(modifier = Modifier.fillMaxSize()) {
                                    points.forEachIndexed { idx, pt ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { onSelectIndex(idx) }
                                                .desktopPointerHand()
                                                .testTag("prod_chart_bar_${pt.isoDate}")
                                        )
                                    }
                                }
                            }

                            // X-Axis Date Labels
                            Row(modifier = Modifier.width(plotContentWidth)) {
                                points.forEachIndexed { idx, pt ->
                                    val isSel = idx == safeIdx
                                    Text(
                                        text = pt.shortLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onSelectIndex(idx) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDayTooltipBanner(
    point: DailyOutputVsTargetPoint,
    targetBasis: ProductionTargetBasis
) {
    val (badgeBg, badgeFg) = statusBadgeColors(point.status)
    val variancePrefix = if (point.varianceKg >= 0.0) "+" else ""
    val varianceColor = when {
        point.targetKg <= 0.0 -> MaterialTheme.colorScheme.primary
        point.varianceKg >= 0.0 -> StatusHealthyTextDark
        point.attainmentPct >= 85.0 -> StatusWatchTextDark
        else -> StatusCriticalTextDark
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("production_chart_selected_tooltip")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${point.shortLabel} (${point.isoDate})",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${point.batchCount} batch(es) • ${point.scheduleCount} run(s) • ${String.format("%.1f%%", point.avgRecoveryPct)} yield",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = point.status.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TooltipMetric(
                    label = "ACTUAL OUTPUT",
                    value = "${OperationsCalculator.formatKg(point.actualOutputKg)} kg",
                    sub = "${point.actualBags} bags",
                    valueColor = MaterialTheme.colorScheme.onSurface
                )
                TooltipMetric(
                    label = "TARGET (${targetBasis.shortLabel.uppercase()})",
                    value = if (point.targetKg > 0.0) "${OperationsCalculator.formatKg(point.targetKg)} kg" else "—",
                    sub = if (point.targetBags > 0L) "${point.targetBags} bags" else "No target set",
                    valueColor = GrainAmberBright
                )
                TooltipMetric(
                    label = "VARIANCE",
                    value = if (point.targetKg > 0.0) {
                        "$variancePrefix${OperationsCalculator.formatKg(point.varianceKg)} kg"
                    } else {
                        "+${OperationsCalculator.formatKg(point.actualOutputKg)} kg"
                    },
                    sub = if (point.targetKg > 0.0) String.format("%.1f%% attained", point.attainmentPct) else "Unscheduled",
                    valueColor = varianceColor,
                    horizontalAlignment = Alignment.End
                )
            }
        }
    }
}

@Composable
private fun TooltipMetric(
    label: String,
    value: String,
    sub: String,
    valueColor: Color,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start
) {
    Column(horizontalAlignment = horizontalAlignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = valueColor
        )
        Text(
            text = sub,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DailyAttainmentTrendChartCard(
    points: List<DailyOutputVsTargetPoint>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val targetPoints = points.filter { it.targetKg > 0.0 }
    val primaryColor = MaterialTheme.colorScheme.primary
    val targetColor = GrainAmberBright
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .testTag("production_attainment_trend_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Target Attainment Trend (%)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Daily % of target achieved against the 100% benchmark line",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (targetPoints.isEmpty()) {
                Text(
                    text = "Add a schedule target or switch to Batch Expected / Custom Daily Target to view % attainment trend.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxPct = max(115.0, targetPoints.maxOf { it.attainmentPct } + 10.0)
                val minPct = 0.0

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (size.width <= 0f || size.height <= 0f) return@Canvas
                        val padTop = 10.dp.toPx()
                        val padBottom = 10.dp.toPx()
                        val chartH = (size.height - padTop - padBottom).coerceAtLeast(1f)

                        fun yForPct(pct: Double): Float {
                            val norm = ((pct - minPct) / (maxPct - minPct)).toFloat().coerceIn(0f, 1f)
                            return padTop + chartH * (1f - norm)
                        }

                        // 100% Target Reference Line
                        val y100 = yForPct(100.0)
                        drawLine(
                            color = targetColor,
                            start = Offset(0f, y100),
                            end = Offset(size.width, y100),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        )

                        // Baseline
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, size.height - padBottom),
                            end = Offset(size.width, size.height - padBottom),
                            strokeWidth = 1.dp.toPx()
                        )

                        val slotW = size.width / targetPoints.size.coerceAtLeast(1)
                        val coords = targetPoints.mapIndexed { idx, pt ->
                            Offset(slotW * idx + slotW / 2f, yForPct(pt.attainmentPct))
                        }

                        if (coords.size >= 2) {
                            val areaPath = Path().apply {
                                moveTo(coords.first().x, size.height - padBottom)
                                coords.forEach { lineTo(it.x, it.y) }
                                lineTo(coords.last().x, size.height - padBottom)
                                close()
                            }
                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(primaryColor.copy(alpha = 0.28f), Color.Transparent),
                                    startY = padTop,
                                    endY = size.height - padBottom
                                )
                            )

                            val linePath = Path().apply {
                                moveTo(coords.first().x, coords.first().y)
                                for (i in 1 until coords.size) {
                                    lineTo(coords[i].x, coords[i].y)
                                }
                            }
                            drawPath(
                                path = linePath,
                                color = primaryColor,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        coords.forEachIndexed { idx, center ->
                            val pt = targetPoints[idx]
                            val globalIdx = points.indexOfFirst { it.isoDate == pt.isoDate }
                            val isSel = globalIdx == selectedIndex
                            val dotColor = statusColor(pt.status, primaryColor)
                            if (isSel) {
                                drawCircle(
                                    color = dotColor.copy(alpha = 0.25f),
                                    radius = 9.dp.toPx(),
                                    center = center
                                )
                            }
                            drawCircle(
                                color = dotColor,
                                radius = if (isSel) 5.dp.toPx() else 3.5.dp.toPx(),
                                center = center
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxSize()) {
                        targetPoints.forEach { pt ->
                            val globalIdx = points.indexOfFirst { it.isoDate == pt.isoDate }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { if (globalIdx >= 0) onSelectIndex(globalIdx) }
                                    .desktopPointerHand()
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendChipDot(color = primaryColor, label = "Attainment %")
                    LegendChipDot(color = targetColor, label = "100% Target")
                }
            }
        }
    }
}

@Composable
fun SectionOutputVsTargetCard(
    sections: List<SectionOutputAttainment>,
    modifier: Modifier = Modifier
) {
    val maxVal = sections.maxOfOrNull { max(it.actualOutputKg, it.targetKg) }?.coerceAtLeast(1.0) ?: 1.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .testTag("production_section_target_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Output vs. Target by Plant Section",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Comparison across milling locations in the selected window",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            sections.forEachIndexed { idx, row ->
                val outFrac = (row.actualOutputKg / maxVal).toFloat().coerceIn(0f, 1f)
                val tgtFrac = (row.targetKg / maxVal).toFloat().coerceIn(0f, 1f)
                val barColor = when {
                    row.targetKg <= 0.0 -> MaterialTheme.colorScheme.primary
                    row.attainmentPct >= 100.0 -> StatusHealthyTextDark
                    row.attainmentPct >= 85.0 -> GrainAmberBright
                    else -> StatusCriticalTextDark
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = row.section.label,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (row.targetKg > 0.0) {
                                "${String.format("%.1f%%", row.attainmentPct)} • ${OperationsCalculator.formatKg(row.actualOutputKg)} / ${OperationsCalculator.formatKg(row.targetKg)} kg"
                            } else {
                                "${OperationsCalculator.formatKg(row.actualOutputKg)} kg (${row.batchCount} batches)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = barColor
                        )
                    }

                    // Dual comparative horizontal bars (Output vs Target)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkChartTrack)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(outFrac)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(barColor)
                            )
                        }
                        if (row.targetKg > 0.0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DarkChartTrack.copy(alpha = 0.65f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(tgtFrac)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(GrainAmberBright.copy(alpha = 0.85f))
                                )
                            }
                        }
                    }
                }

                if (idx < sections.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ProductionEmptyChartPrompt(
    message: String,
    onSwitchAllTime: (() -> Unit)?
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (onSwitchAllTime != null) {
                TextButton(onClick = onSwitchAllTime) {
                    Text("Switch to All-Time")
                }
            }
        }
    }
}

@Composable
private fun LegendChipDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun statusColor(status: DailyTargetStatus, defaultPrimary: Color): Color = when (status) {
    DailyTargetStatus.ON_TARGET -> StatusHealthyTextDark
    DailyTargetStatus.NEAR_TARGET -> GrainAmberBright
    DailyTargetStatus.BELOW_TARGET -> StatusCriticalTextDark
    DailyTargetStatus.UNSCHEDULED_OUTPUT -> defaultPrimary
    DailyTargetStatus.NO_TARGET -> DarkChartTrack
}

fun statusBadgeColors(status: DailyTargetStatus): Pair<Color, Color> = when (status) {
    DailyTargetStatus.ON_TARGET -> StatusHealthyContainer to OnStatusHealthyContainer
    DailyTargetStatus.NEAR_TARGET -> StatusWatchContainer to OnStatusWatchContainer
    DailyTargetStatus.BELOW_TARGET -> StatusCriticalContainer to OnStatusCriticalContainer
    DailyTargetStatus.UNSCHEDULED_OUTPUT -> StatusHealthyContainer to OnStatusHealthyContainer
    DailyTargetStatus.NO_TARGET -> DarkChartTrack to Color.White
}

private fun compactKgLabel(kg: Double): String = when {
    kg >= 1000.0 -> {
        val tons = kg / 1000.0
        if (tons >= 10.0) "${tons.toInt()}t" else String.format("%.1ft", tons)
    }
    else -> "${kg.toInt()}"
}
