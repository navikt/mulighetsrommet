import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { AnsattService, NavAnsattRolle } from "@mr/api-client";

export function useHentBesluttere() {
  return useQuery({
    queryKey: QueryKeys.navansatt(NavAnsattRolle.OKONOMI_BESLUTTER),
    queryFn: () =>
      AnsattService.hentAnsatte({
        roller: [NavAnsattRolle.OKONOMI_BESLUTTER],
      }),
  });
}
