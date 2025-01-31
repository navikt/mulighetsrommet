import { GjennomforingerService, UtbetalingService } from "@mr/api-client-v2";
import { LoaderFunctionArgs } from "react-router";

export async function behandleUtbetalingFormPageLoader({ params }: LoaderFunctionArgs) {
  const { gjennomforingId, refusjonskravId } = params;

  if (!gjennomforingId) {
    throw new Error("gjennomforingId is missing");
  }
  if (!refusjonskravId) {
    throw new Error("refusjonskravId is missing");
  }

  const [{ data: gjennomforing }, { data: utbetaling }, { data: tilsagn }] = await Promise.all([
    GjennomforingerService.getGjennomforing({
      path: { id: gjennomforingId },
    }),
    UtbetalingService.getUtbetaling({ path: { id: refusjonskravId } }),
    UtbetalingService.getTilsagnTilKrav({ path: { id: refusjonskravId } }),
  ]);

  return { gjennomforing, krav: utbetaling.krav, tilsagn };
}
