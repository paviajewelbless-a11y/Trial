package com.example.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.model.DailyPlantLog
import com.example.model.PlantReportData
import com.example.model.ReportSectionOption
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

data class SnapshotTableRow(
    val label: String,
    val value: String
)

data class SnapshotSectionTable(
    val leftHeader: String,
    val rightHeaderTop: String,
    val rightHeaderBottom: String,
    val rows: List<SnapshotTableRow>,
    val totalLabel: String? = null,
    val totalValue: String? = null
)

data class SnapshotPayload(
    val title: String,
    val subtitle: String,
    val kpiSummary: String,
    val tables: List<SnapshotSectionTable>,
    val fileName: String
)

object SnapshotBitmapRenderer {

    fun availablePlantSections(report: PlantReportData): List<String> {
        val sections = if (report.productRows.isNotEmpty()) {
            report.productRows.map { it.section }
        } else {
            report.sectionSummaries.map { it.section.label }
        }
        return ReportExporter.orderedSectionsForNames(sections)
    }

    fun availableDailyLogSections(dayLog: DailyPlantLog, sectionFilter: String = "ALL"): List<String> {
        val filtered = dayLog.skuRows.filter { sectionFilter == "ALL" || it.sku.section == sectionFilter }
        return ReportExporter.orderedSectionsForNames(filtered.map { it.sku.section })
    }

