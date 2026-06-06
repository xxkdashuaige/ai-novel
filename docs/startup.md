# 项目启动说明

这份说明用于让别人拿到源码后，可以在本地启动“AI 小说转剧本工具”。项目分为后端和前端，需要分别打开两个终端窗口运行。下面命令中的 `D:\ai-novel` 是示例路径，如果源码放在其他目录，请替换成自己的实际项目路径。

## 1. 环境要求

请先确认电脑已经安装：

- JDK 21
- Maven
- Node.js 20 或更高版本
- pnpm

在 PowerShell 中可以用下面命令检查：

```powershell
java -version
mvn -version
node -v
pnpm -v
```

如果 `pnpm -v` 没有输出，可以先安装 pnpm：

```powershell
npm install -g pnpm
```

首次启动时 Maven 和 pnpm 都需要下载依赖，所以电脑需要能访问 Maven 仓库和 npm registry。

## 2. 启动后端

打开第一个 PowerShell 终端，进入后端目录：

```powershell
cd D:\ai-novel\backend
```

如果只是演示项目，不调用 AI，可以直接启动：

```powershell
mvn -s maven-settings.xml spring-boot:run
```

这种方式会使用后端内置的规则解析器，不需要 API Key，也可以完成基本转换演示。

如果要启用 DeepSeek AI 转换，请先设置环境变量：

```powershell
$env:OPENAI_API_KEY="替换成你的 DeepSeek API Key"
$env:OPENAI_BASE_URL="https://api.deepseek.com"
$env:OPENAI_MODEL="deepseek-v4-flash"

mvn -s maven-settings.xml spring-boot:run "-Dspring-boot.run.arguments=--app.ai.enabled=true"
```

后端启动成功后，默认地址是：

```text
http://127.0.0.1:8080
```

## 3. 启动前端

打开第二个 PowerShell 终端，进入前端目录：

```powershell
cd D:\ai-novel\frontend
```

首次运行需要安装依赖：

```powershell
pnpm install
```

然后启动前端：

```powershell
pnpm dev --host 127.0.0.1
```

启动成功后访问：

```text
http://127.0.0.1:5173/
```

前端会把 `/api` 请求代理到后端 `http://localhost:8080`，所以需要先保证后端已经启动。

## 4. 启动顺序

推荐顺序：

1. 先启动后端，确认 8080 端口正常。
2. 再启动前端，打开 5173 页面。
3. 在页面输入三章及以上小说内容。
4. 点击“一键转换”，等待 AI 或规则解析器生成剧本。

## 5. API Key 说明

API Key 不要写进代码，也不要提交到 GitHub。只需要在启动后端的终端里临时设置环境变量。

如果没有 API Key：

- 后端仍然可以启动。
- 不加 `--app.ai.enabled=true` 时，会使用规则解析器。
- AI 辅助编辑、AI 风格优化等功能需要可用的 API Key 才能获得更好的效果。

如果使用 DeepSeek：

```powershell
$env:OPENAI_API_KEY="你的 DeepSeek API Key"
$env:OPENAI_BASE_URL="https://api.deepseek.com"
$env:OPENAI_MODEL="deepseek-v4-flash"
```

## 6. 后端历史记录

项目会在后端保存最近 20 次转换历史，默认文件位置是：

```text
backend/data/history.json
```

这个文件属于运行时数据，不需要提交到 GitHub。如果想清空历史，可以在页面点击“清空历史”，或者删除这个文件后重启后端。

## 7. 常见问题

### 前端页面能打开，但点击转换请求失败

通常是后端没有启动，或者后端端口不是 8080。请确认后端终端没有报错，并访问：

```text
http://127.0.0.1:8080/api/schema
```

如果能看到 Schema 文档内容，说明后端正常。

### 后端启动时报 Java 版本错误

项目使用 Spring Boot 3.5，需要 JDK 21。请安装 JDK 21，并确认：

```powershell
java -version
```

输出中应该能看到版本 21。

### Maven 依赖下载失败

首次启动会下载依赖，需要网络。如果网络慢，可以重试：

```powershell
mvn -s maven-settings.xml test
```

### pnpm install 失败

先确认 Node.js 和 pnpm 已安装：

```powershell
node -v
pnpm -v
```

如果依赖下载慢，可以切换 npm 镜像后再安装：

```powershell
pnpm config set registry https://registry.npmmirror.com
pnpm install
```

### 端口被占用

后端默认端口是 8080，前端默认端口是 5173。如果端口被占用，请关闭占用端口的程序后重新启动。

## 8. 验证项目是否正常

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

如果这两个命令都通过，说明项目依赖和主要代码都没有问题。
