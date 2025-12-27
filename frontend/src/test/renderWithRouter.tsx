import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { GlobalLoadingOverlay } from '../components/GlobalLoadingOverlay'
import { PageErrorBanner } from '../components/PageErrorBanner'
import { AuthProvider } from '../state/auth'
import { E2eeProvider } from '../state/e2ee'
import { EntryDraftProvider } from '../state/entryDraft'
import { OfflineProvider } from '../state/offline'
import { RecommendationsProvider } from '../state/recommendations'
import { UiProvider } from '../state/ui'

type RouteConfig = {
  path: string
  element: ReactElement
}

type RenderOptions = {
  initialEntries?: string[]
  routes: RouteConfig[]
}

export const renderWithRouter = ({
  routes,
  initialEntries = ['/'],
}: RenderOptions) => {
  return render(
    <AuthProvider>
      <OfflineProvider>
        <E2eeProvider>
          <EntryDraftProvider>
            <RecommendationsProvider>
              <UiProvider>
                <MemoryRouter initialEntries={initialEntries}>
                  <PageErrorBanner />
                  <GlobalLoadingOverlay />
                  <Routes>
                    {routes.map((route) => (
                      <Route key={route.path} path={route.path} element={route.element} />
                    ))}
                  </Routes>
                </MemoryRouter>
              </UiProvider>
            </RecommendationsProvider>
          </EntryDraftProvider>
        </E2eeProvider>
      </OfflineProvider>
    </AuthProvider>,
  )
}
