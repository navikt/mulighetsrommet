import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { AnsattService, Rolle } from "@mr/api-client-v2";

export function useAvtaleAdministratorer() {
  return useApiQuery({
    queryKey: QueryKeys.navansatt(Rolle.AVTALER_SKRIV),
    queryFn: () =>
      AnsattService.hentAnsatte({
        query: { roller: [Rolle.AVTALER_SKRIV] },
      }),
  });
}
