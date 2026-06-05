package com.example.ainovel.script.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum BeatType {
    DIALOGUE("dialogue"),
    ACTION("action"),
    NARRATION("narration"),
    TRANSITION("transition");

    private final String yamlValue;

    BeatType(String yamlValue) {
        this.yamlValue = yamlValue;
    }

    @JsonValue
    public String yamlValue() {
        return yamlValue;
    }

    @JsonCreator
    public static BeatType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("beat.type 不能为空");
        }

        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value) || type.yamlValue.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的 beat.type: " + value));
    }
}
