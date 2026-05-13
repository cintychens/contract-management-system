// ========== 接口公共方法 ==========
function authFetch(url, options = {}) {
    const token = sessionStorage.getItem("token") || localStorage.getItem("token");
    const headers = new Headers(options.headers || {});

    if (token) {
        headers.set("Authorization", "Bearer " + token);
    }

    return fetch(url, {
        ...options,
        headers
    });
}

async function uploadTemplateFile(file) {
    const fd = new FormData();
    fd.append("file", file);

    const resp = await authFetch("/api/admin/templates/upload", {
        method: "POST",
        body: fd
    });

    if (!resp.ok) {
        throw new Error(await resp.text());
    }

    return await resp.json();
}

/* =======================================================
   ⭐ 合同总览接口
======================================================= */

// 获取全部合同
async function fetchAllContracts(page = 1, size = 20) {

    const resp = await authFetch(
        `/api/contracts?page=${page}&size=${size}`
    );

    if (!resp.ok) {
        throw new Error(await resp.text());
    }

    return await resp.json();
}

// 获取合同详情
async function fetchContractDetail(contractId) {

    const resp = await authFetch(
        `/api/contracts/${contractId}`
    );

    if (!resp.ok) {
        throw new Error(await resp.text());
    }

    return await resp.json();
}