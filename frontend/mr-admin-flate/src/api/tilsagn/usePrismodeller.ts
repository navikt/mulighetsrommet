import { useApiSuspenseQuery } from "@mr/frontend-common";
import { PrismodellService, Tiltakskode } from "@mr/api-client-v2";

export function usePrismodeller(tiltakstype: Tiltakskode) {
  return useApiSuspenseQuery({
    queryFn: () => PrismodellService.getPrismodeller({ query: { tiltakstype } }),
    queryKey: ["prismodeller", tiltakstype],
  });
}
