import { useEffect, useState } from 'react'
import { contactsApi } from '../api/contacts'
import { parseApiError } from '../api/errors'
import { ContactDetailsModal } from '../components/ContactDetailsModal'
import { ContactFormModal } from '../components/ContactFormModal'
import { DeleteConfirmModal } from '../components/DeleteConfirmModal'
import { Pagination } from '../components/Pagination'
import { useDebounce } from '../hooks/useDebounce'

const PAGE_SIZE = 10

export function ContactsPage() {
  const [searchText, setSearchText] = useState('')
  const search = useDebounce(searchText.trim())
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null) // null = not loaded yet
  const [loadError, setLoadError] = useState('')
  const [reloadCount, setReloadCount] = useState(0) // changing it re-runs the effect below
  // which popup is open: null, or { type: 'create' | 'edit' | 'delete' | 'view', contact }
  const [modal, setModal] = useState(null)

  // Runs after the first render and again whenever search, page or reloadCount changes.
  useEffect(() => {
    let ignore = false // protects against an OLD, slower response overwriting a newer one

    // .then() runs when the answer arrives, .catch() when something fails
    contactsApi.list({ search, page, size: PAGE_SIZE })
      .then((result) => {
        if (ignore) return
        // e.g. we deleted the last contact on the last page: step back one page
        if (result.content.length === 0 && page > 0) {
          setPage(page - 1)
          return
        }
        setData(result)
        setLoadError('')
      })
      .catch((err) => {
        if (!ignore) setLoadError(parseApiError(err).message)
      })

    return () => {
      ignore = true
    }
  }, [search, page, reloadCount])

  const reload = () => setReloadCount((n) => n + 1)
  const closeModal = () => setModal(null)

  // after a successful change: close the popup, then fetch the list again
  async function handleSave(values) {
    if (modal.type === 'edit') {
      await contactsApi.update(modal.contact.id, values)
    } else {
      await contactsApi.create(values)
    }
    closeModal()
    reload()
  }

  async function handleDelete() {
    await contactsApi.remove(modal.contact.id)
    closeModal()
    reload()
  }

  return (
    <section>
      <div className="page-head">
        <div>
          <h2>My contacts</h2>
          {data && (
            <p className="hint">
              {data.totalElements} {search ? 'matching' : 'saved'} contact(s)
            </p>
          )}
        </div>
        <button type="button" className="btn btn-primary" onClick={() => setModal({ type: 'create' })}>
          + Create contact
        </button>
      </div>

      <input type="search" className="search" placeholder="Search by first or last name"
        aria-label="Search by first or last name" value={searchText}
        onChange={(e) => {
          setSearchText(e.target.value)
          setPage(0) // a new search always starts on the first page
        }} />

      {loadError && (
        <p className="alert" role="alert">
          {loadError} <button type="button" className="btn btn-small" onClick={reload}>Retry</button>
        </p>
      )}
      {!data && !loadError && <p>Loading contacts...</p>}
      {data && data.content.length === 0 && (
        <p className="empty">{search ? 'No contacts match your search.' : 'No contacts yet. Create your first one.'}</p>
      )}

      {data && data.content.length > 0 && (
        <ul className="contact-list">
          {data.content.map((contact) => (
            <li key={contact.id} className="contact-item">
              <div className="contact-info">
                <strong>{contact.firstName} {contact.lastName}</strong>
                <span className="hint">
                  {[contact.title, contact.phones[0]?.phone, contact.emails[0]?.email]
                    .filter(Boolean).join(' · ')}
                </span>
              </div>
              <div className="contact-actions">
                <button type="button" className="btn btn-small"
                  onClick={() => setModal({ type: 'view', contact })}>View</button>
                <button type="button" className="btn btn-small"
                  onClick={() => setModal({ type: 'edit', contact })}>Update</button>
                <button type="button" className="btn btn-small btn-danger"
                  onClick={() => setModal({ type: 'delete', contact })}>Delete</button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {data && <Pagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />}

      {(modal?.type === 'create' || modal?.type === 'edit') && (
        <ContactFormModal contact={modal.contact} onSave={handleSave} onClose={closeModal} />
      )}
      {modal?.type === 'delete' && (
        <DeleteConfirmModal contact={modal.contact} onConfirm={handleDelete} onClose={closeModal} />
      )}
      {modal?.type === 'view' && <ContactDetailsModal contact={modal.contact} onClose={closeModal} />}
    </section>
  )
}
