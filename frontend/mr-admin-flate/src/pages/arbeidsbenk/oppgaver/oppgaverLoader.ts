import { OppgaverService, TiltakstyperService } from "@mr/api-client";

export async function oppgaverLoader() {
  const oppgaver = await OppgaverService.getOppgaver({
    tiltakstyper: [],
    oppgavetyper: [],
  });

  const tiltakstyper = await TiltakstyperService.getTiltakstyper({});

  return {
    oppgaver,
    tiltakstyper: tiltakstyper.data,
  };
}
