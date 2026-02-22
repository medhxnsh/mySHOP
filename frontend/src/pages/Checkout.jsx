import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import axios from 'axios'
import toast from 'react-hot-toast'
import useAuthStore from '../store/authStore'
import useCartStore from '../store/cartStore'

export default function Checkout() {
    const { user } = useAuthStore()
    const { fetchCartCount } = useCartStore()
    const navigate = useNavigate()

    const [cart, setCart] = useState(null)
    const [loading, setLoading] = useState(true)
    const [placingOrder, setPlacingOrder] = useState(false)

    const [address, setAddress] = useState({
        street: '',
        city: '',
        state: '',
        pincode: ''
    })

    // Payment form state
    const [paymentMethod, setPaymentMethod] = useState('COD')
    const [cardDetails, setCardDetails] = useState({
        number: '',
        expiry: '',
        cvv: ''
    })

    useEffect(() => {
        if (!user) {
            navigate('/login?redirect=/checkout')
            return
        }

        const fetchCart = async () => {
            try {
                const res = await axios.get('/api/v1/cart')
                setCart(res.data.data)

                // If cart is empty, send back to cart page
                if (!res.data.data?.items?.length) {
                    toast.error('Your cart is empty')
                    navigate('/cart')
                }
            } catch (err) {
                toast.error('Failed to load cart for checkout')
                navigate('/cart')
            } finally {
                setLoading(false)
            }
        }

        fetchCart()
    }, [user, navigate])

    const handleInputChange = (e) => {
        const { name, value } = e.target
        setAddress(prev => ({ ...prev, [name]: value }))
    }

    const handleCardChange = (e) => {
        const { name, value } = e.target
        setCardDetails(prev => ({ ...prev, [name]: value }))
    }

    const isFormValid = () => {
        if (!address.street || !address.city || !address.state || !address.pincode) return false
        if (paymentMethod === 'CARD') {
            if (cardDetails.number.length < 15 || cardDetails.expiry.length < 5 || cardDetails.cvv.length < 3) return false
        }
        return true
    }

    const handlePlaceOrder = async (e) => {
        e.preventDefault()
        setPlacingOrder(true)

        try {
            const payload = {
                shippingAddress: {
                    street: address.street,
                    city: address.city,
                    state: address.state,
                    pincode: address.pincode
                },
                paymentMethod: paymentMethod
            }
            // 1. Create the order
            const res = await axios.post('/api/v1/orders', payload)
            const orderId = res.data.data.id

            // 2. If Card, immediately try to resolve payment
            if (paymentMethod === 'CARD') {
                try {
                    await axios.post(`/api/v1/orders/${orderId}/pay`)
                    toast.success('Payment successful! Order placed.')
                } catch (payErr) {
                    toast.error('Payment failed. Order saved as AWAITING PAYMENT.')
                    fetchCartCount() // Cart still empties
                    navigate(`/order-confirmation/${orderId}`)
                    return
                }
            } else {
                toast.success('Order placed successfully (COD)!')
            }

            fetchCartCount() // Cart is now empty
            navigate(`/order-confirmation/${orderId}`)
        } catch (err) {
            toast.error(err.response?.data?.error?.message || 'Failed to place order')
        } finally {
            setPlacingOrder(false)
        }
    }

    if (!user || loading) {
        return <div className="max-w-4xl mx-auto px-6 py-24 text-center text-gray-400">Loading checkout...</div>
    }

    if (!cart) return null

    return (
        <div className="max-w-6xl mx-auto px-6 py-12">
            <h1 className="text-3xl font-semibold mb-8">Checkout</h1>

            <div className="flex flex-col lg:flex-row gap-12">
                {/* Left col: Shipping Address */}
                <div className="w-full lg:w-2/3">
                    <div className="bg-[#0f0f0f] border border-gray-800 rounded-lg p-6 mb-8">
                        <h2 className="text-xl font-medium mb-6">Shipping Address</h2>
                        <form id="checkout-form" onSubmit={handlePlaceOrder} className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-400 mb-1.5">Street Address</label>
                                <input
                                    type="text"
                                    name="street"
                                    required
                                    value={address.street}
                                    onChange={handleInputChange}
                                    className="w-full bg-[#1a1a1a] border border-gray-700 rounded-md px-3 py-2 text-white focus:outline-none focus:border-gray-500"
                                    placeholder="123 Main St"
                                />
                            </div>
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-400 mb-1.5">City</label>
                                    <input
                                        type="text"
                                        name="city"
                                        required
                                        value={address.city}
                                        onChange={handleInputChange}
                                        className="w-full bg-[#1a1a1a] border border-gray-700 rounded-md px-3 py-2 text-white focus:outline-none focus:border-gray-500"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-400 mb-1.5">State</label>
                                    <input
                                        type="text"
                                        name="state"
                                        required
                                        value={address.state}
                                        onChange={handleInputChange}
                                        className="w-full bg-[#1a1a1a] border border-gray-700 rounded-md px-3 py-2 text-white focus:outline-none focus:border-gray-500"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-400 mb-1.5">ZIP / Pincode</label>
                                    <input
                                        type="text"
                                        name="pincode"
                                        required
                                        value={address.pincode}
                                        onChange={handleInputChange}
                                        className="w-full bg-[#1a1a1a] border border-gray-700 rounded-md px-3 py-2 text-white focus:outline-none focus:border-gray-500"
                                    />
                                </div>
                            </div>
                        </form>
                    </div>

                    <div className="bg-[#0f0f0f] border border-gray-800 rounded-lg p-6 mb-8">
                        <h2 className="text-xl font-medium mb-6">Payment Method</h2>

                        <div className="space-y-4">
                            <label className={`block border p-4 rounded-lg cursor-pointer transition-colors ${paymentMethod === 'COD' ? 'border-blue-500 bg-blue-500/10' : 'border-gray-800 bg-[#1a1a1a] hover:border-gray-600'}`}>
                                <div className="flex items-center">
                                    <input
                                        type="radio"
                                        name="paymentMethod"
                                        value="COD"
                                        checked={paymentMethod === 'COD'}
                                        onChange={() => setPaymentMethod('COD')}
                                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300"
                                    />
                                    <span className="ml-3 block text-sm font-medium text-white">Cash on Delivery (COD)</span>
                                </div>
                                <p className="ml-7 mt-1 text-sm text-gray-400">Pay when you receive the order.</p>
                            </label>

                            <label className={`block border p-4 rounded-lg cursor-pointer transition-colors ${paymentMethod === 'CARD' ? 'border-blue-500 bg-blue-500/10' : 'border-gray-800 bg-[#1a1a1a] hover:border-gray-600'}`}>
                                <div className="flex items-center">
                                    <input
                                        type="radio"
                                        name="paymentMethod"
                                        value="CARD"
                                        checked={paymentMethod === 'CARD'}
                                        onChange={() => setPaymentMethod('CARD')}
                                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300"
                                    />
                                    <span className="ml-3 block text-sm font-medium text-white">Credit / Debit Card (Simulated)</span>
                                </div>
                                <p className="ml-7 mt-1 text-sm text-gray-400">90% success rate simulation.</p>
                            </label>
                        </div>

                        {paymentMethod === 'CARD' && (
                            <div className="mt-6 pt-6 border-t border-gray-800 space-y-4 animate-fadeIn">
                                <div>
                                    <label className="block text-sm font-medium text-gray-400 mb-1.5">Card Number</label>
                                    <input
                                        type="text"
                                        name="number"
                                        value={cardDetails.number}
                                        onChange={handleCardChange}
                                        placeholder="0000 0000 0000 0000"
                                        className="w-full bg-[#1a1a1a] border border-gray-700 rounded-md px-3 py-2 text-white focus:outline-none focus:border-gray-500 font-mono"
                                    />
                                </div>
                                <div className="grid grid-cols-2 gap-4">
                                    <div>
                                        <label className="block text-sm font-medium text-gray-400 mb-1.5">Expiry Date</label>
                                        <input
                                            type="text"
                                            name="expiry"
                                            value={cardDetails.expiry}
                                            onChange={handleCardChange}
                                            placeholder="MM/YY"
                                            className="w-full bg-[#1a1a1a] border border-gray-700 rounded-md px-3 py-2 text-white focus:outline-none focus:border-gray-500 font-mono"
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-400 mb-1.5">CVV</label>
                                        <input
                                            type="text"
                                            name="cvv"
                                            value={cardDetails.cvv}
                                            onChange={handleCardChange}
                                            placeholder="123"
                                            className="w-full bg-[#1a1a1a] border border-gray-700 rounded-md px-3 py-2 text-white focus:outline-none focus:border-gray-500 font-mono"
                                        />
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                </div>

                {/* Right col: Order Summary */}
                <div className="w-full lg:w-1/3">
                    <div className="bg-[#0f0f0f] border border-gray-800 rounded-lg p-6 sticky top-24">
                        <h2 className="text-xl font-medium mb-6">Order Summary</h2>

                        <div className="space-y-4 mb-6 max-h-60 overflow-y-auto pr-2">
                            {cart.items.map(item => (
                                <div key={item.productId} className="flex justify-between text-sm">
                                    <div className="text-gray-300 pr-4">
                                        <span className="text-gray-500 mr-2">{item.quantity}x</span>
                                        {item.productName}
                                    </div>
                                    <div className="text-white font-medium">${item.subtotal.toFixed(2)}</div>
                                </div>
                            ))}
                        </div>

                        <div className="border-t border-gray-800 pt-4 mb-8">
                            <div className="flex justify-between items-center text-lg font-medium">
                                <span className="text-gray-400">Total</span>
                                <span className="text-white">${cart.totalAmount.toFixed(2)}</span>
                            </div>
                        </div>

                        <button
                            type="submit"
                            form="checkout-form"
                            disabled={placingOrder || !isFormValid()}
                            className="w-full bg-blue-600 hover:bg-blue-500 text-white py-3 rounded-md font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {placingOrder ? 'Processing...' : (paymentMethod === 'CARD' ? 'Pay Now' : 'Place Order')}
                        </button>

                        <p className="text-xs text-gray-500 mt-4 text-center">
                            By placing your order, you agree to our Terms of Use and Privacy Policy. Note: Payment happens after order placement.
                        </p>
                    </div>
                </div>
            </div>
        </div>
    )
}
