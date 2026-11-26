import { useContext } from 'react'
import { ToastContext } from './ToastContext'

// usage:  const showToast = useToast();  showToast('Contact saved')
export function useToast() {
  return useContext(ToastContext)
}
