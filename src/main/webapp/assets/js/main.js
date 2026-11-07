/**
 * BloodConnect – Core Client Application Logic & UI Utilities
 */

const BloodConnect = (function () {
    // Detect context path dynamically (e.g. "/BloodConnect" or "")
    const getContextPath = function () {
        const path = window.location.pathname;
        if (path.startsWith("/BloodConnect")) {
            return "/BloodConnect";
        }
        return "";
    };

    const CONTEXT_PATH = getContextPath();

    /**
     * Toast notification utility
     */
    const showToast = function (message, type = "info", duration = 4000) {
        let container = document.getElementById("toast-container");
        if (!container) {
            container = document.createElement("div");
            container.id = "toast-container";
            container.className = "toast-container";
            document.body.appendChild(container);
        }

        const toast = document.createElement("div");
        toast.className = `toast toast-${type}`;
        
        const iconSvg = type === "success" 
            ? `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>`
            : type === "error"
            ? `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>`
            : `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>`;

        toast.innerHTML = `<span>${iconSvg}</span><div>${message}</div>`;
        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = "0";
            toast.style.transform = "translateX(50px)";
            toast.style.transition = "all 0.3s ease";
            setTimeout(() => toast.remove(), 300);
        }, duration);
    };

    /**
     * Checks currently authenticated session from backend.
     */
    const checkAuth = async function () {
        try {
            const resp = await fetch(`${CONTEXT_PATH}/api/auth/me`);
            if (resp.ok) {
                const res = await resp.json();
                if (res.success && res.data) {
                    return res.data;
                }
            }
        } catch (e) {
            console.error("Auth check failed:", e);
        }
        return null;
    };

    /**
     * Updates navbar buttons based on login status.
     */
    const updateNavbar = async function () {
        const user = await checkAuth();
        const navActions = document.getElementById("nav-actions");
        if (!navActions) return;

        if (user) {
            let dashboardUrl = `${CONTEXT_PATH}/find-donors.html`;
            if (user.role === "ADMIN") {
                dashboardUrl = `${CONTEXT_PATH}/admin-dashboard.html`;
            } else if (user.role === "DONOR") {
                dashboardUrl = `${CONTEXT_PATH}/donor-dashboard.html`;
            } else {
                dashboardUrl = `${CONTEXT_PATH}/my-requests.html`;
            }

            navActions.innerHTML = `
                <div class="user-menu">
                    <button class="user-btn" id="user-menu-btn" type="button">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
                        <span>${escapeHtml(user.fullName)}</span>
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"></polyline></svg>
                    </button>
                    <div class="user-dropdown" id="user-dropdown-menu">
                        <a href="${dashboardUrl}">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
                            Dashboard
                        </a>
                        ${user.role === 'DONOR' ? `
                            <a href="${CONTEXT_PATH}/donor-dashboard.html">
                                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"></path></svg>
                                My Donor Profile
                            </a>
                        ` : ''}
                        ${user.role === 'REQUESTER' ? `
                            <a href="${CONTEXT_PATH}/my-requests.html">
                                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
                                My Blood Requests
                            </a>
                        ` : ''}
                        <div class="divider"></div>
                        <button type="button" id="logout-btn">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
                            Logout
                        </button>
                    </div>
                </div>
            `;

            // Setup menu click
            const menuBtn = document.getElementById("user-menu-btn");
            const menuDropdown = document.getElementById("user-dropdown-menu");
            if (menuBtn && menuDropdown) {
                menuBtn.addEventListener("click", (e) => {
                    e.stopPropagation();
                    menuDropdown.classList.toggle("show");
                });
                document.addEventListener("click", () => menuDropdown.classList.remove("show"));
            }

            // Setup logout click
            const logoutBtn = document.getElementById("logout-btn");
            if (logoutBtn) {
                logoutBtn.addEventListener("click", async () => {
                    await logout();
                });
            }
        } else {
            navActions.innerHTML = `
                <a href="${CONTEXT_PATH}/login.html" class="btn btn-secondary btn-sm">Login</a>
                <a href="${CONTEXT_PATH}/register.html?type=donor" class="btn btn-primary btn-sm">Become a Donor</a>
            `;
        }
    };

    /**
     * Log out user
     */
    const logout = async function () {
        try {
            await fetch(`${CONTEXT_PATH}/api/auth/logout`, { method: "POST" });
            window.location.href = `${CONTEXT_PATH}/login.html?status=logged_out`;
        } catch (e) {
            window.location.href = `${CONTEXT_PATH}/login.html`;
        }
    };

    /**
     * Escape HTML string
     */
    const escapeHtml = function (str) {
        if (!str) return "";
        return str
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    };

    /**
     * Initialize Mobile menu toggle
     */
    const initMobileMenu = function () {
        const toggleBtn = document.getElementById("mobile-menu-toggle");
        const navLinks = document.getElementById("nav-links");
        if (toggleBtn && navLinks) {
            toggleBtn.addEventListener("click", () => {
                navLinks.classList.toggle("active");
            });
        }
    };

    /**
     * Modal controls
     */
    const openModal = function (modalId) {
        const modal = document.getElementById(modalId);
        if (modal) modal.classList.add("show");
    };

    const closeModal = function (modalId) {
        const modal = document.getElementById(modalId);
        if (modal) modal.classList.remove("show");
    };

    // Public API
    return {
        contextPath: CONTEXT_PATH,
        showToast: showToast,
        checkAuth: checkAuth,
        updateNavbar: updateNavbar,
        logout: logout,
        escapeHtml: escapeHtml,
        initMobileMenu: initMobileMenu,
        openModal: openModal,
        closeModal: closeModal
    };
})();

document.addEventListener("DOMContentLoaded", function () {
    BloodConnect.initMobileMenu();
    BloodConnect.updateNavbar();
});
