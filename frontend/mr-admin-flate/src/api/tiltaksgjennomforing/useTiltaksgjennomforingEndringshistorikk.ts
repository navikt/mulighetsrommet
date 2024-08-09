import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltaksgjennomforingerService } from "mulighetsrommet-api-client";

export function useTiltaksgjennomforingEndringshistorikk(id: string) {
  return useSuspenseQuery({
    queryKey: QueryKeys.tiltaksgjennomforingHistorikk(id),
    queryFn() {
      return TiltaksgjennomforingerService.getTiltaksgjennomforingEndringshistorikk({
        id,
      });
    },
  });
}
