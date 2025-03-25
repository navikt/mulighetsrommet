import { TilsagnService } from "@mr/api-client-v2";

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
