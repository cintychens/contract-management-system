# 合同管理系统

本项目是毕业设计《基于智能技术的物流行业合同管理系统的研发与实现》的核心实现部分，主要面向物流企业合同管理中的合同生成、审批流转、履约跟踪、风险预警和智能问答等业务场景，旨在提高合同管理的信息化和智能化水平。
这是一个基于 Spring Boot 的物流合同管理系统。后端采用 Spring Boot 3、Spring Security、JWT、Spring Data JPA 和 H2 数据库，前端页面放在 `backend/src/main/resources/static` 目录下。

## 技术栈

- Java 17
- Spring Boot 3.4.3
- Spring Web
- Spring Security + JWT
- Spring Data JPA / Hibernate
- H2 数据库，兼容 MySQL 模式
- Lombok
- Apache POI，导出 Word
- OpenPDF，导出 PDF
- 大模型接口调用，基于 `HttpClient` 调用兼容 OpenAI Chat Completions 格式的接口
- Jackson，负责 AI 请求和响应 JSON 解析
- Prompt Engineering，用于合同生成、合同优化、风险分析和问答助手
- 合同数据上下文增强，问答时汇总合同、字段、审批流和履约节点作为 AI 上下文

## 系统架构

系统整体采用前后端分离架构，前端主要负责页面展示与交互，后端主要负责业务逻辑处理、权限控制、合同审批流转以及 AI 能力调用。

```text
前端层（HTML / CSS / JavaScript）
        ↓
REST API 接口层（Controller）
        ↓
业务逻辑层（Service）
        ↓
数据访问层（Repository / JPA）
        ↓
H2 数据库
```

AI 服务通过 `HttpClient` 调用外部大模型接口，实现合同生成、合同优化、风险分析和问答功能。系统通过 Controller 对外提供 REST API，通过 Service 封装核心业务逻辑，通过 Repository 完成数据库读写，整体层次清晰，便于维护和扩展。

## 项目结构

```text
contract-management-system
+-- backend
|   +-- pom.xml
|   +-- src
|       +-- main
|           +-- java/com/contract/contract_backend
|           |   +-- common          # 通用返回、枚举、异常、工具类
|           |   +-- config          # 安全、跨域、JWT、文件上传等配置
|           |   +-- controller      # REST 接口
|           |   +-- dto             # 请求和响应对象
|           |   +-- entity          # JPA 实体
|           |   +-- repository      # 数据访问层
|           |   +-- service         # 核心业务逻辑
|           +-- resources
|               +-- application.yaml
|               +-- static          # 前端静态页面
+-- data                         # H2 数据文件
+-- uploads                      # 合同和模板上传文件
```

## 运行方式

进入后端目录：

```bash
cd backend
```

启动项目：

```bash
./mvnw spring-boot:run
```

Windows 环境可以使用：

```bash
mvnw.cmd spring-boot:run
```

默认访问地址：

```text
http://localhost:8080
```

H2 控制台：

```text
http://localhost:8080/h2-console
```

默认配置见 `backend/src/main/resources/application.yaml`：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:file:D:/contract-management-system/data/contract_db;MODE=MySQL
    username: sa
    password:

app:
  admin:
    username: admin
    password: admin123456
```

## 核心业务流程

系统当前主要支持运输合同和仓储合同两类业务场景，并根据不同合同类型初始化不同的履约节点。系统围绕合同全生命周期展开：

1. 用户注册或登录，登录成功后获得 JWT。
2. 业务人员上传合同文件，或选择模板生成合同草稿。然后再调用大语言模型服务生成AI优化后的合同版本。
3. 系统保存合同信息记录，并初始化履约节点。
4. 合同进入审批流，依次经过业务提交、法务审核、财务审核、审批人审核（涉及大金额合同）最终审批。
5. 合同审批通过后进入生效和履约阶段。
6. 履约节点可设置预计时间、完成状态、延期原因、验收结果和业务处理意见。
7. 系统可查询合同列表、合同详情、履约预警和审批流转记录。
8. AI 问答助手可基于系统中的合同数据回答用户问题。
9. 合同支持导出 PDF 和 Word 文件。

## 核心代码说明

### 1. 合同上传

接口位于 `ContractController`，核心入口是：

```java
@PostMapping("/upload")
public Result<ContractUploadResponse> uploadContract(
        @RequestParam("file") MultipartFile file,
        @RequestParam("title") String title,
        @RequestParam("contractType") String contractType
) {
    ContractUploadResponse response = contractService.uploadContract(file, title, contractType);
    return Result.success(response);
}
```

业务实现位于 `ContractServiceImpl.uploadContract`，主要步骤如下：

```java
String role = getCurrentUserRole();
if (!RoleCode.BUSINESS.equals(role) && !RoleCode.ADMIN.equals(role)) {
    throw new RuntimeException("无权限创建合同");
}

