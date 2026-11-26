import { useEffect, useState } from 'react'
import { contactsApi } from '../api/contacts'
import { parseApiError } from '../api/errors'
import { ContactDetail } from '../components/ContactDetail'
import { ContactFormModal } from '../components/ContactFormModal'
import { ContactList } from '../components/ContactList'
import { DeleteConfirmModal } from '../components/DeleteConfirmModal'
import { Icon } from '../components/Icon'
import { ImportModal } from '../components/ImportModal'
import { ListSkeleton } from '../components/ListSkeleton'
import { Pagination } from '../components/Pagination'
import { useDebounce } from '../hooks/useDebounce'
import { useToast } from '../toast/useToast'
import { downloadFile } from '../utils/downloadFile'

const PAGE_SIZE = 10

// "Master-detail" screen: the list on the left, the chosen contact on the right.
// On a phone only one of the two is visible at a time.
export function ContactsPage() {
  const showToast = useToast()
  const [searchText, setSearchText] = useState('')
  const search = useDebounce(searchText.trim())
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null) // null = not loaded yet
  const [loadError, setLoadError] = useState('')
  const [reloadCount, setReloadCount] = useState(0) // changing it re-runs the effect below
  const [selected, setSelected] = useState(null) // the contact shown on the right
  const [modal, setModal] = useState(null) // null | 'create' | 'edit' | 'delete' | 'import'

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

  // after a successful change: close the popup, show the contact, fetch the list again
  async function handleSave(values) {
    const editing = modal === 'edit'
    const saved = editing
      ? await contactsApi.update(selected.id, values)
      : await contactsApi.create(values)
    setSelected(saved)
    closeModal()
    reload()
    showToast(editing ? 'Contact updated' : 'Contact created')
  }

  async function handleDelete() {
    await contactsApi.remove(selected.id)
    setSelected(null)
    closeModal()
    reload()
    showToast('Contact moved to Trash')
  }

  async function handleExport() {
    try {
      downloadFile(await contactsApi.exportCsv(), 'contacts.csv')
      showToast('Contacts exported')
    } catch (err) {
      showToast(parseApiError(err).message)
    }
  }

  return (
    <div className={selected ? 'split has-selection' : 'split'}>
      <section className="list-pane" aria-label="Contact list">
        <div className="list-head">
          <div className="list-title">
            <h1>Contacts</h1>
            <button type="button" className="btn btn-primary btn-pill" onClick={() => setModal('create')}>
              <Icon name="plus" size={18} /> Create contact
            </button>
          </div>
          <label className="search-box">
            <Icon name="search" size={18} />
            <input type="search" placeholder="Search by first or last name"
              aria-label="Search by first or last name" value={searchText}
              onChange={(e) => {
                setSearchText(e.target.value)
                setPage(0) // a new search always starts on the first page
              }} />
          </label>
          <div className="list-tools">
            {data && (
              <p className="count">{data.totalElements} {search ? 'matching' : 'saved'} contact(s)</p>
            )}
            <button type="button" className="link-btn" onClick={() => setModal('import')}>Import</button>
            <button type="button" className="link-btn" onClick={handleExport}>Export</button>
          </div>
        </div>

        <div className="list-scroll">
          {loadError && (
            <p className="alert" role="alert">
              {loadError} <button type="button" className="btn btn-small" onClick={reload}>Retry</button>
            </p>
          )}
          {!data && !loadError && <ListSkeleton />}
          {data && data.content.length === 0 && (
            <p className="empty">
              {search ? 'No contacts match your search.' : 'No contacts yet. Create your first one.'}
            </p>
          )}
          {data && (
            <ContactList contacts={data.content} selectedId={selected?.id} onSelect={setSelected} />
          )}
        </div>

        {data && <Pagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />}
      </section>

      <section className="detail-pane" aria-label="Contact details">
        {selected ? (
          <ContactDetail contact={selected} onBack={() => setSelected(null)}
            onUpdate={() => setModal('edit')} onDelete={() => setModal('delete')} />
        ) : (
          <div className="placeholder">
            <Icon name="contacts" size={56} />
            <p>Select a contact to see the details</p>
          </div>
        )}
      </section>

      {(modal === 'create' || modal === 'edit') && (
        <ContactFormModal contact={modal === 'edit' ? selected : undefined}
          onSave={handleSave} onClose={closeModal} />
      )}
      {modal === 'import' && <ImportModal onImported={reload} onClose={closeModal} />}
      {modal === 'delete' && (
        <DeleteConfirmModal contact={selected} onConfirm={handleDelete} onClose={closeModal} />
      )}
    </div>
  )
}
