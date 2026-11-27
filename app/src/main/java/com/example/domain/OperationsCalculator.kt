package com.example.domain

import com.example.model.CustomerOrder
import com.example.model.DailyOrderLogRow
import com.example.model.DailyPlantLog
import com.example.model.DailySkuLogRow
import com.example.model.DispatchRecord
import com.example.model.DispatchStatus
import com.example.model.FeedBomEntity
import com.example.model.OrderStatus
import com.example.model.PlantKpis
import com.example.model.PlantReportData
import com.example.model.PlantSection
import com.example.model.ProductReportRow
import com.example.model.ProductionBatch
import com.example.model.RawMaterialConsumptionDay
import com.example.model.RawMaterialEntity
import com.example.model.RawMaterialProjection
import com.example.model.RawMaterialRiskStatus
import com.example.model.ReportSectionOption
import com.example.model.ReprocessItem
import com.example.model.SchedulePriority
import com.example.model.ScheduleRun
import com.example.model.ScheduleStatus
import com.example.model.SectionReportSummary
import com.example.model.SkuEntity
import com.example.model.SkuInventorySummary
import com.example.model.StockHealthStatus
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

object OperationsCalculator {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun parseDateOrNull(dateStr: String): LocalDate? = try {
        LocalDate.parse(dateStr, dateFormatter)
    } catch (_: Exception) {
        null
    }

    fun computeSkuSummaries(
        skus: List<SkuEntity>,
        batches: List<ProductionBatch>,
        dispatches: List<DispatchRecord>,
        orders: List<CustomerOrder>,
        lookbackDays: Int,
        globalSafetyBufferPct: Double? = null,
        reprocessItems: List<ReprocessItem> = emptyList()
    ): List<SkuInventorySummary> {
        val allDates = (dispatches.mapNotNull { parseDateOrNull(it.date) } +
            batches.mapNotNull { parseDateOrNull(it.date) })
        val referenceEnd = allDates.maxOrNull() ?: LocalDate.of(2026, 11, 19)
        val lookbackStart = referenceEnd.minusDays((lookbackDays.coerceAtLeast(1) - 1).toLong())

        return skus.map { sku ->
            val skuBatches = batches.filter { it.productName.equals(sku.productName, ignoreCase = true) }
            val skuDispatches = dispatches.filter { it.productName.equals(sku.productName, ignoreCase = true) }
            val skuReprocess = reprocessItems.filter { it.productName.equals(sku.productName, ignoreCase = true) }
            val skuOpenOrders = orders.filter {
                it.productName.equals(sku.productName, ignoreCase = true) &&
                    (it.status == OrderStatus.OPEN.label || it.status == OrderStatus.PARTIALLY_DISPATCHED.label)
            }

            val recoveredKg = skuBatches.sumOf { it.actualRecoveredKg }
            val dispatchedKg = skuDispatches.sumOf { it.effectiveDispatchedWeightKg() }

            val pendingReprocessKg = skuReprocess.filter { it.isPending() }.sumOf { it.returnedWeightKg }
            val pendingReprocessBags = skuReprocess.filter { it.isPending() }.sumOf { it.returnedBags }
            val pendingFastActionCount = skuReprocess.count { it.isPending() && it.fastActionNeeded }
            val damagedReprocessKg = skuReprocess.filter { it.isDamaged() }.sumOf { it.returnedWeightKg }
            val damagedReprocessBags = skuReprocess.filter { it.isDamaged() }.sumOf { it.returnedBags }
            val nonRestoredLinkedQuarantineKg = skuReprocess
                .filter { !it.flaggedToInventory && it.sourceDispatchId.isNotBlank() }
                .sumOf { it.returnedWeightKg }
            val flaggedLinkedDeltaKg = skuReprocess
                .filter { it.flaggedToInventory && it.sourceDispatchId.isNotBlank() }
                .sumOf { it.effectiveReprocessedWeightKg() - it.returnedWeightKg }
            val flaggedManualAddKg = skuReprocess
                .filter { it.flaggedToInventory && it.sourceDispatchId.isBlank() }
                .sumOf { it.effectiveReprocessedWeightKg() }
            val reprocessedKg = skuReprocess
                .filter { it.flaggedToInventory }
                .sumOf { it.effectiveReprocessedWeightKg() }
            val reprocessedBags = skuReprocess
                .filter { it.flaggedToInventory }
                .sumOf { it.effectiveReprocessedBags() }

            val availableStockKg = sku.openingStockKg + recoveredKg - dispatchedKg -
                nonRestoredLinkedQuarantineKg + flaggedLinkedDeltaKg + flaggedManualAddKg
            val bagWt = sku.standardBagWeightKg.coerceAtLeast(0.1)
            val availableBags = availableStockKg / bagWt

            val recentDispatchedKg = skuDispatches.filter { d ->
                val parsed = parseDateOrNull(d.date)
                parsed != null && !parsed.isBefore(lookbackStart) && !parsed.isAfter(referenceEnd)
            }.sumOf { it.effectiveDispatchedWeightKg() }

            val avgDailyDispatchKg = recentDispatchedKg / lookbackDays.coerceAtLeast(1)
            val daysOfCover = if (avgDailyDispatchKg > 0.0) {
                max(0.0, availableStockKg) / avgDailyDispatchKg
            } else {
                Double.POSITIVE_INFINITY
            }

            val openOrdersKg = skuOpenOrders.sumOf { max(0.0, it.totalWeightKg - it.dispatchedWeightKg) }
            val bufferPct = globalSafetyBufferPct ?: sku.safetyBufferPct
            val demandBasis = max(openOrdersKg, avgDailyDispatchKg * 3.0)
            val safetyBufferKg = demandBasis * (bufferPct / 100.0)
            val netProductionNeededKg = max(0.0, (openOrdersKg + safetyBufferKg) - availableStockKg)
            val suggestedBags = if (netProductionNeededKg > 0.0) {
                ceil(netProductionNeededKg / bagWt).toLong()
            } else {
                0L
            }

            val totalExpected = skuBatches.sumOf { it.expectedWeightKg }
            val avgRecoveryPct = if (totalExpected > 0.0) {
                (recoveredKg / totalExpected) * 100.0
            } else {
                100.0
            }

            val status = when {
                availableStockKg <= 0.0 ||
                    availableStockKg < openOrdersKg ||
                    (avgDailyDispatchKg > 0.0 && daysOfCover < 3.0) -> StockHealthStatus.CRITICAL
                netProductionNeededKg > 0.0 ||
                    (avgDailyDispatchKg > 0.0 && daysOfCover < 7.0) -> StockHealthStatus.LOW_WATCH
                else -> StockHealthStatus.HEALTHY
            }

            val priority = when {
                status == StockHealthStatus.CRITICAL -> SchedulePriority.URGENT
                openOrdersKg > availableStockKg -> SchedulePriority.ORDER_DRIVEN
                netProductionNeededKg > 0.0 -> SchedulePriority.BUFFER_TOP_UP
                else -> null
            }

            SkuInventorySummary(
                sku = sku,
                openingStockKg = sku.openingStockKg,
                recoveredKg = recoveredKg,
                dispatchedKg = dispatchedKg,
                availableStockKg = availableStockKg,
                availableBags = availableBags,
                avgDailyDispatchKg = avgDailyDispatchKg,
                daysOfCover = daysOfCover,
                openOrdersKg = openOrdersKg,
                safetyBufferKg = safetyBufferKg,
                netProductionNeededKg = netProductionNeededKg,
                suggestedBags = suggestedBags,
                avgRecoveryPct = avgRecoveryPct,
                recommendedPriority = priority,
                status = status,
                pendingReprocessKg = pendingReprocessKg,
                pendingReprocessBags = pendingReprocessBags,
                pendingFastActionCount = pendingFastActionCount,
                reprocessedKg = reprocessedKg,
                reprocessedBags = reprocessedBags,
                hasReprocessedFlag = reprocessedKg > 0.0 || reprocessedBags > 0L,
                damagedReprocessKg = damagedReprocessKg,
                damagedReprocessBags = damagedReprocessBags
            )
        }.sortedWith(
            compareBy<SkuInventorySummary>(
                { it.recommendedPriority?.sortRank ?: 99 },
                { -it.netProductionNeededKg },
                { -it.availableStockKg },
                { it.sku.section },
                { it.sku.productName }
            )
        )
    }

