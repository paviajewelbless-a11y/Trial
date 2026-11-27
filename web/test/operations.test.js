import test from "node:test";
import assert from "node:assert/strict";
import {
  FIRESTORE_DATABASE_ID,
  HOSTED_WEB_URL,
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
