import { AnsattService, GjennomforingerService, UtbetalingService } from "@mr/api-client-v2";
import { LoaderFunctionArgs } from "react-router";

export async function utbetalingPageLoader({ params }: LoaderFunctionArgs) {
  const { gjennomforingId, utbetalingId } = params;

  if (!gjennomforingId) {
    throw new Error("gjennomforingId is missing");
  }
  if (!utbetalingId) {
    throw new Error("utbetalingId is missing");
  }

  const [{ data: ansatt }, { data: gjennomforing }, { data: utbetaling }, { data: tilsagn }] =
    await Promise.all([
      AnsattService.hentInfoOmAnsatt(),
      GjennomforingerService.getGjennomforing({
        path: { id: gjennomforingId },
      }),
      UtbetalingService.getUtbetaling({ path: { id: utbetalingId } }),
      UtbetalingService.getTilsagnTilUtbetaling({ path: { id: utbetalingId } }),
    ]);

  return { ansatt, gjennomforing, utbetaling, tilsagn };
}
