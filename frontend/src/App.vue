<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { convertNovel, fetchSchema, validateYaml, type ConversionResponse, type ValidationResult } from './api'

const sampleText = `第一章 雨夜
林舟推开木门，雨水扑面而来。
林舟：“我们必须马上离开。”

第二章 旧城
苏晚站在街角，望向远处的钟楼。
苏晚说：“他还会回来吗？”

第三章 黎明
天光穿过云层，所有人都安静下来。
林舟回答：“会的。”`

const novelText = ref(sampleText)
const yaml = ref('')
const preview = ref<unknown>(null)
const schema = ref<unknown>(null)
const validation = ref<ValidationResult | null>(null)
const loading = ref(false)
const schemaLoading = ref(false)
const schemaDialogVisible = ref(false)

const chapterCount = computed(() => {
  const matches = novelText.value.match(/^第[一二三四五六七八九十百千万0-9]+章/gm)
  return matches?.length ?? 0
})

async function handleConvert() {
  loading.value = true
  yaml.value = ''
  preview.value = null
  validation.value = null
  try {
    const result: ConversionResponse = await convertNovel(novelText.value)
    yaml.value = result.yaml
    preview.value = result.script
    validation.value = result.validation
    ElMessage.success('转换完成')
  } catch (error) {
    ElMessage.error(readError(error))
  } finally {
    loading.value = false
  }
}

async function handleValidate() {
  if (!yaml.value.trim()) {
    ElMessage.warning('请先生成或粘贴 YAML')
    return
  }

  try {
    validation.value = await validateYaml(yaml.value)
    ElMessage[validation.value.valid ? 'success' : 'error'](validation.value.valid ? 'YAML 校验通过' : 'YAML 校验失败')
  } catch (error) {
    ElMessage.error(readError(error))
  }
}

async function handleLoadSchema() {
  schemaLoading.value = true
  try {
    schema.value = await fetchSchema()
    schemaDialogVisible.value = true
    ElMessage.success('Schema 已加载')
  } catch (error) {
    ElMessage.error(readError(error))
  } finally {
    schemaLoading.value = false
  }
}

function handleDownload() {
  const blob = new Blob([yaml.value], { type: 'text/yaml;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'script.yaml'
  link.click()
  URL.revokeObjectURL(url)
}

function readError(error: unknown) {
  if (typeof error === 'object' && error && 'response' in error) {
    const response = (error as { response?: { status?: number, data?: unknown } }).response
    const data = response?.data
    if (typeof data === 'object' && data && 'message' in data) {
      return String((data as { message?: unknown }).message)
    }
    if (typeof data === 'string' && data.trim()) {
      return data
    }
    return `服务返回异常${response?.status ? `（HTTP ${response.status}）` : ''}`
  }
  return '请求失败，请确认后端 8080 和前端 5173 都已启动'
}
</script>

<template>
  <main class="page-shell">
    <section class="hero-card">
      <p class="eyebrow">Spring AI · Vue 3 · Element Plus</p>
      <h1>AI 小说转剧本工具</h1>
      <p class="subtitle">输入三章及以上小说文本，生成可编辑、可校验、可下载的结构化 YAML 剧本。</p>
      <div class="hero-actions">
        <el-button type="primary" size="large" :loading="loading" :disabled="loading" @click="handleConvert">
          {{ loading ? 'AI 转换中' : '一键转换' }}
        </el-button>
        <el-button size="large" :disabled="loading" @click="handleValidate">校验 YAML</el-button>
        <el-button size="large" :loading="schemaLoading" :disabled="loading" @click="handleLoadSchema">查看 Schema</el-button>
        <el-button size="large" :disabled="!yaml" @click="handleDownload">下载 YAML</el-button>
      </div>
      <div v-if="loading" class="ai-status" role="status" aria-live="polite">
        <span class="ai-status__dot"></span>
        <span>DeepSeek 正在理解章节、人物和对白，生成结构化 YAML 剧本，请稍候...</span>
      </div>
    </section>

    <section class="workspace">
      <el-card class="panel input-panel" shadow="never">
        <template #header>
          <div class="panel-header">
            <span>小说文本</span>
            <el-tag :type="chapterCount >= 3 ? 'success' : 'warning'">已识别 {{ chapterCount }} 章</el-tag>
          </div>
        </template>
        <el-input
          v-model="novelText"
          type="textarea"
          :rows="24"
          resize="none"
          placeholder="请粘贴至少三章小说文本，例如：第一章、第二章、第三章..."
        />
      </el-card>

      <el-card class="panel output-panel" shadow="never">
        <template #header>
          <div class="panel-header">
            <span>YAML 剧本</span>
            <el-tag v-if="validation" :type="validation.valid ? 'success' : 'danger'">
              {{ validation.valid ? '校验通过' : '校验失败' }}
            </el-tag>
          </div>
        </template>
        <div class="output-box">
          <el-input v-model="yaml" type="textarea" :rows="24" resize="none" placeholder="转换结果会显示在这里" />
          <div v-if="loading" class="output-loading">
            <div class="output-loading__pulse"></div>
            <strong>AI 正在转换中</strong>
            <span>正在将小说内容拆解为章节、场景、动作和对白...</span>
          </div>
        </div>
      </el-card>
    </section>

    <section class="inspector-grid">
      <el-card class="panel" shadow="never">
        <template #header>JSON 预览</template>
        <pre>{{ JSON.stringify(preview, null, 2) }}</pre>
      </el-card>

      <el-card class="panel" shadow="never">
        <template #header>校验与 Schema</template>
        <el-alert
          v-if="validation && !validation.valid"
          title="Schema 校验失败"
          type="error"
          :closable="false"
          show-icon
        >
          <ul>
            <li v-for="error in validation.errors" :key="error">{{ error }}</li>
          </ul>
        </el-alert>
        <el-alert v-else-if="validation?.valid" title="当前 YAML 符合剧本 Schema" type="success" :closable="false" show-icon />
        <pre>{{ JSON.stringify(schema, null, 2) }}</pre>
      </el-card>
    </section>

    <el-dialog v-model="schemaDialogVisible" title="剧本 YAML Schema" width="680px" class="schema-dialog">
      <div class="schema-summary">
        <p>剧本结构采用“作品 -> 章节 -> 场景 -> 节拍”的层级，方便作者继续编辑、扩写和导出。</p>
        <div class="schema-rules">
          <el-tag type="success">至少 3 章</el-tag>
          <el-tag>每章至少 1 个场景</el-tag>
          <el-tag>beat 支持 4 种类型</el-tag>
        </div>
      </div>
      <pre>{{ JSON.stringify(schema, null, 2) }}</pre>
      <template #footer>
        <el-button type="primary" @click="schemaDialogVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </main>
</template>
