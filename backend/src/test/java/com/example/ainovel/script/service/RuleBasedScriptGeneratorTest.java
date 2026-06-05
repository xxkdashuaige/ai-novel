package com.example.ainovel.script.service;

import com.example.ainovel.script.model.BeatType;
import com.example.ainovel.script.model.ScriptDocument;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleBasedScriptGeneratorTest {

    private final RuleBasedScriptGenerator generator = new RuleBasedScriptGenerator();

    @Test
    void rejectsNovelWithFewerThanThreeChapters() {
        String text = """
                第一章 雨夜
                林舟推开门。

                第二章 追踪
                “快走！”林舟说。
                """;

        assertThatThrownBy(() -> generator.generate(text))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("至少 3 章");
    }

    @Test
    void extractsChaptersAndDialogueBeats() {
        ScriptDocument document = generator.generate(sampleNovel());

        assertThat(document.chapters()).hasSize(3);
        assertThat(document.chapters().getFirst().title()).isEqualTo("第一章 雨夜");
        assertThat(document.chapters().getFirst().scenes().getFirst().beats())
                .anySatisfy(beat -> {
                    assertThat(beat.type()).isEqualTo(BeatType.DIALOGUE);
                    assertThat(beat.speaker()).isEqualTo("林舟");
                    assertThat(beat.content()).contains("马上离开");
                });
        assertThat(document.metadata().sourceChapterCount()).isEqualTo(3);
    }

    static String sampleNovel() {
        return """
                第一章 雨夜
                林舟推开木门，雨水扑面而来。
                林舟：“我们必须马上离开。”

                第二章 旧城
                苏晚站在街角，望向远处的钟楼。
                苏晚说：“他还会回来吗？”

                第三章 黎明
                天光穿过云层，所有人都安静下来。
                林舟回答：“会的。”
                """;
    }
}
