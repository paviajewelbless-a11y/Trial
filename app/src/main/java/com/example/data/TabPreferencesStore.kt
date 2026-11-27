package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.PlantSection
import com.example.model.SkuEntity
import com.example.model.SkuInventorySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.plantUiDataStore: DataStore<Preferences> by preferencesDataStore(name = "plant_ui_prefs")

object TabPreferencesStore {
    private val KEY_BOTTOM_TABS = stringPreferencesKey("bottom_nav_tab_names")
    private val KEY_HIDE_NON_PRIORITY_PRODUCTS = booleanPreferencesKey("hide_non_priority_products")
    private val KEY_PRIORITY_CUSTOMIZED = booleanPreferencesKey("priority_products_customized")
    private val KEY_PRIORITY_PRODUCT_KEYS = stringSetPreferencesKey("priority_product_keys")
    private val KEY_PRESELECTED_DISPATCH_PLANT = stringPreferencesKey("preselected_dispatch_plant")

    data class PriorityProductPrefs(
        val hideNonPriority: Boolean = true,
        val isCustomized: Boolean = false,
        val selectedKeys: Set<String> = emptySet()
    )

    val DEFAULT_BOTTOM_TAB_NAMES: List<String> = listOf(
        "SUMMARY",
        "MONITOR",
        "ORDERS",
        "DISPATCHED"
    )

    const val MAX_PINNED_BOTTOM_TABS = 4
    const val MIN_PINNED_BOTTOM_TABS = 1

    fun skuPriorityKey(sku: SkuEntity): String =
        skuPriorityKey(sku.section, sku.productName)

    fun skuPriorityKey(section: String, productName: String): String =
        "${section.trim().lowercase()}|${productName.trim().lowercase()}"

    fun isAutoActivePriority(summary: SkuInventorySummary): Boolean =
        summary.availableStockKg > 0.0 ||
            summary.openOrdersKg > 0.0 ||
            summary.dispatchedKg > 0.0 ||
            summary.recoveredKg > 0.0 ||
            summary.netProductionNeededKg > 0.0

    fun observeBottomTabNames(context: Context): Flow<List<String>> =
        context.applicationContext.plantUiDataStore.data.map { prefs ->
            val raw = prefs[KEY_BOTTOM_TABS]
            if (raw.isNullOrBlank()) {
                DEFAULT_BOTTOM_TAB_NAMES
            } else {
                val parsed = raw.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .take(MAX_PINNED_BOTTOM_TABS)
                parsed.ifEmpty { DEFAULT_BOTTOM_TAB_NAMES }
            }
        }

    suspend fun saveBottomTabNames(context: Context, tabNames: List<String>) {
        val cleaned = tabNames
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(MAX_PINNED_BOTTOM_TABS)
            .ifEmpty { DEFAULT_BOTTOM_TAB_NAMES }
        context.applicationContext.plantUiDataStore.edit { prefs ->
            prefs[KEY_BOTTOM_TABS] = cleaned.joinToString(",")
        }
    }

    suspend fun resetBottomTabNames(context: Context) {
        context.applicationContext.plantUiDataStore.edit { prefs ->
            prefs.remove(KEY_BOTTOM_TABS)
        }
    }

    fun observePriorityProductPrefs(context: Context): Flow<PriorityProductPrefs> =
        context.applicationContext.plantUiDataStore.data.map { prefs ->
            PriorityProductPrefs(
                hideNonPriority = prefs[KEY_HIDE_NON_PRIORITY_PRODUCTS] ?: true,
                isCustomized = prefs[KEY_PRIORITY_CUSTOMIZED] ?: false,
                selectedKeys = prefs[KEY_PRIORITY_PRODUCT_KEYS] ?: emptySet()
            )
        }

    suspend fun setHideNonPriorityProducts(context: Context, hide: Boolean) {
        context.applicationContext.plantUiDataStore.edit { prefs ->
            prefs[KEY_HIDE_NON_PRIORITY_PRODUCTS] = hide
        }
    }

    suspend fun savePriorityProductKeys(context: Context, keys: Set<String>) {
        context.applicationContext.plantUiDataStore.edit { prefs ->
            prefs[KEY_PRIORITY_CUSTOMIZED] = true
            prefs[KEY_PRIORITY_PRODUCT_KEYS] = keys
        }
    }

    suspend fun resetPriorityProductsToAutoActive(context: Context) {
        context.applicationContext.plantUiDataStore.edit { prefs ->
            prefs[KEY_PRIORITY_CUSTOMIZED] = false
            prefs.remove(KEY_PRIORITY_PRODUCT_KEYS)
            prefs[KEY_HIDE_NON_PRIORITY_PRODUCTS] = true
        }
    }

    fun normalizeDispatchPlantLabel(raw: String): String {
        val cleaned = raw.trim()
        if (cleaned.isEmpty() || cleaned.equals("ALL", ignoreCase = true)) return ""
        return PlantSection.entries.firstOrNull {
            it.label.equals(cleaned, ignoreCase = true) || it.name.equals(cleaned, ignoreCase = true)
        }?.label ?: ""
    }

    fun observePreselectedDispatchPlant(context: Context): Flow<String> =
        context.applicationContext.plantUiDataStore.data.map { prefs ->
            normalizeDispatchPlantLabel(prefs[KEY_PRESELECTED_DISPATCH_PLANT].orEmpty())
        }

    suspend fun savePreselectedDispatchPlant(context: Context, plantLabel: String) {
        val normalized = normalizeDispatchPlantLabel(plantLabel)
        context.applicationContext.plantUiDataStore.edit { prefs ->
            if (normalized.isEmpty()) {
                prefs.remove(KEY_PRESELECTED_DISPATCH_PLANT)
            } else {
                prefs[KEY_PRESELECTED_DISPATCH_PLANT] = normalized
            }
        }
    }

    private fun selectedWorkspaceKey(signedInUid: String) =
        stringPreferencesKey("selected_workspace_owner_${signedInUid.trim()}")

    fun observeSelectedWorkspaceOwnerId(context: Context, signedInUid: String): Flow<String> =
        context.applicationContext.plantUiDataStore.data.map { prefs ->
            prefs[selectedWorkspaceKey(signedInUid)].orEmpty().trim()
        }

    suspend fun saveSelectedWorkspaceOwnerId(
        context: Context,
        signedInUid: String,
        workspaceOwnerId: String
    ) {
        if (signedInUid.isBlank()) return
        val cleaned = workspaceOwnerId.trim()
        context.applicationContext.plantUiDataStore.edit { prefs ->
            if (cleaned.isEmpty()) {
                prefs.remove(selectedWorkspaceKey(signedInUid))
            } else {
                prefs[selectedWorkspaceKey(signedInUid)] = cleaned
            }
        }
    }
}
