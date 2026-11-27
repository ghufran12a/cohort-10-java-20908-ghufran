// Turns an axios error into something we can show. Our backend always sends
// { message, fieldErrors: { fieldName: "..." } } (see GlobalExceptionHandler).
export function parseApiError(error) {
  // problems found in the browser itself carry their own message
  if (error.userMessage) {
    return { message: error.userMessage, fieldErrors: {} }
  }
  if (!error.response) {
    return { message: 'Cannot reach the server. Is the backend running?', fieldErrors: {} }
  }
  const data = error.response.data ?? {}
  return {
    message: data.message ?? 'Something went wrong. Please try again.',
    fieldErrors: data.fieldErrors ?? {},
  }
}
