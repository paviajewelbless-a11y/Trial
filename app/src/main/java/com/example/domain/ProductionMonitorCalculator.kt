package com.example.domain

import com.example.model.PlantSection
import com.example.model.ProductionBatch
import com.example.model.ScheduleRun
import com.example.model.ScheduleStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

enum class ProductionDateWindow(val label: String, val days: Int?) {
    DAYS_7("7d", 7),
    DAYS_14("14d", 14),
    DAYS_30("30d", 30),
    ALL("All", null)
}

enum class ProductionTargetBasis(val label: String, val shortLabel: String) {
    SCHEDULED("Scheduled Runs", "Scheduled"),
    BATCH_EXPECTED("Batch Expected", "Batch Exp"),
    CUSTOM_DAILY("Custom Daily Target", "Custom")
}

enum class DailyTargetStatus(val label: String) {
    ON_TARGET("On Target"),
    NEAR_TARGET("Near Target"),
    BELOW_TARGET("Below Target"),
    UNSCHEDULED_OUTPUT("Extra Output"),
    NO_TARGET("No Activity")
}

data class DailyOutputVsTargetPoint(
    val isoDate: String,
    val shortLabel: String,
    val actualOutputKg: Double,
    val actualBags: Long,
    val targetKg: Double,
    val targetBags: Long,
    val varianceKg: Double,
    val attainmentPct: Double,
    val batchCount: Int,
    val scheduleCount: Int,
    val avgRecoveryPct: Double,
    val status: DailyTargetStatus
)

data class SectionOutputAttainment(
    val section: PlantSection,
    val actualOutputKg: Double,
    val targetKg: Double,
    val varianceKg: Double,
    val attainmentPct: Double,
    val batchCount: Int,
    val scheduleCount: Int
)

data class ProductionMonitorSummary(
    val points: List<DailyOutputVsTargetPoint>,
    val sectionAttainments: List<SectionOutputAttainment>,
    val totalOutputKg: Double,
    val totalTargetKg: Double,
    val totalVarianceKg: Double,
    val overallAttainmentPct: Double,
    val totalBatches: Int,
    val totalActualBags: Long,
    val onTargetDaysCount: Int,
    val evaluatedTargetDaysCount: Int,
    val avgDailyOutputKg: Double,
    val peakDay: DailyOutputVsTargetPoint?,
    val recommendedCustomDailyTargetKg: Double
)

object ProductionMonitorCalculator {

    private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

    fun scheduleTargetKg(run: ScheduleRun): Double =
        if (run.expectedWeightKg > 0.0) {
            run.expectedWeightKg
        } else {
            (run.targetBags.coerceAtLeast(0L) * run.bagWeightKg.coerceAtLeast(0.0))
        }

