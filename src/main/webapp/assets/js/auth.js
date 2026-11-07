/**
 * BloodConnect – Authentication & Registration Scripts
 */

const AuthApp = (function () {

    const initLogin = function () {
        const loginForm = document.getElementById("login-form");
        if (!loginForm) return;

        // Check URL query parameters for notifications
        const params = new URLSearchParams(window.location.search);
        if (params.get("error") === "unauthorized") {
            BloodConnect.showToast("Please log in to access this page.", "error");
        } else if (params.get("error") === "forbidden") {
            BloodConnect.showToast("Access denied: Admin credentials required.", "error");
        } else if (params.get("status") === "logged_out") {
            BloodConnect.showToast("You have been successfully logged out.", "success");
        }

        // Password visibility toggle
        const toggleBtn = document.getElementById("toggle-login-password");
        const passwordInput = document.getElementById("password");
        if (toggleBtn && passwordInput) {
            toggleBtn.addEventListener("click", () => {
                const type = passwordInput.getAttribute("type") === "password" ? "text" : "password";
                passwordInput.setAttribute("type", type);
            });
        }

        // Demo credentials quick-fill buttons
        const demoBtns = document.querySelectorAll("[data-demo-role]");
        demoBtns.forEach(btn => {
            btn.addEventListener("click", (e) => {
                e.preventDefault();
                const role = btn.getAttribute("data-demo-role");
                const emailInput = document.getElementById("email");
                if (role === "admin") {
                    emailInput.value = "admin@bloodconnect.org";
                    passwordInput.value = "Admin@123";
                } else if (role === "donor") {
                    emailInput.value = "rohit.sharma@example.com";
                    passwordInput.value = "Donor@123";
                } else if (role === "requester") {
                    emailInput.value = "rahul.mehta@example.com";
                    passwordInput.value = "Requester@123";
                }
                BloodConnect.showToast(`Filled demo credentials for ${role.toUpperCase()}`, "info", 2000);
            });
        });

        // Form submission
        loginForm.addEventListener("submit", async function (e) {
            e.preventDefault();

            const submitBtn = loginForm.querySelector("button[type='submit']");
            const originalBtnText = submitBtn.innerHTML;

            const email = document.getElementById("email").value.trim();
            const password = document.getElementById("password").value;

            if (!email || !password) {
                BloodConnect.showToast("Please enter both email and password.", "error");
                return;
            }

            try {
                submitBtn.disabled = true;
                submitBtn.innerHTML = `<span class="spinner" style="width:16px;height:16px;border-width:2px;margin-right:8px;"></span> Signing in...`;

                const resp = await fetch(`${BloodConnect.contextPath}/api/auth/login`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ email, password })
                });

                const res = await resp.json();
                if (resp.ok && res.success) {
                    BloodConnect.showToast("Login successful! Redirecting...", "success");
                    setTimeout(() => {
                        window.location.href = res.data.redirectUrl;
                    }, 800);
                } else {
                    BloodConnect.showToast(res.message || "Invalid email or password.", "error");
                }
            } catch (err) {
                BloodConnect.showToast("Connection error. Please verify server is running.", "error");
            } finally {
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalBtnText;
            }
        });
    };

    const initRegister = function () {
        const registerForm = document.getElementById("register-form");
        if (!registerForm) return;

        // Check if query parameter wants donor registration preselected
        const params = new URLSearchParams(window.location.search);
        if (params.get("type") === "donor") {
            const donorRadio = document.getElementById("role-donor");
            if (donorRadio) {
                donorRadio.checked = true;
                handleRoleSwitch("DONOR");
            }
        }

        // Role choice cards
        const roleRadios = document.querySelectorAll("input[name='role']");
        roleRadios.forEach(radio => {
            radio.addEventListener("change", function () {
                handleRoleSwitch(this.value);
            });
        });

        function handleRoleSwitch(role) {
            const donorFields = document.getElementById("donor-fields");
            const choiceCards = document.querySelectorAll(".choice-card");
            choiceCards.forEach(card => card.classList.remove("selected"));

            const targetCard = document.querySelector(`.choice-card[data-for='${role.toLowerCase()}']`);
            if (targetCard) targetCard.classList.add("selected");

            if (role === "DONOR") {
                if (donorFields) donorFields.style.display = "block";
            } else {
                if (donorFields) donorFields.style.display = "none";
            }
        }

        // Toggle Password visibility
        const toggleBtns = document.querySelectorAll(".toggle-pwd");
        toggleBtns.forEach(btn => {
            btn.addEventListener("click", function () {
                const targetId = this.getAttribute("data-target");
                const targetInput = document.getElementById(targetId);
                if (targetInput) {
                    const type = targetInput.getAttribute("type") === "password" ? "text" : "password";
                    targetInput.setAttribute("type", type);
                }
            });
        });

        // Form Submit
        registerForm.addEventListener("submit", async function (e) {
            e.preventDefault();

            // Clear previous errors
            document.querySelectorAll(".field-error").forEach(el => el.classList.remove("show"));

            const fullName = document.getElementById("fullName").value.trim();
            const email = document.getElementById("email").value.trim();
            const phone = document.getElementById("phone").value.trim();
            const password = document.getElementById("password").value;
            const confirmPassword = document.getElementById("confirmPassword").value;
            const city = document.getElementById("city").value.trim();
            const state = document.getElementById("state").value.trim();
            const pincode = document.getElementById("pincode").value.trim();
            const address = document.getElementById("address").value.trim();

            const role = document.querySelector("input[name='role']:checked").value;

            // Client Validation
            let isValid = true;
            if (fullName.length < 2) {
                showFieldError("fullName-error", "Full name must be at least 2 characters.");
                isValid = false;
            }
            if (!/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email)) {
                showFieldError("email-error", "Please enter a valid email address.");
                isValid = false;
            }
            if (!/^[0-9]{10}$/.test(phone)) {
                showFieldError("phone-error", "Please enter a valid 10-digit mobile number.");
                isValid = false;
            }
            if (password.length < 6) {
                showFieldError("password-error", "Password must be at least 6 characters.");
                isValid = false;
            }
            if (password !== confirmPassword) {
                showFieldError("confirmPassword-error", "Passwords do not match.");
                isValid = false;
            }
            if (!city) {
                showFieldError("city-error", "City is required.");
                isValid = false;
            }
            if (!state) {
                showFieldError("state-error", "State is required.");
                isValid = false;
            }
            if (!pincode) {
                showFieldError("pincode-error", "PIN code is required.");
                isValid = false;
            }

            const payload = {
                fullName, email, phone, password, confirmPassword,
                role, city, state, pincode, address
            };

            if (role === "DONOR") {
                const bloodGroup = document.getElementById("bloodGroup").value;
                if (!bloodGroup) {
                    showFieldError("bloodGroup-error", "Please select your blood group.");
                    isValid = false;
                }
                const availabilityStatus = document.getElementById("availabilityStatus").value;
                const isDiscoverable = document.getElementById("isDiscoverable").checked;

                payload.bloodGroup = bloodGroup;
                payload.availabilityStatus = availabilityStatus;
                payload.isDiscoverable = isDiscoverable;
            }

            if (!isValid) return;

            const submitBtn = registerForm.querySelector("button[type='submit']");
            const originalBtnText = submitBtn.innerHTML;

            try {
                submitBtn.disabled = true;
                submitBtn.innerHTML = `<span class="spinner" style="width:16px;height:16px;border-width:2px;margin-right:8px;"></span> Registering...`;

                const resp = await fetch(`${BloodConnect.contextPath}/api/auth/register`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(payload)
                });

                const res = await resp.json();
                if (resp.ok && res.success) {
                    BloodConnect.showToast("Account created successfully! Welcome to BloodConnect.", "success");
                    setTimeout(() => {
                        window.location.href = res.data.redirectUrl;
                    }, 1000);
                } else {
                    BloodConnect.showToast(res.message || "Registration failed.", "error");
                }
            } catch (err) {
                BloodConnect.showToast("Connection error. Please try again.", "error");
            } finally {
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalBtnText;
            }
        });

        function showFieldError(errorId, msg) {
            const el = document.getElementById(errorId);
            if (el) {
                el.innerText = msg;
                el.classList.add("show");
            }
        }
    };

    return {
        initLogin: initLogin,
        initRegister: initRegister
    };
})();
