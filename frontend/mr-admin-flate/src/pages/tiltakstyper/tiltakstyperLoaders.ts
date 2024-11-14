import { TiltakstyperService } from "@mr/api-client";

export async function tiltakstyperLoaders() {
  const data = await TiltakstyperService.getTiltakstyper();

  return data;
}
