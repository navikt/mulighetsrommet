import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useTiltaksgjennomforingDeltakerSummary(id: string) {
  return useQuery({
    queryKey: QueryKeys.tiltaksgjennomforingDeltakerSummary(id),
    queryFn() {
      return mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforingDeltakerSummary({
        id,
      });
    },
    throwOnError: false,
  });
}
