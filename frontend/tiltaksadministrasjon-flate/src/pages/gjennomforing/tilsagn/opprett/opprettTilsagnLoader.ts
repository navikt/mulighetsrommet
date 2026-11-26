import {
  GjennomforingService,
  TilsagnDefaultsRequest,
  TilsagnService,
  TilsagnType,
} from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiSuspenseQueries } from "@mr/frontend-common";
import { z } from "zod";

export function getTilsagnDefaultsRequest(
  gjennomforingId: string,
  searchParams: URLSearchParams,
): TilsagnDefaultsRequest {
  const type = z.enum(TilsagnType).parse(searchParams.get("type") ?? TilsagnType.TILSAGN);
  return {
    gjennomforingId,
    type,
    periodeStart: z.iso.date().nullable().parse(searchParams.get("periodeStart")),
    periodeSlutt: z.iso.date().nullable().parse(searchParams.get("periodeSlutt")),
    deltaker: z.uuid().nullable().parse(searchParams.get("deltakerId")),
  };
}

export function useOpprettTilsagnData(request: TilsagnDefaultsRequest) {
  const [detaljer, defaults] = useApiSuspenseQueries({
    queries: [
      {
        queryKey: QueryKeys.gjennomforing(request.gjennomforingId),
        queryFn: async () =>
          GjennomforingService.getGjennomforing({
            path: { id: request.gjennomforingId },
          }),
      },
      {
        queryKey: [QueryKeys.opprettTilsagn(), request],
        queryFn: async () => TilsagnService.getTilsagnDefaults({ body: request }),
        staleTime: "static",
        gcTime: 0,
      },
    ] as const,
  });

  return { detaljer: detaljer.data, defaults: defaults.data };
}
