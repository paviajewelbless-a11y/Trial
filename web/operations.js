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

export const LINE_REMARKS_OPTIONS = [
  "MIXING/CONVEYOR",
  "GRINDING /MIXING",
  "GRINDING/BLOWER FAN",
  "GRINDING/BLOWER FAN/HAMMERMILL"
];

export const RETURN_REASONS = [
  "Wrong Item",
  "Item with Bad Order",
  "Old Products",
  "Quantity Variance",
  "Other"
];

export const SCHEDULE_PRIORITIES = ["Urgent", "Order-Driven", "Buffer Top-Up"];
export const SCHEDULE_STATUSES = ["Scheduled", "In Progress", "Completed", "Cancelled"];
export const REPROCESS_STATUSES = [
  "Pending Reprocess",
  "Reprocessing",
  "Flagged to Inventory",
  "Damaged"
];
export const REPORT_SECTION_OPTIONS = [
  "Inventory",
  "Orders",
  "Schedule",
  "Dispatches",
  "Recovery"
];

export const MATERIAL_SECTORS = {
  MACRO: { id: "MACRO", label: "Macro Materials", shortLabel: "Macro", isMicro: false },
  MICRO_FAST: { id: "MICRO_FAST", label: "Micro • Fast Moving", shortLabel: "Micro: Fast", isMicro: true },
  MICRO_SLOW: { id: "MICRO_SLOW", label: "Micro • Slow Moving", shortLabel: "Micro: Slow", isMicro: true },
  MICRO_NON_MOVING: { id: "MICRO_NON_MOVING", label: "Micro • Non-Moving / Trial", shortLabel: "Micro: Non-Mov", isMicro: true }
};

