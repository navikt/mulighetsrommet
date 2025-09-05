import { QueryKeys } from "@/api/QueryKeys";
import { OppgaverService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { OppgaverFilterType } from "@/pages/oppgaveoversikt/oppgaver/filter";

export function useOppgaver(filter: OppgaverFilterType) {
  return useApiSuspenseQuery({
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
