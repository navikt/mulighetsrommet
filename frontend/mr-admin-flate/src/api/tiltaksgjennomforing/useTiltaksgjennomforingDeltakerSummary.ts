import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { mulighetsrommetClient } from "@/api/client";

export function useTiltaksgjennomforingDeltakerSummary(id?: string) {
  return useQuery({
    queryKey: QueryKeys.tiltaksgjennomforingDeltakerSummary(id!!),
    queryFn() {
      return mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforingDeltakerSummary({
        id: id!!,
      });
    },
    throwOnError: false,
    enabled: !!id,
  });
}
