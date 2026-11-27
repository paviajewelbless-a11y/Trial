package com.example.model

enum class MaterialSector(
    val label: String,
    val shortLabel: String,
    val isMicro: Boolean,
    val codePrefix: String
) {
    MACRO("Macro Materials", "Macro", false, "MAC"),
    MICRO_FAST("Micro • Fast Moving", "Micro: Fast", true, "MIC-F"),
    MICRO_SLOW("Micro • Slow Moving", "Micro: Slow", true, "MIC-S"),
    MICRO_NON_MOVING("Micro • Non-Moving / Trial", "Micro: Non-Mov", true, "MIC-N")
}

enum class MicroMovementTag(val label: String, val shortCode: String) {
    MACRO("MACRO", "MAC"),
    FAST_MOVING("FAST MOVING", "FAST"),
    SLOW_MOVING("SLOW MOVING", "SLOW"),
    NON_MOVING("NON MOVING", "NON"),
    TRIAL("TRIAL", "TRL"),
    NEW_MICROMIX("NEW MICROMIX", "NEW")
}

data class PlantMaterialSpec(
    val idHint: String,
    val name: String,
    val sector: MaterialSector,
    val movementTag: MicroMovementTag,
    val category: RawMaterialCategory,
    val bomCode: String,
    val defaultInclusionPct: Double,
    val defaultStockKg: Double,
    val defaultReorderKg: Double,
    val defaultLeadDays: Long,
    val defaultSupplier: String = ""
) {
    fun toEntity(userId: String = ""): RawMaterialEntity = RawMaterialEntity(
        userId = userId,
        name = name,
        category = category.label,
        currentStockKg = defaultStockKg,
        reorderThresholdKg = defaultReorderKg,
        usageSharePct = defaultInclusionPct,
        leadTimeDays = defaultLeadDays,
        safetyDays = if (sector == MaterialSector.MACRO) 3L else 5L,
        supplierName = defaultSupplier,
        alertsEnabled = sector != MaterialSector.MICRO_NON_MOVING
    )
}

object PlantMaterialCatalog {

