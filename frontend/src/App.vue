<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  clearHistoryItems,
  convertNovel,
  deleteHistoryItem,
  editBeat,
  editScene,
  fetchAiStatus,
  fetchHistory,
  fetchSchema,
  repairScript,
  renderScript,
  validateYaml,
  type AiStatusResponse,
  type ConversionResponse,
  type QualityReport,
  type ScriptBeat,
  type ScriptDocument,
  type ScriptHistoryItem,
  type ScriptScene,
  type ValidationResult
} from './api'

interface EditTarget {
  chapterIndex: number
  sceneIndex: number
  beatIndex: number
  actionType: EditActionType
  beat: ScriptBeat
  sceneTitle: string
  characters: string[]
}

type EditActionType = 'polish-dialogue' | 'expand-action' | 'enhance-conflict' | 'short-drama-style' | 'compress-dialogue' | 'add-camera-language'
type SceneEditActionType = 'polish-scene-dialogue' | 'expand-scene-action' | 'enhance-scene-conflict'

interface EditAction {
  type: EditActionType
  label: string
  beatTypes?: string[]
}

interface SceneEditAction {
  type: SceneEditActionType
  label: string
}

interface CharacterRelationNode {
  name: string
  sceneCount: number
  dialogueCount: number
  x: number
  y: number
  radius: number
}

interface CharacterRelationLink {
  source: string
  target: string
  weight: number
  x1: number
  y1: number
  x2: number
  y2: number
}

interface CharacterRelationGraph {
  nodes: CharacterRelationNode[]
  links: CharacterRelationLink[]
  maxWeight: number
}

interface ShotRow {
  id: string
  index: number
  chapter: string
  scene: string
  location: string
  timeOfDay: string
  characters: string
  type: string
  speaker: string
  content: string
}

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
const quality = ref<QualityReport | null>(null)
const history = ref<ScriptHistoryItem[]>([])
const aiStatus = ref<AiStatusResponse | null>(null)
const loading = ref(false)
const schemaLoading = ref(false)
const historyLoading = ref(false)
const repairLoading = ref(false)
const schemaDialogVisible = ref(false)
const editDialogVisible = ref(false)
const editLoading = ref(false)
const editSuggestion = ref('')
const editMode = ref('')
const editTarget = ref<EditTarget | null>(null)
const activePreviewTab = ref('chapters')
const conversionStepIndex = ref(0)
const historyKeyword = ref('')
const historyModeFilter = ref('')
const sceneEditLoadingKey = ref('')
let conversionProgressTimer: number | undefined

const editActions: EditAction[] = [
  { type: 'polish-dialogue', label: '优化台词', beatTypes: ['dialogue'] },
  { type: 'compress-dialogue', label: '压缩对白', beatTypes: ['dialogue'] },
  { type: 'expand-action', label: '补充动作', beatTypes: ['action', 'narration'] },
  { type: 'add-camera-language', label: '补充镜头语言', beatTypes: ['action', 'narration', 'transition'] },
  { type: 'enhance-conflict', label: '增强冲突' },
  { type: 'short-drama-style', label: '改成短剧风格' }
]

const sceneEditActions: SceneEditAction[] = [
  { type: 'polish-scene-dialogue', label: '优化整场对白' },
  { type: 'expand-scene-action', label: '补充动作细节' },
  { type: 'enhance-scene-conflict', label: '增强冲突节奏' }
]

const conversionSteps = ['准备文本', '调用 AI/规则解析', '结构化剧本', '校验质量', '保存历史']

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
const relationshipGraph = computed(() => buildCharacterRelationGraph(preview.value))
const shotRows = computed(() => buildShotRows(preview.value))
const filteredHistory = computed(() => {
  const keyword = historyKeyword.value.trim().toLowerCase()
  return history.value.filter((item) => {
    const matchesKeyword = !keyword || [
      item.title,
      item.createdAt,
      item.generationMessage,
      formatGenerationMode(item.generationMode)
    ].some((text) => String(text || '').toLowerCase().includes(keyword))
    const matchesMode = !historyModeFilter.value || item.generationMode === historyModeFilter.value
    return matchesKeyword && matchesMode
  })
})

