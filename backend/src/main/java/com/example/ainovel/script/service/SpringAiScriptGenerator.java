package com.example.ainovel.script.service;

import com.example.ainovel.script.model.ScriptDocument;
import com.example.ainovel.script.model.ScriptMetadata;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SpringAiScriptGenerator implements ScriptGenerator {

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
            return fallbackGenerator.generate(novelText);
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
            return document == null ? fallback(novelText) : withGenerationMode(document, aiGenerationMode());
        } catch (RuntimeException ex) {
            return fallback(novelText);
        }
    }

    private ScriptDocument fallback(String novelText) {
        return withGenerationMode(fallbackGenerator.generate(novelText), "RULE_BASED_FALLBACK");
    }

    private ScriptDocument withGenerationMode(ScriptDocument document, String generationMode) {
        int chapterCount = document.chapters() == null ? 0 : document.chapters().size();
        return new ScriptDocument(
                document.title(),
                document.chapters(),
                new ScriptMetadata(chapterCount, generationMode)
        );
    }

    private String aiGenerationMode() {
        return baseUrl.toLowerCase().contains("deepseek") ? "AI_DEEPSEEK" : "AI_OPENAI_COMPATIBLE";
    }
}
