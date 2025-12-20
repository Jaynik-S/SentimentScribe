import { useUi } from '../state/ui'

export const PageErrorBanner = () => {
  const { pageError, pageErrorAction, clearPageError } = useUi()

  if (!pageError) {
    return null
  }

  return (
    <section className="page-error" role="alert">
      <div className="page-error__content">
        <span className="page-error__message">{pageError}</span>
        <div className="page-error__actions">
          {pageErrorAction ? (
            <button
              type="button"
              className="page-error__action"
              onClick={pageErrorAction.onClick}
            >
              {pageErrorAction.label}
            </button>
          ) : null}
          <button
            type="button"
            className="page-error__dismiss"
            onClick={clearPageError}
          >
            Dismiss
          </button>
        </div>
      </div>
    </section>
  )
}
