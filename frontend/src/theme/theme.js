// Light / dark mode. The choice is a harmless per-browser preference, so localStorage is fine for it.
const KEY = 'cm_theme'

function readSaved() {
  try {
    return localStorage.getItem(KEY)
  } catch {
    return null // storage can be blocked (private mode)
  }
}

export function currentTheme() {
  const saved = readSaved()
  if (saved === 'light' || saved === 'dark') {
    return saved
  }
  // no choice yet: follow the operating system
  return globalThis.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function applyTheme(theme) {
  document.documentElement.dataset.theme = theme // CSS reads [data-theme="dark"]
}

export function saveTheme(theme) {
  applyTheme(theme)
  try {
    localStorage.setItem(KEY, theme)
  } catch {
    // the theme still works for this visit
  }
}
