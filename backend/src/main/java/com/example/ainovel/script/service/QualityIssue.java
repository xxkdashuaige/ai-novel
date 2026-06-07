package com.example.ainovel.script.service;

public record QualityIssue(
        String type,
        String severity,
        String message
) {
}
