import { NavLink, Outlet } from 'react-router'
import { useAuth } from '../auth/useAuth'

// The frame around every logged-in page: top bar + the current page (<Outlet />).
export function Layout() {
  const { user, logout } = useAuth()
  return (
    <>
      <header className="topbar">
        <strong>Contact Manager</strong>
        <nav>
          <NavLink to="/contacts">Contacts</NavLink>
          <NavLink to="/trash">Trash</NavLink>
        </nav>
        <div className="topbar-user">
          <span>{user?.name}</span>
          <button type="button" className="btn" onClick={logout}>Log out</button>
        </div>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </>
  )
}
