import { useQuery } from "@tanstack/react-query";
import { NavEnhetStatus } from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

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
      return mulighetsrommetClient.navEnheter.getEnheter({
        statuser,
      });
    },
  });
}
