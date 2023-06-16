import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";
import { NavAnsattRolle } from "mulighetsrommet-api-client";

export function useHentKontaktpersoner() {
  return useQuery(QueryKeys.kontaktpersoner(), () =>
    mulighetsrommetClient.ansatt.hentKontaktpersoner({
      roller: [NavAnsattRolle.KONTAKTPERSON],
    })
  );
}
