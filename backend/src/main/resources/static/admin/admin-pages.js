// ===================== 状态 =====================
const usersState = {
    page: 1,
    size: 10,
    keyword: "",
    role: "ALL"
};

const templatesState = {
    page: 1,
    size: 10,
    keyword: "",
    contractType: "ALL",
    status: "ALL",
    editingId: null
};

const dictState = {
    page: 1,
    size: 50,
    dictType: "CONTRACT_FIELD"
};

// ✅ 允许的文件类型
const ALLOWED_TEMPLATE_MIME = new Set([
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
]);
const ALLOWED_TEMPLATE_EXT = new Set(["pdf", "doc", "docx"]);
const MAX_TEMPLATE_FILE_SIZE_MB = 20;

// ========== 仪表盘内容 ==========
function renderDashboard() {
    return `
        <div class="system-status">
            <h2>
                <i class="fas fa-server"></i>
                系统运行状态
            </h2>

            <div class="status-grid">
                <div class="status-item">
                    <i class="fas fa-database"></i>
                    <div class="status-value" id="dbStatus">--</div>
                    <div class="status-label">数据库可用性</div>
                    <div><span class="status-indicator indicator-green"></span> 实时状态</div>
                </div>

                <div class="status-item">
                    <i class="fas fa-users"></i>
                    <div class="status-value" id="userCount">--</div>
                    <div class="status-label">注册用户</div>
                    <div><span class="status-indicator indicator-green"></span> 今日新增</div>
                </div>

                <div class="status-item">
                    <i class="fas fa-file-contract"></i>
                    <div class="status-value" id="contractCount">--</div>
                    <div class="status-label">合同总数</div>
                    <div><span class="status-indicator indicator-green"></span> 系统统计</div>
                </div>

                <div class="status-item">
                    <i class="fas fa-clock"></i>
                    <div class="status-value" id="apiLatency">--</div>
                    <div class="status-label">API响应时间</div>
                    <div><span class="status-indicator indicator-yellow"></span> 实时监控</div>
                </div>
            </div>
        </div>
    `;
}

async function loadDashboardData() {
    try {
        const resp = await authFetch("/api/admin/dashboard");
        if (!resp.ok) throw new Error("获取统计数据失败");

        const data = await resp.json();

        const dbStatus = document.getElementById("dbStatus");
        const userCount = document.getElementById("userCount");
        const contractCount = document.getElementById("contractCount");
        const apiLatency = document.getElementById("apiLatency");

        if (dbStatus) dbStatus.textContent = (data.dbAvailability ?? "--") + "%";
        if (userCount) userCount.textContent = data.userCount ?? "--";
        if (contractCount) contractCount.textContent = data.contractCount ?? "--";
        if (apiLatency) apiLatency.textContent = (data.apiLatency ?? "--") + " ms";
    } catch (err) {
        console.error(err);
    }
}

// ========== 用户管理 ==========
function renderUserManagement() {
    return `
    <div class="content-section">
      <div class="section-header">
        <h2><i class="fas fa-users-cog"></i> 用户管理</h2>
        <div class="header-actions">
          <div class="search-box">
            <i class="fas fa-search"></i>
            <input id="userSearchInput" type="text" placeholder="搜索用户名...">
          </div>
        </div>
      </div>

      <div class="stats-mini-grid">
        <div class="stat-mini-card">
          <div class="stat-mini-value" id="userTotalCount">0</div>
          <div>总用户数</div>
        </div>
        <div class="stat-mini-card">
          <div class="stat-mini-value" id="userActiveCount">0</div>
          <div>启用用户</div>
        </div>
        <div class="stat-mini-card">
          <div class="stat-mini-value" id="userDisabledCount">0</div>
          <div>禁用用户</div>
        </div>
        <div class="stat-mini-card">
          <div class="stat-mini-value" id="adminCount">0</div>
          <div>管理员</div>
        </div>
        <div class="stat-mini-card">
          <div class="stat-mini-value" id="normalUserCount">0</div>
          <div>普通用户</div>
        </div>
      </div>

      <div class="tabs" id="userTabs">
        <div class="tab active" onclick="filterUsers('ALL', this)">全部用户</div>
        <div class="tab" onclick="filterUsers('USER', this)">普通用户</div>
        <div class="tab" onclick="filterUsers('ADMIN', this)">管理员</div>
      </div>

      <div class="table-responsive">
        <table>
          <thead>
            <tr>
              <th style="width:70px;">序号</th>
              <th>ID</th>
              <th>用户名</th>
              <th>姓名</th>
              <th>角色</th>
              <th>状态</th>
              <th>备注</th>
              <th>注册时间</th>
              <th>最后登录</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody id="userTableBody"></tbody>
        </table>

        <div id="userEmptyHint" style="padding: 40px 0; text-align: center; color: #6b7b8f;">
          暂无用户数据，请点击右上角“新建用户”创建。
        </div>

        <div id="userPagination"
             style="margin-top: 20px; display:flex; justify-content: space-between; align-items:center; color:#6b7b8f;">
          <div id="userPageInfo">—</div>
          <div style="display:flex; gap:10px;">
            <button class="btn-outline" id="userPrevBtn">上一页</button>
            <button class="btn-outline" id="userNextBtn">下一页</button>
          </div>
        </div>
      </div>
    </div>
  `;
}

function initUsersPage() {
    const input = document.getElementById("userSearchInput");
    if (input) {
        let timer = null;
        input.addEventListener("input", () => {
            clearTimeout(timer);
            timer = setTimeout(() => {
                usersState.keyword = input.value.trim();
                usersState.page = 1;
                loadUsersTable();
                loadUsersStats();
            }, 350);
        });
    }

    const prevBtn = document.getElementById("userPrevBtn");
    const nextBtn = document.getElementById("userNextBtn");

    if (prevBtn) {
        prevBtn.onclick = () => {
            if (usersState.page > 1) {
                usersState.page--;
                loadUsersTable();
            }
        };
    }

    if (nextBtn) {
        nextBtn.onclick = () => {
            usersState.page++;
            loadUsersTable();
        };
    }

    loadUsersStats();
    loadUsersTable();
}