export const FEED_MATERIAL_CATALOG = [
  // Macro (9)
  { idHint: "raw_whole_yellow_corn", name: "Whole Yellow Corn/Balanok Corn", sector: "MACRO", movementTag: "MACRO", category: "Grains & Energy", defaultInclusionPct: 48.0, defaultStockKg: 5000.0, defaultReorderKg: 2500.0, defaultLeadDays: 4, defaultSupplier: "Macro Grain Supply" },
  { idHint: "raw_us_soya", name: "US Soya (Soy Bean Meal)", sector: "MACRO", movementTag: "MACRO", category: "Protein Meals", defaultInclusionPct: 24.0, defaultStockKg: 3200.0, defaultReorderKg: 1500.0, defaultLeadDays: 5, defaultSupplier: "US Soya Import" },
  { idHint: "raw_hard_pollard", name: "Hard Pollard", sector: "MACRO", movementTag: "MACRO", category: "Grains & Energy", defaultInclusionPct: 10.0, defaultStockKg: 2200.0, defaultReorderKg: 800.0, defaultLeadDays: 3, defaultSupplier: "Flour Mill Supply" },
  { idHint: "raw_soft_pollard", name: "Soft Pollard", sector: "MACRO", movementTag: "MACRO", category: "Grains & Energy", defaultInclusionPct: 8.0, defaultStockKg: 1800.0, defaultReorderKg: 700.0, defaultLeadDays: 3, defaultSupplier: "Flour Mill Supply" },
  { idHint: "raw_rice_bran_d1", name: "Rice Bran D1", sector: "MACRO", movementTag: "MACRO", category: "Grains & Energy", defaultInclusionPct: 12.0, defaultStockKg: 3500.0, defaultReorderKg: 1200.0, defaultLeadDays: 3, defaultSupplier: "Nueva Ecija Ricemill" },
  { idHint: "raw_rice_bran_d3", name: "Rice Bran D3", sector: "MACRO", movementTag: "MACRO", category: "Grains & Energy", defaultInclusionPct: 6.0, defaultStockKg: 1600.0, defaultReorderKg: 600.0, defaultLeadDays: 3, defaultSupplier: "Nueva Ecija Ricemill" },
  { idHint: "raw_corn_bran", name: "Corn Bran", sector: "MACRO", movementTag: "MACRO", category: "Grains & Energy", defaultInclusionPct: 5.0, defaultStockKg: 1400.0, defaultReorderKg: 500.0, defaultLeadDays: 4, defaultSupplier: "Corn Mill Supply" },
  { idHint: "raw_cookie_meal", name: "Cookie Meal", sector: "MACRO", movementTag: "MACRO", category: "Grains & Energy", defaultInclusionPct: 5.0, defaultStockKg: 1200.0, defaultReorderKg: 400.0, defaultLeadDays: 4, defaultSupplier: "Bakery By-Product" },
  { idHint: "raw_mollases", name: "Mollases", sector: "MACRO", movementTag: "MACRO", category: "Liquids & Oils", defaultInclusionPct: 2.5, defaultStockKg: 1100.0, defaultReorderKg: 450.0, defaultLeadDays: 4, defaultSupplier: "Sugar Central" },

  // Micro Fast Moving (11)
  { idHint: "raw_mesh_100", name: "Mesh 100 (Powder Limestone)", sector: "MICRO_FAST", movementTag: "FAST MOVING", category: "Minerals & Salt", defaultInclusionPct: 1.5, defaultStockKg: 850.0, defaultReorderKg: 300.0, defaultLeadDays: 3, defaultSupplier: "Micro • Fast Moving" },
  { idHint: "raw_mesh_45", name: "Mesh 45 (Fine Limestone - PCDLY)", sector: "MICRO_FAST", movementTag: "FAST MOVING", category: "Minerals & Salt", defaultInclusionPct: 1.5, defaultStockKg: 800.0, defaultReorderKg: 300.0, defaultLeadDays: 3, defaultSupplier: "Micro • Fast Moving" },
  { idHint: "raw_yellow_dye", name: "Yellow Dye / Ferna Egg Yellow", sector: "MICRO_FAST", movementTag: "FAST MOVING", category: "Premix & Additives", defaultInclusionPct: 0.1, defaultStockKg: 75.0, defaultReorderKg: 25.0, defaultLeadDays: 4, defaultSupplier: "Micro • Fast Moving" },
  { idHint: "raw_luctarom_pure", name: "Luctarom Pure", sector: "MICRO_FAST", movementTag: "FAST MOVING", category: "Premix & Additives", defaultInclusionPct: 0.1, defaultStockKg: 60.0, defaultReorderKg: 20.0, defaultLeadDays: 5, defaultSupplier: "Micro • Fast Moving" },
  { idHint: "raw_luctarom_premix", name: "Luctarom (PREMIX)", sector: "MICRO_FAST", movementTag: "FAST MOVING", category: "Premix & Additives", defaultInclusionPct: 0.2, defaultStockKg: 120.0, defaultReorderKg: 40.0, defaultLeadDays: 4, defaultSupplier: "Micro • Fast Moving" },
  { idHint: "raw_pantutti_premix", name: "PanTutti Fruitti (PREMIX)", sector: "MICRO_FAST", movementTag: "FAST MOVING", category: "Premix & Additives", defaultInclusionPct: 0.2, defaultStockKg: 110.0, defaultReorderKg: 40.0, defaultLeadDays: 4, defaultSupplier: "Micro • Fast Moving" },
  { idHint: "raw_aspartame", name: "Aspartame", sector: "MICRO_FAST", movementTag: "FAST MOVING", category: "Premix & Additives", defaultInclusionPct: 0.1, defaultStockKg: 50.0, defaultReorderKg: 20.0, defaultLeadDays: 5, defaultSupplier: "Micro • Fast Moving" },
  { idHint: "raw_edul_codex_premix", name: "Edul Codex (PREMIX)", sector: "MICRO_FAST", movementTag: "FAST MOVING", category: "Premix & Additives", defaultInclusionPct: 0.2, defaultStockKg: 100.0, defaultReorderKg: 35.0, defaultLeadDays: 4, defaultSupplier: "Micro • Fast Moving" },
  { idHint: "raw_luprosil_salt", name: "Luprosil Salt", sector: "MICRO_FAST", movementTag: "FAST MOVING", category: "Premix & Additives", defaultInclusionPct: 0.2, defaultStockKg: 150.0, defaultReorderKg: 50.0, defaultLeadDays: 4, defaultSupplier: "Micro • Fast Moving" },
  { idHint: "raw_fixar_s", name: "Fixar S", sector: "MICRO_FAST", movementTag: "FAST MOVING", category: "Premix & Additives", defaultInclusionPct: 0.2, defaultStockKg: 160.0, defaultReorderKg: 50.0, defaultLeadDays: 4, defaultSupplier: "Micro • Fast Moving" },
  { idHint: "raw_choline_chloride", name: "Choline Chloride", sector: "MICRO_FAST", movementTag: "FAST MOVING", category: "Premix & Additives", defaultInclusionPct: 0.3, defaultStockKg: 220.0, defaultReorderKg: 75.0, defaultLeadDays: 5, defaultSupplier: "Micro • Fast Moving" },

  // Micro Slow Moving (17)
  { idHint: "raw_iodized_salt", name: "Iodized Salt", sector: "MICRO_SLOW", movementTag: "SLOW MOVING", category: "Minerals & Salt", defaultInclusionPct: 0.4, defaultStockKg: 350.0, defaultReorderKg: 100.0, defaultLeadDays: 3, defaultSupplier: "Micro • Slow Moving" },
  { idHint: "raw_ctc_15", name: "CTC 15% (Chlortetracycline Hydrochloride)", sector: "MICRO_SLOW", movementTag: "SLOW MOVING", category: "Premix & Additives", defaultInclusionPct: 0.2, defaultStockKg: 100.0, defaultReorderKg: 35.0, defaultLeadDays: 6, defaultSupplier: "Micro • Slow Moving" },
  { idHint: "raw_phytase", name: "Phytase", sector: "MICRO_SLOW", movementTag: "SLOW MOVING", category: "Premix & Additives", defaultInclusionPct: 0.1, defaultStockKg: 75.0, defaultReorderKg: 25.0, defaultLeadDays: 6, defaultSupplier: "Micro • Slow Moving" },
  { idHint: "raw_swine_vitamin_1", name: "Swine Vitamin 1", sector: "MICRO_SLOW", movementTag: "SLOW MOVING", category: "Premix & Additives", defaultInclusionPct: 0.3, defaultStockKg: 180.0, defaultReorderKg: 60.0, defaultLeadDays: 5, defaultSupplier: "Micro • Slow Moving" },
  { idHint: "raw_ronozyme_gt", name: "Ronozyme Multigrain GT", sector: "MICRO_SLOW", movementTag: "SLOW MOVING", category: "Premix & Additives", defaultInclusionPct: 0.1, defaultStockKg: 60.0, defaultReorderKg: 20.0, defaultLeadDays: 7, defaultSupplier: "Micro • Slow Moving" },
  { idHint: "raw_prymox", name: "Prymox", sector: "MICRO_SLOW", movementTag: "SLOW MOVING", category: "Premix & Additives", defaultInclusionPct: 0.1, defaultStockKg: 60.0, defaultReorderKg: 20.0, defaultLeadDays: 6, defaultSupplier: "Micro • Slow Moving" },
  { idHint: "raw_primeat_max", name: "Primeat Max", sector: "MICRO_SLOW", movementTag: "SLOW MOVING", category: "Premix & Additives", defaultInclusionPct: 0.2, defaultStockKg: 90.0, defaultReorderKg: 30.0, defaultLeadDays: 6, defaultSupplier: "Micro • Slow Moving" },
  { idHint: "raw_swine_mineral_1", name: "Swine Mineral 1 (Premix)", sector: "MICRO_SLOW", movementTag: "SLOW MOVING", category: "Minerals & Salt", defaultInclusionPct: 0.3, defaultStockKg: 200.0, defaultReorderKg: 60.0, defaultLeadDays: 5, defaultSupplier: "Micro • Slow Moving" },
  { idHint: "raw_mdcp", name: "MDCP/MonodiCalcium Phosphate", sector: "MICRO_SLOW", movementTag: "SLOW MOVING", category: "Minerals & Salt", defaultInclusionPct: 1.2, defaultStockKg: 550.0, defaultReorderKg: 200.0, defaultLeadDays: 5, defaultSupplier: "Micro • Slow Moving" },
  { idHint: "raw_dl_methionine", name: "DL-Methionine", sector: "MICRO_SLOW", movementTag: "SLOW MOVING", category: "Premix & Additives", defaultInclusionPct: 0.2, defaultStockKg: 140.0, defaultReorderKg: 50.0, defaultLeadDays: 6, defaultSupplier: "Micro • Slow Moving" },
  { idHint: "raw_l_threonine", name: "L-Threonine", sector: "MICRO_SLOW", movementTag: "SLOW MOVING", category: "Premix & Additives", defaultInclusionPct: 0.2, defaultStockKg: 120.0, defaultReorderKg: 40.0, defaultLeadDays: 6, defaultSupplier: "Micro • Slow Moving" },
  { idHint: "raw_copper_sulphate", name: "Copper Sulphate Pentahydrate", sector: "MICRO_SLOW", movementTag: "SLOW MOVING", category: "Minerals & Salt", defaultInclusionPct: 0.1, defaultStockKg: 80.0, defaultReorderKg: 25.0, defaultLeadDays: 6, defaultSupplier: "Micro • Slow Moving" },
  { idHint: "raw_l_lysine_hci", name: "L-Lysine HCI", sector: "MICRO_SLOW", movementTag: "SLOW MOVING", category: "Premix & Additives", defaultInclusionPct: 0.3, defaultStockKg: 180.0, defaultReorderKg: 60.0, defaultLeadDays: 6, defaultSupplier: "Micro • Slow Moving" },
  { idHint: "raw_tiamulin", name: "Tiamulin", sector: "MICRO_SLOW", movementTag: "SLOW MOVING", category: "Premix & Additives", defaultInclusionPct: 0.1, defaultStockKg: 45.0, defaultReorderKg: 15.0, defaultLeadDays: 7, defaultSupplier: "Micro • Slow Moving" },
  { idHint: "raw_strepsol_pure", name: "Strepsol / Meat Enhancer (Pure)", sector: "MICRO_SLOW", movementTag: "SLOW MOVING", category: "Premix & Additives", defaultInclusionPct: 0.1, defaultStockKg: 50.0, defaultReorderKg: 15.0, defaultLeadDays: 6, defaultSupplier: "Micro • Slow Moving" },
  { idHint: "raw_strepsol_premixed", name: "Strepsol / Meat Enhancer (Premixed)", sector: "MICRO_SLOW", movementTag: "SLOW MOVING", category: "Premix & Additives", defaultInclusionPct: 0.2, defaultStockKg: 95.0, defaultReorderKg: 30.0, defaultLeadDays: 5, defaultSupplier: "Micro • Slow Moving" },
  { idHint: "raw_bel_feed", name: "Bel Feed", sector: "MICRO_SLOW", movementTag: "SLOW MOVING", category: "Premix & Additives", defaultInclusionPct: 0.2, defaultStockKg: 100.0, defaultReorderKg: 30.0, defaultLeadDays: 6, defaultSupplier: "Micro • Slow Moving" },

  // Micro Non-Moving / Trial (15)
  { idHint: "raw_pantutti_pure", name: "PanTutti Fruitti Pure", sector: "MICRO_NON_MOVING", movementTag: "TRIAL", category: "Premix & Additives", defaultInclusionPct: 0.1, defaultStockKg: 25.0, defaultReorderKg: 10.0, defaultLeadDays: 7, defaultSupplier: "Micro • Trial" },
  { idHint: "raw_aspartame_premix", name: "Aspartame (PREMIX)", sector: "MICRO_NON_MOVING", movementTag: "NEW MICROMIX", category: "Premix & Additives", defaultInclusionPct: 0.2, defaultStockKg: 40.0, defaultReorderKg: 15.0, defaultLeadDays: 5, defaultSupplier: "Micro • New Micromix" },
  { idHint: "raw_edul_codex_pure", name: "Edul Codex (PURE)", sector: "MICRO_NON_MOVING", movementTag: "NEW MICROMIX", category: "Premix & Additives", defaultInclusionPct: 0.1, defaultStockKg: 25.0, defaultReorderKg: 10.0, defaultLeadDays: 6, defaultSupplier: "Micro • New Micromix" },
  { idHint: "raw_ferna_strawberry_red", name: "Ferna Strawberry Red", sector: "MICRO_NON_MOVING", movementTag: "NON MOVING", category: "Premix & Additives", defaultInclusionPct: 0.1, defaultStockKg: 30.0, defaultReorderKg: 10.0, defaultLeadDays: 7, defaultSupplier: "Micro • Non-Moving" },
  { idHint: "raw_l_thryptophan", name: "L-Thryptophan", sector: "MICRO_NON_MOVING", movementTag: "NON MOVING", category: "Premix & Additives", defaultInclusionPct: 0.1, defaultStockKg: 35.0, defaultReorderKg: 10.0, defaultLeadDays: 7, defaultSupplier: "Micro • Non-Moving" },
  { idHint: "raw_sodium_bicarbonate", name: "Sodium Bicarbonate", sector: "MICRO_NON_MOVING", movementTag: "NON MOVING", category: "Minerals & Salt", defaultInclusionPct: 0.2, defaultStockKg: 100.0, defaultReorderKg: 30.0, defaultLeadDays: 5, defaultSupplier: "Micro • Non-Moving" },
  { idHint: "raw_turbo_grow", name: "Turbo Grow", sector: "MICRO_NON_MOVING", movementTag: "NON MOVING", category: "Premix & Additives", defaultInclusionPct: 0.1, defaultStockKg: 40.0, defaultReorderKg: 15.0, defaultLeadDays: 7, defaultSupplier: "Micro • Non-Moving" },
  { idHint: "raw_nutrigano", name: "Nutrigano", sector: "MICRO_NON_MOVING", movementTag: "NON MOVING", category: "Premix & Additives", defaultInclusionPct: 0.1, defaultStockKg: 35.0, defaultReorderKg: 10.0, defaultLeadDays: 7, defaultSupplier: "Micro • Non-Moving" },
  { idHint: "raw_klinofeed", name: "Klinofeed", sector: "MICRO_NON_MOVING", movementTag: "NON MOVING", category: "Premix & Additives", defaultInclusionPct: 0.2, defaultStockKg: 80.0, defaultReorderKg: 25.0, defaultLeadDays: 6, defaultSupplier: "Micro • Non-Moving" },
  { idHint: "raw_pellex", name: "Pellex", sector: "MICRO_NON_MOVING", movementTag: "NON MOVING", category: "Premix & Additives", defaultInclusionPct: 0.2, defaultStockKg: 75.0, defaultReorderKg: 25.0, defaultLeadDays: 6, defaultSupplier: "Micro • Non-Moving" },
  { idHint: "raw_ng_fruit", name: "NG Fruit", sector: "MICRO_NON_MOVING", movementTag: "NON MOVING", category: "Premix & Additives", defaultInclusionPct: 0.1, defaultStockKg: 30.0, defaultReorderKg: 10.0, defaultLeadDays: 7, defaultSupplier: "Micro • Non-Moving" },
  { idHint: "raw_quantum_blue", name: "Quantum Blue", sector: "MICRO_NON_MOVING", movementTag: "NON MOVING", category: "Premix & Additives", defaultInclusionPct: 0.1, defaultStockKg: 30.0, defaultReorderKg: 10.0, defaultLeadDays: 7, defaultSupplier: "Micro • Non-Moving" },
  { idHint: "raw_swine_phytase", name: "Swine Phytase", sector: "MICRO_NON_MOVING", movementTag: "NON MOVING", category: "Premix & Additives", defaultInclusionPct: 0.1, defaultStockKg: 40.0, defaultReorderKg: 15.0, defaultLeadDays: 7, defaultSupplier: "Micro • Non-Moving" },
  { idHint: "raw_aqua_lyso", name: "Aqua Lyso", sector: "MICRO_NON_MOVING", movementTag: "NON MOVING", category: "Premix & Additives", defaultInclusionPct: 0.1, defaultStockKg: 25.0, defaultReorderKg: 10.0, defaultLeadDays: 7, defaultSupplier: "Micro • Non-Moving" },
  { idHint: "raw_carophyl_yellow", name: "Carophyl Yellow", sector: "MICRO_NON_MOVING", movementTag: "NON MOVING", category: "Premix & Additives", defaultInclusionPct: 0.1, defaultStockKg: 25.0, defaultReorderKg: 10.0, defaultLeadDays: 7, defaultSupplier: "Micro • Non-Moving" }
];

