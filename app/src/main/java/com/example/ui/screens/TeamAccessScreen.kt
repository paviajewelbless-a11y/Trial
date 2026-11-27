package com.example.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.SharedWorkspaceOption
import com.example.model.WorkspaceInvite
import com.example.model.WorkspaceInviteStatus
import com.example.model.WorkspaceMember
import com.example.model.WorkspaceRole
import com.example.ui.components.copyReportTextToClipboard
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.ForestEmeraldDark
import com.example.ui.theme.GrainAmber
import com.example.ui.theme.SlateTeal
import com.example.ui.theme.StatusInProgress

private const val GMAIL_PACKAGE = "com.google.android.gm"
const val APP_SHARED_WEB_URL = "https://ais-pre-2k6iew2ypwev3yt7m6gfru-687226281361.asia-southeast1.run.app"
const val WEB_PORTAL_URL = "https://gen-lang-client-0293694787.web.app"

enum class InviteDeliveryMode(
    val title: String,
    val subtitle: String
) {
    GOOGLE_AND_EMAIL(
        title = "Send to Google + Compose Email",
        subtitle = "Auto-sends invite to their Google account in the app and opens Gmail / email pre-filled with an invite link."
    ),
    GOOGLE_ONLY(
        title = "Send to Google Account Only",
        subtitle = "Auto-sends invite directly to their Google account in FeedMill Ops without opening an email app."
    )
}

private data class RecentInviteEmailDraft(
    val email: String,
    val displayName: String,
    val role: WorkspaceRole,
    val customNote: String = "",
    val workspaceOwnerId: String = "",
    val inviteId: String = ""
)

private fun urlEncodeQueryParam(value: String): String =
    java.net.URLEncoder.encode(value, Charsets.UTF_8.name())

fun buildWorkspaceInviteLink(
    workspaceOwnerId: String = "",
    recipientEmail: String = "",
    role: WorkspaceRole = WorkspaceRole.OPERATOR,
    inviteId: String = ""
): String {
    val cleanWorkspace = workspaceOwnerId.trim()
    val cleanEmail = recipientEmail.trim().lowercase()
    val cleanInviteId = inviteId.trim()
    val params = buildList {
        add("action=team_invite")
        if (cleanInviteId.isNotEmpty()) add("invite=${urlEncodeQueryParam(cleanInviteId)}")
        if (cleanWorkspace.isNotEmpty()) add("workspace=${urlEncodeQueryParam(cleanWorkspace)}")
        if (cleanEmail.isNotEmpty()) add("email=${urlEncodeQueryParam(cleanEmail)}")
        add("role=${urlEncodeQueryParam(role.label)}")
    }.joinToString("&")
    return "$APP_SHARED_WEB_URL/?$params"
}

private fun isGmailInstalled(context: Context): Boolean = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(GMAIL_PACKAGE, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(GMAIL_PACKAGE, 0)
    }
    true
} catch (_: Exception) {
    false
}

fun buildWorkspaceInviteEmailContent(
    recipientEmail: String,
    recipientName: String,
    role: WorkspaceRole,
    inviterEmail: String,
    workspaceName: String = "Bulacan Plant",
    customNote: String = "",
    workspaceOwnerId: String = "",
    inviteId: String = ""
): Pair<String, String> {
    val cleanRecipient = recipientEmail.trim().lowercase()
    val greetingName = recipientName.trim().ifBlank { cleanRecipient.substringBefore('@') }
    val inviter = inviterEmail.trim().ifBlank { "a workspace administrator" }
    val note = customNote.trim()
    val inviteLink = buildWorkspaceInviteLink(
        workspaceOwnerId = workspaceOwnerId,
        recipientEmail = cleanRecipient,
        role = role,
        inviteId = inviteId
    )
    val subject = "Invitation to join $workspaceName (FeedMill Ops) as ${role.label}"
    val body = buildString {
        appendLine("Hi $greetingName,")
        appendLine()
        appendLine("$inviter invited you to collaborate on the $workspaceName workspace in FeedMill Ops.")
        if (note.isNotEmpty()) {
            appendLine()
            appendLine("Note: $note")
        }
        appendLine()
        appendLine("Assigned Role: ${role.label}")
        appendLine("Permissions: ${role.summary}")
        appendLine()
        appendLine("Open your invitation link:")
        appendLine(inviteLink)
        appendLine()
        appendLine("How to accept your invite:")
        appendLine("1. Tap the invite link above (or open the FeedMill Ops app).")
        appendLine("2. Sign in with Google using this exact email: $cleanRecipient")
        appendLine("3. Tap \"Accept & Switch\" on the invitation banner (or go to More -> Team).")
    }.trim()
    return subject to body
}

