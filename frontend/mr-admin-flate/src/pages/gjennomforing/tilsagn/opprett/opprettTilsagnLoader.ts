import {
  AvtalerService,
  TilsagnService,
  TilsagnStatus,
  TilsagnType,
  GjennomforingerService,
} from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router";

export async function opprettTilsagnLoader({ params, request }: LoaderFunctionArgs) {
  const { tiltaksgjennomforingId: gjennomforingId } = params;

  if (!gjennomforingId) {
    throw new Error("tiltaksgjennomforingId is missing");
  }

  const url = new URL(request.url);
  const type = (url.searchParams.get("type") as TilsagnType) ?? TilsagnType.TILSAGN;

  const [gjennomforing, defaults, godkjenteTilsagn] = await Promise.all([
    GjennomforingerService.getGjennomforing({ id: gjennomforingId }),
    TilsagnService.getTilsagnDefaults({ gjennomforingId, type }),
    TilsagnService.getAll({
      gjennomforingId,
      statuser: [TilsagnStatus.GODKJENT, TilsagnStatus.TIL_GODKJENNING, TilsagnStatus.RETURNERT],
    }),
  ]);

  // TODO: utled fra url, eller embed prismodell direkte i gjennomføring? Da slipper vi fossefall-requester
  const avtale = await AvtalerService.getAvtale({ id: gjennomforing.avtaleId! });

  return { avtale, gjennomforing, defaults, godkjenteTilsagn };
}
