import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "mulighetsrommet-veileder-flate/src/core/api/clients";
import { useGetAdminTiltaksgjennomforingsIdFraUrl } from "../../hooks/useGetAdminTiltaksgjennomforingsIdFraUrl";
import { QueryKeys } from "../QueryKeys";

export function useTiltaksgjennomforing(
  overstyrTiltaksgjennomforingsId?: string,
) {
  const tiltaksgjennomforingId =
    overstyrTiltaksgjennomforingsId ||
    useGetAdminTiltaksgjennomforingsIdFraUrl();

  const query = useQuery(
    QueryKeys.tiltaksgjennomforing(tiltaksgjennomforingId!!),
    () =>
      mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforing({
        id: tiltaksgjennomforingId!!,
      }),
    { enabled: !!tiltaksgjennomforingId },
  );
  return {
    ...query,
    isLoading: !!tiltaksgjennomforingId && query.isLoading, // https://github.com/TanStack/query/issues/3584
  }
}
