import {
  AvtalerService,
  TilsagnService,
  TilsagnStatus,
  GjennomforingerService,
} from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router";

export async function redigerTilsagnLoader({ params }: LoaderFunctionArgs) {
  const { gjennomforingId: gjennomforingId, tilsagnId } = params;

  if (!gjennomforingId) {
    throw new Error("gjennomforingId is missing");
  }

  if (!tilsagnId) {
    throw new Error("tilsagnId is missing");
  }

  const [gjennomforing, tilsagn, godkjenteTilsagn] = await Promise.all([
    GjennomforingerService.getGjennomforing({ id: gjennomforingId }),
    TilsagnService.getTilsagn({ id: tilsagnId }),
    TilsagnService.getAll({
      gjennomforingId,
      statuser: [TilsagnStatus.GODKJENT, TilsagnStatus.TIL_GODKJENNING, TilsagnStatus.RETURNERT],
    }),
  ]);

  // TODO: utled fra url, eller embed prismodell direkte i gjennomføring? Da slipper vi fossefall-requester
  const avtale = await AvtalerService.getAvtale({ id: gjennomforing.avtaleId! });

  return { avtale, gjennomforing, tilsagn, godkjenteTilsagn };
}
