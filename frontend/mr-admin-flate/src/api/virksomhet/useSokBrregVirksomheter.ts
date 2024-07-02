import { useQuery } from "@tanstack/react-query";
import { useDebounce } from "mulighetsrommet-frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { VirksomhetService } from "mulighetsrommet-api-client";

export function useSokBrregVirksomheter(sokestreng: string) {
  const debouncedSok = useDebounce(sokestreng, 300);

  return useQuery({
    queryKey: QueryKeys.brregVirksomheter(debouncedSok),
    queryFn: () =>
      VirksomhetService.sokBrregVirksomheter({
        sok: debouncedSok.trim(),
      }),
    enabled: !!debouncedSok,
  });
}
