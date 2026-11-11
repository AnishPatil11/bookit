import { createSlice, type PayloadAction } from '@reduxjs/toolkit';

export interface CartLine {
  menuItemId: number;
  name: string;
  price: number;
  quantity: number;
}

interface CartState {
  restaurantId: number | null;
  restaurantName: string;
  lines: CartLine[];
}

const initialState: CartState = { restaurantId: null, restaurantName: '', lines: [] };

const cartSlice = createSlice({
  name: 'cart',
  initialState,
  reducers: {
    addItem(
      state,
      action: PayloadAction<{ restaurantId: number; restaurantName: string; item: Omit<CartLine, 'quantity'> }>,
    ) {
      const { restaurantId, restaurantName, item } = action.payload;
      // A cart belongs to one restaurant; switching restaurants starts a fresh cart.
      if (state.restaurantId !== restaurantId) {
        state.restaurantId = restaurantId;
        state.restaurantName = restaurantName;
        state.lines = [];
      }
      const existing = state.lines.find((l) => l.menuItemId === item.menuItemId);
      if (existing) existing.quantity = Math.min(existing.quantity + 1, 50);
      else state.lines.push({ ...item, quantity: 1 });
    },
    setQuantity(state, action: PayloadAction<{ menuItemId: number; quantity: number }>) {
      const { menuItemId, quantity } = action.payload;
      if (quantity <= 0) state.lines = state.lines.filter((l) => l.menuItemId !== menuItemId);
      else {
        const line = state.lines.find((l) => l.menuItemId === menuItemId);
        if (line) line.quantity = Math.min(quantity, 50);
      }
      if (state.lines.length === 0) state.restaurantId = null;
    },
    clearCart() {
      return initialState;
    },
  },
});

export const { addItem, setQuantity, clearCart } = cartSlice.actions;
export const cartTotal = (lines: CartLine[]): number => lines.reduce((sum, l) => sum + l.price * l.quantity, 0);
export default cartSlice.reducer;
