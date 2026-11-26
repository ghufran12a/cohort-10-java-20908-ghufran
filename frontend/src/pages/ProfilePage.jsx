import { useEffect, useState } from 'react'
import { parseApiError } from '../api/errors'
import { usersApi } from '../api/users'
import { useAuth } from '../auth/useAuth'
import { Avatar } from '../components/Avatar'
import { ChangePasswordModal } from '../components/ChangePasswordModal'
import { ThemeToggle } from '../components/ThemeToggle'
import { useToast } from '../toast/useToast'

export function ProfilePage() {
  const { logout } = useAuth()
  const showToast = useToast()
  const [profile, setProfile] = useState(null)
  const [loadError, setLoadError] = useState('')
  const [showPasswordModal, setShowPasswordModal] = useState(false)

  // [] = run once, when the page is first shown
  useEffect(() => {
    let ignore = false
    usersApi.me()
      .then((result) => {
        if (!ignore) setProfile(result)
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
    showToast('Your password has been changed.')
  }

  return (
    <div className="page">
      <h1>My profile</h1>
      {loadError && <p className="alert" role="alert">{loadError}</p>}
      {!profile && !loadError && <p>Loading profile...</p>}

      {profile && (
        <>
          <header className="detail-head">
            <Avatar firstName={profile.name} size="lg" />
            <h2>{profile.name}</h2>
          </header>
          {/* dl = description list: pairs of "name: value" */}
          <dl className="details">
            <dt>Name</dt>
            <dd>{profile.name}</dd>
            <dt>Email</dt>
            <dd>{profile.email ?? 'Not provided'}</dd>
            <dt>Phone</dt>
            <dd>{profile.phone ?? 'Not provided'}</dd>
            <dt>Member since</dt>
            <dd>{new Date(profile.createdAt).toLocaleDateString()}</dd>
          </dl>
        </>
      )}

      <div className="profile-actions">
        <button type="button" className="btn btn-primary" onClick={() => setShowPasswordModal(true)}>
          Change password
        </button>
        <button type="button" className="btn btn-danger" onClick={logout}>Logout</button>
      </div>
      {/* on a phone the sidebar is hidden, so the theme switch is also here */}
      <div className="phone-only">
        <ThemeToggle />
      </div>

      {showPasswordModal && (
        <ChangePasswordModal onChanged={handlePasswordChanged}
          onClose={() => setShowPasswordModal(false)} />
      )}
    </div>
  )
}
