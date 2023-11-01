import { useQuery } from "react-query";
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
  return useQuery(QueryKeys.navEnheter(statuser), () => {
    return mulighetsrommetClient.navEnheter.getEnheter({
      statuser,
    });
  });
}
