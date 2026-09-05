/**
 * PostOffice frontend — session auth, booking, tracking, ledger, simulated mail.
 */
const API_BASE = "https://postoffice-ns9h.onrender.com";

let state = {
    activeUser: null,
    mockEmailsCount: 0
};

const DOM = {
    authScreen: document.getElementById("auth-screen"),
    appContainer: document.getElementById("app-container"),
    loginForm: document.getElementById("login-form"),
    registerForm: document.getElementById("register-form"),
    loginEmail: document.getElementById("login-email"),
    loginPassword: document.getElementById("login-password"),
    registerName: document.getElementById("register-name"),
    registerEmail: document.getElementById("register-email"),
    registerPassword: document.getElementById("register-password"),
    registerConfirm: document.getElementById("register-confirm"),
    activeUserName: document.getElementById("active-user-name"),
    logoutBtn: document.getElementById("logout-btn"),
    walletBalanceDisplay: document.getElementById("wallet-balance-display"),
    senderDisplay: document.getElementById("sender-display"),
    receiverSelect: document.getElementById("receiver-select"),
    bookingForm: document.getElementById("booking-form"),
    rawMessage: document.getElementById("raw-message"),
    letterFile: document.getElementById("letter-file"),
    letterFileNameLabel: document.getElementById("letter-file-name-label"),
    attachmentFile: document.getElementById("attachment-file"),
    attachmentFileNameLabel: document.getElementById("attachment-file-name-label"),
    breakdownTotal: document.getElementById("breakdown-total"),
    breakdownDelivery: document.getElementById("breakdown-delivery"),
    breakdownStamp: document.getElementById("breakdown-stamp"),
    breakdownProcessing: document.getElementById("breakdown-processing"),
    bookingSubmitBtn: document.getElementById("booking-submit-btn"),
    tabButtons: document.querySelectorAll(".tab-btn"),
    tabContents: document.querySelectorAll(".tab-content"),
    sentPlaceholder: document.getElementById("sent-placeholder"),
    sentLettersList: document.getElementById("sent-letters-list"),
    receivedPlaceholder: document.getElementById("received-placeholder"),
    receivedLettersList: document.getElementById("received-letters-list"),
    ledgerEntries: document.getElementById("ledger-entries"),
    refreshLedgerBtn: document.getElementById("refresh-ledger-btn"),
    upgradeModal: document.getElementById("upgrade-modal"),
    upgradeLetterId: document.getElementById("upgrade-letter-id"),
    upgradeTrackingId: document.getElementById("upgrade-tracking-id"),
    upgradeCardNormal: document.getElementById("upgrade-card-normal"),
    upgradeCardSpeed: document.getElementById("upgrade-card-speed"),
    upgradeCardSuperfast: document.getElementById("upgrade-card-superfast"),
    calcNewCharge: document.getElementById("calc-new-charge"),
    calcNetSurcharge: document.getElementById("calc-net-surcharge"),
    cancelUpgradeBtn: document.getElementById("cancel-upgrade-btn"),
    confirmUpgradeBtn: document.getElementById("confirm-upgrade-btn"),
    closeModalBtn: document.getElementById("close-modal-btn"),
    consoleDrawer: document.getElementById("console-drawer"),
    consoleTrigger: document.getElementById("console-trigger"),
    consoleBadge: document.getElementById("console-badge"),
    consoleLogs: document.getElementById("console-logs"),
    clearConsoleBtn: document.getElementById("clear-console-btn"),
    closeConsoleBtn: document.getElementById("close-console-btn"),
    toastContainer: document.getElementById("toast-container")
};

function api(path, options = {}) {
    return fetch(`${API_BASE}${path}`, {
        credentials: "same-origin",
        ...options
    });
}

document.addEventListener("DOMContentLoaded", async () => {
    setupEventListeners();
    await restoreSession();
    setInterval(() => {
        if (state.activeUser) pollStatusUpdates();
    }, 5000);
});