    fun computeKpis(
        summaries: List<SkuInventorySummary>,
        batches: List<ProductionBatch>,
        dispatches: List<DispatchRecord>
    ): PlantKpis {
        val totalAvailKg = summaries.sumOf { max(0.0, it.availableStockKg) }
        val totalAvailBags = summaries.sumOf { max(0.0, it.availableBags) }
        val totalDispKg = dispatches.sumOf { it.effectiveDispatchedWeightKg() }
        val totalOpenOrdersKg = summaries.sumOf { it.openOrdersKg }

        val totalExpected = batches.sumOf { it.expectedWeightKg }
        val totalActual = batches.sumOf { it.actualRecoveredKg }
        val avgRecovery = if (totalExpected > 0.0) (totalActual / totalExpected) * 100.0 else 100.0

        val needingProd = summaries.count { it.netProductionNeededKg > 0.0 }
        val criticalCount = summaries.count { it.status == StockHealthStatus.CRITICAL }

        return PlantKpis(
            totalAvailableStockKg = totalAvailKg,
            totalAvailableBags = totalAvailBags,
            totalDispatchedKg = totalDispKg,
            totalOpenOrdersKg = totalOpenOrdersKg,
            averageRecoveryPct = avgRecovery,
            skusNeedingProduction = needingProd,
            criticalSkuCount = criticalCount
        )
    }

