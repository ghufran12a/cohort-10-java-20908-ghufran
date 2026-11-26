import { Avatar } from './Avatar'
import { Icon } from './Icon'

// A tappable "label above value" line. tel: and mailto: open the phone / mail app.
function InfoLine({ label, value, href }) {
  return (
    <li className="info-line">
      <span className="info-label">{label}</span>
      <a href={href}>{value}</a>
    </li>
  )
}

function QuickAction({ icon, label, href }) {
  if (!href) {
    return (
      <span className="quick quick-off">
        <Icon name={icon} />
        <span>{label}</span>
      </span>
    )
  }
  return (
    <a className="quick" href={href}>
      <Icon name={icon} />
      <span>{label}</span>
    </a>
  )
}

// The detailed profile of one contact, with the Update and Delete buttons.
export function ContactDetail({ contact, onBack, onUpdate, onDelete }) {
  const phone = contact.phones[0]?.phone
  const email = contact.emails[0]?.email

  return (
    <article className="detail">
      <button type="button" className="back" onClick={onBack}>
        <Icon name="back" /> Contacts
      </button>

      <header className="detail-head">
        <Avatar firstName={contact.firstName} lastName={contact.lastName} size="lg" />
        <h2>{contact.firstName} {contact.lastName}</h2>
        {contact.title && <p className="hint">{contact.title}</p>}
        <div className="quick-row">
          <QuickAction icon="phone" label="Call" href={phone && `tel:${phone}`} />
          <QuickAction icon="mail" label="Email" href={email && `mailto:${email}`} />
        </div>
        <div className="detail-actions">
          <button type="button" className="btn btn-primary" onClick={onUpdate}>Update</button>
          <button type="button" className="btn btn-danger" onClick={onDelete}>Delete</button>
        </div>
      </header>

      <ul className="info-card" aria-label="Phone numbers">
        {contact.phones.map((p) => (
          <InfoLine key={p.id} label={p.label} value={p.phone} href={`tel:${p.phone}`} />
        ))}
      </ul>

      {contact.emails.length > 0 && (
        <ul className="info-card" aria-label="Email addresses">
          {contact.emails.map((e) => (
            <InfoLine key={e.id} label={e.label} value={e.email} href={`mailto:${e.email}`} />
          ))}
        </ul>
      )}

      <p className="hint center">Added on {new Date(contact.createdAt).toLocaleDateString()}</p>
    </article>
  )
}
