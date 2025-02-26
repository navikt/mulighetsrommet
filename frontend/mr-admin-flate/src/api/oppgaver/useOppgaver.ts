import { QueryKeys } from "@/api/QueryKeys";
import { OppgaverService, Tiltakskode } from "@mr/api-client-v2";
import { useApiQuery } from "@mr/frontend-common";
import { OppgaverFilter } from "../atoms";

export function useOppgaver(filter: OppgaverFilter) {
  return useApiQuery({
    queryKey: QueryKeys.oppgaver({ ...filter }),
    queryFn: () =>
      OppgaverService.getOppgaver({
        body: {
          tiltakskoder: filter.tiltakstyper as Tiltakskode[],
          oppgavetyper: filter.type,
          regioner: filter.regioner,
        },
      }),
  });
}
