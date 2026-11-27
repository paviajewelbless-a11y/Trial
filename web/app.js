import { initializeApp } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-app.js";
import {
  GoogleAuthProvider,
  getAuth,
  onAuthStateChanged,
  signInWithPopup,
  signOut
} from "https://www.gstatic.com/firebasejs/11.6.0/firebase-auth.js";
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getFirestore,
  onSnapshot,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
  writeBatch
} from "https://www.gstatic.com/firebasejs/11.6.0/firebase-firestore.js";
import {
  FIREBASE_CONFIG,
  FIRESTORE_DATABASE_ID,
  FIRESTORE_PROJECT_ID,
  HOSTED_WEB_URL,
  PLANT_SECTIONS,
  REPORT_SECTION_OPTIONS,
  buildBatchCreatePayload,
  buildBatchUpdatePayload,
  buildBomCreatePayload,
  buildBomUpdatePayload,
  buildBreadcrumbPayload,
  buildCsvReport,
  buildCustomerCreatePayload,
  buildCustomerUpdatePayload,
  buildDispatchCreatePayload,
  buildDispatchUpdatePayload,
  buildGmailWebComposeUrl,
  buildInviteDocId,
  buildInviteEmailPayload,
  buildInviteRoleUpdatePayload,
  buildMemberRoleUpdatePayload,
  buildMessengerSummaryText,
  buildOrderCreatePayload,
  buildOrderUpdatePayload,
  buildPlantReport,
  buildRawMaterialCreatePayload,
  buildRawMaterialUpdatePayload,
  buildReprocessCreatePayload,
  buildReprocessUpdatePayload,
  buildRushOrderCode,
  buildScheduleCreatePayload,
  buildScheduleUpdatePayload,
  buildSettingsCreatePayload,
  buildSettingsUpdatePayload,
  buildSkuCreatePayload,
  buildSkuUpdatePayload,
  buildWebInviteLink,
  buildWorkspaceInvitePayload,
  computeKpis,
  computeRawMaterialForecasts,
  computeSkuSummaries,
  effectiveDispatchWeightKg,
  makeDocId,
  missingCatalogEntities,
  parseBulkBatchRows,
  parseInviteQueryParams,
  resolveRole,
  resolvedDispatchStatus,
  todayIsoDate
} from "./operations.js";
import {
  escapeHtml,
  formatNum,
  renderCustomersTab,
  renderDispatchedTab,
  renderHistoryTab,
  renderInventoryTab,
  renderMaterialsTab,
  renderOrdersTab,
  renderRecoveryTab,
  renderReportsTab,
  renderReprocessTab,
  renderScheduleTab,
  renderSummaryTab,
  renderTeamTab
} from "./views.js";
import {
  closeModal,
  openAdjustRawModal,
  openBatchModal,
  openBomModal,
  openBulkBatchModal,
  openCompleteScheduleModal,
  openConfirmModal,
  openCustomerModal,
  openDispatchModal,
  openFlagReprocessModal,
  openInviteMemberModal,
  openOrderModal,
  openRawMaterialModal,
  openReprocessModal,
  openRestoreJsonModal,
  openScheduleModal,
  openSelectiveResetModal,
  openSkuModal
} from "./modals.js";

const firebaseApp = initializeApp(FIREBASE_CONFIG);
const auth = getAuth(firebaseApp);
const db = getFirestore(firebaseApp, FIRESTORE_DATABASE_ID);
const googleProvider = new GoogleAuthProvider();
googleProvider.setCustomParameters({ prompt: "select_account" });

const TABS = [
  { id: "summary", label: "Summary", subtitle: "Live KPIs, plant section cards & alerts" },
  { id: "inventory", label: "Finished Goods", subtitle: "Real-time SKU stock, cover & milling needs" },
  { id: "orders", label: "Orders", subtitle: "Customer orders & fulfillment tracking" },
  { id: "dispatched", label: "Dispatched", subtitle: "Delivery receipts, returns & gate holds" },
  { id: "schedule", label: "Schedule", subtitle: "Planned milling queue & priorities" },
  { id: "recovery", label: "Recovery & Logs", subtitle: "Production batches, daily plant log & section monitor" },
  { id: "materials", label: "Materials & BOM", subtitle: "Macro/Micro stock, demand simulator & Feed BOMs" },
  { id: "reprocess", label: "Reprocess", subtitle: "Returned feed quarantine & inventory restoration" },
  { id: "customers", label: "Customers", subtitle: "Customer directory & fulfillment summary" },
  { id: "reports", label: "Reports & Backup", subtitle: "CSV, Excel, PDF reports, JSON backup & reset" },
  { id: "team", label: "Team & Access", subtitle: "Shared workspace, invites & role permissions" },
  { id: "history", label: "History", subtitle: "Recent workspace actions & sync audit trail" }
];

const state = {
  user: null,
  activeTab: "summary",
  searchText: "",
  activeWorkspaceOwnerId: "",
  myInvites: [],
  skus: [],
  orders: [],
  dispatches: [],
  batches: [],
  schedules: [],
  rawMaterials: [],
  boms: [],
  reprocessItems: [],
  customers: [],
  breadcrumbs: [],
  teamMembers: [],
  workspaceInvites: [],
  plantSettings: null,
  myMemberRecord: null,
  updatingAccessKeys: new Set(),
  unsubInvites: null,
  unsubWorkspace: [],

  // Filter & simulator UI state
  sectionFilter: "ALL",
  inventoryStatusFilter: "ALL",
  simLookbackDays: 14,
  simBufferPct: null,
  settingsHydrated: false,
  orderStatusFilter: "ALL",
  dispatchStatusFilter: "ALL",
  scheduleStatusFilter: "ALL",
  recoverySubTab: "batches",
  selectedDailyLogDate: "",
  materialsSubTab: "all",
  rawSectorFilter: "ALL",
  rawRiskFilter: "ALL",
  rawDemandMultiplier: 1.0,
  rawIncludeScheduled: false,
  rawLeadOverrideDays: null,
  reprocessFilter: "ALL",
  reportStartDate: todayIsoDate(),
  reportEndDate: todayIsoDate(),
  reportSectionFilter: "ALL",
  reportIncludedSections: [...REPORT_SECTION_OPTIONS],
  historyCategoryFilter: "ALL"
};

const deepLinkParams = parseInviteQueryParams(window.location.search);
if (deepLinkParams.action === "team_invite" || deepLinkParams.inviteId || deepLinkParams.workspaceOwnerId) {
  state.activeTab = "team";
}

function showToast(message) {
  const el = document.getElementById("toast");
  if (!el) return;
  el.textContent = message;
  el.classList.remove("hidden");
  clearTimeout(showToast._timer);
  showToast._timer = setTimeout(() => el.classList.add("hidden"), 4000);
}

function isLocalOrigin() {
  const host = (window.location.hostname || "").toLowerCase();
  const protocol = (window.location.protocol || "").toLowerCase();
  return protocol === "file:" || host === "localhost" || host === "127.0.0.1" || host === "[::1]";
}

function getCurrentPortalUrl() {
  const origin = window.location.origin || "";
  if (!origin || origin === "null") return HOSTED_WEB_URL;
  const cleanPath = (window.location.pathname || "/")
    .replace(/\/index\.html$/i, "")
    .replace(/\/+$/, "");
  return `${origin}${cleanPath}`;
}

function getWebOriginBase() {
  const origin = window.location.origin || "";
  if (origin.startsWith("https://") && !isLocalOrigin()) {
    return getCurrentPortalUrl();
  }
  return HOSTED_WEB_URL;
}

function getActiveRole() {
  if (!state.user || !state.activeWorkspaceOwnerId) return resolveRole("Viewer");
  if (state.activeWorkspaceOwnerId === state.user.uid) return resolveRole("Admin");
  if (state.myMemberRecord?.role) return resolveRole(state.myMemberRecord.role);
  const activeInvite = state.myInvites.find(
    (inv) => inv.status === "Active" && inv.workspaceOwnerId === state.activeWorkspaceOwnerId
  );
  return resolveRole(activeInvite?.role || "Viewer");
}

function getAvailableWorkspaces() {
  if (!state.user) return [];
  const personal = {
    workspaceOwnerId: state.user.uid,
    label: "My Bulacan Plant Workspace (Owner)",
    role: "Admin",
    ownerEmail: state.user.email || ""
  };
  const sharedMap = new Map();
  for (const inv of state.myInvites) {
    if (inv.status === "Active" && inv.workspaceOwnerId && inv.workspaceOwnerId !== state.user.uid) {
      if (!sharedMap.has(inv.workspaceOwnerId)) {
        sharedMap.set(inv.workspaceOwnerId, {
          workspaceOwnerId: inv.workspaceOwnerId,
          label: `Shared: ${inv.workspaceName || "Bulacan Plant"} (${inv.ownerEmail || inv.invitedByEmail || "Shared"})`,
          role: resolveRole(inv.role).label,
          ownerEmail: inv.ownerEmail || inv.invitedByEmail || ""
        });
      }
    }
  }
  return [personal, ...sharedMap.values()];
}

function getPendingInvitesForMe() {
  return state.myInvites.filter((inv) => inv.status === "Pending" && inv.workspaceOwnerId);
}

async function logBreadcrumb({ category, action, details = "", entityRef = "", severity = "Info", screenTab = "" }) {
  const ownerId = state.activeWorkspaceOwnerId;
  if (!ownerId || !getActiveRole().canWriteOperations) return;
  try {
    const payload = buildBreadcrumbPayload(
      ownerId,
      {
        actorType: "User",
        category,
        action,
        details: `${details}${state.user?.email ? ` (${state.user.email})` : ""}`.trim(),
        screenTab: screenTab || TABS.find((t) => t.id === state.activeTab)?.label || "Web Portal",
        entityRef,
        trailPath: `Web > ${category}`,
        severity
      },
      serverTimestamp()
    );
    await setDoc(doc(db, "users", ownerId, "breadcrumbs", makeDocId("bc")), payload);
  } catch (err) {
    console.warn("Breadcrumb log skipped:", err);
  }
}

async function commitOperationsInChunks(ops) {
  for (let i = 0; i < ops.length; i += 350) {
    const chunk = ops.slice(i, i + 350);
    const batch = writeBatch(db);
    for (const op of chunk) {
      if (op.type === "set") batch.set(op.ref, op.data);
      else if (op.type === "update") batch.update(op.ref, op.data);
      else if (op.type === "delete") batch.delete(op.ref);
    }
    await batch.commit();
  }
}

function clearWorkspaceListeners() {
  for (const unsub of state.unsubWorkspace) {
    try {
      unsub();
    } catch (_) {}
  }
  state.unsubWorkspace = [];
}

