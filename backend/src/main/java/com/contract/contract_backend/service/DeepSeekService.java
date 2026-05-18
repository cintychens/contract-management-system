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
2. 不得修改任何用户填写的内容（金额、数量、日期、人名、公司名等等）
3. 所有占位符 ${xxx} 原样保留
4. 不得编造任何事实、数据、金额、比例、赔偿上限
5. 不得输出“建议协商”“咨询律师”等空话
6. 不得使用“全部损失”“一切损失”“所有损失”等绝对化表述

==============================
【占位符与待确认字段保护规则（必须执行）】

以下内容必须原样保留，不得删除、替换、补全、解释、拆分：

1. 所有 ${xxx} 占位符
2. 所有【待确认】字段
3. 所有用户已经填写的字段值

特别要求：
- 如果原文为：1. 起运地：${origin}
  输出中必须仍然包含：1. 起运地：${origin}
- 不得变成：1. 起运地：
- 不得新增一个空的“1. 起运地：”
- 不得将 ${origin}、${destination}、${transportMode} 替换为空字符串
- 不得生成重复编号条款

==============================
【字段逐行扫描规则（必须执行）】

在进行风险分析前，必须执行以下步骤：

一、逐行扫描合同（强制）
必须逐行检查合同中的每一条字段行（格式：xxx：value）

二、识别未填写字段（必须执行）

仅当满足以下情况，才判定为“未填写字段”：

1. 包含 ${xxx}
2. 包含【待确认】
3. 冒号后为空（如：发货日期：）

==============================
【字段有效性判断规则（新增，必须执行）】

以下情况必须判定为“已填写”（不得标记为未填写）：

1. 冒号后存在任意非空字符串（包括 CC、AA、TEST、123 等）
2. 存在数字、字母、中文内容（如：1000元、张三、北京）
3. 任意非空占位值（即使不具备真实业务含义）

特别强调：
- “CC”、“AA”、“XXX”、“TEST”等测试值 → 视为“已填写”
- 不得因为“值不合理”而标记为未填写
- 本系统仅判断“是否填写”，不判断“是否合理”

错误示例（禁止）：
货物名称：CC → 标记为未填写 ❌

正确示例（必须）：
货物名称：CC → 已填写，不得标记风险 ✔

三、强制输出要求（必须执行）

1. 每一个未填写字段，必须在【风险标注】中单独列出
2. 风险等级必须为：高
3. 必须使用原始字段完整文本作为“位置”

四、禁止行为（强制）

❌ 禁止：
- 仅分析条款，不分析字段
- 跳过字段检测
- 只分析一部分字段
- 将多个字段合并为一条风险

✔ 必须：
- 所有未填写字段全部输出
- 每个字段对应一条风险

五、补充强制校验（重要）

若合同中存在未填写字段，但【风险标注】未列出全部字段，
则视为不合格输出，必须重新生成

【换行结构强制规则（必须执行）】

生成的合同必须满足：

1. 标题必须单独一行
2. 甲方、乙方必须各自独立一行
3. “根据……”必须单独一行
4. 每个“一、二、三”必须单独一行
5. 每个“1. 2. 3.”必须单独一行
6. 不得将多条内容合并为一行

若未满足上述结构，必须重新生成

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

1. 可使用真是的法律依据表达：
   - “根据《中华人民共和国民法典》相关规定”
   - “依据国家相关法律法规”

2. 不得编造具体法条编号（如第XXX条），但可以引用

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

1. 每个条款不得为单句描述，必须为多句结构（至少四句话）
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

【格式要求（必须执行）】
1. 必须严格保留合同原有换行结构
2. 每个条款必须单独一行
3. 不得将多行文本压缩为一段
4. 不得删除换行符
5. 输出必须为标准合同排版（带换行）

==============================
【高亮标注规则（重要修改）】
在“参考优化合同”中：

1. 新增条款 → 绿色
2. 修改条款 → 红色
3. 未修改内容保持原样

❗重要约束：
- 不得为了标注而修改数据
- 不得改变字段值（参考字段保护规则）
- 仅对“必要法律优化”进行修改

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
8. 不得修改所有输入的字段内容，所有输入的字段值必须完全一致

