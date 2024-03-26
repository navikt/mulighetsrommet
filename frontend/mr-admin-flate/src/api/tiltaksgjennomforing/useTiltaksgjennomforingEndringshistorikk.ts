import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { mulighetsrommetClient } from "@/api/client";

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
