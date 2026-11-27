package com.example.domain

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.example.model.BreadcrumbActor
import com.example.model.BreadcrumbEntry
import com.example.model.BreadcrumbSeverity
import com.example.model.CustomerEntity
import com.example.model.CustomerOrder
import com.example.model.DailyPlantLog
import com.example.model.DispatchRecord
import com.example.model.FeedBomEntity
import com.example.model.PlantReportData
import com.example.model.PlantSection
import com.example.model.ProductionBatch
import com.example.model.RawMaterialEntity
import com.example.model.ReportSectionOption
import com.example.model.ReprocessItem
import com.example.model.ScheduleRun
import com.example.model.SkuEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportFileFormat(
    val label: String,
    val extension: String,
    val mimeType: String
) {
    CSV("CSV", "csv", "text/csv"),
    PDF("PDF", "pdf", "application/pdf"),
    XLS("XLS", "xls", "application/vnd.ms-excel")
}

data class PdfSaveResult(
    val uri: Uri?,
    val fileName: String,
    val bytes: ByteArray
)

object ReportExporter {

    private fun periodLabel(report: PlantReportData): String =
        if (report.isDailyReport) "Daily Date: ${report.startDate}" else "Period: ${report.startDate} to ${report.endDate}"

    private fun scopeLabel(report: PlantReportData): String {
        val all = ReportSectionOption.entries.toSet()
        return if (report.includedSections.containsAll(all)) {
            "All Sections"
        } else {
            ReportSectionOption.entries
                .filter { it in report.includedSections }
                .joinToString(", ") { it.label }
        }
    }

    private fun reportTitle(report: PlantReportData): String {
        val all = ReportSectionOption.entries.toSet()
        val prefix = if (report.isDailyReport) "DAILY" else "PERIOD"
        val scope = if (report.includedSections.containsAll(all)) {
            "PLANT SUMMARY REPORT"
        } else {
            report.includedSections
                .sortedBy { it.ordinal }
                .joinToString(" & ") { it.label.uppercase() } + " REPORT"
        }
        return if (report.isDailyReport) "$prefix $scope (${report.startDate})" else scope
    }

    private fun reportFileBase(report: PlantReportData): String {
        val all = ReportSectionOption.entries.toSet()
        val scopeSlug = when {
            report.includedSections.containsAll(all) -> "Summary"
            report.includedSections.size <= 2 -> ReportSectionOption.entries
                .filter { it in report.includedSections }
                .joinToString("_") { it.label }
            else -> "Custom_Summary"
        }
        val periodSlug = if (report.isDailyReport) "Daily_${report.startDate}" else "${report.startDate}_to_${report.endDate}"
        val secSlug = if (report.sectionFilter == "ALL") "" else "_${report.sectionFilter.replace(" ", "_")}"
        return "Bulacan_${scopeSlug}_${periodSlug}${secSlug}"
    }

    internal fun displayProductNameForSection(productName: String, sectionName: String): String {
        val suffix = " [$sectionName]"
        return if (productName.endsWith(suffix, ignoreCase = true)) {
            productName.dropLast(suffix.length).trimEnd()
        } else {
            productName
        }
    }

    internal fun orderedSectionsForNames(sectionNames: Collection<String>): List<String> {
        val canonical = PlantSection.entries.map { it.label }
        val inCanonical = canonical.filter { label -> sectionNames.any { it.equals(label, ignoreCase = true) } }
        val extras = sectionNames.filter { name -> canonical.none { it.equals(name, ignoreCase = true) } }.distinct()
        return inCanonical + extras
    }

    fun buildPlainTextReport(report: PlantReportData): String = buildString {
        val totalAvailKg = report.sectionSummaries.sumOf { it.totalAvailableStockKg }
        val totalAvailBags = report.sectionSummaries.sumOf { it.totalAvailableBags }
        val totalExpectedKg = report.sectionSummaries.sumOf { it.totalExpectedKg }
        val avgRecovery = if (totalExpectedKg > 0.0) (report.totalRecoveredKg / totalExpectedKg) * 100.0 else 100.0

        appendLine("10K AGRI FEEDS - BULACAN PLANT")
        appendLine(reportTitle(report))
        appendLine("${periodLabel(report)} | Location: ${report.sectionFilter} | Scope: ${scopeLabel(report)}")
        appendLine("=".repeat(64))
        if (report.includes(ReportSectionOption.INVENTORY)) {
            appendLine("Available Stock: ${OperationsCalculator.formatKg(totalAvailKg)} kg (${OperationsCalculator.formatBags(totalAvailBags)} bags) | SKUs: ${report.productRows.size} (${report.healthySkuCount} Healthy, ${report.watchSkuCount} Watch, ${report.criticalSkuCount} Critical)")
        }
        if (report.includes(ReportSectionOption.RECOVERY)) {
            appendLine("Produced: ${report.totalProducedBags} bags | ${OperationsCalculator.formatKg(report.totalRecoveredKg)} kg (${String.format("%.2f%%", avgRecovery)} avg recovery)")
        }
        if (report.includes(ReportSectionOption.DISPATCHES)) {
            appendLine("Dispatched: ${OperationsCalculator.formatKg(report.totalDispatchedKg)} kg net (${report.dispatches.size} trips: ${report.deliveredCount} Del, ${report.returnedCount} Ret, ${report.heldCount} Held)")
        }
        if (report.includes(ReportSectionOption.ORDERS)) {
            appendLine("Orders in Period: ${report.orders.size} | Active Open Orders: ${report.activeOpenOrders.size} (${OperationsCalculator.formatKg(report.totalOpenOrdersKg)} kg) | Backlog Shortfall: ${OperationsCalculator.formatKg(report.activeOrderShortfallKg)} kg")
        }
        if (report.includes(ReportSectionOption.SCHEDULE)) {
            appendLine("Schedule Runs in Period: ${report.schedules.size} (${report.scheduledActiveCount} active / ${report.scheduledQueueBags} bags, ${report.scheduledCompletedCount} completed)")
        }
        appendLine()

        if (report.includes(ReportSectionOption.INVENTORY) || report.includes(ReportSectionOption.RECOVERY)) {
            report.sectionSummaries.forEach { sec ->
                appendLine("[${sec.section.label}] (${sec.skuCount} SKUs)")
                if (report.includes(ReportSectionOption.INVENTORY)) {
                    appendLine("Available Stock: ${OperationsCalculator.formatBags(sec.totalAvailableBags)} bags | ${OperationsCalculator.formatKg(sec.totalAvailableStockKg)} kg")
                }
                if (report.includes(ReportSectionOption.RECOVERY)) {
                    appendLine("Produced: ${sec.totalBags} bags | ${OperationsCalculator.formatKg(sec.totalRecoveredKg)} kg | Recovery: ${String.format("%.2f%%", sec.recoveryPct)} | Variance: ${OperationsCalculator.formatKg(sec.totalVarianceKg)} kg")
                    appendLine("-".repeat(64))
                    if (sec.batches.isEmpty()) {
                        appendLine("  No production batches logged in selected period.")
                    } else {
                        sec.batches.forEach { b ->
                            val custNote = b.allScheduledForCustomers().takeIf { it.isNotEmpty() }?.joinToString(", ")?.let { " | For: $it" }.orEmpty()
                            appendLine("  ${b.date} | ${b.productName} | ${b.bagsProduced} bags x ${b.bagWeightKg}kg | Rec: ${OperationsCalculator.formatKg(b.actualRecoveredKg)} kg (${String.format("%.2f%%", b.recoveryPct)}) | ${b.remarks}$custNote")
                        }
                    }
                }
                appendLine()
            }
        }

        if (report.includes(ReportSectionOption.DISPATCHES)) {
            appendLine("DISPATCHES & FLEET TRIPS (${report.dispatches.size})")
            appendLine("-".repeat(64))
            if (report.dispatches.isEmpty()) {
                appendLine("  No dispatches logged in selected period.")
            } else {
                report.dispatches.forEach { d ->
                    val fleet = listOfNotNull(d.driverName.takeIf { it.isNotBlank() }, d.truckNumber.takeIf { it.isNotBlank() }).joinToString(" / ")
                    val fleetTag = if (fleet.isNotBlank()) " | $fleet" else ""
                    appendLine("  ${d.date} | DR ${d.drReference} | ${d.customer} | ${d.productName} | ${d.bagsDispatched}b (${OperationsCalculator.formatKg(d.effectiveDispatchedWeightKg())} kg net) | ${d.resolvedStatusLabel()}$fleetTag")
                }
            }
            appendLine()
        }

        if (report.includes(ReportSectionOption.SCHEDULE)) {
            appendLine("PRODUCTION SCHEDULE RUNS (${report.schedules.size})")
            appendLine("-".repeat(64))
            if (report.schedules.isEmpty()) {
                appendLine("  No scheduled production runs in selected period.")
            } else {
                report.schedules.forEach { s ->
                    val ord = s.allLinkedOrderCodes().takeIf { it.isNotEmpty() }?.joinToString(", ")?.let { " | Orders: $it" }.orEmpty()
                    appendLine("  ${s.scheduledDate} | [${s.section}] ${s.productName} | ${s.targetBags}b | ${s.priority} | ${s.status}$ord")
                }
            }
            appendLine()
        }

        if (report.includes(ReportSectionOption.ORDERS)) {
            appendLine("CUSTOMER ORDERS IN PERIOD (${report.orders.size})")
            appendLine("-".repeat(64))
            if (report.orders.isEmpty()) {
                appendLine("  No customer orders placed or due in selected period.")
            } else {
                report.orders.forEach { o ->
                    val prio = if (o.priority) " [PRIORITY]" else ""
                    appendLine("  ${o.orderCode} | ${o.customer}$prio | ${o.productName} | ${o.orderedBags}b (${OperationsCalculator.formatKg(o.totalWeightKg)} kg) | Due: ${o.dueDate} | ${o.status}")
                }
            }
            appendLine()
            appendLine("LIVE ACTIVE OPEN ORDERS (${report.activeOpenOrders.size})")
            appendLine("-".repeat(64))
            if (report.activeOpenOrders.isEmpty()) {
                appendLine("  No active open customer orders.")
            } else {
                report.activeOpenOrders.forEach { o ->
                    val prio = if (o.priority) " [PRIORITY]" else ""
                    val remKg = kotlin.math.max(0.0, o.totalWeightKg - o.dispatchedWeightKg)
                    appendLine("  ${o.orderCode} | ${o.customer}$prio | ${o.productName} | Rem: ${OperationsCalculator.formatKg(remKg)} kg / ${OperationsCalculator.formatKg(o.totalWeightKg)} kg | Due: ${o.dueDate} | ${o.status}")
                }
            }
            appendLine()
        }

        if (report.includes(ReportSectionOption.INVENTORY)) {
            appendLine("PRODUCT INVENTORY & BALANCE SUMMARY")
            appendLine("-".repeat(64))
            report.productRows.forEach { r ->
                appendLine("  [${r.section}] ${r.productName}: Avail=${OperationsCalculator.formatKg(r.availableStockKg)}kg, Prod=${r.producedBags}b (${OperationsCalculator.formatKg(r.recoveredKg)}kg), Disp=${OperationsCalculator.formatKg(r.dispatchedKg)}kg, OpenOrd=${OperationsCalculator.formatKg(r.openOrdersKg)}kg")
            }
        }
    }

