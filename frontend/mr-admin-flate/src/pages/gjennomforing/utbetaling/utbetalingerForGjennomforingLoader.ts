import { UtbetalingService } from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useUtbetalingerByGjennomforing(gjennomforingId: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.utbetalingerByGjennomforing(gjennomforingId),
    queryFn: async () =>
      UtbetalingService.utbetalingerByGjennomforing({
        path: { gjennomforingId },
      }),
  });
}
