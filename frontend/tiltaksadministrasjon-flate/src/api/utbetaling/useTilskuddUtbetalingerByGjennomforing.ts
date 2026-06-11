import { QueryKeys } from "@/api/QueryKeys";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { TilskuddService } from "@tiltaksadministrasjon/api-client";

export function useTilskuddUtbetalingerByGjennomforing(gjennomforingId: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.utbetalingerByGjennomforing(gjennomforingId),
    queryFn: async () => TilskuddService.getTilskuddUtbetalinger({ query: { gjennomforingId } }),
  });
}
