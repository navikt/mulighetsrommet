import { useApiQuery } from "@mr/frontend-common";
import { type GetEnheterData, NavEnheterService, NavEnhetStatus } from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";

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

  return useApiQuery({
    queryKey: QueryKeys.enheter(filter),

    queryFn: () => {
      return NavEnheterService.getEnheter(filter);
    },
  });
}
