import type { ReactNode } from 'react'

export function Feedback({
  children,
  kind = 'error',
}: {
  children: ReactNode
  kind?: 'error' | 'success' | 'info'
}) {
  return (
    <p className={`feedback feedback-${kind}`} role={kind === 'error' ? 'alert' : 'status'}>
      {children}
    </p>
  )
}