function setupEventListeners() {
    document.querySelectorAll(".auth-tab").forEach((tab) => {
        tab.addEventListener("click", () => {
            document.querySelectorAll(".auth-tab").forEach((t) => t.classList.remove("active"));
            tab.classList.add("active");
            const isLogin = tab.dataset.authTab === "login";
            DOM.loginForm.style.display = isLogin ? "flex" : "none";
            DOM.registerForm.style.display = isLogin ? "none" : "flex";
        });
    });

    DOM.loginForm.addEventListener("submit", submitLogin);
    DOM.registerForm.addEventListener("submit", submitRegister);
    DOM.logoutBtn.addEventListener("click", logout);

    DOM.letterFile.addEventListener("change", (e) => {
        DOM.letterFileNameLabel.innerText = e.target.files[0] ? e.target.files[0].name : "No file selected";
    });
    DOM.attachmentFile.addEventListener("change", (e) => {
        DOM.attachmentFileNameLabel.innerText = e.target.files[0] ? e.target.files[0].name : "No file selected";
    });

    DOM.bookingForm.addEventListener("submit", bookLetter);

    document.querySelectorAll('input[name="shipping-service"]').forEach((radio) => {
        radio.addEventListener("change", updateBookingBreakdown);
    });

    DOM.tabButtons.forEach((btn) => {
        btn.addEventListener("click", () => {
            DOM.tabButtons.forEach((b) => b.classList.remove("active"));
            DOM.tabContents.forEach((c) => c.classList.remove("active"));
            btn.classList.add("active");
            document.getElementById(btn.getAttribute("data-tab")).classList.add("active");
        });
    });

    DOM.refreshLedgerBtn.addEventListener("click", loadLedger);
    DOM.closeModalBtn.addEventListener("click", hideUpgradeModal);
    DOM.cancelUpgradeBtn.addEventListener("click", hideUpgradeModal);
    DOM.confirmUpgradeBtn.addEventListener("click", submitUpgrade);
    document.querySelectorAll('input[name="upgrade-service"]').forEach((radio) => {
        radio.addEventListener("change", recalculateSurcharge);
    });

    DOM.consoleTrigger.addEventListener("click", () => {
        DOM.consoleDrawer.classList.add("expanded");
        DOM.consoleBadge.innerText = "0";
        state.mockEmailsCount = 0;
    });
    DOM.closeConsoleBtn.addEventListener("click", () => DOM.consoleDrawer.classList.remove("expanded"));
    DOM.clearConsoleBtn.addEventListener("click", clearMockEmails);
}

async function restoreSession() {
    try {
        const response = await api("/api/auth/me");
        if (!response.ok) {
            showAuth();
            return;
        }
        enterApp(await response.json());
    } catch (e) {
        showAuth();
    }
}

function showAuth() {
    state.activeUser = null;
    DOM.authScreen.hidden = false;
    DOM.appContainer.hidden = true;
}

function enterApp(user) {
    state.activeUser = user;
    DOM.authScreen.hidden = true;
    DOM.appContainer.hidden = false;
    DOM.activeUserName.textContent = user.name;
    DOM.senderDisplay.value = user.name;
    updateWalletDisplay(user.balance);
    refreshAllData();
}

async function submitLogin(e) {
    e.preventDefault();
    const btn = document.getElementById("login-submit-btn");
    const originalText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Authenticating...';
    
    try {
        const response = await api("/api/auth/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                email: DOM.loginEmail.value,
                password: DOM.loginPassword.value
            })
        });
        const data = await response.json();
        if (!response.ok) throw new Error(data.error || "Login failed");
        enterApp(data);
        showToast(`Welcome back, ${data.name}`, "success");
    } catch (err) {
        showToast(err.message, "error");
        btn.disabled = false;
        btn.innerHTML = originalText;
    }
}

async function submitRegister(e) {
    e.preventDefault();
    const btn = document.getElementById("register-submit-btn");
    const originalText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Creating Account...';
    
    try {
        const response = await api("/api/auth/register", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                name: DOM.registerName.value,
                email: DOM.registerEmail.value,
                password: DOM.registerPassword.value,
                confirmPassword: DOM.registerConfirm.value
            })
        });
        const data = await response.json();
        if (!response.ok) throw new Error(data.error || "Registration failed");
        enterApp(data);
        showToast("Account created. You are signed in.", "success");
    } catch (err) {
        showToast(err.message, "error");
        btn.disabled = false;
        btn.innerHTML = originalText;
    }
}

async function logout() {
    await api("/api/auth/logout", { method: "POST" });
    showAuth();
    showToast("Signed out", "success");
}

async function loadReceivers() {
    const response = await api("/api/users");
    if (!response.ok) return;
    const users = await response.json();
    DOM.receiverSelect.innerHTML = '<option value="">Select a receiver</option>';
    users.forEach((user) => {
        const option = document.createElement("option");
        option.value = user.id;
        option.textContent = `${user.name} (${user.email})`;
        DOM.receiverSelect.appendChild(option);
    });
}

