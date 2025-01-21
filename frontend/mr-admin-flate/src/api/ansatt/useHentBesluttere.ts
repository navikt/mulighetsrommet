import { useApiQuery } from "@/hooks/useApiQuery";
import { QueryKeys } from "@/api/QueryKeys";
import { AnsattService, NavAnsattRolle } from "@mr/api-client-v2";

export function useHentBesluttere() {
  return useApiQuery({
    queryKey: QueryKeys.navansatt(NavAnsattRolle.OKONOMI_BESLUTTER),
    queryFn: () =>
      AnsattService.hentAnsatte({
        query: {
          roller: [NavAnsattRolle.OKONOMI_BESLUTTER],
        },
      }),
  });
}
