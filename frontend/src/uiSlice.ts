import { createSlice, type PayloadAction } from '@reduxjs/toolkit';

export interface Toast {
  id: number;
  kind: 'success' | 'error';
  text: string;
}

let nextId = 1;

const uiSlice = createSlice({
  name: 'ui',
  initialState: { toast: null as Toast | null },
  reducers: {
    notify: {
      reducer(state, action: PayloadAction<Toast>) {
        state.toast = action.payload;
      },
      prepare(kind: Toast['kind'], text: string) {
        return { payload: { id: nextId++, kind, text } };
      },
    },
    dismiss(state) {
      state.toast = null;
    },
  },
});

export const { notify, dismiss } = uiSlice.actions;
export default uiSlice.reducer;