    val allSpecs: List<PlantMaterialSpec> = listOf(
        // ============================================================
        // MACRO MATERIALS (9)
        // ============================================================
        PlantMaterialSpec("raw_whole_yellow_corn", "Whole Yellow Corn/Balanok Corn", MaterialSector.MACRO, MicroMovementTag.MACRO, RawMaterialCategory.ENERGY, "MAC-ENG", 48.0, 5000.0, 2500.0, 4L, "Macro Grain Supply"),
        PlantMaterialSpec("raw_us_soya", "US Soya (Soy Bean Meal)", MaterialSector.MACRO, MicroMovementTag.MACRO, RawMaterialCategory.PROTEIN, "MAC-PRO", 24.0, 3200.0, 1500.0, 5L, "US Soya Import"),
        PlantMaterialSpec("raw_hard_pollard", "Hard Pollard", MaterialSector.MACRO, MicroMovementTag.MACRO, RawMaterialCategory.ENERGY, "MAC-ENG", 10.0, 2200.0, 800.0, 3L, "Flour Mill Supply"),
        PlantMaterialSpec("raw_soft_pollard", "Soft Pollard", MaterialSector.MACRO, MicroMovementTag.MACRO, RawMaterialCategory.ENERGY, "MAC-ENG", 8.0, 1800.0, 700.0, 3L, "Flour Mill Supply"),
        PlantMaterialSpec("raw_rice_bran_d1", "Rice Bran D1", MaterialSector.MACRO, MicroMovementTag.MACRO, RawMaterialCategory.ENERGY, "MAC-ENG", 12.0, 3500.0, 1200.0, 3L, "Nueva Ecija Ricemill"),
        PlantMaterialSpec("raw_rice_bran_d3", "Rice Bran D3", MaterialSector.MACRO, MicroMovementTag.MACRO, RawMaterialCategory.ENERGY, "MAC-ENG", 6.0, 1600.0, 600.0, 3L, "Nueva Ecija Ricemill"),
        PlantMaterialSpec("raw_corn_bran", "Corn Bran", MaterialSector.MACRO, MicroMovementTag.MACRO, RawMaterialCategory.ENERGY, "MAC-ENG", 5.0, 1400.0, 500.0, 4L, "Corn Mill Supply"),
        PlantMaterialSpec("raw_cookie_meal", "Cookie Meal", MaterialSector.MACRO, MicroMovementTag.MACRO, RawMaterialCategory.ENERGY, "MAC-ENG", 5.0, 1200.0, 400.0, 4L, "Bakery By-Product"),
        PlantMaterialSpec("raw_mollases", "Mollases", MaterialSector.MACRO, MicroMovementTag.MACRO, RawMaterialCategory.LIQUID, "MAC-LIQ", 2.5, 1100.0, 450.0, 4L, "Sugar Central"),

        // ============================================================
        // MICRO MATERIALS — SECTOR 1: FAST MOVING (11)
        // ============================================================
        PlantMaterialSpec("raw_mesh_100", "Mesh 100 (Powder Limestone)", MaterialSector.MICRO_FAST, MicroMovementTag.FAST_MOVING, RawMaterialCategory.MINERAL, "MIC-FST", 1.5, 850.0, 300.0, 3L, "Micro • Fast Moving"),
        PlantMaterialSpec("raw_mesh_45", "Mesh 45 (Fine Limestone - PCDLY)", MaterialSector.MICRO_FAST, MicroMovementTag.FAST_MOVING, RawMaterialCategory.MINERAL, "MIC-FST", 1.5, 800.0, 300.0, 3L, "Micro • Fast Moving"),
        PlantMaterialSpec("raw_yellow_dye", "Yellow Dye / Ferna Egg Yellow", MaterialSector.MICRO_FAST, MicroMovementTag.FAST_MOVING, RawMaterialCategory.ADDITIVE, "MIC-FST", 0.1, 75.0, 25.0, 4L, "Micro • Fast Moving"),
        PlantMaterialSpec("raw_luctarom_pure", "Luctarom Pure", MaterialSector.MICRO_FAST, MicroMovementTag.FAST_MOVING, RawMaterialCategory.ADDITIVE, "MIC-FST", 0.1, 60.0, 20.0, 5L, "Micro • Fast Moving"),
        PlantMaterialSpec("raw_luctarom_premix", "Luctarom (PREMIX)", MaterialSector.MICRO_FAST, MicroMovementTag.FAST_MOVING, RawMaterialCategory.ADDITIVE, "MIC-FST", 0.2, 120.0, 40.0, 4L, "Micro • Fast Moving"),
        PlantMaterialSpec("raw_pantutti_premix", "PanTutti Fruitti (PREMIX)", MaterialSector.MICRO_FAST, MicroMovementTag.FAST_MOVING, RawMaterialCategory.ADDITIVE, "MIC-FST", 0.2, 110.0, 40.0, 4L, "Micro • Fast Moving"),
        PlantMaterialSpec("raw_aspartame", "Aspartame", MaterialSector.MICRO_FAST, MicroMovementTag.FAST_MOVING, RawMaterialCategory.ADDITIVE, "MIC-FST", 0.1, 50.0, 20.0, 5L, "Micro • Fast Moving"),
        PlantMaterialSpec("raw_edul_codex_premix", "Edul Codex (PREMIX)", MaterialSector.MICRO_FAST, MicroMovementTag.FAST_MOVING, RawMaterialCategory.ADDITIVE, "MIC-FST", 0.2, 100.0, 35.0, 4L, "Micro • Fast Moving"),
        PlantMaterialSpec("raw_luprosil_salt", "Luprosil Salt", MaterialSector.MICRO_FAST, MicroMovementTag.FAST_MOVING, RawMaterialCategory.ADDITIVE, "MIC-FST", 0.2, 150.0, 50.0, 4L, "Micro • Fast Moving"),
        PlantMaterialSpec("raw_fixar_s", "Fixar S", MaterialSector.MICRO_FAST, MicroMovementTag.FAST_MOVING, RawMaterialCategory.ADDITIVE, "MIC-FST", 0.2, 160.0, 50.0, 4L, "Micro • Fast Moving"),
        PlantMaterialSpec("raw_choline_chloride", "Choline Chloride", MaterialSector.MICRO_FAST, MicroMovementTag.FAST_MOVING, RawMaterialCategory.ADDITIVE, "MIC-FST", 0.3, 220.0, 75.0, 5L, "Micro • Fast Moving"),

        // ============================================================
        // MICRO MATERIALS — SECTOR 2: SLOW MOVING (17)
        // ============================================================
        PlantMaterialSpec("raw_iodized_salt", "Iodized Salt", MaterialSector.MICRO_SLOW, MicroMovementTag.SLOW_MOVING, RawMaterialCategory.MINERAL, "MIC-SLW", 0.4, 350.0, 100.0, 3L, "Micro • Slow Moving"),
        PlantMaterialSpec("raw_ctc_15", "CTC 15% (Chlortetracycline Hydrochloride)", MaterialSector.MICRO_SLOW, MicroMovementTag.SLOW_MOVING, RawMaterialCategory.ADDITIVE, "MIC-SLW", 0.2, 100.0, 35.0, 6L, "Micro • Slow Moving"),
        PlantMaterialSpec("raw_phytase", "Phytase", MaterialSector.MICRO_SLOW, MicroMovementTag.SLOW_MOVING, RawMaterialCategory.ADDITIVE, "MIC-SLW", 0.1, 75.0, 25.0, 6L, "Micro • Slow Moving"),
        PlantMaterialSpec("raw_swine_vitamin_1", "Swine Vitamin 1", MaterialSector.MICRO_SLOW, MicroMovementTag.SLOW_MOVING, RawMaterialCategory.ADDITIVE, "MIC-SLW", 0.3, 180.0, 60.0, 5L, "Micro • Slow Moving"),
        PlantMaterialSpec("raw_ronozyme_gt", "Ronozyme Multigrain GT", MaterialSector.MICRO_SLOW, MicroMovementTag.SLOW_MOVING, RawMaterialCategory.ADDITIVE, "MIC-SLW", 0.1, 60.0, 20.0, 7L, "Micro • Slow Moving"),
        PlantMaterialSpec("raw_prymox", "Prymox", MaterialSector.MICRO_SLOW, MicroMovementTag.SLOW_MOVING, RawMaterialCategory.ADDITIVE, "MIC-SLW", 0.1, 60.0, 20.0, 6L, "Micro • Slow Moving"),
        PlantMaterialSpec("raw_primeat_max", "Primeat Max", MaterialSector.MICRO_SLOW, MicroMovementTag.SLOW_MOVING, RawMaterialCategory.ADDITIVE, "MIC-SLW", 0.2, 90.0, 30.0, 6L, "Micro • Slow Moving"),
        PlantMaterialSpec("raw_swine_mineral_1", "Swine Mineral 1 (Premix)", MaterialSector.MICRO_SLOW, MicroMovementTag.SLOW_MOVING, RawMaterialCategory.MINERAL, "MIC-SLW", 0.3, 200.0, 60.0, 5L, "Micro • Slow Moving"),
        PlantMaterialSpec("raw_mdcp", "MDCP/MonodiCalcium Phosphate", MaterialSector.MICRO_SLOW, MicroMovementTag.SLOW_MOVING, RawMaterialCategory.MINERAL, "MIC-SLW", 1.2, 550.0, 200.0, 5L, "Micro • Slow Moving"),
        PlantMaterialSpec("raw_dl_methionine", "DL-Methionine", MaterialSector.MICRO_SLOW, MicroMovementTag.SLOW_MOVING, RawMaterialCategory.ADDITIVE, "MIC-SLW", 0.2, 140.0, 50.0, 6L, "Micro • Slow Moving"),
        PlantMaterialSpec("raw_l_threonine", "L-Threonine", MaterialSector.MICRO_SLOW, MicroMovementTag.SLOW_MOVING, RawMaterialCategory.ADDITIVE, "MIC-SLW", 0.2, 120.0, 40.0, 6L, "Micro • Slow Moving"),
        PlantMaterialSpec("raw_copper_sulphate", "Copper Sulphate Pentahydrate", MaterialSector.MICRO_SLOW, MicroMovementTag.SLOW_MOVING, RawMaterialCategory.MINERAL, "MIC-SLW", 0.1, 80.0, 25.0, 6L, "Micro • Slow Moving"),
        PlantMaterialSpec("raw_l_lysine_hci", "L-Lysine HCI", MaterialSector.MICRO_SLOW, MicroMovementTag.SLOW_MOVING, RawMaterialCategory.ADDITIVE, "MIC-SLW", 0.3, 180.0, 60.0, 6L, "Micro • Slow Moving"),
        PlantMaterialSpec("raw_tiamulin", "Tiamulin", MaterialSector.MICRO_SLOW, MicroMovementTag.SLOW_MOVING, RawMaterialCategory.ADDITIVE, "MIC-SLW", 0.1, 45.0, 15.0, 7L, "Micro • Slow Moving"),
        PlantMaterialSpec("raw_strepsol_pure", "Strepsol / Meat Enhancer (Pure)", MaterialSector.MICRO_SLOW, MicroMovementTag.SLOW_MOVING, RawMaterialCategory.ADDITIVE, "MIC-SLW", 0.1, 50.0, 15.0, 6L, "Micro • Slow Moving"),
        PlantMaterialSpec("raw_strepsol_premixed", "Strepsol / Meat Enhancer (Premixed)", MaterialSector.MICRO_SLOW, MicroMovementTag.SLOW_MOVING, RawMaterialCategory.ADDITIVE, "MIC-SLW", 0.2, 95.0, 30.0, 5L, "Micro • Slow Moving"),
        PlantMaterialSpec("raw_bel_feed", "Bel Feed", MaterialSector.MICRO_SLOW, MicroMovementTag.SLOW_MOVING, RawMaterialCategory.ADDITIVE, "MIC-SLW", 0.2, 100.0, 30.0, 6L, "Micro • Slow Moving"),

        // ============================================================
        // MICRO MATERIALS — SECTOR 3: NON-MOVING / TRIAL & NEW (15)
        // ============================================================
        PlantMaterialSpec("raw_pantutti_pure", "PanTutti Fruitti Pure", MaterialSector.MICRO_NON_MOVING, MicroMovementTag.TRIAL, RawMaterialCategory.ADDITIVE, "MIC-TRL", 0.1, 25.0, 10.0, 7L, "Micro • Trial"),
        PlantMaterialSpec("raw_aspartame_premix", "Aspartame (PREMIX)", MaterialSector.MICRO_NON_MOVING, MicroMovementTag.NEW_MICROMIX, RawMaterialCategory.ADDITIVE, "MIC-NEW", 0.2, 40.0, 15.0, 5L, "Micro • New Micromix"),
        PlantMaterialSpec("raw_edul_codex_pure", "Edul Codex (PURE)", MaterialSector.MICRO_NON_MOVING, MicroMovementTag.NEW_MICROMIX, RawMaterialCategory.ADDITIVE, "MIC-NEW", 0.1, 25.0, 10.0, 6L, "Micro • New Micromix"),
        PlantMaterialSpec("raw_ferna_strawberry_red", "Ferna Strawberry Red", MaterialSector.MICRO_NON_MOVING, MicroMovementTag.NON_MOVING, RawMaterialCategory.ADDITIVE, "MIC-NON", 0.1, 30.0, 10.0, 7L, "Micro • Non-Moving"),
        PlantMaterialSpec("raw_l_thryptophan", "L-Thryptophan", MaterialSector.MICRO_NON_MOVING, MicroMovementTag.NON_MOVING, RawMaterialCategory.ADDITIVE, "MIC-NON", 0.1, 35.0, 10.0, 7L, "Micro • Non-Moving"),
        PlantMaterialSpec("raw_sodium_bicarbonate", "Sodium Bicarbonate", MaterialSector.MICRO_NON_MOVING, MicroMovementTag.NON_MOVING, RawMaterialCategory.MINERAL, "MIC-NON", 0.2, 100.0, 30.0, 5L, "Micro • Non-Moving"),
        PlantMaterialSpec("raw_turbo_grow", "Turbo Grow", MaterialSector.MICRO_NON_MOVING, MicroMovementTag.NON_MOVING, RawMaterialCategory.ADDITIVE, "MIC-NON", 0.1, 40.0, 15.0, 7L, "Micro • Non-Moving"),
        PlantMaterialSpec("raw_nutrigano", "Nutrigano", MaterialSector.MICRO_NON_MOVING, MicroMovementTag.NON_MOVING, RawMaterialCategory.ADDITIVE, "MIC-NON", 0.1, 35.0, 10.0, 7L, "Micro • Non-Moving"),
        PlantMaterialSpec("raw_klinofeed", "Klinofeed", MaterialSector.MICRO_NON_MOVING, MicroMovementTag.NON_MOVING, RawMaterialCategory.ADDITIVE, "MIC-NON", 0.2, 80.0, 25.0, 6L, "Micro • Non-Moving"),
        PlantMaterialSpec("raw_pellex", "Pellex", MaterialSector.MICRO_NON_MOVING, MicroMovementTag.NON_MOVING, RawMaterialCategory.ADDITIVE, "MIC-NON", 0.2, 75.0, 25.0, 6L, "Micro • Non-Moving"),
        PlantMaterialSpec("raw_ng_fruit", "NG Fruit", MaterialSector.MICRO_NON_MOVING, MicroMovementTag.NON_MOVING, RawMaterialCategory.ADDITIVE, "MIC-NON", 0.1, 30.0, 10.0, 7L, "Micro • Non-Moving"),
        PlantMaterialSpec("raw_quantum_blue", "Quantum Blue", MaterialSector.MICRO_NON_MOVING, MicroMovementTag.NON_MOVING, RawMaterialCategory.ADDITIVE, "MIC-NON", 0.1, 30.0, 10.0, 7L, "Micro • Non-Moving"),
        PlantMaterialSpec("raw_swine_phytase", "Swine Phytase", MaterialSector.MICRO_NON_MOVING, MicroMovementTag.NON_MOVING, RawMaterialCategory.ADDITIVE, "MIC-NON", 0.1, 40.0, 15.0, 7L, "Micro • Non-Moving"),
        PlantMaterialSpec("raw_aqua_lyso", "Aqua Lyso", MaterialSector.MICRO_NON_MOVING, MicroMovementTag.NON_MOVING, RawMaterialCategory.ADDITIVE, "MIC-NON", 0.1, 25.0, 10.0, 7L, "Micro • Non-Moving"),
        PlantMaterialSpec("raw_carophyl_yellow", "Carophyl Yellow", MaterialSector.MICRO_NON_MOVING, MicroMovementTag.NON_MOVING, RawMaterialCategory.ADDITIVE, "MIC-NON", 0.1, 25.0, 10.0, 7L, "Micro • Non-Moving")
    )

