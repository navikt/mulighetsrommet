import { OppgaverService, OppgaveType, Tiltakskode, TiltakstyperService } from "@mr/api-client";
import { OppgaverFilter } from "@/api/atoms";

export async function oppgaverLoader(filter: OppgaverFilter) {
  const oppgaver = await OppgaverService.getOppgaver({
    tiltakstyper: filter.tiltakstyper as Tiltakskode[],
    oppgavetyper: filter.type as unknown as Array<OppgaveType>,
  });

  const tiltakstyper = await TiltakstyperService.getTiltakstyper({});

  return {
    oppgaver,
    tiltakstyper: tiltakstyper.data,
  };
}
