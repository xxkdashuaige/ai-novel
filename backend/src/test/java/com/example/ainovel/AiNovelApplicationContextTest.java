package com.example.ainovel;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.history.file=target/test-history/context-history.json")
class AiNovelApplicationContextTest {

    @Test
    void contextLoads() {
    }
}