const CATALOG_BY_LOWER = new Map();
for (const spec of FEED_MATERIAL_CATALOG) {
  CATALOG_BY_LOWER.set(spec.name.trim().toLowerCase(), spec);
  if (spec.idHint === "raw_l_lysine_hci") CATALOG_BY_LOWER.set("l-lysine hcl", spec);
  if (spec.idHint === "raw_l_thryptophan") CATALOG_BY_LOWER.set("l-tryptophan", spec);
  if (spec.idHint === "raw_mollases") {
    CATALOG_BY_LOWER.set("molasses", spec);
    CATALOG_BY_LOWER.set("cane molasses", spec);
  }
}

export function inferMaterialSector(material) {
  const nameKey = String(material?.name || "").trim().toLowerCase();
  const matched = CATALOG_BY_LOWER.get(nameKey);
  if (matched) return MATERIAL_SECTORS[matched.sector];
  const sup = String(material?.supplierName || "").toLowerCase();
  if (sup.includes("fast moving")) return MATERIAL_SECTORS.MICRO_FAST;
  if (sup.includes("slow moving")) return MATERIAL_SECTORS.MICRO_SLOW;
  if (sup.includes("non-moving") || sup.includes("non moving") || sup.includes("trial") || sup.includes("new micromix")) {
    return MATERIAL_SECTORS.MICRO_NON_MOVING;
  }
  if (sup.includes("macro")) return MATERIAL_SECTORS.MACRO;
  const cat = String(material?.category || "");
  if (cat === "Minerals & Salt" || cat === "Premix & Additives") return MATERIAL_SECTORS.MICRO_SLOW;
  return MATERIAL_SECTORS.MACRO;
}

export function inferMovementTag(material) {
  const nameKey = String(material?.name || "").trim().toLowerCase();
  const matched = CATALOG_BY_LOWER.get(nameKey);
  if (matched) return matched.movementTag;
  const sup = String(material?.supplierName || "").toLowerCase();
  if (sup.includes("fast moving")) return "FAST MOVING";
  if (sup.includes("slow moving")) return "SLOW MOVING";
  if (sup.includes("trial")) return "TRIAL";
  if (sup.includes("new micromix")) return "NEW MICROMIX";
  if (sup.includes("non-moving") || sup.includes("non moving")) return "NON MOVING";
  const sec = inferMaterialSector(material);
  if (sec.id === "MACRO") return "MACRO";
  if (sec.id === "MICRO_FAST") return "FAST MOVING";
  if (sec.id === "MICRO_NON_MOVING") return "NON MOVING";
  return "SLOW MOVING";
}

export function missingCatalogEntities(existingMaterials = [], isMicroFilter = null) {
  const existingSet = new Set(
    existingMaterials.map((m) => String(m.name || "").trim().toLowerCase()).filter(Boolean)
  );
  return FEED_MATERIAL_CATALOG.filter((spec) => {
    const isMicro = spec.sector !== "MACRO";
    if (isMicroFilter !== null && isMicro !== isMicroFilter) return false;
    return !existingSet.has(spec.name.trim().toLowerCase());
  });
}

export const WORKSPACE_ROLES = {
  Admin: {
    label: "Admin",
    canWriteOperations: true,
    canManageTeam: true,
    canResetWorkspace: true,
    canDeleteSkus: true,
    canEditSettings: true,
    summary: "Full operational control, SKU management, resets, backups, and team role management."
  },
  Operator: {
    label: "Operator",
    canWriteOperations: true,
    canManageTeam: false,
    canResetWorkspace: false,
    canDeleteSkus: false,
    canEditSettings: false,
    summary: "Create and update orders, dispatches, schedules, batches, reprocess items, and stock."
  },
  Viewer: {
    label: "Viewer",
    canWriteOperations: false,
    canManageTeam: false,
    canResetWorkspace: false,
    canDeleteSkus: false,
    canEditSettings: false,
    summary: "Read-only access to dashboards, inventory levels, reports, and activity history."
  }
};

export const WORKSPACE_ROLE_OPTIONS = ["Viewer", "Operator", "Admin"];

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

export function isValidIsoDateStr(value) {
  return /^\d{4}-\d{2}-\d{2}$/.test(String(value || "").trim());
}

export function addDaysIsoDate(isoDate, deltaDays) {
  const d = new Date(`${isoDate}T00:00:00Z`);
  if (Number.isNaN(d.getTime())) return todayIsoDate();
  d.setUTCDate(d.getUTCDate() + deltaDays);
  return d.toISOString().slice(0, 10);
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
  if (dispatch.returnConfirmed && Number(dispatch.returnedWeightKg || 0) > 0) {
    return Math.max(0, total - Number(dispatch.returnedWeightKg || 0));
  }
  return total;
}

export function resolvedDispatchStatus(dispatch) {
  if (!dispatch) return "Delivered";
  if (dispatch.isHeld || String(dispatch.dispatchStatus || "").toLowerCase() === "held") return "Held";
  if ((dispatch.hasReturn && dispatch.returnConfirmed) || String(dispatch.dispatchStatus || "").toLowerCase() === "returned") {
    return "Returned";
  }
  return "Delivered";
}

export function computeSkuSummaries({
  skus = [],
  batches = [],
  dispatches = [],
  orders = [],
  reprocessItems = [],
  lookbackDays = 14,
  globalSafetyBufferPct = null
}) {
  const safeLookback = Math.max(1, Number(lookbackDays || 14));
  const allDates = [
    ...dispatches.map((d) => d.date).filter(isValidIsoDateStr),
    ...batches.map((b) => b.date).filter(isValidIsoDateStr)
  ].sort();
  const refDateStr = allDates.length ? allDates[allDates.length - 1] : todayIsoDate();
  const lookbackStartStr = addDaysIsoDate(refDateStr, -(safeLookback - 1));

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

    const isDamaged = (r) => !r.flaggedToInventory && String(r.status || "").toLowerCase() === "damaged";
    const isPending = (r) => !r.flaggedToInventory && !isDamaged(r);
    const effReprocessedKg = (r) =>
      !r.flaggedToInventory ? 0 : Number(r.reprocessedWeightKg || 0) > 0 ? Number(r.reprocessedWeightKg) : Number(r.returnedWeightKg || 0);
    const effReprocessedBags = (r) =>
      !r.flaggedToInventory ? 0 : Number(r.reprocessedBags || 0) > 0 ? Number(r.reprocessedBags) : Number(r.returnedBags || 0);

    const pendingReprocessKg = skuReprocess.filter(isPending).reduce((s, r) => s + Number(r.returnedWeightKg || 0), 0);
    const pendingReprocessBags = skuReprocess.filter(isPending).reduce((s, r) => s + Number(r.returnedBags || 0), 0);
    const pendingFastActionCount = skuReprocess.filter((r) => isPending(r) && r.fastActionNeeded).length;
    const damagedReprocessKg = skuReprocess.filter(isDamaged).reduce((s, r) => s + Number(r.returnedWeightKg || 0), 0);
    const reprocessedKg = skuReprocess.filter((r) => r.flaggedToInventory).reduce((s, r) => s + effReprocessedKg(r), 0);
    const reprocessedBags = skuReprocess.filter((r) => r.flaggedToInventory).reduce((s, r) => s + effReprocessedBags(r), 0);

    const nonRestoredLinkedQuarantineKg = skuReprocess
      .filter((r) => !r.flaggedToInventory && String(r.sourceDispatchId || "").trim() !== "")
      .reduce((sum, r) => sum + Number(r.returnedWeightKg || 0), 0);
    const flaggedLinkedDeltaKg = skuReprocess
      .filter((r) => r.flaggedToInventory && String(r.sourceDispatchId || "").trim() !== "")
      .reduce((sum, r) => sum + (effReprocessedKg(r) - Number(r.returnedWeightKg || 0)), 0);
    const flaggedManualAddKg = skuReprocess
      .filter((r) => r.flaggedToInventory && String(r.sourceDispatchId || "").trim() === "")
      .reduce((sum, r) => sum + effReprocessedKg(r), 0);

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
    const bufferPct = globalSafetyBufferPct !== null && globalSafetyBufferPct !== undefined
      ? Number(globalSafetyBufferPct)
      : Number(sku.safetyBufferPct || 0);
    const demandBasis = Math.max(openOrdersKg, avgDailyDispatchKg * 3);
    const safetyBufferKg = demandBasis * (bufferPct / 100);
    const netProductionNeededKg = Math.max(0, openOrdersKg + safetyBufferKg - availableStockKg);
    const suggestedBags = netProductionNeededKg > 0 ? Math.ceil(netProductionNeededKg / bagWeightKg) : 0;

    const totalExpected = skuBatches.reduce((sum, b) => sum + Number(b.expectedWeightKg || 0), 0);
    const avgRecoveryPct = totalExpected > 0 ? (recoveredKg / totalExpected) * 100 : 100;

    let status = "Healthy";
    if (availableStockKg <= 0 || availableStockKg < openOrdersKg || (avgDailyDispatchKg > 0 && daysOfCover < 3)) {
      status = "Critical";
    } else if (netProductionNeededKg > 0 || (avgDailyDispatchKg > 0 && daysOfCover < 7)) {
      status = "Low Watch";
    }

    let recommendedPriority = null;
    if (status === "Critical") recommendedPriority = "Urgent";
    else if (openOrdersKg > availableStockKg) recommendedPriority = "Order-Driven";
    else if (netProductionNeededKg > 0) recommendedPriority = "Buffer Top-Up";

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
      safetyBufferKg,
      netProductionNeededKg,
      suggestedBags,
      avgRecoveryPct,
      recommendedPriority,
      status,
      pendingReprocessKg,
      pendingReprocessBags,
      pendingFastActionCount,
      reprocessedKg,
      reprocessedBags,
      damagedReprocessKg
    };
  }).sort((a, b) => {
    const rank = (p) => (p === "Urgent" ? 0 : p === "Order-Driven" ? 1 : p === "Buffer Top-Up" ? 2 : 99);
    const rDiff = rank(a.recommendedPriority) - rank(b.recommendedPriority);
    if (rDiff !== 0) return rDiff;
    if (b.netProductionNeededKg !== a.netProductionNeededKg) return b.netProductionNeededKg - a.netProductionNeededKg;
    return String(a.sku.productName || "").localeCompare(String(b.sku.productName || ""));
  });
}

