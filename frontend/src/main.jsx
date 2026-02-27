import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import Lenis from '@studio-freight/lenis'
import './index.css'
import App from './App.jsx'

// ── Lenis smooth scroll — initialized once globally ──────────
const lenis = new Lenis({
  duration: 0.8,
  easing: t => 1 - Math.pow(1 - t, 4),
  smoothWheel: true,
  wheelMultiplier: 1,
  touchMultiplier: 1,
})

lenis.on('scroll', () => { })

function raf(time) {
  lenis.raf(time)
  requestAnimationFrame(raf)
}
requestAnimationFrame(raf)

// ── React root ──────────────────────────────────────────────
const queryClient = new QueryClient()

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </StrictMode>,
)
