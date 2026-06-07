package com.example.ainovel.script.service;

import com.example.ainovel.script.model.ScriptDocument;

public record ScriptHistoryItem(
        String id,
        String title,
        String createdAt,
        String generationMode,
        String generationMessage,
        int chapterCount,
        int sceneCount,
        ScriptDocument script,
        String yaml,
        ValidationResult validation,
        QualityReport quality
) {
    public ScriptHistoryItem(
            String id,
            String title,
            String createdAt,
            String generationMode,
            String generationMessage,
            int chapterCount,
            int sceneCount,
            ScriptDocument script,
            String yaml,
            ValidationResult validation
    ) {
        this(id, title, createdAt, generationMode, generationMessage, chapterCount, sceneCount, script, yaml, validation, null);
    }
}
