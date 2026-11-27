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
  updateDoc,
  where,
  writeBatch
} from "https://www.gstatic.com/firebasejs/11.6.0/firebase-firestore.js";
import {
  FIREBASE_CONFIG,
  FIRESTORE_DATABASE_ID,
  HOSTED_WEB_URL,
  PLANT_SECTIONS,
  RAW_CATEGORIES,
  WORKSPACE_ROLE_OPTIONS,
  buildBatchCreatePayload,
  buildCsvReport,
  buildDispatchCreatePayload,
  buildGmailWebComposeUrl,
  buildInviteDocId,
  buildInviteEmailPayload,
  buildInviteRoleUpdatePayload,
  buildMemberRoleUpdatePayload,
  buildOrderCreatePayload,
  buildRawMaterialCreatePayload,
  buildScheduleCreatePayload,
  buildSkuCreatePayload,
  buildWebInviteLink,
  buildWorkspaceInvitePayload,
  computeKpis,
  computeSkuSummaries,
  effectiveDispatchWeightKg,
  makeDocId,
  parseInviteQueryParams,
  resolveRole,
  todayIsoDate
} from "./operations.js";

const firebaseApp = initializeApp(FIREBASE_CONFIG);
const auth = getAuth(firebaseApp);
const db = getFirestore(firebaseApp, FIRESTORE_DATABASE_ID);
const googleProvider = new GoogleAuthProvider();
googleProvider.setCustomParameters({ prompt: "select_account" });

const TABS = [
  { id: "summary", label: "Summary", subtitle: "Live KPIs, section overview & alerts" },
  { id: "inventory", label: "Finished Goods", subtitle: "Real-time SKU stock, cover & production needs" },
  { id: "orders", label: "Orders", subtitle: "Customer orders & fulfillment tracking" },
  { id: "dispatched", label: "Dispatched", subtitle: "Delivery receipts & outbound dispatches" },
  { id: "schedule", label: "Schedule", subtitle: "Planned milling queue & priorities" },
  { id: "recovery", label: "Recovery", subtitle: "Production batches & recovery variance" },
  { id: "materials", label: "Raw Materials", subtitle: "Ingredient stock, thresholds & suppliers" },
  { id: "reprocess", label: "Reprocess", subtitle: "Returned feed quarantine & re-flagging" },
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
  reprocessItems: [],
  customers: [],
  breadcrumbs: [],
  teamMembers: [],
  workspaceInvites: [],
  myMemberRecord: null,
  updatingAccessKeys: new Set(),
  unsubInvites: null,
  unsubWorkspace: []
};

const deepLinkParams = parseInviteQueryParams(window.location.search);
if (deepLinkParams.action === "team_invite" || deepLinkParams.inviteId || deepLinkParams.workspaceOwnerId) {
  state.activeTab = "team";
}

function escapeHtml(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function showToast(message) {
  const el = document.getElementById("toast");
  el.textContent = message;
  el.classList.remove("hidden");
  clearTimeout(showToast._timer);
  showToast._timer = setTimeout(() => el.classList.add("hidden"), 4000);
}

function formatNum(value, decimals = 1) {
  return Number(value || 0).toLocaleString(undefined, {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  });
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
      if (wsSnap?.exists()) {
        batch.update(wsInvRef, inviteUpdate);
      }
      if (rootSnap?.exists()) {
        batch.update(rootInvRef, inviteUpdate);
      }
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
    if (updatedCount > 0) {
      await batch.commit();
    }
    showToast(`Switched invite for ${memberEmail || "teammate"} to ${resolvedRole}.`);
  } catch (err) {
    showToast(err?.message || "Failed to update invite access.");
  } finally {
    state.updatingAccessKeys.delete(lockKey);
    render();
  }
}

