import { useEffect, useState } from 'react'
import { contactsApi } from '../api/contacts'
import { parseApiError } from '../api/errors'
import { usersApi } from '../api/users'
import { useAuth } from '../auth/useAuth'
import { ChangePasswordModal } from '../components/ChangePasswordModal'

export function ProfilePage() {
  const { logout } = useAuth()
  const [info, setInfo] = useState(null) // { profile, contactCount, trashCount }
  const [loadError, setLoadError] = useState('')
  const [success, setSuccess] = useState('')
  const [showPasswordModal, setShowPasswordModal] = useState(false)

  // [] = run once, when the page is first shown
  useEffect(() => {
    let ignore = false
    // Promise.all sends the three requests at the same time and waits for all of them
    Promise.all([usersApi.me(), contactsApi.list({ page: 0, size: 1 }), contactsApi.trash()])
      .then(([profile, contacts, trash]) => {
        if (!ignore) {
          setInfo({ profile, contactCount: contacts.totalElements, trashCount: trash.length })
        }
      })
      .catch((err) => {
        if (!ignore) setLoadError(parseApiError(err).message)
      })
    return () => {
      ignore = true
    }
  }, [])

  function handlePasswordChanged() {
    setShowPasswordModal(false)
    setSuccess('Your password has been changed.')
  }

  return (
    <section>
      <h2>My profile</h2>
      {success && <p className="success" role="status">{success}</p>}
      {loadError && <p className="alert" role="alert">{loadError}</p>}
      {!info && !loadError && <p>Loading profile...</p>}

      {info && (
        <>
          <div className="stats">
            <div className="stat"><strong>{info.contactCount}</strong><span>Contacts</span></div>
            <div className="stat"><strong>{info.trashCount}</strong><span>In trash</span></div>
          </div>
          {/* dl = description list: pairs of "name: value" */}
          <dl className="details">
            <dt>Name</dt>
            <dd>{info.profile.name}</dd>
            <dt>Email</dt>
            <dd>{info.profile.email ?? 'Not provided'}</dd>
            <dt>Phone</dt>
            <dd>{info.profile.phone ?? 'Not provided'}</dd>
            <dt>Member since</dt>
            <dd>{new Date(info.profile.createdAt).toLocaleDateString()}</dd>
          </dl>
        </>
      )}

      <div className="profile-actions">
        <button type="button" className="btn btn-primary"
          onClick={() => {
            setSuccess('')
            setShowPasswordModal(true)
          }}>
          Change password
        </button>
        <button type="button" className="btn btn-danger" onClick={logout}>Logout</button>
      </div>

      {showPasswordModal && (
        <ChangePasswordModal onChanged={handlePasswordChanged}
          onClose={() => setShowPasswordModal(false)} />
      )}
    </section>
  )
}
