import {
  LINE_REMARKS_OPTIONS,
  PLANT_SECTIONS,
  RAW_CATEGORIES,
  REPROCESS_STATUSES,
  RETURN_REASONS,
  SCHEDULE_PRIORITIES,
  buildGmailWebComposeUrl,
  buildMailtoUrl,
  todayIsoDate
} from "./operations.js";

export function escapeAttr(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

export function closeModal() {
  const root = document.getElementById("modal-root");
  if (!root) return;
  root.classList.add("hidden");
  root.innerHTML = "";
}

export function openModal(title, bodyHtml, onMount) {
  const root = document.getElementById("modal-root");
  if (!root) return;
  root.innerHTML = `
    <div class="modal" role="dialog" aria-modal="true" aria-label="${escapeAttr(title)}">
      <div class="modal-head">
        <h3>${escapeAttr(title)}</h3>
        <button id="modal-close-btn" type="button" class="btn btn-sm">Close</button>
      </div>
      ${bodyHtml}
    </div>
  `;
  root.classList.remove("hidden");
  document.getElementById("modal-close-btn").onclick = closeModal;
  root.onclick = (e) => {
    if (e.target === root) closeModal();
  };
  if (onMount) onMount(root);
}

export function openConfirmModal({ title, message, confirmLabel = "Confirm", danger = false, onConfirm }) {
  openModal(
    title,
    `
      <div style="font-size:13px; color:var(--ink); line-height:1.5;">${escapeAttr(message)}</div>
      <div style="display:flex; justify-content:flex-end; gap:8px;">
        <button id="confirm-cancel-btn" type="button" class="btn btn-sm">Cancel</button>
        <button id="confirm-ok-btn" type="button" class="btn ${danger ? "btn-danger" : "btn-primary"} btn-sm">${escapeAttr(confirmLabel)}</button>
      </div>
    `,
    (root) => {
      root.querySelector("#confirm-cancel-btn").onclick = closeModal;
      root.querySelector("#confirm-ok-btn").onclick = async () => {
        closeModal();
        await onConfirm();
      };
    }
  );
}

function skuSelectOptions(skus, selectedProduct) {
  const names = [...new Set([...skus.map((s) => s.productName), selectedProduct].filter(Boolean))];
  if (!names.length) return `<option value="">No SKUs available</option>`;
  return names
    .map((name) => {
      const sku = skus.find((s) => s.productName === name);
      const wt = sku ? ` (${sku.standardBagWeightKg}kg)` : "";
      return `<option value="${escapeAttr(name)}" ${name === selectedProduct ? "selected" : ""}>${escapeAttr(name)}${wt}</option>`;
    })
    .join("");
}

function customerDatalist(state, listId = "customer-datalist") {
  const names = new Set([
    ...state.customers.map((c) => c.name).filter(Boolean),
    ...state.orders.map((o) => o.customer).filter(Boolean)
  ]);
  return `<datalist id="${listId}">${[...names].sort().map((n) => `<option value="${escapeAttr(n)}"></option>`).join("")}</datalist>`;
}

export function openSkuModal(state, existingSku, actions) {
  const isEdit = Boolean(existingSku);
  const s = existingSku || {
    productName: "",
    section: PLANT_SECTIONS[0],
    openingStockKg: 0,
    standardBagWeightKg: 25,
    targetRecoveryPct: 99.5,
    safetyBufferPct: 15,
    defaultRemarks: "MIXING/CONVEYOR"
  };

  openModal(
    isEdit ? `Edit SKU • ${s.productName}` : "Add Finished Goods SKU",
    `
      <form id="sku-form" class="form-grid">
        <div class="form-field">
          <label>Product Name</label>
          <input name="productName" value="${escapeAttr(s.productName)}" placeholder="e.g., Hog Starter Crumble" required />
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Plant Section</label>
            <select name="section">
              ${PLANT_SECTIONS.map((sec) => `<option value="${escapeAttr(sec)}" ${sec === s.section ? "selected" : ""}>${escapeAttr(sec)}</option>`).join("")}
            </select>
          </div>
          <div class="form-field">
            <label>Bag Weight (kg)</label>
            <input name="standardBagWeightKg" type="number" step="0.1" min="0.1" value="${escapeAttr(s.standardBagWeightKg)}" required />
          </div>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Opening Stock (kg)</label>
            <input name="openingStockKg" type="number" step="0.1" min="0" value="${escapeAttr(s.openingStockKg)}" required />
          </div>
          <div class="form-field">
            <label>Safety Buffer (%)</label>
            <input name="safetyBufferPct" type="number" step="1" min="0" max="200" value="${escapeAttr(s.safetyBufferPct)}" required />
          </div>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Target Recovery (%)</label>
            <input name="targetRecoveryPct" type="number" step="0.1" min="1" max="120" value="${escapeAttr(s.targetRecoveryPct)}" required />
          </div>
          <div class="form-field">
            <label>Default Line Remarks</label>
            <input name="defaultRemarks" value="${escapeAttr(s.defaultRemarks)}" list="line-remarks-dl" />
            <datalist id="line-remarks-dl">${LINE_REMARKS_OPTIONS.map((r) => `<option value="${escapeAttr(r)}"></option>`).join("")}</datalist>
          </div>
        </div>
        <button type="submit" class="btn btn-primary">${isEdit ? "Save SKU Changes" : "Create SKU"}</button>
      </form>
    `,
    (root) => {
      const form = root.querySelector("#sku-form");
      form.onsubmit = async (e) => {
        e.preventDefault();
        await actions.saveSku(existingSku, {
          productName: form.productName.value,
          section: form.section.value,
          standardBagWeightKg: form.standardBagWeightKg.value,
          openingStockKg: form.openingStockKg.value,
          safetyBufferPct: form.safetyBufferPct.value,
          targetRecoveryPct: form.targetRecoveryPct.value,
          defaultRemarks: form.defaultRemarks.value
        });
      };
    }
  );
}

export function openOrderModal(state, existingOrder, actions) {
  const isEdit = Boolean(existingOrder);
  const o = existingOrder || {
    orderCode: `ORD-${Date.now().toString().slice(-5)}`,
    customer: "",
    productName: state.skus[0]?.productName || "",
    orderedBags: 40,
    bagWeightKg: state.skus[0]?.standardBagWeightKg || 25,
    orderDate: todayIsoDate(),
    dueDate: todayIsoDate(),
    priority: false,
    rushOrder: false
  };

  openModal(
    isEdit ? `Edit Order • ${o.orderCode}` : "New Customer Order",
    `
      <form id="order-form" class="form-grid">
        <div class="form-row-2">
          <div class="form-field">
            <label>Order Code</label>
            <input name="orderCode" value="${escapeAttr(o.orderCode)}" ${isEdit ? "disabled" : "required"} />
          </div>
          <div class="form-field">
            <label>Customer</label>
            <input name="customer" value="${escapeAttr(o.customer)}" list="order-customer-dl" placeholder="e.g., Bulacan Farm Supply" required />
            ${customerDatalist(state, "order-customer-dl")}
          </div>
        </div>
        <div class="form-field">
          <label>Feed Product (SKU)</label>
          <select name="productName" ${isEdit ? "disabled" : "required"}>${skuSelectOptions(state.skus, o.productName)}</select>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Ordered Bags</label>
            <input name="orderedBags" type="number" min="1" value="${escapeAttr(o.orderedBags)}" required />
          </div>
          <div class="form-field">
            <label>Bag Weight (kg)</label>
            <input name="bagWeightKg" type="number" step="0.1" min="0.1" value="${escapeAttr(o.bagWeightKg)}" required />
          </div>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Order Date</label>
            <input name="orderDate" type="date" value="${escapeAttr(o.orderDate)}" ${isEdit ? "disabled" : "required"} />
          </div>
          <div class="form-field">
            <label>Due Date</label>
            <input name="dueDate" type="date" value="${escapeAttr(o.dueDate)}" required />
          </div>
        </div>
        <div style="display:flex; gap:14px; font-size:13px;">
          <label><input type="checkbox" name="priority" ${o.priority ? "checked" : ""} /> Priority</label>
          <label><input type="checkbox" name="rushOrder" ${o.rushOrder ? "checked" : ""} /> Rush Order</label>
        </div>
        <button type="submit" class="btn btn-primary">${isEdit ? "Save Order Changes" : "Save Order"}</button>
      </form>
    `,
    (root) => {
      const form = root.querySelector("#order-form");
      if (!isEdit) {
        form.productName.onchange = () => {
          const sku = state.skus.find((s) => s.productName === form.productName.value);
          if (sku) form.bagWeightKg.value = sku.standardBagWeightKg;
        };
      }
      form.onsubmit = async (e) => {
        e.preventDefault();
        await actions.saveOrder(existingOrder, {
          orderCode: form.orderCode.value,
          customer: form.customer.value,
          productName: form.productName.value,
          orderedBags: form.orderedBags.value,
          bagWeightKg: form.bagWeightKg.value,
          orderDate: form.orderDate.value,
          dueDate: form.dueDate.value,
          priority: form.priority.checked,
          rushOrder: form.rushOrder.checked
        });
      };
    }
  );
}

export function openDispatchModal(state, { dispatch = null, prefillOrder = null } = {}, actions) {
  const isEdit = Boolean(dispatch);
  const openOrders = state.orders.filter(
    (o) =>
      o.status === "Open" ||
      o.status === "Partially Dispatched" ||
      (dispatch && dispatch.linkedOrderId === o.id)
  );
  const linkedOrder = prefillOrder || (dispatch?.linkedOrderId ? state.orders.find((o) => o.id === dispatch.linkedOrderId) : null);
  const defaultProduct = dispatch?.productName || linkedOrder?.productName || state.skus[0]?.productName || "";
  const defaultBagWt = dispatch?.bagWeightKg || linkedOrder?.bagWeightKg || state.skus.find((s) => s.productName === defaultProduct)?.standardBagWeightKg || 25;
  const defaultBags = dispatch
    ? dispatch.bagsDispatched
    : linkedOrder
    ? Math.max(1, Math.round((Number(linkedOrder.totalWeightKg || 0) - Number(linkedOrder.dispatchedWeightKg || 0)) / defaultBagWt))
    : 20;

  const d = dispatch || {
    date: todayIsoDate(),
    drReference: `DR-${Date.now().toString().slice(-5)}`,
    productName: defaultProduct,
    customer: linkedOrder?.customer || "",
    bagsDispatched: defaultBags,
    bagWeightKg: defaultBagWt,
    linkedOrderId: linkedOrder?.id || "",
    sourcePlant: state.skus.find((s) => s.productName === defaultProduct)?.section || "",
    driverName: "",
    truckNumber: "",
    helperName: "",
    remarks: "",
    isHeld: false,
    heldRemarks: "",
    returnConfirmed: false,
    returnedBags: 1,
    returnedWeightKg: defaultBagWt,
    returnReason: RETURN_REASONS[0]
  };

  openModal(
    isEdit ? `Edit Dispatch • ${d.drReference}` : "Log Delivery Dispatch",
    `
      <form id="dispatch-form" class="form-grid">
        <div class="form-field">
          <label>Link to Customer Order</label>
          <select name="linkedOrderId">
            <option value="">Unlinked / Direct Dispatch</option>
            ${openOrders
              .map(
                (o) =>
                  `<option value="${escapeAttr(o.id)}" ${o.id === d.linkedOrderId ? "selected" : ""}>${escapeAttr(o.orderCode)} • ${escapeAttr(o.customer)} • ${escapeAttr(o.productName)} (${Math.max(0, Math.round(o.totalWeightKg - o.dispatchedWeightKg))} kg left)</option>`
              )
              .join("")}
          </select>
        </div>
        ${
          !isEdit
            ? `<label style="font-size:12px;"><input type="checkbox" name="createRushOrder" ${!d.linkedOrderId ? "" : ""} /> Create linked Fulfilled Rush Order automatically (when unlinked)</label>`
            : ""
        }
        <div class="form-row-2">
          <div class="form-field">
            <label>Dispatch Date</label>
            <input name="date" type="date" value="${escapeAttr(d.date)}" required />
          </div>
          <div class="form-field">
            <label>DR Reference</label>
            <input name="drReference" value="${escapeAttr(d.drReference)}" required />
          </div>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Product</label>
            <select name="productName" required>${skuSelectOptions(state.skus, d.productName)}</select>
          </div>
          <div class="form-field">
            <label>Customer</label>
            <input name="customer" value="${escapeAttr(d.customer)}" list="disp-cust-dl" placeholder="Customer name" required />
            ${customerDatalist(state, "disp-cust-dl")}
          </div>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Bags Dispatched</label>
            <input name="bagsDispatched" type="number" min="1" value="${escapeAttr(d.bagsDispatched)}" required />
          </div>
          <div class="form-field">
            <label>Bag Weight (kg)</label>
            <input name="bagWeightKg" type="number" step="0.1" min="0.1" value="${escapeAttr(d.bagWeightKg)}" required />
          </div>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Source Plant</label>
            <select name="sourcePlant">
              <option value="">Unspecified</option>
              ${PLANT_SECTIONS.map((sec) => `<option value="${escapeAttr(sec)}" ${sec === d.sourcePlant ? "selected" : ""}>${escapeAttr(sec)}</option>`).join("")}
            </select>
          </div>
          <div class="form-field">
            <label>Driver Name</label>
            <input name="driverName" value="${escapeAttr(d.driverName || "")}" placeholder="Driver" />
          </div>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Truck Plate / No.</label>
            <input name="truckNumber" value="${escapeAttr(d.truckNumber || "")}" placeholder="e.g., ABC-1234" />
          </div>
          <div class="form-field">
            <label>Helper Name</label>
            <input name="helperName" value="${escapeAttr(d.helperName || "")}" placeholder="Optional helper" />
          </div>
        </div>
        <div class="form-field">
          <label>Dispatch Remarks</label>
          <input name="remarks" value="${escapeAttr(d.remarks || "")}" placeholder="Optional delivery notes" />
        </div>

        <div class="sim-bar" style="display:grid; gap:8px;">
          <label style="font-weight:700;"><input type="checkbox" name="isHeld" ${d.isHeld ? "checked" : ""} /> Hold Shipment at Gate / QC</label>
          <div id="held-box" class="${d.isHeld ? "" : "hidden"} form-field">
            <label>Hold Remarks (required if held)</label>
            <input name="heldRemarks" value="${escapeAttr(d.heldRemarks || "")}" placeholder="Reason shipment is held" />
          </div>
          <label style="font-weight:700;"><input type="checkbox" name="returnConfirmed" ${d.returnConfirmed ? "checked" : ""} /> Record Customer Return</label>
          <div id="return-box" class="${d.returnConfirmed ? "form-grid" : "hidden"}">
            <div class="form-row-2">
              <div class="form-field">
                <label>Returned Bags</label>
                <input name="returnedBags" type="number" min="1" value="${escapeAttr(d.returnedBags || 1)}" />
              </div>
              <div class="form-field">
                <label>Returned Weight (kg)</label>
                <input name="returnedWeightKg" type="number" step="0.1" min="0.1" value="${escapeAttr(d.returnedWeightKg || d.bagWeightKg)}" />
              </div>
            </div>
            <div class="form-field">
              <label>Return Reason</label>
              <select name="returnReason">
                ${RETURN_REASONS.map((r) => `<option value="${escapeAttr(r)}" ${r === d.returnReason ? "selected" : ""}>${escapeAttr(r)}</option>`).join("")}
              </select>
            </div>
            ${
              !isEdit || !d.returnConfirmed
                ? `<label style="font-size:12px;"><input type="checkbox" name="autoQueueReprocess" checked /> Automatically queue return into Reprocess Quarantine</label>`
                : ""
            }
          </div>
        </div>

        <button type="submit" class="btn btn-primary">${isEdit ? "Save Dispatch Changes" : "Record Dispatch"}</button>
      </form>
    `,
    (root) => {
      const form = root.querySelector("#dispatch-form");
      const heldBox = root.querySelector("#held-box");
      const returnBox = root.querySelector("#return-box");
      form.isHeld.onchange = () => heldBox.classList.toggle("hidden", !form.isHeld.checked);
      form.returnConfirmed.onchange = () => returnBox.classList.toggle("hidden", !form.returnConfirmed.checked);
      form.returnedBags.oninput = () => {
        form.returnedWeightKg.value = Number(form.returnedBags.value || 1) * Number(form.bagWeightKg.value || 25);
      };
      form.linkedOrderId.onchange = () => {
        const ord = openOrders.find((o) => o.id === form.linkedOrderId.value);
        if (ord) {
          form.productName.value = ord.productName;
          form.customer.value = ord.customer;
          form.bagWeightKg.value = ord.bagWeightKg;
          const remBags = Math.max(1, Math.round((ord.totalWeightKg - ord.dispatchedWeightKg) / (ord.bagWeightKg || 25)));
          form.bagsDispatched.value = remBags;
          const sku = state.skus.find((s) => s.productName === ord.productName);
          if (sku) form.sourcePlant.value = sku.section;
        }
      };
      form.productName.onchange = () => {
        const sku = state.skus.find((s) => s.productName === form.productName.value);
        if (sku) {
          form.bagWeightKg.value = sku.standardBagWeightKg;
          if (!form.sourcePlant.value) form.sourcePlant.value = sku.section;
        }
      };
      form.onsubmit = async (e) => {
        e.preventDefault();
        await actions.saveDispatch(dispatch, {
          date: form.date.value,
          drReference: form.drReference.value,
          productName: form.productName.value,
          customer: form.customer.value,
          bagsDispatched: form.bagsDispatched.value,
          bagWeightKg: form.bagWeightKg.value,
          linkedOrderId: form.linkedOrderId.value,
          createRushOrder: form.createRushOrder ? form.createRushOrder.checked : false,
          sourcePlant: form.sourcePlant.value,
          driverName: form.driverName.value,
          truckNumber: form.truckNumber.value,
          helperName: form.helperName.value,
          remarks: form.remarks.value,
          isHeld: form.isHeld.checked,
          heldRemarks: form.heldRemarks.value,
          returnConfirmed: form.returnConfirmed.checked,
          returnedBags: form.returnedBags.value,
          returnedWeightKg: form.returnedWeightKg.value,
          returnReason: form.returnReason.value,
          autoQueueReprocess: form.autoQueueReprocess ? form.autoQueueReprocess.checked : false
        });
      };
    }
  );
}

export function openScheduleModal(state, { schedule = null, prefillSku = null, prefillOrder = null } = {}, actions) {
  const isEdit = Boolean(schedule);
  const openOrders = state.orders.filter((o) => o.status === "Open" || o.status === "Partially Dispatched");
  const skuObj = prefillSku || (prefillOrder ? state.skus.find((s) => s.productName === prefillOrder.productName) : state.skus[0]);
  const s = schedule || {
    scheduledDate: todayIsoDate(),
    section: skuObj?.section || PLANT_SECTIONS[0],
    productName: prefillOrder?.productName || skuObj?.productName || "",
    targetBags: prefillOrder ? Math.max(1, Math.round((prefillOrder.totalWeightKg - prefillOrder.dispatchedWeightKg) / (prefillOrder.bagWeightKg || 25))) : 80,
    bagWeightKg: prefillOrder?.bagWeightKg || skuObj?.standardBagWeightKg || 25,
    lineRemarks: skuObj?.defaultRemarks || LINE_REMARKS_OPTIONS[0],
    priority: prefillOrder?.rushOrder ? "Urgent" : prefillOrder ? "Order-Driven" : "Order-Driven",
    status: "Scheduled",
    linkedOrderId: prefillOrder?.id || "",
    linkedOrderCode: prefillOrder?.orderCode || ""
  };

  openModal(
    isEdit ? `Edit Schedule • ${s.productName}` : "Schedule Milling Run",
    `
      <form id="schedule-form" class="form-grid">
        <div class="form-row-2">
          <div class="form-field">
            <label>Scheduled Date</label>
            <input name="scheduledDate" type="date" value="${escapeAttr(s.scheduledDate)}" required />
          </div>
          <div class="form-field">
            <label>Plant Section</label>
            <select name="section" ${isEdit ? "disabled" : ""}>
              ${PLANT_SECTIONS.map((sec) => `<option value="${escapeAttr(sec)}" ${sec === s.section ? "selected" : ""}>${escapeAttr(sec)}</option>`).join("")}
            </select>
          </div>
        </div>
        <div class="form-field">
          <label>Feed Product</label>
          <select name="productName" ${isEdit ? "disabled" : "required"}>${skuSelectOptions(state.skus, s.productName)}</select>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Target Bags</label>
            <input name="targetBags" type="number" min="1" value="${escapeAttr(s.targetBags)}" required />
          </div>
          <div class="form-field">
            <label>Bag Weight (kg)</label>
            <input name="bagWeightKg" type="number" step="0.1" min="0.1" value="${escapeAttr(s.bagWeightKg)}" required />
          </div>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Priority</label>
            <select name="priority">
              ${SCHEDULE_PRIORITIES.map((p) => `<option value="${escapeAttr(p)}" ${p === s.priority ? "selected" : ""}>${escapeAttr(p)}</option>`).join("")}
            </select>
          </div>
          <div class="form-field">
            <label>Line Remarks</label>
            <input name="lineRemarks" value="${escapeAttr(s.lineRemarks)}" list="sched-remarks-dl" required />
            <datalist id="sched-remarks-dl">${LINE_REMARKS_OPTIONS.map((r) => `<option value="${escapeAttr(r)}"></option>`).join("")}</datalist>
          </div>
        </div>
        <div class="form-field">
          <label>Link Open Order (optional)</label>
          <select name="linkedOrderId">
            <option value="">None / Stock Build</option>
            ${openOrders
              .map(
                (o) => `<option value="${escapeAttr(o.id)}" ${o.id === s.linkedOrderId ? "selected" : ""}>${escapeAttr(o.orderCode)} • ${escapeAttr(o.customer)} (${escapeAttr(o.productName)})</option>`
              )
              .join("")}
          </select>
        </div>
        <button type="submit" class="btn btn-primary">${isEdit ? "Save Schedule Changes" : "Queue Milling Run"}</button>
      </form>
    `,
    (root) => {
      const form = root.querySelector("#schedule-form");
      if (!isEdit) {
        form.productName.onchange = () => {
          const sku = state.skus.find((x) => x.productName === form.productName.value);
          if (sku) {
            form.section.value = sku.section;
            form.bagWeightKg.value = sku.standardBagWeightKg;
            if (sku.defaultRemarks) form.lineRemarks.value = sku.defaultRemarks;
          }
        };
      }
      form.onsubmit = async (e) => {
        e.preventDefault();
        const linked = openOrders.find((o) => o.id === form.linkedOrderId.value);
        await actions.saveSchedule(schedule, {
          scheduledDate: form.scheduledDate.value,
          section: form.section.value,
          productName: form.productName.value,
          targetBags: form.targetBags.value,
          bagWeightKg: form.bagWeightKg.value,
          priority: form.priority.value,
          lineRemarks: form.lineRemarks.value,
          status: schedule?.status || "Scheduled",
          linkedOrderId: linked?.id || "",
          linkedOrderCode: linked?.orderCode || "",
          linkedOrderIds: linked ? [linked.id] : [],
          linkedOrderCodes: linked ? [linked.orderCode] : []
        });
      };
    }
  );
}

export function openCompleteScheduleModal(schedule, actions) {
  openModal(
    `Complete Run & Log Batch • ${schedule.productName}`,
    `
      <form id="complete-sched-form" class="form-grid">
        <div style="font-size:13px; color:var(--muted);">
          Marks this scheduled run as <strong>Completed</strong> and posts the recovered bags directly to Production Batches &amp; Finished Stock.
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Completion Date</label>
            <input name="date" type="date" value="${escapeAttr(todayIsoDate())}" required />
          </div>
          <div class="form-field">
            <label>Bags Produced</label>
            <input name="bagsProduced" type="number" min="1" value="${escapeAttr(schedule.targetBags)}" required />
          </div>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Actual Recovered (kg)</label>
            <input name="actualRecoveredKg" type="number" step="0.1" min="0.1" value="${escapeAttr(schedule.expectedWeightKg)}" required />
          </div>
          <div class="form-field">
            <label>Remarks</label>
            <input name="remarks" value="${escapeAttr(schedule.lineRemarks)}" />
          </div>
        </div>
        <button type="submit" class="btn btn-primary">Complete &amp; Post Batch</button>
      </form>
    `,
    (root) => {
      const form = root.querySelector("#complete-sched-form");
      form.bagsProduced.oninput = () => {
        form.actualRecoveredKg.value = Number(form.bagsProduced.value || 1) * Number(schedule.bagWeightKg || 25);
      };
      form.onsubmit = async (e) => {
        e.preventDefault();
        await actions.completeScheduleRun(schedule, {
          date: form.date.value,
          bagsProduced: form.bagsProduced.value,
          actualRecoveredKg: form.actualRecoveredKg.value,
          remarks: form.remarks.value
        });
      };
    }
  );
}

export function openBatchModal(state, existingBatch, actions) {
  const isEdit = Boolean(existingBatch);
  const b = existingBatch || {
    date: todayIsoDate(),
    section: state.skus[0]?.section || PLANT_SECTIONS[0],
    productName: state.skus[0]?.productName || "",
    bagsProduced: 60,
    bagWeightKg: state.skus[0]?.standardBagWeightKg || 25,
    actualRecoveredKg: 60 * (state.skus[0]?.standardBagWeightKg || 25),
    remarks: state.skus[0]?.defaultRemarks || LINE_REMARKS_OPTIONS[0]
  };

  openModal(
    isEdit ? `Edit Batch • ${b.productName}` : "Log Production Batch",
    `
      <form id="batch-form" class="form-grid">
        <div class="form-row-2">
          <div class="form-field">
            <label>Date</label>
            <input name="date" type="date" value="${escapeAttr(b.date)}" required />
          </div>
          <div class="form-field">
            <label>Plant Section</label>
            <select name="section">
              ${PLANT_SECTIONS.map((sec) => `<option value="${escapeAttr(sec)}" ${sec === b.section ? "selected" : ""}>${escapeAttr(sec)}</option>`).join("")}
            </select>
          </div>
        </div>
        <div class="form-field">
          <label>Feed Product</label>
          <select name="productName" required>${skuSelectOptions(state.skus, b.productName)}</select>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Bags Produced</label>
            <input name="bagsProduced" type="number" min="1" value="${escapeAttr(b.bagsProduced)}" required />
          </div>
          <div class="form-field">
            <label>Bag Weight (kg)</label>
            <input name="bagWeightKg" type="number" step="0.1" min="0.1" value="${escapeAttr(b.bagWeightKg)}" required />
          </div>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Actual Recovered (kg)</label>
            <input name="actualRecoveredKg" type="number" step="0.1" min="0.1" value="${escapeAttr(b.actualRecoveredKg)}" required />
          </div>
          <div class="form-field">
            <label>Line Remarks</label>
            <input name="remarks" value="${escapeAttr(b.remarks || "")}" list="batch-rem-dl" />
            <datalist id="batch-rem-dl">${LINE_REMARKS_OPTIONS.map((r) => `<option value="${escapeAttr(r)}"></option>`).join("")}</datalist>
          </div>
        </div>
        <button type="submit" class="btn btn-primary">${isEdit ? "Save Batch Changes" : "Save Batch"}</button>
      </form>
    `,
    (root) => {
      const form = root.querySelector("#batch-form");
      const recalc = () => {
        form.actualRecoveredKg.value = Number(form.bagsProduced.value || 0) * Number(form.bagWeightKg.value || 25);
      };
      form.productName.onchange = () => {
        const sku = state.skus.find((s) => s.productName === form.productName.value);
        if (sku) {
          form.section.value = sku.section;
          form.bagWeightKg.value = sku.standardBagWeightKg;
          if (!isEdit) form.remarks.value = sku.defaultRemarks || LINE_REMARKS_OPTIONS[0];
          recalc();
        }
      };
      if (!isEdit) {
        form.bagsProduced.oninput = recalc;
        form.bagWeightKg.oninput = recalc;
      }
      form.onsubmit = async (e) => {
        e.preventDefault();
        await actions.saveBatch(existingBatch, {
          date: form.date.value,
          section: form.section.value,
          productName: form.productName.value,
          bagsProduced: form.bagsProduced.value,
          bagWeightKg: form.bagWeightKg.value,
          actualRecoveredKg: form.actualRecoveredKg.value,
          remarks: form.remarks.value,
          scheduleRunId: existingBatch?.scheduleRunId || "",
          linkedOrderIds: existingBatch?.linkedOrderIds || [],
          linkedOrderCodes: existingBatch?.linkedOrderCodes || [],
          scheduledForCustomers: existingBatch?.scheduledForCustomers || []
        });
      };
    }
  );
}

export function openBulkBatchModal(actions) {
  openModal(
    "Bulk Paste Import Batches",
    `
      <form id="bulk-batch-form" class="form-grid">
        <div style="font-size:12px; color:var(--muted); line-height:1.45;">
          Paste rows from Excel/Sheets (Tab or Comma separated). Format:<br/>
          <code>Date (YYYY-MM-DD), Section, Product Name, Bags, BagKg, ActualKg, Remarks</code>
        </div>
        <div class="form-field">
          <label>Default Section (when omitted)</label>
          <select name="defaultSection">
            ${PLANT_SECTIONS.map((sec) => `<option value="${escapeAttr(sec)}">${escapeAttr(sec)}</option>`).join("")}
          </select>
        </div>
        <div class="form-field">
          <label>Batch Rows</label>
          <textarea name="rawText" rows="7" placeholder="${todayIsoDate()}, SAN ROQUE, Hog Starter Crumble, 60, 25, 1495, MIXING/CONVEYOR" required style="font-family:monospace; font-size:12px; padding:10px; border:1px solid var(--border); border-radius:10px;"></textarea>
        </div>
        <button type="submit" class="btn btn-primary">Import Validated Batches</button>
      </form>
    `,
    (root) => {
      const form = root.querySelector("#bulk-batch-form");
      form.onsubmit = async (e) => {
        e.preventDefault();
        await actions.bulkImportBatches(form.rawText.value, form.defaultSection.value);
      };
    }
  );
}

export function openRawMaterialModal(existingMat, actions) {
  const isEdit = Boolean(existingMat);
  const m = existingMat || {
    name: "",
    category: RAW_CATEGORIES[0],
    currentStockKg: 1000,
    reorderThresholdKg: 300,
    usageSharePct: 10,
    leadTimeDays: 3,
    safetyDays: 2,
    supplierName: "Macro Grain Supply",
    alertsEnabled: true,
    lastRestockedDate: todayIsoDate()
  };

  openModal(
    isEdit ? `Edit Raw Material • ${m.name}` : "Add Raw Material",
    `
      <form id="raw-form" class="form-grid">
        <div class="form-row-2">
          <div class="form-field">
            <label>Material Name</label>
            <input name="name" value="${escapeAttr(m.name)}" placeholder="e.g., Whole Yellow Corn" required />
          </div>
          <div class="form-field">
            <label>Category</label>
            <select name="category">
              ${RAW_CATEGORIES.map((c) => `<option value="${escapeAttr(c)}" ${c === m.category ? "selected" : ""}>${escapeAttr(c)}</option>`).join("")}
            </select>
          </div>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Current Stock (kg)</label>
            <input name="currentStockKg" type="number" step="0.1" min="0" value="${escapeAttr(m.currentStockKg)}" required />
          </div>
          <div class="form-field">
            <label>Reorder Threshold (kg)</label>
            <input name="reorderThresholdKg" type="number" step="0.1" min="0" value="${escapeAttr(m.reorderThresholdKg)}" required />
          </div>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Fallback Usage Share (%)</label>
            <input name="usageSharePct" type="number" step="0.1" min="0.1" max="100" value="${escapeAttr(m.usageSharePct)}" required />
          </div>
          <div class="form-field">
            <label>Lead Time (days)</label>
            <input name="leadTimeDays" type="number" min="1" max="60" value="${escapeAttr(m.leadTimeDays)}" required />
          </div>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Safety Days</label>
            <input name="safetyDays" type="number" min="0" max="30" value="${escapeAttr(m.safetyDays)}" required />
          </div>
          <div class="form-field">
            <label>Sector / Supplier Tag</label>
            <input name="supplierName" value="${escapeAttr(m.supplierName || "")}" placeholder="e.g., Micro • Fast Moving" />
          </div>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Last Restocked Date</label>
            <input name="lastRestockedDate" type="date" value="${escapeAttr(m.lastRestockedDate || todayIsoDate())}" />
          </div>
          <div style="display:flex; align-items:end; padding-bottom:6px; font-size:13px;">
            <label><input type="checkbox" name="alertsEnabled" ${m.alertsEnabled !== false ? "checked" : ""} /> Alerts Enabled</label>
          </div>
        </div>
        <button type="submit" class="btn btn-primary">${isEdit ? "Save Material Changes" : "Add Raw Material"}</button>
      </form>
    `,
    (root) => {
      const form = root.querySelector("#raw-form");
      form.onsubmit = async (e) => {
        e.preventDefault();
        await actions.saveRawMaterial(existingMat, {
          name: form.name.value,
          category: form.category.value,
          currentStockKg: form.currentStockKg.value,
          reorderThresholdKg: form.reorderThresholdKg.value,
          usageSharePct: form.usageSharePct.value,
          leadTimeDays: form.leadTimeDays.value,
          safetyDays: form.safetyDays.value,
          supplierName: form.supplierName.value,
          alertsEnabled: form.alertsEnabled.checked,
          lastRestockedDate: form.lastRestockedDate.value
        });
      };
    }
  );
}

export function openAdjustRawModal(mat, actions) {
  openModal(
    `Adjust Stock • ${mat.name}`,
    `
      <form id="adjust-raw-form" class="form-grid">
        <div style="font-size:13px; color:var(--muted);">Current on-hand stock: <strong>${Number(mat.currentStockKg || 0).toLocaleString()} kg</strong></div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Adjustment Mode</label>
            <select name="mode">
              <option value="add">+ Restock Received (kg)</option>
              <option value="sub">- Manual Consume / Issue (kg)</option>
            </select>
          </div>
          <div class="form-field">
            <label>Amount (kg)</label>
            <input name="amountKg" type="number" step="0.1" min="0.1" value="100" required />
          </div>
        </div>
        <button type="submit" class="btn btn-primary">Apply Stock Update</button>
      </form>
    `,
    (root) => {
      const form = root.querySelector("#adjust-raw-form");
      form.onsubmit = async (e) => {
        e.preventDefault();
        const amt = Number(form.amountKg.value || 0);
        const delta = form.mode.value === "sub" ? -amt : amt;
        await actions.adjustRawMaterialStock(mat, delta);
      };
    }
  );
}

export function openBomModal(state, existingBom, actions) {
  const isEdit = Boolean(existingBom);
  const b = existingBom || {
    productName: state.skus[0]?.productName || "",
    section: state.skus[0]?.section || PLANT_SECTIONS[0],
    batchBaseKg: 1000,
    yieldLossPct: 1.5,
    notes: "",
    lines: state.rawMaterials.slice(0, 2).map((m) => ({
      materialId: m.id,
      materialName: m.name,
      category: m.category,
      inclusionPct: 25
    }))
  };
  let draftLines = (b.lines && b.lines.length ? b.lines : [{ materialName: state.rawMaterials[0]?.name || "", inclusionPct: 50 }]).map((l) => ({ ...l }));

  const matOptions = (selectedName) =>
    state.rawMaterials
      .map((m) => `<option value="${escapeAttr(m.name)}" ${m.name === selectedName ? "selected" : ""}>${escapeAttr(m.name)}</option>`)
      .join("");

  openModal(
    isEdit ? `Edit BOM Formula • ${b.productName}` : "Create Feed BOM Formula",
    `
      <form id="bom-form" class="form-grid">
        <div class="form-row-2">
          <div class="form-field">
            <label>Feed Product (SKU)</label>
            <select name="productName" required>${skuSelectOptions(state.skus, b.productName)}</select>
          </div>
          <div class="form-field">
            <label>Plant Section</label>
            <select name="section">
              ${PLANT_SECTIONS.map((sec) => `<option value="${escapeAttr(sec)}" ${sec === b.section ? "selected" : ""}>${escapeAttr(sec)}</option>`).join("")}
            </select>
          </div>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Batch Base Size (kg)</label>
            <input name="batchBaseKg" type="number" step="10" min="1" value="${escapeAttr(b.batchBaseKg)}" required />
          </div>
          <div class="form-field">
            <label>Yield Loss (%)</label>
            <input name="yieldLossPct" type="number" step="0.1" min="0" max="50" value="${escapeAttr(b.yieldLossPct)}" required />
          </div>
        </div>
        <div class="form-field">
          <div style="display:flex; justify-content:space-between; align-items:center;">
            <label>Ingredient Lines (<span id="bom-total-pct">0</span>%)</label>
            <button id="bom-add-line-btn" type="button" class="btn btn-sm">+ Add Ingredient</button>
          </div>
          <div id="bom-lines-box" style="display:grid; gap:8px; margin-top:6px;"></div>
        </div>
        <div class="form-field">
          <label>Formula Notes</label>
          <input name="notes" value="${escapeAttr(b.notes || "")}" placeholder="Optional formulation notes" />
        </div>
        <button type="submit" class="btn btn-primary">${isEdit ? "Save BOM Formula" : "Save BOM Formula"}</button>
      </form>
    `,
    (root) => {
      const form = root.querySelector("#bom-form");
      const linesBox = root.querySelector("#bom-lines-box");
      const totalLabel = root.querySelector("#bom-total-pct");

      const updateTotal = () => {
        const sum = draftLines.reduce((s, l) => s + Number(l.inclusionPct || 0), 0);
        totalLabel.textContent = sum.toFixed(2);
      };

      const renderLines = () => {
        linesBox.innerHTML = draftLines
          .map(
            (l, idx) => `
              <div class="bom-line-row">
                <select data-bom-mat="${idx}">${matOptions(l.materialName)}</select>
                <input data-bom-pct="${idx}" type="number" step="0.01" min="0" max="100" value="${escapeAttr(l.inclusionPct)}" />
                <button type="button" class="btn btn-danger btn-sm" data-bom-del="${idx}" ${draftLines.length <= 1 ? "disabled" : ""}>×</button>
              </div>
            `
          )
          .join("");
        linesBox.querySelectorAll("[data-bom-mat]").forEach((sel) => {
          sel.onchange = () => {
            const i = Number(sel.dataset.bomMat);
            const mat = state.rawMaterials.find((m) => m.name === sel.value);
            draftLines[i].materialName = sel.value;
            draftLines[i].materialId = mat?.id || "";
            draftLines[i].category = mat?.category || RAW_CATEGORIES[0];
          };
        });
        linesBox.querySelectorAll("[data-bom-pct]").forEach((inp) => {
          inp.oninput = () => {
            draftLines[Number(inp.dataset.bomPct)].inclusionPct = Number(inp.value || 0);
            updateTotal();
          };
        });
        linesBox.querySelectorAll("[data-bom-del]").forEach((btn) => {
          btn.onclick = () => {
            if (draftLines.length > 1) {
              draftLines.splice(Number(btn.dataset.bomDel), 1);
              renderLines();
            }
          };
        });
        updateTotal();
      };

      renderLines();

      root.querySelector("#bom-add-line-btn").onclick = () => {
        const firstMat = state.rawMaterials[0];
        draftLines.push({
          materialId: firstMat?.id || "",
          materialName: firstMat?.name || "",
          category: firstMat?.category || RAW_CATEGORIES[0],
          inclusionPct: 1
        });
        renderLines();
      };

      form.productName.onchange = () => {
        const sku = state.skus.find((x) => x.productName === form.productName.value);
        if (sku) form.section.value = sku.section;
      };

      form.onsubmit = async (e) => {
        e.preventDefault();
        const syncedLines = draftLines.map((l) => {
          const mat = state.rawMaterials.find((m) => m.name === l.materialName);
          return {
            materialId: mat?.id || l.materialId || "",
            materialName: l.materialName || mat?.name || "",
            category: mat?.category || l.category || RAW_CATEGORIES[0],
            inclusionPct: Number(l.inclusionPct || 0),
            notes: l.notes || ""
          };
        });
        await actions.saveBom(existingBom, {
          productName: form.productName.value,
          section: form.section.value,
          batchBaseKg: form.batchBaseKg.value,
          yieldLossPct: form.yieldLossPct.value,
          lines: syncedLines,
          notes: form.notes.value
        });
      };
    }
  );
}

export function openReprocessModal(state, { item = null, prefillDispatch = null } = {}, actions) {
  const isEdit = Boolean(item);
  const d = prefillDispatch;
  const r = item || {
    sourceDispatchId: d?.id || "",
    drReference: d?.drReference || "",
    customer: d?.customer || "",
    productName: d?.productName || state.skus[0]?.productName || "",
    section: d?.sourcePlant || state.skus.find((s) => s.productName === d?.productName)?.section || state.skus[0]?.section || PLANT_SECTIONS[0],
    returnedDate: d?.date || todayIsoDate(),
    returnedBags: d?.returnedBags || 1,
    bagWeightKg: d?.bagWeightKg || state.skus[0]?.standardBagWeightKg || 25,
    returnedWeightKg: d?.returnedWeightKg || d?.bagWeightKg || 25,
    returnReason: d?.returnReason || RETURN_REASONS[0],
    status: "Pending Reprocess",
    fastActionNeeded: true,
    flaggedToInventory: false,
    actionNotes: ""
  };

  openModal(
    isEdit ? `Edit Reprocess • ${r.productName}` : d ? `Queue Return ${d.drReference} for Reprocess` : "Log Reprocess / Quarantine Item",
    `
      <form id="reprocess-form" class="form-grid">
        <div class="form-row-2">
          <div class="form-field">
            <label>Feed Product</label>
            <select name="productName" required>${skuSelectOptions(state.skus, r.productName)}</select>
          </div>
          <div class="form-field">
            <label>Plant Section</label>
            <select name="section">
              ${PLANT_SECTIONS.map((sec) => `<option value="${escapeAttr(sec)}" ${sec === r.section ? "selected" : ""}>${escapeAttr(sec)}</option>`).join("")}
            </select>
          </div>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>DR Reference</label>
            <input name="drReference" value="${escapeAttr(r.drReference || "")}" placeholder="Optional DR #" />
          </div>
          <div class="form-field">
            <label>Customer</label>
            <input name="customer" value="${escapeAttr(r.customer || "")}" placeholder="Optional customer" />
          </div>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Returned Bags</label>
            <input name="returnedBags" type="number" min="1" value="${escapeAttr(r.returnedBags)}" required />
          </div>
          <div class="form-field">
            <label>Bag Weight (kg)</label>
            <input name="bagWeightKg" type="number" step="0.1" min="0.1" value="${escapeAttr(r.bagWeightKg)}" required />
          </div>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Returned Weight (kg)</label>
            <input name="returnedWeightKg" type="number" step="0.1" min="0.1" value="${escapeAttr(r.returnedWeightKg)}" required />
          </div>
          <div class="form-field">
            <label>Returned Date</label>
            <input name="returnedDate" type="date" value="${escapeAttr(r.returnedDate)}" required />
          </div>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Return Reason</label>
            <select name="returnReason">
              ${RETURN_REASONS.map((rr) => `<option value="${escapeAttr(rr)}" ${rr === r.returnReason ? "selected" : ""}>${escapeAttr(rr)}</option>`).join("")}
            </select>
          </div>
          <div class="form-field">
            <label>Status</label>
            <select name="status">
              ${REPROCESS_STATUSES.map((st) => `<option value="${escapeAttr(st)}" ${st === r.status ? "selected" : ""}>${escapeAttr(st)}</option>`).join("")}
            </select>
          </div>
        </div>
        <div class="form-field">
          <label>Action Notes</label>
          <input name="actionNotes" value="${escapeAttr(r.actionNotes || "")}" placeholder="QC / milling notes" />
        </div>
        <label style="font-size:13px;"><input type="checkbox" name="fastActionNeeded" ${r.fastActionNeeded ? "checked" : ""} /> Fast Action Needed (Urgent QC)</label>
        <button type="submit" class="btn btn-primary">${isEdit ? "Save Changes" : "Save Reprocess Item"}</button>
      </form>
    `,
    (root) => {
      const form = root.querySelector("#reprocess-form");
      form.returnedBags.oninput = () => {
        form.returnedWeightKg.value = Number(form.returnedBags.value || 1) * Number(form.bagWeightKg.value || 25);
      };
      form.productName.onchange = () => {
        const sku = state.skus.find((x) => x.productName === form.productName.value);
        if (sku) {
          form.section.value = sku.section;
          form.bagWeightKg.value = sku.standardBagWeightKg;
        }
      };
      form.onsubmit = async (e) => {
        e.preventDefault();
        const st = form.status.value;
        await actions.saveReprocess(item, {
          sourceDispatchId: r.sourceDispatchId || "",
          drReference: form.drReference.value,
          customer: form.customer.value,
          productName: form.productName.value,
          section: form.section.value,
          returnedDate: form.returnedDate.value,
          returnedBags: form.returnedBags.value,
          bagWeightKg: form.bagWeightKg.value,
          returnedWeightKg: form.returnedWeightKg.value,
          reprocessedBags: item?.reprocessedBags || 0,
          reprocessedWeightKg: item?.reprocessedWeightKg || 0,
          returnReason: form.returnReason.value,
          status: st,
          fastActionNeeded: form.fastActionNeeded.checked,
          flaggedToInventory: st === "Flagged to Inventory",
          reprocessedDate: st === "Flagged to Inventory" ? todayIsoDate() : item?.reprocessedDate || "",
          actionNotes: form.actionNotes.value
        });
      };
    }
  );
}

export function openFlagReprocessModal(item, actions) {
  openModal(
    `Move to Inventory Stock • ${item.productName}`,
    `
      <form id="flag-reprocess-form" class="form-grid">
        <div style="font-size:13px; color:var(--muted);">
          Returned: <strong>${item.returnedBags} bags (${item.returnedWeightKg} kg)</strong>. Enter recovered quantity cleared back to Finished Goods stock.
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Reprocessed Bags</label>
            <input name="reprocessedBags" type="number" min="1" value="${escapeAttr(item.returnedBags)}" required />
          </div>
          <div class="form-field">
            <label>Reprocessed Weight (kg)</label>
            <input name="reprocessedWeightKg" type="number" step="0.1" min="0.1" value="${escapeAttr(item.returnedWeightKg)}" required />
          </div>
        </div>
        <div class="form-field">
          <label>QC Release Notes</label>
          <input name="actionNotes" value="${escapeAttr(item.actionNotes || "Cleared to finished stock")}" />
        </div>
        <button type="submit" class="btn btn-primary">Flag to Inventory</button>
      </form>
    `,
    (root) => {
      const form = root.querySelector("#flag-reprocess-form");
      form.reprocessedBags.oninput = () => {
        form.reprocessedWeightKg.value = Number(form.reprocessedBags.value || 1) * Number(item.bagWeightKg || 25);
      };
      form.onsubmit = async (e) => {
        e.preventDefault();
        await actions.saveReprocess(item, {
          ...item,
          reprocessedBags: form.reprocessedBags.value,
          reprocessedWeightKg: form.reprocessedWeightKg.value,
          status: "Flagged to Inventory",
          flaggedToInventory: true,
          fastActionNeeded: false,
          reprocessedDate: todayIsoDate(),
          actionNotes: form.actionNotes.value
        });
      };
    }
  );
}

export function openCustomerModal(existingCustomer, actions) {
  const isEdit = Boolean(existingCustomer);
  const c = existingCustomer || { name: "", notes: "", priority: false };
  openModal(
    isEdit ? `Edit Customer • ${c.name}` : "Add Customer",
    `
      <form id="customer-form" class="form-grid">
        <div class="form-field">
          <label>Customer / Account Name</label>
          <input name="name" value="${escapeAttr(c.name)}" placeholder="e.g., Bulacan Farm Supply" required />
        </div>
        <div class="form-field">
          <label>Notes / Route</label>
          <input name="notes" value="${escapeAttr(c.notes || "")}" placeholder="Delivery contact or route" />
        </div>
        <label style="font-size:13px;"><input type="checkbox" name="priority" ${c.priority ? "checked" : ""} /> Priority Customer</label>
        <button type="submit" class="btn btn-primary">${isEdit ? "Save Customer" : "Add Customer"}</button>
      </form>
    `,
    (root) => {
      const form = root.querySelector("#customer-form");
      form.onsubmit = async (e) => {
        e.preventDefault();
        await actions.saveCustomer(existingCustomer, {
          name: form.name.value,
          notes: form.notes.value,
          priority: form.priority.checked
        });
      };
    }
  );
}

export function openInviteMemberModal(actions) {
  openModal(
    "Invite Team Member",
    `
      <form id="invite-form" class="form-grid">
        <div class="form-field">
          <label>Google Email Address</label>
          <input name="email" type="email" placeholder="teammate@gmail.com" required />
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Name / Position (optional)</label>
            <input name="displayName" placeholder="e.g., Shift Supervisor" />
          </div>
          <div class="form-field">
            <label>Role</label>
            <select name="role">
              <option value="Operator">Operator</option>
              <option value="Viewer">Viewer</option>
              <option value="Admin">Admin</option>
            </select>
          </div>
        </div>
        <div class="form-field">
          <label>Personal Note for Gmail (optional)</label>
          <input name="customNote" placeholder="e.g., Join before morning shift" />
        </div>
        <div style="display:flex; gap:10px; flex-wrap:wrap;">
          <button type="submit" data-mode="gmail" class="btn btn-primary">Send to Google + Open Gmail</button>
          <button type="submit" data-mode="google" class="btn">Send to Google Only</button>
        </div>
      </form>
    `,
    (root) => {
      const form = root.querySelector("#invite-form");
      let chosenMode = "gmail";
      root.querySelectorAll("button[data-mode]").forEach((btn) => {
        btn.onclick = () => {
          chosenMode = btn.dataset.mode;
        };
      });
      form.onsubmit = async (e) => {
        e.preventDefault();
        await actions.sendInvite({
          email: form.email.value,
          displayName: form.displayName.value,
          role: form.role.value,
          customNote: form.customNote.value,
          openGmail: chosenMode === "gmail"
        });
      };
    }
  );
}

export function openRestoreJsonModal(actions) {
  openModal(
    "Restore JSON Workspace Backup (Admin)",
    `
      <form id="restore-json-form" class="form-grid">
        <div style="font-size:12px; color:var(--muted); line-height:1.45;">
          Select a JSON backup file exported from FeedMill Ops (Android or Web). Existing matching document IDs will be updated cleanly under Firestore rules.
        </div>
        <div class="form-field">
          <label>Choose Backup JSON File</label>
          <input id="restore-file-input" type="file" accept="application/json,.json" required />
        </div>
        <button type="submit" class="btn btn-primary">Restore Backup into Workspace</button>
      </form>
    `,
    (root) => {
      const form = root.querySelector("#restore-json-form");
      const fileInp = root.querySelector("#restore-file-input");
      form.onsubmit = async (e) => {
        e.preventDefault();
        const file = fileInp.files?.[0];
        if (!file) return;
        const text = await file.text();
        await actions.restoreJsonBackup(text);
      };
    }
  );
}

export function openSelectiveResetModal(actions) {
  const options = [
    ["orders", "Customer Orders"],
    ["dispatches", "Dispatched Deliveries"],
    ["schedules", "Production Schedules"],
    ["production_batches", "Production Batches"],
    ["reprocess_items", "Reprocess Queue"],
    ["customers", "Customer Directory"],
    ["raw_materials", "Raw Materials"],
    ["boms", "Feed BOM Formulas"],
    ["breadcrumbs", "Activity History Logs"],
    ["skus", "Finished Goods SKUs"]
  ];

  openModal(
    "Selective Reset Workspace Data (Admin)",
    `
      <form id="reset-form" class="form-grid">
        <div style="font-size:12px; color:var(--critical); font-weight:700;">
          Warning: Checked collections will be permanently deleted from your active workspace!
        </div>
        <div style="display:grid; grid-template-columns:1fr 1fr; gap:8px; font-size:13px;">
          ${options
            .map(([key, label]) => `<label><input type="checkbox" name="collections" value="${key}" /> ${escapeAttr(label)}</label>`)
            .join("")}
        </div>
        <div class="form-field">
          <label>Type RESET to confirm</label>
          <input name="confirmWord" placeholder="RESET" required />
        </div>
        <button type="submit" class="btn btn-danger">Permanently Delete Selected Data</button>
      </form>
    `,
    (root) => {
      const form = root.querySelector("#reset-form");
      form.onsubmit = async (e) => {
        e.preventDefault();
        if (form.confirmWord.value.trim().toUpperCase() !== "RESET") {
          return;
        }
        const selected = [...form.querySelectorAll('input[name="collections"]:checked')].map((el) => el.value);
        await actions.runSelectiveReset(selected);
      };
    }
  );
}

export function openMessengerShareModal({
  title = "Send Report to Messenger",
  defaultText = "",
  onDownloadPng = null,
  onCopySuccess = null,
  onSent = null
}) {
  const canSystemShare = typeof navigator !== "undefined" && typeof navigator.share === "function";

  openModal(
    title,
    `
      <div id="messenger-share-box" class="form-grid">
        <div style="font-size:12px; color:var(--muted); line-height:1.45;">
          Review or customize the summary below, then copy &amp; open Messenger (or download a PNG snapshot card to drag directly into your Messenger chat).
        </div>
        <div class="form-field">
          <label>Messenger Message</label>
          <textarea id="messenger-text-area" rows="10" style="font-family:monospace; font-size:12px; padding:10px; border:1px solid var(--border); border-radius:10px; resize:vertical;">${escapeAttr(defaultText)}</textarea>
        </div>
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <button id="msg-send-open-btn" type="button" class="btn btn-messenger">Copy &amp; Open Messenger</button>
          <button id="msg-copy-btn" type="button" class="btn">Copy Summary Text</button>
          ${onDownloadPng ? `<button id="msg-png-btn" type="button" class="btn">Download Image (.png)</button>` : ""}
          ${canSystemShare ? `<button id="msg-native-share-btn" type="button" class="btn">System Share</button>` : ""}
        </div>
      </div>
    `,
    (root) => {
      const textArea = root.querySelector("#messenger-text-area");

      const copyText = async () => {
        const text = textArea.value;
        await navigator.clipboard.writeText(text);
        return text;
      };

      root.querySelector("#msg-send-open-btn").onclick = async () => {
        try {
          await copyText();
        } catch (_) {}
        window.open("https://www.messenger.com/", "_blank", "noopener");
        if (onSent) onSent("Opened Messenger (summary copied — press Ctrl+V to paste)");
      };

      root.querySelector("#msg-copy-btn").onclick = async () => {
        try {
          await copyText();
          if (onCopySuccess) onCopySuccess("Messenger summary copied to clipboard.");
        } catch (err) {
          if (onCopySuccess) onCopySuccess(err?.message || "Clipboard copy failed.");
        }
      };

      const pngBtn = root.querySelector("#msg-png-btn");
      if (pngBtn && onDownloadPng) {
        pngBtn.onclick = () => onDownloadPng();
      }

      const nativeBtn = root.querySelector("#msg-native-share-btn");
      if (nativeBtn && canSystemShare) {
        nativeBtn.onclick = async () => {
          try {
            await navigator.share({ title, text: textArea.value });
            if (onSent) onSent("Report shared.");
          } catch (_) {}
        };
      }
    }
  );
}

export function openEmailShareModal({
  title = "Send Plant Report via Email",
  defaultTo = "",
  defaultSubject = "",
  defaultBody = "",
  suggestedEmails = [],
  onDownloadCsv = null,
  onDownloadXls = null,
  onToast = null,
  onSent = null
}) {
  const uniqueSuggestions = [...new Set(suggestedEmails.map((e) => String(e || "").trim().toLowerCase()).filter(Boolean))];

  openModal(
    title,
    `
      <form id="email-share-form" class="form-grid">
        ${
          uniqueSuggestions.length
            ? `<div class="form-field">
                <label>Quick-Add Team Recipients</label>
                <div class="chip-row">
                  ${uniqueSuggestions
                    .map((email) => `<button type="button" class="chip" data-toggle-email="${escapeAttr(email)}">+ ${escapeAttr(email)}</button>`)
                    .join("")}
                </div>
              </div>`
            : ""
        }
        <div class="form-field">
          <label>To (comma-separated emails)</label>
          <input id="email-to-input" name="to" value="${escapeAttr(defaultTo)}" placeholder="manager@company.com, supervisor@gmail.com" />
        </div>
        <div class="form-field">
          <label>Subject</label>
          <input id="email-subject-input" name="subject" value="${escapeAttr(defaultSubject)}" required />
        </div>
        <div class="form-field">
          <label>Report Body</label>
          <textarea id="email-body-input" name="body" rows="10" style="font-family:monospace; font-size:12px; padding:10px; border:1px solid var(--border); border-radius:10px; resize:vertical;">${escapeAttr(defaultBody)}</textarea>
        </div>
        ${
          onDownloadCsv || onDownloadXls
            ? `<div style="display:flex; gap:8px; flex-wrap:wrap; align-items:center; font-size:12px; color:var(--muted);">
                <span>Need an attachment?</span>
                ${onDownloadCsv ? `<button id="email-dl-csv-btn" type="button" class="btn btn-sm">Download CSV</button>` : ""}
                ${onDownloadXls ? `<button id="email-dl-xls-btn" type="button" class="btn btn-sm">Download Excel (.xls)</button>` : ""}
              </div>`
            : ""
        }
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <button id="email-open-gmail-btn" type="button" class="btn btn-email">Send via Gmail</button>
          <button id="email-open-mailto-btn" type="button" class="btn btn-primary">Open Default Mail App</button>
          <button id="email-copy-body-btn" type="button" class="btn">Copy Email Text</button>
        </div>
      </form>
    `,
    (root) => {
      const toInp = root.querySelector("#email-to-input");
      const subjInp = root.querySelector("#email-subject-input");
      const bodyInp = root.querySelector("#email-body-input");

      const parseEmails = () =>
        toInp.value
          .split(",")
          .map((s) => s.trim())
          .filter(Boolean);

      const syncChips = () => {
        const current = new Set(parseEmails().map((e) => e.toLowerCase()));
        root.querySelectorAll("[data-toggle-email]").forEach((btn) => {
          const em = btn.dataset.toggleEmail.toLowerCase();
          btn.classList.toggle("active", current.has(em));
        });
      };
      syncChips();

      root.querySelectorAll("[data-toggle-email]").forEach((btn) => {
        btn.onclick = () => {
          const target = btn.dataset.toggleEmail;
          const list = parseEmails();
          const idx = list.findIndex((x) => x.toLowerCase() === target.toLowerCase());
          if (idx >= 0) list.splice(idx, 1);
          else list.push(target);
          toInp.value = list.join(", ");
          syncChips();
        };
      });

      const dlCsvBtn = root.querySelector("#email-dl-csv-btn");
      if (dlCsvBtn && onDownloadCsv) dlCsvBtn.onclick = () => onDownloadCsv();

      const dlXlsBtn = root.querySelector("#email-dl-xls-btn");
      if (dlXlsBtn && onDownloadXls) dlXlsBtn.onclick = () => onDownloadXls();

      root.querySelector("#email-open-gmail-btn").onclick = async () => {
        const to = toInp.value.trim();
        const subject = subjInp.value.trim();
        const fullBody = bodyInp.value;
        try {
          await navigator.clipboard.writeText(fullBody);
        } catch (_) {}
        const safeUrlBody =
          fullBody.length > 1600
            ? `${fullBody.slice(0, 1500)}\n\n... (Full report copied to clipboard — press Ctrl+V if needed)`
            : fullBody;
        const gmailUrl = buildGmailWebComposeUrl({ to, subject, body: safeUrlBody });
        window.open(gmailUrl, "_blank", "noopener");
        if (onSent) onSent("Opened Gmail compose window.");
      };

      root.querySelector("#email-open-mailto-btn").onclick = async () => {
        const to = toInp.value.trim();
        const subject = subjInp.value.trim();
        const fullBody = bodyInp.value;
        try {
          await navigator.clipboard.writeText(fullBody);
        } catch (_) {}
        const safeBody = fullBody.length > 1500 ? fullBody.slice(0, 1500) : fullBody;
        window.location.href = buildMailtoUrl({ to, subject, body: safeBody });
        if (onSent) onSent("Opened default Mail client.");
      };

      root.querySelector("#email-copy-body-btn").onclick = async () => {
        try {
          await navigator.clipboard.writeText(`Subject: ${subjInp.value.trim()}\n\n${bodyInp.value}`);
          if (onToast) onToast("Email subject & report copied to clipboard.");
        } catch (err) {
          if (onToast) onToast(err?.message || "Clipboard copy failed.");
        }
      };
    }
  );
}

