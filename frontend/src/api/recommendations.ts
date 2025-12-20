import { request } from './http'
import type { RecommendationRequest, RecommendationResponse } from './types'

export const getRecommendations = (
  payload: RecommendationRequest,
): Promise<RecommendationResponse> =>
  request<RecommendationResponse>('/api/recommendations', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
