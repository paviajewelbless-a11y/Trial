package com.example

import android.Manifest
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Factory
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.GeminiOpsRepository
import com.example.data.PlantRepository
import com.example.data.TabPreferencesStore
import com.example.data.local.FeedInventoryRepository
import com.example.firebase.authStateFlow
import com.example.firebase.configureAppCheck
import com.example.firebase.signOutUser
import com.example.model.BreadcrumbCategory
import com.example.model.CustomerOrder
import com.example.model.DispatchRecord
import com.example.model.OrderStatus
import com.example.model.PlantSection
import com.example.model.ProductionBatch
import com.example.model.RawMaterialRiskStatus
import com.example.model.SchedulePriority
import com.example.model.ScheduleRun
import com.example.model.ScheduleStatus
import com.example.model.SharedWorkspaceOption
import com.example.model.SkuEntity
import com.example.model.SkuInventorySummary
import com.example.model.WorkspaceInvite
import com.example.model.WorkspaceInviteStatus
import com.example.model.WorkspaceRole
import com.example.notifications.StockAlertNotifier
import com.example.notifications.WorkspaceInviteNotifier
import com.example.ui.AuthScreen
import com.example.ui.components.BulkPasteDialog
import com.example.ui.components.CustomizeBottomTabsDialog
import com.example.ui.components.DesktopShortcutsDialog
import com.example.ui.components.LocalIsDesktopLayout
import com.example.ui.components.LocalWindowWidthDp
import com.example.ui.components.LogBatchDialog
import com.example.ui.components.LogDispatchDialog
import com.example.ui.components.ManageCustomersDialog
import com.example.ui.components.ManageSkusDialog
import com.example.ui.components.OrderEditorDialog
import com.example.ui.components.ScheduleDraftSeed
import com.example.ui.components.ScheduleRunDialog
import com.example.ui.components.SelectiveResetDialog
import com.example.ui.components.SkuEditorDialog
import com.example.ui.components.desktopPointerHand
import kotlin.math.ceil
import kotlin.math.max
import com.example.ui.screens.AiOpsScreen
import com.example.ui.screens.DailyLogScreen
import com.example.ui.screens.DispatchedScreen
import com.example.ui.screens.FeedBomScreen
import com.example.ui.screens.FinishedGoodsInventoryScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MaterialsInventoryScreen
import com.example.ui.screens.MonitorScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ProductionMonitorScreen
import com.example.ui.screens.RecoveryScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.ReprocessScreen
import com.example.ui.screens.RoleBadgeChip
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.SummaryScreen
import com.example.ui.screens.TeamAccessScreen
import com.example.ui.theme.FeedMillTheme
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.GrainAmber
import com.example.ui.theme.StatusCritical
import com.example.viewmodel.FeedInventoryViewModel
import com.example.viewmodel.FeedMillViewModel
import com.example.viewmodel.OpsUiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val openRawMaterialsTrigger = MutableStateFlow(false)
    private val openTeamTrigger = MutableStateFlow(false)

    private fun handleIncomingNavigationIntent(incomingIntent: Intent?) {
        if (incomingIntent == null) return
        if (incomingIntent.getBooleanExtra(StockAlertNotifier.EXTRA_OPEN_RAW_MATERIALS, false)) {
            openRawMaterialsTrigger.value = true
        }
        if (incomingIntent.getBooleanExtra(WorkspaceInviteNotifier.EXTRA_OPEN_TEAM, false)) {
            openTeamTrigger.value = true
        }
        val uri = incomingIntent.data
        if (incomingIntent.action == Intent.ACTION_VIEW && uri != null) {
            val isInviteDeepLink = uri.scheme.equals("feedmillops", ignoreCase = true) &&
                uri.host.equals("invite", ignoreCase = true)
            val hasInviteQuery = uri.getQueryParameter("action") == "team_invite" ||
                !uri.getQueryParameter("invite").isNullOrBlank() ||
                !uri.getQueryParameter("workspace").isNullOrBlank() ||
                uri.path?.startsWith("/invite") == true
            if (isInviteDeepLink || hasInviteQuery) {
                openTeamTrigger.value = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)
        handleIncomingNavigationIntent(intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            val openRawRequested by openRawMaterialsTrigger.collectAsStateWithLifecycle()
            val openTeamRequested by openTeamTrigger.collectAsStateWithLifecycle()
            FeedMillTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        openRawMaterialsRequested = openRawRequested,
                        onConsumeOpenRawMaterials = { openRawMaterialsTrigger.value = false },
                        openTeamRequested = openTeamRequested,
                        onConsumeOpenTeam = { openTeamTrigger.value = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingNavigationIntent(intent)
    }
}

enum class OpsTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    SUMMARY("Summary", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_summary"),
    DAILY_LOG("Daily Log", Icons.Filled.EventNote, Icons.Outlined.EventNote, "nav_daily_log"),
    MONITOR("Monitor", Icons.Filled.Inventory, Icons.Outlined.Inventory, "nav_monitor"),
    ORDERS("Orders", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart, "nav_orders"),
    SCHEDULE("Schedule", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_schedule"),
    DISPATCHED("Dispatched", Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping, "nav_dispatched"),
    RECOVERY("Recovery", Icons.Filled.Factory, Icons.Outlined.Factory, "nav_recovery"),
    REPORTS("Reports", Icons.Filled.Assessment, Icons.Outlined.Assessment, "nav_reports"),
    AI_OPS("AI Ops", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_ai_ops"),
    HISTORY("History", Icons.Filled.History, Icons.Outlined.History, "nav_history"),
    FINISHED_GOODS("Finished Goods", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "nav_finished_goods"),
    MACRO_INVENTORY("Macro Inv", Icons.Filled.Grain, Icons.Outlined.Grain, "nav_macro_inventory"),
    MICRO_INVENTORY("Micro Inv", Icons.Filled.Biotech, Icons.Outlined.Biotech, "nav_micro_inventory"),
    FEED_BOM("Feed BOM", Icons.Filled.Science, Icons.Outlined.Science, "nav_feed_bom"),
    PRODUCTION_MONITOR("Prod Monitor", Icons.Filled.BarChart, Icons.Outlined.BarChart, "nav_production_monitor"),
    REPROCESS("Reprocess", Icons.Filled.Autorenew, Icons.Outlined.Autorenew, "nav_reprocess"),
    TEAM("Team", Icons.Filled.Group, Icons.Outlined.Group, "nav_team");

    fun bottomNavLabel(): String = when (this) {
        DISPATCHED -> "Dispatch"
        FINISHED_GOODS -> "Finished"
        MACRO_INVENTORY -> "Macro"
        MICRO_INVENTORY -> "Micro"
        PRODUCTION_MONITOR -> "Prod Mon"
        else -> label
    }

    fun navDescription(): String = when (this) {
        SUMMARY -> "Plant overview and daily highlights"
        DAILY_LOG -> "Daily inventory, orders & dispatch log"
        MONITOR -> "Stock levels & SKU health status"
        ORDERS -> "Customer orders and fulfillment"
        SCHEDULE -> "Planned milling runs & batch queue"
        DISPATCHED -> "Delivery records & dispatches"
        RECOVERY -> "Production output & recovery yield"
        REPORTS -> "Period reports, backups & file exports"
        AI_OPS -> "Smart assistant & plant analysis"
        HISTORY -> "Timeline of your actions & plant updates"
        FINISHED_GOODS -> "Finished feed bags inventory, cover & production needs"
        MACRO_INVENTORY -> "Macro materials stock (grains, soya, meals, molasses)"
        MICRO_INVENTORY -> "Micro ingredients across Fast, Slow & Non-Moving sectors"
        FEED_BOM -> "Feed type BOM cards (Macro & Micro)"
        PRODUCTION_MONITOR -> "Daily feed output vs. target charts & attainment"
        REPROCESS -> "Returned feed quarantine, fast action & inventory re-flagging"
        TEAM -> "Shared workspace, team members & role permissions"
    }
}

private val defaultPrimaryBottomTabs = listOf(
    OpsTab.SUMMARY,
    OpsTab.MONITOR,
    OpsTab.ORDERS,
    OpsTab.DISPATCHED
)

data class ScheduleRequest(
    val sku: SkuEntity? = null,
    val bags: Long = 40L,
    val priority: SchedulePriority = SchedulePriority.ORDER_DRIVEN,
    val linkedOrder: CustomerOrder? = null,
    val items: List<ScheduleDraftSeed> = emptyList()
)

data class BatchDialogState(
    val isOpen: Boolean = false,
    val fromRun: ScheduleRun? = null,
    val existingBatch: ProductionBatch? = null
)

data class DispatchDialogState(
    val isOpen: Boolean = false,
    val initialSku: SkuEntity? = null,
    val initialOrder: CustomerOrder? = null,
    val initialOrders: List<CustomerOrder> = emptyList(),
    val existingDispatch: DispatchRecord? = null
)

data class OrderEditorRequest(
    val open: Boolean = false,
    val existingOrder: CustomerOrder? = null,
    val initialRushOrder: Boolean = false
)

@Composable
fun AppNavigation(
    openRawMaterialsRequested: Boolean = false,
    onConsumeOpenRawMaterials: () -> Unit = {},
    openTeamRequested: Boolean = false,
    onConsumeOpenTeam: () -> Unit = {}
) {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val resolvedEmail = user.email?.trim()?.takeIf { it.contains("@") }
            ?: user.providerData.firstNotNullOfOrNull { info ->
                info.email?.trim()?.takeIf { it.contains("@") }
            }
            ?: "Plant Operator"
        key(user.uid) {
            WorkspaceHost(
                signedInUid = user.uid,
                userEmail = resolvedEmail,
                openRawMaterialsRequested = openRawMaterialsRequested,
                onConsumeOpenRawMaterials = onConsumeOpenRawMaterials,
                openTeamRequested = openTeamRequested,
                onConsumeOpenTeam = onConsumeOpenTeam
            )
        }
    }
}

@Composable
private fun WorkspaceHost(
    signedInUid: String,
    userEmail: String,
    openRawMaterialsRequested: Boolean,
    onConsumeOpenRawMaterials: () -> Unit,
    openTeamRequested: Boolean,
    onConsumeOpenTeam: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val databaseId = remember(context) { context.getString(R.string.firestore_database_id) }
    val workspaceRepo = remember(databaseId) { PlantRepository(FirebaseFirestore.getInstance(databaseId)) }

    val savedWorkspaceState by TabPreferencesStore
        .observeSelectedWorkspaceOwnerId(context, signedInUid)
        .collectAsStateWithLifecycle(initialValue = null)

    var myInvites by remember(signedInUid, userEmail) { mutableStateOf<List<WorkspaceInvite>>(emptyList()) }
    var invitesLoaded by remember(signedInUid, userEmail) { mutableStateOf(false) }
    var workspaceBannerToast by remember(signedInUid) { mutableStateOf<String?>(null) }

    LaunchedEffect(workspaceRepo, signedInUid, userEmail) {
        if (userEmail.contains("@")) {
            workspaceRepo.observeMyInvitesByEmail(userEmail)
                .catch { error ->
                    if (error is CancellationException) throw error
                    invitesLoaded = true
                    emit(emptyList())
                }
                .collect { invites ->
                    myInvites = invites
                    invitesLoaded = true
                }
        } else {
            myInvites = emptyList()
            invitesLoaded = true
        }
    }

    val savedWorkspaceId = savedWorkspaceState
    if (savedWorkspaceId == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val activeInvites = remember(myInvites) {
        myInvites.filter {
            it.resolvedStatus() == WorkspaceInviteStatus.ACTIVE && it.workspaceOwnerId.isNotBlank()
        }
    }
    val pendingInvitesForMe = remember(myInvites) {
        myInvites.filter {
            it.resolvedStatus() == WorkspaceInviteStatus.PENDING && it.workspaceOwnerId.isNotBlank()
        }
    }

    val availableWorkspaces = remember(signedInUid, userEmail, activeInvites) {
        val personal = SharedWorkspaceOption(
            workspaceOwnerId = signedInUid,
            workspaceName = "Bulacan Plant",
            ownerEmail = userEmail,
            role = WorkspaceRole.ADMIN,
            isPersonalOwner = true
        )
        val shared = activeInvites
            .filter { it.workspaceOwnerId != signedInUid }
            .distinctBy { it.workspaceOwnerId }
            .map { inv ->
                SharedWorkspaceOption(
                    workspaceOwnerId = inv.workspaceOwnerId,
                    workspaceName = inv.workspaceName.ifBlank { "Bulacan Plant" },
                    ownerEmail = inv.ownerEmail.ifBlank { inv.invitedByEmail },
                    role = inv.resolvedRole(),
                    isPersonalOwner = false
                )
            }
        listOf(personal) + shared
    }

    // Detect if access to previously active shared workspace was revoked
    LaunchedEffect(invitesLoaded, savedWorkspaceId, activeInvites, signedInUid) {
        if (invitesLoaded &&
            savedWorkspaceId.isNotBlank() &&
            savedWorkspaceId != signedInUid &&
            activeInvites.none { it.workspaceOwnerId == savedWorkspaceId }
        ) {
            TabPreferencesStore.saveSelectedWorkspaceOwnerId(context, signedInUid, signedInUid)
            workspaceBannerToast = "Access to shared workspace ended. Switched to your Bulacan Plant workspace."
        }
    }

    val effectiveWorkspaceId = when {
        savedWorkspaceId.isBlank() -> activeInvites.firstOrNull()?.workspaceOwnerId ?: signedInUid
        savedWorkspaceId == signedInUid -> signedInUid
        activeInvites.any { it.workspaceOwnerId == savedWorkspaceId } -> savedWorkspaceId
        !invitesLoaded -> savedWorkspaceId
        else -> signedInUid
    }

    val activeWorkspaceOption = availableWorkspaces.firstOrNull { it.workspaceOwnerId == effectiveWorkspaceId }
        ?: availableWorkspaces.first()

    key(signedInUid, effectiveWorkspaceId) {
        MainOpsShell(
            currentUserId = effectiveWorkspaceId,
            signedInUid = signedInUid,
            userEmail = userEmail,
            initialRole = activeWorkspaceOption.role,
            availableWorkspaces = availableWorkspaces,
            pendingInvitesForMe = pendingInvitesForMe,
            workspaceBannerToast = workspaceBannerToast,
            onConsumeWorkspaceBannerToast = { workspaceBannerToast = null },
            onSwitchWorkspace = { targetOwnerId ->
                scope.launch {
                    TabPreferencesStore.saveSelectedWorkspaceOwnerId(context, signedInUid, targetOwnerId)
                    val label = if (targetOwnerId == signedInUid) {
                        "Switched to your personal Bulacan Plant workspace."
                    } else {
                        val targetOption = availableWorkspaces.firstOrNull { it.workspaceOwnerId == targetOwnerId }
                        "Switched to shared workspace (${targetOption?.role?.label ?: "Member"})."
                    }
                    workspaceBannerToast = label
                }
            },
            onAcceptInvite = { invite ->
                scope.launch {
                    workspaceRepo.acceptWorkspaceInvite(invite)
                        .onSuccess {
                            TabPreferencesStore.saveSelectedWorkspaceOwnerId(context, signedInUid, invite.workspaceOwnerId)
                            workspaceBannerToast = "Joined ${invite.workspaceName.ifBlank { "Bulacan Plant" }} as ${invite.resolvedRole().label}."
                        }
                        .onFailure { err ->
                            workspaceBannerToast = err.localizedMessage ?: "Failed to accept workspace invite."
                        }
                }
            },
            onDeclineInvite = { invite ->
                scope.launch {
                    workspaceRepo.declineOrLeaveWorkspace(invite)
                        .onSuccess {
                            if (effectiveWorkspaceId == invite.workspaceOwnerId) {
                                TabPreferencesStore.saveSelectedWorkspaceOwnerId(context, signedInUid, signedInUid)
                            }
                            workspaceBannerToast = "Invitation declined."
                        }
                        .onFailure { err ->
                            workspaceBannerToast = err.localizedMessage ?: "Failed to decline invitation."
                        }
                }
            },
            onLeaveActiveSharedWorkspace = {
                scope.launch {
                    val activeInv = activeInvites.firstOrNull { it.workspaceOwnerId == effectiveWorkspaceId }
                    val res = if (activeInv != null) {
                        workspaceRepo.declineOrLeaveWorkspace(activeInv)
                    } else {
                        workspaceRepo.removeWorkspaceMember(effectiveWorkspaceId, signedInUid, "")
                    }
                    res.onSuccess {
                        TabPreferencesStore.saveSelectedWorkspaceOwnerId(context, signedInUid, signedInUid)
                        workspaceBannerToast = "Left shared workspace. Switched to your workspace."
                    }.onFailure { err ->
                        workspaceBannerToast = err.localizedMessage ?: "Failed to leave shared workspace."
                    }
                }
            },
            openRawMaterialsRequested = openRawMaterialsRequested,
            onConsumeOpenRawMaterials = onConsumeOpenRawMaterials,
            openTeamRequested = openTeamRequested,
            onConsumeOpenTeam = onConsumeOpenTeam
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainOpsShell(
    currentUserId: String,
    signedInUid: String = currentUserId,
    userEmail: String,
    initialRole: WorkspaceRole = WorkspaceRole.ADMIN,
    availableWorkspaces: List<SharedWorkspaceOption> = emptyList(),
    pendingInvitesForMe: List<WorkspaceInvite> = emptyList(),
    workspaceBannerToast: String? = null,
    onConsumeWorkspaceBannerToast: () -> Unit = {},
    onSwitchWorkspace: (String) -> Unit = {},
    onAcceptInvite: (WorkspaceInvite) -> Unit = {},
    onDeclineInvite: (WorkspaceInvite) -> Unit = {},
    onLeaveActiveSharedWorkspace: () -> Unit = {},
    openRawMaterialsRequested: Boolean = false,
    onConsumeOpenRawMaterials: () -> Unit = {},
    openTeamRequested: Boolean = false,
    onConsumeOpenTeam: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: FeedMillViewModel = viewModel(
        key = "${signedInUid}_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                FeedMillViewModel(
                    repository = PlantRepository(db),
                    geminiRepo = GeminiOpsRepository(),
                    currentUserId = currentUserId,
                    localInventoryRepository = FeedInventoryRepository(app),
                    signedInUid = signedInUid,
                    signedInEmail = userEmail,
                    initialRole = initialRole
                )
            }
        }
    )

    val feedInventoryViewModel: FeedInventoryViewModel = viewModel(
        key = "room_inventory_$currentUserId",
        factory = run {
            val appContext = context.applicationContext
            FeedInventoryViewModel.provideFactory(appContext)
        }
    )

    val opsState by viewModel.opsState.collectAsStateWithLifecycle()
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val teamMembers by viewModel.teamMembers.collectAsStateWithLifecycle()
    val workspaceInvites by viewModel.workspaceInvites.collectAsStateWithLifecycle()
    val localInventoryUiState by feedInventoryViewModel.uiState.collectAsStateWithLifecycle()
    val lookbackDays by viewModel.lookbackDays.collectAsStateWithLifecycle()
    val safetyBufferPct by viewModel.safetyBufferPct.collectAsStateWithLifecycle()
    val rawDemandMultiplier by viewModel.rawMaterialDemandMultiplier.collectAsStateWithLifecycle()
    val rawIncludeScheduled by viewModel.rawMaterialIncludeScheduled.collectAsStateWithLifecycle()
    val rawLeadOverrideDays by viewModel.rawMaterialLeadOverrideDays.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val aiInsightState by viewModel.aiInsightState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatSending by viewModel.isChatSending.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val isUndoRedoInProgress by viewModel.isUndoRedoInProgress.collectAsStateWithLifecycle()
    val undoLabel by viewModel.undoLabel.collectAsStateWithLifecycle()
    val redoLabel by viewModel.redoLabel.collectAsStateWithLifecycle()
    val breadcrumbs by viewModel.breadcrumbs.collectAsStateWithLifecycle()
    val sessionTrail by viewModel.sessionTrail.collectAsStateWithLifecycle()

    LaunchedEffect(signedInUid, currentUserId, userRole) {
        viewModel.recordSessionStart(userEmail)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(workspaceBannerToast) {
        if (!workspaceBannerToast.isNullOrBlank()) {
            snackbarHostState.showSnackbar(workspaceBannerToast)
            onConsumeWorkspaceBannerToast()
        }
    }
    LaunchedEffect(snackbarMsg) {
        val msg = snackbarMsg
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }
    LaunchedEffect(localInventoryUiState.statusMessage) {
        val msg = localInventoryUiState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            feedInventoryViewModel.consumeStatusMessage()
        }
    }
    LaunchedEffect(localInventoryUiState.errorMessage) {
        val err = localInventoryUiState.errorMessage
        if (!err.isNullOrBlank()) {
            snackbarHostState.showSnackbar(err)
            feedInventoryViewModel.consumeErrorMessage()
        }
    }

    var currentTab by remember { mutableStateOf(OpsTab.SUMMARY) }
    var monitorInitialView by remember { mutableIntStateOf(0) }
    var showMoreNavSheet by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        mutableStateOf(StockAlertNotifier.hasNotificationPermission(context))
    }
    var lastAlertSignature by remember { mutableStateOf("") }
    var lastInviteNotificationSignature by remember { mutableStateOf("") }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val enabledNow = granted || StockAlertNotifier.hasNotificationPermission(context)
        hasNotificationPermission = enabledNow
        val ready = opsState as? OpsUiState.Ready
        if (enabledNow && ready != null) {
            StockAlertNotifier.notifyLowRawMaterials(context, ready.rawMaterialProjections, isManualTrigger = true)
            if (pendingInvitesForMe.isNotEmpty()) {
                WorkspaceInviteNotifier.notifyPendingInvites(context, pendingInvitesForMe)
            }
            scope.launch { snackbarHostState.showSnackbar("Low-stock notifications enabled.") }
        } else if (!enabledNow) {
            scope.launch { snackbarHostState.showSnackbar("Notification permission declined.") }
        }
    }

    val requestNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !StockAlertNotifier.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            hasNotificationPermission = StockAlertNotifier.hasNotificationPermission(context)
            val ready = opsState as? OpsUiState.Ready
            if (hasNotificationPermission && ready != null) {
                StockAlertNotifier.notifyLowRawMaterials(context, ready.rawMaterialProjections, isManualTrigger = true)
            } else {
                scope.launch { snackbarHostState.showSnackbar("Please enable notifications in Android settings.") }
            }
        }
    }

    LaunchedEffect(openRawMaterialsRequested) {
        if (openRawMaterialsRequested) {
            monitorInitialView = 1
            currentTab = OpsTab.MONITOR
            onConsumeOpenRawMaterials()
        }
    }

    LaunchedEffect(openTeamRequested) {
        if (openTeamRequested) {
            currentTab = OpsTab.TEAM
            onConsumeOpenTeam()
        }
    }

    LaunchedEffect(pendingInvitesForMe, hasNotificationPermission) {
        val sig = pendingInvitesForMe.joinToString("|") { "${it.id}:${it.role}:${it.status}" }
        if (pendingInvitesForMe.isNotEmpty() && sig != lastInviteNotificationSignature) {
            lastInviteNotificationSignature = sig
            if (hasNotificationPermission) {
                WorkspaceInviteNotifier.notifyPendingInvites(context, pendingInvitesForMe)
            }
        }
    }

    LaunchedEffect(opsState, hasNotificationPermission) {
        val ready = opsState as? OpsUiState.Ready ?: return@LaunchedEffect
        val urgent = ready.rawMaterialProjections.filter {
            it.material.alertsEnabled &&
                (it.status == RawMaterialRiskStatus.CRITICAL || it.status == RawMaterialRiskStatus.LOW_WATCH)
        }
        val sig = urgent.joinToString("|") { "${it.material.id}:${it.status.name}" }
        if (urgent.isNotEmpty() && sig != lastAlertSignature) {
            lastAlertSignature = sig
            val summaryText = urgent.take(3).joinToString(", ") {
                "${it.material.name} (${it.projectedRunOutLabel})"
            }
            viewModel.recordRawMaterialAlertBreadcrumb(urgent.size, summaryText)
            if (hasNotificationPermission) {
                StockAlertNotifier.notifyLowRawMaterials(
                    context = context,
                    projections = ready.rawMaterialProjections,
                    isManualTrigger = false
                )
            }
        }
    }

    val navigateToTab: (OpsTab, String) -> Unit = { target, source ->
        showMoreNavSheet = false
        if (currentTab != target) {
            val prevLabel = currentTab.label
            currentTab = target
            viewModel.recordNavigationBreadcrumb(
                fromTab = prevLabel,
                toTab = target.label,
                source = source
            )
        }
    }
    BackHandler(enabled = showMoreNavSheet || currentTab != OpsTab.SUMMARY) {
        if (showMoreNavSheet) {
            showMoreNavSheet = false
        } else {
            navigateToTab(OpsTab.SUMMARY, "System Back")
        }
    }

    // Modal state
    var dispatchDialog by remember { mutableStateOf(DispatchDialogState()) }
    var orderEditorState by remember { mutableStateOf(OrderEditorRequest()) }
    var showScheduleDialog by remember { mutableStateOf<ScheduleRequest?>(null) }
    var editingScheduleRun by remember { mutableStateOf<ScheduleRun?>(null) }
    var showBatchDialog by remember { mutableStateOf(BatchDialogState()) }
    var showBulkPasteDialog by remember { mutableStateOf(false) }
    var showSkuEditor by remember { mutableStateOf<Pair<Boolean, SkuEntity?>>(false to null) }
    var showManageSkus by remember { mutableStateOf(false) }
    var showManageCustomers by remember { mutableStateOf(false) }
    var showTopResetDialog by remember { mutableStateOf(false) }
    var showShortcutsDialog by remember { mutableStateOf(false) }
    var showCustomizeTabsDialog by remember { mutableStateOf(false) }
    val tabResetTicks = remember { mutableStateMapOf<OpsTab, Int>() }

    val savedBottomTabNames by TabPreferencesStore.observeBottomTabNames(context)
        .collectAsStateWithLifecycle(initialValue = TabPreferencesStore.DEFAULT_BOTTOM_TAB_NAMES)
    val primaryBottomTabs = remember(savedBottomTabNames) {
        savedBottomTabNames
            .mapNotNull { name -> OpsTab.entries.firstOrNull { it.name == name } }
            .distinct()
            .take(TabPreferencesStore.MAX_PINNED_BOTTOM_TABS)
            .ifEmpty { defaultPrimaryBottomTabs }
    }

    val priorityProductPrefs by TabPreferencesStore.observePriorityProductPrefs(context)
        .collectAsStateWithLifecycle(initialValue = TabPreferencesStore.PriorityProductPrefs())
    val activeSummaries = (opsState as? OpsUiState.Ready)?.summaries.orEmpty()
    val effectivePriorityKeys = remember(priorityProductPrefs, activeSummaries) {
        if (priorityProductPrefs.isCustomized) {
            priorityProductPrefs.selectedKeys
        } else {
            activeSummaries
                .filter { TabPreferencesStore.isAutoActivePriority(it) }
                .map { TabPreferencesStore.skuPriorityKey(it.sku) }
                .toSet()
        }
    }
    val onToggleHideNonPriority: (Boolean) -> Unit = { hide ->
        scope.launch {
            TabPreferencesStore.setHideNonPriorityProducts(context, hide)
        }
    }
    val onToggleProductPriority: (SkuEntity) -> Unit = { sku ->
        val key = TabPreferencesStore.skuPriorityKey(sku)
        val updated = if (key in effectivePriorityKeys) {
            effectivePriorityKeys - key
        } else {
            effectivePriorityKeys + key
        }
        scope.launch {
            TabPreferencesStore.savePriorityProductKeys(context, updated)
        }
    }
    val onSavePriorityProductKeys: (Set<String>) -> Unit = { keys ->
        scope.launch {
            TabPreferencesStore.savePriorityProductKeys(context, keys)
            snackbarHostState.showSnackbar("Updated ${keys.size} priority product(s).")
        }
    }
    val onResetPriorityToAutoActive: () -> Unit = {
        scope.launch {
            TabPreferencesStore.resetPriorityProductsToAutoActive(context)
            snackbarHostState.showSnackbar("Restored auto-active priority products.")
        }
    }

    val preselectedDispatchPlant by TabPreferencesStore.observePreselectedDispatchPlant(context)
        .collectAsStateWithLifecycle(initialValue = "")
    val onSavePreselectedDispatchPlant: (String) -> Unit = { plantLabel ->
        scope.launch {
            TabPreferencesStore.savePreselectedDispatchPlant(context, plantLabel)
            val normalized = TabPreferencesStore.normalizeDispatchPlantLabel(plantLabel)
            snackbarHostState.showSnackbar(
                if (normalized.isBlank()) "Dispatch source plant set to All Plants."
                else "Preselected dispatch plant: $normalized"
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when {
                        event.key == Key.F1 -> {
                            showShortcutsDialog = !showShortcutsDialog
                            true
                        }
                        event.isCtrlPressed && event.key == Key.Slash -> {
                            showShortcutsDialog = !showShortcutsDialog
                            true
                        }
                        event.isCtrlPressed && !event.isShiftPressed && event.key == Key.Z -> {
                            if (canUndo && !isUndoRedoInProgress) viewModel.undoLastAction()
                            true
                        }
                        (event.isCtrlPressed && event.key == Key.Y) ||
                            (event.isCtrlPressed && event.isShiftPressed && event.key == Key.Z) -> {
                            if (canRedo && !isUndoRedoInProgress) viewModel.redoLastAction()
                            true
                        }
                        event.isAltPressed -> {
                            val idx = when (event.key) {
                                Key.One -> 0
                                Key.Two -> 1
                                Key.Three -> 2
                                Key.Four -> 3
                                Key.Five -> 4
                                Key.Six -> 5
                                Key.Seven -> 6
                                Key.Eight -> 7
                                Key.Nine -> 8
                                Key.Zero -> 9
                                else -> -1
                            }
                            if (idx in OpsTab.entries.indices) {
                                navigateToTab(OpsTab.entries[idx], "Keyboard Shortcut (Alt+${if (idx == 9) 0 else idx + 1})")
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        val windowWidthDp = maxWidth.value.toInt()
        val useDesktopSidebar = maxWidth >= 960.dp
        val useRail = maxWidth >= 600.dp && !useDesktopSidebar
        val isDesktopLayout = maxWidth >= 860.dp
        val showShortcutsAction = maxWidth >= 600.dp

        val guardWriteOperation: (String, () -> Unit) -> Unit = { actionDesc, action ->
            if (!userRole.canWriteOperations) {
                scope.launch {
                    snackbarHostState.showSnackbar("Viewer access is read-only. Ask an Admin for Operator permissions to $actionDesc.")
                }
            } else {
                action()
            }
        }

        val resolvedOwnerEmail = remember(availableWorkspaces, currentUserId, userEmail, viewModel.isWorkspaceOwner) {
            if (viewModel.isWorkspaceOwner) {
                userEmail
            } else {
                availableWorkspaces.firstOrNull { it.workspaceOwnerId == currentUserId }?.ownerEmail.orEmpty()
            }
        }

        CompositionLocalProvider(
            LocalIsDesktopLayout provides isDesktopLayout,
            LocalWindowWidthDp provides windowWidthDp
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "FeedMill Ops • Bulacan Plant",
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Surface(
                                        onClick = { navigateToTab(OpsTab.TEAM, "Top Bar Role Chip") },
                                        color = Color.White.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("topbar_workspace_role_chip")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = buildString {
                                                    if (!viewModel.isWorkspaceOwner) append("Shared • ")
                                                    append(userRole.label)
                                                    if (pendingInvitesForMe.isNotEmpty()) {
                                                        append(" • ${pendingInvitesForMe.size} Invite")
                                                    }
                                                },
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (isDesktopLayout) {
                                        "10K Agri Feeds • Desktop Workspace • $userEmail"
                                    } else {
                                        "${currentTab.label} • $userEmail"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = ForestEmerald,
                            titleContentColor = Color.White,
                            actionIconContentColor = Color.White
                        ),
                        actions = {
                            IconButton(
                                onClick = viewModel::undoLastAction,
                                enabled = userRole.canWriteOperations && canUndo && !isUndoRedoInProgress,
                                modifier = Modifier
                                    .desktopPointerHand()
                                    .testTag("undo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = undoLabel?.let { "Undo $it (Ctrl+Z)" } ?: "Undo (Ctrl+Z)",
                                    tint = if (userRole.canWriteOperations && canUndo && !isUndoRedoInProgress) Color.White else Color.White.copy(alpha = 0.38f)
                                )
                            }
                            IconButton(
                                onClick = viewModel::redoLastAction,
                                enabled = userRole.canWriteOperations && canRedo && !isUndoRedoInProgress,
                                modifier = Modifier
                                    .desktopPointerHand()
                                    .testTag("redo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Redo,
                                    contentDescription = redoLabel?.let { "Redo $it (Ctrl+Y)" } ?: "Redo (Ctrl+Y)",
                                    tint = if (userRole.canWriteOperations && canRedo && !isUndoRedoInProgress) Color.White else Color.White.copy(alpha = 0.38f)
                                )
                            }
                            if (showShortcutsAction) {
                                IconButton(
                                    onClick = { showShortcutsDialog = true },
                                    modifier = Modifier
                                        .desktopPointerHand()
                                        .testTag("desktop_shortcuts_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Keyboard,
                                        contentDescription = "PC Keyboard Shortcuts (F1)"
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    if (userRole.canResetWorkspace) {
                                        showTopResetDialog = true
                                        viewModel.recordUiBreadcrumb(
                                            category = BreadcrumbCategory.SYSTEM,
                                            action = "Opened Reset Options Dialog",
                                            screenTab = currentTab.label
                                        )
                                    } else {
                                        viewModel.resetMonitorParameters()
                                        tabResetTicks[currentTab] = (tabResetTicks[currentTab] ?: 0) + 1
                                        scope.launch {
                                            snackbarHostState.showSnackbar("View filters cleared. Admin role is required for workspace data resets.")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .desktopPointerHand()
                                    .testTag("top_reset_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.RestartAlt,
                                    contentDescription = "Reset workspace or filters"
                                )
                            }
                            IconButton(
                                onClick = { signOutUser(credentialManager, scope) },
                                modifier = Modifier
                                    .desktopPointerHand()
                                    .testTag("sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Sign out"
                                )
                            }
                        }
                    )
                },
            bottomBar = {
                if (!useRail && !useDesktopSidebar) {
                    val isMoreSelected = currentTab !in primaryBottomTabs
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                windowInsets = WindowInsets.navigationBars,
                                tonalElevation = 0.dp
                            ) {
                                primaryBottomTabs.forEach { tab ->
                                    val selected = currentTab == tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToTab(tab, "Bottom Bar") },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.bottomNavLabel(),
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }

                                NavigationBarItem(
                                    selected = isMoreSelected || showMoreNavSheet,
                                    onClick = { showMoreNavSheet = true },
                                    icon = {
                                        val showMoreInviteBadge = pendingInvitesForMe.isNotEmpty() && OpsTab.TEAM !in primaryBottomTabs
                                        if (showMoreInviteBadge) {
                                            BadgedBox(
                                                badge = {
                                                    Badge {
                                                        Text(pendingInvitesForMe.size.toString())
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (isMoreSelected) {
                                                        currentTab.selectedIcon
                                                    } else if (showMoreNavSheet) {
                                                        Icons.Filled.Apps
                                                    } else {
                                                        Icons.Outlined.Apps
                                                    },
                                                    contentDescription = "More tools and views",
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = if (isMoreSelected) {
                                                    currentTab.selectedIcon
                                                } else if (showMoreNavSheet) {
                                                    Icons.Filled.Apps
                                                } else {
                                                    Icons.Outlined.Apps
                                                },
                                                contentDescription = "More tools and views",
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = if (isMoreSelected) currentTab.bottomNavLabel() else "More",
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = if (isMoreSelected || showMoreNavSheet) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("nav_more")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useDesktopSidebar) {
                    Surface(
                        modifier = Modifier
                            .width(228.dp)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Computer,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "DESKTOP MODE",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                OpsTab.entries.forEachIndexed { index, tab ->
                                    val selected = currentTab == tab
                                    Surface(
                                        onClick = { navigateToTab(tab, "Desktop Sidebar") },
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            Color.Transparent
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 48.dp)
                                            .desktopPointerHand()
                                            .testTag(tab.testTag)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.label,
                                                    tint = if (selected) {
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = tab.label,
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = if (selected) {
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    },
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = when {
                                                    index == 9 -> "Alt+0"
                                                    index < 9 -> "Alt+${index + 1}"
                                                    tab == OpsTab.FINISHED_GOODS -> "FIN"
                                                    tab == OpsTab.MACRO_INVENTORY -> "MAC"
                                                    tab == OpsTab.MICRO_INVENTORY -> "MIC"
                                                    tab == OpsTab.PRODUCTION_MONITOR -> "OUT"
                                                    tab == OpsTab.REPROCESS -> "REP"
                                                    tab == OpsTab.TEAM -> if (pendingInvitesForMe.isNotEmpty()) "${pendingInvitesForMe.size} NEW" else "TM"
                                                    else -> "BOM"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                OutlinedButton(
                                    onClick = { showShortcutsDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .desktopPointerHand()
                                        .testTag("sidebar_shortcuts_button")
                                ) {
                                    Icon(Icons.Filled.Keyboard, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Shortcuts (F1)", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                } else if (useRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OpsTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab, "Navigation Rail") },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier
                                        .desktopPointerHand()
                                        .testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 1520.dp)
                            .fillMaxSize()
                    ) {
                        if (pendingInvitesForMe.isNotEmpty() && currentTab != OpsTab.TEAM) {
                            val firstInvite = pendingInvitesForMe.first()
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MailOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Workspace Invite (${firstInvite.resolvedRole().label})",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "From ${firstInvite.invitedByEmail.ifBlank { firstInvite.ownerEmail }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Button(
                                        onClick = { onAcceptInvite(firstInvite) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("banner_accept_invite_button")
                                    ) {
                                        Text("Accept")
                                    }
                                    TextButton(
                                        onClick = { navigateToTab(OpsTab.TEAM, "Invite Banner") },
                                        modifier = Modifier.testTag("banner_view_invites_button")
                                    ) {
                                        Text("View")
                                    }
                                }
                            }
                        }

                        if (!userRole.canWriteOperations && currentTab != OpsTab.TEAM) {
                            Surface(
                                color = GrainAmber.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, GrainAmber.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Visibility,
                                        contentDescription = null,
                                        tint = GrainAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Viewer Mode (Read-Only) • Live sync active; creating, editing & deleting are disabled.",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    RoleBadgeChip(role = userRole)
                                }
                            }
                        }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                    when (val s = opsState) {
                        OpsUiState.Loading -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    "Syncing Bulacan Plant Firestore...",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }
                        }

                        is OpsUiState.Error -> {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = s.message,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Button(onClick = viewModel::retryInitialSync) {
                                        Text("Retry Sync")
                                    }
                                }
                            }
                        }

                        is OpsUiState.Ready -> {
                            key(currentTab, tabResetTicks[currentTab] ?: 0) {
                                when (currentTab) {
                                OpsTab.SUMMARY -> SummaryScreen(
                                    state = s,
                                    onNavigateToTab = { target -> navigateToTab(target, "Summary Quick Card") },
                                    onNavigateToRawMaterials = {
                                        monitorInitialView = 1
                                        navigateToTab(OpsTab.MONITOR, "Summary Raw Materials Alert")
                                    },
                                    buildReport = viewModel::buildReport,
                                    onShowToast = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )

                                OpsTab.DAILY_LOG -> DailyLogScreen(
                                    state = s,
                                    onOpenDispatchForSku = { sku ->
                                        guardWriteOperation("log dispatches") {
                                            dispatchDialog = DispatchDialogState(isOpen = true, initialSku = sku)
                                            viewModel.recordUiBreadcrumb(
                                                category = BreadcrumbCategory.DISPATCH,
                                                action = "Opened Dispatch Form",
                                                details = "Pre-selected SKU ${sku.productName}",
                                                screenTab = OpsTab.DAILY_LOG.label,
                                                entityRef = sku.productName
                                            )
                                        }
                                    },
                                    onOpenOrdersTab = { navigateToTab(OpsTab.ORDERS, "Daily Log Action") },
                                    onShowToast = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )

                                OpsTab.MONITOR -> MonitorScreen(
                                    state = s,
                                    lookbackDays = lookbackDays,
                                    safetyBufferPct = safetyBufferPct,
                                    initialMonitorView = monitorInitialView,
                                    onConsumeDeepLink = { monitorInitialView = 0 },
                                    demandMultiplier = rawDemandMultiplier,
                                    includeScheduled = rawIncludeScheduled,
                                    leadOverrideDays = rawLeadOverrideDays,
                                    hasNotificationPermission = hasNotificationPermission,
                                    onRequestNotificationPermission = requestNotificationPermission,
                                    onSendNotificationNow = {
                                        val sent = StockAlertNotifier.notifyLowRawMaterials(
                                            context = context,
                                            projections = s.rawMaterialProjections,
                                            isManualTrigger = true
                                        )
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (sent) "Low-stock summary sent to phone notifications."
                                                else "Enable phone notifications to receive alerts."
                                            )
                                        }
                                    },
                                    onChangeLookback = viewModel::setLookbackDays,
                                    onChangeSafetyBuffer = viewModel::setSafetyBufferPct,
                                    onChangeDemandMultiplier = viewModel::setRawMaterialDemandMultiplier,
                                    onToggleIncludeScheduled = viewModel::setRawMaterialIncludeScheduled,
                                    onChangeLeadOverrideDays = viewModel::setRawMaterialLeadOverrideDays,
                                    onResetSimulator = viewModel::resetRawMaterialSimulator,
                                    onResetFilters = viewModel::resetMonitorParameters,
                                    onAddSku = {
                                        guardWriteOperation("add SKUs") {
                                            showSkuEditor = true to null
                                            viewModel.recordUiBreadcrumb(
                                                category = BreadcrumbCategory.INVENTORY,
                                                action = "Opened Add SKU Dialog",
                                                screenTab = OpsTab.MONITOR.label
                                            )
                                        }
                                    },
                                    onEditSku = { sku ->
                                        guardWriteOperation("edit SKUs") {
                                            showSkuEditor = true to sku
                                            viewModel.recordUiBreadcrumb(
                                                category = BreadcrumbCategory.INVENTORY,
                                                action = "Opened Edit SKU Dialog (${sku.productName})",
                                                screenTab = OpsTab.MONITOR.label,
                                                entityRef = sku.productName
                                            )
                                        }
                                    },
                                    onManageSkus = {
                                        guardWriteOperation("manage SKUs") {
                                            showManageSkus = true
                                            viewModel.recordUiBreadcrumb(
                                                category = BreadcrumbCategory.INVENTORY,
                                                action = "Opened SKU Catalog Manager",
                                                screenTab = OpsTab.MONITOR.label
                                            )
                                        }
                                    },
                                    onOpenDispatchDialog = { sku ->
                                        guardWriteOperation("log dispatches") {
                                            dispatchDialog = DispatchDialogState(isOpen = true, initialSku = sku)
                                            val skuName = sku?.productName ?: "Custom SKU"
                                            viewModel.recordUiBreadcrumb(
                                                category = BreadcrumbCategory.DISPATCH,
                                                action = "Opened Dispatch Form",
                                                details = "From Monitor for $skuName",
                                                screenTab = OpsTab.MONITOR.label,
                                                entityRef = skuName
                                            )
                                        }
                                    },
                                    onOpenScheduleDialog = { sku, bags, priority ->
                                        guardWriteOperation("schedule production runs") {
                                            showScheduleDialog = ScheduleRequest(sku, bags, priority, null)
                                            viewModel.recordUiBreadcrumb(
                                                category = BreadcrumbCategory.SCHEDULE,
                                                action = "Opened Production Schedule Form",
                                                details = "From Monitor: ${sku.productName} ($bags bags)",
                                                screenTab = OpsTab.MONITOR.label,
                                                entityRef = sku.productName
                                            )
                                        }
                                    },
                                    priorityProductKeys = effectivePriorityKeys,
                                    hideNonPriority = priorityProductPrefs.hideNonPriority,
                                    onToggleHideNonPriority = onToggleHideNonPriority,
                                    onToggleProductPriority = onToggleProductPriority,
                                    onSavePriorityProductKeys = onSavePriorityProductKeys,
                                    onResetPriorityToAutoActive = onResetPriorityToAutoActive,
                                    onScheduleMultipleSkus = { pickedItems ->
                                        guardWriteOperation("schedule production runs") {
                                            val seeds = pickedItems.map { (sku, bags) -> ScheduleDraftSeed(sku, bags) }
                                            showScheduleDialog = ScheduleRequest(
                                                priority = SchedulePriority.BUFFER_TOP_UP,
                                                items = seeds
                                            )
                                        }
                                    },
                                    onRestockRawMaterial = viewModel::adjustRawMaterialStock,
                                    onToggleRawMaterialAlert = viewModel::toggleRawMaterialAlert,
                                    onSaveRawMaterial = viewModel::saveRawMaterial,
                                    onDeleteRawMaterial = viewModel::deleteRawMaterial,
                                    onAddRawMaterialsBulk = viewModel::addRawMaterialsBulk,
                                    onSaveBomWithNewMaterials = viewModel::saveBomWithNewMaterials,
                                    onDeleteBom = viewModel::deleteBom,
                                    onOpenFeedBomScreen = { navigateToTab(OpsTab.FEED_BOM, "Monitor BOM Shortcut") },
                                    onOpenFinishedGoodsTab = { navigateToTab(OpsTab.FINISHED_GOODS, "Monitor Finished Goods Shortcut") },
                                    onOpenMacroTab = { navigateToTab(OpsTab.MACRO_INVENTORY, "Monitor Macro Shortcut") },
                                    onOpenMicroTab = { navigateToTab(OpsTab.MICRO_INVENTORY, "Monitor Micro Shortcut") },
                                    onOpenProductionMonitor = { navigateToTab(OpsTab.PRODUCTION_MONITOR, "Monitor Charts Shortcut") }
                                )

                                OpsTab.ORDERS -> OrdersScreen(
                                    state = s,
                                    onAddOrder = {
                                        guardWriteOperation("create orders") {
                                            orderEditorState = OrderEditorRequest(open = true, initialRushOrder = false)
                                            viewModel.recordUiBreadcrumb(
                                                category = BreadcrumbCategory.ORDERS,
                                                action = "Opened New Order Form",
                                                screenTab = OpsTab.ORDERS.label
                                            )
                                        }
                                    },
                                    onAddRushOrder = {
                                        guardWriteOperation("create rush orders") {
                                            orderEditorState = OrderEditorRequest(open = true, initialRushOrder = true)
                                            viewModel.recordUiBreadcrumb(
                                                category = BreadcrumbCategory.ORDERS,
                                                action = "Opened New Rush Order Form",
                                                screenTab = OpsTab.ORDERS.label
                                            )
                                        }
                                    },
                                    onEditOrder = { ord ->
                                        guardWriteOperation("edit orders") {
                                            orderEditorState = OrderEditorRequest(
                                                open = true,
                                                existingOrder = ord,
                                                initialRushOrder = ord.rushOrder
                                            )
                                            viewModel.recordUiBreadcrumb(
                                                category = BreadcrumbCategory.ORDERS,
                                                action = "Opened Edit Order Form (${ord.orderCode})",
                                                screenTab = OpsTab.ORDERS.label,
                                                entityRef = ord.orderCode
                                            )
                                        }
                                    },
                                    onToggleOrderPriority = { ord ->
                                        viewModel.updateOrder(
                                            order = ord,
                                            dueDate = ord.dueDate,
                                            orderedBags = ord.orderedBags,
                                            bagWeightKg = ord.bagWeightKg,
                                            customer = ord.customer,
                                            priority = !ord.priority
                                        )
                                    },
                                    onCancelOrder = viewModel::cancelOrder,
                                    onRetrieveOrder = viewModel::retrieveCancelledOrder,
                                    onDeleteOrder = viewModel::deleteOrder,
                                    onManageCustomers = {
                                        guardWriteOperation("manage customers") {
                                            showManageCustomers = true
                                            viewModel.recordUiBreadcrumb(
                                                category = BreadcrumbCategory.CUSTOMERS,
                                                action = "Opened Customer Directory Manager",
                                                screenTab = OpsTab.ORDERS.label
                                            )
                                        }
                                    },
                                    onScheduleProductionForOrder = { ord, sku, bags, priority ->
                                        guardWriteOperation("schedule production") {
                                            showScheduleDialog = ScheduleRequest(sku, bags, priority, ord)
                                            viewModel.recordUiBreadcrumb(
                                                category = BreadcrumbCategory.SCHEDULE,
                                                action = "Opened Schedule Form for Order ${ord.orderCode}",
                                                details = "${ord.productName} ($bags bags)",
                                                screenTab = OpsTab.ORDERS.label,
                                                entityRef = ord.orderCode
                                            )
                                        }
                                    },
                                    onScheduleMultipleOrders = { selectedOrders ->
                                        guardWriteOperation("schedule production") {
                                            val (prio, seeds) = buildGroupedScheduleSeeds(selectedOrders, s.skus, s.summaries)
                                            showScheduleDialog = ScheduleRequest(
                                                priority = prio,
                                                items = seeds
                                            )
                                            viewModel.recordUiBreadcrumb(
                                                category = BreadcrumbCategory.SCHEDULE,
                                                action = "Opened Batch Schedule for ${selectedOrders.size} Order(s)",
                                                details = selectedOrders.joinToString(", ") { it.orderCode },
                                                screenTab = OpsTab.ORDERS.label
                                            )
                                        }
                                    },
                                    onDispatchMultipleOrders = { selectedOrders ->
                                        guardWriteOperation("log dispatches") {
                                            dispatchDialog = DispatchDialogState(isOpen = true, initialOrders = selectedOrders)
                                            viewModel.recordUiBreadcrumb(
                                                category = BreadcrumbCategory.DISPATCH,
                                                action = "Opened Batch Dispatch for ${selectedOrders.size} Order(s)",
                                                details = selectedOrders.joinToString(", ") { it.orderCode },
                                                screenTab = OpsTab.ORDERS.label
                                            )
                                        }
                                    },
                                    onFulfillOrderViaDispatch = { ord ->
                                        guardWriteOperation("log dispatches") {
                                            dispatchDialog = DispatchDialogState(isOpen = true, initialOrder = ord)
                                            viewModel.recordUiBreadcrumb(
                                                category = BreadcrumbCategory.DISPATCH,
                                                action = "Opened Fulfill Dispatch (${ord.orderCode})",
                                                screenTab = OpsTab.ORDERS.label,
                                                entityRef = ord.orderCode
                                            )
                                        }
                                    },
                                    onNavigateToSchedule = { navigateToTab(OpsTab.SCHEDULE, "Orders Action") }
                                )

                                OpsTab.SCHEDULE -> ScheduleScreen(
                                    state = s,
                                    onOpenOrdersTab = { navigateToTab(OpsTab.ORDERS, "Schedule Action") },
                                    onOpenMultiSchedule = { pickedItems ->
                                        guardWriteOperation("schedule production") {
                                            val seeds = pickedItems.map { (sku, bags) -> ScheduleDraftSeed(sku, bags) }
                                            showScheduleDialog = ScheduleRequest(
                                                priority = SchedulePriority.ORDER_DRIVEN,
                                                items = seeds
                                            )
                                            viewModel.recordUiBreadcrumb(
                                                category = BreadcrumbCategory.SCHEDULE,
                                                action = "Opened Multi-SKU Schedule Form",
                                                details = "${pickedItems.size} selected SKU(s)",
                                                screenTab = OpsTab.SCHEDULE.label
                                            )
                                        }
                                    },
                                    onScheduleSku = { sku, bags, priority ->
                                        guardWriteOperation("schedule production") {
                                            showScheduleDialog = ScheduleRequest(sku, bags, priority, null)
                                        }
                                    },
                                    onScheduleSelectedOrders = { selectedOrders ->
                                        guardWriteOperation("schedule production") {
                                            val (prio, seeds) = buildGroupedScheduleSeeds(selectedOrders, s.skus, s.summaries)
                                            showScheduleDialog = ScheduleRequest(
                                                priority = prio,
                                                items = seeds
                                            )
                                        }
                                    },
                                    onAdvanceScheduleStatus = viewModel::advanceScheduleStatus,
                                    onStartMultipleRuns = viewModel::startMultipleScheduleRuns,
                                    onEditSchedule = { run ->
                                        guardWriteOperation("edit scheduled runs") {
                                            editingScheduleRun = run
                                        }
                                    },
                                    onUpdateScheduleLinks = { run, pickedOrders ->
                                        viewModel.updateScheduleRun(
                                            run = run,
                                            scheduledDate = run.scheduledDate,
                                            targetBags = run.targetBags,
                                            bagWeightKg = run.bagWeightKg,
                                            lineRemarks = run.lineRemarks,
                                            priority = SchedulePriority.fromRaw(run.priority),
                                            linkedOrders = pickedOrders
                                        )
                                    },
                                    onDeleteSchedule = viewModel::deleteScheduleRun,
                                    onCancelSchedule = { run ->
                                        viewModel.advanceScheduleStatus(run, ScheduleStatus.CANCELLED)
                                    },
                                    onCompleteScheduleWithBatch = { run ->
                                        guardWriteOperation("complete scheduled production") {
                                            showBatchDialog = BatchDialogState(isOpen = true, fromRun = run)
                                        }
                                    },
                                    onEditRecoveredBatch = { batch ->
                                        guardWriteOperation("edit production batches") {
                                            showBatchDialog = BatchDialogState(isOpen = true, existingBatch = batch)
                                        }
                                    }
                                )

                                OpsTab.DISPATCHED -> {
                                    val skuSectionByName = remember(s.skus) {
                                        s.skus.associate { it.productName.trim().lowercase() to it.section }
                                    }
                                    DispatchedScreen(
                                        state = s,
                                        preselectedPlant = preselectedDispatchPlant,
                                        onPreselectPlant = onSavePreselectedDispatchPlant,
                                        skuSectionByProductName = skuSectionByName,
                                        onLogDispatch = {
                                            guardWriteOperation("log dispatches") {
                                                dispatchDialog = DispatchDialogState(isOpen = true)
                                                viewModel.recordUiBreadcrumb(
                                                    category = BreadcrumbCategory.DISPATCH,
                                                    action = "Opened Log Dispatch Form",
                                                    screenTab = OpsTab.DISPATCHED.label
                                                )
                                            }
                                        },
                                        onEditDispatch = { rec ->
                                            guardWriteOperation("edit dispatches") {
                                                dispatchDialog = DispatchDialogState(isOpen = true, existingDispatch = rec)
                                            }
                                        },
                                        onDeleteDispatch = viewModel::deleteDispatch,
                                        onAddReturnToReprocess = viewModel::createReprocessFromDispatch,
                                        onOpenReprocessTab = {
                                            navigateToTab(OpsTab.REPROCESS, "Dispatched Return Shortcut")
                                        }
                                    )
                                }

                                OpsTab.RECOVERY -> RecoveryScreen(
                                    state = s,
                                    onOpenLogBatch = {
                                        guardWriteOperation("log production batches") {
                                            showBatchDialog = BatchDialogState(isOpen = true)
                                            viewModel.recordUiBreadcrumb(
                                                category = BreadcrumbCategory.RECOVERY,
                                                action = "Opened Log Production Batch Form",
                                                screenTab = OpsTab.RECOVERY.label
                                            )
                                        }
                                    },
                                    onOpenBulkPaste = {
                                        guardWriteOperation("bulk import batches") {
                                            showBulkPasteDialog = true
                                            viewModel.recordUiBreadcrumb(
                                                category = BreadcrumbCategory.RECOVERY,
                                                action = "Opened Bulk Paste Batch Importer",
                                                screenTab = OpsTab.RECOVERY.label
                                            )
                                        }
                                    },
                                    onEditBatch = { batch ->
                                        guardWriteOperation("edit production batches") {
                                            showBatchDialog = BatchDialogState(isOpen = true, existingBatch = batch)
                                        }
                                    },
                                    onUpdateBatchLinks = viewModel::updateBatchLinkedOrders
                                )

                                OpsTab.REPORTS -> ReportsScreen(
                                    state = s,
                                    buildReport = viewModel::buildReport,
                                    exportJsonBackup = viewModel::exportJsonBackup,
                                    onRestoreJsonBackup = viewModel::restoreJsonBackup,
                                    onResetSeed = viewModel::resetOctober2026Seed,
                                    onOpenSelectiveReset = { showTopResetDialog = true },
                                    canManageBackupsAndResets = userRole.canResetWorkspace,
                                    onShowToast = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )

                                OpsTab.AI_OPS -> AiOpsScreen(
                                    insightState = aiInsightState,
                                    chatMessages = chatMessages,
                                    isChatSending = isChatSending,
                                    onRunPlantAnalysis = viewModel::runGeminiPlantAnalysis,
                                    onSendChatMessage = viewModel::sendChatMessage,
                                    onClearChat = viewModel::clearChatHistory
                                )

                                OpsTab.HISTORY -> HistoryScreen(
                                    breadcrumbs = breadcrumbs,
                                    sessionTrail = sessionTrail,
                                    onJumpToTabLabel = { label ->
                                        val matched = OpsTab.entries.firstOrNull {
                                            it.label.equals(label, ignoreCase = true)
                                        }
                                        if (matched != null) {
                                            navigateToTab(matched, "Session Trail Chip")
                                        }
                                    },
                                    onDeleteEntry = viewModel::deleteBreadcrumb,
                                    onClearHistory = viewModel::clearAllBreadcrumbs,
                                    onHistoryExported = { formats, count ->
                                        val formatNames = formats.joinToString(", ") { it.label }
                                        viewModel.recordUiBreadcrumb(
                                            category = BreadcrumbCategory.REPORTS,
                                            action = "Exported Activity History ($formatNames)",
                                            details = "Saved $count activity entry(ies) as $formatNames",
                                            screenTab = OpsTab.HISTORY.label,
                                            entityRef = formatNames
                                        )
                                    },
                                    onShowToast = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    },
                                    canManageHistory = userRole.canManageTeam
                                )

                                OpsTab.FINISHED_GOODS -> FinishedGoodsInventoryScreen(
                                    state = s,
                                    priorityProductKeys = effectivePriorityKeys,
                                    hideNonPriority = priorityProductPrefs.hideNonPriority,
                                    onToggleHideNonPriority = onToggleHideNonPriority,
                                    onToggleProductPriority = onToggleProductPriority,
                                    onSavePriorityProductKeys = onSavePriorityProductKeys,
                                    onResetPriorityToAutoActive = onResetPriorityToAutoActive,
                                    onScheduleMultipleSkus = { pickedItems ->
                                        guardWriteOperation("schedule production") {
                                            val seeds = pickedItems.map { (sku, bags) -> ScheduleDraftSeed(sku, bags) }
                                            showScheduleDialog = ScheduleRequest(
                                                priority = SchedulePriority.BUFFER_TOP_UP,
                                                items = seeds
                                            )
                                            viewModel.recordUiBreadcrumb(
                                                category = BreadcrumbCategory.SCHEDULE,
                                                action = "Opened Batch Schedule from Finished Goods",
                                                details = "${pickedItems.size} selected SKU(s)",
                                                screenTab = OpsTab.FINISHED_GOODS.label
                                            )
                                        }
                                    },
                                    onAddSku = {
                                        guardWriteOperation("add SKUs") {
                                            showSkuEditor = true to null
                                            viewModel.recordUiBreadcrumb(
                                                category = BreadcrumbCategory.INVENTORY,
                                                action = "Opened Add Finished Goods SKU Dialog",
                                                screenTab = OpsTab.FINISHED_GOODS.label
                                            )
                                        }
                                    },
                                    onEditSku = { sku ->
                                        guardWriteOperation("edit SKUs") {
                                            showSkuEditor = true to sku
                                            viewModel.recordUiBreadcrumb(
                                                category = BreadcrumbCategory.INVENTORY,
                                                action = "Opened Edit SKU Dialog (${sku.productName})",
                                                screenTab = OpsTab.FINISHED_GOODS.label,
                                                entityRef = sku.productName
                                            )
                                        }
                                    },
                                    onManageSkus = {
                                        guardWriteOperation("manage SKUs") {
                                            showManageSkus = true
                                        }
                                    },
                                    onOpenDispatchDialog = { sku ->
                                        guardWriteOperation("log dispatches") {
                                            dispatchDialog = DispatchDialogState(isOpen = true, initialSku = sku)
                                        }
                                    },
                                    onOpenScheduleDialog = { sku, bags, priority ->
                                        guardWriteOperation("schedule production") {
                                            showScheduleDialog = ScheduleRequest(sku, bags, priority, null)
                                        }
                                    },
                                    onOpenReprocessTab = {
                                        navigateToTab(OpsTab.REPROCESS, "Finished Goods Reprocess Alert")
                                    },
                                    localInventoryState = localInventoryUiState,
                                    onLocalSearchChange = feedInventoryViewModel::setSearchQuery,
                                    onLocalUnitFilterChange = feedInventoryViewModel::setUnitFilter,
                                    onLocalLowStockOnlyChange = feedInventoryViewModel::setLowStockOnly,
                                    onLocalSortOptionChange = feedInventoryViewModel::setSortOption,
                                    onLocalClearFilters = feedInventoryViewModel::clearFilters,
                                    onLocalSaveProduct = { existingId, name, qty, unit, category, section, threshold, notes ->
                                        guardWriteOperation("update finished goods ledger") {
                                            feedInventoryViewModel.saveFeedInventoryItem(
                                                productName = name,
                                                stockQuantity = qty,
                                                unitOfMeasurement = unit,
                                                category = category,
                                                section = section,
                                                reorderThreshold = threshold,
                                                notes = notes,
                                                existingId = existingId
                                            )
                                        }
                                    },
                                    onLocalAdjustStock = { id, delta ->
                                        guardWriteOperation("adjust finished goods stock") {
                                            feedInventoryViewModel.adjustStock(id, delta)
                                        }
                                    },
                                    onLocalDeleteItem = { item ->
                                        guardWriteOperation("delete finished goods ledger items") {
                                            feedInventoryViewModel.deleteItem(item)
                                        }
                                    }
                                )

                                OpsTab.MACRO_INVENTORY -> MaterialsInventoryScreen(
                                    state = s,
                                    isMicroInventory = false,
                                    demandMultiplier = rawDemandMultiplier,
                                    hasNotificationPermission = hasNotificationPermission,
                                    onRequestNotificationPermission = requestNotificationPermission,
                                    onSendNotificationNow = {
                                        val sent = StockAlertNotifier.notifyLowRawMaterials(
                                            context = context,
                                            projections = s.rawMaterialProjections,
                                            isManualTrigger = true
                                        )
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (sent) "Low-stock summary sent to phone notifications."
                                                else "Enable phone notifications to receive alerts."
                                            )
                                        }
                                    },
                                    onRestockRawMaterial = viewModel::adjustRawMaterialStock,
                                    onToggleRawMaterialAlert = viewModel::toggleRawMaterialAlert,
                                    onSaveRawMaterial = viewModel::saveRawMaterial,
                                    onDeleteRawMaterial = viewModel::deleteRawMaterial,
                                    onAddRawMaterialsBulk = viewModel::addRawMaterialsBulk,
                                    onOpenBomScreen = { navigateToTab(OpsTab.FEED_BOM, "Macro Inventory BOM Shortcut") },
                                    onSwitchToOtherInventoryTab = { navigateToTab(OpsTab.MICRO_INVENTORY, "Macro Inventory Switch") }
                                )

                                OpsTab.MICRO_INVENTORY -> MaterialsInventoryScreen(
                                    state = s,
                                    isMicroInventory = true,
                                    demandMultiplier = rawDemandMultiplier,
                                    hasNotificationPermission = hasNotificationPermission,
                                    onRequestNotificationPermission = requestNotificationPermission,
                                    onSendNotificationNow = {
                                        val sent = StockAlertNotifier.notifyLowRawMaterials(
                                            context = context,
                                            projections = s.rawMaterialProjections,
                                            isManualTrigger = true
                                        )
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (sent) "Low-stock summary sent to phone notifications."
                                                else "Enable phone notifications to receive alerts."
                                            )
                                        }
                                    },
                                    onRestockRawMaterial = viewModel::adjustRawMaterialStock,
                                    onToggleRawMaterialAlert = viewModel::toggleRawMaterialAlert,
                                    onSaveRawMaterial = viewModel::saveRawMaterial,
                                    onDeleteRawMaterial = viewModel::deleteRawMaterial,
                                    onAddRawMaterialsBulk = viewModel::addRawMaterialsBulk,
                                    onOpenBomScreen = { navigateToTab(OpsTab.FEED_BOM, "Micro Inventory BOM Shortcut") },
                                    onSwitchToOtherInventoryTab = { navigateToTab(OpsTab.MACRO_INVENTORY, "Micro Inventory Switch") }
                                )

                                OpsTab.FEED_BOM -> FeedBomScreen(
                                    state = s,
                                    onSaveBomWithNewMaterials = viewModel::saveBomWithNewMaterials,
                                    onDeleteBom = viewModel::deleteBom,
                                    onAdjustRawMaterialStock = viewModel::adjustRawMaterialStock,
                                    onSaveRawMaterial = viewModel::saveRawMaterial,
                                    onDeleteRawMaterial = viewModel::deleteRawMaterial
                                )

                                OpsTab.PRODUCTION_MONITOR -> ProductionMonitorScreen(
                                    state = s,
                                    onOpenLogBatch = {
                                        guardWriteOperation("log production batches") {
                                            showBatchDialog = BatchDialogState(isOpen = true)
                                            viewModel.recordUiBreadcrumb(
                                                category = BreadcrumbCategory.RECOVERY,
                                                action = "Opened Log Production Batch Form",
                                                screenTab = OpsTab.PRODUCTION_MONITOR.label
                                            )
                                        }
                                    },
                                    onOpenScheduleScreen = {
                                        navigateToTab(OpsTab.SCHEDULE, "Production Monitor Schedule Action")
                                    }
                                )

                                OpsTab.REPROCESS -> ReprocessScreen(
                                    state = s,
                                    onAddReprocessItem = viewModel::addReprocessItem,
                                    onUpdateReprocessItem = viewModel::updateReprocessItem,
                                    onQueueReturnedDispatch = viewModel::createReprocessFromDispatch,
                                    onAdvanceStatus = { item, newStatus ->
                                        viewModel.advanceReprocessStatus(item, newStatus)
                                    },
                                    onToggleFastAction = viewModel::toggleReprocessFastAction,
                                    onFlagToInventory = viewModel::flagReprocessToInventory,
                                    onMarkDamaged = viewModel::markReprocessAsDamaged,
                                    onDeleteReprocessItem = viewModel::deleteReprocessItem,
                                    onOpenFinishedGoodsTab = {
                                        navigateToTab(OpsTab.FINISHED_GOODS, "Reprocess Inventory Shortcut")
                                    }
                                )

                                OpsTab.TEAM -> TeamAccessScreen(
                                    signedInUid = signedInUid,
                                    signedInEmail = userEmail,
                                    activeWorkspaceOwnerId = currentUserId,
                                    activeRole = userRole,
                                    isWorkspaceOwner = viewModel.isWorkspaceOwner,
                                    availableWorkspaces = availableWorkspaces,
                                    pendingInvitesForMe = pendingInvitesForMe,
                                    teamMembers = teamMembers,
                                    workspaceInvites = workspaceInvites,
                                    onSwitchWorkspace = onSwitchWorkspace,
                                    onAcceptInvite = onAcceptInvite,
                                    onDeclineInvite = onDeclineInvite,
                                    onLeaveActiveSharedWorkspace = onLeaveActiveSharedWorkspace,
                                    onInviteMember = { email, displayName, role, onSaved ->
                                        viewModel.inviteTeamMember(email, displayName, role, resolvedOwnerEmail, onSaved)
                                    },
                                    onUpdateMemberRole = viewModel::updateTeamMemberRole,
                                    onRemoveMember = viewModel::removeTeamMember,
                                    onShowMessage = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                                }
                            }

                            val existingCustomerNames = remember(s.orders, s.dispatches) {
                                (s.orders.map { it.customer } + s.dispatches.map { it.customer })
                                    .filter { it.isNotBlank() }
                                    .distinct()
                            }

                            // Dialogs
                            if (dispatchDialog.isOpen) {
                                val initSku = dispatchDialog.initialSku
                                val initOrder = dispatchDialog.initialOrder
                                val initOrders = dispatchDialog.initialOrders
                                val existing = dispatchDialog.existingDispatch
                                LogDispatchDialog(
                                    skus = s.skus,
                                    summaries = s.summaries,
                                    openOrders = s.orders.filter {
                                        it.status == OrderStatus.OPEN.label || it.status == OrderStatus.PARTIALLY_DISPATCHED.label
                                    },
                                    customers = s.customers,
                                    existingCustomers = existingCustomerNames,
                                    initialSku = initSku,
                                    initialSkuName = initSku?.productName,
                                    initialOrder = initOrder,
                                    initialOrders = initOrders,
                                    existingDispatch = existing,
                                    priorityProductKeys = effectivePriorityKeys,
                                    restrictToSelectedProducts = true,
                                    preselectedPlant = preselectedDispatchPlant,
                                    onPreselectPlant = onSavePreselectedDispatchPlant,
                                    onDismiss = { dispatchDialog = DispatchDialogState() },
                                    onManageCustomers = { showManageCustomers = true },
                                    onAddNewProduction = { sku, neededBags, linkedOrd ->
                                        dispatchDialog = DispatchDialogState()
                                        showScheduleDialog = ScheduleRequest(
                                            sku = sku,
                                            bags = neededBags.coerceAtLeast(1L),
                                            priority = SchedulePriority.URGENT,
                                            linkedOrder = linkedOrd
                                        )
                                    },
                                    onAddMultipleProduction = { seeds ->
                                        dispatchDialog = DispatchDialogState()
                                        showScheduleDialog = ScheduleRequest(
                                            priority = SchedulePriority.URGENT,
                                            items = seeds
                                        )
                                    },
                                    onSubmitMultiple = { entries ->
                                        viewModel.logMultipleDispatches(entries)
                                        dispatchDialog = DispatchDialogState()
                                    },
                                    onSubmit = { entry ->
                                        if (existing != null) {
                                            viewModel.updateDispatch(existing, entry)
                                        } else {
                                            viewModel.logDispatch(entry)
                                        }
                                        dispatchDialog = DispatchDialogState()
                                    }
                                )
                            }

                            if (orderEditorState.open) {
                                val existingOrd = orderEditorState.existingOrder
                                OrderEditorDialog(
                                    skus = s.skus,
                                    summaries = s.summaries,
                                    customers = s.customers,
                                    existingCustomers = existingCustomerNames,
                                    existingOrder = existingOrd,
                                    initialRushOrder = orderEditorState.initialRushOrder,
                                    onDismiss = { orderEditorState = OrderEditorRequest() },
                                    onManageCustomers = { showManageCustomers = true },
                                    onSubmitMultiple = { code, oDate, dDate, cust, items, priority, alsoSchedule, rushOrder ->
                                        viewModel.addMultipleOrders(code, oDate, dDate, cust, items, priority, alsoSchedule, rushOrder)
                                        orderEditorState = OrderEditorRequest()
                                    },
                                    onSubmit = { code, oDate, dDate, prod, bags, wt, cust, priority, rushOrder ->
                                        if (existingOrd == null) {
                                            viewModel.addOrder(code, oDate, dDate, prod, bags, wt, cust, priority, rushOrder)
                                        } else {
                                            viewModel.updateOrder(existingOrd, dDate, bags, wt, cust, priority, rushOrder)
                                        }
                                        orderEditorState = OrderEditorRequest()
                                    }
                                )
                            }

                            showScheduleDialog?.let { req ->
                                ScheduleRunDialog(
                                    skus = s.skus,
                                    summaries = s.summaries,
                                    openOrders = s.orders.filter {
                                        it.status == OrderStatus.OPEN.label || it.status == OrderStatus.PARTIALLY_DISPATCHED.label
                                    },
                                    initialSku = req.sku,
                                    initialBags = req.bags,
                                    initialItems = req.items,
                                    initialPriority = req.priority,
                                    linkedOrder = req.linkedOrder,
                                    onDismiss = { showScheduleDialog = null },
                                    onSubmitMultiple = { date, pr, items ->
                                        viewModel.scheduleMultipleProductionRuns(date, pr, items)
                                        showScheduleDialog = null
                                    },
                                    onSubmit = { date, sec, prod, targetBags, bagWt, rem, pr, ordId, ordCode ->
                                        viewModel.scheduleProductionRun(
                                            date, sec, prod, targetBags, bagWt, rem, pr, ordId, ordCode
                                        )
                                        showScheduleDialog = null
                                    }
                                )
                            }

                            editingScheduleRun?.let { runToEdit ->
                                com.example.ui.components.EditScheduleRunDialog(
                                    run = runToEdit,
                                    allOrders = s.orders,
                                    onDismiss = { editingScheduleRun = null },
                                    onSubmit = { date, targetBags, bagWt, rem, pr, pickedOrders ->
                                        viewModel.updateScheduleRun(runToEdit, date, targetBags, bagWt, rem, pr, pickedOrders)
                                        editingScheduleRun = null
                                    }
                                )
                            }

                            if (showBatchDialog.isOpen) {
                                val fromRun = showBatchDialog.fromRun
                                val existingBatch = showBatchDialog.existingBatch
                                LogBatchDialog(
                                    skus = s.skus,
                                    summaries = s.summaries,
                                    orders = s.orders,
                                    schedules = s.schedules,
                                    fromScheduleRun = fromRun,
                                    existingBatch = existingBatch,
                                    onDismiss = { showBatchDialog = BatchDialogState() },
                                    onSubmit = { date, sec, prod, bags, wt, actualKg, rem, pickedOrders ->
                                        when {
                                            existingBatch != null -> {
                                                viewModel.updateProductionBatch(
                                                    existingBatch,
                                                    date,
                                                    sec,
                                                    prod,
                                                    bags,
                                                    wt,
                                                    actualKg,
                                                    rem,
                                                    pickedOrders
                                                )
                                            }
                                            fromRun != null -> {
                                                viewModel.completeScheduleWithBatch(
                                                    fromRun,
                                                    date,
                                                    bags,
                                                    wt,
                                                    actualKg,
                                                    rem,
                                                    pickedOrders
                                                )
                                            }
                                            else -> {
                                                viewModel.logProductionBatch(
                                                    date,
                                                    sec,
                                                    prod,
                                                    bags,
                                                    wt,
                                                    actualKg,
                                                    rem,
                                                    pickedOrders
                                                )
                                            }
                                        }
                                        showBatchDialog = BatchDialogState()
                                    }
                                )
                            }

                            if (showBulkPasteDialog) {
                                BulkPasteDialog(
                                    onDismiss = { showBulkPasteDialog = false },
                                    onImport = { text, defaultSec ->
                                        viewModel.importBulkPasteRows(text, defaultSec)
                                        showBulkPasteDialog = false
                                    }
                                )
                            }

                            if (showSkuEditor.first) {
                                val existingSku = showSkuEditor.second
                                SkuEditorDialog(
                                    existingSku = existingSku,
                                    onDismiss = { showSkuEditor = false to null },
                                    onSubmit = { prod, sec, bags, wt, rec, buf, rem ->
                                        viewModel.saveSku(existingSku, prod, sec, bags, wt, rec, buf, rem)
                                        if (existingSku == null) {
                                            val newKey = TabPreferencesStore.skuPriorityKey(sec.label, prod)
                                            scope.launch {
                                                TabPreferencesStore.savePriorityProductKeys(
                                                    context,
                                                    effectivePriorityKeys + newKey
                                                )
                                            }
                                        }
                                        showSkuEditor = false to null
                                    }
                                )
                            }

                            if (showManageSkus) {
                                ManageSkusDialog(
                                    skus = s.skus,
                                    canDeleteSkus = userRole.canDeleteSkus,
                                    onDismiss = { showManageSkus = false },
                                    onAddNew = { showSkuEditor = true to null },
                                    onEditSku = { sku -> showSkuEditor = true to sku },
                                    onDeleteSku = viewModel::deleteSku
                                )
                            }

                            if (showManageCustomers) {
                                ManageCustomersDialog(
                                    customers = s.customers,
                                    onDismiss = { showManageCustomers = false },
                                    onSaveCustomer = viewModel::saveCustomer,
                                    onDeleteCustomer = viewModel::deleteCustomer
                                )
                            }
                        }
                    }
                    }
                }
            }
        }
        }
    }

    if (showMoreNavSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val moreGroups = listOf(
            "Inventory & Formulas" to listOf(
                OpsTab.FINISHED_GOODS,
                OpsTab.MACRO_INVENTORY,
                OpsTab.MICRO_INVENTORY,
                OpsTab.FEED_BOM
            ),
            "Core Operations" to listOf(
                OpsTab.SUMMARY,
                OpsTab.MONITOR,
                OpsTab.ORDERS,
                OpsTab.DISPATCHED,
                OpsTab.REPROCESS
            ),
            "Production & Daily Tracking" to listOf(
                OpsTab.PRODUCTION_MONITOR,
                OpsTab.SCHEDULE,
                OpsTab.RECOVERY,
                OpsTab.DAILY_LOG,
                OpsTab.HISTORY
            ),
            "Team, Insights & Exports" to listOf(OpsTab.TEAM, OpsTab.REPORTS, OpsTab.AI_OPS)
        )
        ModalBottomSheet(
            onDismissRequest = { showMoreNavSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "All Tabs & Navigation",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Switch views or customize which tabs sit on your bottom bar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            showMoreNavSheet = false
                            showCustomizeTabsDialog = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("edit_bottom_tabs_button")
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Bar", maxLines = 1)
                    }
                }

                moreGroups.forEach { (groupTitle, tabs) ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = groupTitle,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        tabs.forEach { tab ->
                            val isSelected = currentTab == tab
                            val isPinnedOnBar = tab in primaryBottomTabs
                            Surface(
                                onClick = { navigateToTab(tab, "More Menu") },
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 56.dp)
                                    .desktopPointerHand()
                                    .testTag(tab.testTag)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                },
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = null,
                                            tint = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = tab.label,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (tab == OpsTab.TEAM && pendingInvitesForMe.isNotEmpty()) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "${pendingInvitesForMe.size} Invite",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            if (isPinnedOnBar) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "On Bar",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = tab.navDescription(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Current screen",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomizeTabsDialog) {
        CustomizeBottomTabsDialog(
            currentPinnedTabs = primaryBottomTabs,
            onDismiss = { showCustomizeTabsDialog = false },
            onSaveTabs = { updatedTabs ->
                scope.launch {
                    TabPreferencesStore.saveBottomTabNames(context, updatedTabs.map { it.name })
                    snackbarHostState.showSnackbar("Bottom navigation tabs updated.")
                }
                showCustomizeTabsDialog = false
            },
            onResetDefaults = {
                scope.launch {
                    TabPreferencesStore.resetBottomTabNames(context)
                    snackbarHostState.showSnackbar("Restored default bottom tabs.")
                }
                showCustomizeTabsDialog = false
            }
        )
    }

    if (showShortcutsDialog) {
        DesktopShortcutsDialog(onDismiss = { showShortcutsDialog = false })
    }

    if (showTopResetDialog) {
        SelectiveResetDialog(
            currentTab = currentTab,
            currentUserEmail = userEmail,
            onDismiss = { showTopResetDialog = false },
            onExecuteReset = { targets, adminSummary ->
                viewModel.recordUiBreadcrumb(
                    category = BreadcrumbCategory.SYSTEM,
                    action = "Admin-Verified Selective Reset",
                    details = adminSummary,
                    screenTab = currentTab.label
                )
                viewModel.resetSelectedScopes(
                    targets = targets,
                    screenTabLabel = currentTab.label,
                    onFiltersReset = {
                        tabResetTicks[currentTab] = (tabResetTicks[currentTab] ?: 0) + 1
                    }
                )
                showTopResetDialog = false
            },
            onFullBaselineReset = { adminSummary ->
                viewModel.recordUiBreadcrumb(
                    category = BreadcrumbCategory.SYSTEM,
                    action = "Admin-Verified Full Baseline Reset",
                    details = adminSummary,
                    screenTab = currentTab.label
                )
                viewModel.resetOctober2026Seed()
                tabResetTicks.clear()
                showTopResetDialog = false
            }
        )
    }
}
}

private fun buildGroupedScheduleSeeds(
    selectedOrders: List<CustomerOrder>,
    skus: List<SkuEntity>,
    summaries: List<SkuInventorySummary>
): Pair<SchedulePriority, List<ScheduleDraftSeed>> {
    if (selectedOrders.isEmpty()) {
        return SchedulePriority.ORDER_DRIVEN to emptyList()
    }
    val skuByProdLower = skus.associateBy { it.productName.trim().lowercase() }
    val sumByProdLower = summaries.associateBy { it.sku.productName.trim().lowercase() }

    val seeds = selectedOrders
        .groupBy { it.productName.trim().lowercase() }
        .values
        .map { groupOrders ->
            val firstOrder = groupOrders.first()
            val prodKey = firstOrder.productName.trim().lowercase()
            val summary = sumByProdLower[prodKey]
            val resolvedSku = summary?.sku ?: skuByProdLower[prodKey] ?: SkuEntity(
                productName = firstOrder.productName,
                section = PlantSection.SAN_ROQUE.label,
                standardBagWeightKg = firstOrder.bagWeightKg.coerceAtLeast(1.0),
                defaultRemarks = "MIXING/CONVEYOR"
            )
            val totalRemKg = groupOrders.sumOf { max(0.0, it.totalWeightKg - it.dispatchedWeightKg) }
            val availKg = max(0.0, summary?.availableStockKg ?: 0.0)
            val shortfallKg = max(0.0, totalRemKg - availKg)
            val targetKg = if (shortfallKg > 0.0) shortfallKg else totalRemKg
            val bagWt = resolvedSku.standardBagWeightKg.coerceAtLeast(1.0)
            val bags = ceil(targetKg / bagWt).toLong().coerceAtLeast(1L)
            ScheduleDraftSeed(
                sku = resolvedSku,
                bags = bags,
                linkedOrder = firstOrder,
                linkedOrders = groupOrders
            )
        }

    val hasUrgent = selectedOrders.any { ord ->
        val prodKey = ord.productName.trim().lowercase()
        val rem = max(0.0, ord.totalWeightKg - ord.dispatchedWeightKg)
        val avail = max(0.0, sumByProdLower[prodKey]?.availableStockKg ?: 0.0)
        ord.priority || rem > avail + 0.01
    }
    val priority = if (hasUrgent) SchedulePriority.URGENT else SchedulePriority.ORDER_DRIVEN
    return priority to seeds
}