function subscribeWorkspace(ownerId) {
  clearWorkspaceListeners();
  state.activeWorkspaceOwnerId = ownerId;
  state.settingsHydrated = false;
  state.plantSettings = null;
  if (state.user) {
    localStorage.setItem(`feedmill_web_ws_${state.user.uid}`, ownerId);
  }

  const userRef = doc(db, "users", ownerId);
  const attach = (subName, setter) => {
    const unsub = onSnapshot(
      collection(userRef, subName),
      (snap) => {
        setter(snap.docs.map((d) => ({ id: d.id, ...d.data() })));
        render();
      },
      (err) => {
        console.warn(`Snapshot error (${subName}):`, err);
      }
    );
    state.unsubWorkspace.push(unsub);
  };

  attach("skus", (list) => {
    state.skus = list.sort((a, b) => (a.productName || "").localeCompare(b.productName || ""));
  });
  attach("orders", (list) => {
    state.orders = list.sort((a, b) => (b.orderDate || "").localeCompare(a.orderDate || ""));
  });
  attach("dispatches", (list) => {
    state.dispatches = list.sort((a, b) => (b.date || "").localeCompare(a.date || ""));
  });
  attach("production_batches", (list) => {
    state.batches = list.sort((a, b) => (b.date || "").localeCompare(a.date || ""));
  });
  attach("schedules", (list) => {
    state.schedules = list.sort((a, b) => (b.scheduledDate || "").localeCompare(a.scheduledDate || ""));
  });
  attach("raw_materials", (list) => {
    state.rawMaterials = list.sort((a, b) => (a.name || "").localeCompare(b.name || ""));
  });
  attach("boms", (list) => {
    state.boms = list.sort((a, b) => (a.productName || "").localeCompare(b.productName || ""));
  });
  attach("reprocess_items", (list) => {
    state.reprocessItems = list.sort((a, b) => (b.returnedDate || "").localeCompare(a.returnedDate || ""));
  });
  attach("customers", (list) => {
    state.customers = list.sort((a, b) => (a.name || "").localeCompare(b.name || ""));
  });
  attach("breadcrumbs", (list) => {
    state.breadcrumbs = list.sort((a, b) => Number(b.timestampMillis || 0) - Number(a.timestampMillis || 0));
  });
  attach("team_members", (list) => {
    state.teamMembers = list.sort((a, b) => (a.email || "").localeCompare(b.email || ""));
  });
  attach("team_invites", (list) => {
    state.workspaceInvites = list.sort((a, b) => (a.memberEmail || "").localeCompare(b.memberEmail || ""));
  });

  const unsubSettings = onSnapshot(
    doc(userRef, "settings", "plant_state"),
    (snap) => {
      state.plantSettings = snap.exists() ? { id: snap.id, ...snap.data() } : null;
      if (state.plantSettings && !state.settingsHydrated) {
        state.simLookbackDays = Number(state.plantSettings.lookbackDays || 14);
        state.settingsHydrated = true;
      }
      render();
    },
    (err) => console.warn("Settings snapshot error:", err)
  );
  state.unsubWorkspace.push(unsubSettings);

  if (state.user && ownerId !== state.user.uid) {
    const unsubSelf = onSnapshot(doc(userRef, "team_members", state.user.uid), (snap) => {
      state.myMemberRecord = snap.exists() ? snap.data() : null;
      render();
    });
    state.unsubWorkspace.push(unsubSelf);
  } else {
    state.myMemberRecord = null;
  }

  render();
}

function subscribeUserInvites(user) {
  if (state.unsubInvites) {
    state.unsubInvites();
    state.unsubInvites = null;
  }
  const emailLower = (user.email || "").trim().toLowerCase();
  if (!emailLower) return;

  const q = query(collection(db, "workspace_invites"), where("memberEmail", "==", emailLower));
  state.unsubInvites = onSnapshot(
    q,
    (snap) => {
      state.myInvites = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      const workspaces = getAvailableWorkspaces();
      if (!workspaces.some((w) => w.workspaceOwnerId === state.activeWorkspaceOwnerId)) {
        subscribeWorkspace(user.uid);
      } else {
        render();
      }
    },
    (err) => {
      console.warn("Invite listener error:", err);
    }
  );
}

async function acceptInvite(invite) {
  const user = state.user;
  if (!user) return;
  const emailLower = (user.email || "").trim().toLowerCase();
  const ownerId = invite.workspaceOwnerId;
  const displayName = (user.displayName || invite.memberDisplayName || emailLower.split("@")[0] || "Member").slice(0, 120);

  const memberRef = doc(db, "users", ownerId, "team_members", user.uid);
  const rootInvRef = doc(db, "workspace_invites", invite.id);
  const wsInvRef = doc(db, "users", ownerId, "team_invites", invite.id);

  const [memberSnap, wsInvSnap] = await Promise.all([getDoc(memberRef), getDoc(wsInvRef)]);
  const batch = writeBatch(db);
  if (!memberSnap.exists()) {
    batch.set(memberRef, {
      userId: user.uid,
      workspaceOwnerId: ownerId,
      inviteId: invite.id,
      email: emailLower,
      displayName,
      role: resolveRole(invite.role).label,
      invitedByEmail: invite.invitedByEmail || invite.ownerEmail || "admin",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    });
  }
  batch.update(rootInvRef, {
    status: "Active",
    memberUid: user.uid,
    memberDisplayName: displayName,
    updatedAt: serverTimestamp()
  });
  if (wsInvSnap.exists()) {
    batch.update(wsInvRef, {
      status: "Active",
      memberUid: user.uid,
      memberDisplayName: displayName,
      updatedAt: serverTimestamp()
    });
  }
  await batch.commit();
  showToast(`Joined ${invite.workspaceName || "Bulacan Plant"} as ${resolveRole(invite.role).label}.`);
  subscribeWorkspace(ownerId);
}

async function declineOrLeaveInvite(invite) {
  const user = state.user;
  if (!user) return;
  const ownerId = invite.workspaceOwnerId;
  const batch = writeBatch(db);
  batch.delete(doc(db, "users", ownerId, "team_members", user.uid));
  batch.delete(doc(db, "workspace_invites", invite.id));
  batch.delete(doc(db, "users", ownerId, "team_invites", invite.id));
  await batch.commit();
  showToast("Invitation declined.");
  if (state.activeWorkspaceOwnerId === ownerId) {
    subscribeWorkspace(user.uid);
  }
}

async function switchMemberAccessRole({ memberUid, inviteId, memberEmail, memberName, newRole }) {
  const ownerId = state.activeWorkspaceOwnerId;
  if (!ownerId || !memberUid || memberUid === ownerId) return;
  const resolvedRole = resolveRole(newRole).label;
  const lockKey = `member:${memberUid}`;
  if (state.updatingAccessKeys.has(lockKey)) return;

  state.updatingAccessKeys.add(lockKey);
  render();
  try {
    const callerEmail = (state.user?.email || "").trim().toLowerCase();
    const resolvedInviteId =
      String(inviteId || "").trim() ||
      (memberEmail ? await buildInviteDocId(ownerId, memberEmail) : "");

    const batch = writeBatch(db);
    const memberRef = doc(db, "users", ownerId, "team_members", memberUid);
    batch.update(memberRef, buildMemberRoleUpdatePayload(resolvedRole, serverTimestamp()));

    if (resolvedInviteId) {
      const inviteUpdate = buildInviteRoleUpdatePayload(resolvedRole, callerEmail, serverTimestamp());
      const wsInvRef = doc(db, "users", ownerId, "team_invites", resolvedInviteId);
      const rootInvRef = doc(db, "workspace_invites", resolvedInviteId);
      const [wsSnap, rootSnap] = await Promise.all([
        getDoc(wsInvRef).catch(() => null),
        getDoc(rootInvRef).catch(() => null)
      ]);
      if (wsSnap?.exists()) batch.update(wsInvRef, inviteUpdate);
      if (rootSnap?.exists()) batch.update(rootInvRef, inviteUpdate);
    }

    await batch.commit();
    showToast(`Switched ${memberName || memberEmail || "member"} access to ${resolvedRole}.`);
  } catch (err) {
    showToast(err?.message || "Failed to update team member access.");
  } finally {
    state.updatingAccessKeys.delete(lockKey);
    render();
  }
}

async function switchPendingInviteRole({ inviteId, memberEmail, newRole }) {
  const ownerId = state.activeWorkspaceOwnerId;
  if (!ownerId || !inviteId) return;
  const resolvedRole = resolveRole(newRole).label;
  const lockKey = `invite:${inviteId}`;
  if (state.updatingAccessKeys.has(lockKey)) return;

  state.updatingAccessKeys.add(lockKey);
  render();
  try {
    const callerEmail = (state.user?.email || "").trim().toLowerCase();
    const inviteUpdate = buildInviteRoleUpdatePayload(resolvedRole, callerEmail, serverTimestamp());
    const wsInvRef = doc(db, "users", ownerId, "team_invites", inviteId);
    const rootInvRef = doc(db, "workspace_invites", inviteId);
    const [wsSnap, rootSnap] = await Promise.all([
      getDoc(wsInvRef).catch(() => null),
      getDoc(rootInvRef).catch(() => null)
    ]);

    const batch = writeBatch(db);
    let updatedCount = 0;
    if (wsSnap?.exists()) {
      batch.update(wsInvRef, inviteUpdate);
      updatedCount++;
    }
    if (rootSnap?.exists()) {
      batch.update(rootInvRef, inviteUpdate);
      updatedCount++;
    }
    if (updatedCount > 0) await batch.commit();
    showToast(`Switched invite for ${memberEmail || "teammate"} to ${resolvedRole}.`);
  } catch (err) {
    showToast(err?.message || "Failed to update invite access.");
  } finally {
    state.updatingAccessKeys.delete(lockKey);
    render();
  }
}

function downloadFile(filename, content, mimeType) {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
  showToast(`Downloaded ${filename}`);
}

function downloadCsv(filename, headers, rows) {
  downloadFile(filename, buildCsvReport(headers, rows), "text/csv;charset=utf-8;");
}

function applyRecalcOrderOps(ops, ownerId, orderId, dispatchesList) {
  const ord = state.orders.find((o) => o.id === orderId);
  if (!ord) return;
  const newDispKg = Number(
    dispatchesList
      .filter((d) => d.linkedOrderId === orderId && !d.isHeld)
      .reduce((sum, d) => sum + effectiveDispatchWeightKg(d), 0)
      .toFixed(2)
  );
  const totalKg = Number(ord.totalWeightKg || 0);
  const newStatus =
    ord.status === "Cancelled"
      ? "Cancelled"
      : newDispKg >= totalKg - 0.01
      ? "Fulfilled"
      : newDispKg > 0
      ? "Partially Dispatched"
      : "Open";

  const orderRef = doc(db, "users", ownerId, "orders", ord.id);
  if (ord.status === "Open" || ord.status === "Partially Dispatched") {
    ops.push({
      type: "update",
      ref: orderRef,
      data: {
        dispatchedWeightKg: newDispKg,
        status: newStatus,
        updatedAt: serverTimestamp()
      }
    });
  } else {
    ops.push({ type: "delete", ref: orderRef });
    ops.push({
      type: "set",
      ref: orderRef,
      data: buildOrderCreatePayload(
        ownerId,
        { ...ord, dispatchedWeightKg: newDispKg, status: newStatus },
        serverTimestamp()
      )
    });
  }
}

async function persistPlantSettings(lookbackDays, safetyBufferPct) {
  const ownerId = state.activeWorkspaceOwnerId;
  if (!ownerId || !getActiveRole().canEditSettings) return;
  try {
    const ref = doc(db, "users", ownerId, "settings", "plant_state");
    const input = {
      seededOctober2026: state.plantSettings?.seededOctober2026 !== false,
      lookbackDays,
      defaultSafetyBufferPct: safetyBufferPct ?? state.plantSettings?.defaultSafetyBufferPct ?? 15
    };
    if (state.plantSettings) {
      await updateDoc(ref, buildSettingsUpdatePayload(input, serverTimestamp()));
    } else {
      await setDoc(ref, buildSettingsCreatePayload(ownerId, input, serverTimestamp()));
    }
  } catch (err) {
    console.warn("Settings sync note:", err?.message || err);
  }
}