async function loadUsersStats() {
    try {
        const resp = await authFetch("/api/admin/users/stats");
        if (!resp.ok) throw new Error(await resp.text());

        const data = await resp.json();

        const totalEl = document.getElementById("userTotalCount");
        const activeEl = document.getElementById("userActiveCount");
        const disabledEl = document.getElementById("userDisabledCount");
        const adminEl = document.getElementById("adminCount");
        const userEl = document.getElementById("normalUserCount");

        if (adminEl) adminEl.textContent = data.adminCount ?? 0;
        if (userEl) userEl.textContent = data.userCount ?? 0;
        if (totalEl) totalEl.textContent = data.total ?? 0;
        if (activeEl) activeEl.textContent = data.enabled ?? 0;
        if (disabledEl) disabledEl.textContent = data.disabled ?? 0;
    } catch (e) {
        console.error("loadUsersStats error:", e);
    }
}

async function loadUsersTable() {
    const tbody = document.getElementById("userTableBody");
    const emptyHint = document.getElementById("userEmptyHint");
    const pageInfo = document.getElementById("userPageInfo");
    const prevBtn = document.getElementById("userPrevBtn");
    const nextBtn = document.getElementById("userNextBtn");

    if (!tbody) return;

    tbody.innerHTML = `<tr><td colspan="10" style="color:#6b7b8f; padding:18px;">加载中...</td></tr>`;
    if (emptyHint) emptyHint.style.display = "none";

    const qs = new URLSearchParams({
        page: String(usersState.page),
        size: String(usersState.size),
        keyword: usersState.keyword || ""
    });

    if (usersState.role && usersState.role !== "ALL") {
        qs.set("role", usersState.role);
    }

    try {
        const resp = await authFetch(`/api/admin/users?${qs.toString()}`);
        if (!resp.ok) throw new Error(await resp.text());

        const data = await resp.json();
        const records = data.records || [];
        const total = data.total ?? 0;
        const page = data.page ?? usersState.page;
        const size = data.size ?? usersState.size;
        const totalPages = data.totalPages ?? Math.ceil(total / size);

        if (pageInfo) pageInfo.textContent = `第 ${page} / ${totalPages || 1} 页 · 共 ${total} 条`;
        if (prevBtn) prevBtn.disabled = page <= 1;
        if (nextBtn) nextBtn.disabled = totalPages ? page >= totalPages : records.length < size;

        if (!records.length) {
            tbody.innerHTML = "";
            if (emptyHint) emptyHint.style.display = "block";
            return;
        }

        if (emptyHint) emptyHint.style.display = "none";

        tbody.innerHTML = records.map((u, idx) => {
            const seq = (page - 1) * size + (idx + 1);
            const roleText = u.roleCode === "ADMIN" ? "管理员" : "普通用户";
            const statusHtml = u.status === "ENABLED"
                ? `<span class="status-badge status-active">启用</span>`
                : `<span class="status-badge status-disabled">禁用</span>`;

            const fullName = u.fullName || "-";
            const remark = u.remark || "-";
            const lastLogin = u.lastLoginAt || "-";
            const createdAt = u.createdAt || "-";

            return `
                <tr>
                  <td>${seq}</td>
                  <td>${u.userId ?? "-"}</td>
                  <td>${escapeHtml(u.username ?? "-")}</td>
                  <td>${escapeHtml(fullName)}</td>
                  <td>${roleText}</td>
                  <td>${statusHtml}</td>
                  <td class="user-remark-cell" title="${escapeHtml(remark)}">${escapeHtml(remark)}</td>
                  <td>${createdAt}</td>
                  <td>${lastLogin}</td>
                  <td>
                    <button class="action-btn" title="编辑"
                      onclick="openEditUser(
                        ${u.userId},
                        '${escapeAttr(u.username || "")}',
                        '${u.roleCode || ""}',
                        '${u.status || ""}',
                        '${escapeAttr(u.fullName || "")}',
                        '${escapeAttr(u.remark || "")}'
                      )">
                      <i class="fas fa-edit"></i>
                    </button>
                    <button class="action-btn danger" title="切换启用/禁用"
                      onclick="toggleUserStatus(${u.userId}, '${u.status}')">
                      <i class="fas fa-ban"></i>
                    </button>
                  </td>
                </tr>
            `;
        }).join("");
    } catch (e) {
        console.error("loadUsersTable error:", e);
        tbody.innerHTML = `<tr><td colspan="10" style="color:#dc3545; padding:18px;">加载失败：${escapeHtml(String(e.message || e))}</td></tr>`;
    }
}

function filterUsers(role, el) {
    document.querySelectorAll("#userTabs .tab").forEach(t => t.classList.remove("active"));
    if (el) el.classList.add("active");

    usersState.role = role;
    usersState.page = 1;

    loadUsersTable();
    loadUsersStats();
}

function openEditUser(userId, username, roleCode, status, fullName = "", remark = "") {
    document.getElementById("editUsername").value = username;
    document.getElementById("editRole").value = roleCode === "ADMIN" ? "admin" : "user";
    document.getElementById("editStatus").value = status === "ENABLED" ? "active" : "disabled";

    const fullNameEl = document.getElementById("editFullName");
    if (fullNameEl) fullNameEl.value = fullName || "";

    const remarkEl = document.getElementById("editRemark");
    if (remarkEl) remarkEl.value = remark || "";

    showModal("userModal");

    const form = document.getElementById("userForm");
    if (!form) return;

    form.onsubmit = async (e) => {
        e.preventDefault();

        const role = document.getElementById("editRole").value;
        const st = document.getElementById("editStatus").value;
        const newFullName = document.getElementById("editFullName").value.trim();
        const newRemark = document.getElementById("editRemark").value.trim();

        const payload = {
            fullName: newFullName,
            roleCode: role === "admin" ? "ADMIN" : "USER",
            status: st === "active" ? "ENABLED" : "DISABLED",
            remark: newRemark
        };

        try {
            const resp = await authFetch(`/api/admin/users/${userId}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
            if (!resp.ok) throw new Error(await resp.text());

            closeModal("userModal");
            await loadUsersTable();
            await loadUsersStats();
        } catch (err) {
            console.error(err);
            alert("保存失败：" + (err.message || err));
        }
    };
}

async function toggleUserStatus(userId, currentStatus) {
    const next = currentStatus === "ENABLED" ? "DISABLED" : "ENABLED";
    if (!confirm(`确认将用户状态改为：${next === "ENABLED" ? "启用" : "禁用"}？`)) return;

    try {
        const resp = await authFetch(`/api/admin/users/${userId}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ status: next })
        });
        if (!resp.ok) throw new Error(await resp.text());

        await loadUsersTable();
        await loadUsersStats();
    } catch (e) {
        console.error(e);
        alert("操作失败：" + (e.message || e));
    }
}

