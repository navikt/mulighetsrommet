import { useQuery } from "@tanstack/react-query";
import { NavEnheterService, NavEnhetStatus } from "@mr/api-client";
import { QueryKeys } from "@/api/QueryKeys";

export function useNavEnheter(
  statuser: NavEnhetStatus[] = [
    NavEnhetStatus.AKTIV,
    NavEnhetStatus.UNDER_AVVIKLING,
    NavEnhetStatus.UNDER_ETABLERING,
  ],
) {
  return useQuery({
    queryKey: QueryKeys.enheter(),

    queryFn: () => {
      return NavEnheterService.getEnheter({
        statuser,
      });
    },
  });
}
