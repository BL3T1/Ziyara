import { describe, it, expect } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { AuthProvider } from './context/AuthContext'
import { LanguageProvider } from './context/LanguageContext'
import { LayoutProvider } from './context/LayoutContext'
import App from './App'

function renderApp() {
  return render(
    <AuthProvider>
      <LayoutProvider>
        <LanguageProvider>
          <App />
        </LanguageProvider>
      </LayoutProvider>
    </AuthProvider>,
  )
}

describe('App', () => {
  it('company surface: unauthenticated root shows login', async () => {
    renderApp()
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /welcome back/i })).toBeInTheDocument()
    })
  })
})
