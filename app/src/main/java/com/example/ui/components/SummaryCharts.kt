package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.domain.OperationsCalculator
import com.example.ui.theme.DarkChartTrack
import com.example.ui.theme.GrainAmberBright
import com.example.ui.theme.StatusCriticalTextDark
import com.example.ui.theme.StatusHealthyTextDark
import com.example.ui.theme.StatusWatchTextDark
import kotlin.math.max
import kotlin.math.min

data class DailyFlowChartPoint(
    val isoDate: String,
    val shortLabel: String,
    val producedKg: Double,
    val deliveredKg: Double,
    val recoveryPct: Double,
    val batchCount: Int,
    val dispatchCount: Int
)

data class SectionSupplyDemandRow(
    val sectionLabel: String,
    val availableKg: Double,
    val openOrdersKg: Double,
    val deliveredKg: Double
)

data class DonutSliceItem(
    val label: String,
    val count: Int,
    val color: Color
)

@Composable
fun DailyFlowBarChartCard(
    title: String,
    subtitle: String,
    points: List<DailyFlowChartPoint>,
    onOpenAnalytics: (() -> Unit)? = null,
    onSwitchAllTime: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val deliveryColor = GrainAmberBright
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val trackColor = DarkChartTrack

    var selectedIdx by remember(points) {
        mutableIntStateOf((points.size - 1).coerceAtLeast(0))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .testTag("summary_flow_chart_card"),
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onOpenAnalytics != null) {
                    TextButton(
                        onClick = onOpenAnalytics,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("More Charts", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (points.isEmpty()) {
                ChartEmptyPrompt(
                    message = "No production or delivery records in this period.",
                    onSwitchAllTime = onSwitchAllTime
                )
            } else {
                val safeIdx = selectedIdx.coerceIn(0, points.lastIndex)
                val activePoint = points[safeIdx]

                // Tap-to-inspect readout banner
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
                        Column {
                            Text(
                                text = activePoint.shortLabel,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (activePoint.batchCount > 0) {
                                    "Yield ${String.format("%.1f%%", activePoint.recoveryPct)}"
                                } else {
                                    "${activePoint.dispatchCount} delivery trip(s)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Made", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${OperationsCalculator.formatKg(activePoint.producedKg)} kg",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = primaryColor
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Delivered", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${OperationsCalculator.formatKg(activePoint.deliveredKg)} kg",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = deliveryColor
                                )
                            }
                        }
                    }
                }

                // Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartLegendDot(color = primaryColor, label = "Made (kg)")
                    ChartLegendDot(color = deliveryColor, label = "Delivered (kg)")
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Tap a day to inspect",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val maxVal = points.maxOf { max(it.producedKg, it.deliveredKg) }.coerceAtLeast(100.0)

                // Chart Bars Canvas + Tappable Columns
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(144.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .matchParentSize()
                        ) {
                            if (size.width <= 0f || size.height <= 0f) return@Canvas
                            val padTop = 8.dp.toPx()
                            val padBottom = 4.dp.toPx()
                            val chartH = (size.height - padTop - padBottom).coerceAtLeast(1f)

                            // 3 horizontal guide lines (top, mid, baseline)
                            listOf(0f, 0.5f, 1f).forEach { frac ->
                                val y = padTop + chartH * frac
                                drawLine(
                                    color = gridColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = if (frac < 1f) PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) else null
                                )
                            }

                            val slotW = size.width / points.size.coerceAtLeast(1)
                            val barW = min(14.dp.toPx(), slotW * 0.28f)
                            val gap = 3.dp.toPx()

                            points.forEachIndexed { idx, pt ->
                                val centerX = slotW * idx + slotW / 2f
                                if (idx == safeIdx) {
                                    drawRoundRect(
                                        color = trackColor.copy(alpha = 0.7f),
                                        topLeft = Offset(slotW * idx + 3.dp.toPx(), 0f),
                                        size = Size((slotW - 6.dp.toPx()).coerceAtLeast(2f), size.height),
                                        cornerRadius = CornerRadius(10f, 10f)
                                    )
                                }

                                val prodH = ((pt.producedKg / maxVal).toFloat().coerceIn(0f, 1f)) * chartH
                                val delivH = ((pt.deliveredKg / maxVal).toFloat().coerceIn(0f, 1f)) * chartH

                                if (prodH > 0f) {
                                    drawRoundRect(
                                        color = primaryColor,
                                        topLeft = Offset(centerX - barW - gap / 2f, padTop + chartH - prodH),
                                        size = Size(barW, prodH.coerceAtLeast(3.dp.toPx())),
                                        cornerRadius = CornerRadius(5f, 5f)
                                    )
                                }
                                if (delivH > 0f) {
                                    drawRoundRect(
                                        color = deliveryColor,
                                        topLeft = Offset(centerX + gap / 2f, padTop + chartH - delivH),
                                        size = Size(barW, delivH.coerceAtLeast(3.dp.toPx())),
                                        cornerRadius = CornerRadius(5f, 5f)
                                    )
                                }
                            }
                        }

                        // Invisible touch columns for accurate tapping
                        Row(modifier = Modifier.matchParentSize()) {
                            points.forEachIndexed { idx, _ ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(144.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedIdx = idx }
                                        .desktopPointerHand()
                                )
                            }
                        }
                    }

                    // X-axis date labels
                    Row(modifier = Modifier.fillMaxWidth()) {
                        points.forEachIndexed { idx, pt ->
                            val isSel = idx == safeIdx
                            Text(
                                text = pt.shortLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSel) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedIdx = idx }
                            )
                        }
                    }
                }

                val sumProd = points.sumOf { it.producedKg }
                val sumDeliv = points.sumOf { it.deliveredKg }
                val takeaway = when {
                    sumProd <= 0.0 && sumDeliv > 0.0 -> "Delivered ${OperationsCalculator.formatKg(sumDeliv)} kg from existing warehouse stock."
                    sumProd > 0.0 -> {
                        val ratio = ((sumDeliv / sumProd) * 100.0).coerceAtMost(999.0)
                        "Deliveries moved ${String.format("%.0f%%", ratio)} of production volume across these ${points.size} day(s)."
                    }
                    else -> "Showing daily plant activity."
                }
                Text(
                    text = takeaway,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RecoveryTrendChartCard(
    points: List<DailyFlowChartPoint>,
    targetPct: Double = 98.5,
    onSwitchAllTime: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val batchPoints = remember(points) { points.filter { it.batchCount > 0 } }
    val primaryColor = MaterialTheme.colorScheme.primary
    val targetColor = GrainAmberBright
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

    var selectedIdx by remember(batchPoints) {
        mutableIntStateOf((batchPoints.size - 1).coerceAtLeast(0))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .testTag("summary_recovery_chart_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Production Yield Trend",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Daily recovery rate vs. ${String.format("%.1f%%", targetPct)} plant target",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (batchPoints.isEmpty()) {
                ChartEmptyPrompt(
                    message = "No production batches logged in this period.",
                    onSwitchAllTime = onSwitchAllTime
                )
            } else {
                val safeIdx = selectedIdx.coerceIn(0, batchPoints.lastIndex)
                val active = batchPoints[safeIdx]
                val avgYield = batchPoints.map { it.recoveryPct }.average()
                val bestYield = batchPoints.maxOf { it.recoveryPct }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricChipPill(
                        label = "Selected (${active.shortLabel})",
                        value = String.format("%.1f%%", active.recoveryPct),
                        valueColor = if (active.recoveryPct >= targetPct) StatusHealthyTextDark else StatusWatchTextDark,
                        modifier = Modifier.weight(1f)
                    )
                    MetricChipPill(
                        label = "Average",
                        value = String.format("%.1f%%", avgYield),
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    MetricChipPill(
                        label = "Best Day",
                        value = String.format("%.1f%%", bestYield),
                        valueColor = primaryColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                val rawMin = min(targetPct - 1.5, batchPoints.minOf { it.recoveryPct } - 0.8)
                val rawMax = max(targetPct + 1.5, batchPoints.maxOf { it.recoveryPct } + 0.8)
                val span = (rawMax - rawMin).coerceAtLeast(2.0)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp)
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            if (size.width <= 0f || size.height <= 0f) return@Canvas
                            val padTop = 12.dp.toPx()
                            val padBottom = 12.dp.toPx()
                            val chartH = (size.height - padTop - padBottom).coerceAtLeast(1f)

                            fun yForPct(pct: Double): Float {
                                val norm = ((pct - rawMin) / span).toFloat().coerceIn(0f, 1f)
                                return padTop + chartH * (1f - norm)
                            }

                            // Target reference line
                            val targetY = yForPct(targetPct)
                            drawLine(
                                color = targetColor.copy(alpha = 0.85f),
                                start = Offset(0f, targetY),
                                end = Offset(size.width, targetY),
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

                            val slotW = size.width / batchPoints.size.coerceAtLeast(1)
                            val coords = batchPoints.mapIndexed { idx, pt ->
                                Offset(slotW * idx + slotW / 2f, yForPct(pt.recoveryPct))
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
                                        colors = listOf(primaryColor.copy(alpha = 0.30f), Color.Transparent),
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
                                val pt = batchPoints[idx]
                                val dotColor = when {
                                    pt.recoveryPct < targetPct - 1.0 -> StatusCriticalTextDark
                                    pt.recoveryPct < targetPct -> StatusWatchTextDark
                                    else -> StatusHealthyTextDark
                                }
                                val isSel = idx == safeIdx
                                if (isSel) {
                                    drawCircle(
                                        color = dotColor.copy(alpha = 0.25f),
                                        radius = 10.dp.toPx(),
                                        center = center
                                    )
                                }
                                drawCircle(
                                    color = dotColor,
                                    radius = if (isSel) 5.5.dp.toPx() else 4.dp.toPx(),
                                    center = center
                                )
                            }
                        }

                        Row(modifier = Modifier.matchParentSize()) {
                            batchPoints.forEachIndexed { idx, _ ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(132.dp)
                                        .clickable { selectedIdx = idx }
                                        .desktopPointerHand()
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        batchPoints.forEachIndexed { idx, pt ->
                            val isSel = idx == safeIdx
                            Text(
                                text = pt.shortLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSel) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedIdx = idx }
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartLegendDot(color = StatusHealthyTextDark, label = "On target (≥ ${String.format("%.1f%%", targetPct)})")
                    ChartLegendDot(color = targetColor, label = "Target line")
                }
            }
        }
    }
}

@Composable
fun SectionSupplyDemandChartCard(
    rows: List<SectionSupplyDemandRow>,
    modifier: Modifier = Modifier
) {
    val stockColor = MaterialTheme.colorScheme.primary
    val ordersColor = GrainAmberBright
    val trackColor = DarkChartTrack
    val maxKg = rows.maxOfOrNull { max(it.availableKg, it.openOrdersKg) }?.coerceAtLeast(1.0) ?: 1.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .testTag("summary_supply_demand_chart_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Available Stock vs. Open Orders",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Compare ready warehouse stock against customer demand per location",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ChartLegendDot(color = stockColor, label = "Available Stock")
                ChartLegendDot(color = ordersColor, label = "Open Orders")
            }

            rows.forEach { row ->
                val stockFrac = (row.availableKg / maxKg).toFloat().coerceIn(0f, 1f)
                val orderFrac = (row.openOrdersKg / maxKg).toFloat().coerceIn(0f, 1f)
                val hasShortfall = row.openOrdersKg > row.availableKg + 0.01

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = row.sectionLabel,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (hasShortfall) {
                                "Short ${OperationsCalculator.formatKg(row.openOrdersKg - row.availableKg)} kg"
                            } else {
                                "Covered"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (hasShortfall) StatusCriticalTextDark else StatusHealthyTextDark
                        )
                    }

                    // Stock bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(trackColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(stockFrac)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(stockColor)
                            )
                        }
                        Text(
                            text = "${OperationsCalculator.formatKg(row.availableKg)} kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = stockColor,
                            modifier = Modifier.width(68.dp),
                            textAlign = TextAlign.End
                        )
                    }

                    // Open orders bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(trackColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(orderFrac)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (hasShortfall) StatusCriticalTextDark else ordersColor)
                            )
                        }
                        Text(
                            text = "${OperationsCalculator.formatKg(row.openOrdersKg)} kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasShortfall) StatusCriticalTextDark else ordersColor,
                            modifier = Modifier.width(68.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DonutSplitChartCard(
    title: String,
    centerHeadline: String,
    centerSublabel: String,
    slices: List<DonutSliceItem>,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val total = slices.sumOf { max(0, it.count) }
    val trackColor = DarkChartTrack

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (actionLabel != null && onActionClick != null) {
                    TextButton(
                        onClick = onActionClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(actionLabel, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(104.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(104.dp)) {
                        val strokeW = 14.dp.toPx()
                        val pad = strokeW / 2f
                        val arcSize = Size(
                            width = (size.width - strokeW).coerceAtLeast(1f),
                            height = (size.height - strokeW).coerceAtLeast(1f)
                        )
                        drawArc(
                            color = trackColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(pad, pad),
                            size = arcSize,
                            style = Stroke(width = strokeW)
                        )

                        if (total > 0) {
                            var startAngle = -90f
                            slices.filter { it.count > 0 }.forEach { slice ->
                                val sweep = (slice.count.toFloat() / total.toFloat()) * 360f
                                val adjustedSweep = if (slices.count { it.count > 0 } > 1) {
                                    (sweep - 3f).coerceAtLeast(2f)
                                } else {
                                    sweep
                                }
                                drawArc(
                                    color = slice.color,
                                    startAngle = startAngle,
                                    sweepAngle = adjustedSweep,
                                    useCenter = false,
                                    topLeft = Offset(pad, pad),
                                    size = arcSize,
                                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                )
                                startAngle += sweep
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = centerHeadline,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = centerSublabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    slices.forEach { slice ->
                        val pct = if (total > 0) (slice.count * 100.0 / total) else 0.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .background(slice.color, CircleShape)
                                )
                                Text(
                                    text = slice.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${slice.count} (${String.format("%.0f%%", pct)})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
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

@Composable
private fun MetricChipPill(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = valueColor
            )
        }
    }
}

@Composable
private fun ChartEmptyPrompt(
    message: String,
    onSwitchAllTime: (() -> Unit)?
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                    Text("Show All-Time Trends")
                }
            }
        }
    }
}
