import React, { useContext } from 'react';

export const FnrContext = React.createContext<string | null>(null);

export function useFnr(): string {
  const fnr = useContext(FnrContext);

  if (fnr == null) {
    throw Error('Missing fnr');
  }

  return fnr
}
