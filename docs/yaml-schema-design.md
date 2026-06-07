# 剧本 YAML Schema 定义与设计说明

## 1. 文档目的

本项目需要将 3 个章节以上的小说文本自动转换为结构化剧本，输出格式为 YAML。YAML Schema 的作用是约束 AI 或规则解析器生成的剧本结构，让作者获得一份可编辑、可校验、可继续打磨的剧本初稿。

该 Schema 不追求一次性生成完整工业级剧本，而是优先保证结构清晰、字段稳定、便于二次编辑和后续扩展。

## 2. 顶层结构

剧本 YAML 采用“作品 -> 章节 -> 场景 -> 节拍”的层级结构。

```yaml
title: 示例小说
chapters:
  - id: chapter-1
    title: 第一章 雨夜
    summary: 本章剧情摘要
    scenes:
      - id: scene-1
        title: 场景 1
        location: 雨夜街口
        timeOfDay: 夜晚
        characters:
          - 林舟
          - 苏晚
        beats:
          - type: action
            speaker:
            content: 林舟推开木门，雨水扑面而来。
          - type: dialogue
            speaker: 林舟
            content: 我们必须马上离开。
metadata:
  sourceChapterCount: 3
  generationMode: AI_DEEPSEEK
  generationMessage: DeepSeek AI 转换成功。
```

## 3. 字段定义

### 3.1 ScriptDocument

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `title` | string | 是 | 剧本标题，通常来自小说标题或自动生成标题。 |
| `chapters` | array | 是 | 章节列表，至少包含 3 章。 |
| `metadata` | object | 否 | 生成来源、章节数量和提示信息。 |

设计原因：顶层只保留作品级核心信息，避免让 YAML 过早承载复杂配置。`chapters` 保留小说原始章节结构，方便作者按小说逻辑逐章检查和修改。

### 3.2 ScriptChapter

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | string | 是 | 章节唯一编号，例如 `chapter-1`。 |
| `title` | string | 是 | 章节标题。 |
| `summary` | string | 是 | 章节摘要，用于快速理解剧情。 |
| `scenes` | array | 是 | 场景列表，每章至少包含 1 个场景。 |

设计原因：小说通常天然按章节组织，保留章节层可以降低作者理解成本。`summary` 能帮助作者快速定位章节内容，也方便后续做章节级 AI 改写、扩写或压缩。

### 3.3 ScriptScene

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | string | 是 | 场景唯一编号，例如 `scene-1`。 |
| `title` | string | 是 | 场景标题。 |
| `location` | string | 是 | 场景地点，例如 `旧书店`、`雨夜街口`。 |
| `timeOfDay` | string | 是 | 场景时间，例如 `夜晚`、`黎明`。 |
| `characters` | array | 否 | 当前场景出现的人物列表。 |
| `beats` | array | 是 | 场景内的剧本节拍列表。 |

设计原因：场景是剧本创作和拍摄拆分的核心单位。`location` 和 `timeOfDay` 有助于后续生成分镜表、拍摄计划和视觉预览。`characters` 放在场景级别，可以支持人物高亮、关系图和一致性校验。

### 3.4 ScriptBeat

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `type` | enum | 是 | 节拍类型，只能是 `dialogue`、`action`、`narration`、`transition`。 |
| `speaker` | string | 否 | 说话人，仅对白通常需要填写。 |
| `content` | string | 是 | 节拍正文内容。 |

`type` 可选值：

| 值 | 含义 | 示例 |
| --- | --- | --- |
| `dialogue` | 角色对白 | `林舟：我们必须马上离开。` |
| `action` | 动作描写 | `林舟推开木门。` |
| `narration` | 旁白或叙述 | `雨声覆盖了远处的脚步声。` |
| `transition` | 转场说明 | `画面切至旧城钟楼。` |

设计原因：`beats` 是最小可编辑单元。将对白、动作、旁白和转场拆成统一结构后，前端可以逐条展示、逐条 AI 编辑，也可以生成场景分镜表和导出文档。

