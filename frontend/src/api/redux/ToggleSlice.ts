import { createSlice } from '@reduxjs/toolkit';

export interface ToggleState {
  bildeListeVisning: boolean;
}

const initialState: ToggleState = { bildeListeVisning: true };

export const toggle = createSlice({
  name: 'toggle',
  initialState,
  reducers: {
    toggleBildeListeVisning: state => {
      state.bildeListeVisning = !state.bildeListeVisning;
    },
  },
});

export const { toggleBildeListeVisning } = toggle.actions;

export default toggle.reducer;
