import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import axios from 'axios'
import toast from 'react-hot-toast'
import useAuthStore from '../store/authStore'

export default function OrderDetail() {
    const { id } = useParams()
    const { user } = useAuthStore()
    const [order, setOrder] = useState(null)
    const [loading, setLoading] = useState(true)
    const [actionLoading, setActionLoading] = useState(false)

    const fetchOrder = async () => {
        try {
            const res = await axios.get(`/api/v1/orders/${id}`)
            setOrder(res.data.data)
        } catch (err) {
            toast.error('Failed to load order details')
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        if (user) fetchOrder()
    }, [user, id])

    // Poll for status updates
    useEffect(() => {
        if (!user || !order || ['DELIVERED', 'CANCELLED'].includes(order.status)) return
        const interval = setInterval(() => {
            fetchOrder()
        }, 5000)
        return () => clearInterval(interval)
    }, [user, id, order?.status])

    const handleCancel = async () => {
        if (!window.confirm('Are you sure you want to cancel this order?')) return
        setActionLoading(true)
        try {
            await axios.put(`/api/v1/orders/${id}/cancel`)
            toast.success('Order cancelled successfully')
            fetchOrder()
        } catch (err) {
            toast.error(err.response?.data?.error?.message || 'Failed to cancel order')
        } finally {
            setActionLoading(false)
        }
    }

    const handleSimulatePayment = async () => {
        setActionLoading(true)
        try {
            await axios.post(`/api/v1/orders/${id}/pay`)
            toast.success('Payment processed successfully')
            fetchOrder()
        } catch (err) {
            toast.error(err.response?.data?.error?.message || 'Payment failed')
        } finally {
            setActionLoading(false)
        }
    }

    if (!user || loading) {
        return <div className="max-w-4xl mx-auto px-6 py-24 text-center text-gray-400">Loading order details...</div>
    }

    if (!order) {
        return <div className="max-w-4xl mx-auto px-6 py-24 text-center text-gray-400">Order not found.</div>
    }

    return (
        <div className="max-w-4xl mx-auto px-6 py-12">
            <div className="mb-8">
                <Link to="/orders" className="text-gray-400 hover:text-white mb-4 inline-block">&larr; Back to Orders</Link>
                <div className="flex justify-between items-start">
                    <div>
                        <h1 className="text-3xl font-semibold mb-2">Order Details</h1>
                        <p className="font-mono text-sm text-gray-500">ID: {order.id}</p>
                    </div>
                    <div>
                        <span className="px-3 py-1 border text-xs font-bold uppercase tracking-wider rounded bg-gray-800 text-white">
                            {order.status}
                        </span>
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-8">
                <div className="md:col-span-2 bg-[#0f0f0f] border border-gray-800 rounded-lg p-6">
                    <h2 className="text-xl font-medium mb-6">Items</h2>
                    <div className="space-y-4">
                        {(order.items || []).map(item => (
                            <div key={item.id} className="flex justify-between items-center border-b border-gray-800 last:border-0 pb-4 last:pb-0">
                                <div>
                                    <Link to={`/products/${item.productId}`} className="font-medium text-white hover:text-blue-400 block mb-1">
                                        {item.productName}
                                    </Link>
                                    <p className="text-xs text-gray-500">${item.unitPrice.toFixed(2)} x {item.quantity}</p>
                                </div>
                                <div className="font-medium text-white">
                                    ${item.subtotal.toFixed(2)}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                <div className="space-y-8">
                    <div className="bg-[#0f0f0f] border border-gray-800 rounded-lg p-6">
                        <h2 className="text-lg font-medium mb-4">Summary</h2>
                        <div className="flex justify-between items-center text-lg font-medium pt-4 border-t border-gray-800">
                            <span className="text-gray-400">Total</span>
                            <span className="text-white">${order.totalAmount.toFixed(2)}</span>
                        </div>
                    </div>

                    <div className="bg-[#0f0f0f] border border-gray-800 rounded-lg p-6">
                        <h2 className="text-lg font-medium mb-4">Shipping Address</h2>
                        <div className="text-gray-400 text-sm leading-relaxed">
                            {order.shippingAddress ? (
                                <>
                                    <p>{order.shippingAddress.street}</p>
                                    <p>{order.shippingAddress.city}, {order.shippingAddress.state} {order.shippingAddress.pincode}</p>
                                </>
                            ) : (
                                <p>No shipping address provided.</p>
                            )}
                        </div>
                    </div>

                    <div className="bg-[#1a1a1a] border border-gray-800 rounded-lg p-6">
                        <h2 className="text-lg font-medium mb-4">Actions</h2>
                        <div className="space-y-3">
                            {order.status === 'AWAITING_PAYMENT' && (
                                <>
                                    <button
                                        onClick={handleSimulatePayment}
                                        disabled={actionLoading}
                                        className="w-full bg-emerald-600 hover:bg-emerald-500 text-white py-2 rounded-md font-medium transition-colors disabled:opacity-50"
                                    >
                                        {actionLoading ? 'Processing...' : 'Complete Payment'}
                                    </button>
                                </>
                            )}
                            {(order.status === 'PENDING' || order.status === 'AWAITING_PAYMENT') && (
                                <button
                                    onClick={handleCancel}
                                    disabled={actionLoading}
                                    className="w-full border border-red-500/50 text-red-500 hover:bg-red-500/10 py-2 rounded-md font-medium transition-colors disabled:opacity-50 mt-3"
                                >
                                    Cancel Order
                                </button>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}
