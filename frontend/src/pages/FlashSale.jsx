import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import axios from 'axios'
import toast from 'react-hot-toast'
import useAuthStore from '../store/authStore'

// Real flash-sale storefront (Phase 9): the purchase hits the Redis hot path
// (202 + reservationId), then we poll until FlashOrderWorker materializes the
// durable order and redirect to its confirmation page.
export default function FlashSale() {
    const { user } = useAuthStore()
    const navigate = useNavigate()

    const [sale, setSale] = useState(null)
    const [loading, setLoading] = useState(true)
    const [buying, setBuying] = useState(false)
    const [outcome, setOutcome] = useState(null) // 'sold_out' | 'already' | null
    const [now, setNow] = useState(Date.now())
    const pollTimer = useRef(null)

    useEffect(() => {
        axios.get('/api/v1/flash-sales/active')
            .then(res => setSale(res.data.data))
            .catch(() => setSale(null))
            .finally(() => setLoading(false))

        const tick = setInterval(() => setNow(Date.now()), 1000)
        return () => {
            clearInterval(tick)
            if (pollTimer.current) clearInterval(pollTimer.current)
        }
    }, [])

    const pollReservation = (reservationId) => {
        let attempts = 0
        pollTimer.current = setInterval(async () => {
            attempts++
            try {
                const res = await axios.get(`/api/v1/flash-sales/reservations/${reservationId}`)
                const r = res.data.data
                if (r.status === 'CONFIRMED' && r.orderId) {
                    clearInterval(pollTimer.current)
                    toast.success('Order confirmed!')
                    navigate(`/order-confirmation/${r.orderId}`)
                }
            } catch { /* transient — keep polling */ }
            if (attempts >= 20) {
                clearInterval(pollTimer.current)
                setBuying(false)
                toast('Still processing — check your orders page in a moment', { icon: '⏳' })
            }
        }, 500)
    }

    const handleBuy = async () => {
        if (!user) {
            navigate('/login?redirect=/flash-sale')
            return
        }
        setBuying(true)
        try {
            const res = await axios.post(`/api/v1/flash-sales/${sale.id}/purchase`)
            toast.success('You got one! Confirming your order…')
            pollReservation(res.data.data.reservationId)
        } catch (err) {
            setBuying(false)
            const code = err.response?.data?.error?.code
            if (code === 'FLASH_SALE_SOLD_OUT') {
                setOutcome('sold_out')
            } else if (code === 'FLASH_SALE_ALREADY_PURCHASED') {
                setOutcome('already')
            } else {
                toast.error(err.response?.data?.error?.message || 'Purchase failed — try again')
            }
        }
    }

    const countdown = () => {
        if (!sale) return null
        const ms = new Date(sale.endsAt).getTime() - now
        if (ms <= 0) return 'Ended'
        const h = Math.floor(ms / 3600000), m = Math.floor(ms / 60000) % 60, s = Math.floor(ms / 1000) % 60
        return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
    }

    if (loading) {
        return <div className="max-w-4xl mx-auto px-6 py-24 text-center text-gray-400">Loading flash sale…</div>
    }

    if (!sale) {
        return (
            <div className="max-w-4xl mx-auto px-6 py-24 text-center">
                <h1 className="text-3xl font-semibold mb-4 text-orange-500">⚡ Flash Sale</h1>
                <p className="text-gray-400">No flash sale is running right now — check back soon.</p>
            </div>
        )
    }

    const discount = Math.round((1 - sale.salePrice / sale.originalPrice) * 100)
    const soldOut = outcome === 'sold_out' || sale.remainingStock === 0

    return (
        <div className="max-w-4xl mx-auto px-6 py-12">
            <div className="flex items-center justify-between mb-8">
                <h1 className="text-3xl font-semibold text-orange-500">⚡ Flash Sale</h1>
                <div className="text-right">
                    <div className="text-xs text-gray-500 uppercase tracking-wider">Ends in</div>
                    <div className="text-2xl font-mono text-white">{countdown()}</div>
                </div>
            </div>

            <div className="bg-[#0f0f0f] border border-orange-900/40 rounded-xl p-8 flex flex-col md:flex-row gap-10">
                <div className="w-full md:w-1/2 aspect-square bg-white rounded-lg flex items-center justify-center p-6">
                    <img
                        src={sale.productImageUrl || `https://placehold.co/400x400?text=${encodeURIComponent(sale.productName)}`}
                        alt={sale.productName}
                        className="max-w-full max-h-full object-contain"
                    />
                </div>

                <div className="w-full md:w-1/2 flex flex-col justify-center">
                    <h2 className="text-2xl font-medium mb-4">{sale.productName}</h2>
                    <div className="flex items-baseline gap-4 mb-2">
                        <span className="text-4xl font-semibold text-orange-400">${sale.salePrice.toFixed(2)}</span>
                        <span className="text-xl text-gray-500 line-through">${sale.originalPrice.toFixed(2)}</span>
                        <span className="bg-orange-600 text-white text-xs font-bold px-2 py-1 rounded">-{discount}%</span>
                    </div>
                    {sale.remainingStock != null && (
                        <p className="text-sm text-gray-400 mb-6">
                            {sale.remainingStock > 0
                                ? `${sale.remainingStock} of ${sale.totalStock} left`
                                : 'Sold out'}
                        </p>
                    )}

                    {outcome === 'already' ? (
                        <div className="bg-green-900/20 border border-green-800 text-green-400 rounded-md py-4 text-center">
                            ✓ You already secured one — see your orders
                        </div>
                    ) : soldOut ? (
                        <div className="bg-gray-900 border border-gray-800 text-gray-500 rounded-md py-4 text-center">
                            Sold out — better luck next drop
                        </div>
                    ) : (
                        <button
                            onClick={handleBuy}
                            disabled={buying}
                            className="w-full bg-orange-600 hover:bg-orange-500 text-white font-bold py-4 rounded-md transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {buying ? 'Securing yours…' : 'Buy now — one per customer'}
                        </button>
                    )}

                    <p className="text-xs text-gray-600 mt-4">
                        Purchases are atomic (Redis Lua) — no overselling, one unit per account.
                    </p>
                </div>
            </div>
        </div>
    )
}
