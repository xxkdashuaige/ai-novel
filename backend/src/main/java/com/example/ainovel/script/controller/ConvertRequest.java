package com.example.ainovel.script.controller;

import jakarta.validation.constraints.NotBlank;

public record ConvertRequest(
        @NotBlank(message = "novelText 不能为空") String novelText
) {
}
