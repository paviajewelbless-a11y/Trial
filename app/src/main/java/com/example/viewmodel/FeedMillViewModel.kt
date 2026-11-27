package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DispatchBatchItem
import com.example.data.GeminiOpsRepository
import com.example.data.PlantRepository
import com.example.data.local.FeedInventoryLevelEntity
import com.example.data.local.FeedInventoryRepository
import com.example.domain.OperationsCalculator
import com.example.domain.ReportExporter
import com.example.model.AiPlantInsight
import com.example.model.BomIngredientLine
import com.example.model.BreadcrumbActor
import com.example.model.BreadcrumbCategory
import com.example.model.BreadcrumbEntry
import com.example.model.BreadcrumbSeverity
import com.example.model.ChatMessage
import com.example.model.CustomerEntity
import com.example.model.CustomerOrder
import com.example.model.DispatchRecord
import com.example.model.DispatchStatus
import com.example.model.FeedBomEntity
import com.example.model.OrderStatus
import com.example.model.PlantKpis
import com.example.model.PlantReportData
import com.example.model.PlantSection
import com.example.model.PlantSettings
import com.example.model.ProductionBatch
import com.example.model.RawMaterialCategory
import com.example.model.RawMaterialEntity
import com.example.model.RawMaterialProjection
import com.example.model.RawMaterialRiskStatus
import com.example.model.ReportSectionOption
import com.example.model.ReprocessItem
import com.example.model.ReprocessStatus
import com.example.model.ResetScopeTarget
import com.example.model.SchedulePriority
import com.example.model.ScheduleRun
import com.example.model.ScheduleStatus
import com.example.model.SkuEntity
import com.example.model.SkuInventorySummary
import com.example.model.WorkspaceInvite
import com.example.model.WorkspaceMember
import com.example.model.WorkspaceRole
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.round

private const val TAG = "FeedMillVM"

data class DispatchEntryInput(
    val date: String,
    val productName: String,
    val bags: Long,
    val bagWeightKg: Double,
    val totalWeightKg: Double,
    val drReference: String,
    val customer: String,
    val linkedOrder: CustomerOrder? = null,
    val driverName: String = "",
    val truckNumber: String = "",
    val helperName: String = "",
    val remarks: String = "",
    val hasReturn: Boolean = false,
    val returnConfirmed: Boolean = false,
    val returnedBags: Long = 0L,
    val returnedWeightKg: Double = 0.0,
    val returnReason: String = "",
    val isHeld: Boolean = false,
    val heldRemarks: String = "",
    val createRushOrderIfUnlinked: Boolean = false,
    val sendReturnedToReprocess: Boolean = true,
    val sourcePlant: String = ""
)

private fun normalizeSourcePlantLabel(raw: String): String {
    val cleaned = raw.trim()
    if (cleaned.isEmpty()) return ""
    return PlantSection.entries.firstOrNull {
        it.label.equals(cleaned, ignoreCase = true) || it.name.equals(cleaned, ignoreCase = true)
    }?.label ?: ""
}

data class NewOrderItemInput(
    val productName: String,
    val orderedBags: Long,
    val bagWeightKg: Double
)

data class NewScheduleItemInput(
    val section: PlantSection,
    val productName: String,
    val targetBags: Long,
    val bagWeightKg: Double,
    val lineRemarks: String,
    val linkedOrderId: String = "",
    val linkedOrderCode: String = "",
    val linkedOrderIds: List<String> = emptyList(),
    val linkedOrderCodes: List<String> = emptyList()
)

sealed interface OpsUiState {
    data object Loading : OpsUiState
    data class Ready(
        val skus: List<SkuEntity>,
        val batches: List<ProductionBatch>,
        val dispatches: List<DispatchRecord>,
        val orders: List<CustomerOrder>,
        val schedules: List<ScheduleRun>,
        val customers: List<CustomerEntity>,
        val settings: PlantSettings?,
        val summaries: List<SkuInventorySummary>,
        val kpis: PlantKpis,
        val rawMaterials: List<RawMaterialEntity> = emptyList(),
        val rawMaterialProjections: List<RawMaterialProjection> = emptyList(),
        val boms: List<FeedBomEntity> = emptyList(),
        val reprocessItems: List<ReprocessItem> = emptyList()
    ) : OpsUiState

    data class Error(val message: String) : OpsUiState
}

sealed interface AiInsightState {
    data object Idle : AiInsightState
    data object Loading : AiInsightState
    data class Success(val insight: AiPlantInsight) : AiInsightState
    data class Error(val message: String) : AiInsightState
}

