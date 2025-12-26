import './App.css'
import { RouterProvider } from 'react-router-dom'
import { router } from './routes'
import { AuthProvider } from './state/auth'
import { E2eeProvider } from './state/e2ee'
import { EntryDraftProvider } from './state/entryDraft'
import { RecommendationsProvider } from './state/recommendations'
import { UiProvider } from './state/ui'

function App() {
  return (
    <AuthProvider>
      <E2eeProvider>
        <EntryDraftProvider>
          <RecommendationsProvider>
            <UiProvider>
              <RouterProvider router={router} />
            </UiProvider>
          </RecommendationsProvider>
        </EntryDraftProvider>
      </E2eeProvider>
    </AuthProvider>
  )
}

export default App
