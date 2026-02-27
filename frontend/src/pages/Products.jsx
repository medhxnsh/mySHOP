import { useState, useEffect } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import axios from 'axios'

export default function Products() {
    const [searchParams, setSearchParams] = useSearchParams()
    const [products, setProducts] = useState([])
    const [loading, setLoading] = useState(true)
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(1)
    const [sort, setSort] = useState('createdAt')
    const [sortDir, setSortDir] = useState('desc')
    const [categories, setCategories] = useState([])
    const [selectedCategoryId, setSelectedCategoryId] = useState(null)
    const [categoryCounts, setCategoryCounts] = useState({})

    // Read categoryId from URL on mount
    useEffect(() => {
        const urlCatId = searchParams.get('categoryId')
        if (urlCatId) setSelectedCategoryId(urlCatId)
    }, [])

    // Fetch categories from API
    useEffect(() => {
        axios.get('/api/v1/categories')
            .then(res => setCategories(res.data.data))
            .catch(err => console.error('Failed to fetch categories', err))
    }, [])

    // Fetch product counts per category (one call with large size to count)
    useEffect(() => {
        axios.get('/api/v1/products?page=0&size=200')
            .then(res => {
                const counts = {}
                for (const p of res.data.data.content) {
                    const catName = p.categoryName
                    if (catName) counts[catName] = (counts[catName] || 0) + 1
                }
                setCategoryCounts(counts)
            })
            .catch(() => { })
    }, [])

    // Fetch filtered products
    useEffect(() => {
        setLoading(true)
        let url = `/api/v1/products?page=${page}&size=12&sortBy=${sort}&sortDir=${sortDir}`
        if (selectedCategoryId) url += `&categoryId=${selectedCategoryId}`
        axios.get(url)
            .then(res => {
                setProducts(res.data.data.content)
                setTotalPages(res.data.data.totalPages)
                setLoading(false)
            })
            .catch(err => {
                console.error('Failed to fetch products', err)
                setLoading(false)
            })
    }, [page, sort, sortDir, selectedCategoryId])

    const handleCategoryClick = (catId) => {
        setSelectedCategoryId(catId)
        setPage(0)
        if (catId) {
            setSearchParams({ categoryId: catId })
        } else {
            setSearchParams({})
        }
    }

    // Only show categories that have products
    const visibleCategories = categories.filter(cat => (categoryCounts[cat.name] || 0) > 0)

    return (
        <div className="max-w-6xl mx-auto px-6 py-12 flex flex-col md:flex-row gap-12">
            {/* Sidebar Filters */}
            <aside className="w-full md:w-64 shrink-0">
                <div className="mb-8">
                    <h3 className="text-sm font-medium text-gray-400 mb-4 uppercase tracking-wider">Categories</h3>
                    <ul className="space-y-3">
                        <li>
                            <button
                                onClick={() => handleCategoryClick(null)}
                                className={`text-sm transition-colors ${!selectedCategoryId
                                        ? 'text-[#2563eb] font-semibold'
                                        : 'text-gray-400 hover:text-white'
                                    }`}
                            >
                                All Categories
                            </button>
                        </li>
                        {visibleCategories.map(cat => (
                            <li key={cat.id}>
                                <button
                                    onClick={() => handleCategoryClick(cat.id)}
                                    className={`text-sm transition-colors flex items-center gap-2 ${selectedCategoryId === cat.id
                                            ? 'text-[#2563eb] font-semibold'
                                            : 'text-gray-400 hover:text-white'
                                        }`}
                                >
                                    {cat.name}
                                    <span className="text-xs text-gray-600">({categoryCounts[cat.name] || 0})</span>
                                </button>
                            </li>
                        ))}
                    </ul>
                </div>

                <div>
                    <h3 className="text-sm font-medium text-gray-400 mb-4 uppercase tracking-wider">Sort By</h3>
                    <select
                        value={`${sort}-${sortDir}`}
                        onChange={(e) => {
                            const [newSort, newDir] = e.target.value.split('-')
                            setSort(newSort)
                            setSortDir(newDir)
                        }}
                        className="w-full bg-[#0f0f0f] border border-gray-800 text-sm rounded-md px-3 py-2 text-gray-300 focus:outline-none focus:border-gray-600"
                    >
                        <option value="createdAt-desc">Newest Arrivals</option>
                        <option value="price-asc">Price: Low to High</option>
                        <option value="price-desc">Price: High to Low</option>
                    </select>
                </div>
            </aside>

            {/* Product Grid */}
            <main className="flex-1">
                {loading ? (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                        {[...Array(6)].map((_, i) => (
                            <div key={i} className="aspect-square bg-gray-900 rounded-lg animate-pulse" />
                        ))}
                    </div>
                ) : products.length === 0 ? (
                    <div className="text-center py-20 text-gray-500">
                        <p className="text-lg mb-2">No products found</p>
                        <p className="text-sm">Try selecting a different category</p>
                    </div>
                ) : (
                    <>
                        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 mb-12">
                            {products.map(product => (
                                <Link key={product.id} to={`/products/${product.id}`} className="group block">
                                    <div className="aspect-square bg-white border border-gray-800 rounded-lg mb-3 overflow-hidden relative flex items-center justify-center p-4">
                                        <img
                                            src={product.imageUrl || `https://placehold.co/400x400?text=${encodeURIComponent(product.name)}`}
                                            alt={product.name}
                                            className="max-w-full max-h-full object-contain"
                                            onError={(e) => {
                                                e.target.onerror = null;
                                                e.target.src = `https://placehold.co/400x400?text=${encodeURIComponent(product.name)}`;
                                            }}
                                        />
                                    </div>
                                    <h3 className="text-sm font-medium text-gray-200 group-hover:text-[#2563eb] transition-colors truncate">
                                        {product.name}
                                    </h3>
                                    <div className="flex items-center justify-between mt-1">
                                        <span className="text-sm text-gray-400">${product.price.toFixed(2)}</span>
                                        <span className="text-xs text-gray-600">{product.categoryName}</span>
                                    </div>
                                </Link>
                            ))}
                        </div>

                        {/* Pagination Controls */}
                        {totalPages > 1 && (
                            <div className="flex items-center justify-center gap-4">
                                <button
                                    onClick={() => setPage(p => Math.max(0, p - 1))}
                                    disabled={page === 0}
                                    className="btn-secondary text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    Previous
                                </button>
                                <span className="text-sm text-gray-500">Page {page + 1} of {totalPages}</span>
                                <button
                                    onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                                    disabled={page === totalPages - 1}
                                    className="btn-secondary text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    Next
                                </button>
                            </div>
                        )}
                    </>
                )}
            </main>
        </div>
    )
}