    fun buildPlantSnapshotPayload(
        report: PlantReportData,
        targetPlantSection: String? = null
    ): SnapshotPayload {
        val invDayTag = if (report.isDailyReport && report.startDate == OperationsCalculator.todayIsoDate()) {
            "TODAY"
        } else if (report.isDailyReport) {
            report.startDate
        } else {
            "${report.startDate} to ${report.endDate}"
        }
        val periodLabel = if (report.isDailyReport) "Date: ${report.startDate}" else "Period: ${report.startDate} to ${report.endDate}"
        val scopePlant = targetPlantSection ?: report.sectionFilter

        val tables = mutableListOf<SnapshotSectionTable>()

        if (report.includes(ReportSectionOption.INVENTORY)) {
            val rowsBySection = report.productRows
                .filter { targetPlantSection == null || it.section.equals(targetPlantSection, ignoreCase = true) }
                .groupBy { it.section }
            val sectionOrder = ReportExporter.orderedSectionsForNames(rowsBySection.keys)
            sectionOrder.forEach { sectionName ->
                val rows = rowsBySection[sectionName].orEmpty()
                val secSummary = report.sectionSummaries.firstOrNull {
                    it.section.label.equals(sectionName, ignoreCase = true)
                }
                val secBags = secSummary?.totalAvailableBags ?: rows.sumOf { max(0.0, it.availableBags) }
                val secKg = secSummary?.totalAvailableStockKg ?: rows.sumOf { max(0.0, it.availableStockKg) }
                tables.add(
                    SnapshotSectionTable(
                        leftHeader = sectionName.uppercase(),
                        rightHeaderTop = "AVAILABLE",
                        rightHeaderBottom = "INVENTORY ($invDayTag)",
                        rows = rows.map { r ->
                            SnapshotTableRow(
                                label = ReportExporter.displayProductNameForSection(r.productName, sectionName),
                                value = OperationsCalculator.formatBags(r.availableBags)
                            )
                        },
                        totalLabel = "TOTAL $sectionName",
                        totalValue = "${OperationsCalculator.formatBags(secBags)} bags (${OperationsCalculator.formatKg(secKg)} kg)"
                    )
                )
            }
        }

        // If user deselected Inventory (e.g., wants a snapshot of Dispatches/Orders/Recovery/Schedule), render those cleanly
        if (!report.includes(ReportSectionOption.INVENTORY) || tables.isEmpty()) {
            if (report.includes(ReportSectionOption.RECOVERY)) {
                val sections = report.sectionSummaries.filter {
                    targetPlantSection == null || it.section.label.equals(targetPlantSection, ignoreCase = true)
                }
                sections.forEach { sec ->
                    tables.add(
                        SnapshotSectionTable(
                            leftHeader = "${sec.section.label} PRODUCTION",
                            rightHeaderTop = "RECOVERED",
                            rightHeaderBottom = "BAGS / KG",
                            rows = if (sec.batches.isEmpty()) {
                                listOf(SnapshotTableRow("No production batches logged", "-"))
                            } else {
                                sec.batches.map { b ->
                                    val custNote = b.allScheduledForCustomers().takeIf { it.isNotEmpty() }
                                        ?.joinToString(", ")?.let { " • For: $it" }.orEmpty()
                                    val remarksNote = if (b.remarks.isNotBlank()) " • ${b.remarks}" else ""
                                    SnapshotTableRow(
                                        label = "${b.date} • ${ReportExporter.displayProductNameForSection(b.productName, sec.section.label)}\nRecovery: ${String.format(Locale.US, "%.1f%%", b.recoveryPct)} • Expected: ${OperationsCalculator.formatKg(b.expectedWeightKg)} kg • Var: ${OperationsCalculator.formatKg(b.varianceKg)} kg$remarksNote$custNote",
                                        value = "${b.bagsProduced} bags\n(${OperationsCalculator.formatKg(b.actualRecoveredKg)} kg)"
                                    )
                                }
                            },
                            totalLabel = "TOTAL PRODUCED",
                            totalValue = "${sec.totalBags} bags\n(${OperationsCalculator.formatKg(sec.totalRecoveredKg)} kg)"
                        )
                    )
                }
            }
            if (report.includes(ReportSectionOption.DISPATCHES)) {
                tables.add(
                    SnapshotSectionTable(
                        leftHeader = "DISPATCHES (${report.dispatches.size})",
                        rightHeaderTop = "DISPATCHED",
                        rightHeaderBottom = "KG / BAGS",
                        rows = if (report.dispatches.isEmpty()) {
                            listOf(SnapshotTableRow("No dispatches in period", "-"))
                        } else {
                            report.dispatches.map { d ->
                                val fleet = listOfNotNull(
                                    d.driverName.takeIf { it.isNotBlank() },
                                    d.truckNumber.takeIf { it.isNotBlank() }
                                ).joinToString(" / ")
                                val fleetPart = if (fleet.isNotBlank()) " • Fleet: $fleet" else ""
                                val linkedPoCode = report.orders.firstOrNull { it.id.isNotBlank() && it.id == d.linkedOrderId }?.orderCode
                                    ?: report.activeOpenOrders.firstOrNull { it.id.isNotBlank() && it.id == d.linkedOrderId }?.orderCode
                                val orderPart = if (!linkedPoCode.isNullOrBlank()) " • PO: $linkedPoCode" else ""
                                val plantPart = if (d.sourcePlant.isNotBlank()) " • ${d.sourcePlant}" else ""
                                val remarksPart = if (d.remarks.isNotBlank()) " • ${d.remarks}" else ""
                                SnapshotTableRow(
                                    label = "DR ${d.drReference} • ${d.customer}$orderPart\n${d.productName}$plantPart\nDate: ${d.date} • Status: ${d.resolvedStatusLabel()}$fleetPart$remarksPart",
                                    value = "${OperationsCalculator.formatKg(d.effectiveDispatchedWeightKg())} kg\n(${d.bagsDispatched} bags)"
                                )
                            }
                        },
                        totalLabel = "TOTAL DISPATCHED",
                        totalValue = "${OperationsCalculator.formatKg(report.totalDispatchedKg)} kg"
                    )
                )
            }
            if (report.includes(ReportSectionOption.ORDERS)) {
                tables.add(
                    SnapshotSectionTable(
                        leftHeader = "ACTIVE OPEN ORDERS (${report.activeOpenOrders.size})",
                        rightHeaderTop = "REMAINING",
                        rightHeaderBottom = "WEIGHT (KG)",
                        rows = if (report.activeOpenOrders.isEmpty()) {
                            listOf(SnapshotTableRow("No active open orders", "-"))
                        } else {
                            report.activeOpenOrders.map { o ->
                                val remKg = max(0.0, o.totalWeightKg - o.dispatchedWeightKg)
                                val remBags = if (o.bagWeightKg > 0.0) remKg / o.bagWeightKg else 0.0
                                val tags = buildList {
                                    if (o.priority) add("PRIORITY")
                                    if (o.rushOrder) add("RUSH")
                                }.joinToString(" • ")
                                val tagSuffix = if (tags.isNotEmpty()) " [$tags]" else ""
                                val sentPart = if (o.dispatchedWeightKg > 0.0) {
                                    " • Sent: ${OperationsCalculator.formatKg(o.dispatchedWeightKg)} kg"
                                } else {
                                    ""
                                }
                                SnapshotTableRow(
                                    label = "${o.orderCode}$tagSuffix • ${o.customer}\n${o.productName}\nOrdered: ${o.orderedBags} bags (${OperationsCalculator.formatKg(o.totalWeightKg)} kg)$sentPart • Due: ${o.dueDate} • ${o.status}",
                                    value = "${OperationsCalculator.formatKg(remKg)} kg\n(${OperationsCalculator.formatBags(remBags)} bags)"
                                )
                            }
                        },
                        totalLabel = "TOTAL OPEN ORDERS",
                        totalValue = "${OperationsCalculator.formatKg(report.totalOpenOrdersKg)} kg"
                    )
                )
            }
            if (report.includes(ReportSectionOption.SCHEDULE)) {
                val filteredSchedules = report.schedules.filter {
                    targetPlantSection == null || it.section.equals(targetPlantSection, ignoreCase = true)
                }
                val totalSchedBags = filteredSchedules.sumOf { it.targetBags }
                tables.add(
                    SnapshotSectionTable(
                        leftHeader = "SCHEDULED RUNS (${filteredSchedules.size})",
                        rightHeaderTop = "TARGET",
                        rightHeaderBottom = "BAGS / STATUS",
                        rows = if (filteredSchedules.isEmpty()) {
                            listOf(SnapshotTableRow("No scheduled runs in period", "-"))
                        } else {
                            filteredSchedules.map { s ->
                                val ordTag = s.allLinkedOrderCodes().takeIf { it.isNotEmpty() }
                                    ?.joinToString(", ")?.let { " • POs: $it" }.orEmpty()
                                val remarksPart = if (s.lineRemarks.isNotBlank()) " • ${s.lineRemarks}" else ""
                                SnapshotTableRow(
                                    label = "${s.scheduledDate} • ${s.section}\n${ReportExporter.displayProductNameForSection(s.productName, s.section)}\n${s.priority}$remarksPart$ordTag",
                                    value = "${s.targetBags} bags\n(${s.status})"
                                )
                            }
                        },
                        totalLabel = "TOTAL SCHEDULED",
                        totalValue = "$totalSchedBags bags (${filteredSchedules.size} runs)"
                    )
                )
            }
        }

        val kpiParts = buildList {
            if (report.includes(ReportSectionOption.INVENTORY)) {
                val filteredRows = report.productRows.filter {
                    targetPlantSection == null || it.section.equals(targetPlantSection, ignoreCase = true)
                }
                val bags = filteredRows.sumOf { max(0.0, it.availableBags) }
                val kg = filteredRows.sumOf { max(0.0, it.availableStockKg) }
                add("Available: ${OperationsCalculator.formatBags(bags)} bags (${OperationsCalculator.formatKg(kg)} kg)")
            }
            if (report.includes(ReportSectionOption.RECOVERY)) {
                val filteredSecs = report.sectionSummaries.filter {
                    targetPlantSection == null || it.section.label.equals(targetPlantSection, ignoreCase = true)
                }
                val bags = if (targetPlantSection == null) report.totalProducedBags else filteredSecs.sumOf { it.totalBags }
                val kg = if (targetPlantSection == null) report.totalRecoveredKg else filteredSecs.sumOf { it.totalRecoveredKg }
                add("Produced: ${bags}b (${OperationsCalculator.formatKg(kg)} kg)")
            }
            if (report.includes(ReportSectionOption.DISPATCHES) && targetPlantSection == null) {
                add("Dispatched: ${OperationsCalculator.formatKg(report.totalDispatchedKg)} kg (${report.dispatches.size} trips)")
            }
            if (report.includes(ReportSectionOption.ORDERS) && targetPlantSection == null) {
                add("Open Orders: ${OperationsCalculator.formatKg(report.totalOpenOrdersKg)} kg")
            }
            if (report.includes(ReportSectionOption.SCHEDULE)) {
                val filteredSchedules = report.schedules.filter {
                    targetPlantSection == null || it.section.equals(targetPlantSection, ignoreCase = true)
                }
                val activeScheds = filteredSchedules.filter { !it.status.equals("Completed", ignoreCase = true) }
                add("Schedule: ${filteredSchedules.size} runs (${activeScheds.size} active / ${activeScheds.sumOf { it.targetBags }}b)")
            }
        }

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val plantSlug = (targetPlantSection ?: report.sectionFilter).replace(" ", "_")
        val fileName = "Bulacan_Snapshot_${plantSlug}_${report.startDate}_$stamp.jpg"

        return SnapshotPayload(
            title = "10K AGRI FEEDS - BULACAN PLANT",
            subtitle = "$periodLabel  •  Plant: $scopePlant",
            kpiSummary = kpiParts.joinToString("   |   "),
            tables = tables,
            fileName = fileName
        )
    }

