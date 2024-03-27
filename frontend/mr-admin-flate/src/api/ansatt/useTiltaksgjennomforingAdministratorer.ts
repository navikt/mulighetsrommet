import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { mulighetsrommetClient } from "@/api/client";
import { NavAnsattRolle } from "mulighetsrommet-api-client";

export function useTiltaksgjennomforingAdministratorer() {
  return useQuery({
    queryKey: QueryKeys.navansatt(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV),
    queryFn: () =>
      mulighetsrommetClient.ansatt.hentAnsatte({
        roller: [NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV],
      }),
  });
}
