import { useQuery } from "@tanstack/react-query";
import { useGetAdminTiltaksgjennomforingsIdFraUrl } from "../../hooks/useGetAdminTiltaksgjennomforingsIdFraUrl";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useTiltaksgjennomforingById() {
  const tiltaksgjennomforingId = useGetAdminTiltaksgjennomforingsIdFraUrl();

  return useQuery(
    QueryKeys.tiltaksgjennomforing(tiltaksgjennomforingId),
    () =>
      mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforing({
        id: tiltaksgjennomforingId!!,
      }),
    {
      enabled: !!tiltaksgjennomforingId,
    },
  );
}
