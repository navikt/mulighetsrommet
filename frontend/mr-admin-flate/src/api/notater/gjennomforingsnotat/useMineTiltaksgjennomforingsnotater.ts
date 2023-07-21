import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../../clients";
import { QueryKeys } from "../../QueryKeys";
import invariant from "tiny-invariant";
import { useGetAdminTiltaksgjennomforingsIdFraUrl } from "../../../hooks/useGetAdminTiltaksgjennomforingsIdFraUrl";

export function useMineTiltaksgjennomforingsnotater() {
  const gjennomforingsId = useGetAdminTiltaksgjennomforingsIdFraUrl();
  invariant(gjennomforingsId, "Id for tiltaksgjennomføring er ikke satt.");

  return useQuery(
    QueryKeys.mineTiltaksgjennomforingsnotater(gjennomforingsId!!),
    () =>
      mulighetsrommetClient.tiltaksgjennomforingNotater.getMineTiltaksgjennomforingNotater(
        {
          tiltaksgjennomforingId: gjennomforingsId!,
        },
      ),
    {},
  );
}
