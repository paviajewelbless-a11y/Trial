import {
  PLANT_SECTIONS,
  REPORT_SECTION_OPTIONS,
  WORKSPACE_ROLE_OPTIONS,
  buildPlantReport,
  computeDailyLogs,
  computeRawMaterialForecasts,
  effectiveDispatchWeightKg,
  inferMaterialSector,
  missingCatalogEntities,
  resolvedDispatchStatus,
  resolveRole,
  todayIsoDate
} from "./operations.js";

export function escapeHtml(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

export function formatNum(value, decimals = 1) {
  return Number(value || 0).toLocaleString(undefined, {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  });
}

export function statusPill(status) {
  const s = String(status || "");
  if (
    s === "Healthy" ||
    s === "Fulfilled" ||
    s === "Delivered" ||
    s === "Completed" ||
    s === "Active" ||
    s === "Flagged to Inventory" ||
    s === "Success"
  ) {
    return `<span class="pill pill-healthy">${escapeHtml(s)}</span>`;
  }
  if (
    s === "Critical" ||
    s === "Urgent" ||
    s === "Order Now" ||
    s === "Damaged" ||
    s === "Cancelled" ||
    s === "Error"
  ) {
    return `<span class="pill pill-critical">${escapeHtml(s)}</span>`;
  }
  if (
    s === "Low Watch" ||
    s === "Running Low" ||
    s === "Partially Dispatched" ||
    s === "Pending" ||
    s === "Pending Reprocess" ||
    s === "Reprocessing" ||
    s === "Returned" ||
    s === "Held" ||
    s === "Open" ||
    s === "In Progress" ||
    s === "Warning" ||
    s === "Order-Driven"
  ) {
    return `<span class="pill pill-warn">${escapeHtml(s)}</span>`;
  }
  return `<span class="pill pill-neutral">${escapeHtml(s || "—")}</span>`;
}

function renderRoleSwitcher({
  currentRole,
  targetType,
  memberUid = "",
  inviteId = "",
  memberEmail = "",
  memberName = "",
  disabled = false
}) {
  const normalized = resolveRole(currentRole).label;
  return `
    <div class="role-switch" role="group" aria-label="Switch access for ${escapeHtml(memberName || memberEmail || "team member")}">
      ${WORKSPACE_ROLE_OPTIONS.map((option) => {
        const isActive = normalized === option;
        const variantClass = option.toLowerCase();
        const dataAttrs =
          targetType === "member"
            ? `data-switch-member-role="${option}" data-member-uid="${escapeHtml(memberUid)}" data-invite-id="${escapeHtml(inviteId)}" data-member-email="${escapeHtml(memberEmail)}" data-member-name="${escapeHtml(memberName)}"`
            : `data-switch-invite-role="${option}" data-invite-id="${escapeHtml(inviteId)}" data-member-email="${escapeHtml(memberEmail)}"`;
        return `<button type="button" class="role-chip ${isActive ? `active ${variantClass}` : ""}" ${dataAttrs} aria-pressed="${isActive ? "true" : "false"}" ${disabled ? "disabled" : ""}>${option}</button>`;
      }).join("")}
    </div>
  `;
}

export function renderSummaryTab(state, summaries, kpis) {
  const criticalList = summaries.filter((s) => s.status === "Critical" || s.status === "Low Watch").slice(0, 8);
  const openOrders = state.orders.filter((o) => o.status === "Open" || o.status === "Partially Dispatched").slice(0, 8);
  const forecasts = computeRawMaterialForecasts({
    materials: state.rawMaterials,
    batches: state.batches,
    schedules: state.schedules,
    boms: state.boms,
    lookbackDays: state.simLookbackDays
  });
  const urgentMats = forecasts.filter((f) => f.status !== "Healthy").slice(0, 6);
  const pendingReprocess = state.reprocessItems.filter((r) => !r.flaggedToInventory && String(r.status || "").toLowerCase() !== "damaged");

  const sectionCards = PLANT_SECTIONS.map((sec) => {
    const secSkus = summaries.filter((s) => s.sku.section === sec);
    const availKg = secSkus.reduce((sum, s) => sum + Math.max(0, s.availableStockKg), 0);
    const crit = secSkus.filter((s) => s.status === "Critical").length;
    return `
      <div class="card" style="padding:14px; display:grid; gap:4px;">
        <div style="font-size:11px; font-weight:700; color:var(--muted);">${escapeHtml(sec)}</div>
        <div style="font-size:18px; font-weight:800;">${formatNum(availKg, 0)} kg</div>
        <div style="font-size:12px; color:var(--muted);">${secSkus.length} SKUs • ${crit > 0 ? `<span style="color:var(--critical);font-weight:700;">${crit} critical</span>` : "All healthy/watch"}</div>
      </div>
    `;
  }).join("");

  return `
    ${
      urgentMats.length
        ? `<div class="banner banner-warn">
            <div>
              <strong>Raw Material Alerts (${urgentMats.length})</strong>
              <div style="font-size:13px; color:#5a3c07;">${urgentMats.map((m) => `${escapeHtml(m.material.name)} (${escapeHtml(m.projectedRunOutLabel)})`).slice(0, 3).join(" • ")}</div>
            </div>
            <button class="btn btn-sm" data-goto-tab="materials">Open Raw Materials</button>
          </div>`
        : ""
    }
    ${
      pendingReprocess.length
        ? `<div class="banner banner-critical">
            <div>
              <strong>Quarantine / Reprocess Queue (${pendingReprocess.length} items)</strong>
              <div style="font-size:13px; color:#6b1f18;">${formatNum(pendingReprocess.reduce((s, r) => s + Number(r.returnedWeightKg || 0), 0), 0)} kg awaiting reprocess or inventory flag</div>
            </div>
            <button class="btn btn-sm" data-goto-tab="reprocess">Review Reprocess</button>
          </div>`
        : ""
    }

    <div class="kpi-grid">
      <div class="card kpi-card">
        <div class="kpi-label">Finished Stock Available</div>
        <div class="kpi-value">${formatNum(kpis.totalStockKg, 0)} kg</div>
        <div class="kpi-sub">${formatNum(kpis.totalBags, 0)} bags across ${state.skus.length} SKUs</div>
      </div>
      <div class="card kpi-card">
        <div class="kpi-label">Open Customer Orders</div>
        <div class="kpi-value">${kpis.openOrdersCount}</div>
        <div class="kpi-sub">${formatNum(kpis.openOrderKg, 0)} kg remaining to dispatch</div>
      </div>
      <div class="card kpi-card">
        <div class="kpi-label">Dispatched Volume</div>
        <div class="kpi-value">${formatNum(kpis.totalDispatchedKg, 0)} kg</div>
        <div class="kpi-sub">${state.dispatches.length} delivery receipts logged</div>
      </div>
      <div class="card kpi-card">
        <div class="kpi-label">Recovery &amp; Alerts</div>
        <div class="kpi-value">${formatNum(kpis.avgRecoveryPct, 1)}%</div>
        <div class="kpi-sub">${kpis.criticalSkus} critical SKUs • ${kpis.activeSchedules} queued runs</div>
      </div>
    </div>

    <div class="kpi-grid">${sectionCards}</div>

    <div class="two-col">
      <div class="card">
        <div class="section-head">
          <h3>Priority SKU Watchlist</h3>
          <button class="btn btn-sm" data-goto-tab="inventory">All SKUs</button>
        </div>
        ${
          criticalList.length
            ? `<div class="table-wrap"><table><thead><tr><th>SKU</th><th>Available</th><th>Need</th><th>Status</th></tr></thead><tbody>
                ${criticalList
                  .map(
                    (s) => `<tr>
                      <td><strong>${escapeHtml(s.sku.productName)}</strong><div style="font-size:11px;color:var(--muted)">${escapeHtml(s.sku.section)}</div></td>
                      <td>${formatNum(s.availableBags, 1)} b (${formatNum(s.availableStockKg, 0)} kg)</td>
                      <td>${s.suggestedBags > 0 ? `${s.suggestedBags} b` : "—"}</td>
                      <td>${statusPill(s.status)}</td>
                    </tr>`
                  )
                  .join("")}
              </tbody></table></div>`
            : `<div class="empty-state">All SKUs are healthy right now.</div>`
        }
      </div>

      <div class="card">
        <div class="section-head">
          <h3>Active Customer Orders</h3>
          <button class="btn btn-sm" data-goto-tab="orders">All Orders</button>
        </div>
        ${
          openOrders.length
            ? `<div class="table-wrap"><table><thead><tr><th>Order</th><th>Customer</th><th>Product</th><th>Status</th></tr></thead><tbody>
                ${openOrders
                  .map(
                    (o) => `<tr>
                      <td><strong>${escapeHtml(o.orderCode)}</strong><div style="font-size:11px;color:var(--muted)">Due ${escapeHtml(o.dueDate)}</div></td>
                      <td>${escapeHtml(o.customer)}</td>
                      <td>${escapeHtml(o.productName)} (${o.orderedBags} b)</td>
                      <td>${statusPill(o.status)}</td>
                    </tr>`
                  )
                  .join("")}
              </tbody></table></div>`
            : `<div class="empty-state">No open customer orders.</div>`
        }
      </div>
    </div>
  `;
}

export function renderInventoryTab(state, summaries, activeRole) {
  const q = state.searchText.toLowerCase();
  const rows = summaries.filter((s) => {
    const matchesSearch =
      !q ||
      s.sku.productName.toLowerCase().includes(q) ||
      s.sku.section.toLowerCase().includes(q);
    const matchesSec = state.sectionFilter === "ALL" || s.sku.section === state.sectionFilter;
    const matchesStatus = state.inventoryStatusFilter === "ALL" || s.status === state.inventoryStatusFilter;
    return matchesSearch && matchesSec && matchesStatus;
  });

  return `
    <div class="card" style="display:grid; gap:12px;">
      <div class="section-head" style="margin-bottom:0;">
        <h3>Finished Goods &amp; SKU Stock (${rows.length})</h3>
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <input id="tab-search" class="search-input" placeholder="Filter product or plant..." value="${escapeHtml(state.searchText)}" />
          ${activeRole.canWriteOperations ? `<button id="add-sku-btn" class="btn btn-primary btn-sm">+ Add SKU</button>` : ""}
          <button id="export-csv-btn" class="btn btn-sm">Export CSV</button>
        </div>
      </div>

      <div class="sim-bar no-print">
        <div class="sim-group">
          <label>Plant:</label>
          <select id="inv-section-filter">
            <option value="ALL" ${state.sectionFilter === "ALL" ? "selected" : ""}>All Plants</option>
            ${PLANT_SECTIONS.map((s) => `<option value="${s}" ${state.sectionFilter === s ? "selected" : ""}>${s}</option>`).join("")}
          </select>
        </div>
        <div class="sim-group">
          <label>Health:</label>
          <select id="inv-status-filter">
            ${["ALL", "Critical", "Low Watch", "Healthy"].map((st) => `<option value="${st}" ${state.inventoryStatusFilter === st ? "selected" : ""}>${st === "ALL" ? "All Health" : st}</option>`).join("")}
          </select>
        </div>
        <div class="sim-group">
          <label>Lookback Days:</label>
          <select id="inv-lookback-select">
            ${[7, 14, 30].map((d) => `<option value="${d}" ${Number(state.simLookbackDays) === d ? "selected" : ""}>${d} days</option>`).join("")}
          </select>
        </div>
        <div class="sim-group">
          <label>Safety Buffer %:</label>
          <input id="inv-buffer-input" type="number" min="0" max="100" step="5" placeholder="Per SKU" value="${state.simBufferPct !== null ? state.simBufferPct : ""}" style="width:82px;" />
          <button id="inv-reset-sim-btn" class="btn btn-sm">Reset</button>
        </div>
      </div>

      <div class="table-wrap">
        <table>
          <thead><tr><th>Product</th><th>Section</th><th>Available</th><th>Open Orders</th><th>Need to Mill</th><th>Cover</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>
            ${
              rows.length
                ? rows
                    .map(
                      (s) => `<tr>
                        <td><strong>${escapeHtml(s.sku.productName)}</strong><div style="font-size:11px;color:var(--muted)">${s.sku.standardBagWeightKg}kg/bag • Rec ${formatNum(s.avgRecoveryPct, 1)}%</div></td>
                        <td>${escapeHtml(s.sku.section)}</td>
                        <td><strong>${formatNum(s.availableBags, 1)} bags</strong><div style="font-size:11px;color:var(--muted)">${formatNum(s.availableStockKg, 0)} kg</div></td>
                        <td>${formatNum(s.openOrdersKg, 0)} kg</td>
                        <td>${s.suggestedBags > 0 ? `${s.suggestedBags} bags (${formatNum(s.netProductionNeededKg, 0)} kg)` : "—"}</td>
                        <td>${Number.isFinite(s.daysOfCover) ? `${formatNum(s.daysOfCover, 1)} d` : "—"}</td>
                        <td>${statusPill(s.status)}</td>
                        <td class="action-cell">
                          ${
                            activeRole.canWriteOperations
                              ? `<button class="btn btn-sm" data-quick-schedule-sku="${escapeHtml(s.sku.id)}">Schedule</button>
                                 <button class="btn btn-sm" data-edit-sku="${escapeHtml(s.sku.id)}">Edit</button>`
                              : ""
                          }
                          ${activeRole.canDeleteSkus ? `<button class="btn btn-danger btn-sm" data-delete-sku="${escapeHtml(s.sku.id)}">Delete</button>` : ""}
                        </td>
                      </tr>`
                    )
                    .join("")
                : `<tr><td colspan="8" class="empty-state">No SKUs match the current filters.</td></tr>`
            }
          </tbody>
        </table>
      </div>
    </div>
  `;
}

export function renderOrdersTab(state, activeRole) {
  const q = state.searchText.toLowerCase();
  const rows = state.orders.filter((o) => {
    const matchesSearch =
      !q ||
      (o.orderCode || "").toLowerCase().includes(q) ||
      (o.customer || "").toLowerCase().includes(q) ||
      (o.productName || "").toLowerCase().includes(q);
    const f = state.orderStatusFilter;
    const matchesFilter =
      f === "ALL" ||
      (f === "PRIORITY" ? Boolean(o.priority || o.rushOrder) : o.status === f);
    return matchesSearch && matchesFilter;
  });

  return `
    <div class="card" style="display:grid; gap:12px;">
      <div class="section-head" style="margin-bottom:0;">
        <h3>Customer Orders (${rows.length})</h3>
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <input id="tab-search" class="search-input" placeholder="Search order, customer, SKU..." value="${escapeHtml(state.searchText)}" />
          ${activeRole.canWriteOperations ? `<button id="orders-add-btn" class="btn btn-primary btn-sm">+ New Order</button>` : ""}
          <button id="export-csv-btn" class="btn btn-sm">Export CSV</button>
        </div>
      </div>

      <div class="chip-row no-print">
        ${[
          ["ALL", "All"],
          ["Open", "Open"],
          ["Partially Dispatched", "Partial"],
          ["Fulfilled", "Fulfilled"],
          ["Cancelled", "Cancelled"],
          ["PRIORITY", "Priority / Rush"]
        ]
          .map(
            ([val, label]) =>
              `<button type="button" class="chip ${state.orderStatusFilter === val ? "active" : ""}" data-order-filter="${val}">${label}</button>`
          )
          .join("")}
      </div>

      <div class="table-wrap">
        <table>
          <thead><tr><th>Order</th><th>Customer</th><th>Product</th><th>Dispatched / Total</th><th>Due Date</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>
            ${
              rows.length
                ? rows
                    .map((o) => {
                      const canEdit = activeRole.canWriteOperations && (o.status === "Open" || o.status === "Partially Dispatched");
                      return `<tr>
                        <td><strong>${escapeHtml(o.orderCode)}</strong>${o.rushOrder ? ' <span class="pill pill-critical">RUSH</span>' : o.priority ? ' <span class="pill pill-warn">PRIORITY</span>' : ""}</td>
                        <td>${escapeHtml(o.customer)}</td>
                        <td>${escapeHtml(o.productName)} (${o.orderedBags} b)</td>
                        <td>${formatNum(o.dispatchedWeightKg, 0)} / ${formatNum(o.totalWeightKg, 0)} kg</td>
                        <td>${escapeHtml(o.dueDate)}</td>
                        <td>${statusPill(o.status)}</td>
                        <td class="action-cell">
                          ${
                            canEdit
                              ? `<button class="btn btn-sm" data-dispatch-order="${escapeHtml(o.id)}">Dispatch</button>
                                 <button class="btn btn-sm" data-schedule-order="${escapeHtml(o.id)}">Schedule</button>
                                 <button class="btn btn-sm" data-edit-order="${escapeHtml(o.id)}">Edit</button>
                                 <button class="btn btn-sm" data-cancel-order="${escapeHtml(o.id)}">Cancel</button>`
                              : ""
                          }
                          ${
                            activeRole.canWriteOperations && o.status === "Cancelled"
                              ? `<button class="btn btn-primary btn-sm" data-restore-order="${escapeHtml(o.id)}">Restore</button>`
                              : ""
                          }
                          ${activeRole.canWriteOperations ? `<button class="btn btn-danger btn-sm" data-delete-order="${escapeHtml(o.id)}">Delete</button>` : ""}
                        </td>
                      </tr>`;
                    })
                    .join("")
                : `<tr><td colspan="7" class="empty-state">No orders found.</td></tr>`
            }
          </tbody>
        </table>
      </div>
    </div>
  `;
}

export function renderDispatchedTab(state, activeRole) {
  const q = state.searchText.toLowerCase();
  const rows = state.dispatches.filter((d) => {
    const matchesSearch =
      !q ||
      (d.drReference || "").toLowerCase().includes(q) ||
      (d.customer || "").toLowerCase().includes(q) ||
      (d.productName || "").toLowerCase().includes(q) ||
      (d.driverName || "").toLowerCase().includes(q);
    const st = resolvedDispatchStatus(d);
    const matchesStatus = state.dispatchStatusFilter === "ALL" || st === state.dispatchStatusFilter;
    return matchesSearch && matchesStatus;
  });

  return `
    <div class="card" style="display:grid; gap:12px;">
      <div class="section-head" style="margin-bottom:0;">
        <h3>Dispatched Deliveries (${rows.length})</h3>
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <input id="tab-search" class="search-input" placeholder="Search DR, customer, SKU, driver..." value="${escapeHtml(state.searchText)}" />
          ${activeRole.canWriteOperations ? `<button id="dispatches-add-btn" class="btn btn-primary btn-sm">+ Log Dispatch</button>` : ""}
          <button id="export-csv-btn" class="btn btn-sm">Export CSV</button>
        </div>
      </div>

      <div class="chip-row no-print">
        ${["ALL", "Delivered", "Returned", "Held"]
          .map(
            (st) =>
              `<button type="button" class="chip ${state.dispatchStatusFilter === st ? "active" : ""}" data-dispatch-filter="${st}">${st === "ALL" ? "All Trips" : st}</button>`
          )
          .join("")}
      </div>

      <div class="table-wrap">
        <table>
          <thead><tr><th>Date</th><th>DR Ref</th><th>Customer</th><th>Product &amp; Fleet</th><th>Net Weight</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>
            ${
              rows.length
                ? rows
                    .map((d) => {
                      const st = resolvedDispatchStatus(d);
                      const fleet = [d.driverName, d.truckNumber].filter(Boolean).join(" / ");
                      return `<tr>
                        <td>${escapeHtml(d.date)}</td>
                        <td><strong>${escapeHtml(d.drReference)}</strong></td>
                        <td>${escapeHtml(d.customer)}</td>
                        <td><strong>${escapeHtml(d.productName)}</strong> (${d.bagsDispatched} b)${fleet ? `<div style="font-size:11px;color:var(--muted)">${escapeHtml(fleet)}</div>` : ""}</td>
                        <td>${formatNum(effectiveDispatchWeightKg(d), 0)} kg${d.returnConfirmed ? `<div style="font-size:11px;color:var(--critical)">Ret: ${d.returnedBags} b (${formatNum(d.returnedWeightKg, 0)} kg)</div>` : ""}</td>
                        <td>${statusPill(st)}</td>
                        <td class="action-cell">
                          ${
                            activeRole.canWriteOperations
                              ? `<button class="btn btn-sm" data-edit-dispatch="${escapeHtml(d.id)}">Edit</button>
                                 <button class="btn btn-danger btn-sm" data-delete-dispatch="${escapeHtml(d.id)}">Delete</button>`
                              : ""
                          }
                        </td>
                      </tr>`;
                    })
                    .join("")
                : `<tr><td colspan="7" class="empty-state">No dispatch receipts found.</td></tr>`
            }
          </tbody>
        </table>
      </div>
    </div>
  `;
}

export function renderScheduleTab(state, activeRole) {
  const rows = state.schedules.filter((s) => state.scheduleStatusFilter === "ALL" || s.status === state.scheduleStatusFilter);
  return `
    <div class="card" style="display:grid; gap:12px;">
      <div class="section-head" style="margin-bottom:0;">
        <h3>Scheduled Production Queue (${rows.length})</h3>
        ${activeRole.canWriteOperations ? `<button id="schedule-add-btn" class="btn btn-primary btn-sm">+ Schedule Milling Run</button>` : ""}
      </div>

      <div class="chip-row no-print">
        ${["ALL", "Scheduled", "In Progress", "Completed", "Cancelled"]
          .map(
            (st) =>
              `<button type="button" class="chip ${state.scheduleStatusFilter === st ? "active" : ""}" data-schedule-filter="${st}">${st === "ALL" ? "All Runs" : st}</button>`
          )
          .join("")}
      </div>

      <div class="table-wrap">
        <table>
          <thead><tr><th>Date</th><th>Section</th><th>Product</th><th>Target</th><th>Priority</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>
            ${
              rows.length
                ? rows
                    .map((s) => {
                      const canMutate = activeRole.canWriteOperations && (s.status === "Scheduled" || s.status === "In Progress");
                      return `<tr>
                        <td>${escapeHtml(s.scheduledDate)}</td>
                        <td>${escapeHtml(s.section)}</td>
                        <td><strong>${escapeHtml(s.productName)}</strong><div style="font-size:11px;color:var(--muted)">${escapeHtml(s.lineRemarks)}${s.linkedOrderCode ? ` • Order ${escapeHtml(s.linkedOrderCode)}` : ""}</div></td>
                        <td>${s.targetBags} bags (${formatNum(s.expectedWeightKg, 0)} kg)</td>
                        <td>${statusPill(s.priority)}</td>
                        <td>${statusPill(s.status)}</td>
                        <td class="action-cell">
                          ${
                            canMutate
                              ? `${s.status === "Scheduled" ? `<button class="btn btn-sm" data-start-schedule="${escapeHtml(s.id)}">Start</button>` : ""}
                                 <button class="btn btn-primary btn-sm" data-complete-schedule="${escapeHtml(s.id)}">Complete &amp; Log Batch</button>
                                 <button class="btn btn-sm" data-edit-schedule="${escapeHtml(s.id)}">Edit</button>
                                 <button class="btn btn-sm" data-cancel-schedule="${escapeHtml(s.id)}">Cancel</button>`
                              : ""
                          }
                          ${activeRole.canWriteOperations ? `<button class="btn btn-danger btn-sm" data-delete-schedule="${escapeHtml(s.id)}">Delete</button>` : ""}
                        </td>
                      </tr>`;
                    })
                    .join("")
                : `<tr><td colspan="7" class="empty-state">No production runs in this filter.</td></tr>`
            }
          </tbody>
        </table>
      </div>
    </div>
  `;
}

export function renderRecoveryTab(state, activeRole) {
  const sub = state.recoverySubTab || "batches";

  const subNav = `
    <div class="subtabs no-print">
      <button type="button" class="chip ${sub === "batches" ? "active" : ""}" data-recovery-sub="batches">Production Batches (${state.batches.length})</button>
      <button type="button" class="chip ${sub === "daily" ? "active" : ""}" data-recovery-sub="daily">Daily Plant Log</button>
      <button type="button" class="chip ${sub === "monitor" ? "active" : ""}" data-recovery-sub="monitor">Section Output &amp; Recovery Monitor</button>
    </div>
  `;

  if (sub === "daily") {
    const logs = computeDailyLogs({
      skus: state.skus,
      batches: state.batches,
      dispatches: state.dispatches,
      orders: state.orders
    });
    const activeDay = state.selectedDailyLogDate && logs.some((l) => l.date === state.selectedDailyLogDate)
      ? state.selectedDailyLogDate
      : logs[0]?.date || todayIsoDate();
    const dayLog = logs.find((l) => l.date === activeDay) || logs[0];
    return `
      ${subNav}
      <div class="card" style="display:grid; gap:12px;">
        <div class="section-head" style="margin-bottom:0;">
          <h3>Daily Plant Stock &amp; Movement Log</h3>
          <div class="sim-group">
            <label>Date:</label>
            <select id="daily-log-date-select">
              ${logs.map((l) => `<option value="${l.date}" ${l.date === activeDay ? "selected" : ""}>${l.date}${l.isToday ? " (Today)" : ""}</option>`).join("")}
            </select>
          </div>
        </div>
        ${
          dayLog
            ? `
              <div class="kpi-grid">
                <div class="card kpi-card"><div class="kpi-label">Closing Stock (${dayLog.date})</div><div class="kpi-value">${formatNum(dayLog.totalClosingStockKg, 0)} kg</div></div>
                <div class="card kpi-card"><div class="kpi-label">Produced That Day</div><div class="kpi-value">${formatNum(dayLog.totalProducedDayKg, 0)} kg</div></div>
                <div class="card kpi-card"><div class="kpi-label">Dispatched That Day</div><div class="kpi-value">${formatNum(dayLog.totalDispatchedDayKg, 0)} kg</div></div>
                <div class="card kpi-card"><div class="kpi-label">Orders Active / Placed</div><div class="kpi-value">${dayLog.dayOrders.length}</div></div>
              </div>
              <div class="table-wrap">
                <table>
                  <thead><tr><th>Product</th><th>Section</th><th>Produced</th><th>Dispatched</th><th>Closing Stock</th><th>Open Orders</th></tr></thead>
                  <tbody>
                    ${dayLog.skuRows
                      .map(
                        (r) => `<tr>
                          <td><strong>${escapeHtml(r.sku.productName)}</strong></td>
                          <td>${escapeHtml(r.sku.section)}</td>
                          <td>${r.producedDayBags > 0 ? `${r.producedDayBags} b (${formatNum(r.producedDayKg, 0)} kg)` : "—"}</td>
                          <td>${r.dispatchedDayBags > 0 ? `${r.dispatchedDayBags} b (${formatNum(r.dispatchedDayKg, 0)} kg)` : "—"}</td>
                          <td><strong>${formatNum(r.closingBags, 1)} b</strong> (${formatNum(r.closingStockKg, 0)} kg)</td>
                          <td>${formatNum(r.openOrdersSnapshotKg, 0)} kg</td>
                        </tr>`
                      )
                      .join("")}
                  </tbody>
                </table>
              </div>
            `
            : `<div class="empty-state">No activity logs recorded yet.</div>`
        }
      </div>
    `;
  }

  if (sub === "monitor") {
    const sectionRows = PLANT_SECTIONS.map((sec) => {
      const sb = state.batches.filter((b) => b.section === sec);
      const exp = sb.reduce((s, b) => s + Number(b.expectedWeightKg || 0), 0);
      const rec = sb.reduce((s, b) => s + Number(b.actualRecoveredKg || 0), 0);
      const pct = exp > 0 ? (rec / exp) * 100 : 100;
      const queuedBags = state.schedules
        .filter((r) => r.section === sec && (r.status === "Scheduled" || r.status === "In Progress"))
        .reduce((s, r) => s + Number(r.targetBags || 0), 0);
      return `
        <div class="card" style="display:grid; gap:8px;">
          <div style="display:flex; justify-content:space-between; align-items:center;">
            <strong>${escapeHtml(sec)}</strong>
            ${statusPill(pct >= 98.5 ? "Healthy" : pct >= 95 ? "Low Watch" : "Critical")}
          </div>
          <div style="font-size:20px; font-weight:800;">${formatNum(rec, 0)} kg <span style="font-size:13px;font-weight:600;color:var(--muted)">(${formatNum(pct, 2)}% recovery)</span></div>
          <div class="meter ${pct < 95 ? "critical" : pct < 98.5 ? "warn" : ""}"><span style="width:${Math.min(100, Math.max(5, pct))}%"></span></div>
          <div style="font-size:12px; color:var(--muted);">${sb.length} logged batches • ${queuedBags} bags queued in schedule</div>
        </div>
      `;
    }).join("");
    return `${subNav}<div class="two-col">${sectionRows}</div>`;
  }

  return `
    ${subNav}
    <div class="card" style="display:grid; gap:12px;">
      <div class="section-head" style="margin-bottom:0;">
        <h3>Production Recovery Batches (${state.batches.length})</h3>
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          ${
            activeRole.canWriteOperations
              ? `<button id="batch-add-btn" class="btn btn-primary btn-sm">+ Log Batch</button>
                 <button id="batch-bulk-btn" class="btn btn-sm">Bulk Paste Import</button>`
              : ""
          }
          <button id="export-csv-btn" class="btn btn-sm">Export CSV</button>
        </div>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Date</th><th>Section</th><th>Product</th><th>Bags</th><th>Expected vs Recovered</th><th>Recovery %</th><th>Actions</th></tr></thead>
          <tbody>
            ${
              state.batches.length
                ? state.batches
                    .map(
                      (b) => `<tr>
                        <td>${escapeHtml(b.date)}</td>
                        <td>${escapeHtml(b.section)}</td>
                        <td><strong>${escapeHtml(b.productName)}</strong><div style="font-size:11px;color:var(--muted)">${escapeHtml(b.remarks)}</div></td>
                        <td>${b.bagsProduced}</td>
                        <td>${formatNum(b.expectedWeightKg, 0)} / ${formatNum(b.actualRecoveredKg, 0)} kg</td>
                        <td>${statusPill(Number(b.recoveryPct || 100) < 97 ? "Critical" : Number(b.recoveryPct || 100) < 98.5 ? "Low Watch" : "Healthy")} ${formatNum(b.recoveryPct, 1)}%</td>
                        <td class="action-cell">
                          ${
                            activeRole.canWriteOperations
                              ? `<button class="btn btn-sm" data-edit-batch="${escapeHtml(b.id)}">Edit</button>
                                 <button class="btn btn-danger btn-sm" data-delete-batch="${escapeHtml(b.id)}">Delete</button>`
                              : ""
                          }
                        </td>
                      </tr>`
                    )
                    .join("")
                : `<tr><td colspan="7" class="empty-state">No production batches logged yet.</td></tr>`
            }
          </tbody>
        </table>
      </div>
    </div>
  `;
}

export function renderMaterialsTab(state, activeRole) {
  const sub = state.materialsSubTab || "all";
  const subNav = `
    <div class="subtabs no-print">
      <button type="button" class="chip ${sub === "all" ? "active" : ""}" data-materials-sub="all">All Materials (${state.rawMaterials.length})</button>
      <button type="button" class="chip ${sub === "macro" ? "active" : ""}" data-materials-sub="macro">Macro Materials</button>
      <button type="button" class="chip ${sub === "micro" ? "active" : ""}" data-materials-sub="micro">Micro Materials (Fast / Slow / Non-Moving)</button>
      <button type="button" class="chip ${sub === "bom" ? "active" : ""}" data-materials-sub="bom">Feed BOM Formulas (${state.boms.length})</button>
    </div>
  `;

  if (sub === "bom") {
    const matByName = new Map(state.rawMaterials.map((m) => [String(m.name || "").trim().toLowerCase(), m]));
    return `
      ${subNav}
      <div class="card" style="display:grid; gap:12px;">
        <div class="section-head" style="margin-bottom:0;">
          <h3>Feed BOM Formulas (${state.boms.length})</h3>
          ${activeRole.canWriteOperations ? `<button id="bom-add-btn" class="btn btn-primary btn-sm">+ Create BOM Formula</button>` : ""}
        </div>
        ${
          state.boms.length
            ? `<div class="two-col">
                ${state.boms
                  .map((bom) => {
                    const totalPct = (bom.lines || []).reduce((s, l) => s + Number(l.inclusionPct || 0), 0);
                    let minBatches = Infinity;
                    const linesHtml = (bom.lines || [])
                      .map((l) => {
                        const reqKg = (Number(bom.batchBaseKg || 1000) * Number(l.inclusionPct || 0)) / 100;
                        const mat = matByName.get(String(l.materialName || "").trim().toLowerCase());
                        const onHand = mat ? Number(mat.currentStockKg || 0) : 0;
                        const maxB = reqKg > 0 ? Math.floor(onHand / reqKg) : 999;
                        if (maxB < minBatches) minBatches = maxB;
                        return `<tr>
                          <td>${escapeHtml(l.materialName)}</td>
                          <td>${formatNum(l.inclusionPct, 2)}%</td>
                          <td>${formatNum(reqKg, 1)} kg</td>
                          <td>${mat ? `${formatNum(onHand, 0)} kg (${maxB}x)` : "Not in stock"}</td>
                        </tr>`;
                      })
                      .join("");
                    const readyBatches = Number.isFinite(minBatches) ? minBatches : 0;
                    return `
                      <div class="card" style="display:grid; gap:10px;">
                        <div class="section-head" style="margin-bottom:0;">
                          <div>
                            <strong>${escapeHtml(bom.productName)}</strong>
                            <div style="font-size:12px;color:var(--muted)">${escapeHtml(bom.section)} • Batch ${formatNum(bom.batchBaseKg, 0)} kg • Inclusion ${formatNum(totalPct, 1)}%</div>
                          </div>
                          ${statusPill(readyBatches >= 3 ? "Healthy" : readyBatches >= 1 ? "Low Watch" : "Critical")}
                        </div>
                        <div style="font-size:12px; color:var(--muted);">Max millable batches from current raw stock: <strong>${readyBatches}</strong></div>
                        <div class="table-wrap">
                          <table>
                            <thead><tr><th>Ingredient</th><th>%</th><th>Per Batch</th><th>Stock</th></tr></thead>
                            <tbody>${linesHtml}</tbody>
                          </table>
                        </div>
                        ${
                          activeRole.canWriteOperations
                            ? `<div class="action-cell">
                                <button class="btn btn-sm" data-edit-bom="${escapeHtml(bom.id)}">Edit Formula</button>
                                <button class="btn btn-danger btn-sm" data-delete-bom="${escapeHtml(bom.id)}">Delete</button>
                              </div>`
                            : ""
                        }
                      </div>
                    `;
                  })
                  .join("")}
              </div>`
            : `<div class="empty-state">No Feed BOM formulas saved yet. Click "+ Create BOM Formula" to add one.</div>`
        }
      </div>
    `;
  }

  const forecasts = computeRawMaterialForecasts({
    materials: state.rawMaterials,
    batches: state.batches,
    schedules: state.schedules,
    boms: state.boms,
    lookbackDays: state.simLookbackDays,
    demandMultiplier: state.rawDemandMultiplier,
    includeScheduledDemand: state.rawIncludeScheduled,
    leadTimeOverrideDays: state.rawLeadOverrideDays
  });

  const q = state.searchText.toLowerCase();
  const isMicroTab = sub === "micro" ? true : sub === "macro" ? false : null;
  const missingCatalog = missingCatalogEntities(state.rawMaterials, isMicroTab);

  const filtered = forecasts.filter((f) => {
    if (sub === "macro" && f.sector.isMicro) return false;
    if (sub === "micro" && !f.sector.isMicro) return false;
    if (state.rawSectorFilter !== "ALL" && f.sector.id !== state.rawSectorFilter) return false;
    if (state.rawRiskFilter !== "ALL" && f.status !== state.rawRiskFilter) return false;
    if (!q) return true;
    return (
      String(f.material.name || "").toLowerCase().includes(q) ||
      String(f.material.category || "").toLowerCase().includes(q) ||
      String(f.material.supplierName || "").toLowerCase().includes(q) ||
      f.movementTag.toLowerCase().includes(q)
    );
  });

  return `
    ${subNav}
    ${
      activeRole.canWriteOperations && missingCatalog.length > 0
        ? `<div class="banner">
            <div>
              <strong>Plant Raw Material Catalog (${missingCatalog.length} missing items)</strong>
              <div style="font-size:12px; color:#36453e;">Add the remaining standard ${sub === "micro" ? "Micro" : sub === "macro" ? "Macro" : "Macro & Micro"} plant materials in 1 click.</div>
            </div>
            <button id="restore-raw-catalog-btn" class="btn btn-primary btn-sm">Add Missing ${missingCatalog.length} Materials</button>
          </div>`
        : ""
    }

    <div class="card" style="display:grid; gap:12px;">
      <div class="section-head" style="margin-bottom:0;">
        <h3>Raw Material Forecast &amp; Stock (${filtered.length})</h3>
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <input id="tab-search" class="search-input" placeholder="Search material, supplier, movement..." value="${escapeHtml(state.searchText)}" />
          ${activeRole.canWriteOperations ? `<button id="raw-add-btn" class="btn btn-primary btn-sm">+ Add Material</button>` : ""}
          <button id="export-csv-btn" class="btn btn-sm">Export CSV</button>
        </div>
      </div>

      <div class="sim-bar no-print">
        <div class="sim-group">
          <label>Demand Pace:</label>
          <select id="raw-mult-select">
            ${[0.5, 0.75, 1.0, 1.25, 1.5, 2.0].map((v) => `<option value="${v}" ${Number(state.rawDemandMultiplier) === v ? "selected" : ""}>${v}x</option>`).join("")}
          </select>
        </div>
        <div class="sim-group">
          <label><input id="raw-sched-toggle" type="checkbox" ${state.rawIncludeScheduled ? "checked" : ""} /> Include Scheduled Queue</label>
        </div>
        <div class="sim-group">
          <label>Lead Time Override (d):</label>
          <input id="raw-lead-input" type="number" min="1" max="60" placeholder="Auto" value="${state.rawLeadOverrideDays !== null ? state.rawLeadOverrideDays : ""}" style="width:70px;" />
        </div>
        <div class="sim-group">
          <label>Risk:</label>
          <select id="raw-risk-select">
            ${["ALL", "Order Now", "Running Low", "Healthy"].map((r) => `<option value="${r}" ${state.rawRiskFilter === r ? "selected" : ""}>${r === "ALL" ? "All" : r}</option>`).join("")}
          </select>
        </div>
      </div>

      <div class="table-wrap">
        <table>
          <thead><tr><th>Material &amp; Sector</th><th>On Hand</th><th>Usage / Day</th><th>Runway</th><th>Recommended Restock</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>
            ${
              filtered.length
                ? filtered
                    .map(
                      (f) => `<tr>
                        <td>
                          <strong>${escapeHtml(f.material.name)}</strong>
                          <div style="font-size:11px;color:var(--muted)">${escapeHtml(f.sector.shortLabel)} • ${escapeHtml(f.movementTag)} • Min ${formatNum(f.material.reorderThresholdKg, 0)} kg</div>
                        </td>
                        <td><strong>${formatNum(f.material.currentStockKg, 1)} kg</strong></td>
                        <td>${formatNum(f.simulatedAvgDailyKg, 1)} kg/d</td>
                        <td>${escapeHtml(f.projectedRunOutLabel)}</td>
                        <td>${f.recommendedRestockKg > 0 ? `+${formatNum(f.recommendedRestockKg, 0)} kg` : "OK"}</td>
                        <td>${statusPill(f.status)}</td>
                        <td class="action-cell">
                          ${
                            activeRole.canWriteOperations
                              ? `<button class="btn btn-primary btn-sm" data-adjust-raw="${escapeHtml(f.material.id)}">+/- Stock</button>
                                 <button class="btn btn-sm" data-edit-raw="${escapeHtml(f.material.id)}">Edit</button>
                                 <button class="btn btn-danger btn-sm" data-delete-raw="${escapeHtml(f.material.id)}">Delete</button>`
                              : ""
                          }
                        </td>
                      </tr>`
                    )
                    .join("")
                : `<tr><td colspan="7" class="empty-state">No raw materials match the current filters.</td></tr>`
            }
          </tbody>
        </table>
      </div>
    </div>
  `;
}

export function renderReprocessTab(state, activeRole) {
  const linkedDispatchIds = new Set(state.reprocessItems.map((r) => r.sourceDispatchId).filter(Boolean));
  const unqueuedReturned = state.dispatches.filter(
    (d) => d.returnConfirmed && Number(d.returnedBags || 0) > 0 && d.id && !linkedDispatchIds.has(d.id)
  );

  const isDam = (r) => !r.flaggedToInventory && String(r.status || "").toLowerCase() === "damaged";
  const isPend = (r) => !r.flaggedToInventory && !isDam(r);
  const f = state.reprocessFilter;
  const rows = state.reprocessItems.filter((item) => {
    if (f === "FAST") return isPend(item) && Boolean(item.fastActionNeeded);
    if (f === "PENDING") return isPend(item) && item.status === "Pending Reprocess";
    if (f === "REPROCESSING") return isPend(item) && item.status === "Reprocessing";
    if (f === "FLAGGED") return Boolean(item.flaggedToInventory);
    if (f === "DAMAGED") return isDam(item);
    return true;
  });

  return `
    ${
      activeRole.canWriteOperations && unqueuedReturned.length
        ? `<div class="banner banner-warn">
            <div>
              <strong>Unqueued Returned Dispatches (${unqueuedReturned.length})</strong>
              <div style="font-size:12px; color:#5a3c07;">Queue confirmed delivery returns into Reprocess tracking: ${unqueuedReturned.map((d) => escapeHtml(d.drReference)).slice(0, 3).join(", ")}</div>
            </div>
            <div class="action-cell">
              ${unqueuedReturned
                .slice(0, 3)
                .map((d) => `<button class="btn btn-primary btn-sm" data-queue-return-dispatch="${escapeHtml(d.id)}">Queue ${escapeHtml(d.drReference)}</button>`)
                .join("")}
            </div>
          </div>`
        : ""
    }

    <div class="card" style="display:grid; gap:12px;">
      <div class="section-head" style="margin-bottom:0;">
        <h3>Reprocess Quarantine (${rows.length})</h3>
        ${activeRole.canWriteOperations ? `<button id="reprocess-add-btn" class="btn btn-primary btn-sm">+ Log Reprocess Item</button>` : ""}
      </div>

      <div class="chip-row no-print">
        ${[
          ["ALL", "All"],
          ["FAST", "Fast Action"],
          ["PENDING", "Pending Reprocess"],
          ["REPROCESSING", "Reprocessing"],
          ["FLAGGED", "Moved to Stock"],
          ["DAMAGED", "Damaged"]
        ]
          .map(
            ([val, label]) =>
              `<button type="button" class="chip ${state.reprocessFilter === val ? "active" : ""}" data-reprocess-filter="${val}">${label}</button>`
          )
          .join("")}
      </div>

      <div class="table-wrap">
        <table>
          <thead><tr><th>Returned Date</th><th>Product &amp; DR</th><th>Returned</th><th>Reason / Notes</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>
            ${
              rows.length
                ? rows
                    .map((r) => {
                      const pend = isPend(r);
                      return `<tr>
                        <td>${escapeHtml(r.returnedDate)}</td>
                        <td><strong>${escapeHtml(r.productName)}</strong><div style="font-size:11px;color:var(--muted)">${escapeHtml(r.section)} • ${escapeHtml(r.drReference || "Manual")} ${r.customer ? `• ${escapeHtml(r.customer)}` : ""}</div></td>
                        <td>${r.returnedBags} b (${formatNum(r.returnedWeightKg, 0)} kg)${r.flaggedToInventory ? `<div style="font-size:11px;color:var(--emerald)">Restored: ${r.reprocessedBags} b (${formatNum(r.reprocessedWeightKg, 0)} kg)</div>` : ""}</td>
                        <td>${escapeHtml(r.returnReason)}${r.fastActionNeeded ? ' <span class="pill pill-critical">FAST ACTION</span>' : ""}${r.actionNotes ? `<div style="font-size:11px;color:var(--muted)">${escapeHtml(r.actionNotes)}</div>` : ""}</td>
                        <td>${statusPill(r.status)}</td>
                        <td class="action-cell">
                          ${
                            activeRole.canWriteOperations && pend
                              ? `${r.status === "Pending Reprocess" ? `<button class="btn btn-sm" data-start-reprocess="${escapeHtml(r.id)}">Start Reprocess</button>` : ""}
                                 <button class="btn btn-primary btn-sm" data-flag-reprocess="${escapeHtml(r.id)}">Move to Stock</button>
                                 <button class="btn btn-sm" data-damage-reprocess="${escapeHtml(r.id)}">Mark Damaged</button>
                                 <button class="btn btn-sm" data-edit-reprocess="${escapeHtml(r.id)}">Edit</button>`
                              : ""
                          }
                          ${activeRole.canWriteOperations ? `<button class="btn btn-danger btn-sm" data-delete-reprocess="${escapeHtml(r.id)}">Delete</button>` : ""}
                        </td>
                      </tr>`;
                    })
                    .join("")
                : `<tr><td colspan="6" class="empty-state">No reprocess items in this filter.</td></tr>`
            }
          </tbody>
        </table>
      </div>
    </div>
  `;
}

export function renderCustomersTab(state, activeRole) {
  const q = state.searchText.toLowerCase();
  const rows = state.customers.filter(
    (c) => !q || (c.name || "").toLowerCase().includes(q) || (c.notes || "").toLowerCase().includes(q)
  );
  return `
    <div class="card" style="display:grid; gap:12px;">
      <div class="section-head" style="margin-bottom:0;">
        <h3>Customer Directory (${rows.length})</h3>
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <input id="tab-search" class="search-input" placeholder="Search customer..." value="${escapeHtml(state.searchText)}" />
          ${activeRole.canWriteOperations ? `<button id="customer-add-btn" class="btn btn-primary btn-sm">+ Add Customer</button>` : ""}
        </div>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Customer</th><th>Notes</th><th>Open Orders</th><th>Dispatched</th><th>Actions</th></tr></thead>
          <tbody>
            ${
              rows.length
                ? rows
                    .map((c) => {
                      const custKey = String(c.name || "").trim().toLowerCase();
                      const openOrd = state.orders.filter(
                        (o) => String(o.customer || "").trim().toLowerCase() === custKey && (o.status === "Open" || o.status === "Partially Dispatched")
                      ).length;
                      const dispKg = state.dispatches
                        .filter((d) => String(d.customer || "").trim().toLowerCase() === custKey)
                        .reduce((s, d) => s + effectiveDispatchWeightKg(d), 0);
                      return `<tr>
                        <td><strong>${escapeHtml(c.name)}</strong>${c.priority ? ' <span class="pill pill-warn">PRIORITY</span>' : ""}</td>
                        <td>${escapeHtml(c.notes || "—")}</td>
                        <td>${openOrd}</td>
                        <td>${formatNum(dispKg, 0)} kg</td>
                        <td class="action-cell">
                          ${
                            activeRole.canWriteOperations
                              ? `<button class="btn btn-sm" data-edit-customer="${escapeHtml(c.id)}">Edit</button>
                                 <button class="btn btn-danger btn-sm" data-delete-customer="${escapeHtml(c.id)}">Delete</button>`
                              : ""
                          }
                        </td>
                      </tr>`;
                    })
                    .join("")
                : `<tr><td colspan="5" class="empty-state">No customers recorded yet.</td></tr>`
            }
          </tbody>
        </table>
      </div>
    </div>
  `;
}

export function renderReportsTab(state, summaries, activeRole) {
  const report = buildPlantReport({
    startDate: state.reportStartDate,
    endDate: state.reportEndDate,
    sectionFilter: state.reportSectionFilter,
    includedSections: state.reportIncludedSections,
    skus: state.skus,
    batches: state.batches,
    dispatches: state.dispatches,
    orders: state.orders,
    schedules: state.schedules,
    summaries
  });

  return `
    <div class="card" style="display:grid; gap:12px;">
      <div class="section-head" style="margin-bottom:0;">
        <div>
          <h3>Bulacan Plant Reports &amp; Exports</h3>
          <div style="font-size:12px; color:var(--muted);">Period: ${report.isDailyReport ? report.startDate : `${report.startDate} to ${report.endDate}`} • Plant: ${escapeHtml(report.sectionFilter)}</div>
        </div>
        <div class="action-cell no-print">
          <button id="report-download-csv-btn" class="btn btn-sm">Download CSV</button>
          <button id="report-download-xls-btn" class="btn btn-sm">Download Excel (.xls)</button>
          <button id="report-print-pdf-btn" class="btn btn-primary btn-sm">Print / Save PDF</button>
          <button id="report-copy-messenger-btn" class="btn btn-sm">Copy Messenger Summary</button>
        </div>
      </div>

      <div class="sim-bar no-print">
        <div class="sim-group">
          <label>Start:</label>
          <input id="report-start-date" type="date" value="${escapeHtml(state.reportStartDate)}" />
        </div>
        <div class="sim-group">
          <label>End:</label>
          <input id="report-end-date" type="date" value="${escapeHtml(state.reportEndDate)}" />
        </div>
        <div class="sim-group">
          <label>Plant:</label>
          <select id="report-section-select">
            <option value="ALL" ${state.reportSectionFilter === "ALL" ? "selected" : ""}>All Plants</option>
            ${PLANT_SECTIONS.map((s) => `<option value="${s}" ${state.reportSectionFilter === s ? "selected" : ""}>${s}</option>`).join("")}
          </select>
        </div>
        <button id="report-today-btn" class="btn btn-sm">Today Only</button>
      </div>

      <div class="chip-row no-print">
        <span style="font-size:12px; font-weight:700; color:var(--muted);">Include Sections:</span>
        ${REPORT_SECTION_OPTIONS.map((opt) => {
          const checked = state.reportIncludedSections.includes(opt);
          return `<button type="button" class="chip ${checked ? "active" : ""}" data-toggle-report-section="${opt}">${opt}</button>`;
        }).join("")}
      </div>

      <div class="kpi-grid">
        <div class="card kpi-card"><div class="kpi-label">Available Stock</div><div class="kpi-value">${formatNum(report.totalAvailableStockKg, 0)} kg</div><div class="kpi-sub">${formatNum(report.totalAvailableBags, 1)} bags</div></div>
        <div class="card kpi-card"><div class="kpi-label">Produced in Period</div><div class="kpi-value">${report.totalProducedBags} b</div><div class="kpi-sub">${formatNum(report.totalRecoveredKg, 0)} kg (${formatNum(report.avgRecoveryPct, 2)}% rec)</div></div>
        <div class="card kpi-card"><div class="kpi-label">Dispatched in Period</div><div class="kpi-value">${formatNum(report.totalDispatchedKg, 0)} kg</div><div class="kpi-sub">${report.filteredDispatches.length} trips (${report.deliveredCount} Del, ${report.returnedCount} Ret, ${report.heldCount} Held)</div></div>
        <div class="card kpi-card"><div class="kpi-label">Active Open Orders</div><div class="kpi-value">${report.activeOpenOrders.length}</div><div class="kpi-sub">${formatNum(report.totalOpenOrdersKg, 0)} kg remaining</div></div>
      </div>

      <div class="table-wrap">
        <table>
          <thead><tr><th>Plant Section</th><th>SKUs</th><th>Available Stock</th><th>Produced Bags</th><th>Recovered Kg</th><th>Recovery %</th></tr></thead>
          <tbody>
            ${report.sectionSummaries
              .map(
                (s) => `<tr>
                  <td><strong>${escapeHtml(s.section)}</strong></td>
                  <td>${s.skuCount}</td>
                  <td>${formatNum(s.totalAvailableBags, 1)} b (${formatNum(s.totalAvailableStockKg, 0)} kg)</td>
                  <td>${s.totalBags}</td>
                  <td>${formatNum(s.totalRecoveredKg, 0)} kg</td>
                  <td>${formatNum(s.recoveryPct, 2)}%</td>
                </tr>`
              )
              .join("")}
          </tbody>
        </table>
      </div>
    </div>

    <div class="card no-print" style="display:grid; gap:10px;">
      <div class="section-head" style="margin-bottom:0;">
        <h3>JSON Backup &amp; Workspace Administration</h3>
        <div class="action-cell">
          <button id="export-json-backup-btn" class="btn btn-sm">Download JSON Backup</button>
          ${
            activeRole.canResetWorkspace
              ? `<button id="restore-json-backup-btn" class="btn btn-sm">Restore JSON Backup</button>
                 <button id="selective-reset-btn" class="btn btn-danger btn-sm">Selective Reset Data</button>`
              : ""
          }
        </div>
      </div>
      <div style="font-size:12px; color:var(--muted);">
        Download a complete snapshot of SKUs, customers, orders, dispatches, schedules, batches, raw materials, BOMs, and reprocess items. Admin role is required to restore backups or run selective collection resets.
      </div>
    </div>
  `;
}

export function renderTeamTab(state, activeRole, { isLocal, currentUrl, portalUrl }) {
  const pendingInvites = state.workspaceInvites.filter((i) => i.status === "Pending");
  const ownerEmailDisplay =
    state.activeWorkspaceOwnerId === state.user?.uid
      ? state.user?.email
      : state.workspaceInvites.find((i) => i.ownerEmail)?.ownerEmail || "Owner";

  return `
    <div class="card" style="display:grid; gap:10px;">
      <div class="section-head" style="margin-bottom:0;">
        <h3>Computer &amp; Web Portal Connection</h3>
        <button id="copy-web-url-btn" class="btn btn-sm">${isLocal ? "Copy Active Local URL" : "Copy Website URL"}</button>
      </div>
      <div style="font-size:13px; color:var(--muted); line-height:1.5;">
        This web app connects directly to Firebase project <strong>gen-lang-client-0293694787</strong> and the live Bulacan Plant Firestore database, syncing in real time with your Android app.
      </div>
      <div style="font-size:12px; background:var(--surface-alt); border:1px solid var(--border); border-radius:10px; padding:10px 12px; word-break:break-all; display:grid; gap:6px;">
        <div><strong>${isLocal ? "Active Local Portal URL:" : "Portal URL:"}</strong> <span>${escapeHtml(currentUrl)}</span></div>
        ${
          isLocal
            ? `<div style="color:var(--muted); line-height:1.45;">
                <strong>Public Hosting URL (once deployed):</strong> <strong>${escapeHtml(portalUrl)}</strong><br/>
                If <strong>${escapeHtml(portalUrl)}</strong> shows <em>"Site Not Found"</em>, run <strong>deploy-web.bat</strong> once from your project folder to publish the <code>web</code> folder online.
              </div>`
            : ""
        }
      </div>
    </div>

    <div class="card">
      <div class="section-head">
        <h3>Team Members (${state.teamMembers.length + 1})</h3>
        ${activeRole.canManageTeam ? `<button id="team-invite-btn" class="btn btn-primary btn-sm">Invite Team Member</button>` : ""}
      </div>
      <div class="role-legend">
        <span><strong>Viewer:</strong> Read-only dashboards &amp; CSV reports</span>
        <span><strong>Operator:</strong> Log orders, dispatches, batches, BOMs &amp; stock</span>
        <span><strong>Admin:</strong> Full plant control &amp; switch team roles</span>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Member</th><th>Email</th><th>Access Level</th><th>Actions</th></tr></thead>
          <tbody>
            <tr>
              <td><strong>Workspace Owner</strong></td>
              <td>${escapeHtml(ownerEmailDisplay)}</td>
              <td>${statusPill("Admin")}</td>
              <td>Owner</td>
            </tr>
            ${state.teamMembers
              .map((m) => {
                const canEditThisMember =
                  activeRole.canManageTeam &&
                  m.userId !== state.user?.uid &&
                  m.userId !== state.activeWorkspaceOwnerId;
                const isUpdating = state.updatingAccessKeys.has(`member:${m.userId}`);
                const roleCell = canEditThisMember
                  ? renderRoleSwitcher({
                      currentRole: m.role,
                      targetType: "member",
                      memberUid: m.userId,
                      inviteId: m.inviteId || "",
                      memberEmail: m.email || "",
                      memberName: m.displayName || m.email || "",
                      disabled: isUpdating
                    })
                  : statusPill(resolveRole(m.role).label);
                return `<tr>
                  <td><strong>${escapeHtml(m.displayName || m.email)}</strong>${m.userId === state.user?.uid ? ' <span style="font-size:11px;color:var(--muted)">(You)</span>' : ""}</td>
                  <td>${escapeHtml(m.email)}</td>
                  <td>${roleCell}</td>
                  <td>
                    ${
                      canEditThisMember
                        ? `<button class="btn btn-danger btn-sm" data-remove-member="${escapeHtml(m.userId)}" data-invite-id="${escapeHtml(m.inviteId || "")}">Remove</button>`
                        : "—"
                    }
                  </td>
                </tr>`;
              })
              .join("")}
          </tbody>
        </table>
      </div>
    </div>

    <div class="card">
      <div class="section-head"><h3>Pending Invitations (${pendingInvites.length})</h3></div>
      ${
        pendingInvites.length
          ? `<div class="table-wrap"><table><thead><tr><th>Invited Email</th><th>Access Level</th><th>Delivery</th><th>Actions</th></tr></thead><tbody>
              ${pendingInvites
                .map((inv) => {
                  const isUpdating = state.updatingAccessKeys.has(`invite:${inv.id}`);
                  const roleCell = activeRole.canManageTeam
                    ? renderRoleSwitcher({
                        currentRole: inv.role,
                        targetType: "invite",
                        inviteId: inv.id,
                        memberEmail: inv.memberEmail || "",
                        memberName: inv.memberDisplayName || inv.memberEmail || "",
                        disabled: isUpdating
                      })
                    : statusPill(resolveRole(inv.role).label);
                  return `<tr>
                    <td><strong>${escapeHtml(inv.memberEmail)}</strong></td>
                    <td>${roleCell}</td>
                    <td>${escapeHtml(inv.emailDeliveryStatus || "Pending sign-in")}</td>
                    <td class="action-cell">
                      <button class="btn btn-sm" data-gmail-invite="${escapeHtml(inv.id)}">Open Gmail</button>
                      <button class="btn btn-sm" data-copy-invite-link="${escapeHtml(inv.id)}">Copy Link</button>
                      ${
                        activeRole.canManageTeam
                          ? `<button class="btn btn-danger btn-sm" data-cancel-invite="${escapeHtml(inv.id)}">Cancel</button>`
                          : ""
                      }
                    </td>
                  </tr>`;
                })
                .join("")}
            </tbody></table></div>`
          : `<div class="empty-state">No pending team invitations.</div>`
      }
    </div>
  `;
}

export function renderHistoryTab(state, activeRole) {
  const q = state.searchText.toLowerCase();
  const rows = state.breadcrumbs.filter((b) => {
    const matchesSearch =
      !q ||
      (b.action || "").toLowerCase().includes(q) ||
      (b.details || "").toLowerCase().includes(q) ||
      (b.category || "").toLowerCase().includes(q);
    const matchesCat = state.historyCategoryFilter === "ALL" || b.category === state.historyCategoryFilter;
    return matchesSearch && matchesCat;
  });

  return `
    <div class="card" style="display:grid; gap:12px;">
      <div class="section-head" style="margin-bottom:0;">
        <h3>Activity &amp; Audit History (${rows.length})</h3>
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <input id="tab-search" class="search-input" placeholder="Search action or details..." value="${escapeHtml(state.searchText)}" />
          ${activeRole.canResetWorkspace && state.breadcrumbs.length ? `<button id="clear-history-btn" class="btn btn-danger btn-sm">Clear All History</button>` : ""}
        </div>
      </div>

      <div class="chip-row no-print">
        ${["ALL", "Orders", "Dispatch", "Schedule", "Recovery", "Inventory", "Reprocess", "Customers", "Reports", "System"]
          .map(
            (cat) =>
              `<button type="button" class="chip ${state.historyCategoryFilter === cat ? "active" : ""}" data-history-cat="${cat}">${cat === "ALL" ? "All Categories" : cat}</button>`
          )
          .join("")}
      </div>

      <div class="table-wrap">
        <table>
          <thead><tr><th>Date / Time</th><th>Category</th><th>Action</th><th>Details</th><th>Severity</th><th>Actions</th></tr></thead>
          <tbody>
            ${
              rows.length
                ? rows
                    .slice(0, 100)
                    .map(
                      (b) => `<tr>
                        <td>${escapeHtml(b.dateStr)} ${escapeHtml(b.timeLabel)}</td>
                        <td>${escapeHtml(b.category)}</td>
                        <td><strong>${escapeHtml(b.action)}</strong></td>
                        <td>${escapeHtml(b.details)}</td>
                        <td>${statusPill(b.severity)}</td>
                        <td>${activeRole.canResetWorkspace ? `<button class="btn btn-danger btn-sm" data-delete-breadcrumb="${escapeHtml(b.id)}">Delete</button>` : "—"}</td>
                      </tr>`
                    )
                    .join("")
                : `<tr><td colspan="6" class="empty-state">No activity history entries found.</td></tr>`
            }
          </tbody>
        </table>
      </div>
    </div>
  `;
}