// ========== 模板管理 ==========
function renderTemplateManagement() {
    return `
    <div class="content-section">
      <div class="section-header">
        <h2><i class="fas fa-file-alt"></i> 合同模板管理</h2>
        <div class="header-actions">
          <div class="search-box">
            <i class="fas fa-search"></i>
            <input id="templateSearchInput" type="text" placeholder="搜索模板名称...">
          </div>

          <select id="templateTypeFilter" style="padding: 10px 14px; border-radius: 30px; border: 1px solid #ffd700;">
            <option value="ALL">全部类型</option>
            <option value="transport">运输合同</option>
            <option value="warehouse">仓储合同</option>
            <option value="supply">供应链协议</option>
            <option value="distribution">配送服务合同</option>
                <option value="outsourcing">物流外包合同</option>
          </select>

          <select id="templateStatusFilter" style="padding: 10px 14px; border-radius: 30px; border: 1px solid #ffd700;">
            <option value="ALL">全部状态</option>
            <option value="ENABLED">启用</option>
            <option value="DISABLED">禁用</option>
          </select>

          <button class="btn-primary" onclick="showAddTemplateModal()">
             <i class="fas fa-plus"></i> 新建模板
          </button>
        </div>
      </div>

      <div class="stats-mini-grid">
        <div class="stat-mini-card">
          <div class="stat-mini-value" id="templateTotalCount">0</div>
          <div>总模板数</div>
        </div>
        <div class="stat-mini-card">
          <div class="stat-mini-value" id="templateEnabledCount">0</div>
          <div>启用模板</div>
        </div>
        <div class="stat-mini-card">
          <div class="stat-mini-value" id="templateDisabledCount">0</div>
          <div>禁用模板</div>
        </div>
      </div>

      <div class="table-responsive">
        <table>
          <thead>
            <tr>
              <th>模板ID</th>
              <th>模板名称</th>
              <th>合同类型</th>
              <th>状态</th>
              <th>备注</th>
              <th>最后修改</th>
              <th>修改人</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody id="templateTableBody"></tbody>
        </table>

        <div id="templateEmptyHint" style="padding: 40px 0; text-align: center; color: #6b7b8f;">
          暂无模板数据，请点击右上角“新建模板”创建。
        </div>

        <div id="templatePagination"
             style="margin-top: 20px; display:flex; justify-content: space-between; align-items:center; color:#6b7b8f;">
          <div id="templatePageInfo">—</div>
          <div style="display:flex; gap:10px;">
            <button class="btn-outline" id="templatePrevBtn">上一页</button>
            <button class="btn-outline" id="templateNextBtn">下一页</button>
          </div>
        </div>
      </div>
    </div>
  `;
}

function initTemplatesPage() {
    const searchInput = document.getElementById("templateSearchInput");
    const typeFilter = document.getElementById("templateTypeFilter");
    const statusFilter = document.getElementById("templateStatusFilter");
    const prevBtn = document.getElementById("templatePrevBtn");
    const nextBtn = document.getElementById("templateNextBtn");

    if (searchInput) {
        let timer = null;
        searchInput.addEventListener("input", () => {
            clearTimeout(timer);
            timer = setTimeout(() => {
                templatesState.keyword = searchInput.value.trim();
                templatesState.page = 1;
                loadTemplatesTable();
            }, 350);
        });
    }

    if (typeFilter) {
        typeFilter.addEventListener("change", () => {
            templatesState.contractType = typeFilter.value;
            templatesState.page = 1;
            loadTemplatesTable();
        });
    }

    if (statusFilter) {
        statusFilter.addEventListener("change", () => {
            templatesState.status = statusFilter.value;
            templatesState.page = 1;
            loadTemplatesTable();
        });
    }

    if (prevBtn) {
        prevBtn.onclick = () => {
            if (templatesState.page > 1) {
                templatesState.page--;
                loadTemplatesTable();
            }
        };
    }

    if (nextBtn) {
        nextBtn.onclick = () => {
            templatesState.page++;
            loadTemplatesTable();
        };
    }

    loadTemplateStats();
    loadTemplatesTable();
}

async function loadTemplateStats() {
    try {
        const resp = await authFetch("/api/admin/templates/stats");
        if (!resp.ok) throw new Error(await resp.text());

        const data = await resp.json();

        const totalEl = document.getElementById("templateTotalCount");
        const enabledEl = document.getElementById("templateEnabledCount");
        const disabledEl = document.getElementById("templateDisabledCount");

        if (totalEl) totalEl.textContent = data.total ?? 0;
        if (enabledEl) enabledEl.textContent = data.enabled ?? 0;
        if (disabledEl) disabledEl.textContent = data.disabled ?? 0;
    } catch (e) {
        console.error("loadTemplateStats error:", e);
    }
}