const modalActions = {
  async saveSku(existingSku, input) {
    const ownerId = state.activeWorkspaceOwnerId;
    try {
      if (existingSku) {
        await updateDoc(
          doc(db, "users", ownerId, "skus", existingSku.id),
          buildSkuUpdatePayload(input, serverTimestamp())
        );
        closeModal();
        showToast(`Updated SKU ${input.productName}.`);
        logBreadcrumb({ category: "Inventory", action: "Updated SKU", details: input.productName, entityRef: existingSku.id, severity: "Success" });
      } else {
        const id = makeDocId("sku");
        const payload = buildSkuCreatePayload(ownerId, input, serverTimestamp());
        await setDoc(doc(db, "users", ownerId, "skus", id), payload);
        closeModal();
        showToast(`Added SKU ${payload.productName}.`);
        logBreadcrumb({ category: "Inventory", action: "Added SKU", details: payload.productName, entityRef: id, severity: "Success" });
      }
    } catch (err) {
      showToast(err?.message || "Failed to save SKU.");
    }
  },

  async saveOrder(existingOrder, input) {
    const ownerId = state.activeWorkspaceOwnerId;
    try {
      if (existingOrder) {
        const ref = doc(db, "users", ownerId, "orders", existingOrder.id);
        if (existingOrder.status === "Open" || existingOrder.status === "Partially Dispatched") {
          await updateDoc(ref, buildOrderUpdatePayload(input, existingOrder.dispatchedWeightKg, serverTimestamp()));
        } else {
          const ops = [
            { type: "delete", ref },
            {
              type: "set",
              ref,
              data: buildOrderCreatePayload(
                ownerId,
                { ...existingOrder, ...input, dispatchedWeightKg: existingOrder.dispatchedWeightKg },
                serverTimestamp()
              )
            }
          ];
          await commitOperationsInChunks(ops);
        }
        closeModal();
        showToast(`Updated order ${existingOrder.orderCode}.`);
        logBreadcrumb({ category: "Orders", action: "Updated Order", details: existingOrder.orderCode, entityRef: existingOrder.id, severity: "Success" });
      } else {
        const id = makeDocId("ord");
        const payload = buildOrderCreatePayload(ownerId, input, serverTimestamp());
        await setDoc(doc(db, "users", ownerId, "orders", id), payload);
        closeModal();
        showToast(`Saved order ${payload.orderCode}.`);
        logBreadcrumb({ category: "Orders", action: "Created Order", details: `${payload.orderCode} • ${payload.customer}`, entityRef: id, severity: "Success" });
      }
    } catch (err) {
      showToast(err?.message || "Failed to save order.");
    }
  },

  async saveDispatch(existingDispatch, input) {
    const ownerId = state.activeWorkspaceOwnerId;
    try {
      const dispatchId = existingDispatch?.id || makeDocId("disp");
      let effectiveLinkedId = String(input.linkedOrderId || "").trim();
      const ops = [];

      if (!existingDispatch && !effectiveLinkedId && input.createRushOrder) {
        const rushOrderId = makeDocId("ord");
        const rushPayload = buildOrderCreatePayload(
          ownerId,
          {
            orderCode: buildRushOrderCode(input.drReference),
            orderDate: input.date,
            dueDate: input.date,
            productName: input.productName,
            orderedBags: input.bagsDispatched,
            bagWeightKg: input.bagWeightKg,
            dispatchedWeightKg: Number(input.bagsDispatched) * Number(input.bagWeightKg),
            customer: input.customer,
            status: "Fulfilled",
            priority: true,
            rushOrder: true
          },
          serverTimestamp()
        );
        ops.push({ type: "set", ref: doc(db, "users", ownerId, "orders", rushOrderId), data: rushPayload });
        effectiveLinkedId = rushOrderId;
      }

      const cleanInput = { ...input, linkedOrderId: effectiveLinkedId };
      const dispRef = doc(db, "users", ownerId, "dispatches", dispatchId);

      if (existingDispatch) {
        const upd = buildDispatchUpdatePayload(cleanInput, serverTimestamp());
        ops.push({ type: "update", ref: dispRef, data: upd });

        const simulated = state.dispatches.map((d) =>
          d.id === dispatchId ? { ...d, ...upd, id: dispatchId } : d
        );
        const affectedOrderIds = new Set([existingDispatch.linkedOrderId, effectiveLinkedId].filter(Boolean));
        for (const oid of affectedOrderIds) {
          applyRecalcOrderOps(ops, ownerId, oid, simulated);
        }
      } else {
        const payload = buildDispatchCreatePayload(ownerId, cleanInput, serverTimestamp());
        ops.push({ type: "set", ref: dispRef, data: payload });
        const simulated = [...state.dispatches, { ...payload, id: dispatchId }];
        if (effectiveLinkedId) {
          applyRecalcOrderOps(ops, ownerId, effectiveLinkedId, simulated);
        }
      }

      if (cleanInput.returnConfirmed && cleanInput.autoQueueReprocess) {
        const alreadyLinked = state.reprocessItems.some((r) => r.sourceDispatchId === dispatchId);
        if (!alreadyLinked) {
          const repId = makeDocId("rep");
          const repPayload = buildReprocessCreatePayload(
            ownerId,
            {
              sourceDispatchId: dispatchId,
              drReference: cleanInput.drReference,
              customer: cleanInput.customer,
              productName: cleanInput.productName,
              section:
                cleanInput.sourcePlant ||
                state.skus.find((s) => s.productName === cleanInput.productName)?.section ||
                PLANT_SECTIONS[0],
              returnedDate: cleanInput.date,
              returnedBags: cleanInput.returnedBags,
              bagWeightKg: cleanInput.bagWeightKg,
              returnedWeightKg: cleanInput.returnedWeightKg,
              returnReason: cleanInput.returnReason,
              status: "Pending Reprocess",
              fastActionNeeded: true,
              flaggedToInventory: false
            },
            serverTimestamp()
          );
          ops.push({ type: "set", ref: doc(db, "users", ownerId, "reprocess_items", repId), data: repPayload });
        }
      }

      await commitOperationsInChunks(ops);
      closeModal();
      showToast(existingDispatch ? `Updated dispatch ${cleanInput.drReference}.` : `Logged dispatch ${cleanInput.drReference}.`);
      logBreadcrumb({
        category: "Dispatch",
        action: existingDispatch ? "Updated Dispatch" : "Logged Dispatch",
        details: `${cleanInput.drReference} • ${cleanInput.customer} • ${cleanInput.productName}`,
        entityRef: dispatchId,
        severity: cleanInput.isHeld || cleanInput.returnConfirmed ? "Warning" : "Success"
      });
    } catch (err) {
      showToast(err?.message || "Failed to save dispatch.");
    }
  },

  async saveSchedule(existingSchedule, input) {
    const ownerId = state.activeWorkspaceOwnerId;
    try {
      if (existingSchedule) {
        await updateDoc(
          doc(db, "users", ownerId, "schedules", existingSchedule.id),
          buildScheduleUpdatePayload(input, serverTimestamp())
        );
        closeModal();
        showToast(`Updated schedule for ${existingSchedule.productName}.`);
        logBreadcrumb({ category: "Schedule", action: "Updated Schedule", details: existingSchedule.productName, entityRef: existingSchedule.id, severity: "Success" });
      } else {
        const id = makeDocId("sch");
        const payload = buildScheduleCreatePayload(ownerId, input, serverTimestamp());
        await setDoc(doc(db, "users", ownerId, "schedules", id), payload);
        closeModal();
        showToast(`Queued ${payload.targetBags} bags of ${payload.productName}.`);
        logBreadcrumb({ category: "Schedule", action: "Scheduled Milling Run", details: `${payload.productName} (${payload.targetBags} bags)`, entityRef: id, severity: "Success" });
      }
    } catch (err) {
      showToast(err?.message || "Failed to save schedule.");
    }
  },

  async completeScheduleRun(schedule, input) {
    const ownerId = state.activeWorkspaceOwnerId;
    try {
      const batchId = makeDocId("bat");
      const batchPayload = buildBatchCreatePayload(
        ownerId,
        {
          date: input.date,
          section: schedule.section,
          productName: schedule.productName,
          bagsProduced: input.bagsProduced,
          bagWeightKg: schedule.bagWeightKg,
          actualRecoveredKg: input.actualRecoveredKg,
          remarks: input.remarks || schedule.lineRemarks,
          scheduleRunId: schedule.id,
          linkedOrderIds: schedule.linkedOrderIds || (schedule.linkedOrderId ? [schedule.linkedOrderId] : []),
          linkedOrderCodes: schedule.linkedOrderCodes || (schedule.linkedOrderCode ? [schedule.linkedOrderCode] : [])
        },
        serverTimestamp()
      );
      const ops = [
        {
          type: "update",
          ref: doc(db, "users", ownerId, "schedules", schedule.id),
          data: { status: "Completed", updatedAt: serverTimestamp() }
        },
        {
          type: "set",
          ref: doc(db, "users", ownerId, "production_batches", batchId),
          data: batchPayload
        }
      ];
      await commitOperationsInChunks(ops);
      closeModal();
      showToast(`Completed schedule and logged ${batchPayload.bagsProduced} bags.`);
      logBreadcrumb({ category: "Schedule", action: "Completed Milling Run", details: `${schedule.productName} (${batchPayload.bagsProduced} bags)`, entityRef: schedule.id, severity: "Success" });
    } catch (err) {
      showToast(err?.message || "Failed to complete schedule.");
    }
  },

  async saveBatch(existingBatch, input) {
    const ownerId = state.activeWorkspaceOwnerId;
    try {
      if (existingBatch) {
        await updateDoc(
          doc(db, "users", ownerId, "production_batches", existingBatch.id),
          buildBatchUpdatePayload(input, serverTimestamp())
        );
        closeModal();
        showToast(`Updated batch for ${input.productName}.`);
        logBreadcrumb({ category: "Recovery", action: "Updated Batch", details: input.productName, entityRef: existingBatch.id, severity: "Success" });
      } else {
        const id = makeDocId("bat");
        const payload = buildBatchCreatePayload(ownerId, input, serverTimestamp());
        await setDoc(doc(db, "users", ownerId, "production_batches", id), payload);
        closeModal();
        showToast(`Logged ${payload.bagsProduced} bags of ${payload.productName}.`);
        logBreadcrumb({ category: "Recovery", action: "Logged Batch", details: `${payload.productName} (${payload.bagsProduced} bags)`, entityRef: id, severity: "Success" });
      }
    } catch (err) {
      showToast(err?.message || "Failed to save batch.");
    }
  },

  async bulkImportBatches(rawText, defaultSection) {
    const ownerId = state.activeWorkspaceOwnerId;
    const rows = parseBulkBatchRows(rawText, defaultSection);
    if (!rows.length) {
      showToast("No valid rows parsed. Use: Date, Section, Product, Bags, BagKg, ActualKg, Remarks");
      return;
    }
    try {
      const ops = rows.map((row) => ({
        type: "set",
        ref: doc(db, "users", ownerId, "production_batches", makeDocId("bat")),
        data: buildBatchCreatePayload(ownerId, row, serverTimestamp())
      }));
      await commitOperationsInChunks(ops);
      closeModal();
      showToast(`Imported ${rows.length} production batches.`);
      logBreadcrumb({ category: "Recovery", action: "Bulk Imported Batches", details: `${rows.length} batches imported`, severity: "Success" });
    } catch (err) {
      showToast(err?.message || "Bulk import failed.");
    }
  },

  async saveRawMaterial(existingMat, input) {
    const ownerId = state.activeWorkspaceOwnerId;
    try {
      if (existingMat) {
        await updateDoc(
          doc(db, "users", ownerId, "raw_materials", existingMat.id),
          buildRawMaterialUpdatePayload(input, serverTimestamp())
        );
        closeModal();
        showToast(`Updated ${input.name}.`);
        logBreadcrumb({ category: "Inventory", action: "Updated Raw Material", details: input.name, entityRef: existingMat.id, severity: "Success" });
      } else {
        const id = makeDocId("raw");
        const payload = buildRawMaterialCreatePayload(ownerId, input, serverTimestamp());
        await setDoc(doc(db, "users", ownerId, "raw_materials", id), payload);
        closeModal();
        showToast(`Added raw material ${payload.name}.`);
        logBreadcrumb({ category: "Inventory", action: "Added Raw Material", details: payload.name, entityRef: id, severity: "Success" });
      }
    } catch (err) {
      showToast(err?.message || "Failed to save raw material.");
    }
  },

  async adjustRawMaterialStock(mat, deltaKg) {
    const ownerId = state.activeWorkspaceOwnerId;
    try {
      const newStock = Math.max(0, Number((Number(mat.currentStockKg || 0) + Number(deltaKg || 0)).toFixed(2)));
      const upd = buildRawMaterialUpdatePayload(
        {
          ...mat,
          currentStockKg: newStock,
          lastRestockedDate: deltaKg > 0 ? todayIsoDate() : mat.lastRestockedDate || todayIsoDate()
        },
        serverTimestamp()
      );
      await updateDoc(doc(db, "users", ownerId, "raw_materials", mat.id), upd);
      closeModal();
      showToast(`${mat.name} updated to ${formatNum(newStock, 1)} kg.`);
      logBreadcrumb({
        category: "Inventory",
        action: deltaKg >= 0 ? "Restocked Raw Material" : "Consumed Raw Material",
        details: `${mat.name}: ${deltaKg >= 0 ? "+" : ""}${deltaKg} kg`,
        entityRef: mat.id,
        severity: "Success"
      });
    } catch (err) {
      showToast(err?.message || "Failed to update stock.");
    }
  },

  async saveBom(existingBom, input) {
    const ownerId = state.activeWorkspaceOwnerId;
    if (!input.lines || !input.lines.length) {
      showToast("Add at least 1 ingredient line.");
      return;
    }
    try {
      if (existingBom) {
        await updateDoc(
          doc(db, "users", ownerId, "boms", existingBom.id),
          buildBomUpdatePayload(input, serverTimestamp())
        );
        closeModal();
        showToast(`Updated BOM for ${input.productName}.`);
        logBreadcrumb({ category: "Inventory", action: "Updated Feed BOM", details: input.productName, entityRef: existingBom.id, severity: "Success" });
      } else {
        const id = makeDocId("bom");
        const payload = buildBomCreatePayload(ownerId, input, serverTimestamp());
        await setDoc(doc(db, "users", ownerId, "boms", id), payload);
        closeModal();
        showToast(`Created BOM for ${payload.productName}.`);
        logBreadcrumb({ category: "Inventory", action: "Created Feed BOM", details: payload.productName, entityRef: id, severity: "Success" });
      }
    } catch (err) {
      showToast(err?.message || "Failed to save BOM.");
    }
  },

  async saveReprocess(existingItem, input) {
    const ownerId = state.activeWorkspaceOwnerId;
    try {
      if (existingItem) {
        await updateDoc(
          doc(db, "users", ownerId, "reprocess_items", existingItem.id),
          buildReprocessUpdatePayload(input, serverTimestamp())
        );
        closeModal();
        showToast(`Updated reprocess item (${input.status}).`);
        logBreadcrumb({ category: "Reprocess", action: "Updated Reprocess Item", details: `${input.productName} • ${input.status}`, entityRef: existingItem.id, severity: "Success" });
      } else {
        const id = makeDocId("rep");
        const payload = buildReprocessCreatePayload(ownerId, input, serverTimestamp());
        await setDoc(doc(db, "users", ownerId, "reprocess_items", id), payload);
        closeModal();
        showToast(`Logged reprocess item for ${payload.productName}.`);
        logBreadcrumb({ category: "Reprocess", action: "Logged Reprocess Item", details: `${payload.productName} (${payload.returnedBags} b)`, entityRef: id, severity: "Warning" });
      }
    } catch (err) {
      showToast(err?.message || "Failed to save reprocess item.");
    }
  },

  async saveCustomer(existingCustomer, input) {
    const ownerId = state.activeWorkspaceOwnerId;
    try {
      if (existingCustomer) {
        await updateDoc(
          doc(db, "users", ownerId, "customers", existingCustomer.id),
          buildCustomerUpdatePayload(input, serverTimestamp())
        );
        closeModal();
        showToast(`Updated customer ${input.name}.`);
        logBreadcrumb({ category: "Customers", action: "Updated Customer", details: input.name, entityRef: existingCustomer.id, severity: "Success" });
      } else {
        const id = makeDocId("cust");
        const payload = buildCustomerCreatePayload(ownerId, input, serverTimestamp());
        await setDoc(doc(db, "users", ownerId, "customers", id), payload);
        closeModal();
        showToast(`Added customer ${payload.name}.`);
        logBreadcrumb({ category: "Customers", action: "Added Customer", details: payload.name, entityRef: id, severity: "Success" });
      }
    } catch (err) {
      showToast(err?.message || "Failed to save customer.");
    }
  },

  async sendInvite({ email, displayName, role, customNote, openGmail }) {
    try {
      const cleanEmail = String(email || "").trim().toLowerCase();
      const ownerId = state.activeWorkspaceOwnerId;
      const inviteId = await buildInviteDocId(ownerId, cleanEmail);
      const payload = buildWorkspaceInvitePayload(
        {
          workspaceOwnerId: ownerId,
          workspaceName: "Bulacan Plant",
          ownerEmail: state.user?.email || "",
          memberEmail: cleanEmail,
          memberDisplayName: displayName,
          role,
          invitedByEmail: state.user?.email || ""
        },
        serverTimestamp()
      );
      const batch = writeBatch(db);
      batch.set(doc(db, "workspace_invites", inviteId), payload);
      batch.set(doc(db, "users", ownerId, "team_invites", inviteId), payload);
      await batch.commit();

      closeModal();
      showToast(`Invited ${cleanEmail} as ${role}.`);
      logBreadcrumb({ category: "System", action: "Invited Team Member", details: `${cleanEmail} (${role})`, entityRef: inviteId, severity: "Success" });

      if (openGmail) {
        const emailContent = buildInviteEmailPayload({
          recipientEmail: cleanEmail,
          recipientName: displayName,
          role,
          inviterEmail: state.user?.email || "",
          workspaceOwnerId: ownerId,
          inviteId,
          customNote,
          baseUrl: getWebOriginBase()
        });
        window.open(
          buildGmailWebComposeUrl({ to: cleanEmail, subject: emailContent.subject, body: emailContent.body }),
          "_blank",
          "noopener"
        );
      }
    } catch (err) {
      showToast(err?.message || "Failed to send team invite.");
    }
  },

  async restoreJsonBackup(rawJson) {
    const ownerId = state.activeWorkspaceOwnerId;
    try {
      const parsed = JSON.parse(rawJson);
      const ops = [];
      const queueSet = (colName, items, builder, prefix) => {
        if (!Array.isArray(items)) return;
        for (const item of items) {
          const docId = String(item.id || "").trim() || makeDocId(prefix);
          ops.push({
            type: "set",
            ref: doc(db, "users", ownerId, colName, docId),
            data: builder(ownerId, item, serverTimestamp())
          });
        }
      };

      queueSet("skus", parsed.skus, buildSkuCreatePayload, "sku");
      queueSet("customers", parsed.customers, buildCustomerCreatePayload, "cust");
      queueSet("orders", parsed.orders, buildOrderCreatePayload, "ord");
      queueSet("dispatches", parsed.dispatches, buildDispatchCreatePayload, "disp");
      queueSet("schedules", parsed.schedules, buildScheduleCreatePayload, "sch");
      queueSet("production_batches", parsed.batches || parsed.production_batches, buildBatchCreatePayload, "bat");
      queueSet("raw_materials", parsed.rawMaterials || parsed.raw_materials, buildRawMaterialCreatePayload, "raw");
      queueSet("boms", parsed.boms, buildBomCreatePayload, "bom");
      queueSet("reprocess_items", parsed.reprocessItems || parsed.reprocess_items, buildReprocessCreatePayload, "rep");

      if (!ops.length) {
        showToast("Backup JSON contained no restorable records.");
        return;
      }
      await commitOperationsInChunks(ops);
      closeModal();
      showToast(`Restored ${ops.length} workspace records from JSON.`);
      logBreadcrumb({ category: "Reports", action: "Restored JSON Backup", details: `${ops.length} records restored`, severity: "Warning" });
    } catch (err) {
      showToast(err?.message || "Invalid JSON backup file.");
    }
  },

  async runSelectiveReset(collections) {
    const ownerId = state.activeWorkspaceOwnerId;
    if (!collections.length) {
      showToast("Select at least one collection to reset.");
      return;
    }
    const mapList = {
      skus: state.skus,
      orders: state.orders,
      dispatches: state.dispatches,
      schedules: state.schedules,
      production_batches: state.batches,
      raw_materials: state.rawMaterials,
      boms: state.boms,
      reprocess_items: state.reprocessItems,
      customers: state.customers,
      breadcrumbs: state.breadcrumbs
    };
    try {
      const ops = [];
      for (const col of collections) {
        for (const item of mapList[col] || []) {
          ops.push({ type: "delete", ref: doc(db, "users", ownerId, col, item.id) });
        }
      }
      await commitOperationsInChunks(ops);
      closeModal();
      showToast(`Deleted ${ops.length} records across ${collections.length} collections.`);
      if (!collections.includes("breadcrumbs")) {
        logBreadcrumb({ category: "System", action: "Selective Workspace Reset", details: collections.join(", "), severity: "Warning" });
      }
    } catch (err) {
      showToast(err?.message || "Selective reset failed.");
    }
  }
};

