import { QueryKeys } from "@/api/QueryKeys";
import { type GetEnheterData, NavEnheterService, NavEnhetStatus } from "@mr/api-client-v2";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useNavEnheter(
  statuser: NavEnhetStatus[] = [
    NavEnhetStatus.AKTIV,
    NavEnhetStatus.UNDER_AVVIKLING,
    NavEnhetStatus.UNDER_ETABLERING,
  ],
) {
  const filter: Pick<GetEnheterData, "query"> = {
    query: { statuser },
  };

  return useApiSuspenseQuery({
    queryKey: QueryKeys.enheter(filter),

    queryFn: () => {
      return NavEnheterService.getEnheter(filter);
    },
  });
}