async function loadWalletBalance() {
    const response = await api("/api/users/me");
    if (!response.ok) return;
    const user = await response.json();
    state.activeUser = user;
    DOM.activeUserName.textContent = user.name;
    DOM.senderDisplay.value = user.name;
    updateWalletDisplay(user.balance);
}

function updateWalletDisplay(balance) {
    DOM.walletBalanceDisplay.textContent = parseFloat(balance).toFixed(2) + " COINS";
}

function updateBookingBreakdown() {
    const service = document.querySelector('input[name="shipping-service"]:checked').value;
    let total = 5.00, delivery = 2.00, stamp = 1.00, processing = 2.00;
    if (service === "Speed") {
        total = 20.00; delivery = 10.00; stamp = 2.00; processing = 8.00;
    } else if (service === "Superfast") {
        total = 50.00; delivery = 30.00; stamp = 5.00; processing = 15.00;
    }
    DOM.breakdownTotal.innerText = total.toFixed(2) + " coins";
    DOM.breakdownDelivery.innerText = delivery.toFixed(2) + " coin" + (delivery > 1 ? "s" : "");
    DOM.breakdownStamp.innerText = stamp.toFixed(2) + " coin" + (stamp > 1 ? "s" : "");
    DOM.breakdownProcessing.innerText = processing.toFixed(2) + " coin" + (processing > 1 ? "s" : "");
}

async function bookLetter(e) {
    e.preventDefault();
    const receiverId = DOM.receiverSelect.value;
    if (!receiverId) {
        showToast("Please choose a receiver", "error");
        return;
    }
    const letterFile = DOM.letterFile.files[0];
    if (!letterFile) {
        showToast("Please upload a letter document", "error");
        return;
    }

    DOM.bookingSubmitBtn.disabled = true;
    DOM.bookingSubmitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Sending...';

    const formData = new FormData();
    formData.append("receiverId", receiverId);
    formData.append("message", DOM.rawMessage.value);
    formData.append("letterFile", letterFile);
    if (DOM.attachmentFile.files[0]) {
        formData.append("attachmentFile", DOM.attachmentFile.files[0]);
    }
    formData.append("serviceName", document.querySelector('input[name="shipping-service"]:checked').value);

    try {
        const response = await api("/api/letters", { method: "POST", body: formData });
        const data = await response.json();
        if (!response.ok) throw new Error(data.error || "Booking failed");
        showToast("Letter booked and in transit!", "success");
        DOM.bookingForm.reset();
        document.querySelector('input[name="shipping-service"][value="Normal"]').checked = true;
        DOM.letterFileNameLabel.innerText = "No file selected";
        DOM.attachmentFileNameLabel.innerText = "No file selected";
        updateBookingBreakdown();
        await refreshAllData();
    } catch (err) {
        showToast(err.message, "error");
    } finally {
        DOM.bookingSubmitBtn.disabled = false;
        DOM.bookingSubmitBtn.innerHTML = '<i class="fa-solid fa-paper-plane"></i> Send Letter';
    }
}

async function loadLedger() {
    const response = await api("/api/users/me/ledger");
    if (!response.ok) return;
    const rows = await response.json();
    DOM.ledgerEntries.innerHTML = "";
    if (rows.length === 0) {
        DOM.ledgerEntries.innerHTML = `<tr><td colspan="4" style="text-align:center;color:var(--text-muted);">No transactions recorded.</td></tr>`;
        return;
    }
    rows.forEach((t) => {
        const row = document.createElement("tr");
        const amountClass = t.amount > 0 ? "refund" : (t.type === "UPGRADE" ? "surcharge" : "deduction");
        const amountSign = t.amount > 0 ? "+" : "";
        row.innerHTML = `
            <td>${new Date(t.createdAt).toLocaleString()}</td>
            <td><span class="badge ${String(t.type).toLowerCase()}">${t.type}</span></td>
            <td>${t.description}</td>
            <td class="amount-col ${amountClass}">${amountSign}${parseFloat(t.amount).toFixed(2)}</td>
        `;
        DOM.ledgerEntries.appendChild(row);
    });
}

async function refreshAllData() {
    await Promise.all([loadWalletBalance(), loadReceivers(), loadLedger(), loadLetters(), loadMockEmails()]);
}

async function pollStatusUpdates() {
    await Promise.all([loadLetters(), loadMockEmails(), loadWalletBalance()]);
}

async function loadLetters() {
    const [sentRes, recvRes] = await Promise.all([
        api("/api/letters/sent"),
        api("/api/letters/received")
    ]);
    if (sentRes.ok) renderSentLetters(await sentRes.json());
    if (recvRes.ok) renderReceivedLetters(await recvRes.json());
}

