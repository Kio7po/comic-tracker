import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './common/i18n'
import App from './modules/app/App.tsx'
import { registerServiceWorker } from './common/lib/registerServiceWorker'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)

// After the first render, so the Toaster is already mounted to show the update prompt.
registerServiceWorker()
