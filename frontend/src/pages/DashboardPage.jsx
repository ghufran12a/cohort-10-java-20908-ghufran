import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { contactsApi } from '../api/contacts'
import { parseApiError } from '../api/errors'
import { useAuth } from '../auth/useAuth'
import { Icon } from '../components/Icon'

function StatCard({ to, icon, value, label }) {
  return (
    <Link to={to} className="stat">
      <span className="stat-icon"><Icon name={icon} size={26} /></span>
      <strong>{value}</strong>
      <span>{label}</span>
    </Link>
  )
}

export function DashboardPage() {
  const { user } = useAuth()
  const [counts, setCounts] = useState(null)
  const [loadError, setLoadError] = useState('')

  useEffect(() => {
    let ignore = false
    Promise.all([contactsApi.list({ page: 0, size: 1 }), contactsApi.trash()])
      .then(([contacts, trash]) => {
        if (!ignore) setCounts({ contacts: contacts.totalElements, trash: trash.length })
      })
      .catch((err) => {
        if (!ignore) setLoadError(parseApiError(err).message)
      })
    return () => {
      ignore = true
    }
  }, [])

  return (
    <div className="page">
      <h1>Hello, {user?.name}</h1>
      <p className="hint">Here is an overview of your contacts.</p>
      {loadError && <p className="alert" role="alert">{loadError}</p>}
      <div className="stats">
        <StatCard to="/contacts" icon="contacts" value={counts?.contacts ?? '–'} label="Contacts" />
        <StatCard to="/trash" icon="trash" value={counts?.trash ?? '–'} label="In trash" />
      </div>
    </div>
  )
}
