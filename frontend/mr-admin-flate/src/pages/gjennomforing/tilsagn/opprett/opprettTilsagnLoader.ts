import { Prismodell, TilsagnService, TilsagnType } from "@mr/api-client-v2";

import { queryOptions } from "@tanstack/react-query";
import { QueryKeys } from "../../../../api/QueryKeys";

export const tilsagnDefaultsQuery = (params: {
  gjennomforingId?: string;
  type: TilsagnType;
  prismodell: Prismodell | null;
  periodeStart: string | null;
  periodeSlutt: string | null;
  belop: number | null;
  kostnadssted: string | null;
}) =>
  queryOptions({
    queryKey: [QueryKeys.opprettTilsagn(), params],
    queryFn: () =>
      TilsagnService.getTilsagnDefaults({
        body: { gjennomforingId: params.gjennomforingId!, ...params },
      }),
    enabled: !!params.gjennomforingId,
  });