function renderPendingInvitesBanner() {
  const area = document.getElementById("pending-invites-area");
  const pending = getPendingInvitesForMe();
  if (!pending.length) {
    area.innerHTML = "";
    return;
  }
  area.innerHTML = pending
    .map(
      (inv) => `
      <div class="banner" style="margin-bottom: 10px;">
        <div>
          <strong>Workspace Invitation: ${escapeHtml(inv.workspaceName || "Bulacan Plant")}</strong>
          <div style="font-size:13px; color:#36453e;">
            Invited by ${escapeHtml(inv.invitedByEmail || inv.ownerEmail || "Admin")} • Role: <strong>${escapeHtml(inv.role)}</strong>
          </div>
        </div>
        <div style="display:flex; gap:8px;">
          <button class="btn btn-primary btn-sm" data-accept-invite="${escapeHtml(inv.id)}">Accept &amp; Switch</button>
          <button class="btn btn-sm" data-decline-invite="${escapeHtml(inv.id)}">Decline</button>
        </div>
      </div>
    `
    )
    .join("");

  area.querySelectorAll("[data-accept-invite]").forEach((btn) => {
    btn.onclick = () => {
      const inv = pending.find((i) => i.id === btn.dataset.acceptInvite);
      if (inv) acceptInvite(inv).catch((e) => showToast(e.message));
    };
  });
  area.querySelectorAll("[data-decline-invite]").forEach((btn) => {
    btn.onclick = () => {
      const inv = pending.find((i) => i.id === btn.dataset.declineInvite);
      if (inv) declineOrLeaveInvite(inv).catch((e) => showToast(e.message));
    };
  });
}

