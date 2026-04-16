import { lazy } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'

const LandingHomePage = lazy(() =>
  import('./LandingHomePage').then((m) => ({ default: m.LandingHomePage })),
)
const LandingShell = lazy(() =>
  import('./LandingShell').then((m) => ({ default: m.LandingShell })),
)
const LandingAboutPage = lazy(() =>
  import('./LandingAboutPage').then((m) => ({ default: m.LandingAboutPage })),
)
const LandingServicesPage = lazy(() =>
  import('./LandingServicesPage').then((m) => ({ default: m.LandingServicesPage })),
)
const LandingContactPage = lazy(() =>
  import('./LandingContactPage').then((m) => ({ default: m.LandingContactPage })),
)
const LandingFaqPage = lazy(() =>
  import('./LandingFaqPage').then((m) => ({ default: m.LandingFaqPage })),
)
const LandingPrivacyPage = lazy(() =>
  import('./LandingPrivacyPage').then((m) => ({ default: m.LandingPrivacyPage })),
)
const LandingTermsPage = lazy(() =>
  import('./LandingTermsPage').then((m) => ({ default: m.LandingTermsPage })),
)
const LandingServiceTypePage = lazy(() =>
  import('./LandingServiceTypePage').then((m) => ({ default: m.LandingServiceTypePage })),
)
const LandingServiceDetailPage = lazy(() =>
  import('./LandingServiceDetailPage').then((m) => ({ default: m.LandingServiceDetailPage })),
)

export function AppLandingRoutes() {
  return (
    <Routes>
      <Route element={<LandingShell />}>
        <Route path="/" element={<LandingHomePage />} />
        <Route path="/about" element={<LandingAboutPage />} />
        <Route path="/services" element={<LandingServicesPage />} />
        <Route path="/services/:category" element={<LandingServiceTypePage />} />
        <Route path="/services/:category/:id" element={<LandingServiceDetailPage />} />
        <Route path="/hotels" element={<LandingServiceTypePage />} />
        <Route path="/resorts" element={<LandingServiceTypePage />} />
        <Route path="/restaurants" element={<LandingServiceTypePage />} />
        <Route path="/trips" element={<LandingServiceTypePage />} />
        <Route path="/taxis" element={<LandingServiceTypePage />} />
        <Route path="/hotels/:id" element={<LandingServiceDetailPage />} />
        <Route path="/resorts/:id" element={<LandingServiceDetailPage />} />
        <Route path="/restaurants/:id" element={<LandingServiceDetailPage />} />
        <Route path="/trips/:id" element={<LandingServiceDetailPage />} />
        <Route path="/taxis/:id" element={<LandingServiceDetailPage />} />
        <Route path="/contact" element={<LandingContactPage />} />
        <Route path="/faq" element={<LandingFaqPage />} />
        <Route path="/privacy" element={<LandingPrivacyPage />} />
        <Route path="/terms" element={<LandingTermsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
