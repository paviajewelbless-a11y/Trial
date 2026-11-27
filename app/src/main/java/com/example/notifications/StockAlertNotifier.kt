package com.example.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.OperationsCalculator
import com.example.model.RawMaterialProjection
import com.example.model.RawMaterialRiskStatus

object StockAlertNotifier {

    const val CHANNEL_ID = "raw_material_alerts"
    private const val CHANNEL_NAME = "Raw Material Low-Stock Alerts"
    private const val NOTIFICATION_ID_LOW_STOCK = 1042

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Proactive warnings when historical usage shows raw materials are projected to run low."
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun notifyLowRawMaterials(
        context: Context,
        projections: List<RawMaterialProjection>,
        isManualTrigger: Boolean = false
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannel(context)

        val urgent = projections.filter {
            it.material.alertsEnabled &&
                (it.status == RawMaterialRiskStatus.CRITICAL || it.status == RawMaterialRiskStatus.LOW_WATCH)
        }
        if (urgent.isEmpty() && !isManualTrigger) return false

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            setPackage(context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_RAW_MATERIALS, true)
        }
        val pendingIntent = try {
            PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } catch (_: RuntimeException) {
            null
        }

        val criticalCount = urgent.count { it.status == RawMaterialRiskStatus.CRITICAL }
        val watchCount = urgent.count { it.status == RawMaterialRiskStatus.LOW_WATCH }

        val title = when {
            criticalCount > 0 -> "$criticalCount Raw Material${if (criticalCount == 1) "" else "s"} Critical"
            watchCount > 0 -> "$watchCount Raw Material${if (watchCount == 1) "" else "s"} Running Low"
            else -> "All Raw Materials Healthy"
        }

        val topSummary = if (urgent.isEmpty()) {
            "Current raw material stocks are above projected lead-time minimums."
        } else {
            urgent.take(3).joinToString(" • ") { proj ->
                val daysText = if (proj.daysUntilRunOut.isFinite()) {
                    "${String.format("%.1f", proj.daysUntilRunOut)}d left"
                } else {
                    "low stock"
                }
                "${proj.material.name} ($daysText)"
            }
        }

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText("Based on historical mill consumption")
        if (urgent.isEmpty()) {
            inboxStyle.addLine(topSummary)
        } else {
            urgent.take(6).forEach { proj ->
                val orderHint = if (proj.recommendedRestockKg > 0.0) {
                    "Order +${OperationsCalculator.formatKg(proj.recommendedRestockKg)} kg"
                } else {
                    "Inspect level"
                }
                inboxStyle.addLine("${proj.material.name}: ${proj.projectedRunOutLabel} • $orderHint")
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_mill)
            .setContentTitle(title)
            .setContentText(topSummary)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .apply {
                if (pendingIntent != null) {
                    setContentIntent(pendingIntent)
                }
            }

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_LOW_STOCK, builder.build())
            true
        } catch (_: SecurityException) {
            false
        } catch (_: RuntimeException) {
            false
        }
    }

    const val EXTRA_OPEN_RAW_MATERIALS = "extra_open_raw_materials"
}
