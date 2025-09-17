import { useApiQuery } from "@mr/frontend-common";
import { PrismodellService, Tiltakskode } from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "../QueryKeys";

export function useForhandsgodkjenteSatser(tiltakstype: Tiltakskode) {
  return useApiQuery({
    queryFn: () => PrismodellService.getForhandsgodkjenteSatser({ query: { tiltakstype } }),
    queryKey: QueryKeys.avtalteSatser(tiltakstype),
  });
}
