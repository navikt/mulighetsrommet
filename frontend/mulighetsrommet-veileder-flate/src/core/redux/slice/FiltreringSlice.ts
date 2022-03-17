import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface FilterState {
  sokefelt: string;
}

const initialState: FilterState = {
  sokefelt: '',
};

const Filtrering = createSlice({
  name: 'filtrering',
  initialState,
  reducers: {
    skrivSokefelt: (state, action: PayloadAction<string>) => {
      state.sokefelt = action.payload;
    },
  },
});

export const { skrivSokefelt } = Filtrering.actions;

export default Filtrering.reducer;
