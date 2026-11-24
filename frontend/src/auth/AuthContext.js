import { createContext } from 'react'

// A "context" is like a public notice board: any component below the provider can read it,
// without passing props through every level in between.
export const AuthContext = createContext(null)