export function computeKpis({ summaries = [], orders = [], dispatches = [], batches = [], rawMaterials = [], schedules = [] }) {
  const totalStockKg = summaries.reduce((sum, s) => sum + Math.max(0, s.availableStockKg), 0);
  const totalBags = summaries.reduce((sum, s) => sum + Math.max(0, s.availableBags), 0);
  const criticalSkus = summaries.filter((s) => s.status === "Critical").length;
  const openOrders = orders.filter((o) => o.status === "Open" || o.status === "Partially Dispatched");
  const openOrderKg = openOrders.reduce(
    (sum, o) => sum + Math.max(0, Number(o.totalWeightKg || 0) - Number(o.dispatchedWeightKg || 0)),
    0
  );
  const totalDispatchedKg = dispatches.reduce((sum, d) => sum + effectiveDispatchWeightKg(d), 0);
  const totalProducedKg = batches.reduce((sum, b) => sum + Number(b.actualRecoveredKg || 0), 0);
  const totalExpectedKg = batches.reduce((sum, b) => sum + Number(b.expectedWeightKg || 0), 0);
  const avgRecoveryPct = totalExpectedKg > 0 ? (totalProducedKg / totalExpectedKg) * 100 : 100;
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
    avgRecoveryPct,
    lowRawMaterials,
    activeSchedules
  };
}

export function computeRawMaterialForecasts({
  materials = [],
  batches = [],
  schedules = [],
  boms = [],
  lookbackDays = 14,
  demandMultiplier = 1.0,
  includeScheduledDemand = false,
  leadTimeOverrideDays = null
}) {
  if (!materials.length) return [];
  const safeLookback = Math.min(90, Math.max(1, Number(lookbackDays || 14)));
  const safeMultiplier = Math.min(3.0, Math.max(0.25, Number(demandMultiplier || 1.0)));
  const todayStr = todayIsoDate();
  const batchDates = batches.map((b) => b.date).filter(isValidIsoDateStr).sort();
  const refEndStr = batchDates.length ? batchDates[batchDates.length - 1] : todayStr;
  const lookbackStartStr = addDaysIsoDate(refEndStr, -(safeLookback - 1));

  const recentBatches = batches.filter((b) => isValidIsoDateStr(b.date) && b.date >= lookbackStartStr && b.date <= refEndStr);
  const effectiveBatches = recentBatches.length ? recentBatches : batches;
  let effectiveSpanDays = safeLookback;
  if (!recentBatches.length && batchDates.length) {
    const startMs = new Date(`${batchDates[0]}T00:00:00Z`).getTime();
    const endMs = new Date(`${batchDates[batchDates.length - 1]}T00:00:00Z`).getTime();
    effectiveSpanDays = Math.max(1, Math.round((endMs - startMs) / 86400000) + 1);
  }

  const bomByProdLower = new Map();
  for (const bom of boms) {
    bomByProdLower.set(String(bom.productName || "").trim().toLowerCase(), bom);
  }
  const activeSchedules = includeScheduledDemand
    ? schedules.filter((s) => s.status === "Scheduled" || s.status === "In Progress")
    : [];

  return materials
    .map((mat) => {
      const fallbackShare = Math.min(1, Math.max(0.0001, Number(mat.usageSharePct || 10) / 100));
      const shareForProduct = (productName) => {
        const bom = bomByProdLower.get(String(productName || "").trim().toLowerCase());
        if (!bom || !Array.isArray(bom.lines) || !bom.lines.length) return fallbackShare;
        const matchedLine = bom.lines.find(
          (line) =>
            (mat.id && line.materialId === mat.id) ||
            String(line.materialName || "").trim().toLowerCase() === String(mat.name || "").trim().toLowerCase()
        );
        return matchedLine ? Math.min(1, Math.max(0, Number(matchedLine.inclusionPct || 0) / 100)) : 0;
      };

      const recentConsumedKg = effectiveBatches.reduce((sum, b) => {
        const feedKg = Math.max(0, Math.max(Number(b.expectedWeightKg || 0), Number(b.actualRecoveredKg || 0)));
        return sum + feedKg * shareForProduct(b.productName);
      }, 0);

      const historicalAvgDailyKg = effectiveSpanDays > 0 ? recentConsumedKg / effectiveSpanDays : 0;
      const scheduledDemandKg = activeSchedules.reduce(
        (sum, s) => sum + Math.max(0, Number(s.expectedWeightKg || 0)) * shareForProduct(s.productName),
        0
      );
      const scheduledDailyAddKg = includeScheduledDemand && scheduledDemandKg > 0 ? scheduledDemandKg / 7 : 0;
      const simulatedAvgDailyKg = historicalAvgDailyKg * safeMultiplier + scheduledDailyAddKg;

      const effectiveLead = Math.min(60, Math.max(1, Number(leadTimeOverrideDays || mat.leadTimeDays || 3)));
      const totalWatchDays = effectiveLead + Math.max(0, Number(mat.safetyDays || 0));
      const currentStock = Number(mat.currentStockKg || 0);
      const reorderThreshold = Number(mat.reorderThresholdKg || 0);
      const netAfterSched = includeScheduledDemand ? Math.max(0, currentStock - scheduledDemandKg) : Math.max(0, currentStock);

      const daysUntilRunOut = simulatedAvgDailyKg > 0.01 ? Math.max(0, currentStock) / simulatedAvgDailyKg : Infinity;
      const reorderTriggerKg = Math.max(reorderThreshold, simulatedAvgDailyKg * totalWatchDays);
      const targetRestockKg = Math.max(reorderThreshold * 2, simulatedAvgDailyKg * (14 + effectiveLead));
      const recommendedRestockKg = Math.max(0, targetRestockKg - currentStock);

      const isOutOrMin = currentStock <= 0 || currentStock <= reorderThreshold;
      const isCritRunway = Number.isFinite(daysUntilRunOut) && daysUntilRunOut <= effectiveLead;
      const isCritSched = includeScheduledDemand && scheduledDemandKg > currentStock;
      const isWatchRunway = Number.isFinite(daysUntilRunOut) && daysUntilRunOut <= totalWatchDays;
      const isWatchBuffer = currentStock <= reorderTriggerKg * 1.15 && simulatedAvgDailyKg > 0.01;

      let status = "Healthy";
      if (isOutOrMin || isCritRunway || isCritSched) status = "Order Now";
      else if (isWatchRunway || isWatchBuffer || netAfterSched <= reorderThreshold) status = "Running Low";

      let projectedRunOutLabel = "No recent usage";
      if (currentStock <= 0) projectedRunOutLabel = "Out of stock";
      else if (Number.isFinite(daysUntilRunOut)) {
        projectedRunOutLabel = daysUntilRunOut > 180 ? "> 180 days" : `${daysUntilRunOut.toFixed(1)}d left`;
      }

      return {
        material: mat,
        sector: inferMaterialSector(mat),
        movementTag: inferMovementTag(mat),
        historicalAvgDailyKg,
        simulatedAvgDailyKg,
        recentConsumedKg,
        scheduledDemandKg,
        daysUntilRunOut,
        projectedRunOutLabel,
        reorderTriggerKg,
        recommendedRestockKg,
        status
      };
    })
    .sort((a, b) => {
      const rank = (s) => (s === "Order Now" ? 0 : s === "Running Low" ? 1 : 2);
      const r = rank(a.status) - rank(b.status);
      if (r !== 0) return r;
      const da = Number.isFinite(a.daysUntilRunOut) ? a.daysUntilRunOut : Number.MAX_VALUE;
      const db = Number.isFinite(b.daysUntilRunOut) ? b.daysUntilRunOut : Number.MAX_VALUE;
      if (da !== db) return da - db;
      return String(a.material.name || "").localeCompare(String(b.material.name || ""));
    });
}

export function computeDailyLogs({ skus = [], batches = [], dispatches = [], orders = [], todayIso = todayIsoDate() }) {
  const dateSet = new Set();
  if (isValidIsoDateStr(todayIso)) dateSet.add(todayIso);
  for (const b of batches) if (isValidIsoDateStr(b.date)) dateSet.add(b.date);
  for (const d of dispatches) if (isValidIsoDateStr(d.date)) dateSet.add(d.date);
  for (const o of orders) {
    if (isValidIsoDateStr(o.orderDate)) dateSet.add(o.orderDate);
    if (isValidIsoDateStr(o.dueDate)) dateSet.add(o.dueDate);
  }
  const sortedDates = Array.from(dateSet).sort().reverse();
  const activeOrders = orders.filter((o) => o.status === "Open" || o.status === "Partially Dispatched");

  return sortedDates.map((day) => {
    const isToday = day === todayIso;
    const skuRows = skus.map((sku) => {
      const key = String(sku.productName || "").trim().toLowerCase();
      const pBatches = batches.filter((b) => String(b.productName || "").trim().toLowerCase() === key);
      const pDisp = dispatches.filter((d) => String(d.productName || "").trim().toLowerCase() === key);
      const cumRec = pBatches.filter((b) => isValidIsoDateStr(b.date) && b.date <= day).reduce((s, b) => s + Number(b.actualRecoveredKg || 0), 0);
      const cumDisp = pDisp.filter((d) => isValidIsoDateStr(d.date) && d.date <= day).reduce((s, d) => s + effectiveDispatchWeightKg(d), 0);
      const closingStockKg = Number(sku.openingStockKg || 0) + cumRec - cumDisp;
      const bagWt = Math.max(0.1, Number(sku.standardBagWeightKg || 25));
      const dayBatches = pBatches.filter((b) => b.date === day);
      const dayDisp = pDisp.filter((d) => d.date === day);
      return {
        sku,
        closingStockKg,
        closingBags: closingStockKg / bagWt,
        producedDayKg: dayBatches.reduce((s, b) => s + Number(b.actualRecoveredKg || 0), 0),
        producedDayBags: dayBatches.reduce((s, b) => s + Number(b.bagsProduced || 0), 0),
        dispatchedDayKg: dayDisp.reduce((s, d) => s + effectiveDispatchWeightKg(d), 0),
        dispatchedDayBags: dayDisp.reduce((s, d) => s + Number(d.bagsDispatched || 0), 0),
        openOrdersSnapshotKg: activeOrders
          .filter((o) => String(o.productName || "").trim().toLowerCase() === key && (isToday || o.orderDate <= day))
          .reduce((s, o) => s + Math.max(0, Number(o.totalWeightKg || 0) - Number(o.dispatchedWeightKg || 0)), 0)
      };
    });

    const dayOrders = orders.filter((o) => o.orderDate === day || o.dueDate === day);
    return {
      date: day,
      isToday,
      skuRows,
      dayOrders,
      totalClosingStockKg: skuRows.reduce((s, r) => s + Math.max(0, r.closingStockKg), 0),
      totalProducedDayKg: skuRows.reduce((s, r) => s + r.producedDayKg, 0),
      totalDispatchedDayKg: skuRows.reduce((s, r) => s + r.dispatchedDayKg, 0)
    };
  });
}

