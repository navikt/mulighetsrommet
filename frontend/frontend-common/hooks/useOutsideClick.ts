import React from "react";

export function useOutsideClick(callback: () => void) {
  const ref = React.useRef<HTMLElement>(null);

  React.useEffect(() => {
    const handleClick = (event: MouseEvent) => {
      if (ref.current && !event.composedPath().includes(ref.current)) {
        callback();
      }
    };

    document.addEventListener("click", handleClick, true);

    return () => {
      document.removeEventListener("click", handleClick, true);
    };
  }, [ref, callback]);

  return ref;
}
