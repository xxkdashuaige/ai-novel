import axios from 'axios'

export interface ValidationResult {
  valid: boolean
  errors: string[]
  warnings: string[]
}

export interface ConversionResponse {
  script: ScriptDocument
  yaml: string
  validation: ValidationResult
}

export interface AiStatusResponse {
  enabled: boolean
  apiKeyConfigured: boolean
  provider: string
  model: string
  message: string
}

export interface EditResponse {
  result: string
  generationMode: string
}

export interface ScriptHistoryItem {
  id: string
  title: string
  createdAt: string
  generationMode: string
  generationMessage: string
  chapterCount: number
  sceneCount: number
  script: ScriptDocument
  yaml: string
  validation: ValidationResult
}

export interface ScriptDocument {
  title: string
  chapters: ScriptChapter[]
  metadata?: {
    sourceChapterCount: number
    generationMode: string
    generationMessage?: string
  }
}

export interface ScriptChapter {
  id: string
  title: string
  summary: string
  scenes: ScriptScene[]
}

export interface ScriptScene {
  id: string
  title: string
  location: string
  timeOfDay: string
  characters: string[]
  beats: ScriptBeat[]
}

export interface ScriptBeat {
  type: 'dialogue' | 'action' | 'narration' | 'transition' | string
  speaker?: string
  content: string
}

export async function convertNovel(novelText: string): Promise<ConversionResponse> {
  const response = await axios.post<ConversionResponse>('/api/scripts/convert', { novelText })
  return response.data
}

export async function validateYaml(yaml: string): Promise<ValidationResult> {
  const response = await axios.post<ValidationResult>('/api/scripts/validate', { yaml })
  return response.data
}

export async function renderScript(script: ScriptDocument): Promise<ConversionResponse> {
  const response = await axios.post<ConversionResponse>('/api/scripts/render', script)
  return response.data
}

export async function repairScript(script: ScriptDocument): Promise<ConversionResponse> {
  const response = await axios.post<ConversionResponse>('/api/scripts/repair', script)
  return response.data
}

export async function editBeat(input: {
  type: 'polish-dialogue' | 'expand-action'
  content: string
  speaker?: string
  sceneTitle?: string
  characters?: string[]
}): Promise<EditResponse> {
  const response = await axios.post<EditResponse>('/api/scripts/edit', input)
  return response.data
}

export async function fetchSchema(): Promise<unknown> {
  const response = await axios.get('/api/schema')
  return response.data
}

export async function fetchAiStatus(): Promise<AiStatusResponse> {
  const response = await axios.get<AiStatusResponse>('/api/ai/status')
  return response.data
}

export async function fetchHistory(): Promise<ScriptHistoryItem[]> {
  const response = await axios.get<ScriptHistoryItem[]>('/api/scripts/history')
  return response.data
}

export async function deleteHistoryItem(id: string): Promise<void> {
  await axios.delete(`/api/scripts/history/${id}`)
}

export async function clearHistoryItems(): Promise<void> {
  await axios.delete('/api/scripts/history')
}
