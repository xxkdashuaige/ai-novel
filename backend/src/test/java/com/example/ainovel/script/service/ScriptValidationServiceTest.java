package com.example.ainovel.script.service;

import com.example.ainovel.script.model.ScriptDocument;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptValidationServiceTest {

    private final RuleBasedScriptGenerator generator = new RuleBasedScriptGenerator();
    private final ScriptValidationService validationService = new ScriptValidationService();
    private final YamlScriptService yamlScriptService = new YamlScriptService();

    @Test
    void validatesGeneratedYaml() {
        ScriptDocument document = generator.generate(RuleBasedScriptGeneratorTest.sampleNovel());
        String yaml = yamlScriptService.toYaml(document);

        ValidationResult result = validationService.validateYaml(yaml);

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void rejectsYamlWithTooFewChapters() {
        String yaml = """
                title: 示例
                chapters:
                  - id: chapter-1
                    title: 第一章
                    summary: 摘要
                    scenes:
                      - id: scene-1
                        title: 场景 1
                        location: 未知地点
                        timeOfDay: 未知时间
                        characters: []
                        beats:
                          - type: action
                            speaker:
                            content: 开场动作。
                metadata:
                  sourceChapterCount: 1
                  generationMode: RULE_BASED
                """;

        ValidationResult result = validationService.validateYaml(yaml);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).contains("chapters 至少需要 3 章");
    }
}
