/**
 * BloodConnect – Administrator Dashboard Logic
 * Powers Overview KPIs, Donor Management, Blood Request Lifecycle Updates,
 * User Moderation, Reports Analytics, and System Configuration.
 */

const AdminApp = (function () {

    let cachedDonors = [];
    let cachedRequests = [];
    let cachedUsers = [];
    let cachedStats = {};

    const tabTitles = {
        "tab-overview": "Platform Overview",
        "tab-donors": "Donor Management",
        "tab-requests": "Emergency Blood Requests",
        "tab-users": "User Accounts & Moderation",
        "tab-reports": "Reports & System Analytics",
        "tab-settings": "System Settings"
    };

    const init = async function () {
        // Enforce admin access verification
        const user = await BloodConnect.checkAuth();
        if (!user || user.role !== "ADMIN") {
            window.location.href = `${BloodConnect.contextPath}/login.html?error=forbidden`;
            return;
        }

        const adminNameEl = document.getElementById("admin-user-name");
        if (adminNameEl) adminNameEl.innerText = user.fullName;

        // Setup sidebar tabs
        initTabs();

        // Load all data
        await loadStats();
        await loadRequests();
        await loadDonors();
        await loadUsers();

        // Setup search & filter listeners
        setupFilterListeners();
    };

    const initTabs = function () {
        const navItems = document.querySelectorAll(".admin-nav-item");
        navItems.forEach(btn => {
            btn.addEventListener("click", function () {
                const targetTab = this.getAttribute("data-tab");
                if (targetTab) {
                    switchTab(targetTab);
                }
            });
        });
    };

    const switchTab = function (tabId) {
        document.querySelectorAll(".admin-nav-item").forEach(b => {
            if (b.getAttribute("data-tab") === tabId) {
                b.classList.add("active");
            } else {
                b.classList.remove("active");
            }
        });

        document.querySelectorAll(".tab-pane").forEach(p => {
            if (p.id === tabId) {
                p.classList.add("active");
            } else {
                p.classList.remove("active");
            }
        });

        const titleEl = document.getElementById("admin-view-title");
        if (titleEl && tabTitles[tabId]) {
            titleEl.innerText = tabTitles[tabId];
        }

        if (tabId === "tab-reports") {
            renderReports();
        }
    };

    async function loadStats() {
        try {
            const resp = await fetch(`${BloodConnect.contextPath}/api/admin/stats`);
            const res = await resp.json();
            if (resp.ok && res.success) {
                cachedStats = res.data;
                setElText("stat-total-users", cachedStats.totalUsers);
                setElText("stat-total-donors", cachedStats.totalDonors);
                setElText("stat-avail-donors", cachedStats.availableDonors);
                setElText("stat-total-requests", cachedStats.totalRequests);
                setElText("stat-pending-requests", cachedStats.pendingRequests);
                setElText("stat-fulfilled-requests", cachedStats.fulfilledRequests);
            }
        } catch (e) {
            console.error("Error loading admin stats:", e);
        }
    }

    function setElText(id, text) {
        const el = document.getElementById(id);
        if (el) el.innerText = text;
    }

    // ==========================================
    // 1. BLOOD REQUESTS
    // ==========================================
    async function loadRequests() {
        const tbody = document.getElementById("admin-requests-tbody");
        if (!tbody) return;

        try {
            const resp = await fetch(`${BloodConnect.contextPath}/api/admin/requests`);
            const res = await resp.json();
            if (resp.ok && res.success && res.data) {
                cachedRequests = res.data;
                renderRequestsTable(cachedRequests);
                renderOverviewQueue(cachedRequests);
            }
        } catch (e) {
            tbody.innerHTML = `<tr><td colspan="9" style="color:var(--danger); text-align:center;">Failed to load requests.</td></tr>`;
        }
    }

    function renderRequestsTable(requests) {
        const tbody = document.getElementById("admin-requests-tbody");
        if (!tbody) return;

        if (!requests || requests.length === 0) {
            tbody.innerHTML = `<tr><td colspan="9" style="text-align:center; padding:2rem; color:var(--text-secondary);">No blood requests found matching criteria.</td></tr>`;
            return;
        }

        let html = "";
        requests.forEach(r => {
            const urgencyBadge = r.urgency === 'CRITICAL' ? 'badge-critical'
                : r.urgency === 'URGENT' ? 'badge-urgent' : 'badge-normal';

            const statusBadge = r.requestStatus === 'FULFILLED' ? 'badge-available'
                : r.requestStatus === 'CANCELLED' ? 'badge-unavailable'
                : r.requestStatus === 'ACCEPTED' ? 'badge-urgent' : 'badge-critical';

            html += `
                <tr>
                    <td style="font-weight:700; color:var(--primary);">#${r.requestId}</td>
                    <td>
                        <div style="font-weight:600;">${BloodConnect.escapeHtml(r.patientName)}</div>
                        <div style="font-size:0.75rem; color:var(--text-secondary);">By: ${BloodConnect.escapeHtml(r.contactPerson)}</div>
                    </td>
                    <td><span class="donor-blood-badge" style="font-size:0.75rem; padding:0.15rem 0.45rem;">${BloodConnect.escapeHtml(r.bloodGroup)}</span></td>
                    <td style="font-weight:600;">${r.unitsRequired}</td>
                    <td>
                        <div style="font-weight:500;">${BloodConnect.escapeHtml(r.hospitalName)}</div>
                        <div style="font-size:0.75rem; color:var(--text-secondary);">${BloodConnect.escapeHtml(r.city)}, ${BloodConnect.escapeHtml(r.state)}</div>
                    </td>
                    <td><span class="badge ${urgencyBadge}">${r.urgency}</span></td>
                    <td style="font-size:0.8125rem;">${r.requiredDate}</td>
                    <td><span class="badge ${statusBadge}">${r.requestStatus}</span></td>
                    <td>
                        <select class="form-select" style="font-size:0.75rem; padding:0.25rem 0.5rem; width:auto;" onchange="AdminApp.updateRequestStatus(${r.requestId}, this.value)">
                            <option value="PENDING" ${r.requestStatus === 'PENDING' ? 'selected' : ''}>PENDING</option>
                            <option value="ACCEPTED" ${r.requestStatus === 'ACCEPTED' ? 'selected' : ''}>ACCEPTED</option>
                            <option value="FULFILLED" ${r.requestStatus === 'FULFILLED' ? 'selected' : ''}>FULFILLED</option>
                            <option value="CANCELLED" ${r.requestStatus === 'CANCELLED' ? 'selected' : ''}>CANCELLED</option>
                        </select>
                    </td>
                </tr>
            `;
        });
        tbody.innerHTML = html;
    }

    function renderOverviewQueue(requests) {
        const queueContainer = document.getElementById("overview-recent-requests");
        if (!queueContainer) return;

        const openReqs = requests.filter(r => r.requestStatus === 'PENDING' || r.requestStatus === 'ACCEPTED');
        if (openReqs.length === 0) {
            queueContainer.innerHTML = `
                <div style="text-align:center; padding:1.5rem; color:var(--text-secondary);">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--success)" stroke-width="2" style="margin-bottom:0.35rem;">
                        <polyline points="20 6 9 17 4 12"></polyline>
                    </svg>
                    <p style="font-size:0.875rem; margin:0;">All emergency requirements are fulfilled.</p>
                </div>
            `;
            return;
        }

        let html = "";
        openReqs.slice(0, 3).forEach(r => {
            const badgeClass = r.urgency === 'CRITICAL' ? 'badge-critical' : 'badge-urgent';
            html += `
                <div style="display:flex; justify-content:space-between; align-items:center; padding:0.75rem 0; border-bottom:1px solid var(--border-light);">
                    <div>
                        <div style="display:flex; align-items:center; gap:0.5rem; margin-bottom:0.2rem;">
                            <span class="badge ${badgeClass}" style="font-size:0.6875rem;">${r.urgency}</span>
                            <span style="font-weight:700; font-size:0.875rem;">Patient: ${BloodConnect.escapeHtml(r.patientName)}</span>
                        </div>
                        <div style="font-size:0.75rem; color:var(--text-secondary);">
                            ${BloodConnect.escapeHtml(r.hospitalName)} (${BloodConnect.escapeHtml(r.city)}) • ${r.unitsRequired} Unit(s)
                        </div>
                    </div>
                    <div class="donor-blood-badge" style="font-size:0.8125rem;">${BloodConnect.escapeHtml(r.bloodGroup)}</div>
                </div>
            `;
        });
        queueContainer.innerHTML = html;
    }

    const updateRequestStatus = async function (requestId, newStatus) {
        try {
            const resp = await fetch(`${BloodConnect.contextPath}/api/admin/request/status`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ requestId, status: newStatus })
            });
            const res = await resp.json();
            if (resp.ok && res.success) {
                BloodConnect.showToast(res.message || `Request #${requestId} status updated.`, "success");
                await loadStats();
                await loadRequests();
            } else {
                BloodConnect.showToast(res.message || "Failed to update status.", "error");
                await loadRequests();
            }
        } catch (e) {
            BloodConnect.showToast("Network error updating request status.", "error");
        }
    };

    // ==========================================
    // 2. DONORS
    // ==========================================
    async function loadDonors() {
        const tbody = document.getElementById("admin-donors-tbody");
        if (!tbody) return;

        try {
            const resp = await fetch(`${BloodConnect.contextPath}/api/admin/donors`);
            const res = await resp.json();
            if (resp.ok && res.success && res.data) {
                cachedDonors = res.data;
                renderDonorsTable(cachedDonors);
            }
        } catch (e) {
            tbody.innerHTML = `<tr><td colspan="8" style="color:var(--danger); text-align:center;">Failed to load donors.</td></tr>`;
        }
    }

    function renderDonorsTable(donors) {
        const tbody = document.getElementById("admin-donors-tbody");
        if (!tbody) return;

        if (!donors || donors.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding:2rem; color:var(--text-secondary);">No donors found matching criteria.</td></tr>`;
            return;
        }

        let html = "";
        donors.forEach(d => {
            const availBadge = d.availabilityStatus === 'AVAILABLE' ? 'badge-available' : 'badge-unavailable';
            const statusBadge = d.accountStatus === 'ACTIVE' ? 'badge-available' : 'badge-unavailable';

            html += `
                <tr>
                    <td style="font-weight:700;">#${d.donorId}</td>
                    <td>
                        <div style="font-weight:600;">${BloodConnect.escapeHtml(d.fullName)}</div>
                        <div style="font-size:0.75rem; color:var(--text-secondary);">${BloodConnect.escapeHtml(d.phone)} • ${BloodConnect.escapeHtml(d.email)}</div>
                    </td>
                    <td><span class="donor-blood-badge" style="font-size:0.8125rem; padding:0.15rem 0.45rem;">${BloodConnect.escapeHtml(d.bloodGroup)}</span></td>
                    <td>${BloodConnect.escapeHtml(d.city)}, ${BloodConnect.escapeHtml(d.state)} (${d.pincode})</td>
                    <td><span class="badge ${availBadge}">${d.availabilityStatus}</span></td>
                    <td><span class="badge ${statusBadge}">${d.accountStatus}</span></td>
                    <td style="font-size:0.75rem; color:var(--text-secondary);">${d.lastUpdated ? d.lastUpdated.substring(0, 10) : 'Recent'}</td>
                    <td>
                        <button type="button" class="btn btn-secondary btn-sm" onclick="DonorApp.showContactModal('${BloodConnect.escapeHtml(d.fullName)}', '${BloodConnect.escapeHtml(d.phone)}', '${BloodConnect.escapeHtml(d.email)}', '${BloodConnect.escapeHtml(d.bloodGroup)}')">
                            View
                        </button>
                    </td>
                </tr>
            `;
        });
        tbody.innerHTML = html;
    }

    // ==========================================
    // 3. USERS
    // ==========================================
    async function loadUsers() {
        const tbody = document.getElementById("admin-users-tbody");
        if (!tbody) return;

        try {
            const resp = await fetch(`${BloodConnect.contextPath}/api/admin/users`);
            const res = await resp.json();
            if (resp.ok && res.success && res.data) {
                cachedUsers = res.data;
                renderUsersTable(cachedUsers);
            }
        } catch (e) {
            tbody.innerHTML = `<tr><td colspan="7" style="color:var(--danger); text-align:center;">Failed to load users.</td></tr>`;
        }
    }

    function renderUsersTable(users) {
        const tbody = document.getElementById("admin-users-tbody");
        if (!tbody) return;

        if (!users || users.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:2rem; color:var(--text-secondary);">No user records found.</td></tr>`;
            return;
        }

        let html = "";
        users.forEach(u => {
            const statusBadge = u.accountStatus === 'ACTIVE' ? 'badge-available' : 'badge-unavailable';
            const roleStyle = u.role === 'ADMIN' ? 'background:#F3E8FF;color:#7E22CE;'
                : u.role === 'DONOR' ? 'background:#FFE4E6;color:#BE123C;'
                : 'background:#E0F2FE;color:#0369A1;';

            const isProtectedAdmin = (u.email === 'admin@bloodconnect.org');

            html += `
                <tr>
                    <td style="font-weight:700;">#${u.userId}</td>
                    <td>
                        <div style="font-weight:600;">${BloodConnect.escapeHtml(u.fullName)}</div>
                        <div style="font-size:0.75rem; color:var(--text-secondary);">${BloodConnect.escapeHtml(u.email)}</div>
                    </td>
                    <td>${BloodConnect.escapeHtml(u.phone)}</td>
                    <td><span class="badge" style="${roleStyle}">${u.role}</span></td>
                    <td>${BloodConnect.escapeHtml(u.city || '--')}, ${BloodConnect.escapeHtml(u.state || '--')}</td>
                    <td><span class="badge ${statusBadge}">${u.accountStatus}</span></td>
                    <td>
                        ${!isProtectedAdmin ? `
                            <button type="button" class="btn btn-secondary btn-sm" onclick="AdminApp.toggleUserStatus(${u.userId}, '${u.accountStatus}')">
                                ${u.accountStatus === 'ACTIVE' ? 'Suspend Account' : 'Activate Account'}
                            </button>
                        ` : '<span style="font-size:0.75rem; color:var(--text-muted); font-weight:600;">System Protected</span>'}
                    </td>
                </tr>
            `;
        });
        tbody.innerHTML = html;
    }

    const toggleUserStatus = async function (userId, currentStatus) {
        const newStatus = (currentStatus === 'ACTIVE') ? 'SUSPENDED' : 'ACTIVE';
        try {
            const resp = await fetch(`${BloodConnect.contextPath}/api/admin/user/status`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ userId, status: newStatus })
            });
            const res = await resp.json();
            if (resp.ok && res.success) {
                BloodConnect.showToast(res.message || `User #${userId} status set to ${newStatus}.`, "success");
                await loadStats();
                await loadUsers();
            } else {
                BloodConnect.showToast(res.message || "Failed to update user status.", "error");
            }
        } catch (e) {
            BloodConnect.showToast("Network error updating user status.", "error");
        }
    };

    // ==========================================
    // 4. REPORTS & ANALYTICS
    // ==========================================
    function renderReports() {
        const bgGrid = document.getElementById("report-bg-grid");
        if (bgGrid) {
            const bloodGroups = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];
            const bgCounts = {};
            bloodGroups.forEach(bg => bgCounts[bg] = 0);

            cachedDonors.forEach(d => {
                if (bgCounts[d.bloodGroup] !== undefined) {
                    bgCounts[d.bloodGroup]++;
                }
            });

            let bgHtml = "";
            bloodGroups.forEach(bg => {
                bgHtml += `
                    <div class="blood-group-badge" style="cursor:default;">
                        <span class="bg-type">${bg}</span>
                        <span class="bg-label">${bgCounts[bg]} Donor${bgCounts[bg] === 1 ? '' : 's'}</span>
                    </div>
                `;
            });
            bgGrid.innerHTML = bgHtml;
        }

        // Platform Fulfillment Rate
        const rateEl = document.getElementById("report-fulfillment-rate");
        if (rateEl && cachedStats.totalRequests > 0) {
            const rate = Math.round((cachedStats.fulfilledRequests / cachedStats.totalRequests) * 100);
            rateEl.innerText = `${rate}%`;
        } else if (rateEl) {
            rateEl.innerText = "100%";
        }

        // Unique Cities Count
        const cityEl = document.getElementById("report-city-count");
        if (cityEl) {
            const uniqueCities = new Set(cachedDonors.map(d => (d.city || "").trim().toLowerCase()).filter(c => c.length > 0));
            cityEl.innerText = uniqueCities.size || "1";
        }
    }

    // ==========================================
    // 5. FILTER LISTENERS SETUP
    // ==========================================
    function setupFilterListeners() {
        // Donor filters
        const donorSearch = document.getElementById("donor-search-input");
        const donorBg = document.getElementById("donor-bg-filter");
        const donorAvail = document.getElementById("donor-avail-filter");
        const donorReset = document.getElementById("donor-filter-reset-btn");

        function applyDonorFilters() {
            const query = (donorSearch ? donorSearch.value.trim().toLowerCase() : "");
            const bg = (donorBg ? donorBg.value : "ALL");
            const avail = (donorAvail ? donorAvail.value : "ALL");

            const filtered = cachedDonors.filter(d => {
                const matchesQuery = !query ||
                    (d.fullName && d.fullName.toLowerCase().includes(query)) ||
                    (d.city && d.city.toLowerCase().includes(query)) ||
                    (d.phone && d.phone.includes(query));

                const matchesBg = (bg === "ALL") || (d.bloodGroup === bg);
                const matchesAvail = (avail === "ALL") || (d.availabilityStatus === avail);

                return matchesQuery && matchesBg && matchesAvail;
            });

            renderDonorsTable(filtered);
        }

        if (donorSearch) donorSearch.addEventListener("input", applyDonorFilters);
        if (donorBg) donorBg.addEventListener("change", applyDonorFilters);
        if (donorAvail) donorAvail.addEventListener("change", applyDonorFilters);
        if (donorReset) {
            donorReset.addEventListener("click", () => {
                if (donorSearch) donorSearch.value = "";
                if (donorBg) donorBg.value = "ALL";
                if (donorAvail) donorAvail.value = "ALL";
                renderDonorsTable(cachedDonors);
            });
        }

        // Blood Request filters
        const reqSearch = document.getElementById("req-search-input");
        const reqStatus = document.getElementById("admin-filter-status");
        const reqUrgency = document.getElementById("admin-filter-urgency");
        const reqReset = document.getElementById("req-filter-reset-btn");

        function applyRequestFilters() {
            const query = (reqSearch ? reqSearch.value.trim().toLowerCase() : "");
            const status = (reqStatus ? reqStatus.value : "ALL");
            const urgency = (reqUrgency ? reqUrgency.value : "ALL");

            const filtered = cachedRequests.filter(r => {
                const matchesQuery = !query ||
                    (r.patientName && r.patientName.toLowerCase().includes(query)) ||
                    (r.hospitalName && r.hospitalName.toLowerCase().includes(query)) ||
                    (r.city && r.city.toLowerCase().includes(query));

                const matchesStatus = (status === "ALL") || (r.requestStatus === status);
                const matchesUrgency = (urgency === "ALL") || (r.urgency === urgency);

                return matchesQuery && matchesStatus && matchesUrgency;
            });

            renderRequestsTable(filtered);
        }

        if (reqSearch) reqSearch.addEventListener("input", applyRequestFilters);
        if (reqStatus) reqStatus.addEventListener("change", applyRequestFilters);
        if (reqUrgency) reqUrgency.addEventListener("change", applyRequestFilters);
        if (reqReset) {
            reqReset.addEventListener("click", () => {
                if (reqSearch) reqSearch.value = "";
                if (reqStatus) reqStatus.value = "ALL";
                if (reqUrgency) reqUrgency.value = "ALL";
                renderRequestsTable(cachedRequests);
            });
        }

        // User filters
        const userSearch = document.getElementById("user-search-input");
        const userRole = document.getElementById("user-role-filter");
        const userReset = document.getElementById("user-filter-reset-btn");

        function applyUserFilters() {
            const query = (userSearch ? userSearch.value.trim().toLowerCase() : "");
            const role = (userRole ? userRole.value : "ALL");

            const filtered = cachedUsers.filter(u => {
                const matchesQuery = !query ||
                    (u.fullName && u.fullName.toLowerCase().includes(query)) ||
                    (u.email && u.email.toLowerCase().includes(query)) ||
                    (u.city && u.city.toLowerCase().includes(query));

                const matchesRole = (role === "ALL") || (u.role === role);

                return matchesQuery && matchesRole;
            });

            renderUsersTable(filtered);
        }

        if (userSearch) userSearch.addEventListener("input", applyUserFilters);
        if (userRole) userRole.addEventListener("change", applyUserFilters);
        if (userReset) {
            userReset.addEventListener("click", () => {
                if (userSearch) userSearch.value = "";
                if (userRole) userRole.value = "ALL";
                renderUsersTable(cachedUsers);
            });
        }
    }

    // Public API
    return {
        init: init,
        switchTab: switchTab,
        updateRequestStatus: updateRequestStatus,
        toggleUserStatus: toggleUserStatus
    };
})();
