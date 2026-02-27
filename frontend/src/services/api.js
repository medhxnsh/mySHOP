import axios from 'axios';
import useAuthStore from '../store/authStore';

// Axios instance that auto-attaches Bearer token from Zustand auth store
const api = axios.create({
    baseURL: '/api/v1',
});

api.interceptors.request.use((config) => {
    const token = useAuthStore.getState().token;
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export default api;