function downloadCsv(filename, headers, rows) {
  const csv = buildCsvReport(headers, rows);
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
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

function openModal(title, bodyHtml, onMount) {
  const root = document.getElementById("modal-root");
  root.innerHTML = `
    <div class="modal">
      <div class="modal-head">
        <h3>${escapeHtml(title)}</h3>
        <button id="modal-close-btn" class="btn btn-sm">Close</button>
      </div>
      ${bodyHtml}
    </div>
  `;
  root.classList.remove("hidden");
  document.getElementById("modal-close-btn").onclick = closeModal;
  root.onclick = (e) => {
    if (e.target === root) closeModal();
  };
  if (onMount) onMount(root);
}

function closeModal() {
  const root = document.getElementById("modal-root");
  root.classList.add("hidden");
  root.innerHTML = "";
}

function openNewOrderModal() {
  const skuOptions = state.skus.map((s) => `<option value="${escapeHtml(s.productName)}">${escapeHtml(s.productName)} (${s.standardBagWeightKg}kg)</option>`).join("");
  openModal(
    "New Customer Order",
    `
    <form id="order-form" class="form-grid">
      <div class="form-row-2">
        <div class="form-field">
          <label>Order Code</label>
          <input name="orderCode" value="ORD-${Date.now().toString().slice(-5)}" required />
        </div>
        <div class="form-field">
          <label>Customer</label>
          <input name="customer" placeholder="e.g., Bulacan Farm Supply" required />
        </div>
      </div>
      <div class="form-field">
        <label>Feed Product (SKU)</label>
        <select name="productName" required>${skuOptions}</select>
      </div>
      <div class="form-row-2">
        <div class="form-field">
          <label>Ordered Bags</label>
          <input name="orderedBags" type="number" min="1" value="40" required />
        </div>
        <div class="form-field">
          <label>Bag Weight (kg)</label>
          <input name="bagWeightKg" type="number" step="0.1" min="0.1" value="${state.skus[0]?.standardBagWeightKg || 25}" required />
        </div>
      </div>
      <div class="form-row-2">
        <div class="form-field">
          <label>Order Date</label>
          <input name="orderDate" type="date" value="${todayIsoDate()}" required />
        </div>
        <div class="form-field">
          <label>Due Date</label>
          <input name="dueDate" type="date" value="${todayIsoDate()}" required />
        </div>
      </div>
      <div style="display:flex; gap:14px; font-size:13px;">
        <label><input type="checkbox" name="priority" /> Priority</label>
        <label><input type="checkbox" name="rushOrder" /> Rush Order</label>
      </div>
      <button type="submit" class="btn btn-primary">Save Order</button>
    </form>
  `,
    (root) => {
      const form = root.querySelector("#order-form");
      form.productName.onchange = () => {
        const sku = state.skus.find((s) => s.productName === form.productName.value);
        if (sku) form.bagWeightKg.value = sku.standardBagWeightKg;
      };
      form.onsubmit = async (e) => {
        e.preventDefault();
        try {
          const payload = buildOrderCreatePayload(
            state.activeWorkspaceOwnerId,
            {
              orderCode: form.orderCode.value,
              customer: form.customer.value,
              productName: form.productName.value,
              orderedBags: form.orderedBags.value,
              bagWeightKg: form.bagWeightKg.value,
              orderDate: form.orderDate.value,
              dueDate: form.dueDate.value,
              priority: form.priority.checked,
              rushOrder: form.rushOrder.checked
            },
            serverTimestamp()
          );
          const batch = writeBatch(db);
          batch.set(doc(db, "users", state.activeWorkspaceOwnerId, "orders", makeDocId("ord")), payload);
          await batch.commit();
          closeModal();
          showToast(`Saved order ${payload.orderCode}.`);
        } catch (err) {
          showToast(err.message || "Failed to save order.");
        }
      };
    }
  );
}

function openLogDispatchModal() {
  const openOrders = state.orders.filter((o) => o.status === "Open" || o.status === "Partially Dispatched");
  const orderOptions = [
    `<option value="">Unlinked / Direct Dispatch</option>`,
    ...openOrders.map(
      (o) =>
        `<option value="${escapeHtml(o.id)}">${escapeHtml(o.orderCode)} • ${escapeHtml(o.customer)} • ${escapeHtml(o.productName)} (${formatNum(Math.max(0, o.totalWeightKg - o.dispatchedWeightKg), 0)} kg left)</option>`
    )
  ].join("");
  const skuOptions = state.skus.map((s) => `<option value="${escapeHtml(s.productName)}">${escapeHtml(s.productName)}</option>`).join("");
  const plantOptions = [`<option value="">Unspecified</option>`, ...PLANT_SECTIONS.map((s) => `<option value="${s}">${s}</option>`)].join("");

  openModal(
    "Log Dispatch",
    `
    <form id="dispatch-form" class="form-grid">
      <div class="form-field">
        <label>Link to Open Order (optional)</label>
        <select name="linkedOrderId">${orderOptions}</select>
      </div>
      <div class="form-row-2">
        <div class="form-field">
          <label>Date</label>
          <input name="date" type="date" value="${todayIsoDate()}" required />
        </div>
        <div class="form-field">
          <label>DR Reference</label>
          <input name="drReference" value="DR-${Date.now().toString().slice(-5)}" required />
        </div>
      </div>
      <div class="form-row-2">
        <div class="form-field">
          <label>Product</label>
          <select name="productName" required>${skuOptions}</select>
        </div>
        <div class="form-field">
          <label>Customer</label>
          <input name="customer" placeholder="Customer name" required />
        </div>
      </div>
      <div class="form-row-2">
        <div class="form-field">
          <label>Bags Dispatched</label>
          <input name="bagsDispatched" type="number" min="1" value="20" required />
        </div>
        <div class="form-field">
          <label>Bag Weight (kg)</label>
          <input name="bagWeightKg" type="number" step="0.1" min="0.1" value="${state.skus[0]?.standardBagWeightKg || 25}" required />
        </div>
      </div>
      <div class="form-row-2">
        <div class="form-field">
          <label>Source Plant</label>
          <select name="sourcePlant">${plantOptions}</select>
        </div>
        <div class="form-field">
          <label>Truck / Driver</label>
          <input name="driverName" placeholder="Optional driver name" />
        </div>
      </div>
      <button type="submit" class="btn btn-primary">Record Dispatch</button>
    </form>
  `,
    (root) => {
      const form = root.querySelector("#dispatch-form");
      form.linkedOrderId.onchange = () => {
        const ord = openOrders.find((o) => o.id === form.linkedOrderId.value);
        if (ord) {
          form.productName.value = ord.productName;
          form.customer.value = ord.customer;
          form.bagWeightKg.value = ord.bagWeightKg;
          const remBags = Math.max(1, Math.round((ord.totalWeightKg - ord.dispatchedWeightKg) / (ord.bagWeightKg || 25)));
          form.bagsDispatched.value = remBags;
        }
      };
      form.onsubmit = async (e) => {
        e.preventDefault();
        try {
          const payload = buildDispatchCreatePayload(
            state.activeWorkspaceOwnerId,
            {
              date: form.date.value,
              drReference: form.drReference.value,
              productName: form.productName.value,
              customer: form.customer.value,
              bagsDispatched: form.bagsDispatched.value,
              bagWeightKg: form.bagWeightKg.value,
              linkedOrderId: form.linkedOrderId.value,
              sourcePlant: form.sourcePlant.value,
              driverName: form.driverName.value
            },
            serverTimestamp()
          );
          const batch = writeBatch(db);
          batch.set(doc(db, "users", state.activeWorkspaceOwnerId, "dispatches", makeDocId("disp")), payload);
          const linked = openOrders.find((o) => o.id === form.linkedOrderId.value);
          if (linked) {
            const newDispatched = Number((Number(linked.dispatchedWeightKg || 0) + payload.totalWeightKg).toFixed(2));
            const newStatus = newDispatched >= Number(linked.totalWeightKg || 0) ? "Fulfilled" : "Partially Dispatched";
            batch.update(doc(db, "users", state.activeWorkspaceOwnerId, "orders", linked.id), {
              dispatchedWeightKg: newDispatched,
              status: newStatus,
              updatedAt: serverTimestamp()
            });
          }
          await batch.commit();
          closeModal();
          showToast(`Logged dispatch ${payload.drReference}.`);
        } catch (err) {
          showToast(err.message || "Failed to log dispatch.");
        }
      };
    }
  );
}

function openLogBatchModal() {
  const skuOptions = state.skus.map((s) => `<option value="${escapeHtml(s.productName)}">${escapeHtml(s.productName)}</option>`).join("");
  const sectionOptions = PLANT_SECTIONS.map((s) => `<option value="${s}">${s}</option>`).join("");
  openModal(
    "Log Production Batch",
    `
    <form id="batch-form" class="form-grid">
      <div class="form-row-2">
        <div class="form-field">
          <label>Date</label>
          <input name="date" type="date" value="${todayIsoDate()}" required />
        </div>
        <div class="form-field">
          <label>Plant Section</label>
          <select name="section">${sectionOptions}</select>
        </div>
      </div>
      <div class="form-field">
        <label>Feed Product</label>
        <select name="productName" required>${skuOptions}</select>
      </div>
      <div class="form-row-2">
        <div class="form-field">
          <label>Bags Produced</label>
          <input name="bagsProduced" type="number" min="1" value="60" required />
        </div>
        <div class="form-field">
          <label>Bag Weight (kg)</label>
          <input name="bagWeightKg" type="number" step="0.1" min="0.1" value="${state.skus[0]?.standardBagWeightKg || 25}" required />
        </div>
      </div>
      <div class="form-row-2">
        <div class="form-field">
          <label>Actual Recovered (kg)</label>
          <input name="actualRecoveredKg" type="number" step="0.1" min="1" value="1500" required />
        </div>
        <div class="form-field">
          <label>Remarks</label>
          <input name="remarks" placeholder="Shift / batch notes" />
        </div>
      </div>
      <button type="submit" class="btn btn-primary">Save Batch</button>
    </form>
  `,
    (root) => {
      const form = root.querySelector("#batch-form");
      const recalc = () => {
        form.actualRecoveredKg.value = Number(form.bagsProduced.value || 0) * Number(form.bagWeightKg.value || 25);
      };
      form.productName.onchange = () => {
        const sku = state.skus.find((s) => s.productName === form.productName.value);
        if (sku) {
          form.section.value = sku.section;
          form.bagWeightKg.value = sku.standardBagWeightKg;
          recalc();
        }
      };
      form.bagsProduced.oninput = recalc;
      form.bagWeightKg.oninput = recalc;
      form.onsubmit = async (e) => {
        e.preventDefault();
        try {
          const payload = buildBatchCreatePayload(
            state.activeWorkspaceOwnerId,
            {
              date: form.date.value,
              section: form.section.value,
              productName: form.productName.value,
              bagsProduced: form.bagsProduced.value,
              bagWeightKg: form.bagWeightKg.value,
              actualRecoveredKg: form.actualRecoveredKg.value,
              remarks: form.remarks.value
            },
            serverTimestamp()
          );
          const batch = writeBatch(db);
          batch.set(doc(db, "users", state.activeWorkspaceOwnerId, "production_batches", makeDocId("bat")), payload);
          await batch.commit();
          closeModal();
          showToast(`Logged ${payload.bagsProduced} bags of ${payload.productName}.`);
        } catch (err) {
          showToast(err.message || "Failed to log batch.");
        }
      };
    }
  );
}

function openInviteMemberModal() {
  openModal(
    "Invite Team Member",
    `
    <form id="invite-form" class="form-grid">
      <div class="form-field">
        <label>Google Email Address</label>
        <input name="email" type="email" placeholder="teammate@gmail.com" required />
      </div>
      <div class="form-row-2">
        <div class="form-field">
          <label>Name / Position (optional)</label>
          <input name="displayName" placeholder="e.g., Shift Supervisor" />
        </div>
        <div class="form-field">
          <label>Role</label>
          <select name="role">
            <option value="Operator">Operator</option>
            <option value="Viewer">Viewer</option>
            <option value="Admin">Admin</option>
          </select>
        </div>
      </div>
      <div class="form-field">
        <label>Personal Note for Gmail (optional)</label>
        <input name="customNote" placeholder="e.g., Join before morning shift" />
      </div>
      <div style="display:flex; gap:10px; flex-wrap:wrap;">
        <button type="submit" data-mode="gmail" class="btn btn-primary">Send to Google + Open Gmail</button>
        <button type="submit" data-mode="google" class="btn">Send to Google Only</button>
      </div>
    </form>
  `,
    (root) => {
      const form = root.querySelector("#invite-form");
      let chosenMode = "gmail";
      root.querySelectorAll("button[data-mode]").forEach((btn) => {
        btn.onclick = () => {
          chosenMode = btn.dataset.mode;
        };
      });
      form.onsubmit = async (e) => {
        e.preventDefault();
        try {
          const cleanEmail = form.email.value.trim().toLowerCase();
          const role = form.role.value;
          const displayName = form.displayName.value.trim();
          const customNote = form.customNote.value.trim();
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

          if (chosenMode === "gmail") {
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
              buildGmailWebComposeUrl({
                to: cleanEmail,
                subject: emailContent.subject,
                body: emailContent.body
              }),
              "_blank",
              "noopener"
            );
          }
        } catch (err) {
          showToast(err.message || "Failed to send team invite.");
        }
      };
    }
  );
}

