import test from "node:test";
import assert from "node:assert/strict";
import {
  FIRESTORE_DATABASE_ID,
  HOSTED_WEB_URL,
  WORKSPACE_ROLE_OPTIONS,
  buildBatchCreatePayload,
  buildBomCreatePayload,
  buildBreadcrumbPayload,
  buildCsvReport,
  buildCustomerCreatePayload,
  buildDailyLogMessengerText,
  buildDetailedEmailReportText,
  buildDispatchCreatePayload,
  buildGmailWebComposeUrl,
  buildInviteDocId,
  buildInviteEmailPayload,
  buildInviteRoleUpdatePayload,
  buildMailtoUrl,
  buildMemberRoleUpdatePayload,
  buildMessengerSummaryText,
  buildOrderCreatePayload,
  buildOrderUpdatePayload,
  buildPlantReport,
  buildRawMaterialCreatePayload,
  buildReprocessCreatePayload,
  buildScheduleCreatePayload,
  buildSettingsCreatePayload,
  buildSkuCreatePayload,
  buildSkuUpdatePayload,
  buildWebInviteLink,
  buildWorkspaceInvitePayload,
  computeDailyLogs,
  computeKpis,
  computeRawMaterialForecasts,
  computeSkuSummaries,
  missingCatalogEntities,
  parseBulkBatchRows,
  parseInviteQueryParams
} from "../operations.js";

test("targets the exact named Firestore database ID", () => {
  assert.equal(
    FIRESTORE_DATABASE_ID,
    "ai-studio-android-feedmill-ebaf442b-0447-4046-8dd2-306eb64b2b23"
  );
});

test("buildInviteDocId matches deterministic SHA-256 format used by Android PlantRepository", async () => {
  const id1 = await buildInviteDocId("owner_1", " Teammate@Gmail.com ");
  const id2 = await buildInviteDocId("owner_1", "teammate@gmail.com");
  assert.equal(id1, id2);
  assert.match(id1, /^inv_owner_1_[0-9a-f]{24}$/);
});

test("buildWebInviteLink and parseInviteQueryParams round-trip cleanly", () => {
  const url = new URL(
    buildWebInviteLink({
      baseUrl: HOSTED_WEB_URL,
      workspaceOwnerId: "owner_abc",
      recipientEmail: "Worker@Gmail.com",
      role: "Operator",
      inviteId: "inv_123"
    })
  );
  const parsed = parseInviteQueryParams(url.search);
  assert.equal(parsed.action, "team_invite");
  assert.equal(parsed.workspaceOwnerId, "owner_abc");
  assert.equal(parsed.email, "worker@gmail.com");
  assert.equal(parsed.role, "Operator");
  assert.equal(parsed.inviteId, "inv_123");
});

test("buildInviteEmailPayload and buildGmailWebComposeUrl include web portal invite link", () => {
  const payload = buildInviteEmailPayload({
    recipientEmail: "op@10kfeeds.ph",
    recipientName: "Maria",
    role: "Operator",
    inviterEmail: "admin@10kfeeds.ph",
    workspaceOwnerId: "owner_1",
    inviteId: "inv_1"
  });
  assert.match(payload.body, /https:\/\/gen-lang-client-0293694787\.web\.app\/\?action=team_invite/);
  const gmailUrl = buildGmailWebComposeUrl({
    to: "op@10kfeeds.ph",
    subject: payload.subject,
    body: payload.body
  });
  assert.ok(gmailUrl.startsWith("https://mail.google.com/mail/?"));
  assert.match(gmailUrl, /to=op%4010kfeeds\.ph/);
});

