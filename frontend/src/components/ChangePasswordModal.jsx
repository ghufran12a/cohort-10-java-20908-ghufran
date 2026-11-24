import { useState } from 'react'
import { parseApiError } from '../api/errors'
import { usersApi } from '../api/users'
import { FormField } from './FormField'
import { Modal } from './Modal'

const EMPTY = { currentPassword: '', newPassword: '', confirmPassword: '' }
const NO_ERROR = { message: '', fieldErrors: {} }

export function ChangePasswordModal({ onChanged, onClose }) {
  const [form, setForm] = useState(EMPTY)
  const [error, setError] = useState(NO_ERROR)
  const [saving, setSaving] = useState(false)

  function handleChange(event) {
    const { name, value } = event.target
    setForm((previous) => ({ ...previous, [name]: value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    // only the browser knows about "confirm": it protects against a typing mistake
    if (form.newPassword !== form.confirmPassword) {
      setError({ message: '', fieldErrors: { confirmPassword: 'Passwords do not match' } })
      return
    }
    setSaving(true)
    setError(NO_ERROR)
    try {
      await usersApi.changePassword(form.currentPassword, form.newPassword)
      onChanged() // the parent closes the popup
    } catch (err) {
      setError(parseApiError(err))
      setSaving(false)
    }
  }

  return (
    <Modal title="Change password" onClose={onClose}>
      <form onSubmit={handleSubmit} noValidate>
        {error.message && <p className="alert" role="alert">{error.message}</p>}
        <FormField label="Current password" name="currentPassword" type="password"
          value={form.currentPassword} onChange={handleChange}
          error={error.fieldErrors.currentPassword} autoComplete="current-password" />
        <FormField label="New password" name="newPassword" type="password"
          value={form.newPassword} onChange={handleChange}
          error={error.fieldErrors.newPassword} autoComplete="new-password" />
        <p className="hint">At least 8 characters, with a letter and a number.</p>
        <FormField label="Confirm new password" name="confirmPassword" type="password"
          value={form.confirmPassword} onChange={handleChange}
          error={error.fieldErrors.confirmPassword} autoComplete="new-password" />
        <div className="modal-actions">
          <button type="button" className="btn" onClick={onClose} disabled={saving}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Resetting...' : 'Reset'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
