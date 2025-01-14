import { RefusjonskravService, GjennomforingerService } from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router";

export async function refusjonskravForGjennomforingLoader({ params }: LoaderFunctionArgs) {
  const { tiltaksgjennomforingId } = params;

  if (!tiltaksgjennomforingId) {
    throw Error("Fant ikke tiltaksgjennomforingId i route");
  }

  const [gjennomforing, refusjonskrav] = await Promise.all([
    GjennomforingerService.getGjennomforing({
      id: tiltaksgjennomforingId,
    }),

    RefusjonskravService.refusjonskravByGjennomforing({
      tiltaksgjennomforingId,
    }),
  ]);

  return { refusjonskrav, gjennomforing };
}
