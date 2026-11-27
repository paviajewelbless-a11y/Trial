const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_bulacan_1";
const BOB_UID = "bob_bulacan_2";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/skus`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/dispatches`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/orders`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/production_batches`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/schedules`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/customers`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/breadcrumbs`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/raw_materials`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/boms`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/reprocess_items`).get());
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/skus`).doc("sku_1").set({
      userId: ALICE_UID,
      productName: "EVT CRACK CORN",
      section: "SAN ROQUE",
      openingStockKg: 1000,
      standardBagWeightKg: 50,
      targetRecoveryPct: 98.5,
      safetyBufferPct: 10,
      defaultRemarks: "MIXING/CONVEYOR",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated owner: valid SKU, Dispatch, Order, Batch, Schedule, Settings creates and partial updates succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  // 1. SKU create and update
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/skus`).doc("sku_hog_starter").set({
      userId: ALICE_UID,
      productName: "MB BROODSOW MASH",
      section: "SAN ROQUE",
      openingStockKg: 5000,
      standardBagWeightKg: 50,
      targetRecoveryPct: 98.5,
      safetyBufferPct: 15,
      defaultRemarks: "MIXING/CONVEYOR",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/skus`).doc("sku_hog_starter").update({
      productName: "MB BROODSOW MASH",
      section: "SAN ROQUE",
      openingStockKg: 19150,
      standardBagWeightKg: 50,
      targetRecoveryPct: 98.5,
      safetyBufferPct: 15,
      defaultRemarks: "GRINDING /MIXING",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // 2. Dispatch (basic + rich logistics + return/hold validation)
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/dispatches`).doc("disp_1").set({
      userId: ALICE_UID,
      date: "2026-10-15",
      productName: "10K Hog Starter Mash",
      bagsDispatched: 40,
      bagWeightKg: 50,
      totalWeightKg: 2000,
      drReference: "DR-2026-1015",
      customer: "San Rafael Piggery",
      linkedOrderId: "ord_1",
      sourcePlant: "SAN ROQUE",
      driverName: "Juan Dela Cruz",
      truckNumber: "ABC-1234",
      helperName: "Pedro Santos",
      remarks: "Gate 2 unloading",
      dispatchStatus: "Delivered",
      hasReturn: false,
      returnConfirmed: false,
      returnedBags: 0,
      returnedWeightKg: 0,
      returnReason: "",
      isHeld: false,
      heldRemarks: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject invalid sourcePlant section on dispatch
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/dispatches`).doc("disp_bad_plant").set({
      userId: ALICE_UID,
      date: "2026-10-15",
      productName: "10K Hog Starter Mash",
      bagsDispatched: 10,
      bagWeightKg: 50,
      totalWeightKg: 500,
      drReference: "DR-BAD-PLANT",
      customer: "San Rafael Piggery",
      linkedOrderId: "",
      sourcePlant: "UNKNOWN_PLANT",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid update marking partial confirmed return and updating plant
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/dispatches`).doc("disp_1").update({
      sourcePlant: "PANDACAQUI",
      dispatchStatus: "Returned",
      hasReturn: true,
      returnConfirmed: true,
      returnedBags: 5,
      returnedWeightKg: 250,
      returnReason: "Item with Bad Order",
      updatedAt: new Date(Date.now() - 800),
    })
  );

  // Reject held dispatch without heldRemarks
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/dispatches`).doc("disp_bad_hold").set({
      userId: ALICE_UID,
      date: "2026-10-15",
      productName: "10K Hog Starter Mash",
      bagsDispatched: 10,
      bagWeightKg: 50,
      totalWeightKg: 500,
      drReference: "DR-HOLD-1",
      customer: "San Rafael Piggery",
      linkedOrderId: "",
      dispatchStatus: "Held",
      isHeld: true,
      heldRemarks: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject confirmed return without returnReason
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/dispatches`).doc("disp_bad_ret").set({
      userId: ALICE_UID,
      date: "2026-10-15",
      productName: "10K Hog Starter Mash",
      bagsDispatched: 10,
      bagWeightKg: 50,
      totalWeightKg: 500,
      drReference: "DR-RET-1",
      customer: "San Rafael Piggery",
      linkedOrderId: "",
      dispatchStatus: "Returned",
      hasReturn: true,
      returnConfirmed: true,
      returnedBags: 2,
      returnedWeightKg: 100,
      returnReason: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // 3. Order create & partial dispatch update & terminal state lock
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/orders`).doc("ord_1").set({
      userId: ALICE_UID,
      orderCode: "ORD-1001",
      orderDate: "2026-10-14",
      dueDate: "2026-10-18",
      productName: "10K Hog Starter Mash",
      orderedBags: 60,
      bagWeightKg: 50,
      totalWeightKg: 3000,
      dispatchedWeightKg: 0,
      customer: "San Rafael Piggery",
      status: "Open",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/orders`).doc("ord_1").update({
      dispatchedWeightKg: 3000,
      status: "Fulfilled",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Terminal state lock: updating Fulfilled order must fail
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/orders`).doc("ord_1").update({
      status: "Open",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // 3b. Second order create (with priority: true, rushOrder: true) + edit rushOrder/priority/qty update + cancel + delete
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/orders`).doc("ord_2").set({
      userId: ALICE_UID,
      orderCode: "RUSH-DR-BLC-4821",
      orderDate: "2026-11-19",
      dueDate: "2026-11-19",
      productName: "MB BROODSOW MASH",
      orderedBags: 200,
      bagWeightKg: 50,
      totalWeightKg: 10000,
      dispatchedWeightKg: 0,
      customer: "North Metro Agri Ventures",
      status: "Open",
      priority: true,
      rushOrder: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject invalid non-boolean rushOrder
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/orders`).doc("ord_bad_rush").set({
      userId: ALICE_UID,
      orderCode: "RUSH-BAD",
      orderDate: "2026-11-19",
      dueDate: "2026-11-19",
      productName: "MB BROODSOW MASH",
      orderedBags: 20,
      bagWeightKg: 50,
      totalWeightKg: 1000,
      dispatchedWeightKg: 0,
      customer: "North Metro Agri Ventures",
      status: "Open",
      priority: true,
      rushOrder: "yes",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/orders`).doc("ord_2").update({
      dueDate: "2026-11-06",
      orderedBags: 220,
      bagWeightKg: 50,
      totalWeightKg: 11000,
      customer: "North Metro Agri Ventures",
      priority: false,
      rushOrder: false,
      status: "Open",
      updatedAt: new Date(Date.now() - 800),
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/orders`).doc("ord_2").update({
      dueDate: "2026-11-06",
      orderedBags: 220,
      bagWeightKg: 50,
      totalWeightKg: 11000,
      customer: "North Metro Agri Ventures",
      priority: false,
      rushOrder: false,
      status: "Cancelled",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/orders`).doc("ord_2").delete()
  );

  // 4. Production Batch (fractional bag weight + customer schedule attribution + edit recovered bags update)
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/production_batches`).doc("batch_1").set({
      userId: ALICE_UID,
      date: "2026-10-12",
      section: "PANDACAQUI",
      productName: "BARAKO DUCK PELLET",
      bagsProduced: 50,
      bagWeightKg: 10.9,
      expectedWeightKg: 545.0,
      actualRecoveredKg: 543.8,
      varianceKg: -1.2,
      recoveryPct: 99.78,
      remarks: "GRINDING /MIXING",
      scheduleRunId: "sched_1",
      linkedOrderIds: ["ord_1"],
      linkedOrderCodes: ["ORD-1001"],
      scheduledForCustomers: ["San Rafael Piggery"],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/production_batches`).doc("batch_1").update({
      date: "2026-10-12",
      section: "PANDACAQUI",
      productName: "BARAKO DUCK PELLET",
      bagsProduced: 48,
      bagWeightKg: 10.9,
      expectedWeightKg: 523.2,
      actualRecoveredKg: 523.2,
      varianceKg: 0.0,
      recoveryPct: 100.0,
      remarks: "GRINDING /MIXING",
      scheduleRunId: "sched_1",
      linkedOrderIds: ["ord_1", "ord_2"],
      linkedOrderCodes: ["ORD-1001", "PO-2611-32"],
      scheduledForCustomers: ["San Rafael Piggery", "North Metro Agri Ventures"],
      updatedAt: new Date(Date.now() - 400),
    })
  );

  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/production_batches`).doc("batch_bad_cust").set({
      userId: ALICE_UID,
      date: "2026-10-12",
      section: "PANDACAQUI",
      productName: "BARAKO DUCK PELLET",
      bagsProduced: 10,
      bagWeightKg: 50,
      expectedWeightKg: 500,
      actualRecoveredKg: 500,
      varianceKg: 0,
      recoveryPct: 100,
      remarks: "MIXING/CONVEYOR",
      scheduledForCustomers: "NotAList",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // 5. Schedule Run create & status transitions + cancel + terminal lock + all 4 sections
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/schedules`).doc("sched_1").set({
      userId: ALICE_UID,
      scheduledDate: "2026-10-20",
      section: "SAN ROQUE",
      productName: "MB BROODSOW MASH",
      targetBags: 120,
      bagWeightKg: 50,
      expectedWeightKg: 6000,
      lineRemarks: "MIXING/CONVEYOR",
      priority: "Urgent",
      status: "Scheduled",
      linkedOrderId: "ord_1",
      linkedOrderCode: "ORD-1001",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/schedules`).doc("sched_la_union").set({
      userId: ALICE_UID,
      scheduledDate: "2026-11-20",
      section: "LA UNION QUAIL FEEDS",
      productName: "QUAIL LAYER MASH",
      targetBags: 40,
      bagWeightKg: 50,
      expectedWeightKg: 2000,
      lineRemarks: "GRINDING /MIXING",
      priority: "Buffer Top-Up",
      status: "Scheduled",
      linkedOrderId: "",
      linkedOrderCode: "",
      linkedOrderIds: ["ord_1", "ord_2"],
      linkedOrderCodes: ["ORD-1001", "PO-2611-32"],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Edit schedule details and multi-order links, then start, then cancel, then verify terminal lock + delete
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/schedules`).doc("sched_la_union").update({
      scheduledDate: "2026-11-22",
      targetBags: 55,
      bagWeightKg: 50,
      expectedWeightKg: 2750,
      lineRemarks: "MIXING/CONVEYOR",
      priority: "Urgent",
      status: "Scheduled",
      linkedOrderId: "ord_1",
      linkedOrderCode: "ORD-1001",
      linkedOrderIds: ["ord_1", "ord_2", "ord_3"],
      linkedOrderCodes: ["ORD-1001", "PO-2611-32", "PO-2611-58"],
      updatedAt: new Date(Date.now() - 950),
    })
  );

  // Start then cancel a running schedule
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/schedules`).doc("sched_la_union").update({
      status: "In Progress",
      updatedAt: new Date(Date.now() - 900),
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/schedules`).doc("sched_la_union").update({
      status: "Cancelled",
      updatedAt: new Date(Date.now() - 700),
    })
  );

  // Terminal state lock on Cancelled schedule
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/schedules`).doc("sched_la_union").update({
      status: "Completed",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/schedules`).doc("sched_la_union").delete()
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/schedules`).doc("sched_1").update({
      status: "In Progress",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/schedules`).doc("sched_1").update({
      status: "Completed",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/schedules`).doc("sched_1").update({
      status: "Scheduled",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // 6. Settings create, re-seed partial update, and rejecting overwrite of createdAt
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/settings`).doc("plant_state").set({
      userId: ALICE_UID,
      seededOctober2026: true,
      lookbackDays: 14,
      defaultSafetyBufferPct: 15.0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/settings`).doc("plant_state").update({
      seededOctober2026: true,
      lookbackDays: 14,
      defaultSafetyBufferPct: 15.0,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/settings`).doc("plant_state").set({
      userId: ALICE_UID,
      seededOctober2026: true,
      lookbackDays: 14,
      defaultSafetyBufferPct: 15.0,
      createdAt: new Date(Date.now() - 200),
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // 7. Customer create (with priority), edit priority, reject invalid priority type, delete, and list
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/customers`).doc("cust_3k").set({
      userId: ALICE_UID,
      name: "3K Agrivet Supply",
      notes: "Bulacan account",
      priority: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/customers`).doc("cust_3k").update({
      name: "3K Agrivet Supply - Main",
      notes: "Updated branch",
      priority: false,
      updatedAt: new Date(Date.now() - 300),
    })
  );

  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/customers`).doc("cust_3k").update({
      name: "3K Agrivet Supply - Main",
      notes: "Updated branch",
      priority: "HIGH",
      updatedAt: new Date(Date.now() - 250),
    })
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/customers`).get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/customers`).doc("cust_3k").delete());

  // List queries succeed on own subcollections (including empty ones)
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/skus`).get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/dispatches`).get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/orders`).get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/production_batches`).get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/schedules`).get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/breadcrumbs`).get());

  // 8. Breadcrumbs (User & System audit history: create, list, immutable update denied, invalid rejected, delete)
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/breadcrumbs`).doc("bc_user_1").set({
      userId: ALICE_UID,
      actorType: "User",
      category: "Navigation",
      action: "Navigated to Orders",
      details: "Switched workspace tab from Summary to Orders",
      screenTab: "Orders",
      entityRef: "",
      trailPath: "Summary > Orders",
      severity: "Info",
      dateStr: "2026-11-23",
      timeLabel: "19:10:05",
      timestampMillis: 1795403405000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/breadcrumbs`).doc("bc_sys_1").set({
      userId: ALICE_UID,
      actorType: "System",
      category: "Dispatch",
      action: "Blocked Over-Dispatch",
      details: "Requested 500 kg exceeds available stock 200 kg for MB BROODSOW MASH",
      screenTab: "Dispatched",
      entityRef: "MB BROODSOW MASH",
      trailPath: "Orders > Dispatched",
      severity: "Warning",
      dateStr: "2026-11-23",
      timeLabel: "19:12:00",
      timestampMillis: 1795403520000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Breadcrumbs are immutable (updates rejected)
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/breadcrumbs`).doc("bc_user_1").update({
      action: "Tampered Action",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Reject invalid actorType or ghost id
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/breadcrumbs`).doc("bc_bad").set({
      userId: ALICE_UID,
      actorType: "Admin",
      category: "System",
      action: "Invalid actor",
      details: "",
      screenTab: "History",
      entityRef: "",
      trailPath: "History",
      severity: "Info",
      dateStr: "2026-11-23",
      timeLabel: "19:13:00",
      timestampMillis: 1795403580000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/breadcrumbs`).doc("bc_user_1").delete()
  );

  // 9. Raw Materials (create, partial update, invalid category/negative stock denied, list & delete)
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/raw_materials`).doc("raw_yellow_corn").set({
      userId: ALICE_UID,
      name: "Yellow Corn (Grains)",
      category: "Grains & Energy",
      currentStockKg: 4200,
      reorderThresholdKg: 3500,
      usageSharePct: 48.0,
      leadTimeDays: 4,
      safetyDays: 3,
      supplierName: "Pangasinan Grain Silo",
      alertsEnabled: true,
      lastRestockedDate: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/raw_materials`).doc("raw_yellow_corn").update({
      currentStockKg: 6200,
      alertsEnabled: false,
      lastRestockedDate: "2026-11-23",
      updatedAt: new Date(Date.now() - 100),
    })
  );

  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/raw_materials`).doc("raw_bad").set({
      userId: ALICE_UID,
      name: "Bad Corn",
      category: "UnknownCategory",
      currentStockKg: 100,
      reorderThresholdKg: 50,
      usageSharePct: 10,
      leadTimeDays: 3,
      safetyDays: 2,
      supplierName: "",
      alertsEnabled: true,
      lastRestockedDate: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/raw_materials`).get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/raw_materials`).doc("raw_yellow_corn").delete());

  // 10. Bills of Materials (BOM: create, update, invalid empty lines denied, list & delete)
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/boms`).doc("bom_sow_mash").set({
      userId: ALICE_UID,
      productName: "MB BROODSOW MASH",
      section: "SAN ROQUE",
      batchBaseKg: 1000.0,
      yieldLossPct: 1.5,
      lines: [
        {
          materialId: "raw_yellow_corn",
          materialName: "Yellow Corn (Grains)",
          category: "Grains & Energy",
          inclusionPct: 50.0,
          notes: "Coarse grind"
        },
        {
          materialId: "raw_soybean_meal",
          materialName: "Soybean Meal (SBM)",
          category: "Protein Meals",
          inclusionPct: 50.0,
          notes: "High CP"
        }
      ],
      notes: "Test BOM",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/boms`).doc("bom_sow_mash").update({
      batchBaseKg: 1000.0,
      yieldLossPct: 1.2,
      notes: "Updated BOM",
      updatedAt: new Date(Date.now() - 100),
    })
  );

  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/boms`).doc("bom_bad").set({
      userId: ALICE_UID,
      productName: "Empty BOM",
      section: "SAN ROQUE",
      batchBaseKg: 1000.0,
      yieldLossPct: 1.5,
      lines: [],
      notes: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/boms`).get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/boms`).doc("bom_sow_mash").delete());

  // 11. Reprocess Items (create pending fast-action return, transition to Reprocessing -> Flagged to Inventory, reject bad payload, delete)
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/reprocess_items`).doc("rep_1").set({
      userId: ALICE_UID,
      sourceDispatchId: "disp_1",
      drReference: "DR-2026-1015",
      customer: "San Rafael Piggery",
      productName: "10K Hog Starter Mash",
      section: "SAN ROQUE",
      returnedDate: "2026-10-16",
      returnedBags: 5,
      bagWeightKg: 50,
      returnedWeightKg: 250,
      reprocessedBags: 0,
      reprocessedWeightKg: 0,
      returnReason: "Item with Bad Order",
      status: "Pending Reprocess",
      fastActionNeeded: true,
      flaggedToInventory: false,
      reprocessedDate: "",
      actionNotes: "Quarantined for re-milling",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/reprocess_items`).doc("rep_1").update({
      status: "Reprocessing",
      actionNotes: "Blending on mixer line 1",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/reprocess_items`).doc("rep_1").update({
      reprocessedBags: 5,
      reprocessedWeightKg: 245,
      status: "Flagged to Inventory",
      fastActionNeeded: false,
      flaggedToInventory: true,
      reprocessedDate: "2026-10-16",
      actionNotes: "Reprocessed & released to warehouse",
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject missing returnReason or invalid status or flagged without Flagged status
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/reprocess_items`).doc("rep_bad").set({
      userId: ALICE_UID,
      sourceDispatchId: "",
      drReference: "",
      customer: "San Rafael Piggery",
      productName: "10K Hog Starter Mash",
      section: "SAN ROQUE",
      returnedDate: "2026-10-16",
      returnedBags: 5,
      bagWeightKg: 50,
      returnedWeightKg: 250,
      reprocessedBags: 0,
      reprocessedWeightKg: 0,
      returnReason: "",
      status: "Pending Reprocess",
      fastActionNeeded: true,
      flaggedToInventory: true,
      reprocessedDate: "",
      actionNotes: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Mark a separate returned lot as Damaged (terminal write-off: not flaggedToInventory, not fastActionNeeded)
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/reprocess_items`).doc("rep_2").set({
      userId: ALICE_UID,
      sourceDispatchId: "",
      drReference: "DR-2026-1019",
      customer: "Tarlac Hatchery",
      productName: "10K Hog Starter Mash",
      section: "SAN ROQUE",
      returnedDate: "2026-10-16",
      returnedBags: 3,
      bagWeightKg: 50,
      returnedWeightKg: 150,
      reprocessedBags: 0,
      reprocessedWeightKg: 0,
      returnReason: "Item with Bad Order",
      status: "Pending Reprocess",
      fastActionNeeded: true,
      flaggedToInventory: false,
      reprocessedDate: "",
      actionNotes: "Awaiting inspection",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/reprocess_items`).doc("rep_2").update({
      status: "Damaged",
      fastActionNeeded: false,
      flaggedToInventory: false,
      reprocessedBags: 0,
      reprocessedWeightKg: 0,
      reprocessedDate: "2026-10-17",
      actionNotes: "Water-soaked & moldy bags — disposed",
      updatedAt: new Date(Date.now() - 50),
    })
  );

  // Reject marking Damaged while leaving flaggedToInventory or fastActionNeeded true
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/reprocess_items`).doc("rep_2").update({
      status: "Damaged",
      flaggedToInventory: true,
      fastActionNeeded: false,
      updatedAt: new Date(Date.now() - 25),
    })
  );
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/reprocess_items`).doc("rep_2").update({
      status: "Damaged",
      flaggedToInventory: false,
      fastActionNeeded: true,
      updatedAt: new Date(Date.now() - 25),
    })
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/reprocess_items`).get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/reprocess_items`).doc("rep_1").delete());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/reprocess_items`).doc("rep_2").delete());
});

test("Cross-user isolation & shadow/poisoned payloads are rejected", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection(`users/${BOB_UID}/skus`).doc("bob_sku").set({
      userId: BOB_UID,
      productName: "CHOOKMAX 1000 [SELECTA]",
      section: "SELECTA",
      openingStockKg: 500,
      standardBagWeightKg: 25,
      targetRecoveryPct: 98.5,
      safetyBufferPct: 10,
      defaultRemarks: "GRINDING/BLOWER FAN",
      createdAt: new Date(Date.now() - 10000),
      updatedAt: new Date(Date.now() - 10000),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 1000);

  // Cross-user read & write fail
  await assertFails(aliceDb.collection(`users/${BOB_UID}/skus`).get());
  await assertFails(aliceDb.collection(`users/${BOB_UID}/skus`).doc("bob_sku").get());
  await assertFails(aliceDb.collection(`users/${BOB_UID}/customers`).get());
  await assertFails(aliceDb.collection(`users/${BOB_UID}/breadcrumbs`).get());
  await assertFails(aliceDb.collection(`users/${BOB_UID}/raw_materials`).get());
  await assertFails(aliceDb.collection(`users/${BOB_UID}/boms`).get());
  await assertFails(aliceDb.collection(`users/${BOB_UID}/reprocess_items`).get());
  await assertFails(
    aliceDb.collection(`users/${BOB_UID}/skus`).doc("bob_sku").update({
      openingStockKg: 0,
      updatedAt: ts,
    })
  );

  // Reject ghost `id` key (since Kotlin uses @DocumentId)
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/skus`).doc("sku_ghost").set({
      id: "sku_ghost",
      userId: ALICE_UID,
      productName: "Ghost Feed",
      section: "SELECTA",
      openingStockKg: 100,
      standardBagWeightKg: 25,
      targetRecoveryPct: 98.5,
      safetyBufferPct: 10,
      defaultRemarks: "MIXING/CONVEYOR",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject invalid section enum
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/skus`).doc("sku_bad_sec").set({
      userId: ALICE_UID,
      productName: "Bad Section",
      section: "UNKNOWN_LINE",
      openingStockKg: 100,
      standardBagWeightKg: 25,
      targetRecoveryPct: 98.5,
      safetyBufferPct: 10,
      defaultRemarks: "MIXING/CONVEYOR",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/skus`).doc("sku_future").set({
      userId: ALICE_UID,
      productName: "Future Feed",
      section: "SELECTA",
      openingStockKg: 100,
      standardBagWeightKg: 25,
      targetRecoveryPct: 98.5,
      safetyBufferPct: 10,
      defaultRemarks: "MIXING/CONVEYOR",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Shared multi-user workspace: invites, role-based permissions (Admin, Operator, Viewer), escalation protection & instant revocation", async () => {
  const CAROL_UID = "carol_viewer_3";
  const DAN_UID = "dan_admin_4";
  const EVE_UID = "eve_outsider_5";

  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@10kfeeds.ph", email_verified: true })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, { email: "Bob@10KFeeds.ph", email_verified: true })
    .firestore();
  const carolDb = testEnv
    .authenticatedContext(CAROL_UID, { email: "carol@10kfeeds.ph", email_verified: true })
    .firestore();
  const danDb = testEnv
    .authenticatedContext(DAN_UID, { email: "dan@10kfeeds.ph", email_verified: true })
    .firestore();
  const eveDb = testEnv
    .authenticatedContext(EVE_UID, { email: "eve@10kfeeds.ph", email_verified: true })
    .firestore();

  const ts = new Date(Date.now() - 5000);

  // Seed initial SKU and Settings in Alice's workspace
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/settings`).doc("plant_state").set({
      userId: ALICE_UID,
      seededOctober2026: true,
      lookbackDays: 14,
      defaultSafetyBufferPct: 15,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/skus`).doc("sku_starter").set({
      userId: ALICE_UID,
      productName: "PM HOG STARTER",
      section: "SAN ROQUE",
      openingStockKg: 5000,
      standardBagWeightKg: 50,
      targetRecoveryPct: 98.5,
      safetyBufferPct: 15,
      defaultRemarks: "MIXING/CONVEYOR",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice invites Bob (Operator), Carol (Viewer), and Dan (Admin)
  const bobInviteId = "inv_alice_bob";
  const carolInviteId = "inv_alice_carol";
  const danInviteId = "inv_alice_dan";

  await assertSucceeds(
    aliceDb.collection("workspace_invites").doc(bobInviteId).set({
      workspaceOwnerId: ALICE_UID,
      workspaceName: "Bulacan Plant",
      ownerEmail: "alice@10kfeeds.ph",
      memberEmail: "bob@10kfeeds.ph",
      memberDisplayName: "Bob Operator",
      role: "Operator",
      status: "Pending",
      memberUid: "",
      invitedByEmail: "alice@10kfeeds.ph",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("workspace_invites").doc(carolInviteId).set({
      workspaceOwnerId: ALICE_UID,
      workspaceName: "Bulacan Plant",
      ownerEmail: "alice@10kfeeds.ph",
      memberEmail: "carol@10kfeeds.ph",
      memberDisplayName: "Carol Viewer",
      role: "Viewer",
      status: "Pending",
      memberUid: "",
      invitedByEmail: "alice@10kfeeds.ph",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("workspace_invites").doc(danInviteId).set({
      workspaceOwnerId: ALICE_UID,
      workspaceName: "Bulacan Plant",
      ownerEmail: "alice@10kfeeds.ph",
      memberEmail: "dan@10kfeeds.ph",
      memberDisplayName: "Dan Admin",
      role: "Admin",
      status: "Pending",
      memberUid: "",
      invitedByEmail: "alice@10kfeeds.ph",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Owner queries invites for their workspace
  await assertSucceeds(
    aliceDb.collection("workspace_invites").where("workspaceOwnerId", "==", ALICE_UID).get()
  );

  // Bob queries his invites by lowercased email
  await assertSucceeds(
    bobDb.collection("workspace_invites").where("memberEmail", "==", "bob@10kfeeds.ph").get()
  );

  // Eve cannot read Bob's invite or Alice's workspace
  await assertFails(eveDb.collection("workspace_invites").doc(bobInviteId).get());
  await assertFails(
    eveDb.collection("workspace_invites").where("workspaceOwnerId", "==", ALICE_UID).get()
  );
  await assertFails(
    eveDb.collection(`users/${ALICE_UID}/team_members`).doc(EVE_UID).set({
      userId: EVE_UID,
      workspaceOwnerId: ALICE_UID,
      inviteId: bobInviteId,
      email: "eve@10kfeeds.ph",
      displayName: "Eve",
      role: "Admin",
      invitedByEmail: "alice@10kfeeds.ph",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Role escalation rejection: Bob cannot claim his Operator invite as Admin
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/team_members`).doc(BOB_UID).set({
      userId: BOB_UID,
      workspaceOwnerId: ALICE_UID,
      inviteId: bobInviteId,
      email: "bob@10kfeeds.ph",
      displayName: "Bob Operator",
      role: "Admin",
      invitedByEmail: "alice@10kfeeds.ph",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob cannot mutate invite role while accepting
  await assertFails(
    bobDb.collection("workspace_invites").doc(bobInviteId).update({
      role: "Admin",
      status: "Active",
      memberUid: BOB_UID,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob legitimately accepts his Operator invite (creates team_members/{BOB_UID} & marks invite Active)
  await assertSucceeds(
    bobDb.collection(`users/${ALICE_UID}/team_members`).doc(BOB_UID).set({
      userId: BOB_UID,
      workspaceOwnerId: ALICE_UID,
      inviteId: bobInviteId,
      email: "bob@10kfeeds.ph",
      displayName: "Bob Operator",
      role: "Operator",
      invitedByEmail: "alice@10kfeeds.ph",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("workspace_invites").doc(bobInviteId).update({
      status: "Active",
      memberUid: BOB_UID,
      memberDisplayName: "Bob Operator",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Carol claims Viewer invite; Dan claims Admin invite
  await assertSucceeds(
    carolDb.collection(`users/${ALICE_UID}/team_members`).doc(CAROL_UID).set({
      userId: CAROL_UID,
      workspaceOwnerId: ALICE_UID,
      inviteId: carolInviteId,
      email: "carol@10kfeeds.ph",
      displayName: "Carol Viewer",
      role: "Viewer",
      invitedByEmail: "alice@10kfeeds.ph",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    carolDb.collection("workspace_invites").doc(carolInviteId).update({
      status: "Active",
      memberUid: CAROL_UID,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    danDb.collection(`users/${ALICE_UID}/team_members`).doc(DAN_UID).set({
      userId: DAN_UID,
      workspaceOwnerId: ALICE_UID,
      inviteId: danInviteId,
      email: "dan@10kfeeds.ph",
      displayName: "Dan Admin",
      role: "Admin",
      invitedByEmail: "alice@10kfeeds.ph",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    danDb.collection("workspace_invites").doc(danInviteId).update({
      status: "Active",
      memberUid: DAN_UID,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // OPERATOR PERMISSIONS (Bob):
  // Can read SKUs, Settings, Team Members, and log Order / Dispatch / Breadcrumb
  await assertSucceeds(bobDb.collection(`users/${ALICE_UID}/skus`).get());
  await assertSucceeds(bobDb.collection(`users/${ALICE_UID}/settings`).get());
  await assertSucceeds(bobDb.collection(`users/${ALICE_UID}/team_members`).get());
  await assertSucceeds(
    bobDb.collection(`users/${ALICE_UID}/orders`).doc("ord_bob_1").set({
      userId: ALICE_UID,
      orderCode: "PO-2611-77",
      orderDate: "2026-10-15",
      dueDate: "2026-10-18",
      productName: "PM HOG STARTER",
      orderedBags: 20,
      bagWeightKg: 50,
      totalWeightKg: 1000,
      dispatchedWeightKg: 0,
      customer: "San Rafael Piggery",
      status: "Open",
      priority: false,
      rushOrder: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection(`users/${ALICE_UID}/breadcrumbs`).doc("bc_bob_1").set({
      userId: ALICE_UID,
      actorType: "User",
      category: "Orders",
      action: "Created Order PO-2611-77",
      details: "By bob@10kfeeds.ph (Operator)",
      screenTab: "Orders",
      entityRef: "PO-2611-77",
      trailPath: "Summary › Orders",
      severity: "Success",
      dateStr: "2026-10-15",
      timeLabel: "08:30:00",
      timestampMillis: 1792053000000,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  // Operator CANNOT delete SKUs, CANNOT change settings, CANNOT delete breadcrumbs, CANNOT invite/promote users
  await assertFails(bobDb.collection(`users/${ALICE_UID}/skus`).doc("sku_starter").delete());
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/settings`).doc("plant_state").update({
      lookbackDays: 30,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection(`users/${ALICE_UID}/breadcrumbs`).doc("bc_bob_1").delete());
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/team_members`).doc(BOB_UID).update({
      role: "Admin",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // VIEWER PERMISSIONS (Carol):
  // Can read SKUs, Orders, Team Members, Settings
  await assertSucceeds(carolDb.collection(`users/${ALICE_UID}/skus`).get());
  await assertSucceeds(carolDb.collection(`users/${ALICE_UID}/orders`).get());
  // Cannot create Orders, Breadcrumbs, or modify any document
  await assertFails(
    carolDb.collection(`users/${ALICE_UID}/orders`).doc("ord_carol_1").set({
      userId: ALICE_UID,
      orderCode: "PO-2611-88",
      orderDate: "2026-10-15",
      dueDate: "2026-10-18",
      productName: "PM HOG STARTER",
      orderedBags: 10,
      bagWeightKg: 50,
      totalWeightKg: 500,
      dispatchedWeightKg: 0,
      customer: "San Rafael Piggery",
      status: "Open",
      priority: false,
      rushOrder: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    carolDb.collection(`users/${ALICE_UID}/breadcrumbs`).doc("bc_carol_1").set({
      userId: ALICE_UID,
      actorType: "User",
      category: "Navigation",
      action: "Viewed Orders",
      details: "",
      screenTab: "Orders",
      entityRef: "Orders",
      trailPath: "Summary › Orders",
      severity: "Info",
      dateStr: "2026-10-15",
      timeLabel: "09:00:00",
      timestampMillis: 1792054800000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // DELEGATED ADMIN PERMISSIONS (Dan):
  // Can list team_invites and team_members under Alice's workspace, update settings, change Bob's role, and delete SKU
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/team_invites`).doc(bobInviteId).set({
      workspaceOwnerId: ALICE_UID,
      workspaceName: "Bulacan Plant",
      ownerEmail: "alice@10kfeeds.ph",
      memberEmail: "bob@10kfeeds.ph",
      memberDisplayName: "Bob Operator",
      role: "Operator",
      status: "Pending",
      memberUid: "",
      invitedByEmail: "alice@10kfeeds.ph",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(danDb.collection(`users/${ALICE_UID}/team_invites`).get());
  await assertSucceeds(danDb.collection(`users/${ALICE_UID}/team_members`).get());
  await assertSucceeds(
    danDb.collection(`users/${ALICE_UID}/settings`).doc("plant_state").update({
      lookbackDays: 21,
      updatedAt: new Date(Date.now() - 400),
    })
  );
  await assertSucceeds(
    danDb.collection(`users/${ALICE_UID}/team_members`).doc(BOB_UID).update({
      role: "Viewer",
      updatedAt: new Date(Date.now() - 300),
    })
  );
  // Now that Bob was demoted to Viewer, Bob CANNOT write orders anymore
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/orders`).doc("ord_bob_after_demote").set({
      userId: ALICE_UID,
      orderCode: "PO-2611-79",
      orderDate: "2026-10-15",
      dueDate: "2026-10-18",
      productName: "PM HOG STARTER",
      orderedBags: 5,
      bagWeightKg: 50,
      totalWeightKg: 250,
      dispatchedWeightKg: 0,
      customer: "San Rafael Piggery",
      status: "Open",
      priority: false,
      rushOrder: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(danDb.collection(`users/${ALICE_UID}/skus`).doc("sku_starter").delete());

  // DELEGATED ADMIN INVITE MANAGEMENT (Dan):
  // Dan (Admin) can invite Eve to Alice's workspace and later revoke the pending invite
  const eveInviteId = "inv_alice_eve";
  await assertSucceeds(
    danDb.collection("workspace_invites").doc(eveInviteId).set({
      workspaceOwnerId: ALICE_UID,
      workspaceName: "Bulacan Plant",
      ownerEmail: "alice@10kfeeds.ph",
      memberEmail: "eve@10kfeeds.ph",
      memberDisplayName: "Eve Viewer",
      role: "Viewer",
      status: "Pending",
      memberUid: "",
      invitedByEmail: "dan@10kfeeds.ph",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    danDb.collection(`users/${ALICE_UID}/team_invites`).doc(eveInviteId).set({
      workspaceOwnerId: ALICE_UID,
      workspaceName: "Bulacan Plant",
      ownerEmail: "alice@10kfeeds.ph",
      memberEmail: "eve@10kfeeds.ph",
      memberDisplayName: "Eve Viewer",
      role: "Viewer",
      status: "Pending",
      memberUid: "",
      invitedByEmail: "dan@10kfeeds.ph",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(danDb.collection("workspace_invites").doc(eveInviteId).get());
  await assertSucceeds(danDb.collection("workspace_invites").doc(eveInviteId).delete());
  await assertSucceeds(danDb.collection(`users/${ALICE_UID}/team_invites`).doc(eveInviteId).delete());

  // INSTANT REVOCATION: Alice removes Bob from workspace
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/team_members`).doc(BOB_UID).delete());
  await assertSucceeds(aliceDb.collection("workspace_invites").doc(bobInviteId).delete());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/skus`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/orders`).get());
});

