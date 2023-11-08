import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "mulighetsrommet-veileder-flate/src/core/api/clients";
import { useGetAdminTiltaksgjennomforingsIdFraUrl } from "../../hooks/useGetAdminTiltaksgjennomforingsIdFraUrl";
import { QueryKeys } from "../QueryKeys";

export function useTiltaksgjennomforing(overstyrTiltaksgjennomforingsId?: string) {
  const tiltaksgjennomforingId =
    overstyrTiltaksgjennomforingsId || useGetAdminTiltaksgjennomforingsIdFraUrl();

  return useQuery({
    queryKey: QueryKeys.tiltaksgjennomforing(tiltaksgjennomforingId!!),
    queryFn: () =>
      mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforing({
        id: tiltaksgjennomforingId!!,
      }),
    enabled: !!tiltaksgjennomforingId,
  });
}
