import { useState } from 'react'
import { contactsApi } from '../api/contacts'
import { parseApiError } from '../api/errors'
import { Modal } from './Modal'

const EXAMPLE = `First Name,Last Name,Title,Phones,Emails
Ali,Khan,Manager,work:03001234567; home:03219876543,work:ali@test.com`

export function ImportModal({ onImported, onClose }) {
  const [file, setFile] = useState(null)
  const [result, setResult] = useState(null) // the server's report
  const [message, setMessage] = useState('')
  const [uploading, setUploading] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    if (!file) {
      setMessage('Please choose a CSV file')
      return
    }
    setUploading(true)
    setMessage('')
    try {
      const report = await contactsApi.importCsv(file)
      setResult(report)
      onImported()
    } catch (err) {
      setMessage(parseApiError(err).message)
    }
    setUploading(false)
  }

  if (result) {
    return (
      <Modal title="Import finished" onClose={onClose}>
        <p role="status">
          <strong>{result.imported}</strong> imported, <strong>{result.skipped}</strong> skipped.
        </p>
        {result.errors.length > 0 && (
          <ul className="import-errors">
            {result.errors.map((error) => <li key={error}>{error}</li>)}
          </ul>
        )}
        <div className="modal-actions">
          <button type="button" className="btn btn-primary" onClick={onClose}>Done</button>
        </div>
      </Modal>
    )
  }

  return (
    <Modal title="Import contacts" onClose={onClose}>
      <form onSubmit={handleSubmit}>
        {message && <p className="alert" role="alert">{message}</p>}
        <p className="hint">
          Choose a CSV file (maximum 1 MB, 1000 contacts). Separate several phones or emails with a
          semicolon and write them as label:value. Importing the same file twice adds the contacts twice.
        </p>
        <pre className="example">{EXAMPLE}</pre>
        <input type="file" accept=".csv,text/csv" aria-label="CSV file"
          onChange={(e) => setFile(e.target.files[0] ?? null)} />
        <div className="modal-actions">
          <button type="button" className="btn" onClick={onClose} disabled={uploading}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={uploading}>
            {uploading ? 'Importing...' : 'Import'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
