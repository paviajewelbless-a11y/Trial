const test = require("node:test");
const assert = require("node:assert/strict");
const {
  FIRESTORE_DATABASE_ID,
  buildInviteEmailPayload,
  shouldProcessInviteWrite,
  processWorkspaceInviteWrite
} = require("../index.js");

function createMockFirestore() {
  const updates = [];
  const db = {
    collection(name) {
      return {
        doc(id) {
          const basePath = `${name}/${id}`;
          return {
            path: basePath,
            collection(subName) {
              return {
                doc(subId) {
                  return { path: `${basePath}/${subName}/${subId}` };
                }
              };
            }
          };
        }
      };
    },
    batch() {
      const ops = [];
      return {
        update(ref, data) {
          ops.push({ path: ref.path, data });
        },
        async commit() {
          updates.push(...ops);
        }
      };
    }
  };
  return { db, updates };
}

test("targets the exact named Firestore database ID", () => {
  assert.equal(
    FIRESTORE_DATABASE_ID,
    "ai-studio-android-feedmill-ebaf442b-0447-4046-8dd2-306eb64b2b23"
  );
});

test("buildInviteEmailPayload creates personalized subject, text, link, and escaped HTML", () => {
  const payload = buildInviteEmailPayload(
    {
      workspaceOwnerId: "owner_42",
      memberEmail: "Operator@10kfeeds.ph ",
      memberDisplayName: "Alex <Lead>",
      workspaceName: "Bulacan Plant",
      role: "Operator",
      invitedByEmail: "admin@10kfeeds.ph"
    },
    "inv_42"
  );

  assert.equal(payload.recipientEmail, "operator@10kfeeds.ph");
  assert.match(payload.subject, /Bulacan Plant/);
  assert.match(payload.subject, /Operator/);
  assert.match(payload.inviteLink, /^https:\/\/ais-pre-.*action=team_invite.*invite=inv_42/);
  assert.match(payload.text, /https:\/\/ais-pre-/);
  assert.match(payload.text, /Accept & Switch/);
  assert.match(payload.html, /Accept &amp; Join Workspace/);
  assert.match(payload.html, /Alex &lt;Lead&gt;/);
  assert.doesNotMatch(payload.html, /<Lead>/);
});

test("shouldProcessInviteWrite skips self-update loops and non-Pending documents", () => {
  assert.equal(
    shouldProcessInviteWrite(null, {
      status: "Pending",
      memberEmail: "user@example.com",
      role: "Operator"
    }),
    true
  );

  assert.equal(
    shouldProcessInviteWrite(
      { status: "Pending", role: "Operator" },
      {
        status: "Pending",
        memberEmail: "user@example.com",
        role: "Operator",
        emailDeliveryStatus: "Sent",
        lastEmailedRole: "Operator"
      }
    ),
    false
  );

  assert.equal(
    shouldProcessInviteWrite(
      { status: "Pending", role: "Viewer" },
      {
        status: "Pending",
        memberEmail: "user@example.com",
        role: "Operator",
        emailDeliveryStatus: "Sent",
        lastEmailedRole: "Viewer"
      }
    ),
    true
  );

  assert.equal(
    shouldProcessInviteWrite(
      { status: "Pending", role: "Operator" },
      { status: "Active", memberEmail: "user@example.com", role: "Operator" }
    ),
    false
  );
});

test("processWorkspaceInviteWrite sends mail and updates both root and workspace invite docs", async () => {
  const { db, updates } = createMockFirestore();
  const sentMails = [];
  const transporter = {
    async sendMail(msg) {
      sentMails.push(msg);
      return { messageId: "msg-123" };
    }
  };

  const result = await processWorkspaceInviteWrite({
    inviteId: "owner1__abc",
    beforeData: null,
    afterData: {
      workspaceOwnerId: "owner1",
      workspaceName: "Bulacan Plant",
      ownerEmail: "owner@10kfeeds.ph",
      invitedByEmail: "owner@10kfeeds.ph",
      memberEmail: "teammate@gmail.com",
      memberDisplayName: "Teammate",
      role: "Operator",
      status: "Pending"
    },
    db,
    transporter,
    smtpFrom: "invites@10kfeeds.ph",
    serverTimestamp: () => "SERVER_TS",
    logger: { info() {}, error() {} }
  });

  assert.equal(result.skipped, false);
  assert.equal(result.status, "Sent");
  assert.equal(sentMails.length, 1);
  assert.equal(sentMails[0].to, "teammate@gmail.com");
  assert.equal(sentMails[0].from, "invites@10kfeeds.ph");

  assert.equal(updates.length, 2);
  assert.equal(updates[0].path, "workspace_invites/owner1__abc");
  assert.equal(updates[0].data.emailDeliveryStatus, "Sent");
  assert.equal(updates[1].path, "users/owner1/team_invites/owner1__abc");
  assert.equal(updates[1].data.lastEmailedRole, "Operator");
});

test("processWorkspaceInviteWrite records Failed delivery status cleanly when SMTP errors", async () => {
  const { db, updates } = createMockFirestore();
  const transporter = {
    async sendMail() {
      throw new Error("SMTP auth rejected");
    }
  };

  const result = await processWorkspaceInviteWrite({
    inviteId: "owner1__abc",
    beforeData: null,
    afterData: {
      workspaceOwnerId: "owner1",
      memberEmail: "teammate@gmail.com",
      role: "Viewer",
      status: "Pending"
    },
    db,
    transporter,
    smtpFrom: "invites@10kfeeds.ph",
    serverTimestamp: () => "SERVER_TS",
    logger: { info() {}, error() {} }
  });

  assert.equal(result.status, "Failed");
  assert.equal(updates.length, 2);
  assert.equal(updates[0].data.emailDeliveryStatus, "Failed");
  assert.equal(updates[0].data.emailError, "SMTP auth rejected");
});