onMounted(async () => {
  await Promise.all([loadAiStatus(), loadHistory()])
})

async function handleConvert() {
  loading.value = true
  startConversionProgress()
  yaml.value = ''
  preview.value = null
  validation.value = null
  quality.value = null
  try {
    const result: ConversionResponse = await convertNovel(novelText.value)
    conversionStepIndex.value = conversionSteps.length - 1
    setConversionResult(result)
    await loadHistory()
    ElMessage.success(result.script.metadata?.generationMessage || '转换完成，已保存到后端历史记录')
  } catch (error) {
    ElMessage.error(readError(error))
  } finally {
    stopConversionProgress()
    loading.value = false
  }
}

function startConversionProgress() {
  conversionStepIndex.value = 0
  window.clearInterval(conversionProgressTimer)
  conversionProgressTimer = window.setInterval(() => {
    conversionStepIndex.value = Math.min(conversionStepIndex.value + 1, conversionSteps.length - 2)
  }, 1200)
}

function stopConversionProgress() {
  window.clearInterval(conversionProgressTimer)
  conversionProgressTimer = undefined
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
  editMode.value = editActionLabel(target.actionType)
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
    ElMessage.success(`${editMode.value}建议已生成（${formatGenerationMode(response.generationMode)}）`)
  } catch (error) {
    ElMessage.error(readError(error))
    editDialogVisible.value = false
  } finally {
    editLoading.value = false
  }
}

function availableEditActions(beat: ScriptBeat) {
  return editActions.filter((action) => !action.beatTypes || action.beatTypes.includes(beat.type))
}

function editActionLabel(type: EditActionType) {
  return editActions.find((action) => action.type === type)?.label || '内容优化'
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
    await loadHistory()
    editDialogVisible.value = false
    ElMessage.success('AI 编辑已应用，并重新生成 YAML')
  } catch (error) {
    ElMessage.error(readError(error))
  }
}

async function requestSceneEdit(chapterIndex: number, sceneIndex: number, actionType: SceneEditActionType, scene: ScriptScene) {
  if (!preview.value) {
    return
  }
  const loadingKey = `${chapterIndex}-${sceneIndex}-${actionType}`
  sceneEditLoadingKey.value = loadingKey
  try {
    const response = await editScene({
      type: actionType,
      sceneTitle: scene.title,
      characters: scene.characters,
      beats: scene.beats
    })
    const nextScript = cloneScript(preview.value)
    nextScript.chapters[chapterIndex].scenes[sceneIndex].beats = response.beats
    const result = await renderScript(nextScript)
    setConversionResult(result)
    await loadHistory()
    ElMessage.success(`${sceneEditActionLabel(actionType)}已应用（${formatGenerationMode(response.generationMode)}）`)
  } catch (error) {
    ElMessage.error(readError(error))
  } finally {
    sceneEditLoadingKey.value = ''
  }
}

