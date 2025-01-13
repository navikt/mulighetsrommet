import { useQuery } from "@tanstack/react-query";
import { PrismodellService, Tiltakskode } from "@mr/api-client";
import { QueryKeys } from "../QueryKeys";

export function useForhandsgodkjenteSatser(tiltakstype: Tiltakskode) {
  return useQuery({
    queryFn: () => PrismodellService.getForhandsgodkjenteSatser({ tiltakstype }),
    queryKey: QueryKeys.avtalteSatser(tiltakstype),
  });
}