    private val byLowerName: Map<String, PlantMaterialSpec> = buildMap {
        allSpecs.forEach { spec ->
            put(spec.name.trim().lowercase(), spec)
        }
        // Helpful aliases
        allSpecs.firstOrNull { it.idHint == "raw_l_lysine_hci" }?.let { put("l-lysine hcl", it) }
        allSpecs.firstOrNull { it.idHint == "raw_l_thryptophan" }?.let { put("l-tryptophan", it) }
        allSpecs.firstOrNull { it.idHint == "raw_mollases" }?.let {
            put("molasses", it)
            put("cane molasses", it)
        }
    }

    fun specsForSector(sector: MaterialSector): List<PlantMaterialSpec> =
        allSpecs.filter { it.sector == sector }

    fun resolveSpecByName(name: String): PlantMaterialSpec? =
        byLowerName[name.trim().lowercase()]

    fun inferSector(material: RawMaterialEntity): MaterialSector {
        resolveSpecByName(material.name)?.let { return it.sector }
        val sup = material.supplierName.lowercase()
        return when {
            "fast moving" in sup -> MaterialSector.MICRO_FAST
            "slow moving" in sup -> MaterialSector.MICRO_SLOW
            "non-moving" in sup || "non moving" in sup || "trial" in sup || "new micromix" in sup -> MaterialSector.MICRO_NON_MOVING
            "macro" in sup -> MaterialSector.MACRO
            else -> when (RawMaterialCategory.fromRaw(material.category)) {
                RawMaterialCategory.ENERGY,
                RawMaterialCategory.PROTEIN,
                RawMaterialCategory.LIQUID -> MaterialSector.MACRO
                RawMaterialCategory.MINERAL,
                RawMaterialCategory.ADDITIVE -> MaterialSector.MICRO_SLOW
            }
        }
    }