function displayStatus(letter) {
    if (letter.isRead) return "READ";
    if (letter.status === "arrived") return "DELIVERED";
    if (letter.status === "failed") return "FAILED";
    return "IN TRANSIT";
}

function statusClass(letter) {
    return displayStatus(letter).toLowerCase().replace(" ", "-");
}

function isTransit(letter) {
    return letter.status === "start" || letter.status === "inprocess";
}

function renderSentLetters(letters) {
    if (!letters.length) {
        DOM.sentPlaceholder.style.display = "flex";
        DOM.sentLettersList.style.display = "none";
        return;
    }
    DOM.sentPlaceholder.style.display = "none";
    DOM.sentLettersList.style.display = "flex";
    DOM.sentLettersList.innerHTML = "";

    letters.forEach((letter) => {
        const card = document.createElement("div");
        card.className = "letter-card";
        const serviceName = letter.service?.name || "";
        card.innerHTML = `
            <div class="card-top">
                <div class="meta-info">
                    <h4><i class="fa-solid fa-barcode"></i> ${letter.trackingId}</h4>
                    <span class="date">Sent: ${new Date(letter.postDate).toLocaleString()}</span>
                </div>
                <div class="status-badges">
                    <span class="status-badge ${statusClass(letter)}">${displayStatus(letter)}</span>
                    <span class="status-badge type-badge">${serviceName}</span>
                </div>
            </div>
            <div class="card-details">
                <div class="detail-item"><span class="lbl">To:</span><span class="val">${letter.receiver.name}</span></div>
                <div class="detail-item"><span class="lbl">Est. Arrival:</span><span class="val">${new Date(letter.receivingDate).toLocaleString()}</span></div>
                <div class="detail-item"><span class="lbl">Now at:</span><span class="val">${letter.currNodeAddress}</span></div>
                ${letter.message ? `<div class="card-msg">"${letter.message}"</div>` : ""}
            </div>
            <div class="route-stepper">
                <div class="stepper-header">
                    <span>Route Track: ${letter.sender.name} &rarr; ${letter.receiver.name}</span>
                </div>
                <div class="stepper-nodes-wrapper">
                    <div class="stepper-nodes" id="nodes-container-${letter.id}"></div>
                </div>
            </div>
            <div class="stepper-actions">
                ${isTransit(letter) ? `
                    <button class="glass-btn primary-btn" onclick="openUpgradeModal(${letter.id}, '${letter.trackingId}', '${serviceName}')">
                        <i class="fa-solid fa-circle-chevron-up"></i> Modify Service
                    </button>
                    <button class="glass-btn" onclick="triggerSimulateDelivery(${letter.id})">
                        <i class="fa-solid fa-forward-fast"></i> Simulate Delivery
                    </button>
                ` : ""}
            </div>
        `;
        DOM.sentLettersList.appendChild(card);
        drawStepper(letter);
    });
}

function drawStepper(letter) {
    const container = document.getElementById(`nodes-container-${letter.id}`);
    if (!container) return;
    const nodes = letter.routeNodes || [];
    const currentIndex = Math.max(0, nodes.indexOf(letter.currNodeAddress));
    const arrived = letter.status === "arrived" || letter.isRead;
    container.innerHTML = `<div class="stepper-progress" id="progress-${letter.id}"></div>`;
    nodes.forEach((name, idx) => {
        const nodeDiv = document.createElement("div");
        nodeDiv.className = "step-node";
        if (arrived || idx < currentIndex) nodeDiv.classList.add("completed");
        else if (idx === currentIndex) nodeDiv.classList.add("active");
        nodeDiv.innerHTML = `<div class="step-dot"></div><span class="node-name">${name}</span>`;
        container.appendChild(nodeDiv);
    });
    const percent = arrived ? 100 : (nodes.length <= 1 ? 0 : (currentIndex / (nodes.length - 1)) * 100);
    setTimeout(() => {
        const bar = document.getElementById(`progress-${letter.id}`);
        if (bar) bar.style.width = `${percent}%`;
    }, 50);
}

