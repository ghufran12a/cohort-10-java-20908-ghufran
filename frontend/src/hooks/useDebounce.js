import { useEffect, useState } from 'react'

// "Debounce": wait until the user stops typing before using the value,
// so typing "ahmed" sends one request instead of five.
export function useDebounce(value, delayMs = 400) {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs)
    // clean-up: if the value changes again before the time is up, cancel the old timer
    return () => clearTimeout(timer)
  }, [value, delayMs])

  return debounced
}
