import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { AnsattService, Rolle } from "@mr/api-client-v2";

export function useGjennomforingAdministratorer() {
  return useApiQuery({
    queryKey: QueryKeys.navansatt(Rolle.TILTAKSGJENNOMFORINGER_SKRIV),
    queryFn: () =>
      AnsattService.hentAnsatte({
        query: {
          roller: [Rolle.TILTAKSGJENNOMFORINGER_SKRIV],
        },
      }),
  });
}