function sceneEditActionLabel(type: SceneEditActionType) {
  return sceneEditActions.find((action) => action.type === type)?.label || '场景优化'
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

function handleExportWord() {
  if (!preview.value) {
    ElMessage.warning('请先完成一次转换')
    return
  }
  downloadFile('script.doc', toDocumentHtml(preview.value), 'application/msword;charset=utf-8')
}

function handlePrintPdf() {
  if (!preview.value) {
    ElMessage.warning('请先完成一次转换')
    return
  }
  const printWindow = window.open('', '_blank')
  if (!printWindow) {
    ElMessage.warning('浏览器阻止了打印窗口，请允许弹窗后重试')
    return
  }
  printWindow.document.write(toDocumentHtml(preview.value))
  printWindow.document.close()
  printWindow.focus()
  printWindow.print()
}

function handleExportShotCsv() {
  if (!shotRows.value.length) {
    ElMessage.warning('请先完成一次转换')
    return
  }
  downloadFile('shot-table.csv', toShotCsv(shotRows.value), 'text/csv;charset=utf-8')
}

async function handleRepairConsistency() {
  if (!preview.value) {
    ElMessage.warning('请先完成一次转换')
    return
  }
  repairLoading.value = true
  try {
    const result = await repairScript(preview.value)
    setConversionResult(result)
    await loadHistory()
    ElMessage.success('已自动补全场景人物，并重新生成 YAML')
  } catch (error) {
    ElMessage.error(readError(error))
  } finally {
    repairLoading.value = false
  }
}

function loadHistoryItem(item: ScriptHistoryItem) {
  preview.value = cloneScript(item.script)
  yaml.value = item.yaml
  validation.value = item.validation
  quality.value = item.quality || null
  ElMessage.success('已加载历史记录')
}

async function removeHistoryItem(id: string) {
  try {
    await deleteHistoryItem(id)
    await loadHistory()
    ElMessage.success('历史记录已删除')
  } catch (error) {
    ElMessage.error(readError(error))
  }
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
  try {
    await clearHistoryItems()
    await loadHistory()
    ElMessage.success('历史记录已清空')
  } catch (error) {
    ElMessage.error(readError(error))
  }
}

function setConversionResult(result: ConversionResponse) {
  yaml.value = result.yaml
  preview.value = result.script
  validation.value = result.validation
  quality.value = result.quality
}

async function loadAiStatus() {
  try {
    aiStatus.value = await fetchAiStatus()
  } catch (error) {
    ElMessage.warning(readError(error))
  }
}

async function loadHistory() {
  historyLoading.value = true
  try {
    history.value = await fetchHistory()
  } catch (error) {
    ElMessage.warning(readError(error))
  } finally {
    historyLoading.value = false
  }
}

function countScenes(script: ScriptDocument | null) {
  return script?.chapters.reduce((total, chapter) => total + chapter.scenes.length, 0) ?? 0
}

function buildShotRows(script: ScriptDocument | null): ShotRow[] {
  if (!script) {
    return []
  }
  const rows: ShotRow[] = []
  script.chapters.forEach((chapter, chapterIndex) => {
    chapter.scenes.forEach((scene, sceneIndex) => {
      scene.beats.forEach((beat, beatIndex) => {
        rows.push({
          id: `${chapter.id}-${scene.id}-${beatIndex}`,
          index: rows.length + 1,
          chapter: chapter.title || `第 ${chapterIndex + 1} 章`,
          scene: scene.title || `场景 ${sceneIndex + 1}`,
          location: scene.location || '未标注',
          timeOfDay: scene.timeOfDay || '未标注',
          characters: scene.characters?.filter(Boolean).join('、') || '未标注',
          type: formatBeatType(beat.type),
          speaker: beat.speaker || '-',
          content: beat.content
        })
      })
    })
  })
  return rows
}

function buildCharacterRelationGraph(script: ScriptDocument | null): CharacterRelationGraph {
  if (!script) {
    return { nodes: [], links: [], maxWeight: 0 }
  }

  const stats = new Map<string, { sceneCount: number, dialogueCount: number }>()
  const pairWeights = new Map<string, number>()

  script.chapters.forEach((chapter) => {
    chapter.scenes.forEach((scene) => {
      const sceneNames = new Set<string>()
      scene.characters?.forEach((character) => addCharacterName(sceneNames, character))
      scene.beats.forEach((beat) => {
        if (beat.speaker) {
          addCharacterName(sceneNames, beat.speaker)
          const stat = stats.get(beat.speaker.trim()) ?? { sceneCount: 0, dialogueCount: 0 }
          stat.dialogueCount += 1
          stats.set(beat.speaker.trim(), stat)
        }
      })

      const names = [...sceneNames].sort((left, right) => left.localeCompare(right, 'zh-CN'))
      names.forEach((name) => {
        const stat = stats.get(name) ?? { sceneCount: 0, dialogueCount: 0 }
        stat.sceneCount += 1
        stats.set(name, stat)
      })
      names.forEach((source, sourceIndex) => {
        names.slice(sourceIndex + 1).forEach((target) => {
          const key = relationKey(source, target)
          pairWeights.set(key, (pairWeights.get(key) ?? 0) + 1)
        })
      })
    })
  })

  const orderedNames = [...stats.entries()]
    .sort((left, right) => right[1].sceneCount - left[1].sceneCount || left[0].localeCompare(right[0], 'zh-CN'))
    .map(([name]) => name)
  const centerX = 260
  const centerY = 160
  const radiusX = orderedNames.length <= 2 ? 110 : 185
  const radiusY = orderedNames.length <= 2 ? 0 : 108

  const nodes = orderedNames.map((name, index) => {
    const angle = orderedNames.length === 1 ? 0 : (Math.PI * 2 * index) / orderedNames.length - Math.PI / 2
    const stat = stats.get(name) ?? { sceneCount: 0, dialogueCount: 0 }
    return {
      name,
      sceneCount: stat.sceneCount,
      dialogueCount: stat.dialogueCount,
      x: orderedNames.length === 1 ? centerX : centerX + Math.cos(angle) * radiusX,
      y: orderedNames.length === 1 ? centerY : centerY + Math.sin(angle) * radiusY,
      radius: Math.min(30, 16 + stat.sceneCount * 2)
    }
  })
  const nodeMap = new Map(nodes.map((node) => [node.name, node]))
  const links = [...pairWeights.entries()].map(([key, weight]) => {
    const [source, target] = key.split('\u0000')
    const sourceNode = nodeMap.get(source)
    const targetNode = nodeMap.get(target)
    return sourceNode && targetNode
      ? { source, target, weight, x1: sourceNode.x, y1: sourceNode.y, x2: targetNode.x, y2: targetNode.y }
      : null
  }).filter((link): link is CharacterRelationLink => link !== null)

  return {
    nodes,
    links,
    maxWeight: Math.max(0, ...links.map((link) => link.weight))
  }
}

function addCharacterName(names: Set<string>, character: string) {
  const normalized = character.trim()
  if (normalized) {
    names.add(normalized)
  }
}

function relationKey(source: string, target: string) {
  return [source, target].sort((left, right) => left.localeCompare(right, 'zh-CN')).join('\u0000')
}

function cloneScript(script: ScriptDocument) {
  return JSON.parse(JSON.stringify(script)) as ScriptDocument
}

function formatGenerationMode(mode?: string) {
  const labels: Record<string, string> = {
    AI_DEEPSEEK: 'DeepSeek AI',
    AI_OPENAI_COMPATIBLE: 'OpenAI-compatible AI',
    RULE_BASED: '规则解析器',
    RULE_BASED_FALLBACK: '规则兜底'
  }
  return labels[mode || ''] || mode || '未知来源'
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
  return `[${formatBeatType(beat.type)}] ${beat.content}`
}

function formatBeatType(type: string) {
  const labels: Record<string, string> = {
    dialogue: '对白',
    action: '动作',
    narration: '旁白',
    transition: '转场'
  }
  return labels[type] ?? type
}

function toShotCsv(rows: ShotRow[]) {
  const headers = ['序号', '章节', '场景', '地点', '时间', '人物', '类型', '说话人', '内容']
  const lines = rows.map((row) => [
    row.index,
    row.chapter,
    row.scene,
    row.location,
    row.timeOfDay,
    row.characters,
    row.type,
    row.speaker,
    row.content
  ].map(csvCell).join(','))
  return `\uFEFF${headers.join(',')}\n${lines.join('\n')}`
}

function toDocumentHtml(script: ScriptDocument) {
  const body = script.chapters.map((chapter) => `
    <section class="chapter">
      <h2>${escapeHtml(chapter.title)}</h2>
      <p class="summary">${escapeHtml(chapter.summary)}</p>
      ${chapter.scenes.map((scene) => `
        <article class="scene">
          <h3>${escapeHtml(scene.title)} <span>${escapeHtml(scene.location)} / ${escapeHtml(scene.timeOfDay)}</span></h3>
          <p class="characters">人物：${escapeHtml(scene.characters.join('、') || '未标注')}</p>
          ${scene.beats.map((beat) => `<p class="beat">${escapeHtml(formatBeat(beat))}</p>`).join('')}
        </article>
      `).join('')}
    </section>
  `).join('')
  return `<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <title>${escapeHtml(script.title)}</title>
  <style>
    body { font-family: "Microsoft YaHei", sans-serif; color: #1f2a24; line-height: 1.75; padding: 36px; }
    h1 { text-align: center; letter-spacing: 0.08em; }
    h2 { margin-top: 32px; border-bottom: 1px solid #d8ded5; padding-bottom: 8px; }
    h3 { margin-bottom: 4px; }
    h3 span { color: #66776b; font-size: 0.86em; font-weight: 400; }
    .summary, .characters { color: #5e7064; }
    .scene { margin: 18px 0; padding: 14px 18px; border: 1px solid #e4e8df; border-radius: 10px; }
    .beat { margin: 8px 0; }
  </style>
</head>
<body>
  <h1>${escapeHtml(script.title)}</h1>
  ${body}
</body>
</html>`
}

function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function csvCell(value: string | number) {
  const text = String(value)
  return `"${text.replaceAll('"', '""')}"`
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
              <el-dropdown-item @click="handleExportWord">导出 DOC</el-dropdown-item>
              <el-dropdown-item @click="handlePrintPdf">打印 / 保存 PDF</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <div v-if="aiStatus" class="status-strip">
        <el-tag :type="aiStatus.enabled && aiStatus.apiKeyConfigured ? 'success' : 'warning'">
          {{ aiStatus.provider }} · {{ aiStatus.model }}
        </el-tag>
        <span>{{ aiStatus.message }}</span>
      </div>
      <div v-if="preview?.metadata?.generationMessage" class="status-strip status-strip--result">
        <el-tag :type="preview.metadata.generationMode === 'RULE_BASED_FALLBACK' ? 'warning' : 'success'">
          {{ formatGenerationMode(preview.metadata.generationMode) }}
        </el-tag>
        <span>{{ preview.metadata.generationMessage }}</span>
      </div>
      <div v-if="loading" class="ai-status" role="status" aria-live="polite">
        <span class="ai-status__dot"></span>
        <span>{{ aiStatus?.provider || 'AI' }} 正在处理：{{ conversionSteps[conversionStepIndex] }}，请稍候...</span>
      </div>
      <div v-if="loading" class="conversion-steps">
        <span
          v-for="(step, index) in conversionSteps"
          :key="step"
          :class="['conversion-step', { 'conversion-step--active': index <= conversionStepIndex }]"
        >
          {{ step }}
        </span>
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
          <p>最近 20 次转换会保存在后端，便于回看、删除和继续编辑。</p>
        </div>
        <el-button size="small" :loading="historyLoading" :disabled="!history.length" @click="clearHistory">清空历史</el-button>
      </div>
      <div v-if="history.length" class="history-filters">
        <el-input v-model="historyKeyword" clearable placeholder="搜索标题、时间或生成说明" />
        <el-select v-model="historyModeFilter" clearable placeholder="筛选生成模式">
          <el-option label="DeepSeek AI" value="AI_DEEPSEEK" />
          <el-option label="OpenAI-compatible AI" value="AI_OPENAI_COMPATIBLE" />
          <el-option label="规则解析器" value="RULE_BASED" />
          <el-option label="规则兜底" value="RULE_BASED_FALLBACK" />
        </el-select>
      </div>
      <div v-if="history.length" class="history-list">
        <article v-for="item in filteredHistory" :key="item.id" class="history-item">
          <div>
            <strong>{{ item.title }}</strong>
            <p>{{ item.createdAt }} · {{ formatGenerationMode(item.generationMode) }} · {{ item.chapterCount }} 章 / {{ item.sceneCount }} 场</p>
            <small v-if="item.generationMessage">{{ item.generationMessage }}</small>
            <small v-if="item.quality">质量评分：{{ item.quality.score }} / {{ item.quality.level }}</small>
          </div>
          <div class="history-actions">
            <el-button size="small" type="primary" plain @click="loadHistoryItem(item)">加载</el-button>
            <el-button size="small" type="danger" plain @click="removeHistoryItem(item.id)">删除</el-button>
          </div>
        </article>
        <div v-if="!filteredHistory.length" class="empty-history">没有匹配的历史记录，可以调整搜索或筛选条件。</div>
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
            <span>{{ formatGenerationMode(preview.metadata?.generationMode) }}</span>
          </div>
          <div class="character-strip">
            <el-tag v-for="character in characterNames" :key="character" type="info">{{ character }}</el-tag>
          </div>
          <el-tabs v-model="activePreviewTab" class="preview-tabs">
            <el-tab-pane label="章节预览" name="chapters">
              <div class="tab-scroll">
                <article v-for="(chapter, chapterIndex) in preview.chapters" :key="chapter.id" class="chapter-preview">
                  <header>
                    <h3>{{ chapter.title }}</h3>
                    <p>{{ chapter.summary }}</p>
                  </header>
                  <section v-for="(scene, sceneIndex) in chapter.scenes" :key="scene.id" class="scene-preview">
                    <div class="scene-title">
                      <div>
                        <strong>{{ scene.title }}</strong>
                        <el-dropdown trigger="click" :disabled="Boolean(sceneEditLoadingKey)">
                          <el-button size="small" text :loading="sceneEditLoadingKey.startsWith(`${chapterIndex}-${sceneIndex}-`)">
                            AI 场景编辑
                          </el-button>
                          <template #dropdown>
                            <el-dropdown-menu>
                              <el-dropdown-item
                                v-for="action in sceneEditActions"
                                :key="action.type"
                                @click="requestSceneEdit(chapterIndex, sceneIndex, action.type, scene)"
                              >
                                {{ action.label }}
                              </el-dropdown-item>
                            </el-dropdown-menu>
                          </template>
                        </el-dropdown>
                      </div>
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
                            <el-dropdown trigger="click">
                              <el-button size="small" text>AI 编辑</el-button>
                              <template #dropdown>
                                <el-dropdown-menu>
                                  <el-dropdown-item
                                    v-for="action in availableEditActions(beat)"
                                    :key="action.type"
                                    @click="requestBeatEdit({ chapterIndex, sceneIndex, beatIndex, actionType: action.type, beat, sceneTitle: scene.title, characters: scene.characters })"
                                  >
                                    {{ action.label }}
                                  </el-dropdown-item>
                                </el-dropdown-menu>
                              </template>
                            </el-dropdown>
                          </div>
                        </div>
                        <strong v-if="beat.speaker">{{ beat.speaker }}</strong>
                        <p>{{ beat.content }}</p>
                      </div>
                    </div>
                  </section>
                </article>
              </div>
            </el-tab-pane>
            <el-tab-pane label="角色关系图" name="relations">
              <section class="relation-panel">
                <div class="relation-header">
                  <div>
                    <h3>角色关系图</h3>
                    <p>同一场景出现的角色会自动连线，连线越粗代表同场次数越多。</p>
                  </div>
                  <el-tag type="success">{{ relationshipGraph.nodes.length }} 人物 / {{ relationshipGraph.links.length }} 关系</el-tag>
                </div>
                <div v-if="relationshipGraph.nodes.length" class="relation-graph">
                  <svg viewBox="0 0 520 320" role="img" aria-label="角色关系图">
                    <line
                      v-for="link in relationshipGraph.links"
                      :key="`${link.source}-${link.target}`"
                      :x1="link.x1"
                      :y1="link.y1"
                      :x2="link.x2"
                      :y2="link.y2"
                      :stroke-width="2 + (relationshipGraph.maxWeight ? link.weight / relationshipGraph.maxWeight : 0) * 8"
                      class="relation-link"
                    >
                      <title>{{ link.source }} 与 {{ link.target }} 同场 {{ link.weight }} 次</title>
                    </line>
                    <g v-for="node in relationshipGraph.nodes" :key="node.name" class="relation-node">
                      <circle :cx="node.x" :cy="node.y" :r="node.radius">
                        <title>{{ node.name }}：出场 {{ node.sceneCount }} 场，台词 {{ node.dialogueCount }} 条</title>
                      </circle>
                      <text :x="node.x" :y="node.y + node.radius + 18" text-anchor="middle">{{ node.name }}</text>
                      <text :x="node.x" :y="node.y + 4" text-anchor="middle" class="relation-node-count">{{ node.sceneCount }}</text>
                    </g>
                  </svg>
                  <div class="relation-legend">
                    <span>节点数字 = 出场场景数</span>
                    <span>连线粗细 = 同场次数</span>
                  </div>
                </div>
                <div v-else class="relation-empty">当前剧本还没有可识别的人物关系。</div>
              </section>
            </el-tab-pane>
            <el-tab-pane label="场景分镜表" name="shots">
              <section class="shot-panel">
                <div class="shot-header">
                  <div>
                    <h3>场景分镜表</h3>
                    <p>按章节、场景和节拍整理为拍摄/汇报用表格，一条对白、动作或旁白对应一行。</p>
                  </div>
                  <div class="shot-actions">
                    <el-tag type="success">共 {{ shotRows.length }} 条分镜</el-tag>
                    <el-button size="small" type="primary" plain :disabled="!shotRows.length" @click="handleExportShotCsv">
                      导出 CSV
                    </el-button>
                  </div>
                </div>
                <el-table
                  v-if="shotRows.length"
                  :data="shotRows"
                  class="shot-table"
                  border
                  stripe
                  max-height="520"
                >
                  <el-table-column prop="index" label="#" width="58" fixed />
                  <el-table-column prop="chapter" label="章节" min-width="130" />
                  <el-table-column prop="scene" label="场景" min-width="130" />
                  <el-table-column prop="location" label="地点" min-width="120" />
                  <el-table-column prop="timeOfDay" label="时间" width="96" />
                  <el-table-column prop="characters" label="人物" min-width="180" />
                  <el-table-column prop="type" label="类型" width="92" />
                  <el-table-column prop="speaker" label="说话人" width="110" />
                  <el-table-column prop="content" label="内容" min-width="280" show-overflow-tooltip />
                </el-table>
                <div v-else class="relation-empty">当前剧本还没有可生成的分镜记录。</div>
              </section>
            </el-tab-pane>
          </el-tabs>
        </div>
        <div v-else class="empty-preview">转换完成后，这里会按章节和场景展示剧本预览。</div>
      </el-card>

      <el-card class="panel validation-panel" shadow="never">
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
        <section v-if="quality" class="quality-card">
          <div class="quality-score">
            <div>
              <span>剧本质量评分</span>
              <strong>{{ quality.score }}</strong>
            </div>
            <el-tag :type="quality.score >= 90 ? 'success' : quality.score > 60 ? 'warning' : 'danger'">
              {{ quality.level }}
            </el-tag>
          </div>
          <div v-if="quality.issues.length" class="quality-list">
            <p>发现的问题</p>
            <el-tag
              v-for="issue in quality.issues"
              :key="`${issue.type}-${issue.message}`"
              :type="issue.severity === 'error' ? 'danger' : 'warning'"
            >
              {{ issue.message }}
            </el-tag>
          </div>
          <div v-if="quality.suggestions.length" class="quality-list">
            <p>优化建议</p>
            <ul>
              <li v-for="suggestion in quality.suggestions" :key="suggestion">{{ suggestion }}</li>
            </ul>
          </div>
        </section>
        <div v-if="validation?.warnings?.length" class="warning-card">
          <el-alert
            title="一致性提醒"
            type="warning"
            :closable="false"
            show-icon
          >
            <ul>
              <li v-for="warning in validation.warnings" :key="warning">{{ warning }}</li>
            </ul>
          </el-alert>
          <el-button type="warning" plain :loading="repairLoading" :disabled="!preview" @click="handleRepairConsistency">
            一键修复一致性
          </el-button>
        </div>
        <pre>{{ schema ? JSON.stringify(schema, null, 2) : '点击“查看 Schema”后，这里会展示剧本结构规则。' }}</pre>
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
