import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import './index.css'
import App from './App.jsx'

// Native scroll is already smooth on macOS — no library needed.
// Lenis was intercepting gestures and throttling distance per swipe,
// requiring 10+ swipes to travel a single viewport height.

// ── React root ──────────────────────────────────────────────
const queryClient = new QueryClient()

createRoot(document.getElementById('root')).render(
  <QueryClientProvider client={queryClient}>
    <App />
  </QueryClientProvider>,
)
