import { TiltakstyperService } from "@mr/api-client-v2";

export async function oppgaverLoader() {
  const { data: tiltakstyper } = await TiltakstyperService.getTiltakstyper();

  return {
    tiltakstyper: tiltakstyper.data,
  };
}
