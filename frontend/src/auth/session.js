// The ONLY place that touches browser storage for login data.
// sessionStorage survives a page refresh but is cleared when the tab is closed.
// Trade-off: any storage readable by JavaScript can be stolen if the site has an XSS hole,
// so never insert untrusted HTML into the page (React escapes text for us by default).
const TOKEN_KEY = 'cm_token'
const USER_KEY = 'cm_user'

// The browser can BLOCK storage (privacy settings). Then even reading sessionStorage throws an error,
// so every access here is protected and the app keeps working.
function removeQuietly(key) {
  try {
    sessionStorage.removeItem(key)
  } catch (error) {
    console.warn('Could not clear browser storage', error)
  }
}

export const session = {
  getToken: () => {
    try {
      return sessionStorage.getItem(TOKEN_KEY)
    } catch {
      return null // storage is blocked: treat the visitor as logged out
    }
  },

  // false = login cannot work, and the login screen tells the user
  isAvailable: () => {
    try {
      sessionStorage.getItem(TOKEN_KEY)
      return true
    } catch {
      return false
    }
  },

  getUser: () => {
    try {
      return JSON.parse(sessionStorage.getItem(USER_KEY))
    } catch {
      return null
    }
  },

  // returns false when the login could not be saved
  save: (token, user) => {
    try {
      sessionStorage.setItem(TOKEN_KEY, token)
      sessionStorage.setItem(USER_KEY, JSON.stringify(user))
      return true
    } catch {
      session.clear() // never keep half of a login
      return false
    }
  },

  // each key on its own: if one removal fails, the other is still tried
  clear: () => {
    removeQuietly(TOKEN_KEY)
    removeQuietly(USER_KEY)
  },
}
