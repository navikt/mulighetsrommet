import { TiltakstyperService } from "@mr/api-client";

export async function oppgaverLoader() {
  const tiltakstyper = await TiltakstyperService.getTiltakstyper({});

  return {
    tiltakstyper: tiltakstyper.data,
  };
}
