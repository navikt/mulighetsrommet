import { AnsattService, AvtalerService, GjennomforingerService } from "@mr/api-client-v2";
import { LoaderFunctionArgs } from "react-router";

export async function gjennomforingLoader({ params }: LoaderFunctionArgs) {
  if (!params.gjennomforingId) {
    throw Error("Fant ikke gjennomforingId i route");
  }

  const [{ data: ansatt }, { data: gjennomforing }] = await Promise.all([
    AnsattService.hentInfoOmAnsatt(),

    GjennomforingerService.getGjennomforing({
      path: { id: params.gjennomforingId },
    }),
  ]);

  const avtaleId = params?.avtaleId || gjennomforing?.avtaleId;
  const { data: avtale } = avtaleId
    ? await AvtalerService.getAvtale({ path: { id: avtaleId } })
    : { data: undefined };

  return { gjennomforing, avtale, ansatt };
}

export async function gjennomforingFormLoader({ params }: LoaderFunctionArgs) {
  const [{ data: ansatt }, { data: gjennomforing }] = await Promise.all([
    AnsattService.hentInfoOmAnsatt(),

    params.gjennomforingId
      ? await GjennomforingerService.getGjennomforing({
          path: { id: params.gjennomforingId },
        })
      : { data: undefined },
  ]);

  const avtaleId = params?.avtaleId || gjennomforing?.avtaleId;
  const { data: avtale } = avtaleId
    ? await AvtalerService.getAvtale({ path: { id: avtaleId } })
    : { data: undefined };

  return { gjennomforing, avtale, ansatt };
}
