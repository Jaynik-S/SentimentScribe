import './App.css'
import { RouterProvider } from 'react-router-dom'
import { router } from './routes'
import { AuthProvider } from './state/auth'
import { UiProvider } from './state/ui'

function App() {
  return (
    <AuthProvider>
      <UiProvider>
        <RouterProvider router={router} />
      </UiProvider>
    </AuthProvider>
  )
}

export default App