    fun buildDailyLogSnapshotPayload(
        dayLog: DailyPlantLog,
        sectionFilter: String = "ALL",
        targetPlantSection: String? = null
    ): SnapshotPayload {
        val invTag = if (dayLog.isToday) "TODAY" else dayLog.date
        val effectiveFilter = targetPlantSection ?: sectionFilter
        val filteredSkus = dayLog.skuRows.filter {
            (sectionFilter == "ALL" || it.sku.section == sectionFilter) &&
                (targetPlantSection == null || it.sku.section.equals(targetPlantSection, ignoreCase = true))
        }
        val rowsBySection = filteredSkus.groupBy { it.sku.section }
        val sectionOrder = ReportExporter.orderedSectionsForNames(rowsBySection.keys)

        val tables = sectionOrder.map { sectionName ->
            val rows = rowsBySection[sectionName].orEmpty()
            val secBags = rows.sumOf { max(0.0, it.closingBags) }
            val secKg = rows.sumOf { max(0.0, it.closingStockKg) }
            SnapshotSectionTable(
                leftHeader = sectionName.uppercase(),
                rightHeaderTop = "CLOSING",
                rightHeaderBottom = "INVENTORY ($invTag)",
                rows = rows.map { row ->
                    SnapshotTableRow(
                        label = ReportExporter.displayProductNameForSection(row.sku.productName, sectionName),
                        value = OperationsCalculator.formatBags(row.closingBags)
                    )
                },
                totalLabel = "TOTAL $sectionName",
                totalValue = "${OperationsCalculator.formatBags(secBags)} bags (${OperationsCalculator.formatKg(secKg)} kg)"
            )
        }

        val totalBags = filteredSkus.sumOf { max(0.0, it.closingBags) }
        val totalKg = filteredSkus.sumOf { max(0.0, it.closingStockKg) }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val plantSlug = effectiveFilter.replace(" ", "_")
        val fileName = "Bulacan_Daily_Snapshot_${plantSlug}_${dayLog.date}_$stamp.jpg"

        return SnapshotPayload(
            title = "10K AGRI FEEDS - DAILY LOG SNAPSHOT",
            subtitle = "Date: ${dayLog.date}  •  Plant: $effectiveFilter",
            kpiSummary = "Closing Stock: ${OperationsCalculator.formatBags(totalBags)} bags (${OperationsCalculator.formatKg(totalKg)} kg)   |   Day Prod: ${OperationsCalculator.formatKg(dayLog.totalProducedDayKg)} kg   |   Day Disp: ${OperationsCalculator.formatKg(dayLog.totalDispatchedDayKg)} kg",
            tables = tables,
            fileName = fileName
        )
    }

