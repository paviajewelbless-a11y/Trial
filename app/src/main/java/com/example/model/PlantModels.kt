package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PlantSection(val label: String) {
    SAN_ROQUE("SAN ROQUE"),
    PANDACAQUI("PANDACAQUI"),
    LA_UNION_QUAIL_FEEDS("LA UNION QUAIL FEEDS"),
    SELECTA("SELECTA");

    companion object {
        fun fromRaw(raw: String): PlantSection =
            entries.firstOrNull {
                it.label.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true)
            } ?: SAN_ROQUE
    }
}

enum class OrderStatus(val label: String) {
    OPEN("Open"),
    PARTIALLY_DISPATCHED("Partially Dispatched"),
    FULFILLED("Fulfilled"),
    CANCELLED("Cancelled");

    companion object {
        fun fromRaw(raw: String): OrderStatus =
            entries.firstOrNull { it.label == raw || it.name == raw } ?: OPEN
    }
}

enum class SchedulePriority(val label: String, val sortRank: Int) {
    URGENT("Urgent", 0),
    ORDER_DRIVEN("Order-Driven", 1),
    BUFFER_TOP_UP("Buffer Top-Up", 2);

    companion object {
        fun fromRaw(raw: String): SchedulePriority =
            entries.firstOrNull { it.label == raw || it.name == raw } ?: ORDER_DRIVEN
    }
}

enum class ScheduleStatus(val label: String) {
    SCHEDULED("Scheduled"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    companion object {
        fun fromRaw(raw: String): ScheduleStatus =
            entries.firstOrNull { it.label == raw || it.name == raw } ?: SCHEDULED
    }
}

enum class StockHealthStatus(val badgeText: String) {
    CRITICAL("CRITICAL / SCHEDULE NOW"),
    LOW_WATCH("LOW / WATCH"),
    HEALTHY("HEALTHY")
}

enum class DispatchStatus(val label: String) {
    DELIVERED("Delivered"),
    RETURNED("Returned"),
    HELD("Held");

    companion object {
        fun fromRaw(raw: String): DispatchStatus =
            entries.firstOrNull {
                it.label.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true)
            } ?: DELIVERED
    }
}

enum class ReprocessStatus(val label: String) {
    PENDING_REPROCESS("Pending Reprocess"),
    REPROCESSING("Reprocessing"),
    FLAGGED_TO_INVENTORY("Flagged to Inventory"),
    DAMAGED("Damaged");

    companion object {
        fun fromRaw(raw: String): ReprocessStatus =
            entries.firstOrNull {
                it.label.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true)
            } ?: PENDING_REPROCESS
    }
}

object DispatchReturnReasons {
    val options = listOf(
        "Wrong Item",
        "Item with Bad Order",
        "Old Products",
        "Quantity Variance",
        "Other"
    )
}

object PlantLineRemarks {
    val options = listOf(
        "MIXING/CONVEYOR",
        "GRINDING /MIXING",
        "GRINDING/BLOWER FAN",
        "GRINDING/BLOWER FAN/HAMMERMILL"
    )
}

object PlantCustomers {
    val directory = listOf(
        "3K Agrivet Supply",
        "AJL Food Manufacturing Corp.",
        "Allan De Guzman",
        "Apolinario J. Conception",
        "Beth Cano",
        "Black Brick Corporation",
        "Bulaon Farm",
        "C & B Marble ( Stock Order )",
        "C/o Bokal Rc Castro",
        "Capas Pet Supplies and Accessories Shop",
        "Chowmin Agri Trading",
        "Dadas Agricultural and Poultry Supply",
        "Double R",
        "Edelyn's Feeds Agri Supply and General Merchandise",
        "Edwin Villanueva",
        "Elmer Pablo",
        "EMG Poultry Supply",
        "Erin Poultry and Agri Suplly",
        "FAB Poultry Supply",
        "Farmer's Choice Agri & General Merchandise",
        "Galang Agricultural Supply",
        "Gwyneth's Agricultural Supply",
        "Happy Tails",
        "IMC Agri Poultry",
        "J. Cantoria Feed Supply",
        "Jarmar Poultry Supply",
        "Jayson Castro Meat Trading",
        "Jocel Agricultural and Poultry Supply",
        "John Rey Andrade",
        "Johny Buenaventura",
        "Juanito Oliva",
        "Kean / Capas Feeds Supply",
        "KFC",
        "Kuya Dong's Agri-Poultry Supply",
        "Lito's Poultry and Agricultural Supply",
        "Lucas Poultry Supply",
        "M. Valderama Poultry and Feeds Supply",
        "M.P Venturina Agri & Poultry Supply",
        "Magpayo Agri Trading",
        "Maqor Agri Trading",
        "Mark Bernardo",
        "Meo Agri-Poultry Supply",
        "Mico-wyn Store and Poultry Supply",
        "N'Zhar Agri Supply",
        "Nora Garcia",
        "North-Metro Agriventures",
        "Parulan Trading",
        "PSV Agri Supply",
        "R&S",
        "Romy Nacoma",
        "Seignior Manufacturing Corp.",
        "Stock Transfer /10k agri Pandacaqui",
        "Stock Transfer /10k agri San Roque",
        "Taclay Poultry Supply",
        "Tirso Ricardo (Ricky) A. Dela Cruz",
        "Triple J Agri Supply",
        "Vet Health Corporation",
        "Vice Mayor Edison Veneracion",
        "Villanueva, Michael"
    )
}

