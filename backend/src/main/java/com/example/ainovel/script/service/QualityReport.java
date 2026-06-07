package com.example.ainovel.script.service;

import java.util.List;

public record QualityReport(
        int score,
        String level,
        List<QualityIssue> issues,
        List<String> suggestions
) {
}
