package com.example.ainovel.script.service;

import com.example.ainovel.script.model.ScriptDocument;

public record ConversionResponse(
        ScriptDocument script,
        String yaml,
        ValidationResult validation,
        QualityReport quality
) {
    public ConversionResponse(ScriptDocument script, String yaml, ValidationResult validation) {
        this(script, yaml, validation, new QualityReport(100, "优秀", java.util.List.of(), java.util.List.of("结构完整，可以继续进行场景细化和导出。")));
    }
}
