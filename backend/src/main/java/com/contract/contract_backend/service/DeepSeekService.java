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
            String content = (String) data.get("content");
            Map<String, Object> fields = (Map<String, Object>) data.get("fields");

            // ⭐⭐⭐ ⭐⭐⭐ ⭐⭐⭐ 加在这里！！！
            fields = normalizeFields(fields);

            // ⭐⭐⭐ 再做替换
            content = mergeFieldsIntoContent(content, fields);

            if (content == null || content.isBlank()) {
                return Map.of("error", "合同内容不能为空");
            }

            // ⭐ 未填字段
            Set<String> unfilledVars = findUnfilledVars(content);

            boolean blocked = shouldBlockOptimization(content);

            // ⭐ 保护变量
            String protectedContent = protectVars(content);

            String prompt = """
你是企业级合同智能优化系统中的【法律风控引擎】。

==============================
【前置判断】
判断输入是否属于合同（包含双方主体+权利义务+签署约定）。
若不是合同 → 输出：【非合同】类型：[文档类型]，不执行分析。

==============================
【核心原则】
你不得修改原合同的任何一个字符。
所有优化建议必须与原合同分离输出。

==============================
【严格禁止】
1. 不得删除、修改、增加原合同的任何文字、编号、标点
2. 不得修改任何已填写内容（金额、日期、人名、公司名）
3. 所有占位符 ${xxx} 原样保留
4. 不得编造任何事实、数据、金额、比例、赔偿上限
5. 不得输出“建议协商”“咨询律师”等空话
6. 不得使用“全部损失”“一切损失”“所有损失”等绝对化表述

==============================
【缺失条款判断（仅限以下5类）】
- 违约责任
- 不可抗力
- 争议解决
- 付款/结算方式
- 风险/责任划分

==============================
【责任条款严谨性判断】
检查责任条款是否包含以下三要素：
① 触发条件  
② 法律后果  
③ 赔偿范围  

缺失任一 → 在建议中标注并提供补充样板

【赔偿条款红线】
涉及赔偿的条款，若原合同没有明确上限：
- 禁止编造任何具体比例或金额
- 必须使用：“建议由双方另行约定”或“建议参考行业惯例确定”

==============================
【禁止使用的模糊表达】
以下表达必须识别为风险并提出具体化建议：
- “承担相应责任”
- “承担赔偿责任”（未说明范围）
- “合理时间”
- “视情况”
- “友好协商”（作为唯一解决方式）

==============================
【法律规范表达要求】
在生成“建议修改”和“参考优化合同”时：

1. 可使用法律依据表达：
   - “根据《中华人民共和国民法典》相关规定”
   - “依据国家相关法律法规”

2. 不得编造具体法条编号（如第XXX条）

3. 条款必须符合正式法律合同写法，避免口语化

4. 不得使用绝对化表述（如全部损失）

==============================
【标准法律条款生成规则（必须严格执行）】

在生成或补充以下条款时，必须采用标准法律条款结构进行表达：
- 违约责任
- 不可抗力
- 争议解决
- 付款/结算方式
- 风险/责任划分

==============================
【通用结构要求（必须满足）】

所有上述条款必须至少包含以下四个要素：

1. 条件要素（触发情形）
   - 明确说明何种情况下适用该条款
   - 示例：“如一方未履行或未完全履行本合同约定的义务……”

2. 法律后果要素
   - 明确责任承担方式
   - 示例：“应承担违约责任”或“可部分或全部免除责任”

3. 责任范围要素
   - 必须具体说明责任范围
   - 应包含：“直接损失”“可预见损失”等规范表述
   - 禁止使用模糊或绝对化表述（如“全部损失”“一切损失”）

4. 补充说明要素
   - 对责任承担方式进行补充说明
   - 可包括：费用范围（如律师费、诉讼费）、通知义务、处理流程等

==============================
【条款长度与结构要求】

1. 每个条款不得为单句描述，必须为多句结构（至少两句话）
2. 优先采用“总则 + 细化说明”的结构表达
3. 应避免口语化或说明性语句，必须符合正式法律文本风格

==============================
【专项强化要求（必须执行）】

① 违约责任条款：
必须体现：
- “未履行或未完全履行”两种情形
- “构成违约”的明确认定
- 损失范围 + 费用范围（如实现债权费用）

② 不可抗力条款：
必须包含：
- 不可抗力事件列举（自然灾害、战争等）
- 通知义务（在合理期限内通知）
- 证明义务（提供证明材料）
- 减损义务（采取措施减少损失）
- 免责规则（部分或全部免责）

③ 争议解决条款：
必须包含：
- 协商作为前置程序
- 明确法律适用（中华人民共和国法律）
- 明确管辖方式（法院或仲裁）

④ 付款条款：
必须包含：
- 支付条件（如履行完成后）
- 支付期限（时间表达）
- 支付方式（账户/形式）
- 逾期责任（如违约责任或处理方式）

⑤ 风险划分条款：
必须明确：
- 风险承担主体
- 风险转移条件
- 特殊情形（如不可抗力或一方过错）

==============================
【不合格重生成规则】

若生成条款存在以下任一情况，必须重新生成：
- 单句条款
- 缺少四要素中的任意一项
- 使用模糊表达（如“相应责任”）
- 未体现法律逻辑结构

不得直接输出不合格条款。

==============================
【缺失条款落地与一致性规则（必须执行）】

在“参考优化合同”中：

1. 缺失条款落地
   - 所有在“缺失条款”中列出的条款必须补充进合同正文
   - 不得仅出现在建议中
   - 新增条款应合理编号（如新增为第X条）

2. 条款分层原则
   - “具体条款”：描述具体违约或责任情形（如运输责任）
   - “违约责任条款”：作为统一责任规则（总则）
   - 两者不得混用

3. 去重与一致性
   - 不得重复表达同一法律责任内容
   - 若“具体条款”已明确责任范围，“违约责任条款”不得再次逐字重复
   - 若存在重复或冲突，必须合并或改写为不同层级表达

4. 逻辑一致性
   - 条款之间不得出现冲突或前后矛盾
   - 各条款应在逻辑上形成“具体情形 → 统一规则”的结构关系
   
==============================
【输出格式（严格遵循）】

---原合同---
（原样输出，一字不改）

---风险标注---
1. [位置：条款原文] | 等级：高/中/低 | 问题：[具体问题] | 原因：[法律原因]

---缺失条款---
- 缺失：[条款名] | 建议补充：[完整条款样板]

---建议修改---
- 原条款：“[原文引述]”
- 问题：[具体问题]
- 建议修改为：“[具体修改后的条款]”

---参考优化合同---
要求：
1. 不改变原合同结构（条款编号、顺序、标题）
2. 必须严格依据“建议修改”进行调整
3. 所有风险条款必须已修正
4. 所有缺失条款必须已补充
5. 不得新增无关条款
6. 未修改部分保持原样
7. 表达符合正式法律合同语言

（输出完整优化后的合同文本）

---修改摘要---
有风险需处理 / 无风险  
说明：[一句话总结主要问题]

==============================
请处理以下合同：
----------------------
%s
----------------------
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
            result = restoreVars(result);

            Map<String, String> parsed = splitResult(result);
            String finalContract = replaceVariables(parsed.get("contract"), fields);

            String html = highlightUnfilled(finalContract, unfilledVars)
                    .replace("\n", "<br>");

            return Map.of(
                    "contract", finalContract,
                    "highlightHtml", html,
                    "suggestion", blocked
                            ? "⚠ 当前信息较少，AI已做基础格式优化，建议补充关键信息"
                            : parsed.get("suggestion"),
                    "unfilledFields", unfilledVars
            );

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "AI失败：" + e.getMessage());
        }
    }
}