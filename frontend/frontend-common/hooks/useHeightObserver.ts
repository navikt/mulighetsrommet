import {RefObject, useEffect, useState} from "react";

export function useHeightObserver<T extends Element>(ref: RefObject<T | null>) {
  const [height, setHeight] = useState(0);

  useEffect(() => {
    if (!ref.current) return;

    const handleResize = (entry: ResizeObserverEntry) => {
      setHeight(entry.contentRect.height);
    };

    const resizeObserver = new ResizeObserver((entries) => {
      handleResize(entries[0]);
    });

    resizeObserver.observe(ref.current);

    return () => {
      resizeObserver.disconnect();
    };
  }, []);

  return height;
}
