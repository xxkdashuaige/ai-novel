package com.example.ainovel.script.service;

import com.example.ainovel.script.model.BeatType;
import com.example.ainovel.script.model.ScriptBeat;
import com.example.ainovel.script.model.ScriptChapter;
import com.example.ainovel.script.model.ScriptDocument;
import com.example.ainovel.script.model.ScriptMetadata;
import com.example.ainovel.script.model.ScriptScene;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RuleBasedScriptGenerator {

    private static final Pattern CHAPTER_TITLE = Pattern.compile("^(第[一二三四五六七八九十百千万0-9]+章[^\\n\\r]*)$", Pattern.MULTILINE);
    private static final Pattern NAMED_DIALOGUE = Pattern.compile("^\\s*([\\p{IsHan}A-Za-z0-9_]{1,12})(?:说|问|答|喊|道)?[:：]\\s*[“\"]?(.+?)[”\"]?\\s*$");
    private static final Pattern QUOTED_DIALOGUE = Pattern.compile("[“\"](.+?)[”\"]");

    public ScriptDocument generate(String novelText) {
        if (novelText == null || novelText.isBlank()) {
            throw new IllegalArgumentException("小说文本不能为空");
        }

        List<ChapterSlice> slices = splitChapters(novelText);
        if (slices.size() < 3) {
            throw new IllegalArgumentException("小说文本至少 3 章才能转换为剧本");
        }

        List<ScriptChapter> chapters = new ArrayList<>();
        for (int i = 0; i < slices.size(); i++) {
            ChapterSlice slice = slices.get(i);
            chapters.add(toChapter(slice, i + 1));
        }

        return new ScriptDocument("自动生成剧本", chapters, new ScriptMetadata(chapters.size(), "RULE_BASED"));
    }

    private List<ChapterSlice> splitChapters(String novelText) {
        Matcher matcher = CHAPTER_TITLE.matcher(novelText);
        List<Integer> starts = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        while (matcher.find()) {
            starts.add(matcher.start());
            titles.add(matcher.group(1).trim());
        }

        List<ChapterSlice> slices = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            int contentStart = starts.get(i) + titles.get(i).length();
            int contentEnd = i + 1 < starts.size() ? starts.get(i + 1) : novelText.length();
            String body = novelText.substring(contentStart, contentEnd).trim();
            slices.add(new ChapterSlice(titles.get(i), body));
        }
        return slices;
    }

    private ScriptChapter toChapter(ChapterSlice slice, int index) {
        List<ScriptBeat> beats = toBeats(slice.body());
        Set<String> characters = extractCharacters(beats);
        ScriptScene scene = new ScriptScene(
                "scene-" + index + "-1",
                "场景 1",
                guessLocation(slice.body()),
                guessTimeOfDay(slice.body()),
                new ArrayList<>(characters),
                beats
        );

        return new ScriptChapter(
                "chapter-" + index,
                slice.title(),
                summarize(slice.body()),
                List.of(scene)
        );
    }

    private List<ScriptBeat> toBeats(String body) {
        List<ScriptBeat> beats = new ArrayList<>();
        for (String rawLine : body.split("\\R+")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            Matcher namedDialogue = NAMED_DIALOGUE.matcher(line);
            if (namedDialogue.matches()) {
                beats.add(new ScriptBeat(BeatType.DIALOGUE, namedDialogue.group(1), cleanDialogue(namedDialogue.group(2))));
                continue;
            }

            Matcher quotedDialogue = QUOTED_DIALOGUE.matcher(line);
            if (quotedDialogue.find()) {
                beats.add(new ScriptBeat(BeatType.DIALOGUE, null, cleanDialogue(quotedDialogue.group(1))));
                String action = quotedDialogue.replaceAll("").trim();
                if (!action.isBlank()) {
                    beats.add(new ScriptBeat(BeatType.ACTION, null, action));
                }
                continue;
            }

            beats.add(new ScriptBeat(classifyNarrative(line), null, line));
        }

        if (beats.isEmpty()) {
            beats.add(new ScriptBeat(BeatType.NARRATION, null, "本章缺少可识别正文，保留为空场景旁白。"));
        }
        return beats;
    }

    private BeatType classifyNarrative(String line) {
        if (line.contains("转场") || line.contains("与此同时")) {
            return BeatType.TRANSITION;
        }
        if (line.contains("推") || line.contains("走") || line.contains("站") || line.contains("望") || line.contains("跑")) {
            return BeatType.ACTION;
        }
        return BeatType.NARRATION;
    }

    private Set<String> extractCharacters(List<ScriptBeat> beats) {
        Set<String> characters = new LinkedHashSet<>();
        for (ScriptBeat beat : beats) {
            if (beat.speaker() != null && !beat.speaker().isBlank()) {
                characters.add(beat.speaker());
            }
        }
        return characters;
    }

    private String cleanDialogue(String value) {
        return value.replaceAll("[”\"]$", "").trim();
    }

    private String summarize(String body) {
        String normalized = body.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 60) {
            return normalized.isBlank() ? "本章由系统自动生成摘要。" : normalized;
        }
        return normalized.substring(0, 60) + "……";
    }

    private String guessLocation(String body) {
        if (body.contains("街") || body.contains("城")) {
            return "城市街区";
        }
        if (body.contains("门") || body.contains("屋")) {
            return "室内或门口";
        }
        return "未知地点";
    }

    private String guessTimeOfDay(String body) {
        if (body.contains("夜") || body.contains("月")) {
            return "夜晚";
        }
        if (body.contains("黎明") || body.contains("天光")) {
            return "清晨";
        }
        return "未知时间";
    }

    private record ChapterSlice(String title, String body) {
    }
}
