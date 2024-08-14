import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { AnsattService, NavAnsattRolle } from "@mr/api-client";

export function useTiltaksgjennomforingAdministratorer() {
  return useQuery({
    queryKey: QueryKeys.navansatt(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV),
    queryFn: () =>
      AnsattService.hentAnsatte({
        roller: [NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV],
      }),
  });
}