@Keep
@IgnoreExtraProperties
data class SkuEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productName: String = "",
    val section: String = PlantSection.SAN_ROQUE.label,
    val openingStockKg: Double = 0.0,
    val standardBagWeightKg: Double = 50.0,
    val targetRecoveryPct: Double = 98.5,
    val safetyBufferPct: Double = 15.0,
    val defaultRemarks: String = "MIXING/CONVEYOR",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DispatchRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val date: String = "",
    val productName: String = "",
    val bagsDispatched: Long = 0L,
    val bagWeightKg: Double = 50.0,
    val totalWeightKg: Double = 0.0,
    val drReference: String = "",
    val customer: String = "",
    val linkedOrderId: String = "",
    val sourcePlant: String = "",
    val driverName: String = "",
    val truckNumber: String = "",
    val helperName: String = "",
    val remarks: String = "",
    val dispatchStatus: String = DispatchStatus.DELIVERED.label,
    val hasReturn: Boolean = false,
    val returnConfirmed: Boolean = false,
    val returnedBags: Long = 0L,
    val returnedWeightKg: Double = 0.0,
    val returnReason: String = "",
    val isHeld: Boolean = false,
    val heldRemarks: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun effectiveDispatchedWeightKg(): Double {
        val returnedKg = if (returnConfirmed && returnedWeightKg > 0.0) returnedWeightKg else 0.0
        return kotlin.math.max(0.0, totalWeightKg - returnedKg)
    }

    fun resolvedStatusLabel(): String = when {
        isHeld || dispatchStatus.equals(DispatchStatus.HELD.label, ignoreCase = true) -> DispatchStatus.HELD.label
        (hasReturn && returnConfirmed) || dispatchStatus.equals(DispatchStatus.RETURNED.label, ignoreCase = true) -> DispatchStatus.RETURNED.label
        else -> DispatchStatus.DELIVERED.label
    }
}

@Keep
@IgnoreExtraProperties
data class ReprocessItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sourceDispatchId: String = "",
    val drReference: String = "",
    val customer: String = "",
    val productName: String = "",
    val section: String = PlantSection.SAN_ROQUE.label,
    val returnedDate: String = "",
    val returnedBags: Long = 0L,
    val bagWeightKg: Double = 50.0,
    val returnedWeightKg: Double = 0.0,
    val reprocessedBags: Long = 0L,
    val reprocessedWeightKg: Double = 0.0,
    val returnReason: String = "",
    val status: String = ReprocessStatus.PENDING_REPROCESS.label,
    val fastActionNeeded: Boolean = true,
    val flaggedToInventory: Boolean = false,
    val reprocessedDate: String = "",
    val actionNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isDamaged(): Boolean =
        !flaggedToInventory && status.equals(ReprocessStatus.DAMAGED.label, ignoreCase = true)

    fun isPending(): Boolean =
        !flaggedToInventory && !isDamaged()

    fun effectiveReprocessedWeightKg(): Double = when {
        !flaggedToInventory -> 0.0
        reprocessedWeightKg > 0.0 -> reprocessedWeightKg
        else -> returnedWeightKg
    }

    fun effectiveReprocessedBags(): Long = when {
        !flaggedToInventory -> 0L
        reprocessedBags > 0L -> reprocessedBags
        else -> returnedBags
    }
}

