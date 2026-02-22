import { useState, useEffect } from 'react'
import axios from 'axios'
import toast from 'react-hot-toast'
import useAuthStore from '../../store/authStore'

export default function AdminProducts() {
    const [products, setProducts] = useState([])
    const [loading, setLoading] = useState(true)
    const { user } = useAuthStore()

    // Form state
    const [isEditing, setIsEditing] = useState(false)
    const [currentId, setCurrentId] = useState(null)
    const [formData, setFormData] = useState({
        name: '',
        description: '',
        price: '',
        stockQuantity: '',
        categoryId: ''
    })

    const fetchProducts = async () => {
        try {
            const res = await axios.get('/api/v1/products?size=100')
            setProducts(res.data.data.content || [])
        } catch (err) {
            toast.error('Failed to load products')
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchProducts()
    }, [])

    const handleInputChange = (e) => {
        const { name, value } = e.target
        setFormData(prev => ({
            ...prev,
            [name]: name === 'price' ? parseFloat(value) : (name === 'stockQuantity' ? parseInt(value, 10) : value)
        }))
    }

    const resetForm = () => {
        setIsEditing(false)
        setCurrentId(null)
        setFormData({ name: '', description: '', price: '', stockQuantity: '', categoryId: '' })
    }

    const handleEditClick = (product) => {
        setIsEditing(true)
        setCurrentId(product.id)
        setFormData({
            name: product.name,
            description: product.description || '',
            price: product.price,
            stockQuantity: product.stockQuantity,
            categoryId: product.categoryId || ''
        })
        window.scrollTo({ top: 0, behavior: 'smooth' })
    }

    const handleDelete = async (id) => {
        if (!window.confirm('Are you sure you want to delete this product?')) return
        try {
            await axios.delete(`/api/v1/products/${id}`)
            toast.success('Product deleted successfully')
            fetchProducts()
        } catch (err) {
            toast.error(err.response?.data?.error?.message || 'Failed to delete product')
        }
    }

    const handleSubmit = async (e) => {
        e.preventDefault()
        try {
            const payload = {
                ...formData,
                categoryId: formData.categoryId === '' ? null : formData.categoryId
            }
            if (isEditing) {
                await axios.put(`/api/v1/products/${currentId}`, payload)
                toast.success('Product updated successfully')
            } else {
                await axios.post('/api/v1/products', payload)
                toast.success('Product created successfully')
            }
            resetForm()
            fetchProducts()
        } catch (err) {
            toast.error(err.response?.data?.error?.message || 'Failed to save product')
        }
    }

    if (!user || (user.role !== 'ROLE_ADMIN' && user.role !== 'ADMIN')) {
        return (
            <div className="max-w-4xl mx-auto px-6 py-24 text-center">
                <h2 className="text-2xl font-medium mb-4">Unauthorized</h2>
                <p className="text-gray-400">You need admin privileges to view this page.</p>
            </div>
        )
    }

    return (
        <div className="max-w-6xl mx-auto px-6 py-12">
            <h1 className="text-3xl font-semibold mb-8">Product Management</h1>

            {/* Form Section */}
            <div className="bg-[#0f0f0f] border border-gray-800 rounded-lg p-6 mb-12">
                <h2 className="text-xl font-medium mb-4">{isEditing ? 'Edit Product' : 'Create New Product'}</h2>
                <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-400 mb-1.5">Product Name</label>
                            <input
                                type="text"
                                name="name"
                                required
                                value={formData.name}
                                onChange={handleInputChange}
                                className="w-full bg-[#1a1a1a] border border-gray-700 rounded-md px-3 py-2 text-white focus:outline-none focus:border-gray-500"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-400 mb-1.5">Category ID (Optional)</label>
                            <input
                                type="text"
                                name="categoryId"
                                value={formData.categoryId}
                                onChange={handleInputChange}
                                className="w-full bg-[#1a1a1a] border border-gray-700 rounded-md px-3 py-2 text-white focus:outline-none focus:border-gray-500"
                                placeholder="UUID"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-400 mb-1.5">Price ($)</label>
                            <input
                                type="number"
                                name="price"
                                step="0.01"
                                required
                                value={formData.price}
                                onChange={handleInputChange}
                                className="w-full bg-[#1a1a1a] border border-gray-700 rounded-md px-3 py-2 text-white focus:outline-none focus:border-gray-500"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-400 mb-1.5">Stock Quantity</label>
                            <input
                                type="number"
                                name="stockQuantity"
                                required
                                value={formData.stockQuantity}
                                onChange={handleInputChange}
                                className="w-full bg-[#1a1a1a] border border-gray-700 rounded-md px-3 py-2 text-white focus:outline-none focus:border-gray-500"
                            />
                        </div>
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-400 mb-1.5">Description</label>
                        <textarea
                            name="description"
                            rows="3"
                            value={formData.description}
                            onChange={handleInputChange}
                            className="w-full bg-[#1a1a1a] border border-gray-700 rounded-md px-3 py-2 text-white focus:outline-none focus:border-gray-500"
                        />
                    </div>
                    <div className="flex gap-4 pt-4">
                        <button type="submit" className="bg-blue-600 hover:bg-blue-500 text-white px-6 py-2 rounded-md font-medium transition-colors">
                            {isEditing ? 'Update Product' : 'Create Product'}
                        </button>
                        {isEditing && (
                            <button type="button" onClick={resetForm} className="bg-gray-800 hover:bg-gray-700 text-white px-6 py-2 rounded-md font-medium transition-colors">
                                Cancel
                            </button>
                        )}
                    </div>
                </form>
            </div>

            {/* List Section */}
            <div className="bg-[#0f0f0f] border border-gray-800 rounded-lg overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm text-gray-400">
                        <thead className="bg-[#1a1a1a] text-xs uppercase text-gray-500 border-b border-gray-800">
                            <tr>
                                <th className="px-6 py-4 font-medium">Name</th>
                                <th className="px-6 py-4 font-medium">Price</th>
                                <th className="px-6 py-4 font-medium">Stock</th>
                                <th className="px-6 py-4 font-medium text-right">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr>
                                    <td colSpan="4" className="px-6 py-8 text-center text-gray-500">Loading products...</td>
                                </tr>
                            ) : products.length === 0 ? (
                                <tr>
                                    <td colSpan="4" className="px-6 py-8 text-center text-gray-500">No products found.</td>
                                </tr>
                            ) : (
                                products.map((product) => (
                                    <tr key={product.id} className="border-b border-gray-800 hover:bg-[#151515] transition-colors">
                                        <td className="px-6 py-4 font-medium text-white">{product.name}</td>
                                        <td className="px-6 py-4">${product.price.toFixed(2)}</td>
                                        <td className="px-6 py-4">
                                            <span className={`px-2 py-1 rounded text-xs ${product.stockQuantity > 0 ? 'bg-emerald-500/10 text-emerald-500' : 'bg-red-500/10 text-red-500'}`}>
                                                {product.stockQuantity}
                                            </span>
                                        </td>
                                        <td className="px-6 py-4 text-right">
                                            <button onClick={() => handleEditClick(product)} className="text-blue-500 hover:text-blue-400 mr-4 font-medium">Edit</button>
                                            <button onClick={() => handleDelete(product.id)} className="text-red-500 hover:text-red-400 font-medium">Delete</button>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    )
}
