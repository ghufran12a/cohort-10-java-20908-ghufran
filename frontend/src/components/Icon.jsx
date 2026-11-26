// Small line icons drawn with SVG, so we need no icon library.
const PATHS = {
  contacts: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm-8 9a8 8 0 0 1 16 0',
  dashboard: 'M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z',
  trash: 'M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3',
  profile: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Zm0-9a3 3 0 1 0 0-6 3 3 0 0 0 0 6Zm-6 6.5a7 7 0 0 1 12 0',
  plus: 'M12 5v14M5 12h14',
  search: 'M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14Zm5-2 4 4',
  phone: 'M5 4h4l2 5-2.500 1.500a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2',
  mail: 'M3 6h18v12H3zM3 7l9 7 9-7',
  edit: 'M4 20h4L19 9l-4-4L4 16v4ZM14 6l4 4',
  back: 'M15 5l-7 7 7 7',
  moon: 'M20 14.500A8 8 0 0 1 9.500 4 8 8 0 1 0 20 14.500Z',
  sun: 'M12 16a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm0-14v2m0 16v2M2 12h2m16 0h2M5 5l1.500 1.500m11 11L19 19M5 19l1.500-1.500m11-11L19 5',
  logout: 'M14 4H5v16h9M10 12h11m-4-4 4 4-4 4',
}

export function Icon({ name, size = 20 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor"
      strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
      <path d={PATHS[name]} />
    </svg>
  )
}
