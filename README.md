# AI 小说转剧本工具

这是一个使用 Spring Boot、Spring AI、Vue 3 和 Element Plus 实现的编程题项目。它可以把 3 个章节以上的小说文本转换为结构化剧本 YAML，让作者快速获得可编辑、可校验、可继续打磨的剧本初稿。

项目不仅支持小说到剧本的转换，还提供 AI 状态透明化、剧本质量评分、一致性提醒一键修复、后端转换历史、可视化预览、角色关系图、场景分镜表、多格式导出和 AI 辅助编辑能力。

## Demo 视频

Demo 视频：[Bilibili - AI 小说转剧本工具演示](https://www.bilibili.com/video/BV1PWEt6KEHq/?spm_id_from=333.1387.homepage.video_card.click&vd_source=d0e06029c8f855f89f969173f115552a)

录制脚本可参考 [docs/demo-script.md](docs/demo-script.md)。录制完成后，可以把视频链接补充到这里。

## 项目结构

```text
backend/   Spring Boot 后端
frontend/  Vue 3 + Element Plus 前端
docs/      YAML Schema 与设计文档
```

## 核心功能

- 小说一键转换：输入 3 章及以上小说文本，生成结构化 YAML 剧本。
- AI / 规则双模式：配置 DeepSeek Key 后调用 AI；未配置时使用规则解析器兜底。
- AI 状态透明化：展示当前 AI 是否启用、模型、Key 配置状态和生成来源。
- 转换阶段提示：转换时展示准备文本、调用 AI/规则解析、结构化剧本、校验质量、保存历史等阶段。
- YAML Schema 校验：校验标题、章节、场景、节拍类型和必填字段。
- 数据一致性提醒：发现对白人物未列入场景 `characters` 时给出提醒。
- 一键修复一致性：自动把对白 `speaker` 补入对应场景人物列表。
- 剧本质量评分：从结构完整度、场景信息、人物一致性和校验结果给出评分与建议。
- 剧本可视化预览：按章节、场景、动作、对白展示剧本。
- 角色关系图：根据同场出现人物自动生成轻量关系图。
- 场景分镜表：按章节、场景和节拍生成可导出的分镜表 CSV。
- 转换历史记录：后端保存最近 20 次转换，支持搜索、筛选、加载和删除。
- AI 辅助编辑：支持单条台词/动作优化，也支持整场景对白优化、动作补充和冲突增强。
- 多格式导出：支持 YAML、JSON、TXT、CSV、DOC 和打印保存 PDF。

## 技术栈与第三方依赖

后端使用：

- Java 21：后端运行环境。
- Spring Boot 3.5.14：提供 Web 服务、配置管理和应用启动能力。
- Spring AI 1.1.7：通过 OpenAI-compatible 客户端调用 DeepSeek 等大模型。
- Spring Validation：校验请求参数和剧本结构。
- Jackson YAML：实现剧本对象与 YAML 文本之间的转换。
- JUnit 5 / Spring Boot Test：后端单元测试和上下文启动测试。

前端使用：

- Vue 3.5：构建单页应用界面。
- Element Plus 2.11：提供按钮、卡片、弹窗、标签、提示等 UI 组件。
- Axios 1.12：调用后端 REST API。
- Vite 7.1：前端开发服务器和生产构建工具。
- TypeScript 5.9 / vue-tsc：提供类型检查和前端构建校验。
- pnpm：前端依赖管理工具。

项目中的小说转剧本结构设计、规则兜底解析、AI 状态透明化、一致性修复、剧本质量评分、转换历史管理、角色关系图和场景分镜表等业务逻辑为本项目自主实现；第三方依赖仅用于框架、UI、网络请求、序列化和测试能力。

## 文档入口

- [项目启动说明](docs/startup.md)：本地启动、DeepSeek Key 配置和常见问题。
- [YAML Schema 定义与设计说明](docs/yaml-schema-design.md)：说明剧本 YAML 字段、校验规则和 Schema 设计原因。
- [基础 Schema 说明](docs/schema.md)：简版 Schema 结构说明。
- [Demo 演讲稿](docs/demo-script.md)：录制演示视频时可参考的讲解顺序。
- [PR 描述](docs/pr-description.md)：功能说明、实现思路、测试方式和提交记录说明。

## 快速启动

如果你把源码发给别人，建议让对方先阅读 [docs/startup.md](docs/startup.md)。里面包含 Windows PowerShell 下的完整启动步骤、DeepSeek API Key 配置方式、无 API Key 演示方式和常见问题。

## 后端启动

```bash
cd backend
mvn -s maven-settings.xml spring-boot:run
```

默认使用规则解析器，可离线运行。如果要启用 Spring AI：

```bash
$env:OPENAI_API_KEY="你的Key"
mvn -s maven-settings.xml spring-boot:run "-Dspring-boot.run.arguments=--app.ai.enabled=true"
```

如果使用 DeepSeek API Key，可以继续使用 Spring AI 的 OpenAI-compatible 客户端：

```bash
$env:OPENAI_API_KEY="你的DeepSeekKey"
$env:OPENAI_BASE_URL="https://api.deepseek.com"
$env:OPENAI_MODEL="deepseek-v4-flash"
mvn -s maven-settings.xml spring-boot:run "-Dspring-boot.run.arguments=--app.ai.enabled=true"
```

也可以参考 `.env.example` 中的变量名。真实 API Key 只应通过环境变量配置，不要写入源码或提交到 GitHub。

## 前端启动

```bash
cd frontend
pnpm install --store-dir D:\ai-novel\.pnpm-store
pnpm dev --host 127.0.0.1
```

前端默认代理后端 `http://localhost:8080`。

## 核心接口

- `POST /api/scripts/convert`：输入小说文本，返回 YAML、JSON 预览和校验结果。
- `POST /api/scripts/validate`：校验 YAML 是否符合剧本 Schema。
- `POST /api/scripts/repair`：自动修复人物未列入场景 characters 等一致性提醒。
- `POST /api/scripts/edit`：对单条台词、动作或旁白进行 AI 辅助编辑。
- `POST /api/scripts/edit-scene`：对整场景进行 AI 辅助编辑。
- `POST /api/scripts/render`：根据编辑后的剧本对象重新生成 YAML、校验结果和质量评分。
- `GET /api/scripts/history`：读取后端保存的转换历史。
- `DELETE /api/scripts/history/{id}`：删除单条转换历史。
- `DELETE /api/scripts/history`：清空转换历史。
- `GET /api/ai/status`：查看当前 AI 是否启用、模型和 Key 配置状态。
- `GET /api/schema`：返回 Schema 文档。

## 本地验证

后端测试：

```bash
cd backend
mvn -s maven-settings.xml test
```

前端构建：

```bash
cd frontend
pnpm build
```

## 为什么使用 Spring AI

Spring AI 可以把大模型能力封装在 Spring Boot 应用内部，并用 `ChatClient` 统一调用模型。项目没有直接信任模型输出 YAML，而是先生成 Java DTO，再由后端转换成 YAML，降低格式错误风险。没有 API Key 时，规则解析器会作为兜底，保证项目可以稳定演示。
