import { useApiQuery } from "@/hooks/useApiQuery";
import { PrismodellService, Tiltakskode } from "@mr/api-client-v2";
import { QueryKeys } from "../QueryKeys";

export function useForhandsgodkjenteSatser(tiltakstype: Tiltakskode) {
  return useApiQuery({
    queryFn: () => PrismodellService.getForhandsgodkjenteSatser({ query: { tiltakstype } }),
    queryKey: QueryKeys.avtalteSatser(tiltakstype),
  });
}
