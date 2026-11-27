package com.example.model

object October2026SeedData {

    private data class SeedSpec(
        val id: String,
        val productName: String,
        val section: PlantSection,
        val beginningBags: Long,
        val bagWeightKg: Double = 50.0,
        val defaultRemarks: String = "MIXING/CONVEYOR"
    )

    private val specs: List<SeedSpec> = listOf(
        // SAN ROQUE — BEGINNING INVENTORY (TODAY)
        SeedSpec("sku_sr_balanok_corn", "BALANOK CORN", PlantSection.SAN_ROQUE, 0L, defaultRemarks = "GRINDING /MIXING"),
        SeedSpec("sku_sr_dog", "DOG", PlantSection.SAN_ROQUE, 0L),
        SeedSpec("sku_sr_evt_corn_grits", "EVT CORN GRITS", PlantSection.SAN_ROQUE, 0L, defaultRemarks = "GRINDING/BLOWER FAN/HAMMERMILL"),
        SeedSpec("sku_sr_evt_crack_corn", "EVT CRACK CORN", PlantSection.SAN_ROQUE, 78L, defaultRemarks = "GRINDING/BLOWER FAN/HAMMERMILL"),
        SeedSpec("sku_sr_flash_pigeon_reg", "FLASH PIGEON REGULAR [SAN ROQUE]", PlantSection.SAN_ROQUE, 45L),
        SeedSpec("sku_sr_gigamax_1000", "GIGAMAX 1000 [SAN ROQUE]", PlantSection.SAN_ROQUE, 0L),
        SeedSpec("sku_sr_gigamax_2000", "GIGAMAX 2000 [SAN ROQUE]", PlantSection.SAN_ROQUE, 0L),
        SeedSpec("sku_sr_gigamax_3000", "GIGAMAX 3000 [SAN ROQUE]", PlantSection.SAN_ROQUE, 0L),
        SeedSpec("sku_sr_chookmax_1000", "CHOOKMAX 1000 [SAN ROQUE]", PlantSection.SAN_ROQUE, 0L),
        SeedSpec("sku_sr_chookmax_2000", "CHOOKMAX 2000 [SAN ROQUE]", PlantSection.SAN_ROQUE, 0L),
        SeedSpec("sku_sr_chookmax_3000", "CHOOKMAX 3000 [SAN ROQUE]", PlantSection.SAN_ROQUE, 0L),
        SeedSpec("sku_sr_mb_broodsow_mash", "MB BROODSOW MASH", PlantSection.SAN_ROQUE, 383L, defaultRemarks = "GRINDING /MIXING"),
        SeedSpec("sku_sr_mb_broodsow_cheroy", "MB BROODSOW MASH (CHEROY)", PlantSection.SAN_ROQUE, 0L, defaultRemarks = "GRINDING /MIXING"),
        SeedSpec("sku_sr_mb_broodsow_highend", "MB BROODSOW MASH (HIGH END)", PlantSection.SAN_ROQUE, 0L, defaultRemarks = "GRINDING /MIXING"),
        SeedSpec("sku_sr_mb_hpdp_3mm_reg", "MB HPDP 3MM REGULAR [SAN ROQUE]", PlantSection.SAN_ROQUE, 131L),
        SeedSpec("sku_sr_mb_hpdp_3mm_yel", "MB HPDP 3MM YELLOW [SAN ROQUE]", PlantSection.SAN_ROQUE, 0L),
        SeedSpec("sku_sr_mb_hpdp_4mm_reg", "MB HPDP 4MM REGULAR [SAN ROQUE]", PlantSection.SAN_ROQUE, 0L),
        SeedSpec("sku_sr_mb_hpdp_4mm_yel", "MB HPDP 4MM YELLOW [SAN ROQUE]", PlantSection.SAN_ROQUE, 0L),
        SeedSpec("sku_sr_mb_hpdp_yel_crumble", "MB HPDP YELLOW CRUMBLE [SAN ROQUE]", PlantSection.SAN_ROQUE, 0L, defaultRemarks = "GRINDING/BLOWER FAN"),
        SeedSpec("sku_sr_pm_lactation_pellet", "PM LACTATION PELLET", PlantSection.SAN_ROQUE, 0L),
        SeedSpec("sku_sr_pm_broodsow_pellet", "PM BROODSOW PELLET", PlantSection.SAN_ROQUE, 0L),
        SeedSpec("sku_sr_pm_hog_grower_pellet", "PM HOG GROWER PELLET", PlantSection.SAN_ROQUE, 0L),
        SeedSpec("sku_sr_pm_hog_grower_mash", "PM HOG GROWER (MASH)", PlantSection.SAN_ROQUE, 0L, defaultRemarks = "GRINDING /MIXING"),
        SeedSpec("sku_sr_pm_hog_grower_fab", "PM HOG GROWER - FAB (MASH)", PlantSection.SAN_ROQUE, 0L, defaultRemarks = "GRINDING /MIXING"),
        SeedSpec("sku_sr_pm_hog_starter", "PM HOG STARTER", PlantSection.SAN_ROQUE, 0L),
        SeedSpec("sku_sr_pm_hog_starter_mash", "PM HOG STARTER (MASH)", PlantSection.SAN_ROQUE, 0L, defaultRemarks = "GRINDING /MIXING"),
        SeedSpec("sku_sr_pm_hog_starter_fab", "PM HOG STARTER - FAB (MASH)", PlantSection.SAN_ROQUE, 0L, defaultRemarks = "GRINDING /MIXING"),
        SeedSpec("sku_sr_pm_lactation_mash", "PM LACTATION MASH", PlantSection.SAN_ROQUE, 0L, defaultRemarks = "GRINDING /MIXING"),
        SeedSpec("sku_sr_pm_pre_starter", "PM PRE STARTER", PlantSection.SAN_ROQUE, 0L),
        SeedSpec("sku_sr_pm_pre_starter_moldy", "PM PRE STARTER (Moldy)", PlantSection.SAN_ROQUE, 0L),
        SeedSpec("sku_sr_red_pellet", "RED PELLET [SAN ROQUE]", PlantSection.SAN_ROQUE, 0L),
        SeedSpec("sku_sr_ruminant", "RUMINANT [SAN ROQUE]", PlantSection.SAN_ROQUE, 0L),
        SeedSpec("sku_sr_goat_lactation_3mm", "GOAT LACTATION PELLET 3MM [SAN ROQUE]", PlantSection.SAN_ROQUE, 0L),
        SeedSpec("sku_sr_mb_bio_3", "MB BIO 3", PlantSection.SAN_ROQUE, 0L),

        // PANDACAQUI — BEGINNING INVENTORY
        SeedSpec("sku_pd_barako_1000", "BARAKO 1000", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_barako_2000", "BARAKO 2000", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_barako_3mm_yel", "BARAKO 3MM YELLOW", PlantSection.PANDACAQUI, 19L),
        SeedSpec("sku_pd_barako_4mm_yel", "BARAKO 4MM YELLOW", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_barako_yel_crumble", "BARAKO YELLOW CRUMBLE", PlantSection.PANDACAQUI, 0L, defaultRemarks = "GRINDING/BLOWER FAN"),
        SeedSpec("sku_pd_barako_duck_layer", "BARAKO DUCK LAYER PELLET", PlantSection.PANDACAQUI, 196L),
        SeedSpec("sku_pd_barako_duck_pellet", "BARAKO DUCK PELLET", PlantSection.PANDACAQUI, 200L),
        SeedSpec("sku_pd_barako_pigeon_pellet", "BARAKO PIGEON PELLET", PlantSection.PANDACAQUI, 81L),
        SeedSpec("sku_pd_chookmax_bio_1000", "CHOOKMAX BIO 1000", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_chookmax_bio_2000", "CHOOKMAX BIO 2000", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_chookmax_bio_3000", "CHOOKMAX BIO 3000", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_chookmax_bio_4000", "CHOOKMAX BIO 4000", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_duck_grower", "DUCK GROWER", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_flash_pigeon_yel", "FLASH PIGEON YELLOW", PlantSection.PANDACAQUI, 50L),
        SeedSpec("sku_pd_flash_pigeon_reg", "FLASH PIGEON REGULAR [PANDACAQUI]", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_gigamax_1000", "GIGAMAX 1000 [PANDACAQUI]", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_gigamax_2000", "GIGAMAX 2000 [PANDACAQUI]", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_gigamax_3000", "GIGAMAX 3000 [PANDACAQUI]", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_mb_broodsow_pellet", "MB BROODSOW PELLET", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_mb_hpdp_grower", "MB HPDP GROWER", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_mb_hpdp_starter", "MB HPDP STARTER", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_mb_hpdp_3mm_reg", "MB HPDP 3MM REGULAR [PANDACAQUI]", PlantSection.PANDACAQUI, 199L),
        SeedSpec("sku_pd_mb_hpdp_3mm_yel", "MB HPDP 3MM YELLOW [PANDACAQUI]", PlantSection.PANDACAQUI, 150L),
        SeedSpec("sku_pd_mb_hpdp_4mm_reg", "MB HPDP 4MM REGULAR [PANDACAQUI]", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_mb_hpdp_4mm_yel", "MB HPDP 4MM YELLOW [PANDACAQUI]", PlantSection.PANDACAQUI, 20L),
        SeedSpec("sku_pd_mb_hpdp_crumble_reg", "MB HPDP CRUMBLE REGULAR", PlantSection.PANDACAQUI, 0L, defaultRemarks = "GRINDING/BLOWER FAN"),
        SeedSpec("sku_pd_mb_hpdp_yel_crumble", "MB HPDP YELLOW CRUMBLE [PANDACAQUI]", PlantSection.PANDACAQUI, 0L, defaultRemarks = "GRINDING/BLOWER FAN"),
        SeedSpec("sku_pd_pm_broodsow_sup", "PM BROODSOW SUPREME PELLET", PlantSection.PANDACAQUI, 20L),
        SeedSpec("sku_pd_pm_hog_grower_sup", "PM HOG GROWER SUPREME PELLET", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_pm_hog_starter_sup", "PM HOG STARTER SUPREME PELLET", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_pm_sow_lactation_sup", "PM SOW LACTATION SUPREME PELLET", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_red_pellet", "RED PELLET [PANDACAQUI]", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_hpdp_y3_molasses", "HPDP Y3 w/ MOLASSES", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_hpdp_y4_molasses", "HPDP Y4 w/ MOLASSES", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_goat_lactation_3mm", "GOAT LACTATION PELLET 3MM [PANDACAQUI]", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_ruminant", "RUMINANT [PANDACAQUI]", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_hpdp_bio_2_aroma", "HPDP BIO 2 W/AROMA", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_hpdp_bio_4_aroma", "HPDP BIO 4 W/AROMA", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_hpdp_bio_3_aroma", "HPDP BIO 3 W/AROMA", PlantSection.PANDACAQUI, 0L),
        SeedSpec("sku_pd_goat_starter", "GOAT STARTER", PlantSection.PANDACAQUI, 0L),

        // LA UNION QUAIL FEEDS
        SeedSpec("sku_lu_quail_layer_mash", "QUAIL LAYER MASH", PlantSection.LA_UNION_QUAIL_FEEDS, 0L, defaultRemarks = "GRINDING /MIXING"),
        SeedSpec("sku_lu_quail_starter_mash", "QUAIL STARTER MASH", PlantSection.LA_UNION_QUAIL_FEEDS, 0L, defaultRemarks = "GRINDING /MIXING"),
        SeedSpec("sku_lu_pdp_3_0", "PDP 3.0", PlantSection.LA_UNION_QUAIL_FEEDS, 0L),
        SeedSpec("sku_lu_duck_layer_pellet", "DUCK LAYER PELLET", PlantSection.LA_UNION_QUAIL_FEEDS, 0L),
        SeedSpec("sku_lu_hpdp", "HPDP", PlantSection.LA_UNION_QUAIL_FEEDS, 0L),
        SeedSpec("sku_lu_quail_crumble", "QUAIL CRUMBLE", PlantSection.LA_UNION_QUAIL_FEEDS, 0L, defaultRemarks = "GRINDING/BLOWER FAN"),

        // SELECTA
        SeedSpec("sku_sel_chookmax_1000", "CHOOKMAX 1000 [SELECTA]", PlantSection.SELECTA, 39L),
        SeedSpec("sku_sel_chookmax_2000", "CHOOKMAX 2000 [SELECTA]", PlantSection.SELECTA, 12L),
        SeedSpec("sku_sel_chookmax_3000", "CHOOKMAX 3000 [SELECTA]", PlantSection.SELECTA, 0L),
        SeedSpec("sku_sel_hpdp_3mm", "HPDP 3MM", PlantSection.SELECTA, 10L),
        SeedSpec("sku_sel_flash_pigeon", "FLASH PIGEON", PlantSection.SELECTA, 0L)
    )

    fun createSkus(uid: String): List<Pair<String, SkuEntity>> = specs.map { spec ->
        spec.id to SkuEntity(
            userId = uid,
            productName = spec.productName,
            section = spec.section.label,
            openingStockKg = spec.beginningBags * spec.bagWeightKg,
            standardBagWeightKg = spec.bagWeightKg,
            targetRecoveryPct = 98.5,
            safetyBufferPct = 15.0,
            defaultRemarks = spec.defaultRemarks
        )
    }

    fun createBatches(uid: String): List<Pair<String, ProductionBatch>> = emptyList()

    fun createDispatches(uid: String): List<Pair<String, DispatchRecord>> = emptyList()

    fun createOrders(uid: String): List<Pair<String, CustomerOrder>> = emptyList()

    fun createSchedules(uid: String): List<Pair<String, ScheduleRun>> = emptyList()

    fun createRawMaterials(uid: String): List<Pair<String, RawMaterialEntity>> =
        PlantMaterialCatalog.allSpecs.map { spec ->
            spec.idHint to spec.toEntity(uid)
        }

    fun createBoms(uid: String): List<Pair<String, FeedBomEntity>> = listOf(
        "bom_mb_broodsow_mash" to FeedBomEntity(
            userId = uid,
            productName = "MB BROODSOW MASH",
            section = PlantSection.SAN_ROQUE.label,
            batchBaseKg = 1000.0,
            yieldLossPct = 1.5,
            notes = "Standard sow gestation/lactation mash formulation (1,000 kg mix)",
            lines = listOf(
                BomIngredientLine("raw_whole_yellow_corn", "Whole Yellow Corn/Balanok Corn", RawMaterialCategory.ENERGY.label, 46.0, "Macro • Coarse hammermill grind"),
                BomIngredientLine("raw_us_soya", "US Soya (Soy Bean Meal)", RawMaterialCategory.PROTEIN.label, 20.0, "Macro • High-protein base"),
                BomIngredientLine("raw_rice_bran_d1", "Rice Bran D1", RawMaterialCategory.ENERGY.label, 18.0, "Macro • Energy carrier"),
                BomIngredientLine("raw_hard_pollard", "Hard Pollard", RawMaterialCategory.ENERGY.label, 10.5, "Macro • Fiber & binder"),
                BomIngredientLine("raw_mollases", "Mollases", RawMaterialCategory.LIQUID.label, 2.0, "Macro • Palatability"),
                BomIngredientLine("raw_mesh_100", "Mesh 100 (Powder Limestone)", RawMaterialCategory.MINERAL.label, 1.5, "Micro Fast • Calcium"),
                BomIngredientLine("raw_mdcp", "MDCP/MonodiCalcium Phosphate", RawMaterialCategory.MINERAL.label, 1.0, "Micro Slow • Phosphate"),
                BomIngredientLine("raw_iodized_salt", "Iodized Salt", RawMaterialCategory.MINERAL.label, 0.4, "Micro Slow • Electrolyte"),
                BomIngredientLine("raw_swine_vitamin_1", "Swine Vitamin 1", RawMaterialCategory.ADDITIVE.label, 0.3, "Micro Slow • Vitamin pack"),
                BomIngredientLine("raw_swine_mineral_1", "Swine Mineral 1 (Premix)", RawMaterialCategory.MINERAL.label, 0.3, "Micro Slow • Trace minerals")
            )
        ),
        "bom_barako_duck_pellet" to FeedBomEntity(
            userId = uid,
            productName = "BARAKO DUCK PELLET",
            section = PlantSection.PANDACAQUI.label,
            batchBaseKg = 1000.0,
            yieldLossPct = 1.5,
            notes = "Pelletized waterfowl formula with pollard & molasses binder",
            lines = listOf(
                BomIngredientLine("raw_whole_yellow_corn", "Whole Yellow Corn/Balanok Corn", RawMaterialCategory.ENERGY.label, 48.0, "Macro • Fine grind"),
                BomIngredientLine("raw_us_soya", "US Soya (Soy Bean Meal)", RawMaterialCategory.PROTEIN.label, 25.0, "Macro • Amino acid base"),
                BomIngredientLine("raw_rice_bran_d1", "Rice Bran D1", RawMaterialCategory.ENERGY.label, 13.0, "Macro • Energy filler"),
                BomIngredientLine("raw_soft_pollard", "Soft Pollard", RawMaterialCategory.ENERGY.label, 8.0, "Macro • Pellet conditioning"),
                BomIngredientLine("raw_mollases", "Mollases", RawMaterialCategory.LIQUID.label, 2.5, "Macro • Steam pellet binding"),
                BomIngredientLine("raw_mesh_45", "Mesh 45 (Fine Limestone - PCDLY)", RawMaterialCategory.MINERAL.label, 2.0, "Micro Fast • Shell calcium"),
                BomIngredientLine("raw_mdcp", "MDCP/MonodiCalcium Phosphate", RawMaterialCategory.MINERAL.label, 0.8, "Micro Slow • Phosphate"),
                BomIngredientLine("raw_choline_chloride", "Choline Chloride", RawMaterialCategory.ADDITIVE.label, 0.3, "Micro Fast • Hepatic support"),
                BomIngredientLine("raw_iodized_salt", "Iodized Salt", RawMaterialCategory.MINERAL.label, 0.4, "Micro Slow • Mineral balance")
            )
        ),
        "bom_chookmax_1000_sr" to FeedBomEntity(
            userId = uid,
            productName = "CHOOKMAX 1000 [SAN ROQUE]",
            section = PlantSection.SAN_ROQUE.label,
            batchBaseKg = 1000.0,
            yieldLossPct = 1.2,
            notes = "Broiler booster / starter high-digestibility formula",
            lines = listOf(
                BomIngredientLine("raw_whole_yellow_corn", "Whole Yellow Corn/Balanok Corn", RawMaterialCategory.ENERGY.label, 54.0, "Macro • Screened yellow corn"),
                BomIngredientLine("raw_us_soya", "US Soya (Soy Bean Meal)", RawMaterialCategory.PROTEIN.label, 30.0, "Macro • High-CP SBM"),
                BomIngredientLine("raw_rice_bran_d1", "Rice Bran D1", RawMaterialCategory.ENERGY.label, 8.0, "Macro • Digestible D1 bran"),
                BomIngredientLine("raw_cookie_meal", "Cookie Meal", RawMaterialCategory.ENERGY.label, 4.0, "Macro • Palatable energy"),
                BomIngredientLine("raw_mesh_100", "Mesh 100 (Powder Limestone)", RawMaterialCategory.MINERAL.label, 1.5, "Micro Fast • Bone mineral"),
                BomIngredientLine("raw_mdcp", "MDCP/MonodiCalcium Phosphate", RawMaterialCategory.MINERAL.label, 1.2, "Micro Slow • Starter phosphate"),
                BomIngredientLine("raw_l_lysine_hci", "L-Lysine HCI", RawMaterialCategory.ADDITIVE.label, 0.3, "Micro Slow • Amino acid"),
                BomIngredientLine("raw_dl_methionine", "DL-Methionine", RawMaterialCategory.ADDITIVE.label, 0.2, "Micro Slow • Amino acid"),
                BomIngredientLine("raw_choline_chloride", "Choline Chloride", RawMaterialCategory.ADDITIVE.label, 0.3, "Micro Fast • Premix"),
                BomIngredientLine("raw_iodized_salt", "Iodized Salt", RawMaterialCategory.MINERAL.label, 0.5, "Micro Slow • Salt")
            )
        ),
        "bom_quail_layer_mash" to FeedBomEntity(
            userId = uid,
            productName = "QUAIL LAYER MASH",
            section = PlantSection.LA_UNION_QUAIL_FEEDS.label,
            batchBaseKg = 1000.0,
            yieldLossPct = 1.5,
            notes = "High-calcium quail laying mash",
            lines = listOf(
                BomIngredientLine("raw_whole_yellow_corn", "Whole Yellow Corn/Balanok Corn", RawMaterialCategory.ENERGY.label, 48.0, "Macro • Fine hammermill mesh"),
                BomIngredientLine("raw_us_soya", "US Soya (Soy Bean Meal)", RawMaterialCategory.PROTEIN.label, 27.0, "Macro • Layer CP target"),
                BomIngredientLine("raw_rice_bran_d1", "Rice Bran D1", RawMaterialCategory.ENERGY.label, 11.0, "Macro • Energy carrier"),
                BomIngredientLine("raw_corn_bran", "Corn Bran", RawMaterialCategory.ENERGY.label, 6.0, "Macro • Mash texture"),
                BomIngredientLine("raw_mesh_45", "Mesh 45 (Fine Limestone - PCDLY)", RawMaterialCategory.MINERAL.label, 5.5, "Micro Fast • Eggshell calcium"),
                BomIngredientLine("raw_mdcp", "MDCP/MonodiCalcium Phosphate", RawMaterialCategory.MINERAL.label, 1.2, "Micro Slow • Layer phosphate"),
                BomIngredientLine("raw_yellow_dye", "Yellow Dye / Ferna Egg Yellow", RawMaterialCategory.ADDITIVE.label, 0.1, "Micro Fast • Yolk pigment"),
                BomIngredientLine("raw_luctarom_premix", "Luctarom (PREMIX)", RawMaterialCategory.ADDITIVE.label, 0.2, "Micro Fast • Flavor"),
                BomIngredientLine("raw_swine_vitamin_1", "Swine Vitamin 1", RawMaterialCategory.ADDITIVE.label, 0.5, "Micro Slow • Vitamin support"),
                BomIngredientLine("raw_iodized_salt", "Iodized Salt", RawMaterialCategory.MINERAL.label, 0.5, "Micro Slow • Salt")
            )
        )
    )
}
