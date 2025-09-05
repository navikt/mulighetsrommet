import { useApiQuery, useDebounce } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { AnsattService } from "@tiltaksadministrasjon/api-client";

export function useSokNavAnsatt(q: string, id: string) {
  const debouncedSok = useDebounce(q, 300);

  return useApiQuery({
    queryKey: QueryKeys.sokNavansatt(q, id),
    queryFn: () => AnsattService.sokAnsatte({ query: { q: debouncedSok.trim() } }),
    enabled: !!debouncedSok,
  });
}
