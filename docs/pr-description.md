# PR 描述：完善 AI 小说转剧本工具的质量分析、场景编辑与交付文档

## 标题

完善 AI 小说转剧本工具的质量分析、场景编辑与交付文档

## 功能描述

本 PR 在原有小说转剧本、Schema 校验、历史记录和可视化预览基础上，继续增强项目的产品完整度和课程交付质量。

新增剧本质量评分能力。后端会根据 Schema 校验结果、章节数量、场景地点、时间、人物列表、对白说话人和 beat 内容生成质量报告。前端会展示质量分、质量等级、问题列表和优化建议，方便用户判断当前剧本初稿是否适合继续打磨。

新增场景级 AI 编辑能力。用户不仅可以对单条台词或动作进行优化，还可以对整场景执行“优化整场对白”“补充动作细节”“增强冲突节奏”等操作。后端会优先调用 AI，AI 不可用或返回异常时使用规则兜底，保证演示稳定性。

前端增强了转换过程反馈、历史记录管理和导出能力。转换时会展示准备文本、调用 AI/规则解析、结构化剧本、校验质量、保存历史等阶段。历史记录支持搜索、生成模式筛选和质量分展示。导出结果支持 YAML、JSON、TXT、分镜 CSV、DOC 和打印保存 PDF。

本 PR 还补充了交付文档，包括 YAML Schema 设计说明、Demo 视频演讲稿、环境变量示例和 GitHub Actions 自动测试配置。

## 实现思路

后端新增 `ScriptQualityService`，将质量分析从转换服务中拆出，保持职责单一。`ConversionResponse` 和 `ScriptHistoryItem` 增加 `quality` 字段，使每次转换结果和历史记录都能携带质量评分。

后端新增 `/api/scripts/edit-scene` 接口，使用 `SceneEditRequest` 接收场景标题、人物和 beats。AI 模式要求模型只返回 JSON 数组，并尝试映射回 `ScriptBeat`。如果 AI 未启用、返回为空或解析失败，则使用规则兜底生成可展示的场景编辑结果。

前端继续使用单页应用结构，在 `App.vue` 中增加轻量交互：转换阶段条、质量评分卡片、历史筛选、场景级 AI 编辑下拉菜单和导出入口。导出 DOC 使用 Word-compatible HTML，PDF 使用浏览器打印能力，避免引入重型服务端导出依赖。

文档层面新增 `docs/yaml-schema-design.md` 说明 Schema 字段、校验规则和设计原因；新增 `docs/demo-script.md` 作为录制演示视频的讲解稿；新增 `.env.example` 说明 DeepSeek 环境变量配置方式。

## 测试方式

后端验证：

```powershell
cd D:\ai-novel\backend
mvn -s maven-settings.xml test
```

验证内容包括 Spring Boot 上下文启动测试、AI 状态测试、AI 编辑服务测试、剧本质量分析测试、历史记录测试、一致性修复测试、规则解析器测试和 YAML 校验测试。

前端验证：

```powershell
cd D:\ai-novel\frontend
pnpm build
```

前端构建通过。Vite 仍会提示部分 chunk 体积较大，这是 Element Plus 等前端依赖导致的构建体积提示，不影响项目运行。

安全验证：

- API Key 仅通过环境变量配置。
- `.env.example` 只包含占位符，不包含真实 Key。
- 源码中不应出现 `sk-` 形式的真实 API Key。

## 依赖与原创性说明

本项目使用 Spring Boot、Spring AI、Vue 3、Element Plus、Axios、Vite、TypeScript、Jackson YAML 等第三方框架或库，这些依赖已在 README 中说明。

项目的小说转剧本结构设计、规则兜底解析、AI 状态透明化、一致性修复、后端历史记录、角色关系图、场景分镜表、剧本质量评分和场景级 AI 编辑均为本项目自主实现。第三方依赖仅用于框架、UI、网络请求、序列化和测试能力。

## 提交记录说明

项目采用多次 commit 逐步提交功能，避免最后一次性导入所有代码。主要提交包括：

- `08053c2 feat: add ai novel script tool`
- `7388c75 feat: add script preview and export tools`
- `e761cf9 feat: add history and ai editing`
- `14db8f8 feat: improve ai status and script analysis`
- `6e5f714 docs: add dependency and pr documentation`

后续提交建议使用：

- `feat: add quality analysis and scene editing`
- `docs: improve project delivery documentation`

## Demo 说明

Demo 视频链接暂未补充。录制完成后，可将链接补充到 README 和本 PR 文档中。

录制讲解稿见：

```text
docs/demo-script.md
```
