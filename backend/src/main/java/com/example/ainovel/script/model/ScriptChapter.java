package com.example.ainovel.script.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ScriptChapter(
        @NotBlank String id,
        @NotBlank String title,
        @NotBlank String summary,
        @Valid @NotEmpty List<ScriptScene> scenes
) {
}
