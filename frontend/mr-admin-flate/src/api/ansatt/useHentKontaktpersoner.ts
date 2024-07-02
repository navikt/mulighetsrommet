import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { AnsattService, NavAnsattRolle } from "mulighetsrommet-api-client";

export function useHentKontaktpersoner() {
  return useQuery({
    queryKey: QueryKeys.navansatt(NavAnsattRolle.KONTAKTPERSON),
    queryFn: () =>
      AnsattService.hentAnsatte({
        roller: [NavAnsattRolle.KONTAKTPERSON],
      }),
  });
}
