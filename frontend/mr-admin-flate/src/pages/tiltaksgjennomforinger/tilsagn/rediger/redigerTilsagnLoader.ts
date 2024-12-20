import {
  AvtalerService,
  TilsagnService,
  TilsagnStatus,
  TiltaksgjennomforingerService,
} from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router-dom";

export async function redigerTilsagnLoader({ params }: LoaderFunctionArgs) {
  const { tiltaksgjennomforingId: gjennomforingId, tilsagnId } = params;

  if (!gjennomforingId) {
    throw new Error("tiltaksgjennomforingId is missing");
  }

  if (!tilsagnId) {
    throw new Error("tilsagnId is missing");
  }

  const [gjennomforing, tilsagn, godkjenteTilsagn] = await Promise.all([
    TiltaksgjennomforingerService.getTiltaksgjennomforing({ id: gjennomforingId }),
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
