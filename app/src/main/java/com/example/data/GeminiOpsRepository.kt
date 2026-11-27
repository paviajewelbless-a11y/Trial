package com.example.data

import android.util.Log
import com.example.domain.OperationsCalculator
import com.example.model.AiPlantInsight
import com.example.model.PlantKpis
import com.example.model.ProductionBatch
import com.example.model.ScheduleRun
import com.example.model.SkuInventorySummary
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiOpsRepo"

class GeminiOpsRepository {

    private val insightSchema = Schema.obj(
        properties = mapOf(
            "executiveSummary" to Schema.string(
                description = "2-3 sentence operational summary of Bulacan Plant inventory, recovery, and dispatch health."
            ),
            "stockoutAlerts" to Schema.array(
                items = Schema.string(),
                description = "Top urgent SKU stockout or open-order shortfall alerts with exact kg/bags."
            ),
            "recoveryWarnings" to Schema.array(
                items = Schema.string(),
                description = "Production batches or SKUs showing recovery loss below 98.5% or notable variance."
            ),
            "shiftRecommendations" to Schema.array(
                items = Schema.string(),
                description = "Actionable next-shift production scheduling recommendations naming line remarks (MIXING/CONVEYOR, GRINDING /MIXING, etc.)."
            )
        )
    )

    private val insightModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = insightSchema
            }
        )
    }

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    You are the Senior FeedMill Production & Inventory Operations Planner for 10K Agri Feeds – Bulacan Plant.
                    You advise plant supervisors on inventory cover, customer order fulfillment, dispatch velocity,
                    production recovery variance across SAN ROQUE, PANDACAQUI, LA UNION QUAIL FEEDS, and SELECTA lines,
                    and shift scheduling (MIXING/CONVEYOR, GRINDING /MIXING, GRINDING/BLOWER FAN, GRINDING/BLOWER FAN/HAMMERMILL).
                    Be concise, quantitative, practical, and ground every answer in the live Bulacan Plant data snapshot provided.
                    Use kg and bags accurately. Never invent metrics not in the snapshot.
                    """.trimIndent()
                )
            }
        )
    }

    private var activeChat: Chat? = null

    fun resetChat() {
        activeChat = null
    }

    suspend fun analyzePlantOperations(
        kpis: PlantKpis,
        summaries: List<SkuInventorySummary>,
        batches: List<ProductionBatch>,
        schedules: List<ScheduleRun>
    ): Result<AiPlantInsight> {
        return try {
            val contextText = buildPlantContextSnapshot(kpis, summaries, batches, schedules)
            val prompt = """
                Analyze this live 10K Agri Feeds – Bulacan Plant snapshot and produce a structured operational assessment:
                $contextText
            """.trimIndent()

            val response = withTimeout(60_000L) {
                insightModel.generateContent(prompt)
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("Gemini returned an empty response."))
            val json = JSONObject(rawJson)

            fun readStringList(key: String): List<String> {
                val arr = json.optJSONArray(key) ?: return emptyList()
                return (0 until arr.length()).mapNotNull { idx ->
                    arr.optString(idx).takeIf { it.isNotBlank() }
                }
            }

            Result.success(
                AiPlantInsight(
                    executiveSummary = json.optString("executiveSummary", "Analysis complete."),
                    stockoutAlerts = readStringList("stockoutAlerts"),
                    recoveryWarnings = readStringList("recoveryWarnings"),
                    shiftRecommendations = readStringList("shiftRecommendations")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Plant analysis timed out", e)
            Result.failure(IllegalStateException("Gemini analysis timed out after 60s. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Plant analysis failed", e)
            Result.failure(e)
        }
    }

    fun sendChatMessageStream(
        userMessage: String,
        kpis: PlantKpis,
        summaries: List<SkuInventorySummary>,
        batches: List<ProductionBatch>,
        schedules: List<ScheduleRun>
    ): Flow<String> = flow {
        val chat = activeChat ?: chatModel.startChat().also { activeChat = it }
        val snapshot = buildPlantContextSnapshot(kpis, summaries, batches, schedules)
        val prompt = """
            [LIVE BULACAN PLANT SNAPSHOT]
            $snapshot

            [OPERATOR QUESTION]
            $userMessage
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            chat.sendMessageStream(prompt).collect { chunk ->
                val piece = chunk.text.orEmpty()
                if (piece.isNotEmpty()) {
                    accumulated.append(piece)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Gemini returned an empty response.")
        }
    }

    fun buildPlantContextSnapshot(
        kpis: PlantKpis,
        summaries: List<SkuInventorySummary>,
        batches: List<ProductionBatch>,
        schedules: List<ScheduleRun>
    ): String = buildString {
        appendLine("KPIs: TotalAvail=${OperationsCalculator.formatKg(kpis.totalAvailableStockKg)}kg (${OperationsCalculator.formatBags(kpis.totalAvailableBags)} bags), TotalDispatched=${OperationsCalculator.formatKg(kpis.totalDispatchedKg)}kg, OpenOrders=${OperationsCalculator.formatKg(kpis.totalOpenOrdersKg)}kg, AvgRecovery=${String.format("%.2f%%", kpis.averageRecoveryPct)}, SKUsNeedingProd=${kpis.skusNeedingProduction}, CriticalSKUs=${kpis.criticalSkuCount}")
        appendLine("Top Stocked & Critical SKUs:")
        val highlighted = (summaries.filter { it.availableStockKg > 0.0 }.take(8) +
            summaries.filter { it.availableStockKg <= 0.0 }.take(6)).distinctBy { it.sku.productName }
        highlighted.forEach { s ->
            val cover = if (s.daysOfCover.isInfinite()) "No recent dispatch" else "${String.format("%.1f", s.daysOfCover)}d"
            appendLine("- ${s.sku.productName} [${s.sku.section}]: Avail=${OperationsCalculator.formatKg(s.availableStockKg)}kg (${OperationsCalculator.formatBags(s.availableBags)} bags), OpenOrd=${OperationsCalculator.formatKg(s.openOrdersKg)}kg, AvgDisp=${OperationsCalculator.formatKg(s.avgDailyDispatchKg)}kg/d, Cover=$cover, NetNeeded=${OperationsCalculator.formatKg(s.netProductionNeededKg)}kg (${s.suggestedBags} bags), Status=${s.status.badgeText}, Line=${s.sku.defaultRemarks}")
        }
        val lowRecBatches = batches.filter { it.recoveryPct < 98.5 }
        appendLine("Low Recovery Batches (<98.5%): ${if (lowRecBatches.isEmpty()) "None" else lowRecBatches.joinToString("; ") { "${it.date} ${it.productName}: ${String.format("%.2f%%", it.recoveryPct)} (${OperationsCalculator.formatKg(it.varianceKg)}kg)" }}")
        appendLine("Active Scheduled Queue (${schedules.count { it.status != "Completed" }}):")
        schedules.filter { it.status != "Completed" }.take(6).forEach { sc ->
            appendLine("- ${sc.scheduledDate} | ${sc.productName} | ${sc.targetBags} bags (${OperationsCalculator.formatKg(sc.expectedWeightKg)}kg) | ${sc.lineRemarks} | ${sc.priority} | ${sc.status}")
        }
    }
}
