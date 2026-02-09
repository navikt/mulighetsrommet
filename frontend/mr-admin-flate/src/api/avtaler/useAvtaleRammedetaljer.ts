import { QueryKeys } from "@/api/QueryKeys";
import { AvtaleService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useAvtaleRammedetaljer(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.avtaleRammedetaljer(id),
    queryFn: async () => AvtaleService.hentRammedetaljer({ path: { id } }),
    select: (data) => {
      if (typeof data === "object") {
        return data;
      }
      return null;
    },
  });
}
