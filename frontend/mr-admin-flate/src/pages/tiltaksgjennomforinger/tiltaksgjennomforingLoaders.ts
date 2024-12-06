import { AnsattService, AvtalerService, TiltaksgjennomforingerService } from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router-dom";

export async function tiltaksgjennomforingLoader({ params }: LoaderFunctionArgs) {
  const tiltaksgjennomforing = params.tiltaksgjennomforingId
    ? await TiltaksgjennomforingerService.getTiltaksgjennomforing({
        id: params.tiltaksgjennomforingId,
      })
    : undefined;
  const avtaleId = params?.avtaleId || tiltaksgjennomforing?.avtaleId;
  const avtale = avtaleId ? await AvtalerService.getAvtale({ id: avtaleId }) : undefined;
  const ansatt = await AnsattService.hentInfoOmAnsatt();
  return { tiltaksgjennomforing, avtale, ansatt };
}
