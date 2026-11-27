export const FIRESTORE_PROJECT_ID = "gen-lang-client-0293694787";
export const FIRESTORE_DATABASE_ID = "ai-studio-android-feedmill-ebaf442b-0447-4046-8dd2-306eb64b2b23";
export const HOSTED_WEB_URL = "https://gen-lang-client-0293694787.web.app";

export const FIREBASE_CONFIG = {
  apiKey: ["AIzaSyDrU2QWco", "_SxaLxd5TXAV__2QAicVrH1Jg"].join(""),
  authDomain: `${FIRESTORE_PROJECT_ID}.firebaseapp.com`,
  projectId: FIRESTORE_PROJECT_ID,
  storageBucket: `${FIRESTORE_PROJECT_ID}.firebasestorage.app`,
  messagingSenderId: "395127150673"
};

export const PLANT_SECTIONS = [
  "SAN ROQUE",
  "PANDACAQUI",
  "LA UNION QUAIL FEEDS",
  "SELECTA"
];

export const RAW_CATEGORIES = [
  "Grains & Energy",
  "Protein Meals",
  "Minerals & Salt",
  "Premix & Additives",
  "Liquids & Oils"
];

export const WORKSPACE_ROLES = {
  Admin: {
    label: "Admin",
    canWriteOperations: true,
    canManageTeam: true,
    summary: "Full operational control, SKU management, resets, backups, and team role management."
  },
  Operator: {
    label: "Operator",
    canWriteOperations: true,
    canManageTeam: false,
    summary: "Create and update orders, dispatches, schedules, batches, reprocess items, and stock."
  },
  Viewer: {
    label: "Viewer",
    canWriteOperations: false,
    canManageTeam: false,
    summary: "Read-only access to dashboards, inventory levels, reports, and activity history."
  }
};

export function resolveRole(raw) {
  const clean = String(raw || "").trim().toLowerCase();
  if (clean === "admin") return WORKSPACE_ROLES.Admin;
  if (clean === "operator") return WORKSPACE_ROLES.Operator;
  return WORKSPACE_ROLES.Viewer;
}

export function todayIsoDate(date = new Date()) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function makeDocId(prefix) {
  const cleanPrefix = String(prefix || "doc").replace(/[^a-zA-Z0-9_-]/g, "").slice(0, 12) || "doc";
  const rand = typeof crypto !== "undefined" && typeof crypto.randomUUID === "function"
    ? crypto.randomUUID().replace(/-/g, "").slice(0, 12)
    : Math.random().toString(36).slice(2, 14).padEnd(12, "0");
  return `${cleanPrefix}_${rand}`;
}

export async function buildInviteDocId(workspaceOwnerId, memberEmail) {
  const normalizedEmail = String(memberEmail || "").trim().toLowerCase();
  const bytes = new TextEncoder().encode(normalizedEmail);
  const hashBuffer = await globalThis.crypto.subtle.digest("SHA-256", bytes);
  const digest = Array.from(new Uint8Array(hashBuffer))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("")
    .slice(0, 24);
  const cleanOwner = String(workspaceOwnerId || "")
    .replace(/[^a-zA-Z0-9_-]/g, "")
    .slice(0, 48);
  return `inv_${cleanOwner}_${digest}`;
}

export function parseInviteQueryParams(searchStr = "") {
  const params = new URLSearchParams(searchStr || "");
  return {
    action: params.get("action") || "",
    inviteId: (params.get("invite") || "").trim(),
    workspaceOwnerId: (params.get("workspace") || "").trim(),
    email: (params.get("email") || "").trim().toLowerCase(),
    role: (params.get("role") || "").trim()
  };
}

export function buildWebInviteLink({
  baseUrl = HOSTED_WEB_URL,
  workspaceOwnerId = "",
  recipientEmail = "",
  role = "Operator",
  inviteId = ""
}) {
  const cleanBase = String(baseUrl || HOSTED_WEB_URL).replace(/\/+$/, "");
  const params = new URLSearchParams();
  params.set("action", "team_invite");
  if (inviteId) params.set("invite", String(inviteId).trim());
  if (workspaceOwnerId) params.set("workspace", String(workspaceOwnerId).trim());
  if (recipientEmail) params.set("email", String(recipientEmail).trim().toLowerCase());
  params.set("role", resolveRole(role).label);
  return `${cleanBase}/?${params.toString()}`;
}

