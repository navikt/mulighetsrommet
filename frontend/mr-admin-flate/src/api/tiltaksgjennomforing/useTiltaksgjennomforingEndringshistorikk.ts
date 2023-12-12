import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useTiltaksgjennomforingEndringshistorikk(id: string) {
  return useSuspenseQuery({
    queryKey: QueryKeys.tiltaksgjennomforingHistorikk(id),
    queryFn() {
      return mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforingEndringshistorikk({
        id,
      });
    },
  });
}
