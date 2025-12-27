import { useAuth } from '../state/auth'
import { useOffline } from '../state/offline'

export const OfflineIndicator = () => {
  const { isAuthenticated } = useAuth()
  const { isOffline, pendingCount, isSyncing, syncNow } = useOffline()

  if (!isAuthenticated) {
    return null
  }

  if (!isOffline && pendingCount === 0) {
    return null
  }

  const showPending = pendingCount > 0
  const canSync = showPending && !isOffline && !isSyncing
  const syncLabel = isSyncing ? 'Syncing...' : 'Sync now'

  return (
    <div className="offline-indicator" role="status" aria-live="polite">
      <div className="offline-indicator__content">
        <div className="offline-indicator__status">
          {isOffline ? (
            <span className="offline-indicator__badge offline-indicator__badge--offline">
              Offline
            </span>
          ) : null}
          {showPending ? (
            <span className="offline-indicator__badge offline-indicator__badge--pending">
              Sync pending: {pendingCount}
            </span>
          ) : null}
        </div>
        {showPending ? (
          <div className="offline-indicator__actions">
            <button
              className="secondary-button"
              type="button"
              onClick={() => void syncNow()}
              disabled={!canSync}
            >
              {syncLabel}
            </button>
          </div>
        ) : null}
      </div>
    </div>
  )
}
