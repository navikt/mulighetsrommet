import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingerService } from "@mr/api-client-v2";
import { useApiQuery, useApiSuspenseQuery } from "@mr/frontend-common";

function getDeltakerSummaryQuery(id: string) {
  return {
    queryKey: QueryKeys.gjennomforingDeltakerSummary(id),
    queryFn() {
      return GjennomforingerService.getGjennomforingDeltakerSummary({
        path: { id },
      });
    },
  };
}

export function useSuspenseGjennomforingDeltakerSummary(id: string) {
  return useApiSuspenseQuery(getDeltakerSummaryQuery(id));
}

export function useGjennomforingDeltakerSummary(id?: string) {
  return useApiQuery({
    ...getDeltakerSummaryQuery(id ?? ""),
    enabled: !!id,
  });
}
