import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useTiltakstypeSanityData(tiltakstypeId: string) {
  return useQuery(
    QueryKeys.veilederflateTiltaksgjennomforing(tiltakstypeId),
    () =>
      mulighetsrommetClient.tiltakstyper.getSanityDataForTiltakstypeWithId({
        id: tiltakstypeId,
      }),
    {
      enabled: !!tiltakstypeId,
    },
  );
}