function renderReceivedLetters(letters) {
    if (!letters.length) {
        DOM.receivedPlaceholder.style.display = "flex";
        DOM.receivedLettersList.style.display = "none";
        return;
    }

    // Check if any OTP input is currently focused
    const focusedElement = document.activeElement;
    if (focusedElement && focusedElement.classList.contains('otp-field')) {
        return;
    }

    DOM.receivedPlaceholder.style.display = "none";
    DOM.receivedLettersList.style.display = "flex";
    DOM.receivedLettersList.innerHTML = "";

    letters.forEach((letter) => {
        const card = document.createElement("div");
        card.className = "letter-card";
        let stateSectionHtml = "";
        if (isTransit(letter)) {
            stateSectionHtml = `<div class="warning-alert"><i class="fa-solid fa-spinner fa-spin"></i> Arrived: In Transit — currently at ${letter.currNodeAddress}</div>`;
        } else if (letter.status === "arrived" && !letter.isRead) {
            stateSectionHtml = `
                <div class="otp-gate">
                    <span class="otp-title"><i class="fa-solid fa-lock"></i> Encrypted Letter - Enter OTP to Unlock</span>
                    <div class="otp-inputs">
                        <input type="text" maxlength="6" placeholder="Enter 6-digit OTP" class="otp-field" id="otp-input-${letter.id}">
                        <button class="submit-btn" onclick="submitVerifyOtp(${letter.id})">
                            <i class="fa-solid fa-unlock"></i> Unlock & Download
                        </button>
                    </div>
                </div>`;
        } else if (letter.isRead) {
            stateSectionHtml = `
                <div class="download-links">
                    <a href="${API_BASE}/api/letters/download/${letter.letterImage}" class="dl-link">
                        <i class="fa-solid fa-file-pdf"></i> Download Letter Document
                    </a>
                    ${letter.attachmentImage ? `
                        <a href="${API_BASE}/api/letters/download/${letter.attachmentImage}" class="dl-link">
                            <i class="fa-solid fa-image"></i> Download Image Attachment
                        </a>` : ""}
                </div>`;
        }

        card.innerHTML = `
            <div class="card-top">
                <div class="meta-info">
                    <h4><i class="fa-solid fa-barcode"></i> ${letter.trackingId}</h4>
                    <span class="date">Booking: ${new Date(letter.postDate).toLocaleString()}</span>
                </div>
                <div class="status-badges">
                    <span class="status-badge ${statusClass(letter)}">${displayStatus(letter)}</span>
                    <span class="status-badge type-badge">${letter.service?.name || ""}</span>
                </div>
            </div>
            <div class="card-details">
                <div class="detail-item"><span class="lbl">From:</span><span class="val">${letter.sender.name}</span></div>
                <div class="detail-item"><span class="lbl">Location:</span><span class="val">${letter.currNodeAddress}</span></div>
                ${letter.message ? `<div class="card-msg">"${letter.message}"</div>` : ""}
            </div>
            ${stateSectionHtml}
        `;
        DOM.receivedLettersList.appendChild(card);
    });
}

async function triggerSimulateDelivery(letterId) {
    try {
        const response = await api(`/api/letters/${letterId}/simulate-delivery`, { method: "POST" });
        const data = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(data.error || "Simulation failed");
        showToast("Delivery simulation completed", "success");
        await refreshAllData();
    } catch (e) {
        showToast(e.message, "error");
    }
}

function openUpgradeModal(letterId, trackingId, currentService) {
    DOM.upgradeLetterId.value = letterId;
    DOM.upgradeTrackingId.innerText = trackingId;
    [DOM.upgradeCardNormal, DOM.upgradeCardSpeed, DOM.upgradeCardSuperfast].forEach((card) => {
        card.classList.remove("disabled");
    });
    document.querySelectorAll('input[name="upgrade-service"]').forEach((radio) => {
        radio.checked = false;
        radio.disabled = false;
    });

    const current = (currentService || "").toLowerCase();
    if (current === "normal") {
        DOM.upgradeCardNormal.classList.add("disabled");
        document.querySelector('input[name="upgrade-service"][value="Normal"]').disabled = true;
        document.querySelector('input[name="upgrade-service"][value="Speed"]').checked = true;
    } else if (current === "speed") {
        DOM.upgradeCardSpeed.classList.add("disabled");
        document.querySelector('input[name="upgrade-service"][value="Speed"]').disabled = true;
        document.querySelector('input[name="upgrade-service"][value="Superfast"]').checked = true;
    } else if (current === "superfast") {
        showToast("This package is already at maximum service speed", "warning");
        return;
    }
    recalculateSurcharge();
    DOM.upgradeModal.classList.add("active");
}

function hideUpgradeModal() {
    DOM.upgradeModal.classList.remove("active");
}

