/**
 * BloodConnect – Donor Search & Donor Dashboard Module
 */

const DonorApp = (function () {

    // ==========================================
    // DONOR SEARCH
    // ==========================================
    const initSearch = function () {
        const searchForm = document.getElementById("donor-search-form");
        const resultsContainer = document.getElementById("donor-results-grid");
        const resultsCountEl = document.getElementById("results-count");
        const resetBtn = document.getElementById("reset-search-btn");

        if (!searchForm || !resultsContainer) return;

        // Check URL parameters for preset filters (e.g. from homepage blood group badges)
        const urlParams = new URLSearchParams(window.location.search);
        if (urlParams.has("bloodGroup")) {
            const bgSelect = document.getElementById("search-blood-group");
            if (bgSelect) bgSelect.value = urlParams.get("bloodGroup");
        }
        if (urlParams.has("city")) {
            const cityInput = document.getElementById("search-city");
            if (cityInput) cityInput.value = urlParams.get("city");
        }

        // Initial search load
        executeSearch();

        searchForm.addEventListener("submit", function (e) {
            e.preventDefault();
            executeSearch();
        });

        if (resetBtn) {
            resetBtn.addEventListener("click", function () {
                searchForm.reset();
                executeSearch();
            });
        }

        async function executeSearch() {
            const bloodGroup = document.getElementById("search-blood-group").value;
            const city = document.getElementById("search-city").value.trim();
            const state = document.getElementById("search-state").value.trim();
            const pincode = document.getElementById("search-pincode").value.trim();
            const availability = document.getElementById("search-availability").value;

            resultsContainer.innerHTML = `
                <div class="loading-container" style="grid-column: 1 / -1;">
                    <div class="spinner"></div>
                    <p>Searching verified donors in your area...</p>
                </div>
            `;

            try {
                const queryParams = new URLSearchParams({
                    bloodGroup: bloodGroup || "ALL",
                    city: city || "",
                    state: state || "",
                    pincode: pincode || "",
                    availability: availability || "ALL"
                });

                const resp = await fetch(`${BloodConnect.contextPath}/api/donors/search?${queryParams.toString()}`);
                const res = await resp.json();

                if (resp.ok && res.success) {
                    renderDonorCards(res.data);
                } else {
                    renderEmptyState(res.message || "Failed to load donors.");
                }
            } catch (err) {
                renderEmptyState("Network error while loading donors.");
            }
        }

        function renderDonorCards(donors) {
            if (!donors || donors.length === 0) {
                renderEmptyState("No donors found matching your criteria. Try widening your search filters or submit an Emergency Request.");
                if (resultsCountEl) resultsCountEl.innerText = "0 donors found";
                return;
            }

            if (resultsCountEl) {
                resultsCountEl.innerText = `${donors.length} donor${donors.length > 1 ? 's' : ''} found`;
            }

            let html = "";
            donors.forEach(donor => {
                const isAvail = donor.isAvailable;
                const statusBadgeClass = isAvail ? "badge-available" : "badge-unavailable";
                const statusText = isAvail ? "Available to Donate" : "Currently Unavailable";
                const initials = donor.fullName ? donor.fullName.split(" ").map(n => n[0]).slice(0, 2).join("") : "D";

                html += `
                    <div class="donor-card">
                        <div>
                            <div class="donor-card-top">
                                <div class="donor-avatar">${initials}</div>
                                <div class="donor-blood-badge">${BloodConnect.escapeHtml(donor.bloodGroup)}</div>
                            </div>
                            <div class="donor-info">
                                <h4>${BloodConnect.escapeHtml(donor.fullName)}</h4>
                                <div style="margin-bottom: 0.75rem;">
                                    <span class="badge ${statusBadgeClass}">
                                        <span style="width: 6px; height: 6px; border-radius: 50%; background-color: currentColor; display: inline-block;"></span>
                                        ${statusText}
                                    </span>
                                </div>
                                <div class="donor-meta">
                                    <div class="donor-meta-item">
                                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"></path><circle cx="12" cy="10" r="3"></circle></svg>
                                        <span>${BloodConnect.escapeHtml(donor.city)}, ${BloodConnect.escapeHtml(donor.state)} (PIN: ${BloodConnect.escapeHtml(donor.pincode)})</span>
                                    </div>
                                    ${donor.lastUpdated ? `
                                        <div class="donor-meta-item">
                                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                                            <span>Updated: ${donor.lastUpdated.substring(0, 10)}</span>
                                        </div>
                                    ` : ''}
                                </div>
                            </div>
                        </div>

                        <div class="donor-card-actions">
                            ${donor.canContact ? `
                                <button type="button" class="btn btn-primary btn-sm btn-block" onclick="DonorApp.showContactModal('${BloodConnect.escapeHtml(donor.fullName)}', '${BloodConnect.escapeHtml(donor.phone)}', '${BloodConnect.escapeHtml(donor.email)}', '${BloodConnect.escapeHtml(donor.bloodGroup)}')">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path></svg>
                                    Contact Donor
                                </button>
                            ` : `
                                <a href="${BloodConnect.contextPath}/login.html" class="btn btn-outline btn-sm btn-block">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                                    Login to Contact
                                </a>
                            `}
                        </div>
                    </div>
                `;
            });

            resultsContainer.innerHTML = html;
        }

        function renderEmptyState(message) {
            resultsContainer.innerHTML = `
                <div class="empty-state" style="grid-column: 1 / -1;">
                    <div class="empty-state-icon">
                        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
                    </div>
                    <h3 class="empty-state-title">No Donors Found</h3>
                    <p class="empty-state-desc">${message}</p>
                    <a href="${BloodConnect.contextPath}/request-blood.html" class="btn btn-primary btn-sm">Submit Blood Request</a>
                </div>
            `;
        }
    };

    // Show contact modal with verified details
    const showContactModal = function (name, phone, email, bloodGroup) {
        let modalEl = document.getElementById("contact-donor-modal");
        if (!modalEl) {
            const modalDiv = document.createElement("div");
            modalDiv.id = "contact-donor-modal";
            modalDiv.className = "modal-backdrop";
            modalDiv.innerHTML = `
                <div class="modal-box">
                    <div class="modal-header">
                        <h3 id="contact-modal-title">Contact Donor</h3>
                        <button type="button" class="modal-close-btn" onclick="BloodConnect.closeModal('contact-donor-modal')">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                        </button>
                    </div>
                    <div class="modal-body" id="contact-modal-body">
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary btn-sm" onclick="BloodConnect.closeModal('contact-donor-modal')">Close</button>
                    </div>
                </div>
            `;
            document.body.appendChild(modalDiv);
            modalEl = modalDiv;
        }

        document.getElementById("contact-modal-title").innerText = `Contact ${name} (${bloodGroup})`;
        document.getElementById("contact-modal-body").innerHTML = `
            <div style="text-align: center; margin-bottom: 1.5rem;">
                <div style="font-size: 1.25rem; font-weight: 700; color: var(--text-primary); margin-bottom: 0.25rem;">${name}</div>
                <span class="badge badge-available">Registered Donor • ${bloodGroup}</span>
            </div>

            <div style="background-color: var(--bg-main); border-radius: var(--radius-md); padding: 1.25rem; margin-bottom: 1.25rem;">
                <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom: 0.85rem; padding-bottom: 0.85rem; border-bottom: 1px solid var(--border-color);">
                    <span style="font-size:0.875rem; color:var(--text-secondary);">Phone Number:</span>
                    <a href="tel:${phone}" style="font-size:1.0625rem; font-weight:700; color:var(--primary); text-decoration:none;">${phone}</a>
                </div>
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <span style="font-size:0.875rem; color:var(--text-secondary);">Email:</span>
                    <a href="mailto:${email}" style="font-size:0.9375rem; font-weight:500; color:var(--text-primary); text-decoration:none;">${email}</a>
                </div>
            </div>

            <div class="disclaimer-banner" style="margin: 0; padding: 0.85rem 1rem;">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 9v4"></path><path d="M12 17h.01"></path><circle cx="12" cy="12" r="10"></circle></svg>
                <div class="disclaimer-content">
                    <h4>Important Communication Protocol</h4>
                    <p>Please be respectful when contacting voluntary donors. Explain the hospital location and emergency details clearly. Medical compatibility must be verified at the hospital blood bank.</p>
                </div>
            </div>
        `;

        BloodConnect.openModal("contact-donor-modal");
    };

    // ==========================================
    // DONOR DASHBOARD
    // ==========================================
    const initDashboard = async function () {
        const statsContainer = document.getElementById("donor-stats-section");
        if (!statsContainer) return;

        // Load stats
        await loadStats();

        // Load incoming emergency requests
        await loadIncomingRequests();

        // Load response history
        await loadResponseHistory();

        // Setup availability toggle button
        const availToggleBtn = document.getElementById("toggle-availability-btn");
        if (availToggleBtn) {
            availToggleBtn.addEventListener("click", async function () {
                try {
                    availToggleBtn.disabled = true;
                    const resp = await fetch(`${BloodConnect.contextPath}/api/donor/availability`, {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({})
                    });
                    const res = await resp.json();
                    if (resp.ok && res.success) {
                        BloodConnect.showToast(res.message, "success");
                        await loadStats();
                    } else {
                        BloodConnect.showToast(res.message || "Failed to update availability.", "error");
                    }
                } catch (e) {
                    BloodConnect.showToast("Connection error updating availability.", "error");
                } finally {
                    availToggleBtn.disabled = false;
                }
            });
        }

        // Setup Profile Form Update
        const profileForm = document.getElementById("donor-profile-form");
        if (profileForm) {
            profileForm.addEventListener("submit", async function (e) {
                e.preventDefault();
                const payload = {
                    fullName: document.getElementById("prof-name").value.trim(),
                    phone: document.getElementById("prof-phone").value.trim(),
                    bloodGroup: document.getElementById("prof-blood-group").value,
                    city: document.getElementById("prof-city").value.trim(),
                    state: document.getElementById("prof-state").value.trim(),
                    pincode: document.getElementById("prof-pincode").value.trim(),
                    address: document.getElementById("prof-address").value.trim(),
                    isDiscoverable: document.getElementById("prof-discoverable").checked
                };

                try {
                    const resp = await fetch(`${BloodConnect.contextPath}/api/donor/profile`, {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify(payload)
                    });
                    const res = await resp.json();
                    if (resp.ok && res.success) {
                        BloodConnect.showToast("Profile updated successfully!", "success");
                        await loadStats();
                    } else {
                        BloodConnect.showToast(res.message || "Failed to update profile.", "error");
                    }
                } catch (e) {
                    BloodConnect.showToast("Network error updating profile.", "error");
                }
            });
        }

        // Setup Password Form
        const passForm = document.getElementById("donor-password-form");
        if (passForm) {
            passForm.addEventListener("submit", async function (e) {
                e.preventDefault();
                const oldPassword = document.getElementById("old-password").value;
                const newPassword = document.getElementById("new-password").value;
                const confirmPassword = document.getElementById("confirm-new-password").value;

                if (newPassword !== confirmPassword) {
                    BloodConnect.showToast("New passwords do not match.", "error");
                    return;
                }

                try {
                    const resp = await fetch(`${BloodConnect.contextPath}/api/user/password`, {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ oldPassword, newPassword, confirmPassword })
                    });
                    const res = await resp.json();
                    if (resp.ok && res.success) {
                        BloodConnect.showToast("Password updated successfully!", "success");
                        passForm.reset();
                    } else {
                        BloodConnect.showToast(res.message || "Failed to update password.", "error");
                    }
                } catch (e) {
                    BloodConnect.showToast("Network error updating password.", "error");
                }
            });
        }
    };

    async function loadStats() {
        try {
            const resp = await fetch(`${BloodConnect.contextPath}/api/donor/stats`);
            const res = await resp.json();
            if (resp.ok && res.success) {
                const s = res.data;
                const badgeEl = document.getElementById("dash-avail-badge");
                const btnEl = document.getElementById("toggle-availability-btn");
                const bgEl = document.getElementById("dash-blood-group");
                const nameEl = document.getElementById("dash-donor-name");

                if (nameEl) nameEl.innerText = s.fullName;
                if (bgEl) bgEl.innerText = s.bloodGroup;

                if (badgeEl) {
                    if (s.isAvailable) {
                        badgeEl.className = "badge badge-available";
                        badgeEl.innerHTML = `<span style="width:6px;height:6px;border-radius:50%;background:currentColor;display:inline-block;"></span> Available`;
                        if (btnEl) {
                            btnEl.className = "btn btn-outline btn-sm";
                            btnEl.innerText = "Switch to Unavailable";
                        }
                    } else {
                        badgeEl.className = "badge badge-unavailable";
                        badgeEl.innerHTML = `<span style="width:6px;height:6px;border-radius:50%;background:currentColor;display:inline-block;"></span> Unavailable`;
                        if (btnEl) {
                            btnEl.className = "btn btn-success btn-sm";
                            btnEl.innerText = "Switch to Available";
                        }
                    }
                }

                const recCount = document.getElementById("dash-incoming-count");
                const accCount = document.getElementById("dash-accepted-count");
                const compCount = document.getElementById("dash-completed-count");
                const statusText = document.getElementById("dash-status-text");

                if (recCount) recCount.innerText = s.matchingRequests !== undefined ? s.matchingRequests : (s.incomingRequests || 0);
                if (accCount) accCount.innerText = s.acceptedRequests || 0;
                if (compCount) compCount.innerText = s.completedDonations || 0;
                if (statusText) {
                    statusText.innerText = s.isAvailable ? "Available" : "Unavailable";
                    statusText.style.color = s.isAvailable ? "var(--success)" : "var(--danger)";
                }
            }
        } catch (e) {
            console.error("Error loading donor stats:", e);
        }
    }

    async function loadIncomingRequests() {
        const container = document.getElementById("incoming-requests-list");
        if (!container) return;

        try {
            const resp = await fetch(`${BloodConnect.contextPath}/api/donor/requests`);
            const res = await resp.json();

            if (resp.ok && res.success && res.data && res.data.length > 0) {
                let html = "";
                res.data.forEach(req => {
                    const urgencyBadge = req.urgency === 'CRITICAL' ? 'badge-critical'
                        : req.urgency === 'URGENT' ? 'badge-urgent' : 'badge-normal';

                    html += `
                        <div class="card card-hover" style="margin-bottom: 1.25rem;">
                            <div class="card-body">
                                <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:0.75rem;">
                                    <div>
                                        <div style="display:flex; align-items:center; gap:0.5rem; margin-bottom:0.25rem;">
                                            <span class="badge ${urgencyBadge}">${req.urgency}</span>
                                            <span style="font-size:0.8125rem; color:var(--text-secondary);">Request #${req.requestId}</span>
                                        </div>
                                        <h4 style="font-size:1.125rem; font-weight:700;">Patient: ${BloodConnect.escapeHtml(req.patientName)}</h4>
                                    </div>
                                    <div class="donor-blood-badge" style="font-size:0.9375rem;">${BloodConnect.escapeHtml(req.bloodGroup)} • ${req.unitsRequired} Unit(s)</div>
                                </div>

                                <div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(200px, 1fr)); gap:0.75rem; font-size:0.875rem; color:var(--text-secondary); margin-bottom:1rem;">
                                    <div><strong>Hospital:</strong> ${BloodConnect.escapeHtml(req.hospitalName)}</div>
                                    <div><strong>Location:</strong> ${BloodConnect.escapeHtml(req.city)}, ${BloodConnect.escapeHtml(req.state)}</div>
                                    <div><strong>Contact:</strong> ${BloodConnect.escapeHtml(req.contactPerson)} (${BloodConnect.escapeHtml(req.contactPhone)})</div>
                                    <div><strong>Needed By:</strong> ${req.requiredDate}</div>
                                </div>

                                ${req.description ? `
                                    <p style="font-size:0.875rem; background-color:var(--bg-main); padding:0.75rem; border-radius:var(--radius-sm); margin-bottom:1rem; border-left:3px solid var(--primary);">
                                        "${BloodConnect.escapeHtml(req.description)}"
                                    </p>
                                ` : ''}

                                <div style="display:flex; gap:0.75rem;">
                                    <button type="button" class="btn btn-primary btn-sm" onclick="DonorApp.openResponseModal(${req.requestId}, '${BloodConnect.escapeHtml(req.patientName)}', '${BloodConnect.escapeHtml(req.bloodGroup)}')">
                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg>
                                        Accept Request
                                    </button>
                                    <button type="button" class="btn btn-secondary btn-sm" onclick="DonorApp.submitDirectResponse(${req.requestId}, 'DECLINED', 'Unable to donate at this time.')">
                                        Decline
                                    </button>
                                </div>
                            </div>
                        </div>
                    `;
                });
                container.innerHTML = html;
            } else {
                container.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-state-icon">
                            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
                        </div>
                        <h4 class="empty-state-title">No Urgent Requests Pending</h4>
                        <p class="empty-state-desc">There are currently no open blood requests matching your blood group or location. Thank you for staying available!</p>
                    </div>
                `;
            }
        } catch (e) {
            container.innerHTML = `<p style="color:var(--danger);">Error loading incoming requests.</p>`;
        }
    }

    async function loadResponseHistory() {
        const container = document.getElementById("donor-history-list");
        if (!container) return;

        try {
            const resp = await fetch(`${BloodConnect.contextPath}/api/donor/responses`);
            const res = await resp.json();

            if (resp.ok && res.success && res.data && res.data.length > 0) {
                let html = `
                    <div class="table-responsive">
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Request #</th>
                                    <th>Patient</th>
                                    <th>Blood Group</th>
                                    <th>Hospital</th>
                                    <th>My Response</th>
                                    <th>Message</th>
                                    <th>Date</th>
                                </tr>
                            </thead>
                            <tbody>
                `;

                res.data.forEach(r => {
                    const badge = r.responseStatus === 'ACCEPTED' ? 'badge-available' : 'badge-unavailable';
                    html += `
                        <tr>
                            <td>#${r.requestId}</td>
                            <td><strong>${BloodConnect.escapeHtml(r.patientName)}</strong></td>
                            <td><span class="badge" style="background:#FFE4E6;color:#BE123C;">${BloodConnect.escapeHtml(r.bloodGroup)}</span></td>
                            <td>${BloodConnect.escapeHtml(r.hospitalName)}</td>
                            <td><span class="badge ${badge}">${r.responseStatus}</span></td>
                            <td style="font-size:0.8125rem;">${BloodConnect.escapeHtml(r.responseMessage || '-')}</td>
                            <td style="font-size:0.8125rem; color:var(--text-secondary);">${r.respondedAt ? r.respondedAt.substring(0, 16) : '-'}</td>
                        </tr>
                    `;
                });

                html += `</tbody></table></div>`;
                container.innerHTML = html;
            } else {
                container.innerHTML = `
                    <div class="empty-state">
                        <p class="empty-state-desc">You have not responded to any requests yet.</p>
                    </div>
                `;
            }
        } catch (e) {
            container.innerHTML = `<p style="color:var(--danger);">Error loading history.</p>`;
        }
    }

    const openResponseModal = function (requestId, patient, bloodGroup) {
        let modal = document.getElementById("respond-modal");
        if (!modal) {
            const m = document.createElement("div");
            m.id = "respond-modal";
            m.className = "modal-backdrop";
            m.innerHTML = `
                <div class="modal-box">
                    <div class="modal-header">
                        <h3 id="respond-modal-title">Respond to Request</h3>
                        <button type="button" class="modal-close-btn" onclick="BloodConnect.closeModal('respond-modal')">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                        </button>
                    </div>
                    <form id="respond-form">
                        <input type="hidden" id="respond-req-id" />
                        <div class="modal-body">
                            <p id="respond-modal-info" style="margin-bottom: 1rem; font-size: 0.9375rem; color: var(--text-secondary);"></p>
                            <div class="form-group">
                                <label class="form-label">Personal Message to Requester / Hospital:</label>
                                <textarea id="respond-message" class="form-textarea" placeholder="E.g., I am available and can reach Lilavati Hospital by 4:00 PM today."></textarea>
                                <div class="form-hint">Let the patient's family know your estimated arrival time or availability window.</div>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary btn-sm" onclick="BloodConnect.closeModal('respond-modal')">Cancel</button>
                            <button type="submit" class="btn btn-primary btn-sm">Confirm Acceptance</button>
                        </div>
                    </form>
                </div>
            `;
            document.body.appendChild(m);
            modal = m;

            document.getElementById("respond-form").addEventListener("submit", async function (e) {
                e.preventDefault();
                const reqId = document.getElementById("respond-req-id").value;
                const msg = document.getElementById("respond-message").value.trim();
                await submitDirectResponse(reqId, "ACCEPTED", msg);
                BloodConnect.closeModal("respond-modal");
            });
        }

        document.getElementById("respond-req-id").value = requestId;
        document.getElementById("respond-modal-title").innerText = `Accept Request #${requestId}`;
        document.getElementById("respond-modal-info").innerText = `You are confirming willingness to donate ${bloodGroup} blood for patient ${patient}.`;
        document.getElementById("respond-message").value = "I am willing to donate and will coordinate with the contact person.";
        BloodConnect.openModal("respond-modal");
    };

    const submitDirectResponse = async function (requestId, status, message) {
        try {
            const resp = await fetch(`${BloodConnect.contextPath}/api/donor/respond`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ requestId, status, message })
            });
            const res = await resp.json();
            if (resp.ok && res.success) {
                BloodConnect.showToast(status === 'ACCEPTED' ? "Thank you! Response recorded and shared with requester." : "Request declined.", "success");
                await loadStats();
                await loadIncomingRequests();
                await loadResponseHistory();
            } else {
                BloodConnect.showToast(res.message || "Failed to submit response.", "error");
            }
        } catch (e) {
            BloodConnect.showToast("Network error submitting response.", "error");
        }
    };

    return {
        initSearch: initSearch,
        showContactModal: showContactModal,
        initDashboard: initDashboard,
        openResponseModal: openResponseModal,
        submitDirectResponse: submitDirectResponse
    };
})();