fun launchWorkspaceInviteEmail(
    context: Context,
    recipientEmail: String,
    recipientName: String,
    role: WorkspaceRole,
    inviterEmail: String,
    customNote: String = "",
    workspaceOwnerId: String = "",
    inviteId: String = "",
    onStatusMessage: (String) -> Unit
): Boolean {
    val cleanRecipient = recipientEmail.trim().lowercase()
    val (subject, body) = buildWorkspaceInviteEmailContent(
        recipientEmail = cleanRecipient,
        recipientName = recipientName,
        role = role,
        inviterEmail = inviterEmail,
        customNote = customNote,
        workspaceOwnerId = workspaceOwnerId,
        inviteId = inviteId
    )
    val baseMailtoIntent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", cleanRecipient, null)).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(cleanRecipient))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    if (isGmailInstalled(context)) {
        val gmailIntent = Intent(baseMailtoIntent).apply {
            setPackage(GMAIL_PACKAGE)
        }
        try {
            context.startActivity(gmailIntent)
            onStatusMessage("Opened Gmail with invite link for $cleanRecipient.")
            return true
        } catch (_: Exception) {
            // Fall through to generic mailto handler
        }
    }

    try {
        context.startActivity(baseMailtoIntent)
        onStatusMessage("Opened email app to invite $cleanRecipient.")
        return true
    } catch (_: ActivityNotFoundException) {
        // Fall through to share sheet / clipboard
    } catch (_: RuntimeException) {
        // Fall through to share sheet / clipboard
    }

    val copied = copyReportTextToClipboard(context, subject, body)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_EMAIL, arrayOf(cleanRecipient))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    val chooser = Intent.createChooser(shareIntent, "Send Invite to $cleanRecipient").apply {
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    return try {
        context.startActivity(chooser)
        onStatusMessage(
            if (copied) "No email app found — share sheet opened and invite copied."
            else "No email app found — choose an app to send the invite."
        )
        true
    } catch (_: Exception) {
        onStatusMessage(
            if (copied) "Invite sent to Google account. No email app installed — invite copied to clipboard."
            else "Invite sent to Google account, but no email app was found on this device."
        )
        false
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeamAccessScreen(
    signedInUid: String,
    signedInEmail: String,
    activeWorkspaceOwnerId: String,
    activeRole: WorkspaceRole,
    isWorkspaceOwner: Boolean,
    availableWorkspaces: List<SharedWorkspaceOption>,
    pendingInvitesForMe: List<WorkspaceInvite>,
    teamMembers: List<WorkspaceMember>,
    workspaceInvites: List<WorkspaceInvite>,
    onSwitchWorkspace: (String) -> Unit,
    onAcceptInvite: (WorkspaceInvite) -> Unit,
    onDeclineInvite: (WorkspaceInvite) -> Unit,
    onLeaveActiveSharedWorkspace: () -> Unit,
    onInviteMember: (email: String, displayName: String, role: WorkspaceRole, onSaved: (String) -> Unit) -> Unit,
    onUpdateMemberRole: (memberUid: String, inviteId: String, memberEmail: String, newRole: WorkspaceRole) -> Unit,
    onRemoveMember: (memberUid: String, inviteId: String, memberEmail: String) -> Unit,
    onShowMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var showInviteDialog by remember { mutableStateOf(false) }
    var memberPendingRemoval by remember { mutableStateOf<Triple<String, String, String>?>(null) } // memberUid, inviteId, email
    var confirmLeaveShared by remember { mutableStateOf(false) }
    var recentInviteDraft by remember { mutableStateOf<RecentInviteEmailDraft?>(null) }

    val activeOption = remember(availableWorkspaces, activeWorkspaceOwnerId) {
        availableWorkspaces.firstOrNull { it.workspaceOwnerId == activeWorkspaceOwnerId }
    }
    val pendingTeamInvites = remember(workspaceInvites) {
        workspaceInvites.filter { it.resolvedStatus() == WorkspaceInviteStatus.PENDING }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 980.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header Banner
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(ForestEmerald, ForestEmeraldDark)
                                )
                            )
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color.White.copy(alpha = 0.14f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Group,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Team & Shared Access",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isWorkspaceOwner) {
                                            "Bulacan Plant • Owner ($signedInEmail)"
                                        } else {
                                            "Shared Plant • Owner: ${activeOption?.ownerEmail?.ifBlank { activeWorkspaceOwnerId } ?: activeWorkspaceOwnerId}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            RoleBadgeChip(role = activeRole, onDarkSurface = true)
                        }

                        Text(
                            text = activeRole.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (activeRole.canManageTeam) {
                                Button(
                                    onClick = { showInviteDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = ForestEmerald
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("invite_team_member_button")
                                ) {
                                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Invite Team Member", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Surface(
                                    color = Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Lock,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Only Admins can invite or edit team roles",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            if (!isWorkspaceOwner) {
                                OutlinedButton(
                                    onClick = { confirmLeaveShared = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("leave_shared_workspace_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Leave Workspace")
                                }
                            }
                        }
                    }
                }
            }

            recentInviteDraft?.let { draft ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recent_invite_email_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MailOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Invite Sent to ${draft.email}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                TextButton(onClick = { recentInviteDraft = null }) {
                                    Text("Dismiss")
                                }
                            }
                            Text(
                                text = "Google account access (${draft.role.label}) is live in FeedMill Ops. Compose a Gmail/email anytime to notify them.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        launchWorkspaceInviteEmail(
                                            context = context,
                                            recipientEmail = draft.email,
                                            recipientName = draft.displayName,
                                            role = draft.role,
                                            inviterEmail = signedInEmail,
                                            customNote = draft.customNote,
                                            workspaceOwnerId = draft.workspaceOwnerId,
                                            inviteId = draft.inviteId,
                                            onStatusMessage = onShowMessage
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("recent_invite_open_email_button")
                                ) {
                                    Icon(Icons.Filled.MailOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Compose Email")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val inviteLink = buildWorkspaceInviteLink(
                                            workspaceOwnerId = draft.workspaceOwnerId,
                                            recipientEmail = draft.email,
                                            role = draft.role,
                                            inviteId = draft.inviteId
                                        )
                                        val copied = copyReportTextToClipboard(
                                            context,
                                            "FeedMill Ops Invite Link",
                                            inviteLink
                                        )
                                        onShowMessage(
                                            if (copied) "Invite link copied for ${draft.email}."
                                            else "Unable to copy invite link."
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("recent_invite_copy_link_button")
                                ) {
                                    Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy Link")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val (subject, body) = buildWorkspaceInviteEmailContent(
                                            recipientEmail = draft.email,
                                            recipientName = draft.displayName,
                                            role = draft.role,
                                            inviterEmail = signedInEmail,
                                            customNote = draft.customNote,
                                            workspaceOwnerId = draft.workspaceOwnerId,
                                            inviteId = draft.inviteId
                                        )
                                        val copied = copyReportTextToClipboard(context, subject, body)
                                        onShowMessage(
                                            if (copied) "Invite message copied for ${draft.email}."
                                            else "Unable to copy invite message."
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("recent_invite_copy_button")
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy Message")
                                }
                            }
                        }
                    }
                }
            }

            // 2. Pending Invitations Addressed to Signed-In User
            if (pendingInvitesForMe.isNotEmpty()) {
                item {
                    Text(
                        text = "Pending Invitations (${pendingInvitesForMe.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(pendingInvitesForMe, key = { it.id }) { invite ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MailOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = invite.workspaceName.ifBlank { "Bulacan Plant" },
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "Invited by ${invite.invitedByEmail.ifBlank { invite.ownerEmail }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                RoleBadgeChip(role = invite.resolvedRole())
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { onAcceptInvite(invite) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("accept_invite_button")
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Accept & Switch")
                                }
                                OutlinedButton(
                                    onClick = { onDeclineInvite(invite) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("decline_invite_button")
                                ) {
                                    Text("Decline")
                                }
                            }
                        }
                    }
                }
            }

            // 3. Workspace Switcher
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SwapHoriz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Plant Workspaces (${availableWorkspaces.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "Switch between your own workspace and any Bulacan Plant shared with you.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        availableWorkspaces.forEach { option ->
                            val isSelected = option.workspaceOwnerId == activeWorkspaceOwnerId
                            Surface(
                                onClick = { onSwitchWorkspace(option.workspaceOwnerId) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("workspace_option_${option.workspaceOwnerId}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                                        contentDescription = if (isSelected) "Active workspace" else "Select workspace",
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (option.isPersonalOwner) {
                                                "My Bulacan Plant Workspace"
                                            } else {
                                                "Shared: ${option.workspaceName}"
                                            },
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = if (option.isPersonalOwner) {
                                                "Owner • $signedInEmail"
                                            } else {
                                                "Shared by ${option.ownerEmail.ifBlank { option.workspaceOwnerId }}"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    RoleBadgeChip(role = option.role)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Active Team Members Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Workspace Team Members (${teamMembers.size + 1})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Everyone collaborating on this Bulacan Plant workspace in real time",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Workspace Owner row
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(ForestEmerald.copy(alpha = 0.14f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.VerifiedUser,
                                            contentDescription = null,
                                            tint = ForestEmerald,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isWorkspaceOwner) "You (Workspace Owner)" else "Workspace Owner",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = when {
                                                isWorkspaceOwner -> signedInEmail
                                                !activeOption?.ownerEmail.isNullOrBlank() -> activeOption?.ownerEmail.orEmpty()
                                                else -> activeWorkspaceOwnerId
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                RoleBadgeChip(role = WorkspaceRole.ADMIN, labelOverride = "Owner")
                            }
                        }

                        if (teamMembers.isEmpty()) {
                            Text(
                                text = "No collaborators have joined yet. Tap \"Invite Team Member\" to share access by Google email.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            teamMembers.forEach { member ->
                                MemberAccessRow(
                                    displayName = member.displayName.ifBlank { member.email.substringBefore('@') },
                                    email = member.email,
                                    role = member.resolvedRole(),
                                    statusLabel = if (member.userId == signedInUid) "You • Active" else "Active",
                                    canManageTeam = activeRole.canManageTeam && member.userId != signedInUid,
                                    onChangeRole = { newRole ->
                                        onUpdateMemberRole(member.userId, member.inviteId, member.email, newRole)
                                    },
                                    onRemove = {
                                        memberPendingRemoval = Triple(member.userId, member.inviteId, member.email)
                                    }
                                )
                            }
                        }

                        if (pendingTeamInvites.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                text = "Pending Invites (${pendingTeamInvites.size})",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            pendingTeamInvites.forEach { invite ->
                                val deliverySuffix = when (invite.emailDeliveryStatus.trim().lowercase()) {
                                    "sent" -> "Email sent"
                                    "failed" -> "Email failed (tap mail icon)"
                                    else -> "Pending sign-in"
                                }
                                MemberAccessRow(
                                    displayName = invite.memberDisplayName.ifBlank { invite.memberEmail.substringBefore('@') },
                                    email = invite.memberEmail,
                                    role = invite.resolvedRole(),
                                    statusLabel = deliverySuffix,
                                    canManageTeam = activeRole.canManageTeam,
                                    onEmailInvite = {
                                        launchWorkspaceInviteEmail(
                                            context = context,
                                            recipientEmail = invite.memberEmail,
                                            recipientName = invite.memberDisplayName,
                                            role = invite.resolvedRole(),
                                            inviterEmail = signedInEmail,
                                            workspaceOwnerId = invite.workspaceOwnerId.ifBlank { activeWorkspaceOwnerId },
                                            inviteId = invite.id,
                                            onStatusMessage = onShowMessage
                                        )
                                    },
                                    onChangeRole = { newRole ->
                                        onUpdateMemberRole(invite.memberUid, invite.id, invite.memberEmail, newRole)
                                    },
                                    onRemove = {
                                        memberPendingRemoval = Triple(invite.memberUid, invite.id, invite.memberEmail)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 5. Computer & Website Access Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("computer_web_access_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Computer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Use on Computer (Website Portal)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "Open FeedMill Ops in any computer browser synced live with this Bulacan Plant database. Use the AI Studio web link right now, or publish the dedicated Firebase Hosting web app once with: firebase deploy --only hosting",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Live Browser Link: $APP_SHARED_WEB_URL",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Dedicated Website Portal: $WEB_PORTAL_URL",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(APP_SHARED_WEB_URL)).apply {
                                            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(browserIntent)
                                    } catch (_: Exception) {
                                        val copied = copyReportTextToClipboard(context, "FeedMill Ops Computer Link", APP_SHARED_WEB_URL)
                                        onShowMessage(if (copied) "Computer link copied to clipboard." else "Unable to open browser.")
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("open_live_computer_link_button")
                            ) {
                                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Live Link")
                            }
                            OutlinedButton(
                                onClick = {
                                    val copied = copyReportTextToClipboard(context, "FeedMill Ops Website URL", WEB_PORTAL_URL)
                                    onShowMessage(if (copied) "Website Portal URL copied: $WEB_PORTAL_URL" else "Unable to copy URL.")
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("copy_web_portal_url_button")
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Website URL")
                            }
                        }
                    }
                }
            }

            // 6. Role Permissions Matrix Guide
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Roles & Permissions Guide",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        RolePermissionGuideRow(
                            role = WorkspaceRole.ADMIN,
                            icon = Icons.Filled.AdminPanelSettings,
                            bullets = listOf(
                                "Full read/write across inventory, orders, dispatches, schedules & BOMs",
                                "Delete SKUs, restore JSON backups & run selective/factory resets",
                                "Invite members, assign roles & revoke shared access"
                            )
                        )
                        RolePermissionGuideRow(
                            role = WorkspaceRole.OPERATOR,
                            icon = Icons.Filled.Edit,
                            bullets = listOf(
                                "Create & update orders, dispatches, schedules, batches, reprocess & stock",
                                "Generate reports, snapshots & share summaries to Messenger",
                                "Cannot delete SKUs, reset workspace, restore backups, or edit team roles"
                            )
                        )
                        RolePermissionGuideRow(
                            role = WorkspaceRole.VIEWER,
                            icon = Icons.Filled.Visibility,
                            bullets = listOf(
                                "Read-only real-time access to dashboards, stock levels & activity history",
                                "View, export PDF/CSV & share Messenger snapshots",
                                "Blocked from creating, editing, or deleting any workspace records"
                            )
                        )
                    }
                }
            }
        }
    }

    if (showInviteDialog) {
        val existingEmails = remember(teamMembers, pendingTeamInvites) {
            (teamMembers.map { it.email.trim().lowercase() } + pendingTeamInvites.map { it.memberEmail.trim().lowercase() }).toSet()
        }
        InviteTeamMemberDialog(
            inviterEmail = signedInEmail,
            workspaceOwnerId = activeWorkspaceOwnerId,
            existingEmails = existingEmails,
            onDismiss = { showInviteDialog = false },
            onSendInvite = { email, displayName, role, deliveryMode, customNote ->
                showInviteDialog = false
                onInviteMember(email, displayName, role) { inviteId ->
                    recentInviteDraft = RecentInviteEmailDraft(
                        email = email,
                        displayName = displayName,
                        role = role,
                        customNote = customNote,
                        workspaceOwnerId = activeWorkspaceOwnerId,
                        inviteId = inviteId
                    )
                    if (deliveryMode == InviteDeliveryMode.GOOGLE_AND_EMAIL) {
                        launchWorkspaceInviteEmail(
                            context = context,
                            recipientEmail = email,
                            recipientName = displayName,
                            role = role,
                            inviterEmail = signedInEmail,
                            customNote = customNote,
                            workspaceOwnerId = activeWorkspaceOwnerId,
                            inviteId = inviteId,
                            onStatusMessage = onShowMessage
                        )
                    }
                }
            }
        )
    }

    memberPendingRemoval?.let { (memberUid, inviteId, email) ->
        AlertDialog(
            onDismissRequest = { memberPendingRemoval = null },
            title = { Text("Remove Team Access") },
            text = {
                Text("Revoke shared Bulacan Plant access for $email? They will immediately lose access to this workspace.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveMember(memberUid, inviteId, email)
                        memberPendingRemoval = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove Access")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberPendingRemoval = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (confirmLeaveShared) {
        AlertDialog(
            onDismissRequest = { confirmLeaveShared = false },
            title = { Text("Leave Shared Workspace") },
            text = {
                Text("Leave this shared Bulacan Plant workspace and switch back to your personal workspace?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onLeaveActiveSharedWorkspace()
                        confirmLeaveShared = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Leave Workspace")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeaveShared = false }) {
                    Text("Stay")
                }
            }
        )
    }
}

@Composable
private fun MemberAccessRow(
    displayName: String,
    email: String,
    role: WorkspaceRole,
    statusLabel: String,
    canManageTeam: Boolean,
    onEmailInvite: (() -> Unit)? = null,
    onChangeRole: (WorkspaceRole) -> Unit,
    onRemove: () -> Unit
) {
    var roleMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$email • $statusLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (canManageTeam && onEmailInvite != null) {
                IconButton(
                    onClick = onEmailInvite,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("email_invite_${email}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MailOutline,
                        contentDescription = "Send email invite to $email",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (canManageTeam) {
                Box {
                    OutlinedButton(
                        onClick = { roleMenuExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("change_role_${email}")
                    ) {
                        Text(role.label, style = MaterialTheme.typography.labelMedium)
                    }
                    DropdownMenu(
                        expanded = roleMenuExpanded,
                        onDismissRequest = { roleMenuExpanded = false }
                    ) {
                        WorkspaceRole.entries.forEach { candidate ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(candidate.label, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = candidate.summary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    roleMenuExpanded = false
                                    if (candidate != role) {
                                        onChangeRole(candidate)
                                    }
                                }
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("remove_member_${email}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Remove $email",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                RoleBadgeChip(role = role)
            }
        }
    }
}

@Composable
private fun RolePermissionGuideRow(
    role: WorkspaceRole,
    icon: ImageVector,
    bullets: List<String>
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = ForestEmerald, modifier = Modifier.size(18.dp))
                RoleBadgeChip(role = role)
            }
            bullets.forEach { bullet ->
                Text(
                    text = "• $bullet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RoleBadgeChip(
    role: WorkspaceRole,
    onDarkSurface: Boolean = false,
    labelOverride: String? = null
) {
    val bgColor = if (onDarkSurface) {
        Color.White.copy(alpha = 0.2f)
    } else {
        when (role) {
            WorkspaceRole.ADMIN -> ForestEmerald.copy(alpha = 0.14f)
            WorkspaceRole.OPERATOR -> StatusInProgress.copy(alpha = 0.14f)
            WorkspaceRole.VIEWER -> GrainAmber.copy(alpha = 0.16f)
        }
    }
    val fgColor = if (onDarkSurface) {
        Color.White
    } else {
        when (role) {
            WorkspaceRole.ADMIN -> ForestEmerald
            WorkspaceRole.OPERATOR -> SlateTeal
            WorkspaceRole.VIEWER -> GrainAmber
        }
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = labelOverride ?: role.label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = fgColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun InviteTeamMemberDialog(
    inviterEmail: String = "",
    workspaceOwnerId: String = "",
    existingEmails: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onSendInvite: (
        email: String,
        displayName: String,
        role: WorkspaceRole,
        deliveryMode: InviteDeliveryMode,
        customNote: String
    ) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(WorkspaceRole.OPERATOR) }
    var deliveryMode by remember { mutableStateOf(InviteDeliveryMode.GOOGLE_AND_EMAIL) }
    var customNote by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val normalizedEmail = email.trim().lowercase()
    val isExistingCollaborator = normalizedEmail.isNotEmpty() && normalizedEmail in existingEmails
    val previewPair = remember(normalizedEmail, displayName, selectedRole, inviterEmail, customNote, workspaceOwnerId) {
        buildWorkspaceInviteEmailContent(
            recipientEmail = normalizedEmail.ifBlank { "teammate@gmail.com" },
            recipientName = displayName,
            role = selectedRole,
            inviterEmail = inviterEmail,
            customNote = customNote,
            workspaceOwnerId = workspaceOwnerId
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite Team Member") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Enter the collaborator's Google Sign-In email and choose how to deliver their invitation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMsg = null
                    },
                    label = { Text("Google Email Address") },
                    placeholder = { Text("teammate@gmail.com") },
                    singleLine = true,
                    isError = errorMsg != null,
                    supportingText = when {
                        errorMsg != null -> { { Text(errorMsg!!) } }
                        isExistingCollaborator -> {
                            { Text("Already in workspace — sending will update their assigned role.") }
                        }
                        else -> null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invite_email_input")
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Name / Position (optional)") },
                    placeholder = { Text("e.g., Shift Supervisor") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invite_name_input")
                )
                Text(
                    text = "Assign Role",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                WorkspaceRole.entries.forEach { role ->
                    val chosen = selectedRole == role
                    Surface(
                        onClick = { selectedRole = role },
                        shape = RoundedCornerShape(10.dp),
                        color = if (chosen) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        },
                        border = BorderStroke(
                            width = if (chosen) 1.5.dp else 1.dp,
                            color = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invite_role_${role.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = chosen,
                                onClick = { selectedRole = role }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(role.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = role.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Delivery Option",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                InviteDeliveryMode.entries.forEach { mode ->
                    val chosen = deliveryMode == mode
                    Surface(
                        onClick = { deliveryMode = mode },
                        shape = RoundedCornerShape(10.dp),
                        color = if (chosen) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        },
                        border = BorderStroke(
                            width = if (chosen) 1.5.dp else 1.dp,
                            color = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invite_delivery_${mode.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = chosen,
                                onClick = { deliveryMode = mode }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = mode.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (deliveryMode == InviteDeliveryMode.GOOGLE_AND_EMAIL) {
                    OutlinedTextField(
                        value = customNote,
                        onValueChange = { customNote = it },
                        label = { Text("Personal Note for Email (optional)") },
                        placeholder = { Text("e.g., Please accept before morning shift") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invite_custom_note_input")
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invite_auto_email_preview")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Auto-Generated Email Preview",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "To: ${normalizedEmail.ifBlank { "teammate@gmail.com" }}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Subject: ${previewPair.first}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 2.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            Text(
                                text = previewPair.second,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = email.trim().lowercase()
                    if (trimmed.length < 3 || !trimmed.contains('@') || !trimmed.contains('.')) {
                        errorMsg = "Please enter a valid Google email."
                    } else {
                        onSendInvite(trimmed, displayName.trim(), selectedRole, deliveryMode, customNote.trim())
                    }
                },
                modifier = Modifier.testTag("confirm_send_invite_button")
            ) {
                Text(
                    if (deliveryMode == InviteDeliveryMode.GOOGLE_AND_EMAIL) {
                        "Send & Compose Email"
                    } else {
                        "Send to Google"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
