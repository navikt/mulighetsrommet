import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { AnsattService, Rolle } from "@tiltaksadministrasjon/api-client";

export function useAvtaleAdministratorer() {
  return useApiQuery({
    queryKey: QueryKeys.navansatt(Rolle.AVTALER_SKRIV),
    queryFn: () =>
      AnsattService.getAnsatte({
        query: { roller: [Rolle.AVTALER_SKRIV] },
      }),
  });
}
