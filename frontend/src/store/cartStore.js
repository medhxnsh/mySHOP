import { create } from 'zustand'
import axios from 'axios'

const useCartStore = create((set) => ({
    itemsCount: 0,
    pendingAction: null,

    setItemsCount: (count) => set({ itemsCount: count }),
    incrementCount: (qty = 1) => set((state) => ({ itemsCount: state.itemsCount + qty })),

    fetchCartCount: async () => {
        try {
            const res = await axios.get('/api/v1/cart')
            if (res.data?.data?.items) {
                const count = res.data.data.items.reduce((acc, item) => acc + item.quantity, 0)
                set({ itemsCount: count })
            }
        } catch (e) {
            console.error('Failed to fetch cart', e)
        }
    },

    setPendingAction: (action) => set({ pendingAction: action }),
    clearPendingAction: () => set({ pendingAction: null })
}))

export default useCartStore
