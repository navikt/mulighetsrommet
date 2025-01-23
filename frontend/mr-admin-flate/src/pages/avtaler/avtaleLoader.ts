import {
  AnsattService,
  AvtalerService,
  NavEnheterService,
  NavEnhetStatus,
  TiltakstyperService,
} from "@mr/api-client-v2";
import { LoaderFunctionArgs } from "react-router";

export async function avtaleLoader({ params }: LoaderFunctionArgs) {
  if (!params.avtaleId) {
    throw Error("Fant ikke avtaleId i route");
  }

  const [{ data: avtale }, { data: ansatt }] = await Promise.all([
    AvtalerService.getAvtale({ path: { id: params.avtaleId } }),
    AnsattService.hentInfoOmAnsatt(),
  ]);
  return { avtale, ansatt };
}

export async function avtaleSkjemaLoader({ params }: LoaderFunctionArgs) {
  const [{ data: avtale }, { data: tiltakstyper }, { data: ansatt }, { data: enheter }] =
    await Promise.all([
      params.avtaleId
        ? await AvtalerService.getAvtale({ path: { id: params.avtaleId } })
        : { data: undefined },
      TiltakstyperService.getTiltakstyper(),
      AnsattService.hentInfoOmAnsatt(),
      NavEnheterService.getEnheter({
        query: {
          statuser: [
            NavEnhetStatus.AKTIV,
            NavEnhetStatus.UNDER_AVVIKLING,
            NavEnhetStatus.UNDER_ETABLERING,
          ],
        },
      }),
    ]);

  return { avtale, tiltakstyper, ansatt, enheter };
}
