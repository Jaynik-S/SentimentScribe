import type { ReactNode } from 'react'
import { Navigate, Outlet, createBrowserRouter } from 'react-router-dom'
import { GlobalLoadingOverlay } from './components/GlobalLoadingOverlay'
import { OfflineIndicator } from './components/OfflineIndicator'
import { PageErrorBanner } from './components/PageErrorBanner'
import { useAuth } from './state/auth'
import { useE2ee } from './state/e2ee'
import { DiaryEntryPage } from './pages/DiaryEntryPage'
import { HomeMenuPage } from './pages/HomeMenuPage'
import { RecommendationPage } from './pages/RecommendationPage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { UnlockPage } from './pages/UnlockPage'

const RootLayout = () => {
  return (
    <div className="app-shell">
      <PageErrorBanner />
      <GlobalLoadingOverlay />
      <OfflineIndicator />
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

export const RequireUnlocked = ({ children }: { children: ReactNode }) => {
  const { isUnlocked } = useE2ee()

  if (!isUnlocked) {
    return <Navigate to="/unlock" replace />
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
        path: 'unlock',
        element: (
          <RequireAuth>
            <UnlockPage />
          </RequireAuth>
        ),
      },
      {
        path: 'home',
        element: (
          <RequireAuth>
            <RequireUnlocked>
              <HomeMenuPage />
            </RequireUnlocked>
          </RequireAuth>
        ),
      },
      {
        path: 'entry',
        element: (
          <RequireAuth>
            <RequireUnlocked>
              <DiaryEntryPage />
            </RequireUnlocked>
          </RequireAuth>
        ),
      },
      {
        path: 'recommendations',
        element: (
          <RequireAuth>
            <RequireUnlocked>
              <RecommendationPage />
            </RequireUnlocked>
          </RequireAuth>
        ),
      },
    ],
  },
])