export function buildInviteEmailPayload({
  recipientEmail,
  recipientName = "",
  role = "Operator",
  inviterEmail = "",
  workspaceName = "Bulacan Plant",
  workspaceOwnerId = "",
  inviteId = "",
  customNote = "",
  baseUrl = HOSTED_WEB_URL
}) {
  const cleanRecipient = String(recipientEmail || "").trim().toLowerCase();
  const greetingName = String(recipientName || "").trim() || cleanRecipient.split("@")[0] || "Team Member";
  const inviter = String(inviterEmail || "").trim() || "a workspace administrator";
  const resolvedRole = resolveRole(role);
  const inviteLink = buildWebInviteLink({
    baseUrl,
    workspaceOwnerId,
    recipientEmail: cleanRecipient,
    role: resolvedRole.label,
    inviteId
  });
  const subject = `Invitation to join ${workspaceName} (FeedMill Ops) as ${resolvedRole.label}`;
  const lines = [
    `Hi ${greetingName},`,
    "",
    `${inviter} invited you to collaborate on the ${workspaceName} workspace in FeedMill Ops.`
  ];
  if (String(customNote || "").trim()) {
    lines.push("", `Note: ${String(customNote).trim()}`);
  }
  lines.push(
    "",
    `Assigned Role: ${resolvedRole.label}`,
    `Permissions: ${resolvedRole.summary}`,
    "",
    "Open your invitation link (computer or Android):",
    inviteLink,
    "",
    "How to accept your invite:",
    "1. Open the link above in your browser or FeedMill Ops app.",
    `2. Sign in with Google using this exact email: ${cleanRecipient}`,
    '3. Click "Accept & Switch" on the pending invitation banner.'
  );
  return { subject, body: lines.join("\n"), inviteLink };
}

export function buildGmailWebComposeUrl({ to, subject, body }) {
  const params = new URLSearchParams({
    view: "cm",
    fs: "1",
    to: String(to || "").trim().toLowerCase(),
    su: String(subject || ""),
    body: String(body || "")
  });
  return `https://mail.google.com/mail/?${params.toString()}`;
}

export function effectiveDispatchWeightKg(dispatch) {
  if (!dispatch) return 0;
  const total = Number(dispatch.totalWeightKg || 0);
  if (dispatch.returnConfirmed) {
    return Math.max(0, total - Number(dispatch.returnedWeightKg || 0));
  }
  return total;
}

