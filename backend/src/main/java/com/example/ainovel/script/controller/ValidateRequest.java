package com.example.ainovel.script.controller;

import jakarta.validation.constraints.NotBlank;

public record ValidateRequest(
        @NotBlank(message = "yaml 不能为空") String yaml
) {
}
