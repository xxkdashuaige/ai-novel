# AI 小说转剧本工具设计

## 目标

构建一个前后端分离的 AI 辅助剧本创作工具。用户输入三章及以上的小说文本后，系统自动生成结构化剧本 YAML，并提供 Schema 文档说明设计原因。

## 技术栈

- 后端：Spring Boot、Spring AI、Jackson YAML、Bean Validation
- 前端：Vue 3、TypeScript、Vite、Element Plus、Axios
- AI 策略：默认规则解析可离线运行，Spring AI 作为可开启的增强能力

## 架构

前端提供小说输入、转换按钮、JSON 预览、YAML 输出和 Schema 查看。后端提供 REST API，负责输入校验、剧本生成、YAML 序列化和 Schema 校验。

后端把模型输出约束到 Java DTO，而不是直接信任模型输出的 YAML。这样可以复用 Bean Validation 和 Java 类型系统，减少格式漂移，也方便后续替换不同大模型。

## 数据流

1. 用户在 Vue 页面粘贴小说文本。
2. 前端调用 `POST /api/scripts/convert`。
3. 后端优先尝试 Spring AI 生成结构化剧本；默认配置下使用规则生成器。
4. 后端把 `ScriptDocument` 转成 YAML。
5. 前端展示 YAML、JSON 预览和校验结果。

## YAML Schema 摘要

剧本由 `title`、`chapters` 和 `metadata` 组成。每章包含 `scenes`，每个场景包含 `beats`。`beat.type` 使用固定枚举：`dialogue`、`action`、`narration`、`transition`，便于后续扩写为分镜、对白表或拍摄计划。

## 错误处理

- 小说文本为空时返回 400。
- 章节少于三章时返回 400。
- AI 调用失败时返回规则解析结果，并在 `metadata.generationMode` 标记为 `RULE_BASED_FALLBACK`。
- YAML 校验失败时返回具体错误列表。

## 测试

后端核心测试覆盖章节识别、对白识别、YAML 输出和 Schema 校验。前端保留清晰的 API 边界和手工演示路径。
