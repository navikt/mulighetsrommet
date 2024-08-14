import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { useGetTiltaksgjennomforingIdFraUrl } from "@/hooks/useGetTiltaksgjennomforingIdFraUrl";
import { useArbeidsmarkedstiltakFilterValue } from "@/hooks/useArbeidsmarkedstiltakFilter";
import { NavEnhet, VeilederTiltakService } from "@mr/api-client";

export function useTiltaksgjennomforingById() {
  const id = useGetTiltaksgjennomforingIdFraUrl();
  const filter = useArbeidsmarkedstiltakFilterValue();

  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforing(id),
    queryFn: () =>
      VeilederTiltakService.getVeilederTiltaksgjennomforing({
        id,
        enheter: filter.navEnheter.map((enhet: NavEnhet) => enhet.enhetsnummer),
      }),
  });
}

export function useNavTiltaksgjennomforingById() {
  const id = useGetTiltaksgjennomforingIdFraUrl();

  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforing(id),
    queryFn: () => VeilederTiltakService.getNavTiltaksgjennomforing({ id }),
  });
}

export function usePreviewTiltaksgjennomforingById() {
  const id = useGetTiltaksgjennomforingIdFraUrl();
  const filter = useArbeidsmarkedstiltakFilterValue();

  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforingPreview(id),
    queryFn: () =>
      VeilederTiltakService.getPreviewTiltaksgjennomforing({
        id,
        enheter: filter.navEnheter.map((enhet: NavEnhet) => enhet.enhetsnummer),
      }),
  });
}
