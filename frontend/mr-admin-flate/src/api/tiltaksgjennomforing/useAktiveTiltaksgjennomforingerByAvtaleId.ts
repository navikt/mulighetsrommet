import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import {
  TiltaksgjennomforingerService,
  TiltaksgjennomforingStatus,
} from "mulighetsrommet-api-client";

export function useAktiveTiltaksgjennomforingerByAvtaleId(avtaleId: string) {
  return useQuery({
    queryKey: QueryKeys.tiltaksgjennomforing(avtaleId),
    queryFn: () =>
      TiltaksgjennomforingerService.getTiltaksgjennomforinger({
        avtaleId: avtaleId,
        statuser: [TiltaksgjennomforingStatus.GJENNOMFORES, TiltaksgjennomforingStatus.PLANLAGT],
      }),
  });
}
