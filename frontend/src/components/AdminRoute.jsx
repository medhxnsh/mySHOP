import { Navigate } from 'react-router-dom';
import useAuthStore from '../store/authStore';
import toast from 'react-hot-toast';

export default function AdminRoute({ children }) {
    const { user } = useAuthStore();

    if (!user) {
        return <Navigate to="/login" replace />;
    }

    if (user.role !== 'ADMIN' && user.role !== 'ROLE_ADMIN') {
        toast.error('Unauthorized: Admin access required');
        return <Navigate to="/" replace />;
    }

    return children;
}
