import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useGjennomforingDeltakerSummary(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.gjennomforingDeltakerSummary(id),
    queryFn() {
      return GjennomforingService.getGjennomforingDeltakerSummary({
        path: { id },
      });
    },
  });
}