export function computeSkuSummaries({
  skus = [],
  batches = [],
  dispatches = [],
  orders = [],
  reprocessItems = [],
  lookbackDays = 14
}) {
  const safeLookback = Math.max(1, Number(lookbackDays || 14));
  const allDates = [
    ...dispatches.map((d) => d.date).filter(Boolean),
    ...batches.map((b) => b.date).filter(Boolean)
  ].sort();
  const refDateStr = allDates.length ? allDates[allDates.length - 1] : todayIsoDate();
  const refDate = new Date(`${refDateStr}T00:00:00Z`);
  const lookbackStart = new Date(refDate.getTime() - (safeLookback - 1) * 86400000);
  const lookbackStartStr = lookbackStart.toISOString().slice(0, 10);

  return skus.map((sku) => {
    const skuKey = String(sku.productName || "").trim().toLowerCase();
    const skuBatches = batches.filter((b) => String(b.productName || "").trim().toLowerCase() === skuKey);
    const skuDispatches = dispatches.filter((d) => String(d.productName || "").trim().toLowerCase() === skuKey);
    const skuReprocess = reprocessItems.filter((r) => String(r.productName || "").trim().toLowerCase() === skuKey);
    const skuOpenOrders = orders.filter(
      (o) =>
        String(o.productName || "").trim().toLowerCase() === skuKey &&
        (o.status === "Open" || o.status === "Partially Dispatched")
    );

    const openingStockKg = Number(sku.openingStockKg || 0);
    const recoveredKg = skuBatches.reduce((sum, b) => sum + Number(b.actualRecoveredKg || 0), 0);
    const dispatchedKg = skuDispatches.reduce((sum, d) => sum + effectiveDispatchWeightKg(d), 0);

    const nonRestoredLinkedQuarantineKg = skuReprocess
      .filter((r) => !r.flaggedToInventory && String(r.sourceDispatchId || "").trim() !== "")
      .reduce((sum, r) => sum + Number(r.returnedWeightKg || 0), 0);
    const flaggedLinkedDeltaKg = skuReprocess
      .filter((r) => r.flaggedToInventory && String(r.sourceDispatchId || "").trim() !== "")
      .reduce((sum, r) => sum + (Number(r.reprocessedWeightKg || r.returnedWeightKg || 0) - Number(r.returnedWeightKg || 0)), 0);
    const flaggedManualAddKg = skuReprocess
      .filter((r) => r.flaggedToInventory && String(r.sourceDispatchId || "").trim() === "")
      .reduce((sum, r) => sum + Number(r.reprocessedWeightKg || r.returnedWeightKg || 0), 0);

    const availableStockKg =
      openingStockKg +
      recoveredKg -
      dispatchedKg -
      nonRestoredLinkedQuarantineKg +
      flaggedLinkedDeltaKg +
      flaggedManualAddKg;

    const bagWeightKg = Math.max(0.1, Number(sku.standardBagWeightKg || 25));
    const availableBags = availableStockKg / bagWeightKg;
    const recentDispatchedKg = skuDispatches
      .filter((d) => d.date && d.date >= lookbackStartStr && d.date <= refDateStr)
      .reduce((sum, d) => sum + effectiveDispatchWeightKg(d), 0);
    const avgDailyDispatchKg = recentDispatchedKg / safeLookback;
    const daysOfCover = avgDailyDispatchKg > 0 ? Math.max(0, availableStockKg) / avgDailyDispatchKg : Infinity;

    const openOrdersKg = skuOpenOrders.reduce(
      (sum, o) => sum + Math.max(0, Number(o.totalWeightKg || 0) - Number(o.dispatchedWeightKg || 0)),
      0
    );
    const demandBasis = Math.max(openOrdersKg, avgDailyDispatchKg * 3);
    const safetyBufferKg = demandBasis * (Number(sku.safetyBufferPct || 0) / 100);
    const netProductionNeededKg = Math.max(0, openOrdersKg + safetyBufferKg - availableStockKg);

    let status = "Healthy";
    if (availableStockKg <= 0 || availableStockKg < openOrdersKg || (avgDailyDispatchKg > 0 && daysOfCover < 3)) {
      status = "Critical";
    } else if (netProductionNeededKg > 0 || (avgDailyDispatchKg > 0 && daysOfCover < 7)) {
      status = "Low Watch";
    }

    return {
      sku,
      openingStockKg,
      recoveredKg,
      dispatchedKg,
      availableStockKg,
      availableBags,
      avgDailyDispatchKg,
      daysOfCover,
      openOrdersKg,
      netProductionNeededKg,
      status
    };
  });
}

export function computeKpis({ summaries = [], orders = [], dispatches = [], batches = [], rawMaterials = [], schedules = [] }) {
  const totalStockKg = summaries.reduce((sum, s) => sum + s.availableStockKg, 0);
  const totalBags = summaries.reduce((sum, s) => sum + s.availableBags, 0);
  const criticalSkus = summaries.filter((s) => s.status === "Critical").length;
  const openOrders = orders.filter((o) => o.status === "Open" || o.status === "Partially Dispatched");
  const openOrderKg = openOrders.reduce(
    (sum, o) => sum + Math.max(0, Number(o.totalWeightKg || 0) - Number(o.dispatchedWeightKg || 0)),
    0
  );
  const totalDispatchedKg = dispatches.reduce((sum, d) => sum + effectiveDispatchWeightKg(d), 0);
  const totalProducedKg = batches.reduce((sum, b) => sum + Number(b.actualRecoveredKg || 0), 0);
  const lowRawMaterials = rawMaterials.filter(
    (m) => Number(m.currentStockKg || 0) <= Number(m.reorderThresholdKg || 0)
  ).length;
  const activeSchedules = schedules.filter(
    (s) => s.status === "Scheduled" || s.status === "In Progress"
  ).length;

  return {
    totalStockKg,
    totalBags,
    criticalSkus,
    openOrdersCount: openOrders.length,
    openOrderKg,
    totalDispatchedKg,
    totalProducedKg,
    lowRawMaterials,
    activeSchedules
  };
}

export function buildSkuCreatePayload(uid, input, serverTimestampValue) {
  return {
    userId: String(uid).trim(),
    productName: String(input.productName || "").trim().slice(0, 120),
    section: PLANT_SECTIONS.includes(input.section) ? input.section : PLANT_SECTIONS[0],
    openingStockKg: Math.max(0, Number(input.openingStockKg || 0)),
    standardBagWeightKg: Math.max(0.1, Number(input.standardBagWeightKg || 25)),
    targetRecoveryPct: Math.min(120, Math.max(1, Number(input.targetRecoveryPct || 99.5))),
    safetyBufferPct: Math.min(200, Math.max(0, Number(input.safetyBufferPct || 15))),
    defaultRemarks: String(input.defaultRemarks || "").trim().slice(0, 120),
    createdAt: serverTimestampValue,
    updatedAt: serverTimestampValue
  };
}

