import { useState } from 'react'
import { parseApiError } from '../api/errors'
import { useAuth } from '../auth/useAuth'
import { FormField } from './FormField'

export function LoginForm() {
  const { login } = useAuth()
  const [form, setForm] = useState({ identifier: '', password: '' })
  const [error, setError] = useState({ message: '', fieldErrors: {} })
  const [submitting, setSubmitting] = useState(false)

  // "Controlled inputs": React state is the single source of truth for what the boxes show.
  function handleChange(event) {
    const { name, value } = event.target
    setForm((previous) => ({ ...previous, [name]: value })) // copy, then change one key
  }

  async function handleSubmit(event) {
    event.preventDefault() // stop the browser from reloading the page
    setSubmitting(true)
    setError({ message: '', fieldErrors: {} })
    try {
      await login(form.identifier, form.password)
      // no redirect needed: user is now set, so PublicOnlyRoute sends us to /contacts
    } catch (err) {
      setError(parseApiError(err))
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      {error.message && <p className="alert" role="alert">{error.message}</p>}
      <FormField label="Email or phone" name="identifier" value={form.identifier}
        onChange={handleChange} error={error.fieldErrors.identifier} autoComplete="username" />
      <FormField label="Password" name="password" type="password" value={form.password}
        onChange={handleChange} error={error.fieldErrors.password} autoComplete="current-password" />
      <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
        {submitting ? 'Logging in...' : 'Log in'}
      </button>
    </form>
  )
}
