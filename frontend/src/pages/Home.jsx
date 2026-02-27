import { useRef, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useQuery } from '@tanstack/react-query'
import { ChevronDown } from 'lucide-react'
import api from '../services/api'

// ── Hero ─────────────────────────────────────────────────────
// No parallax — static content, entrance animation only
const Hero = () => (
    <section className="relative h-screen max-h-screen flex items-center justify-center overflow-hidden">
        {/* Breathing orb — fixed size, centered, no overflow */}
        <motion.div
            animate={{ scale: [1, 1.2, 1], opacity: [0.15, 0.25, 0.15] }}
            transition={{ duration: 8, repeat: Infinity, ease: 'easeInOut' }}
            className="w-[400px] h-[400px] rounded-full absolute pointer-events-none"
            style={{
                background: 'radial-gradient(circle, #0071e3 0%, transparent 70%)',
                top: '50%',
                left: '50%',
                transform: 'translate(-50%, -50%)',
            }}
        />

        {/* Static hero content */}
        <div className="relative z-10 text-center px-6 max-w-5xl">
            <motion.h1
                initial={{ opacity: 0, y: 40 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 1, ease: [0.16, 1, 0.3, 1] }}
                className="text-7xl md:text-9xl font-bold tracking-tight leading-none mb-6"
            >
                Shop without<br />compromise.
            </motion.h1>

            <motion.p
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 1, delay: 0.2, ease: [0.16, 1, 0.3, 1] }}
                className="text-xl mb-6"
                style={{ color: 'var(--grey-3)' }}
            >
                Premium products, fast delivery, zero friction.
            </motion.p>

            <motion.button
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 1, delay: 0.4, ease: [0.16, 1, 0.3, 1] }}
                whileHover={{ scale: 1.04 }}
                whileTap={{ scale: 0.97 }}
                className="px-8 py-4 text-white rounded-full text-lg font-medium"
                style={{ background: 'var(--accent)' }}
                onClick={() => document.getElementById('products')?.scrollIntoView({ behavior: 'smooth' })}
            >
                Browse Products
            </motion.button>
        </div>

        {/* Scroll indicator */}
        <motion.div
            className="absolute bottom-8 left-1/2 -translate-x-1/2"
            animate={{ y: [0, 8, 0] }}
            transition={{ duration: 1.5, repeat: Infinity, ease: 'easeInOut' }}
        >
            <ChevronDown className="w-6 h-6" style={{ color: 'var(--grey-3)' }} />
        </motion.div>
    </section>
)

// ── Marquee Row ───────────────────────────────────────────────
const MarqueeRow = ({ products, direction = 1, speed = 30, navigate }) => {
    const rowRef = useRef(null)
    const animRef = useRef(null)
    const posRef = useRef(0)
    const pausedRef = useRef(false)

    useEffect(() => {
        const row = rowRef.current
        if (!row) return

        const animate = () => {
            if (!pausedRef.current) {
                posRef.current -= direction * (speed / 60)
                const halfWidth = row.scrollWidth / 2
                if (direction > 0 && Math.abs(posRef.current) >= halfWidth) posRef.current = 0
                if (direction < 0 && posRef.current >= 0) posRef.current = -halfWidth
                row.style.transform = `translateX(${posRef.current}px)`
            }
            animRef.current = requestAnimationFrame(animate)
        }
        animRef.current = requestAnimationFrame(animate)
        return () => cancelAnimationFrame(animRef.current)
    }, [direction, speed])

    const doubled = [...(products || []), ...(products || [])]

    return (
        <div
            className="overflow-hidden"
            onMouseEnter={() => (pausedRef.current = true)}
            onMouseLeave={() => (pausedRef.current = false)}
        >
            <div ref={rowRef} className="flex gap-4 w-max">
                {doubled.map((product, i) => (
                    <motion.div
                        key={`${product.id}-${i}`}
                        whileHover={{ y: -6, transition: { duration: 0.3 } }}
                        className="w-56 flex-shrink-0 cursor-pointer group"
                        onClick={() => navigate(`/products/${product.id}`)}
                    >
                        <div
                            className="rounded-2xl overflow-hidden aspect-square mb-3 flex items-center justify-center p-6"
                            style={{ background: 'var(--grey-1)' }}
                        >
                            <img
                                src={product.imageUrl || `https://placehold.co/400x400?text=${encodeURIComponent(product.name)}`}
                                alt={product.name}
                                className="w-full h-full object-contain group-hover:scale-105 transition-transform duration-500"
                                onError={e => {
                                    e.target.onerror = null
                                    e.target.src = `https://placehold.co/400x400?text=${encodeURIComponent(product.name)}`
                                }}
                            />
                        </div>
                        <p className="text-xs uppercase tracking-widest mb-1" style={{ color: 'var(--grey-3)' }}>
                            {product.categoryName}
                        </p>
                        <p className="text-sm font-medium line-clamp-1">{product.name}</p>
                        <p className="text-sm" style={{ color: 'var(--grey-3)' }}>
                            ${product.price?.toFixed(2)}
                        </p>
                    </motion.div>
                ))}
            </div>
        </div>
    )
}

