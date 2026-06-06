# AI 小说转剧本工具

这是一个使用 Spring Boot、Spring AI、Vue 3 和 Element Plus 实现的编程题项目。它可以把三章及以上的小说文本转换为结构化剧本 YAML，并提供 Schema 说明文档。

项目还支持 AI 状态提示、剧本可视化预览、一致性提醒一键修复、后端转换历史记录和 AI 辅助编辑。

## 项目结构

```text
backend/   Spring Boot 后端
frontend/  Vue 3 + Element Plus 前端
docs/      YAML Schema 与设计文档
```

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

## 前端启动

```bash
cd frontend
pnpm install --store-dir D:\ai-novel\.pnpm-store
pnpm dev
```

前端默认代理后端 `http://localhost:8080`。

## 核心接口

- `POST /api/scripts/convert`：输入小说文本，返回 YAML、JSON 预览和校验结果。
- `POST /api/scripts/validate`：校验 YAML 是否符合剧本 Schema。
- `POST /api/scripts/repair`：自动修复人物未列入场景 characters 等一致性提醒。
- `GET /api/scripts/history`：读取后端保存的转换历史。
- `DELETE /api/scripts/history/{id}`：删除单条转换历史。
- `DELETE /api/scripts/history`：清空转换历史。
- `GET /api/ai/status`：查看当前 AI 是否启用、模型和 Key 配置状态。
- `GET /api/schema`：返回 Schema 文档。

## 为什么使用 Spring AI

Spring AI 可以把大模型能力封装在 Spring Boot 应用内部，并用 `ChatClient` 统一调用模型。项目没有直接信任模型输出 YAML，而是先生成 Java DTO，再由后端转换成 YAML，降低格式错误风险。没有 API Key 时，规则解析器会作为兜底，保证项目可以稳定演示。
