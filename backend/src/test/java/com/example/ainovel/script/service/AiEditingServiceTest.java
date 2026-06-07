package com.example.ainovel.script.service;

import com.example.ainovel.script.controller.EditRequest;
import com.example.ainovel.script.controller.SceneEditRequest;
import com.example.ainovel.script.model.BeatType;
import com.example.ainovel.script.model.ScriptBeat;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiEditingServiceTest {

    private final ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
    private final AiEditingService editingService;

    AiEditingServiceTest() {
        when(chatClientBuilder.build()).thenReturn(mock(ChatClient.class));
        editingService = new AiEditingService(chatClientBuilder, false, "https://api.deepseek.com");
    }

    @Test
    void fallsBackWhenEnhancingConflict() {
        EditResponse response = editingService.edit(request("enhance-conflict", "林夏看向顾沉。"));

        assertThat(response.generationMode()).isEqualTo("RULE_BASED_FALLBACK");
        assertThat(response.result()).contains("冲突");
    }

    @Test
    void fallsBackWhenRewritingAsShortDrama() {
        EditResponse response = editingService.edit(request("short-drama-style", "我们需要尽快离开这里。"));

        assertThat(response.result()).contains("短剧");
    }

    @Test
    void fallsBackWhenCompressingDialogue() {
        EditResponse response = editingService.edit(request("compress-dialogue", "我真的不知道接下来应该怎么办才好。"));

        assertThat(response.result()).contains("更短");
    }

    @Test
    void fallsBackWhenAddingCameraLanguage() {
        EditResponse response = editingService.edit(request("add-camera-language", "林夏站在门口。"));

        assertThat(response.result()).contains("镜头");
    }

    @Test
    void fallsBackWhenPolishingSceneDialogue() {
        SceneEditResponse response = editingService.editScene(sceneRequest("polish-scene-dialogue"));

        assertThat(response.generationMode()).isEqualTo("RULE_BASED_FALLBACK");
        assertThat(response.beats()).hasSize(2);
        assertThat(response.beats().getFirst().content()).contains("语气更集中");
    }

    @Test
    void fallsBackWhenExpandingSceneAction() {
        SceneEditResponse response = editingService.editScene(sceneRequest("expand-scene-action"));

        assertThat(response.beats()).hasSize(2);
        assertThat(response.beats().get(1).content()).contains("可拍动作");
    }

    @Test
    void fallsBackWhenEnhancingSceneConflict() {
        SceneEditResponse response = editingService.editScene(sceneRequest("enhance-scene-conflict"));

        assertThat(response.beats()).extracting(ScriptBeat::content).anyMatch(content -> content.contains("冲突升级"));
    }

    private EditRequest request(String type, String content) {
        return new EditRequest(type, content, "林夏", "旧书店", List.of("林夏", "顾沉"));
    }

    private SceneEditRequest sceneRequest(String type) {
        return new SceneEditRequest(
                type,
                "旧书店对峙",
                List.of("林夏", "顾沉"),
                List.of(
                        new ScriptBeat(BeatType.DIALOGUE, "林夏", "你为什么回来？"),
                        new ScriptBeat(BeatType.ACTION, null, "顾沉站在门口。")
                )
        );
    }
}
