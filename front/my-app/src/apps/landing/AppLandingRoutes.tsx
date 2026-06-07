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
const LandingLoginPage = lazy(() =>
  import('./LandingLoginPage').then((m) => ({ default: m.LandingLoginPage })),
)
const LandingSignUpPage = lazy(() =>
  import('./LandingSignUpPage').then((m) => ({ default: m.LandingSignUpPage })),
)
const LandingForgotPasswordPage = lazy(() =>
  import('./LandingForgotPasswordPage').then((m) => ({ default: m.LandingForgotPasswordPage })),
)
const LandingResetPasswordPage = lazy(() =>
  import('./LandingResetPasswordPage').then((m) => ({ default: m.LandingResetPasswordPage })),
)
const LandingCheckoutPage = lazy(() =>
  import('./LandingCheckoutPage').then((m) => ({ default: m.LandingCheckoutPage })),
)
const LandingMyBookingsPage = lazy(() =>
  import('./LandingMyBookingsPage').then((m) => ({ default: m.LandingMyBookingsPage })),
)
const LandingAccountPage = lazy(() =>
  import('./LandingAccountPage').then((m) => ({ default: m.LandingAccountPage })),
)

export function AppLandingRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LandingLoginPage />} />
      <Route path="/signup" element={<LandingSignUpPage />} />
      <Route path="/forgot-password" element={<LandingForgotPasswordPage />} />
      <Route path="/reset-password" element={<LandingResetPasswordPage />} />
      <Route element={<LandingShell />}>
        <Route path="/checkout" element={<LandingCheckoutPage />} />
        <Route path="/my-bookings" element={<LandingMyBookingsPage />} />
        <Route path="/account" element={<LandingAccountPage />} />
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
