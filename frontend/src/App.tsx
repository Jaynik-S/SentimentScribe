import './App.css'
import { RouterProvider } from 'react-router-dom'
import { router } from './routes'
import { AuthProvider } from './state/auth'
import { E2eeProvider } from './state/e2ee'
import { EntryDraftProvider } from './state/entryDraft'
import { OfflineProvider } from './state/offline'
import { RecommendationsProvider } from './state/recommendations'
import { UiProvider } from './state/ui'

function App() {
  return (
    <AuthProvider>
      <OfflineProvider>
        <E2eeProvider>
          <EntryDraftProvider>
            <RecommendationsProvider>
              <UiProvider>
                <RouterProvider router={router} />
              </UiProvider>
            </RecommendationsProvider>
          </EntryDraftProvider>
        </E2eeProvider>
      </OfflineProvider>
    </AuthProvider>
  )
}

export default App
