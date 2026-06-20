import { Suspense, useState } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import { APP_SURFACE } from './config/appSurface'
import { RoutePageFallback } from './components/RoutePageFallback'
import { ErrorBoundary } from './components/ErrorBoundary'
import { ToastProvider } from './context/ToastContext'
import { AppCompanyRoutes } from './apps/company/AppCompanyRoutes'
import { AppProviderRoutes } from './apps/provider/AppProviderRoutes'
import { AppLandingRoutes } from './apps/landing/AppLandingRoutes'
import { createQueryClient } from './lib/queryClient'

function AppRoutes() {
  switch (APP_SURFACE) {
    case 'provider':
      return <AppProviderRoutes />
    case 'landing':
      return <AppLandingRoutes />
    case 'company':
    default:
      return <AppCompanyRoutes />
  }
}

// createBrowserRouter (data router) is required for useBlocker.
// A single wildcard route delegates all rendering to the existing <Routes> components.
const router = createBrowserRouter([
  {
    path: '*',
    element: (
      <Suspense fallback={<RoutePageFallback />}>
        <AppRoutes />
      </Suspense>
    ),
  },
])

function App() {
  const [queryClient] = useState(() => createQueryClient())
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <ToastProvider>
          <RouterProvider router={router} />
        </ToastProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  )
}

export default App
