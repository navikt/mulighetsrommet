import { UtbetalingService } from "@mr/api-client-v2";
import { queryOptions } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";

export const utbetalingerByGjennomforingQuery = (gjennomforingId?: string) =>
  queryOptions({
    queryKey: QueryKeys.utbetalingerByGjennomforing(gjennomforingId),
    queryFn: () =>
      UtbetalingService.utbetalingerByGjennomforing({
        path: { gjennomforingId: gjennomforingId! },
      }),
    enabled: !!gjennomforingId,
  });
