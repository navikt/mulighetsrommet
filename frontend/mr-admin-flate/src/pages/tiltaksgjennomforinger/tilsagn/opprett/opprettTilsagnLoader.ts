import {
  TilsagnStatus,
  TilsagnService,
  TilsagnType,
  TiltaksgjennomforingerService,
} from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router-dom";

export async function opprettTilsagnLoader({ params, request }: LoaderFunctionArgs) {
  const { tiltaksgjennomforingId: gjennomforingId } = params;

  if (!gjennomforingId) {
    throw new Error("tiltaksgjennomforingId is missing");
  }

  const url = new URL(request.url);
  const type = (url.searchParams.get("type") as TilsagnType) ?? TilsagnType.TILSAGN;

  const [gjennomforing, defaults, godkjenteTilsagn] = await Promise.all([
    TiltaksgjennomforingerService.getTiltaksgjennomforing({ id: gjennomforingId }),
    TilsagnService.getTilsagnDefaults({ gjennomforingId, type }),
    TilsagnService.getAll({
      gjennomforingId,
      statuser: [TilsagnStatus.GODKJENT, TilsagnStatus.TIL_GODKJENNING, TilsagnStatus.RETURNERT],
    }),
  ]);

  return { gjennomforing, defaults, godkjenteTilsagn };
}
