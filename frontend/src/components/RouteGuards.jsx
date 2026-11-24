import { Navigate, Outlet } from 'react-router'
import { useAuth } from '../auth/useAuth'

// <Outlet /> means "show whatever child route matched here".
// NOTE: these guards only control what the browser SHOWS. The real protection is the backend,
// which rejects every request without a valid token.
export function ProtectedRoute() {
  const { user } = useAuth()
  return user ? <Outlet /> : <Navigate to="/login" replace />
}

// A logged-in user should not see the login page. This also does the redirect after login/register.
export function PublicOnlyRoute() {
  const { user } = useAuth()
  return user ? <Navigate to="/contacts" replace /> : <Outlet />
}
