import { useState } from 'react'
import { parseApiError } from '../api/errors'
import { FormField } from './FormField'
import { Modal } from './Modal'

const LABELS = ['work', 'home', 'personal', 'other']

// every row needs a stable "key" so React can tell the rows apart when one is removed
let nextKey = 0
const newRow = (value = '', label = 'personal') => ({ key: ++nextKey, value, label })

// Turns a contact from the API (or nothing) into the form's starting state.
function toForm(contact) {
  if (!contact) {
    return { firstName: '', lastName: '', title: '', emails: [], phones: [newRow()] }
  }
  return {
    firstName: contact.firstName,
    lastName: contact.lastName,
    title: contact.title ?? '',
    emails: contact.emails.map((e) => newRow(e.email, e.label)),
    phones: contact.phones.map((p) => newRow(p.phone, p.label)),
  }
}

// One list of "value + label" rows. Used twice: for emails and for phones.
function RowList({ title, field, rows, type, errors, errorName, onChange, onAdd, onRemove }) {
  return (
    <fieldset className="rows">
      <legend>{title}</legend>
      {/* .map() turns an array of data into an array of elements */}
      {rows.map((row, index) => {
        const error = errors[`${field}[${index}].${errorName}`] ?? errors[`${field}[${index}].label`]
        const options = LABELS.includes(row.label) ? LABELS : [...LABELS, row.label]
        return (
          <div key={row.key}>
            <div className="row">
              <input type={type} value={row.value} aria-label={`${title} ${index + 1}`}
                aria-invalid={Boolean(error)}
                onChange={(e) => onChange(row.key, { value: e.target.value })} />
              <select value={row.label} aria-label={`${title} ${index + 1} label`}
                onChange={(e) => onChange(row.key, { label: e.target.value })}>
                {options.map((label) => <option key={label} value={label}>{label}</option>)}
              </select>
              <button type="button" className="btn" onClick={() => onRemove(row.key)}
                aria-label={`Remove ${title} ${index + 1}`}>✕</button>
            </div>
            {error && <p className="field-error">{error}</p>}
          </div>
        )
      })}
      {errors[field] && <p className="field-error">{errors[field]}</p>}
      <button type="button" className="btn btn-small" onClick={onAdd}>+ Add</button>
    </fieldset>
  )
}

// Used for BOTH popups: "Create" (no contact given) and "Update" (contact given -> prefilled).
export function ContactFormModal({ contact, onSave, onClose }) {
  const [form, setForm] = useState(() => toForm(contact))
  const [error, setError] = useState({ message: '', fieldErrors: {} })
  const [saving, setSaving] = useState(false)

  function handleChange(event) {
    const { name, value } = event.target
    setForm((previous) => ({ ...previous, [name]: value }))
  }

  // Never edit state directly: build a NEW array with the change
  const changeRow = (field) => (key, changes) =>
    setForm((f) => ({ ...f, [field]: f[field].map((r) => (r.key === key ? { ...r, ...changes } : r)) }))
  const addRow = (field) => () => setForm((f) => ({ ...f, [field]: [...f[field], newRow()] }))
  const removeRow = (field) => (key) =>
    setForm((f) => ({ ...f, [field]: f[field].filter((r) => r.key !== key) }))

  async function handleSubmit(event) {
    event.preventDefault()
    setSaving(true)
    setError({ message: '', fieldErrors: {} })
    try {
      // shape the data exactly like the backend's ContactRequest
      await onSave({
        firstName: form.firstName,
        lastName: form.lastName,
        title: form.title,
        emails: form.emails.map((r) => ({ email: r.value.trim(), label: r.label })),
        phones: form.phones.map((r) => ({ phone: r.value.trim(), label: r.label })),
      })
      // on success the parent closes this popup
    } catch (err) {
      setError(parseApiError(err))
      setSaving(false)
    }
  }

  return (
    <Modal title={contact ? 'Update contact' : 'Create contact'} onClose={onClose}>
      <form onSubmit={handleSubmit} noValidate>
        {error.message && <p className="alert" role="alert">{error.message}</p>}
        <FormField label="First name" name="firstName" value={form.firstName}
          onChange={handleChange} error={error.fieldErrors.firstName} />
        <FormField label="Last name" name="lastName" value={form.lastName}
          onChange={handleChange} error={error.fieldErrors.lastName} />
        <FormField label="Title (optional)" name="title" value={form.title}
          onChange={handleChange} error={error.fieldErrors.title} />
        <RowList title="Phone" field="phones" type="tel" errorName="phone" rows={form.phones}
          errors={error.fieldErrors} onChange={changeRow('phones')} onAdd={addRow('phones')}
          onRemove={removeRow('phones')} />
        <RowList title="Email" field="emails" type="email" errorName="email" rows={form.emails}
          errors={error.fieldErrors} onChange={changeRow('emails')} onAdd={addRow('emails')}
          onRemove={removeRow('emails')} />
        <div className="modal-actions">
          <button type="button" className="btn" onClick={onClose} disabled={saving}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Saving...' : 'Save'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
