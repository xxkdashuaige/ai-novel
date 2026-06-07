package com.example.ainovel.script.controller;

import com.example.ainovel.script.model.ScriptBeat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SceneEditRequest(
        @NotBlank(message = "type 不能为空") String type,
        String sceneTitle,
        List<String> characters,
        @Valid @NotEmpty(message = "beats 不能为空") List<ScriptBeat> beats
) {
}
