import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltaksgjennomforingerService } from "@mr/api-client";

export function useTiltaksgjennomforingDeltakerSummary(id?: string) {
  return useSuspenseQuery({
    queryKey: QueryKeys.tiltaksgjennomforingDeltakerSummary(id!),
    queryFn() {
      return TiltaksgjennomforingerService.getTiltaksgjennomforingDeltakerSummary({
        id: id!,
      });
    },
  });
}
