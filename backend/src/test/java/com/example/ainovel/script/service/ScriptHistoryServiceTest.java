package com.example.ainovel.script.service;

import com.example.ainovel.script.model.ScriptDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptHistoryServiceTest {

    private Path historyFile;

    @BeforeEach
    void setUp() throws IOException {
        Path historyDir = Path.of("target", "test-history");
        Files.createDirectories(historyDir);
        historyFile = historyDir.resolve(UUID.randomUUID() + ".json");
    }

    @Test
    void savesNewestHistoryFirstAndLimitsStoredItems() {
        ScriptHistoryService historyService = new ScriptHistoryService(historyFile, 2);
        ConversionResponse first = response("第一版");
        ConversionResponse second = response("第二版");
        ConversionResponse third = response("第三版");

        historyService.save(first);
        historyService.save(second);
        historyService.save(third);

        assertThat(historyService.list()).hasSize(2);
        assertThat(historyService.list()).extracting(ScriptHistoryItem::title)
                .containsExactly("第三版", "第二版");
    }

    @Test
    void deletesAndClearsHistoryItems() {
        ScriptHistoryService historyService = new ScriptHistoryService(historyFile, 20);
        ScriptHistoryItem item = historyService.save(response("第一版"));

        historyService.delete(item.id());
        assertThat(historyService.list()).isEmpty();

        historyService.save(response("第二版"));
        historyService.save(response("第三版"));
        historyService.clear();
        assertThat(historyService.list()).isEmpty();
    }

    private ConversionResponse response(String title) {
        ScriptDocument script = new RuleBasedScriptGenerator().generate("""
                第一章 雨夜
                林夏说：“出发。”

                第二章 旧城
                顾沉说：“跟上。”

                第三章 黎明
                林夏说：“结束了。”
                """);
        ScriptDocument renamed = new ScriptDocument(title, script.chapters(), script.metadata());
        return new ConversionResponse(renamed, "title: " + title, new ValidationResult(true, java.util.List.of(), java.util.List.of()));
    }
}
