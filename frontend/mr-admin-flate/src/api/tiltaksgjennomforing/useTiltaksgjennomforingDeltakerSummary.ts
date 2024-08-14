import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltaksgjennomforingerService } from "@mr/api-client";

export function useTiltaksgjennomforingDeltakerSummary(id?: string) {
  return useQuery({
    queryKey: QueryKeys.tiltaksgjennomforingDeltakerSummary(id!),
    queryFn() {
      return TiltaksgjennomforingerService.getTiltaksgjennomforingDeltakerSummary({
        id: id!,
      });
    },
    throwOnError: false,
    enabled: !!id,
  });
}
