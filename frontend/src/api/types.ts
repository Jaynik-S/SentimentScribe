export type LocalDateTime = string

export type ErrorResponse = {
  error: string
}

export type AuthUser = {
  id: string
  username: string
}

export type E2eeParams = {
  kdf: string
  salt: string
  iterations: number
}

export type AuthTokenResponse = {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: AuthUser
  e2ee: E2eeParams
}

export type LoginRequest = {
  username: string
  password: string
}

export type RegisterRequest = {
  username: string
  password: string
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
