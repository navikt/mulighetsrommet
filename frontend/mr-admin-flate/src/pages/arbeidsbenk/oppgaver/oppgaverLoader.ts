import { OppgaverService } from "@mr/api-client";

export async function oppgaverLoader() {
  const oppgaver = await OppgaverService.getOppgaver({
    tiltakstyper: [],
    oppgavetyper: [],
  });

  return oppgaver;
}
