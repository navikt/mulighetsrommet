import { DependencyList, useEffect, useRef } from 'react';

export default function useUpdateEffect(callback: any, dependencies: DependencyList | undefined) {
  const firstRenderRef = useRef(true);

  useEffect(() => {
    if (firstRenderRef.current) {
      firstRenderRef.current = false;
      return;
    }
    return callback();
  }, dependencies);
}
