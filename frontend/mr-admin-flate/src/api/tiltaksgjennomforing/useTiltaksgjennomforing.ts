import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "mulighetsrommet-veileder-flate/src/core/api/clients";
import { useGetAdminTiltaksgjennomforingsIdFraUrl } from "../../hooks/useGetAdminTiltaksgjennomforingsIdFraUrl";
import { QueryKeys } from "../QueryKeys";

export function useTiltaksgjennomforing() {
  const tiltaksgjennomforingId = useGetAdminTiltaksgjennomforingsIdFraUrl();

  return useQuery(
    QueryKeys.tiltaksgjennomforing(tiltaksgjennomforingId!!),
    () =>
      mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforing({
        id: tiltaksgjennomforingId!!,
      }),
    { enabled: !!tiltaksgjennomforingId },
  );
}
