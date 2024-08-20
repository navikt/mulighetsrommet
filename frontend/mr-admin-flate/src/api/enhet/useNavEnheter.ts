import { useQuery } from "@tanstack/react-query";
import { type GetEnheterData, NavEnheterService, NavEnhetStatus } from "@mr/api-client";
import { QueryKeys } from "@/api/QueryKeys";

export function useNavEnheter(
  statuser: NavEnhetStatus[] = [
    NavEnhetStatus.AKTIV,
    NavEnhetStatus.UNDER_AVVIKLING,
    NavEnhetStatus.UNDER_ETABLERING,
  ],
) {
  const filter: GetEnheterData = {
    statuser,
  };

  return useQuery({
    queryKey: QueryKeys.enheter(filter),

    queryFn: () => {
      return NavEnheterService.getEnheter(filter);
    },
  });
}