### 3.5 ScriptMetadata

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `sourceChapterCount` | number | 否 | 输入小说识别出的章节数量。 |
| `generationMode` | string | 否 | 生成模式，例如 `AI_DEEPSEEK`、`RULE_BASED`、`RULE_BASED_FALLBACK`。 |
| `generationMessage` | string | 否 | 给用户展示的生成说明。 |

设计原因：项目支持 AI 生成和规则兜底。`metadata` 可以让用户知道当前结果来自真实 AI、规则解析器，还是 AI 调用失败后的兜底结果，提升状态透明度。

## 4. 校验规则

后端会对 YAML 进行基础结构校验：

- `title` 不能为空。
- `chapters` 至少包含 3 个章节。
- 每个 `chapter.id` 不应重复。
- 每章至少包含 1 个 `scene`。
- 每个 `scene.id` 不应重复。
- 每个场景至少包含 1 个 `beat`。
- `beat.type` 只能是 `dialogue`、`action`、`narration`、`transition`。
- `beat.content` 不能为空。
- 如果对白 `speaker` 没有出现在当前场景的 `characters` 中，系统会给出一致性提醒。
- 如果场景没有列出人物，系统会给出一致性提醒。

## 5. 为什么选择 YAML

选择 YAML 而不是纯文本，是因为剧本需要被继续编辑、校验和导出。结构化 YAML 可以让后端稳定读取字段，让前端按章节、场景、人物和节拍展示内容。

选择 YAML 而不是只返回 JSON，是因为 YAML 对作者更友好，可读性更接近文档，适合人工检查和微调。同时后端仍然会将 YAML 映射为 Java DTO，避免直接信任 AI 输出文本。

## 6. 设计取舍

该 Schema 有意保持轻量，没有加入复杂字段，例如镜号、景别、机位、服化道、预算、拍摄天数等。原因是本项目的目标是先生成“可编辑剧本初稿”，而不是直接生成完整制片文档。

后续如果需要扩展，可以在 `scene` 或 `beat` 层增加字段：

- `shotType`：景别，例如近景、远景。
- `camera`：镜头语言。
- `duration`：预计时长。
- `props`：道具。
- `notes`：作者备注。

当前结构已经为这些扩展预留了位置，因为场景和节拍是清晰分层的。

## 7. 完整示例

```yaml
title: 雨夜旧城
chapters:
  - id: chapter-1
    title: 第一章 雨夜
    summary: 林舟在雨夜发现危险逼近，决定带苏晚离开。
    scenes:
      - id: scene-1
        title: 雨夜门口
        location: 老宅门口
        timeOfDay: 夜晚
        characters:
          - 林舟
          - 苏晚
        beats:
          - type: action
            speaker:
            content: 林舟推开木门，雨水扑面而来。
          - type: dialogue
            speaker: 林舟
            content: 我们必须马上离开。
          - type: dialogue
            speaker: 苏晚
            content: 你到底看见了什么？
  - id: chapter-2
    title: 第二章 旧城
    summary: 两人在旧城钟楼附近寻找线索。
    scenes:
      - id: scene-2
        title: 旧城钟楼
        location: 旧城街角
        timeOfDay: 傍晚
        characters:
          - 林舟
          - 苏晚
        beats:
          - type: narration
            speaker:
            content: 钟声在空街上回荡。
          - type: action
            speaker:
            content: 苏晚抬头看向钟楼，神情迟疑。
  - id: chapter-3
    title: 第三章 黎明
    summary: 黎明时分，林舟确认危机暂时解除。
    scenes:
      - id: scene-3
        title: 天台黎明
        location: 天台
        timeOfDay: 黎明
        characters:
          - 林舟
          - 苏晚
        beats:
          - type: transition
            speaker:
            content: 画面切至天台，天光穿过云层。
          - type: dialogue
            speaker: 林舟
            content: 结束了，但我们还不能停下。
metadata:
  sourceChapterCount: 3
  generationMode: AI_DEEPSEEK
  generationMessage: DeepSeek AI 转换成功。
```
