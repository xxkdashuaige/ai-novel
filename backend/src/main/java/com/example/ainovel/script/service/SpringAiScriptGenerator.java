package com.example.ainovel.script.service;

import com.example.ainovel.script.model.ScriptDocument;
import com.example.ainovel.script.model.ScriptMetadata;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SpringAiScriptGenerator implements ScriptGenerator {

    private static final Logger log = LoggerFactory.getLogger(SpringAiScriptGenerator.class);

    private final ChatClient chatClient;
    private final RuleBasedScriptGenerator fallbackGenerator;
    private final boolean enabled;
    private final String baseUrl;

    public SpringAiScriptGenerator(
            ChatClient.Builder chatClientBuilder,
            RuleBasedScriptGenerator fallbackGenerator,
            @Value("${app.ai.enabled:false}") boolean enabled,
            @Value("${spring.ai.openai.base-url:https://api.openai.com}") String baseUrl
    ) {
        this.chatClient = chatClientBuilder.build();
        this.fallbackGenerator = fallbackGenerator;
        this.enabled = enabled;
        this.baseUrl = baseUrl;
    }

    @Override
    public ScriptDocument generate(String novelText) {
        if (!enabled) {
            return withGenerationMode(fallbackGenerator.generate(novelText), "RULE_BASED", "AI 未启用，已使用规则解析器转换。");
        }

        try {
            ScriptDocument document = chatClient.prompt()
                    .system("""
                            你是专业剧本统筹。请把小说转换为结构化剧本对象。
                            要求：保留至少三章；每章至少一个场景；beat.type 只能使用 dialogue、action、narration、transition。
                            不要输出 Markdown，不要解释，只返回可映射到 Java record 的结构化内容。
                            """)
                    .user(novelText)
                    .call()
                    .entity(ScriptDocument.class);
            return document == null
                    ? fallback(novelText, "AI 未返回有效内容，已使用规则解析器兜底。")
                    : withGenerationMode(document, aiGenerationMode(), aiGenerationMessage());
        } catch (RuntimeException ex) {
            log.warn("AI conversion failed, falling back to rule based generator: {}", ex.getMessage());
            return fallback(novelText, "AI 调用失败，已自动使用规则解析器兜底。");
        }
    }

    private ScriptDocument fallback(String novelText, String message) {
        return withGenerationMode(fallbackGenerator.generate(novelText), "RULE_BASED_FALLBACK", message);
    }

    private ScriptDocument withGenerationMode(ScriptDocument document, String generationMode, String generationMessage) {
        int chapterCount = document.chapters() == null ? 0 : document.chapters().size();
        return new ScriptDocument(
                document.title(),
                document.chapters(),
                new ScriptMetadata(chapterCount, generationMode, generationMessage)
        );
    }

    private String aiGenerationMode() {
        return baseUrl.toLowerCase().contains("deepseek") ? "AI_DEEPSEEK" : "AI_OPENAI_COMPATIBLE";
    }

    private String aiGenerationMessage() {
        return baseUrl.toLowerCase().contains("deepseek") ? "DeepSeek AI 转换成功。" : "OpenAI-compatible AI 转换成功。";
    }
}
