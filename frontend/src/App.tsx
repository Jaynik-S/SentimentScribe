import './App.css'
import { RouterProvider } from 'react-router-dom'
import { router } from './routes'
import { AuthProvider } from './state/auth'
import { EntryDraftProvider } from './state/entryDraft'
import { UiProvider } from './state/ui'

function App() {
  return (
    <AuthProvider>
      <EntryDraftProvider>
        <UiProvider>
          <RouterProvider router={router} />
        </UiProvider>
      </EntryDraftProvider>
    </AuthProvider>
  )
}

export default App
