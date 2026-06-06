package com.example.ainovel.script.service;

import com.example.ainovel.script.model.ScriptBeat;
import com.example.ainovel.script.model.ScriptChapter;
import com.example.ainovel.script.model.ScriptDocument;
import com.example.ainovel.script.model.ScriptScene;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ConsistencyRepairService {

    public ScriptDocument repair(ScriptDocument document) {
        List<ScriptChapter> chapters = document.chapters().stream()
                .map(this::repairChapter)
                .toList();
        return new ScriptDocument(document.title(), chapters, document.metadata());
    }

    private ScriptChapter repairChapter(ScriptChapter chapter) {
        List<ScriptScene> scenes = chapter.scenes().stream()
                .map(this::repairScene)
                .toList();
        return new ScriptChapter(chapter.id(), chapter.title(), chapter.summary(), scenes);
    }

    private ScriptScene repairScene(ScriptScene scene) {
        Set<String> characters = new LinkedHashSet<>(scene.characters() == null ? List.of() : scene.characters());
        for (ScriptBeat beat : scene.beats()) {
            if (beat.speaker() != null && !beat.speaker().isBlank()) {
                characters.add(beat.speaker());
            }
        }
        return new ScriptScene(
                scene.id(),
                scene.title(),
                scene.location(),
                scene.timeOfDay(),
                new ArrayList<>(characters),
                scene.beats()
        );
    }
}
