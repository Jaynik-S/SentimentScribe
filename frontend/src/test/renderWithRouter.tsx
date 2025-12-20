import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { GlobalLoadingOverlay } from '../components/GlobalLoadingOverlay'
import { PageErrorBanner } from '../components/PageErrorBanner'
import { AuthProvider } from '../state/auth'
import { EntryDraftProvider } from '../state/entryDraft'
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
    </AuthProvider>,
  )
}
