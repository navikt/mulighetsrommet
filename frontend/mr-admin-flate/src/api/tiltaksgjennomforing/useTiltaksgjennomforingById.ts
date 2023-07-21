import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";
import { useGetAdminTiltaksgjennomforingsIdFraUrl } from "../../hooks/useGetAdminTiltaksgjennomforingsIdFraUrl";
import invariant from "tiny-invariant";

export function useTiltaksgjennomforingById() {
  const tiltaksgjennomforingId = useGetAdminTiltaksgjennomforingsIdFraUrl();
  invariant(
    tiltaksgjennomforingId,
    "Klarte ikke hente id for tiltaksgjennomføring.",
  );

  return useQuery(
    QueryKeys.tiltaksgjennomforing(tiltaksgjennomforingId),
    () =>
      mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforing({
        id: tiltaksgjennomforingId,
      }),
    {},
  );
}
