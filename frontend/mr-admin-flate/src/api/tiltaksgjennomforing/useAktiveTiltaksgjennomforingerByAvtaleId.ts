import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { mulighetsrommetClient } from "@/api/client";
import { TiltaksgjennomforingStatus } from "mulighetsrommet-api-client";

export function useAktiveTiltaksgjennomforingerByAvtaleId(avtaleId: string) {
  return useQuery({
    queryKey: QueryKeys.tiltaksgjennomforing(avtaleId),
    queryFn: () =>
      mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforinger({
        avtaleId: avtaleId!!,
        statuser: [TiltaksgjennomforingStatus.GJENNOMFORES, TiltaksgjennomforingStatus.PLANLAGT],
      }),
  });
}
