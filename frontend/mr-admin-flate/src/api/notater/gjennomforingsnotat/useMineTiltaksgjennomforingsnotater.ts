import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../../clients";
import { QueryKeys } from "../../QueryKeys";
import invariant from "tiny-invariant";
import { useGetAdminTiltaksgjennomforingsIdFraUrl } from "../../../hooks/useGetAdminTiltaksgjennomforingsIdFraUrl";

export function useMineTiltaksgjennomforingsnotater() {
  const tiltaksgjennomforingId = useGetAdminTiltaksgjennomforingsIdFraUrl();
  invariant(tiltaksgjennomforingId, "Id for tiltaksgjennomføring er ikke satt.");

  return useQuery(
    QueryKeys.mineTiltaksgjennomforingsnotater(tiltaksgjennomforingId),
    () =>
      mulighetsrommetClient.tiltaksgjennomforingNotater.getMineTiltaksgjennomforingNotater({
        tiltaksgjennomforingId,
      }),
    {},
  );
}
