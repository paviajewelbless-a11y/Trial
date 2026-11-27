package com.example

import com.example.data.TabPreferencesStore
import com.example.domain.OperationsCalculator
import com.example.model.CustomerOrder
import com.example.model.DispatchRecord
import com.example.model.PlantSection
import com.example.model.SkuEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DispatchFilteringTest {

    @Test
    fun `normalizePlantLabel handles section labels names ALL and invalid inputs`() {
        assertEquals(PlantSection.SAN_ROQUE.label, OperationsCalculator.normalizePlantLabel("SAN ROQUE"))
        assertEquals(PlantSection.PANDACAQUI.label, OperationsCalculator.normalizePlantLabel("pandacaqui"))
        assertEquals(PlantSection.LA_UNION_QUAIL_FEEDS.label, OperationsCalculator.normalizePlantLabel("LA_UNION_QUAIL_FEEDS"))
        assertEquals(PlantSection.SELECTA.label, OperationsCalculator.normalizePlantLabel("Selecta"))
        assertEquals("", OperationsCalculator.normalizePlantLabel("ALL"))
        assertEquals("", OperationsCalculator.normalizePlantLabel("   "))
        assertEquals("", OperationsCalculator.normalizePlantLabel("UNKNOWN_PLANT"))

        assertEquals(PlantSection.PANDACAQUI.label, TabPreferencesStore.normalizeDispatchPlantLabel("PANDACAQUI"))
        assertEquals("", TabPreferencesStore.normalizeDispatchPlantLabel("ALL"))
    }

    @Test
    fun `filterDispatchAvailableSelectedSkus returns only available AND selected products by plant`() {
        val skuSrAvailSelected = SkuEntity(
            id = "sku_1",
            productName = "10K Hog Starter Mash",
            section = PlantSection.SAN_ROQUE.label
        )
        val skuSrZeroStockSelected = SkuEntity(
            id = "sku_2",
            productName = "10K Hog Pre-Starter",
            section = PlantSection.SAN_ROQUE.label
        )
        val skuSrAvailUnselected = SkuEntity(
            id = "sku_3",
            productName = "10K Hog Grower Mash",
            section = PlantSection.SAN_ROQUE.label
        )
        val skuPanAvailSelected = SkuEntity(
            id = "sku_4",
            productName = "MB Broodsow Mash",
            section = PlantSection.PANDACAQUI.label
        )

        val allSkus = listOf(
            skuSrAvailSelected,
            skuSrZeroStockSelected,
            skuSrAvailUnselected,
            skuPanAvailSelected
        )
        val availByKey = mapOf(
            "sku_1" to 1200.0,
            "sku_2" to 0.0,
            "sku_3" to 850.0,
            "sku_4" to 600.0
        )
        val availByName = mapOf(
            "10k hog starter mash" to 1200.0,
            "10k hog pre-starter" to 0.0,
            "10k hog grower mash" to 850.0,
            "mb broodsow mash" to 600.0
        )
        val selectedPriorityKeys = setOf(
            OperationsCalculator.dispatchSkuPriorityKey(skuSrAvailSelected),
            OperationsCalculator.dispatchSkuPriorityKey(skuSrZeroStockSelected),
            OperationsCalculator.dispatchSkuPriorityKey(skuPanAvailSelected)
        )

        val allPlantsResult = OperationsCalculator.filterDispatchAvailableSelectedSkus(
            skus = allSkus,
            availByKey = availByKey,
            availByName = availByName,
            selectedPriorityKeys = selectedPriorityKeys,
            restrictToSelectedKeys = true,
            sourcePlantFilter = ""
        )
        assertEquals(listOf("sku_1", "sku_4"), allPlantsResult.map { it.id })

        val sanRoqueOnly = OperationsCalculator.filterDispatchAvailableSelectedSkus(
            skus = allSkus,
            availByKey = availByKey,
            availByName = availByName,
            selectedPriorityKeys = selectedPriorityKeys,
            restrictToSelectedKeys = true,
            sourcePlantFilter = PlantSection.SAN_ROQUE.label
        )
        assertEquals(listOf("sku_1"), sanRoqueOnly.map { it.id })

        val emptySelectedWhenRestricted = OperationsCalculator.filterDispatchAvailableSelectedSkus(
            skus = allSkus,
            availByKey = availByKey,
            availByName = availByName,
            selectedPriorityKeys = emptySet(),
            restrictToSelectedKeys = true,
            sourcePlantFilter = ""
        )
        assertTrue(emptySelectedWhenRestricted.isEmpty())
    }

    @Test
    fun `resolveInitialDispatchSourcePlant honors context item plant and preselected default`() {
        val skuSr = SkuEntity(id = "s1", productName = "Starter Mash", section = PlantSection.SAN_ROQUE.label)
        val skuPan = SkuEntity(id = "s2", productName = "Broodsow Mash", section = PlantSection.PANDACAQUI.label)
        val skuMap = mapOf(
            "starter mash" to skuSr,
            "broodsow mash" to skuPan
        )

        // Fresh open uses preselected plant
        assertEquals(
            PlantSection.SELECTA.label,
            OperationsCalculator.resolveInitialDispatchSourcePlant(
                preselectedPlant = PlantSection.SELECTA.label,
                skuByNameLower = skuMap
            )
        )

        // Existing dispatch preserves explicit sourcePlant or falls back to SKU section
        assertEquals(
            PlantSection.PANDACAQUI.label,
            OperationsCalculator.resolveInitialDispatchSourcePlant(
                existingDispatch = DispatchRecord(productName = "Starter Mash", sourcePlant = PlantSection.PANDACAQUI.label),
                preselectedPlant = PlantSection.SELECTA.label,
                skuByNameLower = skuMap
            )
        )
        assertEquals(
            PlantSection.SAN_ROQUE.label,
            OperationsCalculator.resolveInitialDispatchSourcePlant(
                existingDispatch = DispatchRecord(productName = "Starter Mash", sourcePlant = ""),
                preselectedPlant = PlantSection.SELECTA.label,
                skuByNameLower = skuMap
            )
        )

        // Order shortcut uses order's SKU plant instead of hiding it behind a different preselected plant
        assertEquals(
            PlantSection.PANDACAQUI.label,
            OperationsCalculator.resolveInitialDispatchSourcePlant(
                initialOrder = CustomerOrder(id = "o1", productName = "Broodsow Mash"),
                preselectedPlant = PlantSection.SAN_ROQUE.label,
                skuByNameLower = skuMap
            )
        )

        // Multi-order across multiple plants falls back to preselectedPlant
        assertEquals(
            PlantSection.SELECTA.label,
            OperationsCalculator.resolveInitialDispatchSourcePlant(
                initialOrders = listOf(
                    CustomerOrder(id = "o1", productName = "Starter Mash"),
                    CustomerOrder(id = "o2", productName = "Broodsow Mash")
                ),
                preselectedPlant = PlantSection.SELECTA.label,
                skuByNameLower = skuMap
            )
        )
    }
}
