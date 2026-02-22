import { useState, useEffect } from 'react'
import { Star, ThumbsUp, CheckCircle } from 'lucide-react'
import axios from 'axios'
import useAuthStore from '../store/authStore'

export default function ProductReviews({ productId }) {
    const [reviews, setReviews] = useState([])
    const [loading, setLoading] = useState(true)
    const [eligible, setEligible] = useState(false)
    const [token] = useState(() => localStorage.getItem('token'))

    // Form state
    const [formVisible, setFormVisible] = useState(false)
    const [rating, setRating] = useState(0)
    const [hoverRating, setHoverRating] = useState(0)
    const [title, setTitle] = useState('')
    const [comment, setComment] = useState('')
    const [submitLoading, setSubmitLoading] = useState(false)
    const [error, setError] = useState(null)

    useEffect(() => {
        if (!productId) return;
        fetchReviews()
        if (token) checkEligibility()
    }, [productId, token])

    const fetchReviews = async () => {
        try {
            const res = await axios.get(`/api/v1/products/${productId}/reviews?size=20`)
            if (res.data?.success) {
                setReviews(res.data.data.content || [])
            }
        } catch (err) {
            console.error('Failed to fetch reviews', err)
        } finally {
            setLoading(false)
        }
    }

    const checkEligibility = async () => {
        try {
            const res = await axios.get(`/api/v1/products/${productId}/reviews/eligibility`, {
                headers: { Authorization: `Bearer ${token}` }
            })
            if (res.data?.success) {
                setEligible(res.data.data)
            }
        } catch (err) {
            console.error('Failed to check review eligibility', err)
        }
    }

    const submitReview = async (e) => {
        e.preventDefault()
        if (rating === 0) {
            setError('Please select a rating')
            return
        }
        setSubmitLoading(true)
        setError(null)
        try {
            await axios.post(`/api/v1/products/${productId}/reviews`, {
                rating, title, comment
            }, {
                headers: { Authorization: `Bearer ${token}` }
            })
            // Reset and refresh
            setFormVisible(false)
            setEligible(false) // cannot review twice
            setRating(0)
            setTitle('')
            setComment('')
            fetchReviews()
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to submit review')
        } finally {
            setSubmitLoading(false)
        }
    }

    const markHelpful = async (reviewId) => {
        if (!token) return alert('Please login to mark as helpful')
        try {
            await axios.put(`/api/v1/reviews/${reviewId}/helpful`, {}, {
                headers: { Authorization: `Bearer ${token}` }
            })
            fetchReviews() // naive refresh
        } catch (err) {
            console.error('Failed to mark helpful', err)
        }
    }

    if (loading) return <div className="py-8 animate-pulse text-gray-500">Loading reviews...</div>

    return (
        <div className="mt-16 pt-12 border-t border-gray-800">
            <div className="flex justify-between items-center mb-8">
                <h2 className="text-2xl font-semibold text-white">Customer Reviews</h2>
                {eligible && !formVisible && (
                    <button
                        onClick={() => setFormVisible(true)}
                        className="btn-primary px-4 py-2 text-sm"
                    >
                        Write a Review
                    </button>
                )}
            </div>

            {formVisible && (
                <div className="bg-[#121212] p-6 rounded-lg border border-gray-800 mb-8">
                    <h3 className="text-lg font-medium text-white mb-4">Write your review</h3>
                    {error && <div className="text-red-400 text-sm mb-4">{error}</div>}
                    <form onSubmit={submitReview} className="space-y-4">
                        <div>
                            <label className="block text-sm text-gray-400 mb-2">Overall Rating *</label>
                            <div className="flex gap-1" onMouseLeave={() => setHoverRating(0)}>
                                {[1, 2, 3, 4, 5].map(star => (
                                    <button
                                        key={star}
                                        type="button"
                                        onClick={() => setRating(star)}
                                        onMouseEnter={() => setHoverRating(star)}
                                        className="focus:outline-none transition-transform hover:scale-110"
                                    >
                                        <Star
                                            className={`${(hoverRating || rating) >= star ? 'fill-yellow-400 text-yellow-400' : 'text-gray-600'} transition-all duration-200`}
                                            size={28}
                                        />
                                    </button>
                                ))}
                            </div>
                        </div>
                        <div>
                            <label className="block text-sm text-gray-400 mb-1.5">Review Title *</label>
                            <input
                                required
                                value={title}
                                onChange={e => setTitle(e.target.value)}
                                className="w-full bg-[#0a0a0a] border border-gray-800 rounded px-3 py-2 text-white focus:border-gray-500 outline-none"
                                placeholder="Summarize your experience"
                            />
                        </div>
                        <div>
                            <label className="block text-sm text-gray-400 mb-1.5">Review Comment *</label>
                            <textarea
                                required
                                rows={4}
                                value={comment}
                                onChange={e => setComment(e.target.value)}
                                className="w-full bg-[#0a0a0a] border border-gray-800 rounded px-3 py-2 text-white focus:border-gray-500 outline-none resize-none"
                                placeholder="What did you like or dislike? What did you use this product for?"
                            />
                        </div>
                        <div className="flex gap-3 pt-2">
                            <button
                                type="button"
                                onClick={() => setFormVisible(false)}
                                className="px-4 py-2 rounded text-sm font-medium text-gray-400 border border-gray-800 hover:bg-gray-800 transition-colors"
                            >
                                Cancel
                            </button>
                            <button
                                type="submit"
                                disabled={submitLoading}
                                className="btn-primary px-6 py-2 text-sm disabled:opacity-50"
                            >
                                {submitLoading ? 'Submitting...' : 'Submit Review'}
                            </button>
                        </div>
                    </form>
                </div>
            )}

            {reviews.length === 0 ? (
                <div className="text-gray-500 py-8 text-center bg-[#0f0f0f] rounded-lg border border-gray-800/50">
                    No reviews yet. Be the first to share your thoughts!
                </div>
            ) : (
                <div className="space-y-6">
                    {reviews.map(review => (
                        <div key={review.id} className="border-b border-gray-800 pb-6 last:border-0">
                            <div className="flex items-start justify-between mb-2">
                                <div>
                                    <div className="flex items-center gap-3 mb-1">
                                        <span className="font-medium text-gray-200">{review.userName || 'Anonymous'}</span>
                                        {review.verifiedPurchase && (
                                            <span className="flex items-center gap-1 text-[10px] uppercase font-semibold text-emerald-400 bg-emerald-400/10 px-2 py-0.5 rounded-full border border-emerald-400/20">
                                                <CheckCircle size={10} /> Verified Purchase
                                            </span>
                                        )}
                                    </div>
                                    <div className="flex gap-0.5 mb-2">
                                        {[1, 2, 3, 4, 5].map(star => (
                                            <Star
                                                key={star}
                                                size={14}
                                                className={review.rating >= star ? 'fill-yellow-400 text-yellow-400' : 'text-gray-700'}
                                            />
                                        ))}
                                    </div>
                                    <h4 className="font-semibold text-white mb-2">{review.title}</h4>
                                </div>
                                <span className="text-xs text-gray-500">
                                    {new Date(review.createdAt).toLocaleDateString()}
                                </span>
                            </div>
                            <p className="text-gray-400 text-sm leading-relaxed mb-4">
                                {review.comment}
                            </p>
                            <button
                                onClick={() => markHelpful(review.id)}
                                className="flex items-center gap-1.5 text-xs font-medium text-gray-400 hover:text-white transition-colors group"
                            >
                                <ThumbsUp size={14} className="group-hover:fill-gray-700 transition-colors" />
                                Helpful ({review.helpfulVotes})
                            </button>
                        </div>
                    ))}
                </div>
            )}
        </div>
    )
}
