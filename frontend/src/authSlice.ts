import { createSlice, type PayloadAction } from '@reduxjs/toolkit';
import type { AuthResponse, User } from './types';

interface AuthState {
  token: string | null;
  user: User | null;
}

const STORAGE_KEY = 'bookit.auth';

function load(): AuthState {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return JSON.parse(raw) as AuthState;
  } catch {
    /* storage blocked or corrupted: start signed out */
  }
  return { token: null, user: null };
}

export function persistAuth(state: AuthState): void {
  try {
    if (state.token) localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    else localStorage.removeItem(STORAGE_KEY);
  } catch {
    /* ignore */
  }
}

const authSlice = createSlice({
  name: 'auth',
  initialState: load(),
  reducers: {
    setCredentials(state, action: PayloadAction<AuthResponse>) {
      state.token = action.payload.token;
      state.user = action.payload.user;
    },
    setUser(state, action: PayloadAction<User>) {
      state.user = action.payload;
    },
    logout(state) {
      state.token = null;
      state.user = null;
    },
  },
});

export const { setCredentials, setUser, logout } = authSlice.actions;
export default authSlice.reducer;
