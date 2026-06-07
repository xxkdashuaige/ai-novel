package com.example.ainovel.script.service;

import com.example.ainovel.script.model.ScriptBeat;

import java.util.List;

public record SceneEditResponse(
        List<ScriptBeat> beats,
        String generationMode
) {
}
