import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import axios from 'axios'
import ProductReviews from '../components/ProductReviews'

export default function ProductDetail() {
    const { id } = useParams()
    const [product, setProduct] = useState(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        axios.get(`http://localhost:8080/api/v1/products/${id}`)
            .then(res => {
                setProduct(res.data.data)
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
                <div className="w-full md:w-1/2 aspect-square bg-[#0f0f0f] border border-gray-800 rounded-lg flex items-center justify-center">
                    <span className="text-gray-600 font-mono text-sm tracking-widest uppercase">{product.categoryName || 'Image'}</span>
                </div>

                {/* Product Info */}
                <div className="w-full md:w-1/2 flex flex-col pt-4">
                    <div className="text-sm text-gray-500 uppercase tracking-widest mb-2">
                        {product.categoryName || 'Uncategorized'}
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

                    <div className="mt-auto relative group">
                        <button
                            disabled
                            className="w-full bg-white text-black font-medium py-4 rounded-md disabled:bg-gray-800 disabled:text-gray-500 disabled:cursor-not-allowed transition-colors"
                        >
                            Add to Cart
                        </button>
                        {/* Tooltip */}
                        <div className="absolute opacity-0 group-hover:opacity-100 transition-opacity bottom-full left-1/2 -translate-x-1/2 mb-2 px-3 py-1 bg-gray-800 text-xs text-white rounded whitespace-nowrap pointer-events-none">
                            Coming in Phase 2
                        </div>
                    </div>
                </div>
            </div>

            {/* PHASE 3 REVIEWS */}
            <ProductReviews productId={product.id} />
        </div>
    )
}
