import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'

export default function Products() {
    const [products, setProducts] = useState([])
    const [loading, setLoading] = useState(true)
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(1)
    const [sort, setSort] = useState('createdAt')
    const [sortDir, setSortDir] = useState('desc')
    const [categories] = useState(['Electronics', 'Clothing', 'Books', 'Home', 'Sports'])

    useEffect(() => {
        setLoading(true)
        axios.get(`http://localhost:8080/api/v1/products?page=${page}&size=12&sortBy=${sort}&sortDir=${sortDir}`)
            .then(res => {
                setProducts(res.data.data.content)
                setTotalPages(res.data.data.totalPages)
                setLoading(false)
            })
            .catch(err => {
                console.error('Failed to fetch products', err)
                setLoading(false)
            })
    }, [page, sort, sortDir])

    return (
        <div className="max-w-6xl mx-auto px-6 py-12 flex flex-col md:flex-row gap-12">
            {/* Sidebar Filters */}
            <aside className="w-full md:w-64 shrink-0">
                <div className="mb-8">
                    <h3 className="text-sm font-medium text-gray-400 mb-4 uppercase tracking-wider">Categories</h3>
                    <ul className="space-y-3">
                        <li><button className="text-gray-200 hover:text-white transition-colors text-sm">All Categories</button></li>
                        {categories.map(cat => (
                            <li key={cat}><button className="text-gray-500 hover:text-white transition-colors text-sm">{cat}</button></li>
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
                ) : (
                    <>
                        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 mb-12">
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
