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
  storagePath: string
  createdAt: LocalDateTime | null
  updatedAt: LocalDateTime | null
  titleCiphertext: string
  titleIv: string
  algo: string
  version: number
}

export type EntryRequest = {
  storagePath: string | null
  createdAt: LocalDateTime | null
  titleCiphertext: string
  titleIv: string
  bodyCiphertext: string
  bodyIv: string
  algo: string
  version: number
}

export type EntryResponse = {
  storagePath: string
  createdAt: LocalDateTime | null
  updatedAt: LocalDateTime | null
  titleCiphertext: string
  titleIv: string
  bodyCiphertext: string
  bodyIv: string
  algo: string
  version: number
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
  excludeSongIds?: string[]
  excludeMovieIds?: string[]
}

export type RecommendationResponse = {
  keywords: string[]
  songs: SongRecommendationResponse[]
  movies: MovieRecommendationResponse[]
}

export type SongRecommendationResponse = {
  songId: string
  releaseYear: string
  imageUrl: string
  songName: string
  artistName: string
  popularityScore: string
  externalUrl: string
}

export type MovieRecommendationResponse = {
  movieId: string
  releaseYear: string
  imageUrl: string
  movieTitle: string
  movieRating: string
  overview: string
}
