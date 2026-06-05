package com.example.ainovel.script.service;

import com.example.ainovel.script.model.ScriptChapter;
import com.example.ainovel.script.model.ScriptDocument;
import com.example.ainovel.script.model.ScriptScene;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
            return new ValidationResult(false, List.of(e.getMessage()));
        }
    }

    public ValidationResult validate(ScriptDocument document) {
        List<String> errors = new ArrayList<>();
        if (document.title() == null || document.title().isBlank()) {
            errors.add("title 不能为空");
        }
        if (document.chapters() == null || document.chapters().size() < 3) {
            errors.add("chapters 至少需要 3 章");
            return new ValidationResult(false, errors);
        }
        for (ScriptChapter chapter : document.chapters()) {
            if (chapter.scenes() == null || chapter.scenes().isEmpty()) {
                errors.add(chapter.id() + " 至少需要 1 个 scene");
                continue;
            }
            for (ScriptScene scene : chapter.scenes()) {
                if (scene.beats() == null || scene.beats().isEmpty()) {
                    errors.add(scene.id() + " 至少需要 1 个 beat");
                }
            }
        }
        return new ValidationResult(errors.isEmpty(), errors);
    }
}
