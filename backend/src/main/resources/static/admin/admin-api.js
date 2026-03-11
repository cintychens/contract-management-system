// ========== 接口公共方法 ==========
function authFetch(url, options = {}) {
    const token = sessionStorage.getItem("token");
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