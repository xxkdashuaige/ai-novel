package com.example.ainovel.script.service;

import com.example.ainovel.script.model.BeatType;
import com.example.ainovel.script.model.ScriptBeat;
import com.example.ainovel.script.model.ScriptChapter;
import com.example.ainovel.script.model.ScriptDocument;
import com.example.ainovel.script.model.ScriptMetadata;
import com.example.ainovel.script.model.ScriptScene;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptQualityServiceTest {

    private final ScriptQualityService qualityService = new ScriptQualityService();

    @Test
    void reportsHighScoreForCompleteScript() {
        QualityReport report = qualityService.analyze(completeScript(), new ValidationResult(true, List.of(), List.of()));

        assertThat(report.score()).isEqualTo(100);
        assertThat(report.level()).isEqualTo("优秀");
        assertThat(report.issues()).isEmpty();
        assertThat(report.suggestions()).contains("结构完整，可以继续进行场景细化和导出。");
    }

    @Test
    void deductsScoreForValidationWarningsAndMissingSceneMetadata() {
        ScriptDocument script = new ScriptDocument(
                "测试剧本",
                List.of(
                        chapter("chapter-1", "第一章", scene("scene-1", "", "", List.of("林夏"), List.of(
                                new ScriptBeat(BeatType.DIALOGUE, "顾沉", "你来了。")
                        ))),
                        chapter("chapter-2", "第二章", scene("scene-2", "旧城", "夜晚", List.of("顾沉"), List.of(
                                new ScriptBeat(BeatType.ACTION, null, "顾沉推门。")
                        ))),
                        chapter("chapter-3", "第三章", scene("scene-3", "天台", "黎明", List.of("林夏"), List.of(
                                new ScriptBeat(BeatType.NARRATION, null, "风声渐起。")
                        )))
                ),
                new ScriptMetadata(3, "AI_DEEPSEEK", "DeepSeek AI 转换成功。")
        );

        QualityReport report = qualityService.analyze(script, new ValidationResult(
                true,
                List.of(),
                List.of("scene-1 的对白人物未列入场景 characters：顾沉")
        ));

        assertThat(report.score()).isLessThan(100);
        assertThat(report.issues()).extracting(QualityIssue::type)
                .contains("CONSISTENCY_WARNING", "MISSING_LOCATION", "MISSING_TIME");
        assertThat(report.suggestions()).anyMatch(suggestion -> suggestion.contains("一键修复一致性"));
    }

    @Test
    void marksInvalidValidationErrorsAsBlockingQualityIssues() {
        QualityReport report = qualityService.analyze(completeScript(), new ValidationResult(
                false,
                List.of("chapters 至少需要 3 章"),
                List.of()
        ));

        assertThat(report.score()).isLessThanOrEqualTo(60);
        assertThat(report.level()).isEqualTo("需修复");
        assertThat(report.issues()).extracting(QualityIssue::type).contains("SCHEMA_ERROR");
    }

    private ScriptDocument completeScript() {
        return new ScriptDocument(
                "完整剧本",
                List.of(
                        chapter("chapter-1", "第一章", scene("scene-1", "雨夜街口", "夜晚", List.of("林夏"), List.of(
                                new ScriptBeat(BeatType.DIALOGUE, "林夏", "我们马上走。")
                        ))),
                        chapter("chapter-2", "第二章", scene("scene-2", "旧书店", "傍晚", List.of("顾沉"), List.of(
                                new ScriptBeat(BeatType.ACTION, null, "顾沉合上旧书。")
                        ))),
                        chapter("chapter-3", "第三章", scene("scene-3", "天台", "黎明", List.of("林夏", "顾沉"), List.of(
                                new ScriptBeat(BeatType.NARRATION, null, "天光照亮两人的影子。")
                        )))
                ),
                new ScriptMetadata(3, "AI_DEEPSEEK", "DeepSeek AI 转换成功。")
        );
    }

    private ScriptChapter chapter(String id, String title, ScriptScene scene) {
        return new ScriptChapter(id, title, title + "摘要", List.of(scene));
    }

    private ScriptScene scene(String id, String location, String timeOfDay, List<String> characters, List<ScriptBeat> beats) {
        return new ScriptScene(id, id, location, timeOfDay, characters, beats);
    }
}
