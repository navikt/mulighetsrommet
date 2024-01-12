import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";
import { NavAnsattRolle } from "mulighetsrommet-api-client";

export function useAvtaleAdministratorer() {
  return useQuery({
    queryKey: QueryKeys.navansatt(NavAnsattRolle.AVTALER_SKRIV),
    queryFn: () =>
      mulighetsrommetClient.ansatt.hentAnsatte({
        roller: [NavAnsattRolle.AVTALER_SKRIV],
      }),
  });
}
