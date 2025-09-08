import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { AnsattService, Rolle } from "@tiltaksadministrasjon/api-client";

export function useGjennomforingAdministratorer() {
  return useApiQuery({
    queryKey: QueryKeys.navansatt(Rolle.TILTAKSGJENNOMFORINGER_SKRIV),
    queryFn: () =>
      AnsattService.getAnsatte({
        query: {
          roller: [Rolle.TILTAKSGJENNOMFORINGER_SKRIV],
        },
      }),
  });
}
