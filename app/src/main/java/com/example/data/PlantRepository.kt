package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.BomIngredientLine
import com.example.model.BreadcrumbActor
import com.example.model.BreadcrumbCategory
import com.example.model.BreadcrumbEntry
import com.example.model.BreadcrumbSeverity
import com.example.model.CustomerEntity
import com.example.model.CustomerOrder
import com.example.model.DispatchRecord
import com.example.model.DispatchStatus
import com.example.model.FeedBomEntity
import com.example.model.October2026SeedData
import com.example.model.OrderStatus
import com.example.model.PlantCustomers
import com.example.model.PlantMaterialCatalog
import com.example.model.PlantSection
import com.example.model.PlantSettings
import com.example.model.ProductionBatch
import com.example.model.RawMaterialCategory
import com.example.model.RawMaterialEntity
import com.example.model.ReprocessItem
import com.example.model.ReprocessStatus
import com.example.model.ResetScopeTarget
import com.example.model.ScheduleRun
import com.example.model.ScheduleStatus
import com.example.model.SkuEntity
import com.example.model.WorkspaceInvite
import com.example.model.WorkspaceInviteStatus
import com.example.model.WorkspaceMember
import com.example.model.WorkspaceRole
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.example.domain.OperationsCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

data class DispatchBatchItem(
    val record: DispatchRecord,
    val linkedOrder: CustomerOrder? = null,
    val createRushOrder: Boolean = false,
    val fixedRushOrder: CustomerOrder? = null,
    val createReprocessOnReturn: Boolean = false,
    val reprocessSection: String = PlantSection.SAN_ROQUE.label,
    val fixedReprocessItem: ReprocessItem? = null
)

data class SingleDispatchOutcome(
    val dispatchId: String,
    val createdRushOrder: CustomerOrder? = null,
    val createdReprocessItem: ReprocessItem? = null
)

data class BatchDispatchOutcome(
    val createdRecords: List<DispatchRecord>,
    val createdRushOrders: List<CustomerOrder>,
    val createdReprocessItems: List<ReprocessItem> = emptyList()
)

class PlantRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireSignedInUid(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun requireUid(workspaceOwnerId: String): String {
        requireSignedInUid()
        val trimmed = workspaceOwnerId.trim()
        require(trimmed.isNotEmpty()) { "Workspace owner ID is required." }
        return trimmed
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun observeSkus(userId: String): Flow<List<SkuEntity>> =
        userDoc(userId).collection("skus")
            .snapshots()
            .map { snap ->
                snap.toObjects(SkuEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeDispatches(userId: String): Flow<List<DispatchRecord>> =
        userDoc(userId).collection("dispatches")
            .snapshots()
            .map { snap ->
                snap.toObjects(DispatchRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.date }
            }

    fun observeOrders(userId: String): Flow<List<CustomerOrder>> =
        userDoc(userId).collection("orders")
            .snapshots()
            .map { snap ->
                snap.toObjects(CustomerOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.dueDate }
            }

    fun observeBatches(userId: String): Flow<List<ProductionBatch>> =
        userDoc(userId).collection("production_batches")
            .snapshots()
            .map { snap ->
                snap.toObjects(ProductionBatch::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.date }
            }

    fun observeSchedules(userId: String): Flow<List<ScheduleRun>> =
        userDoc(userId).collection("schedules")
            .snapshots()
            .map { snap ->
                snap.toObjects(ScheduleRun::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.scheduledDate }
            }

    fun observeCustomers(userId: String): Flow<List<CustomerEntity>> =
        userDoc(userId).collection("customers")
            .snapshots()
            .map { snap ->
                snap.toObjects(CustomerEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.name.lowercase() }
            }

    fun observeRawMaterials(userId: String): Flow<List<RawMaterialEntity>> =
        userDoc(userId).collection("raw_materials")
            .snapshots()
            .map { snap ->
                snap.toObjects(RawMaterialEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.name.lowercase() }
            }

    fun observeBoms(userId: String): Flow<List<FeedBomEntity>> =
        userDoc(userId).collection("boms")
            .snapshots()
            .map { snap ->
                snap.toObjects(FeedBomEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.productName.lowercase() }
            }

    fun observeReprocessItems(userId: String): Flow<List<ReprocessItem>> =
        userDoc(userId).collection("reprocess_items")
            .snapshots()
            .map { snap ->
                snap.toObjects(ReprocessItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareBy<ReprocessItem> { it.flaggedToInventory }
                            .thenByDescending { it.fastActionNeeded }
                            .thenByDescending { it.returnedDate }
                    )
            }

    fun observeSettings(userId: String): Flow<PlantSettings?> =
        userDoc(userId).collection("settings").document("plant_state")
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(PlantSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }

    fun observeBreadcrumbs(userId: String): Flow<List<BreadcrumbEntry>> =
        userDoc(userId).collection("breadcrumbs")
            .snapshots()
            .map { snap ->
                snap.toObjects(BreadcrumbEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareByDescending<BreadcrumbEntry> { it.timestampMillis }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
            }

    suspend fun ensureSeededOctober2026(userId: String): Result<Unit> {
        val path = "users/$userId/settings/plant_state"
        return try {
            val callerUid = requireSignedInUid()
            val uid = requireUid(userId)
            // Only the workspace owner initializes/seeds their own plant baseline.
            if (callerUid != uid) {
                return Result.success(Unit)
            }
            val stateDoc = userDoc(uid).collection("settings").document("plant_state").get().await()
            if (stateDoc.exists() && stateDoc.getBoolean("seededOctober2026") == true) {
                val existingSkus = userDoc(uid).collection("skus").get().await()
                val validSections = setOf("SAN ROQUE", "PANDACAQUI", "LA UNION QUAIL FEEDS", "SELECTA")
                val hasLegacyRecords = existingSkus.isEmpty || existingSkus.documents.any {
                    it.getString("section") !in validSections
                }
                if (!hasLegacyRecords) {
                    ensureCustomersSeeded(uid)
                    ensureRawMaterialsSeeded(uid)
                    ensureBomsSeeded(uid)
                    return Result.success(Unit)
                }
                clearAllUserCollections(uid)
            }
            seedOctober2026Internal(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun ensureCustomersSeeded(uid: String) {
        val snap = userDoc(uid).collection("customers").limit(1).get().await()
        if (!snap.isEmpty) return
        val uDoc = userDoc(uid)
        val batch = db.batch()
        PlantCustomers.directory.forEachIndexed { index, custName ->
            val docId = String.format("cust_%03d", index + 1)
            batch.set(
                uDoc.collection("customers").document(docId),
                customerToCreateMap(uid, custName, "")
            )
        }
        batch.commit().await()
    }

    private suspend fun ensureRawMaterialsSeeded(uid: String) {
        val snap = userDoc(uid).collection("raw_materials").limit(1).get().await()
        if (!snap.isEmpty) return
        val uDoc = userDoc(uid)
        val batch = db.batch()
        October2026SeedData.createRawMaterials(uid).forEach { (id, mat) ->
            batch.set(
                uDoc.collection("raw_materials").document(id),
                rawMaterialToCreateMap(uid, mat)
            )
        }
        batch.commit().await()
    }

    private suspend fun ensureBomsSeeded(uid: String) {
        val snap = userDoc(uid).collection("boms").limit(1).get().await()
        if (!snap.isEmpty) return
        val uDoc = userDoc(uid)
        val batch = db.batch()
        October2026SeedData.createBoms(uid).forEach { (id, bom) ->
            batch.set(
                uDoc.collection("boms").document(id),
                bomToCreateMap(uid, bom)
            )
        }
        batch.commit().await()
    }

    suspend fun resetToOctober2026Seed(userId: String): Result<Unit> {
        val path = "users/$userId/settings/plant_state"
        return try {
            val uid = requireUid(userId)
            clearAllUserCollections(uid)
            seedOctober2026Internal(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun resetSelectiveTargets(userId: String, targets: Set<ResetScopeTarget>): Result<Unit> {
        val dataTargets = targets.filter { it.isDataMutation }.toSet()
        if (dataTargets.isEmpty()) return Result.success(Unit)
        val path = "users/$userId/settings/plant_state"
        return try {
            val uid = requireUid(userId)
            val uDoc = userDoc(uid)
            val resetAllRaw = ResetScopeTarget.RAW_MATERIALS in dataTargets ||
                (ResetScopeTarget.MACRO_MATERIALS in dataTargets && ResetScopeTarget.MICRO_MATERIALS in dataTargets)
            val resetMacroOnly = !resetAllRaw && ResetScopeTarget.MACRO_MATERIALS in dataTargets
            val resetMicroOnly = !resetAllRaw && ResetScopeTarget.MICRO_MATERIALS in dataTargets

            val collectionsToClear = buildList {
                if (ResetScopeTarget.SKUS in dataTargets) add("skus")
                if (resetAllRaw) add("raw_materials")
                if (ResetScopeTarget.FEED_BOMS in dataTargets) add("boms")
                if (ResetScopeTarget.ORDERS in dataTargets) add("orders")
                if (ResetScopeTarget.SCHEDULES in dataTargets) add("schedules")
                if (ResetScopeTarget.DISPATCHES in dataTargets) add("dispatches")
                if (ResetScopeTarget.PRODUCTION_BATCHES in dataTargets) add("production_batches")
                if (ResetScopeTarget.REPROCESS_ITEMS in dataTargets) add("reprocess_items")
                if (ResetScopeTarget.HISTORY in dataTargets) add("breadcrumbs")
            }

            val refsToDelete = mutableListOf<com.google.firebase.firestore.DocumentReference>()
            for (col in collectionsToClear) {
                val snap = uDoc.collection(col).get().await()
                snap.documents.forEach { refsToDelete.add(it.reference) }
            }
            if (resetMacroOnly || resetMicroOnly) {
                val rawSnap = uDoc.collection("raw_materials").get().await()
                rawSnap.documents.forEach { doc ->
                    val entity = doc.toObject(RawMaterialEntity::class.java)
                    val isMicro = entity?.let { PlantMaterialCatalog.inferSector(it).isMicro } ?: false
                    if ((resetMacroOnly && !isMicro) || (resetMicroOnly && isMicro)) {
                        refsToDelete.add(doc.reference)
                    }
                }
            }
            refsToDelete.chunked(350).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it) }
                batch.commit().await()
            }

            val writes = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any>>>()
            if (ResetScopeTarget.SKUS in dataTargets) {
                October2026SeedData.createSkus(uid).forEach { (id, sku) ->
                    writes.add(uDoc.collection("skus").document(id) to skuToCreateMap(uid, sku))
                }
            }
            if (resetAllRaw) {
                October2026SeedData.createRawMaterials(uid).forEach { (id, mat) ->
                    writes.add(uDoc.collection("raw_materials").document(id) to rawMaterialToCreateMap(uid, mat))
                }
            } else if (resetMacroOnly || resetMicroOnly) {
                October2026SeedData.createRawMaterials(uid).forEach { (id, mat) ->
                    val isMicro = PlantMaterialCatalog.inferSector(mat).isMicro
                    if ((resetMacroOnly && !isMicro) || (resetMicroOnly && isMicro)) {
                        writes.add(uDoc.collection("raw_materials").document(id) to rawMaterialToCreateMap(uid, mat))
                    }
                }
            }
            if (ResetScopeTarget.FEED_BOMS in dataTargets) {
                October2026SeedData.createBoms(uid).forEach { (id, bom) ->
                    writes.add(uDoc.collection("boms").document(id) to bomToCreateMap(uid, bom))
                }
            }

            writes.chunked(350).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { (ref, map) -> batch.set(ref, map) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun clearAllUserCollections(uid: String) {
        val collections = listOf("skus", "dispatches", "orders", "production_batches", "schedules", "customers", "raw_materials", "boms", "reprocess_items")
        val refs = mutableListOf<com.google.firebase.firestore.DocumentReference>()
        for (col in collections) {
            val snap = userDoc(uid).collection(col).get().await()
            snap.documents.forEach { refs.add(it.reference) }
        }
        refs.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.delete(it) }
            batch.commit().await()
        }
    }

    private suspend fun seedOctober2026Internal(uid: String) {
        val uDoc = userDoc(uid)
        val writes = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any>>>()

        October2026SeedData.createSkus(uid).forEach { (id, sku) ->
            writes.add(uDoc.collection("skus").document(id) to skuToCreateMap(uid, sku))
        }
        October2026SeedData.createBatches(uid).forEach { (id, b) ->
            writes.add(uDoc.collection("production_batches").document(id) to batchToCreateMap(uid, b))
        }
        October2026SeedData.createDispatches(uid).forEach { (id, d) ->
            writes.add(uDoc.collection("dispatches").document(id) to dispatchToCreateMap(uid, d))
        }
        October2026SeedData.createOrders(uid).forEach { (id, o) ->
            writes.add(uDoc.collection("orders").document(id) to orderToCreateMap(uid, o))
        }
        October2026SeedData.createSchedules(uid).forEach { (id, s) ->
            writes.add(uDoc.collection("schedules").document(id) to scheduleToCreateMap(uid, s))
        }
        October2026SeedData.createRawMaterials(uid).forEach { (id, mat) ->
            writes.add(uDoc.collection("raw_materials").document(id) to rawMaterialToCreateMap(uid, mat))
        }
        October2026SeedData.createBoms(uid).forEach { (id, bom) ->
            writes.add(uDoc.collection("boms").document(id) to bomToCreateMap(uid, bom))
        }
        PlantCustomers.directory.forEachIndexed { index, custName ->
            val docId = String.format("cust_%03d", index + 1)
            writes.add(uDoc.collection("customers").document(docId) to customerToCreateMap(uid, custName, ""))
        }

        val settingsRef = uDoc.collection("settings").document("plant_state")
        val settingsExists = settingsRef.get().await().exists()

        val chunks = if (writes.isEmpty()) listOf(emptyList()) else writes.chunked(350)
        chunks.forEachIndexed { index, chunk ->
            val batch = db.batch()
            chunk.forEach { (ref, map) -> batch.set(ref, map) }
            if (index == chunks.lastIndex) {
                if (settingsExists) {
                    batch.update(
                        settingsRef,
                        mapOf(
                            "seededOctober2026" to true,
                            "lookbackDays" to 14L,
                            "defaultSafetyBufferPct" to 15.0,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    batch.set(
                        settingsRef,
                        mapOf(
                            "userId" to uid,
                            "seededOctober2026" to true,
                            "lookbackDays" to 14L,
                            "defaultSafetyBufferPct" to 15.0,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
        }
    }

    suspend fun updateSettings(userId: String, lookbackDays: Long, defaultSafetyBufferPct: Double): Result<Unit> {
        val path = "users/$userId/settings/plant_state"
        return try {
            val uid = requireUid(userId)
            require(lookbackDays in 1..90) { "Lookback days must be between 1 and 90" }
            require(defaultSafetyBufferPct in 0.0..100.0) { "Safety buffer must be 0-100%" }
            val settingsRef = userDoc(uid).collection("settings").document("plant_state")
            if (settingsRef.get().await().exists()) {
                settingsRef.update(
                    mapOf(
                        "lookbackDays" to lookbackDays,
                        "defaultSafetyBufferPct" to defaultSafetyBufferPct,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                settingsRef.set(
                    mapOf(
                        "userId" to uid,
                        "seededOctober2026" to true,
                        "lookbackDays" to lookbackDays,
                        "defaultSafetyBufferPct" to defaultSafetyBufferPct,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addSku(userId: String, sku: SkuEntity, fixedDocId: String? = null): Result<String> {
        val docId = fixedDocId?.takeIf { it.isNotBlank() }
            ?: "sku_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "users/$userId/skus/$docId"
        return try {
            val uid = requireUid(userId)
            require(sku.productName.trim().length in 1..120) { "Product name must be 1-120 characters" }
            require(sku.openingStockKg in 0.0..10000000.0) { "Opening stock must be 0-10,000,000 kg" }
            require(sku.standardBagWeightKg > 0.0 && sku.standardBagWeightKg <= 2000.0) { "Bag weight must be 0-2,000 kg" }
            require(sku.targetRecoveryPct > 0.0 && sku.targetRecoveryPct <= 120.0) { "Target recovery must be 0-120%" }
            require(sku.safetyBufferPct in 0.0..200.0) { "Safety buffer must be 0-200%" }
            require(sku.defaultRemarks.length <= 120) { "Remarks must be <= 120 characters" }

            userDoc(uid).collection("skus").document(docId)
                .set(skuToCreateMap(uid, sku))
                .await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSku(userId: String, skuId: String, sku: SkuEntity): Result<Unit> {
        val path = "users/$userId/skus/$skuId"
        return try {
            val uid = requireUid(userId)
            require(skuId.isNotBlank()) { "SKU ID is required" }
            require(sku.productName.trim().length in 1..120) { "Product name must be 1-120 characters" }
            require(sku.openingStockKg in 0.0..10000000.0) { "Opening stock must be 0-10,000,000 kg" }
            require(sku.standardBagWeightKg > 0.0 && sku.standardBagWeightKg <= 2000.0) { "Bag weight must be 0-2,000 kg" }
            require(sku.targetRecoveryPct > 0.0 && sku.targetRecoveryPct <= 120.0) { "Target recovery must be 0-120%" }
            require(sku.safetyBufferPct in 0.0..200.0) { "Safety buffer must be 0-200%" }
            require(sku.defaultRemarks.length <= 120) { "Remarks must be <= 120 characters" }

            userDoc(uid).collection("skus").document(skuId).update(
                mapOf(
                    "productName" to sku.productName.trim().take(120),
                    "section" to sku.section,
                    "openingStockKg" to sku.openingStockKg,
                    "standardBagWeightKg" to sku.standardBagWeightKg,
                    "targetRecoveryPct" to sku.targetRecoveryPct,
                    "safetyBufferPct" to sku.safetyBufferPct,
                    "defaultRemarks" to sku.defaultRemarks.trim().take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSku(userId: String, skuId: String): Result<Unit> {
        val path = "users/$userId/skus/$skuId"
        return try {
            val uid = requireUid(userId)
            require(skuId.isNotBlank()) { "SKU ID is required" }
            userDoc(uid).collection("skus").document(skuId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun addCustomer(
        userId: String,
        name: String,
        notes: String,
        priority: Boolean = false,
        fixedDocId: String? = null
    ): Result<String> {
        val docId = fixedDocId?.takeIf { it.isNotBlank() }
            ?: "cust_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "users/$userId/customers/$docId"
        return try {
            val uid = requireUid(userId)
            val cleanName = name.trim()
            val cleanNotes = notes.trim().take(120)
            require(cleanName.length in 1..120) { "Customer name must be 1–120 characters" }
            userDoc(uid).collection("customers").document(docId)
                .set(customerToCreateMap(uid, cleanName, cleanNotes, priority))
                .await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCustomer(
        userId: String,
        customerId: String,
        name: String,
        notes: String,
        priority: Boolean = false
    ): Result<Unit> {
        val path = "users/$userId/customers/$customerId"
        return try {
            val uid = requireUid(userId)
            require(customerId.isNotBlank()) { "Customer ID is required" }
            val cleanName = name.trim()
            val cleanNotes = notes.trim().take(120)
            require(cleanName.length in 1..120) { "Customer name must be 1–120 characters" }
            userDoc(uid).collection("customers").document(customerId).update(
                mapOf(
                    "name" to cleanName,
                    "notes" to cleanNotes,
                    "priority" to priority,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(userId: String, customerId: String): Result<Unit> {
        val path = "users/$userId/customers/$customerId"
        return try {
            val uid = requireUid(userId)
            require(customerId.isNotBlank()) { "Customer ID is required" }
            userDoc(uid).collection("customers").document(customerId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun logDispatch(
        userId: String,
        record: DispatchRecord,
        linkedOrder: CustomerOrder? = null,
        fixedDocId: String? = null,
        createRushOrder: Boolean = false,
        fixedRushOrder: CustomerOrder? = null,
        createReprocessOnReturn: Boolean = false,
        reprocessSection: String = PlantSection.SAN_ROQUE.label,
        fixedReprocessItem: ReprocessItem? = null
    ): Result<SingleDispatchOutcome> {
        val docId = fixedDocId?.takeIf { it.isNotBlank() }
            ?: "disp_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "users/$userId/dispatches/$docId"
        return try {
            val uid = requireUid(userId)
            val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
            require(dateRegex.matches(record.date)) { "Dispatch date must be YYYY-MM-DD" }
            require(record.productName.trim().length in 1..120) { "Product name must be 1–120 chars" }
            require(record.bagsDispatched in 1..1_000_000) { "Bags dispatched must be 1–1,000,000" }
            require(record.bagWeightKg > 0.0 && record.bagWeightKg <= 2000.0) { "Bag weight must be <= 2,000 kg" }
            require(record.totalWeightKg > 0.0 && record.totalWeightKg <= 50_000_000.0) { "Total weight must be <= 50,000,000 kg" }
            require(record.drReference.trim().length in 1..80) { "DR reference must be 1–80 chars" }
            require(record.customer.trim().length in 1..120) { "Customer must be 1–120 chars" }
            require(!record.isHeld || record.heldRemarks.trim().isNotEmpty()) { "Held remarks required when dispatch is held" }
            require(!record.returnConfirmed || record.returnReason.trim().isNotEmpty()) { "Return reason required when return is confirmed" }

            val createdRushOrder: CustomerOrder? = when {
                linkedOrder != null -> null
                fixedRushOrder != null -> fixedRushOrder.copy(userId = uid)
                createRushOrder -> {
                    val rushOrdId = "ord_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                    val rushCode = OperationsCalculator.buildRushOrderCode(record.drReference)
                    OperationsCalculator.buildRushOrderFromDispatch(uid, rushOrdId, rushCode, record)
                }
                else -> null
            }

            val cleanSourcePlant = normalizeSourcePlant(record.sourcePlant)
            val normalizedRecord = record.copy(
                id = docId,
                userId = uid,
                productName = record.productName.trim().take(120),
                drReference = record.drReference.trim().take(80),
                customer = record.customer.trim().take(120),
                linkedOrderId = createdRushOrder?.id ?: linkedOrder?.id ?: record.linkedOrderId,
                sourcePlant = cleanSourcePlant
            )

            val createdReprocess: ReprocessItem? = when {
                fixedReprocessItem != null -> normalizeReprocessItem(
                    uid,
                    fixedReprocessItem.copy(id = fixedReprocessItem.id.ifBlank { "rep_${UUID.randomUUID().toString().replace("-", "").take(12)}" })
                )
                createReprocessOnReturn && normalizedRecord.returnConfirmed && normalizedRecord.returnedBags > 0L -> {
                    val repId = "rep_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                    val retKg = if (normalizedRecord.returnedWeightKg > 0.0) {
                        normalizedRecord.returnedWeightKg
                    } else {
                        normalizedRecord.returnedBags * normalizedRecord.bagWeightKg
                    }
                    normalizeReprocessItem(
                        uid,
                        ReprocessItem(
                            id = repId,
                            userId = uid,
                            sourceDispatchId = docId,
                            drReference = normalizedRecord.drReference,
                            customer = normalizedRecord.customer,
                            productName = normalizedRecord.productName,
                            section = PlantSection.fromRaw(reprocessSection).label,
                            returnedDate = normalizedRecord.date,
                            returnedBags = normalizedRecord.returnedBags,
                            bagWeightKg = normalizedRecord.bagWeightKg,
                            returnedWeightKg = retKg,
                            returnReason = normalizedRecord.returnReason.ifBlank { "Customer Return" },
                            status = ReprocessStatus.PENDING_REPROCESS.label,
                            fastActionNeeded = true,
                            flaggedToInventory = false
                        )
                    )
                }
                else -> null
            }
            if (createdReprocess != null) {
                validateReprocessInput(createdReprocess)
            }

            val batch = db.batch()
            val dispatchRef = userDoc(uid).collection("dispatches").document(docId)
            batch.set(dispatchRef, dispatchToCreateMap(uid, normalizedRecord))

            if (createdRushOrder != null) {
                val rushRef = userDoc(uid).collection("orders").document(createdRushOrder.id)
                batch.set(rushRef, orderToCreateMap(uid, createdRushOrder))
            }

            if (createdReprocess != null) {
                val repRef = userDoc(uid).collection("reprocess_items").document(createdReprocess.id)
                batch.set(repRef, reprocessToCreateMap(uid, createdReprocess))
            }

            val netKg = normalizedRecord.effectiveDispatchedWeightKg()
            if (netKg > 0.0 && linkedOrder != null && linkedOrder.id.isNotBlank() &&
                (linkedOrder.status == OrderStatus.OPEN.label || linkedOrder.status == OrderStatus.PARTIALLY_DISPATCHED.label)
            ) {
                val newDispatched = (linkedOrder.dispatchedWeightKg + netKg).coerceAtMost(50_000_000.0)
                val newStatus = if (newDispatched >= linkedOrder.totalWeightKg - 0.01) {
                    OrderStatus.FULFILLED.label
                } else {
                    OrderStatus.PARTIALLY_DISPATCHED.label
                }
                val orderRef = userDoc(uid).collection("orders").document(linkedOrder.id)
                batch.update(
                    orderRef,
                    mapOf(
                        "dispatchedWeightKg" to newDispatched,
                        "status" to newStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(
                SingleDispatchOutcome(
                    dispatchId = docId,
                    createdRushOrder = createdRushOrder,
                    createdReprocessItem = createdReprocess
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun logDispatchesBatch(
        userId: String,
        entries: List<DispatchBatchItem>,
        preserveIds: Boolean = false
    ): Result<BatchDispatchOutcome> {
        val path = "users/$userId/dispatches"
        return try {
            val uid = requireUid(userId)
            require(entries.isNotEmpty()) { "Select at least one product to dispatch" }
            val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")

            val totalNewRush = entries.count { it.linkedOrder == null && it.createRushOrder && it.fixedRushOrder == null }
            var rushCounter = 0

            val prepared = entries.map { item ->
                val record = item.record
                val linkedOrder = item.linkedOrder
                require(dateRegex.matches(record.date)) { "Dispatch date must be YYYY-MM-DD" }
                require(record.productName.trim().length in 1..120) { "Product name must be 1–120 chars" }
                require(record.bagsDispatched in 1..1_000_000) { "Bags dispatched must be 1–1,000,000" }
                require(record.bagWeightKg > 0.0 && record.bagWeightKg <= 2000.0) { "Bag weight must be <= 2,000 kg" }
                require(record.totalWeightKg > 0.0 && record.totalWeightKg <= 50_000_000.0) { "Total weight must be <= 50,000,000 kg" }
                require(record.drReference.trim().length in 1..80) { "DR reference must be 1–80 chars" }
                require(record.customer.trim().length in 1..120) { "Customer must be 1–120 chars" }
                require(!record.isHeld || record.heldRemarks.trim().isNotEmpty()) { "Held remarks required when dispatch is held" }
                require(!record.returnConfirmed || record.returnReason.trim().isNotEmpty()) { "Return reason required when return is confirmed" }

                val docId = if (preserveIds && record.id.isNotBlank()) {
                    record.id
                } else {
                    "disp_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                }

                val rushOrder: CustomerOrder? = when {
                    linkedOrder != null -> null
                    item.fixedRushOrder != null -> item.fixedRushOrder.copy(userId = uid)
                    item.createRushOrder -> {
                        val rushOrdId = "ord_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                        val rushCode = OperationsCalculator.buildRushOrderCode(
                            drReference = record.drReference,
                            rushIndex = rushCounter++,
                            totalRushCount = totalNewRush
                        )
                        OperationsCalculator.buildRushOrderFromDispatch(uid, rushOrdId, rushCode, record)
                    }
                    else -> null
                }

                val normalized = record.copy(
                    id = docId,
                    userId = uid,
                    productName = record.productName.trim().take(120),
                    drReference = record.drReference.trim().take(80),
                    customer = record.customer.trim().take(120),
                    linkedOrderId = rushOrder?.id ?: linkedOrder?.id ?: record.linkedOrderId,
                    sourcePlant = normalizeSourcePlant(record.sourcePlant)
                )
                val reprocessItem: ReprocessItem? = when {
                    item.fixedReprocessItem != null -> normalizeReprocessItem(
                        uid,
                        item.fixedReprocessItem.copy(
                            id = item.fixedReprocessItem.id.ifBlank {
                                "rep_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                            }
                        )
                    )
                    item.createReprocessOnReturn && normalized.returnConfirmed && normalized.returnedBags > 0L -> {
                        val repId = "rep_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                        val retKg = if (normalized.returnedWeightKg > 0.0) {
                            normalized.returnedWeightKg
                        } else {
                            normalized.returnedBags * normalized.bagWeightKg
                        }
                        normalizeReprocessItem(
                            uid,
                            ReprocessItem(
                                id = repId,
                                userId = uid,
                                sourceDispatchId = docId,
                                drReference = normalized.drReference,
                                customer = normalized.customer,
                                productName = normalized.productName,
                                section = PlantSection.fromRaw(item.reprocessSection).label,
                                returnedDate = normalized.date,
                                returnedBags = normalized.returnedBags,
                                bagWeightKg = normalized.bagWeightKg,
                                returnedWeightKg = retKg,
                                returnReason = normalized.returnReason.ifBlank { "Customer Return" },
                                status = ReprocessStatus.PENDING_REPROCESS.label,
                                fastActionNeeded = true,
                                flaggedToInventory = false
                            )
                        )
                    }
                    else -> null
                }
                if (reprocessItem != null) {
                    validateReprocessInput(reprocessItem)
                }
                Triple(normalized, linkedOrder, rushOrder to reprocessItem)
            }

            val orderIncrements = linkedMapOf<String, Pair<CustomerOrder, Double>>()
            prepared.forEach { (rec, ord, _) ->
                val netKg = rec.effectiveDispatchedWeightKg()
                if (netKg > 0.0 && ord != null && ord.id.isNotBlank() &&
                    (ord.status == OrderStatus.OPEN.label || ord.status == OrderStatus.PARTIALLY_DISPATCHED.label)
                ) {
                    val existingPair = orderIncrements[ord.id]
                    val accKg = (existingPair?.second ?: 0.0) + netKg
                    orderIncrements[ord.id] = ord to accKg
                }
            }

            val ops = mutableListOf<(com.google.firebase.firestore.WriteBatch) -> Unit>()
            val createdRushOrders = mutableListOf<CustomerOrder>()
            val createdReprocessItems = mutableListOf<ReprocessItem>()
            prepared.forEach { (normalized, _, extras) ->
                val (rushOrder, reprocessItem) = extras
                ops.add { b ->
                    b.set(userDoc(uid).collection("dispatches").document(normalized.id), dispatchToCreateMap(uid, normalized))
                }
                if (rushOrder != null && rushOrder.id.isNotBlank()) {
                    createdRushOrders.add(rushOrder)
                    ops.add { b ->
                        b.set(userDoc(uid).collection("orders").document(rushOrder.id), orderToCreateMap(uid, rushOrder))
                    }
                }
                if (reprocessItem != null && reprocessItem.id.isNotBlank()) {
                    createdReprocessItems.add(reprocessItem)
                    ops.add { b ->
                        b.set(userDoc(uid).collection("reprocess_items").document(reprocessItem.id), reprocessToCreateMap(uid, reprocessItem))
                    }
                }
            }
            orderIncrements.forEach { (orderId, pair) ->
                val (ord, addedKg) = pair
                val newDispatched = (ord.dispatchedWeightKg + addedKg).coerceAtMost(50_000_000.0)
                val newStatus = if (newDispatched >= ord.totalWeightKg - 0.01) {
                    OrderStatus.FULFILLED.label
                } else {
                    OrderStatus.PARTIALLY_DISPATCHED.label
                }
                val orderRef = userDoc(uid).collection("orders").document(orderId)
                ops.add { b ->
                    b.update(
                        orderRef,
                        mapOf(
                            "dispatchedWeightKg" to newDispatched,
                            "status" to newStatus,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            ops.chunked(350).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { applyOp -> applyOp(batch) }
                batch.commit().await()
            }
            Result.success(
                BatchDispatchOutcome(
                    createdRecords = prepared.map { it.first },
                    createdRushOrders = createdRushOrders,
                    createdReprocessItems = createdReprocessItems
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteDispatchesBatch(
        userId: String,
        records: List<DispatchRecord>,
        previousOrders: List<CustomerOrder>,
        orderIdsToDelete: List<String> = emptyList(),
        reprocessIdsToDelete: List<String> = emptyList()
    ): Result<Unit> {
        val path = "users/$userId/dispatches"
        return try {
            val uid = requireUid(userId)
            val ops = mutableListOf<(com.google.firebase.firestore.WriteBatch) -> Unit>()
            records.filter { it.id.isNotBlank() }.forEach { rec ->
                ops.add { b -> b.delete(userDoc(uid).collection("dispatches").document(rec.id)) }
            }
            orderIdsToDelete.filter { it.isNotBlank() }.distinct().forEach { rushOrdId ->
                ops.add { b -> b.delete(userDoc(uid).collection("orders").document(rushOrdId)) }
            }
            reprocessIdsToDelete.filter { it.isNotBlank() }.distinct().forEach { repId ->
                ops.add { b -> b.delete(userDoc(uid).collection("reprocess_items").document(repId)) }
            }
            previousOrders.distinctBy { it.id }.filter { it.id.isNotBlank() }.forEach { prevOrd ->
                val ref = userDoc(uid).collection("orders").document(prevOrd.id)
                ops.add { b -> b.delete(ref) }
                ops.add { b -> b.set(ref, orderToCreateMap(uid, prevOrd)) }
            }
            ops.chunked(300).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { applyOp -> applyOp(batch) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun updateDispatch(
        userId: String,
        dispatchId: String,
        record: DispatchRecord,
        previousRecord: DispatchRecord? = null,
        linkedOrder: CustomerOrder? = null
    ): Result<Unit> {
        val path = "users/$userId/dispatches/$dispatchId"
        return try {
            val uid = requireUid(userId)
            require(dispatchId.isNotBlank()) { "Dispatch ID required" }
            val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
            require(dateRegex.matches(record.date)) { "Dispatch date must be YYYY-MM-DD" }
            require(record.productName.trim().length in 1..120) { "Product name must be 1–120 chars" }
            require(record.bagsDispatched in 1..1_000_000) { "Bags dispatched must be 1–1,000,000" }
            require(record.bagWeightKg > 0.0 && record.bagWeightKg <= 2000.0) { "Bag weight must be <= 2,000 kg" }
            require(record.totalWeightKg > 0.0 && record.totalWeightKg <= 50_000_000.0) { "Total weight must be <= 50,000,000 kg" }
            require(record.drReference.trim().length in 1..80) { "DR reference must be 1–80 chars" }
            require(record.customer.trim().length in 1..120) { "Customer must be 1–120 chars" }
            require(!record.isHeld || record.heldRemarks.trim().isNotEmpty()) { "Held remarks required when dispatch is held" }
            require(!record.returnConfirmed || record.returnReason.trim().isNotEmpty()) { "Return reason required when return is confirmed" }

            val cleanSourcePlant = normalizeSourcePlant(record.sourcePlant)
            userDoc(uid).collection("dispatches").document(dispatchId)
                .update(
                    mapOf(
                        "date" to record.date,
                        "productName" to record.productName.trim(),
                        "bagsDispatched" to record.bagsDispatched,
                        "bagWeightKg" to record.bagWeightKg,
                        "totalWeightKg" to record.totalWeightKg,
                        "drReference" to record.drReference.trim(),
                        "customer" to record.customer.trim(),
                        "linkedOrderId" to record.linkedOrderId,
                        "sourcePlant" to cleanSourcePlant,
                        "driverName" to record.driverName.trim().take(120),
                        "truckNumber" to record.truckNumber.trim().take(40),
                        "helperName" to record.helperName.trim().take(120),
                        "remarks" to record.remarks.trim().take(300),
                        "dispatchStatus" to record.resolvedStatusLabel(),
                        "hasReturn" to record.hasReturn,
                        "returnConfirmed" to record.returnConfirmed,
                        "returnedBags" to record.returnedBags,
                        "returnedWeightKg" to record.returnedWeightKg,
                        "returnReason" to record.returnReason.trim().take(120),
                        "isHeld" to record.isHeld,
                        "heldRemarks" to record.heldRemarks.trim().take(300),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            if (linkedOrder != null && linkedOrder.id.isNotBlank() && previousRecord != null &&
                linkedOrder.status != OrderStatus.CANCELLED.label
            ) {
                val deltaKg = record.effectiveDispatchedWeightKg() - previousRecord.effectiveDispatchedWeightKg()
                if (kotlin.math.abs(deltaKg) > 0.01) {
                    val newDispatched = (linkedOrder.dispatchedWeightKg + deltaKg).coerceIn(0.0, 50_000_000.0)
                    val newStatus = when {
                        newDispatched >= linkedOrder.totalWeightKg - 0.01 -> OrderStatus.FULFILLED.label
                        newDispatched > 0.0 -> OrderStatus.PARTIALLY_DISPATCHED.label
                        else -> OrderStatus.OPEN.label
                    }
                    val orderRef = userDoc(uid).collection("orders").document(linkedOrder.id)
                    if (linkedOrder.status == OrderStatus.FULFILLED.label) {
                        val updatedOrder = linkedOrder.copy(dispatchedWeightKg = newDispatched, status = newStatus)
                        orderRef.delete().await()
                        orderRef.set(orderToCreateMap(uid, updatedOrder)).await()
                    } else {
                        orderRef.update(
                            mapOf(
                                "dispatchedWeightKg" to newDispatched,
                                "status" to newStatus,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteDispatch(
        userId: String,
        dispatchId: String,
        deletedRecord: DispatchRecord? = null,
        linkedOrder: CustomerOrder? = null
    ): Result<Unit> {
        val path = "users/$userId/dispatches/$dispatchId"
        return try {
            val uid = requireUid(userId)
            require(dispatchId.isNotBlank()) { "Dispatch ID required" }
            userDoc(uid).collection("dispatches").document(dispatchId).delete().await()
            if (deletedRecord != null && linkedOrder != null && linkedOrder.id.isNotBlank() &&
                linkedOrder.status != OrderStatus.CANCELLED.label
            ) {
                val removeKg = deletedRecord.effectiveDispatchedWeightKg()
                if (removeKg > 0.01) {
                    val newDispatched = (linkedOrder.dispatchedWeightKg - removeKg).coerceAtLeast(0.0)
                    val newStatus = when {
                        newDispatched >= linkedOrder.totalWeightKg - 0.01 -> OrderStatus.FULFILLED.label
                        newDispatched > 0.0 -> OrderStatus.PARTIALLY_DISPATCHED.label
                        else -> OrderStatus.OPEN.label
                    }
                    val updatedOrder = linkedOrder.copy(dispatchedWeightKg = newDispatched, status = newStatus)
                    val orderRef = userDoc(uid).collection("orders").document(linkedOrder.id)
                    if (linkedOrder.status == OrderStatus.FULFILLED.label) {
                        orderRef.delete().await()
                        orderRef.set(orderToCreateMap(uid, updatedOrder)).await()
                    } else {
                        orderRef.update(
                            mapOf(
                                "dispatchedWeightKg" to newDispatched,
                                "status" to newStatus,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun undoLogDispatch(
        userId: String,
        dispatchId: String,
        previousOrder: CustomerOrder?,
        createdRushOrderId: String? = null,
        createdReprocessId: String? = null
    ): Result<Unit> {
        val path = "users/$userId/dispatches/$dispatchId"
        return try {
            val uid = requireUid(userId)
            require(dispatchId.isNotBlank()) { "Dispatch ID required" }
            userDoc(uid).collection("dispatches").document(dispatchId).delete().await()
            if (!createdRushOrderId.isNullOrBlank()) {
                userDoc(uid).collection("orders").document(createdRushOrderId).delete().await()
            }
            if (!createdReprocessId.isNullOrBlank()) {
                userDoc(uid).collection("reprocess_items").document(createdReprocessId).delete().await()
            }
            if (previousOrder != null && previousOrder.id.isNotBlank()) {
                val orderRef = userDoc(uid).collection("orders").document(previousOrder.id)
                orderRef.delete().await()
                orderRef.set(orderToCreateMap(uid, previousOrder)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun addOrder(userId: String, order: CustomerOrder, fixedDocId: String? = null): Result<String> {
        val docId = fixedDocId?.takeIf { it.isNotBlank() }
            ?: "ord_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "users/$userId/orders/$docId"
        return try {
            val uid = requireUid(userId)
            val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
            require(order.orderCode.trim().length in 1..40) { "Order code must be 1–40 chars" }
            require(dateRegex.matches(order.orderDate)) { "Order date must be YYYY-MM-DD" }
            require(dateRegex.matches(order.dueDate)) { "Due date must be YYYY-MM-DD" }
            require(order.productName.trim().length in 1..120) { "Product name must be 1–120 chars" }
            require(order.orderedBags in 1..1_000_000) { "Ordered bags must be 1–1,000,000" }
            require(order.bagWeightKg > 0.0 && order.bagWeightKg <= 2000.0) { "Bag weight must be <= 2,000 kg" }
            require(order.totalWeightKg > 0.0 && order.totalWeightKg <= 50_000_000.0) { "Total weight must be <= 50,000,000 kg" }
            require(order.customer.trim().length in 1..120) { "Customer must be 1–120 chars" }

            userDoc(uid).collection("orders").document(docId)
                .set(orderToCreateMap(uid, order))
                .await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun addOrdersBatch(
        userId: String,
        orders: List<CustomerOrder>,
        preserveIds: Boolean = false
    ): Result<List<CustomerOrder>> {
        val path = "users/$userId/orders"
        return try {
            val uid = requireUid(userId)
            require(orders.isNotEmpty()) { "Select at least one item to order" }
            val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
            val prepared = orders.map { order ->
                val cleanCode = order.orderCode.trim().take(40)
                val cleanProd = order.productName.trim().take(120)
                val cleanCustomer = order.customer.trim().take(120)
                require(cleanCode.length in 1..40) { "Order code must be 1–40 chars" }
                require(dateRegex.matches(order.orderDate)) { "Order date must be YYYY-MM-DD" }
                require(dateRegex.matches(order.dueDate)) { "Due date must be YYYY-MM-DD" }
                require(cleanProd.length in 1..120) { "Product name must be 1–120 chars" }
                require(order.orderedBags in 1..1_000_000) { "Ordered bags must be 1–1,000,000" }
                require(order.bagWeightKg > 0.0 && order.bagWeightKg <= 2000.0) { "Bag weight must be <= 2,000 kg" }
                require(order.totalWeightKg > 0.0 && order.totalWeightKg <= 50_000_000.0) { "Total weight must be <= 50,000,000 kg" }
                require(cleanCustomer.length in 1..120) { "Customer must be 1–120 chars" }

                val docId = if (preserveIds && order.id.isNotBlank()) {
                    order.id
                } else {
                    "ord_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                }
                val normalized = order.copy(
                    id = docId,
                    userId = uid,
                    orderCode = cleanCode,
                    productName = cleanProd,
                    customer = cleanCustomer
                )
                docId to normalized
            }

            prepared.chunked(350).forEach { chunk ->
                val writeBatch = db.batch()
                chunk.forEach { (docId, normalized) ->
                    writeBatch.set(
                        userDoc(uid).collection("orders").document(docId),
                        orderToCreateMap(uid, normalized)
                    )
                }
                writeBatch.commit().await()
            }
            Result.success(prepared.map { it.second })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteOrdersBatch(userId: String, orderIds: List<String>): Result<Unit> {
        val path = "users/$userId/orders"
        return try {
            val uid = requireUid(userId)
            val ids = orderIds.filter { it.isNotBlank() }.distinct()
            ids.chunked(350).forEach { chunk ->
                val writeBatch = db.batch()
                chunk.forEach { id ->
                    writeBatch.delete(userDoc(uid).collection("orders").document(id))
                }
                writeBatch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun recreateOrder(userId: String, order: CustomerOrder): Result<Unit> {
        val path = "users/$userId/orders/${order.id}"
        return try {
            val uid = requireUid(userId)
            require(order.id.isNotBlank()) { "Order ID required" }
            val ref = userDoc(uid).collection("orders").document(order.id)
            ref.delete().await()
            ref.set(orderToCreateMap(uid, order)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateOrder(
        userId: String,
        orderId: String,
        dueDate: String,
        orderedBags: Long,
        bagWeightKg: Double,
        totalWeightKg: Double,
        customer: String,
        status: String,
        priority: Boolean,
        rushOrder: Boolean = false
    ): Result<Unit> {
        val path = "users/$userId/orders/$orderId"
        return try {
            val uid = requireUid(userId)
            require(orderId.isNotBlank()) { "Order ID required" }
            require(Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(dueDate)) { "Due date must be YYYY-MM-DD" }
            require(orderedBags in 1..1_000_000) { "Ordered bags must be 1–1,000,000" }
            require(bagWeightKg > 0.0 && bagWeightKg <= 2000.0) { "Bag weight must be <= 2,000 kg" }
            require(totalWeightKg > 0.0 && totalWeightKg <= 50_000_000.0) { "Total weight must be <= 50,000,000 kg" }
            require(customer.trim().length in 1..120) { "Customer must be 1–120 chars" }
            userDoc(uid).collection("orders").document(orderId).update(
                mapOf(
                    "dueDate" to dueDate,
                    "orderedBags" to orderedBags,
                    "bagWeightKg" to bagWeightKg,
                    "totalWeightKg" to totalWeightKg,
                    "customer" to customer.trim(),
                    "priority" to priority,
                    "rushOrder" to rushOrder,
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteOrder(userId: String, orderId: String): Result<Unit> {
        val path = "users/$userId/orders/$orderId"
        return try {
            val uid = requireUid(userId)
            require(orderId.isNotBlank()) { "Order ID required" }
            userDoc(uid).collection("orders").document(orderId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun addProductionBatch(
        userId: String,
        batch: ProductionBatch,
        fixedDocId: String? = null
    ): Result<String> {
        val docId = fixedDocId?.takeIf { it.isNotBlank() }
            ?: "batch_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "users/$userId/production_batches/$docId"
        return try {
            val uid = requireUid(userId)
            require(Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(batch.date)) { "Batch date must be YYYY-MM-DD" }
            require(batch.productName.trim().length in 1..120) { "Product name must be 1–120 chars" }
            require(batch.bagsProduced in 1..1_000_000) { "Bags produced must be 1–1,000,000" }
            require(batch.bagWeightKg > 0.0 && batch.bagWeightKg <= 2000.0) { "Bag weight must be <= 2,000 kg" }
            require(batch.actualRecoveredKg > 0.0 && batch.actualRecoveredKg <= 50_000_000.0) { "Recovered kg must be <= 50,000,000 kg" }
            require(batch.recoveryPct in 0.0..200.0) { "Recovery % must be 0–200%" }

            userDoc(uid).collection("production_batches").document(docId)
                .set(batchToCreateMap(uid, batch))
                .await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProductionBatch(userId: String, batchId: String): Result<Unit> {
        val path = "users/$userId/production_batches/$batchId"
        return try {
            val uid = requireUid(userId)
            require(batchId.isNotBlank()) { "Batch ID required" }
            userDoc(uid).collection("production_batches").document(batchId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProductionBatch(userId: String, batchId: String, batch: ProductionBatch): Result<Unit> {
        val path = "users/$userId/production_batches/$batchId"
        return try {
            val uid = requireUid(userId)
            require(batchId.isNotBlank()) { "Batch ID is required" }
            require(Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(batch.date)) { "Batch date must be YYYY-MM-DD" }
            require(batch.productName.trim().length in 1..120) { "Product name must be 1–120 chars" }
            require(batch.bagsProduced in 1..1_000_000) { "Bags recovered must be 1–1,000,000" }
            require(batch.bagWeightKg > 0.0 && batch.bagWeightKg <= 2000.0) { "Bag weight must be <= 2,000 kg" }
            require(batch.actualRecoveredKg > 0.0 && batch.actualRecoveredKg <= 50_000_000.0) { "Recovered kg must be <= 50,000,000 kg" }
            require(batch.recoveryPct in 0.0..200.0) { "Recovery % must be 0–200%" }

            val normScheduleRunId = batch.scheduleRunId.trim().take(128)
            val normOrderIds = batch.allLinkedOrderIds().map { it.take(128) }.take(100)
            val normOrderCodes = batch.allLinkedOrderCodes().map { it.take(40) }.take(100)
            val normCustomers = batch.allScheduledForCustomers().map { it.take(120) }.take(100)

            userDoc(uid).collection("production_batches").document(batchId).update(
                mapOf(
                    "date" to batch.date,
                    "section" to batch.section,
                    "productName" to batch.productName.trim().take(120),
                    "bagsProduced" to batch.bagsProduced,
                    "bagWeightKg" to batch.bagWeightKg,
                    "expectedWeightKg" to batch.expectedWeightKg,
                    "actualRecoveredKg" to batch.actualRecoveredKg,
                    "varianceKg" to batch.varianceKg,
                    "recoveryPct" to batch.recoveryPct,
                    "remarks" to batch.remarks.trim().take(120),
                    "scheduleRunId" to normScheduleRunId,
                    "linkedOrderIds" to normOrderIds,
                    "linkedOrderCodes" to normOrderCodes,
                    "scheduledForCustomers" to normCustomers,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addProductionBatchesBulk(userId: String, batches: List<ProductionBatch>): Result<Int> {
        val path = "users/$userId/production_batches"
        return try {
            val uid = requireUid(userId)
            require(batches.isNotEmpty()) { "No valid rows to import" }
            batches.chunked(350).forEach { chunk ->
                val writeBatch = db.batch()
                chunk.forEach { b ->
                    val docId = "batch_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                    writeBatch.set(userDoc(uid).collection("production_batches").document(docId), batchToCreateMap(uid, b))
                }
                writeBatch.commit().await()
            }
            Result.success(batches.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun addScheduleRun(
        userId: String,
        run: ScheduleRun,
        fixedDocId: String? = null
    ): Result<String> {
        val docId = fixedDocId?.takeIf { it.isNotBlank() }
            ?: "sched_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "users/$userId/schedules/$docId"
        return try {
            val uid = requireUid(userId)
            require(Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(run.scheduledDate)) { "Scheduled date must be YYYY-MM-DD" }
            require(run.productName.trim().length in 1..120) { "Product name must be 1–120 chars" }
            require(run.targetBags in 1..1_000_000) { "Target bags must be 1–1,000,000" }
            require(run.bagWeightKg > 0.0 && run.bagWeightKg <= 2000.0) { "Bag weight must be <= 2,000 kg" }
            require(run.expectedWeightKg > 0.0 && run.expectedWeightKg <= 50_000_000.0) { "Expected weight must be <= 50,000,000 kg" }

            val normIds = (run.linkedOrderIds + run.linkedOrderId).map { it.trim().take(128) }.filter { it.isNotBlank() }.distinct().take(100)
            val normCodes = (run.linkedOrderCodes + run.linkedOrderCode).map { it.trim().take(40) }.filter { it.isNotBlank() }.distinct().take(100)
            val normalizedRun = run.copy(
                productName = run.productName.trim().take(120),
                lineRemarks = run.lineRemarks.trim().ifBlank { "MIXING/CONVEYOR" }.take(120),
                linkedOrderId = normIds.firstOrNull().orEmpty(),
                linkedOrderCode = normCodes.firstOrNull().orEmpty(),
                linkedOrderIds = normIds,
                linkedOrderCodes = normCodes
            )
            userDoc(uid).collection("schedules").document(docId)
                .set(scheduleToCreateMap(uid, normalizedRun))
                .await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun addScheduleRunsBatch(
        userId: String,
        runs: List<ScheduleRun>,
        preserveIds: Boolean = false
    ): Result<List<ScheduleRun>> {
        val path = "users/$userId/schedules"
        return try {
            val uid = requireUid(userId)
            require(runs.isNotEmpty()) { "Select at least one item to schedule" }
            val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
            val prepared = runs.map { run ->
                require(dateRegex.matches(run.scheduledDate)) { "Scheduled date must be YYYY-MM-DD" }
                require(run.productName.trim().length in 1..120) { "Product name must be 1–120 chars" }
                require(run.targetBags in 1..1_000_000) { "Target bags must be 1–1,000,000" }
                require(run.bagWeightKg > 0.0 && run.bagWeightKg <= 2000.0) { "Bag weight must be <= 2,000 kg" }
                require(run.expectedWeightKg > 0.0 && run.expectedWeightKg <= 50_000_000.0) { "Expected weight must be <= 50,000,000 kg" }

                val docId = if (preserveIds && run.id.isNotBlank()) {
                    run.id
                } else {
                    "sched_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                }
                val normIds = (run.linkedOrderIds + run.linkedOrderId).map { it.trim().take(128) }.filter { it.isNotBlank() }.distinct().take(100)
                val normCodes = (run.linkedOrderCodes + run.linkedOrderCode).map { it.trim().take(40) }.filter { it.isNotBlank() }.distinct().take(100)
                val normalized = run.copy(
                    id = docId,
                    userId = uid,
                    productName = run.productName.trim().take(120),
                    lineRemarks = run.lineRemarks.trim().ifBlank { "MIXING/CONVEYOR" }.take(120),
                    linkedOrderId = normIds.firstOrNull().orEmpty(),
                    linkedOrderCode = normCodes.firstOrNull().orEmpty(),
                    linkedOrderIds = normIds,
                    linkedOrderCodes = normCodes
                )
                docId to normalized
            }

            prepared.chunked(350).forEach { chunk ->
                val writeBatch = db.batch()
                chunk.forEach { (docId, normalized) ->
                    writeBatch.set(
                        userDoc(uid).collection("schedules").document(docId),
                        scheduleToCreateMap(uid, normalized)
                    )
                }
                writeBatch.commit().await()
            }
            Result.success(prepared.map { it.second })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteScheduleRunsBatch(userId: String, scheduleIds: List<String>): Result<Unit> {
        val path = "users/$userId/schedules"
        return try {
            val uid = requireUid(userId)
            val ids = scheduleIds.filter { it.isNotBlank() }.distinct()
            ids.chunked(350).forEach { chunk ->
                val writeBatch = db.batch()
                chunk.forEach { id ->
                    writeBatch.delete(userDoc(uid).collection("schedules").document(id))
                }
                writeBatch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun recreateScheduleRun(userId: String, run: ScheduleRun): Result<Unit> {
        val path = "users/$userId/schedules/${run.id}"
        return try {
            val uid = requireUid(userId)
            require(run.id.isNotBlank()) { "Schedule ID required" }
            val ref = userDoc(uid).collection("schedules").document(run.id)
            ref.delete().await()
            ref.set(scheduleToCreateMap(uid, run)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateScheduleStatus(userId: String, scheduleId: String, newStatus: ScheduleStatus): Result<Unit> {
        val path = "users/$userId/schedules/$scheduleId"
        return try {
            val uid = requireUid(userId)
            require(scheduleId.isNotBlank()) { "Schedule ID required" }
            userDoc(uid).collection("schedules").document(scheduleId).update(
                mapOf(
                    "status" to newStatus.label,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateScheduleStatusesBatch(
        userId: String,
        scheduleIds: List<String>,
        newStatus: ScheduleStatus
    ): Result<Unit> {
        val path = "users/$userId/schedules"
        return try {
            val uid = requireUid(userId)
            val cleanIds = scheduleIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            require(cleanIds.isNotEmpty()) { "Select at least one scheduled run" }
            cleanIds.chunked(350).forEach { chunk ->
                val writeBatch = db.batch()
                chunk.forEach { schedId ->
                    writeBatch.update(
                        userDoc(uid).collection("schedules").document(schedId),
                        mapOf(
                            "status" to newStatus.label,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                writeBatch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateScheduleRun(
        userId: String,
        scheduleId: String,
        scheduledDate: String,
        targetBags: Long,
        bagWeightKg: Double,
        expectedWeightKg: Double,
        lineRemarks: String,
        priority: String,
        status: String,
        linkedOrderId: String,
        linkedOrderCode: String,
        linkedOrderIds: List<String>,
        linkedOrderCodes: List<String>
    ): Result<Unit> {
        val path = "users/$userId/schedules/$scheduleId"
        return try {
            val uid = requireUid(userId)
            require(scheduleId.isNotBlank()) { "Schedule ID required" }
            require(Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(scheduledDate)) { "Scheduled date must be YYYY-MM-DD" }
            require(targetBags in 1..1_000_000) { "Target bags must be 1–1,000,000" }
            require(bagWeightKg > 0.0 && bagWeightKg <= 2000.0) { "Bag weight must be <= 2,000 kg" }
            require(expectedWeightKg > 0.0 && expectedWeightKg <= 50_000_000.0) { "Expected weight must be <= 50,000,000 kg" }
            val cleanRemarks = lineRemarks.trim().ifBlank { "MIXING/CONVEYOR" }.take(120)
            val normIds = (linkedOrderIds + linkedOrderId).map { it.trim().take(128) }.filter { it.isNotBlank() }.distinct().take(100)
            val normCodes = (linkedOrderCodes + linkedOrderCode).map { it.trim().take(40) }.filter { it.isNotBlank() }.distinct().take(100)
            userDoc(uid).collection("schedules").document(scheduleId).update(
                mapOf(
                    "scheduledDate" to scheduledDate,
                    "targetBags" to targetBags,
                    "bagWeightKg" to bagWeightKg,
                    "expectedWeightKg" to expectedWeightKg,
                    "lineRemarks" to cleanRemarks,
                    "priority" to priority,
                    "status" to status,
                    "linkedOrderId" to normIds.firstOrNull().orEmpty(),
                    "linkedOrderCode" to normCodes.firstOrNull().orEmpty(),
                    "linkedOrderIds" to normIds,
                    "linkedOrderCodes" to normCodes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteScheduleRun(userId: String, scheduleId: String): Result<Unit> {
        val path = "users/$userId/schedules/$scheduleId"
        return try {
            val uid = requireUid(userId)
            require(scheduleId.isNotBlank()) { "Schedule ID required" }
            userDoc(uid).collection("schedules").document(scheduleId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun completeScheduleAndLogBatch(
        userId: String,
        scheduleId: String,
        batch: ProductionBatch,
        fixedBatchId: String? = null
    ): Result<String> {
        val path = "users/$userId/schedules/$scheduleId"
        return try {
            val uid = requireUid(userId)
            require(scheduleId.isNotBlank()) { "Schedule ID required" }
            val batchId = fixedBatchId?.takeIf { it.isNotBlank() }
                ?: "batch_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val writeBatch = db.batch()
            writeBatch.update(
                userDoc(uid).collection("schedules").document(scheduleId),
                mapOf(
                    "status" to ScheduleStatus.COMPLETED.label,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            writeBatch.set(
                userDoc(uid).collection("production_batches").document(batchId),
                batchToCreateMap(uid, batch)
            )
            writeBatch.commit().await()
            Result.success(batchId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun undoCompleteScheduleAndLogBatch(
        userId: String,
        batchId: String,
        previousRun: ScheduleRun
    ): Result<Unit> {
        val path = "users/$userId/schedules/${previousRun.id}"
        return try {
            val uid = requireUid(userId)
            require(batchId.isNotBlank()) { "Batch ID required" }
            require(previousRun.id.isNotBlank()) { "Schedule ID required" }
            userDoc(uid).collection("production_batches").document(batchId).delete().await()
            val schedRef = userDoc(uid).collection("schedules").document(previousRun.id)
            schedRef.delete().await()
            schedRef.set(scheduleToCreateMap(uid, previousRun)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun restoreFromJsonBackup(userId: String, jsonText: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUid(userId)
            val root = JSONObject(jsonText)
            val skusArr = root.getJSONArray("skus")
            val batchesArr = root.getJSONArray("productionBatches")
            val dispatchesArr = root.getJSONArray("dispatches")
            val ordersArr = root.getJSONArray("orders")
            val schedulesArr = root.getJSONArray("schedules")

            clearAllUserCollections(uid)

            val writes = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any>>>()
            val uDoc = userDoc(uid)

            for (i in 0 until skusArr.length()) {
                val obj = skusArr.getJSONObject(i)
                val id = sanitizeId(obj.optString("id"), "sku_$i")
                val sku = SkuEntity(
                    userId = uid,
                    productName = obj.getString("productName"),
                    section = obj.getString("section"),
                    openingStockKg = obj.getDouble("openingStockKg"),
                    standardBagWeightKg = obj.getDouble("standardBagWeightKg"),
                    targetRecoveryPct = obj.getDouble("targetRecoveryPct"),
                    safetyBufferPct = obj.getDouble("safetyBufferPct"),
                    defaultRemarks = obj.optString("defaultRemarks", "MIXING/CONVEYOR")
                )
                writes.add(uDoc.collection("skus").document(id) to skuToCreateMap(uid, sku))
            }

            for (i in 0 until batchesArr.length()) {
                val obj = batchesArr.getJSONObject(i)
                val id = sanitizeId(obj.optString("id"), "batch_$i")
                val batchOrderIds = readJsonStringList(obj.optJSONArray("linkedOrderIds"), 128)
                val batchOrderCodes = readJsonStringList(obj.optJSONArray("linkedOrderCodes"), 40)
                val batchCustomers = readJsonStringList(obj.optJSONArray("scheduledForCustomers"), 120)
                val b = ProductionBatch(
                    userId = uid,
                    date = obj.getString("date"),
                    section = obj.getString("section"),
                    productName = obj.getString("productName"),
                    bagsProduced = obj.getLong("bagsProduced"),
                    bagWeightKg = obj.getDouble("bagWeightKg"),
                    expectedWeightKg = obj.getDouble("expectedWeightKg"),
                    actualRecoveredKg = obj.getDouble("actualRecoveredKg"),
                    varianceKg = obj.getDouble("varianceKg"),
                    recoveryPct = obj.getDouble("recoveryPct"),
                    remarks = obj.optString("remarks", "MIXING/CONVEYOR"),
                    scheduleRunId = obj.optString("scheduleRunId", "").trim().take(128),
                    linkedOrderIds = batchOrderIds,
                    linkedOrderCodes = batchOrderCodes,
                    scheduledForCustomers = batchCustomers
                )
                writes.add(uDoc.collection("production_batches").document(id) to batchToCreateMap(uid, b))
            }

            for (i in 0 until dispatchesArr.length()) {
                val obj = dispatchesArr.getJSONObject(i)
                val id = sanitizeId(obj.optString("id"), "disp_$i")
                val d = DispatchRecord(
                    userId = uid,
                    date = obj.getString("date"),
                    productName = obj.getString("productName"),
                    bagsDispatched = obj.getLong("bagsDispatched"),
                    bagWeightKg = obj.getDouble("bagWeightKg"),
                    totalWeightKg = obj.getDouble("totalWeightKg"),
                    drReference = obj.getString("drReference"),
                    customer = obj.getString("customer"),
                    linkedOrderId = obj.optString("linkedOrderId", ""),
                    sourcePlant = normalizeSourcePlant(obj.optString("sourcePlant", "")),
                    driverName = obj.optString("driverName", ""),
                    truckNumber = obj.optString("truckNumber", ""),
                    helperName = obj.optString("helperName", ""),
                    remarks = obj.optString("remarks", ""),
                    dispatchStatus = obj.optString("dispatchStatus", DispatchStatus.DELIVERED.label),
                    hasReturn = obj.optBoolean("hasReturn", false),
                    returnConfirmed = obj.optBoolean("returnConfirmed", false),
                    returnedBags = obj.optLong("returnedBags", 0L),
                    returnedWeightKg = obj.optDouble("returnedWeightKg", 0.0),
                    returnReason = obj.optString("returnReason", ""),
                    isHeld = obj.optBoolean("isHeld", false),
                    heldRemarks = obj.optString("heldRemarks", "")
                )
                writes.add(uDoc.collection("dispatches").document(id) to dispatchToCreateMap(uid, d))
            }

            for (i in 0 until ordersArr.length()) {
                val obj = ordersArr.getJSONObject(i)
                val id = sanitizeId(obj.optString("id"), "ord_$i")
                val o = CustomerOrder(
                    userId = uid,
                    orderCode = obj.getString("orderCode"),
                    orderDate = obj.getString("orderDate"),
                    dueDate = obj.getString("dueDate"),
                    productName = obj.getString("productName"),
                    orderedBags = obj.getLong("orderedBags"),
                    bagWeightKg = obj.getDouble("bagWeightKg"),
                    totalWeightKg = obj.getDouble("totalWeightKg"),
                    dispatchedWeightKg = obj.getDouble("dispatchedWeightKg"),
                    customer = obj.getString("customer"),
                    status = obj.getString("status"),
                    priority = obj.optBoolean("priority", false),
                    rushOrder = obj.optBoolean("rushOrder", false)
                )
                writes.add(uDoc.collection("orders").document(id) to orderToCreateMap(uid, o))
            }

            for (i in 0 until schedulesArr.length()) {
                val obj = schedulesArr.getJSONObject(i)
                val id = sanitizeId(obj.optString("id"), "sched_$i")
                val idsJson = obj.optJSONArray("linkedOrderIds")
                val codesJson = obj.optJSONArray("linkedOrderCodes")
                val restoredIds = buildList {
                    if (idsJson != null) {
                        for (j in 0 until idsJson.length()) {
                            val v = idsJson.optString(j, "").trim()
                            if (v.isNotBlank()) add(v)
                        }
                    }
                    val single = obj.optString("linkedOrderId", "").trim()
                    if (single.isNotBlank()) add(single)
                }.distinct().take(100)
                val restoredCodes = buildList {
                    if (codesJson != null) {
                        for (j in 0 until codesJson.length()) {
                            val v = codesJson.optString(j, "").trim()
                            if (v.isNotBlank()) add(v)
                        }
                    }
                    val single = obj.optString("linkedOrderCode", "").trim()
                    if (single.isNotBlank()) add(single)
                }.distinct().take(100)
                val s = ScheduleRun(
                    userId = uid,
                    scheduledDate = obj.getString("scheduledDate"),
                    section = obj.getString("section"),
                    productName = obj.getString("productName"),
                    targetBags = obj.getLong("targetBags"),
                    bagWeightKg = obj.getDouble("bagWeightKg"),
                    expectedWeightKg = obj.getDouble("expectedWeightKg"),
                    lineRemarks = obj.getString("lineRemarks"),
                    priority = obj.getString("priority"),
                    status = obj.getString("status"),
                    linkedOrderId = restoredIds.firstOrNull().orEmpty(),
                    linkedOrderCode = restoredCodes.firstOrNull().orEmpty(),
                    linkedOrderIds = restoredIds,
                    linkedOrderCodes = restoredCodes
                )
                writes.add(uDoc.collection("schedules").document(id) to scheduleToCreateMap(uid, s))
            }

            val customersArr = root.optJSONArray("customers")
            if (customersArr != null && customersArr.length() > 0) {
                for (i in 0 until customersArr.length()) {
                    val obj = customersArr.getJSONObject(i)
                    val id = sanitizeId(obj.optString("id"), "cust_$i")
                    val name = obj.optString("name").trim().take(120)
                    val notes = obj.optString("notes", "").trim().take(120)
                    val priority = obj.optBoolean("priority", false)
                    if (name.isNotBlank()) {
                        writes.add(uDoc.collection("customers").document(id) to customerToCreateMap(uid, name, notes, priority))
                    }
                }
            } else {
                PlantCustomers.directory.forEachIndexed { index, custName ->
                    val docId = String.format("cust_%03d", index + 1)
                    writes.add(uDoc.collection("customers").document(docId) to customerToCreateMap(uid, custName, "", false))
                }
            }

            val rawMatsArr = root.optJSONArray("rawMaterials")
            if (rawMatsArr != null && rawMatsArr.length() > 0) {
                for (i in 0 until rawMatsArr.length()) {
                    val obj = rawMatsArr.getJSONObject(i)
                    val id = sanitizeId(obj.optString("id"), "raw_$i")
                    val name = obj.optString("name").trim().take(120)
                    if (name.isNotBlank()) {
                        val mat = RawMaterialEntity(
                            userId = uid,
                            name = name,
                            category = RawMaterialCategory.fromRaw(obj.optString("category", "")).label,
                            currentStockKg = obj.optDouble("currentStockKg", 0.0).coerceIn(0.0, 50_000_000.0),
                            reorderThresholdKg = obj.optDouble("reorderThresholdKg", 500.0).coerceIn(0.0, 50_000_000.0),
                            usageSharePct = obj.optDouble("usageSharePct", 10.0).coerceIn(0.1, 100.0),
                            leadTimeDays = obj.optLong("leadTimeDays", 4L).coerceIn(1L, 60L),
                            safetyDays = obj.optLong("safetyDays", 3L).coerceIn(0L, 30L),
                            supplierName = obj.optString("supplierName", "").trim().take(120),
                            alertsEnabled = obj.optBoolean("alertsEnabled", true),
                            lastRestockedDate = obj.optString("lastRestockedDate", "").takeIf {
                                Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(it)
                            }.orEmpty()
                        )
                        writes.add(uDoc.collection("raw_materials").document(id) to rawMaterialToCreateMap(uid, mat))
                    }
                }
            } else {
                October2026SeedData.createRawMaterials(uid).forEach { (id, mat) ->
                    writes.add(uDoc.collection("raw_materials").document(id) to rawMaterialToCreateMap(uid, mat))
                }
            }

            val bomsArr = root.optJSONArray("boms")
            if (bomsArr != null && bomsArr.length() > 0) {
                for (i in 0 until bomsArr.length()) {
                    val obj = bomsArr.getJSONObject(i)
                    val id = sanitizeId(obj.optString("id"), "bom_$i")
                    val prodName = obj.optString("productName").trim().take(120)
                    val linesArr = obj.optJSONArray("lines")
                    val parsedLines = buildList {
                        if (linesArr != null) {
                            for (j in 0 until linesArr.length()) {
                                val lObj = linesArr.getJSONObject(j)
                                val mName = lObj.optString("materialName").trim().take(120)
                                val pct = lObj.optDouble("inclusionPct", 0.0).coerceIn(0.01, 100.0)
                                if (mName.isNotBlank()) {
                                    add(
                                        BomIngredientLine(
                                            materialId = lObj.optString("materialId", "").trim().take(128),
                                            materialName = mName,
                                            category = RawMaterialCategory.fromRaw(lObj.optString("category", "")).label,
                                            inclusionPct = pct,
                                            notes = lObj.optString("notes", "").trim().take(160)
                                        )
                                    )
                                }
                            }
                        }
                    }.take(40)
                    if (prodName.isNotBlank() && parsedLines.isNotEmpty()) {
                        val bom = FeedBomEntity(
                            userId = uid,
                            productName = prodName,
                            section = PlantSection.fromRaw(obj.optString("section", "")).label,
                            batchBaseKg = obj.optDouble("batchBaseKg", 1000.0).coerceIn(1.0, 100_000.0),
                            yieldLossPct = obj.optDouble("yieldLossPct", 1.5).coerceIn(0.0, 50.0),
                            lines = parsedLines,
                            notes = obj.optString("notes", "").trim().take(300)
                        )
                        writes.add(uDoc.collection("boms").document(id) to bomToCreateMap(uid, bom))
                    }
                }
            } else {
                October2026SeedData.createBoms(uid).forEach { (id, bom) ->
                    writes.add(uDoc.collection("boms").document(id) to bomToCreateMap(uid, bom))
                }
            }

            val reprocessArr = root.optJSONArray("reprocessItems")
            if (reprocessArr != null && reprocessArr.length() > 0) {
                val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
                for (i in 0 until reprocessArr.length()) {
                    val obj = reprocessArr.getJSONObject(i)
                    val id = sanitizeId(obj.optString("id"), "rep_$i")
                    val prodName = obj.optString("productName").trim().take(120)
                    val retDate = obj.optString("returnedDate", "").trim()
                    if (prodName.isNotBlank() && dateRegex.matches(retDate)) {
                        val repBags = obj.optLong("returnedBags", 1L).coerceIn(1L, 1_000_000L)
                        val bagWt = obj.optDouble("bagWeightKg", 50.0).coerceIn(0.1, 2000.0)
                        val retKg = obj.optDouble("returnedWeightKg", repBags * bagWt).coerceIn(0.1, 50_000_000.0)
                        val flagged = obj.optBoolean("flaggedToInventory", false)
                        val status = if (flagged) {
                            ReprocessStatus.FLAGGED_TO_INVENTORY.label
                        } else {
                            ReprocessStatus.fromRaw(obj.optString("status", "")).label
                        }
                        val rep = normalizeReprocessItem(
                            uid,
                            ReprocessItem(
                                id = id,
                                userId = uid,
                                sourceDispatchId = obj.optString("sourceDispatchId", ""),
                                drReference = obj.optString("drReference", ""),
                                customer = obj.optString("customer", ""),
                                productName = prodName,
                                section = PlantSection.fromRaw(obj.optString("section", "")).label,
                                returnedDate = retDate,
                                returnedBags = repBags,
                                bagWeightKg = bagWt,
                                returnedWeightKg = retKg,
                                reprocessedBags = obj.optLong("reprocessedBags", 0L).coerceIn(0L, 1_000_000L),
                                reprocessedWeightKg = obj.optDouble("reprocessedWeightKg", 0.0).coerceIn(0.0, 50_000_000.0),
                                returnReason = obj.optString("returnReason", "Customer Return").ifBlank { "Customer Return" },
                                status = status,
                                fastActionNeeded = if (flagged || status == ReprocessStatus.DAMAGED.label) false else obj.optBoolean("fastActionNeeded", true),
                                flaggedToInventory = flagged,
                                reprocessedDate = obj.optString("reprocessedDate", ""),
                                actionNotes = obj.optString("actionNotes", "")
                            )
                        )
                        writes.add(uDoc.collection("reprocess_items").document(id) to reprocessToCreateMap(uid, rep))
                    }
                }
            }

            writes.chunked(350).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { (ref, map) -> batch.set(ref, map) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private fun sanitizeId(raw: String, fallback: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        return if (cleaned.isNotBlank()) cleaned.take(120) else fallback
    }

    private fun readJsonStringList(arr: org.json.JSONArray?, maxItemLen: Int): List<String> {
        if (arr == null) return emptyList()
        return buildList {
            for (j in 0 until arr.length()) {
                val v = arr.optString(j, "").trim().take(maxItemLen)
                if (v.isNotBlank()) add(v)
            }
        }.distinct().take(100)
    }

    private fun skuToCreateMap(uid: String, s: SkuEntity): Map<String, Any> = mapOf(
        "userId" to uid,
        "productName" to s.productName,
        "section" to s.section,
        "openingStockKg" to s.openingStockKg,
        "standardBagWeightKg" to s.standardBagWeightKg,
        "targetRecoveryPct" to s.targetRecoveryPct,
        "safetyBufferPct" to s.safetyBufferPct,
        "defaultRemarks" to s.defaultRemarks,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun normalizeSourcePlant(raw: String): String {
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) return ""
        return PlantSection.entries.firstOrNull {
            it.label.equals(cleaned, ignoreCase = true) || it.name.equals(cleaned, ignoreCase = true)
        }?.label ?: ""
    }

    private fun dispatchToCreateMap(uid: String, d: DispatchRecord): Map<String, Any> = mapOf(
        "userId" to uid,
        "date" to d.date,
        "productName" to d.productName,
        "bagsDispatched" to d.bagsDispatched,
        "bagWeightKg" to d.bagWeightKg,
        "totalWeightKg" to d.totalWeightKg,
        "drReference" to d.drReference,
        "customer" to d.customer,
        "linkedOrderId" to d.linkedOrderId,
        "sourcePlant" to normalizeSourcePlant(d.sourcePlant),
        "driverName" to d.driverName.trim().take(120),
        "truckNumber" to d.truckNumber.trim().take(40),
        "helperName" to d.helperName.trim().take(120),
        "remarks" to d.remarks.trim().take(300),
        "dispatchStatus" to d.resolvedStatusLabel(),
        "hasReturn" to d.hasReturn,
        "returnConfirmed" to d.returnConfirmed,
        "returnedBags" to d.returnedBags,
        "returnedWeightKg" to d.returnedWeightKg,
        "returnReason" to d.returnReason.trim().take(120),
        "isHeld" to d.isHeld,
        "heldRemarks" to d.heldRemarks.trim().take(300),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun orderToCreateMap(uid: String, o: CustomerOrder): Map<String, Any> = mapOf(
        "userId" to uid,
        "orderCode" to o.orderCode,
        "orderDate" to o.orderDate,
        "dueDate" to o.dueDate,
        "productName" to o.productName,
        "orderedBags" to o.orderedBags,
        "bagWeightKg" to o.bagWeightKg,
        "totalWeightKg" to o.totalWeightKg,
        "dispatchedWeightKg" to o.dispatchedWeightKg,
        "customer" to o.customer,
        "status" to o.status,
        "priority" to o.priority,
        "rushOrder" to o.rushOrder,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun batchToCreateMap(uid: String, b: ProductionBatch): Map<String, Any> = mapOf(
        "userId" to uid,
        "date" to b.date,
        "section" to b.section,
        "productName" to b.productName.trim().take(120),
        "bagsProduced" to b.bagsProduced,
        "bagWeightKg" to b.bagWeightKg,
        "expectedWeightKg" to b.expectedWeightKg,
        "actualRecoveredKg" to b.actualRecoveredKg,
        "varianceKg" to b.varianceKg,
        "recoveryPct" to b.recoveryPct,
        "remarks" to b.remarks.trim().take(120),
        "scheduleRunId" to b.scheduleRunId.trim().take(128),
        "linkedOrderIds" to b.allLinkedOrderIds().map { it.take(128) }.take(100),
        "linkedOrderCodes" to b.allLinkedOrderCodes().map { it.take(40) }.take(100),
        "scheduledForCustomers" to b.allScheduledForCustomers().map { it.take(120) }.take(100),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun scheduleToCreateMap(uid: String, s: ScheduleRun): Map<String, Any> = mapOf(
        "userId" to uid,
        "scheduledDate" to s.scheduledDate,
        "section" to s.section,
        "productName" to s.productName,
        "targetBags" to s.targetBags,
        "bagWeightKg" to s.bagWeightKg,
        "expectedWeightKg" to s.expectedWeightKg,
        "lineRemarks" to s.lineRemarks,
        "priority" to s.priority,
        "status" to s.status,
        "linkedOrderId" to s.linkedOrderId,
        "linkedOrderCode" to s.linkedOrderCode,
        "linkedOrderIds" to s.linkedOrderIds,
        "linkedOrderCodes" to s.linkedOrderCodes,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    suspend fun addBreadcrumb(
        userId: String,
        entry: BreadcrumbEntry,
        fixedDocId: String? = null
    ): Result<String> {
        val docId = fixedDocId?.takeIf { it.isNotBlank() }
            ?: "bc_${System.currentTimeMillis()}_${UUID.randomUUID().toString().replace("-", "").take(8)}"
        val path = "users/$userId/breadcrumbs/$docId"
        return try {
            val uid = requireUid(userId)
            val cleanAction = entry.action.trim().take(160)
            require(cleanAction.isNotEmpty()) { "Breadcrumb action is required" }
            val normalizedDate = entry.dateStr.takeIf { Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(it) }
                ?: "2026-11-23"
            val normalizedTime = entry.timeLabel.trim().ifBlank { "00:00:00" }.take(20)
            val normalized = entry.copy(
                userId = uid,
                actorType = BreadcrumbActor.fromRaw(entry.actorType).label,
                category = BreadcrumbCategory.fromRaw(entry.category).label,
                action = cleanAction,
                details = entry.details.trim().take(500),
                screenTab = entry.screenTab.trim().take(60),
                entityRef = entry.entityRef.trim().take(120),
                trailPath = entry.trailPath.trim().take(240),
                severity = BreadcrumbSeverity.fromRaw(entry.severity).label,
                dateStr = normalizedDate,
                timeLabel = normalizedTime,
                timestampMillis = entry.timestampMillis.coerceAtLeast(0L)
            )
            userDoc(uid).collection("breadcrumbs").document(docId)
                .set(breadcrumbToCreateMap(uid, normalized))
                .await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBreadcrumb(userId: String, breadcrumbId: String): Result<Unit> {
        val path = "users/$userId/breadcrumbs/$breadcrumbId"
        return try {
            val uid = requireUid(userId)
            require(breadcrumbId.isNotBlank()) { "Breadcrumb ID required" }
            userDoc(uid).collection("breadcrumbs").document(breadcrumbId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun clearBreadcrumbs(userId: String): Result<Int> {
        val path = "users/$userId/breadcrumbs"
        return try {
            val uid = requireUid(userId)
            val snap = userDoc(uid).collection("breadcrumbs").get().await()
            val refs = snap.documents.map { it.reference }
            refs.chunked(350).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it) }
                batch.commit().await()
            }
            Result.success(refs.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun breadcrumbToCreateMap(uid: String, b: BreadcrumbEntry): Map<String, Any> = mapOf(
        "userId" to uid,
        "actorType" to b.actorType,
        "category" to b.category,
        "action" to b.action,
        "details" to b.details,
        "screenTab" to b.screenTab,
        "entityRef" to b.entityRef,
        "trailPath" to b.trailPath,
        "severity" to b.severity,
        "dateStr" to b.dateStr,
        "timeLabel" to b.timeLabel,
        "timestampMillis" to b.timestampMillis,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun customerToCreateMap(
        uid: String,
        name: String,
        notes: String,
        priority: Boolean = false
    ): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to name.trim().take(120),
        "notes" to notes.trim().take(120),
        "priority" to priority,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    suspend fun addRawMaterial(
        userId: String,
        material: RawMaterialEntity,
        fixedDocId: String? = null
    ): Result<String> {
        val docId = fixedDocId?.takeIf { it.isNotBlank() }
            ?: "raw_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "users/$userId/raw_materials/$docId"
        return try {
            val uid = requireUid(userId)
            validateRawMaterialInput(material)
            userDoc(uid).collection("raw_materials").document(docId)
                .set(rawMaterialToCreateMap(uid, material))
                .await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateRawMaterial(
        userId: String,
        materialId: String,
        material: RawMaterialEntity
    ): Result<Unit> {
        val path = "users/$userId/raw_materials/$materialId"
        return try {
            val uid = requireUid(userId)
            require(materialId.isNotBlank()) { "Material ID is required" }
            validateRawMaterialInput(material)
            val cleanDate = material.lastRestockedDate.trim().takeIf {
                it.isEmpty() || Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(it)
            }.orEmpty()
            userDoc(uid).collection("raw_materials").document(materialId).update(
                mapOf(
                    "name" to material.name.trim().take(120),
                    "category" to RawMaterialCategory.fromRaw(material.category).label,
                    "currentStockKg" to material.currentStockKg,
                    "reorderThresholdKg" to material.reorderThresholdKg,
                    "usageSharePct" to material.usageSharePct,
                    "leadTimeDays" to material.leadTimeDays,
                    "safetyDays" to material.safetyDays,
                    "supplierName" to material.supplierName.trim().take(120),
                    "alertsEnabled" to material.alertsEnabled,
                    "lastRestockedDate" to cleanDate,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteRawMaterial(userId: String, materialId: String): Result<Unit> {
        val path = "users/$userId/raw_materials/$materialId"
        return try {
            val uid = requireUid(userId)
            require(materialId.isNotBlank()) { "Material ID is required" }
            userDoc(uid).collection("raw_materials").document(materialId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun validateRawMaterialInput(material: RawMaterialEntity) {
        require(material.name.trim().length in 1..120) { "Material name must be 1–120 characters" }
        require(material.currentStockKg in 0.0..50_000_000.0) { "Stock must be 0–50,000,000 kg" }
        require(material.reorderThresholdKg in 0.0..50_000_000.0) { "Reorder point must be 0–50,000,000 kg" }
        require(material.usageSharePct > 0.0 && material.usageSharePct <= 100.0) { "Usage share must be >0 and <=100%" }
        require(material.leadTimeDays in 1L..60L) { "Delivery lead time must be 1–60 days" }
        require(material.safetyDays in 0L..30L) { "Safety days must be 0–30 days" }
        require(material.supplierName.trim().length <= 120) { "Supplier name must be <= 120 chars" }
    }

    private fun rawMaterialToCreateMap(uid: String, m: RawMaterialEntity): Map<String, Any> {
        val cleanDate = m.lastRestockedDate.trim().takeIf {
            it.isEmpty() || Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(it)
        }.orEmpty()
        return mapOf(
            "userId" to uid,
            "name" to m.name.trim().take(120),
            "category" to RawMaterialCategory.fromRaw(m.category).label,
            "currentStockKg" to m.currentStockKg,
            "reorderThresholdKg" to m.reorderThresholdKg,
            "usageSharePct" to m.usageSharePct,
            "leadTimeDays" to m.leadTimeDays,
            "safetyDays" to m.safetyDays,
            "supplierName" to m.supplierName.trim().take(120),
            "alertsEnabled" to m.alertsEnabled,
            "lastRestockedDate" to cleanDate,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    suspend fun addRawMaterialsBulk(
        userId: String,
        materials: List<RawMaterialEntity>
    ): Result<List<Pair<String, RawMaterialEntity>>> {
        val path = "users/$userId/raw_materials"
        return try {
            val uid = requireUid(userId)
            require(materials.isNotEmpty()) { "At least one material is required" }
            val created = materials.map { mat ->
                validateRawMaterialInput(mat)
                val docId = mat.id.takeIf { it.isNotBlank() }
                    ?: "raw_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                docId to mat.copy(id = docId, userId = uid)
            }
            created.chunked(300).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { (docId, mat) ->
                    batch.set(
                        userDoc(uid).collection("raw_materials").document(docId),
                        rawMaterialToCreateMap(uid, mat)
                    )
                }
                batch.commit().await()
            }
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun addBom(
        userId: String,
        bom: FeedBomEntity,
        fixedDocId: String? = null
    ): Result<String> {
        val docId = fixedDocId?.takeIf { it.isNotBlank() }
            ?: "bom_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "users/$userId/boms/$docId"
        return try {
            val uid = requireUid(userId)
            validateBomInput(bom)
            userDoc(uid).collection("boms").document(docId)
                .set(bomToCreateMap(uid, bom))
                .await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateBom(
        userId: String,
        bomId: String,
        bom: FeedBomEntity
    ): Result<Unit> {
        val path = "users/$userId/boms/$bomId"
        return try {
            val uid = requireUid(userId)
            require(bomId.isNotBlank()) { "BOM ID is required" }
            validateBomInput(bom)
            userDoc(uid).collection("boms").document(bomId).update(
                mapOf(
                    "productName" to bom.productName.trim().take(120),
                    "section" to PlantSection.fromRaw(bom.section).label,
                    "batchBaseKg" to bom.batchBaseKg,
                    "yieldLossPct" to bom.yieldLossPct,
                    "lines" to bomLinesToMaps(bom.lines),
                    "notes" to bom.notes.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBom(userId: String, bomId: String): Result<Unit> {
        val path = "users/$userId/boms/$bomId"
        return try {
            val uid = requireUid(userId)
            require(bomId.isNotBlank()) { "BOM ID is required" }
            userDoc(uid).collection("boms").document(bomId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun validateBomInput(bom: FeedBomEntity) {
        require(bom.productName.trim().length in 1..120) { "Product name must be 1–120 characters" }
        require(bom.batchBaseKg in 1.0..100_000.0) { "Batch base weight must be 1–100,000 kg" }
        require(bom.yieldLossPct in 0.0..50.0) { "Yield loss % must be 0–50%" }
        require(bom.lines.size in 1..40) { "BOM must include 1–40 raw material lines" }
        require(bom.notes.trim().length <= 300) { "Notes must be <= 300 chars" }
        bom.lines.forEach { line ->
            require(line.materialName.trim().isNotEmpty()) { "Every BOM line needs a material name" }
            require(line.inclusionPct > 0.0 && line.inclusionPct <= 100.0) { "Inclusion % must be >0 and <=100%" }
        }
    }

    private fun bomLinesToMaps(lines: List<BomIngredientLine>): List<Map<String, Any>> =
        lines.take(40).map { line ->
            mapOf(
                "materialId" to line.materialId.trim().take(128),
                "materialName" to line.materialName.trim().take(120),
                "category" to RawMaterialCategory.fromRaw(line.category).label,
                "inclusionPct" to line.inclusionPct,
                "notes" to line.notes.trim().take(160)
            )
        }

    private fun bomToCreateMap(uid: String, bom: FeedBomEntity): Map<String, Any> = mapOf(
        "userId" to uid,
        "productName" to bom.productName.trim().take(120),
        "section" to PlantSection.fromRaw(bom.section).label,
        "batchBaseKg" to bom.batchBaseKg,
        "yieldLossPct" to bom.yieldLossPct,
        "lines" to bomLinesToMaps(bom.lines),
        "notes" to bom.notes.trim().take(300),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    suspend fun addReprocessItem(
        userId: String,
        item: ReprocessItem,
        fixedDocId: String? = null
    ): Result<String> {
        val docId = fixedDocId?.takeIf { it.isNotBlank() }
            ?: item.id.takeIf { it.isNotBlank() }
            ?: "rep_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "users/$userId/reprocess_items/$docId"
        return try {
            val uid = requireUid(userId)
            val normalized = normalizeReprocessItem(uid, item.copy(id = docId))
            validateReprocessInput(normalized)
            userDoc(uid).collection("reprocess_items").document(docId)
                .set(reprocessToCreateMap(uid, normalized))
                .await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateReprocessItem(
        userId: String,
        reprocessId: String,
        item: ReprocessItem
    ): Result<Unit> {
        val path = "users/$userId/reprocess_items/$reprocessId"
        return try {
            val uid = requireUid(userId)
            require(reprocessId.isNotBlank()) { "Reprocess item ID is required" }
            val normalized = normalizeReprocessItem(uid, item.copy(id = reprocessId))
            validateReprocessInput(normalized)
            userDoc(uid).collection("reprocess_items").document(reprocessId).update(
                mapOf(
                    "sourceDispatchId" to normalized.sourceDispatchId,
                    "drReference" to normalized.drReference,
                    "customer" to normalized.customer,
                    "productName" to normalized.productName,
                    "section" to normalized.section,
                    "returnedDate" to normalized.returnedDate,
                    "returnedBags" to normalized.returnedBags,
                    "bagWeightKg" to normalized.bagWeightKg,
                    "returnedWeightKg" to normalized.returnedWeightKg,
                    "reprocessedBags" to normalized.reprocessedBags,
                    "reprocessedWeightKg" to normalized.reprocessedWeightKg,
                    "returnReason" to normalized.returnReason,
                    "status" to normalized.status,
                    "fastActionNeeded" to normalized.fastActionNeeded,
                    "flaggedToInventory" to normalized.flaggedToInventory,
                    "reprocessedDate" to normalized.reprocessedDate,
                    "actionNotes" to normalized.actionNotes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun flagReprocessToInventory(
        userId: String,
        existing: ReprocessItem,
        reprocessedBags: Long,
        reprocessedWeightKg: Double,
        reprocessedDate: String,
        actionNotes: String = existing.actionNotes
    ): Result<Unit> {
        val updated = existing.copy(
            reprocessedBags = reprocessedBags.coerceIn(1L, 1_000_000L),
            reprocessedWeightKg = reprocessedWeightKg.coerceIn(0.01, 50_000_000.0),
            status = ReprocessStatus.FLAGGED_TO_INVENTORY.label,
            fastActionNeeded = false,
            flaggedToInventory = true,
            reprocessedDate = reprocessedDate,
            actionNotes = actionNotes
        )
        return updateReprocessItem(userId, existing.id, updated)
    }

    suspend fun markReprocessAsDamaged(
        userId: String,
        existing: ReprocessItem,
        damagedDate: String,
        damageNotes: String = existing.actionNotes
    ): Result<Unit> {
        val updated = existing.copy(
            reprocessedBags = 0L,
            reprocessedWeightKg = 0.0,
            status = ReprocessStatus.DAMAGED.label,
            fastActionNeeded = false,
            flaggedToInventory = false,
            reprocessedDate = damagedDate,
            actionNotes = damageNotes
        )
        return updateReprocessItem(userId, existing.id, updated)
    }

    suspend fun deleteReprocessItem(userId: String, reprocessId: String): Result<Unit> {
        val path = "users/$userId/reprocess_items/$reprocessId"
        return try {
            val uid = requireUid(userId)
            require(reprocessId.isNotBlank()) { "Reprocess item ID is required" }
            userDoc(uid).collection("reprocess_items").document(reprocessId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun normalizeReprocessItem(uid: String, item: ReprocessItem): ReprocessItem {
        val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        val cleanReprocDate = item.reprocessedDate.trim().takeIf {
            it.isEmpty() || dateRegex.matches(it)
        }.orEmpty()
        val resolvedStatus = if (item.flaggedToInventory) {
            ReprocessStatus.FLAGGED_TO_INVENTORY.label
        } else {
            ReprocessStatus.fromRaw(item.status).label
        }
        val isDamaged = !item.flaggedToInventory && resolvedStatus == ReprocessStatus.DAMAGED.label
        return item.copy(
            userId = uid,
            sourceDispatchId = item.sourceDispatchId.trim().take(128),
            drReference = item.drReference.trim().take(80),
            customer = item.customer.trim().take(120),
            productName = item.productName.trim().take(120),
            section = PlantSection.fromRaw(item.section).label,
            returnedDate = item.returnedDate.trim(),
            returnedBags = item.returnedBags.coerceAtLeast(1L),
            bagWeightKg = item.bagWeightKg.coerceAtLeast(0.01),
            returnedWeightKg = item.returnedWeightKg.coerceAtLeast(0.01),
            reprocessedBags = if (isDamaged) 0L else item.reprocessedBags.coerceAtLeast(0L),
            reprocessedWeightKg = if (isDamaged) 0.0 else item.reprocessedWeightKg.coerceAtLeast(0.0),
            returnReason = item.returnReason.trim().ifBlank { "Customer Return" }.take(120),
            status = resolvedStatus,
            fastActionNeeded = if (item.flaggedToInventory || isDamaged) false else item.fastActionNeeded,
            flaggedToInventory = item.flaggedToInventory,
            reprocessedDate = cleanReprocDate,
            actionNotes = item.actionNotes.trim().take(300)
        )
    }

    private fun validateReprocessInput(item: ReprocessItem) {
        val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        require(item.productName.trim().length in 1..120) { "Product name must be 1–120 characters" }
        require(dateRegex.matches(item.returnedDate)) { "Returned date must be YYYY-MM-DD" }
        require(item.returnedBags in 1L..1_000_000L) { "Returned bags must be 1–1,000,000" }
        require(item.bagWeightKg > 0.0 && item.bagWeightKg <= 2000.0) { "Bag weight must be <= 2,000 kg" }
        require(item.returnedWeightKg > 0.0 && item.returnedWeightKg <= 50_000_000.0) { "Returned weight must be <= 50,000,000 kg" }
        require(item.reprocessedBags in 0L..1_000_000L) { "Reprocessed bags must be 0–1,000,000" }
        require(item.reprocessedWeightKg in 0.0..50_000_000.0) { "Reprocessed weight must be 0–50,000,000 kg" }
        require(item.returnReason.trim().length in 1..120) { "Return reason is required (1–120 chars)" }
        require(item.reprocessedDate.isEmpty() || dateRegex.matches(item.reprocessedDate)) { "Reprocessed date must be YYYY-MM-DD" }
        require(!item.flaggedToInventory || item.status == ReprocessStatus.FLAGGED_TO_INVENTORY.label) {
            "Flagged reprocess items must have 'Flagged to Inventory' status"
        }
        require(item.status != ReprocessStatus.DAMAGED.label || (!item.flaggedToInventory && !item.fastActionNeeded)) {
            "Damaged reprocess items cannot be flagged to inventory or marked fast action"
        }
    }

    private fun reprocessToCreateMap(uid: String, item: ReprocessItem): Map<String, Any> {
        val resolvedStatus = if (item.flaggedToInventory) {
            ReprocessStatus.FLAGGED_TO_INVENTORY.label
        } else {
            ReprocessStatus.fromRaw(item.status).label
        }
        val isDamaged = !item.flaggedToInventory && resolvedStatus == ReprocessStatus.DAMAGED.label
        return mapOf(
            "userId" to uid,
            "sourceDispatchId" to item.sourceDispatchId.trim().take(128),
            "drReference" to item.drReference.trim().take(80),
            "customer" to item.customer.trim().take(120),
            "productName" to item.productName.trim().take(120),
            "section" to PlantSection.fromRaw(item.section).label,
            "returnedDate" to item.returnedDate.trim(),
            "returnedBags" to item.returnedBags,
            "bagWeightKg" to item.bagWeightKg,
            "returnedWeightKg" to item.returnedWeightKg,
            "reprocessedBags" to if (isDamaged) 0L else item.reprocessedBags,
            "reprocessedWeightKg" to if (isDamaged) 0.0 else item.reprocessedWeightKg,
            "returnReason" to item.returnReason.trim().ifBlank { "Customer Return" }.take(120),
            "status" to resolvedStatus,
            "fastActionNeeded" to if (item.flaggedToInventory || isDamaged) false else item.fastActionNeeded,
            "flaggedToInventory" to item.flaggedToInventory,
            "reprocessedDate" to item.reprocessedDate.trim(),
            "actionNotes" to item.actionNotes.trim().take(300),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    // -------------------------------------------------------------------------
    // Shared Multi-User Workspaces, Invites & Team Roles
    // -------------------------------------------------------------------------

    fun buildInviteDocId(workspaceOwnerId: String, memberEmail: String): String {
        val normalizedEmail = memberEmail.trim().lowercase()
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(normalizedEmail.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(24)
        val cleanOwner = workspaceOwnerId.replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(48)
        return "inv_${cleanOwner}_$digest"
    }

    fun observeWorkspaceMembers(workspaceOwnerId: String): Flow<List<WorkspaceMember>> =
        userDoc(workspaceOwnerId).collection("team_members")
            .snapshots()
            .map { snap ->
                snap.toObjects(WorkspaceMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareBy<WorkspaceMember> { member ->
                            when (member.resolvedRole()) {
                                WorkspaceRole.ADMIN -> 0
                                WorkspaceRole.OPERATOR -> 1
                                WorkspaceRole.VIEWER -> 2
                            }
                        }.thenBy { it.email.lowercase() }
                    )
            }

    fun observeWorkspaceMemberSelf(workspaceOwnerId: String, memberUid: String): Flow<WorkspaceMember?> =
        userDoc(workspaceOwnerId).collection("team_members").document(memberUid)
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(WorkspaceMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }

    fun observeWorkspaceInvites(workspaceOwnerId: String): Flow<List<WorkspaceInvite>> =
        userDoc(workspaceOwnerId).collection("team_invites")
            .snapshots()
            .map { snap ->
                snap.toObjects(WorkspaceInvite::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareBy<WorkspaceInvite> {
                            if (it.resolvedStatus() == WorkspaceInviteStatus.PENDING) 0 else 1
                        }.thenBy { it.memberEmail.lowercase() }
                    )
            }

    fun observeMyInvitesByEmail(userEmail: String): Flow<List<WorkspaceInvite>> {
        val normalized = userEmail.trim().lowercase()
        return db.collection("workspace_invites")
            .whereEqualTo("memberEmail", normalized)
            .snapshots()
            .map { snap ->
                snap.toObjects(WorkspaceInvite::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareBy<WorkspaceInvite> {
                            if (it.resolvedStatus() == WorkspaceInviteStatus.PENDING) 0 else 1
                        }.thenBy { it.workspaceName.lowercase() }
                    )
            }
    }

    suspend fun inviteWorkspaceMember(
        workspaceOwnerId: String,
        workspaceName: String,
        ownerEmail: String,
        memberEmail: String,
        memberDisplayName: String,
        role: WorkspaceRole
    ): Result<String> {
        val callerEmail = auth.currentUser?.email?.trim()?.lowercase().orEmpty()
        val callerUid = try {
            requireSignedInUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanOwnerId = workspaceOwnerId.trim()
        val cleanMemberEmail = memberEmail.trim().lowercase()
        val inviteId = buildInviteDocId(cleanOwnerId, cleanMemberEmail)
        val path = "workspace_invites/$inviteId"
        return try {
            require(cleanOwnerId.isNotEmpty()) { "Workspace owner ID is required." }
            require(cleanMemberEmail.length in 3..160 && cleanMemberEmail.contains('@') && cleanMemberEmail.contains('.')) {
                "Enter a valid Google email address."
            }
            if (cleanMemberEmail == callerEmail) {
                throw IllegalArgumentException("You already have access to this workspace.")
            }
            if (ownerEmail.isNotBlank() && cleanMemberEmail == ownerEmail.trim().lowercase()) {
                throw IllegalArgumentException("Workspace owner already has full Admin access.")
            }
            val cleanWorkspaceName = workspaceName.trim().ifBlank { "Bulacan Plant" }.take(120)
            val cleanDisplayName = memberDisplayName.trim()
                .ifBlank { cleanMemberEmail.substringBefore('@') }
                .take(120)
            val cleanOwnerEmail = ownerEmail.trim().lowercase().take(160)
            val inviterEmail = callerEmail.ifBlank { cleanOwnerEmail.ifBlank { callerUid } }.take(160)

            val rootRef = db.collection("workspace_invites").document(inviteId)
            val wsInviteRef = userDoc(cleanOwnerId).collection("team_invites").document(inviteId)
            val existingWsInvite = wsInviteRef.get().await()

            val batch = db.batch()
            if (existingWsInvite.exists()) {
                val existingMemberUid = existingWsInvite.getString("memberUid").orEmpty().trim()
                val updateMap = mapOf(
                    "role" to role.label,
                    "memberDisplayName" to cleanDisplayName,
                    "workspaceName" to cleanWorkspaceName,
                    "invitedByEmail" to inviterEmail,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.update(wsInviteRef, updateMap)
                val rootExists = try {
                    rootRef.get().await().exists()
                } catch (_: Exception) {
                    false
                }
                if (rootExists) {
                    batch.update(rootRef, updateMap)
                } else if (existingMemberUid.isEmpty()) {
                    val recreateRootMap = mapOf(
                        "workspaceOwnerId" to cleanOwnerId,
                        "workspaceName" to cleanWorkspaceName,
                        "ownerEmail" to cleanOwnerEmail,
                        "memberEmail" to cleanMemberEmail,
                        "memberDisplayName" to cleanDisplayName,
                        "role" to role.label,
                        "status" to WorkspaceInviteStatus.PENDING.label,
                        "memberUid" to "",
                        "invitedByEmail" to inviterEmail,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(rootRef, recreateRootMap)
                }
                if (existingMemberUid.isNotEmpty() && existingMemberUid != cleanOwnerId) {
                    val memberRef = userDoc(cleanOwnerId).collection("team_members").document(existingMemberUid)
                    val memberExists = try {
                        memberRef.get().await().exists()
                    } catch (_: Exception) {
                        false
                    }
                    if (memberExists) {
                        batch.update(
                            memberRef,
                            mapOf(
                                "role" to role.label,
                                "displayName" to cleanDisplayName,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            } else {
                val createMap = mapOf(
                    "workspaceOwnerId" to cleanOwnerId,
                    "workspaceName" to cleanWorkspaceName,
                    "ownerEmail" to cleanOwnerEmail,
                    "memberEmail" to cleanMemberEmail,
                    "memberDisplayName" to cleanDisplayName,
                    "role" to role.label,
                    "status" to WorkspaceInviteStatus.PENDING.label,
                    "memberUid" to "",
                    "invitedByEmail" to inviterEmail,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(rootRef, createMap)
                batch.set(wsInviteRef, createMap)
            }
            batch.commit().await()
            Result.success(inviteId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun acceptWorkspaceInvite(invite: WorkspaceInvite): Result<Unit> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("Please sign in with Google first."))
        val uid = currentUser.uid
        val emailLower = currentUser.email?.trim()?.lowercase().orEmpty()
        val ownerId = invite.workspaceOwnerId.trim()
        val path = "users/$ownerId/team_members/$uid"
        return try {
            require(ownerId.isNotEmpty()) { "Invalid workspace invitation." }
            require(ownerId != uid) { "You already own this workspace." }
            require(emailLower.isNotEmpty()) { "Google account email is required to join." }

            val displayName = currentUser.displayName?.trim().orEmpty()
                .ifBlank { invite.memberDisplayName }
                .ifBlank { emailLower.substringBefore('@') }
                .take(120)

            val memberRef = userDoc(ownerId).collection("team_members").document(uid)
            val rootInviteRef = db.collection("workspace_invites").document(invite.id)
            val wsInviteRef = userDoc(ownerId).collection("team_invites").document(invite.id)

            val memberExists = memberRef.get().await().exists()
            val wsInviteExists = try {
                wsInviteRef.get().await().exists()
            } catch (_: Exception) {
                false
            }

            val batch = db.batch()
            var hasWrites = false
            if (!memberExists) {
                hasWrites = true
                batch.set(
                    memberRef,
                    mapOf(
                        "userId" to uid,
                        "workspaceOwnerId" to ownerId,
                        "inviteId" to invite.id,
                        "email" to emailLower,
                        "displayName" to displayName,
                        "role" to invite.resolvedRole().label,
                        "invitedByEmail" to invite.invitedByEmail.ifBlank { invite.ownerEmail }.ifBlank { "admin" },
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            if (invite.resolvedStatus() == WorkspaceInviteStatus.PENDING) {
                hasWrites = true
                val inviteUpdate = mapOf(
                    "status" to WorkspaceInviteStatus.ACTIVE.label,
                    "memberUid" to uid,
                    "memberDisplayName" to displayName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.update(rootInviteRef, inviteUpdate)
                if (wsInviteExists) {
                    batch.update(wsInviteRef, inviteUpdate)
                }
            }
            if (hasWrites) {
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun declineOrLeaveWorkspace(invite: WorkspaceInvite): Result<Unit> {
        val uid = try {
            requireSignedInUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val ownerId = invite.workspaceOwnerId.trim()
        val path = "workspace_invites/${invite.id}"
        return try {
            require(ownerId.isNotEmpty() && invite.id.isNotBlank()) { "Invalid invitation." }
            val rootInviteRef = db.collection("workspace_invites").document(invite.id)
            val wsInviteRef = userDoc(ownerId).collection("team_invites").document(invite.id)
            val memberRef = userDoc(ownerId).collection("team_members").document(uid)

            val memberExists = try { memberRef.get().await().exists() } catch (_: Exception) { false }
            val wsInviteExists = try { wsInviteRef.get().await().exists() } catch (_: Exception) { false }
            val rootInviteExists = try { rootInviteRef.get().await().exists() } catch (_: Exception) { false }

            val batch = db.batch()
            if (memberExists && uid != ownerId) batch.delete(memberRef)
            if (wsInviteExists) batch.delete(wsInviteRef)
            if (rootInviteExists) batch.delete(rootInviteRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun updateWorkspaceMemberRole(
        workspaceOwnerId: String,
        memberUid: String,
        inviteId: String,
        newRole: WorkspaceRole
    ): Result<Unit> {
        val cleanOwnerId = workspaceOwnerId.trim()
        val callerEmail = auth.currentUser?.email?.trim()?.lowercase().orEmpty()
        val path = "users/$cleanOwnerId/team_members/$memberUid"
        return try {
            requireUid(cleanOwnerId)
            require(memberUid != cleanOwnerId) { "Workspace owner always retains Admin role." }
            val nowUpdate = mapOf(
                "role" to newRole.label,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val batch = db.batch()
            if (memberUid.isNotBlank()) {
                val memberRef = userDoc(cleanOwnerId).collection("team_members").document(memberUid)
                if (memberRef.get().await().exists()) {
                    batch.update(memberRef, nowUpdate)
                }
            }
            if (inviteId.isNotBlank()) {
                val inviteUpdate = buildMap<String, Any> {
                    put("role", newRole.label)
                    put("updatedAt", FieldValue.serverTimestamp())
                    if (callerEmail.isNotEmpty()) put("invitedByEmail", callerEmail)
                }
                val wsInviteRef = userDoc(cleanOwnerId).collection("team_invites").document(inviteId)
                if (wsInviteRef.get().await().exists()) {
                    batch.update(wsInviteRef, inviteUpdate)
                }
                val rootInviteRef = db.collection("workspace_invites").document(inviteId)
                if (try { rootInviteRef.get().await().exists() } catch (_: Exception) { false }) {
                    batch.update(rootInviteRef, inviteUpdate)
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeWorkspaceMember(
        workspaceOwnerId: String,
        memberUid: String,
        inviteId: String
    ): Result<Unit> {
        val cleanOwnerId = workspaceOwnerId.trim()
        val path = "users/$cleanOwnerId/team_members/$memberUid"
        return try {
            requireUid(cleanOwnerId)
            require(memberUid != cleanOwnerId) { "Cannot remove the workspace owner." }
            val batch = db.batch()
            if (memberUid.isNotBlank()) {
                val memberRef = userDoc(cleanOwnerId).collection("team_members").document(memberUid)
                if (memberRef.get().await().exists()) {
                    batch.delete(memberRef)
                }
            }
            if (inviteId.isNotBlank()) {
                val wsInviteRef = userDoc(cleanOwnerId).collection("team_invites").document(inviteId)
                if (wsInviteRef.get().await().exists()) {
                    batch.delete(wsInviteRef)
                }
                val rootInviteRef = db.collection("workspace_invites").document(inviteId)
                if (try { rootInviteRef.get().await().exists() } catch (_: Exception) { false }) {
                    batch.delete(rootInviteRef)
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
