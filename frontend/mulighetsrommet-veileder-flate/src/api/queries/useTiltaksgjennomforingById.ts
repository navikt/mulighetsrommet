import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../client";
import { QueryKeys } from "../query-keys";
import { useGetTiltaksgjennomforingIdFraUrl } from "./useGetTiltaksgjennomforingIdFraUrl";
import { useArbeidsmarkedstiltakFilterValue } from "@/hooks/useArbeidsmarkedstiltakFilter";
import { NavEnhet } from "mulighetsrommet-api-client";

export function useTiltaksgjennomforingById() {
  const id = useGetTiltaksgjennomforingIdFraUrl();
  const filter = useArbeidsmarkedstiltakFilterValue();

  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforing(id),
    queryFn: () =>
      mulighetsrommetClient.veilederTiltak.getVeilederTiltaksgjennomforing({
        id,
        enheter: filter.navEnheter.map((enhet: NavEnhet) => enhet.enhetsnummer),
      }),
  });
}

export function useNavTiltaksgjennomforingById() {
  const id = useGetTiltaksgjennomforingIdFraUrl();

  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforing(id),
    queryFn: () => mulighetsrommetClient.veilederTiltak.getNavTiltaksgjennomforing({ id }),
  });
}

export function usePreviewTiltaksgjennomforingById() {
  const id = useGetTiltaksgjennomforingIdFraUrl();
  const filter = useArbeidsmarkedstiltakFilterValue();

  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforingPreview(id),
    queryFn: () =>
      mulighetsrommetClient.veilederTiltak.getPreviewTiltaksgjennomforing({
        id,
        enheter: filter.navEnheter.map((enhet: NavEnhet) => enhet.enhetsnummer),
      }),
  });
}