function render() {
  const authScreen = document.getElementById("auth-screen");
  const appShell = document.getElementById("app-shell");
  if (!state.user) {
    authScreen.classList.remove("hidden");
    appShell.classList.add("hidden");
    return;
  }
  authScreen.classList.add("hidden");
  appShell.classList.remove("hidden");

  const activeSearchEl = document.activeElement?.id === "tab-search";
  const searchSelStart = activeSearchEl ? document.activeElement.selectionStart : null;

  const activeRole = getActiveRole();
  const workspaces = getAvailableWorkspaces();
  const wsSelect = document.getElementById("workspace-select");
  wsSelect.innerHTML = workspaces
    .map(
      (w) => `<option value="${escapeHtml(w.workspaceOwnerId)}" ${w.workspaceOwnerId === state.activeWorkspaceOwnerId ? "selected" : ""}>${escapeHtml(w.label)}</option>`
    )
    .join("");
  wsSelect.onchange = () => {
    if (wsSelect.value && wsSelect.value !== state.activeWorkspaceOwnerId) {
      subscribeWorkspace(wsSelect.value);
    }
  };

  document.getElementById("active-role-line").textContent = `Role: ${activeRole.label}`;
  document.getElementById("signed-in-email").textContent = state.user.email || state.user.displayName || "Signed In";

  const navList = document.getElementById("nav-list");
  navList.innerHTML = TABS.map(
    (tab) => `<button class="nav-btn ${state.activeTab === tab.id ? "active" : ""}" data-tab="${tab.id}">${escapeHtml(tab.label)}</button>`
  ).join("");
  navList.querySelectorAll("[data-tab]").forEach((btn) => {
    btn.onclick = () => {
      state.activeTab = btn.dataset.tab;
      state.searchText = "";
      render();
    };
  });

  const currentTabMeta = TABS.find((t) => t.id === state.activeTab) || TABS[0];
  document.getElementById("page-title").textContent = currentTabMeta.label;
  document.getElementById("page-subtitle").textContent = currentTabMeta.subtitle;

  const topbarActions = document.getElementById("topbar-actions");
  topbarActions.innerHTML = activeRole.canWriteOperations
    ? `
      <button id="top-new-order" class="btn btn-sm">+ New Order</button>
      <button id="top-log-dispatch" class="btn btn-sm">+ Log Dispatch</button>
      <button id="top-log-batch" class="btn btn-primary btn-sm">+ Log Batch</button>
    `
    : `<span class="pill pill-neutral">Read-Only (${activeRole.label})</span>`;

  if (activeRole.canWriteOperations) {
    document.getElementById("top-new-order").onclick = () => openOrderModal(state, null, modalActions);
    document.getElementById("top-log-dispatch").onclick = () => openDispatchModal(state, {}, modalActions);
    document.getElementById("top-log-batch").onclick = () => openBatchModal(state, null, modalActions);
  }

  renderPendingInvitesBanner();

  const summaries = computeSkuSummaries({
    skus: state.skus,
    batches: state.batches,
    dispatches: state.dispatches,
    orders: state.orders,
    reprocessItems: state.reprocessItems,
    lookbackDays: state.simLookbackDays,
    globalSafetyBufferPct: state.simBufferPct
  });
  const kpis = computeKpis({
    summaries,
    orders: state.orders,
    dispatches: state.dispatches,
    batches: state.batches,
    rawMaterials: state.rawMaterials,
    schedules: state.schedules
  });

  const viewRoot = document.getElementById("view-root");
  if (state.activeTab === "summary") {
    viewRoot.innerHTML = renderSummaryTab(state, summaries, kpis);
  } else if (state.activeTab === "inventory") {
    viewRoot.innerHTML = renderInventoryTab(state, summaries, activeRole);
  } else if (state.activeTab === "orders") {
    viewRoot.innerHTML = renderOrdersTab(state, activeRole);
  } else if (state.activeTab === "dispatched") {
    viewRoot.innerHTML = renderDispatchedTab(state, activeRole);
  } else if (state.activeTab === "schedule") {
    viewRoot.innerHTML = renderScheduleTab(state, activeRole);
  } else if (state.activeTab === "recovery") {
    viewRoot.innerHTML = renderRecoveryTab(state, activeRole);
  } else if (state.activeTab === "materials") {
    viewRoot.innerHTML = renderMaterialsTab(state, activeRole);
  } else if (state.activeTab === "reprocess") {
    viewRoot.innerHTML = renderReprocessTab(state, activeRole);
  } else if (state.activeTab === "customers") {
    viewRoot.innerHTML = renderCustomersTab(state, activeRole);
  } else if (state.activeTab === "reports") {
    viewRoot.innerHTML = renderReportsTab(state, summaries, activeRole);
  } else if (state.activeTab === "team") {
    viewRoot.innerHTML = renderTeamTab(state, activeRole, {
      isLocal: isLocalOrigin(),
      currentUrl: getCurrentPortalUrl(),
      portalUrl: getWebOriginBase()
    });
  } else if (state.activeTab === "history") {
    viewRoot.innerHTML = renderHistoryTab(state, activeRole);
  }

  bindViewEvents(viewRoot, summaries, activeRole);

  if (activeSearchEl) {
    const newSearchEl = document.getElementById("tab-search");
    if (newSearchEl) {
      newSearchEl.focus();
      if (searchSelStart !== null) {
        newSearchEl.setSelectionRange(searchSelStart, searchSelStart);
      }
    }
  }
}