    internal fun wrapTextLines(
        text: String,
        maxWidth: Float,
        measureText: (String) -> Float
    ): List<String> {
        if (text.isEmpty()) return listOf("")
        val safeMaxW = max(24f, maxWidth)
        val out = mutableListOf<String>()
        text.split('\n').forEach { rawParagraph ->
            val trimmed = rawParagraph.trim()
            if (trimmed.isEmpty()) {
                out.add("")
                return@forEach
            }
            val words = trimmed.split(Regex("\\s+"))
            var current = ""
            fun flushWordByChar(longWord: String) {
                var chunk = ""
                for (ch in longWord) {
                    val candidate = chunk + ch
                    if (chunk.isNotEmpty() && measureText(candidate) > safeMaxW) {
                        out.add(chunk)
                        chunk = ch.toString()
                    } else {
                        chunk = candidate
                    }
                }
                if (chunk.isNotEmpty()) {
                    current = chunk
                }
            }
            for (word in words) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (measureText(candidate) <= safeMaxW) {
                    current = candidate
                } else {
                    if (current.isNotEmpty()) {
                        out.add(current)
                        current = ""
                    }
                    if (measureText(word) <= safeMaxW) {
                        current = word
                    } else {
                        flushWordByChar(word)
                    }
                }
            }
            if (current.isNotEmpty()) {
                out.add(current)
            }
        }
        return out.ifEmpty { listOf("") }
    }

    fun renderSnapshotBitmap(
        payload: SnapshotPayload,
        noteText: String = ""
    ): Bitmap {
        val tables = payload.tables.ifEmpty {
            listOf(
                SnapshotSectionTable(
                    leftHeader = "INVENTORY SNAPSHOT",
                    rightHeaderTop = "QUANTITY",
                    rightHeaderBottom = "(BAGS)",
                    rows = listOf(SnapshotTableRow("No matching records", "0"))
                )
            )
        }

        val twoColumns = tables.size >= 2
        val outerMargin = 28f
        val columnGap = 24f
        val tableWidth = if (twoColumns) 740f else 840f
        val canvasWidth = if (twoColumns) {
            (outerMargin * 2 + tableWidth * 2 + columnGap).toInt()
        } else {
            (outerMargin * 2 + tableWidth).toInt()
        }
        val leftColFraction = if (twoColumns) 0.63f else 0.68f
        val tableHeaderHeight = 62f
        val singleLineRowHeight = 38f
        val rowLineHeight = 25f
        val rowVerticalPad = 18f
        val totalRowBaseHeight = 42f
        val totalLineHeight = 24f
        val tableSpacingY = 24f

        // Paints (created up front so exact metrics drive banner and row layout)
        val bannerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(242, 246, 241)
            style = Paint.Style.FILL
        }
        val bannerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(26, 66, 42)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(22, 60, 36)
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(55, 68, 60)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val kpiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(32, 78, 46)
            textSize = 16.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 76, 129)
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val greenHeaderPaint = Paint().apply {
            color = Color.rgb(42, 92, 22)
            style = Paint.Style.FILL
        }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val whiteRowPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val zebraRowPaint = Paint().apply {
            color = Color.rgb(244, 247, 245)
            style = Paint.Style.FILL
        }
        val totalRowPaint = Paint().apply {
            color = Color.rgb(228, 239, 225)
            style = Paint.Style.FILL
        }
        val gridPaint = Paint().apply {
            color = Color.rgb(74, 84, 78)
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
        }
        val headerDividerPaint = Paint().apply {
            color = Color.rgb(200, 220, 195)
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
        }
        val cellTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(17, 24, 20)
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val qtyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(17, 24, 20)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val totalTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(22, 60, 36)
            textSize = 18.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(98, 110, 102)
            textSize = 15f
        }

        // Compute top summary banner blocks cleanly from wrapped lines & font metrics
        val bannerPadX = 18f
        val bannerPadTop = 16f
        val bannerPadBottom = 16f
        val bannerMaxW = (canvasWidth - outerMargin * 2f) - bannerPadX * 2f
        data class BannerLine(val text: String, val baselineY: Float, val paint: Paint)

        val bannerLines = mutableListOf<BannerLine>()
        var cursorY = outerMargin + bannerPadTop
        var lastDescent = 0f

        fun appendBannerBlock(rawText: String, paint: Paint, lineSpacing: Float, blockGapTop: Float) {
            if (bannerLines.isNotEmpty()) {
                cursorY += blockGapTop
            }
            val fm = paint.fontMetrics
            val lines = wrapTextLines(rawText, bannerMaxW) { paint.measureText(it) }
            lines.forEachIndexed { idx, line ->
                if (idx == 0) {
                    cursorY += -fm.ascent
                } else {
                    cursorY += lineSpacing
                }
                bannerLines.add(BannerLine(line, cursorY, paint))
                lastDescent = fm.descent
            }
            cursorY += lastDescent
        }

        appendBannerBlock(payload.title, titlePaint, lineSpacing = 30f, blockGapTop = 0f)
        appendBannerBlock(payload.subtitle, subtitlePaint, lineSpacing = 22f, blockGapTop = 6f)
        if (payload.kpiSummary.isNotBlank()) {
            appendBannerBlock(payload.kpiSummary, kpiPaint, lineSpacing = 22f, blockGapTop = 6f)
        }
        if (noteText.isNotBlank()) {
            appendBannerBlock("Note: ${noteText.trim()}", notePaint, lineSpacing = 22f, blockGapTop = 6f)
        }

        val bannerBottom = cursorY + bannerPadBottom
        val topBannerY = bannerBottom + 20f

        val leftW = tableWidth * leftColFraction
        val rightW = tableWidth - leftW
        val leftCellPadX = 14f
        val rightCellPadX = 12f

        data class MeasuredRow(
            val leftLines: List<String>,
            val rightLines: List<String>,
            val height: Float
        )

        data class MeasuredTable(
            val source: SnapshotSectionTable,
            val leftHeaderLines: List<String>,
            val rightHeaderLines: List<String>,
            val headerHeight: Float,
            val rows: List<MeasuredRow>,
            val totalLeftLines: List<String>,
            val totalRightLines: List<String>,
            val totalHeight: Float,
            val totalTableHeight: Float
        )

        val measuredTables = tables.map { table ->
            val lHeaderLines = wrapTextLines(table.leftHeader, leftW - 24f) { headerTextPaint.measureText(it) }
            val rHeaderLines = listOf(table.rightHeaderTop, table.rightHeaderBottom)
                .filter { it.isNotBlank() }
                .flatMap { part -> wrapTextLines(part, rightW - 18f) { headerTextPaint.measureText(it) } }
            val hLineCount = max(lHeaderLines.size, rHeaderLines.size)
            val measuredHeaderH = max(tableHeaderHeight, 18f + hLineCount * 22f)

            val rawRows = table.rows.ifEmpty { listOf(SnapshotTableRow("No products", "0")) }
            val mRows = rawRows.map { r ->
                val lLines = wrapTextLines(r.label, leftW - leftCellPadX * 2f) { cellTextPaint.measureText(it) }
                val rLines = wrapTextLines(r.value, rightW - rightCellPadX * 2f) { qtyTextPaint.measureText(it) }
                val lineCount = max(lLines.size, rLines.size)
                val h = if (lineCount <= 1) singleLineRowHeight else rowVerticalPad + lineCount * rowLineHeight
                MeasuredRow(lLines, rLines, h)
            }
            val tLeftLines = table.totalLabel?.let {
                wrapTextLines(it, leftW - leftCellPadX * 2f) { s -> totalTextPaint.measureText(s) }
            }.orEmpty()
            val tRightLines = if (table.totalLabel != null) {
                wrapTextLines(table.totalValue.orEmpty(), rightW - rightCellPadX * 2f) { s -> totalTextPaint.measureText(s) }
            } else {
                emptyList()
            }
            val tLineCount = max(tLeftLines.size, tRightLines.size)
            val tHeight = when {
                table.totalLabel == null -> 0f
                tLineCount <= 1 -> totalRowBaseHeight
                else -> rowVerticalPad + tLineCount * totalLineHeight
            }
            val sumRowsH = mRows.sumOf { it.height.toDouble() }.toFloat()
            MeasuredTable(
                source = table,
                leftHeaderLines = lHeaderLines,
                rightHeaderLines = rHeaderLines,
                headerHeight = measuredHeaderH,
                rows = mRows,
                totalLeftLines = tLeftLines,
                totalRightLines = tRightLines,
                totalHeight = tHeight,
                totalTableHeight = measuredHeaderH + sumRowsH + tHeight
            )
        }

        // Distribute tables evenly across columns (1 or 2 columns)
        val colAssignments: List<Pair<Int, MeasuredTable>>
        val maxContentBottom: Float
        if (!twoColumns) {
            colAssignments = measuredTables.map { 0 to it }
            val h = measuredTables.sumOf { it.totalTableHeight.toDouble() }.toFloat() +
                max(0, measuredTables.size - 1) * tableSpacingY
            maxContentBottom = topBannerY + h
        } else {
            var col0Y = topBannerY
            var col1Y = topBannerY
            val list = mutableListOf<Pair<Int, MeasuredTable>>()
            measuredTables.forEach { mTable ->
                if (col0Y <= col1Y) {
                    list.add(0 to mTable)
                    col0Y += mTable.totalTableHeight + tableSpacingY
                } else {
                    list.add(1 to mTable)
                    col1Y += mTable.totalTableHeight + tableSpacingY
                }
            }
            colAssignments = list
            maxContentBottom = max(col0Y, col1Y) - tableSpacingY
        }

        val footerHeight = 40f
        val canvasHeight = (maxContentBottom + footerHeight + outerMargin).toInt().coerceAtLeast(320)

        val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // Draw top summary banner box and text strictly inside bounds
        val bannerRect = RectF(outerMargin, outerMargin, canvasWidth - outerMargin, bannerBottom)
        canvas.drawRoundRect(bannerRect, 10f, 10f, bannerBgPaint)
        canvas.drawRoundRect(bannerRect, 10f, 10f, bannerBorderPaint)

        bannerLines.forEach { line ->
            canvas.drawText(line.text, outerMargin + bannerPadX, line.baselineY, line.paint)
        }

        fun drawCenteredLinesInCell(
            lines: List<String>,
            cellTop: Float,
            cellHeight: Float,
            lineHeight: Float,
            paint: Paint,
            drawXForLine: (String) -> Float
        ) {
            if (lines.isEmpty()) return
            val fm = paint.fontMetrics
            val textHeight = fm.descent - fm.ascent
            val blockHeight = (lines.size - 1) * lineHeight + textHeight
            var baseline = cellTop + (cellHeight - blockHeight) / 2f - fm.ascent
            lines.forEach { line ->
                canvas.drawText(line, drawXForLine(line), baseline, paint)
                baseline += lineHeight
            }
        }

        // Draw each plant/section table
        val colCursors = floatArrayOf(topBannerY, topBannerY)
        colAssignments.forEach { (colIdx, mTable) ->
            val table = mTable.source
            val x = if (colIdx == 0) outerMargin else outerMargin + tableWidth + columnGap
            var y = colCursors[colIdx]
            val startY = y
            val splitX = x + leftW
            val endX = x + tableWidth

            // Green header cell
            canvas.drawRect(x, y, endX, y + mTable.headerHeight, greenHeaderPaint)
            canvas.drawLine(splitX, y, splitX, y + mTable.headerHeight, headerDividerPaint)

            drawCenteredLinesInCell(
                lines = mTable.leftHeaderLines,
                cellTop = y,
                cellHeight = mTable.headerHeight,
                lineHeight = 22f,
                paint = headerTextPaint
            ) { line ->
                x + (leftW - headerTextPaint.measureText(line)) / 2f
            }

            drawCenteredLinesInCell(
                lines = mTable.rightHeaderLines,
                cellTop = y,
                cellHeight = mTable.headerHeight,
                lineHeight = 22f,
                paint = headerTextPaint
            ) { line ->
                splitX + (rightW - headerTextPaint.measureText(line)) / 2f
            }

            y += mTable.headerHeight

            // Data rows
            mTable.rows.forEachIndexed { idx, row ->
                val bgPaint = if (idx % 2 == 0) whiteRowPaint else zebraRowPaint
                canvas.drawRect(x, y, endX, y + row.height, bgPaint)

                drawCenteredLinesInCell(
                    lines = row.leftLines,
                    cellTop = y,
                    cellHeight = row.height,
                    lineHeight = rowLineHeight,
                    paint = cellTextPaint
                ) { _ ->
                    x + leftCellPadX
                }

                drawCenteredLinesInCell(
                    lines = row.rightLines,
                    cellTop = y,
                    cellHeight = row.height,
                    lineHeight = rowLineHeight,
                    paint = qtyTextPaint
                ) { line ->
                    splitX + (rightW - qtyTextPaint.measureText(line)) / 2f
                }

                canvas.drawLine(x, y, endX, y, gridPaint)
                y += row.height
            }

            // Total footer row
            if (table.totalLabel != null) {
                canvas.drawRect(x, y, endX, y + mTable.totalHeight, totalRowPaint)

                drawCenteredLinesInCell(
                    lines = mTable.totalLeftLines,
                    cellTop = y,
                    cellHeight = mTable.totalHeight,
                    lineHeight = totalLineHeight,
                    paint = totalTextPaint
                ) { _ ->
                    x + leftCellPadX
                }

                drawCenteredLinesInCell(
                    lines = mTable.totalRightLines,
                    cellTop = y,
                    cellHeight = mTable.totalHeight,
                    lineHeight = totalLineHeight,
                    paint = totalTextPaint
                ) { line ->
                    splitX + (rightW - totalTextPaint.measureText(line)) / 2f
                }

                canvas.drawLine(x, y, endX, y, gridPaint)
                y += mTable.totalHeight
            }

            // Outer table border & column divider
            canvas.drawRect(x, startY, endX, y, gridPaint)
            canvas.drawLine(splitX, startY + mTable.headerHeight, splitX, y, gridPaint)

            colCursors[colIdx] = y + tableSpacingY
        }

        val generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        canvas.drawText(
            "Generated $generatedAt • 10K Agri Feeds FeedMill Ops",
            outerMargin,
            canvasHeight - 16f,
            footerPaint
        )

        return bitmap
    }

    fun compressBitmapToPngBytes(bitmap: Bitmap): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return out.toByteArray()
    }

    fun compressBitmapToJpegBytes(bitmap: Bitmap, quality: Int = 94): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return out.toByteArray()
    }

    fun generatePlantSnapshotPngBytes(
        report: PlantReportData,
        targetPlantSection: String? = null,
        noteText: String = ""
    ): Pair<String, ByteArray> {
        val payload = buildPlantSnapshotPayload(report, targetPlantSection)
        val bitmap = renderSnapshotBitmap(payload, noteText)
        val bytes = compressBitmapToJpegBytes(bitmap)
        bitmap.recycle()
        return payload.fileName to bytes
    }

    fun generateDailyLogSnapshotPngBytes(
        dayLog: DailyPlantLog,
        sectionFilter: String = "ALL",
        targetPlantSection: String? = null,
        noteText: String = ""
    ): Pair<String, ByteArray> {
        val payload = buildDailyLogSnapshotPayload(dayLog, sectionFilter, targetPlantSection)
        val bitmap = renderSnapshotBitmap(payload, noteText)
        val bytes = compressBitmapToJpegBytes(bitmap)
        bitmap.recycle()
        return payload.fileName to bytes
    }
}
