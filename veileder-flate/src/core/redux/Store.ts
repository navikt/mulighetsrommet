import { configureStore as store } from '@reduxjs/toolkit';
import toggleReducer from './slice/ToggleSlice';
import filterReducer from './slice/FiltreringSlice';

export default store({
  reducer: {
    toggleReducer,
    filterReducer,
  },
});
