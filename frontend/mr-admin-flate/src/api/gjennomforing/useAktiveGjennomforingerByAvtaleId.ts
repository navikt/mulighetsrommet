import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltaksgjennomforingerService, TiltaksgjennomforingStatus } from "@mr/api-client";

export function useAktiveGjennomforingerByAvtaleId(avtaleId: string) {
  return useQuery({
    queryKey: QueryKeys.gjennomforing(avtaleId),
    queryFn: () =>
      TiltaksgjennomforingerService.getTiltaksgjennomforinger({
        avtaleId: avtaleId,
        statuser: [TiltaksgjennomforingStatus.GJENNOMFORES],
      }),
  });
}