@Keep
@IgnoreExtraProperties
data class CustomerOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderCode: String = "",
    val orderDate: String = "",
    val dueDate: String = "",
    val productName: String = "",
    val orderedBags: Long = 0L,
    val bagWeightKg: Double = 50.0,
    val totalWeightKg: Double = 0.0,
    val dispatchedWeightKg: Double = 0.0,
    val customer: String = "",
    val status: String = OrderStatus.OPEN.label,
    val priority: Boolean = false,
    val rushOrder: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ProductionBatch(
    @DocumentId val id: String = "",
    val userId: String = "",
    val date: String = "",
    val section: String = PlantSection.SAN_ROQUE.label,
    val productName: String = "",
    val bagsProduced: Long = 0L,
    val bagWeightKg: Double = 50.0,
    val expectedWeightKg: Double = 0.0,
    val actualRecoveredKg: Double = 0.0,
    val varianceKg: Double = 0.0,
    val recoveryPct: Double = 100.0,
    val remarks: String = "MIXING/CONVEYOR",
    val scheduleRunId: String = "",
    val linkedOrderIds: List<String> = emptyList(),
    val linkedOrderCodes: List<String> = emptyList(),
    val scheduledForCustomers: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun allLinkedOrderIds(): List<String> =
        linkedOrderIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()

    fun allLinkedOrderCodes(): List<String> =
        linkedOrderCodes.map { it.trim() }.filter { it.isNotBlank() }.distinct()

    fun allScheduledForCustomers(): List<String> =
        scheduledForCustomers.map { it.trim() }.filter { it.isNotBlank() }.distinct()
}

@Keep
@IgnoreExtraProperties
data class ScheduleRun(
    @DocumentId val id: String = "",
    val userId: String = "",
    val scheduledDate: String = "",
    val section: String = PlantSection.SAN_ROQUE.label,
    val productName: String = "",
    val targetBags: Long = 0L,
    val bagWeightKg: Double = 50.0,
    val expectedWeightKg: Double = 0.0,
    val lineRemarks: String = "MIXING/CONVEYOR",
    val priority: String = SchedulePriority.ORDER_DRIVEN.label,
    val status: String = ScheduleStatus.SCHEDULED.label,
    val linkedOrderId: String = "",
    val linkedOrderCode: String = "",
    val linkedOrderIds: List<String> = emptyList(),
    val linkedOrderCodes: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun allLinkedOrderIds(): List<String> =
        (linkedOrderIds + linkedOrderId).map { it.trim() }.filter { it.isNotBlank() }.distinct()

    fun allLinkedOrderCodes(): List<String> =
        (linkedOrderCodes + linkedOrderCode).map { it.trim() }.filter { it.isNotBlank() }.distinct()
}

@Keep
@IgnoreExtraProperties
data class PlantSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val seededOctober2026: Boolean = false,
    val lookbackDays: Long = 14L,
    val defaultSafetyBufferPct: Double = 15.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CustomerEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val notes: String = "",
    val priority: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class SkuInventorySummary(
    val sku: SkuEntity,
    val openingStockKg: Double,
    val recoveredKg: Double,
    val dispatchedKg: Double,
    val availableStockKg: Double,
    val availableBags: Double,
    val avgDailyDispatchKg: Double,
    val daysOfCover: Double,
    val openOrdersKg: Double,
    val safetyBufferKg: Double,
    val netProductionNeededKg: Double,
    val suggestedBags: Long,
    val avgRecoveryPct: Double,
    val recommendedPriority: SchedulePriority?,
    val status: StockHealthStatus,
    val pendingReprocessKg: Double = 0.0,
    val pendingReprocessBags: Long = 0L,
    val pendingFastActionCount: Int = 0,
    val reprocessedKg: Double = 0.0,
    val reprocessedBags: Long = 0L,
    val hasReprocessedFlag: Boolean = false,
    val damagedReprocessKg: Double = 0.0,
    val damagedReprocessBags: Long = 0L
)

data class PlantKpis(
    val totalAvailableStockKg: Double,
    val totalAvailableBags: Double,
    val totalDispatchedKg: Double,
    val totalOpenOrdersKg: Double,
    val averageRecoveryPct: Double,
    val skusNeedingProduction: Int,
    val criticalSkuCount: Int
)

