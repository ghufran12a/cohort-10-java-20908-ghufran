import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
// The entry point: React takes over the <div id="root"> in index.html and draws <App /> inside it.
import App from './App.jsx'
import { applyTheme, currentTheme } from './theme/theme'

applyTheme(currentTheme()) // before the first paint, so the page does not flash the wrong colours

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