class FeedMillViewModel(
    private val repository: PlantRepository,
    private val geminiRepo: GeminiOpsRepository,
    private val currentUserId: String,
    private val localInventoryRepository: FeedInventoryRepository? = null,
    private val signedInUid: String = currentUserId,
    private val signedInEmail: String = "",
    initialRole: WorkspaceRole = if (signedInUid == currentUserId) WorkspaceRole.ADMIN else WorkspaceRole.VIEWER
) : ViewModel() {

    val isWorkspaceOwner: Boolean = signedInUid == currentUserId

    val userRole: StateFlow<WorkspaceRole> = if (isWorkspaceOwner) {
        MutableStateFlow(WorkspaceRole.ADMIN).asStateFlow()
    } else {
        repository.observeWorkspaceMemberSelf(currentUserId, signedInUid)
            .map { member -> member?.resolvedRole() ?: WorkspaceRole.VIEWER }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(initialRole)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = initialRole
            )
    }

    val teamMembers: StateFlow<List<WorkspaceMember>> = repository.observeWorkspaceMembers(currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing workspace members", error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val workspaceInvites: StateFlow<List<WorkspaceInvite>> = repository.observeWorkspaceInvites(currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing workspace invites", error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val localInventoryLevels: StateFlow<List<FeedInventoryLevelEntity>> =
        (localInventoryRepository?.inventoryLevels ?: flowOf(emptyList()))
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing local Room inventory levels", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    private val _lookbackDays = MutableStateFlow(14)
    val lookbackDays: StateFlow<Int> = _lookbackDays.asStateFlow()

    private val _safetyBufferPct = MutableStateFlow<Double?>(null)
    val safetyBufferPct: StateFlow<Double?> = _safetyBufferPct.asStateFlow()

    private val _rawMaterialDemandMultiplier = MutableStateFlow(1.0)
    val rawMaterialDemandMultiplier: StateFlow<Double> = _rawMaterialDemandMultiplier.asStateFlow()

    private val _rawMaterialIncludeScheduled = MutableStateFlow(false)
    val rawMaterialIncludeScheduled: StateFlow<Boolean> = _rawMaterialIncludeScheduled.asStateFlow()

    private val _rawMaterialLeadOverrideDays = MutableStateFlow<Long?>(null)
    val rawMaterialLeadOverrideDays: StateFlow<Long?> = _rawMaterialLeadOverrideDays.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _aiInsightState = MutableStateFlow<AiInsightState>(AiInsightState.Idle)
    val aiInsightState: StateFlow<AiInsightState> = _aiInsightState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "welcome",
                isUser = false,
                text = "Hello! I'm your Bulacan Plant Operations Assistant. Ask me about stockout risks, October recovery variance, customer order priorities, or next-shift line scheduling."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatSending = MutableStateFlow(false)
    val isChatSending: StateFlow<Boolean> = _isChatSending.asStateFlow()

    private data class UndoableAction(
        val label: String,
        val undo: suspend () -> Result<Unit>,
        val redo: suspend () -> Result<Unit>
    )

    private val undoStack = mutableListOf<UndoableAction>()
    private val redoStack = mutableListOf<UndoableAction>()
    private val maxHistorySize = 40

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _isUndoRedoInProgress = MutableStateFlow(false)
    val isUndoRedoInProgress: StateFlow<Boolean> = _isUndoRedoInProgress.asStateFlow()

    private val _undoLabel = MutableStateFlow<String?>(null)
    val undoLabel: StateFlow<String?> = _undoLabel.asStateFlow()

    private val _redoLabel = MutableStateFlow<String?>(null)
    val redoLabel: StateFlow<String?> = _redoLabel.asStateFlow()

    private val _sessionTrail = MutableStateFlow(listOf("Summary"))
    val sessionTrail: StateFlow<List<String>> = _sessionTrail.asStateFlow()

    val breadcrumbs: StateFlow<List<BreadcrumbEntry>> = repository.observeBreadcrumbs(currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing breadcrumbs", error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private fun currentTimeLabel(): String =
        SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())

    private fun currentDateLabel(): String = OperationsCalculator.todayIsoDate()

    private fun currentTrailString(): String =
        _sessionTrail.value.joinToString(" › ")

    private fun requireOperationalWrite(actionName: String): Boolean {
        if (!userRole.value.canWriteOperations) {
            _snackbarMessage.value = "Viewer access is read-only. Ask an Admin for Operator permissions to $actionName."
            return false
        }
        return true
    }

    private fun requireAdminPermission(actionName: String): Boolean {
        if (!userRole.value.canManageTeam) {
            _snackbarMessage.value = "Admin role is required to $actionName."
            return false
        }
        return true
    }

    fun inviteTeamMember(
        memberEmail: String,
        memberDisplayName: String,
        role: WorkspaceRole,
        ownerEmail: String = "",
        onSaved: (String) -> Unit = {}
    ) {
        if (!requireAdminPermission("invite team members")) return
        viewModelScope.launch {
            val resolvedOwnerEmail = ownerEmail.ifBlank { if (isWorkspaceOwner) signedInEmail else "" }
            repository.inviteWorkspaceMember(
                workspaceOwnerId = currentUserId,
                workspaceName = "Bulacan Plant",
                ownerEmail = resolvedOwnerEmail,
                memberEmail = memberEmail,
                memberDisplayName = memberDisplayName,
                role = role
            ).onSuccess { inviteId ->
                val cleanEmail = memberEmail.trim().lowercase()
                _snackbarMessage.value = "Invited $cleanEmail as ${role.label}."
                logBreadcrumb(
                    actor = BreadcrumbActor.USER,
                    category = BreadcrumbCategory.SYSTEM,
                    action = "Invited Team Member ($cleanEmail)",
                    details = "Assigned role: ${role.label}",
                    screenTab = "Team",
                    entityRef = cleanEmail,
                    severity = BreadcrumbSeverity.SUCCESS
                )
                onSaved(inviteId)
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to send team invite."
            }
        }
    }

    fun updateTeamMemberRole(
        memberUid: String,
        inviteId: String,
        memberEmail: String,
        newRole: WorkspaceRole
    ) {
        if (!requireAdminPermission("change team member roles")) return
        viewModelScope.launch {
            repository.updateWorkspaceMemberRole(
                workspaceOwnerId = currentUserId,
                memberUid = memberUid,
                inviteId = inviteId,
                newRole = newRole
            ).onSuccess {
                _snackbarMessage.value = "Updated $memberEmail to ${newRole.label}."
                logBreadcrumb(
                    actor = BreadcrumbActor.USER,
                    category = BreadcrumbCategory.SYSTEM,
                    action = "Updated Member Role ($memberEmail)",
                    details = "Changed role to ${newRole.label}",
                    screenTab = "Team",
                    entityRef = memberEmail,
                    severity = BreadcrumbSeverity.INFO
                )
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to update role."
            }
        }
    }

    fun removeTeamMember(
        memberUid: String,
        inviteId: String,
        memberEmail: String
    ) {
        if (!requireAdminPermission("remove team members")) return
        viewModelScope.launch {
            repository.removeWorkspaceMember(
                workspaceOwnerId = currentUserId,
                memberUid = memberUid,
                inviteId = inviteId
            ).onSuccess {
                _snackbarMessage.value = "Removed $memberEmail from workspace."
                logBreadcrumb(
                    actor = BreadcrumbActor.USER,
                    category = BreadcrumbCategory.SYSTEM,
                    action = "Removed Team Member ($memberEmail)",
                    details = "Revoked shared Bulacan Plant workspace access",
                    screenTab = "Team",
                    entityRef = memberEmail,
                    severity = BreadcrumbSeverity.WARNING
                )
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to remove team member."
            }
        }
    }

    private fun logBreadcrumb(
        actor: BreadcrumbActor,
        category: BreadcrumbCategory,
        action: String,
        details: String = "",
        screenTab: String = _sessionTrail.value.lastOrNull() ?: "Summary",
        entityRef: String = "",
        severity: BreadcrumbSeverity = BreadcrumbSeverity.INFO,
        trailOverride: String? = null
    ) {
        if (!userRole.value.canWriteOperations) return
        val resolvedTrail = (trailOverride ?: currentTrailString()).take(235)
        val actorEmail = signedInEmail.trim()
        val attributedDetails = if (actorEmail.isNotBlank() && !details.contains(actorEmail, ignoreCase = true)) {
            if (details.isBlank()) {
                "By $actorEmail (${userRole.value.label})"
            } else {
                "$details • By $actorEmail (${userRole.value.label})"
            }
        } else {
            details
        }
        val nowMs = System.currentTimeMillis()
        val entry = BreadcrumbEntry(
            userId = currentUserId,
            actorType = actor.label,
            category = category.label,
            action = action.ifBlank { "Plant Event" }.take(155),
            details = attributedDetails.take(490),
            screenTab = screenTab.take(55),
            entityRef = entityRef.take(115),
            trailPath = resolvedTrail,
            severity = severity.label,
            dateStr = currentDateLabel(),
            timeLabel = currentTimeLabel(),
            timestampMillis = nowMs
        )
        viewModelScope.launch {
            repository.addBreadcrumb(currentUserId, entry)
        }
    }

    fun recordSessionStart(userEmail: String) {
        val role = userRole.value
        if (!role.canWriteOperations) return
        val workspaceKind = if (isWorkspaceOwner) "Owner" else "Shared ${role.label}"
        logBreadcrumb(
            actor = BreadcrumbActor.SYSTEM,
            category = BreadcrumbCategory.SYSTEM,
            action = "Workspace Session Started",
            details = "Authenticated via Google Sign-In ($userEmail • $workspaceKind) • Real-time Bulacan Plant listeners attached",
            screenTab = "Summary",
            entityRef = userEmail,
            severity = BreadcrumbSeverity.SUCCESS,
            trailOverride = "Auth › Summary"
        )
    }

    fun recordNavigationBreadcrumb(fromTab: String, toTab: String, source: String = "Navigation") {
        if (fromTab == toTab) return
        val updatedTrail = (_sessionTrail.value + toTab).let { list ->
            if (list.size > 6) list.takeLast(6) else list
        }
        _sessionTrail.value = updatedTrail
        logBreadcrumb(
            actor = BreadcrumbActor.USER,
            category = BreadcrumbCategory.NAVIGATION,
            action = "Navigated to $toTab",
            details = "From $fromTab to $toTab via $source",
            screenTab = toTab,
            entityRef = toTab,
            severity = BreadcrumbSeverity.INFO,
            trailOverride = updatedTrail.joinToString(" › ")
        )
    }

    fun recordUiBreadcrumb(
        category: BreadcrumbCategory,
        action: String,
        details: String = "",
        screenTab: String = _sessionTrail.value.lastOrNull() ?: "Summary",
        entityRef: String = ""
    ) {
        logBreadcrumb(
            actor = BreadcrumbActor.USER,
            category = category,
            action = action,
            details = details,
            screenTab = screenTab,
            entityRef = entityRef,
            severity = BreadcrumbSeverity.INFO
        )
    }

    fun deleteBreadcrumb(breadcrumbId: String) {
        if (!requireAdminPermission("delete activity history")) return
        viewModelScope.launch {
            repository.deleteBreadcrumb(currentUserId, breadcrumbId)
                .onFailure { err ->
                    _snackbarMessage.value = "Failed to delete history entry: ${err.localizedMessage}"
                }
        }
    }

    fun clearAllBreadcrumbs() {
        if (!requireAdminPermission("clear activity history")) return
        viewModelScope.launch {
            repository.clearBreadcrumbs(currentUserId)
                .onSuccess { removedCount ->
                    _snackbarMessage.value = "Cleared $removedCount history breadcrumb(s)."
                    logBreadcrumb(
                        actor = BreadcrumbActor.SYSTEM,
                        category = BreadcrumbCategory.SYSTEM,
                        action = "Breadcrumb History Cleared",
                        details = "User cleared $removedCount previous audit entries",
                        screenTab = "History",
                        severity = BreadcrumbSeverity.WARNING
                    )
                }
                .onFailure { err ->
                    _snackbarMessage.value = "Failed to clear history: ${err.localizedMessage}"
                }
        }
    }

    init {
        if (isWorkspaceOwner) {
            viewModelScope.launch {
                repository.ensureSeededOctober2026(currentUserId)
                    .onSuccess {
                        logBreadcrumb(
                            actor = BreadcrumbActor.SYSTEM,
                            category = BreadcrumbCategory.SYSTEM,
                            action = "Firestore Plant Sync Ready",
                            details = "Verified October 2026 baseline SKUs, customers & operational collections",
                            screenTab = "Summary",
                            severity = BreadcrumbSeverity.SUCCESS
                        )
                    }
                    .onFailure { err ->
                        _snackbarMessage.value = "Initial seed notice: ${err.localizedMessage ?: "Failed to verify seed data"}"
                        logBreadcrumb(
                            actor = BreadcrumbActor.SYSTEM,
                            category = BreadcrumbCategory.SYSTEM,
                            action = "Initial Seed Verification Failed",
                            details = err.localizedMessage ?: "Failed to verify seed data",
                            screenTab = "Summary",
                            severity = BreadcrumbSeverity.ERROR
                        )
                    }
            }
        }
    }

    private fun syncHistoryFlags() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        _undoLabel.value = undoStack.lastOrNull()?.label
        _redoLabel.value = redoStack.lastOrNull()?.label
    }

    private fun recordUndoable(
        label: String,
        undo: suspend () -> Result<Unit>,
        redo: suspend () -> Result<Unit>
    ) {
        undoStack.add(UndoableAction(label, undo, redo))
        if (undoStack.size > maxHistorySize) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        syncHistoryFlags()
    }

    private fun clearUndoRedoHistory() {
        undoStack.clear()
        redoStack.clear()
        syncHistoryFlags()
    }

    fun undoLastAction() {
        if (!requireOperationalWrite("undo changes")) return
        if (_isUndoRedoInProgress.value) return
        val action = undoStack.lastOrNull() ?: return
        viewModelScope.launch {
            _isUndoRedoInProgress.value = true
            try {
                action.undo()
                    .onSuccess {
                        if (undoStack.lastOrNull() === action) {
                            undoStack.removeAt(undoStack.lastIndex)
                        } else {
                            undoStack.remove(action)
                        }
                        redoStack.add(action)
                        syncHistoryFlags()
                        _snackbarMessage.value = "Undone: ${action.label}"
                        logBreadcrumb(
                            actor = BreadcrumbActor.USER,
                            category = BreadcrumbCategory.SYSTEM,
                            action = "Undo: ${action.label}",
                            details = "Rolled back previous action via Undo stack",
                            entityRef = action.label,
                            severity = BreadcrumbSeverity.WARNING
                        )
                    }
                    .onFailure { err ->
                        _snackbarMessage.value = "Undo failed (${action.label}): ${err.localizedMessage}"
                        logBreadcrumb(
                            actor = BreadcrumbActor.SYSTEM,
                            category = BreadcrumbCategory.SYSTEM,
                            action = "Undo Failed: ${action.label}",
                            details = err.localizedMessage ?: "Unknown error",
                            entityRef = action.label,
                            severity = BreadcrumbSeverity.ERROR
                        )
                    }
            } finally {
                _isUndoRedoInProgress.value = false
            }
        }
    }

    fun redoLastAction() {
        if (!requireOperationalWrite("redo changes")) return
        if (_isUndoRedoInProgress.value) return
        val action = redoStack.lastOrNull() ?: return
        viewModelScope.launch {
            _isUndoRedoInProgress.value = true
            try {
                action.redo()
                    .onSuccess {
                        if (redoStack.lastOrNull() === action) {
                            redoStack.removeAt(redoStack.lastIndex)
                        } else {
                            redoStack.remove(action)
                        }
                        undoStack.add(action)
                        syncHistoryFlags()
                        _snackbarMessage.value = "Redone: ${action.label}"
                        logBreadcrumb(
                            actor = BreadcrumbActor.USER,
                            category = BreadcrumbCategory.SYSTEM,
                            action = "Redo: ${action.label}",
                            details = "Re-applied action via Redo stack",
                            entityRef = action.label,
                            severity = BreadcrumbSeverity.SUCCESS
                        )
                    }
                    .onFailure { err ->
                        _snackbarMessage.value = "Redo failed (${action.label}): ${err.localizedMessage}"
                        logBreadcrumb(
                            actor = BreadcrumbActor.SYSTEM,
                            category = BreadcrumbCategory.SYSTEM,
                            action = "Redo Failed: ${action.label}",
                            details = err.localizedMessage ?: "Unknown error",
                            entityRef = action.label,
                            severity = BreadcrumbSeverity.ERROR
                        )
                    }
            } finally {
                _isUndoRedoInProgress.value = false
            }
        }
    }

    private data class RawCollections(
        val skus: List<SkuEntity>,
        val batches: List<ProductionBatch>,
        val dispatches: List<DispatchRecord>,
        val orders: List<CustomerOrder>,
        val schedules: List<ScheduleRun>,
        val rawMaterials: List<RawMaterialEntity> = emptyList(),
        val boms: List<FeedBomEntity> = emptyList(),
        val reprocessItems: List<ReprocessItem> = emptyList()
    )

    private data class ForecastParams(
        val lookbackDays: Int,
        val safetyBufferPct: Double?,
        val demandMultiplier: Double,
        val includeScheduled: Boolean,
        val leadOverrideDays: Long?
    )

    private val rawStreams = combine(
        repository.observeSkus(currentUserId),
        repository.observeBatches(currentUserId),
        repository.observeDispatches(currentUserId),
        repository.observeOrders(currentUserId),
        repository.observeSchedules(currentUserId)
    ) { skus, batches, dispatches, orders, schedules ->
        RawCollections(skus, batches, dispatches, orders, schedules)
    }.combine(repository.observeRawMaterials(currentUserId)) { base, materials ->
        base.copy(rawMaterials = materials)
    }.combine(repository.observeBoms(currentUserId)) { base, boms ->
        base.copy(boms = boms)
    }.combine(repository.observeReprocessItems(currentUserId)) { base, reprocessItems ->
        base.copy(reprocessItems = reprocessItems)
    }

    private val forecastParamsFlow = combine(
        _lookbackDays,
        _safetyBufferPct,
        _rawMaterialDemandMultiplier,
        _rawMaterialIncludeScheduled,
        _rawMaterialLeadOverrideDays
    ) { lookback, buffer, mult, includeSched, leadOverride ->
        ForecastParams(lookback, buffer, mult, includeSched, leadOverride)
    }

    val opsState: StateFlow<OpsUiState> = combine(
        rawStreams,
        repository.observeCustomers(currentUserId),
        repository.observeSettings(currentUserId),
        forecastParamsFlow
    ) { raw, customers, settings, params ->
        val summaries = OperationsCalculator.computeSkuSummaries(
            skus = raw.skus,
            batches = raw.batches,
            dispatches = raw.dispatches,
            orders = raw.orders,
            lookbackDays = params.lookbackDays,
            globalSafetyBufferPct = params.safetyBufferPct,
            reprocessItems = raw.reprocessItems
        )
        val kpis = OperationsCalculator.computeKpis(
            summaries = summaries,
            batches = raw.batches,
            dispatches = raw.dispatches
        )
        val rawMaterialProjections = OperationsCalculator.computeRawMaterialForecasts(
            materials = raw.rawMaterials,
            batches = raw.batches,
            schedules = raw.schedules,
            lookbackDays = params.lookbackDays,
            demandMultiplier = params.demandMultiplier,
            includeScheduledDemand = params.includeScheduled,
            leadTimeOverrideDays = params.leadOverrideDays,
            boms = raw.boms
        )
        OpsUiState.Ready(
            skus = raw.skus,
            batches = raw.batches,
            dispatches = raw.dispatches,
            orders = raw.orders,
            schedules = raw.schedules,
            customers = customers,
            settings = settings,
            summaries = summaries,
            kpis = kpis,
            rawMaterials = raw.rawMaterials,
            rawMaterialProjections = rawMaterialProjections,
            boms = raw.boms,
            reprocessItems = raw.reprocessItems
        ) as OpsUiState
    }.onEach { state ->
        if (state is OpsUiState.Ready && localInventoryRepository != null) {
            val nowMs = System.currentTimeMillis()
            val finishedEntities = state.summaries.map { summary ->
                val reproNotes = buildList {
                    if (summary.reprocessedKg > 0.0) {
                        add("REPROCESSED +${OperationsCalculator.formatKg(summary.reprocessedKg)} kg (${summary.reprocessedBags} bags)")
                    }
                    if (summary.pendingReprocessKg > 0.0) {
                        val fastTag = if (summary.pendingFastActionCount > 0) " • FAST ACTION (${summary.pendingFastActionCount})" else ""
                        add("IN REPROCESS ${OperationsCalculator.formatKg(summary.pendingReprocessKg)} kg$fastTag")
                    }
                    if (summary.damagedReprocessKg > 0.0) {
                        add("DAMAGED -${OperationsCalculator.formatKg(summary.damagedReprocessKg)} kg (${summary.damagedReprocessBags} bags)")
                    }
                }.joinToString(" | ")
                FeedInventoryLevelEntity(
                    itemName = summary.sku.productName,
                    stockQuantity = summary.availableStockKg,
                    unit = "kg",
                    category = "Finished Feed",
                    section = summary.sku.section,
                    notes = reproNotes,
                    lastUpdatedTimestamp = nowMs
                )
            }
            val rawEntities = state.rawMaterials.map { mat ->
                FeedInventoryLevelEntity(
                    itemName = mat.name,
                    stockQuantity = mat.currentStockKg,
                    unit = "kg",
                    category = mat.category,
                    section = mat.supplierName,
                    lastUpdatedTimestamp = nowMs
                )
            }
            viewModelScope.launch {
                localInventoryRepository.syncInventorySnapshot(finishedEntities + rawEntities)
            }
        }
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing plant streams", rootError)
        emit(OpsUiState.Error(rootError.message ?: "Failed to load Bulacan Plant operations"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = OpsUiState.Loading
    )

    fun setLookbackDays(days: Int) {
        _lookbackDays.value = days
        logBreadcrumb(
            actor = BreadcrumbActor.USER,
            category = BreadcrumbCategory.INVENTORY,
            action = "Changed Dispatch Lookback Window",
            details = "Set rolling lookback window to $days day(s)",
            screenTab = "Monitor",
            entityRef = "${days}d",
            severity = BreadcrumbSeverity.INFO
        )
    }

    fun setSafetyBufferPct(pct: Double?) {
        _safetyBufferPct.value = pct
        val label = pct?.let { "${String.format("%.0f", it)}%" } ?: "SKU Default"
        logBreadcrumb(
            actor = BreadcrumbActor.USER,
            category = BreadcrumbCategory.INVENTORY,
            action = "Updated Safety Buffer Override",
            details = "Global safety buffer override set to $label",
            screenTab = "Monitor",
            entityRef = label,
            severity = BreadcrumbSeverity.INFO
        )
    }

    fun setRawMaterialDemandMultiplier(multiplier: Double) {
        _rawMaterialDemandMultiplier.value = multiplier.coerceIn(0.5, 2.5)
    }

    fun setRawMaterialIncludeScheduled(include: Boolean) {
        _rawMaterialIncludeScheduled.value = include
        logBreadcrumb(
            actor = BreadcrumbActor.USER,
            category = BreadcrumbCategory.INVENTORY,
            action = if (include) "Enabled Planned Schedule Demand in Forecast" else "Excluded Planned Schedule Demand",
            details = "Raw material projection forecast updated",
            screenTab = "Monitor",
            severity = BreadcrumbSeverity.INFO
        )
    }

    fun setRawMaterialLeadOverrideDays(days: Long?) {
        _rawMaterialLeadOverrideDays.value = days?.coerceIn(1L, 30L)
    }

    fun resetRawMaterialSimulator() {
        _rawMaterialDemandMultiplier.value = 1.0
        _rawMaterialIncludeScheduled.value = false
        _rawMaterialLeadOverrideDays.value = null
        _lookbackDays.value = 14
        _snackbarMessage.value = "Forecast simulator reset to 14d historical pace (1.0x)."
    }

    fun adjustRawMaterialStock(
        material: RawMaterialEntity,
        deltaKg: Double,
        reasonLabel: String = if (deltaKg >= 0) "Restock" else "Manual Usage"
    ) {
        if (!requireOperationalWrite("adjust raw material stock")) return
        if (material.id.isBlank() || kotlin.math.abs(deltaKg) < 0.01) return
        viewModelScope.launch {
            val newStock = max(0.0, round((material.currentStockKg + deltaKg) * 100.0) / 100.0)
            val newRestockDate = if (deltaKg > 0.0) currentDateLabel() else material.lastRestockedDate
            val updated = material.copy(
                currentStockKg = newStock,
                lastRestockedDate = newRestockDate
            )
            repository.updateRawMaterial(currentUserId, material.id, updated)
                .onSuccess {
                    recordUndoable(
                        label = "$reasonLabel ${material.name}",
                        undo = { repository.updateRawMaterial(currentUserId, material.id, material) },
                        redo = { repository.updateRawMaterial(currentUserId, material.id, updated) }
                    )
                    val deltaSign = if (deltaKg >= 0) "+" else ""
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.INVENTORY,
                        action = "$reasonLabel: ${material.name}",
                        details = "$deltaSign${OperationsCalculator.formatKg(deltaKg)} kg → New stock ${OperationsCalculator.formatKg(newStock)} kg",
                        screenTab = "Monitor",
                        entityRef = material.name,
                        severity = if (newStock <= material.reorderThresholdKg) BreadcrumbSeverity.WARNING else BreadcrumbSeverity.SUCCESS
                    )
                    if (newStock <= material.reorderThresholdKg) {
                        logBreadcrumb(
                            actor = BreadcrumbActor.SYSTEM,
                            category = BreadcrumbCategory.INVENTORY,
                            action = "Raw Material Low-Stock Alert: ${material.name}",
                            details = "Stock (${OperationsCalculator.formatKg(newStock)} kg) is at/below ${OperationsCalculator.formatKg(material.reorderThresholdKg)} kg reorder minimum",
                            screenTab = "Monitor",
                            entityRef = material.name,
                            severity = BreadcrumbSeverity.WARNING
                        )
                    }
                    _snackbarMessage.value = "${material.name}: ${OperationsCalculator.formatKg(newStock)} kg on hand."
                }
                .onFailure { err ->
                    _snackbarMessage.value = "Failed to update ${material.name}: ${err.localizedMessage}"
                }
        }
    }

    fun toggleRawMaterialAlert(material: RawMaterialEntity) {
        if (!requireOperationalWrite("toggle stock alerts")) return
        if (material.id.isBlank()) return
        viewModelScope.launch {
            val updated = material.copy(alertsEnabled = !material.alertsEnabled)
            repository.updateRawMaterial(currentUserId, material.id, updated)
                .onSuccess {
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.INVENTORY,
                        action = "${if (updated.alertsEnabled) "Enabled" else "Muted"} Alerts: ${material.name}",
                        details = "Low-stock alert preference updated",
                        screenTab = "Monitor",
                        entityRef = material.name,
                        severity = BreadcrumbSeverity.INFO
                    )
                    _snackbarMessage.value = if (updated.alertsEnabled) {
                        "Alerts enabled for ${material.name}."
                    } else {
                        "Alerts muted for ${material.name}."
                    }
                }
                .onFailure { err ->
                    _snackbarMessage.value = "Failed to toggle alert: ${err.localizedMessage}"
                }
        }
    }

    fun saveRawMaterial(
        existing: RawMaterialEntity?,
        name: String,
        category: RawMaterialCategory,
        currentStockKg: Double,
        reorderThresholdKg: Double,
        usageSharePct: Double,
        leadTimeDays: Long,
        safetyDays: Long,
        supplierName: String,
        alertsEnabled: Boolean
    ) {
        if (!requireOperationalWrite("save raw materials")) return
        viewModelScope.launch {
            val entity = RawMaterialEntity(
                id = existing?.id.orEmpty(),
                userId = currentUserId,
                name = name.trim(),
                category = category.label,
                currentStockKg = round(currentStockKg * 100.0) / 100.0,
                reorderThresholdKg = round(reorderThresholdKg * 100.0) / 100.0,
                usageSharePct = round(usageSharePct * 100.0) / 100.0,
                leadTimeDays = leadTimeDays.coerceIn(1L, 60L),
                safetyDays = safetyDays.coerceIn(0L, 30L),
                supplierName = supplierName.trim(),
                alertsEnabled = alertsEnabled,
                lastRestockedDate = existing?.lastRestockedDate.orEmpty()
            )
            if (existing == null || existing.id.isBlank()) {
                repository.addRawMaterial(currentUserId, entity)
                    .onSuccess { createdId ->
                        val saved = entity.copy(id = createdId)
                        recordUndoable(
                            label = "Add raw material ${saved.name}",
                            undo = { repository.deleteRawMaterial(currentUserId, createdId) },
                            redo = { repository.addRawMaterial(currentUserId, saved, fixedDocId = createdId).map {} }
                        )
                        logBreadcrumb(
                            actor = BreadcrumbActor.USER,
                            category = BreadcrumbCategory.INVENTORY,
                            action = "Added Raw Material: ${saved.name}",
                            details = "${OperationsCalculator.formatKg(saved.currentStockKg)} kg on hand • ${saved.usageSharePct}% mix share",
                            screenTab = "Monitor",
                            entityRef = saved.name,
                            severity = BreadcrumbSeverity.SUCCESS
                        )
                        _snackbarMessage.value = "Added raw material ${saved.name}."
                    }
                    .onFailure { err ->
                        _snackbarMessage.value = "Failed to add raw material: ${err.localizedMessage}"
                    }
            } else {
                repository.updateRawMaterial(currentUserId, existing.id, entity)
                    .onSuccess {
                        recordUndoable(
                            label = "Edit raw material ${entity.name}",
                            undo = { repository.updateRawMaterial(currentUserId, existing.id, existing) },
                            redo = { repository.updateRawMaterial(currentUserId, existing.id, entity) }
                        )
                        logBreadcrumb(
                            actor = BreadcrumbActor.USER,
                            category = BreadcrumbCategory.INVENTORY,
                            action = "Updated Raw Material: ${entity.name}",
                            details = "Stock ${OperationsCalculator.formatKg(entity.currentStockKg)} kg • Min ${OperationsCalculator.formatKg(entity.reorderThresholdKg)} kg • Lead ${entity.leadTimeDays}d",
                            screenTab = "Monitor",
                            entityRef = entity.name,
                            severity = BreadcrumbSeverity.INFO
                        )
                        _snackbarMessage.value = "Updated ${entity.name}."
                    }
                    .onFailure { err ->
                        _snackbarMessage.value = "Failed to update raw material: ${err.localizedMessage}"
                    }
            }
        }
    }

    fun deleteRawMaterial(material: RawMaterialEntity) {
        if (!requireOperationalWrite("delete raw materials")) return
        if (material.id.isBlank()) return
        viewModelScope.launch {
            repository.deleteRawMaterial(currentUserId, material.id)
                .onSuccess {
                    recordUndoable(
                        label = "Delete raw material ${material.name}",
                        undo = { repository.addRawMaterial(currentUserId, material, fixedDocId = material.id).map {} },
                        redo = { repository.deleteRawMaterial(currentUserId, material.id) }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.INVENTORY,
                        action = "Deleted Raw Material: ${material.name}",
                        details = "Removed ingredient from raw material monitor",
                        screenTab = "Monitor",
                        entityRef = material.name,
                        severity = BreadcrumbSeverity.WARNING
                    )
                    _snackbarMessage.value = "Removed ${material.name}."
                }
                .onFailure { err ->
                    _snackbarMessage.value = "Failed to remove material: ${err.localizedMessage}"
                }
        }
    }

    fun addRawMaterialsBulk(items: List<RawMaterialEntity>) {
        if (!requireOperationalWrite("import raw materials")) return
        if (items.isEmpty()) return
        viewModelScope.launch {
            val prepared = items.map { item ->
                item.copy(
                    userId = currentUserId,
                    name = item.name.trim(),
                    currentStockKg = round(item.currentStockKg * 100.0) / 100.0,
                    reorderThresholdKg = round(item.reorderThresholdKg * 100.0) / 100.0,
                    usageSharePct = (round(item.usageSharePct * 100.0) / 100.0).coerceIn(0.1, 100.0),
                    leadTimeDays = item.leadTimeDays.coerceIn(1L, 60L),
                    safetyDays = item.safetyDays.coerceIn(0L, 30L),
                    supplierName = item.supplierName.trim()
                )
            }
            repository.addRawMaterialsBulk(currentUserId, prepared)
                .onSuccess { createdPairs ->
                    val createdMaterials = createdPairs.map { it.second }
                    recordUndoable(
                        label = "Bulk add ${createdMaterials.size} raw material(s)",
                        undo = {
                            var lastErr: Throwable? = null
                            createdPairs.forEach { (id, _) ->
                                repository.deleteRawMaterial(currentUserId, id).onFailure { lastErr = it }
                            }
                            lastErr?.let { Result.failure<Unit>(it) } ?: Result.success(Unit)
                        },
                        redo = {
                            repository.addRawMaterialsBulk(currentUserId, createdMaterials).map {}
                        }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.INVENTORY,
                        action = "Bulk Added ${createdMaterials.size} Raw Material(s)",
                        details = createdMaterials.take(4).joinToString(", ") { it.name },
                        screenTab = "Monitor",
                        entityRef = "Raw Materials",
                        severity = BreadcrumbSeverity.SUCCESS
                    )
                    _snackbarMessage.value = "Added ${createdMaterials.size} raw material(s) to inventory."
                }
                .onFailure { err ->
                    _snackbarMessage.value = "Bulk add failed: ${err.localizedMessage}"
                }
        }
    }

    fun saveBom(
        existing: FeedBomEntity?,
        productName: String,
        section: PlantSection,
        batchBaseKg: Double,
        yieldLossPct: Double,
        lines: List<BomIngredientLine>,
        notes: String
    ) {
        if (!requireOperationalWrite("save feed BOM formulas")) return
        saveBomWithNewMaterials(
            newMaterials = emptyList(),
            existingBom = existing,
            productName = productName,
            section = section,
            batchBaseKg = batchBaseKg,
            yieldLossPct = yieldLossPct,
            draftLines = lines,
            notes = notes
        )
    }

    fun saveBomWithNewMaterials(
        newMaterials: List<RawMaterialEntity>,
        existingBom: FeedBomEntity?,
        productName: String,
        section: PlantSection,
        batchBaseKg: Double,
        yieldLossPct: Double,
        draftLines: List<BomIngredientLine>,
        notes: String
    ) {
        if (!requireOperationalWrite("save feed BOM formulas")) return
        val cleanProduct = productName.trim()
        if (cleanProduct.isBlank() || draftLines.isEmpty()) return
        viewModelScope.launch {
            var createdMaterialPairs: List<Pair<String, RawMaterialEntity>> = emptyList()
            if (newMaterials.isNotEmpty()) {
                val preparedNew = newMaterials.map { item ->
                    item.copy(
                        userId = currentUserId,
                        name = item.name.trim(),
                        currentStockKg = round(item.currentStockKg * 100.0) / 100.0,
                        reorderThresholdKg = round(item.reorderThresholdKg * 100.0) / 100.0,
                        usageSharePct = (round(item.usageSharePct * 100.0) / 100.0).coerceIn(0.1, 100.0),
                        leadTimeDays = item.leadTimeDays.coerceIn(1L, 60L),
                        safetyDays = item.safetyDays.coerceIn(0L, 30L),
                        supplierName = item.supplierName.trim()
                    )
                }
                val bulkResult = repository.addRawMaterialsBulk(currentUserId, preparedNew)
                if (bulkResult.isFailure) {
                    _snackbarMessage.value = "Failed to save new ingredients: ${bulkResult.exceptionOrNull()?.localizedMessage}"
                    return@launch
                }
                createdMaterialPairs = bulkResult.getOrNull().orEmpty()
            }

            val idByName = createdMaterialPairs.associate { (docId, mat) ->
                mat.name.trim().lowercase() to docId
            }
            val resolvedLines = draftLines.map { line ->
                val resolvedId = line.materialId.ifBlank {
                    idByName[line.materialName.trim().lowercase()].orEmpty()
                }
                line.copy(
                    materialId = resolvedId,
                    materialName = line.materialName.trim(),
                    inclusionPct = (round(line.inclusionPct * 100.0) / 100.0).coerceIn(0.01, 100.0),
                    notes = line.notes.trim()
                )
            }

            val entity = FeedBomEntity(
                id = existingBom?.id.orEmpty(),
                userId = currentUserId,
                productName = cleanProduct,
                section = section.label,
                batchBaseKg = round(batchBaseKg * 100.0) / 100.0,
                yieldLossPct = round(yieldLossPct * 100.0) / 100.0,
                lines = resolvedLines,
                notes = notes.trim()
            )

            val createdMaterials = createdMaterialPairs.map { it.second }
            if (existingBom == null || existingBom.id.isBlank()) {
                repository.addBom(currentUserId, entity)
                    .onSuccess { createdBomId ->
                        val savedBom = entity.copy(id = createdBomId)
                        recordUndoable(
                            label = "Save BOM ${savedBom.productName}",
                            undo = {
                                createdMaterialPairs.forEach { (matId, _) ->
                                    repository.deleteRawMaterial(currentUserId, matId)
                                }
                                repository.deleteBom(currentUserId, createdBomId)
                            },
                            redo = {
                                val matRes = if (createdMaterials.isNotEmpty()) {
                                    repository.addRawMaterialsBulk(currentUserId, createdMaterials).map {}
                                } else Result.success(Unit)
                                if (matRes.isFailure) matRes
                                else repository.addBom(currentUserId, savedBom, fixedDocId = createdBomId).map {}
                            }
                        )
                        val newSuffix = if (createdMaterials.isNotEmpty()) {
                            " + ${createdMaterials.size} new ingredient(s)"
                        } else ""
                        logBreadcrumb(
                            actor = BreadcrumbActor.USER,
                            category = BreadcrumbCategory.INVENTORY,
                            action = "Created BOM: ${savedBom.productName}",
                            details = "${savedBom.lines.size} ingredients (${String.format("%.1f%%", savedBom.totalInclusionPct())})$newSuffix",
                            screenTab = "Monitor",
                            entityRef = savedBom.productName,
                            severity = BreadcrumbSeverity.SUCCESS
                        )
                        _snackbarMessage.value = "Saved BOM for ${savedBom.productName}$newSuffix."
                    }
                    .onFailure { err ->
                        _snackbarMessage.value = "Failed to save BOM: ${err.localizedMessage}"
                    }
            } else {
                repository.updateBom(currentUserId, existingBom.id, entity)
                    .onSuccess {
                        recordUndoable(
                            label = "Update BOM ${entity.productName}",
                            undo = {
                                createdMaterialPairs.forEach { (matId, _) ->
                                    repository.deleteRawMaterial(currentUserId, matId)
                                }
                                repository.updateBom(currentUserId, existingBom.id, existingBom)
                            },
                            redo = {
                                val matRes = if (createdMaterials.isNotEmpty()) {
                                    repository.addRawMaterialsBulk(currentUserId, createdMaterials).map {}
                                } else Result.success(Unit)
                                if (matRes.isFailure) matRes
                                else repository.updateBom(currentUserId, existingBom.id, entity)
                            }
                        )
                        val newSuffix = if (createdMaterials.isNotEmpty()) {
                            " + ${createdMaterials.size} new ingredient(s)"
                        } else ""
                        logBreadcrumb(
                            actor = BreadcrumbActor.USER,
                            category = BreadcrumbCategory.INVENTORY,
                            action = "Updated BOM: ${entity.productName}",
                            details = "${entity.lines.size} ingredients (${String.format("%.1f%%", entity.totalInclusionPct())})$newSuffix",
                            screenTab = "Monitor",
                            entityRef = entity.productName,
                            severity = BreadcrumbSeverity.INFO
                        )
                        _snackbarMessage.value = "Updated BOM for ${entity.productName}$newSuffix."
                    }
                    .onFailure { err ->
                        _snackbarMessage.value = "Failed to update BOM: ${err.localizedMessage}"
                    }
            }
        }
    }

    fun deleteBom(bom: FeedBomEntity) {
        if (!requireOperationalWrite("delete feed BOM formulas")) return
        if (bom.id.isBlank()) return
        viewModelScope.launch {
            repository.deleteBom(currentUserId, bom.id)
                .onSuccess {
                    recordUndoable(
                        label = "Delete BOM ${bom.productName}",
                        undo = { repository.addBom(currentUserId, bom, fixedDocId = bom.id).map {} },
                        redo = { repository.deleteBom(currentUserId, bom.id) }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.INVENTORY,
                        action = "Deleted BOM: ${bom.productName}",
                        details = "Removed ${bom.lines.size}-line Bill of Materials",
                        screenTab = "Monitor",
                        entityRef = bom.productName,
                        severity = BreadcrumbSeverity.WARNING
                    )
                    _snackbarMessage.value = "Deleted BOM for ${bom.productName}."
                }
                .onFailure { err ->
                    _snackbarMessage.value = "Failed to delete BOM: ${err.localizedMessage}"
                }
        }
    }

    fun recordRawMaterialAlertBreadcrumb(urgentCount: Int, summaryText: String) {
        if (urgentCount <= 0) return
        logBreadcrumb(
            actor = BreadcrumbActor.SYSTEM,
            category = BreadcrumbCategory.INVENTORY,
            action = "Low Raw Material Forecast Alert ($urgentCount)",
            details = summaryText,
            screenTab = "Monitor",
            entityRef = "Raw Materials",
            severity = BreadcrumbSeverity.WARNING
        )
    }

    fun resetMonitorParameters() {
        _lookbackDays.value = 14
        _safetyBufferPct.value = null
        _rawMaterialDemandMultiplier.value = 1.0
        _rawMaterialIncludeScheduled.value = false
        _rawMaterialLeadOverrideDays.value = null
        _snackbarMessage.value = "Filters reset to 14d window and default parameters."
        logBreadcrumb(
            actor = BreadcrumbActor.USER,
            category = BreadcrumbCategory.INVENTORY,
            action = "Reset Monitor Filters",
            details = "Restored 14d dispatch/consumption window and default parameters",
            severity = BreadcrumbSeverity.INFO
        )
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun retryInitialSync() {
        viewModelScope.launch {
            repository.ensureSeededOctober2026(currentUserId)
                .onSuccess {
                    _snackbarMessage.value = "Synced Bulacan Plant SKU records."
                    logBreadcrumb(
                        actor = BreadcrumbActor.SYSTEM,
                        category = BreadcrumbCategory.SYSTEM,
                        action = "Manual Firestore Re-Sync Succeeded",
                        details = "Synced Bulacan Plant SKU records after retry",
                        severity = BreadcrumbSeverity.SUCCESS
                    )
                }
                .onFailure { err ->
                    _snackbarMessage.value = "Sync failed: ${err.localizedMessage}"
                    logBreadcrumb(
                        actor = BreadcrumbActor.SYSTEM,
                        category = BreadcrumbCategory.SYSTEM,
                        action = "Manual Firestore Re-Sync Failed",
                        details = err.localizedMessage ?: "Unknown sync error",
                        severity = BreadcrumbSeverity.ERROR
                    )
                }
        }
    }

    fun saveSku(
        existingSku: SkuEntity?,
        productName: String,
        section: PlantSection,
        openingBags: Double,
        standardBagWeightKg: Double,
        targetRecoveryPct: Double,
        safetyBufferPct: Double,
        defaultRemarks: String
    ) {
        if (!requireOperationalWrite("save SKU stock")) return
        viewModelScope.launch {
            val openingKg = round(openingBags * standardBagWeightKg * 100.0) / 100.0
            val entity = SkuEntity(
                id = existingSku?.id.orEmpty(),
                userId = currentUserId,
                productName = productName.trim(),
                section = section.label,
                openingStockKg = openingKg,
                standardBagWeightKg = standardBagWeightKg,
                targetRecoveryPct = targetRecoveryPct,
                safetyBufferPct = safetyBufferPct,
                defaultRemarks = defaultRemarks
            )
            if (existingSku == null || existingSku.id.isBlank()) {
                repository.addSku(currentUserId, entity)
                    .onSuccess { createdId ->
                        recordUndoable(
                            label = "Add ${entity.productName}",
                            undo = { repository.deleteSku(currentUserId, createdId) },
                            redo = { repository.addSku(currentUserId, entity, fixedDocId = createdId).map {} }
                        )
                        _snackbarMessage.value = "Added ${entity.productName} (${OperationsCalculator.formatKg(openingKg)} kg)."
                        logBreadcrumb(
                            actor = BreadcrumbActor.USER,
                            category = BreadcrumbCategory.INVENTORY,
                            action = "Added SKU ${entity.productName}",
                            details = "Section ${entity.section} • Opening ${OperationsCalculator.formatKg(openingKg)} kg (${OperationsCalculator.formatBags(openingBags)} bags)",
                            entityRef = entity.productName,
                            severity = BreadcrumbSeverity.SUCCESS
                        )
                    }
                    .onFailure {
                        _snackbarMessage.value = "Failed to save raw material / SKU: ${it.localizedMessage}"
                        logBreadcrumb(
                            actor = BreadcrumbActor.SYSTEM,
                            category = BreadcrumbCategory.INVENTORY,
                            action = "SKU Save Failed: ${entity.productName}",
                            details = it.localizedMessage ?: "Unknown error",
                            entityRef = entity.productName,
                            severity = BreadcrumbSeverity.ERROR
                        )
                    }
            } else {
                repository.updateSku(currentUserId, existingSku.id, entity)
                    .onSuccess {
                        recordUndoable(
                            label = "Update ${entity.productName}",
                            undo = { repository.updateSku(currentUserId, existingSku.id, existingSku) },
                            redo = { repository.updateSku(currentUserId, existingSku.id, entity) }
                        )
                        _snackbarMessage.value = "Updated ${entity.productName} (${OperationsCalculator.formatKg(openingKg)} kg)."
                        logBreadcrumb(
                            actor = BreadcrumbActor.USER,
                            category = BreadcrumbCategory.INVENTORY,
                            action = "Updated SKU ${entity.productName}",
                            details = "Section ${entity.section} • Opening ${OperationsCalculator.formatKg(openingKg)} kg",
                            entityRef = entity.productName,
                            severity = BreadcrumbSeverity.SUCCESS
                        )
                    }
                    .onFailure {
                        _snackbarMessage.value = "Failed to save raw material / SKU: ${it.localizedMessage}"
                        logBreadcrumb(
                            actor = BreadcrumbActor.SYSTEM,
                            category = BreadcrumbCategory.INVENTORY,
                            action = "SKU Update Failed: ${entity.productName}",
                            details = it.localizedMessage ?: "Unknown error",
                            entityRef = entity.productName,
                            severity = BreadcrumbSeverity.ERROR
                        )
                    }
            }
        }
    }

    fun deleteSku(sku: SkuEntity) {
        if (!requireAdminPermission("delete SKUs")) return
        viewModelScope.launch {
            repository.deleteSku(currentUserId, sku.id)
                .onSuccess {
                    recordUndoable(
                        label = "Delete ${sku.productName}",
                        undo = { repository.addSku(currentUserId, sku, fixedDocId = sku.id).map {} },
                        redo = { repository.deleteSku(currentUserId, sku.id) }
                    )
                    _snackbarMessage.value = "Removed ${sku.productName} from list."
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.INVENTORY,
                        action = "Deleted SKU ${sku.productName}",
                        details = "Removed from ${sku.section}",
                        entityRef = sku.productName,
                        severity = BreadcrumbSeverity.WARNING
                    )
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to delete item: ${it.localizedMessage}"
                }
        }
    }

    fun saveCustomer(
        existingCustomer: CustomerEntity?,
        name: String,
        notes: String,
        priority: Boolean = false
    ) {
        if (!requireOperationalWrite("save customers")) return
        viewModelScope.launch {
            val trimmedName = name.trim()
            val trimmedNotes = notes.trim()
            val tag = if (priority) " [Priority]" else ""
            if (existingCustomer == null || existingCustomer.id.isBlank()) {
                repository.addCustomer(currentUserId, trimmedName, trimmedNotes, priority)
                    .onSuccess { createdId ->
                        recordUndoable(
                            label = "Add customer $trimmedName",
                            undo = { repository.deleteCustomer(currentUserId, createdId) },
                            redo = {
                                repository.addCustomer(
                                    userId = currentUserId,
                                    name = trimmedName,
                                    notes = trimmedNotes,
                                    priority = priority,
                                    fixedDocId = createdId
                                ).map {}
                            }
                        )
                        _snackbarMessage.value = "Added customer $trimmedName$tag."
                        logBreadcrumb(
                            actor = BreadcrumbActor.USER,
                            category = BreadcrumbCategory.CUSTOMERS,
                            action = "Added Customer $trimmedName",
                            details = if (priority) "Marked as Priority account" else "Standard account",
                            entityRef = trimmedName,
                            severity = BreadcrumbSeverity.SUCCESS
                        )
                    }
                    .onFailure {
                        _snackbarMessage.value = "Failed to save customer: ${it.localizedMessage}"
                    }
            } else {
                repository.updateCustomer(currentUserId, existingCustomer.id, trimmedName, trimmedNotes, priority)
                    .onSuccess {
                        recordUndoable(
                            label = "Update customer $trimmedName",
                            undo = {
                                repository.updateCustomer(
                                    userId = currentUserId,
                                    customerId = existingCustomer.id,
                                    name = existingCustomer.name,
                                    notes = existingCustomer.notes,
                                    priority = existingCustomer.priority
                                )
                            },
                            redo = {
                                repository.updateCustomer(
                                    userId = currentUserId,
                                    customerId = existingCustomer.id,
                                    name = trimmedName,
                                    notes = trimmedNotes,
                                    priority = priority
                                )
                            }
                        )
                        _snackbarMessage.value = "Updated customer $trimmedName$tag."
                        logBreadcrumb(
                            actor = BreadcrumbActor.USER,
                            category = BreadcrumbCategory.CUSTOMERS,
                            action = "Updated Customer $trimmedName",
                            details = if (priority) "Priority account" else "Standard account",
                            entityRef = trimmedName,
                            severity = BreadcrumbSeverity.SUCCESS
                        )
                    }
                    .onFailure {
                        _snackbarMessage.value = "Failed to save customer: ${it.localizedMessage}"
                    }
            }
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        if (!requireOperationalWrite("delete customers")) return
        viewModelScope.launch {
            repository.deleteCustomer(currentUserId, customer.id)
                .onSuccess {
                    recordUndoable(
                        label = "Delete customer ${customer.name}",
                        undo = {
                            repository.addCustomer(
                                userId = currentUserId,
                                name = customer.name,
                                notes = customer.notes,
                                priority = customer.priority,
                                fixedDocId = customer.id
                            ).map {}
                        },
                        redo = { repository.deleteCustomer(currentUserId, customer.id) }
                    )
                    _snackbarMessage.value = "Removed customer ${customer.name}."
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.CUSTOMERS,
                        action = "Removed Customer ${customer.name}",
                        details = "Deleted customer directory entry",
                        entityRef = customer.name,
                        severity = BreadcrumbSeverity.WARNING
                    )
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to remove customer: ${it.localizedMessage}"
                }
        }
    }

    fun logDispatch(input: DispatchEntryInput) {
        if (!requireOperationalWrite("log dispatches")) return
        viewModelScope.launch {
            val netKg = if (input.hasReturn && input.returnConfirmed) {
                max(0.0, input.totalWeightKg - input.returnedWeightKg)
            } else {
                input.totalWeightKg
            }
            val ready = opsState.value as? OpsUiState.Ready
            val cleanInputPlant = normalizeSourcePlantLabel(input.sourcePlant)
            val matchedSummary = ready?.summaries?.firstOrNull {
                cleanInputPlant.isNotBlank() &&
                    it.sku.section.equals(cleanInputPlant, ignoreCase = true) &&
                    it.sku.productName.equals(input.productName, ignoreCase = true)
            } ?: ready?.summaries?.firstOrNull {
                it.sku.productName.equals(input.productName, ignoreCase = true)
            }
            if (matchedSummary != null && netKg > 0.0) {
                val availKg = matchedSummary.availableStockKg.coerceAtLeast(0.0)
                if (netKg > availKg + 0.01) {
                    val msg =
                        "Flagged: Dispatch (${OperationsCalculator.formatKg(netKg)} kg) exceeds available stock (${OperationsCalculator.formatKg(availKg)} kg). Add new production or cancel dispatch."
                    _snackbarMessage.value = msg
                    logBreadcrumb(
                        actor = BreadcrumbActor.SYSTEM,
                        category = BreadcrumbCategory.DISPATCH,
                        action = "Dispatch Blocked by Stock Guard",
                        details = "${input.productName}: requested ${OperationsCalculator.formatKg(netKg)} kg vs ${OperationsCalculator.formatKg(availKg)} kg available (DR ${input.drReference.trim()})",
                        screenTab = "Dispatched",
                        entityRef = input.drReference.trim().ifBlank { input.productName },
                        severity = BreadcrumbSeverity.WARNING
                    )
                    return@launch
                }
            }
            val matchedSection = matchedSummary?.sku?.section
                ?: ready?.skus?.firstOrNull { it.productName.equals(input.productName, ignoreCase = true) }?.section
                ?: ""
            val resolvedSourcePlant = cleanInputPlant.ifBlank { normalizeSourcePlantLabel(matchedSection) }
            val statusLabel = when {
                input.isHeld -> DispatchStatus.HELD.label
                input.hasReturn && input.returnConfirmed -> DispatchStatus.RETURNED.label
                else -> DispatchStatus.DELIVERED.label
            }
            val record = DispatchRecord(
                userId = currentUserId,
                date = input.date,
                productName = input.productName,
                bagsDispatched = input.bags,
                bagWeightKg = input.bagWeightKg,
                totalWeightKg = input.totalWeightKg,
                drReference = input.drReference.trim(),
                customer = input.customer.trim(),
                linkedOrderId = input.linkedOrder?.id ?: "",
                sourcePlant = resolvedSourcePlant,
                driverName = input.driverName.trim(),
                truckNumber = input.truckNumber.trim(),
                helperName = input.helperName.trim(),
                remarks = input.remarks.trim(),
                dispatchStatus = statusLabel,
                hasReturn = input.hasReturn,
                returnConfirmed = input.hasReturn && input.returnConfirmed,
                returnedBags = if (input.hasReturn && input.returnConfirmed) input.returnedBags else 0L,
                returnedWeightKg = if (input.hasReturn && input.returnConfirmed) input.returnedWeightKg else 0.0,
                returnReason = if (input.hasReturn && input.returnConfirmed) input.returnReason.trim() else "",
                isHeld = input.isHeld,
                heldRemarks = if (input.isHeld) input.heldRemarks.trim() else ""
            )
            val routeToReprocess = input.hasReturn && input.returnConfirmed && input.sendReturnedToReprocess && input.returnedBags > 0L

            repository.logDispatch(
                userId = currentUserId,
                record = record,
                linkedOrder = input.linkedOrder,
                createRushOrder = input.linkedOrder == null && input.createRushOrderIfUnlinked,
                createReprocessOnReturn = routeToReprocess,
                reprocessSection = resolvedSourcePlant.ifBlank { PlantSection.SAN_ROQUE.label }
            )
                .onSuccess { outcome ->
                    val dispatchId = outcome.dispatchId
                    val createdRush = outcome.createdRushOrder
                    val createdReprocess = outcome.createdReprocessItem
                    recordUndoable(
                        label = "Dispatch ${record.drReference}",
                        undo = {
                            repository.undoLogDispatch(
                                userId = currentUserId,
                                dispatchId = dispatchId,
                                previousOrder = input.linkedOrder,
                                createdRushOrderId = createdRush?.id,
                                createdReprocessId = createdReprocess?.id
                            )
                        },
                        redo = {
                            repository.logDispatch(
                                userId = currentUserId,
                                record = if (createdRush != null) record.copy(linkedOrderId = createdRush.id) else record,
                                linkedOrder = input.linkedOrder,
                                fixedDocId = dispatchId,
                                fixedRushOrder = createdRush,
                                fixedReprocessItem = createdReprocess
                            ).map {}
                        }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.DISPATCH,
                        action = "Logged Dispatch ${record.drReference}",
                        details = "${record.productName} • ${record.bagsDispatched} bags (${OperationsCalculator.formatKg(netKg)} kg net) to ${record.customer} [$statusLabel]",
                        screenTab = "Dispatched",
                        entityRef = record.drReference.ifBlank { record.productName },
                        severity = BreadcrumbSeverity.SUCCESS
                    )
                    input.linkedOrder?.let { linked ->
                        val updatedDispKg = linked.dispatchedWeightKg + netKg
                        val newOrderStatus = when {
                            linked.status == OrderStatus.CANCELLED.label -> OrderStatus.CANCELLED.label
                            updatedDispKg >= linked.totalWeightKg - 0.01 -> OrderStatus.FULFILLED.label
                            updatedDispKg > 0.0 -> OrderStatus.PARTIALLY_DISPATCHED.label
                            else -> OrderStatus.OPEN.label
                        }
                        logBreadcrumb(
                            actor = BreadcrumbActor.SYSTEM,
                            category = BreadcrumbCategory.ORDERS,
                            action = "Linked Order Updated (${linked.orderCode})",
                            details = "Order ${linked.orderCode} transitioned ${linked.status} → $newOrderStatus after DR ${record.drReference}",
                            screenTab = "Orders",
                            entityRef = linked.orderCode,
                            severity = BreadcrumbSeverity.INFO
                        )
                    }
                    if (createdRush != null) {
                        logBreadcrumb(
                            actor = BreadcrumbActor.SYSTEM,
                            category = BreadcrumbCategory.ORDERS,
                            action = "Added Rush Order (${createdRush.orderCode})",
                            details = "Auto-added ${createdRush.customer} • ${createdRush.productName} (${createdRush.orderedBags} bags) to Orders → Rush Orders",
                            screenTab = "Orders",
                            entityRef = createdRush.orderCode,
                            severity = BreadcrumbSeverity.SUCCESS
                        )
                    }
                    if (createdReprocess != null) {
                        logBreadcrumb(
                            actor = BreadcrumbActor.SYSTEM,
                            category = BreadcrumbCategory.REPROCESS,
                            action = "Queued Return for Fast Reprocess (${createdReprocess.productName})",
                            details = "${createdReprocess.returnedBags} bags (${OperationsCalculator.formatKg(createdReprocess.returnedWeightKg)} kg) from DR ${createdReprocess.drReference} • Reason: ${createdReprocess.returnReason}",
                            screenTab = "Reprocess",
                            entityRef = createdReprocess.drReference.ifBlank { createdReprocess.productName },
                            severity = BreadcrumbSeverity.WARNING
                        )
                    }
                    val rushNote = if (createdRush != null) " • Added ${createdRush.orderCode} to Rush Orders" else ""
                    val reproNote = if (createdReprocess != null) " • Queued in Reprocess (Fast Action)" else ""
                    _snackbarMessage.value = "Dispatch ${record.drReference} ($statusLabel) logged (${OperationsCalculator.formatKg(netKg)} kg net)$rushNote$reproNote."
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to log dispatch: ${it.localizedMessage}"
                    logBreadcrumb(
                        actor = BreadcrumbActor.SYSTEM,
                        category = BreadcrumbCategory.DISPATCH,
                        action = "Dispatch Save Failed",
                        details = "DR ${record.drReference}: ${it.localizedMessage ?: "Unknown error"}",
                        screenTab = "Dispatched",
                        entityRef = record.drReference,
                        severity = BreadcrumbSeverity.ERROR
                    )
                }
        }
    }

    fun logMultipleDispatches(inputs: List<DispatchEntryInput>) {
        if (!requireOperationalWrite("log dispatches")) return
        if (inputs.isEmpty()) return
        viewModelScope.launch {
            val ready = opsState.value as? OpsUiState.Ready
            if (ready != null) {
                val netByProduct = linkedMapOf<String, Double>()
                inputs.forEach { input ->
                    val netKg = if (input.hasReturn && input.returnConfirmed) {
                        max(0.0, input.totalWeightKg - input.returnedWeightKg)
                    } else {
                        input.totalWeightKg
                    }
                    val key = input.productName.trim().lowercase()
                    netByProduct[key] = (netByProduct[key] ?: 0.0) + netKg
                }
                for ((prodKey, requestedKg) in netByProduct) {
                    val matchedSummary = ready.summaries.firstOrNull {
                        it.sku.productName.trim().lowercase() == prodKey
                    }
                    if (matchedSummary != null && requestedKg > 0.0) {
                        val availKg = matchedSummary.availableStockKg.coerceAtLeast(0.0)
                        if (requestedKg > availKg + 0.01) {
                            _snackbarMessage.value =
                                "Flagged: ${matchedSummary.sku.productName} (${OperationsCalculator.formatKg(requestedKg)} kg) exceeds available stock (${OperationsCalculator.formatKg(availKg)} kg)."
                            logBreadcrumb(
                                actor = BreadcrumbActor.SYSTEM,
                                category = BreadcrumbCategory.DISPATCH,
                                action = "Batch Dispatch Blocked by Stock Guard",
                                details = "${matchedSummary.sku.productName}: ${OperationsCalculator.formatKg(requestedKg)} kg exceeds ${OperationsCalculator.formatKg(availKg)} kg available",
                                screenTab = "Dispatched",
                                entityRef = matchedSummary.sku.productName,
                                severity = BreadcrumbSeverity.WARNING
                            )
                            return@launch
                        }
                    }
                }
            }

            val skuSectionMap = ready?.skus
                ?.associate { it.productName.trim().lowercase() to it.section }
                .orEmpty()
            val batchItems = inputs.map { input ->
                val resolvedSourcePlant = normalizeSourcePlantLabel(input.sourcePlant)
                    .ifBlank { normalizeSourcePlantLabel(skuSectionMap[input.productName.trim().lowercase()].orEmpty()) }
                val statusLabel = when {
                    input.isHeld -> DispatchStatus.HELD.label
                    input.hasReturn && input.returnConfirmed -> DispatchStatus.RETURNED.label
                    else -> DispatchStatus.DELIVERED.label
                }
                val record = DispatchRecord(
                    userId = currentUserId,
                    date = input.date,
                    productName = input.productName,
                    bagsDispatched = input.bags,
                    bagWeightKg = input.bagWeightKg,
                    totalWeightKg = input.totalWeightKg,
                    drReference = input.drReference.trim(),
                    customer = input.customer.trim(),
                    linkedOrderId = input.linkedOrder?.id ?: "",
                    sourcePlant = resolvedSourcePlant,
                    driverName = input.driverName.trim(),
                    truckNumber = input.truckNumber.trim(),
                    helperName = input.helperName.trim(),
                    remarks = input.remarks.trim(),
                    dispatchStatus = statusLabel,
                    hasReturn = input.hasReturn,
                    returnConfirmed = input.hasReturn && input.returnConfirmed,
                    returnedBags = if (input.hasReturn && input.returnConfirmed) input.returnedBags else 0L,
                    returnedWeightKg = if (input.hasReturn && input.returnConfirmed) input.returnedWeightKg else 0.0,
                    returnReason = if (input.hasReturn && input.returnConfirmed) input.returnReason.trim() else "",
                    isHeld = input.isHeld,
                    heldRemarks = if (input.isHeld) input.heldRemarks.trim() else ""
                )
                DispatchBatchItem(
                    record = record,
                    linkedOrder = input.linkedOrder,
                    createRushOrder = input.linkedOrder == null && input.createRushOrderIfUnlinked,
                    createReprocessOnReturn = input.hasReturn && input.returnConfirmed && input.sendReturnedToReprocess && input.returnedBags > 0L,
                    reprocessSection = resolvedSourcePlant.ifBlank { PlantSection.SAN_ROQUE.label }
                )
            }

            val previousOrders = inputs.mapNotNull { it.linkedOrder }.distinctBy { it.id }
            val totalNetKg = batchItems.sumOf { it.record.effectiveDispatchedWeightKg() }
            val distinctDrs = batchItems.map { it.record.drReference.trim() }.filter { it.isNotBlank() }.distinct()
            val firstDr = distinctDrs.firstOrNull().orEmpty()
            val drLabel = when {
                distinctDrs.isEmpty() -> "No DR#"
                distinctDrs.size == 1 -> firstDr
                else -> "$firstDr (+${distinctDrs.size - 1} DRs)"
            }
            val distinctCustomers = batchItems.map { it.record.customer.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }
            val customerLabel = when (distinctCustomers.size) {
                0 -> "Unspecified Customer"
                1 -> distinctCustomers.first()
                2 -> distinctCustomers.joinToString(" & ")
                else -> "${distinctCustomers.first()} + ${distinctCustomers.size - 1} more"
            }
            val truckPlate = batchItems.first().record.truckNumber.trim()
            val truckNote = if (truckPlate.isNotBlank()) " • Truck $truckPlate" else ""

            repository.logDispatchesBatch(currentUserId, batchItems)
                .onSuccess { outcome ->
                    val createdRecords = outcome.createdRecords
                    val createdRushOrders = outcome.createdRushOrders
                    val createdReprocessItems = outcome.createdReprocessItems
                    val rushById = createdRushOrders.associateBy { it.id }
                    val repByDispatchId = createdReprocessItems.associateBy { it.sourceDispatchId }
                    val redoItems = createdRecords.mapIndexed { idx, rec ->
                        DispatchBatchItem(
                            record = rec,
                            linkedOrder = batchItems.getOrNull(idx)?.linkedOrder,
                            createRushOrder = false,
                            fixedRushOrder = rushById[rec.linkedOrderId],
                            fixedReprocessItem = repByDispatchId[rec.id]
                        )
                    }
                    recordUndoable(
                        label = if (createdRecords.size == 1) {
                            "Dispatch $drLabel"
                        } else {
                            "Dispatch ${createdRecords.size} stops ($drLabel)"
                        },
                        undo = {
                            repository.deleteDispatchesBatch(
                                userId = currentUserId,
                                records = createdRecords,
                                previousOrders = previousOrders,
                                orderIdsToDelete = createdRushOrders.map { it.id },
                                reprocessIdsToDelete = createdReprocessItems.map { it.id }
                            )
                        },
                        redo = { repository.logDispatchesBatch(currentUserId, redoItems, preserveIds = true).map {} }
                    )
                    val linkedOrderCount = previousOrders.size
                    val rushOrderCount = createdRushOrders.size
                    val reproCount = createdReprocessItems.size
                    val orderNote = if (linkedOrderCount > 0) " • linked to $linkedOrderCount order(s)" else ""
                    val rushNote = if (rushOrderCount > 0) " • +$rushOrderCount Rush Order(s) in Orders" else ""
                    val reproNote = if (reproCount > 0) " • +$reproCount returned item(s) in Reprocess" else ""
                    val custNote = if (distinctCustomers.size > 1) "${distinctCustomers.size} customers ($customerLabel)" else "Customer $customerLabel"
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.DISPATCH,
                        action = if (createdRecords.size == 1) "Logged Dispatch $drLabel" else "Logged ${createdRecords.size} Dispatch Stops ($drLabel)",
                        details = "${OperationsCalculator.formatKg(totalNetKg)} kg net$orderNote$rushNote$reproNote$truckNote • $custNote",
                        screenTab = "Dispatched",
                        entityRef = truckPlate.ifBlank { firstDr.ifBlank { batchItems.first().record.productName } },
                        severity = BreadcrumbSeverity.SUCCESS
                    )
                    if (linkedOrderCount > 0) {
                        logBreadcrumb(
                            actor = BreadcrumbActor.SYSTEM,
                            category = BreadcrumbCategory.ORDERS,
                            action = "Auto-Updated $linkedOrderCount Linked Order(s)",
                            details = "Orders synced: ${previousOrders.joinToString(", ") { it.orderCode }}",
                            screenTab = "Orders",
                            entityRef = previousOrders.joinToString(", ") { it.orderCode },
                            severity = BreadcrumbSeverity.INFO
                        )
                    }
                    if (rushOrderCount > 0) {
                        logBreadcrumb(
                            actor = BreadcrumbActor.SYSTEM,
                            category = BreadcrumbCategory.ORDERS,
                            action = "Added $rushOrderCount Rush Order(s) from Dispatch",
                            details = "Added to Rush Orders tab: ${createdRushOrders.joinToString(", ") { "${it.orderCode} (${it.customer})" }}",
                            screenTab = "Orders",
                            entityRef = createdRushOrders.joinToString(", ") { it.orderCode },
                            severity = BreadcrumbSeverity.SUCCESS
                        )
                    }
                    if (reproCount > 0) {
                        logBreadcrumb(
                            actor = BreadcrumbActor.SYSTEM,
                            category = BreadcrumbCategory.REPROCESS,
                            action = "Queued $reproCount Return(s) for Fast Reprocess",
                            details = createdReprocessItems.joinToString(", ") { "${it.productName} (${it.returnedBags}b, ${it.customer})" },
                            screenTab = "Reprocess",
                            entityRef = firstDr,
                            severity = BreadcrumbSeverity.WARNING
                        )
                    }
                    _snackbarMessage.value = if (createdRecords.size == 1) {
                        "Dispatch $drLabel logged (${OperationsCalculator.formatKg(totalNetKg)} kg net)$orderNote$rushNote$reproNote."
                    } else {
                        "Logged ${createdRecords.size} stops for $custNote$truckNote (${OperationsCalculator.formatKg(totalNetKg)} kg net)$orderNote$rushNote$reproNote."
                    }
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to log multi-item dispatch: ${it.localizedMessage}"
                    logBreadcrumb(
                        actor = BreadcrumbActor.SYSTEM,
                        category = BreadcrumbCategory.DISPATCH,
                        action = "Multi-Dispatch Save Failed",
                        details = "DR $firstDr: ${it.localizedMessage ?: "Unknown error"}",
                        screenTab = "Dispatched",
                        entityRef = firstDr,
                        severity = BreadcrumbSeverity.ERROR
                    )
                }
        }
    }

    fun updateDispatch(existing: DispatchRecord, input: DispatchEntryInput) {
        if (!requireOperationalWrite("edit dispatches")) return
        viewModelScope.launch {
            val ready = opsState.value as? OpsUiState.Ready
            val resolvedLinkedOrder = input.linkedOrder ?: ready?.orders?.firstOrNull {
                it.id.isNotBlank() && it.id == existing.linkedOrderId
            }
            val statusLabel = when {
                input.isHeld -> DispatchStatus.HELD.label
                input.hasReturn && input.returnConfirmed -> DispatchStatus.RETURNED.label
                else -> DispatchStatus.DELIVERED.label
            }
            val resolvedSourcePlant = normalizeSourcePlantLabel(input.sourcePlant)
                .ifBlank { normalizeSourcePlantLabel(existing.sourcePlant) }
                .ifBlank {
                    ready?.skus?.firstOrNull {
                        it.productName.equals(input.productName, ignoreCase = true)
                    }?.section.orEmpty().let(::normalizeSourcePlantLabel)
                }
            val updated = existing.copy(
                date = input.date,
                productName = input.productName,
                bagsDispatched = input.bags,
                bagWeightKg = input.bagWeightKg,
                totalWeightKg = input.totalWeightKg,
                drReference = input.drReference.trim(),
                customer = input.customer.trim(),
                linkedOrderId = resolvedLinkedOrder?.id ?: existing.linkedOrderId,
                sourcePlant = resolvedSourcePlant,
                driverName = input.driverName.trim(),
                truckNumber = input.truckNumber.trim(),
                helperName = input.helperName.trim(),
                remarks = input.remarks.trim(),
                dispatchStatus = statusLabel,
                hasReturn = input.hasReturn,
                returnConfirmed = input.hasReturn && input.returnConfirmed,
                returnedBags = if (input.hasReturn && input.returnConfirmed) input.returnedBags else 0L,
                returnedWeightKg = if (input.hasReturn && input.returnConfirmed) input.returnedWeightKg else 0.0,
                returnReason = if (input.hasReturn && input.returnConfirmed) input.returnReason.trim() else "",
                isHeld = input.isHeld,
                heldRemarks = if (input.isHeld) input.heldRemarks.trim() else ""
            )
            repository.updateDispatch(currentUserId, existing.id, updated, existing, resolvedLinkedOrder)
                .onSuccess {
                    val alreadyLinked = ready?.reprocessItems?.any {
                        it.sourceDispatchId == existing.id
                    } == true
                    var createdRepItem: ReprocessItem? = null
                    if (updated.returnConfirmed && updated.returnedBags > 0L && input.sendReturnedToReprocess && !alreadyLinked) {
                        val section = resolvedSourcePlant.ifBlank {
                            ready?.skus?.firstOrNull {
                                it.productName.equals(updated.productName, ignoreCase = true)
                            }?.section ?: PlantSection.SAN_ROQUE.label
                        }
                        val repId = "rep_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                        val retKg = if (updated.returnedWeightKg > 0.0) updated.returnedWeightKg else updated.returnedBags * updated.bagWeightKg
                        val candidate = ReprocessItem(
                            id = repId,
                            userId = currentUserId,
                            sourceDispatchId = existing.id,
                            drReference = updated.drReference,
                            customer = updated.customer,
                            productName = updated.productName,
                            section = section,
                            returnedDate = updated.date,
                            returnedBags = updated.returnedBags,
                            bagWeightKg = updated.bagWeightKg,
                            returnedWeightKg = retKg,
                            returnReason = updated.returnReason.ifBlank { "Customer Return" },
                            status = ReprocessStatus.PENDING_REPROCESS.label,
                            fastActionNeeded = true,
                            flaggedToInventory = false
                        )
                        if (repository.addReprocessItem(currentUserId, candidate, fixedDocId = repId).isSuccess) {
                            createdRepItem = candidate
                            logBreadcrumb(
                                actor = BreadcrumbActor.SYSTEM,
                                category = BreadcrumbCategory.REPROCESS,
                                action = "Queued Return for Fast Reprocess (${candidate.productName})",
                                details = "${candidate.returnedBags} bags (${OperationsCalculator.formatKg(candidate.returnedWeightKg)} kg) from DR ${candidate.drReference}",
                                screenTab = "Reprocess",
                                entityRef = candidate.drReference.ifBlank { candidate.productName },
                                severity = BreadcrumbSeverity.WARNING
                            )
                        }
                    }

                    val repCreatedForUndo = createdRepItem
                    recordUndoable(
                        label = "Update ${updated.drReference}",
                        undo = {
                            if (repCreatedForUndo != null) {
                                repository.deleteReprocessItem(currentUserId, repCreatedForUndo.id)
                            }
                            repository.updateDispatch(currentUserId, existing.id, existing, updated, resolvedLinkedOrder)
                        },
                        redo = {
                            if (repCreatedForUndo != null) {
                                repository.addReprocessItem(currentUserId, repCreatedForUndo, fixedDocId = repCreatedForUndo.id)
                            }
                            repository.updateDispatch(currentUserId, existing.id, updated, existing, resolvedLinkedOrder)
                        }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.DISPATCH,
                        action = "Updated Dispatch ${updated.drReference}",
                        details = "${updated.productName} • ${OperationsCalculator.formatKg(updated.effectiveDispatchedWeightKg())} kg net [$statusLabel]",
                        screenTab = "Dispatched",
                        entityRef = updated.drReference,
                        severity = if (statusLabel == DispatchStatus.DELIVERED.label) BreadcrumbSeverity.INFO else BreadcrumbSeverity.WARNING
                    )
                    val reproNote = if (createdRepItem != null) " • Queued in Reprocess (Fast Action)" else ""
                    _snackbarMessage.value = "Updated ${updated.drReference} ($statusLabel)$reproNote."
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to update dispatch: ${it.localizedMessage}"
                }
        }
    }

    fun deleteDispatch(record: DispatchRecord) {
        if (!requireOperationalWrite("delete dispatches")) return
        viewModelScope.launch {
            val ready = opsState.value as? OpsUiState.Ready
            val linkedOrder = ready?.orders?.firstOrNull {
                it.id.isNotBlank() && it.id == record.linkedOrderId
            }
            repository.deleteDispatch(currentUserId, record.id, record, linkedOrder)
                .onSuccess {
                    recordUndoable(
                        label = "Delete ${record.drReference}",
                        undo = { repository.logDispatch(currentUserId, record, linkedOrder, fixedDocId = record.id).map {} },
                        redo = { repository.deleteDispatch(currentUserId, record.id, record, linkedOrder) }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.DISPATCH,
                        action = "Deleted Dispatch ${record.drReference}",
                        details = "Removed ${record.productName} (${record.bagsDispatched} bags) for ${record.customer}",
                        screenTab = "Dispatched",
                        entityRef = record.drReference,
                        severity = BreadcrumbSeverity.WARNING
                    )
                    _snackbarMessage.value = "Deleted dispatch ${record.drReference}."
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to delete dispatch: ${it.localizedMessage}"
                }
        }
    }

    fun addReprocessItem(
        productName: String,
        section: PlantSection,
        returnedDate: String,
        returnedBags: Long,
        bagWeightKg: Double,
        returnedWeightKg: Double,
        returnReason: String,
        customer: String,
        drReference: String,
        sourceDispatchId: String = "",
        fastActionNeeded: Boolean = true,
        actionNotes: String = ""
    ) {
        if (!requireOperationalWrite("add reprocess items")) return
        viewModelScope.launch {
            val item = ReprocessItem(
                userId = currentUserId,
                sourceDispatchId = sourceDispatchId.trim(),
                drReference = drReference.trim(),
                customer = customer.trim(),
                productName = productName.trim(),
                section = section.label,
                returnedDate = returnedDate,
                returnedBags = returnedBags,
                bagWeightKg = bagWeightKg,
                returnedWeightKg = returnedWeightKg,
                returnReason = returnReason.trim().ifBlank { "Customer Return" },
                status = ReprocessStatus.PENDING_REPROCESS.label,
                fastActionNeeded = fastActionNeeded,
                flaggedToInventory = false,
                actionNotes = actionNotes.trim()
            )
            repository.addReprocessItem(currentUserId, item)
                .onSuccess { docId ->
                    val saved = item.copy(id = docId)
                    recordUndoable(
                        label = "Queue Reprocess (${saved.productName})",
                        undo = { repository.deleteReprocessItem(currentUserId, docId) },
                        redo = { repository.addReprocessItem(currentUserId, saved, fixedDocId = docId).map {} }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.REPROCESS,
                        action = "Added Return to Reprocess (${saved.productName})",
                        details = "${saved.returnedBags} bags (${OperationsCalculator.formatKg(saved.returnedWeightKg)} kg) • Reason: ${saved.returnReason}${if (saved.fastActionNeeded) " • FAST ACTION" else ""}",
                        screenTab = "Reprocess",
                        entityRef = saved.drReference.ifBlank { saved.productName },
                        severity = if (saved.fastActionNeeded) BreadcrumbSeverity.WARNING else BreadcrumbSeverity.INFO
                    )
                    _snackbarMessage.value = "${saved.productName} queued in Reprocess${if (saved.fastActionNeeded) " (Fast Action Needed)" else ""}."
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to add to reprocess: ${it.localizedMessage}"
                }
        }
    }

    fun createReprocessFromDispatch(record: DispatchRecord) {
        if (!requireOperationalWrite("queue reprocess items")) return
        val ready = opsState.value as? OpsUiState.Ready
        val existing = ready?.reprocessItems?.firstOrNull {
            it.sourceDispatchId.isNotBlank() && it.sourceDispatchId == record.id
        }
        if (existing != null) {
            _snackbarMessage.value = "DR ${record.drReference} is already in Reprocess (${existing.status})."
            return
        }
        val section = record.sourcePlant
            .takeIf { it.isNotBlank() }
            ?.let { PlantSection.fromRaw(it) }
            ?: ready?.skus
                ?.firstOrNull { it.productName.equals(record.productName, ignoreCase = true) }
                ?.section
                ?.let { PlantSection.fromRaw(it) }
            ?: PlantSection.SAN_ROQUE
        val bags = record.returnedBags.coerceAtLeast(1L)
        val weightKg = if (record.returnedWeightKg > 0.0) {
            record.returnedWeightKg
        } else {
            bags * record.bagWeightKg.coerceAtLeast(1.0)
        }
        addReprocessItem(
            productName = record.productName,
            section = section,
            returnedDate = record.date.ifBlank { OperationsCalculator.todayIsoDate() },
            returnedBags = bags,
            bagWeightKg = record.bagWeightKg.coerceAtLeast(1.0),
            returnedWeightKg = weightKg,
            returnReason = record.returnReason.ifBlank { "Customer Return" },
            customer = record.customer,
            drReference = record.drReference,
            sourceDispatchId = record.id,
            fastActionNeeded = true,
            actionNotes = record.remarks
        )
    }

    fun advanceReprocessStatus(
        item: ReprocessItem,
        newStatus: ReprocessStatus,
        notes: String = item.actionNotes
    ) {
        if (!requireOperationalWrite("update reprocess status")) return
        if (newStatus == ReprocessStatus.FLAGGED_TO_INVENTORY) {
            val bags = if (item.reprocessedBags > 0L) item.reprocessedBags else item.returnedBags
            val kg = if (item.reprocessedWeightKg > 0.0) item.reprocessedWeightKg else item.returnedWeightKg
            val date = item.reprocessedDate.ifBlank { OperationsCalculator.todayIsoDate() }
            flagReprocessToInventory(item, bags, kg, date, notes)
            return
        }
        if (newStatus == ReprocessStatus.DAMAGED) {
            val date = item.reprocessedDate.ifBlank { OperationsCalculator.todayIsoDate() }
            markReprocessAsDamaged(item, date, notes)
            return
        }
        viewModelScope.launch {
            val updated = item.copy(
                status = newStatus.label,
                flaggedToInventory = false,
                fastActionNeeded = item.fastActionNeeded,
                actionNotes = notes.trim()
            )
            repository.updateReprocessItem(currentUserId, item.id, updated)
                .onSuccess {
                    recordUndoable(
                        label = "${item.productName} → ${newStatus.label}",
                        undo = { repository.updateReprocessItem(currentUserId, item.id, item) },
                        redo = { repository.updateReprocessItem(currentUserId, item.id, updated) }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.REPROCESS,
                        action = "Updated Reprocess Status (${item.productName})",
                        details = "${item.status} → ${newStatus.label}${if (item.drReference.isNotBlank()) " (DR ${item.drReference})" else ""}",
                        screenTab = "Reprocess",
                        entityRef = item.drReference.ifBlank { item.productName },
                        severity = BreadcrumbSeverity.INFO
                    )
                    _snackbarMessage.value = "${item.productName} marked as ${newStatus.label}."
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to update reprocess status: ${it.localizedMessage}"
                }
        }
    }

    fun toggleReprocessFastAction(item: ReprocessItem) {
        if (!requireOperationalWrite("flag reprocess priority")) return
        if (item.flaggedToInventory || item.isDamaged()) return
        viewModelScope.launch {
            val updated = item.copy(fastActionNeeded = !item.fastActionNeeded)
            repository.updateReprocessItem(currentUserId, item.id, updated)
                .onSuccess {
                    recordUndoable(
                        label = if (updated.fastActionNeeded) "Flag Fast Action (${item.productName})" else "Clear Fast Action (${item.productName})",
                        undo = { repository.updateReprocessItem(currentUserId, item.id, item) },
                        redo = { repository.updateReprocessItem(currentUserId, item.id, updated) }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.REPROCESS,
                        action = if (updated.fastActionNeeded) "Flagged Fast Action (${item.productName})" else "Cleared Fast Action (${item.productName})",
                        details = "${item.returnedBags} bags (${OperationsCalculator.formatKg(item.returnedWeightKg)} kg)",
                        screenTab = "Reprocess",
                        entityRef = item.drReference.ifBlank { item.productName },
                        severity = if (updated.fastActionNeeded) BreadcrumbSeverity.WARNING else BreadcrumbSeverity.INFO
                    )
                    _snackbarMessage.value = if (updated.fastActionNeeded) {
                        "${item.productName} flagged for Fast Action."
                    } else {
                        "Fast Action alert cleared on ${item.productName}."
                    }
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to update fast action flag: ${it.localizedMessage}"
                }
        }
    }

    fun updateReprocessItem(
        existing: ReprocessItem,
        productName: String,
        section: PlantSection,
        returnedDate: String,
        returnedBags: Long,
        bagWeightKg: Double,
        returnedWeightKg: Double,
        returnReason: String,
        customer: String,
        drReference: String,
        fastActionNeeded: Boolean,
        actionNotes: String
    ) {
        if (!requireOperationalWrite("edit reprocess items")) return
        viewModelScope.launch {
            val updated = existing.copy(
                productName = productName.trim(),
                section = section.label,
                returnedDate = returnedDate.trim(),
                returnedBags = returnedBags,
                bagWeightKg = bagWeightKg,
                returnedWeightKg = returnedWeightKg,
                returnReason = returnReason.trim().ifBlank { "Customer Return" },
                customer = customer.trim(),
                drReference = drReference.trim(),
                fastActionNeeded = if (existing.flaggedToInventory || existing.isDamaged()) false else fastActionNeeded,
                actionNotes = actionNotes.trim()
            )
            repository.updateReprocessItem(currentUserId, existing.id, updated)
                .onSuccess {
                    recordUndoable(
                        label = "Edit Reprocess (${updated.productName})",
                        undo = { repository.updateReprocessItem(currentUserId, existing.id, existing) },
                        redo = { repository.updateReprocessItem(currentUserId, existing.id, updated) }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.REPROCESS,
                        action = "Edited Reprocess Item (${updated.productName})",
                        details = "${updated.returnedBags} bags (${OperationsCalculator.formatKg(updated.returnedWeightKg)} kg) • ${updated.returnReason}",
                        screenTab = "Reprocess",
                        entityRef = updated.drReference.ifBlank { updated.productName },
                        severity = BreadcrumbSeverity.INFO
                    )
                    _snackbarMessage.value = "Updated reprocess entry for ${updated.productName}."
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to update reprocess item: ${it.localizedMessage}"
                }
        }
    }

    fun flagReprocessToInventory(
        item: ReprocessItem,
        reprocessedBags: Long,
        reprocessedWeightKg: Double,
        reprocessedDate: String,
        actionNotes: String = item.actionNotes
    ) {
        if (!requireOperationalWrite("flag reprocess to inventory")) return
        viewModelScope.launch {
            val cleanDate = reprocessedDate.trim().ifBlank { OperationsCalculator.todayIsoDate() }
            val updated = item.copy(
                reprocessedBags = reprocessedBags.coerceIn(1L, 1_000_000L),
                reprocessedWeightKg = reprocessedWeightKg.coerceIn(0.01, 50_000_000.0),
                status = ReprocessStatus.FLAGGED_TO_INVENTORY.label,
                fastActionNeeded = false,
                flaggedToInventory = true,
                reprocessedDate = cleanDate,
                actionNotes = actionNotes.trim()
            )
            repository.flagReprocessToInventory(
                userId = currentUserId,
                existing = item,
                reprocessedBags = updated.reprocessedBags,
                reprocessedWeightKg = updated.reprocessedWeightKg,
                reprocessedDate = cleanDate,
                actionNotes = updated.actionNotes
            )
                .onSuccess {
                    recordUndoable(
                        label = "Moved Reprocessed to Stock (${item.productName})",
                        undo = { repository.updateReprocessItem(currentUserId, item.id, item) },
                        redo = { repository.updateReprocessItem(currentUserId, item.id, updated) }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.REPROCESS,
                        action = "Moved Reprocessed Feed Back to Stock (${item.productName})",
                        details = "+${updated.reprocessedBags} bags (+${OperationsCalculator.formatKg(updated.reprocessedWeightKg)} kg) released back to Finished Goods on $cleanDate",
                        screenTab = "Reprocess",
                        entityRef = item.drReference.ifBlank { item.productName },
                        severity = BreadcrumbSeverity.SUCCESS
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.SYSTEM,
                        category = BreadcrumbCategory.INVENTORY,
                        action = "Inventory Credited from Reprocess (${item.productName})",
                        details = "Flagged REPROCESSED +${OperationsCalculator.formatKg(updated.reprocessedWeightKg)} kg (${updated.reprocessedBags} bags) in Finished Goods",
                        screenTab = "Finished Goods",
                        entityRef = item.productName,
                        severity = BreadcrumbSeverity.SUCCESS
                    )
                    _snackbarMessage.value = "${item.productName} moved back to stock (+${OperationsCalculator.formatKg(updated.reprocessedWeightKg)} kg)."
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to move back to stock: ${it.localizedMessage}"
                }
        }
    }

    fun markReprocessAsDamaged(
        item: ReprocessItem,
        damagedDate: String,
        damageNotes: String = item.actionNotes
    ) {
        if (!requireOperationalWrite("mark reprocess as damaged")) return
        viewModelScope.launch {
            val cleanDate = damagedDate.trim().ifBlank { OperationsCalculator.todayIsoDate() }
            val cleanNotes = damageNotes.trim().ifBlank { "Marked Damaged / Unfit for Stock" }.take(300)
            val updated = item.copy(
                reprocessedBags = 0L,
                reprocessedWeightKg = 0.0,
                status = ReprocessStatus.DAMAGED.label,
                fastActionNeeded = false,
                flaggedToInventory = false,
                reprocessedDate = cleanDate,
                actionNotes = cleanNotes
            )
            repository.markReprocessAsDamaged(
                userId = currentUserId,
                existing = item,
                damagedDate = cleanDate,
                damageNotes = cleanNotes
            )
                .onSuccess {
                    recordUndoable(
                        label = "Mark Damaged (${item.productName})",
                        undo = { repository.updateReprocessItem(currentUserId, item.id, item) },
                        redo = { repository.updateReprocessItem(currentUserId, item.id, updated) }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.REPROCESS,
                        action = "Marked Reprocess Item as Damaged (${item.productName})",
                        details = "-${item.returnedBags} bags (-${OperationsCalculator.formatKg(item.returnedWeightKg)} kg) written off on $cleanDate • $cleanNotes",
                        screenTab = "Reprocess",
                        entityRef = item.drReference.ifBlank { item.productName },
                        severity = BreadcrumbSeverity.ERROR
                    )
                    _snackbarMessage.value = "${item.productName} marked as Damaged (${OperationsCalculator.formatKg(item.returnedWeightKg)} kg write-off)."
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to mark item as damaged: ${it.localizedMessage}"
                }
        }
    }

    fun deleteReprocessItem(item: ReprocessItem) {
        if (!requireOperationalWrite("delete reprocess items")) return
        viewModelScope.launch {
            repository.deleteReprocessItem(currentUserId, item.id)
                .onSuccess {
                    recordUndoable(
                        label = "Delete Reprocess (${item.productName})",
                        undo = { repository.addReprocessItem(currentUserId, item, fixedDocId = item.id).map {} },
                        redo = { repository.deleteReprocessItem(currentUserId, item.id) }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.REPROCESS,
                        action = "Deleted Reprocess Entry (${item.productName})",
                        details = "Removed ${item.returnedBags} bags (${OperationsCalculator.formatKg(item.returnedWeightKg)} kg)",
                        screenTab = "Reprocess",
                        entityRef = item.drReference.ifBlank { item.productName },
                        severity = BreadcrumbSeverity.WARNING
                    )
                    _snackbarMessage.value = "Deleted reprocess entry for ${item.productName}."
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to delete reprocess entry: ${it.localizedMessage}"
                }
        }
    }

    fun addOrder(
        orderCode: String,
        orderDate: String,
        dueDate: String,
        productName: String,
        orderedBags: Long,
        bagWeightKg: Double,
        customer: String,
        priority: Boolean = false,
        rushOrder: Boolean = false
    ) {
        if (!requireOperationalWrite("add customer orders")) return
        viewModelScope.launch {
            val totalWeight = round(orderedBags * bagWeightKg * 100.0) / 100.0
            val order = CustomerOrder(
                userId = currentUserId,
                orderCode = orderCode.trim(),
                orderDate = orderDate,
                dueDate = dueDate,
                productName = productName,
                orderedBags = orderedBags,
                bagWeightKg = bagWeightKg,
                totalWeightKg = totalWeight,
                dispatchedWeightKg = 0.0,
                customer = customer.trim(),
                status = OrderStatus.OPEN.label,
                priority = priority || rushOrder,
                rushOrder = rushOrder
            )
            repository.addOrder(currentUserId, order)
                .onSuccess { orderId ->
                    recordUndoable(
                        label = "Add order ${order.orderCode}",
                        undo = { repository.deleteOrder(currentUserId, orderId) },
                        redo = { repository.addOrder(currentUserId, order, fixedDocId = orderId).map {} }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.ORDERS,
                        action = if (rushOrder) "Created Rush Order ${order.orderCode}" else "Created Order ${order.orderCode}",
                        details = "${order.customer} • ${order.productName} • $orderedBags bags (${OperationsCalculator.formatKg(totalWeight)} kg), due $dueDate",
                        screenTab = "Orders",
                        entityRef = order.orderCode,
                        severity = BreadcrumbSeverity.SUCCESS
                    )
                    _snackbarMessage.value = if (rushOrder) {
                        "Rush order $orderCode added (${OperationsCalculator.formatKg(totalWeight)} kg)."
                    } else {
                        "Order $orderCode added (${OperationsCalculator.formatKg(totalWeight)} kg)."
                    }
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to save order: ${it.localizedMessage}"
                    logBreadcrumb(
                        actor = BreadcrumbActor.SYSTEM,
                        category = BreadcrumbCategory.ORDERS,
                        action = "Order Save Failed",
                        details = "${order.orderCode}: ${it.localizedMessage ?: "Unknown error"}",
                        screenTab = "Orders",
                        entityRef = order.orderCode,
                        severity = BreadcrumbSeverity.ERROR
                    )
                }
        }
    }

    fun addMultipleOrders(
        orderCode: String,
        orderDate: String,
        dueDate: String,
        customer: String,
        items: List<NewOrderItemInput>,
        priority: Boolean = false,
        alsoScheduleProduction: Boolean = false,
        rushOrder: Boolean = false
    ) {
        if (!requireOperationalWrite("add customer orders")) return
        if (items.isEmpty()) return
        viewModelScope.launch {
            val baseCode = orderCode.trim()
            val cleanCustomer = customer.trim()
            val ordersToCreate = items.mapIndexed { idx, item ->
                val itemCode = if (items.size == 1) {
                    baseCode.take(40)
                } else {
                    val suffix = "-${idx + 1}"
                    "${baseCode.take((40 - suffix.length).coerceAtLeast(1))}$suffix"
                }
                val totalWeight = round(item.orderedBags * item.bagWeightKg * 100.0) / 100.0
                CustomerOrder(
                    userId = currentUserId,
                    orderCode = itemCode,
                    orderDate = orderDate,
                    dueDate = dueDate,
                    productName = item.productName.trim(),
                    orderedBags = item.orderedBags,
                    bagWeightKg = item.bagWeightKg,
                    totalWeightKg = totalWeight,
                    dispatchedWeightKg = 0.0,
                    customer = cleanCustomer,
                    status = OrderStatus.OPEN.label,
                    priority = priority || rushOrder,
                    rushOrder = rushOrder
                )
            }
            val grandTotalKg = ordersToCreate.sumOf { it.totalWeightKg }
            repository.addOrdersBatch(currentUserId, ordersToCreate)
                .onSuccess { createdOrders ->
                    val createdOrderIds = createdOrders.map { it.id }
                    if (!alsoScheduleProduction) {
                        recordUndoable(
                            label = if (createdOrders.size == 1) {
                                "Add order ${createdOrders.first().orderCode}"
                            } else {
                                "Add ${createdOrders.size} orders"
                            },
                            undo = { repository.deleteOrdersBatch(currentUserId, createdOrderIds) },
                            redo = { repository.addOrdersBatch(currentUserId, createdOrders, preserveIds = true).map {} }
                        )
                        _snackbarMessage.value = if (createdOrders.size == 1) {
                            "Order ${createdOrders.first().orderCode} added (${OperationsCalculator.formatKg(grandTotalKg)} kg)."
                        } else {
                            "Added ${createdOrders.size} order items under $baseCode (${OperationsCalculator.formatKg(grandTotalKg)} kg)."
                        }
                        logBreadcrumb(
                            actor = BreadcrumbActor.USER,
                            category = BreadcrumbCategory.ORDERS,
                            action = if (createdOrders.size == 1) "Created Order ${createdOrders.first().orderCode}" else "Created ${createdOrders.size} Order Items ($baseCode)",
                            details = "$cleanCustomer • ${OperationsCalculator.formatKg(grandTotalKg)} kg total • due $dueDate",
                            screenTab = "Orders",
                            entityRef = baseCode,
                            severity = BreadcrumbSeverity.SUCCESS
                        )
                    } else {
                        val ready = opsState.value as? OpsUiState.Ready
                        val skuByName = ready?.skus?.associateBy { it.productName }.orEmpty()
                        val summaryByName = ready?.summaries?.associateBy { it.sku.productName }.orEmpty()
                        val runs = createdOrders.map { ord ->
                            val sku = skuByName[ord.productName]
                            val availKg = max(0.0, summaryByName[ord.productName]?.availableStockKg ?: 0.0)
                            val runPriority = if (ord.priority || ord.totalWeightKg > availKg + 0.01) {
                                SchedulePriority.URGENT
                            } else {
                                SchedulePriority.ORDER_DRIVEN
                            }
                            ScheduleRun(
                                userId = currentUserId,
                                scheduledDate = ord.dueDate,
                                section = sku?.section ?: PlantSection.SAN_ROQUE.label,
                                productName = ord.productName,
                                targetBags = ord.orderedBags,
                                bagWeightKg = ord.bagWeightKg,
                                expectedWeightKg = ord.totalWeightKg,
                                lineRemarks = sku?.defaultRemarks?.ifBlank { null } ?: "MIXING/CONVEYOR",
                                priority = runPriority.label,
                                status = ScheduleStatus.SCHEDULED.label,
                                linkedOrderId = ord.id,
                                linkedOrderCode = ord.orderCode,
                                linkedOrderIds = listOf(ord.id),
                                linkedOrderCodes = listOf(ord.orderCode)
                            )
                        }
                        repository.addScheduleRunsBatch(currentUserId, runs)
                            .onSuccess { createdRuns ->
                                val createdRunIds = createdRuns.map { it.id }
                                recordUndoable(
                                    label = "Add ${createdOrders.size} order(s) & ${createdRuns.size} schedule(s)",
                                    undo = {
                                        val sRes = repository.deleteScheduleRunsBatch(currentUserId, createdRunIds)
                                        if (sRes.isFailure) sRes else repository.deleteOrdersBatch(currentUserId, createdOrderIds)
                                    },
                                    redo = {
                                        val oRes = repository.addOrdersBatch(currentUserId, createdOrders, preserveIds = true)
                                        if (oRes.isFailure) oRes.map {} else repository.addScheduleRunsBatch(currentUserId, createdRuns, preserveIds = true).map {}
                                    }
                                )
                                logBreadcrumb(
                                    actor = BreadcrumbActor.USER,
                                    category = BreadcrumbCategory.ORDERS,
                                    action = "Created ${createdOrders.size} Order(s) & Queued Runs ($baseCode)",
                                    details = "$cleanCustomer • ${OperationsCalculator.formatKg(grandTotalKg)} kg total",
                                    screenTab = "Orders",
                                    entityRef = baseCode,
                                    severity = BreadcrumbSeverity.SUCCESS
                                )
                                logBreadcrumb(
                                    actor = BreadcrumbActor.SYSTEM,
                                    category = BreadcrumbCategory.SCHEDULE,
                                    action = "Auto-Queued ${createdRuns.size} Production Run(s)",
                                    details = "Auto-scheduled linked production runs for ${createdOrders.joinToString(", ") { it.orderCode }}",
                                    screenTab = "Schedule",
                                    entityRef = baseCode,
                                    severity = BreadcrumbSeverity.INFO
                                )
                                _snackbarMessage.value =
                                    "Added ${createdOrders.size} order item(s) & queued ${createdRuns.size} linked production run(s)."
                            }
                            .onFailure { schedErr ->
                                recordUndoable(
                                    label = "Add ${createdOrders.size} order(s)",
                                    undo = { repository.deleteOrdersBatch(currentUserId, createdOrderIds) },
                                    redo = { repository.addOrdersBatch(currentUserId, createdOrders, preserveIds = true).map {} }
                                )
                                logBreadcrumb(
                                    actor = BreadcrumbActor.SYSTEM,
                                    category = BreadcrumbCategory.SCHEDULE,
                                    action = "Auto-Scheduling Partial Failure",
                                    details = "Orders $baseCode saved, but schedule queueing failed: ${schedErr.localizedMessage ?: "Unknown error"}",
                                    screenTab = "Orders",
                                    entityRef = baseCode,
                                    severity = BreadcrumbSeverity.WARNING
                                )
                                _snackbarMessage.value =
                                    "Orders saved, but scheduling failed: ${schedErr.localizedMessage}"
                            }
                    }
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to save order items: ${it.localizedMessage}"
                }
        }
    }

    fun cancelOrder(order: CustomerOrder) {
        if (!requireOperationalWrite("cancel orders")) return
        viewModelScope.launch {
            repository.updateOrder(
                userId = currentUserId,
                orderId = order.id,
                dueDate = order.dueDate,
                orderedBags = order.orderedBags,
                bagWeightKg = order.bagWeightKg,
                totalWeightKg = order.totalWeightKg,
                customer = order.customer,
                status = OrderStatus.CANCELLED.label,
                priority = order.priority,
                rushOrder = order.rushOrder
            ).onSuccess {
                recordUndoable(
                    label = "Cancel order ${order.orderCode}",
                    undo = { repository.recreateOrder(currentUserId, order) },
                    redo = {
                        repository.updateOrder(
                            userId = currentUserId,
                            orderId = order.id,
                            dueDate = order.dueDate,
                            orderedBags = order.orderedBags,
                            bagWeightKg = order.bagWeightKg,
                            totalWeightKg = order.totalWeightKg,
                            customer = order.customer,
                            status = OrderStatus.CANCELLED.label,
                            priority = order.priority,
                            rushOrder = order.rushOrder
                        )
                    }
                )
                _snackbarMessage.value = "Order ${order.orderCode} cancelled."
                logBreadcrumb(
                    actor = BreadcrumbActor.USER,
                    category = BreadcrumbCategory.ORDERS,
                    action = "Cancelled Order ${order.orderCode}",
                    details = "${order.customer} • ${order.productName} (${order.orderedBags} bags)",
                    screenTab = "Orders",
                    entityRef = order.orderCode,
                    severity = BreadcrumbSeverity.WARNING
                )
            }.onFailure {
                _snackbarMessage.value = "Failed to cancel order: ${it.localizedMessage}"
            }
        }
    }

    fun retrieveCancelledOrder(order: CustomerOrder) {
        if (!requireOperationalWrite("restore cancelled orders")) return
        if (order.status != OrderStatus.CANCELLED.label) return
        viewModelScope.launch {
            val restoredStatus = when {
                order.dispatchedWeightKg >= order.totalWeightKg - 0.01 -> OrderStatus.FULFILLED.label
                order.dispatchedWeightKg > 0.0 -> OrderStatus.PARTIALLY_DISPATCHED.label
                else -> OrderStatus.OPEN.label
            }
            val restoredOrder = order.copy(status = restoredStatus)
            repository.recreateOrder(currentUserId, restoredOrder)
                .onSuccess {
                    recordUndoable(
                        label = "Retrieve order ${order.orderCode}",
                        undo = { repository.recreateOrder(currentUserId, order) },
                        redo = { repository.recreateOrder(currentUserId, restoredOrder) }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.ORDERS,
                        action = "Restored Order ${order.orderCode}",
                        details = "Retrieved from Cancelled → $restoredStatus (${order.customer})",
                        screenTab = "Orders",
                        entityRef = order.orderCode,
                        severity = BreadcrumbSeverity.SUCCESS
                    )
                    _snackbarMessage.value = "Retrieved order ${order.orderCode} as $restoredStatus."
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to retrieve order: ${it.localizedMessage}"
                }
        }
    }

    fun deleteOrder(order: CustomerOrder) {
        if (!requireOperationalWrite("delete orders")) return
        viewModelScope.launch {
            repository.deleteOrder(currentUserId, order.id)
                .onSuccess {
                    recordUndoable(
                        label = "Delete order ${order.orderCode}",
                        undo = { repository.addOrder(currentUserId, order, fixedDocId = order.id).map {} },
                        redo = { repository.deleteOrder(currentUserId, order.id) }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.ORDERS,
                        action = "Deleted Order ${order.orderCode}",
                        details = "Removed ${order.customer} • ${order.productName} (${order.orderedBags} bags)",
                        screenTab = "Orders",
                        entityRef = order.orderCode,
                        severity = BreadcrumbSeverity.WARNING
                    )
                    _snackbarMessage.value = "Deleted order ${order.orderCode}."
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to delete order: ${it.localizedMessage}"
                }
        }
    }

    fun updateOrder(
        order: CustomerOrder,
        dueDate: String,
        orderedBags: Long,
        bagWeightKg: Double,
        customer: String,
        priority: Boolean,
        rushOrder: Boolean = order.rushOrder
    ) {
        if (!requireOperationalWrite("edit orders")) return
        viewModelScope.launch {
            val totalWeight = round(orderedBags * bagWeightKg * 100.0) / 100.0
            val cleanCustomer = customer.trim()
            val status = if (order.dispatchedWeightKg >= totalWeight - 0.01) {
                OrderStatus.FULFILLED.label
            } else if (order.dispatchedWeightKg > 0.0) {
                OrderStatus.PARTIALLY_DISPATCHED.label
            } else {
                OrderStatus.OPEN.label
            }
            repository.updateOrder(
                userId = currentUserId,
                orderId = order.id,
                dueDate = dueDate,
                orderedBags = orderedBags,
                bagWeightKg = bagWeightKg,
                totalWeightKg = totalWeight,
                customer = cleanCustomer,
                status = status,
                priority = priority,
                rushOrder = rushOrder
            ).onSuccess {
                recordUndoable(
                    label = "Edit order ${order.orderCode}",
                    undo = {
                        if (status == OrderStatus.FULFILLED.label || status == OrderStatus.CANCELLED.label) {
                            repository.recreateOrder(currentUserId, order)
                        } else {
                            repository.updateOrder(
                                userId = currentUserId,
                                orderId = order.id,
                                dueDate = order.dueDate,
                                orderedBags = order.orderedBags,
                                bagWeightKg = order.bagWeightKg,
                                totalWeightKg = order.totalWeightKg,
                                customer = order.customer,
                                status = order.status,
                                priority = order.priority,
                                rushOrder = order.rushOrder
                            )
                        }
                    },
                    redo = {
                        repository.updateOrder(
                            userId = currentUserId,
                            orderId = order.id,
                            dueDate = dueDate,
                            orderedBags = orderedBags,
                            bagWeightKg = bagWeightKg,
                            totalWeightKg = totalWeight,
                            customer = cleanCustomer,
                            status = status,
                            priority = priority,
                            rushOrder = rushOrder
                        )
                    }
                )
                logBreadcrumb(
                    actor = BreadcrumbActor.USER,
                    category = BreadcrumbCategory.ORDERS,
                    action = "Updated Order ${order.orderCode}",
                    details = "$cleanCustomer • ${order.productName} • $orderedBags bags • Due $dueDate${if (priority) " [PRIORITY]" else ""}${if (rushOrder) " [RUSH]" else ""}",
                    screenTab = "Orders",
                    entityRef = order.orderCode,
                    severity = BreadcrumbSeverity.INFO
                )
                _snackbarMessage.value = "Order ${order.orderCode} updated."
            }.onFailure {
                _snackbarMessage.value = "Failed to update order: ${it.localizedMessage}"
            }
        }
    }

    fun scheduleProductionRun(
        scheduledDate: String,
        section: PlantSection,
        productName: String,
        targetBags: Long,
        bagWeightKg: Double,
        lineRemarks: String,
        priority: SchedulePriority,
        linkedOrderId: String = "",
        linkedOrderCode: String = "",
        linkedOrderIds: List<String> = emptyList(),
        linkedOrderCodes: List<String> = emptyList()
    ) {
        if (!requireOperationalWrite("schedule production runs")) return
        viewModelScope.launch {
            val expectedWeight = round(targetBags * bagWeightKg * 100.0) / 100.0
            val allIds = (linkedOrderIds + linkedOrderId).map { it.trim() }.filter { it.isNotBlank() }.distinct()
            val allCodes = (linkedOrderCodes + linkedOrderCode).map { it.trim() }.filter { it.isNotBlank() }.distinct()
            val run = ScheduleRun(
                userId = currentUserId,
                scheduledDate = scheduledDate,
                section = section.label,
                productName = productName,
                targetBags = targetBags,
                bagWeightKg = bagWeightKg,
                expectedWeightKg = expectedWeight,
                lineRemarks = lineRemarks,
                priority = priority.label,
                status = ScheduleStatus.SCHEDULED.label,
                linkedOrderId = allIds.firstOrNull().orEmpty(),
                linkedOrderCode = allCodes.firstOrNull().orEmpty(),
                linkedOrderIds = allIds,
                linkedOrderCodes = allCodes
            )
            repository.addScheduleRun(currentUserId, run)
                .onSuccess { runId ->
                    recordUndoable(
                        label = "Schedule $productName",
                        undo = { repository.deleteScheduleRun(currentUserId, runId) },
                        redo = { repository.addScheduleRun(currentUserId, run, fixedDocId = runId).map {} }
                    )
                    val linkNote = when {
                        allCodes.size > 1 -> " for ${allCodes.size} orders"
                        allCodes.size == 1 -> " for ${allCodes.first()}"
                        else -> ""
                    }
                    _snackbarMessage.value = "Scheduled $targetBags bags of $productName$linkNote on $lineRemarks."
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.SCHEDULE,
                        action = "Scheduled Run: $productName",
                        details = "$targetBags bags on $scheduledDate • ${section.label} • $lineRemarks (${priority.label})$linkNote",
                        screenTab = "Schedule",
                        entityRef = allCodes.firstOrNull() ?: productName,
                        severity = BreadcrumbSeverity.SUCCESS
                    )
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to schedule run: ${it.localizedMessage}"
                }
        }
    }

    fun scheduleMultipleProductionRuns(
        scheduledDate: String,
        priority: SchedulePriority,
        items: List<NewScheduleItemInput>
    ) {
        if (!requireOperationalWrite("schedule production runs")) return
        if (items.isEmpty()) return
        viewModelScope.launch {
            val runs = items.map { item ->
                val expectedWeight = round(item.targetBags * item.bagWeightKg * 100.0) / 100.0
                val allIds = (item.linkedOrderIds + item.linkedOrderId).map { it.trim() }.filter { it.isNotBlank() }.distinct()
                val allCodes = (item.linkedOrderCodes + item.linkedOrderCode).map { it.trim() }.filter { it.isNotBlank() }.distinct()
                ScheduleRun(
                    userId = currentUserId,
                    scheduledDate = scheduledDate,
                    section = item.section.label,
                    productName = item.productName,
                    targetBags = item.targetBags,
                    bagWeightKg = item.bagWeightKg,
                    expectedWeightKg = expectedWeight,
                    lineRemarks = item.lineRemarks,
                    priority = priority.label,
                    status = ScheduleStatus.SCHEDULED.label,
                    linkedOrderId = allIds.firstOrNull().orEmpty(),
                    linkedOrderCode = allCodes.firstOrNull().orEmpty(),
                    linkedOrderIds = allIds,
                    linkedOrderCodes = allCodes
                )
            }
            val totalBags = runs.sumOf { it.targetBags }
            repository.addScheduleRunsBatch(currentUserId, runs)
                .onSuccess { createdRuns ->
                    val createdIds = createdRuns.map { it.id }
                    recordUndoable(
                        label = if (createdRuns.size == 1) {
                            "Schedule ${createdRuns.first().productName}"
                        } else {
                            "Schedule ${createdRuns.size} runs"
                        },
                        undo = { repository.deleteScheduleRunsBatch(currentUserId, createdIds) },
                        redo = { repository.addScheduleRunsBatch(currentUserId, createdRuns, preserveIds = true).map {} }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.SCHEDULE,
                        action = if (createdRuns.size == 1) "Scheduled Run: ${createdRuns.first().productName}" else "Scheduled ${createdRuns.size} Production Runs",
                        details = "$totalBags total bags for $scheduledDate (${priority.label})",
                        screenTab = "Schedule",
                        entityRef = createdRuns.first().productName,
                        severity = BreadcrumbSeverity.SUCCESS
                    )
                    _snackbarMessage.value = if (createdRuns.size == 1) {
                        val first = createdRuns.first()
                        val codes = first.allLinkedOrderCodes()
                        val linkNote = when {
                            codes.size > 1 -> " for ${codes.size} orders"
                            codes.size == 1 -> " for ${codes.first()}"
                            else -> ""
                        }
                        "Scheduled ${first.targetBags} bags of ${first.productName}$linkNote on ${first.lineRemarks}."
                    } else {
                        "Scheduled ${createdRuns.size} production runs ($totalBags total bags) for $scheduledDate."
                    }
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to schedule runs: ${it.localizedMessage}"
                }
        }
    }

    fun advanceScheduleStatus(run: ScheduleRun, targetStatus: ScheduleStatus) {
        if (!requireOperationalWrite("update production schedules")) return
        viewModelScope.launch {
            repository.updateScheduleStatus(currentUserId, run.id, targetStatus)
                .onSuccess {
                    val prevStatus = ScheduleStatus.fromRaw(run.status)
                    val isTerminalTarget = targetStatus == ScheduleStatus.COMPLETED || targetStatus == ScheduleStatus.CANCELLED
                    recordUndoable(
                        label = "${run.productName} → ${targetStatus.label}",
                        undo = {
                            if (isTerminalTarget) {
                                repository.recreateScheduleRun(currentUserId, run)
                            } else {
                                repository.updateScheduleStatus(currentUserId, run.id, prevStatus)
                            }
                        },
                        redo = { repository.updateScheduleStatus(currentUserId, run.id, targetStatus) }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.SCHEDULE,
                        action = "Schedule ${targetStatus.label}: ${run.productName}",
                        details = "${run.targetBags} bags (${run.scheduledDate}) transitioned ${prevStatus.label} → ${targetStatus.label}",
                        screenTab = "Schedule",
                        entityRef = run.allLinkedOrderCodes().firstOrNull() ?: run.productName,
                        severity = if (targetStatus == ScheduleStatus.CANCELLED) BreadcrumbSeverity.WARNING else BreadcrumbSeverity.INFO
                    )
                    _snackbarMessage.value = "${run.productName} moved to ${targetStatus.label}."
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to update status: ${it.localizedMessage}"
                }
        }
    }

    fun startMultipleScheduleRuns(runs: List<ScheduleRun>) {
        if (!requireOperationalWrite("start production schedules")) return
        val validRuns = runs.filter { it.id.isNotBlank() && it.status == ScheduleStatus.SCHEDULED.label }
        if (validRuns.isEmpty()) return
        val ids = validRuns.map { it.id }.distinct()
        viewModelScope.launch {
            repository.updateScheduleStatusesBatch(currentUserId, ids, ScheduleStatus.IN_PROGRESS)
                .onSuccess {
                    recordUndoable(
                        label = if (validRuns.size == 1) {
                            "${validRuns.first().productName} → In Progress"
                        } else {
                            "Start ${validRuns.size} runs"
                        },
                        undo = {
                            repository.updateScheduleStatusesBatch(currentUserId, ids, ScheduleStatus.SCHEDULED)
                        },
                        redo = {
                            repository.updateScheduleStatusesBatch(currentUserId, ids, ScheduleStatus.IN_PROGRESS)
                        }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.SCHEDULE,
                        action = if (validRuns.size == 1) "Started Run: ${validRuns.first().productName}" else "Started ${validRuns.size} Scheduled Runs",
                        details = "Moved to In Progress: ${validRuns.joinToString(", ") { it.productName }}",
                        screenTab = "Schedule",
                        entityRef = validRuns.first().productName,
                        severity = BreadcrumbSeverity.INFO
                    )
                    _snackbarMessage.value = if (validRuns.size == 1) {
                        "${validRuns.first().productName} moved to In Progress."
                    } else {
                        "Started ${validRuns.size} scheduled runs — moved to Active."
                    }
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to start runs: ${it.localizedMessage}"
                }
        }
    }

    fun updateScheduleRun(
        run: ScheduleRun,
        scheduledDate: String,
        targetBags: Long,
        bagWeightKg: Double,
        lineRemarks: String,
        priority: SchedulePriority,
        linkedOrders: List<CustomerOrder>? = null
    ) {
        if (!requireOperationalWrite("edit production schedules")) return
        viewModelScope.launch {
            val expectedWeight = round(targetBags * bagWeightKg * 100.0) / 100.0
            val ids = linkedOrders?.map { it.id }?.filter { it.isNotBlank() }?.distinct()
                ?: run.allLinkedOrderIds()
            val codes = linkedOrders?.map { it.orderCode }?.filter { it.isNotBlank() }?.distinct()
                ?: run.allLinkedOrderCodes()
            repository.updateScheduleRun(
                userId = currentUserId,
                scheduleId = run.id,
                scheduledDate = scheduledDate,
                targetBags = targetBags,
                bagWeightKg = bagWeightKg,
                expectedWeightKg = expectedWeight,
                lineRemarks = lineRemarks,
                priority = priority.label,
                status = run.status,
                linkedOrderId = ids.firstOrNull().orEmpty(),
                linkedOrderCode = codes.firstOrNull().orEmpty(),
                linkedOrderIds = ids,
                linkedOrderCodes = codes
            ).onSuccess {
                recordUndoable(
                    label = "Edit schedule ${run.productName}",
                    undo = {
                        repository.updateScheduleRun(
                            userId = currentUserId,
                            scheduleId = run.id,
                            scheduledDate = run.scheduledDate,
                            targetBags = run.targetBags,
                            bagWeightKg = run.bagWeightKg,
                            expectedWeightKg = run.expectedWeightKg,
                            lineRemarks = run.lineRemarks,
                            priority = run.priority,
                            status = run.status,
                            linkedOrderId = run.linkedOrderId,
                            linkedOrderCode = run.linkedOrderCode,
                            linkedOrderIds = run.allLinkedOrderIds(),
                            linkedOrderCodes = run.allLinkedOrderCodes()
                        )
                    },
                    redo = {
                        repository.updateScheduleRun(
                            userId = currentUserId,
                            scheduleId = run.id,
                            scheduledDate = scheduledDate,
                            targetBags = targetBags,
                            bagWeightKg = bagWeightKg,
                            expectedWeightKg = expectedWeight,
                            lineRemarks = lineRemarks,
                            priority = priority.label,
                            status = run.status,
                            linkedOrderId = ids.firstOrNull().orEmpty(),
                            linkedOrderCode = codes.firstOrNull().orEmpty(),
                            linkedOrderIds = ids,
                            linkedOrderCodes = codes
                        )
                    }
                )
                val linkSuffix = if (codes.isNotEmpty()) " • ${codes.size} linked order(s)" else ""
                logBreadcrumb(
                    actor = BreadcrumbActor.USER,
                    category = BreadcrumbCategory.SCHEDULE,
                    action = "Updated Schedule: ${run.productName}",
                    details = "$targetBags bags on $scheduledDate • $lineRemarks (${priority.label})$linkSuffix",
                    screenTab = "Schedule",
                    entityRef = codes.firstOrNull() ?: run.productName,
                    severity = BreadcrumbSeverity.INFO
                )
                _snackbarMessage.value = "Updated schedule for ${run.productName} ($targetBags bags on $scheduledDate)$linkSuffix."
            }.onFailure {
                _snackbarMessage.value = "Failed to update schedule: ${it.localizedMessage}"
            }
        }
    }

    fun deleteScheduleRun(run: ScheduleRun) {
        if (!requireOperationalWrite("delete production schedules")) return
        viewModelScope.launch {
            repository.deleteScheduleRun(currentUserId, run.id)
                .onSuccess {
                    recordUndoable(
                        label = "Delete schedule ${run.productName}",
                        undo = { repository.addScheduleRun(currentUserId, run, fixedDocId = run.id).map {} },
                        redo = { repository.deleteScheduleRun(currentUserId, run.id) }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.SCHEDULE,
                        action = "Deleted Schedule: ${run.productName}",
                        details = "Removed ${run.targetBags} bags run for ${run.scheduledDate}",
                        screenTab = "Schedule",
                        entityRef = run.productName,
                        severity = BreadcrumbSeverity.WARNING
                    )
                    _snackbarMessage.value = "Deleted schedule run for ${run.productName}."
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to delete schedule run: ${it.localizedMessage}"
                }
        }
    }

    fun completeScheduleWithBatch(
        run: ScheduleRun,
        date: String,
        bagsProduced: Long,
        bagWeightKg: Double,
        actualRecoveredKg: Double,
        remarks: String,
        linkedOrdersOverride: List<CustomerOrder>? = null
    ) {
        if (!requireOperationalWrite("complete production batches")) return
        viewModelScope.launch {
            val expectedKg = round(bagsProduced * bagWeightKg * 100.0) / 100.0
            val varianceKg = round((actualRecoveredKg - expectedKg) * 100.0) / 100.0
            val recoveryPct = if (expectedKg > 0.0) round((actualRecoveredKg / expectedKg) * 10000.0) / 100.0 else 100.0
            val readyOrders = (opsState.value as? OpsUiState.Ready)?.orders.orEmpty()
            val runIds = run.allLinkedOrderIds()
            val runCodes = run.allLinkedOrderCodes()
            val resolvedOrders = linkedOrdersOverride ?: run {
                val idSet = runIds.toSet()
                val codeSet = runCodes.toSet()
                readyOrders.filter { it.id in idSet || it.orderCode in codeSet }
            }
            val finalIds = if (linkedOrdersOverride != null) {
                resolvedOrders.map { it.id }.filter { it.isNotBlank() }.distinct()
            } else {
                (runIds + resolvedOrders.map { it.id }).filter { it.isNotBlank() }.distinct()
            }
            val finalCodes = if (linkedOrdersOverride != null) {
                resolvedOrders.map { it.orderCode }.filter { it.isNotBlank() }.distinct()
            } else {
                (runCodes + resolvedOrders.map { it.orderCode }).filter { it.isNotBlank() }.distinct()
            }
            val finalCustomers = resolvedOrders.map { it.customer.trim() }.filter { it.isNotBlank() }.distinct()

            val batch = ProductionBatch(
                userId = currentUserId,
                date = date,
                section = run.section,
                productName = run.productName,
                bagsProduced = bagsProduced,
                bagWeightKg = bagWeightKg,
                expectedWeightKg = expectedKg,
                actualRecoveredKg = actualRecoveredKg,
                varianceKg = varianceKg,
                recoveryPct = recoveryPct,
                remarks = remarks,
                scheduleRunId = run.id,
                linkedOrderIds = finalIds,
                linkedOrderCodes = finalCodes,
                scheduledForCustomers = finalCustomers
            )
            repository.completeScheduleAndLogBatch(currentUserId, run.id, batch)
                .onSuccess { batchId ->
                    recordUndoable(
                        label = "Complete ${run.productName}",
                        undo = { repository.undoCompleteScheduleAndLogBatch(currentUserId, batchId, run) },
                        redo = { repository.completeScheduleAndLogBatch(currentUserId, run.id, batch, fixedBatchId = batchId).map {} }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.RECOVERY,
                        action = "Completed Run & Logged Recovery: ${run.productName}",
                        details = "$bagsProduced bags • ${OperationsCalculator.formatKg(actualRecoveredKg)} kg (${String.format("%.2f%%", recoveryPct)})",
                        screenTab = "Schedule",
                        entityRef = finalCodes.firstOrNull() ?: run.productName,
                        severity = if (recoveryPct < 98.5) BreadcrumbSeverity.WARNING else BreadcrumbSeverity.SUCCESS
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.SYSTEM,
                        category = BreadcrumbCategory.SCHEDULE,
                        action = "Run Auto-Completed (${run.productName})",
                        details = "Atomic transition → Completed & inventory credited (${OperationsCalculator.formatKg(actualRecoveredKg)} kg)",
                        screenTab = "Schedule",
                        entityRef = run.productName,
                        severity = BreadcrumbSeverity.INFO
                    )
                    _snackbarMessage.value = "Run completed & recovery logged (${String.format("%.2f%%", recoveryPct)})."
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to complete run: ${it.localizedMessage}"
                }
        }
    }

    fun logProductionBatch(
        date: String,
        section: PlantSection,
        productName: String,
        bagsProduced: Long,
        bagWeightKg: Double,
        actualRecoveredKg: Double,
        remarks: String,
        linkedOrders: List<CustomerOrder> = emptyList()
    ) {
        if (!requireOperationalWrite("log production batches")) return
        viewModelScope.launch {
            val expectedKg = round(bagsProduced * bagWeightKg * 100.0) / 100.0
            val varianceKg = round((actualRecoveredKg - expectedKg) * 100.0) / 100.0
            val recoveryPct = if (expectedKg > 0.0) round((actualRecoveredKg / expectedKg) * 10000.0) / 100.0 else 100.0
            val batch = ProductionBatch(
                userId = currentUserId,
                date = date,
                section = section.label,
                productName = productName,
                bagsProduced = bagsProduced,
                bagWeightKg = bagWeightKg,
                expectedWeightKg = expectedKg,
                actualRecoveredKg = actualRecoveredKg,
                varianceKg = varianceKg,
                recoveryPct = recoveryPct,
                remarks = remarks,
                linkedOrderIds = linkedOrders.map { it.id }.filter { it.isNotBlank() }.distinct(),
                linkedOrderCodes = linkedOrders.map { it.orderCode }.filter { it.isNotBlank() }.distinct(),
                scheduledForCustomers = linkedOrders.map { it.customer.trim() }.filter { it.isNotBlank() }.distinct()
            )
            repository.addProductionBatch(currentUserId, batch)
                .onSuccess { batchId ->
                    recordUndoable(
                        label = "Log batch $productName",
                        undo = { repository.deleteProductionBatch(currentUserId, batchId) },
                        redo = { repository.addProductionBatch(currentUserId, batch, fixedDocId = batchId).map {} }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.RECOVERY,
                        action = "Logged Production Batch: $productName",
                        details = "$bagsProduced bags • ${OperationsCalculator.formatKg(actualRecoveredKg)} kg (${String.format("%.2f%%", recoveryPct)}) on $date",
                        screenTab = "Recovery",
                        entityRef = productName,
                        severity = if (recoveryPct < 98.5) BreadcrumbSeverity.WARNING else BreadcrumbSeverity.SUCCESS
                    )
                    if (recoveryPct < 98.5) {
                        logBreadcrumb(
                            actor = BreadcrumbActor.SYSTEM,
                            category = BreadcrumbCategory.RECOVERY,
                            action = "Low Recovery Alert: $productName",
                            details = "Recovery ${String.format("%.2f%%", recoveryPct)} is below 98.50% plant target (variance ${OperationsCalculator.formatKg(varianceKg)} kg)",
                            screenTab = "Recovery",
                            entityRef = productName,
                            severity = BreadcrumbSeverity.WARNING
                        )
                    }
                    _snackbarMessage.value = "Logged $bagsProduced bags of $productName (${String.format("%.2f%%", recoveryPct)} recovery)."
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to log batch: ${it.localizedMessage}"
                }
        }
    }

    fun updateProductionBatch(
        existingBatch: ProductionBatch,
        date: String,
        section: PlantSection,
        productName: String,
        bagsProduced: Long,
        bagWeightKg: Double,
        actualRecoveredKg: Double,
        remarks: String,
        linkedOrders: List<CustomerOrder>? = null
    ) {
        if (!requireOperationalWrite("edit production batches")) return
        viewModelScope.launch {
            val expectedKg = round(bagsProduced * bagWeightKg * 100.0) / 100.0
            val varianceKg = round((actualRecoveredKg - expectedKg) * 100.0) / 100.0
            val recoveryPct = if (expectedKg > 0.0) round((actualRecoveredKg / expectedKg) * 10000.0) / 100.0 else 100.0
            val updated = existingBatch.copy(
                date = date,
                section = section.label,
                productName = productName,
                bagsProduced = bagsProduced,
                bagWeightKg = bagWeightKg,
                expectedWeightKg = expectedKg,
                actualRecoveredKg = actualRecoveredKg,
                varianceKg = varianceKg,
                recoveryPct = recoveryPct,
                remarks = remarks,
                linkedOrderIds = linkedOrders?.map { it.id }?.filter { it.isNotBlank() }?.distinct()
                    ?: existingBatch.allLinkedOrderIds(),
                linkedOrderCodes = linkedOrders?.map { it.orderCode }?.filter { it.isNotBlank() }?.distinct()
                    ?: existingBatch.allLinkedOrderCodes(),
                scheduledForCustomers = linkedOrders?.map { it.customer.trim() }?.filter { it.isNotBlank() }?.distinct()
                    ?: existingBatch.allScheduledForCustomers()
            )
            repository.updateProductionBatch(currentUserId, existingBatch.id, updated)
                .onSuccess {
                    recordUndoable(
                        label = "Edit recovery $productName",
                        undo = { repository.updateProductionBatch(currentUserId, existingBatch.id, existingBatch) },
                        redo = { repository.updateProductionBatch(currentUserId, existingBatch.id, updated) }
                    )
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.RECOVERY,
                        action = "Updated Batch: $productName",
                        details = "$bagsProduced bags • ${OperationsCalculator.formatKg(actualRecoveredKg)} kg (${String.format("%.2f%%", recoveryPct)})",
                        screenTab = "Recovery",
                        entityRef = productName,
                        severity = if (recoveryPct < 98.5) BreadcrumbSeverity.WARNING else BreadcrumbSeverity.INFO
                    )
                    _snackbarMessage.value = "Updated recovered batch: $bagsProduced bags of $productName (${OperationsCalculator.formatKg(actualRecoveredKg)} kg)."
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to update batch: ${it.localizedMessage}"
                }
        }
    }

    fun updateBatchLinkedOrders(batch: ProductionBatch, linkedOrders: List<CustomerOrder>) {
        if (!requireOperationalWrite("update batch linked orders")) return
        updateProductionBatch(
            existingBatch = batch,
            date = batch.date,
            section = PlantSection.fromRaw(batch.section),
            productName = batch.productName,
            bagsProduced = batch.bagsProduced,
            bagWeightKg = batch.bagWeightKg,
            actualRecoveredKg = batch.actualRecoveredKg,
            remarks = batch.remarks,
            linkedOrders = linkedOrders
        )
    }

    fun importBulkPasteRows(rawText: String, defaultSection: PlantSection) {
        if (!requireOperationalWrite("import production batches")) return
        viewModelScope.launch {
            val parsed = parseBulkBatchRows(rawText, defaultSection)
            if (parsed.isEmpty()) {
                _snackbarMessage.value = "No valid rows found. Format: Date, Section, Product, Bags, BagWt, ActualKg, Remarks"
                logBreadcrumb(
                    actor = BreadcrumbActor.SYSTEM,
                    category = BreadcrumbCategory.RECOVERY,
                    action = "Bulk Batch Import Validation Failed",
                    details = "Submitted bulk paste text contained 0 valid production rows",
                    screenTab = "Recovery",
                    entityRef = defaultSection.label,
                    severity = BreadcrumbSeverity.WARNING
                )
                return@launch
            }
            repository.addProductionBatchesBulk(currentUserId, parsed)
                .onSuccess { count ->
                    logBreadcrumb(
                        actor = BreadcrumbActor.USER,
                        category = BreadcrumbCategory.RECOVERY,
                        action = "Bulk Imported $count Batch Row(s)",
                        details = "Default section ${defaultSection.label} • Imported $count rows",
                        screenTab = "Recovery",
                        entityRef = defaultSection.label,
                        severity = BreadcrumbSeverity.SUCCESS
                    )
                    _snackbarMessage.value = "Imported $count production batch rows."
                }
                .onFailure {
                    _snackbarMessage.value = "Bulk import failed: ${it.localizedMessage}"
                    logBreadcrumb(
                        actor = BreadcrumbActor.SYSTEM,
                        category = BreadcrumbCategory.RECOVERY,
                        action = "Bulk Batch Import Failed",
                        details = it.localizedMessage ?: "Firestore error",
                        screenTab = "Recovery",
                        severity = BreadcrumbSeverity.ERROR
                    )
                }
        }
    }

    private fun parseBulkBatchRows(rawText: String, defaultSection: PlantSection): List<ProductionBatch> {
        val result = mutableListOf<ProductionBatch>()
        rawText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                val delimiter = if (line.contains('\t')) '\t' else ','
                val cols = line.split(delimiter).map { it.trim().trim('"') }
                if (cols.size >= 5) {
                    // Support either: Date, Section, Product, Bags, BagWt, ActualKg, Remarks
                    // Or: Date, Product, Bags, BagWt, ActualKg, Remarks
                    val hasSection = cols.size >= 6 && PlantSection.entries.any {
                        it.label.equals(cols[1], true) || it.name.equals(cols[1], true)
                    }
                    val date = cols[0].takeIf { Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(it) } ?: "2026-11-19"
                    val section = if (hasSection) PlantSection.fromRaw(cols[1]).label else defaultSection.label
                    val prodIdx = if (hasSection) 2 else 1
                    val productName = cols[prodIdx]
                    val bags = cols.getOrNull(prodIdx + 1)?.toLongOrNull() ?: 0L
                    val bagWt = cols.getOrNull(prodIdx + 2)?.toDoubleOrNull() ?: 0.0
                    val actualKg = cols.getOrNull(prodIdx + 3)?.toDoubleOrNull() ?: (bags * bagWt)
                    val remarks = cols.getOrNull(prodIdx + 4)?.ifBlank { "MIXING/CONVEYOR" } ?: "MIXING/CONVEYOR"

                    if (productName.isNotBlank() && bags > 0 && bagWt > 0.0 && actualKg > 0.0) {
                        val expectedKg = round(bags * bagWt * 100.0) / 100.0
                        val varianceKg = round((actualKg - expectedKg) * 100.0) / 100.0
                        val recoveryPct = round((actualKg / expectedKg) * 10000.0) / 100.0
                        result.add(
                            ProductionBatch(
                                userId = currentUserId,
                                date = date,
                                section = section,
                                productName = productName,
                                bagsProduced = bags,
                                bagWeightKg = bagWt,
                                expectedWeightKg = expectedKg,
                                actualRecoveredKg = actualKg,
                                varianceKg = varianceKg,
                                recoveryPct = recoveryPct,
                                remarks = remarks
                            )
                        )
                    }
                }
            }
        return result
    }

    fun buildReport(
        startDate: String,
        endDate: String,
        sectionFilter: String,
        includedSections: Set<ReportSectionOption> = ReportSectionOption.entries.toSet()
    ): PlantReportData? {
        val state = opsState.value as? OpsUiState.Ready ?: return null
        return OperationsCalculator.buildReport(
            startDate = startDate,
            endDate = endDate,
            sectionFilter = sectionFilter,
            skus = state.skus,
            batches = state.batches,
            dispatches = state.dispatches,
            orders = state.orders,
            summaries = state.summaries,
            schedules = state.schedules,
            includedSections = includedSections
        )
    }

    fun exportJsonBackup(): String? {
        val state = opsState.value as? OpsUiState.Ready ?: return null
        val json = ReportExporter.buildJsonBackup(
            skus = state.skus,
            batches = state.batches,
            dispatches = state.dispatches,
            orders = state.orders,
            schedules = state.schedules,
            customers = state.customers,
            rawMaterials = state.rawMaterials,
            boms = state.boms,
            reprocessItems = state.reprocessItems
        )
        logBreadcrumb(
            actor = BreadcrumbActor.USER,
            category = BreadcrumbCategory.REPORTS,
            action = "Generated JSON Workspace Backup",
            details = "${state.skus.size} SKUs, ${state.orders.size} orders, ${state.dispatches.size} dispatches",
            screenTab = "Reports",
            entityRef = "JSON Backup",
            severity = BreadcrumbSeverity.INFO
        )
        return json
    }

    fun restoreJsonBackup(jsonText: String) {
        if (!requireAdminPermission("restore JSON workspace backups")) return
        viewModelScope.launch {
            repository.restoreFromJsonBackup(currentUserId, jsonText)
                .onSuccess {
                    clearUndoRedoHistory()
                    logBreadcrumb(
                        actor = BreadcrumbActor.SYSTEM,
                        category = BreadcrumbCategory.SYSTEM,
                        action = "Workspace Restored from JSON Backup",
                        details = "Replaced plant collections from uploaded JSON snapshot and cleared undo stack",
                        screenTab = "Reports",
                        entityRef = "JSON Restore",
                        severity = BreadcrumbSeverity.SUCCESS
                    )
                    _snackbarMessage.value = "JSON backup restored to Bulacan Plant workspace."
                }
                .onFailure {
                    _snackbarMessage.value = "Restore failed: ${it.localizedMessage}"
                    logBreadcrumb(
                        actor = BreadcrumbActor.SYSTEM,
                        category = BreadcrumbCategory.SYSTEM,
                        action = "JSON Backup Restore Failed",
                        details = it.localizedMessage ?: "Invalid backup payload",
                        screenTab = "Reports",
                        severity = BreadcrumbSeverity.ERROR
                    )
                }
        }
    }

    fun resetOctober2026Seed() {
        if (!requireAdminPermission("reset the workspace to baseline seed")) return
        viewModelScope.launch {
            repository.resetToOctober2026Seed(currentUserId)
                .onSuccess {
                    clearUndoRedoHistory()
                    logBreadcrumb(
                        actor = BreadcrumbActor.SYSTEM,
                        category = BreadcrumbCategory.SYSTEM,
                        action = "Workspace Reset to Baseline Seed",
                        details = "Restored Beginning Inventory baseline for Bulacan Plant sections",
                        screenTab = _sessionTrail.value.lastOrNull() ?: "Summary",
                        entityRef = "Baseline Seed",
                        severity = BreadcrumbSeverity.WARNING
                    )
                    _snackbarMessage.value = "Workspace reset to Beginning Inventory baseline."
                }
                .onFailure {
                    _snackbarMessage.value = "Reset failed: ${it.localizedMessage}"
                    logBreadcrumb(
                        actor = BreadcrumbActor.SYSTEM,
                        category = BreadcrumbCategory.SYSTEM,
                        action = "Baseline Reset Failed",
                        details = it.localizedMessage ?: "Firestore error",
                        screenTab = _sessionTrail.value.lastOrNull() ?: "Summary",
                        severity = BreadcrumbSeverity.ERROR
                    )
                }
        }
    }

    fun resetSelectedScopes(
        targets: Set<ResetScopeTarget>,
        screenTabLabel: String,
        onFiltersReset: () -> Unit = {}
    ) {
        if (targets.isEmpty()) return
        if (ResetScopeTarget.VIEW_FILTERS in targets) {
            _lookbackDays.value = 14
            _safetyBufferPct.value = null
            _rawMaterialDemandMultiplier.value = 1.0
            _rawMaterialIncludeScheduled.value = false
            _rawMaterialLeadOverrideDays.value = null
            onFiltersReset()
        }
        if (ResetScopeTarget.AI_CHAT in targets) {
            _aiInsightState.value = AiInsightState.Idle
            clearChatHistory()
        }

        val dataTargets = targets.filter { it.isDataMutation }.toSet()
        if (dataTargets.isEmpty()) {
            val nonDataNames = targets.joinToString(", ") { it.label }
            _snackbarMessage.value = "Reset $nonDataNames for $screenTabLabel."
            return
        }
        if (!requireAdminPermission("reset shared workspace data")) return

        viewModelScope.launch {
            repository.resetSelectiveTargets(currentUserId, dataTargets)
                .onSuccess {
                    clearUndoRedoHistory()
                    val summaryNames = targets.joinToString(", ") { it.label }
                    if (ResetScopeTarget.HISTORY !in dataTargets) {
                        logBreadcrumb(
                            actor = BreadcrumbActor.USER,
                            category = BreadcrumbCategory.SYSTEM,
                            action = "Selective Reset ($screenTabLabel)",
                            details = "Reset: $summaryNames",
                            screenTab = screenTabLabel,
                            entityRef = "Selective Reset",
                            severity = BreadcrumbSeverity.WARNING
                        )
                    }
                    _snackbarMessage.value = if (targets.size == 1) {
                        "Reset ${targets.first().label}."
                    } else {
                        "Reset ${targets.size} selected areas."
                    }
                }
                .onFailure { err ->
                    _snackbarMessage.value = "Reset failed: ${err.localizedMessage}"
                }
        }
    }

    fun runGeminiPlantAnalysis() {
        val state = opsState.value as? OpsUiState.Ready ?: return
        logBreadcrumb(
            actor = BreadcrumbActor.USER,
            category = BreadcrumbCategory.AI_OPS,
            action = "Requested Gemini Plant Analysis",
            details = "Submitted ${state.summaries.size} SKUs, ${state.batches.size} batches, ${state.schedules.size} schedules",
            screenTab = "AI Ops",
            entityRef = "AI Analysis",
            severity = BreadcrumbSeverity.INFO
        )
        viewModelScope.launch {
            _aiInsightState.value = AiInsightState.Loading
            geminiRepo.analyzePlantOperations(
                kpis = state.kpis,
                summaries = state.summaries,
                batches = state.batches,
                schedules = state.schedules
            ).onSuccess { insight ->
                _aiInsightState.value = AiInsightState.Success(insight)
                logBreadcrumb(
                    actor = BreadcrumbActor.SYSTEM,
                    category = BreadcrumbCategory.AI_OPS,
                    action = "Gemini Plant Analysis Completed",
                    details = "${insight.stockoutAlerts.size} stockout alerts • ${insight.shiftRecommendations.size} shift recommendations",
                    screenTab = "AI Ops",
                    entityRef = "AI Analysis",
                    severity = BreadcrumbSeverity.SUCCESS
                )
            }.onFailure { err ->
                val errMsg = err.localizedMessage ?: "Gemini analysis failed."
                _aiInsightState.value = AiInsightState.Error(errMsg)
                logBreadcrumb(
                    actor = BreadcrumbActor.SYSTEM,
                    category = BreadcrumbCategory.AI_OPS,
                    action = "Gemini Plant Analysis Failed",
                    details = errMsg,
                    screenTab = "AI Ops",
                    entityRef = "AI Analysis",
                    severity = BreadcrumbSeverity.ERROR
                )
            }
        }
    }

    fun sendChatMessage(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isBlank() || _isChatSending.value) return
        val state = opsState.value as? OpsUiState.Ready ?: return

        val userMsgId = UUID.randomUUID().toString()
        val botMsgId = UUID.randomUUID().toString()

        _chatMessages.value = _chatMessages.value +
            ChatMessage(id = userMsgId, isUser = true, text = trimmed) +
            ChatMessage(id = botMsgId, isUser = false, text = "", isStreaming = true)
        _isChatSending.value = true

        logBreadcrumb(
            actor = BreadcrumbActor.USER,
            category = BreadcrumbCategory.AI_OPS,
            action = "Sent AI Operations Prompt",
            details = trimmed,
            screenTab = "AI Ops",
            entityRef = "Gemini Chat",
            severity = BreadcrumbSeverity.INFO
        )

        viewModelScope.launch {
            try {
                var finalResponse = ""
                geminiRepo.sendChatMessageStream(
                    userMessage = trimmed,
                    kpis = state.kpis,
                    summaries = state.summaries,
                    batches = state.batches,
                    schedules = state.schedules
                ).collect { partial ->
                    finalResponse = partial
                    _chatMessages.value = _chatMessages.value.map { msg ->
                        if (msg.id == botMsgId) msg.copy(text = partial, isStreaming = true) else msg
                    }
                }
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == botMsgId) msg.copy(isStreaming = false) else msg
                }
                logBreadcrumb(
                    actor = BreadcrumbActor.SYSTEM,
                    category = BreadcrumbCategory.AI_OPS,
                    action = "AI Assistant Response Received",
                    details = "Responded (${finalResponse.length} chars) to: ${trimmed.take(80)}",
                    screenTab = "AI Ops",
                    entityRef = "Gemini Chat",
                    severity = BreadcrumbSeverity.SUCCESS
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Chat timed out", e)
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == botMsgId) {
                        msg.copy(
                            text = "Request timed out after 60 seconds. Please try a shorter question.",
                            isStreaming = false
                        )
                    } else msg
                }
                logBreadcrumb(
                    actor = BreadcrumbActor.SYSTEM,
                    category = BreadcrumbCategory.AI_OPS,
                    action = "AI Assistant Timed Out",
                    details = "Prompt timed out after 60s: ${trimmed.take(80)}",
                    screenTab = "AI Ops",
                    entityRef = "Gemini Chat",
                    severity = BreadcrumbSeverity.ERROR
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat failed", e)
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == botMsgId) {
                        msg.copy(
                            text = "Error contacting Gemini AI: ${e.localizedMessage ?: "Please check App Check and connectivity."}",
                            isStreaming = false
                        )
                    } else msg
                }
                logBreadcrumb(
                    actor = BreadcrumbActor.SYSTEM,
                    category = BreadcrumbCategory.AI_OPS,
                    action = "AI Assistant Request Failed",
                    details = e.localizedMessage ?: "App Check / connectivity error",
                    screenTab = "AI Ops",
                    entityRef = "Gemini Chat",
                    severity = BreadcrumbSeverity.ERROR
                )
            } finally {
                _isChatSending.value = false
            }
        }
    }

    fun clearChatHistory() {
        geminiRepo.resetChat()
        _chatMessages.value = listOf(
            ChatMessage(
                id = "welcome_${System.currentTimeMillis()}",
                isUser = false,
                text = "Conversation cleared. Ask me anything about 10K Agri Feeds – Bulacan Plant stock, dispatches, orders, or recovery."
            )
        )
        logBreadcrumb(
            actor = BreadcrumbActor.USER,
            category = BreadcrumbCategory.AI_OPS,
            action = "Cleared AI Chat History",
            details = "Reset Gemini Operations conversation session",
            screenTab = "AI Ops",
            entityRef = "Gemini Chat",
            severity = BreadcrumbSeverity.INFO
        )
    }
}
