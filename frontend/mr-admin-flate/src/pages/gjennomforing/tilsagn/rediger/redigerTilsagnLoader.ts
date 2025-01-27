import {
  AvtalerService,
  TilsagnService,
  TilsagnStatus,
  GjennomforingerService,
} from "@mr/api-client-v2";
import { LoaderFunctionArgs } from "react-router";

export async function redigerTilsagnLoader({ params }: LoaderFunctionArgs) {
  const { gjennomforingId: gjennomforingId, tilsagnId } = params;

  if (!gjennomforingId) {
    throw new Error("gjennomforingId is missing");
  }

  if (!tilsagnId) {
    throw new Error("tilsagnId is missing");
  }

  const [{ data: gjennomforing }, { data: tilsagn }, { data: godkjenteTilsagn }] =
    await Promise.all([
      GjennomforingerService.getGjennomforing({ path: { id: gjennomforingId } }),
      TilsagnService.getTilsagn({ path: { id: tilsagnId } }),
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
    ]);

  // TODO: utled fra url, eller embed prismodell direkte i gjennomføring? Da slipper vi fossefall-requester
  const { data: avtale } = await AvtalerService.getAvtale({
    path: { id: gjennomforing.avtaleId! },
  });

  return { avtale, gjennomforing, tilsagn, godkjenteTilsagn };
}
