// A plain link cannot send our JWT header, so we fetch the file with axios first
// and then hand the data to the browser as a download.
export function downloadFile(blob, filename) {
  const url = URL.createObjectURL(blob) // a temporary address for data held in memory
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.append(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url) // free the memory
}
