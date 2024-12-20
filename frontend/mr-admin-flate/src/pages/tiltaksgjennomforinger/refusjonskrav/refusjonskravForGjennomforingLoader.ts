import { AnsattService, RefusjonskravService } from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router";

export async function refusjonskravForGjennomforingLoader({ params }: LoaderFunctionArgs) {
  const { tiltaksgjennomforingId } = params;
  const refusjonskrav = tiltaksgjennomforingId
    ? await RefusjonskravService.refusjonskravByTiltaksgjennomforing({
        tiltaksgjennomforingId,
      })
    : undefined;
  const ansatt = await AnsattService.hentInfoOmAnsatt();

  return { refusjonskrav, ansatt };
}