function statusPill(status) {
  const s = String(status || "");
  if (s === "Healthy" || s === "Fulfilled" || s === "Delivered" || s === "Completed" || s === "Active") {
    return `<span class="pill pill-healthy">${escapeHtml(s)}</span>`;
  }
  if (s === "Critical" || s === "Urgent" || s === "Damaged" || s === "Cancelled") {
    return `<span class="pill pill-critical">${escapeHtml(s)}</span>`;
  }
  if (s === "Low Watch" || s === "Partially Dispatched" || s === "Pending" || s === "Open" || s === "In Progress") {
    return `<span class="pill pill-warn">${escapeHtml(s)}</span>`;
  }
  return `<span class="pill pill-neutral">${escapeHtml(s || "—")}</span>`;
}

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

function renderSummaryTab(summaries, kpis) {
  const criticalList = summaries.filter((s) => s.status === "Critical" || s.status === "Low Watch").slice(0, 8);
  const openOrders = state.orders.filter((o) => o.status === "Open" || o.status === "Partially Dispatched").slice(0, 8);

  return `
    <div class="kpi-grid">
      <div class="card kpi-card">
        <div class="kpi-label">Finished Stock Available</div>
        <div class="kpi-value">${formatNum(kpis.totalStockKg, 0)} kg</div>
        <div class="kpi-sub">${formatNum(kpis.totalBags, 0)} bags across ${state.skus.length} SKUs</div>
      </div>
      <div class="card kpi-card">
        <div class="kpi-label">Open Customer Orders</div>
        <div class="kpi-value">${kpis.openOrdersCount}</div>
        <div class="kpi-sub">${formatNum(kpis.openOrderKg, 0)} kg remaining to dispatch</div>
      </div>
      <div class="card kpi-card">
        <div class="kpi-label">Dispatched Volume</div>
        <div class="kpi-value">${formatNum(kpis.totalDispatchedKg, 0)} kg</div>
        <div class="kpi-sub">${state.dispatches.length} delivery receipts logged</div>
      </div>
      <div class="card kpi-card">
        <div class="kpi-label">Stock Alerts</div>
        <div class="kpi-value">${kpis.criticalSkus} SKUs • ${kpis.lowRawMaterials} Raw</div>
        <div class="kpi-sub">${kpis.activeSchedules} active milling runs queued</div>
      </div>
    </div>

    <div class="two-col">
      <div class="card">
        <div class="section-head">
          <h3>Priority SKU Watchlist</h3>
        </div>
        ${
          criticalList.length
            ? `<div class="table-wrap"><table><thead><tr><th>SKU</th><th>Available</th><th>Open Orders</th><th>Status</th></tr></thead><tbody>
                ${criticalList
                  .map(
                    (s) => `<tr>
                      <td><strong>${escapeHtml(s.sku.productName)}</strong><div style="font-size:11px;color:var(--muted)">${escapeHtml(s.sku.section)}</div></td>
                      <td>${formatNum(s.availableBags, 1)} bags (${formatNum(s.availableStockKg, 0)} kg)</td>
                      <td>${formatNum(s.openOrdersKg, 0)} kg</td>
                      <td>${statusPill(s.status)}</td>
                    </tr>`
                  )
                  .join("")}
              </tbody></table></div>`
            : `<div class="empty-state">All SKUs are healthy right now.</div>`
        }
      </div>

      <div class="card">
        <div class="section-head">
          <h3>Active Customer Orders</h3>
        </div>
        ${
          openOrders.length
            ? `<div class="table-wrap"><table><thead><tr><th>Order</th><th>Customer</th><th>Product</th><th>Status</th></tr></thead><tbody>
                ${openOrders
                  .map(
                    (o) => `<tr>
                      <td><strong>${escapeHtml(o.orderCode)}</strong><div style="font-size:11px;color:var(--muted)">Due ${escapeHtml(o.dueDate)}</div></td>
                      <td>${escapeHtml(o.customer)}</td>
                      <td>${escapeHtml(o.productName)} (${o.orderedBags} bags)</td>
                      <td>${statusPill(o.status)}</td>
                    </tr>`
                  )
                  .join("")}
              </tbody></table></div>`
            : `<div class="empty-state">No open customer orders.</div>`
        }
      </div>
    </div>
  `;
}

