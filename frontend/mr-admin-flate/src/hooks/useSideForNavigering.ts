import { useLocation } from "react-router-dom";
import { useEffect, useState } from "react";

export function useSideForNavigering() {
  const [side, setSide] = useState("/mine");

  const location = useLocation();

  useEffect(() => {
    const sidenavn = "/" + location.pathname.split("/")[1];
    // @ts-ignore
    return setSide(sidenavn);
  }, [location.pathname]);

  return side;
}
