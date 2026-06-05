package com.example.ainovel.script.service;

import com.example.ainovel.script.model.ScriptDocument;

public record ConversionResponse(
        ScriptDocument script,
        String yaml,
        ValidationResult validation
) {
}
