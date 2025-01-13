import { useQuery } from "@tanstack/react-query";
import { OppgaverFilter } from "../atoms";
import { QueryKeys } from "@/api/QueryKeys";
import { OppgaverService, Tiltakskode } from "@mr/api-client";

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
