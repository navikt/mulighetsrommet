import { RefusjonskravService, TiltaksgjennomforingerService } from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router";

export async function refusjonskravDetaljerLoader({ params }: LoaderFunctionArgs) {
  const { tiltaksgjennomforingId, refusjonskravId } = params;

  if (!tiltaksgjennomforingId) {
    throw new Error("tiltaksgjennomforingId is missing");
  }

  if (!refusjonskravId) {
    throw new Error("refusjonskravId is missing");
  }

  const [gjennomforing, refusjonskrav] = await Promise.all([
    TiltaksgjennomforingerService.getTiltaksgjennomforing({
      id: tiltaksgjennomforingId,
    }),
    RefusjonskravService.getRefusjonskrav({ id: refusjonskravId }),
  ]);

  return { gjennomforing, refusjonskrav };
}
