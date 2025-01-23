import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { AnsattService } from "@mr/api-client-v2";
import { useDebounce } from "@mr/frontend-common";

export function useSokNavAnsatt(q: string, id: string) {
  const debouncedSok = useDebounce(q, 300);

  return useApiQuery({
    queryKey: QueryKeys.sokNavansatt(q, id),
    queryFn: () => AnsattService.sokAnsatt({ query: { q: debouncedSok.trim() } }),
    enabled: !!debouncedSok,
  });
}
