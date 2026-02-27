import { Navigate } from 'react-router-dom'
import useAuthStore from '../store/authStore'

export default function ProtectedRoute({ children, requireAdmin = false }) {
    const { token, user } = useAuthStore()

    if (!token) return <Navigate to="/login" replace />
    if (requireAdmin && user?.role !== 'ADMIN' && user?.role !== 'ROLE_ADMIN') {
        return <Navigate to="/" replace />
    }

    return children
}
