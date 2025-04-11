import { TilsagnService, TilsagnStatus } from "@mr/api-client-v2";

import { queryOptions } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";

export const tilsagnQuery = (tilsagnId?: string) =>
  queryOptions({
    queryKey: QueryKeys.getTilsagn(tilsagnId),
    queryFn: () => TilsagnService.getTilsagn({ path: { id: tilsagnId! } }),
    enabled: !!tilsagnId,
  });

export const tilsagnHistorikkQuery = (tilsagnId?: string) =>
  queryOptions({
    queryKey: ["tilsagn", tilsagnId, "historikk"],
    queryFn: () => TilsagnService.getTilsagnEndringshistorikk({ path: { id: tilsagnId! } }),
    enabled: !!tilsagnId,
  });

export const aktiveTilsagnQuery = (gjennomforingId?: string) =>
  queryOptions({
    queryKey: QueryKeys.getTilsagnForGjennomforing(gjennomforingId),
    queryFn: () =>
      TilsagnService.getAll({
        query: {
          gjennomforingId,
          statuser: [
            TilsagnStatus.GODKJENT,
            TilsagnStatus.TIL_GODKJENNING,
            TilsagnStatus.RETURNERT,
          ],
        },
      }),
    enabled: !!gjennomforingId,
  });
