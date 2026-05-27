package com.exam.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.exam.dto.AiGenerateRequestDto;
import com.exam.dto.QuestionImportDto;
import com.exam.service.KimiAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class KimiAiServiceImpl implements KimiAiService {

    @Value("${deepseek.api.api-key:}")
    private String deepseekApiKey;

    @Value("${deepseek.api.base-url:https://api.deepseek.com/v1}")
    private String deepseekBaseUrl;

    @Value("${deepseek.api.model:deepseek-chat}")
    private String deepseekModel;

    @Value("${deepseek.api.max-tokens:4000}")
    private Integer deepseekMaxTokens;

    @Value("${deepseek.api.temperature:0.7}")
    private Double deepseekTemperature;

    private final WebClient webClient;

    public KimiAiServiceImpl() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public List<QuestionImportDto> generateQuestions(AiGenerateRequestDto request) {
        try {
            log.info("AI题目生成服务启动:");
            log.info("DeepSeek API配置检查:");
            log.info("deepseekApiKey: {}", deepseekApiKey != null && !deepseekApiKey.isEmpty() ? "已配置" : "未配置");
            log.info("deepseekBaseUrl: {}", deepseekBaseUrl);
            log.info("deepseekModel: {}", deepseekModel);

            String prompt = buildPrompt(request);
            String response = callDeepSeekApi(prompt);
            return parseResponse(response, request);

        } catch (Exception e) {
            log.error("调用DeepSeek API生成题目失败", e);
            throw new RuntimeException("AI生成题目失败: " + e.getMessage());
        }
    }

    private String buildPrompt(AiGenerateRequestDto request) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("请为我生成").append(request.getCount()).append("道关于【")
              .append(request.getTopic()).append("】的题目。\n\n");

        prompt.append("要求：\n");

        if (request.getTypes() != null && !request.getTypes().isEmpty()) {
            List<String> typeList = Arrays.asList(request.getTypes().split(","));
            prompt.append("- 题目类型：");
            for (String type : typeList) {
                switch (type.trim()) {
                    case "CHOICE":
                        prompt.append("选择题");
                        if (request.getIncludeMultiple() != null && request.getIncludeMultiple()) {
                            prompt.append("(包含单选和多选)");
                        }
                        prompt.append(" ");
                        break;
                    case "JUDGE":
                        prompt.append("判断题（**重要：确保正确答案和错误答案的数量大致平衡，不要全部都是正确或错误**） ");
                        break;
                    case "TEXT":
                        prompt.append("简答题 ");
                        break;
                }
            }
            prompt.append("\n");
        }

        if (request.getDifficulty() != null) {
            String difficultyText = switch (request.getDifficulty()) {
                case "EASY" -> "简单";
                case "MEDIUM" -> "中等";
                case "HARD" -> "困难";
                default -> "中等";
            };
            prompt.append("- 难度等级：").append(difficultyText).append("\n");
        }

        if (request.getRequirements() != null && !request.getRequirements().isEmpty()) {
            prompt.append("- 特殊要求：").append(request.getRequirements()).append("\n");
        }

        if (request.getTypes() != null && request.getTypes().contains("JUDGE")) {
            prompt.append("- **判断题特别要求**：\n");
            prompt.append("  * 确保生成的判断题中，正确答案(TRUE)和错误答案(FALSE)的数量尽量平衡\n");
            prompt.append("  * 不要所有判断题都是正确的或都是错误的\n");
            prompt.append("  * 错误的陈述应该是常见的误解或容易混淆的概念\n");
            prompt.append("  * 正确的陈述应该是重要的基础知识点\n");
        }

        prompt.append("\n请严格按照以下JSON格式返回，不要包含任何其他文字：\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"questions\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"title\": \"题目内容\",\n");
        prompt.append("      \"type\": \"CHOICE|JUDGE|TEXT\",\n");
        prompt.append("      \"multi\": true/false,\n");
        prompt.append("      \"difficulty\": \"EASY|MEDIUM|HARD\",\n");
        prompt.append("      \"score\": 5,\n");
        prompt.append("      \"choices\": [\n");
        prompt.append("        {\"content\": \"选项内容\", \"isCorrect\": true/false, \"sort\": 1}\n");
        prompt.append("      ],\n");
        prompt.append("      \"answer\": \"TRUE或FALSE(判断题专用)|文本答案(简答题专用)\",\n");
        prompt.append("      \"analysis\": \"题目解析\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```\n\n");

        prompt.append("注意：\n");
        prompt.append("1. 选择题必须有choices数组，判断题和简答题设置answer字段\n");
        prompt.append("2. 多选题的multi字段设为true，单选题设为false\n");
        prompt.append("3. **判断题的answer字段只能是\"TRUE\"或\"FALSE\"，请确保答案分布合理**\n");
        prompt.append("4. 每道题都要有详细的解析\n");
        prompt.append("5. 题目要有实际价值，贴近实际应用场景\n");
        prompt.append("6. 严格按照JSON格式返回，确保可以正确解析\n");

        if (request.getTypes() != null && request.getTypes().equals("JUDGE") && request.getCount() > 1) {
            prompt.append("7. **判断题答案分布要求**：在").append(request.getCount()).append("道判断题中，");
            int halfCount = request.getCount() / 2;
            if (request.getCount() % 2 == 0) {
                prompt.append("请生成").append(halfCount).append("道正确(TRUE)和").append(halfCount).append("道错误(FALSE)的题目");
            } else {
                prompt.append("请生成约").append(halfCount).append("-").append(halfCount + 1).append("道正确(TRUE)和约").append(halfCount).append("-").append(halfCount + 1).append("道错误(FALSE)的题目");
            }
        }

        return prompt.toString();
    }

    private String callDeepSeekApi(String prompt) {
        if (deepseekApiKey == null || deepseekApiKey.isEmpty()) {
            throw new RuntimeException("未配置DeepSeek API密钥，请在配置文件中设置deepseek.api.api-key");
        }

        log.info("开始调用DeepSeek API生成题目...");

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", deepseekModel);
        requestBody.put("max_tokens", deepseekMaxTokens != null ? deepseekMaxTokens : 4000);
        requestBody.put("temperature", deepseekTemperature != null ? deepseekTemperature : 0.7);

        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);
        requestBody.put("messages", messages);

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("第{}次尝试调用DeepSeek API", attempt);

                Mono<String> responseMono = webClient.post()
                        .uri(deepseekBaseUrl + "/chat/completions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + deepseekApiKey)
                        .bodyValue(requestBody.toJSONString())
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(120));

                String response = responseMono.block();
                log.info("DeepSeek API调用成功，响应长度: {}", response != null ? response.length() : 0);

                JSONObject responseJson = JSON.parseObject(response);
                if (responseJson.containsKey("error")) {
                    JSONObject error = responseJson.getJSONObject("error");
                    String errorMessage = error.getString("message");
                    log.error("DeepSeek API返回错误: {}", errorMessage);
                    if (errorMessage.contains("rate limit") || errorMessage.contains("too many requests")) {
                        if (attempt < maxRetries) {
                            log.info("遇到限流，等待{}秒后重试...", attempt * 5);
                            Thread.sleep(attempt * 5000);
                            continue;
                        }
                    }
                    throw new RuntimeException("DeepSeek API错误: " + errorMessage);
                }

                JSONArray choices = responseJson.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    String content = choices.getJSONObject(0).getJSONObject("message").getString("content");
                    log.info("AI生成内容获取成功，内容长度: {}", content.length());
                    return content;
                } else {
                    throw new RuntimeException("DeepSeek API返回的响应格式不正确");
                }

            } catch (Exception e) {
                log.error("第{}次调用DeepSeek API失败: {}", attempt, e.getMessage());

                if (attempt == maxRetries) {
                    if (e.getMessage().contains("timeout")) {
                        throw new RuntimeException("AI服务响应超时，请稍后重试。如果问题持续存在，建议减少生成题目数量或简化需求描述。");
                    } else if (e.getMessage().contains("rate limit")) {
                        throw new RuntimeException("AI服务请求过于频繁，请稍后再试。");
                    } else if (e.getMessage().contains("unauthorized") || e.getMessage().contains("invalid")) {
                        throw new RuntimeException("AI服务认证失败，请检查API密钥配置。");
                    } else {
                        throw new RuntimeException("AI服务暂时不可用: " + e.getMessage() + "。请稍后重试或联系管理员。");
                    }
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("AI生成被中断");
                }
            }
        }

        throw new RuntimeException("AI生成题目失败，已重试" + maxRetries + "次");
    }

    private List<QuestionImportDto> parseResponse(String response, AiGenerateRequestDto request) {
        List<QuestionImportDto> questions = new ArrayList<>();

        try {
            String jsonContent = extractJsonFromResponse(response);

            JSONObject jsonResponse = JSON.parseObject(jsonContent);
            JSONArray questionsArray = jsonResponse.getJSONArray("questions");

            for (int i = 0; i < questionsArray.size(); i++) {
                JSONObject questionJson = questionsArray.getJSONObject(i);
                QuestionImportDto question = new QuestionImportDto();

                question.setTitle(questionJson.getString("title"));
                question.setType(questionJson.getString("type"));
                question.setMulti(questionJson.getBoolean("multi"));
                question.setDifficulty(questionJson.getString("difficulty"));
                question.setScore(questionJson.getInteger("score"));
                question.setAnalysis(questionJson.getString("analysis"));
                question.setCategoryId(request.getCategoryId());

                if ("CHOICE".equals(question.getType()) && questionJson.containsKey("choices")) {
                    JSONArray choicesArray = questionJson.getJSONArray("choices");
                    List<QuestionImportDto.ChoiceImportDto> choices = new ArrayList<>();

                    for (int j = 0; j < choicesArray.size(); j++) {
                        JSONObject choiceJson = choicesArray.getJSONObject(j);
                        QuestionImportDto.ChoiceImportDto choice = new QuestionImportDto.ChoiceImportDto();
                        choice.setContent(choiceJson.getString("content"));
                        choice.setIsCorrect(choiceJson.getBoolean("isCorrect"));
                        choice.setSort(choiceJson.getInteger("sort"));
                        choices.add(choice);
                    }
                    question.setChoices(choices);
                } else {
                    String rawAnswer = questionJson.getString("answer");

                    if ("JUDGE".equals(question.getType()) && rawAnswer != null) {
                        if ("正确".equals(rawAnswer) || "对".equals(rawAnswer) || "TRUE".equalsIgnoreCase(rawAnswer)) {
                            rawAnswer = "TRUE";
                        } else if ("错误".equals(rawAnswer) || "错".equals(rawAnswer) || "FALSE".equalsIgnoreCase(rawAnswer)) {
                            rawAnswer = "FALSE";
                        }
                        if (!"TRUE".equals(rawAnswer) && !"FALSE".equals(rawAnswer)) {
                            log.warn("AI生成的判断题答案格式不标准: {}, 题目: {}", rawAnswer, question.getTitle());
                        }
                    }

                    question.setAnswer(rawAnswer);
                }

                questions.add(question);
            }

        } catch (Exception e) {
            log.error("解析AI响应失败", e);
            throw new RuntimeException("解析AI生成的题目失败: " + e.getMessage());
        }

        return questions;
    }

    private String extractJsonFromResponse(String response) {
        int startIndex = response.indexOf("```json");
        int endIndex = response.lastIndexOf("```");

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return response.substring(startIndex + 7, endIndex).trim();
        }

        startIndex = response.indexOf("{");
        endIndex = response.lastIndexOf("}");

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1).trim();
        }

        throw new RuntimeException("无法从AI响应中提取JSON内容");
    }
}