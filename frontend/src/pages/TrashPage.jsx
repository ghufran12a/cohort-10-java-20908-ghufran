import { useEffect, useState } from 'react'
import { contactsApi } from '../api/contacts'
import { parseApiError } from '../api/errors'
import { Avatar } from '../components/Avatar'
import { ListSkeleton } from '../components/ListSkeleton'
import { useToast } from '../toast/useToast'

const KEEP_DAYS = 30
const DAY_MS = 24 * 60 * 60 * 1000

function daysLeft(deletedAt, now) {
  const gone = (now - new Date(deletedAt).getTime()) / DAY_MS
  // Kept between 0 and 30: MySQL rounds the time to whole seconds, so "deleted at" can be a fraction
  // of a second in the future, which would otherwise show 31 days.
  return Math.min(KEEP_DAYS, Math.max(0, Math.ceil(KEEP_DAYS - gone)))
}

export function TrashPage() {
  const showToast = useToast()
  const [result, setResult] = useState(null) // { contacts, loadedAt }
  const [message, setMessage] = useState('')
  const [reloadCount, setReloadCount] = useState(0)

  useEffect(() => {
    let ignore = false
    contactsApi.trash()
      .then((contacts) => {
        if (!ignore) setResult({ contacts, loadedAt: Date.now() })
      })
      .catch((err) => {
        if (!ignore) setMessage(parseApiError(err).message)
      })
    return () => {
      ignore = true
    }
  }, [reloadCount])

  async function handleRestore(id) {
    setMessage('')
    try {
      await contactsApi.restore(id)
      showToast('Contact restored')
    } catch (err) {
      setMessage(parseApiError(err).message)
    }
    setReloadCount((n) => n + 1)
  }

  return (
    <div className="page">
      <h1>Trash</h1>
      <p className="hint">Deleted contacts can be restored for {KEEP_DAYS} days, then they are removed for good.</p>
      {message && <p className="alert" role="alert">{message}</p>}
      {!result && !message && <ListSkeleton rows={3} />}
      {result && result.contacts.length === 0 && <p className="empty">The trash is empty.</p>}
      {result && result.contacts.length > 0 && (
        <ul className="trash-list">
          {result.contacts.map((c) => (
            <li key={c.id} className="trash-item">
              <Avatar firstName={c.firstName} lastName={c.lastName} />
              <span className="contact-info">
                <strong>{c.firstName} {c.lastName}</strong>
                <span className="contact-sub">{daysLeft(c.deletedAt, result.loadedAt)} day(s) left to restore</span>
              </span>
              <button type="button" className="btn btn-small" onClick={() => handleRestore(c.id)}>
                Restore
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
