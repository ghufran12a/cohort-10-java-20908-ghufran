import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/client'
import { AuthContext } from './AuthContext'
import { session } from './session'

// Holds "who is logged in" for the whole app.
export function AuthProvider({ children }) {
  // useState: React re-draws the screen whenever this value changes.
  // The function form runs only on the first render: it restores the login after a page refresh.
  const [user, setUser] = useState(() => (session.getToken() ? session.getUser() : null))

  const startSession = useCallback((data) => {
    session.save(data.token, data.user)
    setUser(data.user)
  }, [])

  const login = useCallback(
    async (identifier, password) => {
      const { data } = await api.post('/auth/login', { identifier, password })
      startSession(data)
    },
    [startSession],
  )

  const register = useCallback(
    async (form) => {
      const { data } = await api.post('/auth/register', form)
      startSession(data)
    },
    [startSession],
  )

  const logout = useCallback(() => {
    session.clear()
    setUser(null)
  }, [])

  // useEffect: code that connects React to the outside world, here a browser event.
  // The returned function is the clean-up: it removes the listener when no longer needed.
  useEffect(() => {
    window.addEventListener('auth:expired', logout)
    return () => window.removeEventListener('auth:expired', logout)
  }, [logout])

  return (
    <AuthContext.Provider value={{ user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}