// ── Home Page ─────────────────────────────────────────────────
export default function Home() {
    const navigate = useNavigate()

    const { data: products = [] } = useQuery({
        queryKey: ['featured-products'],
        queryFn: () => api.get('/products?page=0&size=12').then(r => r.data.data.content),
    })

    const { data: categories = [] } = useQuery({
        queryKey: ['categories'],
        queryFn: () => api.get('/categories').then(r => r.data.data),
    })

    return (
        <main>
            <Hero />

            {/* Category pills */}
            <section className="px-6 py-8 max-w-7xl mx-auto">
                <div className="flex gap-3 overflow-x-auto scrollbar-hide">
                    {categories.map(cat => (
                        <motion.button
                            key={cat.id}
                            whileHover={{ scale: 1.03 }}
                            whileTap={{ scale: 0.97 }}
                            onClick={() => navigate(`/products?categoryId=${cat.id}`)}
                            className="px-5 py-2 rounded-full border text-sm whitespace-nowrap transition-all duration-200 flex-shrink-0"
                            style={{ borderColor: 'var(--grey-2)', color: 'var(--white)' }}
                            onMouseEnter={e => {
                                e.currentTarget.style.borderColor = 'var(--accent)'
                                e.currentTarget.style.color = 'var(--accent)'
                            }}
                            onMouseLeave={e => {
                                e.currentTarget.style.borderColor = 'var(--grey-2)'
                                e.currentTarget.style.color = 'var(--white)'
                            }}
                        >
                            {cat.name}
                        </motion.button>
                    ))}
                </div>
            </section>

            {/* Auto-scrolling marquee product rows */}
            <section id="products" className="py-12 overflow-hidden">
                <div className="max-w-7xl mx-auto px-6 mb-10 flex justify-between items-baseline">
                    <h2 className="text-4xl font-bold">Featured</h2>
                    <button
                        onClick={() => navigate('/products')}
                        className="text-sm transition-opacity hover:opacity-70"
                        style={{ color: 'var(--accent)' }}
                    >
                        View all →
                    </button>
                </div>

                {products.length === 0 ? (
                    <div className="max-w-7xl mx-auto px-6 grid grid-cols-4 gap-4">
                        {[...Array(8)].map((_, i) => (
                            <div key={i}>
                                <div className="aspect-square rounded-2xl animate-pulse mb-3" style={{ background: 'var(--grey-1)' }} />
                                <div className="h-3 rounded animate-pulse mb-2 w-2/3" style={{ background: 'var(--grey-2)' }} />
                                <div className="h-3 rounded animate-pulse w-1/3" style={{ background: 'var(--grey-2)' }} />
                            </div>
                        ))}
                    </div>
                ) : (
                    <div className="space-y-4">
                        <MarqueeRow products={products.slice(0, 8)} direction={1} speed={25} navigate={navigate} />
                        <MarqueeRow products={products.slice(4, 12)} direction={-1} speed={20} navigate={navigate} />
                    </div>
                )}
            </section>

            <div className="h-12" />
        </main>
    )
}
