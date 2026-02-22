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
import AdminRoute from './components/AdminRoute'
import useAuthStore from './store/authStore'

// Configure axios to always send cookies (for httpOnly refresh token)
axios.defaults.withCredentials = true;

// Intercept requests to attach auth token
axios.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config;
});

export default function App() {
  const [rateLimit, setRateLimit] = useState(null)

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

  return (
    <Router>
      <div className="min-h-screen flex flex-col bg-[#0a0a0a] text-white font-sans selection:bg-[#2563eb] selection:text-white">
        <Navbar />
        <main className="flex-grow">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/status" element={<Status />} />
            <Route path="/products" element={<Products />} />
            <Route path="/products/:id" element={<ProductDetail />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/cart" element={<Cart />} />
            <Route path="/checkout" element={<Checkout />} />
            <Route path="/order-confirmation/:id" element={<OrderConfirmation />} />
            <Route path="/orders" element={<OrderHistory />} />
            <Route path="/orders/:id" element={<OrderDetail />} />
            <Route path="/admin/cache" element={<AdminRoute><AdminCache /></AdminRoute>} />
            <Route path="/admin/kafka" element={<AdminRoute><AdminKafka /></AdminRoute>} />
            <Route path="/admin/products" element={<AdminRoute><AdminProducts /></AdminRoute>} />
            <Route path="/flash-sale" element={<FlashSale />} />
          </Routes>
        </main>
        <Footer />
        <RateLimitModal rateLimit={rateLimit} onClose={() => setRateLimit(null)} />
      </div>
    </Router>
  )
}
