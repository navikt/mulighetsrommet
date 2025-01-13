import { useQuery } from "@tanstack/react-query";
import { PAGE_SIZE } from "@/constants";
import { OppgaverFilter, TiltakstypeFilter } from "../atoms";
import { QueryKeys } from "@/api/QueryKeys";
import { OppgaverService, OppgaveType, Tiltakskode, TiltakstyperService } from "@mr/api-client";

export function useOppgaver(filter: OppgaverFilter) {
  return useQuery({
    queryKey: QueryKeys.oppgaver(filter),
    queryFn: () =>
      OppgaverService.getOppgaver({
        tiltakstyper: filter.tiltakstyper as Tiltakskode[],
        oppgavetyper: filter.type,
      }),
  });
}
