import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import {
  isFilterReady,
  useArbeidsmarkedstiltakFilterValue,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import { NavEnhet, VeilederTiltakService } from "@mr/api-client";

export function useModiaArbeidsmarkedstiltak() {
  const { isFilterReady, filter } = useGetArbeidsmarkedstiltakFilterAsQuery();

  return useQuery({
    queryKey: QueryKeys.arbeidsmarkedstiltak.tiltak(filter),
    queryFn: () => VeilederTiltakService.getAllVeilederTiltak(filter),
    enabled: isFilterReady,
  });
}

export function useNavArbeidsmarkedstiltak({ preview }: { preview: boolean }) {
  const { isFilterReady, filter } = useGetArbeidsmarkedstiltakFilterAsQuery();

  return useQuery({
    queryKey: QueryKeys.arbeidsmarkedstiltak.tiltak({ ...filter, preview }),
    queryFn() {
      return preview
        ? VeilederTiltakService.getAllPreviewTiltak(filter)
        : VeilederTiltakService.getAllNavTiltak(filter);
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
      apentForPamelding: filter.apentForPamelding,
      innsatsgruppe: filter.innsatsgruppe?.nokkel,
      enheter: filter.navEnheter.map((enhet: NavEnhet) => enhet.enhetsnummer),
      tiltakstyper,
      erSykmeldtMedArbeidsgiver: filter.erSykmeldtMedArbeidsgiver,
    },
  };
}