test("computeSkuSummaries and computeKpis match Android stock math", () => {
  const skus = [
    {
      productName: "Hog Starter Crumble",
      section: "SAN ROQUE",
      openingStockKg: 500,
      standardBagWeightKg: 25,
      safetyBufferPct: 20
    }
  ];
  const batches = [
    { productName: "Hog Starter Crumble", date: "2026-11-20", actualRecoveredKg: 1000 }
  ];
  const dispatches = [
    {
      productName: "Hog Starter Crumble",
      date: "2026-11-21",
      totalWeightKg: 300,
      returnConfirmed: false,
      returnedWeightKg: 0
    }
  ];
  const orders = [
    {
      productName: "Hog Starter Crumble",
      status: "Open",
      totalWeightKg: 400,
      dispatchedWeightKg: 100
    }
  ];

  const summaries = computeSkuSummaries({ skus, batches, dispatches, orders });
  assert.equal(summaries.length, 1);
  assert.equal(summaries[0].availableStockKg, 1200);
  assert.equal(summaries[0].availableBags, 48);
  assert.equal(summaries[0].openOrdersKg, 300);

  const kpis = computeKpis({ summaries, orders, dispatches, batches, rawMaterials: [], schedules: [] });
  assert.equal(kpis.totalStockKg, 1200);
  assert.equal(kpis.openOrdersCount, 1);
  assert.equal(kpis.openOrderKg, 300);
  assert.equal(kpis.totalDispatchedKg, 300);
});

test("Firestore create payload builders emit exact allowed schema keys", () => {
  const ts = "SERVER_TS";
  const sku = buildSkuCreatePayload("uid_1", { productName: "Chick Booster" }, ts);
  assert.deepEqual(
    Object.keys(sku).sort(),
    [
      "createdAt",
      "defaultRemarks",
      "openingStockKg",
      "productName",
      "safetyBufferPct",
      "section",
      "standardBagWeightKg",
      "targetRecoveryPct",
      "updatedAt",
      "userId"
    ]
  );

  const order = buildOrderCreatePayload("uid_1", { productName: "Chick Booster", customer: "Farm A", orderedBags: 10, bagWeightKg: 25 }, ts);
  assert.equal(order.totalWeightKg, 250);
  assert.equal(order.status, "Open");

  const dispatch = buildDispatchCreatePayload("uid_1", { productName: "Chick Booster", customer: "Farm A", bagsDispatched: 4, bagWeightKg: 25, drReference: "DR-1" }, ts);
  assert.equal(dispatch.totalWeightKg, 100);
  assert.equal(dispatch.dispatchStatus, "Delivered");

  const batch = buildBatchCreatePayload("uid_1", { productName: "Chick Booster", bagsProduced: 10, bagWeightKg: 25, actualRecoveredKg: 245 }, ts);
  assert.equal(batch.varianceKg, -5);
  assert.equal(batch.recoveryPct, 98);

  const sched = buildScheduleCreatePayload("uid_1", { productName: "Chick Booster", targetBags: 10, bagWeightKg: 25 }, ts);
  assert.equal(sched.status, "Scheduled");

  const raw = buildRawMaterialCreatePayload("uid_1", { name: "Yellow Corn", currentStockKg: 5000, reorderThresholdKg: 1000 }, ts);
  assert.equal(raw.category, "Grains & Energy");

  const invite = buildWorkspaceInvitePayload({ workspaceOwnerId: "uid_1", memberEmail: "New@Gmail.com", invitedByEmail: "owner@10kfeeds.ph" }, ts);
  assert.equal(invite.memberEmail, "new@gmail.com");
  assert.equal(invite.status, "Pending");
});

test("buildCsvReport escapes commas and quotes properly", () => {
  const csv = buildCsvReport(["Name", "Notes"], [['Farm "Alpha", Inc.', "25kg"]]);
  assert.equal(csv, 'Name,Notes\n"Farm ""Alpha"", Inc.",25kg');
});

test("role update payload builders restrict keys to Firestore rules allowed fields", () => {
  assert.deepEqual(WORKSPACE_ROLE_OPTIONS, ["Viewer", "Operator", "Admin"]);
  const memberUpdate = buildMemberRoleUpdatePayload("operator", "SERVER_TS");
  assert.deepEqual(memberUpdate, {
    role: "Operator",
    updatedAt: "SERVER_TS"
  });

  const inviteUpdate = buildInviteRoleUpdatePayload("admin", " Owner@10kfeeds.ph ", "SERVER_TS");
  assert.deepEqual(inviteUpdate, {
    role: "Admin",
    invitedByEmail: "owner@10kfeeds.ph",
    updatedAt: "SERVER_TS"
  });
});

