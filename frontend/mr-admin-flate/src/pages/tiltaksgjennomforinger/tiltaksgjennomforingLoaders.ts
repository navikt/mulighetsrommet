import { AnsattService, AvtalerService, TiltaksgjennomforingerService } from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router";

export async function tiltaksgjennomforingLoader({ params }: LoaderFunctionArgs) {
  if (!params.tiltaksgjennomforingId) {
    throw Error("Fant ikke tiltaksgjennomforingId i route");
  }

  const [ansatt, tiltaksgjennomforing] = await Promise.all([
    AnsattService.hentInfoOmAnsatt(),

    TiltaksgjennomforingerService.getTiltaksgjennomforing({
      id: params.tiltaksgjennomforingId,
    }),
  ]);

  const avtaleId = params?.avtaleId || tiltaksgjennomforing?.avtaleId;
  const avtale = avtaleId ? await AvtalerService.getAvtale({ id: avtaleId }) : undefined;

  return { tiltaksgjennomforing, avtale, ansatt };
}

export async function tiltaksgjennomforingSkjemaLoader({ params }: LoaderFunctionArgs) {
  const [ansatt, tiltaksgjennomforing] = await Promise.all([
    AnsattService.hentInfoOmAnsatt(),

    params.tiltaksgjennomforingId
      ? await TiltaksgjennomforingerService.getTiltaksgjennomforing({
          id: params.tiltaksgjennomforingId,
        })
      : undefined,
  ]);

  const avtaleId = params?.avtaleId || tiltaksgjennomforing?.avtaleId;
  const avtale = avtaleId ? await AvtalerService.getAvtale({ id: avtaleId }) : undefined;

  return { tiltaksgjennomforing, avtale, ansatt };
}
