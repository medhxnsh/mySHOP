import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import axios from 'axios'
import toast from 'react-hot-toast'
import useAuthStore from '../store/authStore'
import useCartStore from '../store/cartStore'

export default function Cart() {
    const [cart, setCart] = useState(null)
    const [loading, setLoading] = useState(true)
    const { user } = useAuthStore()
    const { fetchCartCount } = useCartStore()
    const navigate = useNavigate()

    const fetchCart = async () => {
        try {
            const res = await axios.get('/api/v1/cart')
            setCart(res.data.data)
            fetchCartCount()
        } catch (err) {
            toast.error('Failed to fetch cart')
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        if (user) {
            fetchCart()
        } else {
            navigate('/login?redirect=/cart')
        }
    }, [user, navigate])

    const handleUpdateQuantity = async (productId, currentQty, change) => {
        const newQty = currentQty + change
        if (newQty < 1) return

        try {
            await axios.put(`/api/v1/cart/items/${productId}?quantity=${newQty}`)
            fetchCart()
        } catch (err) {
            toast.error(err.response?.data?.error?.message || 'Failed to update quantity')
        }
    }

    const handleRemoveItem = async (productId) => {
        try {
            await axios.delete(`/api/v1/cart/items/${productId}`)
            toast.success('Item removed')
            fetchCart()
        } catch (err) {
            toast.error('Failed to remove item')
        }
    }

    const handleClearCart = async () => {
        if (!window.confirm('Are you sure you want to clear your cart?')) return
        try {
            await axios.delete('/api/v1/cart')
            toast.success('Cart cleared')
            fetchCart()
        } catch (err) {
            toast.error('Failed to clear cart')
        }
    }

    if (loading) {
        return <div className="max-w-4xl mx-auto px-6 py-24 text-center text-gray-400">Loading cart...</div>
    }

    if (!cart || !cart.items || cart.items.length === 0) {
        return (
            <div className="max-w-4xl mx-auto px-6 py-24 text-center">
                <h2 className="text-2xl font-medium mb-4">Your cart is empty</h2>
                <Link to="/products" className="text-blue-500 hover:text-blue-400">Browse Products</Link>
            </div>
        )
    }

    return (
        <div className="max-w-4xl mx-auto px-6 py-12">
            <div className="flex justify-between items-end mb-8">
                <h1 className="text-3xl font-semibold">Shopping Cart</h1>
                <button
                    onClick={handleClearCart}
                    className="text-sm text-red-500 hover:text-red-400 transition-colors"
                >
                    Clear Cart
                </button>
            </div>

            <div className="bg-[#0f0f0f] border border-gray-800 rounded-lg overflow-hidden mb-8">
                <table className="w-full text-left text-sm text-gray-400">
                    <thead className="bg-[#1a1a1a] text-xs uppercase text-gray-500 border-b border-gray-800">
                        <tr>
                            <th className="px-6 py-4 font-medium">Product</th>
                            <th className="px-6 py-4 font-medium">Price</th>
                            <th className="px-6 py-4 font-medium text-center">Quantity</th>
                            <th className="px-6 py-4 font-medium text-right">Subtotal</th>
                            <th className="px-6 py-4 font-medium"></th>
                        </tr>
                    </thead>
                    <tbody>
                        {cart.items.map((item) => (
                            <tr key={item.productId} className="border-b border-gray-800 last:border-0 hover:bg-[#151515] transition-colors">
                                <td className="px-6 py-4">
                                    <Link to={`/products/${item.productId}`} className="font-medium text-white hover:text-blue-400 transition-colors">
                                        {item.productName}
                                    </Link>
                                </td>
                                <td className="px-6 py-4">${item.unitPrice.toFixed(2)}</td>
                                <td className="px-6 py-4">
                                    <div className="flex items-center justify-center gap-3">
                                        <button
                                            onClick={() => handleUpdateQuantity(item.productId, item.quantity, -1)}
                                            className="w-8 h-8 flex items-center justify-center bg-gray-800 hover:bg-gray-700 text-white rounded transition-colors"
                                            disabled={item.quantity <= 1}
                                        >
                                            -
                                        </button>
                                        <span className="w-8 text-center text-white">{item.quantity}</span>
                                        <button
                                            onClick={() => handleUpdateQuantity(item.productId, item.quantity, 1)}
                                            className="w-8 h-8 flex items-center justify-center bg-gray-800 hover:bg-gray-700 text-white rounded transition-colors"
                                        >
                                            +
                                        </button>
                                    </div>
                                </td>
                                <td className="px-6 py-4 text-right text-white font-medium">
                                    ${item.subtotal.toFixed(2)}
                                </td>
                                <td className="px-6 py-4 text-right">
                                    <button
                                        onClick={() => handleRemoveItem(item.productId)}
                                        className="text-gray-500 hover:text-red-500 transition-colors"
                                    >
                                        Remove
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            <div className="flex flex-col items-end gap-6">
                <div className="text-2xl font-medium">
                    <span className="text-gray-400 text-lg mr-4">Total:</span>
                    ${cart.totalAmount.toFixed(2)}
                </div>
                <Link
                    to="/checkout"
                    className="bg-blue-600 hover:bg-blue-500 text-white px-8 py-3 rounded-md font-medium transition-colors"
                >
                    Proceed to Checkout
                </Link>
            </div>
        </div>
    )
}
