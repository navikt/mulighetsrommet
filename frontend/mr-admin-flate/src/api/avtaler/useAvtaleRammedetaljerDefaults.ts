import { QueryKeys } from "@/api/QueryKeys";
import { AvtaleService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useAvtaleRammedetaljerDefaults(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.avtaleRammedetaljerDefaults(id),
    queryFn: async () => AvtaleService.hentRammedetaljerDefaults({ path: { id } }),
  });
}
