package com.example.ainovel.script.service;

import com.example.ainovel.script.model.ScriptDocument;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ScriptHistoryService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/M/d HH:mm:ss");

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Path historyFile;
    private final int maxItems;

    @Autowired
    public ScriptHistoryService(@Value("${app.history.file:data/history.json}") String historyFile) {
        this(Path.of(historyFile), 20);
    }

    ScriptHistoryService(Path historyFile, int maxItems) {
        this.historyFile = historyFile;
        this.maxItems = maxItems;
    }

    public synchronized List<ScriptHistoryItem> list() {
        return read();
    }

    public synchronized ScriptHistoryItem save(ConversionResponse response) {
        List<ScriptHistoryItem> items = new ArrayList<>(read());
        ScriptDocument script = response.script();
        ScriptHistoryItem item = new ScriptHistoryItem(
                UUID.randomUUID().toString(),
                script.title(),
                LocalDateTime.now().format(FORMATTER),
                script.metadata() == null ? "UNKNOWN" : script.metadata().generationMode(),
                script.metadata() == null ? "" : script.metadata().generationMessage(),
                script.chapters().size(),
                countScenes(script),
                script,
                response.yaml(),
                response.validation()
        );
        items.addFirst(item);
        write(items.stream().limit(maxItems).toList());
        return item;
    }

    public synchronized void delete(String id) {
        write(read().stream().filter(item -> !item.id().equals(id)).toList());
    }

    public synchronized void clear() {
        write(List.of());
    }

    private int countScenes(ScriptDocument script) {
        return script.chapters().stream().mapToInt(chapter -> chapter.scenes().size()).sum();
    }

    private List<ScriptHistoryItem> read() {
        if (!Files.exists(historyFile)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(historyFile.toFile(), new TypeReference<>() {
            });
        } catch (IOException e) {
            return List.of();
        }
    }

    private void write(List<ScriptHistoryItem> items) {
        try {
            Path parent = historyFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(historyFile.toFile(), items);
        } catch (IOException e) {
            throw new IllegalStateException("保存转换历史失败：" + e.getMessage(), e);
        }
    }
}
