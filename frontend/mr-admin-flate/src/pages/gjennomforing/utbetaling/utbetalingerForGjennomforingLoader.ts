import { GjennomforingerService, UtbetalingService } from "@mr/api-client-v2";
import { LoaderFunctionArgs } from "react-router";

export async function utbetalingerForGjennomforingLoader({ params }: LoaderFunctionArgs) {
  const { gjennomforingId } = params;

  if (!gjennomforingId) {
    throw Error("Fant ikke gjennomforingId i route");
  }

  const [{ data: gjennomforing }, { data: utbetalinger }] = await Promise.all([
    GjennomforingerService.getGjennomforing({
      path: { id: gjennomforingId },
    }),

    UtbetalingService.utbetalingerByGjennomforing({
      path: { gjennomforingId },
    }),
  ]);

  return { utbetalinger, gjennomforing };
}
