const FIRESTORE_DATABASE_ID = "ai-studio-android-feedmill-ebaf442b-0447-4046-8dd2-306eb64b2b23";

function escapeHtml(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function roleSummary(role) {
  switch (String(role ?? "").trim().toLowerCase()) {
    case "admin":
      return "Full operational control, SKU management, resets, backups, and team role management.";
    case "operator":
      return "Create and update orders, dispatches, schedules, batches, reprocess items, and stock.";
    default:
      return "Read-only access to dashboards, inventory levels, reports, and activity history.";
  }
}

const APP_SHARED_WEB_URL = "https://ais-pre-2k6iew2ypwev3yt7m6gfru-687226281361.asia-southeast1.run.app";

function buildInviteLink({ inviteId = "", workspaceOwnerId = "", recipientEmail = "", role = "Viewer" }) {
  const params = new URLSearchParams();
  params.set("action", "team_invite");
  if (inviteId) params.set("invite", String(inviteId).trim());
  if (workspaceOwnerId) params.set("workspace", String(workspaceOwnerId).trim());
  if (recipientEmail) params.set("email", String(recipientEmail).trim().toLowerCase());
  params.set("role", String(role || "Viewer").trim());
  return `${APP_SHARED_WEB_URL}/?${params.toString()}`;
}

function buildInviteEmailPayload(invite, inviteId = "") {
  const recipientEmail = String(invite?.memberEmail ?? "").trim().toLowerCase();
  const recipientName = String(invite?.memberDisplayName ?? "").trim() || recipientEmail.split("@")[0] || "Team Member";
  const workspaceName = String(invite?.workspaceName ?? "").trim() || "Bulacan Plant";
  const workspaceOwnerId = String(invite?.workspaceOwnerId ?? "").trim();
  const role = String(invite?.role ?? "Viewer").trim() || "Viewer";
  const inviterEmail = String(invite?.invitedByEmail ?? invite?.ownerEmail ?? "").trim() || "a workspace administrator";
  const permissions = roleSummary(role);
  const inviteLink = buildInviteLink({ inviteId, workspaceOwnerId, recipientEmail, role });

  const subject = `Invitation to join ${workspaceName} (FeedMill Ops) as ${role}`;
  const text = [
    `Hi ${recipientName},`,
    "",
    `${inviterEmail} invited you to collaborate on the ${workspaceName} workspace in FeedMill Ops.`,
    "",
    `Assigned Role: ${role}`,
    `Permissions: ${permissions}`,
    "",
    "Open your invitation link:",
    inviteLink,
    "",
    "How to join:",
    "1. Tap the invite link above (or open the FeedMill Ops Android app).",
    `2. Sign in with Google using this email address: ${recipientEmail}`,
    '3. Tap "Accept & Switch" on the pending invitation banner (or go to More -> Team).'
  ].join("\n");

  const html = `
    <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #1a2421; max-width: 560px; margin: 0 auto; padding: 24px; border: 1px solid #d7e2dc; border-radius: 12px;">
      <h2 style="margin: 0 0 12px; color: #164e36;">You're invited to ${escapeHtml(workspaceName)}</h2>
      <p style="margin: 0 0 16px; line-height: 1.5;">Hi ${escapeHtml(recipientName)},</p>
      <p style="margin: 0 0 16px; line-height: 1.5;">
        <strong>${escapeHtml(inviterEmail)}</strong> invited you to collaborate in <strong>FeedMill Ops</strong>.
      </p>
      <div style="background: #f2f7f4; border-left: 4px solid #164e36; padding: 12px 16px; border-radius: 6px; margin-bottom: 18px;">
        <div style="font-weight: 700; margin-bottom: 4px;">Role: ${escapeHtml(role)}</div>
        <div style="font-size: 14px; color: #435148;">${escapeHtml(permissions)}</div>
      </div>
      <div style="margin: 0 0 18px;">
        <a href="${escapeHtml(inviteLink)}" style="display: inline-block; background: #164e36; color: #ffffff; text-decoration: none; font-weight: 600; padding: 12px 20px; border-radius: 8px;">Accept &amp; Join Workspace</a>
      </div>
      <p style="font-size: 12px; color: #5b6961; word-break: break-all; margin: 0 0 18px;">
        Or open this link: <a href="${escapeHtml(inviteLink)}">${escapeHtml(inviteLink)}</a>
      </p>
      <p style="font-weight: 600; margin: 0 0 8px;">How to accept:</p>
      <ol style="margin: 0 0 18px; padding-left: 20px; line-height: 1.6;">
        <li>Tap <strong>Accept &amp; Join Workspace</strong> above (or open the <strong>FeedMill Ops</strong> Android app).</li>
        <li>Sign in with Google using <strong>${escapeHtml(recipientEmail)}</strong>.</li>
        <li>Tap <strong>Accept &amp; Switch</strong> on the invitation banner (or open <strong>More &rarr; Team</strong>).</li>
      </ol>
    </div>
  `.trim();

  return { recipientEmail, subject, text, html, inviteLink };
}

function shouldProcessInviteWrite(beforeData, afterData) {
  if (!afterData) return false;
  if (afterData.status !== "Pending") return false;

  const email = String(afterData.memberEmail ?? "").trim();
  if (!email.includes("@")) return false;

  if (!beforeData) return true;

  // Avoid infinite loops when the function itself updates emailDeliveryStatus/emailSentAt.
  if (afterData.emailDeliveryStatus === "Sent" && afterData.lastEmailedRole === afterData.role) {
    return false;
  }

  // Re-send if Pending invite is newly restored or role changed while still Pending.
  return beforeData.status !== "Pending" || beforeData.role !== afterData.role;
}

async function processWorkspaceInviteWrite({
  inviteId,
  beforeData,
  afterData,
  db,
  transporter,
  smtpFrom,
  serverTimestamp,
  logger = console
}) {
  if (!shouldProcessInviteWrite(beforeData, afterData)) {
    return { skipped: true };
  }

  const ownerId = String(afterData.workspaceOwnerId ?? "").trim();
  const payload = buildInviteEmailPayload(afterData, inviteId);
  const rootRef = db.collection("workspace_invites").doc(inviteId);
  const wsInviteRef = ownerId
    ? db.collection("users").doc(ownerId).collection("team_invites").doc(inviteId)
    : null;

  try {
    const info = await transporter.sendMail({
      from: smtpFrom || "FeedMill Ops <no-reply@feedmillops.app>",
      to: payload.recipientEmail,
      subject: payload.subject,
      text: payload.text,
      html: payload.html
    });

    const sentUpdate = {
      emailDeliveryStatus: "Sent",
      lastEmailedRole: String(afterData.role ?? ""),
      emailSentAt: serverTimestamp(),
      emailError: ""
    };
    const batch = db.batch();
    batch.update(rootRef, sentUpdate);
    if (wsInviteRef) {
      batch.update(wsInviteRef, sentUpdate);
    }
    await batch.commit();
    logger.info(`Sent workspace invite email to ${payload.recipientEmail} (messageId=${info?.messageId ?? "ok"})`);
    return { skipped: false, status: "Sent", messageId: info?.messageId ?? "" };
  } catch (err) {
    const errorMsg = String(err?.message ?? err ?? "Email send failed").slice(0, 240);
    const failedUpdate = {
      emailDeliveryStatus: "Failed",
      emailError: errorMsg,
      updatedAt: serverTimestamp()
    };
    const batch = db.batch();
    batch.update(rootRef, failedUpdate);
    if (wsInviteRef) {
      batch.update(wsInviteRef, failedUpdate);
    }
    await batch.commit();
    logger.error(`Failed to send invite email to ${payload.recipientEmail}: ${errorMsg}`);
    return { skipped: false, status: "Failed", error: errorMsg };
  }
}

function registerFirebaseFunctions() {
  const { onDocumentWritten } = require("firebase-functions/v2/firestore");
  const { defineSecret } = require("firebase-functions/params");
  const logger = require("firebase-functions/logger");
  const { initializeApp, getApps } = require("firebase-admin/app");
  const { getFirestore, FieldValue } = require("firebase-admin/firestore");
  const nodemailer = require("nodemailer");

  if (!getApps().length) {
    initializeApp();
  }

  const smtpHost = defineSecret("SMTP_HOST");
  const smtpPort = defineSecret("SMTP_PORT");
  const smtpUser = defineSecret("SMTP_USER");
  const smtpPass = defineSecret("SMTP_PASS");
  const smtpFrom = defineSecret("SMTP_FROM");

  const onWorkspaceInviteWritten = onDocumentWritten(
    {
      document: "workspace_invites/{inviteId}",
      database: FIRESTORE_DATABASE_ID,
      secrets: [smtpHost, smtpPort, smtpUser, smtpPass, smtpFrom]
    },
    async (event) => {
      const db = getFirestore(FIRESTORE_DATABASE_ID);
      const portNum = Number(smtpPort.value() || 587);
      const transporter = nodemailer.createTransport({
        host: smtpHost.value(),
        port: portNum,
        secure: portNum === 465,
        auth: {
          user: smtpUser.value(),
          pass: smtpPass.value()
        }
      });

      return processWorkspaceInviteWrite({
        inviteId: event.params.inviteId,
        beforeData: event.data?.before?.data() ?? null,
        afterData: event.data?.after?.data() ?? null,
        db,
        transporter,
        smtpFrom: smtpFrom.value(),
        serverTimestamp: () => FieldValue.serverTimestamp(),
        logger
      });
    }
  );

  return { onWorkspaceInviteWritten };
}

let exportedFunctions = {};
try {
  // Only load Firebase runtime SDKs when installed/running in Firebase Cloud Functions
  require.resolve("firebase-functions/v2/firestore");
  exportedFunctions = registerFirebaseFunctions();
} catch (_) {
  // Allow standalone unit tests (`node --test`) to run without deployed node_modules
}

module.exports = {
  ...exportedFunctions,
  FIRESTORE_DATABASE_ID,
  buildInviteEmailPayload,
  shouldProcessInviteWrite,
  processWorkspaceInviteWrite
};
