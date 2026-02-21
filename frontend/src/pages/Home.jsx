import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { Monitor, Shirt, Book, Home, Activity } from 'lucide-react'
import axios from 'axios'

const categories = [
    { name: 'Electronics', icon: <Monitor size={20} /> },
    { name: 'Clothing', icon: <Shirt size={20} /> },
    { name: 'Books', icon: <Book size={20} /> },
    { name: 'Home', icon: <Home size={20} /> },
    { name: 'Sports', icon: <Activity size={20} /> }
]

export default function HomePage() {
    const [products, setProducts] = useState([])
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        // Fetch featured products
        axios.get('http://localhost:8080/api/v1/products?page=0&size=8')
            .then(res => {
                setProducts(res.data.data.content)
                setLoading(false)
            })
            .catch(err => {
                console.error('Failed to fetch products', err)
                setLoading(false)
            })
    }, [])

    return (
        <div className="min-h-screen flex flex-col">
            {/* Hero Section */}
            <section className="bg-[#0a0a0a] py-32 px-6 border-b border-gray-800">
                <div className="max-w-6xl mx-auto text-center">
                    <h1 className="text-5xl md:text-7xl font-semibold mb-6 tracking-tight text-white">
                        Shop without compromise.
                    </h1>
                    <p className="text-xl text-gray-400 max-w-2xl mx-auto mb-10">
                        Premium products, fast delivery, zero friction.
                    </p>
                    <Link to="/products" className="bg-white text-black font-medium px-8 py-3 rounded-md hover:bg-gray-200 transition-colors inline-flex items-center justify-center">
                        Browse Products
                    </Link>
                </div>
            </section>

            {/* Categories Row */}
            <section className="py-16 px-6 max-w-6xl mx-auto w-full border-b border-gray-800">
                <h2 className="text-xl font-medium mb-8">Featured Categories</h2>
                <div className="flex overflow-x-auto gap-4 pb-4 snap-x">
                    {categories.map((cat) => (
                        <div key={cat.name} className="flex-none snap-start bg-[#0f0f0f] border border-gray-800 rounded-lg p-6 w-48 flex flex-col items-center justify-center gap-3 hover:border-gray-600 transition-colors cursor-pointer">
                            <div className="text-gray-400">{cat.icon}</div>
                            <div className="font-medium text-sm text-gray-200">{cat.name}</div>
                        </div>
                    ))}
                </div>
            </section>

            {/* Featured Products Grid */}
            <section className="py-16 px-6 max-w-6xl mx-auto w-full">
                <div className="flex items-center justify-between mb-8">
                    <h2 className="text-xl font-medium">Featured Products</h2>
                    <Link to="/products" className="text-sm text-[#2563eb] hover:text-blue-400 font-medium">View all →</Link>
                </div>

                {loading ? (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                        {[...Array(8)].map((_, i) => (
                            <div key={i} className="aspect-square bg-gray-900 rounded-lg animate-pulse" />
                        ))}
                    </div>
                ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                        {products.map(product => (
                            <Link key={product.id} to={`/products/${product.id}`} className="group block">
                                <div className="aspect-square bg-[#0f0f0f] border border-gray-800 rounded-lg mb-3 overflow-hidden relative">
                                    <div className="absolute inset-0 flex items-center justify-center text-gray-600">
                                        <span className="text-xs font-mono uppercase tracking-widest">{product.categoryName || 'Product'}</span>
                                    </div>
                                </div>
                                <h3 className="text-sm font-medium text-gray-200 group-hover:text-[#2563eb] transition-colors truncate">
                                    {product.name}
                                </h3>
                                <div className="text-sm text-gray-400 mt-1">
                                    ${product.price.toFixed(2)}
                                </div>
                            </Link>
                        ))}
                    </div>
                )}
            </section>
        </div>
    )
}