data class SectionReportSummary(
    val section: PlantSection,
    val totalBags: Long,
    val totalExpectedKg: Double,
    val totalRecoveredKg: Double,
    val totalVarianceKg: Double,
    val recoveryPct: Double,
    val batches: List<ProductionBatch>,
    val skuCount: Int = 0,
    val totalAvailableStockKg: Double = 0.0,
    val totalAvailableBags: Double = 0.0
)

data class ProductReportRow(
    val productName: String,
    val section: String,
    val producedBags: Long,
    val recoveredKg: Double,
    val varianceKg: Double,
    val dispatchedKg: Double,
    val openOrdersKg: Double,
    val availableStockKg: Double,
    val availableBags: Double = availableStockKg / 50.0
)

enum class ReportSectionOption(val label: String) {
    INVENTORY("Inventory"),
    ORDERS("Orders"),
    SCHEDULE("Schedule"),
    DISPATCHES("Dispatches"),
    RECOVERY("Recovery")
}

data class PlantReportData(
    val startDate: String,
    val endDate: String,
    val sectionFilter: String,
    val sectionSummaries: List<SectionReportSummary>,
    val productRows: List<ProductReportRow>,
    val totalProducedBags: Long,
    val totalRecoveredKg: Double,
    val totalDispatchedKg: Double,
    val totalOpenOrdersKg: Double,
    val isDailyReport: Boolean = startDate == endDate,
    val dispatches: List<DispatchRecord> = emptyList(),
    val orders: List<CustomerOrder> = emptyList(),
    val activeOpenOrders: List<CustomerOrder> = emptyList(),
    val schedules: List<ScheduleRun> = emptyList(),
    val activeOrderShortfallKg: Double = 0.0,
    val deliveredCount: Int = 0,
    val returnedCount: Int = 0,
    val heldCount: Int = 0,
    val returnedKg: Double = 0.0,
    val scheduledQueueBags: Long = 0L,
    val scheduledActiveCount: Int = 0,
    val scheduledCompletedCount: Int = 0,
    val healthySkuCount: Int = 0,
    val watchSkuCount: Int = 0,
    val criticalSkuCount: Int = 0,
    val includedSections: Set<ReportSectionOption> = ReportSectionOption.entries.toSet()
) {
    fun includes(option: ReportSectionOption): Boolean = option in includedSections
}

data class AiPlantInsight(
    val executiveSummary: String,
    val stockoutAlerts: List<String>,
    val recoveryWarnings: List<String>,
    val shiftRecommendations: List<String>
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false
)

data class DailySkuLogRow(
    val sku: SkuEntity,
    val closingStockKg: Double,
    val closingBags: Double,
    val producedDayKg: Double,
    val producedDayBags: Long,
    val dispatchedDayKg: Double,
    val dispatchedDayBags: Long,
    val returnedDayKg: Double,
    val openOrdersSnapshotKg: Double,
    val editedOnDay: Boolean,
    val editedTimeLabel: String = ""
)

data class DailyOrderLogRow(
    val order: CustomerOrder,
    val placedOnDay: Boolean,
    val dueOnDay: Boolean,
    val editedOnDay: Boolean,
    val editedTimeLabel: String = "",
    val dispatchedOnDayKg: Double,
    val dispatchedOnDayBags: Long
)

data class DailyPlantLog(
    val date: String,
    val isToday: Boolean,
    val skuRows: List<DailySkuLogRow>,
    val orderRows: List<DailyOrderLogRow>,
    val totalClosingStockKg: Double,
    val totalClosingBags: Double,
    val totalProducedDayKg: Double,
    val totalDispatchedDayKg: Double,
    val totalOrdersLoggedKg: Double,
    val totalOpenOrdersSnapshotKg: Double,
    val skusEditedCount: Int,
    val ordersEditedCount: Int,
    val ordersPlacedCount: Int,
    val ordersDueCount: Int,
    val linkedDispatchOrdersCount: Int
)

enum class BreadcrumbActor(val label: String) {
    USER("User"),
    SYSTEM("System");

    companion object {
        fun fromRaw(raw: String): BreadcrumbActor =
            entries.firstOrNull {
                it.label.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true)
            } ?: USER
    }
}

enum class BreadcrumbCategory(val label: String) {
    NAVIGATION("Navigation"),
    INVENTORY("Inventory"),
    ORDERS("Orders"),
    DISPATCH("Dispatch"),
    SCHEDULE("Schedule"),
    RECOVERY("Recovery"),
    REPROCESS("Reprocess"),
    CUSTOMERS("Customers"),
    REPORTS("Reports"),
    AI_OPS("AI Ops"),
    SYSTEM("System");

