// ========== 管理员信息加载 ==========
function loadAdminInfo() {
    try {
        const userInfoStr = sessionStorage.getItem("userInfo");

        if (!userInfoStr) {
            window.location.href = "/auth/login.html";
            return;
        }

        const userInfo = JSON.parse(userInfoStr);
        const isAdmin = userInfo.roleCode === "ADMIN";

        const userNameEl = document.getElementById("userName");
        const userRoleEl = document.getElementById("userRole");
        const userAvatarEl = document.getElementById("userAvatar");

        if (userNameEl) {
            userNameEl.textContent =
                userInfo.name || userInfo.fullName || userInfo.username || "系统管理员";
        }

        if (userRoleEl) {
            userRoleEl.textContent = isAdmin ? "超级管理员" : "普通用户";
        }

        if (userAvatarEl) {
            userAvatarEl.textContent = (userInfo.username || "管").charAt(0).toUpperCase();
        }

        if (!isAdmin) {
            window.location.href = "/dashboard/index.html";
        }
    } catch (e) {
        console.error("加载管理员信息失败:", e);
        window.location.href = "/auth/login.html";
    }
}

// ========== 标签页切换 ==========
function setActiveNavByTab(tabName) {
    document.querySelectorAll(".nav-item").forEach(item => item.classList.remove("active"));
    const target = document.querySelector(`.nav-item[data-tab="${tabName}"]`);
    if (target) target.classList.add("active");
}

function switchTab(tabName, el) {
    console.log("切换 tab:", tabName);

    document.querySelectorAll(".nav-item").forEach(item => item.classList.remove("active"));

    if (el && el.classList) {
        el.classList.add("active");
    } else {
        setActiveNavByTab(tabName);
    }

    const contentArea = document.getElementById("content-area");
    if (!contentArea) {
        console.error("未找到 #content-area");
        return;
    }

    try {
        switch (tabName) {
            case "dashboard":
                contentArea.innerHTML = renderDashboard();
                loadDashboardData();
                break;
            case "users":
                contentArea.innerHTML = renderUserManagement();
                initUsersPage();
                break;
            case "templates":
                contentArea.innerHTML = renderTemplateManagement();
                initTemplatesPage();
                break;
            case "dictionary":
                contentArea.innerHTML = renderDictionaryManagement();
                initDictionaryPage();
                break;
            case "logs":
                contentArea.innerHTML = renderOperationLogs();
                break;
            case "audit":
                contentArea.innerHTML = renderAuditRecords();
                break;
            case "parameters":
                contentArea.innerHTML = renderSystemParameters();
                break;
            case "contract-upload":
                contentArea.innerHTML = renderContractUploadPanel();
                loadRecentContracts();
                break;
            case "backup":
                contentArea.innerHTML = renderDataMaintenance();
                break;
            default:
                console.warn("未知 tab:", tabName);
                contentArea.innerHTML = `<div style="padding: 24px;">暂无页面内容</div>`;
        }
    } catch (err) {
        console.error("switchTab 执行失败:", err);
        contentArea.innerHTML = `
            <div style="padding:24px; color:#dc3545;">
                页面加载失败：${escapeHtml(String(err.message || err))}
            </div>
        `;
    }
}

// ========== 退出 ==========
function logout() {
    try {
        sessionStorage.removeItem("userInfo");
        sessionStorage.removeItem("isAdmin");
        sessionStorage.removeItem("token");
        window.location.href = "/auth/login.html";
    } catch (e) {
        console.error("logout error:", e);
        window.location.href = "/auth/login.html";
    }
}

// ========== 模态框控制 ==========
function showModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add("active");
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove("active");
    }
}

// ========== 小工具：防XSS ==========
function escapeHtml(str) {
    return String(str)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

function escapeAttr(str) {
    return escapeHtml(str).replaceAll("\n", " ");
}

// ========== 初始化页面 ==========
document.addEventListener("DOMContentLoaded", function () {
    try {
        loadAdminInfo();

        const contentArea = document.getElementById("content-area");
        if (!contentArea) {
            console.error("未找到 #content-area");
            return;
        }

        contentArea.innerHTML = renderDashboard();
        loadDashboardData();

        const templateFileInput = document.getElementById("templateFile");
        if (templateFileInput) {
            templateFileInput.addEventListener("change", handleTemplateFileChange);
        }

        const templateForm = document.getElementById("templateForm");
        if (templateForm) {
            templateForm.addEventListener("submit", async function (e) {
                e.preventDefault();

                const name = document.getElementById("editTemplateName")?.value.trim() || "";
                const contractType = document.getElementById("editContractType")?.value || "transport";
                const content = document.getElementById("editTemplateContent")?.value.trim() || "";
                const remark = document.getElementById("editTemplateRemark")?.value.trim() || "";
                const status = document.getElementById("editTemplateStatus")?.value === "active"
                    ? "ENABLED"
                    : "DISABLED";
                const file = document.getElementById("templateFile")?.files?.[0];

                if (!name) {
                    alert("请填写模板名称");
                    return;
                }

                if (!content) {
                    alert("请填写模板内容");
                    return;
                }

                let fileInfo = null;

                if (file) {
                    try {
                        fileInfo = await uploadTemplateFile(file);
                    } catch (err) {
                        alert("附件上传失败：" + (err.message || err));
                        return;
                    }
                }

                const userInfo = JSON.parse(sessionStorage.getItem("userInfo") || "{}");

                const payload = {
                    name,
                    contractType,
                    content,
                    remark,
                    status,
                    updatedBy: userInfo.username || "admin",
                    fileName: fileInfo?.fileName || null,
                    fileObjectKey: fileInfo?.fileObjectKey || null
                };

                try {
                    let resp;

                    if (templatesState.editingId) {
                        resp = await authFetch(`/api/admin/templates/${templatesState.editingId}`, {
                            method: "PUT",
                            headers: { "Content-Type": "application/json" },
                            body: JSON.stringify(payload)
                        });
                    } else {
                        resp = await authFetch("/api/admin/templates", {
                            method: "POST",
                            headers: { "Content-Type": "application/json" },
                            body: JSON.stringify(payload)
                        });
                    }

                    if (!resp.ok) {
                        const text = await resp.text();
                        throw new Error(text || "保存失败");
                    }

                    alert("模板已保存");
                    closeModal("templateModal");
                    clearTemplateFile();
                    await loadTemplatesTable();
                    await loadTemplateStats();
                } catch (err) {
                    console.error(err);
                    alert("保存失败：" + (err.message || err));
                }
            });
        }

        window.onclick = function (event) {
            if (event.target.classList.contains("modal")) {
                event.target.classList.remove("active");
            }
        };
    } catch (err) {
        console.error("页面初始化失败:", err);
    }
});