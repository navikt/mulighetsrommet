import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingService } from "@tiltaksadministrasjon/api-client";

export function useGjennomforingEndringshistorikk(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.gjennomforingHistorikk(id),
    queryFn() {
      return GjennomforingService.getGjennomforingEndringshistorikk({
        path: { id },
      });
    },
  });
}
