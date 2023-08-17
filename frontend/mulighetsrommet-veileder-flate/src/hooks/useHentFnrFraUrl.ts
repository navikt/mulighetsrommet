import React, { useContext } from 'react';

export const FnrContext = React.createContext<string>('12345678910');

export function useHentFnrFraUrl() {
  return useContext(FnrContext);
}