export function buildOrderCreatePayload(uid, input, serverTimestampValue) {
  const bags = Math.max(1, Math.round(Number(input.orderedBags || 1)));
  const bagWeightKg = Math.max(0.1, Number(input.bagWeightKg || 25));
  const totalWeightKg = Number((bags * bagWeightKg).toFixed(2));
  return {
    userId: String(uid).trim(),
    orderCode: String(input.orderCode || `ORD-${Date.now().toString().slice(-6)}`).trim().slice(0, 40),
    orderDate: String(input.orderDate || todayIsoDate()),
    dueDate: String(input.dueDate || input.orderDate || todayIsoDate()),
    productName: String(input.productName || "").trim().slice(0, 120),
    orderedBags: bags,
    bagWeightKg,
    totalWeightKg,
    dispatchedWeightKg: 0,
    customer: String(input.customer || "").trim().slice(0, 120),
    status: "Open",
    priority: Boolean(input.priority),
    rushOrder: Boolean(input.rushOrder),
    createdAt: serverTimestampValue,
    updatedAt: serverTimestampValue
  };
}

export function buildDispatchCreatePayload(uid, input, serverTimestampValue) {
  const bags = Math.max(1, Math.round(Number(input.bagsDispatched || 1)));
  const bagWeightKg = Math.max(0.1, Number(input.bagWeightKg || 25));
  const totalWeightKg = Number((bags * bagWeightKg).toFixed(2));
  const sourcePlant = PLANT_SECTIONS.includes(input.sourcePlant) ? input.sourcePlant : "";
  return {
    userId: String(uid).trim(),
    date: String(input.date || todayIsoDate()),
    productName: String(input.productName || "").trim().slice(0, 120),
    bagsDispatched: bags,
    bagWeightKg,
    totalWeightKg,
    drReference: String(input.drReference || "").trim().slice(0, 80),
    customer: String(input.customer || "").trim().slice(0, 120),
    linkedOrderId: String(input.linkedOrderId || "").trim().slice(0, 128),
    sourcePlant,
    driverName: String(input.driverName || "").trim().slice(0, 120),
    truckNumber: String(input.truckNumber || "").trim().slice(0, 40),
    helperName: String(input.helperName || "").trim().slice(0, 120),
    remarks: String(input.remarks || "").trim().slice(0, 300),
    dispatchStatus: "Delivered",
    hasReturn: false,
    returnConfirmed: false,
    returnedBags: 0,
    returnedWeightKg: 0,
    returnReason: "",
    isHeld: false,
    heldRemarks: "",
    createdAt: serverTimestampValue,
    updatedAt: serverTimestampValue
  };
}

export function buildBatchCreatePayload(uid, input, serverTimestampValue) {
  const bags = Math.max(1, Math.round(Number(input.bagsProduced || 1)));
  const bagWeightKg = Math.max(0.1, Number(input.bagWeightKg || 25));
  const expectedWeightKg = Number((bags * bagWeightKg).toFixed(2));
  const actualRecoveredKg = Math.max(0.1, Number(input.actualRecoveredKg || expectedWeightKg));
  const varianceKg = Number((actualRecoveredKg - expectedWeightKg).toFixed(2));
  const recoveryPct = expectedWeightKg > 0 ? Number(((actualRecoveredKg / expectedWeightKg) * 100).toFixed(2)) : 100;
  return {
    userId: String(uid).trim(),
    date: String(input.date || todayIsoDate()),
    section: PLANT_SECTIONS.includes(input.section) ? input.section : PLANT_SECTIONS[0],
    productName: String(input.productName || "").trim().slice(0, 120),
    bagsProduced: bags,
    bagWeightKg,
    expectedWeightKg,
    actualRecoveredKg,
    varianceKg,
    recoveryPct,
    remarks: String(input.remarks || "").trim().slice(0, 120),
    scheduleRunId: String(input.scheduleRunId || "").trim().slice(0, 128),
    linkedOrderIds: Array.isArray(input.linkedOrderIds) ? input.linkedOrderIds.slice(0, 100) : [],
    linkedOrderCodes: Array.isArray(input.linkedOrderCodes) ? input.linkedOrderCodes.slice(0, 100) : [],
    scheduledForCustomers: Array.isArray(input.scheduledForCustomers) ? input.scheduledForCustomers.slice(0, 100) : [],
    createdAt: serverTimestampValue,
    updatedAt: serverTimestampValue
  };
}

