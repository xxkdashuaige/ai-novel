<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  convertNovel,
  editBeat,
  fetchSchema,
  renderScript,
  validateYaml,
  type ConversionResponse,
  type ScriptBeat,
  type ScriptDocument,
  type ValidationResult
} from './api'

interface HistoryItem {
  id: string
  title: string
  createdAt: string
  generationMode: string
  chapterCount: number
  sceneCount: number
  script: ScriptDocument
  yaml: string
  validation: ValidationResult
}

interface EditTarget {
  chapterIndex: number
  sceneIndex: number
  beatIndex: number
  actionType: 'polish-dialogue' | 'expand-action'
  beat: ScriptBeat
  sceneTitle: string
  characters: string[]
}

const HISTORY_KEY = 'ai-novel-script-history'

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
const preview = ref<ScriptDocument | null>(null)
const schema = ref<unknown>(null)
const validation = ref<ValidationResult | null>(null)
const history = ref<HistoryItem[]>([])
const loading = ref(false)
const schemaLoading = ref(false)
const schemaDialogVisible = ref(false)
const editDialogVisible = ref(false)
const editLoading = ref(false)
const editSuggestion = ref('')
const editMode = ref('')
const editTarget = ref<EditTarget | null>(null)

const chapterCount = computed(() => {
  const matches = novelText.value.match(/^第[一二三四五六七八九十百千万0-9]+章/gm)
  return matches?.length ?? 0
})

const sceneCount = computed(() => countScenes(preview.value))
const characterNames = computed(() => {
  const names = new Set<string>()
  preview.value?.chapters.forEach((chapter) => {
    chapter.scenes.forEach((scene) => {
      scene.characters?.forEach((character) => {
        if (character.trim()) {
          names.add(character.trim())
        }
      })
    })
  })
  return [...names]
})

onMounted(() => {
  history.value = readHistory()
})

