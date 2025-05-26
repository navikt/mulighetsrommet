import { QueryKeys } from "@/api/QueryKeys";
import { OppgaverService } from "@mr/api-client-v2";
import { useApiQuery } from "@mr/frontend-common";
import { OppgaverFilterType } from "@/pages/oppgaveoversikt/oppgaver/filter";

export function useOppgaver(filter: OppgaverFilterType) {
  return useApiQuery({
    queryKey: QueryKeys.oppgaver({ ...filter }),
    queryFn: () =>
      OppgaverService.getOppgaver({
        body: {
          tiltakskoder: filter.tiltakstyper,
          oppgavetyper: filter.type,
          regioner: filter.regioner,
        },
      }),
  });
}
