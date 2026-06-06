package com.example.ainovel.script.service;

import com.example.ainovel.script.model.BeatType;
import com.example.ainovel.script.model.ScriptBeat;
import com.example.ainovel.script.model.ScriptChapter;
import com.example.ainovel.script.model.ScriptDocument;
import com.example.ainovel.script.model.ScriptMetadata;
import com.example.ainovel.script.model.ScriptScene;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConsistencyRepairServiceTest {

    private final ConsistencyRepairService repairService = new ConsistencyRepairService();
    private final ScriptValidationService validationService = new ScriptValidationService();

    @Test
    void addsDialogueSpeakersToSceneCharacters() {
        ScriptDocument document = documentWithMissingSpeaker();

        ScriptDocument repaired = repairService.repair(document);

        List<String> characters = repaired.chapters().getFirst().scenes().getFirst().characters();
        assertThat(characters).containsExactly("林夏", "沈万钧");
        assertThat(validationService.validate(repaired).warnings()).isEmpty();
    }

    private ScriptDocument documentWithMissingSpeaker() {
        ScriptScene scene = new ScriptScene(
                "scene-1",
                "码头对峙",
                "南码头",
                "夜晚",
                new ArrayList<>(List.of("林夏")),
                List.of(new ScriptBeat(BeatType.DIALOGUE, "沈万钧", "把账本交出来。"))
        );
        ScriptChapter chapter = new ScriptChapter("chapter-1", "第一章", "摘要", List.of(scene));
        return new ScriptDocument(
                "测试剧本",
                List.of(chapter, chapterWithId("chapter-2"), chapterWithId("chapter-3")),
                new ScriptMetadata(3, "AI_DEEPSEEK")
        );
    }

    private ScriptChapter chapterWithId(String id) {
        return new ScriptChapter(
                id,
                id,
                "摘要",
                List.of(new ScriptScene(
                        id + "-scene",
                        "场景",
                        "未知地点",
                        "未知时间",
                        new ArrayList<>(List.of("林夏")),
                        List.of(new ScriptBeat(BeatType.NARRATION, null, "旁白。"))
                ))
        );
    }
}
