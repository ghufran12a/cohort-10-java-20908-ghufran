import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from '../api/client'
import { AuthContext } from './AuthContext'
import { session } from './session'

// Holds "who is logged in" for the whole app.
export function AuthProvider({ children }) {
  // useState: React re-draws the screen whenever this value changes.
  // The function form runs only on the first render: it restores the login after a page refresh.
  const [user, setUser] = useState(() => (session.getToken() ? session.getUser() : null))

  // useRef: a value that survives re-draws without causing one. It numbers the login/register requests.
  const latestRequest = useRef(0)

  // Only the NEWEST login/register request may start a session. If the user switched tabs and sent a
  // second one, a late answer to the first is ignored.
  const authenticate = useCallback(async (url, body) => {
    latestRequest.current += 1
    const requestNumber = latestRequest.current
    const { data } = await api.post(url, body)
    if (requestNumber !== latestRequest.current) return
    if (!session.save(data.token, data.user)) {
      const error = new Error('Browser storage is blocked')
      error.userMessage = 'Your browser is blocking storage, so you cannot log in. Allow site data and try again.'
      throw error
    }
    setUser(data.user)
  }, [])

  const login = useCallback(
    (identifier, password) => authenticate('/auth/login', { identifier, password }),
    [authenticate],
  )

  const register = useCallback((form) => authenticate('/auth/register', form), [authenticate])

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
