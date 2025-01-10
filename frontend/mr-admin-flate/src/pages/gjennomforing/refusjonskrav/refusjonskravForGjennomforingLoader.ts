import { RefusjonskravService, TiltaksgjennomforingerService } from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router";

export async function refusjonskravForGjennomforingLoader({ params }: LoaderFunctionArgs) {
  const { tiltaksgjennomforingId } = params;

  if (!tiltaksgjennomforingId) {
    throw Error("Fant ikke tiltaksgjennomforingId i route");
  }

  const [gjennomforing, refusjonskrav] = await Promise.all([
    TiltaksgjennomforingerService.getTiltaksgjennomforing({
      id: tiltaksgjennomforingId,
    }),

    RefusjonskravService.refusjonskravByTiltaksgjennomforing({
      tiltaksgjennomforingId,
    }),
  ]);

  return { refusjonskrav, gjennomforing };
}
