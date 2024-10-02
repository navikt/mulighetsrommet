import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { AnsattService, NavAnsattRolle } from "@mr/api-client";
import { useDebounce } from "@mr/frontend-common";

export function useSokNavAnsatt(q: string) {
  const debouncedSok = useDebounce(q, 300);

  return useQuery({
    queryKey: QueryKeys.navansatt(NavAnsattRolle.KONTAKTPERSON),
    queryFn: () =>
      AnsattService.sokAnsatt({ q: debouncedSok.trim() }),
    enabled: !!debouncedSok,
  });
}