validateUpload(file, title, contractType);

String originalFileName = FileTypeUtil.sanitizeFileName(file.getOriginalFilename());
String extension = FileTypeUtil.getExtension(originalFileName);
String contractNo = generateUniqueContractNo();
String objectKey = ObjectKeyUtil.buildContractObjectKey(contractNo, originalFileName);
String fileHash = HashUtil.sha256(file.getInputStream());
String savedObjectKey = fileStorageService.uploadFile(file, objectKey);
```

上传成功后，系统会保存：

- `Contract`：合同主表记录。
- `ContractVersion`：合同版本记录。
- `ContractMilestone`：合同履约节点。

### 2. 模板生成合同草稿

接口：

```java
@PostMapping("/generate-draft")
public Result<ContractGenerateDto.GenerateResp> generateDraft(
        @RequestBody ContractGenerateDto.GenerateReq req
) {
    return Result.success(contractService.generateDraft(req));
}
```

核心逻辑：

```java
Template template = templateRepository.findById(req.getTemplateId())
        .orElseThrow(() -> new IllegalArgumentException("模板不存在"));

if (!"ENABLED".equalsIgnoreCase(template.getStatus())) {
    throw new IllegalArgumentException("模板未启用，不能用于生成合同");
}

String draftContent = generateByLlmOrFallback(template, req);
```

系统优先调用外部大模型生成合同草稿；如果没有配置 `LLM_API_URL`，则使用本地模板变量替换生成兜底草稿。

合同生成支持两条主要路径：

```text
模板变量替换生成：
模板内容 + 用户填写字段 -> 替换 ${fieldKey} 或 {{fieldKey}} -> 生成合同草稿

