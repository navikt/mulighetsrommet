import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import {
  isFilterReady,
  useArbeidsmarkedstiltakFilterValue,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import { NavEnhet, VeilederTiltakService } from "@mr/api-client";

export function useVeilederTiltaksgjennomforinger() {
  const { isFilterReady, filter } = useGetArbeidsmarkedstiltakFilterAsQuery();

  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforinger(filter),
    queryFn: () => VeilederTiltakService.getVeilederTiltaksgjennomforinger(filter),
    enabled: isFilterReady,
  });
}

export function useNavTiltaksgjennomforinger({ preview }: { preview: boolean }) {
  const { isFilterReady, filter } = useGetArbeidsmarkedstiltakFilterAsQuery();

  return useQuery({
    queryKey: QueryKeys.sanity.tiltaksgjennomforinger({ ...filter, preview }),
    queryFn() {
      return preview
        ? VeilederTiltakService.getPreviewTiltaksgjennomforinger(filter)
        : VeilederTiltakService.getNavTiltaksgjennomforinger(filter);
    },
    enabled: isFilterReady,
  });
}

function useGetArbeidsmarkedstiltakFilterAsQuery() {
  const filter = useArbeidsmarkedstiltakFilterValue();

  const tiltakstyper =
    filter.tiltakstyper.length !== 0 ? filter.tiltakstyper.map(({ id }) => id) : undefined;

  return {
    isFilterReady: isFilterReady(filter),
    filter: {
      search: filter.search || undefined,
      apentForInnsok: filter.apentForInnsok,
      innsatsgruppe: filter.innsatsgruppe?.nokkel,
      enheter: filter.navEnheter.map((enhet: NavEnhet) => enhet.enhetsnummer),
      tiltakstyper,
    },
  };
}
