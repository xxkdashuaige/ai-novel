package com.example.ainovel.script.service;

public record AiStatusResponse(
        boolean enabled,
        boolean apiKeyConfigured,
        String provider,
        String model,
        String message
) {
}
