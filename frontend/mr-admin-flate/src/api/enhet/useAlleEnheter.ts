import { useQuery } from "@tanstack/react-query";
import { NavEnhetStatus } from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useAlleEnheter() {
  return useQuery(QueryKeys.enheter(), () => {
    return mulighetsrommetClient.hentEnheter.hentAlleEnheter({
      statuser: [
        NavEnhetStatus.AKTIV,
        NavEnhetStatus.UNDER_AVVIKLING,
        NavEnhetStatus.UNDER_ETABLERING,
      ],
    });
  });
}
