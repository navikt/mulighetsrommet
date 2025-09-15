import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { AnsattService, Rolle } from "@tiltaksadministrasjon/api-client";

export function useAvtaleAdministratorer() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.navansatt(Rolle.AVTALER_SKRIV),
    queryFn: () =>
      AnsattService.getAnsatte({
        query: { roller: [Rolle.AVTALER_SKRIV] },
      }),
  });
}
