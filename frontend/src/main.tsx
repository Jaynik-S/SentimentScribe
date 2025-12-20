import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { registerApiDebug } from './api/debug'
import './index.css'
import App from './App.tsx'

registerApiDebug()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
