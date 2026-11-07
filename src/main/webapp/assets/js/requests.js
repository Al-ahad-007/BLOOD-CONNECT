/**
 * BloodConnect – Emergency Blood Request Submission & Request Tracking Module
 */

const RequestsApp = (function () {

    // ==========================================
    // CREATE BLOOD REQUEST
    // ==========================================
    const initCreate = function () {
        const form = document.getElementById("blood-request-form");
        if (!form) return;

        // Auto pre-fill user info if logged in
        BloodConnect.checkAuth().then(user => {
            if (user) {
                const contactPerson = document.getElementById("contactPerson");
                const contactPhone = document.getElementById("contactPhone");
                const city = document.getElementById("city");
                const state = document.getElementById("state");
                const pincode = document.getElementById("pincode");

                if (contactPerson && !contactPerson.value) contactPerson.value = user.fullName || "";
                if (contactPhone && !contactPhone.value) contactPhone.value = user.phone || "";
                if (city && !city.value) city.value = user.city || "";
                if (state && !state.value) state.value = user.state || "";
                if (pincode && !pincode.value) pincode.value = user.pincode || "";
            }
        });

        // Preselect today or tomorrow as min date
        const dateInput = document.getElementById("requiredDate");
        if (dateInput) {
            const today = new Date().toISOString().split("T")[0];
            dateInput.min = today;
            dateInput.value = today;
        }

        form.addEventListener("submit", async function (e) {
            e.preventDefault();

            // Clear previous errors
            document.querySelectorAll(".field-error").forEach(el => el.classList.remove("show"));

            const patientName = document.getElementById("patientName").value.trim();
            const bloodGroup = document.getElementById("bloodGroup").value;
            const unitsRequired = parseInt(document.getElementById("unitsRequired").value, 10);
            const hospitalName = document.getElementById("hospitalName").value.trim();
            const hospitalAddress = document.getElementById("hospitalAddress").value.trim();
            const city = document.getElementById("city").value.trim();
            const state = document.getElementById("state").value.trim();
            const pincode = document.getElementById("pincode").value.trim();
            const contactPerson = document.getElementById("contactPerson").value.trim();
            const contactPhone = document.getElementById("contactPhone").value.trim();
            const requiredDate = document.getElementById("requiredDate").value;
            const urgency = document.getElementById("urgency").value;
            const description = document.getElementById("description").value.trim();

            let isValid = true;
            if (!patientName) {
                showError("patientName-error", "Patient name is required.");
                isValid = false;
            }
            if (!bloodGroup) {
                showError("bloodGroup-error", "Please select the required blood group.");
                isValid = false;
            }
            if (isNaN(unitsRequired) || unitsRequired < 1 || unitsRequired > 20) {
                showError("unitsRequired-error", "Units must be between 1 and 20.");
                isValid = false;
            }
            if (!hospitalName) {
                showError("hospitalName-error", "Hospital name is required.");
                isValid = false;
            }
            if (!hospitalAddress) {
                showError("hospitalAddress-error", "Hospital address is required.");
                isValid = false;
            }
            if (!city) {
                showError("city-error", "City is required.");
                isValid = false;
            }
            if (!state) {
                showError("state-error", "State is required.");
                isValid = false;
            }
            if (!pincode) {
                showError("pincode-error", "PIN code is required.");
                isValid = false;
            }
            if (!contactPerson) {
                showError("contactPerson-error", "Contact person name is required.");
                isValid = false;
            }
            if (!/^[0-9]{10}$/.test(contactPhone)) {
                showError("contactPhone-error", "Please enter a valid 10-digit emergency contact phone number.");
                isValid = false;
            }
            if (!requiredDate) {
                showError("requiredDate-error", "Required date is mandatory.");
                isValid = false;
            }

            if (!isValid) return;

            const submitBtn = form.querySelector("button[type='submit']");
            const originalBtnText = submitBtn.innerHTML;

            try {
                submitBtn.disabled = true;
                submitBtn.innerHTML = `<span class="spinner" style="width:16px;height:16px;border-width:2px;margin-right:8px;"></span> Submitting Request...`;

                const resp = await fetch(`${BloodConnect.contextPath}/api/requests/create`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        patientName, bloodGroup, unitsRequired, hospitalName, hospitalAddress,
                        city, state, pincode, contactPerson, contactPhone, requiredDate,
                        urgency, description
                    })
                });

                const res = await resp.json();
                if (resp.ok && res.success) {
                    showSuccessConfirmation(res.data);
                    form.reset();
                } else {
                    BloodConnect.showToast(res.message || "Failed to submit request.", "error");
                }
            } catch (err) {
                BloodConnect.showToast("Network error submitting request.", "error");
            } finally {
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalBtnText;
            }
        });

        function showError(id, msg) {
            const el = document.getElementById(id);
            if (el) {
                el.innerText = msg;
                el.classList.add("show");
            }
        }

        function showSuccessConfirmation(data) {
            let modal = document.getElementById("request-success-modal");
            if (!modal) {
                const m = document.createElement("div");
                m.id = "request-success-modal";
                m.className = "modal-backdrop";
                m.innerHTML = `
                    <div class="modal-box">
                        <div class="modal-body" style="text-align: center; padding: 2.5rem 1.5rem 1.5rem;">
                            <div style="width:64px; height:64px; border-radius:50%; background:#DCFCE7; color:#16A34A; display:flex; align-items:center; justify-content:center; margin:0 auto 1.25rem;">
                                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                            </div>
                            <h3 style="font-size:1.375rem; font-weight:700; color:var(--text-primary); margin-bottom:0.5rem;">Request Broadcasted Successfully!</h3>
                            <p style="font-size:0.9375rem; color:var(--text-secondary); margin-bottom:1.5rem;">
                                Your emergency request has been registered and is now visible to matching voluntary donors.
                            </p>
                            <div style="background:var(--bg-main); border-radius:var(--radius-md); padding:1rem; margin-bottom:1.5rem; text-align:left; font-size:0.875rem;">
                                <div style="display:flex; justify-content:space-between; margin-bottom:0.35rem;">
                                    <span style="color:var(--text-secondary);">Request ID:</span>
                                    <strong id="conf-req-id" style="color:var(--primary);"></strong>
                                </div>
                                <div style="display:flex; justify-content:space-between; margin-bottom:0.35rem;">
                                    <span style="color:var(--text-secondary);">Patient:</span>
                                    <strong id="conf-patient"></strong>
                                </div>
                                <div style="display:flex; justify-content:space-between;">
                                    <span style="color:var(--text-secondary);">Blood Group & Units:</span>
                                    <strong id="conf-blood"></strong>
                                </div>
                            </div>
                            <div style="display:flex; gap:0.75rem; justify-content:center;">
                                <a href="${BloodConnect.contextPath}/my-requests.html" class="btn btn-primary btn-sm">View My Requests</a>
                                <button type="button" class="btn btn-secondary btn-sm" onclick="BloodConnect.closeModal('request-success-modal')">Close</button>
                            </div>
                        </div>
                    </div>
                `;
                document.body.appendChild(m);
                modal = m;
            }

            document.getElementById("conf-req-id").innerText = `#${data.requestId}`;
            document.getElementById("conf-patient").innerText = data.patientName;
            document.getElementById("conf-blood").innerText = `${data.bloodGroup} (${data.unitsRequired} Unit${data.unitsRequired > 1 ? 's' : ''})`;

            BloodConnect.openModal("request-success-modal");
        }
    };

    // ==========================================
    // TRACK MY REQUESTS
    // ==========================================
    const initMyRequests = function () {
        const tableBody = document.getElementById("my-requests-tbody");
        const statusFilter = document.getElementById("filter-status");

        if (!tableBody) return;

        loadRequests();

        if (statusFilter) {
            statusFilter.addEventListener("change", loadRequests);
        }

        async function loadRequests() {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="8" style="text-align: center; padding: 2.5rem;">
                        <div class="spinner"></div>
                        <p style="margin-top: 0.5rem; color: var(--text-secondary);">Loading your blood requests...</p>
                    </td>
                </tr>
            `;

            try {
                const resp = await fetch(`${BloodConnect.contextPath}/api/requests/my`);
                const res = await resp.json();

                if (resp.ok && res.success && res.data) {
                    renderRequestsTable(res.data);
                } else {
                    renderTableEmpty("Failed to load requests.");
                }
            } catch (err) {
                renderTableEmpty("Network error loading requests.");
            }
        }

        function renderRequestsTable(requests) {
            const filter = statusFilter ? statusFilter.value : "ALL";
            const filtered = filter === "ALL" ? requests : requests.filter(r => r.requestStatus === filter);

            if (filtered.length === 0) {
                renderTableEmpty("You have not submitted any blood requests yet.");
                return;
            }

            let html = "";
            filtered.forEach(req => {
                const statusBadge = req.requestStatus === 'PENDING' ? 'badge-pending'
                    : req.requestStatus === 'ACCEPTED' ? 'badge-accepted'
                    : req.requestStatus === 'FULFILLED' ? 'badge-fulfilled' : 'badge-cancelled';

                const urgencyBadge = req.urgency === 'CRITICAL' ? 'badge-critical'
                    : req.urgency === 'URGENT' ? 'badge-urgent' : 'badge-normal';

                const canCancel = (req.requestStatus === 'PENDING' || req.requestStatus === 'ACCEPTED');

                html += `
                    <tr>
                        <td><strong>#${req.requestId}</strong></td>
                        <td>
                            <div style="font-weight:600;">${BloodConnect.escapeHtml(req.patientName)}</div>
                            <div style="font-size:0.75rem; color:var(--text-secondary);">${req.createdAt ? req.createdAt.substring(0, 10) : ''}</div>
                        </td>
                        <td><span class="badge" style="background:#FFE4E6;color:#BE123C;font-size:0.875rem;">${BloodConnect.escapeHtml(req.bloodGroup)}</span></td>
                        <td>${req.unitsRequired}</td>
                        <td>
                            <div>${BloodConnect.escapeHtml(req.hospitalName)}</div>
                            <div style="font-size:0.75rem; color:var(--text-secondary);">${BloodConnect.escapeHtml(req.city)}</div>
                        </td>
                        <td><span class="badge ${urgencyBadge}">${req.urgency}</span></td>
                        <td><span class="badge ${statusBadge}">${req.requestStatus}</span></td>
                        <td>
                            <div style="display:flex; align-items:center; gap:0.5rem;">
                                <button type="button" class="btn btn-secondary btn-sm" onclick="RequestsApp.viewResponses(${req.requestId})">
                                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
                                    Responses (${req.responseCount || 0})
                                </button>
                                ${canCancel ? `
                                    <button type="button" class="btn btn-outline btn-sm" style="color:var(--danger); border-color:var(--danger);" onclick="RequestsApp.cancelRequest(${req.requestId})">
                                        Cancel
                                    </button>
                                ` : ''}
                            </div>
                        </td>
                    </tr>
                `;
            });

            tableBody.innerHTML = html;
        }

        function renderTableEmpty(msg) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="8">
                        <div class="empty-state">
                            <p class="empty-state-desc">${msg}</p>
                            <a href="${BloodConnect.contextPath}/request-blood.html" class="btn btn-primary btn-sm">Raise Emergency Request</a>
                        </div>
                    </td>
                </tr>
            `;
        }
    };

    const viewResponses = async function (requestId) {
        let modal = document.getElementById("view-responses-modal");
        if (!modal) {
            const m = document.createElement("div");
            m.id = "view-responses-modal";
            m.className = "modal-backdrop";
            m.innerHTML = `
                <div class="modal-box" style="max-width: 650px;">
                    <div class="modal-header">
                        <h3 id="responses-modal-title">Donor Responses</h3>
                        <button type="button" class="modal-close-btn" onclick="BloodConnect.closeModal('view-responses-modal')">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                        </button>
                    </div>
                    <div class="modal-body" id="responses-modal-body">
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary btn-sm" onclick="BloodConnect.closeModal('view-responses-modal')">Close</button>
                    </div>
                </div>
            `;
            document.body.appendChild(m);
            modal = m;
        }

        const bodyEl = document.getElementById("responses-modal-body");
        bodyEl.innerHTML = `<div class="loading-container"><div class="spinner"></div><p>Loading donor responses...</p></div>`;
        BloodConnect.openModal("view-responses-modal");

        try {
            const resp = await fetch(`${BloodConnect.contextPath}/api/requests/detail?id=${requestId}`);
            const res = await resp.json();

            if (resp.ok && res.success && res.data) {
                const req = res.data.request;
                const responses = res.data.responses;

                document.getElementById("responses-modal-title").innerText = `Responses for Request #${requestId} (${req.bloodGroup})`;

                if (!responses || responses.length === 0) {
                    bodyEl.innerHTML = `
                        <div class="empty-state" style="padding: 2rem 1rem;">
                            <p class="empty-state-desc">No donor responses recorded yet. Potential matching donors have been notified in this area.</p>
                        </div>
                    `;
                    return;
                }

                let html = `<div style="display:flex; flex-direction:column; gap:1rem;">`;
                responses.forEach(r => {
                    const badge = r.responseStatus === 'ACCEPTED' ? 'badge-available' : 'badge-unavailable';
                    html += `
                        <div class="card" style="padding:1.25rem;">
                            <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:0.5rem;">
                                <div>
                                    <h4 style="font-size:1rem; font-weight:700;">${BloodConnect.escapeHtml(r.donorName)}</h4>
                                    <div style="font-size:0.8125rem; color:var(--text-secondary);">${BloodConnect.escapeHtml(r.donorCity)} • Blood Group: <strong>${BloodConnect.escapeHtml(r.bloodGroup)}</strong></div>
                                </div>
                                <span class="badge ${badge}">${r.responseStatus}</span>
                            </div>
                            ${r.responseMessage ? `
                                <p style="font-size:0.875rem; background:var(--bg-main); padding:0.65rem 0.85rem; border-radius:var(--radius-sm); margin-bottom:0.75rem;">
                                    "${BloodConnect.escapeHtml(r.responseMessage)}"
                                </p>
                            ` : ''}
                            <div style="display:flex; justify-content:space-between; align-items:center; font-size:0.8125rem; padding-top:0.5rem; border-top:1px solid var(--border-color);">
                                <span style="color:var(--text-secondary);">Responded: ${r.respondedAt ? r.respondedAt.substring(0, 16) : '-'}</span>
                                ${r.donorPhone ? `
                                    <a href="tel:${r.donorPhone}" class="btn btn-primary btn-sm" style="padding:0.25rem 0.65rem;">
                                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path></svg>
                                        Call Donor (${r.donorPhone})
                                    </a>
                                ` : ''}
                            </div>
                        </div>
                    `;
                });
                html += `</div>`;
                bodyEl.innerHTML = html;
            } else {
                bodyEl.innerHTML = `<p style="color:var(--danger);">Failed to load response details.</p>`;
            }
        } catch (e) {
            bodyEl.innerHTML = `<p style="color:var(--danger);">Connection error loading details.</p>`;
        }
    };

    const cancelRequest = async function (requestId) {
        if (!confirm(`Are you sure you want to cancel Blood Request #${requestId}?`)) return;

        try {
            const resp = await fetch(`${BloodConnect.contextPath}/api/requests/cancel`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ requestId })
            });
            const res = await resp.json();
            if (resp.ok && res.success) {
                BloodConnect.showToast("Request cancelled successfully.", "success");
                initMyRequests();
            } else {
                BloodConnect.showToast(res.message || "Failed to cancel request.", "error");
            }
        } catch (e) {
            BloodConnect.showToast("Network error cancelling request.", "error");
        }
    };

    return {
        initCreate: initCreate,
        initMyRequests: initMyRequests,
        viewResponses: viewResponses,
        cancelRequest: cancelRequest
    };
})();
