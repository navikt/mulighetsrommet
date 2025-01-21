import { useApiQuery } from "@/hooks/useApiQuery";
import { useDebounce } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { VirksomhetService } from "@mr/api-client-v2";

export function useSokBrregVirksomheter(sokestreng: string) {
  const debouncedSok = useDebounce(sokestreng, 300);

  return useApiQuery({
    queryKey: QueryKeys.brregVirksomheter(debouncedSok),
    queryFn: () =>
      VirksomhetService.sokBrregVirksomheter({
        query: { sok: debouncedSok.trim() },
      }),
    enabled: !!debouncedSok,
  });
}
