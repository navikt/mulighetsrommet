import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useTiltakstypeFaneinnhold(tiltakstypeId: string) {
  return useQuery({
    queryKey: QueryKeys.veilederflateTiltaksgjennomforing(tiltakstypeId),
    queryFn: () =>
      mulighetsrommetClient.tiltakstyper.getTiltakstypeFaneinnhold({
        id: tiltakstypeId,
      }),
  });
}