    companion object {
        fun fromRaw(raw: String): BreadcrumbCategory =
            entries.firstOrNull {
                it.label.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true)
            } ?: SYSTEM
    }
}

enum class BreadcrumbSeverity(val label: String) {
    INFO("Info"),
    SUCCESS("Success"),
    WARNING("Warning"),
    ERROR("Error");

    companion object {
        fun fromRaw(raw: String): BreadcrumbSeverity =
            entries.firstOrNull {
                it.label.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true)
            } ?: INFO
    }
}

@Keep
@IgnoreExtraProperties
data class BreadcrumbEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val actorType: String = BreadcrumbActor.USER.label,
    val category: String = BreadcrumbCategory.SYSTEM.label,
    val action: String = "",
    val details: String = "",
    val screenTab: String = "",
    val entityRef: String = "",
    val trailPath: String = "",
    val severity: String = BreadcrumbSeverity.INFO.label,
    val dateStr: String = "",
    val timeLabel: String = "",
    val timestampMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class RawMaterialCategory(val label: String) {
    ENERGY("Grains & Energy"),
    PROTEIN("Protein Meals"),
    MINERAL("Minerals & Salt"),
    ADDITIVE("Premix & Additives"),
    LIQUID("Liquids & Oils");

    companion object {
        fun fromRaw(raw: String): RawMaterialCategory =
            entries.firstOrNull {
                it.label.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true)
            } ?: ENERGY
    }
}

enum class RawMaterialRiskStatus(val label: String) {
    CRITICAL("Order Now"),
    LOW_WATCH("Running Low"),
    HEALTHY("Healthy")
}

@Keep
@IgnoreExtraProperties
data class RawMaterialEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = RawMaterialCategory.ENERGY.label,
    val currentStockKg: Double = 0.0,
    val reorderThresholdKg: Double = 500.0,
    val usageSharePct: Double = 10.0,
    val leadTimeDays: Long = 4L,
    val safetyDays: Long = 3L,
    val supplierName: String = "",
    val alertsEnabled: Boolean = true,
    val lastRestockedDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class RawMaterialConsumptionDay(
    val isoDate: String,
    val shortLabel: String,
    val feedProducedKg: Double,
    val materialConsumedKg: Double
)

data class RawMaterialProjection(
    val material: RawMaterialEntity,
    val historicalAvgDailyKg: Double,
    val simulatedAvgDailyKg: Double,
    val recentTotalConsumedKg: Double,
    val scheduledDemandKg: Double,
    val daysUntilRunOut: Double,
    val projectedRunOutIsoDate: String?,
    val projectedRunOutLabel: String,
    val reorderTriggerKg: Double,
    val recommendedRestockKg: Double,
    val status: RawMaterialRiskStatus,
    val statusReason: String,
    val dailyTrend: List<RawMaterialConsumptionDay>
)

@Keep
@IgnoreExtraProperties
data class BomIngredientLine(
    val materialId: String = "",
    val materialName: String = "",
    val category: String = RawMaterialCategory.ENERGY.label,
    val inclusionPct: Double = 0.0,
    val notes: String = ""
)

@Keep
@IgnoreExtraProperties
data class FeedBomEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productName: String = "",
    val section: String = PlantSection.SAN_ROQUE.label,
    val batchBaseKg: Double = 1000.0,
    val yieldLossPct: Double = 1.5,
    val lines: List<BomIngredientLine> = emptyList(),
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun totalInclusionPct(): Double = lines.sumOf { it.inclusionPct }
}

