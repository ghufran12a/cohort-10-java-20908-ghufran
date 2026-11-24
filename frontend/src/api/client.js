import axios from 'axios'
import { session } from '../auth/session'

// One configured axios "messenger" that every part of the app uses to talk to the Spring Boot API.
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api',
  timeout: 15000,
})

// REQUEST interceptor: runs before every request. Attaches the JWT, so we never repeat this code.
api.interceptors.request.use((config) => {
  const token = session.getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// RESPONSE interceptor: if the server says 401 on a protected url, the token is expired/invalid.
// We announce it with an event; the AuthProvider listens and logs the user out.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthCall = error.config?.url?.startsWith('/auth/')
    if (error.response?.status === 401 && !isAuthCall) {
      window.dispatchEvent(new Event('auth:expired'))
    }
    return Promise.reject(error)
  },
)
