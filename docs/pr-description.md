# PR 描述：新增剧本角色关系图与 AI 转换体验增强

## 标题

新增剧本角色关系图与 AI 转换体验增强

## 功能描述

本 PR 为 AI 小说转剧本工具新增轻量版角色关系图，并增强 AI 转换过程中的状态反馈、历史记录管理和一致性修复体验。

用户在完成小说转剧本后，可以在“剧本可视化预览”中看到角色关系图。系统会根据同一场景中共同出现的角色自动生成关系连线，节点数字表示角色出场场景数，连线粗细表示角色同场次数，方便用户快速理解剧本人物关系。

页面会展示当前 AI 模型状态，例如是否启用 DeepSeek、API Key 是否配置、当前使用模型等。转换历史改为后端保存，支持加载、删除和清空。对于“对白人物未列入场景 characters”的一致性提醒，新增一键修复功能。

## 实现思路

后端新增 AI 状态接口、历史记录服务和一致性修复服务。转换结果中增加 `generationMessage`，用于告诉用户当前结果来自 DeepSeek AI、规则解析器，还是 AI 失败后的规则兜底。

前端新增角色关系图计算逻辑，不额外引入图表库，直接基于剧本中的 `scene.characters` 和对白 `speaker` 统计人物共现关系，并使用 SVG 绘制节点和连线，降低依赖成本。

历史记录从浏览器 `localStorage` 调整为后端 JSON 文件存储，默认保存最近 20 条转换记录。运行时历史文件已加入 `.gitignore`，避免提交到仓库。

## 测试方式

后端验证：

```powershell
cd D:\ai-novel\backend
mvn -s maven-settings.xml test
```

验证内容包括 Spring Boot 上下文启动测试、AI 状态测试、历史记录测试、一致性修复测试、规则解析器测试和 YAML 校验测试。

前端验证：

```powershell
cd D:\ai-novel\frontend
pnpm build
```

前端构建通过。Vite 仍会提示部分 chunk 体积较大，这是 Element Plus 整体引入导致的构建体积提示，不影响项目运行。

## 依赖与原创性说明

本项目使用 Spring Boot、Spring AI、Vue 3、Element Plus、Axios、Vite、TypeScript、Jackson YAML 等第三方框架或库，这些依赖已在 README 中说明。

项目的小说转剧本结构设计、规则兜底解析、AI 状态透明化、一致性修复、后端历史记录和角色关系图均为本项目自主实现。未复用本人过往代码片段，也未复制第三方业务代码。

## 提交记录说明

项目采用多次 commit 逐步提交功能，避免最后一天一次性导入所有代码。主要提交包括：

- `08053c2 feat: add ai novel script tool`
- `7388c75 feat: add script preview and export tools`
- `e761cf9 feat: add history and ai editing`
- `14db8f8 feat: improve ai status and script analysis`

其中 `14db8f8` 为本次主要功能增强提交。
