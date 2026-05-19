package com.contract.contract_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DeepSeekService {

    @Value("${vectorengine.api-key}")
    private String apiKey;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // =============================
    // 变量保护
    // =============================
    private String protectVars(String content) {
        return content.replaceAll("\\$\\{(\\w+)}", "__VAR_$1__");
    }

    private String restoreVars(String content) {
        return content.replaceAll("__VAR_(\\w+)__", "\\${$1}");
    }

    private String normalizeEmptyFields(String text) {

        if (text == null) return "";

        // 1️⃣ 把 ${xxx} → 【待确认】
        text = text.replaceAll("\\$\\{.*?}", "【待确认】");

        // 2️⃣ 把空字段补成【待确认】
        // 例如：发货日期： → 发货日期：【待确认】
        text = text.replaceAll("：\\s*(?=\\n|$)", "：【待确认】");

        // 3️⃣ 把 --- 或 ___ 这种占位也变成【待确认】
        text = text.replaceAll("：[-_\\s]+(?=\\n|$)", "：【待确认】");

        return text;
    }

    // =============================
    // 提取变量
    // =============================
    private Set<String> extractVars(String content) {

        Set<String> vars = new HashSet<>();

        Matcher m1 = Pattern.compile("\\$\\{(\\w+)}").matcher(content);
        while (m1.find()) {
            vars.add(m1.group(1));   // ⭐ 改这里
        }

        Matcher m2 = Pattern.compile("\\{\\{(\\w+)}}").matcher(content);
        while (m2.find()) {
            vars.add(m2.group(1));   // ⭐ 改这里
        }

        return vars;
    }

    private String extractFinalContract(String aiOutput) {

        if (aiOutput == null) return "";

        String startFlag = "---参考优化合同---";
        int start = aiOutput.indexOf(startFlag);

        if (start == -1) {
            return aiOutput; // 兜底
        }

        String sub = aiOutput.substring(start + startFlag.length());

        int end = sub.indexOf("---修改摘要---");
        if (end != -1) {
            sub = sub.substring(0, end);
        }

        return sub.trim();
    }

    private static boolean isFieldLine(String line) {

        if (line == null) return false;

        line = line.trim();

        // ① 包含冒号（字段）
        if (!line.contains("：")) return false;

        // ② 很短（字段特征）
        if (line.length() <= 20) return true;

        // ③ 包含数字或变量
        if (line.matches(".*\\d+.*")) return true;
        if (line.contains("${")) return true;
        if (line.contains("【待确认】")) return true;

        return false;
    }

    public static class DiffUtil {

        public static String diff(String oldText, String newText) {

            StringBuilder result = new StringBuilder();

            String[] oldArr = oldText.split("\n");
            String[] newArr = newText.split("\n");

            int max = Math.max(oldArr.length, newArr.length);

            for (int i = 0; i < max; i++) {

                String oldLine = i < oldArr.length ? oldArr[i] : "";
                String newLine = i < newArr.length ? newArr[i] : "";

                // ⭐⭐⭐ 关键：字段行直接跳过 diff
                if (isFieldLine(oldLine) || isFieldLine(newLine)) {
                    result.append("<div>")
                            .append(escape(newLine))
                            .append("</div>");
                    continue;
                }

                if (oldLine.equals(newLine)) {
                    result.append("<div>")
                            .append(escape(oldLine))
                            .append("</div>");
                } else {
                    result.append("<div>")
                            .append(diffLine(oldLine, newLine))
                            .append("</div>");
                }
            }

            return result.toString();
        }

        private static String diffLine(String oldLine, String newLine) {

            int m = oldLine.length();
            int n = newLine.length();

            int[][] dp = new int[m + 1][n + 1];

            // ⭐ 构建LCS表
            for (int i = m - 1; i >= 0; i--) {
                for (int j = n - 1; j >= 0; j--) {
                    if (oldLine.charAt(i) == newLine.charAt(j)) {
                        dp[i][j] = dp[i + 1][j + 1] + 1;
                    } else {
                        dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
                    }
                }
            }

            // ⭐ 回溯生成diff
            StringBuilder sb = new StringBuilder();

            int i = 0, j = 0;

            while (i < m && j < n) {

                if (oldLine.charAt(i) == newLine.charAt(j)) {
                    sb.append(escape(String.valueOf(oldLine.charAt(i))));
                    i++;
                    j++;
                } else if (dp[i + 1][j] >= dp[i][j + 1]) {
                    // 删除
                    sb.append("<span style='background:#ffecec;color:red;'>")
                            .append(escape(String.valueOf(oldLine.charAt(i))))
                            .append("</span>");
                    i++;
                } else {
                    // 新增
                    sb.append("<span style='background:#eaffea;color:green;'>")
                            .append(escape(String.valueOf(newLine.charAt(j))))
                            .append("</span>");
                    j++;
                }
            }

            // 剩余删除
            while (i < m) {
                sb.append("<span style='background:#ffecec;color:red;'>")
                        .append(escape(String.valueOf(oldLine.charAt(i))))
                        .append("</span>");
                i++;
            }

            // 剩余新增
            while (j < n) {
                sb.append("<span style='background:#eaffea;color:green;'>")
                        .append(escape(String.valueOf(newLine.charAt(j))))
                        .append("</span>");
                j++;
            }

            return sb.toString();
        }

        private static String escape(String s) {
            return s.replace("<", "&lt;").replace(">", "&gt;");
        }
    }

    // =============================
    // 未填写字段
    // =============================
    private Set<String> findUnfilledVars(String content) {

        Set<String> unfilled = new HashSet<>();

        // ⭐ 检测 ${变量}
        Matcher m = Pattern.compile("\\$\\{(\\w+)}").matcher(content);
        while (m.find()) {
            unfilled.add(m.group(1));
        }

        // ⭐ 检测【待确认】
        if (content.contains("【待确认】")) {
            unfilled.add("UNFILLED_PLACEHOLDER");
        }

        return unfilled;
    }

    // =============================
    // 高亮
    // =============================
    private String highlightUnfilled(String content, Set<String> unfilled) {

        // ⭐ 高亮 ${变量}
        for (String var : unfilled) {

            String t1 = "${" + var + "}";
            String t2 = "{{" + var + "}}";

            String replacement = "<span style='color:red;font-weight:bold'>" + var + "</span>";

            content = content.replace(t1, replacement);
            content = content.replace(t2, replacement);
        }

        // ⭐ 高亮【待确认】
        content = content.replace(
                "【待确认】",
                "<span style='color:red;font-weight:bold'>【待确认】</span>"
        );

        return content;
    }

    // =============================
    // 智能判断
    // =============================
    private boolean shouldBlockOptimization(String content) {

        int varCount = extractVars(content).size();

        int placeholderCount = content.contains("【待确认】") ? 3 : 0;

        int totalUnfilled = varCount + placeholderCount;

        return totalUnfilled > 3; // 超过3个就阻止AI
    }

    private List<String> extractFieldNames(String content) {

        List<String> fields = new ArrayList<>();

        String[] lines = content.split("\n");

        for (String line : lines) {

            if (!line.contains("【待确认】")) continue;

            // ✅ 去掉编号（1. 2. （一）等）
            line = line.replaceAll("^\\s*[（(]?[一二三四五六七八九十0-9]+[）).．、]\\s*", "");

            // ✅ 提取“字段名：”
            if (line.contains("：")) {
                String field = line.split("：")[0].trim();

                // ✅ 过滤太长的（防止整句话进来）
                if (field.length() <= 10 && field.length() >= 2) {
                    fields.add(field);
                }
            }
        }

        return fields;
    }

    private String mergeFieldsIntoContent(String content, Map<String, Object> fields) {

        if (fields == null) return content;

        for (Map.Entry<String, Object> entry : fields.entrySet()) {

            String key = entry.getKey();
            Object value = entry.getValue();

            if (value == null) continue;

            String val = value.toString().trim();

            if (val.isEmpty()) continue;

            // ⭐ 替换 ${变量}
            content = content.replace("${" + key + "}", val);
            content = content.replace("{{" + key + "}}", val);
        }

        return content;
    }

    // =============================
    // 拆分AI结果
    // =============================
    private Map<String, String> splitResult(String text) {

        String contract = text;
        String suggestion = "";

        if (text.contains("【风险与优化建议】")) {
            String[] parts = text.split("【风险与优化建议】", 2);
            contract = parts[0];
            suggestion = parts[1];
        }

        return Map.of(
                "contract", contract.trim(),
                "suggestion", suggestion.trim()
        );
    }

    private Map<String, Object> normalizeFields(Map<String, Object> fields) {

        if (fields == null) {
            return new HashMap<>();
        }

        Map<String, Object> normalized = new HashMap<>();

        for (Map.Entry<String, Object> entry : fields.entrySet()) {

            String key = entry.getKey();

            // snake_case → camelCase
            if (key.contains("_")) {
                String[] parts = key.split("_");
                StringBuilder sb = new StringBuilder(parts[0]);

                for (int i = 1; i < parts.length; i++) {
                    sb.append(parts[i].substring(0,1).toUpperCase())
                            .append(parts[i].substring(1));
                }

                key = sb.toString();
            }

            normalized.put(key, entry.getValue());
        }

        return normalized;
    }

    private String replaceVariables(String text, Map<String, Object> fields) {

        if (text == null) return "";

        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() == null ? "" : entry.getValue().toString();

            // 替换 ${xxx}
            text = text.replace("${" + key + "}", value);
        }

        return text;
    }

    // ⭐ 新增：提取 ${xxx} 变量
    private List<String> extractVariables(String content) {

        List<String> vars = new ArrayList<>();

        if (content == null) return vars;

        Matcher matcher = Pattern.compile("\\$\\{(.*?)}").matcher(content);

        while (matcher.find()) {
            vars.add(matcher.group(1));
        }

        return vars;
    }

    // =============================
    // 主方法
    // =============================
    public Map<String, Object> generateWithTemplate(Map<String, Object> data, String templateType) {

        try {
            // ⭐ 处理智能问答模式
            if ("qa".equalsIgnoreCase(templateType)) {
                String question = (String) data.get("question");
                String content = (String) data.get("content");
                
                if (question == null || question.isBlank()) {
                    return Map.of("error", "问题不能为空");
                }
                
                String answer = answerQuestion(question, content);
                return Map.of(
                        "contract", answer,
                        "suggestion", "",
                        "risk", "",
                        "missing", ""
                );
            }

            // 原有的合同优化逻辑
            String content = (String) data.get("content");
            Map<String, Object> fields = (Map<String, Object>) data.get("fields");

            // ⭐⭐⭐ ⭐⭐⭐ ⭐⭐⭐ 加在这里！！！
            fields = normalizeFields(fields);

            // ⭐⭐⭐ 再做替换
            content = mergeFieldsIntoContent(content, fields);

            content = normalizeEmptyFields(content);

            if (content == null || content.isBlank()) {
                return Map.of("error", "合同内容不能为空");
            }

            // ⭐ 未填字段
            Set<String> unfilledVars = findUnfilledVars(content);
            // ⭐ 把未填写字段拼成提示文本（交给AI用）
            String missingFieldTip = "";

            if (!unfilledVars.isEmpty()) {
                List<String> fieldNames = extractFieldNames(content);

                if (!fieldNames.isEmpty()) {
                    missingFieldTip = "⚠ 以下字段尚未填写：" + String.join("、", fieldNames) + "。\n" +
                            "请在【风险标注】中识别为高风险，并说明可能带来的法律风险。";
                }
            }

            boolean blocked = shouldBlockOptimization(content);

            // ⭐ 保护变量
            String protectedContent = protectVars(content);

            String prompt = """
你是企业级物流合同法律风控助手。

你的职责：
1. 分析合同风险
2. 识别缺失条款
3. 提供法律修改建议
4. 生成参考优化合同

==============================
【最高优先级规则（必须执行）】

你绝对不能修改用户输入的内容。

包括但不限于：

- 金额
- 日期
- 数量
- 地址
- 公司名称
- 联系人
- 电话
- 运输信息
- 仓储信息
- ${xxx} 占位符
- 【待确认】字段

所有字段必须逐字保留。

==============================
【严格禁止】

禁止：

1. 删除字段
2. 修改字段
3. 新增空字段
4. 修改编号
5. 修改标题
6. 修改用户填写内容
7. 编造金额
8. 编造赔偿比例
9. 编造法律条文编号
10. 删除 ${xxx}

错误示例：

原文：
发货时间：2026-05-01

错误：
发货时间：合理时间内

原文：
起运地：${origin}

错误：
起运地：

==============================
【字段保护规则（必须执行）】

以下内容必须原样保留：

1. ${xxx}
2. 【待确认】
3. 所有“字段：内容”结构

例如：

货物名称：家用电器
数量：200件
运输方式：公路运输

这些内容必须逐字复制。

不得改写。
不得解释。
不得补充。

==============================
【原合同结构锁定规则（最高优先级）】

必须严格保持原合同结构。

禁止：

1. 合并标题
2. 合并章节
3. 合并字段
4. 删除空行
5. 自动重组合同结构
6. 自动优化排版
7. 自动重新组织章节
8. 自动追加重复内容

==============================

【标题锁定】

以下内容必须保持原样：

- 主标题
- 副标题
- “运输C类”
- “仓储C类”
- “运输合同（普通运输）”
- “仓储合同（长期存储）”

每个标题必须独立一行。

不得合并。

错误示例：

仓储C类仓储合同（长期存储）

正确示例：

仓储C类

仓储合同（长期存储）

==============================

【尾部锁定规则】

合同尾部：

- 争议解决
- 签章
- 签署日期

只能出现一次。

禁止重复生成。

若原合同已有：
“九、争议解决”

则不得再次新增：
“九、争议解决”

==============================

【禁止自动美化】

禁止：

- 自动整理格式
- 自动压缩标题
- 自动合并段落
- 自动补齐编号
- 自动生成重复章节

必须最大程度保持原合同结构。

==============================

【优化合同生成规则】

生成“参考优化合同”时：

仅允许：

1. 修改风险条款
2. 补充缺失法律条款
3. 新增必要法律章节

除此之外：

所有原结构必须保持不变。

==============================

==============================
【合同原意保护规则（最高优先级）】

AI只能：

- 补充
- 细化
- 明确

不得改变原合同的法律含义。

==============================

【禁止改变责任主体】

若原合同已经明确责任主体：

例如：

“由甲方承担责任”
“由乙方承担责任”

则：

不得改写为：

- “责任方承担”
- “过错方承担”
- “双方协商”
- “相关方承担”

必须保持原责任主体不变。

==============================

【禁止改变责任范围】

若原合同：

仅写“承担相应责任”

AI可以：

- 补充赔偿方式
- 补充处理流程

但不得：

- 扩大责任
- 新增无限责任
- 新增全额赔偿
- 新增连带责任

除非原合同已有明确表达。

==============================

【禁止重复新增章节】

若合同已经存在：

- 运输责任
- 货损责任
- 延误责任
- 风险责任

则不得重复新增同类章节。

AI只能：

- 在原章节内补充
- 在原条款内细化

不得重新新增重复法律章节。

==============================

【禁止改写标题】

合同标题必须逐字保留。

例如：

原文：
北京快捷 - - -华北极速

运输合同B类

则输出时：

必须完全一致。

不得：

- 删除符号
- 合并标题
- 修改空格
- 重新组合标题

==============================

【优化原则】

AI优化的目标：

是“法律明确化”。

不是：

“重写合同”。

==============================

==============================
【未填写字段识别】

仅以下情况属于未填写：

1. 包含 ${xxx}
2. 包含【待确认】
3. 冒号后为空

例如：

发货时间：
起运地：${origin}

属于未填写。

==============================
【已填写判断（必须执行）】

以下情况视为已填写：

- CC
- TEST
- 123
- 任意中文
- 任意数字
- 任意非空内容

即使内容不合理，
也不得标记为未填写。

系统只判断：
“是否为空”。

不判断：
“是否合理”。

==============================
【风险分析重点】

必须重点分析：

1. 责任范围不明确
2. 风险划分不明确
3. 验收规则缺失
4. 赔偿范围缺失
5. 结算规则不明确
6. 仓储责任缺失
7. 运输延误责任缺失
8. 不可抗力缺失

==============================
【法律条款生成规则】

新增法律条款时：

1. 必须使用正式合同语言
2. 必须生成完整条款
3. 不得只输出一句建议
4. 不得口语化

每个条款至少包含：

- 适用条件
- 责任主体
- 处理方式
- 责任范围

==============================
【物流行业限定】

若属于运输合同：

重点补充：

- 运输责任
- 货损责任
- 延误责任
- 风险转移
- 验收规则

若属于仓储合同：

重点补充：

- 入库管理
- 出库管理
- 仓储保管责任
- 超期仓储
- 库存异常处理

==============================
【禁止模糊表达】

以下内容必须识别风险：

- “承担相应责任”
- “合理时间”
- “视情况处理”
- “友好协商”

必须给出明确法律表达。

==============================
【合同排版格式规则（必须执行）】

输出合同时，必须严格保持标准合同排版格式。

必须遵守以下规则：

1. 合同标题必须单独一行

错误：
仓储合同（长期存储）甲方（存货方）...

正确：
仓储合同（长期存储）

甲方（存货方）：...

==============================

2. 甲方、乙方、联系人、联系电话必须各自独立一行

正确示例：

甲方（存货方）：国药医药物流集团有限公司
乙方（仓储服务方）：智慧冷链仓储股份有限公司

联系人：王敏
联系电话：13777779999

==============================

3. “一、二、三……”章节标题必须单独一行

正确示例：

一、货物基本信息

货物名称：医药冷链产品
数量：1200箱

==============================

4. “1. 2. 3.” 子条款必须单独一行

错误：
1. 建立库存台账 2. 定期盘点

正确：
1. 建立库存台账

2. 定期盘点

==============================

5. 字段行必须保持原有换行结构

例如：

货物名称：医药冷链产品
数量：1200箱
重量：28吨

不得压缩为：

货物名称：医药冷链产品 数量：1200箱 重量：28吨

==============================

6. 每个大章节之间必须空一行

例如：

五、费用与结算

仓储费用：268000元

（空一行）

六、风险与责任

==============================

7. 不得将整个合同压缩成一个自然段

8. 不得删除原有换行

9. 不得改变原有章节顺序

10. 必须输出标准合同格式文本

若未满足上述规则，
则视为不合格输出。
==============================

==============================
【输出格式（严格固定）】

---原合同---
（原样输出）

---风险标注---
1. [位置：原文]
| 等级：高/中/低
| 问题：[问题]
| 原因：[原因]

---缺失条款---
- 缺失：[条款名]
- 建议补充：[完整条款]

---建议修改---
- 原条款：[原文]
- 建议：[修改建议]

---参考优化合同---
要求：

1. 保持原合同结构
2. 不修改字段值
3. 不修改编号
4. 不删除任何内容
5. 仅新增法律条款
6. 所有原内容必须保留

（输出完整优化合同）

---修改摘要---
一句话总结风险。

==============================
请处理以下合同：

%s
""".formatted(protectedContent);

            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4o-mini",
                    "messages", new Object[]{
                            Map.of("role", "system", "content", "法律合同助手"),
                            Map.of("role", "user", "content", prompt)
                    }
            );

            String body = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.vectorengine.ai/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String res = response.body();
            JsonNode root = objectMapper.readTree(res);

            String result = root.get("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            // ⭐ 恢复变量
            // ⭐ 恢复变量
            result = restoreVars(result);

// ⭐⭐⭐ 1. 拆分AI结构
            Map<String, String> sections = splitAISections(result);

            String risk = sections.get("risk");
            String missing = sections.get("missing");
            String suggestion = sections.get("suggestion");
            String contract = sections.get("contract");

// ⭐⭐⭐ 2. 替换变量（只对合同）
            contract = replaceVariables(contract, fields);

// ⭐⭐⭐ 3. 高亮（只对合同）
            String html = highlightUnfilled(contract, unfilledVars)
                    .replace("\n", "<br>");

// ⭐⭐⭐ 4. 返回（重点！！！）
            return Map.of(
                    "risk", risk,
                    "missing", missing,
                    "suggestion", suggestion,
                    "contract", contract,
                    "highlightHtml", html,
                    "suggestionMsg", blocked
                            ? "⚠ 当前信息较少，建议补充字段"
                            : "AI已完成合同优化",
                    "unfilledFields", unfilledVars
            );

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "AI失败：" + e.getMessage());
        }
    }

    private Map<String, String> splitAISections(String text) {

        if (text == null) {
            text = "";
        }

        // ⭐ 兼容 AI 可能输出的中文标题格式
        text = text.replace("【风险标注】", "---风险标注---")
                .replace("【缺失条款】", "---缺失条款---")
                .replace("【建议修改】", "---建议修改---")
                .replace("【参考优化合同】", "---参考优化合同---")
                .replace("【优化合同】", "---参考优化合同---")
                .replace("【修改摘要】", "---修改摘要---");

        String risk = safeExtract(text, "---风险标注---", "---缺失条款---");
        String missing = safeExtract(text, "---缺失条款---", "---建议修改---");
        String suggestion = safeExtract(text, "---建议修改---", "---参考优化合同---");

        // ⭐ 合同部分截到“修改摘要”之前，避免摘要混进合同
        String contract = safeExtract(text, "---参考优化合同---", "---修改摘要---");

        return Map.of(
                "risk", risk,
                "missing", missing,
                "suggestion", suggestion,
                "contract", contract
        );
    }

    private String safeExtract(String text, String start, String end) {
        if (text == null || !text.contains(start)) {
            return "";
        }

        String sub = text.split(Pattern.quote(start), 2)[1];

        if (end != null && sub.contains(end)) {
            sub = sub.split(Pattern.quote(end), 2)[0];
        }

        return sub.trim();
    }

    private Map<String, String> splitContract(String text) {

        if (text == null) {
            return Map.of("base", "", "aiPart", "");
        }

        String[] lines = text.split("\n");

        StringBuilder base = new StringBuilder();
        StringBuilder aiPart = new StringBuilder();

        boolean aiStarted = false;

        for (String line : lines) {

            // ⭐ 判断是否进入“条款区”（不是字段行）
            if (!aiStarted && !isFieldLine(line)) {
                aiStarted = true;
            }

            if (aiStarted) {
                aiPart.append(line).append("\n");
            } else {
                base.append(line).append("\n");
            }
        }

        return Map.of(
                "base", base.toString(),
                "aiPart", aiPart.toString()
        );
    }

    // ===== 智能问答方法 =====
    public String answerQuestion(String question, String content) {
        try {
            String contextInfo = "";
            
            // 如果提供了合同内容，加入到上下文中
            if (content != null && !content.isBlank()) {
                contextInfo = "系统数据：\n" + content.substring(0, Math.min(50000, content.length())) + "\n\n";
            }

            String systemPrompt = "你是一位专业的合同管理专家和法律顾问，擅长：1.分析合同信息和风险；2.统计和总结合同数据；3.识别合同中的关键条款和问题；4.提供合同管理建议。";
            
            String prompt = """
根据以下信息回答用户的问题。信息中可能包含合同列表、统计数据或合同详细内容。

%s

用户问题：%s

请用简洁、清晰的语言回答。如果涉及合同数据，请基于提供的信息进行分析。
""".formatted(contextInfo, question);

            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4o-mini",
                    "messages", new Object[]{
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", prompt)
                    },
                    "temperature", 0.7
            );

            String body = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.vectorengine.ai/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String res = response.body();
            JsonNode root = objectMapper.readTree(res);

            // 获取 AI 的回答
            String answer = root.get("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            return answer != null && !answer.isBlank() ? answer : "无法获得回答，请稍后重试。";

        } catch (Exception e) {
            e.printStackTrace();
            return "抱歉，处理您的问题时出错：" + e.getMessage();
        }
    }

    public String chat(String context, String question) {

        try {

            String prompt = """
你是“智能物流合同管理系统”中的企业级AI合同助手。

你的职责：
1. 解答合同内容问题
2. 分析合同风险
3. 解释合同条款
4. 分析合同履约状态
5. 分析运输与仓储业务情况
6. 辅助用户理解系统合同数据
7. 提供物流合同管理建议

==============================
【回答规则】

1. 必须严格基于系统提供的数据回答
2. 不得编造不存在的信息
3. 不得虚构法律条文
4. 不得虚构合同状态
5. 不得编造履约信息
6. 回答必须简洁、专业、正式
7. 不要使用 Markdown
8. 不要闲聊
9. 不要输出无关内容
10. 若系统中不存在相关内容，则直接回复：
“系统中未找到相关内容”

==============================
【系统分析规则】

当系统存在多个合同、履约记录或预警信息时：

AI不得简单重复原始数据。

禁止：

- 逐条朗读合同
- 重复输出合同ID
- 像数据库查询一样输出
- 重复全部字段内容

AI应优先：

1. 总结合同整体情况
2. 分析运输与仓储业务状态
3. 分析合同状态分布
4. 分析履约进度
5. 分析风险情况
6. 分析预警信息

==============================
【物流业务分析规则】

若问题涉及：

- 运输合同
- 仓储合同
- 履约管理
- 发货
- 到货
- 验收
- 结算
- 风险预警

则必须结合：

- 合同状态
- 履约节点
- 风险信息
- 当前业务情况

进行综合分析。

==============================
【风险分析规则】

当发现以下情况时，应主动提示风险：

- 合同长期未完成
- 合同存在逾期
- 履约节点异常
- 缺少关键时间信息
- 缺少责任条款
- 缺少验收规则
- 存在高风险责任描述

==============================
【推荐回答风格】

错误示例：

系统有3个合同：
合同ID...
合同标题...

正确示例：

当前系统主要以运输合同与仓储合同为主，
运输合同涉及紧急运输、大额运输等场景。
目前部分合同仍处于草稿状态，
说明合同尚未正式进入履约阶段。

==============================
【系统数据】

%s

==============================
【用户问题】

%s
""".formatted(context, question);

            Map<String, Object> body = Map.of(
                    "model", "deepseek-chat",
                    "messages", List.of(
                            Map.of(
                                    "role", "user",
                                    "content", prompt
                            )
                    )
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.deepseek.com/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(body)
                    ))
                    .build();

            HttpResponse<String> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            JsonNode root =
                    objectMapper.readTree(response.body());

            return root.path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

        } catch (Exception e) {

            e.printStackTrace();

            return "AI问答服务暂时不可用";
        }
    }
}