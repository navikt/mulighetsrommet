import { useApiQuery } from "@/hooks/useApiQuery";
import { OppgaverFilter } from "../atoms";
import { QueryKeys } from "@/api/QueryKeys";
import { OppgaverService, Tiltakskode } from "@mr/api-client-v2";

export function useOppgaver(filter: OppgaverFilter) {
  return useApiQuery({
    queryKey: QueryKeys.oppgaver(filter),
    queryFn: () =>
      OppgaverService.getOppgaver({
        query: {
          tiltakstyper: filter.tiltakstyper as Tiltakskode[],
          oppgavetyper: filter.type,
        },
      }),
  });
}
