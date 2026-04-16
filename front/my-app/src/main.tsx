import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './global.css'
import { AuthProvider } from './context/AuthContext'
import { LanguageProvider } from './context/LanguageContext'
import { LayoutProvider } from './context/LayoutContext'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <LayoutProvider>
        <LanguageProvider>
          <App />
        </LanguageProvider>
      </LayoutProvider>
    </AuthProvider>
  </StrictMode>,
)
