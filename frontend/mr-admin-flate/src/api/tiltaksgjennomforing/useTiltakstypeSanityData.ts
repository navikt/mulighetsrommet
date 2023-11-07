import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useTiltakstypeSanityData(tiltakstypeId: string) {
  return useQuery({
    queryKey: QueryKeys.veilederflateTiltaksgjennomforing(tiltakstypeId),
    queryFn: () =>
      mulighetsrommetClient.tiltakstyper.getSanityDataForTiltakstypeWithId({
        id: tiltakstypeId,
      }),
  });
}