async function loadTemplatesTable() {
    const tbody = document.getElementById("templateTableBody");
    const emptyHint = document.getElementById("templateEmptyHint");
    const pageInfo = document.getElementById("templatePageInfo");
    const prevBtn = document.getElementById("templatePrevBtn");
    const nextBtn = document.getElementById("templateNextBtn");

    if (!tbody) return;

    tbody.innerHTML = `<tr><td colspan="8" style="color:#6b7b8f; padding:18px;">加载中...</td></tr>`;
    if (emptyHint) emptyHint.style.display = "none";

    const qs = new URLSearchParams({
        page: String(templatesState.page),
        size: String(templatesState.size),
        keyword: templatesState.keyword || ""
    });

    if (templatesState.contractType && templatesState.contractType !== "ALL") {
        qs.set("contractType", templatesState.contractType);
    }

    if (templatesState.status && templatesState.status !== "ALL") {
        qs.set("status", templatesState.status);
    }

    try {
        const resp = await authFetch(`/api/admin/templates?${qs.toString()}`);
        if (!resp.ok) throw new Error(await resp.text());

        const data = await resp.json();
        const records = data.records || [];
        const total = data.total ?? 0;
        const page = data.page ?? templatesState.page;
        const size = data.size ?? templatesState.size;
        const totalPages = data.totalPages ?? Math.ceil(total / size);

        if (pageInfo) pageInfo.textContent = `第 ${page} / ${totalPages || 1} 页 · 共 ${total} 条`;
        if (prevBtn) prevBtn.disabled = page <= 1;
        if (nextBtn) nextBtn.disabled = totalPages ? page >= totalPages : records.length < size;

        if (!records.length) {
            tbody.innerHTML = "";
            if (emptyHint) emptyHint.style.display = "block";
            return;
        }

        if (emptyHint) emptyHint.style.display = "none";

        tbody.innerHTML = records.map(t => {
            const statusHtml = t.status === "ENABLED"
                ? `<span class="status-badge status-active">启用</span>`
                : `<span class="status-badge status-disabled">禁用</span>`;

            return `
                <tr>
                  <td>${t.templateId ?? "-"}</td>
                  <td>${escapeHtml(t.name ?? "-")}</td>
                  <td>${escapeHtml(getContractTypeText(t.contractType))}</td>
                  <td>${statusHtml}</td>
                  <td class="user-remark-cell" title="${escapeHtml(t.remark || "-")}">${escapeHtml(t.remark || "-")}</td>
                  <td>${t.updatedAt || "-"}</td>
                  <td>${escapeHtml(t.updatedBy || "-")}</td>
                  <td>
                    <button class="action-btn" title="编辑" onclick="editTemplate(${t.templateId})">
                      <i class="fas fa-edit"></i>
                    </button>
                    <button class="action-btn" title="切换状态" onclick="toggleTemplateStatus(${t.templateId}, '${t.status}')">
                      <i class="fas fa-toggle-on"></i>
                    </button>
                    <button class="action-btn danger" title="删除" onclick="deleteTemplate(${t.templateId})">
                      <i class="fas fa-trash"></i>
                    </button>
                  </td>
                </tr>
            `;
        }).join("");
    } catch (e) {
        console.error("loadTemplatesTable error:", e);
        tbody.innerHTML = `<tr><td colspan="8" style="color:#dc3545; padding:18px;">加载失败：${escapeHtml(String(e.message || e))}</td></tr>`;
    }
}

function formatBytes(bytes) {
    if (bytes === 0) return "0 B";
    const k = 1024;
    const sizes = ["B", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return (bytes / Math.pow(k, i)).toFixed(2) + " " + sizes[i];
}

function getContractTypeText(type) {
    const map = {
        transport: "运输合同",
        warehouse: "仓储合同",
        supply: "供应链协议",
        distribution: "配送服务合同",
        outsourcing: "物流外包合同"
    };
    return map[type] || type || "-";
}

function getExt(filename) {
    const idx = filename.lastIndexOf(".");
    return idx >= 0 ? filename.slice(idx + 1).toLowerCase() : "";
}

function clearTemplateFile() {
    const input = document.getElementById("templateFile");
    const preview = document.getElementById("templateFilePreview");
    const nameEl = document.getElementById("templateFileName");
    const metaEl = document.getElementById("templateFileMeta");
    if (input) input.value = "";
    if (nameEl) nameEl.textContent = "-";
    if (metaEl) metaEl.textContent = "-";
    if (preview) preview.style.display = "none";
}

function handleTemplateFileChange() {
    const input = document.getElementById("templateFile");
    const preview = document.getElementById("templateFilePreview");
    const nameEl = document.getElementById("templateFileName");
    const metaEl = document.getElementById("templateFileMeta");

    if (!input || !input.files || input.files.length === 0) {
        clearTemplateFile();
        return;
    }

    const file = input.files[0];
    const ext = getExt(file.name);
    const sizeMB = file.size / (1024 * 1024);

    const mimeOk = ALLOWED_TEMPLATE_MIME.has(file.type);
    const extOk = ALLOWED_TEMPLATE_EXT.has(ext);

    if (!mimeOk && !extOk) {
        alert("只允许上传 PDF 或 Word（.pdf/.doc/.docx）");
        clearTemplateFile();
        return;
    }

    if (sizeMB > MAX_TEMPLATE_FILE_SIZE_MB) {
        alert(`文件过大，建议不超过 ${MAX_TEMPLATE_FILE_SIZE_MB}MB`);
        clearTemplateFile();
        return;
    }

    if (nameEl) nameEl.textContent = file.name;
    if (metaEl) metaEl.textContent = `${file.type || ext.toUpperCase()} · ${formatBytes(file.size)}`;
    if (preview) preview.style.display = "block";
}

function showAddTemplateModal() {
    templatesState.editingId = null;

    document.getElementById("editTemplateName").value = "";
    document.getElementById("editContractType").value = "transport";
    document.getElementById("editTemplateContent").value = "";
    document.getElementById("editTemplateStatus").value = "active";

    const remarkEl = document.getElementById("editTemplateRemark");
    if (remarkEl) remarkEl.value = "";

    const titleEl = document.querySelector("#templateModal .modal-header h3");
    if (titleEl) {
        titleEl.innerHTML = '<i class="fas fa-plus"></i> 新建模板';
    }

    clearTemplateFile();
    showModal("templateModal");
}

async function editTemplate(templateId) {
    try {
        const resp = await authFetch(`/api/admin/templates/${templateId}`);
        if (!resp.ok) throw new Error(await resp.text());

        const data = await resp.json();
        templatesState.editingId = templateId;

        const titleEl = document.querySelector("#templateModal .modal-header h3");
        if (titleEl) {
            titleEl.innerHTML = '<i class="fas fa-edit"></i> 编辑模板';
        }

        document.getElementById("editTemplateName").value = data.name || "";
        document.getElementById("editContractType").value = data.contractType || "transport";
        document.getElementById("editTemplateContent").value = data.content || "";
        document.getElementById("editTemplateStatus").value =
            data.status === "DISABLED" ? "disabled" : "active";

        const remarkEl = document.getElementById("editTemplateRemark");
        if (remarkEl) remarkEl.value = data.remark || "";

        clearTemplateFile();
        showModal("templateModal");
    } catch (err) {
        console.error(err);
        alert("加载模板详情失败：" + (err.message || err));
    }
}

async function toggleTemplateStatus(templateId, currentStatus) {
    const next = currentStatus === "ENABLED" ? "DISABLED" : "ENABLED";
    const text = next === "ENABLED" ? "启用" : "禁用";

    if (!confirm(`确认要${text}该模板吗？`)) return;

    try {
        const userInfo = JSON.parse(sessionStorage.getItem("userInfo") || "{}");

        const resp = await authFetch(`/api/admin/templates/${templateId}/status`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                status: next,
                updatedBy: userInfo.username || "admin"
            })
        });

        if (!resp.ok) throw new Error(await resp.text());

        await loadTemplatesTable();
        await loadTemplateStats();
    } catch (e) {
        console.error(e);
        alert("状态切换失败：" + (e.message || e));
    }
}