enum class ResetScopeTarget(
    val label: String,
    val description: String,
    val isDataMutation: Boolean
) {
    VIEW_FILTERS(
        label = "Active Tab Filters & Parameters",
        description = "Resets search bar, filter chips, 14d window & 1.0x simulator pace",
        isDataMutation = false
    ),
    SKUS(
        label = "Finished Goods Inventory (SKUs)",
        description = "Restores default finished feed SKUs for all 4 plant sections",
        isDataMutation = true
    ),
    RAW_MATERIALS(
        label = "All Raw Materials (Macro & Micro)",
        description = "Restores all 52 Macro & Micro catalog raw materials",
        isDataMutation = true
    ),
    MACRO_MATERIALS(
        label = "Macro Materials Inventory",
        description = "Restores the 9 Macro catalog materials only",
        isDataMutation = true
    ),
    MICRO_MATERIALS(
        label = "Micro Materials Inventory",
        description = "Restores the 43 Micro materials across Fast, Slow & Non-Moving sectors",
        isDataMutation = true
    ),
    FEED_BOMS(
        label = "Feed BOM Formulas",
        description = "Restores default Macro & Micro Feed BOM recipes",
        isDataMutation = true
    ),
    ORDERS(
        label = "Customer Orders",
        description = "Clears all customer orders",
        isDataMutation = true
    ),
    SCHEDULES(
        label = "Production Schedule",
        description = "Clears all scheduled, active & completed milling runs",
        isDataMutation = true
    ),
    DISPATCHES(
        label = "Dispatches & Deliveries",
        description = "Clears all delivery & return records",
        isDataMutation = true
    ),
    REPROCESS_ITEMS(
        label = "Returned & Reprocess Queue",
        description = "Clears all returned products in Reprocess & inventory reprocess flags",
        isDataMutation = true
    ),
    PRODUCTION_BATCHES(
        label = "Production & Recovery",
        description = "Clears all logged production & recovery batches",
        isDataMutation = true
    ),
    AI_CHAT(
        label = "AI Ops Chat & Insights",
        description = "Clears Gemini chat conversation & analysis card",
        isDataMutation = false
    ),
    HISTORY(
        label = "Activity History",
        description = "Clears all recorded action breadcrumbs",
        isDataMutation = true
    )
}

enum class WorkspaceRole(
    val label: String,
    val summary: String
) {
    ADMIN(
        label = "Admin",
        summary = "Full control: operations, SKU delete, settings, resets & team access"
    ),
    OPERATOR(
        label = "Operator",
        summary = "Create and edit orders, dispatches, schedules, batches, reprocess & stock"
    ),
    VIEWER(
        label = "Viewer",
        summary = "Read-only access to plant dashboards, reports & snapshots"
    );

    val canWriteOperations: Boolean
        get() = this == ADMIN || this == OPERATOR

    val canManageTeam: Boolean
        get() = this == ADMIN

    val canResetWorkspace: Boolean
        get() = this == ADMIN

    val canDeleteSkus: Boolean
        get() = this == ADMIN

    val canEditSettings: Boolean
        get() = this == ADMIN

    companion object {
        fun fromRaw(raw: String): WorkspaceRole =
            entries.firstOrNull {
                it.label.equals(raw.trim(), ignoreCase = true) ||
                    it.name.equals(raw.trim(), ignoreCase = true)
            } ?: VIEWER
    }
}

enum class WorkspaceInviteStatus(val label: String) {
    PENDING("Pending"),
    ACTIVE("Active");

    companion object {
        fun fromRaw(raw: String): WorkspaceInviteStatus =
            entries.firstOrNull {
                it.label.equals(raw.trim(), ignoreCase = true) ||
                    it.name.equals(raw.trim(), ignoreCase = true)
            } ?: PENDING
    }
}

@Keep
@IgnoreExtraProperties
data class WorkspaceMember(
    @DocumentId val id: String = "",
    val userId: String = "",
    val workspaceOwnerId: String = "",
    val inviteId: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = WorkspaceRole.OPERATOR.label,
    val invitedByEmail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedRole(): WorkspaceRole = WorkspaceRole.fromRaw(role)
}

@Keep
@IgnoreExtraProperties
data class WorkspaceInvite(
    @DocumentId val id: String = "",
    val workspaceOwnerId: String = "",
    val workspaceName: String = "Bulacan Plant",
    val ownerEmail: String = "",
    val memberEmail: String = "",
    val memberDisplayName: String = "",
    val role: String = WorkspaceRole.VIEWER.label,
    val status: String = WorkspaceInviteStatus.PENDING.label,
    val memberUid: String = "",
    val invitedByEmail: String = "",
    val emailDeliveryStatus: String = "",
    val emailError: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedRole(): WorkspaceRole = WorkspaceRole.fromRaw(role)
    fun resolvedStatus(): WorkspaceInviteStatus = WorkspaceInviteStatus.fromRaw(status)
}

data class SharedWorkspaceOption(
    val workspaceOwnerId: String,
    val workspaceName: String,
    val ownerEmail: String,
    val role: WorkspaceRole,
    val isPersonalOwner: Boolean,
    val inviteId: String = ""
)


