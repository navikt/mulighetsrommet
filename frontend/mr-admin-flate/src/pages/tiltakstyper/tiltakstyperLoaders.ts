import { TiltakstyperService } from "@mr/api-client-v2";
import { LoaderFunctionArgs } from "react-router";

export async function tiltakstyperLoaders() {
  const data = await TiltakstyperService.getTiltakstyper();

  return data;
}

export async function tiltakstypeLoader({ params }: LoaderFunctionArgs) {
  if (!params.tiltakstypeId) throw Error("Fant ikke tiltakstypeId i route");

  const { data } = await TiltakstyperService.getTiltakstypeById({
    path: { id: params.tiltakstypeId },
  });
  return data;
}
