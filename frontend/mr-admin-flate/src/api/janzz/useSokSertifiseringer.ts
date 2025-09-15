import { useApiQuery, useDebounce } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { JanzzService } from "@tiltaksadministrasjon/api-client";

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