export function buildPlantReport({
  startDate,
  endDate,
  sectionFilter = "ALL",
  includedSections = REPORT_SECTION_OPTIONS,
  skus = [],
  batches = [],
  dispatches = [],
  orders = [],
  schedules = [],
  summaries = []
}) {
  const sDate = startDate <= endDate ? startDate : endDate;
  const eDate = startDate <= endDate ? endDate : startDate;
  const inRange = (d) => isValidIsoDateStr(d) && d >= sDate && d <= eDate;
  const secByProd = new Map(skus.map((s) => [String(s.productName || "").trim().toLowerCase(), s.section]));

  const filteredBatches = batches.filter((b) => inRange(b.date) && (sectionFilter === "ALL" || b.section === sectionFilter));
  const filteredDispatches = dispatches.filter((d) => {
    const sec = d.sourcePlant || secByProd.get(String(d.productName || "").trim().toLowerCase());
    return inRange(d.date) && (sectionFilter === "ALL" || sec === sectionFilter);
  });
  const filteredSchedules = schedules.filter((s) => inRange(s.scheduledDate) && (sectionFilter === "ALL" || s.section === sectionFilter));
  const filteredOrders = orders.filter((o) => {
    const sec = secByProd.get(String(o.productName || "").trim().toLowerCase());
    return (inRange(o.orderDate) || inRange(o.dueDate)) && (sectionFilter === "ALL" || sec === sectionFilter);
  });
  const filteredSummaries = summaries.filter((s) => sectionFilter === "ALL" || s.sku.section === sectionFilter);

  const sectionSummaries = PLANT_SECTIONS.filter((sec) => sectionFilter === "ALL" || sec === sectionFilter).map((sec) => {
    const sb = filteredBatches.filter((b) => b.section === sec);
    const ss = summaries.filter((s) => s.sku.section === sec);
    const exp = sb.reduce((sum, b) => sum + Number(b.expectedWeightKg || 0), 0);
    const rec = sb.reduce((sum, b) => sum + Number(b.actualRecoveredKg || 0), 0);
    return {
      section: sec,
      batches: sb,
      totalBags: sb.reduce((sum, b) => sum + Number(b.bagsProduced || 0), 0),
      totalExpectedKg: exp,
      totalRecoveredKg: rec,
      recoveryPct: exp > 0 ? (rec / exp) * 100 : 100,
      skuCount: ss.length,
      totalAvailableStockKg: ss.reduce((sum, s) => sum + Math.max(0, s.availableStockKg), 0),
      totalAvailableBags: ss.reduce((sum, s) => sum + Math.max(0, s.availableBags), 0)
    };
  });

  const activeOpenOrders = orders.filter((o) => {
    const sec = secByProd.get(String(o.productName || "").trim().toLowerCase());
    return (o.status === "Open" || o.status === "Partially Dispatched") && (sectionFilter === "ALL" || sec === sectionFilter);
  });

  const totalExpectedKg = filteredBatches.reduce((sum, b) => sum + Number(b.expectedWeightKg || 0), 0);
  const totalRecoveredKg = filteredBatches.reduce((sum, b) => sum + Number(b.actualRecoveredKg || 0), 0);
  const activeSchedules = filteredSchedules.filter((s) => s.status === "Scheduled" || s.status === "In Progress");
  const scheduledQueueBags = activeSchedules.reduce((sum, s) => sum + Number(s.targetBags || 0), 0);
  const scheduledCompletedCount = filteredSchedules.filter((s) => s.status === "Completed").length;

  return {
    startDate: sDate,
    endDate: eDate,
    isDailyReport: sDate === eDate,
    sectionFilter,
    includedSections: includedSections.length ? includedSections : REPORT_SECTION_OPTIONS,
    sectionSummaries,
    filteredBatches,
    filteredDispatches,
    filteredSchedules,
    filteredOrders,
    filteredSummaries,
    activeOpenOrders,
    totalProducedBags: filteredBatches.reduce((sum, b) => sum + Number(b.bagsProduced || 0), 0),
    totalRecoveredKg,
    avgRecoveryPct: totalExpectedKg > 0 ? (totalRecoveredKg / totalExpectedKg) * 100 : 100,
    totalDispatchedKg: filteredDispatches.reduce((sum, d) => sum + effectiveDispatchWeightKg(d), 0),
    totalOpenOrdersKg: activeOpenOrders.reduce((sum, o) => sum + Math.max(0, Number(o.totalWeightKg || 0) - Number(o.dispatchedWeightKg || 0)), 0),
    totalAvailableStockKg: sectionSummaries.reduce((sum, s) => sum + s.totalAvailableStockKg, 0),
    totalAvailableBags: sectionSummaries.reduce((sum, s) => sum + s.totalAvailableBags, 0),
    scheduledActiveCount: activeSchedules.length,
    scheduledQueueBags,
    scheduledCompletedCount,
    deliveredCount: filteredDispatches.filter((d) => resolvedDispatchStatus(d) === "Delivered").length,
    returnedCount: filteredDispatches.filter((d) => resolvedDispatchStatus(d) === "Returned").length,
    heldCount: filteredDispatches.filter((d) => resolvedDispatchStatus(d) === "Held").length
  };
}

export function buildMessengerSummaryText(report) {
  const period = report.isDailyReport ? `Daily (${report.startDate})` : `${report.startDate} to ${report.endDate}`;
  const inc = new Set(report.includedSections);
  const scopeLabel = report.includedSections.length === REPORT_SECTION_OPTIONS.length
    ? "All Sections"
    : report.includedSections.join(", ");
  const lines = [
    "*10K AGRI FEEDS - BULACAN PLANT*",
    `*${report.isDailyReport ? "DAILY" : "PERIOD"} PLANT SUMMARY REPORT*`,
    `Period: ${period} | Plant: ${report.sectionFilter} | Scope: ${scopeLabel}`,
    "=============================="
  ];
  if (inc.has("Inventory")) {
    lines.push(`Available Stock: ${report.totalAvailableStockKg.toFixed(0)} kg (${report.totalAvailableBags.toFixed(1)} bags) across ${report.filteredSummaries.length} SKUs`);
  }
  if (inc.has("Recovery")) {
    lines.push(`Produced: ${report.totalProducedBags} bags | ${report.totalRecoveredKg.toFixed(0)} kg (${report.avgRecoveryPct.toFixed(2)}% recovery)`);
  }
  if (inc.has("Dispatches")) {
    lines.push(`Dispatched: ${report.totalDispatchedKg.toFixed(0)} kg net (${report.filteredDispatches.length} trips: ${report.deliveredCount} Del, ${report.returnedCount} Ret, ${report.heldCount} Held)`);
  }
  if (inc.has("Orders")) {
    lines.push(`Orders in Period: ${report.filteredOrders.length} | Active Open Orders: ${report.activeOpenOrders.length} (${report.totalOpenOrdersKg.toFixed(0)} kg remaining)`);
  }
  if (inc.has("Schedule")) {
    lines.push(`Scheduled Runs in Period: ${report.filteredSchedules.length} (${report.scheduledActiveCount || 0} active / ${report.scheduledQueueBags || 0} bags, ${report.scheduledCompletedCount || 0} completed)`);
  }
  if ((inc.has("Inventory") || inc.has("Recovery")) && report.sectionSummaries?.length) {
    lines.push("", "*By Plant Section:*");
    for (const s of report.sectionSummaries) {
      const parts = [];
      if (inc.has("Inventory")) parts.push(`Avail ${s.totalAvailableBags.toFixed(1)} b (${s.totalAvailableStockKg.toFixed(0)} kg)`);
      if (inc.has("Recovery")) parts.push(`Prod ${s.totalBags} b (${s.recoveryPct.toFixed(1)}%)`);
      lines.push(`• ${s.section}: ${parts.join(" | ")}`);
    }
  }
  if (inc.has("Dispatches") && report.filteredDispatches.length) {
    lines.push("", "*Dispatches:*");
    for (const d of report.filteredDispatches.slice(0, 8)) {
      lines.push(`• ${d.date} | ${d.drReference} | ${d.customer} | ${effectiveDispatchWeightKg(d).toFixed(0)} kg [${resolvedDispatchStatus(d)}]`);
    }
  }
  if (inc.has("Orders") && (report.filteredOrders.length || report.activeOpenOrders.length)) {
    lines.push("", "*Orders:*");
    const list = report.filteredOrders.length ? report.filteredOrders : report.activeOpenOrders;
    for (const o of list.slice(0, 8)) {
      const rem = Math.max(0, Number(o.totalWeightKg || 0) - Number(o.dispatchedWeightKg || 0));
      lines.push(`• ${o.orderCode} | ${o.customer} | ${o.productName} | Rem ${rem.toFixed(0)} kg [${o.status}]`);
    }
  }
  if (inc.has("Schedule") && report.filteredSchedules.length) {
    lines.push("", "*Schedule Runs:*");
    for (const sch of report.filteredSchedules.slice(0, 8)) {
      lines.push(`• ${sch.scheduledDate} | ${sch.section} | ${sch.productName} (${sch.targetBags}b) [${sch.status}]`);
    }
  }
  return lines.join("\n");
}

