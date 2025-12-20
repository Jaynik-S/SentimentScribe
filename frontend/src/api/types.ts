export type LocalDateTime = string

export type ErrorResponse = {
  error: string
}

export type AuthRequest = {
  password: string
}

export type AuthResponse = {
  status: string
  entries: EntrySummaryResponse[]
}

export type EntrySummaryResponse = {
  title: string
  storagePath: string
  createdAt: LocalDateTime | null
  updatedAt: LocalDateTime | null
  keywords: string[]
}

export type EntryRequest = {
  title: string
  text: string
  storagePath: string | null
  keywords: string[]
  createdAt: LocalDateTime | null
}

export type EntryResponse = {
  title: string
  text: string
  storagePath: string
  createdAt: LocalDateTime | null
  updatedAt: LocalDateTime | null
  keywords: string[]
}

export type DeleteResponse = {
  deleted: boolean
  storagePath: string
}

export type AnalysisRequest = {
  text: string
}

export type AnalysisResponse = {
  keywords: string[]
}

export type RecommendationRequest = {
  text: string
}

export type RecommendationResponse = {
  keywords: string[]
  songs: SongRecommendationResponse[]
  movies: MovieRecommendationResponse[]
}

export type SongRecommendationResponse = {
  releaseYear: string
  imageUrl: string
  songName: string
  artistName: string
  popularityScore: string
  externalUrl: string
}

export type MovieRecommendationResponse = {
  releaseYear: string
  imageUrl: string
  movieTitle: string
  movieRating: string
  overview: string
}
