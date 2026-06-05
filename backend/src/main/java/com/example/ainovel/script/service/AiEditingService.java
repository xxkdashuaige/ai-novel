package com.example.ainovel.script.service;

import com.example.ainovel.script.controller.EditRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiEditingService {

    private final ChatClient chatClient;
    private final boolean enabled;
    private final String baseUrl;

    public AiEditingService(
            ChatClient.Builder chatClientBuilder,
            @Value("${app.ai.enabled:false}") boolean enabled,
            @Value("${spring.ai.openai.base-url:https://api.openai.com}") String baseUrl
    ) {
        this.chatClient = chatClientBuilder.build();
        this.enabled = enabled;
        this.baseUrl = baseUrl;
    }

    public EditResponse edit(EditRequest request) {
        if (!enabled) {
            return fallback(request);
        }

        try {
            String result = chatClient.prompt()
                    .system("""
                            你是专业剧本编辑。根据用户要求改写一条剧本内容。
                            只输出改写后的文本，不要输出解释、标题、Markdown 或 JSON。
                            台词要自然、有戏剧张力；动作要可拍、具体、简洁。
                            """)
                    .user(buildPrompt(request))
                    .call()
                    .content();
            if (result == null || result.isBlank()) {
                return fallback(request);
            }
            return new EditResponse(result.trim(), aiGenerationMode());
        } catch (RuntimeException ex) {
            return fallback(request);
        }
    }

    private String buildPrompt(EditRequest request) {
        String instruction = switch (request.type()) {
            case "polish-dialogue" -> "请优化这句台词，让它更符合人物情绪和短剧节奏。";
            case "expand-action" -> "请补充这段动作描写，让它更有画面感且适合拍摄。";
            default -> "请优化这条剧本内容，保持原意但提升表现力。";
        };
        return """
                %s
                场景：%s
                人物：%s
                说话人：%s
                原文：%s
                """.formatted(
                instruction,
                blankToDefault(request.sceneTitle(), "未标注"),
                request.characters() == null ? "未标注" : String.join("、", request.characters()),
                blankToDefault(request.speaker(), "无"),
                request.content()
        );
    }

    private EditResponse fallback(EditRequest request) {
        String result = switch (request.type()) {
            case "polish-dialogue" -> request.content() + "（语气更坚定，情绪更集中。）";
            case "expand-action" -> request.content() + " 角色停顿片刻，目光扫过四周，动作带出紧张感。";
            default -> request.content();
        };
        return new EditResponse(result, "RULE_BASED_FALLBACK");
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String aiGenerationMode() {
        return baseUrl.toLowerCase().contains("deepseek") ? "AI_DEEPSEEK" : "AI_OPENAI_COMPATIBLE";
    }
}