    fun buildMessengerSummaryText(report: PlantReportData): String = buildString {
        val totalAvailKg = report.sectionSummaries.sumOf { it.totalAvailableStockKg }
        val totalAvailBags = report.sectionSummaries.sumOf { it.totalAvailableBags }
        val totalExpectedKg = report.sectionSummaries.sumOf { it.totalExpectedKg }
        val avgRecovery = if (totalExpectedKg > 0.0) (report.totalRecoveredKg / totalExpectedKg) * 100.0 else 100.0
        val periodText = if (report.isDailyReport) "Daily (${report.startDate})" else "${report.startDate} to ${report.endDate}"
        val invDayTag = if (report.isDailyReport && report.startDate == OperationsCalculator.todayIsoDate()) {
            "TODAY"
        } else if (report.isDailyReport) {
            report.startDate
        } else {
            "${report.startDate} to ${report.endDate}"
        }

        appendLine("*10K AGRI FEEDS - BULACAN PLANT*")
        appendLine("*${reportTitle(report)}*")
        appendLine("Period: $periodText | Plant: ${report.sectionFilter} | Scope: ${scopeLabel(report)}")
        appendLine("==============================")

        if (report.includes(ReportSectionOption.INVENTORY)) {
            appendLine("Total Available: ${OperationsCalculator.formatBags(totalAvailBags)} bags (${OperationsCalculator.formatKg(totalAvailKg)} kg)")
            appendLine("SKUs: ${report.productRows.size} (${report.healthySkuCount} Healthy | ${report.watchSkuCount} Watch | ${report.criticalSkuCount} Critical)")
        }
        if (report.includes(ReportSectionOption.RECOVERY)) {
            appendLine("Produced: ${report.totalProducedBags} bags (${OperationsCalculator.formatKg(report.totalRecoveredKg)} kg) | Avg Recovery: ${String.format(Locale.US, "%.2f%%", avgRecovery)}")
        }
        if (report.includes(ReportSectionOption.DISPATCHES)) {
            appendLine("Dispatched: ${OperationsCalculator.formatKg(report.totalDispatchedKg)} kg net (${report.dispatches.size} trips: ${report.deliveredCount} Del, ${report.returnedCount} Ret, ${report.heldCount} Held)")
        }
        if (report.includes(ReportSectionOption.ORDERS)) {
            appendLine("Period Orders: ${report.orders.size} | Live Open Orders: ${report.activeOpenOrders.size} (${OperationsCalculator.formatKg(report.totalOpenOrdersKg)} kg) | Backlog: ${OperationsCalculator.formatKg(report.activeOrderShortfallKg)} kg")
        }
        if (report.includes(ReportSectionOption.SCHEDULE)) {
            appendLine("Schedule Runs: ${report.schedules.size} (${report.scheduledActiveCount} active / ${report.scheduledQueueBags} bags, ${report.scheduledCompletedCount} completed)")
        }

        if (report.includes(ReportSectionOption.INVENTORY)) {
            val rowsBySection = report.productRows.groupBy { it.section }
            val sectionOrder = orderedSectionsForNames(rowsBySection.keys)
            if (sectionOrder.isEmpty()) {
                appendLine()
                appendLine("*INVENTORY ($invDayTag)*")
                appendLine("  - No products matched the selected location filter.")
            } else {
                sectionOrder.forEach { sectionName ->
                    val rows = rowsBySection[sectionName].orEmpty()
                    val secSummary = report.sectionSummaries.firstOrNull { it.section.label.equals(sectionName, ignoreCase = true) }
                    val secBags = secSummary?.totalAvailableBags ?: rows.sumOf { kotlin.math.max(0.0, it.availableBags) }
                    val secKg = secSummary?.totalAvailableStockKg ?: rows.sumOf { kotlin.math.max(0.0, it.availableStockKg) }

                    appendLine()
                    appendLine("*$sectionName | AVAILABLE INVENTORY ($invDayTag)*")
                    appendLine("Product — Qty (Bags)")
                    rows.forEach { r ->
                        val cleanName = displayProductNameForSection(r.productName, sectionName)
                        appendLine("$cleanName — ${OperationsCalculator.formatBags(r.availableBags)}")
                    }
                    appendLine("Total $sectionName: ${OperationsCalculator.formatBags(secBags)} bags (${OperationsCalculator.formatKg(secKg)} kg)")
                }
            }
        }

        if (report.includes(ReportSectionOption.RECOVERY)) {
            appendLine()
            appendLine("*PRODUCTION & RECOVERY SUMMARY*")
            report.sectionSummaries.forEach { sec ->
                appendLine()
                appendLine("*${sec.section.label}*: ${sec.totalBags} bags (${OperationsCalculator.formatKg(sec.totalRecoveredKg)} kg) | Exp ${OperationsCalculator.formatKg(sec.totalExpectedKg)} kg | Var ${OperationsCalculator.formatKg(sec.totalVarianceKg)} kg | Rec ${String.format(Locale.US, "%.2f%%", sec.recoveryPct)}")
                if (sec.batches.isEmpty()) {
                    appendLine("  - No production batches logged in selected period.")
                } else {
                    sec.batches.forEach { b ->
                        val cleanProd = displayProductNameForSection(b.productName, sec.section.label)
                        val custNote = b.allScheduledForCustomers().takeIf { it.isNotEmpty() }?.joinToString(", ")?.let { " | For: $it" }.orEmpty()
                        val remNote = b.remarks.takeIf { it.isNotBlank() }?.let { " | $it" }.orEmpty()
                        appendLine("  - ${b.date} | $cleanProd: ${b.bagsProduced}b (${OperationsCalculator.formatKg(b.actualRecoveredKg)} kg, ${String.format(Locale.US, "%.2f%%", b.recoveryPct)})$remNote$custNote")
                    }
                }
            }
        }

        if (report.includes(ReportSectionOption.DISPATCHES)) {
            appendLine()
            appendLine("*DISPATCHES & FLEET TRIPS (${report.dispatches.size})*")
            if (report.dispatches.isEmpty()) {
                appendLine("  - No dispatches logged in selected period.")
            } else {
                report.dispatches.forEach { d ->
                    val fleet = listOfNotNull(d.driverName.takeIf { it.isNotBlank() }, d.truckNumber.takeIf { it.isNotBlank() }).joinToString(" / ")
                    val fleetTag = if (fleet.isNotBlank()) " | $fleet" else ""
                    val retTag = if (d.returnedWeightKg > 0.0) " (Ret ${OperationsCalculator.formatKg(d.returnedWeightKg)}kg)" else ""
                    val remTag = d.remarks.takeIf { it.isNotBlank() }?.let { " | $it" }.orEmpty()
                    appendLine("  - ${d.date} | DR ${d.drReference} | ${d.customer} | ${d.productName}: ${d.bagsDispatched}b (${OperationsCalculator.formatKg(d.effectiveDispatchedWeightKg())} kg net)$retTag | ${d.resolvedStatusLabel()}$fleetTag$remTag")
                }
            }
        }

        if (report.includes(ReportSectionOption.SCHEDULE)) {
            appendLine()
            appendLine("*PRODUCTION SCHEDULE RUNS (${report.schedules.size})*")
            if (report.schedules.isEmpty()) {
                appendLine("  - No scheduled production runs in selected period.")
            } else {
                report.schedules.forEach { s ->
                    val cleanProd = displayProductNameForSection(s.productName, s.section)
                    val ord = s.allLinkedOrderCodes().takeIf { it.isNotEmpty() }?.joinToString(", ")?.let { " | Orders: $it" }.orEmpty()
                    val notes = s.lineRemarks.takeIf { it.isNotBlank() }?.let { " | $it" }.orEmpty()
                    appendLine("  - ${s.scheduledDate} | ${s.section} | $cleanProd: ${s.targetBags}b | ${s.priority} | ${s.status}$ord$notes")
                }
            }
        }

        if (report.includes(ReportSectionOption.ORDERS)) {
            appendLine()
            appendLine("*CUSTOMER ORDERS IN PERIOD (${report.orders.size})*")
            if (report.orders.isEmpty()) {
                appendLine("  - No customer orders placed or due in selected period.")
            } else {
                report.orders.forEach { o ->
                    val prio = if (o.priority) " [PRIORITY]" else ""
                    val remKg = kotlin.math.max(0.0, o.totalWeightKg - o.dispatchedWeightKg)
                    appendLine("  - ${o.orderCode}$prio | ${o.customer} | ${o.productName}: ${o.orderedBags}b (${OperationsCalculator.formatKg(o.totalWeightKg)} kg) | Disp ${OperationsCalculator.formatKg(o.dispatchedWeightKg)} kg | Rem ${OperationsCalculator.formatKg(remKg)} kg | Due ${o.dueDate} | ${o.status}")
                }
            }

            appendLine()
            appendLine("*LIVE ACTIVE OPEN ORDERS (${report.activeOpenOrders.size})*")
            if (report.activeOpenOrders.isEmpty()) {
                appendLine("  - No active open customer orders.")
            } else {
                report.activeOpenOrders.forEach { o ->
                    val prio = if (o.priority) " [PRIORITY]" else ""
                    val remKg = kotlin.math.max(0.0, o.totalWeightKg - o.dispatchedWeightKg)
                    appendLine("  - ${o.orderCode}$prio | ${o.customer} | ${o.productName}: ${o.orderedBags}b (${OperationsCalculator.formatKg(o.totalWeightKg)} kg) | Rem ${OperationsCalculator.formatKg(remKg)} kg | Due ${o.dueDate} | ${o.status}")
                }
            }
        }
    }.trimEnd()

    fun buildDailyLogMessengerText(dayLog: DailyPlantLog, sectionFilter: String = "ALL"): String = buildString {
        val filteredSkus = dayLog.skuRows.filter { sectionFilter == "ALL" || it.sku.section == sectionFilter }
        val invTag = if (dayLog.isToday) "TODAY" else dayLog.date

        appendLine("*10K AGRI FEEDS - BULACAN PLANT*")
        appendLine("*DAILY INVENTORY & ORDERS LOG (${dayLog.date})*")
        appendLine("Plant: $sectionFilter | SKUs: ${filteredSkus.size} (${dayLog.skusEditedCount} updated) | Orders: ${dayLog.orderRows.size}")
        appendLine("==============================")
        appendLine("Total Closing Stock: ${OperationsCalculator.formatBags(dayLog.totalClosingBags)} bags (${OperationsCalculator.formatKg(dayLog.totalClosingStockKg)} kg)")
        appendLine("Day Produced: ${OperationsCalculator.formatKg(dayLog.totalProducedDayKg)} kg | Day Dispatched: ${OperationsCalculator.formatKg(dayLog.totalDispatchedDayKg)} kg")
        appendLine("Open Orders Snapshot: ${OperationsCalculator.formatKg(dayLog.totalOpenOrdersSnapshotKg)} kg (${dayLog.ordersPlacedCount} placed, ${dayLog.ordersDueCount} due, ${dayLog.linkedDispatchOrdersCount} dispatched POs)")

        val rowsBySection = filteredSkus.groupBy { it.sku.section }
        val sectionOrder = orderedSectionsForNames(rowsBySection.keys)
        if (sectionOrder.isEmpty()) {
            appendLine()
            appendLine("*DAILY INVENTORY ($invTag)*")
            appendLine("  - No SKUs matched the selected location filter.")
        } else {
            sectionOrder.forEach { sectionName ->
                val rows = rowsBySection[sectionName].orEmpty()
                val secClosingBags = rows.sumOf { kotlin.math.max(0.0, it.closingBags) }
                val secClosingKg = rows.sumOf { kotlin.math.max(0.0, it.closingStockKg) }
                appendLine()
                appendLine("*$sectionName | CLOSING INVENTORY ($invTag)*")
                appendLine("Product — Qty (Bags)")
                rows.forEach { row ->
                    val cleanName = displayProductNameForSection(row.sku.productName, sectionName)
                    appendLine("$cleanName — ${OperationsCalculator.formatBags(row.closingBags)}")
                }
                appendLine("Total $sectionName: ${OperationsCalculator.formatBags(secClosingBags)} bags (${OperationsCalculator.formatKg(secClosingKg)} kg)")

                val activeRows = rows.filter { it.producedDayKg > 0.0 || it.dispatchedDayKg > 0.0 || it.returnedDayKg > 0.0 || it.editedOnDay }
                if (activeRows.isNotEmpty()) {
                    appendLine("Day Activity ($sectionName):")
                    activeRows.forEach { row ->
                        val cleanName = displayProductNameForSection(row.sku.productName, sectionName)
                        val editNote = if (row.editedOnDay) " | Edited ${row.editedTimeLabel}".trimEnd() else ""
                        appendLine("  - $cleanName: Prod ${row.producedDayBags}b (${OperationsCalculator.formatKg(row.producedDayKg)} kg) | Disp ${row.dispatchedDayBags}b (${OperationsCalculator.formatKg(row.dispatchedDayKg)} kg)$editNote")
                    }
                }
            }
        }

        appendLine()
        appendLine("*DAILY ORDERS LOG (${dayLog.orderRows.size})*")
        if (dayLog.orderRows.isEmpty()) {
            appendLine("  - No customer order activity recorded on ${dayLog.date}.")
        } else {
            dayLog.orderRows.forEach { row ->
                val o = row.order
                val prio = if (o.priority) " [PRIORITY]" else ""
                val tags = buildList {
                    if (row.placedOnDay) add("Placed")
                    if (row.dueOnDay) add("Due")
                    if (row.editedOnDay) add("Edited ${row.editedTimeLabel}".trim())
                    if (row.dispatchedOnDayKg > 0.0) add("Day Disp ${OperationsCalculator.formatKg(row.dispatchedOnDayKg)}kg")
                }.joinToString(", ")
                val tagSuffix = if (tags.isNotBlank()) " | Activity: $tags" else ""
                appendLine("  - ${o.orderCode}$prio | ${o.customer} | ${o.productName}: ${o.orderedBags}b (${OperationsCalculator.formatKg(o.totalWeightKg)} kg) | Disp Total ${OperationsCalculator.formatKg(o.dispatchedWeightKg)} kg | Day Disp ${OperationsCalculator.formatKg(row.dispatchedOnDayKg)} kg | Due ${o.dueDate} | ${o.status}$tagSuffix")
            }
        }
    }.trimEnd()

