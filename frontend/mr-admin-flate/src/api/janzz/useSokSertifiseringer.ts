import { useApiQuery } from "@mr/frontend-common";
import { useDebounce } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { JanzzService } from "@mr/api-client-v2";

export function useSokSertifiseringer(q: string) {
  const debouncedSok = useDebounce(q, 300);

  return useApiQuery({
    queryKey: QueryKeys.sokSertifiseringer(debouncedSok),
    queryFn: () =>
      JanzzService.sokSertifiseringer({
        query: { q: debouncedSok.trim() },
      }),
    enabled: !!debouncedSok,
  });
}
