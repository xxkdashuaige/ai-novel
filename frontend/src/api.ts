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

export interface EditResponse {
  result: string
  generationMode: string
}

export interface ScriptDocument {
  title: string
  chapters: ScriptChapter[]
  metadata?: {
    sourceChapterCount: number
    generationMode: string
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
