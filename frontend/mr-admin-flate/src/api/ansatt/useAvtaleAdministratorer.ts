import { useApiQuery } from "@/hooks/useApiQuery";
import { QueryKeys } from "@/api/QueryKeys";
import { AnsattService, NavAnsattRolle } from "@mr/api-client-v2";

export function useAvtaleAdministratorer() {
  return useApiQuery({
    queryKey: QueryKeys.navansatt(NavAnsattRolle.AVTALER_SKRIV),
    queryFn: () =>
      AnsattService.hentAnsatte({
        query: { roller: [NavAnsattRolle.AVTALER_SKRIV] },
      }),
  });
}