    fun buildCsvReport(report: PlantReportData): String = buildString {
        fun esc(s: String): String = "\"${s.replace("\"", "\"\"")}\""
        appendLine("Report Type,Included Sections,Period Start,Period End,Location Filter,Produced Bags,Recovered (kg),Dispatched Net (kg),Open Orders (kg),Order Backlog (kg),Healthy SKUs,Watch SKUs,Critical SKUs")
        appendLine("${esc(if (report.isDailyReport) "Daily Summary" else "Period Summary")},${esc(scopeLabel(report))},${esc(report.startDate)},${esc(report.endDate)},${esc(report.sectionFilter)},${report.totalProducedBags},${report.totalRecoveredKg},${report.totalDispatchedKg},${report.totalOpenOrdersKg},${report.activeOrderShortfallKg},${report.healthySkuCount},${report.watchSkuCount},${report.criticalSkuCount}")
        appendLine()

        if (report.includes(ReportSectionOption.RECOVERY)) {
            appendLine("Production Location,Date,Product Name,Bags Produced,Bag Weight (kg),Expected (kg),Recovered (kg),Variance (kg),Recovery %,Remarks,Scheduled Customers")
            report.sectionSummaries.forEach { sec ->
                sec.batches.forEach { b ->
                    val custs = b.allScheduledForCustomers().joinToString("; ")
                    appendLine("${esc(b.section)},${esc(b.date)},${esc(b.productName)},${b.bagsProduced},${b.bagWeightKg},${b.expectedWeightKg},${b.actualRecoveredKg},${b.varianceKg},${b.recoveryPct},${esc(b.remarks)},${esc(custs)}")
                }
            }
            appendLine()
        }

        if (report.includes(ReportSectionOption.DISPATCHES)) {
            appendLine("Dispatch Date,DR Reference,Customer,Product Name,Bags Dispatched,Net Weight (kg),Returned Weight (kg),Status,Driver,Truck,Remarks")
            report.dispatches.forEach { d ->
                appendLine("${esc(d.date)},${esc(d.drReference)},${esc(d.customer)},${esc(d.productName)},${d.bagsDispatched},${d.effectiveDispatchedWeightKg()},${d.returnedWeightKg},${esc(d.resolvedStatusLabel())},${esc(d.driverName)},${esc(d.truckNumber)},${esc(d.remarks)}")
            }
            appendLine()
        }

        if (report.includes(ReportSectionOption.SCHEDULE)) {
            appendLine("Schedule Date,Location,Product Name,Target Bags,Bag Weight (kg),Priority,Status,Line Remarks,Linked Orders")
            report.schedules.forEach { s ->
                appendLine("${esc(s.scheduledDate)},${esc(s.section)},${esc(s.productName)},${s.targetBags},${s.bagWeightKg},${esc(s.priority)},${esc(s.status)},${esc(s.lineRemarks)},${esc(s.allLinkedOrderCodes().joinToString("; "))}")
            }
            appendLine()
        }

        if (report.includes(ReportSectionOption.ORDERS)) {
            appendLine("Order Scope,Order Code,Order Date,Due Date,Customer,Product Name,Ordered Bags,Total Weight (kg),Dispatched (kg),Remaining (kg),Status,Priority")
            report.orders.forEach { o ->
                val remKg = kotlin.math.max(0.0, o.totalWeightKg - o.dispatchedWeightKg)
                appendLine("${esc("Period")},${esc(o.orderCode)},${esc(o.orderDate)},${esc(o.dueDate)},${esc(o.customer)},${esc(o.productName)},${o.orderedBags},${o.totalWeightKg},${o.dispatchedWeightKg},$remKg,${esc(o.status)},${o.priority}")
            }
            report.activeOpenOrders.forEach { o ->
                val remKg = kotlin.math.max(0.0, o.totalWeightKg - o.dispatchedWeightKg)
                appendLine("${esc("Active Open")},${esc(o.orderCode)},${esc(o.orderDate)},${esc(o.dueDate)},${esc(o.customer)},${esc(o.productName)},${o.orderedBags},${o.totalWeightKg},${o.dispatchedWeightKg},$remKg,${esc(o.status)},${o.priority}")
            }
            appendLine()
        }

        if (report.includes(ReportSectionOption.INVENTORY)) {
            appendLine("Product Summary,Location,Produced Bags,Recovered (kg),Variance (kg),Dispatched (kg),Open Orders (kg),Available Stock (kg)")
            report.productRows.forEach { r ->
                appendLine("${esc(r.productName)},${esc(r.section)},${r.producedBags},${r.recoveredKg},${r.varianceKg},${r.dispatchedKg},${r.openOrdersKg},${r.availableStockKg}")
            }
        }
    }

    fun buildJsonBackup(
        skus: List<SkuEntity>,
        batches: List<ProductionBatch>,
        dispatches: List<DispatchRecord>,
        orders: List<CustomerOrder>,
        schedules: List<ScheduleRun>,
        customers: List<CustomerEntity> = emptyList(),
        rawMaterials: List<RawMaterialEntity> = emptyList(),
        boms: List<FeedBomEntity> = emptyList(),
        reprocessItems: List<ReprocessItem> = emptyList()
    ): String {
        val root = JSONObject()
        root.put("plant", "10K Agri Feeds - Bulacan Plant")
        root.put("version", 3)

        root.put("skus", JSONArray().apply {
            skus.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id)
                    put("productName", s.productName)
                    put("section", s.section)
                    put("openingStockKg", s.openingStockKg)
                    put("standardBagWeightKg", s.standardBagWeightKg)
                    put("targetRecoveryPct", s.targetRecoveryPct)
                    put("safetyBufferPct", s.safetyBufferPct)
                    put("defaultRemarks", s.defaultRemarks)
                })
            }
        })

        root.put("productionBatches", JSONArray().apply {
            batches.forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id)
                    put("date", b.date)
                    put("section", b.section)
                    put("productName", b.productName)
                    put("bagsProduced", b.bagsProduced)
                    put("bagWeightKg", b.bagWeightKg)
                    put("expectedWeightKg", b.expectedWeightKg)
                    put("actualRecoveredKg", b.actualRecoveredKg)
                    put("varianceKg", b.varianceKg)
                    put("recoveryPct", b.recoveryPct)
                    put("remarks", b.remarks)
                    put("scheduleRunId", b.scheduleRunId)
                    put("linkedOrderIds", JSONArray(b.allLinkedOrderIds()))
                    put("linkedOrderCodes", JSONArray(b.allLinkedOrderCodes()))
                    put("scheduledForCustomers", JSONArray(b.allScheduledForCustomers()))
                })
            }
        })

        root.put("dispatches", JSONArray().apply {
            dispatches.forEach { d ->
                put(JSONObject().apply {
                    put("id", d.id)
                    put("date", d.date)
                    put("productName", d.productName)
                    put("bagsDispatched", d.bagsDispatched)
                    put("bagWeightKg", d.bagWeightKg)
                    put("totalWeightKg", d.totalWeightKg)
                    put("drReference", d.drReference)
                    put("customer", d.customer)
                    put("linkedOrderId", d.linkedOrderId)
                    put("sourcePlant", d.sourcePlant)
                    put("driverName", d.driverName)
                    put("truckNumber", d.truckNumber)
                    put("helperName", d.helperName)
                    put("remarks", d.remarks)
                    put("dispatchStatus", d.resolvedStatusLabel())
                    put("hasReturn", d.hasReturn)
                    put("returnConfirmed", d.returnConfirmed)
                    put("returnedBags", d.returnedBags)
                    put("returnedWeightKg", d.returnedWeightKg)
                    put("returnReason", d.returnReason)
                    put("isHeld", d.isHeld)
                    put("heldRemarks", d.heldRemarks)
                })
            }
        })

        root.put("orders", JSONArray().apply {
            orders.forEach { o ->
                put(JSONObject().apply {
                    put("id", o.id)
                    put("orderCode", o.orderCode)
                    put("orderDate", o.orderDate)
                    put("dueDate", o.dueDate)
                    put("productName", o.productName)
                    put("orderedBags", o.orderedBags)
                    put("bagWeightKg", o.bagWeightKg)
                    put("totalWeightKg", o.totalWeightKg)
                    put("dispatchedWeightKg", o.dispatchedWeightKg)
                    put("customer", o.customer)
                    put("status", o.status)
                    put("priority", o.priority)
                    put("rushOrder", o.rushOrder)
                })
            }
        })

        root.put("schedules", JSONArray().apply {
            schedules.forEach { sc ->
                put(JSONObject().apply {
                    put("id", sc.id)
                    put("scheduledDate", sc.scheduledDate)
                    put("section", sc.section)
                    put("productName", sc.productName)
                    put("targetBags", sc.targetBags)
                    put("bagWeightKg", sc.bagWeightKg)
                    put("expectedWeightKg", sc.expectedWeightKg)
                    put("lineRemarks", sc.lineRemarks)
                    put("priority", sc.priority)
                    put("status", sc.status)
                    put("linkedOrderId", sc.linkedOrderId)
                    put("linkedOrderCode", sc.linkedOrderCode)
                    put("linkedOrderIds", JSONArray(sc.allLinkedOrderIds()))
                    put("linkedOrderCodes", JSONArray(sc.allLinkedOrderCodes()))
                })
            }
        })

        root.put("customers", JSONArray().apply {
            customers.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("notes", c.notes)
                    put("priority", c.priority)
                })
            }
        })

        root.put("rawMaterials", JSONArray().apply {
            rawMaterials.forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id)
                    put("name", m.name)
                    put("category", m.category)
                    put("currentStockKg", m.currentStockKg)
                    put("reorderThresholdKg", m.reorderThresholdKg)
                    put("usageSharePct", m.usageSharePct)
                    put("leadTimeDays", m.leadTimeDays)
                    put("safetyDays", m.safetyDays)
                    put("supplierName", m.supplierName)
                    put("alertsEnabled", m.alertsEnabled)
                    put("lastRestockedDate", m.lastRestockedDate)
                })
            }
        })

        root.put("boms", JSONArray().apply {
            boms.forEach { bom ->
                put(JSONObject().apply {
                    put("id", bom.id)
                    put("productName", bom.productName)
                    put("section", bom.section)
                    put("batchBaseKg", bom.batchBaseKg)
                    put("yieldLossPct", bom.yieldLossPct)
                    put("notes", bom.notes)
                    put("lines", JSONArray().apply {
                        bom.lines.forEach { line ->
                            put(JSONObject().apply {
                                put("materialId", line.materialId)
                                put("materialName", line.materialName)
                                put("category", line.category)
                                put("inclusionPct", line.inclusionPct)
                                put("notes", line.notes)
                            })
                        }
                    })
                })
            }
        })

        root.put("reprocessItems", JSONArray().apply {
            reprocessItems.forEach { rep ->
                put(JSONObject().apply {
                    put("id", rep.id)
                    put("sourceDispatchId", rep.sourceDispatchId)
                    put("drReference", rep.drReference)
                    put("customer", rep.customer)
                    put("productName", rep.productName)
                    put("section", rep.section)
                    put("returnedDate", rep.returnedDate)
                    put("returnedBags", rep.returnedBags)
                    put("bagWeightKg", rep.bagWeightKg)
                    put("returnedWeightKg", rep.returnedWeightKg)
                    put("reprocessedBags", rep.reprocessedBags)
                    put("reprocessedWeightKg", rep.reprocessedWeightKg)
                    put("returnReason", rep.returnReason)
                    put("status", rep.status)
                    put("fastActionNeeded", rep.fastActionNeeded)
                    put("flaggedToInventory", rep.flaggedToInventory)
                    put("reprocessedDate", rep.reprocessedDate)
                    put("actionNotes", rep.actionNotes)
                })
            }
        })

        return root.toString(2)
    }

    private fun wrapPdfLines(text: String, maxWidth: Float, paint: Paint): List<String> =
        SnapshotBitmapRenderer.wrapTextLines(text, maxWidth) { paint.measureText(it) }

    fun generateReportPdfBytes(report: PlantReportData): ByteArray {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val contentWidth = pageWidth - margin * 2

        val pdf = PdfDocument()
        var pageNum = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas
        var y = margin

        val titlePaint = Paint().apply {
            color = Color.rgb(26, 66, 42)
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(67, 81, 72)
            textSize = 9.5f
            isAntiAlias = true
        }
        val sectionHeaderPaint = Paint().apply {
            color = Color.rgb(26, 66, 42)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(17, 28, 22)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(17, 28, 22)
            textSize = 8.5f
            isAntiAlias = true
        }
        val mutedPaint = Paint().apply {
            color = Color.rgb(90, 105, 96)
            textSize = 8f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 206)
            strokeWidth = 0.8f
        }
        val fillBoxPaint = Paint().apply {
            color = Color.rgb(240, 244, 241)
            style = Paint.Style.FILL
        }

        fun drawFooter(pNum: Int) {
            canvas.drawLine(margin, pageHeight - 28f, pageWidth - margin, pageHeight - 28f, linePaint)
            canvas.drawText(
                "10K Agri Feeds – Bulacan Plant | FeedMill Ops Report",
                margin,
                pageHeight - 16f,
                mutedPaint
            )
            val pageStr = "Page $pNum"
            val w = mutedPaint.measureText(pageStr)
            canvas.drawText(pageStr, pageWidth - margin - w, pageHeight - 16f, mutedPaint)
        }

        fun ensureSpace(neededHeight: Float, repeatHeader: (() -> Unit)? = null) {
            if (y + neededHeight > pageHeight - 36f) {
                drawFooter(pageNum)
                pdf.finishPage(page)
                pageNum++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = page.canvas
                y = margin
                repeatHeader?.invoke()
            }
        }

        val generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val periodBanner = if (report.isDailyReport) {
            "Daily Report  |  Date: ${report.startDate}  |  Location: ${report.sectionFilter}  |  Scope: ${scopeLabel(report)}"
        } else {
            "Period Report  |  ${report.startDate} to ${report.endDate}  |  Location: ${report.sectionFilter}  |  Scope: ${scopeLabel(report)}"
        }
        canvas.drawText("10K AGRI FEEDS – BULACAN PLANT", margin, y + 14f, titlePaint)
        y += 20f
        canvas.drawText(periodBanner, margin, y + 9f, subtitlePaint)
        y += 14f
        canvas.drawText("Generated: $generatedAt", margin, y + 8f, mutedPaint)
        y += 14f

        // Dynamic Overall KPI box based on selected sections
        val totalAvailKg = report.sectionSummaries.sumOf { it.totalAvailableStockKg }
        val totalAvailBags = report.sectionSummaries.sumOf { it.totalAvailableBags }
        val totalExpectedKg = report.sectionSummaries.sumOf { it.totalExpectedKg }
        val avgRecovery = if (totalExpectedKg > 0.0) (report.totalRecoveredKg / totalExpectedKg) * 100.0 else 100.0
        val rawKpiBlocks = buildList {
            if (report.includes(ReportSectionOption.INVENTORY)) {
                add("Available Stock: ${OperationsCalculator.formatKg(totalAvailKg)} kg (${OperationsCalculator.formatBags(totalAvailBags)} bags)   |   SKUs: ${report.productRows.size} (${report.healthySkuCount} Healthy / ${report.watchSkuCount} Watch / ${report.criticalSkuCount} Critical)")
            }
            if (report.includes(ReportSectionOption.RECOVERY)) {
                add("Produced: ${report.totalProducedBags} bags (${OperationsCalculator.formatKg(report.totalRecoveredKg)} kg)   |   Avg Recovery: ${String.format("%.2f%%", avgRecovery)}")
            }
            if (report.includes(ReportSectionOption.DISPATCHES)) {
                add("Dispatched: ${OperationsCalculator.formatKg(report.totalDispatchedKg)} kg net   |   Trips: ${report.dispatches.size} (${report.deliveredCount} Delivered, ${report.returnedCount} Returned, ${report.heldCount} Held)")
            }
            if (report.includes(ReportSectionOption.ORDERS)) {
                add("Period Orders: ${report.orders.size}   |   Live Open Orders: ${report.activeOpenOrders.size} (${OperationsCalculator.formatKg(report.totalOpenOrdersKg)} kg, Backlog: ${OperationsCalculator.formatKg(report.activeOrderShortfallKg)} kg)")
            }
            if (report.includes(ReportSectionOption.SCHEDULE)) {
                add("Schedule Runs: ${report.schedules.size} (${report.scheduledActiveCount} active / ${report.scheduledQueueBags} bags, ${report.scheduledCompletedCount} completed)")
            }
        }
        val wrappedKpiEntries = rawKpiBlocks.flatMapIndexed { blockIdx, text ->
            val p = if (blockIdx == 0) tableHeaderPaint else bodyPaint
            wrapPdfLines(text, contentWidth - 20f, p).map { line -> line to p }
        }
        val boxLineHeight = 13f
        val boxHeight = 14f + wrappedKpiEntries.size * boxLineHeight + 4f
        canvas.drawRoundRect(margin, y, margin + contentWidth, y + boxHeight, 6f, 6f, fillBoxPaint)
        wrappedKpiEntries.forEachIndexed { idx, (line, p) ->
            canvas.drawText(
                line,
                margin + 10f,
                y + 15f + idx * boxLineHeight,
                p
            )
        }
        y += boxHeight + 12f

        // Location Inventory & Production Summary
        if (report.includes(ReportSectionOption.INVENTORY) || report.includes(ReportSectionOption.RECOVERY)) {
            val secHeading = when {
                report.includes(ReportSectionOption.INVENTORY) && report.includes(ReportSectionOption.RECOVERY) ->
                    "LOCATION INVENTORY & PRODUCTION SUMMARY"
                report.includes(ReportSectionOption.INVENTORY) ->
                    "LOCATION INVENTORY SUMMARY"
                else ->
                    "LOCATION PRODUCTION & RECOVERY SUMMARY"
            }
            ensureSpace(26f)
            canvas.drawText(secHeading, margin, y + 10f, sectionHeaderPaint)
            y += 16f
            report.sectionSummaries.forEach { sec ->
                ensureSpace(34f)
                canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                y += 12f
                canvas.drawText(
                    "${sec.section.label} (${sec.skuCount} SKUs)",
                    margin,
                    y,
                    tableHeaderPaint
                )
                if (report.includes(ReportSectionOption.INVENTORY)) {
                    val rightText = "Avail: ${OperationsCalculator.formatBags(sec.totalAvailableBags)} bags (${OperationsCalculator.formatKg(sec.totalAvailableStockKg)} kg)"
                    canvas.drawText(rightText, pageWidth - margin - tableHeaderPaint.measureText(rightText), y, tableHeaderPaint)
                }
                y += 11f
                if (report.includes(ReportSectionOption.RECOVERY)) {
                    canvas.drawText(
                        "Produced: ${sec.totalBags} bags (${OperationsCalculator.formatKg(sec.totalRecoveredKg)} kg)  •  Variance: ${OperationsCalculator.formatKg(sec.totalVarianceKg)} kg  •  Recovery: ${String.format("%.2f%%", sec.recoveryPct)}",
                        margin + 6f,
                        y,
                        mutedPaint
                    )
                    y += 8f

                    if (sec.batches.isEmpty()) {
                        ensureSpace(12f)
                        canvas.drawText("  No production batches logged in selected period.", margin + 6f, y + 9f, mutedPaint)
                        y += 12f
                    } else {
                        sec.batches.forEach { b ->
                            val custNote = b.allScheduledForCustomers().takeIf { it.isNotEmpty() }?.joinToString(", ")?.let { " • For: $it" }.orEmpty()
                            val rawLine = "  ${b.date} | ${b.productName} | ${b.bagsProduced}b x ${b.bagWeightKg}kg = ${OperationsCalculator.formatKg(b.actualRecoveredKg)} kg (${b.remarks})$custNote"
                            val wrapped = wrapPdfLines(rawLine, contentWidth - 12f, bodyPaint)
                            ensureSpace(wrapped.size * 11f + 3f)
                            wrapped.forEach { wl ->
                                canvas.drawText(wl, margin + 6f, y + 9f, bodyPaint)
                                y += 11f
                            }
                            y += 2f
                        }
                    }
                }
            }
            y += 10f
        }

        // Dispatches & Fleet Trips
        if (report.includes(ReportSectionOption.DISPATCHES)) {
            ensureSpace(28f)
            canvas.drawText("DISPATCHES & FLEET TRIPS (${report.dispatches.size})", margin, y + 10f, sectionHeaderPaint)
            y += 16f
            if (report.dispatches.isEmpty()) {
                canvas.drawText("No dispatches logged in selected period.", margin + 6f, y + 9f, mutedPaint)
                y += 14f
            } else {
                report.dispatches.forEach { d ->
                    val rawLine = "${d.date} | DR ${d.drReference} | ${d.customer} | ${d.productName} | ${d.bagsDispatched}b (${OperationsCalculator.formatKg(d.effectiveDispatchedWeightKg())} kg) | ${d.resolvedStatusLabel()}"
                    val wrapped = wrapPdfLines(rawLine, contentWidth - 8f, bodyPaint)
                    ensureSpace(wrapped.size * 11f + 3f)
                    wrapped.forEach { wl ->
                        canvas.drawText(wl, margin + 4f, y + 9f, bodyPaint)
                        y += 11f
                    }
                    y += 2f
                }
            }
            y += 10f
        }

        // Schedule Runs
        if (report.includes(ReportSectionOption.SCHEDULE)) {
            ensureSpace(28f)
            canvas.drawText("PRODUCTION SCHEDULE RUNS (${report.schedules.size})", margin, y + 10f, sectionHeaderPaint)
            y += 16f
            if (report.schedules.isEmpty()) {
                canvas.drawText("No scheduled production runs recorded for selected period.", margin + 6f, y + 9f, mutedPaint)
                y += 14f
            } else {
                report.schedules.forEach { s ->
                    val ord = s.allLinkedOrderCodes().takeIf { it.isNotEmpty() }?.joinToString(", ")?.let { " | $it" }.orEmpty()
                    val rawLine = "${s.scheduledDate} | ${s.section} | ${s.productName} | ${s.targetBags}b | ${s.priority} (${s.status})$ord"
                    val wrapped = wrapPdfLines(rawLine, contentWidth - 8f, bodyPaint)
                    ensureSpace(wrapped.size * 11f + 3f)
                    wrapped.forEach { wl ->
                        canvas.drawText(wl, margin + 4f, y + 9f, bodyPaint)
                        y += 11f
                    }
                    y += 2f
                }
            }
            y += 10f
        }

        // Customer Orders (Period + Live Active Open Orders)
        if (report.includes(ReportSectionOption.ORDERS)) {
            ensureSpace(28f)
            canvas.drawText("CUSTOMER ORDERS IN PERIOD (${report.orders.size})", margin, y + 10f, sectionHeaderPaint)
            y += 16f
            if (report.orders.isEmpty()) {
                canvas.drawText("No customer orders placed or due in selected period.", margin + 6f, y + 9f, mutedPaint)
                y += 14f
            } else {
                report.orders.forEach { o ->
                    val prio = if (o.priority) " ★" else ""
                    val rawLine = "${o.orderCode} | ${o.customer}$prio | ${o.productName} | ${o.orderedBags}b (${OperationsCalculator.formatKg(o.totalWeightKg)} kg) | Due ${o.dueDate} (${o.status})"
                    val wrapped = wrapPdfLines(rawLine, contentWidth - 8f, bodyPaint)
                    ensureSpace(wrapped.size * 11f + 3f)
                    wrapped.forEach { wl ->
                        canvas.drawText(wl, margin + 4f, y + 9f, bodyPaint)
                        y += 11f
                    }
                    y += 2f
                }
            }
            y += 6f
            ensureSpace(24f)
            canvas.drawText("LIVE ACTIVE OPEN ORDERS (${report.activeOpenOrders.size})", margin, y + 10f, sectionHeaderPaint)
            y += 16f
            if (report.activeOpenOrders.isEmpty()) {
                canvas.drawText("No active open orders remaining.", margin + 6f, y + 9f, mutedPaint)
                y += 14f
            } else {
                report.activeOpenOrders.forEach { o ->
                    val prio = if (o.priority) " ★" else ""
                    val remKg = kotlin.math.max(0.0, o.totalWeightKg - o.dispatchedWeightKg)
                    val rawLine = "${o.orderCode} | ${o.customer}$prio | ${o.productName} | Ordered ${o.orderedBags}b (${OperationsCalculator.formatKg(o.totalWeightKg)} kg) | Rem ${OperationsCalculator.formatKg(remKg)} kg | Due ${o.dueDate} (${o.status})"
                    val wrapped = wrapPdfLines(rawLine, contentWidth - 8f, bodyPaint)
                    ensureSpace(wrapped.size * 11f + 3f)
                    wrapped.forEach { wl ->
                        canvas.drawText(wl, margin + 4f, y + 9f, bodyPaint)
                        y += 11f
                    }
                    y += 2f
                }
            }
            y += 10f
        }

        if (report.includes(ReportSectionOption.INVENTORY)) {
            val invTag = if (report.isDailyReport && report.startDate == OperationsCalculator.todayIsoDate()) {
                "TODAY"
            } else if (report.isDailyReport) {
                report.startDate
            } else {
                "${report.startDate} to ${report.endDate}"
            }
            val greenHeaderPaint = Paint().apply {
                color = Color.rgb(26, 66, 42)
                style = Paint.Style.FILL
            }
            val whiteHeaderTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            fun drawPlantInventoryHeader(plantName: String) {
                ensureSpace(34f)
                canvas.drawRect(margin, y, margin + contentWidth, y + 18f, greenHeaderPaint)
                canvas.drawText(plantName, margin + 6f, y + 12f, whiteHeaderTextPaint)
                canvas.drawText("AVAILABLE INVENTORY ($invTag)", margin + 235f, y + 12f, whiteHeaderTextPaint)
                canvas.drawText("Avail (kg)", margin + 410f, y + 12f, whiteHeaderTextPaint)
                canvas.drawText("Prod (kg)", margin + 468f, y + 12f, whiteHeaderTextPaint)
                y += 22f
            }

            val rowsBySection = report.productRows.groupBy { it.section }
            val sectionOrder = orderedSectionsForNames(rowsBySection.keys)
            ensureSpace(22f)
            canvas.drawText("INVENTORY PER PLANT (${report.productRows.size} SKUs)", margin, y + 10f, sectionHeaderPaint)
            y += 16f

            sectionOrder.forEach { sectionName ->
                val rows = rowsBySection[sectionName].orEmpty()
                drawPlantInventoryHeader(sectionName)
                rows.forEach { r ->
                    ensureSpace(15f) { drawPlantInventoryHeader(sectionName) }
                    val cleanName = displayProductNameForSection(r.productName, sectionName)
                    val nameTrim = if (cleanName.length > 40) cleanName.take(37) + "..." else cleanName
                    canvas.drawText(nameTrim, margin + 6f, y + 9f, bodyPaint)
                    canvas.drawText(OperationsCalculator.formatBags(r.availableBags), margin + 235f, y + 9f, tableHeaderPaint)
                    canvas.drawText(OperationsCalculator.formatKg(r.availableStockKg), margin + 410f, y + 9f, bodyPaint)
                    canvas.drawText(OperationsCalculator.formatKg(r.recoveredKg), margin + 468f, y + 9f, mutedPaint)
                    y += 13f
                    canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                    y += 2f
                }
                y += 8f
            }
        }

        drawFooter(pageNum)
        pdf.finishPage(page)

        val out = ByteArrayOutputStream()
        pdf.writeTo(out)
        pdf.close()
        return out.toByteArray()
    }

    fun savePdfToDownloads(context: Context, report: PlantReportData): PdfSaveResult {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${reportFileBase(report)}_$stamp.pdf"
        val pdfBytes = generateReportPdfBytes(report)
        return saveBytesToDownloads(context, fileName, ExportFileFormat.PDF.mimeType, pdfBytes)
    }

    fun saveCsvToDownloads(context: Context, report: PlantReportData): PdfSaveResult {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${reportFileBase(report)}_$stamp.csv"
        val csvBytes = buildCsvReport(report).toByteArray(Charsets.UTF_8)
        return saveBytesToDownloads(context, fileName, ExportFileFormat.CSV.mimeType, csvBytes)
    }

    fun saveXlsToDownloads(context: Context, report: PlantReportData): PdfSaveResult {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${reportFileBase(report)}_$stamp.xls"
        val xlsBytes = buildXlsSpreadsheetXml(report).toByteArray(Charsets.UTF_8)
        return saveBytesToDownloads(context, fileName, ExportFileFormat.XLS.mimeType, xlsBytes)
    }

    fun saveReportToDownloads(
        context: Context,
        report: PlantReportData,
        format: ExportFileFormat
    ): PdfSaveResult = when (format) {
        ExportFileFormat.CSV -> saveCsvToDownloads(context, report)
        ExportFileFormat.PDF -> savePdfToDownloads(context, report)
        ExportFileFormat.XLS -> saveXlsToDownloads(context, report)
    }

    fun reportExportFileName(report: PlantReportData, format: ExportFileFormat): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "${reportFileBase(report)}_$stamp.${format.extension}"
    }

    fun generateReportBytes(report: PlantReportData, format: ExportFileFormat): Pair<String, ByteArray> {
        val fileName = reportExportFileName(report, format)
        val bytes = when (format) {
            ExportFileFormat.CSV -> buildCsvReport(report).toByteArray(Charsets.UTF_8)
            ExportFileFormat.PDF -> generateReportPdfBytes(report)
            ExportFileFormat.XLS -> buildXlsSpreadsheetXml(report).toByteArray(Charsets.UTF_8)
        }
        return fileName to bytes
    }

    fun generateDailyLogBytes(
        dayLog: DailyPlantLog,
        sectionFilter: String = "ALL",
        format: ExportFileFormat
    ): Pair<String, ByteArray> {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val secSlug = if (sectionFilter == "ALL") "" else "_${sectionFilter.replace(" ", "_")}"
        val fileName = "Bulacan_Daily_Log_${dayLog.date}${secSlug}_$stamp.${format.extension}"
        val bytes = when (format) {
            ExportFileFormat.CSV -> buildDailyLogCsv(dayLog, sectionFilter).toByteArray(Charsets.UTF_8)
            ExportFileFormat.PDF -> generateDailyLogPdfBytes(dayLog, sectionFilter)
            ExportFileFormat.XLS -> buildDailyLogXlsSpreadsheetXml(dayLog, sectionFilter).toByteArray(Charsets.UTF_8)
        }
        return fileName to bytes
    }

    fun createShareableReportUri(context: Context, fileName: String, bytes: ByteArray): Uri {
        val dir = File(context.cacheDir, "shared_reports").apply { mkdirs() }
        // Keep cache tidy by removing files older than 2 hours
        val cutoff = System.currentTimeMillis() - 2 * 60 * 60 * 1000L
        dir.listFiles()?.forEach { old ->
            if (old.lastModified() < cutoff) {
                old.delete()
            }
        }
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val outFile = File(dir, safeName)
        outFile.writeBytes(bytes)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    }

    fun createShareableSnapshotImageUri(
        context: Context,
        fileName: String,
        jpegBytes: ByteArray,
        width: Int,
        height: Int
    ): Uri {
        // Save a copy to gallery as best effort, but always share via FileProvider URI so
        // grantUriPermission / FLAG_GRANT_READ_URI_PERMISSION succeeds cleanly across Binder.
        saveSnapshotImageToGallery(context, fileName, jpegBytes, width, height)
        return createShareableReportUri(context, fileName, jpegBytes)
    }

    fun saveSnapshotImageToGallery(
        context: Context,
        fileName: String,
        jpegBytes: ByteArray,
        width: Int,
        height: Int
    ): PdfSaveResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/FeedMill Ops")
                    put(MediaStore.Images.Media.WIDTH, width)
                    put(MediaStore.Images.Media.HEIGHT, height)
                    put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { stream ->
                        stream.write(jpegBytes)
                        stream.flush()
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    return PdfSaveResult(uri = uri, fileName = fileName, bytes = jpegBytes)
                }
            } catch (_: Exception) {
                // Fall back to FileProvider when MediaStore insert is unavailable
            }
        }
        return PdfSaveResult(uri = null, fileName = fileName, bytes = jpegBytes)
    }

    fun buildDailyLogCsv(dayLog: DailyPlantLog, sectionFilter: String = "ALL"): String {
        fun esc(s: String): String = "\"${s.replace("\"", "\"\"")}\""
        val filteredSkus = dayLog.skuRows.filter { sectionFilter == "ALL" || it.sku.section == sectionFilter }
        return buildString {
            appendLine("10K Agri Feeds - Bulacan Plant Auto Daily Inventory & Orders Log")
            appendLine("Date,${esc(dayLog.date)},Location,${esc(sectionFilter)},Closing Stock (kg),${dayLog.totalClosingStockKg},Day Produced (kg),${dayLog.totalProducedDayKg},Day Dispatched (kg),${dayLog.totalDispatchedDayKg},Daily Orders,${dayLog.orderRows.size}")
            appendLine()
            appendLine("DAILY INVENTORY LOG")
            appendLine("Date,Product,Location,Closing Stock (kg),Closing Bags,Produced Day (kg),Dispatched Day (kg),Open Orders Snapshot (kg),Edited On Day")
            filteredSkus.forEach { row ->
                val editNote = if (row.editedOnDay) "Edited ${row.editedTimeLabel}".trim() else ""
                appendLine("${dayLog.date},${esc(row.sku.productName)},${esc(row.sku.section)},${row.closingStockKg},${String.format(Locale.US, "%.2f", row.closingBags)},${row.producedDayKg},${row.dispatchedDayKg},${row.openOrdersSnapshotKg},${esc(editNote)}")
            }
            appendLine()
            appendLine("DAILY ORDERS LOG")
            appendLine("Order Code,Customer,Product,Ordered Bags,Total (kg),Dispatched Total (kg),Day Dispatched (kg),Order Date,Due Date,Status,Priority,Activity On Date")
            dayLog.orderRows.forEach { row ->
                val o = row.order
                val tags = buildList {
                    if (row.placedOnDay) add("Placed")
                    if (row.dueOnDay) add("Due")
                    if (row.editedOnDay) add("Edited ${row.editedTimeLabel}".trim())
                    if (row.dispatchedOnDayKg > 0.0) add("Dispatched ${OperationsCalculator.formatKg(row.dispatchedOnDayKg)}kg")
                }.joinToString(" | ")
                appendLine("${esc(o.orderCode)},${esc(o.customer)},${esc(o.productName)},${o.orderedBags},${o.totalWeightKg},${o.dispatchedWeightKg},${row.dispatchedOnDayKg},${o.orderDate},${o.dueDate},${esc(o.status)},${o.priority},${esc(tags)}")
            }
        }
    }

    fun generateDailyLogPdfBytes(
        dayLog: DailyPlantLog,
        sectionFilter: String = "ALL"
    ): ByteArray {
        val filteredSkus = dayLog.skuRows.filter { sectionFilter == "ALL" || it.sku.section == sectionFilter }
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val contentWidth = pageWidth - margin * 2

        val pdf = PdfDocument()
        var pageNum = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas
        var y = margin

        val titlePaint = Paint().apply {
            color = Color.rgb(26, 66, 42)
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(67, 81, 72)
            textSize = 9.5f
            isAntiAlias = true
        }
        val sectionHeaderPaint = Paint().apply {
            color = Color.rgb(26, 66, 42)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(17, 28, 22)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(17, 28, 22)
            textSize = 8.2f
            isAntiAlias = true
        }
        val mutedPaint = Paint().apply {
            color = Color.rgb(90, 105, 96)
            textSize = 7.8f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 206)
            strokeWidth = 0.8f
        }
        val fillBoxPaint = Paint().apply {
            color = Color.rgb(240, 244, 241)
            style = Paint.Style.FILL
        }

        fun drawFooter(pNum: Int) {
            canvas.drawLine(margin, pageHeight - 28f, pageWidth - margin, pageHeight - 28f, linePaint)
            canvas.drawText(
                "10K Agri Feeds – Bulacan Plant | Auto Daily Log (${dayLog.date})",
                margin,
                pageHeight - 16f,
                mutedPaint
            )
            val pageStr = "Page $pNum"
            canvas.drawText(pageStr, pageWidth - margin - mutedPaint.measureText(pageStr), pageHeight - 16f, mutedPaint)
        }

        fun ensureSpace(neededHeight: Float, repeatHeader: (() -> Unit)? = null) {
            if (y + neededHeight > pageHeight - 36f) {
                drawFooter(pageNum)
                pdf.finishPage(page)
                pageNum++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = page.canvas
                y = margin
                repeatHeader?.invoke()
            }
        }

        val generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("10K AGRI FEEDS – AUTO DAILY LOG", margin, y + 14f, titlePaint)
        y += 20f
        canvas.drawText("Date: ${dayLog.date}  |  Location: $sectionFilter  |  Generated: $generatedAt", margin, y + 9f, subtitlePaint)
        y += 16f

        val dailyKpi1 = "Closing Stock: ${OperationsCalculator.formatKg(dayLog.totalClosingStockKg)} kg  |  Day Produced: ${OperationsCalculator.formatKg(dayLog.totalProducedDayKg)} kg  |  Day Dispatched: ${OperationsCalculator.formatKg(dayLog.totalDispatchedDayKg)} kg"
        val dailyKpi2 = "SKUs Logged: ${filteredSkus.size} (${dayLog.skusEditedCount} updated)  |  Daily Orders: ${dayLog.orderRows.size} (${dayLog.ordersPlacedCount} placed, ${dayLog.ordersDueCount} due, ${dayLog.linkedDispatchOrdersCount} dispatched POs)"
        val wrappedDailyKpis = wrapPdfLines(dailyKpi1, contentWidth - 20f, tableHeaderPaint).map { it to tableHeaderPaint } +
            wrapPdfLines(dailyKpi2, contentWidth - 20f, bodyPaint).map { it to bodyPaint }
        val boxLineHeight = 13f
        val boxHeight = 14f + wrappedDailyKpis.size * boxLineHeight + 4f
        canvas.drawRoundRect(margin, y, margin + contentWidth, y + boxHeight, 6f, 6f, fillBoxPaint)
        wrappedDailyKpis.forEachIndexed { idx, (line, p) ->
            canvas.drawText(line, margin + 10f, y + 15f + idx * boxLineHeight, p)
        }
        y += boxHeight + 14f

        fun drawInventoryHeader() {
            ensureSpace(24f)
            canvas.drawText("DAILY INVENTORY LOG (${filteredSkus.size} SKUs)", margin, y + 10f, sectionHeaderPaint)
            y += 16f
            canvas.drawRect(margin, y, margin + contentWidth, y + 16f, fillBoxPaint)
            canvas.drawText("Product Name", margin + 4f, y + 11f, tableHeaderPaint)
            canvas.drawText("Location", margin + 195f, y + 11f, tableHeaderPaint)
            canvas.drawText("Closing (kg)", margin + 305f, y + 11f, tableHeaderPaint)
            canvas.drawText("Prod (kg)", margin + 382f, y + 11f, tableHeaderPaint)
            canvas.drawText("Disp (kg)", margin + 448f, y + 11f, tableHeaderPaint)
            y += 20f
        }

        drawInventoryHeader()
        if (filteredSkus.isEmpty()) {
            canvas.drawText("No SKUs matched the selected location filter.", margin + 4f, y + 10f, mutedPaint)
            y += 16f
        } else {
            filteredSkus.forEach { row ->
                ensureSpace(16f) { drawInventoryHeader() }
                val pName = if (row.sku.productName.length > 33) row.sku.productName.take(30) + "..." else row.sku.productName
                val loc = if (row.sku.section.length > 17) row.sku.section.take(15) + ".." else row.sku.section
                canvas.drawText(pName, margin + 4f, y + 9f, bodyPaint)
                canvas.drawText(loc, margin + 195f, y + 9f, mutedPaint)
                canvas.drawText(OperationsCalculator.formatKg(row.closingStockKg), margin + 305f, y + 9f, bodyPaint)
                canvas.drawText(OperationsCalculator.formatKg(row.producedDayKg), margin + 382f, y + 9f, bodyPaint)
                canvas.drawText(OperationsCalculator.formatKg(row.dispatchedDayKg), margin + 448f, y + 9f, bodyPaint)
                y += 13f
                canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                y += 2f
            }
        }

        y += 10f
        fun drawOrdersHeader() {
            ensureSpace(24f)
            canvas.drawText("DAILY ORDERS LOG (${dayLog.orderRows.size})", margin, y + 10f, sectionHeaderPaint)
            y += 16f
            canvas.drawRect(margin, y, margin + contentWidth, y + 16f, fillBoxPaint)
            canvas.drawText("Order / Customer", margin + 4f, y + 11f, tableHeaderPaint)
            canvas.drawText("Product", margin + 185f, y + 11f, tableHeaderPaint)
            canvas.drawText("Ordered", margin + 340f, y + 11f, tableHeaderPaint)
            canvas.drawText("Day Disp", margin + 408f, y + 11f, tableHeaderPaint)
            canvas.drawText("Status", margin + 468f, y + 11f, tableHeaderPaint)
            y += 20f
        }

        drawOrdersHeader()
        if (dayLog.orderRows.isEmpty()) {
            canvas.drawText("No customer order activity recorded on ${dayLog.date}.", margin + 4f, y + 10f, mutedPaint)
            y += 16f
        } else {
            dayLog.orderRows.forEach { row ->
                ensureSpace(16f) { drawOrdersHeader() }
                val o = row.order
                val cust = "${o.orderCode} ${o.customer}".let { if (it.length > 31) it.take(28) + "..." else it }
                val prod = if (o.productName.length > 26) o.productName.take(23) + "..." else o.productName
                val st = if (o.status.length > 10) o.status.take(9) + "." else o.status
                canvas.drawText(cust, margin + 4f, y + 9f, bodyPaint)
                canvas.drawText(prod, margin + 185f, y + 9f, mutedPaint)
                canvas.drawText("${o.orderedBags}b", margin + 340f, y + 9f, bodyPaint)
                canvas.drawText(OperationsCalculator.formatKg(row.dispatchedOnDayKg), margin + 408f, y + 9f, bodyPaint)
                canvas.drawText(st, margin + 468f, y + 9f, bodyPaint)
                y += 13f
                canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                y += 2f
            }
        }

        drawFooter(pageNum)
        pdf.finishPage(page)

        val out = ByteArrayOutputStream()
        pdf.writeTo(out)
        pdf.close()
        return out.toByteArray()
    }

    fun buildDailyLogXlsSpreadsheetXml(
        dayLog: DailyPlantLog,
        sectionFilter: String = "ALL"
    ): String {
        val filteredSkus = dayLog.skuRows.filter { sectionFilter == "ALL" || it.sku.section == sectionFilter }
        val overviewRows = listOf(
            XlsRow(listOf(XlsCell.Title("10K Agri Feeds - Bulacan Plant Auto Daily Log"))),
            XlsRow(listOf(XlsCell.Subtitle("Date: ${dayLog.date} | Location: $sectionFilter"))),
            XlsRow(emptyList()),
            XlsRow(listOf(XlsCell.Header("Metric"), XlsCell.Header("Value"))),
            XlsRow(listOf(XlsCell.Text("Closing Stock (kg)"), XlsCell.Num(dayLog.totalClosingStockKg))),
            XlsRow(listOf(XlsCell.Text("Day Produced (kg)"), XlsCell.Num(dayLog.totalProducedDayKg))),
            XlsRow(listOf(XlsCell.Text("Day Dispatched (kg)"), XlsCell.Num(dayLog.totalDispatchedDayKg))),
            XlsRow(listOf(XlsCell.Text("SKUs Logged"), XlsCell.IntNum(filteredSkus.size.toLong()))),
            XlsRow(listOf(XlsCell.Text("SKUs Edited on Date"), XlsCell.IntNum(dayLog.skusEditedCount.toLong()))),
            XlsRow(listOf(XlsCell.Text("Daily Orders"), XlsCell.IntNum(dayLog.orderRows.size.toLong()))),
            XlsRow(listOf(XlsCell.Text("Orders Placed on Date"), XlsCell.IntNum(dayLog.ordersPlacedCount.toLong()))),
            XlsRow(listOf(XlsCell.Text("Orders Due on Date"), XlsCell.IntNum(dayLog.ordersDueCount.toLong()))),
            XlsRow(listOf(XlsCell.Text("Dispatched POs on Date"), XlsCell.IntNum(dayLog.linkedDispatchOrdersCount.toLong())))
        )

        val invRows = buildList {
            add(
                XlsRow(
                    listOf(
                        "Date", "Product Name", "Location", "Closing Stock (kg)", "Closing Bags",
                        "Produced Day (kg)", "Dispatched Day (kg)", "Open Orders Snapshot (kg)", "Edited On Day"
                    ).map { XlsCell.Header(it) }
                )
            )
            filteredSkus.forEach { row ->
                val editNote = if (row.editedOnDay) "Edited ${row.editedTimeLabel}".trim() else ""
                add(
                    XlsRow(
                        listOf(
                            XlsCell.Text(dayLog.date),
                            XlsCell.Text(row.sku.productName),
                            XlsCell.Text(row.sku.section),
                            XlsCell.Num(row.closingStockKg),
                            XlsCell.Num(row.closingBags),
                            XlsCell.Num(row.producedDayKg),
                            XlsCell.Num(row.dispatchedDayKg),
                            XlsCell.Num(row.openOrdersSnapshotKg),
                            XlsCell.Text(editNote)
                        )
                    )
                )
            }
        }

        val orderRows = buildList {
            add(
                XlsRow(
                    listOf(
                        "Order Code", "Customer", "Product Name", "Ordered Bags", "Total Weight (kg)",
                        "Dispatched Total (kg)", "Day Dispatched (kg)", "Order Date", "Due Date", "Status",
                        "Priority", "Activity On Date"
                    ).map { XlsCell.Header(it) }
                )
            )
            dayLog.orderRows.forEach { row ->
                val o = row.order
                val tags = buildList {
                    if (row.placedOnDay) add("Placed")
                    if (row.dueOnDay) add("Due")
                    if (row.editedOnDay) add("Edited ${row.editedTimeLabel}".trim())
                    if (row.dispatchedOnDayKg > 0.0) add("Dispatched ${OperationsCalculator.formatKg(row.dispatchedOnDayKg)}kg")
                }.joinToString(" | ")
                add(
                    XlsRow(
                        listOf(
                            XlsCell.Text(o.orderCode),
                            XlsCell.Text(o.customer),
                            XlsCell.Text(o.productName),
                            XlsCell.IntNum(o.orderedBags),
                            XlsCell.Num(o.totalWeightKg),
                            XlsCell.Num(o.dispatchedWeightKg),
                            XlsCell.Num(row.dispatchedOnDayKg),
                            XlsCell.Text(o.orderDate),
                            XlsCell.Text(o.dueDate),
                            XlsCell.Text(o.status),
                            XlsCell.Text(if (o.priority) "PRIORITY" else "Normal"),
                            XlsCell.Text(tags)
                        )
                    )
                )
            }
        }

        val invTag = if (dayLog.isToday) "TODAY" else dayLog.date
        val perPlantSheets = buildList {
            val rowsBySection = filteredSkus.groupBy { it.sku.section }
            orderedSectionsForNames(rowsBySection.keys).forEach { sectionName ->
                val rows = rowsBySection[sectionName].orEmpty()
                val sheetRows = buildList {
                    add(XlsRow(listOf(XlsCell.Header(sectionName), XlsCell.Header("CLOSING INVENTORY ($invTag)"))))
                    rows.forEach { row ->
                        add(
                            XlsRow(
                                listOf(
                                    XlsCell.Text(displayProductNameForSection(row.sku.productName, sectionName)),
                                    XlsCell.Num(row.closingBags)
                                )
                            )
                        )
                    }
                }
                add(XlsSheet(sectionName, sheetRows))
            }
        }

        return buildSpreadsheetWorkbook(
            listOf(
                XlsSheet("Overview", overviewRows),
                XlsSheet("Daily Inventory", invRows),
                XlsSheet("Daily Orders", orderRows)
            ) + perPlantSheets
        )
    }

    fun buildXlsSpreadsheetXml(report: PlantReportData): String {
        val totalAvailKg = report.sectionSummaries.sumOf { it.totalAvailableStockKg }
        val totalAvailBags = report.sectionSummaries.sumOf { it.totalAvailableBags }
        val totalExpectedKg = report.sectionSummaries.sumOf { it.totalExpectedKg }
        val avgRecovery = if (totalExpectedKg > 0.0) (report.totalRecoveredKg / totalExpectedKg) * 100.0 else 100.0

        val sheets = mutableListOf<XlsSheet>()

        val overviewRows = buildList {
            add(XlsRow(listOf(XlsCell.Title("10K Agri Feeds - Bulacan Plant"))))
            add(XlsRow(listOf(XlsCell.Subtitle("${reportTitle(report)} | ${periodLabel(report)} | Location: ${report.sectionFilter}"))))
            add(XlsRow(listOf(XlsCell.Subtitle("Scope: ${scopeLabel(report)}"))))
            add(XlsRow(emptyList()))
            add(XlsRow(listOf(XlsCell.Header("Key Metric"), XlsCell.Header("Value"))))
            if (report.includes(ReportSectionOption.INVENTORY)) {
                add(XlsRow(listOf(XlsCell.Text("Available Stock (kg)"), XlsCell.Num(totalAvailKg))))
                add(XlsRow(listOf(XlsCell.Text("Available Stock (bags)"), XlsCell.Num(totalAvailBags))))
                add(XlsRow(listOf(XlsCell.Text("SKU Count (Healthy / Watch / Critical)"), XlsCell.Text("${report.productRows.size} (${report.healthySkuCount} / ${report.watchSkuCount} / ${report.criticalSkuCount})"))))
            }
            if (report.includes(ReportSectionOption.RECOVERY)) {
                add(XlsRow(listOf(XlsCell.Text("Produced Bags"), XlsCell.IntNum(report.totalProducedBags))))
                add(XlsRow(listOf(XlsCell.Text("Recovered Weight (kg)"), XlsCell.Num(report.totalRecoveredKg))))
                add(XlsRow(listOf(XlsCell.Text("Average Recovery %"), XlsCell.Num(avgRecovery))))
            }
            if (report.includes(ReportSectionOption.DISPATCHES)) {
                add(XlsRow(listOf(XlsCell.Text("Dispatched Net Weight (kg)"), XlsCell.Num(report.totalDispatchedKg))))
                add(XlsRow(listOf(XlsCell.Text("Dispatch Trips (Del / Ret / Held)"), XlsCell.Text("${report.dispatches.size} (${report.deliveredCount} / ${report.returnedCount} / ${report.heldCount})"))))
            }
            if (report.includes(ReportSectionOption.ORDERS)) {
                add(XlsRow(listOf(XlsCell.Text("Orders in Period"), XlsCell.IntNum(report.orders.size.toLong()))))
                add(XlsRow(listOf(XlsCell.Text("Live Open Orders (kg)"), XlsCell.Num(report.totalOpenOrdersKg))))
                add(XlsRow(listOf(XlsCell.Text("Order Backlog Shortfall (kg)"), XlsCell.Num(report.activeOrderShortfallKg))))
            }
            if (report.includes(ReportSectionOption.SCHEDULE)) {
                add(XlsRow(listOf(XlsCell.Text("Schedule Runs in Period"), XlsCell.IntNum(report.schedules.size.toLong()))))
                add(XlsRow(listOf(XlsCell.Text("Scheduled Active Bags"), XlsCell.IntNum(report.scheduledQueueBags))))
            }
            if (report.includes(ReportSectionOption.INVENTORY) || report.includes(ReportSectionOption.RECOVERY)) {
                add(XlsRow(emptyList()))
                add(
                    XlsRow(
                        listOf("Location", "SKUs", "Avail Bags", "Avail Stock (kg)", "Produced Bags", "Recovered (kg)", "Variance (kg)", "Recovery %")
                            .map { XlsCell.Header(it) }
                    )
                )
                report.sectionSummaries.forEach { sec ->
                    add(
                        XlsRow(
                            listOf(
                                XlsCell.Text(sec.section.label),
                                XlsCell.IntNum(sec.skuCount.toLong()),
                                XlsCell.Num(sec.totalAvailableBags),
                                XlsCell.Num(sec.totalAvailableStockKg),
                                XlsCell.IntNum(sec.totalBags),
                                XlsCell.Num(sec.totalRecoveredKg),
                                XlsCell.Num(sec.totalVarianceKg),
                                XlsCell.Num(sec.recoveryPct)
                            )
                        )
                    )
                }
            }
        }
        sheets.add(XlsSheet("Overview", overviewRows))

        if (report.includes(ReportSectionOption.RECOVERY)) {
            val prodRows = buildList {
                add(
                    XlsRow(
                        listOf(
                            "Production Location", "Date", "Product Name", "Bags Produced",
                            "Bag Weight (kg)", "Expected (kg)", "Recovered (kg)", "Variance (kg)",
                            "Recovery %", "Remarks", "Scheduled Customers"
                        ).map { XlsCell.Header(it) }
                    )
                )
                report.sectionSummaries.forEach { sec ->
                    sec.batches.forEach { b ->
                        add(
                            XlsRow(
                                listOf(
                                    XlsCell.Text(b.section),
                                    XlsCell.Text(b.date),
                                    XlsCell.Text(b.productName),
                                    XlsCell.IntNum(b.bagsProduced),
                                    XlsCell.Num(b.bagWeightKg),
                                    XlsCell.Num(b.expectedWeightKg),
                                    XlsCell.Num(b.actualRecoveredKg),
                                    XlsCell.Num(b.varianceKg),
                                    XlsCell.Num(b.recoveryPct),
                                    XlsCell.Text(b.remarks),
                                    XlsCell.Text(b.allScheduledForCustomers().joinToString("; "))
                                )
                            )
                        )
                    }
                }
            }
            sheets.add(XlsSheet("Production", prodRows))
        }

        if (report.includes(ReportSectionOption.DISPATCHES)) {
            val dispRows = buildList {
                add(
                    XlsRow(
                        listOf(
                            "Dispatch Date", "DR Reference", "Customer", "Product Name",
                            "Bags Dispatched", "Net Weight (kg)", "Returned Weight (kg)",
                            "Status", "Driver", "Truck", "Remarks"
                        ).map { XlsCell.Header(it) }
                    )
                )
                report.dispatches.forEach { d ->
                    add(
                        XlsRow(
                            listOf(
                                XlsCell.Text(d.date),
                                XlsCell.Text(d.drReference),
                                XlsCell.Text(d.customer),
                                XlsCell.Text(d.productName),
                                XlsCell.IntNum(d.bagsDispatched),
                                XlsCell.Num(d.effectiveDispatchedWeightKg()),
                                XlsCell.Num(d.returnedWeightKg),
                                XlsCell.Text(d.resolvedStatusLabel()),
                                XlsCell.Text(d.driverName),
                                XlsCell.Text(d.truckNumber),
                                XlsCell.Text(d.remarks)
                            )
                        )
                    )
                }
            }
            sheets.add(XlsSheet("Dispatches", dispRows))
        }

        if (report.includes(ReportSectionOption.SCHEDULE)) {
            val schedRows = buildList {
                add(
                    XlsRow(
                        listOf(
                            "Schedule Date", "Location", "Product Name", "Target Bags",
                            "Bag Weight (kg)", "Priority", "Status", "Line Remarks", "Linked Orders"
                        ).map { XlsCell.Header(it) }
                    )
                )
                report.schedules.forEach { s ->
                    add(
                        XlsRow(
                            listOf(
                                XlsCell.Text(s.scheduledDate),
                                XlsCell.Text(s.section),
                                XlsCell.Text(s.productName),
                                XlsCell.IntNum(s.targetBags),
                                XlsCell.Num(s.bagWeightKg),
                                XlsCell.Text(s.priority),
                                XlsCell.Text(s.status),
                                XlsCell.Text(s.lineRemarks),
                                XlsCell.Text(s.allLinkedOrderCodes().joinToString("; "))
                            )
                        )
                    )
                }
            }
            sheets.add(XlsSheet("Schedule", schedRows))
        }

        if (report.includes(ReportSectionOption.ORDERS)) {
            val ordRows = buildList {
                add(
                    XlsRow(
                        listOf(
                            "Order Scope", "Order Code", "Order Date", "Due Date", "Customer",
                            "Product Name", "Ordered Bags", "Total Weight (kg)", "Dispatched (kg)",
                            "Remaining (kg)", "Status", "Priority"
                        ).map { XlsCell.Header(it) }
                    )
                )
                report.orders.forEach { o ->
                    val remKg = kotlin.math.max(0.0, o.totalWeightKg - o.dispatchedWeightKg)
                    add(
                        XlsRow(
                            listOf(
                                XlsCell.Text("Period"),
                                XlsCell.Text(o.orderCode),
                                XlsCell.Text(o.orderDate),
                                XlsCell.Text(o.dueDate),
                                XlsCell.Text(o.customer),
                                XlsCell.Text(o.productName),
                                XlsCell.IntNum(o.orderedBags),
                                XlsCell.Num(o.totalWeightKg),
                                XlsCell.Num(o.dispatchedWeightKg),
                                XlsCell.Num(remKg),
                                XlsCell.Text(o.status),
                                XlsCell.Text(if (o.priority) "PRIORITY" else "Normal")
                            )
                        )
                    )
                }
                report.activeOpenOrders.forEach { o ->
                    val remKg = kotlin.math.max(0.0, o.totalWeightKg - o.dispatchedWeightKg)
                    add(
                        XlsRow(
                            listOf(
                                XlsCell.Text("Active Open"),
                                XlsCell.Text(o.orderCode),
                                XlsCell.Text(o.orderDate),
                                XlsCell.Text(o.dueDate),
                                XlsCell.Text(o.customer),
                                XlsCell.Text(o.productName),
                                XlsCell.IntNum(o.orderedBags),
                                XlsCell.Num(o.totalWeightKg),
                                XlsCell.Num(o.dispatchedWeightKg),
                                XlsCell.Num(remKg),
                                XlsCell.Text(o.status),
                                XlsCell.Text(if (o.priority) "PRIORITY" else "Normal")
                            )
                        )
                    )
                }
            }
            sheets.add(XlsSheet("Orders", ordRows))
        }

        if (report.includes(ReportSectionOption.INVENTORY)) {
            val invRows = buildList {
                add(
                    XlsRow(
                        listOf(
                            "Product Name", "Location", "Produced Bags", "Recovered (kg)",
                            "Variance (kg)", "Dispatched (kg)", "Open Orders (kg)", "Available Stock (kg)"
                        ).map { XlsCell.Header(it) }
                    )
                )
                report.productRows.forEach { r ->
                    add(
                        XlsRow(
                            listOf(
                                XlsCell.Text(r.productName),
                                XlsCell.Text(r.section),
                                XlsCell.IntNum(r.producedBags),
                                XlsCell.Num(r.recoveredKg),
                                XlsCell.Num(r.varianceKg),
                                XlsCell.Num(r.dispatchedKg),
                                XlsCell.Num(r.openOrdersKg),
                                XlsCell.Num(r.availableStockKg)
                            )
                        )
                    )
                }
            }
            sheets.add(XlsSheet("Inventory", invRows))

            val invTag = if (report.isDailyReport && report.startDate == OperationsCalculator.todayIsoDate()) {
                "TODAY"
            } else if (report.isDailyReport) {
                report.startDate
            } else {
                "${report.startDate} to ${report.endDate}"
            }
            val rowsBySection = report.productRows.groupBy { it.section }
            orderedSectionsForNames(rowsBySection.keys).forEach { sectionName ->
                val rows = rowsBySection[sectionName].orEmpty()
                val plantRows = buildList {
                    add(
                        XlsRow(
                            listOf(
                                XlsCell.Header(sectionName),
                                XlsCell.Header("AVAILABLE INVENTORY ($invTag)")
                            )
                        )
                    )
                    rows.forEach { r ->
                        add(
                            XlsRow(
                                listOf(
                                    XlsCell.Text(displayProductNameForSection(r.productName, sectionName)),
                                    XlsCell.Num(r.availableBags)
                                )
                            )
                        )
                    }
                }
                sheets.add(XlsSheet(sectionName, plantRows))
            }
        }

        return buildSpreadsheetWorkbook(sheets)
    }

    private sealed interface XlsCell {
        data class Title(val value: String) : XlsCell
        data class Subtitle(val value: String) : XlsCell
        data class Header(val value: String) : XlsCell
        data class Text(val value: String) : XlsCell
        data class Num(val value: Double) : XlsCell
        data class IntNum(val value: Long) : XlsCell
    }

    private data class XlsRow(val cells: List<XlsCell>)
    private data class XlsSheet(val name: String, val rows: List<XlsRow>)

    private fun xmlEscape(raw: String): String = buildString(raw.length) {
        for (ch in raw) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                '\t', '\n', '\r' -> append(ch)
                else -> if (ch.code >= 0x20) append(ch)
            }
        }
    }

    private fun safeSheetName(raw: String): String {
        val cleaned = raw.replace(Regex("[\\\\/:*?\\[\\]]"), "_").trim().ifEmpty { "Sheet" }
        return cleaned.take(31)
    }

    private fun buildSpreadsheetWorkbook(sheets: List<XlsSheet>): String = buildString {
        appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        appendLine("<?mso-application progid=\"Excel.Sheet\"?>")
        appendLine("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"")
        appendLine(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"")
        appendLine(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"")
        appendLine(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">")
        appendLine(" <Styles>")
        appendLine("  <Style ss:ID=\"Default\" ss:Name=\"Normal\"><Alignment ss:Vertical=\"Center\"/><Font ss:FontName=\"Calibri\" ss:Size=\"10\" ss:Color=\"#111C16\"/></Style>")
        appendLine("  <Style ss:ID=\"Title\"><Font ss:FontName=\"Calibri\" ss:Size=\"13\" ss:Bold=\"1\" ss:Color=\"#1A422A\"/></Style>")
        appendLine("  <Style ss:ID=\"Subtitle\"><Font ss:FontName=\"Calibri\" ss:Size=\"10\" ss:Italic=\"1\" ss:Color=\"#435148\"/></Style>")
        appendLine("  <Style ss:ID=\"Header\"><Font ss:FontName=\"Calibri\" ss:Size=\"10\" ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/><Interior ss:Color=\"#1A422A\" ss:Pattern=\"Solid\"/></Style>")
        appendLine("  <Style ss:ID=\"Number\"><NumberFormat ss:Format=\"#,##0.00\"/></Style>")
        appendLine("  <Style ss:ID=\"Integer\"><NumberFormat ss:Format=\"#,##0\"/></Style>")
        appendLine(" </Styles>")
        sheets.forEach { sheet ->
            appendLine(" <Worksheet ss:Name=\"${xmlEscape(safeSheetName(sheet.name))}\">")
            appendLine("  <Table>")
            val rows = sheet.rows.ifEmpty { listOf(XlsRow(listOf(XlsCell.Text("No records")))) }
            rows.forEach { row ->
                append("   <Row>")
                if (row.cells.isEmpty()) {
                    append("<Cell><Data ss:Type=\"String\"></Data></Cell>")
                } else {
                    row.cells.forEach { cell ->
                        when (cell) {
                            is XlsCell.Title -> append("<Cell ss:StyleID=\"Title\"><Data ss:Type=\"String\">${xmlEscape(cell.value)}</Data></Cell>")
                            is XlsCell.Subtitle -> append("<Cell ss:StyleID=\"Subtitle\"><Data ss:Type=\"String\">${xmlEscape(cell.value)}</Data></Cell>")
                            is XlsCell.Header -> append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">${xmlEscape(cell.value)}</Data></Cell>")
                            is XlsCell.Text -> append("<Cell><Data ss:Type=\"String\">${xmlEscape(cell.value)}</Data></Cell>")
                            is XlsCell.Num -> {
                                val safeVal = if (cell.value.isFinite()) cell.value else 0.0
                                append("<Cell ss:StyleID=\"Number\"><Data ss:Type=\"Number\">${String.format(Locale.US, "%.4f", safeVal)}</Data></Cell>")
                            }
                            is XlsCell.IntNum -> append("<Cell ss:StyleID=\"Integer\"><Data ss:Type=\"Number\">${cell.value}</Data></Cell>")
                        }
                    }
                }
                appendLine("</Row>")
            }
            appendLine("  </Table>")
            appendLine(" </Worksheet>")
        }
        appendLine("</Workbook>")
    }

    fun saveDailyLogCsvToDownloads(
        context: Context,
        dayLog: DailyPlantLog,
        sectionFilter: String = "ALL"
    ): PdfSaveResult {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val secSlug = if (sectionFilter == "ALL") "" else "_${sectionFilter.replace(" ", "_")}"
        val fileName = "Bulacan_Daily_Log_${dayLog.date}${secSlug}_$stamp.csv"
        val csv = buildDailyLogCsv(dayLog, sectionFilter)
        return saveBytesToDownloads(context, fileName, ExportFileFormat.CSV.mimeType, csv.toByteArray(Charsets.UTF_8))
    }

    fun saveDailyLogPdfToDownloads(
        context: Context,
        dayLog: DailyPlantLog,
        sectionFilter: String = "ALL"
    ): PdfSaveResult {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val secSlug = if (sectionFilter == "ALL") "" else "_${sectionFilter.replace(" ", "_")}"
        val fileName = "Bulacan_Daily_Log_${dayLog.date}${secSlug}_$stamp.pdf"
        val bytes = generateDailyLogPdfBytes(dayLog, sectionFilter)
        return saveBytesToDownloads(context, fileName, ExportFileFormat.PDF.mimeType, bytes)
    }

    fun saveDailyLogXlsToDownloads(
        context: Context,
        dayLog: DailyPlantLog,
        sectionFilter: String = "ALL"
    ): PdfSaveResult {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val secSlug = if (sectionFilter == "ALL") "" else "_${sectionFilter.replace(" ", "_")}"
        val fileName = "Bulacan_Daily_Log_${dayLog.date}${secSlug}_$stamp.xls"
        val xml = buildDailyLogXlsSpreadsheetXml(dayLog, sectionFilter)
        return saveBytesToDownloads(context, fileName, ExportFileFormat.XLS.mimeType, xml.toByteArray(Charsets.UTF_8))
    }

    fun saveDailyLogToDownloads(
        context: Context,
        dayLog: DailyPlantLog,
        sectionFilter: String = "ALL",
        format: ExportFileFormat
    ): PdfSaveResult = when (format) {
        ExportFileFormat.CSV -> saveDailyLogCsvToDownloads(context, dayLog, sectionFilter)
        ExportFileFormat.PDF -> saveDailyLogPdfToDownloads(context, dayLog, sectionFilter)
        ExportFileFormat.XLS -> saveDailyLogXlsToDownloads(context, dayLog, sectionFilter)
    }

    internal fun saveBytesToDownloads(
        context: Context,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): PdfSaveResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { stream ->
                    stream.write(bytes)
                    stream.flush()
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                return PdfSaveResult(uri = uri, fileName = fileName, bytes = bytes)
            }
        }
        return PdfSaveResult(uri = null, fileName = fileName, bytes = bytes)
    }

    fun printReportHtml(context: Context, report: PlantReportData) {
        val heading = reportTitle(report)
        val html = buildString {
            append("<html><head><style>")
            append("body{font-family:sans-serif;padding:24px;color:#111C16;}")
            append("h1{color:#1A422A;margin-bottom:4px;} h2{color:#1A422A;border-bottom:2px solid #1A422A;padding-bottom:4px;margin-top:20px;}")
            append("table{width:100%;border-collapse:collapse;margin-top:8px;font-size:12px;}")
            append("th,td{border:1px solid #CBD5CE;padding:6px 8px;text-align:left;}")
            append("th{background:#E8ECE9;} .num{text-align:right;font-family:monospace;}")
            append(".summary{background:#F4F6F4;padding:12px;border-radius:8px;margin-bottom:16px;}")
            append("</style></head><body>")
            append("<h1>10K Agri Feeds - Bulacan Plant</h1>")
            append("<p><strong>$heading</strong> | ${periodLabel(report)} | Location: ${report.sectionFilter} | Scope: ${scopeLabel(report)}</p>")
            append("<div class='summary'>")
            if (report.includes(ReportSectionOption.INVENTORY)) {
                val totalAvailKg = report.sectionSummaries.sumOf { it.totalAvailableStockKg }
                append("<strong>Available Stock:</strong> ${OperationsCalculator.formatKg(totalAvailKg)} kg &nbsp;|&nbsp; ")
            }
            if (report.includes(ReportSectionOption.RECOVERY)) {
                append("<strong>Produced:</strong> ${report.totalProducedBags} bags (${OperationsCalculator.formatKg(report.totalRecoveredKg)} kg) &nbsp;|&nbsp; ")
            }
            if (report.includes(ReportSectionOption.DISPATCHES)) {
                append("<strong>Dispatched:</strong> ${OperationsCalculator.formatKg(report.totalDispatchedKg)} kg (${report.dispatches.size} trips) &nbsp;|&nbsp; ")
            }
            if (report.includes(ReportSectionOption.ORDERS)) {
                append("<strong>Open Orders:</strong> ${OperationsCalculator.formatKg(report.totalOpenOrdersKg)} kg (Backlog: ${OperationsCalculator.formatKg(report.activeOrderShortfallKg)} kg) &nbsp;|&nbsp; ")
            }
            if (report.includes(ReportSectionOption.SCHEDULE)) {
                append("<strong>Scheduled:</strong> ${report.schedules.size} (${report.scheduledQueueBags}b active)")
            }
            append("</div>")

            if (report.includes(ReportSectionOption.INVENTORY) || report.includes(ReportSectionOption.RECOVERY)) {
                report.sectionSummaries.forEach { sec ->
                    append("<h2>${sec.section.label} (${sec.skuCount} SKUs) — Avail: ${OperationsCalculator.formatBags(sec.totalAvailableBags)} bags (${OperationsCalculator.formatKg(sec.totalAvailableStockKg)} kg)</h2>")
                    if (report.includes(ReportSectionOption.RECOVERY)) {
                        if (sec.batches.isNotEmpty()) {
                            append("<table><tr><th>Date</th><th>Product</th><th class='num'>Bags</th><th class='num'>Bag Wt</th><th class='num'>Recovered (kg)</th><th class='num'>Var (kg)</th><th>Remarks</th></tr>")
                            sec.batches.forEach { b ->
                                append("<tr><td>${b.date}</td><td>${b.productName}</td><td class='num'>${b.bagsProduced}</td><td class='num'>${b.bagWeightKg}</td><td class='num'>${OperationsCalculator.formatKg(b.actualRecoveredKg)}</td><td class='num'>${OperationsCalculator.formatKg(b.varianceKg)}</td><td>${b.remarks}</td></tr>")
                            }
                            append("</table>")
                        } else {
                            append("<p>No production batches logged in selected period.</p>")
                        }
                    }
                }
            }

            if (report.includes(ReportSectionOption.DISPATCHES)) {
                append("<h2>Dispatches & Fleet (${report.dispatches.size})</h2>")
                if (report.dispatches.isNotEmpty()) {
                    append("<table><tr><th>Date</th><th>DR #</th><th>Customer</th><th>Product</th><th class='num'>Bags</th><th class='num'>Net (kg)</th><th>Status</th></tr>")
                    report.dispatches.forEach { d ->
                        append("<tr><td>${d.date}</td><td>${d.drReference}</td><td>${d.customer}</td><td>${d.productName}</td><td class='num'>${d.bagsDispatched}</td><td class='num'>${OperationsCalculator.formatKg(d.effectiveDispatchedWeightKg())}</td><td>${d.resolvedStatusLabel()}</td></tr>")
                    }
                    append("</table>")
                } else {
                    append("<p>No dispatches logged in selected period.</p>")
                }
            }

            if (report.includes(ReportSectionOption.SCHEDULE)) {
                append("<h2>Scheduled Runs (${report.schedules.size})</h2>")
                if (report.schedules.isNotEmpty()) {
                    append("<table><tr><th>Date</th><th>Section</th><th>Product</th><th class='num'>Target Bags</th><th>Priority</th><th>Status</th></tr>")
                    report.schedules.forEach { s ->
                        append("<tr><td>${s.scheduledDate}</td><td>${s.section}</td><td>${s.productName}</td><td class='num'>${s.targetBags}</td><td>${s.priority}</td><td>${s.status}</td></tr>")
                    }
                    append("</table>")
                } else {
                    append("<p>No scheduled production runs in selected period.</p>")
                }
            }

            if (report.includes(ReportSectionOption.ORDERS)) {
                append("<h2>Customer Orders in Period (${report.orders.size})</h2>")
                if (report.orders.isNotEmpty()) {
                    append("<table><tr><th>Order Code</th><th>Customer</th><th>Product</th><th class='num'>Total (kg)</th><th>Due Date</th><th>Status</th></tr>")
                    report.orders.forEach { o ->
                        append("<tr><td>${o.orderCode}</td><td>${o.customer}</td><td>${o.productName}</td><td class='num'>${OperationsCalculator.formatKg(o.totalWeightKg)}</td><td>${o.dueDate}</td><td>${o.status}</td></tr>")
                    }
                    append("</table>")
                } else {
                    append("<p>No orders placed or due in selected period.</p>")
                }
                append("<h2>Live Active Open Orders (${report.activeOpenOrders.size})</h2>")
                if (report.activeOpenOrders.isNotEmpty()) {
                    append("<table><tr><th>Order Code</th><th>Customer</th><th>Product</th><th class='num'>Remaining (kg)</th><th>Due Date</th><th>Status</th></tr>")
                    report.activeOpenOrders.forEach { o ->
                        val remKg = kotlin.math.max(0.0, o.totalWeightKg - o.dispatchedWeightKg)
                        append("<tr><td>${o.orderCode}</td><td>${o.customer}</td><td>${o.productName}</td><td class='num'>${OperationsCalculator.formatKg(remKg)}</td><td>${o.dueDate}</td><td>${o.status}</td></tr>")
                    }
                    append("</table>")
                } else {
                    append("<p>No active open orders remaining.</p>")
                }
            }

            if (report.includes(ReportSectionOption.INVENTORY)) {
                append("<h2>Product Inventory & Balance</h2>")
                append("<table><tr><th>Product</th><th>Location</th><th class='num'>Produced (kg)</th><th class='num'>Dispatched (kg)</th><th class='num'>Open Orders (kg)</th><th class='num'>Available (kg)</th></tr>")
                report.productRows.forEach { r ->
                    append("<tr><td>${r.productName}</td><td>${r.section}</td><td class='num'>${OperationsCalculator.formatKg(r.recoveredKg)}</td><td class='num'>${OperationsCalculator.formatKg(r.dispatchedKg)}</td><td class='num'>${OperationsCalculator.formatKg(r.openOrdersKg)}</td><td class='num'>${OperationsCalculator.formatKg(r.availableStockKg)}</td></tr>")
                }
                append("</table>")
            }
            append("</body></html>")
        }

        tailrec fun Context.findPrintActivity(): Activity? = when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findPrintActivity()
            else -> null
        }

        val activity = context.findPrintActivity()
        if (activity == null || activity.isFinishing || activity.isDestroyed) return

        try {
            val webView = WebView(activity)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    if (activity.isFinishing || activity.isDestroyed) return
                    try {
                        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                        val adapter = view.createPrintDocumentAdapter(reportFileBase(report))
                        printManager?.print(
                            heading,
                            adapter,
                            PrintAttributes.Builder().build()
                        )
                    } catch (_: Exception) {
                        // Ignore PrintManager Binder/window failures
                    }
                }
            }
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        } catch (_: Exception) {
            // Ignore WebView initialization failures
        }
    }

    fun buildBreadcrumbsCsv(entries: List<BreadcrumbEntry>): String = buildString {
        fun esc(v: String): String = "\"${v.replace("\"", "\"\"")}\""
        appendLine("Date,Time,Actor,Category,Severity,Screen,Action,Details,EntityRef,TrailPath")
        entries.forEach { e ->
            appendLine(
                listOf(
                    esc(e.dateStr),
                    esc(e.timeLabel),
                    esc(e.actorType),
                    esc(e.category),
                    esc(e.severity),
                    esc(e.screenTab),
                    esc(e.action),
                    esc(e.details),
                    esc(e.entityRef),
                    esc(e.trailPath)
                ).joinToString(",")
            )
        }
    }

    fun generateBreadcrumbsPdfBytes(
        entries: List<BreadcrumbEntry>,
        scopeLabel: String = "All Activity"
    ): ByteArray {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val contentWidth = pageWidth - margin * 2

        val pdf = PdfDocument()
        var pageNum = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas
        var y = margin

        val titlePaint = Paint().apply {
            color = Color.rgb(26, 66, 42)
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(67, 81, 72)
            textSize = 9.5f
            isAntiAlias = true
        }
        val sectionHeaderPaint = Paint().apply {
            color = Color.rgb(26, 66, 42)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(17, 28, 22)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(17, 28, 22)
            textSize = 8.2f
            isAntiAlias = true
        }
        val mutedPaint = Paint().apply {
            color = Color.rgb(90, 105, 96)
            textSize = 7.8f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 206)
            strokeWidth = 0.8f
        }
        val fillBoxPaint = Paint().apply {
            color = Color.rgb(240, 244, 241)
            style = Paint.Style.FILL
        }

        fun drawFooter(pNum: Int) {
            canvas.drawLine(margin, pageHeight - 28f, pageWidth - margin, pageHeight - 28f, linePaint)
            canvas.drawText(
                "10K Agri Feeds – Bulacan Plant | Activity History ($scopeLabel)",
                margin,
                pageHeight - 16f,
                mutedPaint
            )
            val pageStr = "Page $pNum"
            canvas.drawText(pageStr, pageWidth - margin - mutedPaint.measureText(pageStr), pageHeight - 16f, mutedPaint)
        }

        fun ensureSpace(neededHeight: Float, repeatHeader: (() -> Unit)? = null) {
            if (y + neededHeight > pageHeight - 36f) {
                drawFooter(pageNum)
                pdf.finishPage(page)
                pageNum++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = page.canvas
                y = margin
                repeatHeader?.invoke()
            }
        }

        val generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val userCount = entries.count { it.actorType.equals(BreadcrumbActor.USER.label, ignoreCase = true) }
        val systemCount = entries.count { it.actorType.equals(BreadcrumbActor.SYSTEM.label, ignoreCase = true) }
        val alertCount = entries.count {
            it.severity.equals(BreadcrumbSeverity.WARNING.label, ignoreCase = true) ||
                it.severity.equals(BreadcrumbSeverity.ERROR.label, ignoreCase = true)
        }

        canvas.drawText("10K AGRI FEEDS – ACTIVITY HISTORY", margin, y + 14f, titlePaint)
        y += 20f
        canvas.drawText("Scope: $scopeLabel  |  Generated: $generatedAt", margin, y + 9f, subtitlePaint)
        y += 16f

        val historyKpi = "Events Exported: ${entries.size}   |   Your Actions: $userCount   |   System Events: $systemCount   |   Alerts: $alertCount"
        val wrappedHistoryKpis = wrapPdfLines(historyKpi, contentWidth - 20f, tableHeaderPaint)
        val boxLineHeight = 13f
        val boxHeight = 14f + wrappedHistoryKpis.size * boxLineHeight + 4f
        canvas.drawRoundRect(margin, y, margin + contentWidth, y + boxHeight, 6f, 6f, fillBoxPaint)
        wrappedHistoryKpis.forEachIndexed { idx, line ->
            canvas.drawText(line, margin + 10f, y + 15f + idx * boxLineHeight, tableHeaderPaint)
        }
        y += boxHeight + 14f

        fun drawEntriesHeader() {
            ensureSpace(24f)
            canvas.drawText("CHRONOLOGICAL AUDIT TRAIL (${entries.size})", margin, y + 10f, sectionHeaderPaint)
            y += 16f
            canvas.drawRect(margin, y, margin + contentWidth, y + 16f, fillBoxPaint)
            canvas.drawText("Date & Time", margin + 4f, y + 11f, tableHeaderPaint)
            canvas.drawText("Who / Area", margin + 105f, y + 11f, tableHeaderPaint)
            canvas.drawText("Event & Details", margin + 205f, y + 11f, tableHeaderPaint)
            y += 20f
        }

        drawEntriesHeader()
        if (entries.isEmpty()) {
            canvas.drawText("No activity history entries matched the export scope.", margin + 4f, y + 10f, mutedPaint)
        } else {
            entries.forEach { e ->
                val hasDetails = e.details.isNotBlank()
                val rowHeight = if (hasDetails) 26f else 15f
                ensureSpace(rowHeight + 4f) { drawEntriesHeader() }
                val stamp = "${e.dateStr} ${e.timeLabel}".trim()
                val whoArea = "${if (e.actorType.equals("User", true)) "You" else "System"} • ${e.category}"
                val actionLine = if (e.action.length > 56) e.action.take(53) + "..." else e.action
                canvas.drawText(stamp, margin + 4f, y + 9f, mutedPaint)
                canvas.drawText(
                    if (whoArea.length > 18) whoArea.take(16) + ".." else whoArea,
                    margin + 105f,
                    y + 9f,
                    bodyPaint
                )
                canvas.drawText(actionLine, margin + 205f, y + 9f, tableHeaderPaint)
                if (hasDetails) {
                    val detailLine = if (e.details.length > 68) e.details.take(65) + "..." else e.details
                    canvas.drawText(detailLine, margin + 205f, y + 20f, mutedPaint)
                }
                y += rowHeight
                canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                y += 2f
            }
        }

        drawFooter(pageNum)
        pdf.finishPage(page)

        val out = ByteArrayOutputStream()
        pdf.writeTo(out)
        pdf.close()
        return out.toByteArray()
    }

    fun buildBreadcrumbsXlsSpreadsheetXml(
        entries: List<BreadcrumbEntry>,
        scopeLabel: String = "All Activity"
    ): String {
        val userCount = entries.count { it.actorType.equals(BreadcrumbActor.USER.label, ignoreCase = true) }
        val systemCount = entries.count { it.actorType.equals(BreadcrumbActor.SYSTEM.label, ignoreCase = true) }
        val alertCount = entries.count {
            it.severity.equals(BreadcrumbSeverity.WARNING.label, ignoreCase = true) ||
                it.severity.equals(BreadcrumbSeverity.ERROR.label, ignoreCase = true)
        }

        val overviewRows = listOf(
            XlsRow(listOf(XlsCell.Title("10K Agri Feeds - Activity History"))),
            XlsRow(listOf(XlsCell.Subtitle("Scope: $scopeLabel | Exported: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}"))),
            XlsRow(emptyList()),
            XlsRow(listOf(XlsCell.Header("Metric"), XlsCell.Header("Value"))),
            XlsRow(listOf(XlsCell.Text("Total Events"), XlsCell.IntNum(entries.size.toLong()))),
            XlsRow(listOf(XlsCell.Text("User Actions"), XlsCell.IntNum(userCount.toLong()))),
            XlsRow(listOf(XlsCell.Text("System Events"), XlsCell.IntNum(systemCount.toLong()))),
            XlsRow(listOf(XlsCell.Text("Warnings & Errors"), XlsCell.IntNum(alertCount.toLong())))
        )

        val historyRows = buildList {
            add(
                XlsRow(
                    listOf(
                        "Date", "Time", "Actor", "Category", "Severity",
                        "Screen", "Action", "Details", "Reference", "Navigation Trail"
                    ).map { XlsCell.Header(it) }
                )
            )
            entries.forEach { e ->
                add(
                    XlsRow(
                        listOf(
                            XlsCell.Text(e.dateStr),
                            XlsCell.Text(e.timeLabel),
                            XlsCell.Text(e.actorType),
                            XlsCell.Text(e.category),
                            XlsCell.Text(e.severity),
                            XlsCell.Text(e.screenTab),
                            XlsCell.Text(e.action),
                            XlsCell.Text(e.details),
                            XlsCell.Text(e.entityRef),
                            XlsCell.Text(e.trailPath)
                        )
                    )
                )
            }
        }

        return buildSpreadsheetWorkbook(
            listOf(
                XlsSheet("Overview", overviewRows),
                XlsSheet("Activity History", historyRows)
            )
        )
    }

    fun saveBreadcrumbsToDownloads(
        context: Context,
        entries: List<BreadcrumbEntry>,
        format: ExportFileFormat,
        scopeLabel: String = "All Activity"
    ): PdfSaveResult {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Bulacan_Activity_History_$stamp.${format.extension}"
        val bytes = when (format) {
            ExportFileFormat.CSV -> buildBreadcrumbsCsv(entries).toByteArray(Charsets.UTF_8)
            ExportFileFormat.PDF -> generateBreadcrumbsPdfBytes(entries, scopeLabel)
            ExportFileFormat.XLS -> buildBreadcrumbsXlsSpreadsheetXml(entries, scopeLabel).toByteArray(Charsets.UTF_8)
        }
        return saveBytesToDownloads(context, fileName, format.mimeType, bytes)
    }
}
