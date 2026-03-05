// api.js - 系统所有API接口

const API = {
    baseURL: '/api',

    // ========== 认证相关 ==========

    // 用户注册
    async register(userData) {
        const response = await fetch(`${this.baseURL}/auth/register`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(userData)
        });
        return this.handleResponse(response);
    },

    // 用户登录
    async login(credentials) {
        const response = await fetch(`${this.baseURL}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(credentials)
        });
        return this.handleResponse(response);
    },

    // 获取当前用户信息
    async getCurrentUser() {
        const response = await fetch(`${this.baseURL}/auth/me`, {
            method: 'GET',
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    },

    // 检查用户名是否可用
    async checkUsername(username) {
        const response = await fetch(`${this.baseURL}/auth/check-username?username=${encodeURIComponent(username)}`);
        return this.handleResponse(response);
    },

    // 发送手机验证码
    async sendVerificationCode(phone) {
        const response = await fetch(`${this.baseURL}/auth/send-code`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ phone })
        });
        return this.handleResponse(response);
    },

    // 验证手机验证码
    async verifyCode(phone, code) {
        const response = await fetch(`${this.baseURL}/auth/verify-code`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ phone, code })
        });
        return this.handleResponse(response);
    },

    // ========== 管理员接口 ==========

    // 获取用户列表
    async getUsers(params = {}) {
        const queryParams = new URLSearchParams(params).toString();
        const url = `${this.baseURL}/admin/users${queryParams ? '?' + queryParams : ''}`;

        const response = await fetch(url, {
            method: 'GET',
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    },

    // 获取单个用户
    async getUserById(userId) {
        const response = await fetch(`${this.baseURL}/admin/users/${userId}`, {
            method: 'GET',
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    },

    // 创建用户
    async createUser(userData) {
        const response = await fetch(`${this.baseURL}/admin/users`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify(userData)
        });
        return this.handleResponse(response);
    },

    // 更新用户
    async updateUser(userId, userData) {
        const response = await fetch(`${this.baseURL}/admin/users/${userId}`, {
            method: 'PUT',
            headers: this.getHeaders(),
            body: JSON.stringify(userData)
        });
        return this.handleResponse(response);
    },

    // 删除用户
    async deleteUser(userId) {
        const response = await fetch(`${this.baseURL}/admin/users/${userId}`, {
            method: 'DELETE',
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    },

    // 启用/禁用用户
    async toggleUserStatus(userId, enabled) {
        const response = await fetch(`${this.baseURL}/admin/users/${userId}/status`, {
            method: 'PATCH',
            headers: this.getHeaders(),
            body: JSON.stringify({ enabled })
        });
        return this.handleResponse(response);
    },

    // 重置用户密码
    async resetUserPassword(userId) {
        const response = await fetch(`${this.baseURL}/admin/users/${userId}/reset-password`, {
            method: 'POST',
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    },

    // 审核用户注册
    async approveUserRegistration(userId, approved) {
        const response = await fetch(`${this.baseURL}/admin/users/${userId}/approve`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify({ approved })
        });
        return this.handleResponse(response);
    },

    // 获取模板列表
    async getTemplates(params = {}) {
        const queryParams = new URLSearchParams(params).toString();
        const url = `${this.baseURL}/admin/templates${queryParams ? '?' + queryParams : ''}`;

        const response = await fetch(url, {
            method: 'GET',
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    },

    // 创建模板
    async createTemplate(templateData) {
        const response = await fetch(`${this.baseURL}/admin/templates`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify(templateData)
        });
        return this.handleResponse(response);
    },

    // 更新模板
    async updateTemplate(templateId, templateData) {
        const response = await fetch(`${this.baseURL}/admin/templates/${templateId}`, {
            method: 'PUT',
            headers: this.getHeaders(),
            body: JSON.stringify(templateData)
        });
        return this.handleResponse(response);
    },

    // 删除模板
    async deleteTemplate(templateId) {
        const response = await fetch(`${this.baseURL}/admin/templates/${templateId}`, {
            method: 'DELETE',
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    },

    // 启用/禁用模板
    async toggleTemplateStatus(templateId, enabled) {
        const response = await fetch(`${this.baseURL}/admin/templates/${templateId}/status`, {
            method: 'PATCH',
            headers: this.getHeaders(),
            body: JSON.stringify({ enabled })
        });
        return this.handleResponse(response);
    },

    // 获取字段列表
    async getFields(params = {}) {
        const queryParams = new URLSearchParams(params).toString();
        const url = `${this.baseURL}/admin/fields${queryParams ? '?' + queryParams : ''}`;

        const response = await fetch(url, {
            method: 'GET',
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    },

    // 创建字段
    async createField(fieldData) {
        const response = await fetch(`${this.baseURL}/admin/fields`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify(fieldData)
        });
        return this.handleResponse(response);
    },

    // 更新字段
    async updateField(fieldId, fieldData) {
        const response = await fetch(`${this.baseURL}/admin/fields/${fieldId}`, {
            method: 'PUT',
            headers: this.getHeaders(),
            body: JSON.stringify(fieldData)
        });
        return this.handleResponse(response);
    },

    // 删除字段
    async deleteField(fieldId) {
        const response = await fetch(`${this.baseURL}/admin/fields/${fieldId}`, {
            method: 'DELETE',
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    },

    // 获取操作日志
    async getLogs(params = {}) {
        const queryParams = new URLSearchParams(params).toString();
        const url = `${this.baseURL}/admin/logs${queryParams ? '?' + queryParams : ''}`;

        const response = await fetch(url, {
            method: 'GET',
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    },

    // 导出日志
    async exportLogs(params = {}) {
      const queryParams = new URLSearchParams(params).toString();
      const url = `${this.baseURL}/admin/logs/export${queryParams ? '?' + queryParams : ''}`;

      const response = await fetch(url, {
        method: 'GET',
        headers: this.getHeaders()
      });

      return this.handleBlobDownload(response, `logs_${new Date().toISOString().split('T')[0]}.csv`);
    },

    // 获取审计记录
    async getAuditRecords(params = {}) {
        const queryParams = new URLSearchParams(params).toString();
        const url = `${this.baseURL}/admin/audit${queryParams ? '?' + queryParams : ''}`;

        const response = await fetch(url, {
            method: 'GET',
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    },

    // 导出审计报告
    async exportAuditReport(params = {}) {
        const queryParams = new URLSearchParams(params).toString();
        const url = `${this.baseURL}/admin/audit/export${queryParams ? '?' + queryParams : ''}`;

        const response = await fetch(url, {
            method: 'GET',
            headers: this.getHeaders()
        });

        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = downloadUrl;
        a.download = `audit_report_${new Date().toISOString().split('T')[0]}.pdf`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(downloadUrl);
        document.body.removeChild(a);

        return true;
    },

    // 获取系统状态
    async getSystemStatus() {
        const response = await fetch(`${this.baseURL}/admin/monitor/status`, {
            method: 'GET',
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    },

    // 获取系统指标
    async getSystemMetrics(period = '24h') {
        const response = await fetch(`${this.baseURL}/admin/monitor/metrics?period=${period}`, {
            method: 'GET',
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    },

    // 获取系统参数
    async getSystemParameters() {
        const response = await fetch(`${this.baseURL}/admin/parameters`, {
            method: 'GET',
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    },

    // 更新系统参数
    async updateSystemParameters(params) {
        const response = await fetch(`${this.baseURL}/admin/parameters`, {
            method: 'PUT',
            headers: this.getHeaders(),
            body: JSON.stringify(params)
        });
        return this.handleResponse(response);
    },

    // 获取备份列表
    async getBackups() {
        const response = await fetch(`${this.baseURL}/admin/backups`, {
            method: 'GET',
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    },

    // 创建备份
    async createBackup() {
        const response = await fetch(`${this.baseURL}/admin/backups`, {
            method: 'POST',
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    },

    // 下载备份
    async downloadBackup(backupId) {
        const response = await fetch(`${this.baseURL}/admin/backups/${backupId}/download`, {
            method: 'GET',
            headers: this.getHeaders()
        });

        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = downloadUrl;
        a.download = `backup_${backupId}.sql`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(downloadUrl);
        document.body.removeChild(a);

        return true;
    },

    // 删除备份
    async deleteBackup(backupId) {
        const response = await fetch(`${this.baseURL}/admin/backups/${backupId}`, {
            method: 'DELETE',
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    },

    // 清理数据
    async cleanupData(cleanupConfig) {
        const response = await fetch(`${this.baseURL}/admin/cleanup`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify(cleanupConfig)
        });
        return this.handleResponse(response);
    },

    // 获取系统统计
    async getSystemStats() {
        const response = await fetch(`${this.baseURL}/admin/stats`, {
            method: 'GET',
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    },

    // 获取用户统计
    async getUserStats() {
        const response = await fetch(`${this.baseURL}/admin/stats/users`, {
            method: 'GET',
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    },

    // 获取合同统计
    async getContractStats() {
        const response = await fetch(`${this.baseURL}/admin/stats/contracts`, {
            method: 'GET',
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    },

    // ========== 工具方法 ==========

    // 获取认证token
    getToken() {
      return localStorage.getItem('token') || sessionStorage.getItem('token')
          || localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
    },

    // 清理登录信息（可按你项目增加）
    clearAuth() {
      localStorage.removeItem('token');
      sessionStorage.removeItem('token');
      localStorage.removeItem('accessToken');
      sessionStorage.removeItem('accessToken');

      localStorage.removeItem('userInfo');
      sessionStorage.removeItem('userInfo');
      localStorage.removeItem('isAdmin');
      sessionStorage.removeItem('isAdmin');
    },

    // 获取请求头
    getHeaders(extraHeaders = {}) {
      const headers = {
        'Content-Type': 'application/json',
        ...extraHeaders
      };

      const token = this.getToken();
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      return headers;
    },

    // blob 下载通用工具（更稳）
    async handleBlobDownload(response, filename) {
      // 401 统一处理
      if (response.status === 401) {
        this.clearAuth();
        alert('登录已过期，请重新登录');
        window.location.href = '/auth/login.html';
        return false;
      }

      if (!response.ok) {
        let errorMessage = `下载失败: ${response.status}`;
        try {
          const text = await response.text();
          if (text) errorMessage = text;
        } catch (e) {}
        throw new Error(errorMessage);
      }

      const blob = await response.blob();
      const downloadUrl = window.URL.createObjectURL(blob);

      const a = document.createElement('a');
      a.href = downloadUrl;
      a.download = filename || `download_${Date.now()}`;
      document.body.appendChild(a);
      a.click();

      window.URL.revokeObjectURL(downloadUrl);
      document.body.removeChild(a);
      return true;
    },

    // 统一处理响应（增强版：401跳转、204、非json容错）
    async handleResponse(response) {
      // ✅ 401 统一处理
      if (response.status === 401) {
        this.clearAuth();
        alert('登录已过期，请重新登录');
        window.location.href = '/auth/login.html';
        throw new Error('Unauthorized');
      }

      // ✅ 204 No Content
      if (response.status === 204) return null;

      if (!response.ok) {
        let errorMessage = `请求失败: ${response.status}`;
        try {
          const contentType = response.headers.get('content-type') || '';
          if (contentType.includes('application/json')) {
            const errorData = await response.json();
            errorMessage = errorData.message || errorData.error || errorMessage;
          } else {
            const text = await response.text();
            if (text) errorMessage = text;
          }
        } catch (e) {
          // 忽略解析错误
        }
        throw new Error(errorMessage);
      }

      // ✅ 自动识别响应类型
      const contentType = response.headers.get('content-type') || '';
      if (contentType.includes('application/json')) {
        return await response.json();
      }

      // 可能是纯文本 / 空
      try {
        return await response.text();
      } catch (e) {
        return null;
      }
    }
};