import { useState } from 'react'
import { LoginForm } from '../components/LoginForm'
import { RegisterForm } from '../components/RegisterForm'

// One screen, two forms. "mode" (state) decides which one is shown.
export function AuthPage() {
  const [mode, setMode] = useState('login')

  return (
    <main className="auth-page">
      <section className="card">
        <h1>Contact Manager</h1>
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
