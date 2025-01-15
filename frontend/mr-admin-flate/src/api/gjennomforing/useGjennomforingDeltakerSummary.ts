import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingerService } from "@mr/api-client";
import { useQuery, useSuspenseQuery } from "@tanstack/react-query";

function getDeltakerSummaryQuery(id: string) {
  return {
    queryKey: QueryKeys.gjennomforingDeltakerSummary(id),
    queryFn() {
      return GjennomforingerService.getGjennomforingDeltakerSummary({
        id,
      });
    },
  };
}

export function useSuspenseGjennomforingDeltakerSummary(id: string) {
  return useSuspenseQuery(getDeltakerSummaryQuery(id));
}

export function useGjennomforingDeltakerSummary(id?: string) {
  return useQuery({
    ...getDeltakerSummaryQuery(id ?? ""),
    enabled: !!id,
  });
}
