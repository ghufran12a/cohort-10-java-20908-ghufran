import { useState } from 'react'
import { parseApiError } from '../api/errors'
import { Modal } from './Modal'

export function DeleteConfirmModal({ contact, onConfirm, onClose }) {
  const [message, setMessage] = useState('')
  const [deleting, setDeleting] = useState(false)

  async function handleConfirm() {
    setDeleting(true)
    try {
      await onConfirm()
    } catch (err) {
      setMessage(parseApiError(err).message)
      setDeleting(false)
    }
  }

  return (
    <Modal title="Delete contact" onClose={onClose}>
      {message && <p className="alert" role="alert">{message}</p>}
      <p>
        Delete <strong>{contact.firstName} {contact.lastName}</strong>?
        You can restore it from the Trash for 30 days.
      </p>
      <div className="modal-actions">
        <button type="button" className="btn" onClick={onClose} disabled={deleting}>Cancel</button>
        <button type="button" className="btn btn-danger" onClick={handleConfirm} disabled={deleting}>
          {deleting ? 'Deleting...' : 'Confirm'}
        </button>
      </div>
    </Modal>
  )
}