function serviceCharge(name) {
    if (name === "Speed") return 20;
    if (name === "Superfast") return 50;
    return 5;
}

function recalculateSurcharge() {
    const newService = document.querySelector('input[name="upgrade-service"]:checked')?.value;
    if (!newService) return;
    const newCost = serviceCharge(newService);
    DOM.calcNewCharge.innerText = newCost.toFixed(2) + " coins";
    DOM.calcNetSurcharge.innerText = (newCost + 5).toFixed(2) + " coins";
}

async function submitUpgrade() {
    const letterId = DOM.upgradeLetterId.value;
    const newService = document.querySelector('input[name="upgrade-service"]:checked')?.value;
    if (!newService) {
        showToast("Select a service to change", "error");
        return;
    }
    try {
        const response = await api(`/api/letters/${letterId}/change-service?newServiceName=${encodeURIComponent(newService)}`, {
            method: "POST"
        });
        const data = await response.json();
        if (!response.ok) throw new Error(data.error || "Upgrade failed");
        showToast("Shipping service updated", "success");
        hideUpgradeModal();
        await refreshAllData();
    } catch (e) {
        showToast(e.message, "error");
    }
}

async function submitVerifyOtp(letterId) {
    const input = document.getElementById(`otp-input-${letterId}`);
    const otp = input ? input.value.trim() : "";
    if (otp.length !== 6 || isNaN(otp)) {
        showToast("Please enter a valid 6-digit verification code", "error");
        return;
    }
    try {
        const response = await api(`/api/letters/${letterId}/verify-otp?otp=${encodeURIComponent(otp)}`, { method: "POST" });
        const letter = await response.json();
        if (!response.ok) throw new Error(letter.error || "Verification failed");
        showToast("OTP verified. Download starting...", "success");
        await refreshAllData();
        triggerDownload(letter.letterImage);
        if (letter.attachmentImage) {
            setTimeout(() => triggerDownload(letter.attachmentImage), 800);
        }
    } catch (e) {
        showToast(e.message, "error");
    }
}

function triggerDownload(filename) {
    const url = `${API_BASE}/api/letters/download/${filename}`;
    const iframe = document.createElement("iframe");
    iframe.style.display = "none";
    iframe.src = url;
    document.body.appendChild(iframe);
    setTimeout(() => document.body.removeChild(iframe), 2000);
}

async function loadMockEmails() {
    try {
        const response = await api("/api/mock-emails");
        if (!response.ok) return;
        const emails = await response.json();
        DOM.consoleLogs.innerHTML = "";
        if (emails.length === 0) {
            DOM.consoleLogs.innerHTML = `
                <div class="log-entry system">
                    <span class="log-time">[System]</span>
                    <span class="log-msg">No logs yet. Deliver a letter to see notifications.</span>
                </div>`;
            return;
        }
        emails.forEach((email) => {
            const entry = document.createElement("div");
            entry.className = "log-entry email";
            let bodyText = email.body.replace(/OTP: (\d{6})/,
                'OTP: <strong style="color:var(--warning);background:rgba(245,158,11,0.1);padding:2px 6px;border-radius:4px;">$1</strong>');
            entry.innerHTML = `
                <span class="log-time">${new Date(email.timestamp).toLocaleString()}</span>
                <span class="log-recipient">To: ${email.to}</span>
                <span class="log-subject">Sub: ${email.subject}</span>
                <span class="log-msg">${bodyText}</span>`;
            DOM.consoleLogs.appendChild(entry);
        });
        DOM.consoleLogs.scrollTop = DOM.consoleLogs.scrollHeight;
        if (emails.length > state.mockEmailsCount && !DOM.consoleDrawer.classList.contains("expanded")) {
            DOM.consoleBadge.innerText = String(emails.length - state.mockEmailsCount);
            DOM.consoleBadge.style.display = "block";
        }
        state.mockEmailsCount = emails.length;
    } catch (err) {
        console.error(err);
    }
}

async function clearMockEmails() {
    await api("/api/mock-emails", { method: "DELETE" });
    await loadMockEmails();
    showToast("Simulation logs cleared", "success");
}

function showToast(message, type = "success") {
    const toast = document.createElement("div");
    toast.className = `toast ${type}`;
    let iconClass = "fa-circle-check";
    if (type === "error") iconClass = "fa-circle-xmark";
    else if (type === "warning") iconClass = "fa-circle-exclamation";
    toast.innerHTML = `<i class="fa-solid ${iconClass}"></i> <span>${message}</span>`;
    DOM.toastContainer.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
}
