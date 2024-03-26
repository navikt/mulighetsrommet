import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { mulighetsrommetClient } from "@/api/client";

export function useTiltakstypeFaneinnhold(tiltakstypeId: string) {
  return useSuspenseQuery({
    queryKey: QueryKeys.tiltakstypeFaneinnhold(tiltakstypeId),
    queryFn: () =>
      mulighetsrommetClient.tiltakstyper.getTiltakstypeFaneinnhold({
        id: tiltakstypeId,
      }),
  });
}
