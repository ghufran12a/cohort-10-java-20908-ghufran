import { useEffect, useRef } from 'react'

// A reusable popup built on the browser's own <dialog> element: it blocks the page behind it,
// keeps keyboard focus inside, and closes with the Esc key.
export function Modal({ title, onClose, children }) {
  // useRef gives us direct access to the real DOM element
  const dialogRef = useRef(null)

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog.open) {
      dialog.showModal()
    }
  }, [])

  function handleCancel(event) {
    event.preventDefault() // Esc key: let React (our state) close it
    onClose()
  }

  return (
    <dialog ref={dialogRef} className="modal" onCancel={handleCancel} aria-label={title}>
      <h3>{title}</h3>
      {/* "children" = whatever was written between <Modal> and </Modal> */}
      {children}
    </dialog>
  )
}