AI 生成：
模板内容 + 用户填写字段 + 合同生成提示词 -> 调用大模型 -> 返回正式合同草稿
```

生成逻辑中会校验模板是否存在、模板是否启用、合同标题和双方主体是否完整。生成失败或未配置 AI 地址时，系统会走本地兜底逻辑，避免合同生成功能完全不可用。

### 2.1 AI 合同优化和风险分析

系统还有一个通用 AI 处理接口：

```java
@PostMapping("/generate")
public Map<String, Object> generate(@RequestBody Map<String, Object> data) {
    String templateType = (String) data.get("templateType");
    Map<String, Object> result = deepSeekService.generateWithTemplate(data, templateType);

    if (result.containsKey("error")) {
        return Map.of("success", false, "message", result.get("error"));
    }

    return Map.of("success", true, "data", result);
}
```

对应接口：

```text
POST /api/ai/generate
```

该接口主要用于合同智能处理，`DeepSeekService.generateWithTemplate` 会根据合同内容和字段信息调用大模型，输出：

- 风险标注
- 缺失条款
- 修改建议
- 参考优化合同
- 未填写字段提示
- 高亮后的合同 HTML

核心处理思路：

```text
合同原文
-> 保护模板变量
-> 合并用户填写字段
-> 检测未填写字段
-> 构造合同风控 Prompt
-> 调用 AI 接口
-> 拆分风险、缺失条款、修改建议、优化合同
-> 恢复变量并返回给前端展示
```

### 3. 确认生成合同

接口：

```java
@PostMapping("/confirm-generated")
public Result<ContractGenerateDto.ConfirmResp> confirmGenerated(
        @RequestBody ContractGenerateDto.ConfirmReq req
) {
    return Result.success(contractService.confirmGeneratedContract(req));
}
```

核心逻辑包括：

- 创建合同主记录。
- 保存生成内容为第一个合同版本。
- 初始化履约节点。
- 调用合同解析服务抽取字段。
- 合并模板字段、用户输入字段和解析字段。
- 保存最终字段到 `ContractField`。

字段合并优先级：

```text
用户输入字段 > NLP 解析字段 > 模板默认待确认字段
```

### 4. 合同列表和详情

合同列表支持分页、关键词和状态筛选：

```java
@GetMapping
public Result<Map<String, Object>> getContracts(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status
) {
    return Result.success(contractService.getContracts(page, size, keyword, status));
}
```

合同详情接口：

```java
@GetMapping("/{contractId}")
public Result<Contract> getContractDetail(@PathVariable Long contractId) {
    return Result.success(contractService.getContractDetail(contractId));
}
```

### 5. 审批流

审批接口位于 `ContractFlowController`，主要动作包括：

```text
POST /api/contracts/{contractId}/flow/submit             业务人员提交合同，进入法务审核
POST /api/contracts/{contractId}/flow/legal/approve      法务审核通过，流转到财务审核
POST /api/contracts/{contractId}/flow/legal/reject       法务审核驳回，合同退回业务处理
POST /api/contracts/{contractId}/flow/finance/approve    财务审核通过，流转到最终审批
POST /api/contracts/{contractId}/flow/finance/reject     财务审核驳回，合同退回业务处理
POST /api/contracts/{contractId}/flow/approver/approve   最终审批通过，合同正式生效
POST /api/contracts/{contractId}/flow/approver/reject    最终审批驳回，合同退回业务处理
GET  /api/contracts/{contractId}/flow/records            查询合同审批流转记录
```

合同状态会随着审批动作流转，例如：

```text
DRAFT -> PENDING_LEGAL -> PENDING_FINANCE -> PENDING_APPROVAL -> ACTIVE
```

如果审核被驳回，合同会回到对应的待处理状态，并记录审批意见。

### 6. 履约节点和预警

履约节点接口位于 `ContractMilestoneController`，核心能力包括：

```text
GET  /api/milestones/{contractId}        查询某个合同的履约节点列表
POST /api/milestones/{id}/expected       设置履约节点预计完成时间
POST /api/milestones/{id}/complete       标记履约节点已完成
POST /api/milestones/{id}/delay          上报履约节点延期原因
POST /api/milestones/{id}/accept         对履约结果进行验收处理
POST /api/milestones/{id}/legal          法务处理履约异常或争议
GET  /api/milestones/alerts              查询履约预警列表
GET  /api/milestones/{contractId}/logs   查询某个合同的履约操作日志
```

合同创建或生成后会自动初始化履约节点，后续可根据节点预计完成时间、实际完成状态和验收结果生成预警。

运输合同典型履约节点：

```text
发货 -> 到货 -> 验收 -> 结算 -> 最终确认
```

仓储合同典型履约节点：

```text
入库 -> 在库 -> 出库 -> 结算 -> 最终确认
```

系统会根据不同合同类型自动初始化对应履约节点，并支持节点状态更新、延期上报、履约日志记录和预警生成。通过履约节点管理，可以把合同从“审批完成”继续延伸到“实际执行过程”，更贴近物流运输和仓储业务场景。

### 7. 合同导出

PDF 导出：

```java
@GetMapping("/{contractId}/export/pdf")
public ResponseEntity<byte[]> exportPdf(@PathVariable Long contractId) {
    Contract contract = contractService.getContractDetail(contractId);
    byte[] data = contractExportService.exportPdf(contractId);
    String fileName = buildFileName(contract.getTitle(), ".pdf");

    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
            .contentType(MediaType.APPLICATION_PDF)
            .body(data);
}
```

Word 导出：

```java
@GetMapping("/{contractId}/export/word")
public ResponseEntity<byte[]> exportWord(@PathVariable Long contractId) {
    Contract contract = contractService.getContractDetail(contractId);
    byte[] data = contractExportService.exportWord(contractId);
    String fileName = buildFileName(contract.getTitle(), ".docx");

    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
            .contentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            .body(data);
}
```

导出实现位于 `ContractExportServiceImpl`：

- Word 使用 Apache POI 的 `XWPFDocument`。
- PDF 使用 OpenPDF 的 `Document`、`PdfWriter` 和中文字体 `STSong-Light`。

### 8. 问答助手

问答助手接口位于 `QaController`：

```java
@PostMapping("/ask")
public Result<QaResponse> ask(@Valid @RequestBody QaRequest request) {
    return Result.success(qaService.ask(request));
}
```

对应接口：

```text
POST /api/qa/ask
```

核心服务是 `QaServiceImpl`：

```java
@Override
public QaResponse ask(QaRequest request) {
    String question = request.getQuestion().trim();
    String context = contractAiContextService.buildAllContractsContext();
    String answer = deepSeekService.answerQuestion(question, context);

    return QaResponse.builder()
            .contractId(request.getContractId())
            .question(question)
            .answer(answer)
            .source("api-ai-qa")
            .references(List.of("all contracts"))
            .build();
}
```

问答助手不是简单聊天，而是基于系统已有合同数据回答问题。`ContractAiContextService` 会汇总以下内容作为 AI 上下文：

- 合同基础信息：合同 ID、标题、编号、类型、状态、当前处理角色。
- 合同字段信息：合同解析出来的关键字段和值。
- 审批流转记录：动作类型、状态变化、审批意见和时间。
- 履约节点信息：节点名称、节点状态、预计时间和实际时间。
- 合同正文内容。

处理流程：

```text
用户问题
-> 查询系统全部合同数据
-> 拼接合同上下文
-> 构造问答 Prompt
-> 调用大模型接口
-> 返回中文答案、问题、来源和引用范围
```

问答助手适合回答：

- 当前有多少合同、分别处于什么状态。
- 哪些合同存在履约延期风险。
- 某个合同的关键条款是什么。
- 合同审批进行到哪一步。
- 运输合同和仓储合同的业务情况总结。
- 根据系统合同数据给出管理建议。

## 主要接口汇总

### 认证接口

```text
POST /api/auth/register    用户注册
POST /api/auth/login       用户登录
GET  /api/auth/me          获取当前登录用户
```

### 合同接口

```text
POST /api/contracts/upload               上传合同文件，并创建合同主记录和版本记录
POST /api/contracts/generate-draft       根据模板和用户填写信息生成合同草稿
POST /api/contracts/confirm-generated    确认 AI 或模板生成的合同草稿，并保存为正式合同记录
GET  /api/contracts                      分页查询合同列表，支持关键词和状态筛选
GET  /api/contracts/{contractId}         查询指定合同的详情信息
GET  /api/contracts/{contractId}/fields  查询指定合同解析出的关键字段
GET  /api/contracts/{contractId}/export/pdf   将指定合同导出为 PDF 文件
GET  /api/contracts/{contractId}/export/word  将指定合同导出为 Word 文件
```

### AI 接口

```text
POST /api/ai/generate    对合同内容进行 AI 优化、风险分析、缺失条款识别和修改建议生成
POST /api/qa/ask         合同问答助手，基于系统合同数据回答用户问题
```

### 审批接口

```text
POST /api/contracts/{contractId}/flow/submit                 业务提交合同，进入法务审核
POST /api/contracts/{contractId}/flow/legal/approve          法务审核通过
POST /api/contracts/{contractId}/flow/legal/reject           法务审核驳回
POST /api/contracts/{contractId}/flow/finance/approve        财务审核通过
POST /api/contracts/{contractId}/flow/finance/reject         财务审核驳回
POST /api/contracts/{contractId}/flow/approver/approve       最终审批通过，合同生效
POST /api/contracts/{contractId}/flow/approver/reject        最终审批驳回
POST /api/contracts/{contractId}/flow/complete               标记合同履约完成
POST /api/contracts/{contractId}/flow/request-termination    提交合同终止申请
POST /api/contracts/{contractId}/flow/approve-termination    审批通过合同终止申请
POST /api/contracts/{contractId}/flow/reject-termination     驳回合同终止申请
GET  /api/contracts/{contractId}/flow/records                查询合同审批和流转记录
```

### 履约接口

```text
GET  /api/milestones/{contractId}        查询合同履约节点
POST /api/milestones/{id}/expected       设置节点预计完成时间
POST /api/milestones/{id}/complete       完成履约节点
POST /api/milestones/{id}/delay          上报节点延期
POST /api/milestones/{id}/accept         验收履约节点
POST /api/milestones/{id}/legal          法务处理履约异常
GET  /api/milestones/alerts              查询履约预警
GET  /api/milestones/{contractId}/logs   查询履约节点操作日志
```

## 权限设计

系统使用 Spring Security 和 JWT 做登录认证。登录、注册、静态资源和 H2 控制台允许匿名访问；大多数 `/api/**` 接口需要登录。

典型角色包括：

```text
BUSINESS  业务人员：负责合同创建、合同上传、模板生成合同、提交审批以及履约过程中的业务处理等操作。
LEGAL     法务人员：负责审核合同内容的合法性与规范性，对合同风险及争议问题进行处理。
FINANCE   财务人员：负责审核合同金额、付款方式及财务相关条款，并参与合同费用结算审核。
APPROVER  审批人员：主要针对大金额或重要合同进行最终审批，决定合同是否正式生效。
ADMIN     管理员：负责系统用户管理、权限管理、模板管理等工作。
```

管理员接口 `/api/admin/**` 需要 `ADMIN` 权限。

## 数据模型核心表

主要实体包括：

- `User`：用户信息。
- `Contract`：合同主记录。
- `ContractVersion`：合同版本。
- `ContractField`：合同字段解析结果。
- `ContractFlowRecord`：合同审批流转记录。
- `ContractMilestone`：履约节点。
- `ContractMilestoneLog`：履约节点操作日志。
- `Template`：合同模板。
- `TemplateField`：模板字段。
- `TemplateFieldBind`：模板字段绑定关系。
- `SysDictItem`：系统字典项。

其中 `Contract` 是核心实体，保存合同编号、标题、类型、状态、当前版本、当前处理角色、创建时间和合同正文。

## 本人主要完成工作

1. 参与物流合同管理系统需求分析与业务流程设计，梳理合同创建、审批、履约和归档等核心流程。
2. 完成合同上传、模板生成、审批流转、履约节点管理和合同导出等核心功能开发。
3. 完成前端页面开发与页面交互实现，包括合同列表、合同详情、履约时间轴和 AI 问答页面等。
4. 完成 Spring Boot 后端接口开发、JWT 登录认证与权限控制实现。
5. 完成合同、审批流、履约节点及日志等数据库表结构设计。
6. 接入大模型接口，实现合同生成、合同优化、风险分析和问答助手等 AI 功能。
7. 参与系统测试、问题调试与功能优化，对合同状态流转、节点更新和权限异常等问题进行排查和修复。

## 注意事项

- 当前数据库使用本地 H2 文件，路径固定为 `D:/contract-management-system/data/contract_db`。
- 上传文件默认保存到 `D:/contract-management-system/uploads`。
- 默认管理员账号为 `admin`，默认密码为 `admin123456`。
- `application.yaml` 中存在开发环境密钥和默认密码，正式环境应改为环境变量或安全配置中心。
- 如果需要启用大模型生成合同，需要设置 `LLM_API_URL` 和 `LLM_API_KEY` 环境变量。
- 当前项目部分源码注释可能存在编码显示问题，建议统一使用 UTF-8 保存和打开源码文件。
- AI 合同生成、合同优化和问答助手依赖外部大模型接口；如果接口不可用，问答和智能优化会受到影响。
- 问答助手会把系统合同数据拼接为上下文发送给模型，生产环境需要注意敏感合同信息的脱敏和权限隔离。
