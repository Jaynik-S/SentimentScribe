import './App.css'
import { RouterProvider } from 'react-router-dom'
import { router } from './routes'
import { AuthProvider } from './state/auth'
import { EntryDraftProvider } from './state/entryDraft'
import { RecommendationsProvider } from './state/recommendations'
import { UiProvider } from './state/ui'

function App() {
  return (
    <AuthProvider>
      <EntryDraftProvider>
        <RecommendationsProvider>
          <UiProvider>
            <RouterProvider router={router} />
          </UiProvider>
        </RecommendationsProvider>
      </EntryDraftProvider>
    </AuthProvider>
  )
}

export default App
