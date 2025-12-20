import { useUi } from '../state/ui'

export const PageErrorBanner = () => {
  const { pageError, clearPageError } = useUi()

  if (!pageError) {
    return null
  }

  return (
    <section className="page-error" role="alert">
      <div className="page-error__content">
        <span className="page-error__message">{pageError}</span>
        <button
          type="button"
          className="page-error__dismiss"
          onClick={clearPageError}
        >
          Dismiss
        </button>
      </div>
    </section>
  )
}