test("computeRawMaterialForecasts uses BOM inclusion percentages and simulation multipliers", () => {
  const materials = [
    {
      id: "m_corn",
      name: "Whole Yellow Corn/Balanok Corn",
      category: "Grains & Energy",
      currentStockKg: 2000,
      reorderThresholdKg: 800,
      leadTimeDays: 3,
      safetyDays: 2,
      usageSharePct: 10
    }
  ];
  const boms = [
    {
      productName: "Hog Starter Crumble",
      lines: [{ materialId: "m_corn", materialName: "Whole Yellow Corn/Balanok Corn", inclusionPct: 50 }]
    }
  ];
  const batches = [
    { date: "2026-11-25", productName: "Hog Starter Crumble", expectedWeightKg: 1400, actualRecoveredKg: 1400 }
  ];
  const schedules = [
    { status: "Scheduled", productName: "Hog Starter Crumble", expectedWeightKg: 7000 }
  ];

  const base = computeRawMaterialForecasts({ materials, batches, schedules, boms, lookbackDays: 7 });
  assert.equal(base.length, 1);
  assert.equal(base[0].sector.id, "MACRO");
  assert.equal(base[0].historicalAvgDailyKg, 100);
  assert.equal(base[0].simulatedAvgDailyKg, 100);

  const sim = computeRawMaterialForecasts({
    materials,
    batches,
    schedules,
    boms,
    lookbackDays: 7,
    demandMultiplier: 2,
    includeScheduledDemand: true
  });
  assert.equal(sim[0].simulatedAvgDailyKg, 700);
  assert.equal(sim[0].status, "Order Now");
});

test("computeDailyLogs and buildPlantReport summarize daily and multi-section activity", () => {
  const skus = [{ productName: "Hog Starter Crumble", section: "SAN ROQUE", openingStockKg: 500, standardBagWeightKg: 25 }];
  const batches = [{ date: "2026-11-25", section: "SAN ROQUE", productName: "Hog Starter Crumble", bagsProduced: 40, expectedWeightKg: 1000, actualRecoveredKg: 990 }];
  const dispatches = [{ date: "2026-11-25", sourcePlant: "SAN ROQUE", productName: "Hog Starter Crumble", bagsDispatched: 10, totalWeightKg: 250 }];
  const orders = [{ orderDate: "2026-11-25", dueDate: "2026-11-26", productName: "Hog Starter Crumble", totalWeightKg: 500, dispatchedWeightKg: 250, status: "Partially Dispatched" }];
  const summaries = computeSkuSummaries({ skus, batches, dispatches, orders });

  const logs = computeDailyLogs({ skus, batches, dispatches, orders, todayIso: "2026-11-26" });
  const log25 = logs.find((l) => l.date === "2026-11-25");
  assert.ok(log25);
  assert.equal(log25.totalProducedDayKg, 990);
  assert.equal(log25.totalDispatchedDayKg, 250);
  assert.equal(log25.skuRows[0].closingStockKg, 1240);

  const report = buildPlantReport({
    startDate: "2026-11-25",
    endDate: "2026-11-25",
    sectionFilter: "SAN ROQUE",
    skus,
    batches,
    dispatches,
    orders,
    summaries
  });
  assert.equal(report.isDailyReport, true);
  assert.equal(report.totalProducedBags, 40);
  assert.equal(report.totalDispatchedKg, 250);
  assert.match(buildMessengerSummaryText(report), /DAILY PLANT SUMMARY REPORT/);
});

