import { UtbetalingService } from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useUtbetalingerByGjennomforing(gjennomforingId: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.utbetalingerByGjennomforing(gjennomforingId),
    queryFn: async () => UtbetalingService.getUtbetalinger({ query: { gjennomforingId } }),
  });
}