    fun buildReport(
        startDate: String,
        endDate: String,
        sectionFilter: String,
        skus: List<SkuEntity>,
        batches: List<ProductionBatch>,
        dispatches: List<DispatchRecord>,
        orders: List<CustomerOrder>,
        summaries: List<SkuInventorySummary>,
        schedules: List<ScheduleRun> = emptyList(),
        includedSections: Set<ReportSectionOption> = ReportSectionOption.entries.toSet()
    ): PlantReportData {
        val rawStart = parseDateOrNull(startDate)
        val rawEnd = parseDateOrNull(endDate)
        val start = if (rawStart != null && rawEnd != null && rawStart.isAfter(rawEnd)) rawEnd else rawStart
        val end = if (rawStart != null && rawEnd != null && rawStart.isAfter(rawEnd)) rawStart else rawEnd

        fun inRange(dateStr: String): Boolean {
            val d = parseDateOrNull(dateStr) ?: return (start == null && end == null)
            if (start != null && d.isBefore(start)) return false
            if (end != null && d.isAfter(end)) return false
            return true
        }

        val sectionByProdLower = skus.associate { it.productName.lowercase() to it.section }

        val filteredBatches = batches.filter {
            inRange(it.date) && (sectionFilter == "ALL" || it.section == sectionFilter)
        }.sortedWith(compareBy<ProductionBatch>({ it.date }, { it.section }, { it.productName }))

        val filteredDispatches = dispatches.filter { d ->
            val skuSection = sectionByProdLower[d.productName.lowercase()]
            inRange(d.date) && (sectionFilter == "ALL" || skuSection == sectionFilter)
        }.sortedWith(compareBy<DispatchRecord>({ it.date }, { it.drReference }, { it.customer }))

        val filteredSchedules = schedules.filter { s ->
            inRange(s.scheduledDate) && (sectionFilter == "ALL" || s.section == sectionFilter)
        }.sortedWith(compareBy<ScheduleRun>({ it.scheduledDate }, { SchedulePriority.fromRaw(it.priority).sortRank }, { it.productName }))

        val filteredOrdersInRange = orders.filter { o ->
            val skuSection = sectionByProdLower[o.productName.lowercase()]
            (inRange(o.orderDate) || inRange(o.dueDate)) &&
                (sectionFilter == "ALL" || skuSection == sectionFilter)
        }.sortedWith(compareBy<CustomerOrder>({ !it.priority }, { it.dueDate }, { it.orderCode }))

        val filteredSummaries = summaries.filter { sectionFilter == "ALL" || it.sku.section == sectionFilter }
        val availByProdLower = filteredSummaries.associate { it.sku.productName.lowercase() to max(0.0, it.availableStockKg) }
        val activeFilteredOrders = orders.filter { o ->
            val skuSection = sectionByProdLower[o.productName.lowercase()]
            (o.status == OrderStatus.OPEN.label || o.status == OrderStatus.PARTIALLY_DISPATCHED.label) &&
                (sectionFilter == "ALL" || skuSection == sectionFilter)
        }
        val activeOrderShortfallKg = activeFilteredOrders
            .groupBy { it.productName.lowercase() }
            .entries
            .sumOf { (prodLower, list) ->
                val rem = list.sumOf { max(0.0, it.totalWeightKg - it.dispatchedWeightKg) }
                val avail = availByProdLower[prodLower] ?: 0.0
                max(0.0, rem - avail)
            }

        val summaryMap = summaries.associateBy { it.sku.productName }
        val sectionSummaries = PlantSection.entries
            .filter { sectionFilter == "ALL" || it.label == sectionFilter }
            .map { sec ->
                val secBatches = filteredBatches.filter { it.section == sec.label }
                val secSummaries = summaries.filter { it.sku.section == sec.label }
                val bags = secBatches.sumOf { it.bagsProduced }
                val exp = secBatches.sumOf { it.expectedWeightKg }
                val rec = secBatches.sumOf { it.actualRecoveredKg }
                val varKg = secBatches.sumOf { it.varianceKg }
                val recPct = if (exp > 0.0) (rec / exp) * 100.0 else 100.0
                SectionReportSummary(
                    section = sec,
                    totalBags = bags,
                    totalExpectedKg = exp,
                    totalRecoveredKg = rec,
                    totalVarianceKg = varKg,
                    recoveryPct = recPct,
                    batches = secBatches,
                    skuCount = secSummaries.size,
                    totalAvailableStockKg = secSummaries.sumOf { max(0.0, it.availableStockKg) },
                    totalAvailableBags = secSummaries.sumOf { max(0.0, it.availableBags) }
                )
            }
        val productRows = skus
            .filter { sectionFilter == "ALL" || it.section == sectionFilter }
            .map { sku ->
                val pBatches = filteredBatches.filter { it.productName == sku.productName }
                val pDispatches = filteredDispatches.filter { it.productName == sku.productName }
                val pOrders = orders.filter {
                    it.productName == sku.productName &&
                        (it.status == OrderStatus.OPEN.label || it.status == OrderStatus.PARTIALLY_DISPATCHED.label)
                }
                ProductReportRow(
                    productName = sku.productName,
                    section = sku.section,
                    producedBags = pBatches.sumOf { it.bagsProduced },
                    recoveredKg = pBatches.sumOf { it.actualRecoveredKg },
                    varianceKg = pBatches.sumOf { it.varianceKg },
                    dispatchedKg = pDispatches.sumOf { it.effectiveDispatchedWeightKg() },
                    openOrdersKg = pOrders.sumOf { max(0.0, it.totalWeightKg - it.dispatchedWeightKg) },
                    availableStockKg = summaryMap[sku.productName]?.availableStockKg ?: sku.openingStockKg,
                    availableBags = summaryMap[sku.productName]?.availableBags
                        ?: (sku.openingStockKg / sku.standardBagWeightKg.coerceAtLeast(1.0))
                )
            }

        val deliveredCount = filteredDispatches.count { it.resolvedStatusLabel() == DispatchStatus.DELIVERED.label }
        val returnedDispatches = filteredDispatches.filter { it.resolvedStatusLabel() == DispatchStatus.RETURNED.label }
        val heldCount = filteredDispatches.count { it.resolvedStatusLabel() == DispatchStatus.HELD.label }
        val activeSchedules = filteredSchedules.filter {
            it.status == ScheduleStatus.SCHEDULED.label || it.status == ScheduleStatus.IN_PROGRESS.label
        }

        val normStart = start?.format(dateFormatter) ?: startDate
        val normEnd = end?.format(dateFormatter) ?: endDate
        val effectiveIncluded = includedSections.ifEmpty { ReportSectionOption.entries.toSet() }
        val sortedActiveOrders = activeFilteredOrders.sortedWith(
            compareBy<CustomerOrder>({ !it.priority }, { it.dueDate }, { it.orderCode })
        )

        return PlantReportData(
            startDate = normStart,
            endDate = normEnd,
            sectionFilter = sectionFilter,
            sectionSummaries = sectionSummaries,
            productRows = productRows,
            totalProducedBags = filteredBatches.sumOf { it.bagsProduced },
            totalRecoveredKg = filteredBatches.sumOf { it.actualRecoveredKg },
            totalDispatchedKg = filteredDispatches.sumOf { it.effectiveDispatchedWeightKg() },
            totalOpenOrdersKg = productRows.sumOf { it.openOrdersKg },
            isDailyReport = normStart == normEnd,
            dispatches = filteredDispatches,
            orders = filteredOrdersInRange,
            activeOpenOrders = sortedActiveOrders,
            schedules = filteredSchedules,
            activeOrderShortfallKg = activeOrderShortfallKg,
            deliveredCount = deliveredCount,
            returnedCount = returnedDispatches.size,
            heldCount = heldCount,
            returnedKg = returnedDispatches.sumOf { it.returnedWeightKg },
            scheduledQueueBags = activeSchedules.sumOf { it.targetBags },
            scheduledActiveCount = activeSchedules.size,
            scheduledCompletedCount = filteredSchedules.count { it.status == ScheduleStatus.COMPLETED.label },
            healthySkuCount = filteredSummaries.count { it.status == StockHealthStatus.HEALTHY },
            watchSkuCount = filteredSummaries.count { it.status == StockHealthStatus.LOW_WATCH },
            criticalSkuCount = filteredSummaries.count { it.status == StockHealthStatus.CRITICAL },
            includedSections = effectiveIncluded
        )
    }

