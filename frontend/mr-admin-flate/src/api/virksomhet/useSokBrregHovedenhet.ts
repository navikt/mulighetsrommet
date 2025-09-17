import { useApiQuery, useDebounce } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { BrregService } from "@tiltaksadministrasjon/api-client";

export function useSokBrregHovedenhet(sokestreng: string) {
  const debouncedSok = useDebounce(sokestreng, 300);

  return useApiQuery({
    queryKey: QueryKeys.brregVirksomheter(debouncedSok),
    queryFn: () =>
      BrregService.sokBrregHovedenhet({
        query: { q: debouncedSok.trim() },
      }),
    enabled: !!debouncedSok,
  });
}
