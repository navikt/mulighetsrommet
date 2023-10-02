import { useLocation } from "react-router-dom";

export function useGetQueryParam(key: string): string | null {
  const search = useLocation().search;
  return new URLSearchParams(search).get(key);
}
