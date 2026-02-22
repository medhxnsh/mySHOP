import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useState } from 'react'
import axios from 'axios'
import toast from 'react-hot-toast'
import useAuthStore from '../store/authStore'
import useCartStore from '../store/cartStore'

export default function Register() {
    const [fullName, setFullName] = useState('')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [loading, setLoading] = useState(false)
    const navigate = useNavigate()
    const [searchParams] = useSearchParams()
    const { setAuth } = useAuthStore()
    const { pendingAction, clearPendingAction, incrementCount } = useCartStore()

    const handleSubmit = async (e) => {
        e.preventDefault()
        setLoading(true)
        try {
            // 1. Register
            await axios.post('/api/v1/auth/register', {
                fullName: fullName.trim(),
                email,
                password
            })

            // 2. Auto-login
            const loginRes = await axios.post('/api/v1/auth/login', { email, password })
            const { accessToken, user } = loginRes.data.data
            setAuth(accessToken, user)

            let alertMsg = 'Account created and logged in!'
            if (pendingAction?.type === 'ADD_TO_CART') {
                try {
                    await axios.post('/api/v1/cart/items', pendingAction.payload)
                    alertMsg = 'Account created and item added to cart!'
                    incrementCount(pendingAction.payload.quantity)
                } catch (e) {
                    toast.error('Failed to add pending item to cart')
                }
                clearPendingAction()
            }

            toast.success(alertMsg)
            const redirect = searchParams.get('redirect')
            navigate(redirect || '/')
        } catch (err) {
            toast.error(err.response?.data?.error?.message || 'Failed to register')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="min-h-[70vh] flex items-center justify-center px-6">
            <div className="w-full max-w-sm">
                <div className="mb-8 text-center">
                    <h1 className="text-2xl font-semibold mb-2">Create an account</h1>
                    <p className="text-gray-500 text-sm">Join myShop to start exploring premium products</p>
                </div>

                <form className="space-y-4" onSubmit={handleSubmit}>
                    <div>
                        <label className="block text-sm font-medium text-gray-400 mb-1.5">Full Name</label>
                        <input
                            type="text"
                            required
                            value={fullName}
                            onChange={(e) => setFullName(e.target.value)}
                            className="w-full bg-[#0f0f0f] border border-gray-800 rounded-md px-3 py-2 text-white focus:outline-none focus:border-gray-500"
                            placeholder="John Doe"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-400 mb-1.5">Email address</label>
                        <input
                            type="email"
                            required
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            className="w-full bg-[#0f0f0f] border border-gray-800 rounded-md px-3 py-2 text-white focus:outline-none focus:border-gray-500"
                            placeholder="name@example.com"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-400 mb-1.5">Password</label>
                        <input
                            type="password"
                            required
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            className="w-full bg-[#0f0f0f] border border-gray-800 rounded-md px-3 py-2 text-white focus:outline-none focus:border-gray-500"
                            placeholder="••••••••"
                        />
                    </div>
                    <button type="submit" disabled={loading} className="w-full btn-primary mt-6 disabled:opacity-50">
                        {loading ? 'Creating Account...' : 'Create Account'}
                    </button>
                </form>

                <p className="mt-6 text-center text-sm text-gray-500">
                    Already have an account? <Link to="/login" className="text-white hover:underline">Sign in</Link>
                </p>
            </div>
        </div>
    )
}