export function buildDetailedEmailReportText(report) {
  const period = report.isDailyReport ? `Daily (${report.startDate})` : `${report.startDate} to ${report.endDate}`;
  const inc = new Set(report.includedSections);
  const scopeLabel = report.includedSections.length === REPORT_SECTION_OPTIONS.length
    ? "All Sections"
    : report.includedSections.join(", ");
  const lines = [
    "10K AGRI FEEDS - BULACAN PLANT OPERATIONS REPORT",
    `Period: ${period}`,
    `Plant Filter: ${report.sectionFilter} | Included Sections: ${scopeLabel}`,
    "------------------------------------------------------------"
  ];
  if (inc.has("Inventory")) {
    lines.push(`Finished Stock Available: ${report.totalAvailableStockKg.toFixed(0)} kg (${report.totalAvailableBags.toFixed(1)} bags across ${report.filteredSummaries.length} SKUs)`);
  }
  if (inc.has("Recovery")) {
    lines.push(`Production Batches: ${report.filteredBatches.length} runs | ${report.totalProducedBags} bags | ${report.totalRecoveredKg.toFixed(0)} kg (${report.avgRecoveryPct.toFixed(2)}% recovery)`);
  }
  if (inc.has("Dispatches")) {
    lines.push(`Dispatched Deliveries: ${report.filteredDispatches.length} trips | ${report.totalDispatchedKg.toFixed(0)} kg net (${report.deliveredCount} Delivered, ${report.returnedCount} Returned, ${report.heldCount} Held)`);
  }
  if (inc.has("Orders")) {
    lines.push(`Orders in Period: ${report.filteredOrders.length} | Active Open Orders: ${report.activeOpenOrders.length} orders (${report.totalOpenOrdersKg.toFixed(0)} kg remaining)`);
  }
  if (inc.has("Schedule")) {
    lines.push(`Scheduled Runs: ${report.filteredSchedules.length} (${report.scheduledActiveCount || 0} active / ${report.scheduledQueueBags || 0} bags, ${report.scheduledCompletedCount || 0} completed)`);
  }

  if ((inc.has("Inventory") || inc.has("Recovery")) && report.sectionSummaries?.length) {
    lines.push("", "PLANT SECTION BREAKDOWN:");
    for (const s of report.sectionSummaries) {
      const parts = [];
      if (inc.has("Inventory")) parts.push(`Avail ${s.totalAvailableBags.toFixed(1)} b (${s.totalAvailableStockKg.toFixed(0)} kg)`);
      if (inc.has("Recovery")) parts.push(`Produced ${s.totalBags} b (${s.totalRecoveredKg.toFixed(0)} kg, ${s.recoveryPct.toFixed(1)}%)`);
      lines.push(`- ${s.section} (${s.skuCount} SKUs): ${parts.join(" | ")}`);
    }
  }

  if (inc.has("Inventory") && report.filteredSummaries.length) {
    lines.push("", "INVENTORY SKU BALANCE:");
    for (const row of report.filteredSummaries.slice(0, 30)) {
      lines.push(`- [${row.sku.section}] ${row.sku.productName}: Avail ${row.availableBags.toFixed(1)} b (${row.availableStockKg.toFixed(0)} kg) | OpenOrd ${row.openOrdersKg.toFixed(0)} kg | ${row.status}`);
    }
  }

  if (inc.has("Recovery") && report.filteredBatches.length) {
    lines.push("", "PRODUCTION RECOVERY BATCHES:");
    for (const b of report.filteredBatches.slice(0, 25)) {
      lines.push(`- ${b.date} | ${b.section} | ${b.productName} | ${b.bagsProduced} b | Rec ${Number(b.actualRecoveredKg || 0).toFixed(0)} kg (${Number(b.recoveryPct || 100).toFixed(1)}%)`);
    }
  }

  if (inc.has("Dispatches") && report.filteredDispatches.length) {
    lines.push("", "DISPATCHES IN PERIOD:");
    for (const d of report.filteredDispatches.slice(0, 25)) {
      lines.push(`- ${d.date} | ${d.drReference} | ${d.customer} | ${d.productName} | ${d.bagsDispatched} bags (${effectiveDispatchWeightKg(d).toFixed(0)} kg net) | ${resolvedDispatchStatus(d)}`);
    }
  }

  if (inc.has("Schedule") && report.filteredSchedules.length) {
    lines.push("", "SCHEDULED MILLING RUNS:");
    for (const sch of report.filteredSchedules.slice(0, 25)) {
      lines.push(`- ${sch.scheduledDate} | ${sch.section} | ${sch.productName} | ${sch.targetBags} bags (${Number(sch.expectedWeightKg || 0).toFixed(0)} kg) | ${sch.priority} | ${sch.status}`);
    }
  }

  if (inc.has("Orders") && (report.filteredOrders.length || report.activeOpenOrders.length)) {
    if (report.filteredOrders.length) {
      lines.push("", "ORDERS IN PERIOD:");
      for (const o of report.filteredOrders.slice(0, 25)) {
        lines.push(`- ${o.orderCode} | ${o.customer} | ${o.productName} | ${o.orderedBags} b (${Number(o.totalWeightKg || 0).toFixed(0)} kg) | Due ${o.dueDate} | ${o.status}`);
      }
    }
    if (report.activeOpenOrders.length) {
      lines.push("", "ACTIVE OPEN ORDERS:");
      for (const o of report.activeOpenOrders.slice(0, 25)) {
        const rem = Math.max(0, Number(o.totalWeightKg || 0) - Number(o.dispatchedWeightKg || 0));
        lines.push(`- ${o.orderCode} | ${o.customer} | ${o.productName} | ${rem.toFixed(0)} kg left | Due ${o.dueDate} | ${o.status}`);
      }
    }
  }

  lines.push("", "Generated from FeedMill Ops Web Portal.");
  return lines.join("\n");
}

export function buildPlantReportCsv(report) {
  const inc = new Set(report.includedSections);
  const rows = [];
  const pushHeader = (cols) => rows.push(cols);

  pushHeader(["10K AGRI FEEDS - BULACAN PLANT REPORT", report.isDailyReport ? report.startDate : `${report.startDate} to ${report.endDate}`, `Plant: ${report.sectionFilter}`, `Scope: ${report.includedSections.join(", ")}`]);
  rows.push([]);

  if (inc.has("Inventory") || inc.has("Recovery")) {
    pushHeader(["PLANT SECTION SUMMARY"]);
    pushHeader(["Plant Section", "SKUs", "Available Bags", "Available Kg", "Produced Bags", "Recovered Kg", "Recovery %"]);
    for (const s of report.sectionSummaries) {
      rows.push([
        s.section,
        s.skuCount,
        s.totalAvailableBags.toFixed(1),
        s.totalAvailableStockKg.toFixed(1),
        s.totalBags,
        s.totalRecoveredKg.toFixed(1),
        s.recoveryPct.toFixed(2)
      ]);
    }
    rows.push([]);
  }

  if (inc.has("Inventory")) {
    pushHeader(["INVENTORY SKU DETAILS"]);
    pushHeader(["Product", "Section", "Available Bags", "Available Kg", "Produced Kg", "Dispatched Kg", "Open Orders Kg", "Need to Mill Kg", "Status"]);
    for (const r of report.filteredSummaries) {
      rows.push([
        r.sku.productName,
        r.sku.section,
        r.availableBags.toFixed(1),
        r.availableStockKg.toFixed(1),
        r.recoveredKg.toFixed(1),
        r.dispatchedKg.toFixed(1),
        r.openOrdersKg.toFixed(1),
        r.netProductionNeededKg.toFixed(1),
        r.status
      ]);
    }
    rows.push([]);
  }

  if (inc.has("Orders")) {
    pushHeader(["CUSTOMER ORDERS IN PERIOD"]);
    pushHeader(["Order Code", "Order Date", "Due Date", "Customer", "Product", "Ordered Bags", "Total Kg", "Dispatched Kg", "Remaining Kg", "Status"]);
    const orderRows = report.filteredOrders.length ? report.filteredOrders : report.activeOpenOrders;
    for (const o of orderRows) {
      const rem = Math.max(0, Number(o.totalWeightKg || 0) - Number(o.dispatchedWeightKg || 0));
      rows.push([
        o.orderCode,
        o.orderDate || "",
        o.dueDate || "",
        o.customer,
        o.productName,
        o.orderedBags,
        Number(o.totalWeightKg || 0).toFixed(1),
        Number(o.dispatchedWeightKg || 0).toFixed(1),
        rem.toFixed(1),
        o.status
      ]);
    }
    rows.push([]);
  }

  if (inc.has("Schedule")) {
    pushHeader(["SCHEDULED PRODUCTION RUNS"]);
    pushHeader(["Scheduled Date", "Section", "Product", "Target Bags", "Expected Kg", "Priority", "Linked Order", "Status", "Remarks"]);
    for (const sch of report.filteredSchedules) {
      rows.push([
        sch.scheduledDate,
        sch.section,
        sch.productName,
        sch.targetBags,
        Number(sch.expectedWeightKg || 0).toFixed(1),
        sch.priority,
        sch.linkedOrderCode || "",
        sch.status,
        sch.lineRemarks || ""
      ]);
    }
    rows.push([]);
  }

  if (inc.has("Dispatches")) {
    pushHeader(["DISPATCHES IN PERIOD"]);
    pushHeader(["Date", "DR Reference", "Customer", "Product", "Source Plant", "Bags", "Net Weight Kg", "Driver", "Truck", "Status"]);
    for (const d of report.filteredDispatches) {
      rows.push([
        d.date,
        d.drReference,
        d.customer,
        d.productName,
        d.sourcePlant || "",
        d.bagsDispatched,
        effectiveDispatchWeightKg(d).toFixed(1),
        d.driverName || "",
        d.truckNumber || "",
        resolvedDispatchStatus(d)
      ]);
    }
    rows.push([]);
  }

  if (inc.has("Recovery")) {
    pushHeader(["PRODUCTION RECOVERY BATCHES"]);
    pushHeader(["Date", "Section", "Product", "Bags", "Expected Kg", "Recovered Kg", "Recovery %", "Remarks"]);
    for (const b of report.filteredBatches) {
      rows.push([
        b.date,
        b.section,
        b.productName,
        b.bagsProduced,
        Number(b.expectedWeightKg || 0).toFixed(1),
        Number(b.actualRecoveredKg || 0).toFixed(1),
        Number(b.recoveryPct || 100).toFixed(2),
        b.remarks || ""
      ]);
    }
    rows.push([]);
  }

  return rows.map((row) => row.map((cell) => escapeCsvCell(cell)).join(",")).join("\n");
}

