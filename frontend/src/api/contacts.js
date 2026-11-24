import { api } from './client'

// All contact requests in one place. Components never build urls themselves.
// REST: the url says WHAT, the HTTP method says what to DO with it.
export const contactsApi = {
  list: async ({ search, page, size }) =>
    (await api.get('/contacts', { params: { search: search || undefined, page, size } })).data,

  create: async (contact) => (await api.post('/contacts', contact)).data,

  update: async (id, contact) => (await api.put(`/contacts/${id}`, contact)).data,

  remove: async (id) => {
    await api.delete(`/contacts/${id}`)
  },

  trash: async () => (await api.get('/contacts/trash')).data,

  restore: async (id) => (await api.post(`/contacts/${id}/restore`)).data,
}
