package com.example.ainovel.script.service;

import com.example.ainovel.script.model.ScriptDocument;
import org.springframework.stereotype.Service;

@Service
public class ScriptConversionService {

    private final SpringAiScriptGenerator generator;
    private final YamlScriptService yamlScriptService;
    private final ScriptValidationService validationService;
    private final ScriptQualityService qualityService;

    public ScriptConversionService(
            SpringAiScriptGenerator generator,
            YamlScriptService yamlScriptService,
            ScriptValidationService validationService,
            ScriptQualityService qualityService
    ) {
        this.generator = generator;
        this.yamlScriptService = yamlScriptService;
        this.validationService = validationService;
        this.qualityService = qualityService;
    }

    public ConversionResponse convert(String novelText) {
        ScriptDocument document = generator.generate(novelText);
        return render(document);
    }

    public ConversionResponse render(ScriptDocument document) {
        String yaml = yamlScriptService.toYaml(document);
        ValidationResult validation = validationService.validate(document);
        QualityReport quality = qualityService.analyze(document, validation);
        return new ConversionResponse(document, yaml, validation, quality);
    }
}
