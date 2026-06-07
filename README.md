# AI 小说转剧本工具

一个基于 **Spring Boot + Spring AI + Vue 3 + Element Plus** 的课程项目，用于将 **3 个章节以上的小说文本自动转换为结构化剧本 YAML**，帮助作者快速获得可编辑、可校验、可继续打磨的剧本初稿。

## Demo 视频

- [Bilibili - AI 小说转剧本工具演示](https://www.bilibili.com/video/BV1PWEt6KEHq/?spm_id_from=333.1387.homepage.video_card.click&vd_source=d0e06029c8f855f89f969173f115552a)

Demo 讲解稿见 [docs/demo-script.md](docs/demo-script.md)。

## 项目亮点

- 小说一键转换：输入 3 章及以上小说文本，生成结构化 YAML 剧本。
- AI / 规则双模式：配置 DeepSeek Key 后调用 AI；未配置时使用规则解析器兜底。
- AI 状态透明化：展示当前 AI 是否启用、模型、Key 配置状态和生成来源。
- 转换阶段提示：展示准备文本、调用 AI/规则解析、结构化剧本、校验质量、保存历史等阶段。
- YAML Schema 校验：自动校验标题、章节、场景、节拍类型和必填字段。
- 一键修复一致性：自动把对白 `speaker` 补入对应场景 `characters` 列表。
- 剧本质量评分：从结构完整度、场景信息、人物一致性和校验结果给出评分与建议。
- 可视化预览：支持章节预览、角色关系图、场景分镜表。
- AI 辅助编辑：支持单条 beat 编辑，也支持整场景对白优化、动作补充和冲突增强。
- 多格式导出：支持 YAML、JSON、TXT、CSV、DOC 和打印保存 PDF。

## AI 辅助体现在哪里

这个项目的 AI 辅助不只是把小说变成一段文本，而是贯穿了整个剧本创作流程：

- 使用大模型将小说内容转换为结构化剧本对象
- 对单条台词、动作、旁白做局部优化
- 对整场景做对白优化、动作补充和冲突增强
- 结合结构校验和质量分析，帮助作者判断剧本初稿是否可继续打磨

没有 API Key 时，系统会自动切换到规则解析器模式，保证项目仍然可以稳定演示。

## 快速启动

更多细节见 [docs/startup.md](docs/startup.md)。

### 1. 启动后端

无 API Key 演示模式：

```powershell
cd D:\ai-novel\backend
mvn -s maven-settings.xml spring-boot:run
```

DeepSeek AI 模式：

```powershell
cd D:\ai-novel\backend

$env:OPENAI_API_KEY="你的 DeepSeek Key"
$env:OPENAI_BASE_URL="https://api.deepseek.com"
$env:OPENAI_MODEL="deepseek-v4-flash"

mvn -s maven-settings.xml spring-boot:run "-Dspring-boot.run.arguments=--app.ai.enabled=true"
```

### 2. 启动前端

```powershell
cd D:\ai-novel\frontend
pnpm install --store-dir D:\ai-novel\.pnpm-store
pnpm dev --host 127.0.0.1
```

访问地址：

- 前端：`http://127.0.0.1:5173/`
- 后端：`http://127.0.0.1:8080`

## 文档入口

- [项目启动说明](docs/startup.md)
- [YAML Schema 定义与设计说明](docs/yaml-schema-design.md)
- [基础 Schema 说明](docs/schema.md)
- [Demo 演讲稿](docs/demo-script.md)
- [PR 描述](docs/pr-description.md)
- [最终提交前自查清单](docs/final-checklist.md)

## 技术栈

### 后端

- Java 21
- Spring Boot 3.5.14
- Spring AI 1.1.7
- Spring Validation
- Jackson YAML
- JUnit 5 / Spring Boot Test

### 前端

- Vue 3.5
- Element Plus 2.11
- Axios 1.12
- Vite 7
- TypeScript 5.9
- pnpm

## 本地验证

后端测试：

```powershell
cd D:\ai-novel\backend
mvn -s maven-settings.xml test
```

前端构建：

```powershell
cd D:\ai-novel\frontend
pnpm build
```

## 核心接口

- `POST /api/scripts/convert`：输入小说文本，返回 YAML、JSON 预览、校验结果和质量评分。
- `POST /api/scripts/validate`：校验 YAML 是否符合剧本 Schema。
- `POST /api/scripts/repair`：自动修复人物未列入场景 `characters` 的一致性问题。
- `POST /api/scripts/edit`：对单条台词、动作或旁白进行 AI 辅助编辑。
- `POST /api/scripts/edit-scene`：对整场景进行 AI 辅助编辑。
- `POST /api/scripts/render`：根据编辑后的剧本对象重新生成 YAML、校验结果和质量评分。
- `GET /api/scripts/history`：读取后端保存的转换历史。
- `DELETE /api/scripts/history/{id}`：删除单条转换历史。
- `DELETE /api/scripts/history`：清空转换历史。
- `GET /api/ai/status`：查看当前 AI 是否启用、模型和 Key 配置状态。
- `GET /api/schema`：返回 Schema 文档。

## 项目结构

```text
backend/   Spring Boot 后端
frontend/  Vue 3 + Element Plus 前端
docs/      YAML Schema、PR、Demo 与交付文档
```

## 依赖与原创性说明

本项目使用 Spring Boot、Spring AI、Vue 3、Element Plus、Axios、Vite、TypeScript、Jackson YAML 等第三方框架或库，这些依赖已在本文档中列出。

项目中的小说转剧本结构设计、规则兜底解析、AI 状态透明化、一致性修复、剧本质量评分、转换历史管理、角色关系图、场景分镜表和场景级 AI 编辑均为本项目自主实现。第三方依赖仅用于框架、UI、网络请求、序列化和测试能力。
