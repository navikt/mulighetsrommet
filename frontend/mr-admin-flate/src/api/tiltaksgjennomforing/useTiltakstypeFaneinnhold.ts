import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useTiltakstypeFaneinnhold(tiltakstypeId: string) {
  return useSuspenseQuery({
    queryKey: QueryKeys.tiltakstypeFaneinnhold(tiltakstypeId),
    queryFn: () =>
      mulighetsrommetClient.tiltakstyper.getTiltakstypeFaneinnhold({
        id: tiltakstypeId,
      }),
  });
}
