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

export async function fetchSchema(): Promise<unknown> {
  const response = await axios.get('/api/schema')
  return response.data
}
