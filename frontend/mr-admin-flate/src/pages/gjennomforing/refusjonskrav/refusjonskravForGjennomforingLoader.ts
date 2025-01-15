import { RefusjonskravService, GjennomforingerService } from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router";

export async function refusjonskravForGjennomforingLoader({ params }: LoaderFunctionArgs) {
  const { gjennomforingId } = params;

  if (!gjennomforingId) {
    throw Error("Fant ikke gjennomforingId i route");
  }

  const [gjennomforing, refusjonskrav] = await Promise.all([
    GjennomforingerService.getGjennomforing({
      id: gjennomforingId,
    }),

    RefusjonskravService.refusjonskravByGjennomforing({
      gjennomforingId,
    }),
  ]);

  return { refusjonskrav, gjennomforing };
}
