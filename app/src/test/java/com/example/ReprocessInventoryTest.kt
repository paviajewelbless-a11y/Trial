package com.example

import com.example.domain.OperationsCalculator
import com.example.model.DispatchRecord
import com.example.model.DispatchStatus
import com.example.model.PlantSection
import com.example.model.ReprocessItem
import com.example.model.ReprocessStatus
import com.example.model.SkuEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReprocessInventoryTest {

    private val starterSku = SkuEntity(
        id = "sku_starter",
        productName = "Booster Starter Crumble",
        section = PlantSection.SAN_ROQUE.label,
        openingStockKg = 1000.0,
        standardBagWeightKg = 50.0,
        targetRecoveryPct = 98.5,
        safetyBufferPct = 15.0
    )

    @Test
    fun pendingDispatchReturnQuarantinesStockAndFlagsFastAction() {
        val returnedDispatch = DispatchRecord(
            id = "disp_1",
            date = "2026-10-15",
            productName = starterSku.productName,
            bagsDispatched = 10L,
            bagWeightKg = 50.0,
            totalWeightKg = 500.0,
            drReference = "DR-1001",
            customer = "Sample Poultry",
            dispatchStatus = DispatchStatus.RETURNED.label,
            hasReturn = true,
            returnConfirmed = true,
            returnedBags = 4L,
            returnedWeightKg = 200.0,
            returnReason = "Damaged sacks"
        )
        val pendingReprocess = ReprocessItem(
            id = "rep_1",
            sourceDispatchId = "disp_1",
            drReference = "DR-1001",
            customer = "Sample Poultry",
            productName = starterSku.productName,
            section = PlantSection.SAN_ROQUE.label,
            returnedDate = "2026-10-15",
            returnedBags = 4L,
            bagWeightKg = 50.0,
            returnedWeightKg = 200.0,
            returnReason = "Damaged sacks",
            status = ReprocessStatus.PENDING_REPROCESS.label,
            fastActionNeeded = true,
            flaggedToInventory = false
        )

        val summary = OperationsCalculator.computeSkuSummaries(
            skus = listOf(starterSku),
            batches = emptyList(),
            dispatches = listOf(returnedDispatch),
            orders = emptyList(),
            lookbackDays = 14,
            reprocessItems = listOf(pendingReprocess)
        ).single()

        // Dispatched 500 kg gross, 200 kg returned => net dispatched = 300 kg.
        // Because 200 kg linked return is quarantined in Reprocess, available = 1000 - 300 - 200 = 500 kg.
        assertEquals(300.0, summary.dispatchedKg, 0.01)
        assertEquals(500.0, summary.availableStockKg, 0.01)
        assertEquals(200.0, summary.pendingReprocessKg, 0.01)
        assertEquals(4L, summary.pendingReprocessBags)
        assertEquals(1, summary.pendingFastActionCount)
        assertEquals(0.0, summary.reprocessedKg, 0.01)
        assertFalse(summary.hasReprocessedFlag)
    }

    @Test
    fun flaggingLinkedReturnToInventoryRestoresNetReprocessedStockAndSetsFlag() {
        val returnedDispatch = DispatchRecord(
            id = "disp_1",
            date = "2026-10-15",
            productName = starterSku.productName,
            bagsDispatched = 10L,
            bagWeightKg = 50.0,
            totalWeightKg = 500.0,
            drReference = "DR-1001",
            customer = "Sample Poultry",
            dispatchStatus = DispatchStatus.RETURNED.label,
            hasReturn = true,
            returnConfirmed = true,
            returnedBags = 4L,
            returnedWeightKg = 200.0
        )
        val flaggedReprocess = ReprocessItem(
            id = "rep_1",
            sourceDispatchId = "disp_1",
            productName = starterSku.productName,
            section = PlantSection.SAN_ROQUE.label,
            returnedDate = "2026-10-15",
            returnedBags = 4L,
            bagWeightKg = 50.0,
            returnedWeightKg = 200.0,
            reprocessedBags = 3L,
            reprocessedWeightKg = 175.0,
            status = ReprocessStatus.FLAGGED_TO_INVENTORY.label,
            fastActionNeeded = false,
            flaggedToInventory = true,
            reprocessedDate = "2026-10-16"
        )

        val summary = OperationsCalculator.computeSkuSummaries(
            skus = listOf(starterSku),
            batches = emptyList(),
            dispatches = listOf(returnedDispatch),
            orders = emptyList(),
            lookbackDays = 14,
            reprocessItems = listOf(flaggedReprocess)
        ).single()

        // 1000 opening - 300 net dispatched + (175 reprocessed - 200 quarantined baseline) = 675 kg
        assertEquals(675.0, summary.availableStockKg, 0.01)
        assertEquals(0.0, summary.pendingReprocessKg, 0.01)
        assertEquals(0, summary.pendingFastActionCount)
        assertEquals(175.0, summary.reprocessedKg, 0.01)
        assertEquals(3L, summary.reprocessedBags)
        assertTrue(summary.hasReprocessedFlag)
    }

    @Test
    fun manualReturnCreditsInventoryOnlyAfterFlaggedAsReprocessed() {
        val manualPending = ReprocessItem(
            id = "rep_manual",
            sourceDispatchId = "",
            productName = starterSku.productName,
            section = PlantSection.SAN_ROQUE.label,
            returnedDate = "2026-10-16",
            returnedBags = 2L,
            bagWeightKg = 50.0,
            returnedWeightKg = 100.0,
            status = ReprocessStatus.REPROCESSING.label,
            fastActionNeeded = true,
            flaggedToInventory = false
        )

        val whilePending = OperationsCalculator.computeSkuSummaries(
            skus = listOf(starterSku),
            batches = emptyList(),
            dispatches = emptyList(),
            orders = emptyList(),
            lookbackDays = 14,
            reprocessItems = listOf(manualPending)
        ).single()

        assertEquals(1000.0, whilePending.availableStockKg, 0.01)
        assertEquals(100.0, whilePending.pendingReprocessKg, 0.01)
        assertEquals(1, whilePending.pendingFastActionCount)
        assertFalse(whilePending.hasReprocessedFlag)

        val manualFlagged = manualPending.copy(
            reprocessedBags = 2L,
            reprocessedWeightKg = 95.0,
            status = ReprocessStatus.FLAGGED_TO_INVENTORY.label,
            fastActionNeeded = false,
            flaggedToInventory = true,
            reprocessedDate = "2026-10-16"
        )

        val afterFlagged = OperationsCalculator.computeSkuSummaries(
            skus = listOf(starterSku),
            batches = emptyList(),
            dispatches = emptyList(),
            orders = emptyList(),
            lookbackDays = 14,
            reprocessItems = listOf(manualFlagged)
        ).single()

        assertEquals(1095.0, afterFlagged.availableStockKg, 0.01)
        assertEquals(0.0, afterFlagged.pendingReprocessKg, 0.01)
        assertEquals(95.0, afterFlagged.reprocessedKg, 0.01)
        assertEquals(2L, afterFlagged.reprocessedBags)
        assertTrue(afterFlagged.hasReprocessedFlag)
    }

    @Test
    fun markingLinkedReturnAsDamagedKeepsStockDeductedAndClearsPending() {
        val returnedDispatch = DispatchRecord(
            id = "disp_dmg",
            date = "2026-10-15",
            productName = starterSku.productName,
            bagsDispatched = 10L,
            bagWeightKg = 50.0,
            totalWeightKg = 500.0,
            drReference = "DR-1002",
            customer = "Sample Poultry",
            dispatchStatus = DispatchStatus.RETURNED.label,
            hasReturn = true,
            returnConfirmed = true,
            returnedBags = 4L,
            returnedWeightKg = 200.0
        )
        val damagedItem = ReprocessItem(
            id = "rep_dmg_1",
            sourceDispatchId = "disp_dmg",
            productName = starterSku.productName,
            section = PlantSection.SAN_ROQUE.label,
            returnedDate = "2026-10-15",
            returnedBags = 4L,
            bagWeightKg = 50.0,
            returnedWeightKg = 200.0,
            reprocessedBags = 0L,
            reprocessedWeightKg = 0.0,
            returnReason = "Item with Bad Order",
            status = ReprocessStatus.DAMAGED.label,
            fastActionNeeded = false,
            flaggedToInventory = false,
            reprocessedDate = "2026-10-17",
            actionNotes = "Damaged: Water / Moisture Spoilage"
        )

        val summary = OperationsCalculator.computeSkuSummaries(
            skus = listOf(starterSku),
            batches = emptyList(),
            dispatches = listOf(returnedDispatch),
            orders = emptyList(),
            lookbackDays = 14,
            reprocessItems = listOf(damagedItem)
        ).single()

        // 1000 opening - 300 net dispatched - 200 damaged write-off = 500 kg (cannot re-enter sellable stock)
        assertEquals(500.0, summary.availableStockKg, 0.01)
        assertEquals(0.0, summary.pendingReprocessKg, 0.01)
        assertEquals(0L, summary.pendingReprocessBags)
        assertEquals(0, summary.pendingFastActionCount)
        assertEquals(200.0, summary.damagedReprocessKg, 0.01)
        assertEquals(4L, summary.damagedReprocessBags)
        assertEquals(0.0, summary.reprocessedKg, 0.01)
        assertFalse(summary.hasReprocessedFlag)
    }

    @Test
    fun markingManualReprocessItemAsDamagedClearsPendingWithoutChangingStock() {
        val manualDamaged = ReprocessItem(
            id = "rep_manual_dmg",
            sourceDispatchId = "",
            productName = starterSku.productName,
            section = PlantSection.SAN_ROQUE.label,
            returnedDate = "2026-10-16",
            returnedBags = 2L,
            bagWeightKg = 50.0,
            returnedWeightKg = 100.0,
            reprocessedBags = 0L,
            reprocessedWeightKg = 0.0,
            status = ReprocessStatus.DAMAGED.label,
            fastActionNeeded = false,
            flaggedToInventory = false,
            reprocessedDate = "2026-10-17"
        )

        val summary = OperationsCalculator.computeSkuSummaries(
            skus = listOf(starterSku),
            batches = emptyList(),
            dispatches = emptyList(),
            orders = emptyList(),
            lookbackDays = 14,
            reprocessItems = listOf(manualDamaged)
        ).single()

        assertEquals(1000.0, summary.availableStockKg, 0.01)
        assertEquals(0.0, summary.pendingReprocessKg, 0.01)
        assertEquals(0, summary.pendingFastActionCount)
        assertEquals(100.0, summary.damagedReprocessKg, 0.01)
        assertEquals(2L, summary.damagedReprocessBags)
        assertFalse(summary.hasReprocessedFlag)
    }
}
