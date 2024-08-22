import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { AnsattService, NavAnsattRolle } from "@mr/api-client";

export function useAvtaleAdministratorer() {
  return useQuery({
    queryKey: QueryKeys.navansatt(NavAnsattRolle.AVTALER_SKRIV),
    queryFn: () =>
      AnsattService.hentAnsatte({
        roller: [NavAnsattRolle.AVTALER_SKRIV],
      }),
  });
}
