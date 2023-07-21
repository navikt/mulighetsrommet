import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../../QueryKeys";
import { mulighetsrommetClient } from "../../clients";
import invariant from "tiny-invariant";
import { useGetAdminTiltaksgjennomforingsIdFraUrl } from "../../../hooks/useGetAdminTiltaksgjennomforingsIdFraUrl";

export function useTiltaksgjennomforingsnotater() {
  const gjennomforingsId = useGetAdminTiltaksgjennomforingsIdFraUrl();
  invariant(gjennomforingsId, "Id for tiltaksgjennomføring er ikke satt i URL");
  return useQuery(
    QueryKeys.tiltaksgjennomforingsnotater(gjennomforingsId),
    () =>
      mulighetsrommetClient.tiltaksgjennomforingNotater.getNotaterForTiltaksgjennomforing(
        {
          tiltaksgjennomforingId: gjennomforingsId,
        },
      ),
    {
      enabled: !!gjennomforingsId,
    },
  );
}
