import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";
import { useGetTiltaksgjennomforingIdFraUrl } from "./useGetTiltaksgjennomforingIdFraUrl";
import {
  useArbeidsmarkedstiltakFilterValue,
  valgteEnhetsnumre,
} from "../../../hooks/useArbeidsmarkedstiltakFilter";

export function useTiltaksgjennomforingById() {
  const id = useGetTiltaksgjennomforingIdFraUrl();
  const filter = useArbeidsmarkedstiltakFilterValue();

  const requestBody = { enheter: valgteEnhetsnumre(filter), id };

  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforing(id),
    queryFn: () =>
      mulighetsrommetClient.veilederTiltak.getVeilederTiltaksgjennomforing({ requestBody }),
  });
}

export function useNavTiltaksgjennomforingById() {
  const id = useGetTiltaksgjennomforingIdFraUrl();

  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforing(id),
    queryFn: () =>
      mulighetsrommetClient.veilederTiltak.getNavTiltaksgjennomforing({
        requestBody: { id },
      }),
  });
}

export function usePreviewTiltaksgjennomforingById() {
  const id = useGetTiltaksgjennomforingIdFraUrl();
  const filter = useArbeidsmarkedstiltakFilterValue();

  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforingPreview(id),
    queryFn: () =>
      mulighetsrommetClient.veilederTiltak.getPreviewTiltaksgjennomforing({
        requestBody: { id, enheter: valgteEnhetsnumre(filter) },
      }),
  });
}
