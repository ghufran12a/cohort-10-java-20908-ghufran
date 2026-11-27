import { useState } from 'react'
import { parseApiError } from '../api/errors'
import { useAuth } from '../auth/useAuth'
import { FormField } from './FormField'

const EMPTY = { name: '', email: '', phone: '', password: '', confirmPassword: '' }
const NO_ERROR = { message: '', fieldErrors: {} }

export function RegisterForm() {
  const { register } = useAuth()
  const [form, setForm] = useState(EMPTY)
  const [error, setError] = useState(NO_ERROR)
  const [submitting, setSubmitting] = useState(false)

  function handleChange(event) {
    const { name, value } = event.target
    setForm((previous) => ({ ...previous, [name]: value }))
    // the old messages are about the old text, so remove them as soon as the user edits
    setError((previous) => {
      const fieldErrors = { ...previous.fieldErrors }
      delete fieldErrors[name]
      if (name === 'password') delete fieldErrors.confirmPassword // "do not match" depends on both boxes
      return { message: '', fieldErrors }
    })
  }

  async function handleSubmit(event) {
    event.preventDefault()
    // quick checks in the browser for a fast answer. The backend checks everything again.
    if (!form.email.trim() && !form.phone.trim()) {
      setError({ message: 'Please enter an email or a phone number', fieldErrors: {} })
      return
    }
    if (form.password !== form.confirmPassword) {
      setError({ message: '', fieldErrors: { confirmPassword: 'Passwords do not match' } })
      return
    }

    setSubmitting(true)
    setError(NO_ERROR)
    try {
      await register({
        name: form.name,
        email: form.email.trim(),
        phone: form.phone.trim(),
        password: form.password,
      })
    } catch (err) {
      setError(parseApiError(err))
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      {error.message && <p className="alert" role="alert">{error.message}</p>}
      <FormField label="Name" name="name" value={form.name} onChange={handleChange}
        error={error.fieldErrors.name} autoComplete="name" />
      <p className="hint">Enter an email or a phone number (at least one).</p>
      <FormField label="Email" name="email" type="email" value={form.email} onChange={handleChange}
        error={error.fieldErrors.email} autoComplete="email" />
      <FormField label="Phone" name="phone" type="tel" value={form.phone} onChange={handleChange}
        error={error.fieldErrors.phone} autoComplete="tel" />
      <FormField label="Password" name="password" type="password" value={form.password}
        onChange={handleChange} error={error.fieldErrors.password} autoComplete="new-password" />
      <FormField label="Confirm password" name="confirmPassword" type="password"
        value={form.confirmPassword} onChange={handleChange}
        error={error.fieldErrors.confirmPassword} autoComplete="new-password" />
      <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
        {submitting ? 'Creating account...' : 'Create account'}
      </button>
    </form>
  )
}