（输出完整优化后的合同文本）

---修改摘要---
有风险需处理 / 无风险 
说明：[一句话总结主要问题]

==============================
【禁止生成空字段规则】

若原合同中某一行包含字段值、【待确认】或 ${xxx} 占位符，则该行在输出中必须完整保留。

禁止出现以下情况：
1. 原文有字段值，输出变为空
2. 原文有 ${xxx}，输出删除 ${xxx}
3. 原文有【待确认】，输出删除【待确认】
4. 同一个编号重复出现两次
5. 为了补充法律内容而新增空字段行

错误示例：
原文：1. 起运地：${origin}
错误：1. 起运地：

正确示例：
1. 起运地：${origin}

                    ==============================
                    【行业限定与法律来源（必须执行）】
                    
                    当前合同属于【物流运输合同】场景，必须遵守以下限定：
                    
                    一、行业限定（强制）
                    
                    1. 本合同仅适用于：
                       - 货物运输
                       - 仓储配送
                       - 物流履约相关业务
                    
                    2. 所有风险分析与条款优化：
                       必须围绕物流运输场景展开
                    
                    3. 不得输出与以下无关内容：
                       ❌ 劳动关系
                       ❌ 股权交易
                       ❌ 房屋买卖
                       ❌ 婚姻/继承
                       ❌ 金融投资（除运输费用外）
                    
                    二、法律来源限定（必须遵守）
                    
                    在进行风险分析和条款优化时，仅可依据：
                    
                    1. 《中华人民共和国民法典》
                       （重点：合同编 · 运输合同相关规定）
                    
                    2. 物流行业通用规则：
                       - 货物运输责任规则
                       - 承运人责任范围
                       - 货损、延误、灭失处理规则
                    
                    3. 国家相关法律法规（仅限运输领域）
                    
                    禁止：
                    ❌ 编造具体法条编号 \s
                    ❌ 引用无关法律（如劳动法、刑法等）
                    
                    三、法律表达要求（强化）
                    
                    在输出中：
                    
                    ✔ 可以使用：
                    - “根据《中华人民共和国民法典》相关规定”
                    - “依据运输合同相关法律规则”
                    
                    ❌ 禁止：
                    - 输出不存在的法律名称
                    - 虚构条文编号（如第123条）
                    
                    四、风险判断必须符合物流场景
                    
                    以下问题必须优先识别为风险：
                    
                    1. 运输责任不明确（如货损、延误）
                    2. 风险转移不清（起运地 → 目的地）
                    3. 交付/签收责任缺失
                    4. 运输时间不明确
                    5. 费用与结算方式不明确
                    
                    若未识别上述问题 → 视为不合格输出
                    
==============================
【字段行禁止追加规则（强制）】

以下类型的行属于“字段行”，必须严格保持原样，不得进行任何追加或修改：

1. 含有 ${xxx} 的行（如：发货日期：${deliveryDate}）
2. 含有【待确认】的行
3. 含有简单值的行（如：数量：45）
4. 所有“字段定义类行”（格式：xxx：value）

对于上述行：

❌ 禁止：
- 在末尾追加任何说明
- 拼接重复字段（如：发货日期：...发货日期：）
- 将字段扩展为句子
- 改变字段结构

✔ 必须：
- 完全逐字复制该行

==============================
【最终一致性校验（必须执行）】

在输出前必须检查：

1. 是否输出了完整合同？若是 → 重新生成
2. 是否修改了任何字段值？若是 → 重新生成
3. 是否包含未修改的内容？若是 → 删除，仅保留差异
4. 若用户输入的任意数字段发生变化 → 整体作废并重新生成
5. 必须逐行复制原合同字段内容，仅在条款描述中插入新增句子

==============================

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
                contextInfo = "系统数据：\n" + content.substring(0, Math.min(2500, content.length())) + "\n\n";
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
你是物流合同管理系统中的AI合同问答助手。

请严格基于提供的合同内容回答问题。

要求：
1. 不要编造不存在的信息
2. 用中文简洁回答
3. 如果合同中没有相关内容，直接说“合同中未找到相关内容”
4. 不要输出 markdown

【合同内容】
%s

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