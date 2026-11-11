import { configureStore } from '@reduxjs/toolkit';
import { useDispatch, useSelector, type TypedUseSelectorHook } from 'react-redux';
import { api } from './api';
import authReducer, { persistAuth } from './authSlice';
import cartReducer from './cartSlice';
import uiReducer from './uiSlice';

export const store = configureStore({
  reducer: {
    [api.reducerPath]: api.reducer,
    auth: authReducer,
    cart: cartReducer,
    ui: uiReducer,
  },
  middleware: (getDefault) => getDefault().concat(api.middleware),
});

// Keep the session across page reloads.
let lastToken = store.getState().auth.token;
store.subscribe(() => {
  const auth = store.getState().auth;
  if (auth.token !== lastToken) {
    lastToken = auth.token;
    persistAuth(auth);
    // Never show one user's cached bookings or orders to the next person who signs in.
    store.dispatch(api.util.resetApiState());
  }
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
export const useAppDispatch: () => AppDispatch = useDispatch;
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
