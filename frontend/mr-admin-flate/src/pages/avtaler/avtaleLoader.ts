import {
  AnsattService,
  AvtalerService,
  NavEnheterService,
  NavEnhetStatus,
  TiltakstyperService,
} from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router-dom";

export async function avtaleLoader({ params }: LoaderFunctionArgs) {
  if (!params.avtaleId) throw Error("Fant ikke avtaleId i route");
  return await AvtalerService.getAvtale({ id: params.avtaleId });
}

export async function avtaleSkjemaLoader({ params }: LoaderFunctionArgs) {
  const avtale = params.avtaleId
    ? await AvtalerService.getAvtale({ id: params.avtaleId })
    : undefined;
  const tiltakstyper = await TiltakstyperService.getTiltakstyper();
  const ansatt = await AnsattService.hentInfoOmAnsatt();
  const navEnheter = await NavEnheterService.getEnheter({
    statuser: [
      NavEnhetStatus.AKTIV,
      NavEnhetStatus.UNDER_AVVIKLING,
      NavEnhetStatus.UNDER_ETABLERING,
    ],
  });

  return { avtale, tiltakstyper, ansatt, navEnheter };
}
