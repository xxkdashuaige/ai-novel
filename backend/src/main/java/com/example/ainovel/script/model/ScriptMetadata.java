package com.example.ainovel.script.model;

public record ScriptMetadata(
        int sourceChapterCount,
        String generationMode,
        String generationMessage
) {
    public ScriptMetadata(int sourceChapterCount, String generationMode) {
        this(sourceChapterCount, generationMode, "");
    }
}
