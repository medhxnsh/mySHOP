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
        <nav className="bg-black border-b border-white border-opacity-5 sticky top-0 z-50">
            <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
                {/* Logo */}
                <Link to="/" className="text-lg font-semibold tracking-tight text-white hover:text-gray-300 transition-colors duration-300">
                    myShop
                </Link>

                {/* Navigation Items */}
                <div className="flex items-center gap-8 text-sm font-medium">
                    <Link to="/products" className="text-gray-400 hover:text-white transition-colors duration-300">
                        Browse
                    </Link>
                    
                    {(user?.role === 'ROLE_ADMIN' || user?.role === 'ADMIN') && (
                        <>
                            <Link to="/admin/products" className="text-gray-500 hover:text-gray-200 transition-colors duration-300">
                                Products
                            </Link>
                            <Link to="/admin/kafka" className="text-gray-500 hover:text-gray-200 transition-colors duration-300">
                                Kafka
                            </Link>
                        </>
                    )}

                    {/* Divider */}
                    <div className="w-px h-4 bg-white bg-opacity-10 hidden sm:block" />

                    {/* Cart */}
                    <Link to="/cart" className="text-gray-400 hover:text-white transition-colors duration-300 relative flex items-center gap-2">
                        Cart
                        {itemsCount > 0 && (
                            <span className="absolute -top-2 -right-4 bg-apple-blue text-white text-[10px] font-semibold px-2 py-0.5 rounded-full min-w-[20px] text-center">
                                {itemsCount}
                            </span>
                        )}
                    </Link>

                    {/* Notification Bell */}
                    <NotificationBell />

                    {/* Auth Section */}
                    {user ? (
                        <div className="flex items-center gap-6 ml-4 pl-8 border-l border-white border-opacity-10">
                            <span className="text-gray-400 text-sm">
                                {user.fullName || user.email}
                            </span>
                            <Link to="/orders" className="text-gray-400 hover:text-white transition-colors duration-300 text-sm">
                                Orders
                            </Link>
                            <button
                                onClick={handleLogout}
                                className="text-sm font-medium text-gray-400 hover:text-red-400 transition-colors duration-300"
                            >
                                Logout
                            </button>
                        </div>
                    ) : (
                        <div className="flex items-center gap-4 ml-4 pl-8 border-l border-white border-opacity-10">
                            <Link to="/login" className="text-gray-400 hover:text-white transition-colors duration-300">
                                Login
                            </Link>
                            <Link to="/register" className="bg-white text-black px-4 py-2 rounded-lg font-semibold hover:bg-gray-200 transition-colors duration-300 text-sm">
                                Sign up
                            </Link>
                        </div>
                    )}
                </div>
            </div>
        </nav>
    )
}