async function deleteTemplate(templateId) {
    if (!confirm("确认删除该模板吗？删除后不可恢复。")) return;

    try {
        const resp = await authFetch(`/api/admin/templates/${templateId}`, {
            method: "DELETE"
        });

        if (!resp.ok) throw new Error(await resp.text());

        await loadTemplatesTable();
        await loadTemplateStats();
        alert("删除成功");
    } catch (e) {
        console.error(e);
        alert("删除失败：" + (e.message || e));
    }
}

// ========== 字段字典 ==========
function renderDictionaryManagement() {
    return `
        <div class="content-section">
            <div class="section-header">
                <h2><i class="fas fa-book"></i> 字段字典管理</h2>
                <div class="header-actions">
                    <button class="btn-primary" onclick="showAddDictModal()">
                        <i class="fas fa-plus"></i> 新增字段
                    </button>
                </div>
            </div>

            <div class="tabs" id="dictTabs">
                <div class="tab active" onclick="switchDictTab('CONTRACT_FIELD', this)">合同字段</div>
                <div class="tab" onclick="switchDictTab('NODE_TYPE', this)">履约节点类型</div>
                <div class="tab" onclick="switchDictTab('ALERT_RULE', this)">预警规则</div>
                <div class="tab" onclick="switchDictTab('ENUM_VALUE', this)">枚举值</div>
            </div>

            <div class="table-responsive">
                <table>
                    <thead>
                        <tr>
                            <th>字段编码</th>
                            <th>字段名称</th>
                            <th>字段类型</th>
                            <th>所属模块</th>
                            <th>是否必填</th>
                            <th>枚举值</th>
                            <th>状态</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody id="dictTableBody">
                        <tr>
                            <td colspan="8" style="color:#6b7b8f; padding:18px;">加载中...</td>
                        </tr>
                    </tbody>
                </table>

                <div id="dictEmptyHint" style="padding: 40px 0; text-align: center; color: #6b7b8f; display:none;">
                    暂无字典数据。
                </div>
            </div>
        </div>
    `;
}

function initDictionaryPage() {
    loadDictTable();
}

function switchDictTab(dictType, el) {
    dictState.dictType = dictType;
    dictState.page = 1;

    document.querySelectorAll("#dictTabs .tab").forEach(tab => {
        tab.classList.remove("active");
    });
    if (el) el.classList.add("active");

    loadDictTable();
}

async function loadDictTable() {
    const tbody = document.getElementById("dictTableBody");
    const emptyHint = document.getElementById("dictEmptyHint");

    if (!tbody) return;

    tbody.innerHTML = `<tr><td colspan="8" style="color:#6b7b8f; padding:18px;">加载中...</td></tr>`;
    if (emptyHint) emptyHint.style.display = "none";

    try {
        const qs = new URLSearchParams({
            page: String(dictState.page),
            size: String(dictState.size),
            dictType: dictState.dictType
        });

        const resp = await authFetch(`/api/admin/dict-items?${qs.toString()}`);
        if (!resp.ok) throw new Error(await resp.text());

        const result = await resp.json();
        const data = result.data || {};
        const records = data.records || [];

        if (!records.length) {
            tbody.innerHTML = "";
            if (emptyHint) emptyHint.style.display = "block";
            return;
        }

        if (emptyHint) emptyHint.style.display = "none";

        tbody.innerHTML = records.map(item => {
            const requiredText = item.requiredFlag ? "是" : "否";
            const statusHtml = item.status === "ENABLED"
                ? `<span class="status-badge status-active">启用</span>`
                : `<span class="status-badge status-disabled">禁用</span>`;

            return `
                <tr>
                    <td>${escapeHtml(item.itemKey || "-")}</td>
                    <td>${escapeHtml(item.itemName || "-")}</td>
                    <td>${escapeHtml(item.valueType || "-")}</td>
                    <td>${escapeHtml(item.moduleName || "-")}</td>
                    <td>${requiredText}</td>
                    <td>${escapeHtml(item.itemValue || "-")}</td>
                    <td>${statusHtml}</td>
                    <td>
                        <button class="action-btn" title="编辑" onclick="editDictItem(${item.id})">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="action-btn" title="切换状态" onclick="toggleDictStatus(${item.id}, '${item.status}')">
                            <i class="fas fa-toggle-on"></i>
                        </button>
                    </td>
                </tr>
            `;
        }).join("");
    } catch (e) {
        console.error("loadDictTable error:", e);
        tbody.innerHTML = `<tr><td colspan="8" style="color:#dc3545; padding:18px;">加载失败：${escapeHtml(String(e.message || e))}</td></tr>`;
    }
}

async function editDictItem(id) {
    try {
        const resp = await authFetch(`/api/admin/dict-items/${id}`);
        if (!resp.ok) throw new Error(await resp.text());

        const result = await resp.json();
        const item = result.data || {};

        alert(
            "编辑功能下一步再接，这里先确认详情已拿到：\n\n" +
            "字段编码：" + (item.itemKey || "-") + "\n" +
            "字段名称：" + (item.itemName || "-") + "\n" +
            "字段类型：" + (item.valueType || "-")
        );
    } catch (e) {
        console.error(e);
        alert("获取字典详情失败：" + (e.message || e));
    }
}

async function toggleDictStatus(id, currentStatus) {
    const next = currentStatus === "ENABLED" ? "DISABLED" : "ENABLED";
    const text = next === "ENABLED" ? "启用" : "禁用";

    if (!confirm(`确认要${text}该字典项吗？`)) return;

    try {
        const resp = await authFetch(`/api/admin/dict-items/${id}/status`, {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ status: next })
        });

        if (!resp.ok) throw new Error(await resp.text());

        await loadDictTable();
    } catch (e) {
        console.error(e);
        alert("状态切换失败：" + (e.message || e));
    }
}

function showAddDictModal() {
    alert("新增字段弹窗下一步再接，这一步先完成列表和状态切换。");
}