function bindViewEvents(viewRoot, summaries, activeRole) {
  const ownerId = state.activeWorkspaceOwnerId;

  // Shared search
  const searchEl = document.getElementById("tab-search");
  if (searchEl) {
    searchEl.oninput = (e) => {
      state.searchText = e.target.value;
      render();
    };
  }

  // Summary quick-jump
  viewRoot.querySelectorAll("[data-goto-tab]").forEach((btn) => {
    btn.onclick = () => {
      state.activeTab = btn.dataset.gotoTab;
      state.searchText = "";
      render();
    };
  });

  // Inventory actions & simulator controls
  const addSkuBtn = document.getElementById("add-sku-btn");
  if (addSkuBtn) addSkuBtn.onclick = () => openSkuModal(state, null, modalActions);

  const invSec = document.getElementById("inv-section-filter");
  if (invSec) invSec.onchange = () => { state.sectionFilter = invSec.value; render(); };

  const invStatus = document.getElementById("inv-status-filter");
  if (invStatus) invStatus.onchange = () => { state.inventoryStatusFilter = invStatus.value; render(); };

  const invLookback = document.getElementById("inv-lookback-select");
  if (invLookback) {
    invLookback.onchange = () => {
      state.simLookbackDays = Number(invLookback.value || 14);
      persistPlantSettings(state.simLookbackDays, state.simBufferPct);
      render();
    };
  }

  const invBuffer = document.getElementById("inv-buffer-input");
  if (invBuffer) {
    invBuffer.onchange = () => {
      const val = invBuffer.value.trim();
      state.simBufferPct = val === "" ? null : Math.min(100, Math.max(0, Number(val)));
      if (state.simBufferPct !== null) persistPlantSettings(state.simLookbackDays, state.simBufferPct);
      render();
    };
  }

  const invReset = document.getElementById("inv-reset-sim-btn");
  if (invReset) {
    invReset.onclick = () => {
      state.simLookbackDays = Number(state.plantSettings?.lookbackDays || 14);
      state.simBufferPct = null;
      state.sectionFilter = "ALL";
      state.inventoryStatusFilter = "ALL";
      render();
    };
  }

  viewRoot.querySelectorAll("[data-quick-schedule-sku]").forEach((btn) => {
    btn.onclick = () => {
      const sku = state.skus.find((s) => s.id === btn.dataset.quickScheduleSku);
      if (sku) openScheduleModal(state, { prefillSku: sku }, modalActions);
    };
  });

  viewRoot.querySelectorAll("[data-edit-sku]").forEach((btn) => {
    btn.onclick = () => {
      const sku = state.skus.find((s) => s.id === btn.dataset.editSku);
      if (sku) openSkuModal(state, sku, modalActions);
    };
  });

  viewRoot.querySelectorAll("[data-delete-sku]").forEach((btn) => {
    btn.onclick = () => {
      const sku = state.skus.find((s) => s.id === btn.dataset.deleteSku);
      if (!sku) return;
      openConfirmModal({
        title: `Delete SKU • ${sku.productName}`,
        message: "Delete this finished goods SKU? Historical batches and orders will remain in history.",
        confirmLabel: "Delete SKU",
        danger: true,
        onConfirm: async () => {
          await deleteDoc(doc(db, "users", ownerId, "skus", sku.id));
          showToast(`Deleted SKU ${sku.productName}.`);
          logBreadcrumb({ category: "Inventory", action: "Deleted SKU", details: sku.productName, entityRef: sku.id, severity: "Warning" });
        }
      });
    };
  });

  // Orders actions
  const ordAddBtn = document.getElementById("orders-add-btn");
  if (ordAddBtn) ordAddBtn.onclick = () => openOrderModal(state, null, modalActions);

  viewRoot.querySelectorAll("[data-order-filter]").forEach((btn) => {
    btn.onclick = () => { state.orderStatusFilter = btn.dataset.orderFilter; render(); };
  });

  viewRoot.querySelectorAll("[data-dispatch-order]").forEach((btn) => {
    btn.onclick = () => {
      const ord = state.orders.find((o) => o.id === btn.dataset.dispatchOrder);
      if (ord) openDispatchModal(state, { prefillOrder: ord }, modalActions);
    };
  });

  viewRoot.querySelectorAll("[data-schedule-order]").forEach((btn) => {
    btn.onclick = () => {
      const ord = state.orders.find((o) => o.id === btn.dataset.scheduleOrder);
      if (ord) openScheduleModal(state, { prefillOrder: ord }, modalActions);
    };
  });

  viewRoot.querySelectorAll("[data-edit-order]").forEach((btn) => {
    btn.onclick = () => {
      const ord = state.orders.find((o) => o.id === btn.dataset.editOrder);
      if (ord) openOrderModal(state, ord, modalActions);
    };
  });

  viewRoot.querySelectorAll("[data-cancel-order]").forEach((btn) => {
    btn.onclick = () => {
      const ord = state.orders.find((o) => o.id === btn.dataset.cancelOrder);
      if (!ord) return;
      openConfirmModal({
        title: `Cancel Order ${ord.orderCode}?`,
        message: `Mark ${ord.orderCode} (${ord.customer}) as Cancelled? You can restore it later.`,
        confirmLabel: "Cancel Order",
        danger: true,
        onConfirm: async () => {
          await updateDoc(
            doc(db, "users", ownerId, "orders", ord.id),
            buildOrderUpdatePayload({ ...ord, status: "Cancelled" }, ord.dispatchedWeightKg, serverTimestamp())
          );
          showToast(`Cancelled order ${ord.orderCode}.`);
          logBreadcrumb({ category: "Orders", action: "Cancelled Order", details: ord.orderCode, entityRef: ord.id, severity: "Warning" });
        }
      });
    };
  });

  viewRoot.querySelectorAll("[data-restore-order]").forEach((btn) => {
    btn.onclick = async () => {
      const ord = state.orders.find((o) => o.id === btn.dataset.restoreOrder);
      if (!ord) return;
      const disp = Number(ord.dispatchedWeightKg || 0);
      const restoredStatus = disp > 0 ? "Partially Dispatched" : "Open";
      const ref = doc(db, "users", ownerId, "orders", ord.id);
      await commitOperationsInChunks([
        { type: "delete", ref },
        {
          type: "set",
          ref,
          data: buildOrderCreatePayload(ownerId, { ...ord, status: restoredStatus }, serverTimestamp())
        }
      ]);
      showToast(`Restored order ${ord.orderCode} to ${restoredStatus}.`);
      logBreadcrumb({ category: "Orders", action: "Restored Order", details: ord.orderCode, entityRef: ord.id, severity: "Success" });
    };
  });

  viewRoot.querySelectorAll("[data-delete-order]").forEach((btn) => {
    btn.onclick = () => {
      const ord = state.orders.find((o) => o.id === btn.dataset.deleteOrder);
      if (!ord) return;
      openConfirmModal({
        title: `Delete Order ${ord.orderCode}?`,
        message: "Permanently delete this customer order?",
        confirmLabel: "Delete",
        danger: true,
        onConfirm: async () => {
          await deleteDoc(doc(db, "users", ownerId, "orders", ord.id));
          showToast(`Deleted order ${ord.orderCode}.`);
          logBreadcrumb({ category: "Orders", action: "Deleted Order", details: ord.orderCode, entityRef: ord.id, severity: "Warning" });
        }
      });
    };
  });

  // Dispatches actions
  const dispAddBtn = document.getElementById("dispatches-add-btn");
  if (dispAddBtn) dispAddBtn.onclick = () => openDispatchModal(state, {}, modalActions);

  viewRoot.querySelectorAll("[data-dispatch-filter]").forEach((btn) => {
    btn.onclick = () => { state.dispatchStatusFilter = btn.dataset.dispatchFilter; render(); };
  });

  viewRoot.querySelectorAll("[data-edit-dispatch]").forEach((btn) => {
    btn.onclick = () => {
      const d = state.dispatches.find((x) => x.id === btn.dataset.editDispatch);
      if (d) openDispatchModal(state, { dispatch: d }, modalActions);
    };
  });

  viewRoot.querySelectorAll("[data-delete-dispatch]").forEach((btn) => {
    btn.onclick = () => {
      const d = state.dispatches.find((x) => x.id === btn.dataset.deleteDispatch);
      if (!d) return;
      openConfirmModal({
        title: `Delete Dispatch ${d.drReference}?`,
        message: "Deleting this dispatch will recalculate any linked customer order progress.",
        confirmLabel: "Delete Dispatch",
        danger: true,
        onConfirm: async () => {
          const ops = [{ type: "delete", ref: doc(db, "users", ownerId, "dispatches", d.id) }];
          if (d.linkedOrderId) {
            const remaining = state.dispatches.filter((x) => x.id !== d.id);
            applyRecalcOrderOps(ops, ownerId, d.linkedOrderId, remaining);
          }
          await commitOperationsInChunks(ops);
          showToast(`Deleted dispatch ${d.drReference}.`);
          logBreadcrumb({ category: "Dispatch", action: "Deleted Dispatch", details: d.drReference, entityRef: d.id, severity: "Warning" });
        }
      });
    };
  });

  // Schedule actions
  const schedAddBtn = document.getElementById("schedule-add-btn");
  if (schedAddBtn) schedAddBtn.onclick = () => openScheduleModal(state, {}, modalActions);

  viewRoot.querySelectorAll("[data-schedule-filter]").forEach((btn) => {
    btn.onclick = () => { state.scheduleStatusFilter = btn.dataset.scheduleFilter; render(); };
  });

  viewRoot.querySelectorAll("[data-start-schedule]").forEach((btn) => {
    btn.onclick = async () => {
      const sch = state.schedules.find((x) => x.id === btn.dataset.startSchedule);
      if (!sch) return;
      await updateDoc(doc(db, "users", ownerId, "schedules", sch.id), {
        status: "In Progress",
        updatedAt: serverTimestamp()
      });
      showToast(`Started milling run: ${sch.productName}.`);
      logBreadcrumb({ category: "Schedule", action: "Started Milling Run", details: sch.productName, entityRef: sch.id, severity: "Info" });
    };
  });

  viewRoot.querySelectorAll("[data-complete-schedule]").forEach((btn) => {
    btn.onclick = () => {
      const sch = state.schedules.find((x) => x.id === btn.dataset.completeSchedule);
      if (sch) openCompleteScheduleModal(sch, modalActions);
    };
  });

  viewRoot.querySelectorAll("[data-edit-schedule]").forEach((btn) => {
    btn.onclick = () => {
      const sch = state.schedules.find((x) => x.id === btn.dataset.editSchedule);
      if (sch) openScheduleModal(state, { schedule: sch }, modalActions);
    };
  });

  viewRoot.querySelectorAll("[data-cancel-schedule]").forEach((btn) => {
    btn.onclick = async () => {
      const sch = state.schedules.find((x) => x.id === btn.dataset.cancelSchedule);
      if (!sch) return;
      await updateDoc(doc(db, "users", ownerId, "schedules", sch.id), {
        status: "Cancelled",
        updatedAt: serverTimestamp()
      });
      showToast(`Cancelled schedule for ${sch.productName}.`);
      logBreadcrumb({ category: "Schedule", action: "Cancelled Schedule", details: sch.productName, entityRef: sch.id, severity: "Warning" });
    };
  });

  viewRoot.querySelectorAll("[data-delete-schedule]").forEach((btn) => {
    btn.onclick = () => {
      const sch = state.schedules.find((x) => x.id === btn.dataset.deleteSchedule);
      if (!sch) return;
      openConfirmModal({
        title: `Delete Schedule • ${sch.productName}`,
        message: "Remove this scheduled milling run?",
        confirmLabel: "Delete",
        danger: true,
        onConfirm: async () => {
          await deleteDoc(doc(db, "users", ownerId, "schedules", sch.id));
          showToast("Schedule deleted.");
          logBreadcrumb({ category: "Schedule", action: "Deleted Schedule", details: sch.productName, entityRef: sch.id, severity: "Warning" });
        }
      });
    };
  });

  // Recovery / Daily Log / Monitor actions
  viewRoot.querySelectorAll("[data-recovery-sub]").forEach((btn) => {
    btn.onclick = () => { state.recoverySubTab = btn.dataset.recoverySub; render(); };
  });

  const dailyDateSel = document.getElementById("daily-log-date-select");
  if (dailyDateSel) {
    dailyDateSel.onchange = () => { state.selectedDailyLogDate = dailyDateSel.value; render(); };
  }

  const batchAddBtn = document.getElementById("batch-add-btn");
  if (batchAddBtn) batchAddBtn.onclick = () => openBatchModal(state, null, modalActions);

  const batchBulkBtn = document.getElementById("batch-bulk-btn");
  if (batchBulkBtn) batchBulkBtn.onclick = () => openBulkBatchModal(modalActions);

  viewRoot.querySelectorAll("[data-edit-batch]").forEach((btn) => {
    btn.onclick = () => {
      const b = state.batches.find((x) => x.id === btn.dataset.editBatch);
      if (b) openBatchModal(state, b, modalActions);
    };
  });

  viewRoot.querySelectorAll("[data-delete-batch]").forEach((btn) => {
    btn.onclick = () => {
      const b = state.batches.find((x) => x.id === btn.dataset.deleteBatch);
      if (!b) return;
      openConfirmModal({
        title: `Delete Batch • ${b.productName}`,
        message: `Delete production batch (${b.bagsProduced} bags on ${b.date})?`,
        confirmLabel: "Delete Batch",
        danger: true,
        onConfirm: async () => {
          await deleteDoc(doc(db, "users", ownerId, "production_batches", b.id));
          showToast("Production batch deleted.");
          logBreadcrumb({ category: "Recovery", action: "Deleted Batch", details: `${b.productName} (${b.date})`, entityRef: b.id, severity: "Warning" });
        }
      });
    };
  });

  // Materials & BOM actions
  viewRoot.querySelectorAll("[data-materials-sub]").forEach((btn) => {
    btn.onclick = () => { state.materialsSubTab = btn.dataset.materialsSub; render(); };
  });

  const restoreCatBtn = document.getElementById("restore-raw-catalog-btn");
  if (restoreCatBtn) {
    restoreCatBtn.onclick = async () => {
      const sub = state.materialsSubTab;
      const isMicro = sub === "micro" ? true : sub === "macro" ? false : null;
      const missing = missingCatalogEntities(state.rawMaterials, isMicro);
      if (!missing.length) return;
      const existingIds = new Set(state.rawMaterials.map((m) => m.id));
      const ops = missing.map((spec) => {
        const docId = existingIds.has(spec.idHint) ? makeDocId("raw") : spec.idHint;
        return {
          type: "set",
          ref: doc(db, "users", ownerId, "raw_materials", docId),
          data: buildRawMaterialCreatePayload(
            ownerId,
            {
              name: spec.name,
              category: spec.category,
              currentStockKg: spec.defaultStockKg,
              reorderThresholdKg: spec.defaultReorderKg,
              usageSharePct: spec.defaultInclusionPct,
              leadTimeDays: spec.defaultLeadDays,
              safetyDays: 2,
              supplierName: spec.defaultSupplier,
              alertsEnabled: true
            },
            serverTimestamp()
          )
        };
      });
      await commitOperationsInChunks(ops);
      showToast(`Added ${missing.length} catalog raw materials.`);
      logBreadcrumb({ category: "Inventory", action: "Restored Raw Material Catalog", details: `${missing.length} items added`, severity: "Success" });
    };
  }

  const rawAddBtn = document.getElementById("raw-add-btn");
  if (rawAddBtn) rawAddBtn.onclick = () => openRawMaterialModal(null, modalActions);

  const rawMultSel = document.getElementById("raw-mult-select");
  if (rawMultSel) rawMultSel.onchange = () => { state.rawDemandMultiplier = Number(rawMultSel.value || 1); render(); };

  const rawSchedToggle = document.getElementById("raw-sched-toggle");
  if (rawSchedToggle) rawSchedToggle.onchange = () => { state.rawIncludeScheduled = rawSchedToggle.checked; render(); };

  const rawLeadInput = document.getElementById("raw-lead-input");
  if (rawLeadInput) {
    rawLeadInput.onchange = () => {
      const v = rawLeadInput.value.trim();
      state.rawLeadOverrideDays = v === "" ? null : Math.min(60, Math.max(1, Number(v)));
      render();
    };
  }

  const rawRiskSel = document.getElementById("raw-risk-select");
  if (rawRiskSel) rawRiskSel.onchange = () => { state.rawRiskFilter = rawRiskSel.value; render(); };

  viewRoot.querySelectorAll("[data-adjust-raw]").forEach((btn) => {
    btn.onclick = () => {
      const m = state.rawMaterials.find((x) => x.id === btn.dataset.adjustRaw);
      if (m) openAdjustRawModal(m, modalActions);
    };
  });

  viewRoot.querySelectorAll("[data-edit-raw]").forEach((btn) => {
    btn.onclick = () => {
      const m = state.rawMaterials.find((x) => x.id === btn.dataset.editRaw);
      if (m) openRawMaterialModal(m, modalActions);
    };
  });

  viewRoot.querySelectorAll("[data-delete-raw]").forEach((btn) => {
    btn.onclick = () => {
      const m = state.rawMaterials.find((x) => x.id === btn.dataset.deleteRaw);
      if (!m) return;
      openConfirmModal({
        title: `Delete Raw Material • ${m.name}`,
        message: "Remove this ingredient from raw material tracking?",
        confirmLabel: "Delete",
        danger: true,
        onConfirm: async () => {
          await deleteDoc(doc(db, "users", ownerId, "raw_materials", m.id));
          showToast(`Deleted ${m.name}.`);
          logBreadcrumb({ category: "Inventory", action: "Deleted Raw Material", details: m.name, entityRef: m.id, severity: "Warning" });
        }
      });
    };
  });

  const bomAddBtn = document.getElementById("bom-add-btn");
  if (bomAddBtn) bomAddBtn.onclick = () => openBomModal(state, null, modalActions);

  viewRoot.querySelectorAll("[data-edit-bom]").forEach((btn) => {
    btn.onclick = () => {
      const bom = state.boms.find((x) => x.id === btn.dataset.editBom);
      if (bom) openBomModal(state, bom, modalActions);
    };
  });

  viewRoot.querySelectorAll("[data-delete-bom]").forEach((btn) => {
    btn.onclick = () => {
      const bom = state.boms.find((x) => x.id === btn.dataset.deleteBom);
      if (!bom) return;
      openConfirmModal({
        title: `Delete BOM Formula • ${bom.productName}`,
        message: "Delete this feed formulation?",
        confirmLabel: "Delete BOM",
        danger: true,
        onConfirm: async () => {
          await deleteDoc(doc(db, "users", ownerId, "boms", bom.id));
          showToast(`Deleted BOM for ${bom.productName}.`);
          logBreadcrumb({ category: "Inventory", action: "Deleted Feed BOM", details: bom.productName, entityRef: bom.id, severity: "Warning" });
        }
      });
    };
  });

  // Reprocess actions
  const repAddBtn = document.getElementById("reprocess-add-btn");
  if (repAddBtn) repAddBtn.onclick = () => openReprocessModal(state, {}, modalActions);

  viewRoot.querySelectorAll("[data-queue-return-dispatch]").forEach((btn) => {
    btn.onclick = () => {
      const d = state.dispatches.find((x) => x.id === btn.dataset.queueReturnDispatch);
      if (d) openReprocessModal(state, { prefillDispatch: d }, modalActions);
    };
  });

  viewRoot.querySelectorAll("[data-reprocess-filter]").forEach((btn) => {
    btn.onclick = () => { state.reprocessFilter = btn.dataset.reprocessFilter; render(); };
  });

  viewRoot.querySelectorAll("[data-start-reprocess]").forEach((btn) => {
    btn.onclick = async () => {
      const item = state.reprocessItems.find((x) => x.id === btn.dataset.startReprocess);
      if (!item) return;
      await updateDoc(
        doc(db, "users", ownerId, "reprocess_items", item.id),
        buildReprocessUpdatePayload({ ...item, status: "Reprocessing", flaggedToInventory: false }, serverTimestamp())
      );
      showToast(`Started reprocessing for ${item.productName}.`);
    };
  });

  viewRoot.querySelectorAll("[data-flag-reprocess]").forEach((btn) => {
    btn.onclick = () => {
      const item = state.reprocessItems.find((x) => x.id === btn.dataset.flagReprocess);
      if (item) openFlagReprocessModal(item, modalActions);
    };
  });

  viewRoot.querySelectorAll("[data-damage-reprocess]").forEach((btn) => {
    btn.onclick = () => {
      const item = state.reprocessItems.find((x) => x.id === btn.dataset.damageReprocess);
      if (!item) return;
      openConfirmModal({
        title: `Mark Damaged • ${item.productName}`,
        message: "Mark this returned feed as Damaged (write-off, will NOT return to finished stock)?",
        confirmLabel: "Mark Damaged",
        danger: true,
        onConfirm: async () => {
          await updateDoc(
            doc(db, "users", ownerId, "reprocess_items", item.id),
            buildReprocessUpdatePayload(
              { ...item, status: "Damaged", flaggedToInventory: false, fastActionNeeded: false },
              serverTimestamp()
            )
          );
          showToast(`Marked ${item.productName} return as Damaged.`);
          logBreadcrumb({ category: "Reprocess", action: "Marked Return Damaged", details: `${item.productName} (${item.returnedBags} b)`, entityRef: item.id, severity: "Warning" });
        }
      });
    };
  });

  viewRoot.querySelectorAll("[data-edit-reprocess]").forEach((btn) => {
    btn.onclick = () => {
      const item = state.reprocessItems.find((x) => x.id === btn.dataset.editReprocess);
      if (item) openReprocessModal(state, { item }, modalActions);
    };
  });

  viewRoot.querySelectorAll("[data-delete-reprocess]").forEach((btn) => {
    btn.onclick = () => {
      const item = state.reprocessItems.find((x) => x.id === btn.dataset.deleteReprocess);
      if (!item) return;
      openConfirmModal({
        title: `Delete Reprocess Record • ${item.productName}`,
        message: "Remove this reprocess entry?",
        confirmLabel: "Delete",
        danger: true,
        onConfirm: async () => {
          await deleteDoc(doc(db, "users", ownerId, "reprocess_items", item.id));
          showToast("Reprocess item deleted.");
          logBreadcrumb({ category: "Reprocess", action: "Deleted Reprocess Item", details: item.productName, entityRef: item.id, severity: "Warning" });
        }
      });
    };
  });

  // Customers actions
  const custAddBtn = document.getElementById("customer-add-btn");
  if (custAddBtn) custAddBtn.onclick = () => openCustomerModal(null, modalActions);

  viewRoot.querySelectorAll("[data-edit-customer]").forEach((btn) => {
    btn.onclick = () => {
      const c = state.customers.find((x) => x.id === btn.dataset.editCustomer);
      if (c) openCustomerModal(c, modalActions);
    };
  });

  viewRoot.querySelectorAll("[data-delete-customer]").forEach((btn) => {
    btn.onclick = () => {
      const c = state.customers.find((x) => x.id === btn.dataset.deleteCustomer);
      if (!c) return;
      openConfirmModal({
        title: `Delete Customer • ${c.name}`,
        message: "Remove this customer from the directory?",
        confirmLabel: "Delete",
        danger: true,
        onConfirm: async () => {
          await deleteDoc(doc(db, "users", ownerId, "customers", c.id));
          showToast(`Deleted customer ${c.name}.`);
          logBreadcrumb({ category: "Customers", action: "Deleted Customer", details: c.name, entityRef: c.id, severity: "Warning" });
        }
      });
    };
  });

  // Reports & Backup actions
  const buildCurrentReport = () =>
    buildPlantReport({
      startDate: state.reportStartDate,
      endDate: state.reportEndDate,
      sectionFilter: state.reportSectionFilter,
      includedSections: state.reportIncludedSections,
      skus: state.skus,
      batches: state.batches,
      dispatches: state.dispatches,
      orders: state.orders,
      schedules: state.schedules,
      summaries
    });

  const repStart = document.getElementById("report-start-date");
  if (repStart) repStart.onchange = () => { state.reportStartDate = repStart.value; render(); };

  const repEnd = document.getElementById("report-end-date");
  if (repEnd) repEnd.onchange = () => { state.reportEndDate = repEnd.value; render(); };

  const repSec = document.getElementById("report-section-select");
  if (repSec) repSec.onchange = () => { state.reportSectionFilter = repSec.value; render(); };

  const repToday = document.getElementById("report-today-btn");
  if (repToday) {
    repToday.onclick = () => {
      const t = todayIsoDate();
      state.reportStartDate = t;
      state.reportEndDate = t;
      render();
    };
  }

  viewRoot.querySelectorAll("[data-toggle-report-section]").forEach((btn) => {
    btn.onclick = () => {
      const key = btn.dataset.toggleReportSection;
      const cur = new Set(state.reportIncludedSections);
      if (cur.has(key) && cur.size > 1) cur.delete(key);
      else cur.add(key);
      state.reportIncludedSections = REPORT_SECTION_OPTIONS.filter((o) => cur.has(o));
      render();
    };
  });

  const repCsvBtn = document.getElementById("report-download-csv-btn");
  if (repCsvBtn) {
    repCsvBtn.onclick = () => {
      const r = buildCurrentReport();
      const rows = r.sectionSummaries.map((s) => [
        s.section,
        s.skuCount,
        s.totalAvailableBags.toFixed(1),
        s.totalAvailableStockKg.toFixed(1),
        s.totalBags,
        s.totalRecoveredKg.toFixed(1),
        s.recoveryPct.toFixed(2)
      ]);
      downloadCsv(
        `bulacan_plant_report_${r.startDate}_to_${r.endDate}.csv`,
        ["Plant Section", "SKUs", "Available Bags", "Available Kg", "Produced Bags", "Recovered Kg", "Recovery %"],
        rows
      );
      logBreadcrumb({ category: "Reports", action: "Exported Plant CSV", details: `${r.startDate} to ${r.endDate}`, severity: "Info" });
    };
  }

  const repXlsBtn = document.getElementById("report-download-xls-btn");
  if (repXlsBtn) {
    repXlsBtn.onclick = () => {
      const r = buildCurrentReport();
      const html = `
        <html><head><meta charset="utf-8"/></head><body>
          <h2>10K AGRI FEEDS • BULACAN PLANT REPORT (${escapeHtml(r.startDate)} to ${escapeHtml(r.endDate)})</h2>
          <p>Plant Filter: ${escapeHtml(r.sectionFilter)} | Dispatched: ${r.totalDispatchedKg.toFixed(0)} kg | Open Orders: ${r.totalOpenOrdersKg.toFixed(0)} kg</p>
          <table border="1" cellspacing="0" cellpadding="6">
            <thead><tr><th>Section</th><th>SKUs</th><th>Available Bags</th><th>Available Kg</th><th>Produced Bags</th><th>Recovered Kg</th><th>Recovery %</th></tr></thead>
            <tbody>
              ${r.sectionSummaries
                .map(
                  (s) => `<tr><td>${escapeHtml(s.section)}</td><td>${s.skuCount}</td><td>${s.totalAvailableBags.toFixed(1)}</td><td>${s.totalAvailableStockKg.toFixed(1)}</td><td>${s.totalBags}</td><td>${s.totalRecoveredKg.toFixed(1)}</td><td>${s.recoveryPct.toFixed(2)}%</td></tr>`
                )
                .join("")}
            </tbody>
          </table>
        </body></html>
      `;
      downloadFile(`bulacan_plant_report_${r.startDate}.xls`, html, "application/vnd.ms-excel");
      logBreadcrumb({ category: "Reports", action: "Exported Excel Report", details: `${r.startDate} to ${r.endDate}`, severity: "Info" });
    };
  }

  const repPdfBtn = document.getElementById("report-print-pdf-btn");
  if (repPdfBtn) {
    repPdfBtn.onclick = () => {
      window.print();
      logBreadcrumb({ category: "Reports", action: "Printed PDF Report", details: `${state.reportStartDate} to ${state.reportEndDate}`, severity: "Info" });
    };
  }

  const repMsgBtn = document.getElementById("report-copy-messenger-btn");
  if (repMsgBtn) {
    repMsgBtn.onclick = async () => {
      const text = buildMessengerSummaryText(buildCurrentReport());
      await navigator.clipboard.writeText(text);
      showToast("Copied formatted Messenger summary to clipboard.");
    };
  }

  const exportJsonBtn = document.getElementById("export-json-backup-btn");
  if (exportJsonBtn) {
    exportJsonBtn.onclick = () => {
      const snapshot = {
        exportedAt: new Date().toISOString(),
        workspaceOwnerId: ownerId,
        skus: state.skus,
        customers: state.customers,
        orders: state.orders,
        dispatches: state.dispatches,
        schedules: state.schedules,
        batches: state.batches,
        rawMaterials: state.rawMaterials,
        boms: state.boms,
        reprocessItems: state.reprocessItems
      };
      downloadFile(`feedmill_backup_${todayIsoDate()}.json`, JSON.stringify(snapshot, null, 2), "application/json");
      logBreadcrumb({ category: "Reports", action: "Exported JSON Backup", details: `${state.skus.length} SKUs, ${state.orders.length} orders`, severity: "Info" });
    };
  }

  const restoreJsonBtn = document.getElementById("restore-json-backup-btn");
  if (restoreJsonBtn) restoreJsonBtn.onclick = () => openRestoreJsonModal(modalActions);

  const selectiveResetBtn = document.getElementById("selective-reset-btn");
  if (selectiveResetBtn) selectiveResetBtn.onclick = () => openSelectiveResetModal(modalActions);

  // Per-tab quick CSV export button
  const csvBtn = document.getElementById("export-csv-btn");
  if (csvBtn) {
    csvBtn.onclick = () => {
      if (state.activeTab === "inventory") {
        downloadCsv(
          `feedmill_inventory_${todayIsoDate()}.csv`,
          ["Product", "Section", "Available Bags", "Available Kg", "Open Orders Kg", "Need Kg", "Cover Days", "Status"],
          summaries.map((s) => [
            s.sku.productName,
            s.sku.section,
            s.availableBags.toFixed(1),
            s.availableStockKg.toFixed(1),
            s.openOrdersKg.toFixed(1),
            s.netProductionNeededKg.toFixed(1),
            Number.isFinite(s.daysOfCover) ? s.daysOfCover.toFixed(1) : "Inf",
            s.status
          ])
        );
      } else if (state.activeTab === "orders") {
        downloadCsv(
          `feedmill_orders_${todayIsoDate()}.csv`,
          ["Order Code", "Customer", "Product", "Ordered Bags", "Total Kg", "Dispatched Kg", "Due Date", "Status"],
          state.orders.map((o) => [o.orderCode, o.customer, o.productName, o.orderedBags, o.totalWeightKg, o.dispatchedWeightKg, o.dueDate, o.status])
        );
      } else if (state.activeTab === "dispatched") {
        downloadCsv(
          `feedmill_dispatches_${todayIsoDate()}.csv`,
          ["Date", "DR Ref", "Customer", "Product", "Bags", "Net Weight Kg", "Driver", "Truck", "Status"],
          state.dispatches.map((d) => [d.date, d.drReference, d.customer, d.productName, d.bagsDispatched, effectiveDispatchWeightKg(d), d.driverName || "", d.truckNumber || "", resolvedDispatchStatus(d)])
        );
      } else if (state.activeTab === "recovery") {
        downloadCsv(
          `feedmill_batches_${todayIsoDate()}.csv`,
          ["Date", "Section", "Product", "Bags", "Expected Kg", "Recovered Kg", "Recovery %", "Remarks"],
          state.batches.map((b) => [b.date, b.section, b.productName, b.bagsProduced, b.expectedWeightKg, b.actualRecoveredKg, b.recoveryPct, b.remarks])
        );
      } else if (state.activeTab === "materials") {
        const forecasts = computeRawMaterialForecasts({
          materials: state.rawMaterials,
          batches: state.batches,
          schedules: state.schedules,
          boms: state.boms,
          lookbackDays: state.simLookbackDays,
          demandMultiplier: state.rawDemandMultiplier,
          includeScheduledDemand: state.rawIncludeScheduled,
          leadTimeOverrideDays: state.rawLeadOverrideDays
        });
        downloadCsv(
          `feedmill_raw_materials_${todayIsoDate()}.csv`,
          ["Material", "Sector", "Movement", "On Hand Kg", "Usage/Day Kg", "Runway", "Restock Kg", "Status"],
          forecasts.map((f) => [
            f.material.name,
            f.sector.label,
            f.movementTag,
            f.material.currentStockKg,
            f.simulatedAvgDailyKg.toFixed(1),
            f.projectedRunOutLabel,
            f.recommendedRestockKg.toFixed(0),
            f.status
          ])
        );
      }
    };
  }

  // Team & Access bindings
  const copyWebBtn = document.getElementById("copy-web-url-btn");
  if (copyWebBtn) {
    copyWebBtn.onclick = async () => {
      const urlToCopy = getCurrentPortalUrl();
      await navigator.clipboard.writeText(urlToCopy);
      showToast(`Copied URL: ${urlToCopy}`);
    };
  }
  const teamInviteBtn = document.getElementById("team-invite-btn");
  if (teamInviteBtn) teamInviteBtn.onclick = () => openInviteMemberModal(modalActions);

  viewRoot.querySelectorAll("[data-gmail-invite]").forEach((btn) => {
    btn.onclick = () => {
      const inv = state.workspaceInvites.find((i) => i.id === btn.dataset.gmailInvite);
      if (!inv) return;
      const mail = buildInviteEmailPayload({
        recipientEmail: inv.memberEmail,
        recipientName: inv.memberDisplayName,
        role: inv.role,
        inviterEmail: state.user?.email || "",
        workspaceOwnerId: inv.workspaceOwnerId || ownerId,
        inviteId: inv.id,
        baseUrl: getWebOriginBase()
      });
      window.open(buildGmailWebComposeUrl({ to: inv.memberEmail, subject: mail.subject, body: mail.body }), "_blank", "noopener");
    };
  });

  viewRoot.querySelectorAll("[data-copy-invite-link]").forEach((btn) => {
    btn.onclick = async () => {
      const inv = state.workspaceInvites.find((i) => i.id === btn.dataset.copyInviteLink);
      if (!inv) return;
      const link = buildWebInviteLink({
        baseUrl: getWebOriginBase(),
        workspaceOwnerId: inv.workspaceOwnerId || ownerId,
        recipientEmail: inv.memberEmail,
        role: inv.role,
        inviteId: inv.id
      });
      await navigator.clipboard.writeText(link);
      showToast(`Invite link copied for ${inv.memberEmail}.`);
    };
  });

  viewRoot.querySelectorAll("[data-cancel-invite]").forEach((btn) => {
    btn.onclick = async () => {
      const invId = btn.dataset.cancelInvite;
      const batch = writeBatch(db);
      batch.delete(doc(db, "workspace_invites", invId));
      batch.delete(doc(db, "users", ownerId, "team_invites", invId));
      await batch.commit();
      showToast("Invite cancelled.");
    };
  });

  viewRoot.querySelectorAll("[data-switch-member-role]").forEach((btn) => {
    btn.onclick = () => {
      const targetRole = btn.dataset.switchMemberRole;
      const memberUid = btn.dataset.memberUid;
      const existing = state.teamMembers.find((m) => m.userId === memberUid);
      if (existing && resolveRole(existing.role).label === resolveRole(targetRole).label) return;
      switchMemberAccessRole({
        memberUid,
        inviteId: btn.dataset.inviteId || "",
        memberEmail: btn.dataset.memberEmail || "",
        memberName: btn.dataset.memberName || "",
        newRole: targetRole
      });
    };
  });

  viewRoot.querySelectorAll("[data-switch-invite-role]").forEach((btn) => {
    btn.onclick = () => {
      const targetRole = btn.dataset.switchInviteRole;
      const inviteId = btn.dataset.inviteId;
      const existing = state.workspaceInvites.find((i) => i.id === inviteId);
      if (existing && resolveRole(existing.role).label === resolveRole(targetRole).label) return;
      switchPendingInviteRole({
        inviteId,
        memberEmail: btn.dataset.memberEmail || "",
        newRole: targetRole
      });
    };
  });

  viewRoot.querySelectorAll("[data-remove-member]").forEach((btn) => {
    btn.onclick = async () => {
      const memberUid = btn.dataset.removeMember;
      const invId = btn.dataset.inviteId;
      const batch = writeBatch(db);
      batch.delete(doc(db, "users", ownerId, "team_members", memberUid));
      if (invId) {
        batch.delete(doc(db, "workspace_invites", invId));
        batch.delete(doc(db, "users", ownerId, "team_invites", invId));
      }
      await batch.commit();
      showToast("Team member removed.");
    };
  });

  // History actions
  viewRoot.querySelectorAll("[data-history-cat]").forEach((btn) => {
    btn.onclick = () => { state.historyCategoryFilter = btn.dataset.historyCat; render(); };
  });

  viewRoot.querySelectorAll("[data-delete-breadcrumb]").forEach((btn) => {
    btn.onclick = async () => {
      await deleteDoc(doc(db, "users", ownerId, "breadcrumbs", btn.dataset.deleteBreadcrumb));
      showToast("History entry deleted.");
    };
  });

  const clearHistBtn = document.getElementById("clear-history-btn");
  if (clearHistBtn && activeRole.canResetWorkspace) {
    clearHistBtn.onclick = () => {
      openConfirmModal({
        title: "Clear All Activity History?",
        message: `Permanently delete ${state.breadcrumbs.length} history logs from this workspace?`,
        confirmLabel: "Clear History",
        danger: true,
        onConfirm: async () => {
          const ops = state.breadcrumbs.map((b) => ({
            type: "delete",
            ref: doc(db, "users", ownerId, "breadcrumbs", b.id)
          }));
          await commitOperationsInChunks(ops);
          showToast("Cleared activity history.");
        }
      });
    };
  }
}

