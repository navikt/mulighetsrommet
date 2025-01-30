import { NavEnheterService, TiltakstyperService } from "@mr/api-client-v2";

export async function oppgaverLoader() {
  const { data: tiltakstyper } = await TiltakstyperService.getTiltakstyper();
  const { data: regioner } = await NavEnheterService.getRegioner();

  return {
    tiltakstyper: tiltakstyper.data,
    regioner,
  };
}
