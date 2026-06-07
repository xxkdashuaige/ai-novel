package com.example.ainovel.script.service;

import com.example.ainovel.script.model.BeatType;
import com.example.ainovel.script.model.ScriptBeat;
import com.example.ainovel.script.model.ScriptChapter;
import com.example.ainovel.script.model.ScriptDocument;
import com.example.ainovel.script.model.ScriptScene;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ScriptQualityService {

    public QualityReport analyze(ScriptDocument document, ValidationResult validation) {
        List<QualityIssue> issues = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        addValidationIssues(validation, issues, suggestions);
        addStructureIssues(document, issues, suggestions);

        int score = Math.max(0, 100 - issues.stream().mapToInt(this::deduction).sum());
        String level = score >= 90 ? "优秀" : score >= 75 ? "良好" : score > 60 ? "需关注" : "需修复";
        if (issues.isEmpty()) {
            suggestions.add("结构完整，可以继续进行场景细化和导出。");
        }
        return new QualityReport(score, level, List.copyOf(issues), suggestions.stream().distinct().toList());
    }

    private void addValidationIssues(ValidationResult validation, List<QualityIssue> issues, List<String> suggestions) {
        if (validation == null) {
            return;
        }
        validation.errors().forEach(error -> issues.add(new QualityIssue("SCHEMA_ERROR", "error", error)));
        validation.warnings().forEach(warning -> issues.add(new QualityIssue("CONSISTENCY_WARNING", "warning", warning)));
        if (!validation.errors().isEmpty()) {
            suggestions.add("先修复 Schema 错误，再进行导出或 AI 编辑。");
        }
        if (!validation.warnings().isEmpty()) {
            suggestions.add("建议点击一键修复一致性，补全场景人物列表。");
        }
    }

    private void addStructureIssues(ScriptDocument document, List<QualityIssue> issues, List<String> suggestions) {
        if (document == null || document.chapters() == null) {
            issues.add(new QualityIssue("EMPTY_SCRIPT", "error", "剧本结构为空，无法评估质量。"));
            suggestions.add("重新输入三章以上小说内容后再转换。");
            return;
        }
        if (document.chapters().size() < 3) {
            issues.add(new QualityIssue("CHAPTER_COUNT", "error", "章节数量少于 3 章。"));
            suggestions.add("补充小说章节内容，转换结果会更稳定。");
        }
        for (ScriptChapter chapter : document.chapters()) {
            if (chapter.scenes() == null || chapter.scenes().isEmpty()) {
                issues.add(new QualityIssue("EMPTY_SCENE", "error", chapter.title() + " 没有可展示场景。"));
                continue;
            }
            for (ScriptScene scene : chapter.scenes()) {
                inspectScene(scene, issues, suggestions);
            }
        }
    }

    private void inspectScene(ScriptScene scene, List<QualityIssue> issues, List<String> suggestions) {
        if (isBlank(scene.location()) || "未知地点".equals(scene.location()) || "未标注".equals(scene.location())) {
            issues.add(new QualityIssue("MISSING_LOCATION", "warning", scene.id() + " 缺少明确地点。"));
            suggestions.add("为每个场景补充地点，方便生成分镜和拍摄计划。");
        }
        if (isBlank(scene.timeOfDay()) || "未知时间".equals(scene.timeOfDay()) || "未标注".equals(scene.timeOfDay())) {
            issues.add(new QualityIssue("MISSING_TIME", "warning", scene.id() + " 缺少明确时间。"));
            suggestions.add("为每个场景补充时间，提升剧本可拍摄性。");
        }
        Set<String> characters = new HashSet<>(scene.characters() == null ? List.of() : scene.characters());
        if (characters.isEmpty()) {
            issues.add(new QualityIssue("EMPTY_CHARACTERS", "warning", scene.id() + " 没有列出登场人物。"));
            suggestions.add("补全场景人物列表，角色关系图会更准确。");
        }
        if (scene.beats() == null || scene.beats().isEmpty()) {
            issues.add(new QualityIssue("EMPTY_BEATS", "error", scene.id() + " 没有剧本节拍。"));
            return;
        }
        for (ScriptBeat beat : scene.beats()) {
            if (isBlank(beat.content())) {
                issues.add(new QualityIssue("EMPTY_BEAT_CONTENT", "error", scene.id() + " 存在空白节拍内容。"));
            }
            if (beat.type() == BeatType.DIALOGUE && isBlank(beat.speaker())) {
                issues.add(new QualityIssue("MISSING_SPEAKER", "warning", scene.id() + " 存在未标注说话人的对白。"));
                suggestions.add("为对白补充说话人，便于后续 AI 编辑和演员阅读。");
            }
        }
    }

    private int deduction(QualityIssue issue) {
        return "error".equals(issue.severity()) ? 40 : 8;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
