import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingerService, GjennomforingStatus } from "@mr/api-client-v2";

export function useAktiveGjennomforingerByAvtaleId(avtaleId: string) {
  return useApiQuery({
    queryKey: QueryKeys.gjennomforing(avtaleId),
    queryFn: () =>
      GjennomforingerService.getGjennomforinger({
        query: {
          avtaleId: avtaleId,
          statuser: [GjennomforingStatus.GJENNOMFORES],
        },
      }),
  });
}
