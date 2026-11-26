import { useState } from 'react'
import { currentTheme, saveTheme } from '../theme/theme'
import { Icon } from './Icon'

export function ThemeToggle() {
  const [theme, setTheme] = useState(currentTheme)
  const next = theme === 'dark' ? 'light' : 'dark'

  return (
    <button type="button" className="nav-item theme-toggle" aria-label={`Switch to ${next} mode`}
      onClick={() => {
        saveTheme(next)
        setTheme(next)
      }}>
      <Icon name={next === 'dark' ? 'moon' : 'sun'} />
      <span>{next === 'dark' ? 'Dark mode' : 'Light mode'}</span>
    </button>
  )
}