async function handleConvert() {
  loading.value = true
  yaml.value = ''
  preview.value = null
  validation.value = null
  try {
    const result: ConversionResponse = await convertNovel(novelText.value)
    setConversionResult(result)
    saveHistory(result)
    ElMessage.success('转换完成，已保存到历史记录')
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

async function requestBeatEdit(target: EditTarget) {
  editTarget.value = target
  editSuggestion.value = ''
  editMode.value = target.actionType === 'polish-dialogue' ? '台词优化' : '动作补充'
  editDialogVisible.value = true
  editLoading.value = true
  try {
    const response = await editBeat({
      type: target.actionType,
      content: target.beat.content,
      speaker: target.beat.speaker,
      sceneTitle: target.sceneTitle,
      characters: target.characters
    })
    editSuggestion.value = response.result
    ElMessage.success(`${editMode.value}建议已生成`)
  } catch (error) {
    ElMessage.error(readError(error))
    editDialogVisible.value = false
  } finally {
    editLoading.value = false
  }
}

async function applyEditSuggestion() {
  if (!preview.value || !editTarget.value || !editSuggestion.value.trim()) {
    return
  }

  const nextScript = cloneScript(preview.value)
  const target = editTarget.value
  nextScript.chapters[target.chapterIndex].scenes[target.sceneIndex].beats[target.beatIndex].content = editSuggestion.value.trim()

  try {
    const result = await renderScript(nextScript)
    setConversionResult(result)
    saveHistory(result)
    editDialogVisible.value = false
    ElMessage.success('AI 编辑已应用，并重新生成 YAML')
  } catch (error) {
    ElMessage.error(readError(error))
  }
}

function handleDownload() {
  downloadFile('script.yaml', yaml.value, 'text/yaml;charset=utf-8')
}

function handleExportJson() {
  if (!preview.value) {
    ElMessage.warning('请先完成一次转换')
    return
  }
  downloadFile('script.json', JSON.stringify(preview.value, null, 2), 'application/json;charset=utf-8')
}

function handleExportText() {
  if (!preview.value) {
    ElMessage.warning('请先完成一次转换')
    return
  }
  downloadFile('script.txt', toReadableText(preview.value), 'text/plain;charset=utf-8')
}

function loadHistoryItem(item: HistoryItem) {
  preview.value = cloneScript(item.script)
  yaml.value = item.yaml
  validation.value = item.validation
  ElMessage.success('已加载历史记录')
}

function removeHistoryItem(id: string) {
  history.value = history.value.filter((item) => item.id !== id)
  writeHistory(history.value)
  ElMessage.success('历史记录已删除')
}

async function clearHistory() {
  if (!history.value.length) {
    return
  }
  await ElMessageBox.confirm('确定清空全部转换历史吗？', '清空历史', {
    confirmButtonText: '清空',
    cancelButtonText: '取消',
    type: 'warning'
  })
  history.value = []
  writeHistory(history.value)
  ElMessage.success('历史记录已清空')
}

function setConversionResult(result: ConversionResponse) {
  yaml.value = result.yaml
  preview.value = result.script
  validation.value = result.validation
}

function saveHistory(result: ConversionResponse) {
  const item: HistoryItem = {
    id: `${Date.now()}`,
    title: result.script.title || '未命名剧本',
    createdAt: new Date().toLocaleString(),
    generationMode: result.script.metadata?.generationMode || 'UNKNOWN',
    chapterCount: result.script.chapters.length,
    sceneCount: countScenes(result.script),
    script: result.script,
    yaml: result.yaml,
    validation: result.validation
  }
  history.value = [item, ...history.value.filter((historyItem) => historyItem.yaml !== item.yaml)].slice(0, 12)
  writeHistory(history.value)
}

function readHistory() {
  try {
    return JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]') as HistoryItem[]
  } catch {
    return []
  }
}

function writeHistory(items: HistoryItem[]) {
  localStorage.setItem(HISTORY_KEY, JSON.stringify(items))
}

function countScenes(script: ScriptDocument | null) {
  return script?.chapters.reduce((total, chapter) => total + chapter.scenes.length, 0) ?? 0
}

function cloneScript(script: ScriptDocument) {
  return JSON.parse(JSON.stringify(script)) as ScriptDocument
}

function downloadFile(filename: string, content: string, type: string) {
  const blob = new Blob([content], { type })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

function toReadableText(script: ScriptDocument) {
  return script.chapters
    .map((chapter) => {
      const scenes = chapter.scenes
        .map((scene) => {
          const beats = scene.beats.map((beat) => formatBeat(beat)).join('\n')
          return `【${scene.title}】${scene.location} / ${scene.timeOfDay}\n人物：${scene.characters.join('、') || '未标注'}\n${beats}`
        })
        .join('\n\n')
      return `${chapter.title}\n${chapter.summary}\n\n${scenes}`
    })
    .join('\n\n')
}

function formatBeat(beat: ScriptBeat) {
  if (beat.type === 'dialogue') {
    return `${beat.speaker || '角色'}：${beat.content}`
  }
  const labels: Record<string, string> = {
    action: '动作',
    narration: '旁白',
    transition: '转场'
  }
  return `[${labels[beat.type] ?? beat.type}] ${beat.content}`
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
        <el-dropdown :disabled="!yaml">
          <el-button size="large" :disabled="!yaml">导出结果</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleDownload">导出 YAML</el-dropdown-item>
              <el-dropdown-item @click="handleExportJson">导出 JSON</el-dropdown-item>
              <el-dropdown-item @click="handleExportText">导出 TXT</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
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

    <section class="history-panel">
      <div class="history-header">
        <div>
          <h2>转换历史</h2>
          <p>最近 12 次转换会保存在当前浏览器，便于回看和继续编辑。</p>
        </div>
        <el-button size="small" :disabled="!history.length" @click="clearHistory">清空历史</el-button>
      </div>
      <div v-if="history.length" class="history-list">
        <article v-for="item in history" :key="item.id" class="history-item">
          <div>
            <strong>{{ item.title }}</strong>
            <p>{{ item.createdAt }} · {{ item.generationMode }} · {{ item.chapterCount }} 章 / {{ item.sceneCount }} 场</p>
          </div>
          <div class="history-actions">
            <el-button size="small" type="primary" plain @click="loadHistoryItem(item)">加载</el-button>
            <el-button size="small" type="danger" plain @click="removeHistoryItem(item.id)">删除</el-button>
          </div>
        </article>
      </div>
      <div v-else class="empty-history">完成一次转换后，这里会自动保存历史记录。</div>
    </section>

    <section class="inspector-grid">
      <el-card class="panel" shadow="never">
        <template #header>
          <div class="panel-header">
            <span>剧本可视化预览</span>
            <el-tag v-if="preview" type="success">{{ sceneCount }} 个场景</el-tag>
          </div>
        </template>
        <div v-if="preview" class="script-preview">
          <div class="preview-meta">
            <strong>{{ preview.title }}</strong>
            <span>{{ preview.metadata?.generationMode || 'UNKNOWN' }}</span>
          </div>
          <div class="character-strip">
            <el-tag v-for="character in characterNames" :key="character" type="info">{{ character }}</el-tag>
          </div>
          <article v-for="(chapter, chapterIndex) in preview.chapters" :key="chapter.id" class="chapter-preview">
            <header>
              <h3>{{ chapter.title }}</h3>
              <p>{{ chapter.summary }}</p>
            </header>
            <section v-for="(scene, sceneIndex) in chapter.scenes" :key="scene.id" class="scene-preview">
              <div class="scene-title">
                <strong>{{ scene.title }}</strong>
                <span>{{ scene.location }} · {{ scene.timeOfDay }}</span>
              </div>
              <div class="scene-characters">
                <el-tag v-for="character in scene.characters" :key="character" size="small">{{ character }}</el-tag>
              </div>
              <div class="beat-list">
                <div v-for="(beat, beatIndex) in scene.beats" :key="`${scene.id}-${beatIndex}`" :class="['beat-item', `beat-item--${beat.type}`]">
                  <div class="beat-heading">
                    <span class="beat-type">{{ beat.type }}</span>
                    <div class="beat-actions">
                      <el-button
                        v-if="beat.type === 'dialogue'"
                        size="small"
                        text
                        @click="requestBeatEdit({ chapterIndex, sceneIndex, beatIndex, actionType: 'polish-dialogue', beat, sceneTitle: scene.title, characters: scene.characters })"
                      >
                        优化台词
                      </el-button>
                      <el-button
                        v-if="beat.type === 'action' || beat.type === 'narration'"
                        size="small"
                        text
                        @click="requestBeatEdit({ chapterIndex, sceneIndex, beatIndex, actionType: 'expand-action', beat, sceneTitle: scene.title, characters: scene.characters })"
                      >
                        补充动作
                      </el-button>
                    </div>
                  </div>
                  <strong v-if="beat.speaker">{{ beat.speaker }}</strong>
                  <p>{{ beat.content }}</p>
                </div>
              </div>
            </section>
          </article>
        </div>
        <div v-else class="empty-preview">转换完成后，这里会按章节和场景展示剧本预览。</div>
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
        <el-alert
          v-if="validation?.warnings?.length"
          title="一致性提醒"
          type="warning"
          :closable="false"
          show-icon
        >
          <ul>
            <li v-for="warning in validation.warnings" :key="warning">{{ warning }}</li>
          </ul>
        </el-alert>
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

    <el-dialog v-model="editDialogVisible" :title="`AI 辅助编辑：${editMode}`" width="720px">
      <div class="edit-dialog">
        <div class="edit-block">
          <span>原文</span>
          <p>{{ editTarget?.beat.content }}</p>
        </div>
        <div class="edit-block edit-block--suggestion">
          <span>AI 建议</span>
          <div v-if="editLoading" class="edit-loading">DeepSeek 正在生成编辑建议...</div>
          <p v-else>{{ editSuggestion }}</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="editLoading || !editSuggestion" @click="applyEditSuggestion">应用到剧本</el-button>
      </template>
    </el-dialog>
  </main>
</template>
