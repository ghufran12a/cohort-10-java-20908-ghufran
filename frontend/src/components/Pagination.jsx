// The server counts pages from 0; people count from 1.
export function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) {
    return null
  }
  return (
    <nav className="pagination" aria-label="Pages">
      <button type="button" className="btn" disabled={page === 0}
        onClick={() => onPageChange(page - 1)}>Previous</button>
      <span>Page {page + 1} of {totalPages}</span>
      <button type="button" className="btn" disabled={page + 1 >= totalPages}
        onClick={() => onPageChange(page + 1)}>Next</button>
    </nav>
  )
}
