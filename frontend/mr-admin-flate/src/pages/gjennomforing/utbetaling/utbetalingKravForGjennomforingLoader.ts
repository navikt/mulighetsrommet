import { RefusjonskravService, GjennomforingerService } from "@mr/api-client-v2";
import { LoaderFunctionArgs } from "react-router";

export async function utbetalingskravForGjennomforingLoader({ params }: LoaderFunctionArgs) {
  const { gjennomforingId } = params;

  if (!gjennomforingId) {
    throw Error("Fant ikke gjennomforingId i route");
  }

  const [{ data: gjennomforing }, { data: refusjonskrav }] = await Promise.all([
    GjennomforingerService.getGjennomforing({
      path: { id: gjennomforingId },
    }),

    RefusjonskravService.refusjonskravByGjennomforing({
      path: { gjennomforingId },
    }),
  ]);

  return { refusjonskrav, gjennomforing };
}
