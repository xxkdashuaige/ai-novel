package com.example.ainovel.script.service;

import com.example.ainovel.script.controller.EditRequest;
import com.example.ainovel.script.controller.SceneEditRequest;
import com.example.ainovel.script.model.BeatType;
import com.example.ainovel.script.model.ScriptBeat;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiEditingService {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final ChatClient chatClient;
    private final boolean enabled;
    private final String baseUrl;

    public AiEditingService(
            ChatClient.Builder chatClientBuilder,
            @Value("${app.ai.enabled:false}") boolean enabled,
            @Value("${spring.ai.openai.base-url:https://api.openai.com}") String baseUrl
    ) {
        this.chatClient = chatClientBuilder.build();
        this.enabled = enabled;
        this.baseUrl = baseUrl;
    }

    public EditResponse edit(EditRequest request) {
        if (!enabled) {
            return fallback(request);
        }

        try {
            String result = chatClient.prompt()
                    .system("""
                            你是专业剧本编辑。根据用户要求改写一条剧本内容。
                            只输出改写后的文本，不要输出解释、标题、Markdown 或 JSON。
                            台词要自然、有戏剧张力；动作要可拍、具体、简洁。
                            """)
                    .user(buildPrompt(request))
                    .call()
                    .content();
            if (result == null || result.isBlank()) {
                return fallback(request);
            }
            return new EditResponse(result.trim(), aiGenerationMode());
        } catch (RuntimeException ex) {
            return fallback(request);
        }
    }

    public SceneEditResponse editScene(SceneEditRequest request) {
        if (!enabled) {
            return sceneFallback(request);
        }

        try {
            String result = chatClient.prompt()
                    .system("""
                            你是专业剧本编辑。根据用户要求优化一个场景的剧本节拍。
                            只返回 JSON 数组，不要输出解释、Markdown 或代码块。
                            数组元素字段只能包含 type、speaker、content；type 只能是 dialogue、action、narration、transition。
                            """)
                    .user(buildScenePrompt(request))
                    .call()
                    .content();
            if (result == null || result.isBlank()) {
                return sceneFallback(request);
            }
            List<ScriptBeat> beats = objectMapper.readValue(result.trim(), new TypeReference<>() {
            });
            return beats.isEmpty() ? sceneFallback(request) : new SceneEditResponse(beats, aiGenerationMode());
        } catch (RuntimeException | java.io.IOException ex) {
            return sceneFallback(request);
        }
    }

    private String buildPrompt(EditRequest request) {
        String instruction = switch (request.type()) {
            case "polish-dialogue" -> "请优化这句台词，让它更符合人物情绪和短剧节奏。";
            case "expand-action" -> "请补充这段动作描写，让它更有画面感且适合拍摄。";
            case "enhance-conflict" -> "请增强这条剧本内容里的矛盾、阻碍或悬念，但不要改变核心事件。";
            case "short-drama-style" -> "请把这条内容改成短剧风格：更口语、更快节奏、更有钩子。";
            case "compress-dialogue" -> "请压缩这条内容，保留核心信息，让表达更短、更适合演员说出口。";
            case "add-camera-language" -> "请补充镜头语言、视线、走位或停顿，让内容更适合拍摄。";
            default -> "请优化这条剧本内容，保持原意但提升表现力。";
        };
        return """
                %s
                场景：%s
                人物：%s
                说话人：%s
                原文：%s
                """.formatted(
                instruction,
                blankToDefault(request.sceneTitle(), "未标注"),
                request.characters() == null ? "未标注" : String.join("、", request.characters()),
                blankToDefault(request.speaker(), "无"),
                request.content()
        );
    }

    private String buildScenePrompt(SceneEditRequest request) {
        String instruction = switch (request.type()) {
            case "polish-scene-dialogue" -> "请优化整场对白，让人物表达更自然、更有情绪推进。";
            case "expand-scene-action" -> "请补充整场动作细节，让每条动作更具体、更可拍。";
            case "enhance-scene-conflict" -> "请增强整场冲突节奏，让场景更有阻碍、反转或悬念。";
            default -> "请优化这个场景，保持剧情含义不变。";
        };
        String beatsJson;
        try {
            beatsJson = objectMapper.writeValueAsString(request.beats());
        } catch (java.io.IOException ex) {
            beatsJson = "[]";
        }
        return """
                %s
                场景：%s
                人物：%s
                原始 beats JSON：%s
                """.formatted(
                instruction,
                blankToDefault(request.sceneTitle(), "未标注"),
                request.characters() == null ? "未标注" : String.join("、", request.characters()),
                beatsJson
        );
    }

    private EditResponse fallback(EditRequest request) {
        String result = switch (request.type()) {
            case "polish-dialogue" -> request.content() + "（语气更坚定，情绪更集中。）";
            case "expand-action" -> request.content() + " 角色停顿片刻，目光扫过四周，动作带出紧张感。";
            case "enhance-conflict" -> request.content() + " 冲突进一步升级，双方的目标变得更加对立。";
            case "short-drama-style" -> request.content() + "（短剧节奏：更直接、更有悬念。）";
            case "compress-dialogue" -> request.content() + "（更短表达：保留核心，删去铺垫。）";
            case "add-camera-language" -> "镜头推近，" + request.content() + " 角色短暂停顿，视线落在关键物件上。";
            default -> request.content();
        };
        return new EditResponse(result, "RULE_BASED_FALLBACK");
    }

    private SceneEditResponse sceneFallback(SceneEditRequest request) {
        List<ScriptBeat> beats = request.beats().stream()
                .map(beat -> new ScriptBeat(beat.type(), beat.speaker(), sceneFallbackContent(request.type(), beat)))
                .toList();
        return new SceneEditResponse(beats, "RULE_BASED_FALLBACK");
    }

    private String sceneFallbackContent(String type, ScriptBeat beat) {
        return switch (type) {
            case "polish-scene-dialogue" -> beat.type() == BeatType.DIALOGUE
                    ? beat.content() + "（语气更集中，情绪推进更明确。）"
                    : beat.content();
            case "expand-scene-action" -> beat.type() == BeatType.ACTION || beat.type() == BeatType.NARRATION
                    ? beat.content() + " 增加可拍动作：角色短暂停顿，视线和走位带出现场压力。"
                    : beat.content();
            case "enhance-scene-conflict" -> beat.content() + " 冲突升级，双方目标更明显地发生碰撞。";
            default -> beat.content();
        };
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String aiGenerationMode() {
        return baseUrl.toLowerCase().contains("deepseek") ? "AI_DEEPSEEK" : "AI_OPENAI_COMPATIBLE";
    }
}
