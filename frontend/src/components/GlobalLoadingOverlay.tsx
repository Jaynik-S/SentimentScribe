import { useUi } from '../state/ui'

export const GlobalLoadingOverlay = () => {
  const { isPageLoading } = useUi()

  if (!isPageLoading) {
    return null
  }

  return (
    <div className="loading-overlay" role="status" aria-live="polite">
      <div className="loading-overlay__card">
        <span className="spinner" aria-hidden="true" />
        <span>Loading...</span>
      </div>
    </div>
  )
}
