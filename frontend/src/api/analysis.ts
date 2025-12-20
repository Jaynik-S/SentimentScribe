import { request } from './http'
import type { AnalysisRequest, AnalysisResponse } from './types'

export const analyzeText = (payload: AnalysisRequest): Promise<AnalysisResponse> =>
  request<AnalysisResponse>('/api/analysis', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
