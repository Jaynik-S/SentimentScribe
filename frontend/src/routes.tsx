import type { ReactNode } from 'react'
import { Navigate, Outlet, createBrowserRouter } from 'react-router-dom'
import { GlobalLoadingOverlay } from './components/GlobalLoadingOverlay'
import { PageErrorBanner } from './components/PageErrorBanner'
import { useAuth } from './state/auth'
import { DiaryEntryPage } from './pages/DiaryEntryPage'
import { HomeMenuPage } from './pages/HomeMenuPage'
import { RecommendationPage } from './pages/RecommendationPage'
import { VerifyPasswordPage } from './pages/VerifyPasswordPage'

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

const RequireUnlocked = ({ children }: { children: ReactNode }) => {
  const { isUnlocked } = useAuth()

  if (!isUnlocked) {
    return <Navigate to="/" replace />
  }

  return children
}

export const router = createBrowserRouter([
  {
    path: '/',
    element: <RootLayout />,
    children: [
      { index: true, element: <VerifyPasswordPage /> },
      {
        path: 'home',
        element: (
          <RequireUnlocked>
            <HomeMenuPage />
          </RequireUnlocked>
        ),
      },
      {
        path: 'entry',
        element: (
          <RequireUnlocked>
            <DiaryEntryPage />
          </RequireUnlocked>
        ),
      },
      {
        path: 'recommendations',
        element: (
          <RequireUnlocked>
            <RecommendationPage />
          </RequireUnlocked>
        ),
      },
    ],
  },
])