export function buildScheduleCreatePayload(uid, input, serverTimestampValue) {
  const bags = Math.max(1, Math.round(Number(input.targetBags || 1)));
  const bagWeightKg = Math.max(0.1, Number(input.bagWeightKg || 25));
  const expectedWeightKg = Number((bags * bagWeightKg).toFixed(2));
  const validPriorities = ["Urgent", "Order-Driven", "Buffer Top-Up"];
  return {
    userId: String(uid).trim(),
    scheduledDate: String(input.scheduledDate || todayIsoDate()),
    section: PLANT_SECTIONS.includes(input.section) ? input.section : PLANT_SECTIONS[0],
    productName: String(input.productName || "").trim().slice(0, 120),
    targetBags: bags,
    bagWeightKg,
    expectedWeightKg,
    lineRemarks: String(input.lineRemarks || "Scheduled via Web Portal").trim().slice(0, 120) || "Scheduled via Web Portal",
    priority: validPriorities.includes(input.priority) ? input.priority : "Order-Driven",
    status: "Scheduled",
    linkedOrderId: String(input.linkedOrderId || "").trim().slice(0, 128),
    linkedOrderCode: String(input.linkedOrderCode || "").trim().slice(0, 40),
    linkedOrderIds: Array.isArray(input.linkedOrderIds) ? input.linkedOrderIds.slice(0, 100) : [],
    linkedOrderCodes: Array.isArray(input.linkedOrderCodes) ? input.linkedOrderCodes.slice(0, 100) : [],
    createdAt: serverTimestampValue,
    updatedAt: serverTimestampValue
  };
}

export function buildRawMaterialCreatePayload(uid, input, serverTimestampValue) {
  return {
    userId: String(uid).trim(),
    name: String(input.name || "").trim().slice(0, 120),
    category: RAW_CATEGORIES.includes(input.category) ? input.category : RAW_CATEGORIES[0],
    currentStockKg: Math.max(0, Number(input.currentStockKg || 0)),
    reorderThresholdKg: Math.max(0, Number(input.reorderThresholdKg || 0)),
    usageSharePct: Math.min(100, Math.max(0.1, Number(input.usageSharePct || 10))),
    leadTimeDays: Math.min(60, Math.max(1, Math.round(Number(input.leadTimeDays || 3)))),
    safetyDays: Math.min(30, Math.max(0, Math.round(Number(input.safetyDays || 2)))),
    supplierName: String(input.supplierName || "").trim().slice(0, 120),
    alertsEnabled: input.alertsEnabled !== false,
    lastRestockedDate: String(input.lastRestockedDate || todayIsoDate()),
    createdAt: serverTimestampValue,
    updatedAt: serverTimestampValue
  };
}

export function buildWorkspaceInvitePayload({
  workspaceOwnerId,
  workspaceName = "Bulacan Plant",
  ownerEmail = "",
  memberEmail,
  memberDisplayName = "",
  role = "Operator",
  invitedByEmail
}, serverTimestampValue) {
  const cleanEmail = String(memberEmail || "").trim().toLowerCase().slice(0, 160);
  const displayName = String(memberDisplayName || "").trim() || cleanEmail.split("@")[0] || "Team Member";
  return {
    workspaceOwnerId: String(workspaceOwnerId || "").trim(),
    workspaceName: String(workspaceName || "Bulacan Plant").trim().slice(0, 120),
    ownerEmail: String(ownerEmail || "").trim().toLowerCase().slice(0, 160),
    memberEmail: cleanEmail,
    memberDisplayName: displayName.slice(0, 120),
    role: resolveRole(role).label,
    status: "Pending",
    memberUid: "",
    invitedByEmail: String(invitedByEmail || ownerEmail || workspaceOwnerId || "").trim().slice(0, 160),
    createdAt: serverTimestampValue,
    updatedAt: serverTimestampValue
  };
}

function escapeCsvCell(value) {
  const str = String(value ?? "");
  if (/[",\n\r]/.test(str)) {
    return `"${str.replace(/"/g, '""')}"`;
  }
  return str;
}

export function buildCsvReport(headers = [], rows = []) {
  const allRows = [headers, ...rows];
  return allRows
    .map((row) => row.map((cell) => escapeCsvCell(cell)).join(","))
    .join("\n");
}
