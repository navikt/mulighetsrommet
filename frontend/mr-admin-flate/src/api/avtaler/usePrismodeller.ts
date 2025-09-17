import { useApiSuspenseQuery } from "@mr/frontend-common";
import { PrismodellService, Tiltakskode } from "@tiltaksadministrasjon/api-client";

export function usePrismodeller(tiltakstype: Tiltakskode) {
  return useApiSuspenseQuery({
    queryFn: () => PrismodellService.getPrismodeller({ query: { tiltakstype } }),
    queryKey: ["prismodeller", tiltakstype],
  });
}
