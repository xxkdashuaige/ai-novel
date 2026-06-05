import axios from 'axios'

export interface ValidationResult {
  valid: boolean
  errors: string[]
}

export interface ConversionResponse {
  script: unknown
  yaml: string
  validation: ValidationResult
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
