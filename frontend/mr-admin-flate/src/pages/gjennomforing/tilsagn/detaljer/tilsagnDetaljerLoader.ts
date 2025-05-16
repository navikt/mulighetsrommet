import { TilsagnService, TilsagnStatus } from "@mr/api-client-v2";

import { QueryKeys } from "@/api/QueryKeys";

export const tilsagnQuery = (tilsagnId?: string) => ({
  queryKey: QueryKeys.getTilsagn(tilsagnId),
  queryFn: () => TilsagnService.getTilsagn({ path: { id: tilsagnId! } }),
  enabled: !!tilsagnId,
});

export const tilsagnHistorikkQuery = (tilsagnId?: string) => ({
  queryKey: ["tilsagn", tilsagnId, "historikk"],
  queryFn: () => TilsagnService.getTilsagnEndringshistorikk({ path: { id: tilsagnId! } }),
  enabled: !!tilsagnId,
});

export const aktiveTilsagnQuery = (gjennomforingId?: string) => ({
  queryKey: QueryKeys.getTilsagnForGjennomforing(gjennomforingId),
  queryFn: () =>
    TilsagnService.getAll({
      query: {
        gjennomforingId,
        statuser: [TilsagnStatus.GODKJENT, TilsagnStatus.TIL_GODKJENNING, TilsagnStatus.RETURNERT],
      },
    }),
  enabled: !!gjennomforingId,
});
