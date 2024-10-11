import { QueryKeys } from "@/api/QueryKeys";
import { TiltaksgjennomforingerService } from "@mr/api-client";
import { useQuery, useSuspenseQuery } from "@tanstack/react-query";

function getDeltakerSummaryQuery(id: string) {
  return {
    queryKey: QueryKeys.tiltaksgjennomforingDeltakerSummary(id),
    queryFn() {
      return TiltaksgjennomforingerService.getTiltaksgjennomforingDeltakerSummary({
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
