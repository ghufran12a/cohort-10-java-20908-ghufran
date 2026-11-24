// The ONLY place that touches browser storage for login data.
// sessionStorage survives a page refresh but is cleared when the tab is closed.
// Trade-off: any storage readable by JavaScript can be stolen if the site has an XSS hole,
// so never insert untrusted HTML into the page (React escapes text for us by default).
const TOKEN_KEY = 'cm_token'
const USER_KEY = 'cm_user'

export const session = {
  getToken: () => sessionStorage.getItem(TOKEN_KEY),

  getUser: () => {
    try {
      return JSON.parse(sessionStorage.getItem(USER_KEY))
    } catch {
      return null
    }
  },

  save: (token, user) => {
    sessionStorage.setItem(TOKEN_KEY, token)
    sessionStorage.setItem(USER_KEY, JSON.stringify(user))
  },

  clear: () => {
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(USER_KEY)
  },
}
