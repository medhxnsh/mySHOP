import { useState, useEffect } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import axios from 'axios'
import toast from 'react-hot-toast'
import ProductReviews from '../components/ProductReviews'
import useAuthStore from '../store/authStore'
import useCartStore from '../store/cartStore'

export default function ProductDetail() {
    const { id } = useParams()
    const navigate = useNavigate()
    const { user } = useAuthStore()
    const { setPendingAction, incrementCount } = useCartStore()
    const [product, setProduct] = useState(null)
    const [loading, setLoading] = useState(true)
    const [cacheStatus, setCacheStatus] = useState(null)

    useEffect(() => {
        axios.get(`/api/v1/products/${id}`)
            .then(res => {
                setProduct(res.data.data)
                setCacheStatus(res.headers['x-cache'] || 'MISS')
                setLoading(false)
            })
            .catch(err => {
                console.error('Failed to fetch product', err)
                setLoading(false)
            })
    }, [id])

    if (loading) {
        return (
            <div className="max-w-4xl mx-auto px-6 py-12 flex gap-12 animate-pulse">
                <div className="w-1/2 aspect-square bg-gray-900 rounded-lg"></div>
                <div className="w-1/2 space-y-4 pt-8">
                    <div className="h-8 bg-gray-900 rounded w-3/4"></div>
                    <div className="h-6 bg-gray-900 rounded w-1/4"></div>
                    <div className="h-24 bg-gray-900 rounded w-full mt-8"></div>
                </div>
            </div>
        )
    }

    if (!product) {
        return (
            <div className="max-w-4xl mx-auto px-6 py-24 text-center">
                <h2 className="text-2xl font-medium mb-4">Product Not Found</h2>
                <Link to="/products" className="text-[#2563eb] hover:underline">Return to products</Link>
            </div>
        )
    }

    return (
        <div className="max-w-6xl mx-auto px-6 py-12">
            <div className="flex flex-col md:flex-row gap-12">
                {/* Product Image Placeholder */}
                <div className="w-full md:w-1/2 aspect-square bg-[#0f0f0f] border border-gray-800 rounded-lg overflow-hidden flex items-center justify-center">
                    <img
                        src={`https://placehold.co/600x400/2563eb/ffffff?text=${encodeURIComponent(product.name)}`}
                        alt={product.name}
                        className="w-full h-full object-cover opacity-80"
                    />
                </div>

                {/* Product Info */}
                <div className="w-full md:w-1/2 flex flex-col pt-4">
                    <div className="flex items-center gap-4 mb-2">
                        <div className="text-sm text-gray-500 uppercase tracking-widest">
                            {product.categoryName || 'Uncategorized'}
                        </div>
                        {cacheStatus && (
                            <span className={`text-[10px] uppercase tracking-wider px-2 py-0.5 rounded font-bold ${cacheStatus === 'HIT' ? 'bg-green-500/20 text-green-400' : 'bg-orange-500/20 text-orange-400'
                                }`} title="Dev Mode: Cache Status">
                                Cache {cacheStatus}
                            </span>
                        )}
                    </div>
                    <h1 className="text-3xl font-semibold mb-4 text-white">
                        {product.name}
                    </h1>

                    {/* Display Phase 3 Avg Rating overview if reviews exist */}
                    {product.reviewCount > 0 && (
                        <div className="flex items-center gap-2 mb-4">
                            <span className="text-yellow-400 font-medium">{product.avgRating.toFixed(1)}</span>
                            <span className="text-sm text-gray-500">({product.reviewCount} reviews)</span>
                        </div>
                    )}

                    <div className="text-2xl font-medium text-gray-300 mb-8">
                        ${product.price.toFixed(2)}
                    </div>

                    <p className="text-gray-400 mb-8 leading-relaxed">
                        {product.description || 'No description provided.'}
                    </p>

                    <div className="mb-8">
                        <span className={`text-sm font-medium px-3 py-1 rounded-full ${product.stockQuantity > 0
                            ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                            : 'bg-red-500/10 text-red-400 border border-red-500/20'
                            }`}>
                            {product.stockQuantity > 0 ? `In Stock (${product.stockQuantity})` : 'Out of Stock'}
                        </span>
                    </div>

                    <div className="mt-auto">
                        <button
                            onClick={async () => {
                                if (!user) {
                                    setPendingAction({ type: 'ADD_TO_CART', payload: { productId: id, quantity: 1 } })
                                    navigate(`/login?redirect=/products/${id}`)
                                    return
                                }
                                try {
                                    await axios.post('/api/v1/cart/items', { productId: id, quantity: 1 })
                                    toast.success('Added to cart!')
                                    incrementCount(1)
                                } catch (err) {
                                    toast.error('Failed to add to cart')
                                }
                            }}
                            className="w-full bg-[#2563eb] text-white hover:bg-blue-600 font-medium py-4 rounded-md transition-colors"
                        >
                            Add to Cart
                        </button>
                    </div>
                </div>
            </div>

            {/* PHASE 3 REVIEWS */}
            <ProductReviews productId={product.id} />
        </div>
    )
}
