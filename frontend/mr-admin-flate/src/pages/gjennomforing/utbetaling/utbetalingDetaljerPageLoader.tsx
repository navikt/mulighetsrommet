import { GjennomforingerService, UtbetalingService } from "@mr/api-client-v2";
import { LoaderFunctionArgs } from "react-router";

export async function utbetalingDetaljerPageLoader({ params }: LoaderFunctionArgs) {
  const { gjennomforingId, utbetalingId } = params;

  if (!gjennomforingId) {
    throw new Error("gjennomforingId is missing");
  }
  if (!utbetalingId) {
    throw new Error("utbetalingId is missing");
  }

  const [{ data: gjennomforing }, { data: utbetaling }] = await Promise.all([
    GjennomforingerService.getGjennomforing({
      path: { id: gjennomforingId },
    }),
    UtbetalingService.getUtbetaling({ path: { id: utbetalingId } }),
  ]);

  return { gjennomforing, utbetaling };
}
