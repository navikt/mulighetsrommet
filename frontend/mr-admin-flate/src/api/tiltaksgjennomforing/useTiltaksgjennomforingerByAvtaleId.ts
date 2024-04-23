import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { mulighetsrommetClient } from "@/api/client";

export function useTiltaksgjennomforingerByAvtaleId(avtaleId: string) {
  return useQuery({
    queryKey: QueryKeys.tiltaksgjennomforing(avtaleId),
    queryFn: () =>
      mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforinger({
        avtaleId: avtaleId!!,
      }),
  });
}
