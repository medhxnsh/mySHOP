import { create } from 'zustand'

const useAuthStore = create((set) => ({
    token: null,
    user: null,
    setAuth: (token, user) => {
        set({ token, user })
    },
    clearAuth: () => {
        set({ token: null, user: null })
    },
}))

export default useAuthStore
