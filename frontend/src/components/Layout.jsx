import { NavLink, Outlet } from 'react-router'
import { useAuth } from '../auth/useAuth'
import { Avatar } from './Avatar'
import { Icon } from './Icon'
import { ThemeToggle } from './ThemeToggle'

const LINKS = [
  { to: '/contacts', label: 'Contacts', icon: 'contacts' },
  { to: '/dashboard', label: 'Dashboard', icon: 'dashboard' },
  { to: '/trash', label: 'Trash', icon: 'trash' },
  { to: '/profile', label: 'Profile', icon: 'profile' },
]

// The frame around every logged-in page.
// Laptop: sidebar on the left. Phone: small top bar + tab bar at the bottom (CSS decides).
export function Layout() {
  const { user, logout } = useAuth()
  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark"><Icon name="contacts" /></span>
          <strong>Contact Manager</strong>
        </div>
        <nav className="nav">
          {LINKS.map((link) => (
            // NavLink adds the class "active" to the link of the current page
            <NavLink key={link.to} to={link.to} className="nav-item">
              <Icon name={link.icon} />
              <span>{link.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-foot">
          <ThemeToggle />
          <div className="me">
            <Avatar firstName={user?.name ?? '?'} size="sm" />
            <span className="me-name">{user?.name}</span>
          </div>
          <button type="button" className="nav-item sidebar-logout" onClick={logout}>
            <Icon name="logout" />
            <span>Log out</span>
          </button>
        </div>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
