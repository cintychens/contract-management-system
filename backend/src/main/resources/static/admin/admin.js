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

        document.getElementById("userName").textContent =
            userInfo.name || userInfo.fullName || userInfo.username || "系统管理员";

        document.getElementById("userRole").textContent =
            isAdmin ? "超级管理员" : "普通用户";

        const avatar = document.getElementById("userAvatar");
        avatar.textContent = (userInfo.username || "管").charAt(0).toUpperCase();

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
    document.querySelectorAll('.nav-item').forEach(item => item.classList.remove('active'));
    const target = document.querySelector(`.nav-item[data-tab="${tabName}"]`);
    if (target) target.classList.add('active');
}

function switchTab(tabName, el) {
    console.log("切换 tab:", tabName);

    document.querySelectorAll('.nav-item').forEach(item => item.classList.remove('active'));

    if (el && el.classList) {
        el.classList.add('active');
    } else {
        setActiveNavByTab(tabName);
    }

    const contentArea = document.getElementById('content-area');

    try {
        switch (tabName) {
            case 'dashboard':
                contentArea.innerHTML = renderDashboard();
                loadDashboardData();
                break;
            case 'users':
                contentArea.innerHTML = renderUserManagement();
                initUsersPage();
                break;
            case 'templates':
                contentArea.innerHTML = renderTemplateManagement();
                initTemplatesPage();
                break;
            case 'dictionary':
                contentArea.innerHTML = renderDictionaryManagement();
                break;
            case 'logs':
                contentArea.innerHTML = renderOperationLogs();
                break;
            case 'audit':
                contentArea.innerHTML = renderAuditRecords();
                break;
            case 'parameters':
                contentArea.innerHTML = renderSystemParameters();
                break;
            case 'contract-upload':
                contentArea.innerHTML = renderContractUploadPanel();
                loadRecentContracts();
                break;
            case 'backup':
                contentArea.innerHTML = renderDataMaintenance();
                break;
            default:
                console.warn("未知 tab:", tabName);
        }
    } catch (err) {
        console.error("switchTab 执行失败:", err);
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
    document.getElementById(modalId).classList.add('active');
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('active');
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
document.addEventListener('DOMContentLoaded', function () {
    loadAdminInfo();

    // 默认加载仪表盘
    document.getElementById('content-area').innerHTML = renderDashboard();
    loadDashboardData();

    // 绑定模板附件上传 change
    const templateFileInput = document.getElementById("templateFile");
    if (templateFileInput) {
        templateFileInput.addEventListener("change", handleTemplateFileChange);
    }

    // 绑定模板表单 submit
    const templateForm = document.getElementById("templateForm");
    if (templateForm) {
        templateForm.addEventListener("submit", async function (e) {
            e.preventDefault();

            const name = document.getElementById("editTemplateName").value.trim();
            const contractType = document.getElementById("editContractType").value;
            const content = document.getElementById("editTemplateContent").value.trim();
            const remark = document.getElementById("editTemplateRemark")?.value.trim() || "";
            const status = document.getElementById("editTemplateStatus").value === "active"
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

    // 智能生成提交绑定
    const generateForm = document.getElementById("generateForm");
    if (generateForm) {
        generateForm.addEventListener("submit", async function (e) {
            e.preventDefault();

            const templateSelect = document.getElementById("generateTemplateSelect");
            const templateId = templateSelect?.value;
            const selectedOption = templateSelect?.selectedOptions?.[0];
            const contractType = selectedOption?.dataset?.contractType || "";

            const titleInput = document.getElementById("generateContractTitle");
            const previewInput = document.getElementById("generatePreview");

            const title = titleInput ? titleInput.value.trim() : "";
            const draftContent = previewInput ? previewInput.value.trim() : "";
            if (!templateId) {
                alert("请先选择模板");
                return;
            }

            if (!title) {
                alert("请填写合同标题");
                return;
            }

            if (!draftContent) {
                alert("请先生成合同内容");
                return;
            }

            if (!contractType) {
                alert("未获取到合同类型，请重新选择模板");
                return;
            }

            const payload = {
                templateId: Number(templateId),
                title: title,
                contractType: contractType,
                draftContent: draftContent
            };

            // 同样把动态字段一起补上，避免后端保存时还需要这些值
            const fieldMap = {
                partyA: "partyA",
                partyB: "partyB",
                amount: "amount",
                signDate: "signDate",
                effectiveDate: "effectiveDate",
                expireDate: "expireDate",
                serviceContent: "serviceContent",
                paymentTerms: "paymentTerms",
                breachLiability: "breachLiability",

                paymentTerm: "paymentTerms",
                paymentMethod: "paymentTerms",
                reconciliationCycle: "paymentTerms",
                disputeCourt: "breachLiability",
                requireDate: "expireDate"
            };

            document.querySelectorAll(".generate-field").forEach(input => {
                const rawKey = input.dataset.key;
                const val = input.value.trim();

                if (!rawKey) return;

                const mappedKey = fieldMap[rawKey] || rawKey;
                payload[mappedKey] = val;
            });

            console.log("保存为合同参数：", payload);

            try {
                const resp = await authFetch("/api/contracts/confirm-generated", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(payload)
                });

                const result = await resp.json().catch(() => ({}));
                console.log("保存返回:", result);

                if (!resp.ok || result.code !== 200) {
                    throw new Error(result.message || "保存失败");
                }

                alert("保存成功");
                closeModal("generateModal");

                if (titleInput) titleInput.value = "";
                if (previewInput) previewInput.value = "";

                const fieldsBox = document.getElementById("generateFieldsContainer");
                if (fieldsBox) fieldsBox.innerHTML = "";

            } catch (e) {
                console.error(e);
                alert("保存失败：" + (e.message || e));
            }
        });
    }

    // 模态框点击外部关闭
    window.onclick = function (event) {
        if (event.target.classList.contains('modal')) {
            event.target.classList.remove('active');
        }
    };
});