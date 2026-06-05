package com.example.ainovel.script.controller;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record EditRequest(
        @NotBlank(message = "type 不能为空") String type,
        @NotBlank(message = "content 不能为空") String content,
        String speaker,
        String sceneTitle,
        List<String> characters
) {
}
