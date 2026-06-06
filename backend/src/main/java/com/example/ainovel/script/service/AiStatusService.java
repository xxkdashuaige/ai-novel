package com.example.ainovel.script.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiStatusService {

    private final boolean enabled;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public AiStatusService(
            @Value("${app.ai.enabled:false}") boolean enabled,
            @Value("${spring.ai.openai.api-key:demo-key}") String apiKey,
            @Value("${spring.ai.openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${spring.ai.openai.chat.options.model:gpt-4.1-mini}") String model
    ) {
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    public AiStatusResponse status() {
        boolean configured = apiKey != null && !apiKey.isBlank() && !"demo-key".equals(apiKey);
        if (!enabled) {
            return new AiStatusResponse(false, configured, "规则解析器", model, "AI 未启用，当前使用规则解析器转换。");
        }
        if (!configured) {
            return new AiStatusResponse(true, false, provider(), model, "AI 已启用，但未配置 API Key，调用失败时会使用规则兜底。");
        }
        return new AiStatusResponse(true, true, provider(), model, provider() + " 已配置，转换会优先调用 AI。");
    }

    private String provider() {
        return baseUrl.toLowerCase().contains("deepseek") ? "DeepSeek" : "OpenAI Compatible";
    }
}
