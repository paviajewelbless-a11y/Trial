package com.example

import com.example.domain.DailyTargetStatus
import com.example.domain.ProductionDateWindow
import com.example.domain.ProductionMonitorCalculator
import com.example.domain.ProductionTargetBasis
import com.example.model.PlantSection
import com.example.model.ProductionBatch
import com.example.model.ScheduleRun
import com.example.model.ScheduleStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionMonitorTest {

    private val sampleBatches = listOf(
        ProductionBatch(
            id = "b1",
            date = "2026-10-10",
            section = PlantSection.SAN_ROQUE.label,
            productName = "MB BROODSOW MASH",
            bagsProduced = 100L,
            bagWeightKg = 50.0,
            expectedWeightKg = 5000.0,
            actualRecoveredKg = 4950.0,
            varianceKg = -50.0,
            recoveryPct = 99.0
        ),
        ProductionBatch(
            id = "b2",
            date = "2026-10-15",
            section = PlantSection.SAN_ROQUE.label,
            productName = "PM HOG GROWER PELLET",
            bagsProduced = 120L,
            bagWeightKg = 50.0,
            expectedWeightKg = 6000.0,
            actualRecoveredKg = 6100.0,
            varianceKg = 100.0,
            recoveryPct = 101.67
        ),
        ProductionBatch(
            id = "b3",
            date = "2026-10-16",
            section = PlantSection.PANDACAQUI.label,
            productName = "BARAKO DUCK PELLET",
            bagsProduced = 80L,
            bagWeightKg = 50.0,
            expectedWeightKg = 4000.0,
            actualRecoveredKg = 3600.0,
            varianceKg = -400.0,
            recoveryPct = 90.0
        )
    )

    private val sampleSchedules = listOf(
        ScheduleRun(
            id = "s1",
            scheduledDate = "2026-10-15",
            section = PlantSection.SAN_ROQUE.label,
            productName = "PM HOG GROWER PELLET",
            targetBags = 120L,
            bagWeightKg = 50.0,
            expectedWeightKg = 6000.0,
            status = ScheduleStatus.COMPLETED.label
        ),
        ScheduleRun(
            id = "s2",
            scheduledDate = "2026-10-16",
            section = PlantSection.PANDACAQUI.label,
            productName = "BARAKO DUCK PELLET",
            targetBags = 100L,
            bagWeightKg = 50.0,
            expectedWeightKg = 5000.0,
            status = ScheduleStatus.IN_PROGRESS.label
        ),
        ScheduleRun(
            id = "s3_cancelled",
            scheduledDate = "2026-10-16",
            section = PlantSection.PANDACAQUI.label,
            productName = "BARAKO DUCK PELLET",
            targetBags = 200L,
            bagWeightKg = 50.0,
            expectedWeightKg = 10000.0,
            status = ScheduleStatus.CANCELLED.label
        )
    )

    @Test
    fun `scheduled basis aggregates daily output vs active schedules and excludes cancelled`() {
        val summary = ProductionMonitorCalculator.computeSummary(
            batches = sampleBatches,
            schedules = sampleSchedules,
            window = ProductionDateWindow.DAYS_7,
            sectionFilter = "ALL",
            targetBasis = ProductionTargetBasis.SCHEDULED,
            referenceDateOverride = LocalDate.of(2026, 10, 16)
        )

        // 2026-10-10 is outside the 7d window (Oct 10..Oct 16? Wait: minusDays(6) = Oct 10; let's verify!)
        // Oct 16 minus 6 days = Oct 10, so all 3 dates are inside [Oct 10, Oct 16]!
        assertEquals(3, summary.points.size)
        assertEquals(14650.0, summary.totalOutputKg, 0.01)
        // Target excludes cancelled s3_cancelled (10,000 kg): only 6,000 + 5,000 = 11,000 kg
        assertEquals(11000.0, summary.totalTargetKg, 0.01)

        val oct15 = summary.points.first { it.isoDate == "2026-10-15" }
        assertEquals(6100.0, oct15.actualOutputKg, 0.01)
        assertEquals(6000.0, oct15.targetKg, 0.01)
        assertEquals(100.0, oct15.varianceKg, 0.01)
        assertEquals(DailyTargetStatus.ON_TARGET, oct15.status)

        val oct16 = summary.points.first { it.isoDate == "2026-10-16" }
        assertEquals(3600.0, oct16.actualOutputKg, 0.01)
        assertEquals(5000.0, oct16.targetKg, 0.01)
        assertEquals(-1400.0, oct16.varianceKg, 0.01)
        assertEquals(72.0, oct16.attainmentPct, 0.01)
        assertEquals(DailyTargetStatus.BELOW_TARGET, oct16.status)

        val oct10 = summary.points.first { it.isoDate == "2026-10-10" }
        assertEquals(DailyTargetStatus.UNSCHEDULED_OUTPUT, oct10.status)
    }

    @Test
    fun `7d window anchored at Oct 17 excludes Oct 10`() {
        val summary = ProductionMonitorCalculator.computeSummary(
            batches = sampleBatches,
            schedules = sampleSchedules,
            window = ProductionDateWindow.DAYS_7,
            sectionFilter = "ALL",
            targetBasis = ProductionTargetBasis.SCHEDULED,
            referenceDateOverride = LocalDate.of(2026, 10, 17)
        )

        assertEquals(2, summary.points.size)
        assertEquals(9700.0, summary.totalOutputKg, 0.01)
        assertEquals(11000.0, summary.totalTargetKg, 0.01)
        assertEquals(1, summary.onTargetDaysCount)
        assertEquals(2, summary.evaluatedTargetDaysCount)
    }

    @Test
    fun `section filter and custom daily target basis calculate correctly`() {
        val summary = ProductionMonitorCalculator.computeSummary(
            batches = sampleBatches,
            schedules = sampleSchedules,
            window = ProductionDateWindow.ALL,
            sectionFilter = PlantSection.SAN_ROQUE.label,
            targetBasis = ProductionTargetBasis.CUSTOM_DAILY,
            customDailyTargetKg = 5500.0
        )

        assertEquals(2, summary.points.size)
        assertEquals(11050.0, summary.totalOutputKg, 0.01)
        assertEquals(11000.0, summary.totalTargetKg, 0.01)
        assertEquals(50.0, summary.totalVarianceKg, 0.01)
        assertEquals(1, summary.onTargetDaysCount)
        assertEquals(1, summary.sectionAttainments.size)
        assertEquals(PlantSection.SAN_ROQUE, summary.sectionAttainments.first().section)
    }

    @Test
    fun `batch expected basis compares actual recovery against batch expected weight`() {
        val summary = ProductionMonitorCalculator.computeSummary(
            batches = sampleBatches,
            schedules = sampleSchedules,
            window = ProductionDateWindow.ALL,
            sectionFilter = "ALL",
            targetBasis = ProductionTargetBasis.BATCH_EXPECTED
        )

        assertEquals(14650.0, summary.totalOutputKg, 0.01)
        assertEquals(15000.0, summary.totalTargetKg, 0.01)
        assertEquals(-350.0, summary.totalVarianceKg, 0.01)
        assertNotNull(summary.peakDay)
        assertEquals("2026-10-15", summary.peakDay?.isoDate)
        assertTrue(summary.overallAttainmentPct in 97.6..97.8)
    }
}
