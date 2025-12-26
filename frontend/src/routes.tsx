import type { ReactNode } from 'react'
import { Navigate, Outlet, createBrowserRouter } from 'react-router-dom'
import { GlobalLoadingOverlay } from './components/GlobalLoadingOverlay'
import { PageErrorBanner } from './components/PageErrorBanner'
import { useAuth } from './state/auth'
import { DiaryEntryPage } from './pages/DiaryEntryPage'
import { HomeMenuPage } from './pages/HomeMenuPage'
import { RecommendationPage } from './pages/RecommendationPage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'

const RootLayout = () => {
  return (
    <div className="app-shell">
      <PageErrorBanner />
      <GlobalLoadingOverlay />
      <main className="app-content">
        <Outlet />
      </main>
    </div>
  )
}

export const RequireAuth = ({ children }: { children: ReactNode }) => {
  const { isAuthenticated } = useAuth()

  if (!isAuthenticated) {
    return <Navigate to="/" replace />
  }

  return children
}

export const router = createBrowserRouter([
  {
    path: '/',
    element: <RootLayout />,
    children: [
      { index: true, element: <LoginPage /> },
      { path: 'register', element: <RegisterPage /> },
      {
        path: 'home',
        element: (
          <RequireAuth>
            <HomeMenuPage />
          </RequireAuth>
        ),
      },
      {
        path: 'entry',
        element: (
          <RequireAuth>
            <DiaryEntryPage />
          </RequireAuth>
        ),
      },
      {
        path: 'recommendations',
        element: (
          <RequireAuth>
            <RecommendationPage />
          </RequireAuth>
        ),
      },
    ],
  },
])
