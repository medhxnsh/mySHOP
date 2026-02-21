import { Link } from 'react-router-dom'
import NotificationBell from './NotificationBell'

export default function Navbar() {
    return (
        <nav className="border-b border-gray-800 bg-[#0a0a0a] sticky top-0 z-50">
            <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
                <Link to="/" className="text-xl font-semibold tracking-tight text-white hover:text-gray-300 transition-colors">
                    myShop
                </Link>
                <div className="flex items-center gap-6 text-sm font-medium">
                    <Link to="/products" className="text-gray-300 hover:text-white transition-colors">Browse</Link>
                    <div className="w-px h-4 bg-gray-800 hidden sm:block"></div>
                    <NotificationBell />
                    <Link to="/login" className="text-gray-300 hover:text-white transition-colors">Login</Link>
                    <Link to="/register" className="bg-white text-black px-4 py-1.5 rounded-md hover:bg-gray-200 transition-colors">
                        Register
                    </Link>
                </div>
            </div>
        </nav>
    )
}
