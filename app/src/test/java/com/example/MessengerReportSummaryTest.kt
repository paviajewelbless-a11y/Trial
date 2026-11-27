package com.example

import com.example.domain.ReportExporter
import com.example.domain.SnapshotBitmapRenderer
import com.example.model.CustomerOrder
import com.example.model.DailyOrderLogRow
import com.example.model.DailyPlantLog
import com.example.model.DailySkuLogRow
import com.example.model.DispatchRecord
import com.example.model.PlantReportData
import com.example.model.PlantSection
import com.example.model.ProductReportRow
import com.example.model.ProductionBatch
import com.example.model.ReportSectionOption
import com.example.model.ScheduleRun
import com.example.model.SectionReportSummary
import com.example.model.SkuEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessengerReportSummaryTest {

    @Test
    fun buildMessengerSummaryText_includesAllProductsAndLineItemsWithoutTruncation() {
        val productRows = (1..12).map { idx ->
            ProductReportRow(
                productName = "Feed SKU $idx",
                section = if (idx <= 6) PlantSection.SAN_ROQUE.label else PlantSection.PANDACAQUI.label,
                producedBags = (idx * 10).toLong(),
                recoveredKg = idx * 500.0,
                varianceKg = -5.0,
                dispatchedKg = idx * 200.0,
                openOrdersKg = idx * 100.0,
                availableStockKg = idx * 300.0
            )
        }
        val batches = listOf(
            ProductionBatch(
                id = "b1",
                date = "2026-11-25",
                section = PlantSection.SAN_ROQUE.label,
                productName = "Feed SKU 1",
                bagsProduced = 40,
                bagWeightKg = 50.0,
                expectedWeightKg = 2000.0,
                actualRecoveredKg = 1990.0,
                varianceKg = -10.0,
                recoveryPct = 99.5,
                remarks = "Clean run"
            )
        )
        val dispatches = (1..10).map { idx ->
            DispatchRecord(
                id = "d$idx",
                date = "2026-11-25",
                productName = "Feed SKU $idx",
                bagsDispatched = 15,
                bagWeightKg = 50.0,
                totalWeightKg = 750.0,
                drReference = "DR-10$idx",
                customer = "Farm Customer $idx"
            )
        }
        val orders = (1..10).map { idx ->
            CustomerOrder(
                id = "o$idx",
                orderCode = "PO-20$idx",
                orderDate = "2026-11-24",
                dueDate = "2026-11-26",
                productName = "Feed SKU $idx",
                orderedBags = 20,
                bagWeightKg = 50.0,
                totalWeightKg = 1000.0,
                dispatchedWeightKg = 250.0,
                customer = "Farm Customer $idx",
                status = "Open",
                priority = idx == 1
            )
        }
        val schedules = (1..10).map { idx ->
            ScheduleRun(
                id = "s$idx",
                scheduledDate = "2026-11-25",
                section = PlantSection.PANDACAQUI.label,
                productName = "Feed SKU $idx",
                targetBags = 30,
                bagWeightKg = 50.0,
                expectedWeightKg = 1500.0,
                priority = "High",
                status = "Scheduled"
            )
        }

        val report = PlantReportData(
            startDate = "2026-11-25",
            endDate = "2026-11-25",
            sectionFilter = "ALL",
            sectionSummaries = listOf(
                SectionReportSummary(
                    section = PlantSection.SAN_ROQUE,
                    totalBags = 40,
                    totalExpectedKg = 2000.0,
                    totalRecoveredKg = 1990.0,
                    totalVarianceKg = -10.0,
                    recoveryPct = 99.5,
                    batches = batches,
                    skuCount = 6,
                    totalAvailableStockKg = 6300.0,
                    totalAvailableBags = 126.0
                ),
                SectionReportSummary(
                    section = PlantSection.PANDACAQUI,
                    totalBags = 0,
                    totalExpectedKg = 0.0,
                    totalRecoveredKg = 0.0,
                    totalVarianceKg = 0.0,
                    recoveryPct = 100.0,
                    batches = emptyList(),
                    skuCount = 6,
                    totalAvailableStockKg = 17100.0,
                    totalAvailableBags = 342.0
                )
            ),
            productRows = productRows,
            totalProducedBags = 40,
            totalRecoveredKg = 1990.0,
            totalDispatchedKg = 7500.0,
            totalOpenOrdersKg = 7500.0,
            dispatches = dispatches,
            orders = orders,
            activeOpenOrders = orders,
            schedules = schedules,
            includedSections = ReportSectionOption.entries.toSet()
        )

        val text = ReportExporter.buildMessengerSummaryText(report)

        // All 12 SKUs and their available bag quantities appear under their plant blocks
        (1..12).forEach { idx ->
            assertTrue("Missing Feed SKU $idx", text.contains("Feed SKU $idx — ${idx * 6}"))
        }
        assertTrue(text.contains("*SAN ROQUE | AVAILABLE INVENTORY"))
        assertTrue(text.contains("*PANDACAQUI | AVAILABLE INVENTORY"))
        assertTrue(text.contains("Total SAN ROQUE: 126 bags (6,300.00 kg)"))
        assertTrue(text.contains("Total PANDACAQUI: 342 bags (17,100.00 kg)"))
        assertTrue(text.contains("Clean run"))
        assertTrue(text.contains("DR-1010"))
        assertTrue(text.contains("PO-2010"))
        assertTrue(text.contains("*CUSTOMER ORDERS IN PERIOD (10)*"))
        assertFalse(text.contains("...and "))

        val xls = ReportExporter.buildXlsSpreadsheetXml(report)
        assertTrue(xls.contains("<Worksheet ss:Name=\"SAN ROQUE\">"))
        assertTrue(xls.contains("<Worksheet ss:Name=\"PANDACAQUI\">"))

        val allSnap = SnapshotBitmapRenderer.buildPlantSnapshotPayload(report, targetPlantSection = null)
        assertTrue(allSnap.tables.size == 2)
        assertTrue(allSnap.tables[0].leftHeader == "SAN ROQUE")
        assertTrue(allSnap.tables[0].rows.size == 6)
        assertTrue(allSnap.tables[1].leftHeader == "PANDACAQUI")
        assertTrue(allSnap.tables[1].rows.size == 6)

        val singleSnap = SnapshotBitmapRenderer.buildPlantSnapshotPayload(report, targetPlantSection = PlantSection.SAN_ROQUE.label)
        assertTrue(singleSnap.tables.size == 1)
        assertTrue(singleSnap.tables[0].leftHeader == "SAN ROQUE")
        assertTrue(singleSnap.tables[0].rows[0].label == "Feed SKU 1")
        assertTrue(singleSnap.tables[0].rows[0].value == "6")
    }

    @Test
    fun buildDailyLogMessengerText_includesAllSkusEvenUneditedAndAllOrders() {
        val skuRows = (1..12).map { idx ->
            DailySkuLogRow(
                sku = SkuEntity(
                    id = "sku$idx",
                    productName = "Daily SKU $idx",
                    section = PlantSection.SAN_ROQUE.label,
                    openingStockKg = 1000.0,
                    standardBagWeightKg = 50.0
                ),
                closingStockKg = 1000.0 + idx * 10.0,
                closingBags = (1000.0 + idx * 10.0) / 50.0,
                producedDayKg = if (idx == 1) 500.0 else 0.0,
                producedDayBags = if (idx == 1) 10L else 0L,
                dispatchedDayKg = 0.0,
                dispatchedDayBags = 0L,
                returnedDayKg = 0.0,
                openOrdersSnapshotKg = 200.0,
                editedOnDay = idx == 1,
                editedTimeLabel = if (idx == 1) "08:30" else ""
            )
        }
        val orderRows = (1..10).map { idx ->
            DailyOrderLogRow(
                order = CustomerOrder(
                    id = "do$idx",
                    orderCode = "DPO-$idx",
                    orderDate = "2026-11-25",
                    dueDate = "2026-11-26",
                    productName = "Daily SKU $idx",
                    orderedBags = 12,
                    bagWeightKg = 50.0,
                    totalWeightKg = 600.0,
                    dispatchedWeightKg = 100.0,
                    customer = "Daily Customer $idx",
                    status = "Open"
                ),
                placedOnDay = true,
                dueOnDay = false,
                editedOnDay = false,
                dispatchedOnDayKg = 100.0,
                dispatchedOnDayBags = 2L
            )
        }
        val dayLog = DailyPlantLog(
            date = "2026-11-25",
            isToday = true,
            skuRows = skuRows,
            orderRows = orderRows,
            totalClosingStockKg = 12780.0,
            totalClosingBags = 255.6,
            totalProducedDayKg = 500.0,
            totalDispatchedDayKg = 1000.0,
            totalOrdersLoggedKg = 6000.0,
            totalOpenOrdersSnapshotKg = 2400.0,
            skusEditedCount = 1,
            ordersEditedCount = 0,
            ordersPlacedCount = 10,
            ordersDueCount = 0,
            linkedDispatchOrdersCount = 10
        )

        val text = ReportExporter.buildDailyLogMessengerText(dayLog, "ALL")
        assertTrue(text.contains("*SAN ROQUE | CLOSING INVENTORY (TODAY)*"))
        (1..12).forEach { idx ->
            assertTrue("Missing Daily SKU $idx", text.contains("Daily SKU $idx — "))
        }
        (1..10).forEach { idx ->
            assertTrue("Missing DPO-$idx", text.contains("DPO-$idx"))
        }
        assertFalse(text.contains("...and "))

        val dailySnap = SnapshotBitmapRenderer.buildDailyLogSnapshotPayload(dayLog, sectionFilter = "ALL")
        assertTrue(dailySnap.tables.size == 1)
        assertTrue(dailySnap.tables[0].leftHeader == "SAN ROQUE")
        assertTrue(dailySnap.tables[0].rightHeaderTop == "CLOSING")
        assertTrue(dailySnap.tables[0].rows.size == 12)
    }

    @Test
    fun buildPlantSnapshotPayload_ordersOnly_includesFullUnclippedOrderDetails() {
        val longOrder = CustomerOrder(
            id = "o_long",
            orderCode = "PO-2611-46-1",
            orderDate = "2026-11-25",
            dueDate = "2026-11-26",
            productName = "MB BROODSOW MASH (HIGH END)",
            orderedBags = 30,
            bagWeightKg = 50.0,
            totalWeightKg = 1500.0,
            dispatchedWeightKg = 0.0,
            customer = "Allan De Guzman",
            status = "Open",
            priority = true
        )
        val report = PlantReportData(
            startDate = "2026-11-26",
            endDate = "2026-11-26",
            sectionFilter = "ALL",
            sectionSummaries = emptyList(),
            productRows = emptyList(),
            totalProducedBags = 0,
            totalRecoveredKg = 0.0,
            totalDispatchedKg = 0.0,
            totalOpenOrdersKg = 1500.0,
            dispatches = emptyList(),
            orders = listOf(longOrder),
            activeOpenOrders = listOf(longOrder),
            schedules = emptyList(),
            includedSections = setOf(ReportSectionOption.ORDERS)
        )

        val payload = SnapshotBitmapRenderer.buildPlantSnapshotPayload(report)
        assertTrue(payload.kpiSummary.contains("Open Orders: 1,500.00 kg"))
        assertTrue(payload.tables.size == 1)
        val orderRow = payload.tables[0].rows[0]
        assertTrue(orderRow.label.contains("PO-2611-46-1"))
        assertTrue(orderRow.label.contains("Allan De Guzman"))
        assertTrue(orderRow.label.contains("MB BROODSOW MASH (HIGH END)"))
        assertTrue(orderRow.label.contains("Ordered: 30 bags (1,500.00 kg)"))
        assertTrue(orderRow.label.contains("Due: 2026-11-26"))
        assertTrue(orderRow.label.contains("Open"))
        assertFalse(orderRow.label.contains("..."))
        assertTrue(orderRow.value.contains("1,500.00 kg"))
        assertTrue(orderRow.value.contains("30 bags"))

        val wrapped = SnapshotBitmapRenderer.wrapTextLines(
            text = orderRow.label,
            maxWidth = 220f,
            measureText = { s -> s.length * 10f }
        )
        assertTrue(wrapped.size >= 4)
        assertFalse(wrapped.any { it.contains("...") })
    }

    @Test
    fun buildPlantSnapshotPayload_otherReports_includeKpisAndFullRowDetails() {
        val batch = ProductionBatch(
            id = "b1",
            date = "2026-11-26",
            section = PlantSection.SAN_ROQUE.label,
            productName = "MB BROODSOW MASH (HIGH END)",
            bagsProduced = 40,
            bagWeightKg = 50.0,
            expectedWeightKg = 2000.0,
            actualRecoveredKg = 1980.0,
            varianceKg = -20.0,
            recoveryPct = 99.0,
            remarks = "Morning Run",
            scheduledForCustomers = listOf("Allan De Guzman")
        )
        val linkedOrder = CustomerOrder(
            id = "o2",
            orderCode = "PO-2611-46-2",
            orderDate = "2026-11-25",
            dueDate = "2026-11-26",
            productName = "PM HOG GROWER PELLET",
            orderedBags = 25,
            bagWeightKg = 50.0,
            totalWeightKg = 1250.0,
            customer = "San Mateo Farm",
            status = "Delivered"
        )
        val dispatch = DispatchRecord(
            id = "d1",
            date = "2026-11-26",
            productName = "PM HOG GROWER PELLET",
            bagsDispatched = 25,
            bagWeightKg = 50.0,
            totalWeightKg = 1250.0,
            drReference = "DR-7788",
            customer = "San Mateo Farm",
            linkedOrderId = "o2",
            sourcePlant = PlantSection.SAN_ROQUE.label,
            driverName = "Juan",
            truckNumber = "ABC-1234"
        )
        val schedule = ScheduleRun(
            id = "s1",
            scheduledDate = "2026-11-26",
            section = PlantSection.PANDACAQUI.label,
            productName = "PM HOG STARTER PELLET",
            targetBags = 60,
            bagWeightKg = 50.0,
            expectedWeightKg = 3000.0,
            lineRemarks = "Pellet Line 1",
            priority = "Rush",
            status = "Scheduled",
            linkedOrderCodes = listOf("PO-2611-46-3")
        )
        val baseReport = PlantReportData(
            startDate = "2026-11-26",
            endDate = "2026-11-26",
            sectionFilter = "ALL",
            sectionSummaries = listOf(
                SectionReportSummary(
                    section = PlantSection.SAN_ROQUE,
                    totalBags = 40,
                    totalExpectedKg = 2000.0,
                    totalRecoveredKg = 1980.0,
                    totalVarianceKg = -20.0,
                    recoveryPct = 99.0,
                    batches = listOf(batch),
                    skuCount = 1
                )
            ),
            productRows = emptyList(),
            totalProducedBags = 40,
            totalRecoveredKg = 1980.0,
            totalDispatchedKg = 1250.0,
            totalOpenOrdersKg = 0.0,
            dispatches = listOf(dispatch),
            orders = listOf(linkedOrder),
            schedules = listOf(schedule)
        )

        val recoverySnap = SnapshotBitmapRenderer.buildPlantSnapshotPayload(
            baseReport.copy(includedSections = setOf(ReportSectionOption.RECOVERY)),
            targetPlantSection = PlantSection.SAN_ROQUE.label
        )
        assertTrue(recoverySnap.kpiSummary.contains("Produced: 40b (1,980.00 kg)"))
        assertTrue(recoverySnap.tables[0].rows[0].label.contains("MB BROODSOW MASH (HIGH END)"))
        assertTrue(recoverySnap.tables[0].rows[0].label.contains("Allan De Guzman"))
        assertTrue(recoverySnap.tables[0].rows[0].value.contains("40 bags"))

        val dispatchSnap = SnapshotBitmapRenderer.buildPlantSnapshotPayload(
            baseReport.copy(includedSections = setOf(ReportSectionOption.DISPATCHES))
        )
        assertTrue(dispatchSnap.kpiSummary.contains("Dispatched: 1,250.00 kg (1 trips)"))
        assertTrue(dispatchSnap.tables[0].rows[0].label.contains("DR DR-7788"))
        assertTrue(dispatchSnap.tables[0].rows[0].label.contains("PM HOG GROWER PELLET"))
        assertTrue(dispatchSnap.tables[0].rows[0].label.contains("PO: PO-2611-46-2"))
        assertTrue(dispatchSnap.tables[0].rows[0].value.contains("25 bags"))

        val scheduleSnap = SnapshotBitmapRenderer.buildPlantSnapshotPayload(
            baseReport.copy(includedSections = setOf(ReportSectionOption.SCHEDULE))
        )
        assertTrue(scheduleSnap.kpiSummary.contains("Schedule: 1 runs (1 active / 60b)"))
        assertTrue(scheduleSnap.tables[0].rows[0].label.contains("PM HOG STARTER PELLET"))
        assertTrue(scheduleSnap.tables[0].rows[0].label.contains("PO-2611-46-3"))
        assertTrue(scheduleSnap.tables[0].rows[0].value.contains("60 bags"))
    }
}
