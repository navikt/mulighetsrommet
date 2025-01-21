import { useApiSuspenseQuery } from "@/hooks/useApiQuery";
import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingerService } from "@mr/api-client-v2";

export function useGjennomforingEndringshistorikk(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.gjennomforingHistorikk(id),
    queryFn() {
      return GjennomforingerService.getGjennomforingEndringshistorikk({
        path: { id },
      });
    },
  });
}