    fun todayIsoDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun todayPlusDaysIso(days: Long): String = try {
        LocalDate.now().plusDays(days).format(dateFormatter)
    } catch (_: Exception) {
        todayIsoDate()
    }

    fun utcMillisToIsoDate(millis: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return fmt.format(Date(millis))
    }

    fun isoDateToUtcMillis(isoDate: String): Long? = try {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }
        fmt.parse(isoDate.trim())?.time
    } catch (_: Exception) {
        null
    }

    fun formatKg(value: Double): String = String.format("%,.2f", value)

    fun formatBags(value: Double): String {
        val rounded = (value * 10.0).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) String.format("%,d", rounded.toLong())
        else String.format("%,.1f", rounded)
    }

    fun timestampToLocalIsoDate(ts: Timestamp?): String? {
        val d = ts?.toDate() ?: return null
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d)
    }

    fun timestampToLocalTimeLabel(ts: Timestamp?): String {
        val d = ts?.toDate() ?: return ""
        return SimpleDateFormat("HH:mm", Locale.US).format(d)
    }

    fun computeDailyLogs(
        skus: List<SkuEntity>,
        batches: List<ProductionBatch>,
        dispatches: List<DispatchRecord>,
        orders: List<CustomerOrder>,
        todayIso: String = todayIsoDate()
    ): List<DailyPlantLog> {
        val dateSet = linkedSetOf<String>()
        if (parseDateOrNull(todayIso) != null) dateSet.add(todayIso)
        batches.forEach { b ->
            if (parseDateOrNull(b.date) != null) dateSet.add(b.date)
            timestampToLocalIsoDate(b.createdAt)?.let { if (parseDateOrNull(it) != null) dateSet.add(it) }
            timestampToLocalIsoDate(b.updatedAt)?.let { if (parseDateOrNull(it) != null) dateSet.add(it) }
        }
        dispatches.forEach { d ->
            if (parseDateOrNull(d.date) != null) dateSet.add(d.date)
            timestampToLocalIsoDate(d.createdAt)?.let { if (parseDateOrNull(it) != null) dateSet.add(it) }
            timestampToLocalIsoDate(d.updatedAt)?.let { if (parseDateOrNull(it) != null) dateSet.add(it) }
        }
        orders.forEach { o ->
            if (parseDateOrNull(o.orderDate) != null) dateSet.add(o.orderDate)
            if (parseDateOrNull(o.dueDate) != null) dateSet.add(o.dueDate)
            timestampToLocalIsoDate(o.createdAt)?.let { if (parseDateOrNull(it) != null) dateSet.add(it) }
            timestampToLocalIsoDate(o.updatedAt)?.let { if (parseDateOrNull(it) != null) dateSet.add(it) }
        }
        skus.forEach { s ->
            timestampToLocalIsoDate(s.createdAt)?.let { if (parseDateOrNull(it) != null) dateSet.add(it) }
            timestampToLocalIsoDate(s.updatedAt)?.let { if (parseDateOrNull(it) != null) dateSet.add(it) }
        }

        val sortedDates = dateSet.sortedDescending()
        val batchesByProd = batches.groupBy { it.productName.lowercase() }
        val dispatchesByProd = dispatches.groupBy { it.productName.lowercase() }
        val activeOrders = orders.filter {
            it.status == OrderStatus.OPEN.label || it.status == OrderStatus.PARTIALLY_DISPATCHED.label
        }
        val activeOrdersByProd = activeOrders.groupBy { it.productName.lowercase() }
        val dispatchesByDate = dispatches.groupBy { it.date }

        return sortedDates.map { day ->
            val isToday = day == todayIso
            val dayDate = parseDateOrNull(day)
            val dayDispatches = dispatchesByDate[day].orEmpty()
            val dispatchesByOrderIdOnDay = dayDispatches
                .filter { it.linkedOrderId.isNotBlank() }
                .groupBy { it.linkedOrderId }

            val skuRows = skus.map { sku ->
                val prodKey = sku.productName.lowercase()
                val pBatches = batchesByProd[prodKey].orEmpty()
                val pDispatches = dispatchesByProd[prodKey].orEmpty()

                val cumRecovered = pBatches
                    .filter { b -> parseDateOrNull(b.date)?.let { dayDate != null && !it.isAfter(dayDate) } == true }
                    .sumOf { it.actualRecoveredKg }
                val cumDispatched = pDispatches
                    .filter { d -> parseDateOrNull(d.date)?.let { dayDate != null && !it.isAfter(dayDate) } == true }
                    .sumOf { it.effectiveDispatchedWeightKg() }

                val closingStockKg = sku.openingStockKg + cumRecovered - cumDispatched
                val bagWt = sku.standardBagWeightKg.coerceAtLeast(0.1)
                val closingBags = closingStockKg / bagWt

                val dayBatchesForSku = pBatches.filter { it.date == day }
                val dayDispatchesForSku = pDispatches.filter { it.date == day }
                val producedDayKg = dayBatchesForSku.sumOf { it.actualRecoveredKg }
                val producedDayBags = dayBatchesForSku.sumOf { it.bagsProduced }
                val dispatchedDayKg = dayDispatchesForSku.sumOf { it.effectiveDispatchedWeightKg() }
                val dispatchedDayBags = dayDispatchesForSku.sumOf { it.bagsDispatched }
                val returnedDayKg = dayDispatchesForSku
                    .filter { it.returnConfirmed && it.returnedWeightKg > 0.0 }
                    .sumOf { it.returnedWeightKg }

                val openOrdersSnapshotKg = activeOrdersByProd[prodKey].orEmpty()
                    .filter { ord ->
                        isToday || parseDateOrNull(ord.orderDate)?.let { dayDate != null && !it.isAfter(dayDate) } == true
                    }
                    .sumOf { max(0.0, it.totalWeightKg - it.dispatchedWeightKg) }

                val skuUpdDate = timestampToLocalIsoDate(sku.updatedAt)
                val batchTouchedOnDay = pBatches.any {
                    timestampToLocalIsoDate(it.updatedAt) == day || timestampToLocalIsoDate(it.createdAt) == day
                }
                val dispatchTouchedOnDay = pDispatches.any {
                    timestampToLocalIsoDate(it.updatedAt) == day || timestampToLocalIsoDate(it.createdAt) == day
                }
                val editedOnDay = skuUpdDate == day || batchTouchedOnDay || dispatchTouchedOnDay
                val latestTouchTs = listOfNotNull(
                    sku.updatedAt?.takeIf { timestampToLocalIsoDate(it) == day },
                    *pBatches.mapNotNull { it.updatedAt?.takeIf { ts -> timestampToLocalIsoDate(ts) == day } }.toTypedArray(),
                    *pDispatches.mapNotNull { it.updatedAt?.takeIf { ts -> timestampToLocalIsoDate(ts) == day } }.toTypedArray()
                ).maxByOrNull { it.toDate().time }
                val editedTime = if (editedOnDay) timestampToLocalTimeLabel(latestTouchTs) else ""

                DailySkuLogRow(
                    sku = sku,
                    closingStockKg = closingStockKg,
                    closingBags = closingBags,
                    producedDayKg = producedDayKg,
                    producedDayBags = producedDayBags,
                    dispatchedDayKg = dispatchedDayKg,
                    dispatchedDayBags = dispatchedDayBags,
                    returnedDayKg = returnedDayKg,
                    openOrdersSnapshotKg = openOrdersSnapshotKg,
                    editedOnDay = editedOnDay,
                    editedTimeLabel = editedTime
                )
            }.sortedWith(
                compareByDescending<DailySkuLogRow> { it.editedOnDay }
                    .thenByDescending { it.dispatchedDayKg + it.producedDayKg }
                    .thenBy { it.sku.section }
                    .thenBy { it.sku.productName }
            )

            val orderRows = orders.mapNotNull { order ->
                val placedOnDay = order.orderDate == day
                val dueOnDay = order.dueDate == day
                val ordUpdDate = timestampToLocalIsoDate(order.updatedAt)
                val editedOnDay = ordUpdDate == day
                val dayOrderDispatches = dispatchesByOrderIdOnDay[order.id].orEmpty()
                val dispKg = dayOrderDispatches.sumOf { it.effectiveDispatchedWeightKg() }
                val dispBags = dayOrderDispatches.sumOf { it.bagsDispatched }
                if (placedOnDay || dueOnDay || editedOnDay || dayOrderDispatches.isNotEmpty()) {
                    DailyOrderLogRow(
                        order = order,
                        placedOnDay = placedOnDay,
                        dueOnDay = dueOnDay,
                        editedOnDay = editedOnDay,
                        editedTimeLabel = if (editedOnDay) timestampToLocalTimeLabel(order.updatedAt) else "",
                        dispatchedOnDayKg = dispKg,
                        dispatchedOnDayBags = dispBags
                    )
                } else {
                    null
                }
            }.sortedWith(
                compareByDescending<DailyOrderLogRow> { it.order.priority }
                    .thenByDescending { it.editedOnDay }
                    .thenByDescending { it.placedOnDay }
                    .thenBy { it.order.orderCode }
            )

            val totalClosingKg = skuRows.sumOf { max(0.0, it.closingStockKg) }
            val totalClosingBags = skuRows.sumOf { max(0.0, it.closingBags) }
            val totalProdDayKg = skuRows.sumOf { it.producedDayKg }
            val totalDispDayKg = skuRows.sumOf { it.dispatchedDayKg }
            val totalOrdersLoggedKg = orderRows.sumOf { it.order.totalWeightKg }
            val totalOpenSnapshotKg = skuRows.sumOf { it.openOrdersSnapshotKg }

            DailyPlantLog(
                date = day,
                isToday = isToday,
                skuRows = skuRows,
                orderRows = orderRows,
                totalClosingStockKg = totalClosingKg,
                totalClosingBags = totalClosingBags,
                totalProducedDayKg = totalProdDayKg,
                totalDispatchedDayKg = totalDispDayKg,
                totalOrdersLoggedKg = totalOrdersLoggedKg,
                totalOpenOrdersSnapshotKg = totalOpenSnapshotKg,
                skusEditedCount = skuRows.count { it.editedOnDay },
                ordersEditedCount = orderRows.count { it.editedOnDay },
                ordersPlacedCount = orderRows.count { it.placedOnDay },
                ordersDueCount = orderRows.count { it.dueOnDay },
                linkedDispatchOrdersCount = orderRows.count { it.dispatchedOnDayKg > 0.0 || it.dispatchedOnDayBags > 0L }
            )
        }
    }

    fun computeRawMaterialForecasts(
        materials: List<RawMaterialEntity>,
        batches: List<ProductionBatch>,
        schedules: List<ScheduleRun> = emptyList(),
        lookbackDays: Int = 14,
        demandMultiplier: Double = 1.0,
        includeScheduledDemand: Boolean = false,
        leadTimeOverrideDays: Long? = null,
        boms: List<FeedBomEntity> = emptyList()
    ): List<RawMaterialProjection> {
        if (materials.isEmpty()) return emptyList()

        val safeLookback = lookbackDays.coerceIn(1, 90)
        val safeMultiplier = demandMultiplier.coerceIn(0.25, 3.0)
        val today = parseDateOrNull(todayIsoDate()) ?: LocalDate.of(2026, 11, 23)
        val batchDates = batches.mapNotNull { parseDateOrNull(it.date) }
        val referenceEnd = batchDates.maxOrNull() ?: today
        val lookbackStart = referenceEnd.minusDays((safeLookback - 1).toLong())

        val recentBatches = batches.filter { b ->
            val d = parseDateOrNull(b.date)
            d != null && !d.isBefore(lookbackStart) && !d.isAfter(referenceEnd)
        }

        // Fall back to all historical batches if the recent rolling window has zero logged batches
        val effectiveBatches = if (recentBatches.isNotEmpty()) recentBatches else batches
        val effectiveSpanDays = if (recentBatches.isNotEmpty()) {
            safeLookback.toDouble()
        } else {
            val minD = batchDates.minOrNull()
            val maxD = batchDates.maxOrNull()
            if (minD != null && maxD != null) {
                (java.time.temporal.ChronoUnit.DAYS.between(minD, maxD) + 1L).coerceAtLeast(1L).toDouble()
            } else {
                safeLookback.toDouble()
            }
        }

        val bomByProductLower = boms.associateBy { it.productName.trim().lowercase() }
        val activeSchedules = if (includeScheduledDemand) {
            schedules.filter {
                it.status == ScheduleStatus.SCHEDULED.label || it.status == ScheduleStatus.IN_PROGRESS.label
            }
        } else {
            emptyList()
        }

        val batchesByDate = batches.groupBy { it.date }
        val shortFmt = DateTimeFormatter.ofPattern("MMM d", Locale.US)
        val chartDates = (0 until 7).map { offset ->
            referenceEnd.minusDays((6 - offset).toLong())
        }

        return materials.map { mat ->
            val fallbackShareFrac = (mat.usageSharePct / 100.0).coerceIn(0.0001, 1.0)
            fun shareForProduct(productName: String): Double {
                val bom = bomByProductLower[productName.trim().lowercase()] ?: return fallbackShareFrac
                val matchedLine = bom.lines.firstOrNull { line ->
                    (mat.id.isNotBlank() && line.materialId == mat.id) ||
                        line.materialName.equals(mat.name, ignoreCase = true)
                }
                return matchedLine?.let { (it.inclusionPct / 100.0).coerceIn(0.0, 1.0) } ?: 0.0
            }

            val recentMaterialConsumedKg = effectiveBatches.sumOf { b ->
                val batchFeedKg = max(b.expectedWeightKg, b.actualRecoveredKg).coerceAtLeast(0.0)
                batchFeedKg * shareForProduct(b.productName)
            }
            val historicalAvgDailyKg = if (effectiveSpanDays > 0.0) {
                recentMaterialConsumedKg / effectiveSpanDays
            } else {
                0.0
            }
            val scheduledMaterialKg = activeSchedules.sumOf { sched ->
                max(0.0, sched.expectedWeightKg) * shareForProduct(sched.productName)
            }
            // Spread planned schedule load across a 7-day horizon when blended in
            val scheduledDailyAddKg = if (includeScheduledDemand && scheduledMaterialKg > 0.0) {
                scheduledMaterialKg / 7.0
            } else {
                0.0
            }
            val simulatedAvgDailyKg = (historicalAvgDailyKg * safeMultiplier) + scheduledDailyAddKg

            val effectiveLead = (leadTimeOverrideDays ?: mat.leadTimeDays).coerceIn(1L, 60L)
            val totalWatchHorizonDays = (effectiveLead + mat.safetyDays.coerceAtLeast(0L)).toDouble()
            val netAvailableAfterScheduled = if (includeScheduledDemand) {
                max(0.0, mat.currentStockKg - scheduledMaterialKg)
            } else {
                max(0.0, mat.currentStockKg)
            }

            val daysUntilRunOut = if (simulatedAvgDailyKg > 0.01) {
                max(0.0, mat.currentStockKg) / simulatedAvgDailyKg
            } else {
                Double.POSITIVE_INFINITY
            }

            val projectedRunOutIsoDate = if (daysUntilRunOut.isFinite() && daysUntilRunOut <= 365.0) {
                today.plusDays(ceil(daysUntilRunOut).toLong()).format(dateFormatter)
            } else {
                null
            }

            val reorderTriggerKg = max(
                mat.reorderThresholdKg,
                simulatedAvgDailyKg * totalWatchHorizonDays
            )

            val targetRestockLevelKg = max(
                mat.reorderThresholdKg * 2.0,
                simulatedAvgDailyKg * (14.0 + effectiveLead)
            )
            val recommendedRestockKg = max(0.0, targetRestockLevelKg - mat.currentStockKg)

            val isOutOrBelowMin = mat.currentStockKg <= 0.0 || mat.currentStockKg <= mat.reorderThresholdKg
            val isCriticalByRunway = daysUntilRunOut.isFinite() && daysUntilRunOut <= effectiveLead.toDouble()
            val isCriticalBySchedule = includeScheduledDemand && scheduledMaterialKg > mat.currentStockKg
            val isWatchByRunway = daysUntilRunOut.isFinite() && daysUntilRunOut <= totalWatchHorizonDays
            val isWatchByBuffer = mat.currentStockKg <= reorderTriggerKg * 1.15 && simulatedAvgDailyKg > 0.01

            val status = when {
                isOutOrBelowMin || isCriticalByRunway || isCriticalBySchedule -> RawMaterialRiskStatus.CRITICAL
                isWatchByRunway || isWatchByBuffer || netAvailableAfterScheduled <= mat.reorderThresholdKg -> RawMaterialRiskStatus.LOW_WATCH
                else -> RawMaterialRiskStatus.HEALTHY
            }

            val statusReason = when {
                mat.currentStockKg <= 0.0 -> "Out of stock — replenishment needed immediately"
                isCriticalBySchedule -> "Scheduled runs need ${formatKg(scheduledMaterialKg)} kg vs ${formatKg(mat.currentStockKg)} kg on hand"
                isCriticalByRunway && mat.currentStockKg <= mat.reorderThresholdKg ->
                    "Below ${formatKg(mat.reorderThresholdKg)} kg minimum & runs out in ${String.format("%.1f", daysUntilRunOut)}d"
                isCriticalByRunway ->
                    "Projected run-out (${String.format("%.1f", daysUntilRunOut)}d) is inside ${effectiveLead}d delivery lead time"
                mat.currentStockKg <= mat.reorderThresholdKg ->
                    "Stock (${formatKg(mat.currentStockKg)} kg) is at or below ${formatKg(mat.reorderThresholdKg)} kg minimum"
                isWatchByRunway ->
                    "Projected to hit 0 in ${String.format("%.1f", daysUntilRunOut)}d (reorder window: ${totalWatchHorizonDays.toInt()}d)"
                isWatchByBuffer ->
                    "Approaching calculated reorder point (${formatKg(reorderTriggerKg)} kg)"
                simulatedAvgDailyKg <= 0.01 ->
                    "No recent production batches logged yet — above ${formatKg(mat.reorderThresholdKg)} kg minimum"
                else ->
                    "Covers ~${String.format("%.1f", daysUntilRunOut)} days at ${formatKg(simulatedAvgDailyKg)} kg/day"
            }

            val projectedRunOutLabel = when {
                mat.currentStockKg <= 0.0 -> "Out of stock"
                daysUntilRunOut.isInfinite() -> "No recent usage"
                daysUntilRunOut > 180.0 -> "> 180 days"
                else -> {
                    val dateLabel = parseDateOrNull(projectedRunOutIsoDate.orEmpty())?.format(shortFmt) ?: ""
                    val daysStr = String.format("%.1f", daysUntilRunOut)
                    if (dateLabel.isNotBlank()) "$dateLabel (${daysStr}d)" else "${daysStr}d left"
                }
            }

            val dailyTrend = chartDates.map { dateObj ->
                val iso = dateObj.format(dateFormatter)
                val dayBatches = batchesByDate[iso].orEmpty()
                val dayFeedKg = dayBatches.sumOf { b ->
                    max(b.expectedWeightKg, b.actualRecoveredKg).coerceAtLeast(0.0)
                } * safeMultiplier
                val dayMatKg = dayBatches.sumOf { b ->
                    val batchFeedKg = max(b.expectedWeightKg, b.actualRecoveredKg).coerceAtLeast(0.0)
                    batchFeedKg * shareForProduct(b.productName)
                } * safeMultiplier
                RawMaterialConsumptionDay(
                    isoDate = iso,
                    shortLabel = dateObj.format(shortFmt),
                    feedProducedKg = dayFeedKg,
                    materialConsumedKg = dayMatKg
                )
            }

            RawMaterialProjection(
                material = mat,
                historicalAvgDailyKg = historicalAvgDailyKg,
                simulatedAvgDailyKg = simulatedAvgDailyKg,
                recentTotalConsumedKg = recentMaterialConsumedKg,
                scheduledDemandKg = scheduledMaterialKg,
                daysUntilRunOut = daysUntilRunOut,
                projectedRunOutIsoDate = projectedRunOutIsoDate,
                projectedRunOutLabel = projectedRunOutLabel,
                reorderTriggerKg = reorderTriggerKg,
                recommendedRestockKg = recommendedRestockKg,
                status = status,
                statusReason = statusReason,
                dailyTrend = dailyTrend
            )
        }.sortedWith(
            compareBy<RawMaterialProjection>(
                { it.status.ordinal },
                { if (it.daysUntilRunOut.isFinite()) it.daysUntilRunOut else Double.MAX_VALUE },
                { it.material.name }
            )
        )
    }

    fun buildRushOrderCode(
        drReference: String,
        rushIndex: Int = 0,
        totalRushCount: Int = 1
    ): String {
        val cleanDr = drReference.trim()
            .ifBlank { "DISP" }
            .replace(Regex("\\s+"), "-")
        val prefix = "RUSH-"
        val suffix = if (totalRushCount > 1) "-${rushIndex + 1}" else ""
        val maxBodyLen = (40 - prefix.length - suffix.length).coerceAtLeast(1)
        return "$prefix${cleanDr.take(maxBodyLen)}$suffix".take(40)
    }

    fun buildRushOrderFromDispatch(
        userId: String,
        orderId: String,
        orderCode: String,
        record: DispatchRecord
    ): CustomerOrder {
        val netKg = record.effectiveDispatchedWeightKg().coerceIn(0.0, 50_000_000.0)
        val statusLabel = when {
            netKg >= record.totalWeightKg - 0.01 -> OrderStatus.FULFILLED.label
            netKg > 0.0 -> OrderStatus.PARTIALLY_DISPATCHED.label
            else -> OrderStatus.OPEN.label
        }
        return CustomerOrder(
            id = orderId,
            userId = userId,
            orderCode = orderCode.trim().take(40),
            orderDate = record.date,
            dueDate = record.date,
            productName = record.productName.trim().take(120),
            orderedBags = record.bagsDispatched,
            bagWeightKg = record.bagWeightKg,
            totalWeightKg = record.totalWeightKg,
            dispatchedWeightKg = netKg,
            customer = record.customer.trim().take(120),
            status = statusLabel,
            priority = true,
            rushOrder = true
        )
    }

    fun normalizePlantLabel(raw: String): String {
        val cleaned = raw.trim()
        if (cleaned.isEmpty() || cleaned.equals("ALL", ignoreCase = true)) return ""
        return PlantSection.entries.firstOrNull {
            it.label.equals(cleaned, ignoreCase = true) || it.name.equals(cleaned, ignoreCase = true)
        }?.label ?: ""
    }

    fun dispatchSkuPriorityKey(sku: SkuEntity): String =
        "${sku.section.trim().lowercase()}|${sku.productName.trim().lowercase()}"

    fun filterDispatchAvailableSelectedSkus(
        skus: List<SkuEntity>,
        availByKey: Map<String, Double>,
        availByName: Map<String, Double>,
        selectedPriorityKeys: Set<String>,
        restrictToSelectedKeys: Boolean = selectedPriorityKeys.isNotEmpty(),
        sourcePlantFilter: String = ""
    ): List<SkuEntity> {
        val normalizedPlant = normalizePlantLabel(sourcePlantFilter)
        return skus.filter { sku ->
            val stableKey = sku.id.ifBlank { "${sku.section}_${sku.productName}" }
            val availKg = availByKey[stableKey]
                ?: availByName[sku.productName.trim().lowercase()]
                ?: 0.0
            val isAvailable = availKg > 0.01
            val isSelected = !restrictToSelectedKeys || dispatchSkuPriorityKey(sku) in selectedPriorityKeys
            val matchesPlant = normalizedPlant.isBlank() || sku.section.equals(normalizedPlant, ignoreCase = true)
            isAvailable && isSelected && matchesPlant
        }
    }

    fun resolveInitialDispatchSourcePlant(
        existingDispatch: DispatchRecord? = null,
        initialOrder: CustomerOrder? = null,
        initialOrders: List<CustomerOrder> = emptyList(),
        initialSku: SkuEntity? = null,
        preselectedPlant: String = "",
        skuByNameLower: Map<String, SkuEntity> = emptyMap()
    ): String {
        val cleanPreselected = normalizePlantLabel(preselectedPlant)
        if (existingDispatch != null) {
            return normalizePlantLabel(existingDispatch.sourcePlant)
                .ifBlank { normalizePlantLabel(skuByNameLower[existingDispatch.productName.trim().lowercase()]?.section.orEmpty()) }
        }
        if (initialOrder != null) {
            return normalizePlantLabel(skuByNameLower[initialOrder.productName.trim().lowercase()]?.section.orEmpty())
                .ifBlank { cleanPreselected }
        }
        if (initialOrders.isNotEmpty()) {
            val plants = initialOrders
                .mapNotNull { ord ->
                    normalizePlantLabel(skuByNameLower[ord.productName.trim().lowercase()]?.section.orEmpty())
                        .takeIf { it.isNotBlank() }
                }
                .distinct()
            return if (plants.size == 1) plants.first() else cleanPreselected
        }
        if (initialSku != null) {
            return normalizePlantLabel(initialSku.section).ifBlank { cleanPreselected }
        }
        return cleanPreselected
    }
}

