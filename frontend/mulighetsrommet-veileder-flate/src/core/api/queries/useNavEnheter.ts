import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";
import { NavEnhetStatus, NavEnhetType } from "mulighetsrommet-api-client";

export function useNavEnheter(
  statuser: NavEnhetStatus[] = [
    NavEnhetStatus.AKTIV,
    NavEnhetStatus.UNDER_AVVIKLING,
    NavEnhetStatus.UNDER_ETABLERING,
  ],
  typer: NavEnhetType[] = [
    NavEnhetType.ALS,
    NavEnhetType.FYLKE,
    NavEnhetType.LOKAL,
    NavEnhetType.KO,
    NavEnhetType.TILTAK,
  ],
) {
  return useQuery({
    queryKey: QueryKeys.navEnheter(statuser, typer),
    queryFn: () => {
      return mulighetsrommetClient.navEnheter.getEnheter({
        statuser,
        typer,
      });
    },
  });
}
