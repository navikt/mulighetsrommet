import { useApiQuery } from "@mr/frontend-common";
import { useDebounce } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { BrregService } from "@mr/api-client-v2";

export function useSokBrregHovedenhet(sokestreng: string) {
  const debouncedSok = useDebounce(sokestreng, 300);

  return useApiQuery({
    queryKey: QueryKeys.brregVirksomheter(debouncedSok),
    queryFn: () =>
      BrregService.sokBrregHovedenhet({
        query: { sok: debouncedSok.trim() },
      }),
    enabled: !!debouncedSok,
  });
}
