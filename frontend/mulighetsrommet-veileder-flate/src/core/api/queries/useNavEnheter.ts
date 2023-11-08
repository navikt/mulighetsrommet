import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";
import { NavEnhetStatus } from "mulighetsrommet-api-client";

export function useNavEnheter(
  statuser: NavEnhetStatus[] = [
    NavEnhetStatus.AKTIV,
    NavEnhetStatus.UNDER_AVVIKLING,
    NavEnhetStatus.UNDER_ETABLERING,
  ],
) {
  return useQuery({
    queryKey: QueryKeys.navEnheter(statuser),
    queryFn: () => {
      return mulighetsrommetClient.navEnheter.getEnheter({
        statuser,
      });
    },
  });
}
