import { useState } from 'react'
import { Icon } from '../components/Icon'
import { LoginForm } from '../components/LoginForm'
import { RegisterForm } from '../components/RegisterForm'

// One screen, two forms. "mode" (state) decides which one is shown.
export function AuthPage() {
  const [mode, setMode] = useState('login')

  return (
    <main className="auth-page">
      <section className="card">
        <span className="brand-mark brand-mark-lg"><Icon name="contacts" size={32} /></span>
        <h1>Contact Manager</h1>
        <p className="hint center">All your people, in one place.</p>
        <div className="tabs">
          <button type="button" className={mode === 'login' ? 'tab active' : 'tab'}
            onClick={() => setMode('login')}>Log in</button>
          <button type="button" className={mode === 'register' ? 'tab active' : 'tab'}
            onClick={() => setMode('register')}>Register</button>
        </div>
        {/* conditional rendering */}
        {mode === 'login' ? <LoginForm /> : <RegisterForm />}
      </section>
    </main>
  )
}
