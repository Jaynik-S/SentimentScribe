import type { EntrySummaryResponse } from '../api/types'

export type EntrySummaryView = EntrySummaryResponse & {
  titlePlaintext?: string
}