// ========== 操作日志 ==========
function renderOperationLogs() {
    return `
    <div class="content-section">
      <div class="section-header">
        <h2><i class="fas fa-history"></i> 操作日志</h2>
        <div class="header-actions">
          <div class="search-box">
            <i class="fas fa-search"></i>
            <input type="text" placeholder="搜索日志...">
          </div>
          <select style="padding: 8px; border-radius: 30px; border: 1px solid #ffd700;">
            <option value="">全部操作类型</option>
            <option value="user">用户管理</option>
            <option value="template">模板管理</option>
            <option value="contract">合同操作</option>
            <option value="system">系统配置</option>
          </select>
        </div>
      </div>

      <div class="table-responsive">
        <table>
          <thead>
            <tr>
              <th>时间</th>
              <th>操作人</th>
              <th>操作类型</th>
              <th>操作内容</th>
              <th>IP地址</th>
              <th>结果</th>
              <th>详情</th>
            </tr>
          </thead>
          <tbody id="logTableBody"></tbody>
        </table>

        <div id="logEmptyHint" style="padding: 40px 0; text-align: center; color: #6b7b8f;">
          暂无操作日志数据。
        </div>
      </div>

      <div id="logPagination"
           style="margin-top: 20px; display: flex; justify-content: space-between; align-items: center; color:#6b7b8f;">
        <div>—</div>
        <div style="display: flex; gap: 10px;">
          <button class="btn-outline" disabled>上一页</button>
          <button class="btn-outline" disabled>下一页</button>
        </div>
      </div>
    </div>
  `;
}

// ========== 审计记录 ==========
function renderAuditRecords() {
    return `
    <div class="content-section">
      <div class="section-header">
        <h2><i class="fas fa-shield-alt"></i> 审计记录</h2>
        <div class="header-actions">
          <div class="search-box">
            <i class="fas fa-search"></i>
            <input type="text" placeholder="搜索审计记录...">
          </div>
          <button class="btn-primary">
            <i class="fas fa-file-export"></i> 导出审计报告
          </button>
        </div>
      </div>

      <div class="stats-mini-grid">
        <div class="stat-mini-card">
          <div class="stat-mini-value" id="auditTotalCount">0</div>
          <div>总审计记录</div>
        </div>
        <div class="stat-mini-card">
          <div class="stat-mini-value" id="auditTodayCount">0</div>
          <div>今日新增</div>
        </div>
        <div class="stat-mini-card">
          <div class="stat-mini-value" id="auditAbnormalCount">0</div>
          <div>异常记录</div>
        </div>
      </div>

      <div class="table-responsive">
        <table>
          <thead>
            <tr>
              <th>审计ID</th>
              <th>时间</th>
              <th>操作用户</th>
              <th>操作类型</th>
              <th>资源类型</th>
              <th>资源ID</th>
              <th>操作结果</th>
              <th>IP地址</th>
            </tr>
          </thead>
          <tbody id="auditTableBody"></tbody>
        </table>

        <div id="auditEmptyHint" style="padding: 40px 0; text-align: center; color: #6b7b8f;">
          暂无审计记录数据。
        </div>
      </div>
    </div>
  `;
}

// ========== 系统参数 ==========
function renderSystemParameters() {
    return `
        <div class="content-section">
            <div class="section-header">
                <h2><i class="fas fa-sliders-h"></i> 系统参数配置</h2>
                <button class="btn-primary">
                    <i class="fas fa-save"></i> 保存修改
                </button>
            </div>

            <div style="display: grid; gap: 30px;">
                <div>
                    <h3 style="margin-bottom: 20px; color: #ffd700;">提醒预警配置</h3>
                    <div style="display: grid; gap: 20px;">
                        <div class="form-group">
                            <label>合同到期提醒提前天数</label>
                            <input type="number" value="7" min="1" max="30">
                        </div>
                        <div class="form-group">
                            <label>履约节点提醒提前天数</label>
                            <input type="number" value="3" min="1" max="15">
                        </div>
                        <div class="form-group">
                            <label>逾期预警时间（小时）</label>
                            <input type="number" value="24" min="1" max="72">
                        </div>
                    </div>
                </div>

                <div>
                    <h3 style="margin-bottom: 20px; color: #ffd700;">系统安全配置</h3>
                    <div style="display: grid; gap: 20px;">
                        <div class="form-group">
                            <label>密码最小长度</label>
                            <input type="number" value="8" min="6" max="20">
                        </div>
                        <div class="form-group">
                            <label>登录失败锁定次数</label>
                            <input type="number" value="5" min="3" max="10">
                        </div>
                        <div class="form-group">
                            <label>会话超时时间（分钟）</label>
                            <input type="number" value="30" min="10" max="120">
                        </div>
                    </div>
                </div>

                <div>
                    <h3 style="margin-bottom: 20px; color: #ffd700;">解析与生成配置</h3>
                    <div style="display: grid; gap: 20px;">
                        <div class="form-group">
                            <label>解析超时时间（秒）</label>
                            <input type="number" value="30" min="10" max="120">
                        </div>
                        <div class="form-group">
                            <label>生成超时时间（秒）</label>
                            <input type="number" value="30" min="10" max="120">
                        </div>
                        <div class="checkbox-group">
                            <input type="checkbox" id="autoParse" checked>
                            <label for="autoParse">上传后自动解析</label>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;
}

// ========== 数据维护 ==========
function renderDataMaintenance() {
    return `
        <div class="content-section">
            <div class="section-header">
                <h2><i class="fas fa-database"></i> 数据维护</h2>
            </div>

            <div style="display: grid; gap: 20px;">
                <div style="background: #f8fafc; border-radius: 20px; padding: 20px;">
                    <h3 style="margin-bottom: 15px; color: #ffd700;">数据库备份</h3>
                    <div style="display: grid; gap: 15px;">
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <div>
                                <strong>上次备份时间：</strong> 2024-03-15 03:00:00
                            </div>
                            <div>
                                <span class="status-badge status-active">成功</span>
                            </div>
                        </div>
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <div>
                                <strong>备份文件大小：</strong> 1.2 GB
                            </div>
                            <div>
                                <button class="btn-primary">
                                    <i class="fas fa-download"></i> 下载
                                </button>
                            </div>
                        </div>
                        <div style="display: flex; gap: 15px; margin-top: 10px;">
                            <button class="btn-primary">
                                <i class="fas fa-database"></i> 立即备份
                            </button>
                            <button class="btn-outline">
                                <i class="fas fa-cog"></i> 备份设置
                            </button>
                        </div>
                    </div>
                </div>

                <div style="background: #f8fafc; border-radius: 20px; padding: 20px;">
                    <h3 style="margin-bottom: 15px; color: #ffd700;">数据清理</h3>
                    <div style="display: grid; gap: 15px;">
                        <div style="display: flex; justify-content: space-between;">
                            <span>操作日志保留天数：</span>
                            <input type="number" value="90" style="width: 100px; padding: 5px;">
                        </div>
                        <div style="display: flex; justify-content: space-between;">
                            <span>审计日志保留天数：</span>
                            <input type="number" value="365" style="width: 100px; padding: 5px;">
                        </div>
                        <div style="display: flex; justify-content: space-between;">
                            <span>临时文件保留天数：</span>
                            <input type="number" value="7" style="width: 100px; padding: 5px;">
                        </div>
                        <button class="btn-outline" style="margin-top: 10px;">
                            <i class="fas fa-trash"></i> 立即清理
                        </button>
                    </div>
                </div>

                <div style="background: #f8fafc; border-radius: 20px; padding: 20px;">
                    <h3 style="margin-bottom: 15px; color: #ffd700;">数据导入/导出</h3>
                    <div style="display: flex; gap: 15px;">
                        <button class="btn-outline">
                            <i class="fas fa-file-import"></i> 导入数据
                        </button>
                        <button class="btn-outline">
                            <i class="fas fa-file-export"></i> 导出数据
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
}

