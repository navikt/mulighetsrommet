import { AnsattService, AvtalerService, TiltaksgjennomforingerService } from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router";

export async function gjennomforingLoader({ params }: LoaderFunctionArgs) {
  if (!params.tiltaksgjennomforingId) {
    throw Error("Fant ikke gjennomforingId i route");
  }

  const [ansatt, gjennomforing] = await Promise.all([
    AnsattService.hentInfoOmAnsatt(),

    TiltaksgjennomforingerService.getTiltaksgjennomforing({
      id: params.tiltaksgjennomforingId,
    }),
  ]);

  const avtaleId = params?.avtaleId || gjennomforing?.avtaleId;
  const avtale = avtaleId ? await AvtalerService.getAvtale({ id: avtaleId }) : undefined;

  return { gjennomforing, avtale, ansatt };
}

export async function gjennomforingFormLoader({ params }: LoaderFunctionArgs) {
  const [ansatt, gjennomforing] = await Promise.all([
    AnsattService.hentInfoOmAnsatt(),

    params.tiltaksgjennomforingId
      ? await TiltaksgjennomforingerService.getTiltaksgjennomforing({
          id: params.tiltaksgjennomforingId,
        })
      : undefined,
  ]);

  const avtaleId = params?.avtaleId || gjennomforing?.avtaleId;
  const avtale = avtaleId ? await AvtalerService.getAvtale({ id: avtaleId }) : undefined;

  return { gjennomforing, avtale, ansatt };
}
