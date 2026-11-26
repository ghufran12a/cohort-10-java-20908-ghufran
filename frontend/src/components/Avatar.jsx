const COLOR_COUNT = 6

// The same name always gets the same colour: we add up the letter codes.
function colorFor(name) {
  let total = 0
  for (const letter of name) {
    total += letter.codePointAt(0)
  }
  return total % COLOR_COUNT
}

// Round circle with initials, like the Contacts app on a phone.
export function Avatar({ firstName, lastName = '', size = 'md' }) {
  const initials = `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase()
  return (
    <span className={`avatar avatar-${size} avatar-color-${colorFor(firstName + lastName)}`} aria-hidden="true">
      {initials}
    </span>
  )
}
