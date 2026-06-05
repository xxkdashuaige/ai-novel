# AI 小说转剧本工具

这是一个使用 Spring Boot、Spring AI、Vue 3 和 Element Plus 实现的编程题项目。它可以把三章及以上的小说文本转换为结构化剧本 YAML，并提供 Schema 说明文档。

## 项目结构

```text
backend/   Spring Boot 后端
frontend/  Vue 3 + Element Plus 前端
docs/      YAML Schema 与设计文档
```

## 后端启动

```bash
cd backend
mvn -s maven-settings.xml spring-boot:run
```

默认使用规则解析器，可离线运行。如果要启用 Spring AI：

```bash
set OPENAI_API_KEY=你的Key
mvn -s maven-settings.xml spring-boot:run -Dspring-boot.run.arguments="--app.ai.enabled=true"
```

如果使用 DeepSeek API Key，可以继续使用 Spring AI 的 OpenAI-compatible 客户端：

```bash
set OPENAI_API_KEY=你的DeepSeekKey
set OPENAI_BASE_URL=https://api.deepseek.com
set OPENAI_MODEL=deepseek-v4-flash
mvn -s maven-settings.xml spring-boot:run -Dspring-boot.run.arguments="--app.ai.enabled=true"
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
- `GET /api/schema`：返回 Schema 文档。

## 为什么使用 Spring AI

Spring AI 可以把大模型能力封装在 Spring Boot 应用内部，并用 `ChatClient` 统一调用模型。项目没有直接信任模型输出 YAML，而是先生成 Java DTO，再由后端转换成 YAML，降低格式错误风险。没有 API Key 时，规则解析器会作为兜底，保证项目可以稳定演示。
