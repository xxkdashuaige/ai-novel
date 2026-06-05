package com.example.ainovel.script.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ScriptDocument(
        @NotBlank String title,
        @Valid @Size(min = 3, message = "chapters 至少需要 3 章") List<ScriptChapter> chapters,
        ScriptMetadata metadata
) {
}