function renderInventoryTab(summaries) {
  const q = state.searchText.toLowerCase();
  const rows = summaries.filter(
    (s) =>
      !q ||
      s.sku.productName.toLowerCase().includes(q) ||
      s.sku.section.toLowerCase().includes(q)
  );
  return `
    <div class="card">
      <div class="section-head">
        <h3>Finished Goods Inventory (${rows.length})</h3>
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <input id="tab-search" class="search-input" placeholder="Filter product or plant..." value="${escapeHtml(state.searchText)}" />
          <button id="export-csv-btn" class="btn btn-sm">Export CSV</button>
        </div>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Product</th><th>Section</th><th>Available Bags</th><th>Available (kg)</th><th>Open Orders (kg)</th><th>Cover</th><th>Status</th></tr></thead>
          <tbody>
            ${rows
              .map(
                (s) => `<tr>
                  <td><strong>${escapeHtml(s.sku.productName)}</strong></td>
                  <td>${escapeHtml(s.sku.section)}</td>
                  <td>${formatNum(s.availableBags, 1)}</td>
                  <td>${formatNum(s.availableStockKg, 0)} kg</td>
                  <td>${formatNum(s.openOrdersKg, 0)} kg</td>
                  <td>${Number.isFinite(s.daysOfCover) ? `${formatNum(s.daysOfCover, 1)} d` : "—"}</td>
                  <td>${statusPill(s.status)}</td>
                </tr>`
              )
              .join("")}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

function renderOrdersTab() {
  const q = state.searchText.toLowerCase();
  const rows = state.orders.filter(
    (o) =>
      !q ||
      (o.orderCode || "").toLowerCase().includes(q) ||
      (o.customer || "").toLowerCase().includes(q) ||
      (o.productName || "").toLowerCase().includes(q)
  );
  return `
    <div class="card">
      <div class="section-head">
        <h3>Customer Orders (${rows.length})</h3>
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <input id="tab-search" class="search-input" placeholder="Search order, customer, SKU..." value="${escapeHtml(state.searchText)}" />
          <button id="export-csv-btn" class="btn btn-sm">Export CSV</button>
        </div>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Order</th><th>Customer</th><th>Product</th><th>Ordered / Dispatched</th><th>Due Date</th><th>Status</th></tr></thead>
          <tbody>
            ${rows
              .map(
                (o) => `<tr>
                  <td><strong>${escapeHtml(o.orderCode)}</strong>${o.rushOrder ? ' <span class="pill pill-critical">RUSH</span>' : ""}</td>
                  <td>${escapeHtml(o.customer)}</td>
                  <td>${escapeHtml(o.productName)}</td>
                  <td>${formatNum(o.totalWeightKg, 0)} kg / ${formatNum(o.dispatchedWeightKg, 0)} kg</td>
                  <td>${escapeHtml(o.dueDate)}</td>
                  <td>${statusPill(o.status)}</td>
                </tr>`
              )
              .join("")}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

function renderDispatchedTab() {
  const q = state.searchText.toLowerCase();
  const rows = state.dispatches.filter(
    (d) =>
      !q ||
      (d.drReference || "").toLowerCase().includes(q) ||
      (d.customer || "").toLowerCase().includes(q) ||
      (d.productName || "").toLowerCase().includes(q)
  );
  return `
    <div class="card">
      <div class="section-head">
        <h3>Dispatched Deliveries (${rows.length})</h3>
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <input id="tab-search" class="search-input" placeholder="Search DR, customer, SKU..." value="${escapeHtml(state.searchText)}" />
          <button id="export-csv-btn" class="btn btn-sm">Export CSV</button>
        </div>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Date</th><th>DR Ref</th><th>Customer</th><th>Product</th><th>Bags</th><th>Net Weight</th><th>Status</th></tr></thead>
          <tbody>
            ${rows
              .map(
                (d) => `<tr>
                  <td>${escapeHtml(d.date)}</td>
                  <td><strong>${escapeHtml(d.drReference)}</strong></td>
                  <td>${escapeHtml(d.customer)}</td>
                  <td>${escapeHtml(d.productName)}</td>
                  <td>${d.bagsDispatched}</td>
                  <td>${formatNum(effectiveDispatchWeightKg(d), 0)} kg</td>
                  <td>${statusPill(d.dispatchStatus || "Delivered")}</td>
                </tr>`
              )
              .join("")}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

function renderScheduleTab() {
  return `
    <div class="card">
      <div class="section-head"><h3>Scheduled Production Queue (${state.schedules.length})</h3></div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Date</th><th>Section</th><th>Product</th><th>Target</th><th>Priority</th><th>Status</th><th>Remarks</th></tr></thead>
          <tbody>
            ${state.schedules
              .map(
                (s) => `<tr>
                  <td>${escapeHtml(s.scheduledDate)}</td>
                  <td>${escapeHtml(s.section)}</td>
                  <td><strong>${escapeHtml(s.productName)}</strong></td>
                  <td>${s.targetBags} bags (${formatNum(s.expectedWeightKg, 0)} kg)</td>
                  <td>${statusPill(s.priority)}</td>
                  <td>${statusPill(s.status)}</td>
                  <td>${escapeHtml(s.lineRemarks)}</td>
                </tr>`
              )
              .join("")}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

function renderRecoveryTab() {
  return `
    <div class="card">
      <div class="section-head"><h3>Production Recovery Batches (${state.batches.length})</h3></div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Date</th><th>Section</th><th>Product</th><th>Bags</th><th>Expected vs Recovered</th><th>Recovery %</th></tr></thead>
          <tbody>
            ${state.batches
              .map(
                (b) => `<tr>
                  <td>${escapeHtml(b.date)}</td>
                  <td>${escapeHtml(b.section)}</td>
                  <td><strong>${escapeHtml(b.productName)}</strong></td>
                  <td>${b.bagsProduced}</td>
                  <td>${formatNum(b.expectedWeightKg, 0)} / ${formatNum(b.actualRecoveredKg, 0)} kg</td>
                  <td>${formatNum(b.recoveryPct, 1)}%</td>
                </tr>`
              )
              .join("")}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

function renderMaterialsTab() {
  return `
    <div class="card">
      <div class="section-head">
        <h3>Raw Materials Inventory (${state.rawMaterials.length})</h3>
        <button id="export-csv-btn" class="btn btn-sm">Export CSV</button>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Material</th><th>Category</th><th>Current Stock</th><th>Reorder At</th><th>Supplier</th><th>Status</th></tr></thead>
          <tbody>
            ${state.rawMaterials
              .map((m) => {
                const low = Number(m.currentStockKg || 0) <= Number(m.reorderThresholdKg || 0);
                return `<tr>
                  <td><strong>${escapeHtml(m.name)}</strong></td>
                  <td>${escapeHtml(m.category)}</td>
                  <td>${formatNum(m.currentStockKg, 0)} kg</td>
                  <td>${formatNum(m.reorderThresholdKg, 0)} kg</td>
                  <td>${escapeHtml(m.supplierName || "—")}</td>
                  <td>${statusPill(low ? "Critical" : "Healthy")}</td>
                </tr>`;
              })
              .join("")}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

function renderReprocessTab() {
  return `
    <div class="card">
      <div class="section-head"><h3>Reprocess Quarantine (${state.reprocessItems.length})</h3></div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Returned Date</th><th>DR Ref</th><th>Product</th><th>Returned</th><th>Reason</th><th>Status</th></tr></thead>
          <tbody>
            ${state.reprocessItems
              .map(
                (r) => `<tr>
                  <td>${escapeHtml(r.returnedDate)}</td>
                  <td>${escapeHtml(r.drReference || "Manual")}</td>
                  <td><strong>${escapeHtml(r.productName)}</strong></td>
                  <td>${r.returnedBags} bags (${formatNum(r.returnedWeightKg, 0)} kg)</td>
                  <td>${escapeHtml(r.returnReason)}</td>
                  <td>${statusPill(r.status)}</td>
                </tr>`
              )
              .join("")}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

function renderRoleSwitcher({
  currentRole,
  targetType,
  memberUid = "",
  inviteId = "",
  memberEmail = "",
  memberName = "",
  disabled = false
}) {
  const normalized = resolveRole(currentRole).label;
  return `
    <div class="role-switch" role="group" aria-label="Switch access for ${escapeHtml(memberName || memberEmail || "team member")}">
      ${WORKSPACE_ROLE_OPTIONS.map((option) => {
        const isActive = normalized === option;
        const variantClass = option.toLowerCase();
        const dataAttrs =
          targetType === "member"
            ? `data-switch-member-role="${option}" data-member-uid="${escapeHtml(memberUid)}" data-invite-id="${escapeHtml(inviteId)}" data-member-email="${escapeHtml(memberEmail)}" data-member-name="${escapeHtml(memberName)}"`
            : `data-switch-invite-role="${option}" data-invite-id="${escapeHtml(inviteId)}" data-member-email="${escapeHtml(memberEmail)}"`;
        return `<button type="button" class="role-chip ${isActive ? `active ${variantClass}` : ""}" ${dataAttrs} aria-pressed="${isActive ? "true" : "false"}" ${disabled ? "disabled" : ""}>${option}</button>`;
      }).join("")}
    </div>
  `;
}

function renderTeamTab() {
  const role = getActiveRole();
  const pendingInvites = state.workspaceInvites.filter((i) => i.status === "Pending");
  const isLocal = isLocalOrigin();
  const currentUrl = getCurrentPortalUrl();
  const portalUrl = getWebOriginBase();
  const ownerEmailDisplay =
    state.activeWorkspaceOwnerId === state.user?.uid
      ? state.user?.email
      : state.workspaceInvites.find((i) => i.ownerEmail)?.ownerEmail || "Owner";

  return `
    <div class="card" style="display:grid; gap:10px;">
      <div class="section-head" style="margin-bottom:0;">
        <h3>Computer &amp; Web Portal Connection</h3>
        <button id="copy-web-url-btn" class="btn btn-sm">${isLocal ? "Copy Active Local URL" : "Copy Website URL"}</button>
      </div>
      <div style="font-size:13px; color:var(--muted); line-height:1.5;">
        This web app connects directly to Firebase project <strong>gen-lang-client-0293694787</strong> and the live Bulacan Plant Firestore database, syncing in real time with your Android app.
      </div>
      <div style="font-size:12px; background:var(--surface-alt); border:1px solid var(--border); border-radius:10px; padding:10px 12px; word-break:break-all; display:grid; gap:6px;">
        <div><strong>${isLocal ? "Active Local Portal URL:" : "Portal URL:"}</strong> <span>${escapeHtml(currentUrl)}</span></div>
        ${
          isLocal
            ? `<div style="color:var(--muted); line-height:1.45;">
                <strong>Public Hosting URL (once deployed):</strong> <strong>${escapeHtml(portalUrl)}</strong><br/>
                If <strong>${escapeHtml(portalUrl)}</strong> shows <em>"Site Not Found"</em>, run <strong>deploy-web.bat</strong> once from your project folder (or run <code>firebase deploy --only hosting</code>) to publish the <code>web</code> folder online.
              </div>`
            : ""
        }
      </div>
    </div>

    <div class="card">
      <div class="section-head">
        <h3>Team Members (${state.teamMembers.length + 1})</h3>
        ${role.canManageTeam ? `<button id="team-invite-btn" class="btn btn-primary btn-sm">Invite Team Member</button>` : ""}
      </div>
      <div class="role-legend">
        <span><strong>Viewer:</strong> Read-only dashboards &amp; CSV reports</span>
        <span><strong>Operator:</strong> Log orders, dispatches, batches &amp; stock</span>
        <span><strong>Admin:</strong> Full plant control &amp; switch team roles</span>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Member</th><th>Email</th><th>Access Level</th><th>Actions</th></tr></thead>
          <tbody>
            <tr>
              <td><strong>Workspace Owner</strong></td>
              <td>${escapeHtml(ownerEmailDisplay)}</td>
              <td>${statusPill("Admin")}</td>
              <td>Owner</td>
            </tr>
            ${state.teamMembers
              .map((m) => {
                const canEditThisMember =
                  role.canManageTeam &&
                  m.userId !== state.user?.uid &&
                  m.userId !== state.activeWorkspaceOwnerId;
                const isUpdating = state.updatingAccessKeys.has(`member:${m.userId}`);
                const roleCell = canEditThisMember
                  ? renderRoleSwitcher({
                      currentRole: m.role,
                      targetType: "member",
                      memberUid: m.userId,
                      inviteId: m.inviteId || "",
                      memberEmail: m.email || "",
                      memberName: m.displayName || m.email || "",
                      disabled: isUpdating
                    })
                  : statusPill(resolveRole(m.role).label);
                return `<tr>
                  <td><strong>${escapeHtml(m.displayName || m.email)}</strong>${m.userId === state.user?.uid ? ' <span style="font-size:11px;color:var(--muted)">(You)</span>' : ""}</td>
                  <td>${escapeHtml(m.email)}</td>
                  <td>${roleCell}</td>
                  <td>
                    ${
                      canEditThisMember
                        ? `<button class="btn btn-danger btn-sm" data-remove-member="${escapeHtml(m.userId)}" data-invite-id="${escapeHtml(m.inviteId || "")}">Remove</button>`
                        : "—"
                    }
                  </td>
                </tr>`;
              })
              .join("")}
          </tbody>
        </table>
      </div>
    </div>

    <div class="card">
      <div class="section-head"><h3>Pending Invitations (${pendingInvites.length})</h3></div>
      ${
        pendingInvites.length
          ? `<div class="table-wrap"><table><thead><tr><th>Invited Email</th><th>Access Level</th><th>Delivery</th><th>Actions</th></tr></thead><tbody>
              ${pendingInvites
                .map((inv) => {
                  const isUpdating = state.updatingAccessKeys.has(`invite:${inv.id}`);
                  const roleCell = role.canManageTeam
                    ? renderRoleSwitcher({
                        currentRole: inv.role,
                        targetType: "invite",
                        inviteId: inv.id,
                        memberEmail: inv.memberEmail || "",
                        memberName: inv.memberDisplayName || inv.memberEmail || "",
                        disabled: isUpdating
                      })
                    : statusPill(resolveRole(inv.role).label);
                  return `<tr>
                    <td><strong>${escapeHtml(inv.memberEmail)}</strong></td>
                    <td>${roleCell}</td>
                    <td>${escapeHtml(inv.emailDeliveryStatus || "Pending sign-in")}</td>
                    <td style="display:flex; gap:6px; flex-wrap:wrap;">
                      <button class="btn btn-sm" data-gmail-invite="${escapeHtml(inv.id)}">Open Gmail</button>
                      <button class="btn btn-sm" data-copy-invite-link="${escapeHtml(inv.id)}">Copy Link</button>
                      ${
                        role.canManageTeam
                          ? `<button class="btn btn-danger btn-sm" data-cancel-invite="${escapeHtml(inv.id)}">Cancel</button>`
                          : ""
                      }
                    </td>
                  </tr>`;
                })
                .join("")}
            </tbody></table></div>`
          : `<div class="empty-state">No pending team invitations.</div>`
      }
    </div>
  `;
}

function renderHistoryTab() {
  return `
    <div class="card">
      <div class="section-head"><h3>Recent Activity History (${state.breadcrumbs.length})</h3></div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Date / Time</th><th>Category</th><th>Action</th><th>Details</th><th>Severity</th></tr></thead>
          <tbody>
            ${state.breadcrumbs
              .slice(0, 60)
              .map(
                (b) => `<tr>
                  <td>${escapeHtml(b.dateStr)} ${escapeHtml(b.timeLabel)}</td>
                  <td>${escapeHtml(b.category)}</td>
                  <td><strong>${escapeHtml(b.action)}</strong></td>
                  <td>${escapeHtml(b.details)}</td>
                  <td>${statusPill(b.severity)}</td>
                </tr>`
              )
              .join("")}
          </tbody>
        </table>
      </div>
    </div>
  `;
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

  const activeRole = getActiveRole();
  const pendingCount = getPendingInvitesForMe().length;
  document.getElementById("signed-in-email").textContent = state.user.email || state.user.uid;
  document.getElementById("active-role-line").textContent = `Role: ${activeRole.label}`;

  // Workspace dropdown
  const wsSelect = document.getElementById("workspace-select");
  const workspaces = getAvailableWorkspaces();
  wsSelect.innerHTML = workspaces
    .map(
      (ws) =>
        `<option value="${escapeHtml(ws.workspaceOwnerId)}" ${ws.workspaceOwnerId === state.activeWorkspaceOwnerId ? "selected" : ""}>${escapeHtml(ws.label)}</option>`
    )
    .join("");
  wsSelect.onchange = () => subscribeWorkspace(wsSelect.value);

  // Sidebar navigation
  const navList = document.getElementById("nav-list");
  navList.innerHTML = TABS.map((t) => {
    const badge = t.id === "team" && pendingCount > 0 ? `<span class="nav-badge">${pendingCount}</span>` : "";
    return `<button class="nav-btn ${state.activeTab === t.id ? "active" : ""}" data-tab="${t.id}">${escapeHtml(t.label)}${badge}</button>`;
  }).join("");
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
  if (activeRole.canWriteOperations) {
    topbarActions.innerHTML = `
      <button id="top-new-order" class="btn btn-primary btn-sm">+ Order</button>
      <button id="top-log-dispatch" class="btn btn-sm">+ Dispatch</button>
      <button id="top-log-batch" class="btn btn-sm">+ Batch</button>
    `;
    document.getElementById("top-new-order").onclick = openNewOrderModal;
    document.getElementById("top-log-dispatch").onclick = openLogDispatchModal;
    document.getElementById("top-log-batch").onclick = openLogBatchModal;
  } else {
    topbarActions.innerHTML = `<span class="pill pill-warn">Viewer (Read-Only)</span>`;
  }

  renderPendingInvitesBanner();

  const summaries = computeSkuSummaries({
    skus: state.skus,
    batches: state.batches,
    dispatches: state.dispatches,
    orders: state.orders,
    reprocessItems: state.reprocessItems
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
  switch (state.activeTab) {
    case "inventory":
      viewRoot.innerHTML = renderInventoryTab(summaries);
      break;
    case "orders":
      viewRoot.innerHTML = renderOrdersTab();
      break;
    case "dispatched":
      viewRoot.innerHTML = renderDispatchedTab();
      break;
    case "schedule":
      viewRoot.innerHTML = renderScheduleTab();
      break;
    case "recovery":
      viewRoot.innerHTML = renderRecoveryTab();
      break;
    case "materials":
      viewRoot.innerHTML = renderMaterialsTab();
      break;
    case "reprocess":
      viewRoot.innerHTML = renderReprocessTab();
      break;
    case "team":
      viewRoot.innerHTML = renderTeamTab();
      break;
    case "history":
      viewRoot.innerHTML = renderHistoryTab();
      break;
    default:
      viewRoot.innerHTML = renderSummaryTab(summaries, kpis);
      break;
  }

  const searchInput = document.getElementById("tab-search");
  if (searchInput) {
    searchInput.oninput = (e) => {
      state.searchText = e.target.value;
      render();
      const refocused = document.getElementById("tab-search");
      if (refocused) {
        refocused.focus();
        refocused.setSelectionRange(refocused.value.length, refocused.value.length);
      }
    };
  }

  const csvBtn = document.getElementById("export-csv-btn");
  if (csvBtn) {
    csvBtn.onclick = () => {
      if (state.activeTab === "inventory") {
        downloadCsv(
          `feedmill_inventory_${todayIsoDate()}.csv`,
          ["Product", "Section", "Available Bags", "Available Kg", "Open Orders Kg", "Status"],
          summaries.map((s) => [s.sku.productName, s.sku.section, s.availableBags.toFixed(1), s.availableStockKg.toFixed(1), s.openOrdersKg.toFixed(1), s.status])
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
          ["Date", "DR Ref", "Customer", "Product", "Bags", "Net Weight Kg", "Status"],
          state.dispatches.map((d) => [d.date, d.drReference, d.customer, d.productName, d.bagsDispatched, effectiveDispatchWeightKg(d), d.dispatchStatus || "Delivered"])
        );
      } else if (state.activeTab === "materials") {
        downloadCsv(
          `feedmill_raw_materials_${todayIsoDate()}.csv`,
          ["Material", "Category", "Current Stock Kg", "Reorder Threshold Kg", "Supplier"],
          state.rawMaterials.map((m) => [m.name, m.category, m.currentStockKg, m.reorderThresholdKg, m.supplierName])
        );
      }
    };
  }

  // Team tab bindings
  const copyWebBtn = document.getElementById("copy-web-url-btn");
  if (copyWebBtn) {
    copyWebBtn.onclick = async () => {
      const urlToCopy = getCurrentPortalUrl();
      await navigator.clipboard.writeText(urlToCopy);
      showToast(`Copied URL: ${urlToCopy}`);
    };
  }
  const teamInviteBtn = document.getElementById("team-invite-btn");
  if (teamInviteBtn) teamInviteBtn.onclick = openInviteMemberModal;

  viewRoot.querySelectorAll("[data-gmail-invite]").forEach((btn) => {
    btn.onclick = () => {
      const inv = state.workspaceInvites.find((i) => i.id === btn.dataset.gmailInvite);
      if (!inv) return;
      const mail = buildInviteEmailPayload({
        recipientEmail: inv.memberEmail,
        recipientName: inv.memberDisplayName,
        role: inv.role,
        inviterEmail: state.user?.email || "",
        workspaceOwnerId: inv.workspaceOwnerId || state.activeWorkspaceOwnerId,
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
        workspaceOwnerId: inv.workspaceOwnerId || state.activeWorkspaceOwnerId,
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
      batch.delete(doc(db, "users", state.activeWorkspaceOwnerId, "team_invites", invId));
      await batch.commit();
      showToast("Invite cancelled.");
    };
  });

  viewRoot.querySelectorAll("[data-switch-member-role]").forEach((btn) => {
    btn.onclick = () => {
      const targetRole = btn.dataset.switchMemberRole;
      const memberUid = btn.dataset.memberUid;
      const existing = state.teamMembers.find((m) => m.userId === memberUid);
      if (existing && resolveRole(existing.role).label === resolveRole(targetRole).label) {
        return;
      }
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
      if (existing && resolveRole(existing.role).label === resolveRole(targetRole).label) {
        return;
      }
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
      batch.delete(doc(db, "users", state.activeWorkspaceOwnerId, "team_members", memberUid));
      if (invId) {
        batch.delete(doc(db, "workspace_invites", invId));
        batch.delete(doc(db, "users", state.activeWorkspaceOwnerId, "team_invites", invId));
      }
      await batch.commit();
      showToast("Team member removed.");
    };
  });
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
