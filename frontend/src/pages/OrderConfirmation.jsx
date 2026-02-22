import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import axios from 'axios'

export default function OrderConfirmation() {
    const { id } = useParams()
    const [order, setOrder] = useState(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        const fetchOrder = async () => {
            try {
                const res = await axios.get(`/api/v1/orders/${id}`)
                setOrder(res.data.data)
            } catch (err) {
                console.error("Failed to fetch order", err)
            } finally {
                setLoading(false)
            }
        }
        fetchOrder()
    }, [id])

    if (loading) {
        return <div className="max-w-4xl mx-auto px-6 py-24 text-center text-gray-400">Loading order...</div>
    }

    if (!order) {
        return <div className="max-w-4xl mx-auto px-6 py-24 text-center text-gray-400">Order not found.</div>
    }

    const isFailed = order.status === 'PAYMENT_FAILED'

    return (
        <div className="max-w-2xl mx-auto px-6 py-24 text-center animate-fadeIn">
            {isFailed ? (
                <div className="w-16 h-16 bg-red-500/10 text-red-500 rounded-full flex items-center justify-center mx-auto mb-6">
                    <svg className="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                    </svg>
                </div>
            ) : (
                <div className="w-16 h-16 bg-emerald-500/10 text-emerald-500 rounded-full flex items-center justify-center mx-auto mb-6">
                    <svg className="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                    </svg>
                </div>
            )}

            <h1 className="text-3xl font-semibold mb-4">
                {isFailed ? 'Payment Failed' : 'Order Placed Successfully!'}
            </h1>
            <p className="text-gray-400 mb-8 max-w-md mx-auto">
                {isFailed
                    ? "We couldn't process your simulated card payment. Your order has been saved, but you'll need to complete payment to finalize it."
                    : "Thank you for your order. We've received it and it is now in our system."}
            </p>

            <div className={`border rounded-lg p-6 mb-8 text-left ${isFailed ? 'bg-red-500/5 border-red-900/50' : 'bg-[#0f0f0f] border-gray-800'}`}>
                <div className="flex justify-between items-center mb-4">
                    <div>
                        <div className="text-sm text-gray-500 mb-1">Order ID</div>
                        <div className="font-mono text-white text-lg break-all">{id}</div>
                    </div>
                    <div className="text-right">
                        <div className="text-sm text-gray-500 mb-1">Status</div>
                        <span className={`px-2 py-1 text-xs font-bold rounded ${isFailed ? 'bg-red-900/50 text-red-200' : 'bg-emerald-900/50 text-emerald-200'}`}>
                            {order.status}
                        </span>
                    </div>
                </div>
                <div className="border-t border-gray-800 pt-4 mt-4">
                    <div className="flex justify-between text-sm">
                        <span className="text-gray-500">Total Amount</span>
                        <span className="text-white font-medium">${order.totalAmount.toFixed(2)}</span>
                    </div>
                </div>
            </div>

            <div className="flex justify-center gap-4">
                <Link to={`/orders/${id}`} className={`px-6 py-2 rounded-md font-medium transition-colors ${isFailed ? 'bg-emerald-600 hover:bg-emerald-500 text-white' : 'bg-blue-600 hover:bg-blue-500 text-white'}`}>
                    {isFailed ? 'Try Paying Again' : 'View Order Details'}
                </Link>
                <Link to="/products" className="bg-gray-800 hover:bg-gray-700 text-white px-6 py-2 rounded-md font-medium transition-colors">
                    Continue Shopping
                </Link>
            </div>
        </div>
    )
}
