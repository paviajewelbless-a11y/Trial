package com.example.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.model.WorkspaceInvite

object WorkspaceInviteNotifier {

    const val CHANNEL_ID = "workspace_invites"
    private const val CHANNEL_NAME = "Workspace Team Invitations"
    private const val NOTIFICATION_ID_INVITES = 1043
    const val EXTRA_OPEN_TEAM = "extra_open_team"

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
                    description = "Alerts when you are invited to collaborate on a Bulacan Plant workspace."
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun notifyPendingInvites(
        context: Context,
        pendingInvites: List<WorkspaceInvite>
    ): Boolean {
        if (pendingInvites.isEmpty()) return false
        if (!StockAlertNotifier.hasNotificationPermission(context)) return false
        ensureChannel(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            setPackage(context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TEAM, true)
        }
        val pendingIntent = try {
            PendingIntent.getActivity(
                context,
                1,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } catch (_: RuntimeException) {
            null
        }

        val first = pendingInvites.first()
        val workspaceName = first.workspaceName.ifBlank { "Bulacan Plant" }
        val inviter = first.invitedByEmail.ifBlank { first.ownerEmail }.ifBlank { "an Admin" }

        val title = if (pendingInvites.size == 1) {
            "Workspace Invitation: $workspaceName"
        } else {
            "${pendingInvites.size} Pending Workspace Invitations"
        }

        val summaryText = if (pendingInvites.size == 1) {
            "$inviter invited you as ${first.resolvedRole().label}. Tap to accept."
        } else {
            "Invites waiting from $inviter and ${pendingInvites.size - 1} more. Tap to review."
        }

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText("FeedMill Ops Team Access")
        pendingInvites.take(5).forEach { inv ->
            val from = inv.invitedByEmail.ifBlank { inv.ownerEmail }.ifBlank { "Admin" }
            val ws = inv.workspaceName.ifBlank { "Bulacan Plant" }
            inboxStyle.addLine("$ws (${inv.resolvedRole().label}) — from $from")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_mill)
            .setContentTitle(title)
            .setContentText(summaryText)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .apply {
                if (pendingIntent != null) {
                    setContentIntent(pendingIntent)
                }
            }

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_INVITES, builder.build())
            true
        } catch (_: SecurityException) {
            false
        } catch (_: RuntimeException) {
            false
        }
    }
}
