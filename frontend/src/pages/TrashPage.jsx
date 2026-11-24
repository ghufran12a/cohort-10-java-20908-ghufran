import { useEffect, useState } from 'react'
import { contactsApi } from '../api/contacts'
import { parseApiError } from '../api/errors'

const KEEP_DAYS = 30
const DAY_MS = 24 * 60 * 60 * 1000

function daysLeft(deletedAt, now) {
  const gone = (now - new Date(deletedAt).getTime()) / DAY_MS
  return Math.max(0, Math.ceil(KEEP_DAYS - gone))
}

export function TrashPage() {
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
    } catch (err) {
      setMessage(parseApiError(err).message)
    }
    setReloadCount((n) => n + 1)
  }

  return (
    <section>
      <h2>Trash</h2>
      <p className="hint">Deleted contacts can be restored for {KEEP_DAYS} days, then they are removed for good.</p>
      {message && <p className="alert" role="alert">{message}</p>}
      {!result && !message && <p>Loading...</p>}
      {result && result.contacts.length === 0 && <p className="empty">The trash is empty.</p>}
      {result && result.contacts.length > 0 && (
        <ul className="contact-list">
          {result.contacts.map((c) => (
            <li key={c.id} className="contact-item">
              <div className="contact-info">
                <strong>{c.firstName} {c.lastName}</strong>
                <span className="hint">{daysLeft(c.deletedAt, result.loadedAt)} day(s) left to restore</span>
              </div>
              <div className="contact-actions">
                <button type="button" className="btn btn-small" onClick={() => handleRestore(c.id)}>
                  Restore
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