document.getElementById("google-signin-btn").onclick = async () => {
  const errEl = document.getElementById("auth-error");
  errEl.classList.add("hidden");
  try {
    await signInWithPopup(auth, googleProvider);
  } catch (err) {
    const code = err?.code || "";
    if (code === "auth/unauthorized-domain") {
      const host = window.location.hostname || "localhost";
      errEl.innerHTML =
        `Domain <strong>${escapeHtml(host)}</strong> is not authorized yet in Firebase Auth. ` +
        `Open <a href="https://console.firebase.google.com/project/${FIRESTORE_PROJECT_ID}/authentication/settings" target="_blank" rel="noopener">Firebase Authentication Settings</a> ` +
        `→ <strong>Authorized domains</strong> → click <strong>Add domain</strong> → add <strong>${escapeHtml(host)}</strong>, then try Sign in again.`;
    } else {
      errEl.textContent = err?.message || "Google Sign-In failed.";
    }
    errEl.classList.remove("hidden");
  }
};

document.getElementById("signout-btn").onclick = async () => {
  clearWorkspaceListeners();
  if (state.unsubInvites) state.unsubInvites();
  await signOut(auth);
};

onAuthStateChanged(auth, (user) => {
  state.user = user;
  if (!user) {
    clearWorkspaceListeners();
    if (state.unsubInvites) state.unsubInvites();
    render();
    return;
  }
  const savedWs = localStorage.getItem(`feedmill_web_ws_${user.uid}`) || user.uid;
  subscribeWorkspace(savedWs);
  subscribeUserInvites(user);
  render();
});
