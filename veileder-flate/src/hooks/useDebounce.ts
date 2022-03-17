import { useEffect } from 'react';
import useTimeout from './useTimeout';

export default function useDebounce(
  callback: () => { payload: string; type: string },
  delay: number,
  dependencies: string[]
) {
  const { reset, clear } = useTimeout(callback, delay);
  useEffect(reset, [...dependencies, reset]);
  useEffect(clear, [clear]);
}
