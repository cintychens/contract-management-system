// =========================
// 1. 通用请求（带 token）
// =========================
function authFetch(url, options = {}) {
    const token = localStorage.getItem("token") || sessionStorage.getItem("token");

    const headers = new Headers(options.headers || {});
    if (token) {
        headers.set("Authorization", "Bearer " + token);
    }

    return fetch(url, { ...options, headers });
}


// =========================
// 2. 用户信息（右上角）
// =========================
function loadUserInfo() {
    try {
        const userInfoStr = localStorage.getItem("userInfo") || sessionStorage.getItem("userInfo");
        if (!userInfoStr) return;

        const userInfo = JSON.parse(userInfoStr);

        const nameEl = document.getElementById('userName');
        const avatarEl = document.getElementById('userAvatar');

        if (nameEl) nameEl.textContent = userInfo.username || '用户';
        if (avatarEl) avatarEl.textContent = (userInfo.username || 'U').charAt(0);

    } catch (e) {
        console.error("用户信息解析失败", e);
    }
}


// =========================
// 3. 全局预警（核心）
// =========================
async function loadGlobalAlerts() {

    try {
        const resp = await authFetch(`/api/contracts`);
        if (!resp.ok) return;

        const result = await resp.json();
        const contracts = result.data || result || [];

        let total = 0;

        for (const c of contracts) {

            const contractId = c.contractId || c.id;

            const r = await authFetch(`/api/milestones/${contractId}`);
            if (!r.ok) continue;

            const res = await r.json();
            const list = res.data || res || [];

            const now = new Date();

            list.forEach(m => {
                if (!m.expectedDate || m.status === 'COMPLETED') return;

                const due = new Date(m.expectedDate);
                const diff = (due - now) / (1000 * 60 * 60 * 24);

                // ⭐ 逾期 或 3天内
                if (diff < 0 || diff <= 3) {
                    total++;
                }
            });
        }

        updateAlertBadge(total);

    } catch (e) {
        console.error("全局预警加载失败", e);
    }
}


// =========================
// 4. 更新侧边栏红点
// =========================
function updateAlertBadge(count) {

    const badge = document.getElementById("globalAlertBadge");

    if (!badge) return;

    if (count > 0) {
        badge.style.display = "inline-block";
        badge.innerText = count;
    } else {
        badge.style.display = "none";
    }
}


// =========================
// 5. 页面初始化（统一入口）
// =========================
async function initGlobal() {
    loadUserInfo();
    await loadGlobalAlerts();
}