// ========== 合同上传面板 ==========
function renderContractUploadPanel() {
    return `
    <div class="content-section">
      <div class="section-header">
        <h2><i class="fas fa-file-upload"></i> 合同文件上传</h2>
      </div>

      <div style="display:grid; gap:20px; max-width:900px;">
        <div class="form-group">
          <label for="contractUploadTitle">合同标题</label>
          <input type="text" id="contractUploadTitle" placeholder="请输入合同标题">
        </div>

        <div class="form-group">
          <label for="contractUploadType">合同类型</label>
          <select id="contractUploadType">
            <option value="">请选择合同类型</option>
            <option value="transport">运输合同</option>
            <option value="warehouse">仓储合同</option>
            <option value="supply">供应链协议</option>
            <option value="distribution">配送服务合同</option>
                <option value="outsourcing">物流外包合同</option>
          </select>
        </div>

        <div class="form-group">
          <label for="contractUploadFile">上传合同文件（PDF / DOC / DOCX / TXT）</label>
          <input
            type="file"
            id="contractUploadFile"
            accept=".pdf,.doc,.docx,.txt,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/plain"
          >
          <div style="margin-top:10px; font-size:13px; color:#6b7b8f;">
            仅支持：.pdf / .doc / .docx / .txt（建议 ≤ 20MB）
          </div>
        </div>

        <div>
          <button type="button" class="btn-primary" onclick="uploadContractFile()">
            <i class="fas fa-upload"></i> 上传合同
          </button>
        </div>

        <div id="contractUploadResult" style="display:none; padding:14px; border-radius:16px; background:#f8fafc; border:1px solid #ffd700;"></div>

        <div id="contractFieldSection" style="display:none;">
          <div class="content-section" style="margin-bottom:0; padding:20px; background:#fffdf6;">
            <div class="section-header" style="margin-bottom:16px;">
              <h2><i class="fas fa-list-check"></i> 解析结果</h2>
            </div>

            <div id="contractFieldLoading" style="display:none; color:#6b7b8f; padding:12px 0;">
              正在加载解析结果...
            </div>

            <div id="contractFieldEmpty" style="display:none; color:#6b7b8f; padding:12px 0;">
              暂无解析字段结果。
            </div>

            <div class="table-responsive">
              <table id="contractFieldTable" style="display:none;">
                <thead>
                  <tr>
                    <th style="width: 180px;">字段编码</th>
                    <th style="width: 180px;">字段名称</th>
                    <th>字段值</th>
                    <th style="width: 120px;">置信度</th>
                  </tr>
                </thead>
                <tbody id="contractFieldTableBody"></tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="content-section" style="margin-top:24px;">
      <div class="section-header">
        <h2><i class="fas fa-file-contract"></i> 最近合同</h2>
        <div class="header-actions">
          <button class="btn-outline" onclick="loadRecentContracts()">
            <i class="fas fa-rotate"></i> 刷新
          </button>
        </div>
      </div>

      <div class="table-responsive">
        <table>
          <thead>
            <tr>
              <th>合同ID</th>
              <th>合同编号</th>
              <th>标题</th>
              <th>类型</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody id="recentContractTableBody"></tbody>
        </table>

        <div id="recentContractEmptyHint" style="padding: 40px 0; text-align: center; color: #6b7b8f;">
          暂无合同数据。
        </div>
      </div>
    </div>

    <div class="content-section" id="contractDetailSection" style="display:none;">
      <div class="section-header">
        <h2><i class="fas fa-file-lines"></i> 合同详情</h2>
      </div>
      <div id="contractDetailContent" style="display:grid; gap:20px;"></div>
    </div>
  `;
}

async function loadContractFields(contractId) {
    const section = document.getElementById("contractFieldSection");
    const loading = document.getElementById("contractFieldLoading");
    const empty = document.getElementById("contractFieldEmpty");
    const table = document.getElementById("contractFieldTable");
    const tbody = document.getElementById("contractFieldTableBody");

    if (!section || !loading || !empty || !table || !tbody) return;

    section.style.display = "block";
    loading.style.display = "block";
    empty.style.display = "none";
    table.style.display = "none";
    tbody.innerHTML = "";

    try {
        const response = await authFetch(`/api/contracts/${contractId}/fields`, {
            method: "GET"
        });

        const result = await response.json();

        loading.style.display = "none";

        if (!response.ok || result.code !== 200) {
            empty.style.display = "block";
            empty.textContent = "解析结果加载失败：" + (result.message || "未知错误");
            return;
        }

        const fields = result.data || [];

        if (!fields.length) {
            empty.style.display = "block";
            empty.textContent = "暂无解析字段结果。";
            return;
        }

        tbody.innerHTML = fields.map(field => {
            const confidence = field.confidence != null
                ? (Number(field.confidence) * 100).toFixed(0) + "%"
                : "-";

            return `
                <tr>
                  <td>${escapeHtml(field.fieldKey || "-")}</td>
                  <td>${escapeHtml(field.fieldName || "-")}</td>
                  <td>${escapeHtml(field.fieldValue || "-")}</td>
                  <td>${confidence}</td>
                </tr>
            `;
        }).join("");

        table.style.display = "table";
    } catch (error) {
        console.error("加载解析结果失败：", error);
        loading.style.display = "none";
        empty.style.display = "block";
        empty.textContent = "解析结果加载失败：" + (error.message || error);
    }
}

