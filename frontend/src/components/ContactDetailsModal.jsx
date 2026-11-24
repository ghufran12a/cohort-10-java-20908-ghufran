import { Modal } from './Modal'

// The detailed profile of one contact.
export function ContactDetailsModal({ contact, onClose }) {
  return (
    <Modal title={`${contact.firstName} ${contact.lastName}`} onClose={onClose}>
      <p className="hint">{contact.title || 'No title'}</p>

      <h4>Phone numbers</h4>
      <ul>
        {contact.phones.map((p) => <li key={p.id}>{p.phone} <span className="tag">{p.label}</span></li>)}
      </ul>

      <h4>Email addresses</h4>
      {contact.emails.length === 0 ? (
        <p className="hint">No email added.</p>
      ) : (
        <ul>
          {contact.emails.map((e) => <li key={e.id}>{e.email} <span className="tag">{e.label}</span></li>)}
        </ul>
      )}

      <p className="hint">Added on {new Date(contact.createdAt).toLocaleDateString()}</p>
      <div className="modal-actions">
        <button type="button" className="btn" onClick={onClose}>Close</button>
      </div>
    </Modal>
  )
}
