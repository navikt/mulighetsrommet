import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";
import { NavAnsattRolle } from "mulighetsrommet-api-client";

export function useHentBetabrukere() {
  return useQuery(QueryKeys.kontaktpersoner(), () =>
    mulighetsrommetClient.ansatt.hentAnsatte({
      roller: [NavAnsattRolle.BETABRUKER],
    })
  );
}