import './index.css'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { useState, useEffect } from 'react'
import axios from 'axios'
import Navbar from './components/Navbar'
import Footer from './components/Footer'
import Home from './pages/Home'
import Status from './pages/Status'
import Products from './pages/Products'
import ProductDetail from './pages/ProductDetail'
import AdminCache from './pages/AdminCache'
import AdminKafka from './pages/AdminKafka'
import AdminProducts from './pages/admin/AdminProducts'
import Cart from './pages/Cart'
import OrderConfirmation from './pages/OrderConfirmation'
import OrderHistory from './pages/OrderHistory'
import OrderDetail from './pages/OrderDetail'
import FlashSale from './pages/FlashSale'
import Login from './pages/Login'
import Register from './pages/Register'
import Checkout from './pages/Checkout'
import RateLimitModal from './components/RateLimitModal'
import ProtectedRoute from './components/ProtectedRoute'
import useAuthStore from './store/authStore'

// Configure axios to always send cookies (for httpOnly refresh token)
axios.defaults.withCredentials = true;

// Intercept requests to attach auth token from in-memory store
axios.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config;
});

// 401 interceptor: attempt silent refresh once, then redirect to login
axios.interceptors.response.use(
  response => response,
  async error => {
    const original = error.config;
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true;
      try {
        const res = await axios.post('/api/v1/auth/refresh');
        const { accessToken, user } = res.data.data;
        useAuthStore.getState().setAuth(accessToken, user);
        original.headers.Authorization = `Bearer ${accessToken}`;
        return axios(original);
      } catch {
        useAuthStore.getState().clearAuth();
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default function App() {
  const [rateLimit, setRateLimit] = useState(null)
  const [authLoading, setAuthLoading] = useState(true)
  const { token, setAuth, clearAuth } = useAuthStore()

  // Silent refresh on mount — restore session from httpOnly refresh cookie
  useEffect(() => {
    const tryRefresh = async () => {
      try {
        const res = await axios.post('/api/v1/auth/refresh');
        setAuth(res.data.data.accessToken, res.data.data.user);
      } catch {
        clearAuth();
      } finally {
        setAuthLoading(false);
      }
    };
    if (!token) {
      tryRefresh();
    } else {
      setAuthLoading(false);
    }
  }, []);

  // Rate limit interceptor
  useEffect(() => {
    const interceptor = axios.interceptors.response.use(
      response => response,
      error => {
        if (error.response && error.response.status === 429) {
          const retryAfter = error.response.headers['retry-after'] || 60;
          setRateLimit({
            retryAfter: parseInt(retryAfter, 10),
            message: error.response.data?.error?.message || "Rate limit exceeded."
          });
        }
        return Promise.reject(error);
      }
    );
    return () => axios.interceptors.response.eject(interceptor);
  }, []);

  // Show nothing until silent refresh completes to avoid flashing login state
  if (authLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#0a0a0a]">
        <div className="w-6 h-6 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
      </div>
    )
  }

  return (
    <Router>
      <div className="min-h-screen flex flex-col bg-[#0a0a0a] text-white font-sans selection:bg-[#2563eb] selection:text-white">
        <Navbar />
        <main className="flex-grow">
          <Routes>
            {/* Public routes */}
            <Route path="/" element={<Home />} />
            <Route path="/status" element={<Status />} />
            <Route path="/products" element={<Products />} />
            <Route path="/products/:id" element={<ProductDetail />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/flash-sale" element={<FlashSale />} />

            {/* Authenticated routes */}
            <Route path="/cart" element={<ProtectedRoute><Cart /></ProtectedRoute>} />
            <Route path="/checkout" element={<ProtectedRoute><Checkout /></ProtectedRoute>} />
            <Route path="/order-confirmation/:id" element={<ProtectedRoute><OrderConfirmation /></ProtectedRoute>} />
            <Route path="/orders" element={<ProtectedRoute><OrderHistory /></ProtectedRoute>} />
            <Route path="/orders/:id" element={<ProtectedRoute><OrderDetail /></ProtectedRoute>} />

            {/* Admin routes */}
            <Route path="/admin/cache" element={<ProtectedRoute requireAdmin={true}><AdminCache /></ProtectedRoute>} />
            <Route path="/admin/kafka" element={<ProtectedRoute requireAdmin={true}><AdminKafka /></ProtectedRoute>} />
            <Route path="/admin/products" element={<ProtectedRoute requireAdmin={true}><AdminProducts /></ProtectedRoute>} />
          </Routes>
        </main>
        <Footer />
        <RateLimitModal rateLimit={rateLimit} onClose={() => setRateLimit(null)} />
      </div>
    </Router>
  )
}
