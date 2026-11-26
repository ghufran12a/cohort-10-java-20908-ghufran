import { Avatar } from './Avatar'

// Puts contacts under A, B, C... headings. The server already sends them sorted by first name.
function groupByLetter(contacts) {
  const groups = new Map()
  for (const contact of contacts) {
    const first = contact.firstName.charAt(0).toUpperCase()
    const letter = /\p{L}/u.test(first) ? first : '#' // numbers and symbols go under #
    if (!groups.has(letter)) {
      groups.set(letter, [])
    }
    groups.get(letter).push(contact)
  }
  return [...groups]
}

export function ContactList({ contacts, selectedId, onSelect }) {
  return groupByLetter(contacts).map(([letter, items]) => (
    <section key={letter} aria-label={letter}>
      <h3 className="letter">{letter}</h3>
      <ul className="rows-list">
        {items.map((contact) => (
          <li key={contact.id}>
            <button type="button" className={contact.id === selectedId ? 'contact-row selected' : 'contact-row'}
              aria-current={contact.id === selectedId} onClick={() => onSelect(contact)}>
              <Avatar firstName={contact.firstName} lastName={contact.lastName} />
              <span className="contact-info">
                <span className="contact-name">{contact.firstName} <strong>{contact.lastName}</strong></span>
                <span className="contact-sub">{contact.title || contact.phones[0]?.phone}</span>
              </span>
            </button>
          </li>
        ))}
      </ul>
    </section>
  ))
}
