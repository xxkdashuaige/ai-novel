package com.example.ainovel.script.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiStatusServiceTest {

    @Test
    void identifiesConfiguredDeepSeekProvider() {
        AiStatusService statusService = new AiStatusService(
                true,
                "sk-test",
                "https://api.deepseek.com",
                "deepseek-v4-flash"
        );

        AiStatusResponse status = statusService.status();

        assertThat(status.enabled()).isTrue();
        assertThat(status.apiKeyConfigured()).isTrue();
        assertThat(status.provider()).isEqualTo("DeepSeek");
        assertThat(status.model()).isEqualTo("deepseek-v4-flash");
        assertThat(status.message()).contains("DeepSeek");
    }

    @Test
    void reportsRuleBasedModeWhenAiIsDisabled() {
        AiStatusService statusService = new AiStatusService(
                false,
                "demo-key",
                "https://api.openai.com",
                "gpt-4.1-mini"
        );

        AiStatusResponse status = statusService.status();

        assertThat(status.enabled()).isFalse();
        assertThat(status.apiKeyConfigured()).isFalse();
        assertThat(status.provider()).isEqualTo("规则解析器");
        assertThat(status.message()).contains("规则");
    }
}
