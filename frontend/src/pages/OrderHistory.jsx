import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'
import toast from 'react-hot-toast'
import useAuthStore from '../store/authStore'

export default function OrderHistory() {
    const [orders, setOrders] = useState([])
    const [loading, setLoading] = useState(true)
    const { user } = useAuthStore()

    const fetchOrders = async () => {
        try {
            const res = await axios.get('/api/v1/orders?size=50')
            setOrders(res.data.data.content || [])
        } catch (err) {
            toast.error('Failed to load order history')
        } finally {
            setLoading(false)
        }
    }

    // Initial fetch
    useEffect(() => {
        if (user) fetchOrders()
    }, [user])

    // Poll for status updates every 10 seconds
    useEffect(() => {
        if (!user) return
        const interval = setInterval(() => {
            fetchOrders()
        }, 10000)
        return () => clearInterval(interval)
    }, [user])

    const getStatusColor = (status) => {
        switch (status) {
            case 'PENDING': return 'bg-yellow-500/10 text-yellow-500 border-yellow-500/20'
            case 'PROCESSING': return 'bg-blue-500/10 text-blue-500 border-blue-500/20'
            case 'SHIPPED': return 'bg-purple-500/10 text-purple-500 border-purple-500/20'
            case 'DELIVERED': return 'bg-emerald-500/10 text-emerald-500 border-emerald-500/20'
            case 'CANCELLED': return 'bg-red-500/10 text-red-500 border-red-500/20'
            default: return 'bg-gray-500/10 text-gray-500 border-gray-500/20'
        }
    }

    if (!user) {
        return <div className="max-w-4xl mx-auto px-6 py-24 text-center">Please log in to view orders.</div>
    }

    return (
        <div className="max-w-6xl mx-auto px-6 py-12">
            <h1 className="text-3xl font-semibold mb-8">Order History</h1>

            <div className="bg-[#0f0f0f] border border-gray-800 rounded-lg overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm text-gray-400">
                        <thead className="bg-[#1a1a1a] text-xs uppercase text-gray-500 border-b border-gray-800">
                            <tr>
                                <th className="px-6 py-4 font-medium">Order ID</th>
                                <th className="px-6 py-4 font-medium">Date</th>
                                <th className="px-6 py-4 font-medium">Status</th>
                                <th className="px-6 py-4 font-medium">Total Amount</th>
                                <th className="px-6 py-4 font-medium text-right">Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading && orders.length === 0 ? (
                                <tr>
                                    <td colSpan="5" className="px-6 py-8 text-center text-gray-500">Loading orders...</td>
                                </tr>
                            ) : orders.length === 0 ? (
                                <tr>
                                    <td colSpan="5" className="px-6 py-8 text-center text-gray-500">
                                        You haven't placed any orders yet. <Link to="/products" className="text-blue-500 hover:text-blue-400 ml-2">Start Shopping</Link>
                                    </td>
                                </tr>
                            ) : (
                                orders.map((order) => (
                                    <tr key={order.id} className="border-b border-gray-800 last:border-0 hover:bg-[#151515] transition-colors">
                                        <td className="px-6 py-4 font-mono text-xs text-white">
                                            {order.id.split('-')[0]}...
                                        </td>
                                        <td className="px-6 py-4">
                                            {new Date(order.createdAt).toLocaleDateString()}
                                        </td>
                                        <td className="px-6 py-4">
                                            <span className={`px-2 py-1 border text-[10px] font-bold uppercase tracking-wider rounded ${getStatusColor(order.status)}`}>
                                                {order.status}
                                            </span>
                                        </td>
                                        <td className="px-6 py-4 font-medium text-white">
                                            ${order.totalAmount.toFixed(2)}
                                        </td>
                                        <td className="px-6 py-4 text-right">
                                            <Link
                                                to={`/orders/${order.id}`}
                                                className="text-blue-500 hover:text-blue-400 font-medium transition-colors"
                                            >
                                                View Details
                                            </Link>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    )
}
