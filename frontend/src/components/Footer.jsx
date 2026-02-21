import { Link } from 'react-router-dom'

export default function Footer() {
    return (
        <footer className="border-t border-gray-800 py-8 mt-auto bg-[#0a0a0a]">
            <div className="max-w-6xl mx-auto px-6 flex items-center justify-between text-xs text-gray-500">
                <div>myShop © 2026</div>
                <Link to="/status" className="hover:text-gray-300 transition-colors">Developer Dashboard</Link>
            </div>
        </footer>
    )
}
