import { QueryKeys } from "@/api/QueryKeys";
import { OppgaverService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useGetOppgavetyper() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.oppgavetyper(),
    queryFn: () => OppgaverService.getOppgavetyper(),
  });
}
