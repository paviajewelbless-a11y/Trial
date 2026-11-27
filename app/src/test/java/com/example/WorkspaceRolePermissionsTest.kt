package com.example

import com.example.model.WorkspaceInvite
import com.example.model.WorkspaceInviteStatus
import com.example.model.WorkspaceMember
import com.example.model.WorkspaceRole
import com.example.ui.screens.InviteDeliveryMode
import com.example.ui.screens.buildWorkspaceInviteEmailContent
import com.example.ui.screens.buildWorkspaceInviteLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceRolePermissionsTest {

    @Test
    fun `admin role grants full operational and management permissions`() {
        val role = WorkspaceRole.ADMIN
        assertTrue(role.canWriteOperations)
        assertTrue(role.canManageTeam)
        assertTrue(role.canResetWorkspace)
        assertTrue(role.canDeleteSkus)
        assertTrue(role.canEditSettings)
    }

    @Test
    fun `operator role grants operational writes without admin privileges`() {
        val role = WorkspaceRole.OPERATOR
        assertTrue(role.canWriteOperations)
        assertFalse(role.canManageTeam)
        assertFalse(role.canResetWorkspace)
        assertFalse(role.canDeleteSkus)
        assertFalse(role.canEditSettings)
    }

    @Test
    fun `viewer role is strictly read-only`() {
        val role = WorkspaceRole.VIEWER
        assertFalse(role.canWriteOperations)
        assertFalse(role.canManageTeam)
        assertFalse(role.canResetWorkspace)
        assertFalse(role.canDeleteSkus)
        assertFalse(role.canEditSettings)
    }

    @Test
    fun `fromRaw falls back safely to least-privilege viewer role`() {
        assertEquals(WorkspaceRole.ADMIN, WorkspaceRole.fromRaw("admin"))
        assertEquals(WorkspaceRole.OPERATOR, WorkspaceRole.fromRaw(" Operator "))
        assertEquals(WorkspaceRole.VIEWER, WorkspaceRole.fromRaw("Viewer"))
        assertEquals(WorkspaceRole.VIEWER, WorkspaceRole.fromRaw("unknown_role"))
        assertEquals(WorkspaceRole.VIEWER, WorkspaceRole.fromRaw(""))
    }

    @Test
    fun `workspace invite and member resolve roles and statuses safely`() {
        val pendingInvite = WorkspaceInvite(
            id = "inv_1",
            workspaceOwnerId = "owner_1",
            memberEmail = "operator@10kfeeds.ph",
            role = "Operator",
            status = "Pending"
        )
        assertEquals(WorkspaceRole.OPERATOR, pendingInvite.resolvedRole())
        assertEquals(WorkspaceInviteStatus.PENDING, pendingInvite.resolvedStatus())

        val activeAdminMember = WorkspaceMember(
            id = "uid_2",
            userId = "uid_2",
            workspaceOwnerId = "owner_1",
            inviteId = "inv_2",
            email = "admin@10kfeeds.ph",
            role = "Admin"
        )
        assertEquals(WorkspaceRole.ADMIN, activeAdminMember.resolvedRole())
        assertEquals("", pendingInvite.emailDeliveryStatus)

        val emailedInvite = pendingInvite.copy(emailDeliveryStatus = "Sent")
        assertEquals("Sent", emailedInvite.emailDeliveryStatus)
    }

    @Test
    fun `buildWorkspaceInviteEmailContent includes recipient role custom note link and sign-in instructions`() {
        val (subject, body) = buildWorkspaceInviteEmailContent(
            recipientEmail = "Teammate@Gmail.com ",
            recipientName = "Juan",
            role = WorkspaceRole.OPERATOR,
            inviterEmail = "owner@10kfeeds.ph",
            customNote = "Please join before the 6 AM shift.",
            workspaceOwnerId = "owner_123",
            inviteId = "inv_abc"
        )
        assertTrue(subject.contains("Bulacan Plant"))
        assertTrue(subject.contains("Operator"))
        assertTrue(body.contains("Hi Juan,"))
        assertTrue(body.contains("teammate@gmail.com"))
        assertTrue(body.contains("owner@10kfeeds.ph"))
        assertTrue(body.contains("Please join before the 6 AM shift."))
        assertTrue(body.contains("https://ais-pre-"))
        assertTrue(body.contains("action=team_invite"))
        assertTrue(body.contains("invite=inv_abc"))
        assertTrue(body.contains("workspace=owner_123"))
        assertTrue(body.contains("Accept & Switch"))
    }

    @Test
    fun `buildWorkspaceInviteLink encodes query parameters cleanly`() {
        val link = buildWorkspaceInviteLink(
            workspaceOwnerId = "owner_1",
            recipientEmail = "Teammate@Gmail.com",
            role = WorkspaceRole.OPERATOR,
            inviteId = "inv_99"
        )
        assertTrue(link.startsWith("https://"))
        assertTrue(link.contains("action=team_invite"))
        assertTrue(link.contains("invite=inv_99"))
        assertTrue(link.contains("workspace=owner_1"))
        assertTrue(link.contains("email=teammate%40gmail.com"))
        assertTrue(link.contains("role=Operator"))
    }

    @Test
    fun `invite delivery modes expose Google plus email and Google only options`() {
        val modes = InviteDeliveryMode.entries
        assertEquals(2, modes.size)
        assertTrue(modes.contains(InviteDeliveryMode.GOOGLE_AND_EMAIL))
        assertTrue(modes.contains(InviteDeliveryMode.GOOGLE_ONLY))
    }
}
