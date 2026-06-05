package com.example.ainovel.script.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ScriptScene(
        @NotBlank String id,
        @NotBlank String title,
        @NotBlank String location,
        @NotBlank String timeOfDay,
        List<String> characters,
        @Valid @NotEmpty List<ScriptBeat> beats
) {
}
