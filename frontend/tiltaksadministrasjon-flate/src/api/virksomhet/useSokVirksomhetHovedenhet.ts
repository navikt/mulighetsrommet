import { useApiQuery, useDebounce } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { VirksomhetService } from "@tiltaksadministrasjon/api-client";

export function useSokVirksomhetHovedenhet(sok: string) {
  const debouncedSok = useDebounce(sok, 300);

  return useApiQuery({
    queryKey: QueryKeys.virksomhetHovedenhet(debouncedSok),
    queryFn: () =>
      VirksomhetService.sokHovedenheter({
        query: { sok: debouncedSok.trim() },
      }),
    enabled: !!debouncedSok,
  });
}
