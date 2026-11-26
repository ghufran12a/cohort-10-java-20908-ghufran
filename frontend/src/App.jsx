import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { AuthProvider } from './auth/AuthProvider'
import { Layout } from './components/Layout'
import { ProtectedRoute, PublicOnlyRoute } from './components/RouteGuards'
import { AuthPage } from './pages/AuthPage'
import { ContactsPage } from './pages/ContactsPage'
import { DashboardPage } from './pages/DashboardPage'
import { ProfilePage } from './pages/ProfilePage'
import { TrashPage } from './pages/TrashPage'
import { ToastProvider } from './toast/ToastProvider'

// Routing: which component shows for which url (without reloading the page).
export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <Routes>
            <Route element={<PublicOnlyRoute />}>
              <Route path="/login" element={<AuthPage />} />
            </Route>

            <Route element={<ProtectedRoute />}>
              <Route element={<Layout />}>
                <Route path="/contacts" element={<ContactsPage />} />
                <Route path="/dashboard" element={<DashboardPage />} />
                <Route path="/trash" element={<TrashPage />} />
                <Route path="/profile" element={<ProfilePage />} />
              </Route>
            </Route>

            {/* any other url */}
            <Route path="*" element={<Navigate to="/contacts" replace />} />
          </Routes>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}