async function loadRecentContracts() {
    const tbody = document.getElementById("recentContractTableBody");
    const emptyHint = document.getElementById("recentContractEmptyHint");

    if (!tbody) return;

    tbody.innerHTML = `<tr><td colspan="7" style="color:#6b7b8f; padding:18px;">加载中...</td></tr>`;
    if (emptyHint) emptyHint.style.display = "none";

    try {
        const resp = await authFetch("/api/contracts?page=1&size=10");
        if (!resp.ok) throw new Error(await resp.text());

        const result = await resp.json();
        const data = result.data || {};
        const records = data.records || [];

        if (!records.length) {
            tbody.innerHTML = "";
            if (emptyHint) emptyHint.style.display = "block";
            return;
        }

        tbody.innerHTML = records.map(c => `
            <tr>
              <td>${c.contractId ?? "-"}</td>
              <td>${escapeHtml(c.contractNo ?? "-")}</td>
              <td>${escapeHtml(c.title ?? "-")}</td>
              <td>${escapeHtml(getContractTypeText(c.contractType))}</td>
              <td>${escapeHtml(c.status ?? "-")}</td>
              <td>${escapeHtml(c.createdAt ?? "-")}</td>
              <td>
                <button class="action-btn" title="查看详情" onclick="viewContractDetail(${c.contractId})">
                  <i class="fas fa-eye"></i>
                </button>
                <button class="action-btn" title="查看字段" onclick="loadContractFields(${c.contractId})">
                  <i class="fas fa-list"></i>
                </button>
              </td>
            </tr>
        `).join("");
    } catch (e) {
        console.error(e);
        tbody.innerHTML = `<tr><td colspan="7" style="color:#dc3545; padding:18px;">加载失败：${escapeHtml(String(e.message || e))}</td></tr>`;
    }
}

async function viewContractDetail(contractId) {
    const section = document.getElementById("contractDetailSection");
    const content = document.getElementById("contractDetailContent");

    if (!section || !content) return;

    section.style.display = "block";
    content.innerHTML = `<div style="color:#6b7b8f;">加载详情中...</div>`;

    try {
        const resp = await authFetch(`/api/contracts/${contractId}`);
        if (!resp.ok) throw new Error(await resp.text());

        const result = await resp.json();
        const contract = result.data || result;

        content.innerHTML = `
          <div style="display:grid; grid-template-columns: repeat(2, 1fr); gap:20px;">
            <div class="stat-mini-card" style="text-align:left;">
              <div><strong>合同ID：</strong>${contract.contractId ?? "-"}</div>
              <div><strong>合同编号：</strong>${escapeHtml(contract.contractNo ?? "-")}</div>
              <div><strong>标题：</strong>${escapeHtml(contract.title ?? "-")}</div>
              <div><strong>类型：</strong>${escapeHtml(contract.contractType ?? "-")}</div>
            </div>

            <div class="stat-mini-card" style="text-align:left;">
              <div><strong>状态：</strong>${escapeHtml(contract.status ?? "-")}</div>
              <div><strong>当前版本：</strong>${contract.currentVersionId ?? "-"}</div>
              <div><strong>创建时间：</strong>${escapeHtml(contract.createdAt ?? "-")}</div>
              <div><strong>模板ID：</strong>${contract.templateId ?? "-"}</div>
            </div>
          </div>

          <div>
            <h3 style="margin-bottom:10px; color:#0a1a2b;">合同正文</h3>
            <div style="background:#f8fafc; border:1px solid #eef2f6; border-radius:16px; padding:18px; min-height:220px; white-space:pre-wrap; line-height:1.8;">
              ${escapeHtml(contract.content || "暂无正文内容")}
            </div>
          </div>
        `;

        section.scrollIntoView({ behavior: "smooth" });
    } catch (e) {
        console.error(e);
        content.innerHTML = `<div style="color:#dc3545;">详情加载失败：${escapeHtml(String(e.message || e))}</div>`;
    }
}

async function uploadContractFile() {
    const fileInput = document.getElementById("contractUploadFile");
    const title = document.getElementById("contractUploadTitle")?.value.trim() || "";
    const contractType = document.getElementById("contractUploadType")?.value || "";
    const resultBox = document.getElementById("contractUploadResult");

    if (!fileInput || !fileInput.files || fileInput.files.length === 0) {
        alert("请选择合同文件");
        return;
    }

    if (!title) {
        alert("请输入合同标题");
        return;
    }

    if (!contractType) {
        alert("请选择合同类型");
        return;
    }

    const file = fileInput.files[0];
    const formData = new FormData();
    formData.append("file", file);
    formData.append("title", title);
    formData.append("contractType", contractType);

    try {
        const response = await authFetch("/api/contracts/upload", {
            method: "POST",
            body: formData
        });

        const result = await response.json();

        if (result.code === 200) {
            const contractId = result.data.contractId;

            alert("上传成功，合同ID：" + contractId);

            if (resultBox) {
                resultBox.style.display = "block";
                resultBox.innerHTML = `
                  <div style="color:#0a1a2b; font-weight:700; margin-bottom:8px;">上传成功</div>
                  <div>合同ID：${result.data.contractId}</div>
                  <div>合同编号：${result.data.contractNo}</div>
                  <div>版本ID：${result.data.versionId}</div>
                  <div>状态：${result.data.status}</div>
                `;
            }

            await loadContractFields(contractId);
            await loadRecentContracts();

            document.getElementById("contractUploadTitle").value = "";
            document.getElementById("contractUploadType").value = "";
            document.getElementById("contractUploadFile").value = "";
        } else {
            alert("上传失败：" + result.message);

            if (resultBox) {
                resultBox.style.display = "block";
                resultBox.innerHTML = `
                  <div style="color:#dc3545; font-weight:700; margin-bottom:8px;">上传失败</div>
                  <div>${result.message || "未知错误"}</div>
                `;
            }
    const resp = await authFetch("/api/contracts?page=1&size=10");    }
    } catch (error) {
        console.error("合同上传异常：", error);
        alert("上传失败：网络异常或服务器错误");

        if (resultBox) {
            resultBox.style.display = "block";
            resultBox.innerHTML = `
              <div style="color:#dc3545; font-weight:700; margin-bottom:8px;">上传失败</div>
              <div>${error.message || error}</div>
            `;
        }
    }
}