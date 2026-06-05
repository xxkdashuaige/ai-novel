# 剧本 YAML Schema 设计说明

## 设计目标

小说文本通常包含章节、叙述、对白、动作和场景变化。剧本创作需要把这些内容拆成可编辑、可扩展的结构。因此 Schema 采用“作品 -> 章节 -> 场景 -> 节拍”的层级。

## 字段结构

```yaml
title: 示例小说
chapters:
  - id: chapter-1
    title: 第一章 雨夜
    summary: 本章自动摘要
    scenes:
      - id: scene-1
        title: 场景 1
        location: 未知地点
        timeOfDay: 未知时间
        characters:
          - 林舟
        beats:
          - type: dialogue
            speaker: 林舟
            content: 我们必须马上离开。
          - type: action
            content: 林舟推开木门，雨水扑面而来。
metadata:
  sourceChapterCount: 3
  generationMode: RULE_BASED
```

## 设计原因

- `chapters` 保留小说原有结构，方便作者逐章修改。
- `scenes` 是剧本扩写的核心单位，后续可以继续扩展为分镜、拍摄地点和道具。
- `beats` 表示最小可编辑内容块，支持对白、动作、旁白和转场。
- `characters` 放在场景级别，方便快速查看某个场景涉及的人物。
- `metadata` 记录生成方式和来源信息，便于区分 AI 生成、规则生成和兜底生成。

## 校验规则

- `title` 不能为空。
- `chapters` 至少包含 3 个章节。
- 每章至少包含 1 个场景。
- 每个场景至少包含 1 个 beat。
- `beat.type` 只能是 `dialogue`、`action`、`narration`、`transition`。
