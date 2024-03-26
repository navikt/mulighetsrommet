import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { mulighetsrommetClient } from "@/api/client";
import { NavAnsattRolle } from "mulighetsrommet-api-client";

export function useHentKontaktpersoner() {
  return useQuery({
    queryKey: QueryKeys.navansatt(NavAnsattRolle.KONTAKTPERSON),
    queryFn: () =>
      mulighetsrommetClient.ansatt.hentAnsatte({
        roller: [NavAnsattRolle.KONTAKTPERSON],
      }),
  });
}
