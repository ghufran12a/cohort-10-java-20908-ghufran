import { useCallback, useEffect, useState } from 'react'
import { ToastContext } from './ToastContext'

const VISIBLE_MS = 3500

// A "toast" is a small message that appears and disappears by itself. Any component can show one.
export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null) // { id, text }

  const showToast = useCallback((text) => setToast({ id: Date.now(), text }), [])

  useEffect(() => {
    if (!toast) {
      return undefined
    }
    const timer = setTimeout(() => setToast(null), VISIBLE_MS)
    return () => clearTimeout(timer)
  }, [toast])

  return (
    <ToastContext.Provider value={showToast}>
      {children}
      {/* aria-live makes screen readers announce the message */}
      <output className="toast-area" aria-live="polite">
        {toast && <span className="toast" key={toast.id}>{toast.text}</span>}
      </output>
    </ToastContext.Provider>
  )
}
