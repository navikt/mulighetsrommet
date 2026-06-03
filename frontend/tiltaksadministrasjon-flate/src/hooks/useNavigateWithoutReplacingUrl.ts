import { useNavigate } from "react-router";

export function useNavigateAndReplaceUrl() {
  const navigate = useNavigate();

  function navigateAndReplaceUrl(to: string) {
    navigate(to, { replace: true });
  }

  return { navigateAndReplaceUrl };
}