    fun inferMovementTag(material: RawMaterialEntity): MicroMovementTag {
        resolveSpecByName(material.name)?.let { return it.movementTag }
        val sup = material.supplierName.lowercase()
        return when {
            "fast moving" in sup -> MicroMovementTag.FAST_MOVING
            "slow moving" in sup -> MicroMovementTag.SLOW_MOVING
            "trial" in sup -> MicroMovementTag.TRIAL
            "new micromix" in sup -> MicroMovementTag.NEW_MICROMIX
            "non-moving" in sup || "non moving" in sup -> MicroMovementTag.NON_MOVING
            else -> when (inferSector(material)) {
                MaterialSector.MACRO -> MicroMovementTag.MACRO
                MaterialSector.MICRO_FAST -> MicroMovementTag.FAST_MOVING
                MaterialSector.MICRO_SLOW -> MicroMovementTag.SLOW_MOVING
                MaterialSector.MICRO_NON_MOVING -> MicroMovementTag.NON_MOVING
            }
        }
    }

    fun missingCatalogEntities(
        existingMaterials: List<RawMaterialEntity>,
        sectorFilter: MaterialSector? = null
    ): List<RawMaterialEntity> {
        val existingLower = existingMaterials.map { it.name.trim().lowercase() }.toSet()
        return allSpecs
            .filter { sectorFilter == null || it.sector == sectorFilter }
            .filter { it.name.trim().lowercase() !in existingLower }
            .map { it.toEntity() }
    }
}
