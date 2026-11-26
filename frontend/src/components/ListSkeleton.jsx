// Grey "ghost" rows shown while loading, so the screen does not jump when data arrives.
export function ListSkeleton({ rows = 6 }) {
  return (
    <div role="status" aria-label="Loading">
      {Array.from({ length: rows }, (_, i) => `ghost-${i}`).map((key) => (
        <div key={key} className="skeleton-row">
          <span className="skeleton skeleton-circle" />
          <span className="skeleton skeleton-text" />
        </div>
      ))}
    </div>
  )
}
