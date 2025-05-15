import { QueryKeys } from "@/api/QueryKeys";
import { OppgaverService } from "@mr/api-client-v2";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useGetOppgavetyper() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.oppgavetyper(),
    queryFn: () => OppgaverService.getOppgavetyper(),
  });
}
