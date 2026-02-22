import { useState } from 'react'
import axios from 'axios'

export default function AdminCache() {
    const [status, setStatus] = useState(null)
    const [loading, setLoading] = useState(false)

    const clearCache = async (cacheName) => {
        setLoading(true)
        setStatus(null)
        try {
            // Need authorization context here ideally, but since we're using cookies and roles, 
            // the HttpOnly cookie and/or Bearer token needs to be an Admin token.
            // For this phase, we just try the request.
            await axios.delete(`/api/v1/admin/cache/${cacheName}`)
            setStatus({ type: 'success', message: `Cache '${cacheName}' cleared successfully!` })
        } catch (error) {
            setStatus({ type: 'error', message: error.response?.data?.error?.message || `Failed to clear '${cacheName}' cache.` })
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="max-w-4xl mx-auto px-6 py-12">
            <h1 className="text-3xl font-semibold mb-8">Admin Cache Management</h1>
            <p className="text-gray-400 mb-8">
                As an administrator, you can manually invalidate Redis caches to ensure data consistency after manual DB updates.
            </p>

            {status && (
                <div className={`mb-8 p-4 rounded-md border ${status.type === 'success' ? 'bg-green-500/10 border-green-500/20 text-green-400' : 'bg-red-500/10 border-red-500/20 text-red-400'
                    }`}>
                    {status.message}
                </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="bg-[#0f0f0f] border border-gray-800 p-6 rounded-lg">
                    <h3 className="text-xl font-medium mb-2">Products Cache</h3>
                    <p className="text-sm text-gray-500 mb-6">Invalidates all cached product lists.</p>
                    <button
                        disabled={loading}
                        onClick={() => clearCache('products_paged')}
                        className="btn-primary text-sm w-full disabled:opacity-50"
                    >
                        Clear 'products_paged'
                    </button>
                </div>

                <div className="bg-[#0f0f0f] border border-gray-800 p-6 rounded-lg">
                    <h3 className="text-xl font-medium mb-2">Category Cache</h3>
                    <p className="text-sm text-gray-500 mb-6">Invalidates cached category lists.</p>
                    <button
                        disabled={loading}
                        onClick={() => clearCache('categories')}
                        className="btn-primary text-sm w-full disabled:opacity-50"
                    >
                        Clear 'categories'
                    </button>
                </div>
            </div>
        </div>
    )
}
