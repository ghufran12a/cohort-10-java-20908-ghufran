// A reusable label + input + error message. The values it receives from its parent are "props".
export function FormField({ label, name, error, ...inputProps }) {
  return (
    <div className="field">
      <label htmlFor={name}>{label}</label>
      {/* {...inputProps} passes type, value, onChange... straight to the input */}
      <input id={name} name={name} aria-invalid={Boolean(error)} {...inputProps} />
      {error && <p className="field-error">{error}</p>}
    </div>
  )
}
