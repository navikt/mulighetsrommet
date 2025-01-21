import {
  AvtalerService,
  TilsagnService,
  TilsagnStatus,
  TilsagnType,
  GjennomforingerService,
} from "@mr/api-client-v2";
import { LoaderFunctionArgs } from "react-router";

export async function opprettTilsagnLoader({ params, request }: LoaderFunctionArgs) {
  const { gjennomforingId: gjennomforingId } = params;

  if (!gjennomforingId) {
    throw new Error("gjennomforingId is missing");
  }

  const url = new URL(request.url);
  const type = (url.searchParams.get("type") as TilsagnType) ?? TilsagnType.TILSAGN;

  const [{ data: gjennomforing }, { data: defaults }, { data: godkjenteTilsagn }] =
    await Promise.all([
      GjennomforingerService.getGjennomforing({ path: { id: gjennomforingId } }),
      TilsagnService.getTilsagnDefaults({ query: { gjennomforingId, type } }),
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

  return { avtale, gjennomforing, defaults, godkjenteTilsagn };
}