test("parseBulkBatchRows, missingCatalogEntities, and additional payload builders", () => {
  const parsed = parseBulkBatchRows(
    "2026-11-25, PANDACAQUI, Chick Booster, 50, 25, 1240, MIXING/CONVEYOR\nInvalidLine",
    "SAN ROQUE"
  );
  assert.equal(parsed.length, 1);
  assert.equal(parsed[0].section, "PANDACAQUI");
  assert.equal(parsed[0].actualRecoveredKg, 1240);

  const missingMacro = missingCatalogEntities([{ name: "Whole Yellow Corn/Balanok Corn" }], false);
  assert.equal(missingMacro.length, 8);

  const ts = "TS";
  assert.equal(buildSkuUpdatePayload({ productName: "Test SKU", openingStockKg: 100 }, ts).updatedAt, ts);
  assert.equal(buildOrderUpdatePayload({ orderedBags: 10, bagWeightKg: 25 }, 100, ts).status, "Partially Dispatched");

  const bom = buildBomCreatePayload("uid1", {
    productName: "Hog Starter Crumble",
    lines: [{ materialId: "m1", materialName: "US Soya", inclusionPct: 24 }]
  }, ts);
  assert.deepEqual(Object.keys(bom).sort(), [
    "batchBaseKg", "createdAt", "lines", "notes", "productName", "section", "updatedAt", "userId", "yieldLossPct"
  ]);

  const rep = buildReprocessCreatePayload("uid1", {
    productName: "Hog Starter Crumble",
    returnedBags: 2,
    bagWeightKg: 25,
    status: "Damaged",
    fastActionNeeded: true
  }, ts);
  assert.equal(rep.fastActionNeeded, false);
  assert.equal(rep.flaggedToInventory, false);

  const cust = buildCustomerCreatePayload("uid1", { name: "Bulacan Agro", priority: true }, ts);
  assert.equal(cust.priority, true);

  const settings = buildSettingsCreatePayload("uid1", { lookbackDays: 21, defaultSafetyBufferPct: 20 }, ts);
  assert.deepEqual(Object.keys(settings).sort(), [
    "createdAt", "defaultSafetyBufferPct", "lookbackDays", "seededOctober2026", "updatedAt", "userId"
  ]);

  const bc = buildBreadcrumbPayload("uid1", { category: "Orders", action: "Test" }, ts);
  assert.equal(bc.category, "Orders");
  assert.equal(bc.actorType, "User");
});

test("buildDetailedEmailReportText, buildDailyLogMessengerText, and buildMailtoUrl format report sharing outputs", () => {
  const skus = [{ productName: "Hog Starter Crumble", section: "SAN ROQUE", openingStockKg: 500, standardBagWeightKg: 25 }];
  const batches = [{ date: "2026-11-26", section: "SAN ROQUE", productName: "Hog Starter Crumble", bagsProduced: 20, expectedWeightKg: 500, actualRecoveredKg: 495 }];
  const dispatches = [{ date: "2026-11-26", drReference: "DR-900", customer: "Farm One", sourcePlant: "SAN ROQUE", productName: "Hog Starter Crumble", bagsDispatched: 10, totalWeightKg: 250 }];
  const orders = [{ orderDate: "2026-11-26", dueDate: "2026-11-27", orderCode: "PO-900", customer: "Farm One", productName: "Hog Starter Crumble", totalWeightKg: 500, dispatchedWeightKg: 250, status: "Partially Dispatched" }];
  const summaries = computeSkuSummaries({ skus, batches, dispatches, orders });

  const report = buildPlantReport({
    startDate: "2026-11-26",
    endDate: "2026-11-26",
    sectionFilter: "ALL",
    skus,
    batches,
    dispatches,
    orders,
    summaries
  });

  const emailText = buildDetailedEmailReportText(report);
  assert.match(emailText, /10K AGRI FEEDS - BULACAN PLANT OPERATIONS REPORT/);
  assert.match(emailText, /DR-900/);
  assert.match(emailText, /PO-900/);

  const logs = computeDailyLogs({ skus, batches, dispatches, orders, todayIso: "2026-11-26" });
  const dailyMsg = buildDailyLogMessengerText(logs[0], "SAN ROQUE");
  assert.match(dailyMsg, /\*10K AGRI FEEDS - DAILY PLANT LOG\*/);
  assert.match(dailyMsg, /Hog Starter Crumble/);

  const mailto = buildMailtoUrl({
    to: "manager@10kfeeds.ph",
    subject: "Plant Report",
    body: "Hello Team"
  });
  assert.equal(mailto, "mailto:manager%4010kfeeds.ph?subject=Plant%20Report&body=Hello%20Team");
});

