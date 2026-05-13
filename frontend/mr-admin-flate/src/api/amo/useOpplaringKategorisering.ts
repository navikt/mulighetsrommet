import { QueryKeys } from "@/api/QueryKeys";
import { KodeverkService, Tiltakskode } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useOpplaringKategorisering(tiltakskode: Tiltakskode) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.opplaringKategorisering(tiltakskode),
    queryFn: () => KodeverkService.getOpplaringKategorisering({ query: { tiltakskode } }),
  });
}
