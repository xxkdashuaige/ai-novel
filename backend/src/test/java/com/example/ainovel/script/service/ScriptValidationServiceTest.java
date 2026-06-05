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
        assertThat(result.warnings()).isEmpty();
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

    @Test
    void warnsWhenDialogueSpeakerIsMissingFromSceneCharacters() {
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
                        characters:
                          - 林舟
                        beats:
                          - type: dialogue
                            speaker: 苏晚
                            content: 你听见了吗？
                  - id: chapter-2
                    title: 第二章
                    summary: 摘要
                    scenes:
                      - id: scene-2
                        title: 场景 2
                        location: 未知地点
                        timeOfDay: 未知时间
                        characters:
                          - 苏晚
                        beats:
                          - type: action
                            speaker:
                            content: 苏晚转身离开。
                  - id: chapter-3
                    title: 第三章
                    summary: 摘要
                    scenes:
                      - id: scene-3
                        title: 场景 3
                        location: 未知地点
                        timeOfDay: 未知时间
                        characters:
                          - 林舟
                        beats:
                          - type: narration
                            speaker:
                            content: 夜色渐深。
                metadata:
                  sourceChapterCount: 3
                  generationMode: AI_DEEPSEEK
                """;

        ValidationResult result = validationService.validateYaml(yaml);

        assertThat(result.valid()).isTrue();
        assertThat(result.warnings()).contains("scene-1 的对白人物未列入场景 characters：苏晚");
    }
}
