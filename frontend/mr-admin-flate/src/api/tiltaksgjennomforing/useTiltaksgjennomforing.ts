import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "mulighetsrommet-veileder-flate/src/core/api/clients";
import invariant from "tiny-invariant";
import { QueryKeys } from "../QueryKeys";
import { useGetAdminTiltaksgjennomforingsIdFraUrl } from "../../hooks/useGetAdminTiltaksgjennomforingsIdFraUrl";

export function useTiltaksgjennomforing() {
  const tiltaksgjennomforingId = useGetAdminTiltaksgjennomforingsIdFraUrl();
  invariant(
    tiltaksgjennomforingId,
    "Klarte ikke hente id for tiltaksgjennomføring.",
  );

  return useQuery(
    QueryKeys.tiltaksgjennomforing(tiltaksgjennomforingId!!),
    () =>
      mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforing({
        id: tiltaksgjennomforingId,
      }),
    {},
  );
}
