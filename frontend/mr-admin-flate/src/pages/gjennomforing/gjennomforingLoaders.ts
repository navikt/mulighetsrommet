import { AnsattService, AvtalerService, GjennomforingerService } from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router";

export async function gjennomforingLoader({ params }: LoaderFunctionArgs) {
  if (!params.gjennomforingId) {
    throw Error("Fant ikke gjennomforingId i route");
  }

  const [ansatt, gjennomforing] = await Promise.all([
    AnsattService.hentInfoOmAnsatt(),

    GjennomforingerService.getGjennomforing({
      id: params.gjennomforingId,
    }),
  ]);

  const avtaleId = params?.avtaleId || gjennomforing?.avtaleId;
  const avtale = avtaleId ? await AvtalerService.getAvtale({ id: avtaleId }) : undefined;

  return { gjennomforing, avtale, ansatt };
}

export async function gjennomforingFormLoader({ params }: LoaderFunctionArgs) {
  const [ansatt, gjennomforing] = await Promise.all([
    AnsattService.hentInfoOmAnsatt(),

    params.gjennomforingId
      ? await GjennomforingerService.getGjennomforing({
          id: params.gjennomforingId,
        })
      : undefined,
  ]);

  const avtaleId = params?.avtaleId || gjennomforing?.avtaleId;
  const avtale = avtaleId ? await AvtalerService.getAvtale({ id: avtaleId }) : undefined;

  return { gjennomforing, avtale, ansatt };
}
