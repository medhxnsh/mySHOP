import { Link, useNavigate } from 'react-router-dom'
import { useEffect } from 'react'
import NotificationBell from './NotificationBell'
import useAuthStore from '../store/authStore'
import useCartStore from '../store/cartStore'

export default function Navbar() {
    const { user, clearAuth } = useAuthStore()
    const { itemsCount, fetchCartCount } = useCartStore()
    const navigate = useNavigate()

    useEffect(() => {
        if (user && user.role !== 'ADMIN' && user.role !== 'ROLE_ADMIN') {
            fetchCartCount()
        }
    }, [user, fetchCartCount])

    const handleLogout = () => {
        clearAuth()
        navigate('/login')
    }

    return (
        <nav className="border-b border-gray-800 bg-[#0a0a0a] sticky top-0 z-50">
            <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
                <Link to="/" className="text-xl font-semibold tracking-tight text-white hover:text-gray-300 transition-colors">
                    myShop
                </Link>
                <div className="flex items-center gap-6 text-sm font-medium">
                    <Link to="/products" className="text-gray-300 hover:text-white transition-colors">Browse</Link>
                    {(user?.role === 'ROLE_ADMIN' || user?.role === 'ADMIN') && (
                        <>
                            <Link to="/admin/products" className="text-gray-400 hover:text-white transition-colors">Products Admin</Link>
                            <Link to="/admin/kafka" className="text-gray-400 hover:text-white transition-colors">Kafka</Link>
                        </>
                    )}
                    <div className="w-px h-4 bg-gray-800 hidden sm:block"></div>
                    <Link to="/cart" className="text-gray-300 hover:text-white transition-colors relative">
                        Cart
                        {itemsCount > 0 && (
                            <span className="absolute -top-2 -right-3 bg-blue-600 text-white text-[10px] px-1.5 py-0.5 rounded-full min-w-[16px] text-center">
                                {itemsCount}
                            </span>
                        )}
                    </Link>
                    <NotificationBell />
                    {user ? (
                        <div className="flex items-center gap-4">
                            <span className="text-gray-300 text-sm">
                                {user.firstName ? `Hi, ${user.firstName}` : user.email}
                            </span>
                            <Link to="/orders" className="text-gray-300 hover:text-white transition-colors text-sm">Orders</Link>
                            <button
                                onClick={handleLogout}
                                className="text-sm font-medium text-red-400 hover:text-red-300 transition-colors"
                            >
                                Logout
                            </button>
                        </div>
                    ) : (
                        <>
                            <Link to="/login" className="text-gray-300 hover:text-white transition-colors">Login</Link>
                            <Link to="/register" className="bg-white text-black px-4 py-1.5 rounded-md hover:bg-gray-200 transition-colors">
                                Register
                            </Link>
                        </>
                    )}
                </div>
            </div>
        </nav>
    )
}