export function buildDailyLogMessengerText(dayLog, sectionFilter = "ALL") {
  if (!dayLog) return "No daily plant log data available.";
  const rows = (dayLog.skuRows || []).filter(
    (r) => sectionFilter === "ALL" || r.sku.section === sectionFilter
  );
  const lines = [
    "*10K AGRI FEEDS - DAILY PLANT LOG*",
    `Date: ${dayLog.date}${dayLog.isToday ? " (Today)" : ""} | Plant: ${sectionFilter}`,
    "==============================",
    `Closing Stock: ${Number(dayLog.totalClosingStockKg || 0).toFixed(0)} kg`,
    `Produced Today: ${Number(dayLog.totalProducedDayKg || 0).toFixed(0)} kg`,
    `Dispatched Today: ${Number(dayLog.totalDispatchedDayKg || 0).toFixed(0)} kg`,
    `Orders on Date: ${(dayLog.dayOrders || []).length}`,
    "",
    "*Active SKU Movements:*"
  ];
  const moved = rows.filter((r) => r.producedDayKg > 0 || r.dispatchedDayKg > 0 || r.openOrdersSnapshotKg > 0);
  const list = moved.length ? moved : rows.slice(0, 10);
  for (const r of list.slice(0, 15)) {
    lines.push(`• ${r.sku.productName}: Stock ${r.closingBags.toFixed(1)} b | Prod ${r.producedDayBags} b | Disp ${r.dispatchedDayBags} b`);
  }
  return lines.join("\n");
}

export function buildMailtoUrl({ to = "", subject = "", body = "" }) {
  const cleanTo = String(to || "").trim();
  const params = new URLSearchParams({
    subject: String(subject || ""),
    body: String(body || "")
  });
  return `mailto:${encodeURIComponent(cleanTo)}?${params.toString().replace(/\+/g, "%20")}`;
}

// =========================================================================
// FIRESTORE PAYLOAD BUILDERS (MATCHING firestore.rules 100%)
// =========================================================================

export function buildSkuCreatePayload(uid, input, serverTimestampValue) {
  return {
    userId: String(uid).trim(),
    productName: String(input.productName || "").trim().slice(0, 120),
    section: PLANT_SECTIONS.includes(input.section) ? input.section : PLANT_SECTIONS[0],
    openingStockKg: Math.max(0, Number(input.openingStockKg || 0)),
    standardBagWeightKg: Math.max(0.1, Number(input.standardBagWeightKg || 25)),
    targetRecoveryPct: Math.min(120, Math.max(1, Number(input.targetRecoveryPct || 99.5))),
    safetyBufferPct: Math.min(200, Math.max(0, Number(input.safetyBufferPct || 15))),
    defaultRemarks: String(input.defaultRemarks || "MIXING/CONVEYOR").trim().slice(0, 120),
    createdAt: serverTimestampValue,
    updatedAt: serverTimestampValue
  };
}

export function buildSkuUpdatePayload(input, serverTimestampValue) {
  return {
    productName: String(input.productName || "").trim().slice(0, 120),
    section: PLANT_SECTIONS.includes(input.section) ? input.section : PLANT_SECTIONS[0],
    openingStockKg: Math.max(0, Number(input.openingStockKg || 0)),
    standardBagWeightKg: Math.max(0.1, Number(input.standardBagWeightKg || 25)),
    targetRecoveryPct: Math.min(120, Math.max(1, Number(input.targetRecoveryPct || 99.5))),
    safetyBufferPct: Math.min(200, Math.max(0, Number(input.safetyBufferPct || 15))),
    defaultRemarks: String(input.defaultRemarks || "").trim().slice(0, 120),
    updatedAt: serverTimestampValue
  };
}

export function buildOrderCreatePayload(uid, input, serverTimestampValue) {
  const bags = Math.max(1, Math.round(Number(input.orderedBags || 1)));
  const bagWeightKg = Math.max(0.1, Number(input.bagWeightKg || 25));
  const totalWeightKg = Number((bags * bagWeightKg).toFixed(2));
  const dispatchedWeightKg = Math.max(0, Math.min(totalWeightKg, Number(input.dispatchedWeightKg || 0)));
  const validStatuses = ["Open", "Partially Dispatched", "Fulfilled", "Cancelled"];
  return {
    userId: String(uid).trim(),
    orderCode: String(input.orderCode || `ORD-${Date.now().toString().slice(-6)}`).trim().slice(0, 40),
    orderDate: String(input.orderDate || todayIsoDate()),
    dueDate: String(input.dueDate || input.orderDate || todayIsoDate()),
    productName: String(input.productName || "").trim().slice(0, 120),
    orderedBags: bags,
    bagWeightKg,
    totalWeightKg,
    dispatchedWeightKg,
    customer: String(input.customer || "").trim().slice(0, 120),
    status: validStatuses.includes(input.status) ? input.status : "Open",
    priority: Boolean(input.priority),
    rushOrder: Boolean(input.rushOrder),
    createdAt: serverTimestampValue,
    updatedAt: serverTimestampValue
  };
}

export function buildOrderUpdatePayload(input, existingDispatchedKg, serverTimestampValue) {
  const bags = Math.max(1, Math.round(Number(input.orderedBags || 1)));
  const bagWeightKg = Math.max(0.1, Number(input.bagWeightKg || 25));
  const totalWeightKg = Number((bags * bagWeightKg).toFixed(2));
  const dispKg = Number(existingDispatchedKg || 0);
  let status = input.status;
  if (!["Open", "Partially Dispatched", "Fulfilled", "Cancelled"].includes(status)) {
    status = dispKg >= totalWeightKg - 0.01 ? "Fulfilled" : dispKg > 0 ? "Partially Dispatched" : "Open";
  }
  return {
    dueDate: String(input.dueDate || todayIsoDate()),
    orderedBags: bags,
    bagWeightKg,
    totalWeightKg,
    customer: String(input.customer || "").trim().slice(0, 120),
    priority: Boolean(input.priority),
    rushOrder: Boolean(input.rushOrder),
    status,
    updatedAt: serverTimestampValue
  };
}

export function buildRushOrderCode(drReference) {
  const cleanDr = (String(drReference || "DISP").trim() || "DISP").replace(/\s+/g, "-");
  return `RUSH-${cleanDr.slice(0, 35)}`.slice(0, 40);
}

export function buildDispatchCreatePayload(uid, input, serverTimestampValue) {
  const bags = Math.max(1, Math.round(Number(input.bagsDispatched || 1)));
  const bagWeightKg = Math.max(0.1, Number(input.bagWeightKg || 25));
  const totalWeightKg = Number((bags * bagWeightKg).toFixed(2));
  const sourcePlant = PLANT_SECTIONS.includes(input.sourcePlant) ? input.sourcePlant : "";
  const isHeld = Boolean(input.isHeld);
  const returnConfirmed = Boolean(input.returnConfirmed);
  const hasReturn = Boolean(input.hasReturn || returnConfirmed);
  const returnedBags = returnConfirmed ? Math.min(bags, Math.max(1, Math.round(Number(input.returnedBags || 1)))) : 0;
  const returnedWeightKg = returnConfirmed
    ? Math.min(totalWeightKg, Math.max(0, Number(input.returnedWeightKg || returnedBags * bagWeightKg)))
    : 0;
  const dispatchStatus = isHeld ? "Held" : returnConfirmed ? "Returned" : "Delivered";

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
    dispatchStatus,
    hasReturn,
    returnConfirmed,
    returnedBags,
    returnedWeightKg: Number(returnedWeightKg.toFixed(2)),
    returnReason: returnConfirmed ? (String(input.returnReason || "Quantity Variance").trim().slice(0, 120) || "Other") : "",
    isHeld,
    heldRemarks: isHeld ? (String(input.heldRemarks || "Held at gate").trim().slice(0, 300) || "Held") : "",
    createdAt: serverTimestampValue,
    updatedAt: serverTimestampValue
  };
}

export function buildDispatchUpdatePayload(input, serverTimestampValue) {
  const base = buildDispatchCreatePayload("unused", input, serverTimestampValue);
  delete base.userId;
  delete base.createdAt;
  return base;
}

