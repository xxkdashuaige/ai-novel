package com.example.ainovel.script.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BeatTypeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsUppercaseAndLowercaseValues() throws Exception {
        assertThat(objectMapper.readValue("\"ACTION\"", BeatType.class)).isEqualTo(BeatType.ACTION);
        assertThat(objectMapper.readValue("\"action\"", BeatType.class)).isEqualTo(BeatType.ACTION);
        assertThat(objectMapper.readValue("\"DIALOGUE\"", BeatType.class)).isEqualTo(BeatType.DIALOGUE);
        assertThat(objectMapper.readValue("\"dialogue\"", BeatType.class)).isEqualTo(BeatType.DIALOGUE);
    }
}
