package com.example.ainovel.script.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ScriptBeat(
        @NotNull BeatType type,
        String speaker,
        @NotBlank String content
) {
}
