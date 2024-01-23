import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";
import { useGetTiltaksgjennomforingIdFraUrl } from "./useGetTiltaksgjennomforingIdFraUrl";
import {
  navEnheter,
  useArbeidsmarkedstiltakFilterValue,
} from "../../../hooks/useArbeidsmarkedstiltakFilter";

export default function useTiltaksgjennomforingById() {
  const id = useGetTiltaksgjennomforingIdFraUrl();
  const filter = useArbeidsmarkedstiltakFilterValue();

  const requestBody = { enheter: navEnheter(filter), id };

  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforing(id),
    queryFn: () => mulighetsrommetClient.veilederTiltak.getTiltaksgjennomforing({ requestBody }),
  });
}
