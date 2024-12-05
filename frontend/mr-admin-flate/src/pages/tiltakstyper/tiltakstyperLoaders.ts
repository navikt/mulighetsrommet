import { TiltakstyperService } from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router-dom";

export async function tiltakstyperLoaders() {
  const data = await TiltakstyperService.getTiltakstyper();

  return data;
}

export async function tiltakstypeLoader({ params }: LoaderFunctionArgs) {
  if (!params.tiltakstypeId) throw Error("Fant ikke tiltakstypeId i route");

  return await TiltakstyperService.getTiltakstypeById({ id: params.tiltakstypeId });
}