export function buildBatchCreatePayload(uid, input, serverTimestampValue) {
  const bags = Math.max(1, Math.round(Number(input.bagsProduced || 1)));
  const bagWeightKg = Math.max(0.1, Number(input.bagWeightKg || 25));
  const expectedWeightKg = Number((bags * bagWeightKg).toFixed(2));
  const actualRecoveredKg = Math.max(0.1, Number(input.actualRecoveredKg || expectedWeightKg));
  const varianceKg = Number(Math.min(5000000, Math.max(-5000000, actualRecoveredKg - expectedWeightKg)).toFixed(2));
  const recoveryPct = expectedWeightKg > 0
    ? Number(Math.min(200, Math.max(0, (actualRecoveredKg / expectedWeightKg) * 100)).toFixed(2))
    : 100;
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

export function buildBatchUpdatePayload(input, serverTimestampValue) {
  const base = buildBatchCreatePayload("unused", input, serverTimestampValue);
  delete base.userId;
  delete base.createdAt;
  return base;
}

export function buildScheduleCreatePayload(uid, input, serverTimestampValue) {
  const bags = Math.max(1, Math.round(Number(input.targetBags || 1)));
  const bagWeightKg = Math.max(0.1, Number(input.bagWeightKg || 25));
  const expectedWeightKg = Number((bags * bagWeightKg).toFixed(2));
  return {
    userId: String(uid).trim(),
    scheduledDate: String(input.scheduledDate || todayIsoDate()),
    section: PLANT_SECTIONS.includes(input.section) ? input.section : PLANT_SECTIONS[0],
    productName: String(input.productName || "").trim().slice(0, 120),
    targetBags: bags,
    bagWeightKg,
    expectedWeightKg,
    lineRemarks: String(input.lineRemarks || "MIXING/CONVEYOR").trim().slice(0, 120) || "MIXING/CONVEYOR",
    priority: SCHEDULE_PRIORITIES.includes(input.priority) ? input.priority : "Order-Driven",
    status: SCHEDULE_STATUSES.includes(input.status) ? input.status : "Scheduled",
    linkedOrderId: String(input.linkedOrderId || "").trim().slice(0, 128),
    linkedOrderCode: String(input.linkedOrderCode || "").trim().slice(0, 40),
    linkedOrderIds: Array.isArray(input.linkedOrderIds) ? input.linkedOrderIds.slice(0, 100) : [],
    linkedOrderCodes: Array.isArray(input.linkedOrderCodes) ? input.linkedOrderCodes.slice(0, 100) : [],
    createdAt: serverTimestampValue,
    updatedAt: serverTimestampValue
  };
}

export function buildScheduleUpdatePayload(input, serverTimestampValue) {
  const bags = Math.max(1, Math.round(Number(input.targetBags || 1)));
  const bagWeightKg = Math.max(0.1, Number(input.bagWeightKg || 25));
  const expectedWeightKg = Number((bags * bagWeightKg).toFixed(2));
  return {
    scheduledDate: String(input.scheduledDate || todayIsoDate()),
    targetBags: bags,
    bagWeightKg,
    expectedWeightKg,
    lineRemarks: String(input.lineRemarks || "MIXING/CONVEYOR").trim().slice(0, 120) || "MIXING/CONVEYOR",
    priority: SCHEDULE_PRIORITIES.includes(input.priority) ? input.priority : "Order-Driven",
    status: SCHEDULE_STATUSES.includes(input.status) ? input.status : "Scheduled",
    linkedOrderId: String(input.linkedOrderId || "").trim().slice(0, 128),
    linkedOrderCode: String(input.linkedOrderCode || "").trim().slice(0, 40),
    linkedOrderIds: Array.isArray(input.linkedOrderIds) ? input.linkedOrderIds.slice(0, 100) : [],
    linkedOrderCodes: Array.isArray(input.linkedOrderCodes) ? input.linkedOrderCodes.slice(0, 100) : [],
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
    lastRestockedDate: isValidIsoDateStr(input.lastRestockedDate) ? input.lastRestockedDate : todayIsoDate(),
    createdAt: serverTimestampValue,
    updatedAt: serverTimestampValue
  };
}

export function buildRawMaterialUpdatePayload(input, serverTimestampValue) {
  const base = buildRawMaterialCreatePayload("unused", input, serverTimestampValue);
  delete base.userId;
  delete base.createdAt;
  return base;
}

export function buildBomCreatePayload(uid, input, serverTimestampValue) {
  const lines = (Array.isArray(input.lines) ? input.lines : [])
    .filter((l) => String(l.materialName || "").trim())
    .slice(0, 40)
    .map((l) => ({
      materialId: String(l.materialId || "").trim().slice(0, 128),
      materialName: String(l.materialName || "").trim().slice(0, 120),
      category: RAW_CATEGORIES.includes(l.category) ? l.category : RAW_CATEGORIES[0],
      inclusionPct: Math.min(100, Math.max(0, Number(l.inclusionPct || 0))),
      notes: String(l.notes || "").trim().slice(0, 120)
    }));
  return {
    userId: String(uid).trim(),
    productName: String(input.productName || "").trim().slice(0, 120),
    section: PLANT_SECTIONS.includes(input.section) ? input.section : PLANT_SECTIONS[0],
    batchBaseKg: Math.min(100000, Math.max(1, Number(input.batchBaseKg || 1000))),
    yieldLossPct: Math.min(50, Math.max(0, Number(input.yieldLossPct || 1.5))),
    lines,
    notes: String(input.notes || "").trim().slice(0, 300),
    createdAt: serverTimestampValue,
    updatedAt: serverTimestampValue
  };
}

export function buildBomUpdatePayload(input, serverTimestampValue) {
  const base = buildBomCreatePayload("unused", input, serverTimestampValue);
  delete base.userId;
  delete base.createdAt;
  return base;
}

export function buildReprocessCreatePayload(uid, input, serverTimestampValue) {
  const returnedBags = Math.max(1, Math.round(Number(input.returnedBags || 1)));
  const bagWeightKg = Math.max(0.1, Number(input.bagWeightKg || 25));
  const returnedWeightKg = Number(Math.max(0.1, Number(input.returnedWeightKg || returnedBags * bagWeightKg)).toFixed(2));
  const flaggedToInventory = Boolean(input.flaggedToInventory);
  const status = flaggedToInventory
    ? "Flagged to Inventory"
    : REPROCESS_STATUSES.includes(input.status)
    ? input.status
    : "Pending Reprocess";
  const isDamaged = status === "Damaged";
  return {
    userId: String(uid).trim(),
    sourceDispatchId: String(input.sourceDispatchId || "").trim().slice(0, 128),
    drReference: String(input.drReference || "").trim().slice(0, 80),
    customer: String(input.customer || "").trim().slice(0, 120),
    productName: String(input.productName || "").trim().slice(0, 120),
    section: PLANT_SECTIONS.includes(input.section) ? input.section : PLANT_SECTIONS[0],
    returnedDate: isValidIsoDateStr(input.returnedDate) ? input.returnedDate : todayIsoDate(),
    returnedBags,
    bagWeightKg,
    returnedWeightKg,
    reprocessedBags: Math.max(0, Math.round(Number(input.reprocessedBags || 0))),
    reprocessedWeightKg: Number(Math.max(0, Number(input.reprocessedWeightKg || 0)).toFixed(2)),
    returnReason: (String(input.returnReason || "Quantity Variance").trim() || "Other").slice(0, 120),
    status,
    fastActionNeeded: isDamaged || flaggedToInventory ? false : input.fastActionNeeded !== false,
    flaggedToInventory,
    reprocessedDate: isValidIsoDateStr(input.reprocessedDate) ? input.reprocessedDate : "",
    actionNotes: String(input.actionNotes || "").trim().slice(0, 300),
    createdAt: serverTimestampValue,
    updatedAt: serverTimestampValue
  };
}

export function buildReprocessUpdatePayload(input, serverTimestampValue) {
  const base = buildReprocessCreatePayload("unused", input, serverTimestampValue);
  delete base.userId;
  delete base.createdAt;
  return base;
}

export function buildCustomerCreatePayload(uid, input, serverTimestampValue) {
  return {
    userId: String(uid).trim(),
    name: String(input.name || "").trim().slice(0, 120),
    notes: String(input.notes || "").trim().slice(0, 120),
    priority: Boolean(input.priority),
    createdAt: serverTimestampValue,
    updatedAt: serverTimestampValue
  };
}

export function buildCustomerUpdatePayload(input, serverTimestampValue) {
  return {
    name: String(input.name || "").trim().slice(0, 120),
    notes: String(input.notes || "").trim().slice(0, 120),
    priority: Boolean(input.priority),
    updatedAt: serverTimestampValue
  };
}

export function buildSettingsCreatePayload(uid, input, serverTimestampValue) {
  return {
    userId: String(uid).trim(),
    seededOctober2026: input.seededOctober2026 !== false,
    lookbackDays: Math.min(90, Math.max(1, Math.round(Number(input.lookbackDays || 14)))),
    defaultSafetyBufferPct: Math.min(100, Math.max(0, Number(input.defaultSafetyBufferPct ?? 15))),
    createdAt: serverTimestampValue,
    updatedAt: serverTimestampValue
  };
}

export function buildSettingsUpdatePayload(input, serverTimestampValue) {
  return {
    seededOctober2026: input.seededOctober2026 !== false,
    lookbackDays: Math.min(90, Math.max(1, Math.round(Number(input.lookbackDays || 14)))),
    defaultSafetyBufferPct: Math.min(100, Math.max(0, Number(input.defaultSafetyBufferPct ?? 15))),
    updatedAt: serverTimestampValue
  };
}

export function buildBreadcrumbPayload(uid, input, serverTimestampValue) {
  const validCategories = [
    "Navigation", "Inventory", "Orders", "Dispatch", "Schedule",
    "Recovery", "Reprocess", "Customers", "Reports", "AI Ops", "System"
  ];
  const validSeverities = ["Info", "Success", "Warning", "Error"];
  const now = new Date();
  const timeLabel = `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}:${String(now.getSeconds()).padStart(2, "0")}`;
  return {
    userId: String(uid).trim(),
    actorType: input.actorType === "System" ? "System" : "User",
    category: validCategories.includes(input.category) ? input.category : "System",
    action: (String(input.action || "Web Action").trim() || "Web Action").slice(0, 160),
    details: String(input.details || "").trim().slice(0, 500),
    screenTab: String(input.screenTab || "Web Portal").trim().slice(0, 60),
    entityRef: String(input.entityRef || "").trim().slice(0, 120),
    trailPath: String(input.trailPath || "Web Portal").trim().slice(0, 240),
    severity: validSeverities.includes(input.severity) ? input.severity : "Info",
    dateStr: todayIsoDate(now),
    timeLabel,
    timestampMillis: Date.now(),
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

export function buildMemberRoleUpdatePayload(role, serverTimestampValue) {
  return {
    role: resolveRole(role).label,
    updatedAt: serverTimestampValue
  };
}

export function buildInviteRoleUpdatePayload(role, invitedByEmail, serverTimestampValue) {
  const payload = {
    role: resolveRole(role).label,
    updatedAt: serverTimestampValue
  };
  const cleanEmail = String(invitedByEmail || "").trim().toLowerCase().slice(0, 160);
  if (cleanEmail) {
    payload.invitedByEmail = cleanEmail;
  }
  return payload;
}

export function parseBulkBatchRows(rawText, defaultSection = PLANT_SECTIONS[0]) {
  const lines = String(rawText || "").split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
  const results = [];
  for (const line of lines) {
    const parts = line.includes("\t") ? line.split("\t") : line.split(",");
    if (parts.length < 3) continue;
    const trimmed = parts.map((p) => p.trim());
    let date = todayIsoDate();
    let idx = 0;
    if (isValidIsoDateStr(trimmed[0])) {
      date = trimmed[0];
      idx = 1;
    }
    const maybeSection = trimmed[idx]?.toUpperCase();
    let section = defaultSection;
    if (PLANT_SECTIONS.includes(maybeSection)) {
      section = maybeSection;
      idx += 1;
    }
    const productName = trimmed[idx] || "";
    const bagsProduced = Number(trimmed[idx + 1] || 0);
    const bagWeightKg = Number(trimmed[idx + 2] || 25) || 25;
    const actualRecoveredKg = Number(trimmed[idx + 3] || bagsProduced * bagWeightKg);
    const remarks = trimmed[idx + 4] || "MIXING/CONVEYOR";
    if (!productName || !Number.isFinite(bagsProduced) || bagsProduced <= 0) continue;
    results.push({ date, section, productName, bagsProduced, bagWeightKg, actualRecoveredKg, remarks });
  }
  return results;
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
