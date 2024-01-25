import { NavEnhetStatus, NavEnhetType } from "mulighetsrommet-api-client";
import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { mulighetsrommetClient } from "../clients";

export function useNavEnheter(
  typer: NavEnhetType[] = [
    NavEnhetType.FYLKE,
    NavEnhetType.ALS,
    NavEnhetType.LOKAL,
    NavEnhetType.KO,
    NavEnhetType.TILTAK,
  ],
  statuser: NavEnhetStatus[] = [
    NavEnhetStatus.AKTIV,
    NavEnhetStatus.UNDER_AVVIKLING,
    NavEnhetStatus.UNDER_ETABLERING,
  ],
) {
  return useQuery({
    queryKey: QueryKeys.navEnheter(statuser, typer),
    queryFn: () => {
      return mulighetsrommetClient.navEnheter.getEnheter({ statuser, typer });
    },
  });
}
