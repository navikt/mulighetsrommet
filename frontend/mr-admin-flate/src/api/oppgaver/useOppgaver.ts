import { useApiQuery } from "@mr/frontend-common";
import { OppgaverFilter } from "../atoms";
import { QueryKeys } from "@/api/QueryKeys";
import { OppgaverService, Tiltakskode } from "@mr/api-client-v2";

export function useOppgaver(filter: OppgaverFilter) {
  return useApiQuery({
    queryKey: QueryKeys.oppgaver({ ...filter }),
    queryFn: () =>
      OppgaverService.getOppgaver({
        body: {
          tiltakstyper: filter.tiltakstyper as Tiltakskode[],
          oppgavetyper: filter.type,
        },
      }),
  });
}
