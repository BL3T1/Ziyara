import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './global.css'
import { AuthProvider } from './context/AuthContext'
import { LanguageProvider } from './context/LanguageContext'
import { LayoutProvider } from './context/LayoutContext'
import { DisplayCurrencyProvider } from './context/DisplayCurrencyContext'
import { PermissionsProvider } from './context/PermissionsContext'
import { PendingCountsProvider } from './context/PendingCountsContext'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <LayoutProvider>
        <LanguageProvider>
          <DisplayCurrencyProvider>
            <PermissionsProvider>
              <PendingCountsProvider>
                <App />
              </PendingCountsProvider>
            </PermissionsProvider>
          </DisplayCurrencyProvider>
        </LanguageProvider>
      </LayoutProvider>
    </AuthProvider>
  </StrictMode>,
)
