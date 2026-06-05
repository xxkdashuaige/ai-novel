package com.example.ainovel.script.service;

import com.example.ainovel.script.model.ScriptDocument;
import org.springframework.stereotype.Service;

@Service
public class ScriptConversionService {

    private final SpringAiScriptGenerator generator;
    private final YamlScriptService yamlScriptService;
    private final ScriptValidationService validationService;

    public ScriptConversionService(
            SpringAiScriptGenerator generator,
            YamlScriptService yamlScriptService,
            ScriptValidationService validationService
    ) {
        this.generator = generator;
        this.yamlScriptService = yamlScriptService;
        this.validationService = validationService;
    }

    public ConversionResponse convert(String novelText) {
        ScriptDocument document = generator.generate(novelText);
        String yaml = yamlScriptService.toYaml(document);
        ValidationResult validation = validationService.validate(document);
        return new ConversionResponse(document, yaml, validation);
    }
}