    fun computeSummary(
        batches: List<ProductionBatch>,
        schedules: List<ScheduleRun>,
        window: ProductionDateWindow = ProductionDateWindow.DAYS_14,
        sectionFilter: String = "ALL",
        targetBasis: ProductionTargetBasis = ProductionTargetBasis.SCHEDULED,
        customDailyTargetKg: Double = 5000.0,
        referenceDateOverride: LocalDate? = null
    ): ProductionMonitorSummary {
        val scopedBatches = batches.mapNotNull { batch ->
            if (sectionFilter != "ALL" && batch.section != sectionFilter) return@mapNotNull null
            val parsed = OperationsCalculator.parseDateOrNull(batch.date) ?: return@mapNotNull null
            parsed to batch
        }

        val scopedSchedules = schedules.mapNotNull { run ->
            if (sectionFilter != "ALL" && run.section != sectionFilter) return@mapNotNull null
            if (ScheduleStatus.fromRaw(run.status) == ScheduleStatus.CANCELLED) return@mapNotNull null
            val parsed = OperationsCalculator.parseDateOrNull(run.scheduledDate) ?: return@mapNotNull null
            parsed to run
        }

        val allDates = (scopedBatches.map { it.first } + scopedSchedules.map { it.first })
        val anchorEnd = referenceDateOverride
            ?: allDates.maxOrNull()
            ?: OperationsCalculator.parseDateOrNull(OperationsCalculator.todayIsoDate())
            ?: LocalDate.of(2026, 11, 24)

        val windowStart = window.days?.let { anchorEnd.minusDays((it - 1).toLong()) }

        val windowBatches = scopedBatches.filter { (d, _) ->
            (windowStart == null || !d.isBefore(windowStart)) && !d.isAfter(anchorEnd)
        }
        val windowSchedules = scopedSchedules.filter { (d, _) ->
            (windowStart == null || !d.isBefore(windowStart)) && !d.isAfter(anchorEnd)
        }

        val batchesByDate = windowBatches.groupBy({ it.first }, { it.second })
        val schedulesByDate = windowSchedules.groupBy({ it.first }, { it.second })

        val activeDates = (batchesByDate.keys + schedulesByDate.keys).toSortedSet()
        val safeCustomTargetKg = customDailyTargetKg.coerceAtLeast(0.0)

        val points = activeDates.map { date ->
            val dayBatches = batchesByDate[date].orEmpty()
            val daySchedules = schedulesByDate[date].orEmpty()

            val actualKg = dayBatches.sumOf { it.actualRecoveredKg.coerceAtLeast(0.0) }
            val actualBags = dayBatches.sumOf { it.bagsProduced.coerceAtLeast(0L) }
            val batchExpectedKg = dayBatches.sumOf { it.expectedWeightKg.coerceAtLeast(0.0) }
            val schedKg = daySchedules.sumOf { scheduleTargetKg(it) }
            val schedBags = daySchedules.sumOf { it.targetBags.coerceAtLeast(0L) }

            val targetKg = when (targetBasis) {
                ProductionTargetBasis.SCHEDULED -> schedKg
                ProductionTargetBasis.BATCH_EXPECTED -> batchExpectedKg
                ProductionTargetBasis.CUSTOM_DAILY -> safeCustomTargetKg
            }
            val targetBags = when (targetBasis) {
                ProductionTargetBasis.SCHEDULED -> schedBags
                ProductionTargetBasis.BATCH_EXPECTED -> actualBags
                ProductionTargetBasis.CUSTOM_DAILY -> 0L
            }

            val varianceKg = actualKg - targetKg
            val attainmentPct = when {
                targetKg > 0.0 -> (actualKg / targetKg) * 100.0
                actualKg > 0.0 -> 100.0
                else -> 0.0
            }
            val avgRec = if (batchExpectedKg > 0.0) (actualKg / batchExpectedKg) * 100.0 else 100.0
            val status = when {
                targetKg <= 0.0 && actualKg > 0.0 -> DailyTargetStatus.UNSCHEDULED_OUTPUT
                targetKg <= 0.0 -> DailyTargetStatus.NO_TARGET
                attainmentPct >= 100.0 -> DailyTargetStatus.ON_TARGET
                attainmentPct >= 85.0 -> DailyTargetStatus.NEAR_TARGET
                else -> DailyTargetStatus.BELOW_TARGET
            }

            DailyOutputVsTargetPoint(
                isoDate = date.toString(),
                shortLabel = date.format(shortDateFormatter),
                actualOutputKg = actualKg,
                actualBags = actualBags,
                targetKg = targetKg,
                targetBags = targetBags,
                varianceKg = varianceKg,
                attainmentPct = attainmentPct,
                batchCount = dayBatches.size,
                scheduleCount = daySchedules.size,
                avgRecoveryPct = avgRec,
                status = status
            )
        }

        val activeSections = PlantSection.entries.filter {
            sectionFilter == "ALL" || it.label == sectionFilter
        }
        val sectionCount = activeSections.size.coerceAtLeast(1)

        val sectionAttainments = activeSections.map { sec ->
            val secBatches = windowBatches.filter { it.second.section == sec.label }.map { it.second }
            val secSchedules = windowSchedules.filter { it.second.section == sec.label }.map { it.second }

            val secActualKg = secBatches.sumOf { it.actualRecoveredKg.coerceAtLeast(0.0) }
            val secTargetKg = when (targetBasis) {
                ProductionTargetBasis.SCHEDULED -> secSchedules.sumOf { scheduleTargetKg(it) }
                ProductionTargetBasis.BATCH_EXPECTED -> secBatches.sumOf { it.expectedWeightKg.coerceAtLeast(0.0) }
                ProductionTargetBasis.CUSTOM_DAILY -> {
                    if (points.isEmpty()) 0.0 else (safeCustomTargetKg / sectionCount) * points.size
                }
            }
            val secVariance = secActualKg - secTargetKg
            val secAttainment = when {
                secTargetKg > 0.0 -> (secActualKg / secTargetKg) * 100.0
                secActualKg > 0.0 -> 100.0
                else -> 0.0
            }

            SectionOutputAttainment(
                section = sec,
                actualOutputKg = secActualKg,
                targetKg = secTargetKg,
                varianceKg = secVariance,
                attainmentPct = secAttainment,
                batchCount = secBatches.size,
                scheduleCount = secSchedules.size
            )
        }

        val totalOutputKg = points.sumOf { it.actualOutputKg }
        val totalTargetKg = points.sumOf { it.targetKg }
        val totalVarianceKg = totalOutputKg - totalTargetKg
        val overallAttainmentPct = when {
            totalTargetKg > 0.0 -> (totalOutputKg / totalTargetKg) * 100.0
            totalOutputKg > 0.0 -> 100.0
            else -> 0.0
        }
        val totalBatches = windowBatches.size
        val totalActualBags = points.sumOf { it.actualBags }
        val evaluatedDays = points.count { it.targetKg > 0.0 }
        val onTargetDays = points.count { it.targetKg > 0.0 && it.actualOutputKg >= it.targetKg }
        val avgDailyOutputKg = if (points.isNotEmpty()) totalOutputKg / points.size else 0.0
        val peakDay = points.maxByOrNull { it.actualOutputKg }
        val recommendedCustomTarget = if (avgDailyOutputKg > 0.0) {
            ((avgDailyOutputKg / 500.0).roundToInt() * 500.0).coerceAtLeast(1000.0)
        } else {
            5000.0
        }

        return ProductionMonitorSummary(
            points = points,
            sectionAttainments = sectionAttainments,
            totalOutputKg = totalOutputKg,
            totalTargetKg = totalTargetKg,
            totalVarianceKg = totalVarianceKg,
            overallAttainmentPct = overallAttainmentPct,
            totalBatches = totalBatches,
            totalActualBags = totalActualBags,
            onTargetDaysCount = onTargetDays,
            evaluatedTargetDaysCount = evaluatedDays,
            avgDailyOutputKg = avgDailyOutputKg,
            peakDay = peakDay,
            recommendedCustomDailyTargetKg = recommendedCustomTarget
        )
    }
}
