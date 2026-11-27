package com.example

import com.example.domain.OperationsCalculator
import com.example.model.CustomerOrder
import com.example.model.DispatchRecord
import com.example.model.DispatchStatus
import com.example.model.OrderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RushOrdersDispatchTest {

    @Test
    fun `buildRushOrderCode respects 40-char limit and indexes multiple rush stops`() {
        val singleCode = OperationsCalculator.buildRushOrderCode("DR-BLC-4821")
        assertEquals("RUSH-DR-BLC-4821", singleCode)
        assertTrue(singleCode.length <= 40)

        val multiFirst = OperationsCalculator.buildRushOrderCode("DR-BLC-4821", rushIndex = 0, totalRushCount = 2)
        val multiSecond = OperationsCalculator.buildRushOrderCode("DR-BLC-4821", rushIndex = 1, totalRushCount = 2)
        assertEquals("RUSH-DR-BLC-4821-1", multiFirst)
        assertEquals("RUSH-DR-BLC-4821-2", multiSecond)

        val longDr = "DR-VERY-LONG-CUSTOMER-DISPATCH-REFERENCE-NUMBER-2026-11"
        val truncated = OperationsCalculator.buildRushOrderCode(longDr, rushIndex = 1, totalRushCount = 3)
        assertTrue(truncated.startsWith("RUSH-"))
        assertTrue(truncated.endsWith("-2"))
        assertTrue(truncated.length in 1..40)
    }

    @Test
    fun `buildRushOrderFromDispatch marks full delivery as Fulfilled rush order`() {
        val dispatch = DispatchRecord(
            id = "disp_101",
            userId = "user_1",
            date = "2026-11-24",
            productName = "10K Hog Starter Mash",
            bagsDispatched = 25L,
            bagWeightKg = 50.0,
            totalWeightKg = 1250.0,
            drReference = "DR-BLC-9001",
            customer = "Extra Farm Stop",
            dispatchStatus = DispatchStatus.DELIVERED.label
        )

        val rushOrder = OperationsCalculator.buildRushOrderFromDispatch(
            userId = "user_1",
            orderId = "ord_rush_1",
            orderCode = OperationsCalculator.buildRushOrderCode(dispatch.drReference),
            record = dispatch
        )

        assertTrue(rushOrder.rushOrder)
        assertTrue(rushOrder.priority)
        assertEquals("RUSH-DR-BLC-9001", rushOrder.orderCode)
        assertEquals("Extra Farm Stop", rushOrder.customer)
        assertEquals(25L, rushOrder.orderedBags)
        assertEquals(1250.0, rushOrder.totalWeightKg, 0.001)
        assertEquals(1250.0, rushOrder.dispatchedWeightKg, 0.001)
        assertEquals(OrderStatus.FULFILLED.label, rushOrder.status)
    }

    @Test
    fun `buildRushOrderFromDispatch marks confirmed partial return as Partially Dispatched`() {
        val dispatch = DispatchRecord(
            id = "disp_102",
            userId = "user_1",
            date = "2026-11-24",
            productName = "MB BROODSOW MASH",
            bagsDispatched = 20L,
            bagWeightKg = 50.0,
            totalWeightKg = 1000.0,
            drReference = "DR-BLC-9002",
            customer = "San Rafael Piggery",
            dispatchStatus = DispatchStatus.RETURNED.label,
            hasReturn = true,
            returnConfirmed = true,
            returnedBags = 4L,
            returnedWeightKg = 200.0,
            returnReason = "Quantity Variance"
        )

        val rushOrder = OperationsCalculator.buildRushOrderFromDispatch(
            userId = "user_1",
            orderId = "ord_rush_2",
            orderCode = OperationsCalculator.buildRushOrderCode(dispatch.drReference),
            record = dispatch
        )

        assertTrue(rushOrder.rushOrder)
        assertTrue(rushOrder.priority)
        assertEquals(800.0, rushOrder.dispatchedWeightKg, 0.001)
        assertEquals(OrderStatus.PARTIALLY_DISPATCHED.label, rushOrder.status)
    }

    @Test
    fun `rush orders tab filter isolates only rush orders while All Orders keeps all`() {
        val regular = CustomerOrder(id = "ord_reg", orderCode = "PO-1", rushOrder = false)
        val rush = CustomerOrder(id = "ord_rush", orderCode = "RUSH-1", rushOrder = true)
        val allOrders = listOf(regular, rush)

        val rushTabOrders = allOrders.filter { it.rushOrder }
        assertEquals(1, rushTabOrders.size)
        assertEquals("ord_rush", rushTabOrders.single().id)
        assertFalse(regular.rushOrder)
    }
}
