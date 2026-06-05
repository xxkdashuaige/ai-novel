package com.example.ainovel.script.service;

import com.example.ainovel.script.model.ScriptChapter;
import com.example.ainovel.script.model.ScriptDocument;
import com.example.ainovel.script.model.ScriptScene;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ScriptValidationService {

    private final YamlScriptService yamlScriptService;

    public ScriptValidationService() {
        this(new YamlScriptService());
    }

    public ScriptValidationService(YamlScriptService yamlScriptService) {
        this.yamlScriptService = yamlScriptService;
    }

    public ValidationResult validateYaml(String yaml) {
        try {
            return validate(yamlScriptService.fromYaml(yaml));
        } catch (IllegalArgumentException e) {
            return new ValidationResult(false, List.of(e.getMessage()), List.of());
        }
    }

    public ValidationResult validate(ScriptDocument document) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (document.title() == null || document.title().isBlank()) {
            errors.add("title 不能为空");
        }
        if (document.chapters() == null || document.chapters().size() < 3) {
            errors.add("chapters 至少需要 3 章");
            return new ValidationResult(false, errors, warnings);
        }

        Set<String> chapterIds = new HashSet<>();
        Set<String> sceneIds = new HashSet<>();
        for (ScriptChapter chapter : document.chapters()) {
            if (!chapterIds.add(chapter.id())) {
                errors.add("章节编号重复：" + chapter.id());
            }
            if (chapter.scenes() == null || chapter.scenes().isEmpty()) {
                errors.add(chapter.id() + " 至少需要 1 个 scene");
                continue;
            }
            for (ScriptScene scene : chapter.scenes()) {
                if (!sceneIds.add(scene.id())) {
                    errors.add("场景编号重复：" + scene.id());
                }
                if (scene.beats() == null || scene.beats().isEmpty()) {
                    errors.add(scene.id() + " 至少需要 1 个 beat");
                    continue;
                }
                Set<String> sceneCharacters = new HashSet<>(scene.characters() == null ? List.of() : scene.characters());
                scene.beats().stream()
                        .filter(beat -> beat.speaker() != null && !beat.speaker().isBlank())
                        .filter(beat -> !sceneCharacters.contains(beat.speaker()))
                        .forEach(beat -> warnings.add(scene.id() + " 的对白人物未列入场景 characters：" + beat.speaker()));
                if (sceneCharacters.isEmpty()) {
                    warnings.add(scene.id() + " 没有列出登场人物");
                }
            }
        }
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
}
