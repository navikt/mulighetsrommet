import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingerService } from "@mr/api-client-v2";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useAdminGjennomforingById(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.gjennomforing(id),
    queryFn: () => GjennomforingerService.getGjennomforing({ path: { id } }),
  });
}

export function useGjennomforingHandlinger(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.gjennomforingHandlinger(id),
    queryFn: () => GjennomforingerService.getGjennomforingHandlinger({ path: { id } }),
  });
}
