import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingerService, GjennomforingStatus } from "@mr/api-client";

export function useAktiveGjennomforingerByAvtaleId(avtaleId: string) {
  return useQuery({
    queryKey: QueryKeys.gjennomforing(avtaleId),
    queryFn: () =>
      GjennomforingerService.getGjennomforinger({
        avtaleId: avtaleId,
        statuser: [GjennomforingStatus.GJENNOMFORES],
      }),
  });
}
