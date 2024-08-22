import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltaksgjennomforingerService } from "@mr/api-client";

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
