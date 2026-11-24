import { useContext } from 'react'
import { AuthContext } from './AuthContext'

// A small custom hook: components just write  const { user, logout } = useAuth()
export function useAuth() {
  return useContext(AuthContext)
}
