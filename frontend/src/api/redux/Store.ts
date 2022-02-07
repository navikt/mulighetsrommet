import { configureStore as store } from '@reduxjs/toolkit';
import toggleReducer from './ToggleSlice';
import filterReducer from './FiltreringSlice';

export default store({
  reducer: {
    toggleReducer,
    filterReducer,
  },
});
