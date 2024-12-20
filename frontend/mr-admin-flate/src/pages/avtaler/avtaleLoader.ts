import {
  AnsattService,
  AvtalerService,
  NavEnheterService,
  NavEnhetStatus,
  TiltakstyperService,
} from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router";

export async function avtaleLoader({ params }: LoaderFunctionArgs) {
  if (!params.avtaleId) throw Error("Fant ikke avtaleId i route");
  const avtale = await AvtalerService.getAvtale({ id: params.avtaleId });
  const ansatt = await AnsattService.hentInfoOmAnsatt();
  return { avtale, ansatt };
}

export async function avtaleSkjemaLoader({ params }: LoaderFunctionArgs) {
  const avtale = params.avtaleId
    ? await AvtalerService.getAvtale({ id: params.avtaleId })
    : undefined;
  const tiltakstyper = await TiltakstyperService.getTiltakstyper();
  const ansatt = await AnsattService.hentInfoOmAnsatt();
  const enheter = await NavEnheterService.getEnheter({
    statuser: [
      NavEnhetStatus.AKTIV,
      NavEnhetStatus.UNDER_AVVIKLING,
      NavEnhetStatus.UNDER_ETABLERING,
    ],
  });

  return { avtale, tiltakstyper, ansatt, enheter };
}
