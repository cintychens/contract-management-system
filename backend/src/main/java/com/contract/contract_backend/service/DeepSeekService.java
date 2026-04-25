package com.contract.contract_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Service
public class DeepSeekService {

    @Value("${deepseek.api-key}")
    private String apiKey;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateWithTemplate(Map<String, Object> data, String templateType) {

        try {

            // ⭐⭐⭐ 优先判断：是否是“草稿优化模式”
            String content = (String) data.get("content");

            String prompt;

            if (content != null && !content.isBlank()) {

                // ===== 模式1：AI优化草稿（你现在用的）=====
                prompt = """
请基于以下合同草稿进行优化并生成完整合同：

%s

要求：
1. 补充完整条款（违约责任、争议解决等）
2. 语言正式，符合中国法律合同规范
3. 条款结构清晰
""".formatted(content);

            } else {

                // ===== 模式2：字段生成合同（备用）=====
                StringBuilder sb = new StringBuilder();

                sb.append("请生成一份").append(templateType).append("：\n\n");

                for (Map.Entry<String, Object> entry : data.entrySet()) {

                    if ("templateType".equals(entry.getKey())) continue;
                    if (entry.getValue() == null) continue;

                    sb.append(entry.getKey())
                            .append("：")
                            .append(entry.getValue())
                            .append("\n");
                }

                sb.append("""
                        
要求：
1. 必须包含：合同主体、服务内容、费用条款、双方责任、违约责任、争议解决
2. 语言正式，符合中国法律合同规范
3. 条款完整清晰
""");

                prompt = sb.toString();
            }

            // ⭐⭐⭐ 使用Jackson构造JSON（避免转义问题）
            Map<String, Object> requestBody = Map.of(
                    "model", "deepseek-chat",
                    "messages", new Object[]{
                            Map.of("role", "system", "content", "你是一个专业的法律合同生成助手"),
                            Map.of("role", "user", "content", prompt)
                    },
                    "temperature", 0.7
            );

            String body = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.deepseek.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            System.out.println("🔥 AI调用中...");

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            String res = response.body();

            // ⭐⭐⭐ 更稳的解析方式（不会截断）
            JsonNode root = objectMapper.readTree(res);
            String result = root
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return "AI生成失败：" + e.getMessage();
        }
    }
}