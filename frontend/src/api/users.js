import { api } from './client'

export const usersApi = {
  me: async () => (await api.get('/users/me')).data,

  changePassword: async (currentPassword, newPassword) => {
    await api.put('/users/me/password', { currentPassword, newPassword })
  },
}
