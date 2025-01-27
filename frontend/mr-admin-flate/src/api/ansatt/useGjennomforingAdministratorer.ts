import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { AnsattService, NavAnsattRolle } from "@mr/api-client-v2";

export function useGjennomforingAdministratorer() {
  return useApiQuery({
    queryKey: QueryKeys.navansatt(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV),
    queryFn: () =>
      AnsattService.hentAnsatte({
        query: {
          roller: [NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV],
        },
      }),
  });
}
